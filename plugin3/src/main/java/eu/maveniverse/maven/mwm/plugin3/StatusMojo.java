/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.plugin3;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MWM status dumps MWM, if used.
 */
@Mojo(name = "status", threadSafe = true)
public class StatusMojo extends AbstractMojo {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private MavenSession mavenSession;

    @Inject
    private WorkspaceManager workspaceManager;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Map<String, String> configProperties = new HashMap<>();
            mavenSession.getRepositorySession().getConfigProperties().forEach((key, value) -> {
                if (value instanceof String) {
                    configProperties.put(key, (String) value);
                }
            });

            Optional<Workspace> wo = workspaceManager.detectWorkspace(
                    mavenSession.getRootDirectory(),
                    mavenSession.getRepositorySession().getLocalRepository().getBasePath(),
                    configProperties);
            if (wo.isPresent()) {
                Workspace workspace = wo.get();
                logger.info("MWM is active");
                logger.info("=============");
                logger.info("WS ID      = {}", workspace.workspaceId());
                logger.info(
                        "WS handler = {}",
                        workspace.workspaceHandler().getClass().getSimpleName());
                logger.info("WS output  = {}", workspace.buildOutputDirectory());
                logger.info("-------------");
                logger.info("Considered properties:");
                workspace.properties().forEach((key, value) -> logger.info("{} = {}", key, value));
            } else {
                logger.info("MWM is not used.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting MWM status", e);
        }
    }
}
