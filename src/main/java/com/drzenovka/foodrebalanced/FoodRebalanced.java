package com.drzenovka.foodrebalanced;

import com.drzenovka.foodrebalanced.command.CommandFoodRebalanced;
import com.drzenovka.foodrebalanced.config.FoodConfigManager;
import com.drzenovka.foodrebalanced.handler.FoodEffectHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameData;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Random;

@Mod(modid = FoodRebalanced.MODID, name = FoodRebalanced.NAME, version = FoodRebalanced.VERSION)
public class FoodRebalanced {

    public static final String MODID = "foodrebalanced";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "Food Rebalanced";

    public static File configDir;
    private static final Random RAND = new Random();

    // --- MOD LIFECYCLE EVENTS ---

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Setup config directory
        configDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!configDir.exists()) configDir.mkdirs();

        // Load or generate food overrides JSON
        FoodConfigManager.loadConfig();

        // Register the FoodEffectHandler for runtime events
        FoodEffectHandler.register();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Safe command registration for 1.7.10
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandFoodRebalanced());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        applyJsonOverridesToFoodItems();
    }

    // --- APPLY FOOD OVERRIDES ---

    private void applyJsonOverridesToFoodItems() {
        for (Object obj : GameData.getItemRegistry()) {
            if (!(obj instanceof ItemFood food)) continue;
            String id = GameData.getItemRegistry().getNameForObject(food);

            com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData data =
                FoodConfigManager.getFoodData(new ItemStack(food));
            if (data == null) continue;

            try {
                // Override hunger & saturation
                Field healField = getFieldAny("healAmount", "field_77853_b");
                Field saturationField = getFieldAny("saturationModifier", "field_77854_c");
                healField.setInt(food, data.hunger != null ? data.hunger : 0);
                saturationField.setFloat(food, data.saturation != null ? data.saturation : 0f);

                // Reset default potion data
                Field potionIdField = getFieldAny("potionId", "field_77851_ca");
                Field potionDurationField = getFieldAny("potionDuration", "field_77852_cb");
                Field potionAmplifierField = getFieldAny("potionAmplifier", "field_77853_cc");
                Field potionEffectProbabilityField = getFieldAny("potionEffectProbability", "field_77854_cd");

                potionIdField.setInt(food, -1);
                potionDurationField.setInt(food, 0);
                potionAmplifierField.setInt(food, 0);
                potionEffectProbabilityField.setFloat(food, 0f);

                // Apply first JSON effect for vanilla handling
                if (data.effects != null && !data.effects.isEmpty()) {
                    com.drzenovka.foodrebalanced.config.FoodConfigManager.FoodData.EffectData e = data.effects.get(0);
                    Potion potion = getPotionByName(e.id);
                    if (potion != null) {
                        potionIdField.setInt(food, potion.id);
                        potionDurationField.setInt(food, e.duration != null ? e.duration : 0);
                        potionAmplifierField.setInt(food, e.amplifier != null ? e.amplifier : 0);
                        potionEffectProbabilityField.setFloat(food, e.chance != null ? e.chance : 0f);
                    }
                }

            } catch (Exception ex) {
                System.err.println("[FoodRebalanced] Failed to apply data for " + id);
                ex.printStackTrace();
            }
        }

        System.out.println("[FoodRebalanced] Applied food overrides successfully.");
    }

    // --- UTILITY METHODS ---

    private static Field getFieldAny(String... names) throws NoSuchFieldException {
        for (String n : names) {
            try {
                Field f = ItemFood.class.getDeclaredField(n);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException("No matching field found in " + ItemFood.class);
    }

    private static Potion getPotionByName(String id) {
        if (id == null) return null;
        if (id.startsWith("minecraft:")) id = id.substring(10);
        for (Potion p : Potion.potionTypes) {
            if (p != null && id.equalsIgnoreCase(p.getName())) return p;
        }
        return null;
    }
}
