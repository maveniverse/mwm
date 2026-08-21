/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * MWM workspace list and dump.
 */
@Mojo(name = "list", threadSafe = true, requiresProject = false)
public class ListMojo extends AbstractMojoSupport {
    @Parameter(property = "mwm.details")
    private boolean details;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            int found = 0;
            for (Workspace workspace : workspaceManager.listAll()) {
                found++;
                if (details) {
                    logger.info("==================");
                    dumpWorkspace(workspace);
                } else {
                    logger.info("WS ID  = {}", workspace.workspaceId());
                    logger.info("WS DIS = {}", workspace.discriminator());
                }
            }
            logger.info("==================");
            logger.info("Total of {} workspaces", found);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting MWM status", e);
        }
    }
}
