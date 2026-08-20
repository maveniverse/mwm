/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import eu.maveniverse.maven.mwm.core.Config;
import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceHandler;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple workspace handler, uses following properties:
 * <ul>
 *     <li>{@code nisse.jgit.remoteName}</li>
 *     <li>{@code nisse.jgit.remoteUrl}</li>
 *     <li>{@code nisse.jgit.branchName}</li>
 * </ul>
 * All properties must be present. And based on them, "comes up" with some workspace.
 */
@Singleton
@Named(DefaultWorkspaceHandler.NAME)
public final class DefaultWorkspaceHandler implements WorkspaceHandler {
    public static final String NAME = "simple";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<Workspace> detectWorkspace(
            Path projectDirectory, Path localRepository, Map<String, String> properties) {
        final Config config = config(properties);
        final String remoteName = properties.get(DefaultWorkspaceManager.KEY_REMOTE_NAME);
        final String remoteUrl = properties.get(DefaultWorkspaceManager.KEY_REMOTE_URL);
        final String branchName = properties.get(DefaultWorkspaceManager.KEY_BRANCH_NAME);
        final String commonDir = properties.get(DefaultWorkspaceManager.KEY_COMMON_DIR);

        if (remoteName != null && remoteUrl != null && branchName != null) {
            // remoteUrl: git@github.com:maveniverse/mwm.git or https://github.com/example/repo.git
            // we want host + owner (without slashes) + repo
            String workspaceId = remoteName + "-"
                    + remoteUrl
                            .replaceFirst("^(git@|https://)", "")
                            .replaceFirst("\\.git$", "")
                            .replaceAll("[:/]", "-") + "-" + branchName;
            logger.debug("WS {}", workspaceId);

            final Path buildCacheDirectory;
            final Path buildOutputDirectory;
            if (config.getBuildCacheScope() == Config.Scope.PROJECT) {
                buildCacheDirectory = projectDirectory.resolve(".mvn-local").resolve("cache");
            } else if (config.getBuildCacheScope() == Config.Scope.USER) {
                buildCacheDirectory = localRepository.resolve("cache");
            } else {
                throw new IllegalArgumentException(
                        "Invalid build cache scope provided: " + config.getBuildCacheScope());
            }
            if (config.getBuildOutputScope() == Config.Scope.PROJECT) {
                buildOutputDirectory = projectDirectory.resolve(".mvn-local").resolve("installed");
            } else if (config.getBuildOutputScope() == Config.Scope.USER) {
                buildOutputDirectory = localRepository.resolve("build").resolve(workspaceId);
            } else {
                throw new IllegalArgumentException(
                        "Invalid build output scope provided: " + config.getBuildCacheScope());
            }

            HashMap<String, String> props = new HashMap<>();
            props.put("git.remoteName", remoteName);
            props.put("git.remoteUrl", remoteUrl);
            props.put("git.branchName", branchName);
            props.put("workspaceId", workspaceId);
            props.put("handler", NAME);
            props.put("rootDirectory", projectDirectory.toString());
            return Optional.of(new DefaultWorkspace(
                    workspaceId, this, props, buildCacheDirectory, buildOutputDirectory, Collections.emptyList()));
        }
        return Optional.empty();
    }

    private Config config(Map<String, String> properties) {
        return new Config() {};
    }
}
