/*
 * Copyright Lena Schönburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.maveniverse.maven.mwm.extension39.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Workspace;
import eu.maveniverse.maven.mwm.core.WorkspaceManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposer;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactorySupport;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactory} that additionally
 * scopes the local (install) prefix of the split local repository by the current git branch:
 * installed artifacts land under {@code installed/<branch>/...} while downloaded artifacts stay
 * shared under {@code cached/...}.
 *
 * <p>The higher Sisu {@link Priority} makes this component win over the resolver's default factory
 * when Maven injects the unqualified {@code LocalPathPrefixComposerFactory} into the enhanced
 * local repository manager factory.
 */
@Priority(100)
public final class BranchScopedLocalPathPrefixComposerFactory extends LocalPathPrefixComposerFactorySupport {

    /** Session config property to disable branch scoping without removing the extension. */
    static final String CONFIG_PROP_ENABLED = "branchScopedLocalRepo.enabled";

    static final String CONFIG_PROP_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchScopedLocalPathPrefixComposerFactory.class);

    private final WorkspaceManager workspaceManager;

    @Inject
    public BranchScopedLocalPathPrefixComposerFactory(WorkspaceManager workspaceManager) {
        this.workspaceManager = requireNonNull(workspaceManager);
    }

    @Override
    public LocalPathPrefixComposer createComposer(final RepositorySystemSession session) {
        // A detected branch turns the split local repository on, so registering the extension
        // (e.g. user-wide in ~/.m2/extensions.xml) is the only opt-in step needed. An explicit
        // aether.enhancedLocalRepository.split setting still wins.
        boolean split = isSplit(session);
        String localPrefix = getLocalPrefix(session);

        final Path projectRoot = projectRoot(session);
        if (projectRoot != null) {
            Map<String, String> configProperties = new HashMap<>();
            session.getConfigProperties().forEach((key, value) -> {
                if (value instanceof String) {
                    configProperties.put(key, (String) value);
                }
            });

            // we are very early in process; need to figure out LRM as input session is incomplete; is being built

            Workspace workspace = workspaceManager
                    .detectWorkspace(projectRoot, configProperties)
                    .orElse(null);

            if (workspace != null) {
                localPrefix = workspace.workspaceId();
                LOGGER.debug("Branch-scoped local repository: split={}, using local prefix '{}'", split, localPrefix);
            }
        }
        return new BranchScopedComposer(
                split,
                localPrefix,
                isSplitLocal(session),
                getRemotePrefix(session),
                isSplitRemote(session),
                isSplitRemoteRepository(session),
                isSplitRemoteRepositoryLast(session),
                getReleasesPrefix(session),
                getSnapshotsPrefix(session));
    }

    private static Path projectRoot(final RepositorySystemSession session) {
        final String directory = ConfigUtils.getString(session, null, CONFIG_PROP_PROJECT_DIRECTORY);
        if (directory == null) {
            return null;
        }
        final Path path = Paths.get(directory);
        return Files.isDirectory(path) ? path : null;
    }

    /**
     * All path composition is inherited from {@link LocalPathPrefixComposerSupport}; only the {@code
     * localPrefix} passed in differs from the default composer. Immutable, as required of composers.
     */
    private static final class BranchScopedComposer extends LocalPathPrefixComposerSupport {

        @SuppressWarnings("checkstyle:parameternumber")
        private BranchScopedComposer(
                final boolean split,
                final String localPrefix,
                final boolean splitLocal,
                final String remotePrefix,
                final boolean splitRemote,
                final boolean splitRemoteRepository,
                final boolean splitRemoteRepositoryLast,
                final String releasesPrefix,
                final String snapshotsPrefix) {
            super(
                    split,
                    localPrefix,
                    splitLocal,
                    remotePrefix,
                    splitRemote,
                    splitRemoteRepository,
                    splitRemoteRepositoryLast,
                    releasesPrefix,
                    snapshotsPrefix);
        }
    }
}
