package com.semka.autoominous.mixin;

import com.semka.autoominous.AutoOminousMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.server.level.ServerLevel;

/**
 * Mixin to detect when a Raid ends. We inject at the tail of the end() method.
 * When a raid ends we call AutoOminousMod#onRaidEnd with the raid's level.
 *
 * NOTE: If in your mappings the method is named differently, change "end" to the correct method name.
 */
@Mixin(Raid.class)
public abstract class RaidMixin implements RaidAccessor {

    @Inject(method = "end", at = @At("TAIL"))
    private void onRaidEndInject(CallbackInfo ci) {
        ServerLevel level = getLevel();
        if (level == null) return;
        // Call mod logic (this runs on server thread because raid end is executed there)
        AutoOminousMod.getInstance().onRaidEnd(level);
    }
}