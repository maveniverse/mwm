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

/**
 * Maven Workspace.
 */
public interface Workspace {
    /**
     * The ID of the workspace.
     */
    String workspaceId();

    /**
     * The handler of workspace.
     */
    WorkspaceHandler workspaceHandler();

    /**
     * The workspace properties.
     */
    Map<String, String> properties();

    /**
     * The location of the build cache output.
     */
    Path buildCacheDirectory();

    /**
     * The location of the build output directory.
     */
    Path buildOutputDirectory();
}
