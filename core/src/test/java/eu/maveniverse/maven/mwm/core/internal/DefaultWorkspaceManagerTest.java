/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mwm.core.Config;
import eu.maveniverse.maven.mwm.core.Workspace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DefaultWorkspaceManagerTest {
    @Test
    void parseRemoteUrl() {
        String[] expected = {"github.com", "apache", "maven"};
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("git@github.com:apache/maven.git"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("https://github.com/apache/maven.git"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("https://github.com/apache/maven"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("https://user@github.com/apache/maven/"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("http://github.com/apache/maven.git"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("ssh://git@github.com/apache/maven.git"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("ssh://git@github.com:22/apache/maven.git"));
        assertArrayEquals(expected, DefaultWorkspaceManager.parseRemoteUrl("git://github.com/apache/maven.git"));
        assertArrayEquals(
                new String[] {"gitlab.com", "group", "sub", "repo"},
                DefaultWorkspaceManager.parseRemoteUrl("git@gitlab.com:group/sub/repo.git"));
    }

    @Test
    void discriminatorElementsTolerateWhitespace() {
        assertEquals(
                java.util.EnumSet.of(Config.DiscriminatorElements.HOST, Config.DiscriminatorElements.BRANCH),
                new ConfigImpl(Collections.singletonMap(ConfigImpl.DISCRIMINATOR_ELEMENTS, " host , branch,"))
                        .getDiscriminatorElements());
    }

    @Test
    void linkCycleAndDiamond(@TempDir Path tmp) throws IOException {
        // a -> b, a -> c, b -> a (cycle), b -> d, c -> d (diamond)
        Path a = project(tmp, "a", "b", "c");
        project(tmp, "b", "a", "d");
        project(tmp, "c", "d");
        project(tmp, "d");

        DefaultWorkspaceManager wm = workspaceManager();

        Workspace ws = wm.detectWorkspace(a, tmp.resolve("repo"), Collections.emptyMap(), true)
                .orElseThrow(AssertionError::new);

        List<String> seen = new ArrayList<>();
        ArrayDeque<Workspace> queue = new ArrayDeque<>(ws.linkedWorkspaces());
        while (!queue.isEmpty()) {
            Workspace w = queue.pop();
            seen.add(w.properties().get(Config.DiscriminatorElements.REPOSITORY.name()));
            queue.addAll(w.linkedWorkspaces());
        }
        seen.sort(null);
        assertEquals(java.util.Arrays.asList("b", "c", "d"), seen);
    }

    @Test
    void linkIsIdempotent(@TempDir Path tmp) throws IOException {
        DefaultWorkspaceManager wm = workspaceManager();
        Workspace a = wm.detectWorkspace(project(tmp, "a"), tmp.resolve("repo"), Collections.emptyMap(), true)
                .orElseThrow(AssertionError::new);
        Workspace b = wm.detectWorkspace(project(tmp, "b"), tmp.resolve("repo"), Collections.emptyMap(), true)
                .orElseThrow(AssertionError::new);

        assertTrue(wm.linkWorkspace(a, b));
        assertFalse(wm.linkWorkspace(a, b));
        assertThrows(IOException.class, () -> wm.linkWorkspace(a, a));
        assertTrue(wm.unlinkWorkspace(a, b));
        assertFalse(wm.unlinkWorkspace(a, b));
    }

    @Test
    void workspaceIdIsPathSafe(@TempDir Path tmp) throws IOException {
        Map<String, String> props = new HashMap<>();
        props.put(PropertiesManager.KEY_REMOTE_NAME, "origin");
        props.put(PropertiesManager.KEY_REMOTE_URL, "git@github.com:o/r.git");
        props.put(PropertiesManager.KEY_BRANCH_NAME, "feature/x");
        Workspace ws = workspaceManager()
                .detectWorkspace(project(tmp, "a"), tmp.resolve("repo"), props, false)
                .orElseThrow(AssertionError::new);
        assertEquals("origin-github.com-o-r-feature_x", ws.workspaceId());
        assertEquals("feature/x", ws.properties().get("git.branchName"));
    }

    @Test
    void persistOnlyWhenAskedAndChanged(@TempDir Path tmp) throws IOException {
        DefaultWorkspaceManager wm = workspaceManager();
        Path a = project(tmp, "a");
        project(tmp, "b");
        Path file = a.resolve(Config.MVN_TARGET).resolve("workspace.properties");
        // stale key must go, links must stay
        Files.write(
                file,
                java.util.Arrays.asList(
                        "stale=true",
                        "workspace.links=" + tmp.resolve("b").toString().replace('\\', '/')));

        wm.detectWorkspace(a, tmp.resolve("repo"), Collections.emptyMap(), false);
        assertTrue(new String(Files.readAllBytes(file)).contains("stale=true"));

        wm.detectWorkspace(a, tmp.resolve("repo"), Collections.emptyMap(), true);
        Map<String, String> saved = new ConfigurationManager().loadWorkspace(a).orElseThrow(AssertionError::new);
        assertFalse(saved.containsKey("stale"));
        assertTrue(saved.containsKey("workspace.links"));
        assertTrue(
                Files.isRegularFile(tmp.resolve("b").resolve(Config.MVN_TARGET).resolve("workspace.properties")));

        FileTime old = FileTime.fromMillis(0);
        Files.setLastModifiedTime(file, old);
        wm.detectWorkspace(a, tmp.resolve("repo"), Collections.emptyMap(), true);
        assertEquals(old, Files.getLastModifiedTime(file));
    }

    private static DefaultWorkspaceManager workspaceManager() {
        return new DefaultWorkspaceManager(new ConfigurationManager(), new PropertiesManager(c -> {
            Map<String, String> p = new HashMap<>();
            p.put(PropertiesManager.KEY_REMOTE_NAME, "origin");
            p.put(
                    PropertiesManager.KEY_REMOTE_URL,
                    "https://github.com/o/" + c.getSessionRootDirectory().getFileName() + ".git");
            p.put(PropertiesManager.KEY_BRANCH_NAME, "main");
            return p;
        }));
    }

    private static Path project(Path tmp, String name, String... links) throws IOException {
        Path dir = tmp.resolve(name);
        Path target = dir.resolve(Config.MVN_TARGET);
        Files.createDirectories(target);
        // pin config, so user's ~/.m2/mwm.properties does not interfere
        Files.write(target.resolve("mwm.properties"), Collections.singletonList("build-output-scope=project"));
        if (links.length > 0) {
            List<String> paths = new ArrayList<>();
            for (String link : links) {
                paths.add(tmp.resolve(link).toString().replace('\\', '/'));
            }
            Files.write(
                    target.resolve("workspace.properties"),
                    Collections.singletonList("workspace.links=" + String.join(",", paths)));
        }
        return dir;
    }
}
