package com.semka.autoominous.handlers;

import com.semka.autoominous.AutoOminousMod;
import com.semka.autoominous.config.AutoOminousConfig;
import com.semka.autoominous.util.ConsumptionUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the Timer mode: every N ticks try to consume ominous bottle for each player.
 * Registration/unregistration of event listener is controlled here.
 */
public final class TickHandler {

    private final AutoOminousMod mod;
    private final AtomicLong tickCounter = new AtomicLong(0);
    private volatile boolean registered = false;

    public TickHandler(AutoOminousMod mod) {
        this.mod = mod;
    }

    public synchronized void register() {
        if (registered) return;
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        registered = true;
        tickCounter.set(0);
    }

    public synchronized void unregister() {
        // Fabric's event registration returns nothing to unregister easily; to avoid double-registration,
        // we track state and simply ignore if not registered. If running in an environment where
        // unregistering is required, more advanced registration handling is necessary.
        registered = false;
    }

    private void onServerTick(MinecraftServer server) {
        if (!registered) return; // do not run if logically unregistered

        AutoOminousConfig cfg = mod.getConfig();

        if (cfg == null || !cfg.enabled || cfg.mode != AutoOminousConfig.Mode.Timer) {
            return;
        }

        long ticks = tickCounter.incrementAndGet();
        if (ticks % Math.max(1, cfg.timerTicks) != 0) {
            return;
        }

        // For each player on the server, try to consume one ominous bottle
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Only server thread here; safe to interact with player inventory and apply effects.
            ConsumptionUtil.findAndConsumeOminousAndApplyEffect(player, cfg);
        }
    }
}