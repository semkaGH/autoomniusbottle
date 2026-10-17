package com.semka.autoominous.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.entity.raid.Raid;

/**
 * Accessor mixin to expose Raid's level field (server level).
 */
@Mixin(Raid.class)
public interface RaidAccessor {
    @Accessor("level")
    ServerLevel getLevel();
}