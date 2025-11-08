package com.drzenovka.foodrebalanced.handler;

import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import com.drzenovka.foodrebalanced.config.FoodConfigManager;
import com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData;
import com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData.EffectData;
import com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData.EnchantData;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodEffectHandler {

    private static final Random RNG = new Random();

    /** Register event handler on the Forge event bus */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new FoodEffectHandler());
    }

    /** Reload the food overrides from config at runtime */
    public static void reloadConfig() {
        FoodConfigManager.reload();
        System.out.println("[FoodRebalanced] FoodEffectHandler: overrides reloaded.");
    }

    /** Called when a player finishes eating an item */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerUse(PlayerUseItemEvent.Finish event) {
        ItemStack stack = event.item;
        if (stack == null) return;

        // First, register the item if it's not yet in the config
        FoodData fd = FoodConfigManager.getFoodData(stack);
        if (fd == null) {
            // Use the item's real hunger/saturation if it's ItemFood, else default
            int hunger = 1;
            float saturation = 0.3f;

            if (stack.getItem() instanceof ItemFood food) {
                hunger = food.func_150905_g(stack);
                saturation = food.func_150906_h(stack);
            }

            FoodConfigManager.registerEatenItem(stack, hunger, saturation);
            fd = FoodConfigManager.getFoodData(stack);
            if (fd == null) return; // safety
        }

        EntityPlayer player = event.entityPlayer;

        // Apply potion effects
        if (fd.effects != null) {
            for (FoodData.EffectData ed : fd.effects) {
                if (ed == null || ed.id == null) continue;

                float chance = (ed.chance == null || ed.chance <= 0f) ? 1f : ed.chance;
                if (RNG.nextFloat() > chance) continue;

                Potion potion = getPotionByName(ed.id);
                if (potion != null && ed.duration != null && ed.duration > 0) {
                    int amp = (ed.amplifier != null) ? ed.amplifier : 0;
                    player.addPotionEffect(new PotionEffect(potion.id, ed.duration, amp));
                }
            }
        }

        // Apply enchantments
        if (fd.enchantments != null) {
            for (FoodData.EnchantData enf : fd.enchantments) {
                if (enf == null || enf.id == null) continue;
                Enchantment enc = getEnchantmentByName(enf.id);
                if (enc != null && enf.level != null) {
                    try {
                        stack.addEnchantment(enc, enf.level);
                    } catch (Exception ignored) {}
                }
            }
        }
    }


    /** Resolve a Potion from a namespaced ID (e.g., minecraft:regeneration) */
    private Potion getPotionByName(String namespaced) {
        if (namespaced == null) return null;

        String n = namespaced.toLowerCase();
        if (n.startsWith("minecraft:")) n = n.substring(10);

        // Vanilla 1.7.10 potion mappings
        switch (n) {
            case "regeneration":
                return Potion.regeneration;
            case "absorption":
                return Potion.field_76444_x;
            case "hunger":
                return Potion.hunger;
            case "strength":
            case "damage_boost":
                return Potion.damageBoost;
            case "heal":
            case "instant_health":
                return Potion.heal;
            case "instant_damage":
                return Potion.harm;
            case "fire_resistance":
                return Potion.fireResistance;
            case "resistance":
                return Potion.resistance;
            case "speed":
                return Potion.moveSpeed;
            case "slowness":
                return Potion.moveSlowdown;
            case "poison":
                return Potion.poison;
            case "wither":
                return Potion.wither;
            case "night_vision":
                return Potion.nightVision;
            case "invisibility":
                return Potion.invisibility;
            case "water_breathing":
                return Potion.waterBreathing;
            case "jump_boost":
                return Potion.jump;
        }

        // Attempt reflection fallback
        try {
            java.lang.reflect.Field f = Potion.class.getField(n);
            Object o = f.get(null);
            if (o instanceof Potion) return (Potion) o;
        } catch (Exception ignored) {}

        return null;
    }

    /** Resolve an Enchantment from a namespaced ID or numeric string */
    private Enchantment getEnchantmentByName(String namespaced) {
        if (namespaced == null) return null;

        String n = namespaced.toLowerCase();
        if (n.startsWith("minecraft:")) n = n.substring(10);

        // Try numeric ID first
        try {
            int id = Integer.parseInt(n);
            if (id >= 0 && id < Enchantment.enchantmentsList.length) return Enchantment.enchantmentsList[id];
        } catch (NumberFormatException ignored) {}

        // Search by name
        for (Enchantment e : Enchantment.enchantmentsList) {
            if (e == null) continue;
            if ((e.getName() != null && e.getName()
                .toLowerCase()
                .contains(n)) || (e.getTranslatedName(1) != null
                    && e.getTranslatedName(1)
                        .toLowerCase()
                        .contains(n))) {
                return e;
            }
        }

        return null;
    }
}
