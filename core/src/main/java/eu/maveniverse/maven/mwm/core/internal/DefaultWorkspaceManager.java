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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        logger.info("MWM {} (Resolver {})", Version.version(), Version.resolverVersion());
    }

    @Override
    public Collection<Workspace> listAll() {
        return Collections.emptyList();
    }

    @Override
    public Optional<Workspace> lookup(String workspaceId) {
        return Optional.empty();
    }

    @Override
    public boolean drop(String workspaceId, boolean purge) {
        return false;
    }

    @Override
    public Optional<Workspace> detectWorkspace(
            Path projectDirectory, Path localRepository, Map<String, String> properties) {
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
    public void linkWorkspace(Workspace target, Workspace tail) {}

    @Override
    public void unlinkWorkspace(Workspace target, Workspace tail) {}

    private Optional<Workspace> workspaceReDetector(
            Path projectDirectory, Path localRepository, Map<String, String> properties) {
        // cleanse to force Nisse invocation for another directory
        return detectWorkspace(projectDirectory, localRepository, propertiesManager.cleanseProperties(properties));
    }

    private Optional<Workspace> detectWorkspace(
            Config config, Path projectDirectory, Path localRepository, Map<String, String> properties) {
        final String remoteName = properties.get(PropertiesManager.KEY_REMOTE_NAME);
        final String remoteUrl = properties.get(PropertiesManager.KEY_REMOTE_URL);
        final String branchName = properties.get(PropertiesManager.KEY_BRANCH_NAME);
        final String commonDir = properties.get(PropertiesManager.KEY_COMMON_DIR);

        if (remoteName != null && remoteUrl != null && branchName != null) {
            // remoteUrl: git@github.com:maveniverse/mwm.git or https://github.com/example/repo.git
            // we want host + owner (without slashes) + repo
            String workspaceId = remoteName + "-"
                    + remoteUrl
                            .replaceFirst("^(git@|https://)", "")
                            .replaceFirst("\\.git$", "")
                            .replaceAll("[:/]", "-") + "-" + branchName;
            logger.debug("WS {}", workspaceId);

            final Path buildCacheDirectory = resolveWorkspacePath(
                    config.getBuildCacheScope(), projectDirectory, localRepository, true, workspaceId);
            final Path buildOutputDirectory = resolveWorkspacePath(
                    config.getBuildOutputScope(), projectDirectory, localRepository, false, workspaceId);

            HashMap<String, String> props = new HashMap<>();
            props.put("git.remoteName", remoteName);
            props.put("git.remoteUrl", remoteUrl);
            props.put("git.branchName", branchName);
            props.put("workspaceId", workspaceId);
            props.put("handler", getClass().getSimpleName());
            props.put("rootDirectory", projectDirectory.toString());
            props.put("buildCacheDirectory", buildCacheDirectory.toString());
            props.put("buildOutputDirectory", buildOutputDirectory.toString());
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
            return Optional.of(new DefaultWorkspace(
                    workspaceId,
                    workspaceId,
                    props,
                    projectDirectory,
                    buildCacheDirectory,
                    buildOutputDirectory,
                    linkedWorkspaces));
        }
        return Optional.empty();
    }

    private Path resolveWorkspacePath(
            Config.Scope scope, Path projectDirectory, Path localRepository, boolean cache, String workspaceId) {
        if (scope == Config.Scope.PROJECT) {
            if (cache) {
                return projectDirectory.resolve(".mvn-local").resolve("cache");
            } else {
                return projectDirectory.resolve(".mvn-local").resolve("build").resolve(workspaceId);
            }
        } else if (scope == Config.Scope.USER) {
            if (cache) {
                return localRepository;
            } else {
                return localRepository.resolve("mwm-workspace").resolve(workspaceId);
            }
        } else {
            throw new IllegalArgumentException("Invalid build cache scope provided: " + scope);
        }
    }
}
