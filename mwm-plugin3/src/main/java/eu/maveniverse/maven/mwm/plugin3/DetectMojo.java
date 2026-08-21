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

/**
 * MWM workspace detection and dump, if used.
 */
@Mojo(name = "detect", threadSafe = true, requiresProject = false)
public class DetectMojo extends AbstractMojoSupport {
    @Override
    public void execute() throws MojoExecutionException {
        try {
            Workspace workspace = getWorkspace().orElse(null);
            if (workspace != null) {
                logger.info("MWM is active");
                logger.info("=============");
                dumpWorkspace(workspace);
            } else {
                logger.info("MWM is not active.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting MWM status", e);
        }
    }
}
