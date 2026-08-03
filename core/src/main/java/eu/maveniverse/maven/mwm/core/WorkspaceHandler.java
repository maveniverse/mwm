/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Maven Workspace handler.
 */
public interface WorkspaceHandler {
    /**
     * Nisse key for remote name.
     */
    String KEY_REMOTE_NAME = "nisse.jgit.remoteName";
    /**
     * Nisse key for remote URL.
     */
    String KEY_REMOTE_URL = "nisse.jgit.remoteUrl";
    /**
     * Nisse key for branch name,
     */
    String KEY_BRANCH_NAME = "nisse.jgit.branchName";

    /**
     * Detects and may create a workspace handle.
     */
    Optional<Workspace> detectWorkspace(Path rootDirectory, Path localRepository, Map<String, String> properties);
}
