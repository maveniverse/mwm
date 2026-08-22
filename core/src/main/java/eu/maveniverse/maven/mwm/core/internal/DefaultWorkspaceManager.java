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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class DefaultWorkspaceManager implements WorkspaceManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigurationManager configurationManager;
    private final PropertiesManager propertiesManager;

    @Inject
    public DefaultWorkspaceManager(ConfigurationManager configurationManager, PropertiesManager propertiesManager) {
        this.configurationManager = requireNonNull(configurationManager);
        this.propertiesManager = requireNonNull(propertiesManager);
        logger.info("MWM {}", Version.version());
    }

    @Override
    public Collection<Workspace> listAll() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Optional<Workspace> lookup(String workspaceId) throws IOException {
        return Optional.empty();
    }

    @Override
    public boolean drop(String workspaceId, boolean purge) throws IOException {
        return false;
    }

    @Override
    public Optional<Workspace> detectWorkspace(
            Path projectDirectory, Path localRepository, Map<String, String> properties) throws IOException {
        Optional<Map<String, String>> propsOptional = propertiesManager.maySeedProperties(projectDirectory, properties);
        if (propsOptional.isPresent()) {
            Map<String, String> props = propsOptional.orElse(Collections.emptyMap());
            final Config config = configurationManager.getConfig(projectDirectory, props);
            Optional<Workspace> wo = detectWorkspace(config, projectDirectory, localRepository, props);
            if (wo.isPresent()) {
                logger.debug("Workspace detected");
                return wo;
            }
        }
        logger.debug("No workspace detected");
        return Optional.empty();
    }

    @Override
    public void linkWorkspace(Workspace target, Workspace tail) throws IOException {}

    @Override
    public void unlinkWorkspace(Workspace target, Workspace tail) throws IOException {}

    private Optional<Workspace> workspaceReDetector(
            Path projectDirectory, Path localRepository, Map<String, String> properties) throws IOException {
        // cleanse to force Nisse invocation for another directory
        return detectWorkspace(projectDirectory, localRepository, propertiesManager.cleanseProperties(properties));
    }

    private Optional<Workspace> detectWorkspace(
            Config config, Path projectDirectory, Path localRepository, Map<String, String> properties)
            throws IOException {
        final String remoteName = properties.get(PropertiesManager.KEY_REMOTE_NAME);
        final String remoteUrl = properties.get(PropertiesManager.KEY_REMOTE_URL);
        final String branchName = properties.get(PropertiesManager.KEY_BRANCH_NAME);
        final String commonDir = properties.get(PropertiesManager.KEY_COMMON_DIR);

        if (remoteName != null && remoteUrl != null && branchName != null) {
            // remoteUrl: git@github.com:maveniverse/mwm.git or https://github.com/example/repo.git
            // we want DiscriminatorElements elements
            String[] shavenUrl = remoteUrl
                    .replaceFirst("^(git@|https://)", "")
                    .replaceFirst("\\.git$", "")
                    .split("[/:]");
            if (shavenUrl.length < 3) {
                return Optional.empty();
            }

            HashMap<Config.DiscriminatorElements, String> elems = new HashMap<>();
            elems.put(Config.DiscriminatorElements.REMOTE, remoteName);
            elems.put(Config.DiscriminatorElements.HOST, shavenUrl[0]);
            elems.put(
                    Config.DiscriminatorElements.OWNER,
                    String.join("-", Arrays.asList(shavenUrl).subList(1, shavenUrl.length - 1)));
            elems.put(Config.DiscriminatorElements.REPOSITORY, shavenUrl[shavenUrl.length - 1]);
            elems.put(Config.DiscriminatorElements.BRANCH, branchName);

            ArrayList<String> workspaceIdArr = new ArrayList<>();
            ArrayList<String> workspaceDiscriminatorArr = new ArrayList<>();
            for (Config.DiscriminatorElements e : Config.DiscriminatorElements.values()) {
                workspaceIdArr.add(elems.get(e));
                if (config.getDiscriminatorElements().contains(e)) {
                    workspaceDiscriminatorArr.add(elems.get(e));
                }
            }
            final String workspaceId = String.join("-", workspaceIdArr);
            final String workspaceDiscriminator = String.join("-", workspaceDiscriminatorArr);
            logger.debug("WS {} ({})", workspaceId, workspaceDiscriminator);

            final Path buildCacheDirectory = resolveWorkspacePath(
                    config, config.getBuildCacheScope(), projectDirectory, localRepository, true, workspaceId);
            final Path buildOutputDirectory = resolveWorkspacePath(
                    config, config.getBuildOutputScope(), projectDirectory, localRepository, false, workspaceId);

            HashMap<String, String> props = new HashMap<>();
            props.put("git.remoteName", remoteName);
            props.put("git.remoteUrl", remoteUrl);
            props.put("git.branchName", branchName);
            if (commonDir != null) {
                props.put("git.commonDir", commonDir);
            }
            props.put("workspace.id", workspaceId);
            props.put("workspace.discriminator", workspaceDiscriminator);
            ArrayList<Workspace> linkedWorkspaces = new ArrayList<>();
            if (config.isWorktreeJoined() && commonDir != null) {
                Path commonProjectDir = Paths.get(commonDir);
                if (Files.isDirectory(commonProjectDir)
                        && commonProjectDir.getParent() != null
                        && Files.isDirectory(commonProjectDir.getParent())) {
                    workspaceReDetector(commonProjectDir.getParent(), localRepository, properties)
                            .ifPresent(linkedWorkspaces::add);
                }
            }
            // TODO: config for linked workspaces
            // TODO: discriminator
            return Optional.of(new WorkspaceImpl(
                    workspaceId,
                    workspaceDiscriminator,
                    props,
                    projectDirectory,
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
                return projectDirectory.resolve(config.mvnLocal()).resolve(config.cachedDir());
            } else {
                return projectDirectory.resolve(config.mvnLocal()).resolve(config.installedDir());
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
