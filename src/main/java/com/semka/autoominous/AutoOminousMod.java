package com.semka.autoominous;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.semka.autoominous.config.AutoOminousConfig;
import com.semka.autoominous.handlers.TickHandler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class AutoOminousMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "auto_ominous";
    public static final ResourceLocation RELOAD_KEY = new ResourceLocation(MOD_ID, "config_reload");
    private static AutoOminousMod INSTANCE;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private final AutoOminousConfig config;
    private final TickHandler tickHandler;

    public AutoOminousMod() {
        INSTANCE = this;
        Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        this.configPath = configDir.resolve(MOD_ID + ".json");
        this.config = AutoOminousConfig.loadOrCreate(configPath, gson);
        this.tickHandler = new TickHandler(this);
    }

    public static AutoOminousMod getInstance() {
        return INSTANCE;
    }

    public synchronized AutoOminousConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeServer(MinecraftServer server) {
        // Register server lifecycle events and reload listener to support /reload
        ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            // Register tick handler if needed (we register/unregister dynamically based on config)
            if (getConfig().mode == AutoOminousConfig.Mode.Timer) {
                tickHandler.register();
            }
        });

        // Register a resource reload listener so that /reload triggers config reload
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void onResourceManagerReload(net.minecraft.server.packs.resources.ResourceManager manager) {
                // reload config safely on server thread
                AutoOminousConfig reloaded = AutoOminousConfig.loadOrCreate(configPath, gson);
                synchronized (AutoOminousMod.this) {
                    config.copyFrom(reloaded);
                    // adjust tick handler registration based on new config
                    if (config.mode == AutoOminousConfig.Mode.Timer) {
                        tickHandler.register();
                    } else {
                        tickHandler.unregister();
                    }
                }
            }

            @Override
            public ResourceLocation getFabricId() {
                return RELOAD_KEY;
            }
        });

        // Also ensure on server starting we adopt current config mode
        ServerLifecycleEvents.SERVER_STOPPED.register(srv -> {
            tickHandler.unregister();
        });
    }

    /**
     * Called by the Raid mixin when a raid ends. This is executed on the server thread (Mixin injection tail).
     */
    public void onRaidEnd(ServerLevel raidLevel) {
        // operate on players in the same level
        Objects.requireNonNull(raidLevel, "raidLevel");
        getConfig(); // ensure config loaded
        // Find all players in the raid level and attempt to auto-consume for each
        raidLevel.players().forEach(player -> {
            // call player-level consumption logic
            tryConsumeForPlayer(player);
        });
    }

    /**
     * Try to find an ominous bottle in player's inventory and consume one, applying BAD_OMEN directly.
     * This method must be executed on server thread.
     */
    public boolean tryConsumeForPlayer(net.minecraft.world.entity.player.Player player) {
        if (player == null) return false;
        synchronized (this) {
            AutoOminousConfig cfg = this.config;
            if (!cfg.enabled) return false;
            return com.semka.autoominous.util.ConsumptionUtil.findAndConsumeOminousAndApplyEffect(player, cfg);
        }
    }
}