/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.extension.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public final class MwmSessionWrapper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repositorySystem;
    private final WorkspaceManager workspaceManager;

    @Inject
    public MwmSessionWrapper(RepositorySystem repositorySystem, WorkspaceManager workspaceManager) {
        this.repositorySystem = requireNonNull(repositorySystem);
        this.workspaceManager = requireNonNull(workspaceManager);
    }

    public RepositorySystemSession.SessionBuilder wrap(Path projectRoot, RepositorySystemSession.SessionBuilder builder)
            throws IOException {
        newLocalRepositoryManager(projectRoot, builder).ifPresent(builder::setLocalRepositoryManager);
        return builder;
    }

    private Optional<LocalRepositoryManager> newLocalRepositoryManager(
            Path projectRoot, RepositorySystemSession.SessionBuilder builder) throws IOException {
        try (RepositorySystemSession.CloseableSession protoSession = builder.build()) {
            if (protoSession.getLocalRepositoryManager() instanceof ChainedLocalRepositoryManager) {
                logger.info("Chained LRM detected; MWM is not interfering with it");
                return Optional.empty();
            }
            if (protoSession.getLocalRepositoryManager() == null) {
                logger.info("No LRM detected; This session is incomplete with MWM");
                return Optional.empty();
            }

            // share data with proto
            builder.setSessionDataSupplier(protoSession::getData);

            Path localRepository = protoSession.getLocalRepository().getBasePath();
            Map<String, String> configProperties = new HashMap<>();
            protoSession.getConfigProperties().forEach((key, value) -> {
                if (value instanceof String) {
                    configProperties.put(key, (String) value);
                }
            });
            Workspace workspace = workspaceManager
                    .detectWorkspace(projectRoot, localRepository, configProperties)
                    .orElse(null);
            if (workspace != null) {
                // proto session shares data with "real" one
                protoSession.getData().set(Workspace.class, workspace);
                logger.info("Using MWM workspace: {}", workspace.workspaceId());
                LocalRepositoryManager head = repositorySystem.newLocalRepositoryManager(
                        protoSession, new LocalRepository(workspace.buildCacheDirectory()));
                ArrayList<LocalRepositoryManager> tail = new ArrayList<>();
                tail.add(repositorySystem.newLocalRepositoryManager(
                        protoSession, new LocalRepository(workspace.buildOutputDirectory())));
                for (Workspace linked : workspace.linkedWorkspaces()) {
                    tail.add(repositorySystem.newLocalRepositoryManager(
                            protoSession, new LocalRepository(linked.buildOutputDirectory())));
                }
                return Optional.of(new ChainedLocalRepositoryManager(head, tail, false, 1, 0));
            }

            return Optional.empty();
        }
    }
}
