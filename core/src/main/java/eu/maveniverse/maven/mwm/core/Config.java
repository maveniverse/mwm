/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core;

import java.util.EnumSet;

/**
 * Maven Workspace Manager configuration.
 */
public interface Config {
    /**
     * Project scoped, user scoped.
     */
    enum Scope {
        /**
         * Content is kept with project in {@code .mvn-local/[cached|installed]} directory.
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
     * Returns the elements creating workspace discriminator.
     */
    default EnumSet<DiscriminatorElements> getDiscriminatorElements() {
        return EnumSet.of(DiscriminatorElements.HOST, DiscriminatorElements.OWNER, DiscriminatorElements.BRANCH);
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
        return Scope.PROJECT;
    }
}
