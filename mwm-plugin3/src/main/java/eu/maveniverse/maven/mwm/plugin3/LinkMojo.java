/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * MWM workspace link.
 */
@Mojo(name = "link", threadSafe = true, requiresProject = false)
public class LinkMojo extends AbstractMojoSupport {
    @Parameter(property = "mwm.other")
    private String other;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Optional<Workspace> workspace = getWorkspace();
            if (workspace.isPresent()) {
                Workspace ws = workspace.orElseThrow(() -> new MojoExecutionException("value not present"));
                Path basedir = FileUtils.discoverPathFromSystemProperty(
                        "basedir",
                        FileUtils.discoverUserCurrentWorkingDirectory().toString());
                Optional<Workspace> otherWs = workspaceManager.detectWorkspace(
                        basedir.resolve(other), ws.localRepository(), getConfigPropertiesAsString());
                if (otherWs.isPresent()) {
                    Workspace ot = otherWs.orElseThrow(() -> new MojoExecutionException("value not present"));
                    workspaceManager.linkWorkspace(ws, ot);
                    logger.info("Workspace {} linked with {}", ws.workspaceId(), ot.workspaceId());
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
