/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * MWM workspace unlink.
 */
@Mojo(name = "unlink", threadSafe = true, requiresProject = false)
public class UnlinkMojo extends AbstractLinkMojoSupport {
    @Override
    protected void execute(Workspace workspace, Workspace otherWorkspace) throws IOException {
        if (workspaceManager.unlinkWorkspace(workspace, otherWorkspace)) {
            logger.info("Workspace {} unlinked from {}", workspace.workspaceId(), otherWorkspace.workspaceId());
        } else {
            logger.info("Workspace {} was not linked with {}", workspace.workspaceId(), otherWorkspace.workspaceId());
        }
    }
}
