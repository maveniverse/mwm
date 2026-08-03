/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.util.Map;

/**
 * Workspace manager, that detects and manages workspaces.
 */
public interface WorkspaceManager {
    /**
     * Detects and may create a workspace handle.
     */
    Map<String, String> createProperties(Map<String, String> properties);
}
