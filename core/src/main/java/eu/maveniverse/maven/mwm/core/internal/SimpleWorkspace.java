/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class SimpleWorkspace implements Workspace {
    private final String workspaceId;
    private final WorkspaceHandler workspaceHandler;
    private final Map<String, String> properties;
    private final Path buildCacheDirectory;
    private final Path buildOutputDirectory;
    private final List<Workspace> linkedWorkspaces;

    public SimpleWorkspace(
            String workspaceId,
            WorkspaceHandler workspaceHandler,
            Map<String, String> properties,
            Path buildCacheDirectory,
            Path buildOutputDirectory,
            List<Workspace> linkedWorkspaces) {
        this.workspaceId = requireNonNull(workspaceId);
        this.workspaceHandler = requireNonNull(workspaceHandler);
        this.properties = requireNonNull(properties);
        this.buildCacheDirectory = requireNonNull(buildCacheDirectory);
        this.buildOutputDirectory = requireNonNull(buildOutputDirectory);
        this.linkedWorkspaces = requireNonNull(linkedWorkspaces);
    }

    @Override
    public String workspaceId() {
        return workspaceId;
    }

    @Override
    public WorkspaceHandler workspaceHandler() {
        return workspaceHandler;
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public Path buildCacheDirectory() {
        return buildCacheDirectory;
    }

    @Override
    public Path buildOutputDirectory() {
        return buildOutputDirectory;
    }

    @Override
    public List<Workspace> linkedWorkspaces() {
        return linkedWorkspaces;
    }
}
