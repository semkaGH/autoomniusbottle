package com.semka.autoominous.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for the Auto Ominous mod.
 * Stored as JSON in /config/auto_ominous.json
 */
public final class AutoOminousConfig {

    public enum Mode {
        @SerializedName("AfterRaid")
        AfterRaid,
        @SerializedName("Timer")
        Timer
    }

    // If false, mod does nothing
    public boolean enabled = true;

    // Mode: AfterRaid or Timer
    public Mode mode = Mode.AfterRaid;

    // Timer ticks interval when using Timer mode (server ticks). Default: 6000 ticks (~5 minutes).
    public int timerTicks = 6000;

    // BAD_OMEN effect level given (1-based). We will convert to amplifier (level-1) when applying.
    public int badOmenLevel = 1;

    // BAD_OMEN duration in ticks. Default: 12000 (~10 minutes)
    public int badOmenDurationTicks = 12000;

    // Safe load; if file missing, returns defaults and writes file back.
    public static AutoOminousConfig loadOrCreate(Path configPath, Gson gson) {
        Objects.requireNonNull(configPath, "configPath");
        try {
            if (Files.exists(configPath)) {
                try (Reader r = Files.newBufferedReader(configPath)) {
                    AutoOminousConfig cfg = gson.fromJson(r, AutoOminousConfig.class);
                    if (cfg == null) {
                        cfg = new AutoOminousConfig();
                    }
                    // sanitize
                    cfg.sanitize();
                    // ensure file saved in canonical form
                    try (Writer w = Files.newBufferedWriter(configPath)) {
                        gson.toJson(cfg, w);
                    }
                    return cfg;
                } catch (Exception ex) {
                    // If parsing failed, back up file and return defaults
                    Path backup = configPath.resolveSibling(configPath.getFileName().toString() + ".broken");
                    try {
                        Files.move(configPath, backup);
                    } catch (IOException ignored) {}
                    AutoOminousConfig defaultCfg = new AutoOminousConfig();
                    try (Writer w = Files.newBufferedWriter(configPath)) {
                        gson.toJson(defaultCfg, w);
                    }
                    return defaultCfg;
                }
            } else {
                // create directories and default file
                try {
                    if (configPath.getParent() != null) Files.createDirectories(configPath.getParent());
                } catch (IOException ignored) {}
                AutoOminousConfig defaultCfg = new AutoOminousConfig();
                try (Writer w = Files.newBufferedWriter(configPath)) {
                    gson.toJson(defaultCfg, w);
                }
                return defaultCfg;
            }
        } catch (Exception ex) {
            // as a last resort, return defaults
            AutoOminousConfig fallback = new AutoOminousConfig();
            fallback.sanitize();
            return fallback;
        }
    }

    // Copy settings from another instance (used on reload)
    public void copyFrom(AutoOminousConfig other) {
        if (other == null) return;
        this.enabled = other.enabled;
        this.mode = other.mode;
        this.timerTicks = other.timerTicks;
        this.badOmenLevel = other.badOmenLevel;
        this.badOmenDurationTicks = other.badOmenDurationTicks;
        sanitize();
    }

    private void sanitize() {
        if (timerTicks <= 0) timerTicks = 6000;
        if (badOmenLevel <= 0) badOmenLevel = 1;
        if (badOmenDurationTicks <= 0) badOmenDurationTicks = 12000;
        if (mode == null) mode = Mode.AfterRaid;
    }
}