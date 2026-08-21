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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Workspace manager, that detects and manages workspaces.
 */
public interface WorkspaceManager {
    /**
     * List known workspaces.
     */
    Collection<Workspace> listAll() throws IOException;

    /**
     * Lookup a workspace by ID.
     */
    Optional<Workspace> lookup(String workspaceId) throws IOException;

    /**
     * Drops a workspace by ID, purges if needed. Returns {@code true} if WS was found and could be dropped.
     */
    boolean drop(String workspaceId, boolean purge) throws IOException;

    /**
     * Detects and may create a workspace handle.
     */
    Optional<Workspace> detectWorkspace(Path projectDirectory, Path localRepository, Map<String, String> properties)
            throws IOException;

    /**
     * Links {@code tail} workspace to {@code target} workspace.
     */
    void linkWorkspace(Workspace target, Workspace tail) throws IOException;

    /**
     * Unlinks {@code tail} workspace from {@code target} workspace.
     */
    void unlinkWorkspace(Workspace target, Workspace tail) throws IOException;
}
