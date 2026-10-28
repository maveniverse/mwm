/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Config;
import eu.maveniverse.maven.mwm.core.Version;
import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class DefaultWorkspaceManager implements WorkspaceManager {
    private static final String WORKSPACE_LINKS = "workspace.links";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigurationManager configurationManager;
    private final PropertiesManager propertiesManager;

    @Inject
    public DefaultWorkspaceManager(ConfigurationManager configurationManager, PropertiesManager propertiesManager) {
        this.configurationManager = requireNonNull(configurationManager);
        this.propertiesManager = requireNonNull(propertiesManager);
        logger.debug("MWM {}", Version.version());
    }

    @Override
    public Optional<Workspace> detectWorkspace(Path pd, Path ld, Map<String, String> properties, boolean persist)
            throws IOException {
        Optional<Workspace> workspace = doDetectWorkspace(new HashSet<>(), pd, ld, properties);
        if (persist && workspace.isPresent()) {
            persistWorkspace(workspace.get());
        }
        return workspace;
    }

    /**
     * Saves workspace (and linked ones) properties, if changed.
     */
    private void persistWorkspace(Workspace workspace) throws IOException {
        if (!configurationManager
                .loadWorkspace(workspace.projectDirectory())
                .equals(Optional.of(workspace.properties()))) {
            configurationManager.saveWorkspace(workspace.projectDirectory(), workspace.properties());
        }
        for (Workspace linked : workspace.linkedWorkspaces()) {
            persistWorkspace(linked);
        }
    }

    @Override
    public boolean linkWorkspace(Workspace target, Workspace other) throws IOException {
        if (target.projectDirectory().equals(other.projectDirectory())) {
            throw new IOException("Cannot link Workspace to itself");
        }
        // target may have never been persisted (ie. detected by plugin only)
        Map<String, String> props =
                configurationManager.loadWorkspace(target.projectDirectory()).orElseGet(HashMap::new);
        String link = other.projectDirectory().toString();
        if (props.containsKey(WORKSPACE_LINKS)) {
            List<String> links = Arrays.asList(props.get(WORKSPACE_LINKS).split(","));
            if (links.contains(link)) {
                return false;
            }
            props.put(WORKSPACE_LINKS, props.get(WORKSPACE_LINKS) + "," + link);
        } else {
            props.put(WORKSPACE_LINKS, link);
        }
        configurationManager.saveWorkspace(target.projectDirectory(), props);
        return true;
    }

    @Override
    public boolean unlinkWorkspace(Workspace target, Workspace other) throws IOException {
        Map<String, String> props =
                configurationManager.loadWorkspace(target.projectDirectory()).orElseGet(HashMap::new);
        if (props.containsKey(WORKSPACE_LINKS)) {
            List<String> links =
                    new ArrayList<>(Arrays.asList(props.get(WORKSPACE_LINKS).split(",")));
            boolean res = links.remove(other.projectDirectory().toString());
            if (res) {
                if (links.isEmpty()) {
                    props.remove(WORKSPACE_LINKS);
                } else {
                    props.put(WORKSPACE_LINKS, String.join(",", links));
                }
                configurationManager.saveWorkspace(target.projectDirectory(), props);
            }
            return res;
        } else {
            return false;
        }
    }

    private Optional<Workspace> doDetectWorkspace(
            Set<Path> processedPds, Path pd, Path ld, Map<String, String> properties) throws IOException {
        Path projectDirectory = FileUtils.normalizePath(pd);
        Path localRepository = FileUtils.normalizePath(ld);
        processedPds.add(projectDirectory);
        Optional<Map<String, String>> propsOptional = propertiesManager.maySeedProperties(projectDirectory, properties);
        if (propsOptional.isPresent()) {
            Map<String, String> props = propsOptional.orElse(Collections.emptyMap());
            final Config config = configurationManager.getConfig(projectDirectory, props);
            if (config.getBuildOutputScope() != Config.Scope.USER || config.getBuildCacheScope() != Config.Scope.USER) {
                Optional<Workspace> wo =
                        detectWorkspace(config, processedPds, projectDirectory, localRepository, props);
                if (wo.isPresent()) {
                    logger.debug("Workspace detected");
                    return wo;
                } else {
                    logger.debug("No workspace detected");
                }
            } else {
                logger.debug("MWM configured to not interfere");
            }
        }
        return Optional.empty();
    }

    private Optional<Workspace> workspaceReDetector(
            Set<Path> processedProjectPaths,
            Path projectDirectory,
            Path localRepository,
            Map<String, String> properties)
            throws IOException {
        // cleanse to force Nisse invocation for another directory
        return doDetectWorkspace(
                processedProjectPaths,
                projectDirectory,
                localRepository,
                propertiesManager.cleanseProperties(properties));
    }

    /**
     * Parses remote URL into {@code [host, owner..., repository]}. Supports scp-like {@code git@host:owner/repo.git}
     * and URL forms like {@code https://host/owner/repo.git} or {@code ssh://git@host:22/owner/repo.git}.
     */
    static String[] parseRemoteUrl(String remoteUrl) {
        String url = remoteUrl.trim().replaceFirst("\\.git/?$", "");
        boolean hasScheme = url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
        url = url.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "").replaceFirst("^[^@/]+@", "");
        if (hasScheme) {
            url = url.replaceFirst("^([^/:]+):\\d+/", "$1/"); // drop port
        }
        return url.split("[/:]+");
    }

    private Optional<Workspace> detectWorkspace(
            Config config,
            Set<Path> processedProjectPaths,
            Path projectDirectory,
            Path localRepository,
            Map<String, String> properties)
            throws IOException {
        final String remoteName = properties.get(PropertiesManager.KEY_REMOTE_NAME);
        final String remoteUrl = properties.get(PropertiesManager.KEY_REMOTE_URL);
        final String branchName = properties.get(PropertiesManager.KEY_BRANCH_NAME);
        final String commonDir = properties.get(PropertiesManager.KEY_COMMON_DIR);

        if (remoteName != null && remoteUrl != null && branchName != null) {
            // we want DiscriminatorElements elements
            String[] shavenUrl = parseRemoteUrl(remoteUrl);
            if (shavenUrl.length < 3) {
                return Optional.empty();
            }

            // start fresh to not carry stale keys; only links are user state
            Map<String, String> elems = new HashMap<>();
            configurationManager
                    .loadWorkspace(projectDirectory)
                    .map(e -> e.get(WORKSPACE_LINKS))
                    .ifPresent(l -> elems.put(WORKSPACE_LINKS, l));
            elems.put(Config.DiscriminatorElements.REMOTE.name(), remoteName);
            elems.put(Config.DiscriminatorElements.HOST.name(), shavenUrl[0]);
            elems.put(
                    Config.DiscriminatorElements.OWNER.name(),
                    String.join("-", Arrays.asList(shavenUrl).subList(1, shavenUrl.length - 1)));
            elems.put(Config.DiscriminatorElements.REPOSITORY.name(), shavenUrl[shavenUrl.length - 1]);
            elems.put(Config.DiscriminatorElements.BRANCH.name(), branchName);

            ArrayList<String> workspaceIdArr = new ArrayList<>();
            ArrayList<String> workspaceDiscriminatorArr = new ArrayList<>();
            for (Config.DiscriminatorElements e : Config.DiscriminatorElements.values()) {
                // ID is used as directory name: branch "feature/x" must not create nested directories
                String part = elems.get(e.name()).replaceAll("[^A-Za-z0-9._-]", "_");
                workspaceIdArr.add(part);
                if (config.getDiscriminatorElements().contains(e)) {
                    workspaceDiscriminatorArr.add(part);
                }
            }
            final String workspaceId = String.join("-", workspaceIdArr);
            final String workspaceDiscriminator = String.join("-", workspaceDiscriminatorArr);
            logger.debug("WS {} ({})", workspaceId, workspaceDiscriminator);

            final Path buildCacheDirectory = resolveWorkspacePath(
                    config, config.getBuildCacheScope(), projectDirectory, localRepository, true, workspaceId);
            final Path buildOutputDirectory = resolveWorkspacePath(
                    config, config.getBuildOutputScope(), projectDirectory, localRepository, false, workspaceId);

            elems.put("git.remoteName", remoteName);
            elems.put("git.remoteUrl", remoteUrl);
            elems.put("git.branchName", branchName);
            if (commonDir != null) {
                elems.put("git.commonDir", commonDir);
            }
            elems.put("workspace.id", workspaceId);
            elems.put("workspace.discriminator", workspaceDiscriminator);
            ArrayList<Workspace> linkedWorkspaces = new ArrayList<>();
            if (config.isWorktreeJoined() && commonDir != null) {
                Path commonProjectDir = Paths.get(commonDir);
                if (Files.isDirectory(commonProjectDir)
                        && commonProjectDir.getParent() != null
                        && Files.isDirectory(commonProjectDir.getParent())
                        && !processedProjectPaths.contains(FileUtils.normalizePath(commonProjectDir.getParent()))) {
                    workspaceReDetector(
                                    processedProjectPaths, commonProjectDir.getParent(), localRepository, properties)
                            .ifPresent(linkedWorkspaces::add);
                }
            }
            if (elems.containsKey(WORKSPACE_LINKS)) {
                String[] links = elems.get(WORKSPACE_LINKS).split(",");
                for (String link : links) {
                    Path projectDir = Paths.get(link);
                    if (processedProjectPaths.contains(FileUtils.normalizePath(projectDir))) {
                        // cycle or diamond: already part of this chain
                        logger.debug("Linked workspace already processed: {}", link);
                    } else if (Files.isDirectory(projectDir)) {
                        Optional<Workspace> linkedWs =
                                workspaceReDetector(processedProjectPaths, projectDir, localRepository, properties);
                        if (linkedWs.isPresent()) {
                            linkedWorkspaces.add(linkedWs.get());
                        } else {
                            if (config.isLinkEnforced()) {
                                throw new IOException("Linked workspace at path not found: " + link);
                            } else {
                                logger.warn("Linked workspace at path not found: {}", link);
                            }
                        }
                    } else {
                        if (config.isLinkEnforced()) {
                            throw new IOException("Stale workspace link to path: " + link);
                        } else {
                            logger.warn("Stale workspace link to path: {}", link);
                        }
                    }
                }
            }
            return Optional.of(new WorkspaceImpl(
                    workspaceId,
                    workspaceDiscriminator,
                    elems,
                    projectDirectory,
                    localRepository,
                    buildCacheDirectory,
                    buildOutputDirectory,
                    linkedWorkspaces));
        }
        return Optional.empty();
    }

    private Path resolveWorkspacePath(
            Config config,
            Config.Scope scope,
            Path projectDirectory,
            Path localRepository,
            boolean cache,
            String workspaceId) {
        if (scope == Config.Scope.PROJECT) {
            if (cache) {
                return projectDirectory.resolve(Config.MVN_TARGET).resolve(config.cachedDir());
            } else {
                return projectDirectory.resolve(Config.MVN_TARGET).resolve(config.installedDir());
            }
        } else if (scope == Config.Scope.USER_SCOPED) {
            if (cache) {
                return localRepository.resolve(config.cachedDir()).resolve(workspaceId);
            } else {
                return localRepository.resolve(config.installedDir()).resolve(workspaceId);
            }
        } else if (scope == Config.Scope.USER) {
            return localRepository;
        } else {
            throw new IllegalArgumentException("Invalid build cache scope provided: " + scope);
        }
    }
}
