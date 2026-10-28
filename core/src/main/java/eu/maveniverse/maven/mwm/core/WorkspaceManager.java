/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Workspace manager, that detects and manages workspaces.
 */
public interface WorkspaceManager {
    /**
     * Detects a workspace handle. If {@code persist}, the detected workspace (and linked ones) properties are saved
     * into {@code .mvn/target/workspace.properties}, if changed.
     */
    Optional<Workspace> detectWorkspace(
            Path projectDirectory, Path localRepository, Map<String, String> properties, boolean persist)
            throws IOException;

    /**
     * Links {@code other} workspace to {@code target} workspace. Returns {@code false} if already linked.
     */
    boolean linkWorkspace(Workspace target, Workspace other) throws IOException;

    /**
     * Unlinks {@code other} workspace from {@code target} workspace.
     */
    boolean unlinkWorkspace(Workspace target, Workspace other) throws IOException;
}
