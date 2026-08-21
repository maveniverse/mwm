/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * MWM workspace unlink.
 */
@Mojo(name = "unlink", threadSafe = true, requiresProject = false)
public class UnlinkMojo extends AbstractMojoSupport {
    @Parameter(property = "mwm.workspaceId")
    private String workspaceId;

    @Parameter(property = "mwm.other", required = true)
    private String other;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Optional<Workspace> workspace = workspaceId != null ? workspaceManager.lookup(workspaceId) : getWorkspace();
            if (workspace.isPresent()) {
                Workspace target = workspace.orElseThrow(() -> new MojoExecutionException("value not present"));
                Workspace tail = workspaceManager.lookup(other).orElse(null);
                if (tail != null) {
                    workspaceManager.unlinkWorkspace(target, tail);
                    logger.info("Workspace {} unlinked from {}", target.workspaceId(), tail.workspaceId());
                } else {
                    logger.info("Other Workspace {} not found", other);
                }
            } else {
                logger.info("Workspace not found.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting MWM status", e);
        }
    }
}
