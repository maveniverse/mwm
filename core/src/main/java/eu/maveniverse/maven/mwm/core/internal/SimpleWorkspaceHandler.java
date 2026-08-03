/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceHandler;
import java.nio.file.Path;
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
@Named(SimpleWorkspaceHandler.NAME)
public final class SimpleWorkspaceHandler implements WorkspaceHandler {
    public static final String NAME = "simple";

    @Override
    public Optional<Workspace> detectWorkspace(
            Path rootDirectory, Path localRepository, Map<String, String> properties) {
        // late create; https://github.com/apache/maven/issues/12668
        final Logger logger = LoggerFactory.getLogger(getClass());

        String remoteName = properties.get(KEY_REMOTE_NAME);
        String remoteUrl = properties.get(KEY_REMOTE_URL);
        String branchName = properties.get(KEY_BRANCH_NAME);
        if (remoteName != null && remoteUrl != null && branchName != null) {
            // remoteUrl: git@github.com:maveniverse/mwm.git or https://github.com/example/repo.git
            // we want host + owner (without slashes) + repo
            String workspaceId = remoteName + "-"
                    + remoteUrl
                            .replaceFirst("^(git@|https://)", "")
                            .replaceFirst("\\.git$", "")
                            .replaceAll("[:/]", "-") + "-" + branchName;
            logger.debug("WS {}", workspaceId);
            Path buildOutputPath = localRepository.resolve(".mwn").resolve(workspaceId);
            HashMap<String, String> props = new HashMap<>();
            props.put(KEY_REMOTE_NAME, remoteName);
            props.put(KEY_REMOTE_URL, remoteUrl);
            props.put(KEY_BRANCH_NAME, branchName);
            props.put("workspaceId", workspaceId);
            props.put("handler", NAME);
            props.put("rootDirectory", rootDirectory.toString());
            props.put("buildOutputPath", buildOutputPath.toString());
            return Optional.of(new SimpleWorkspace(workspaceId, this, props, buildOutputPath));
        }
        return Optional.empty();
    }
}
