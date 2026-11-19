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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Support for mojos operating on current and "other" workspace.
 */
public abstract class AbstractLinkMojoSupport extends AbstractMojoSupport {
    /**
     * Path to the other project, resolved against basedir.
     */
    @Parameter(property = "mwm.other", required = true)
    private String other;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Optional<Workspace> workspace = getWorkspace();
            if (!workspace.isPresent()) {
                logger.info("Workspace not found.");
                return;
            }
            Optional<Workspace> otherWorkspace = detectOtherWorkspace(workspace.get());
            if (!otherWorkspace.isPresent()) {
                logger.info("Other Workspace {} not found", other);
                return;
            }
            execute(workspace.get(), otherWorkspace.get());
        } catch (Exception e) {
            throw new MojoExecutionException("Error while executing on MWM workspaces", e);
        }
    }

    protected abstract void execute(Workspace workspace, Workspace otherWorkspace) throws IOException;

    /**
     * Detects the workspace at {@link #other} path (resolved against basedir), never persists.
     */
    private Optional<Workspace> detectOtherWorkspace(Workspace workspace) throws IOException {
        Path basedir = FileUtils.discoverPathFromSystemProperty(
                "basedir", FileUtils.discoverUserCurrentWorkingDirectory().toString());
        Map<String, String> properties = getConfigPropertiesAsString();
        // git properties of current checkout (ie. from Nisse extension) must not be reused for other checkout
        properties.keySet().removeIf(k -> k.startsWith("nisse.jgit."));
        return workspaceManager.detectWorkspace(basedir.resolve(other), workspace.localRepository(), properties, false);
    }
}
