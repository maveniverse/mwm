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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
@Named
public final class DefaultWorkspaceHandler implements WorkspaceHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Optional<Workspace> detectWorkspace(
            Path projectDirectory,
            Path localRepository,
            Map<String, String> properties,
            Function<Path, Optional<Workspace>> workspaceDetector) {
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

            final Path buildCacheDirectory =
                    resolve(config.getBuildCacheScope(), projectDirectory, localRepository, true, workspaceId);
            final Path buildOutputDirectory =
                    resolve(config.getBuildOutputScope(), projectDirectory, localRepository, false, workspaceId);

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
                    workspaceDetector.apply(commonProjectDir.getParent()).ifPresent(linkedWorkspaces::add);
                }
            }
            // TODO: config for linked workspaces
            return Optional.of(new DefaultWorkspace(
                    workspaceId, this, props, buildCacheDirectory, buildOutputDirectory, linkedWorkspaces));
        }
        return Optional.empty();
    }

    private Path resolve(
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

    private Config config(Map<String, String> properties) {
        return new Config() {};
    }
}
