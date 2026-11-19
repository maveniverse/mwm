/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mwm.core.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mwm.core.Config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfigImpl implements Config {
    static final String CACHED_DIR = "cached-dir";
    static final String INSTALLED_DIR = "installed-dir";
    static final String DISCRIMINATOR_ELEMENTS = "discriminator-elements";
    static final String WORKTREE_JOINED = "worktree-joined";
    static final String LINK_ENFORCED = "link-enforced";
    static final String WS_KEY = "ws-key";
    static final String BUILD_CACHE_SCOPE = "build-cache-scope";
    static final String BUILD_OUTPUT_SCOPE = "build-output-scope";

    private final Map<String, String> config;

    public ConfigImpl(Map<String, String> config) {
        this.config = requireNonNull(config);
    }

    /**
     * Returns a snapshot/copy of config map.
     */
    @Override
    public Map<String, String> config() {
        return new HashMap<>(config);
    }

    @Override
    public Path cachedDir() {
        if (config.containsKey(CACHED_DIR)) {
            return Paths.get(config.get(CACHED_DIR));
        }
        return Config.super.cachedDir();
    }

    @Override
    public Path installedDir() {
        if (config.containsKey(INSTALLED_DIR)) {
            return Paths.get(config.get(INSTALLED_DIR));
        }
        return Config.super.installedDir();
    }

    @Override
    public EnumSet<DiscriminatorElements> getDiscriminatorElements() {
        if (config.containsKey(DISCRIMINATOR_ELEMENTS)) {
            EnumSet<DiscriminatorElements> res = EnumSet.noneOf(DiscriminatorElements.class);
            Arrays.stream(config.get(DISCRIMINATOR_ELEMENTS).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> res.add(enumValue(DiscriminatorElements.class, DISCRIMINATOR_ELEMENTS, s)));
            return res;
        }
        return Config.super.getDiscriminatorElements();
    }

    @Override
    public boolean isWorktreeJoined() {
        return Boolean.parseBoolean(
                config.getOrDefault(WORKTREE_JOINED, Boolean.toString(Config.super.isWorktreeJoined())));
    }

    @Override
    public boolean isLinkEnforced() {
        return Boolean.parseBoolean(
                config.getOrDefault(LINK_ENFORCED, Boolean.toString(Config.super.isLinkEnforced())));
    }

    @Override
    public WSKey getWSKey() {
        return enumValue(
                WSKey.class,
                WS_KEY,
                config.getOrDefault(WS_KEY, Config.super.getWSKey().name()));
    }

    @Override
    public Scope getBuildCacheScope() {
        return enumValue(
                Scope.class,
                BUILD_CACHE_SCOPE,
                config.getOrDefault(
                        BUILD_CACHE_SCOPE, Config.super.getBuildCacheScope().name()));
    }

    @Override
    public Scope getBuildOutputScope() {
        return enumValue(
                Scope.class,
                BUILD_OUTPUT_SCOPE,
                config.getOrDefault(
                        BUILD_OUTPUT_SCOPE, Config.super.getBuildOutputScope().name()));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String key, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid value '" + value + "' for MWM configuration key '" + key + "' (mwm.properties); allowed: "
                            + Arrays.toString(type.getEnumConstants()),
                    e);
        }
    }
}
