package com.drzenovka.foodrebalanced.handler;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;

import com.drzenovka.foodrebalanced.config.FoodConfigManager;
import com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodEffectHandler {

    private static final Random RNG = new Random();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new FoodEffectHandler());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerUse(PlayerUseItemEvent.Finish event) {
        ItemStack stack = event.item;
        if (stack == null || !(stack.getItem() instanceof ItemFood)) return;

        FoodData data = FoodConfigManager.getFoodData(stack);
        if (data == null) {
            FoodConfigManager.registerEatenItem(stack);
            data = FoodConfigManager.getFoodData(stack);
            if (data == null) return;
        }

        System.out.println("eating: " + data.name);

        EntityPlayer player = event.entityPlayer;
        ItemFood item = (ItemFood) stack.getItem();

        // Override hunger and saturation from JSON if provided
        int jsonHunger = data.hunger != null ? data.hunger : item.func_150905_g(stack);
        float jsonSaturation = data.saturation != null ? data.saturation : item.func_150906_h(stack);

        // Get vanilla values
        int vanillaHunger = item.func_150905_g(stack);
        float vanillaSaturation = item.func_150906_h(stack);

        if (!player.worldObj.isRemote) {
            if (player.getFoodStats().getFoodLevel() <= 20) {
                // Remove the vanilla contribution first
                player.getFoodStats().addStats(-vanillaHunger, -vanillaSaturation);
                // Apply the JSON override
                player.getFoodStats().addStats(jsonHunger, jsonSaturation);
            }

        }

        // Handle potion effects
        if (data.effects != null) {
            for (FoodData.EffectData ed : data.effects) {
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
    }

    private Potion getPotionByName(String name) {
        if (name == null) return null;
        name = name.toLowerCase();
        if (name.startsWith("minecraft:")) name = name.substring(10);

        switch (name) {
            case "regeneration": return Potion.regeneration;
            case "absorption": return Potion.field_76444_x;
            case "hunger": return Potion.hunger;
            case "strength": case "damage_boost": return Potion.damageBoost;
            case "heal": case "instant_health": return Potion.heal;
            case "instant_damage": return Potion.harm;
            case "fire_resistance": return Potion.fireResistance;
            case "resistance": return Potion.resistance;
            case "speed": return Potion.moveSpeed;
            case "slowness": return Potion.moveSlowdown;
            case "poison": return Potion.poison;
            case "wither": return Potion.wither;
            case "night_vision": return Potion.nightVision;
            case "invisibility": return Potion.invisibility;
            case "water_breathing": return Potion.waterBreathing;
            case "jump_boost": return Potion.jump;
        }
        return null;
    }
}
