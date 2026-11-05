/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

/**
 * Maven Workspace Manager global configuration.
 * <p>
 * Note: when both, {@link #getBuildCacheScope()} and {@link #getBuildOutputScope()} set to {@link Scope#USER}, MWM
 * will keep itself inactive, as that configuration is basically "let Maven work as by default".
 */
public interface Config {
    /**
     * Special "maven target" directory; should be git-ignored just like target is.
     * This same directory is used by Maven 4 as well (but different layout).
     */
    String MVN_TARGET = ".mvn/target";

    /**
     * What to use as workspace key, when it comes to paths?
     */
    enum WSKey {
        /**
         * Will use workspace ID, as MWM did so far.
         */
        ID,
        /**
         * Will use workspace discriminator, allows cross project sharing.
         */
        DISCRIMINATOR;
    }

    /**
     * Project scoped, user scoped.
     */
    enum Scope {
        /**
         * Build cache: Cache is stored as {@link #USER}.
         * Build output: Output is installed either as {@link #PROJECT} or {@link #USER}, depending on is worktree detected or not.
         */
        AUTO,
        /**
         * Content is kept with project in {@code .mvn/target/[cached|installed]} directory.
         */
        PROJECT,
        /**
         * Content is kept in user home under {@code ~/.m2/repository/[cached|installed]/$WORKSPACE_ID}.
         */
        USER_SCOPED,
        /**
         * Content is kept in user home under {@code ~/.m2/repository} (as Maven 3 always did).
         */
        USER
    }
    /**
     * Discriminator elements.
     */
    enum DiscriminatorElements {
        /**
         * The git remote name, for example {@code origin}.
         */
        REMOTE,
        /**
         * The repository host, for example {@code github.com}.
         */
        HOST,
        /**
         * The repository owner, for example {@code apache}.
         */
        OWNER,
        /**
         * The repository name, for example {@code maven}.
         */
        REPOSITORY,
        /**
         * The branch name, for example {@code master}.
         */
        BRANCH
    }

    /**
     * Returns a snapshot/copy of config map.
     */
    default Map<String, String> config() {
        return Collections.emptyMap();
    }

    /**
     * The name of the directory for cached artifacts.
     */
    default Path cachedDir() {
        return Paths.get("cached");
    }

    /**
     * The name of the directory for installed artifacts.
     */
    default Path installedDir() {
        return Paths.get("installed");
    }

    /**
     * Returns the elements creating workspace discriminator.
     */
    default EnumSet<DiscriminatorElements> getDiscriminatorElements() {
        return EnumSet.of(DiscriminatorElements.HOST, DiscriminatorElements.OWNER, DiscriminatorElements.BRANCH);
    }

    /**
     * In case of git worktree, is current branch tailed with default branch workspace or not.
     * <p>
     * If development happens on single "main" branch, and feature branches are used, this should be {@code true}.
     * As a counter example, in case of Maven development, where you have "unrelated" branches like
     * {@code maven-3.9.x} and {@code maven-3.10.x}, feature branches are created against these, and this
     * should be {@code false}.
     */
    default boolean isWorktreeJoined() {
        return true;
    }

    /**
     * If links are enforced, then MWM will fail if there is a link, but the Workspace could not be detected.
     * If links are not enforced, MWM will silently skip (but not drop) linked, but non-detected Workspaces.
     */
    default boolean isLinkEnforced() {
        return true;
    }

    /**
     * The WS key to use.
     */
    default WSKey getWSKey() {
        return WSKey.ID;
    }

    /**
     * The scope of build cache.
     */
    default Scope getBuildCacheScope() {
        return Scope.USER;
    }

    /**
     * The scope of build output.
     */
    default Scope getBuildOutputScope() {
        return Scope.AUTO;
    }
}
