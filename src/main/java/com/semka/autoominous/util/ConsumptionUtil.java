package com.semka.autoominous.util;

import com.semka.autoominous.config.AutoOminousConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.core.NonNullList;

/**
 * Utility methods for locating and consuming ominous bottles and applying BAD_OMEN effect.
 */
public final class ConsumptionUtil {

    private ConsumptionUtil() {}

    /**
     * Find an OMINOUS_BOTTLE in player's inventory, consume one (shrink), and apply BAD_OMEN directly.
     * Returns true if consumption occurred.
     *
     * This method performs direct item shrinking (stack.shrink(1)) and direct effect application
     * (player.addEffect(...)). It does NOT simulate use() or consumeItem().
     *
     * Must be called on server thread.
     */
    public static boolean findAndConsumeOminousAndApplyEffect(Player player, AutoOminousConfig cfg) {
        if (player == null || cfg == null) return false;
        PlayerInventory inventory = player.getInventory();
        NonNullList<ItemStack> items = inventory.items;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack != null && stack.is(Items.OMINOUS_BOTTLE)) {
                // consume one
                stack.shrink(1);
                // if stack becomes empty, keep the empty slot (player inventory handles it)
                int amplifier = Math.max(0, cfg.badOmenLevel - 1);
                int duration = Math.max(1, cfg.badOmenDurationTicks);
                // Directly apply effect
                MobEffectInstance eff = new MobEffectInstance(MobEffects.BAD_OMEN, duration, amplifier, false, true);
                if (player instanceof LivingEntity) {
                    ((LivingEntity) player).addEffect(eff);
                } else {
                    player.addEffect(eff);
                }
                return true;
            }
        }
        return false;
    }
}