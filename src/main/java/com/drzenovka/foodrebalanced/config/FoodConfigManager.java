package com.drzenovka.foodrebalanced.config;

import com.drzenovka.foodrebalanced.FoodRebalanced;
import com.google.gson.*;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import cpw.mods.fml.common.registry.GameData;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class FoodConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, FoodData> FOOD_DATA = new LinkedHashMap<>();

    /** Initialize / load config */
    public static void init() {
        loadConfig();
    }

    /** Load config from JSON, generate defaults if missing */
    public static void loadConfig() {
        File configDir = FoodRebalanced.configDir;
        if (!configDir.exists()) configDir.mkdirs();

        File configFile = new File(configDir, "food_overrides.json");
        if (!configFile.exists()) {
            generateDefaultConfig(configFile);
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            FOOD_DATA.clear();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String id = entry.getKey();
                FoodData data = GSON.fromJson(entry.getValue(), FoodData.class);
                FOOD_DATA.put(id, data);
            }
            System.out.println("[FoodRebalanced] Loaded " + FOOD_DATA.size() + " food entries.");
        } catch (Exception e) {
            System.err.println("[FoodRebalanced] Failed to load food_overrides.json");
            e.printStackTrace();
        }
    }

    /** Reload config at runtime */
    public static void reload() {
        loadConfig();
        System.out.println("[FoodRebalanced] Config reloaded.");
    }

    /** Save current FOOD_DATA to JSON */
    public static void saveConfig() {
        File configFile = new File(FoodRebalanced.configDir, "food_overrides.json");
        try (Writer writer = new FileWriter(configFile)) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, FoodData> entry : FOOD_DATA.entrySet()) {
                obj.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
            }
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            System.err.println("[FoodRebalanced] Failed to save food_overrides.json");
            e.printStackTrace();
        }
    }

    /** Generate default JSON based on vanilla ItemFood items */
    private static void generateDefaultConfig(File configFile) {
        FOOD_DATA.clear();

        for (Object obj : GameData.getItemRegistry()) {
            if (!(obj instanceof ItemFood food)) continue;
            String id = GameData.getItemRegistry().getNameForObject(food);

            FoodData data = new FoodData();
            data.hunger = food.func_150905_g(new ItemStack(food));
            data.saturation = food.func_150906_h(new ItemStack(food));
            data.effects = new ArrayList<>();
            data.enchantments = new ArrayList<>();

            // Extract vanilla potion effects
            try {
                Field potionIdField = getFieldAny("potionId", "field_77851_ca");
                Field potionDurationField = getFieldAny("potionDuration", "field_77852_cb");
                Field potionAmplifierField = getFieldAny("potionAmplifier", "field_77853_cc");
                Field potionEffectProbabilityField = getFieldAny("potionEffectProbability", "field_77854_cd");

                int potionId = potionIdField.getInt(food);
                int duration = potionDurationField.getInt(food);
                int amplifier = potionAmplifierField.getInt(food);
                float chance = potionEffectProbabilityField.getFloat(food);

                if (potionId > 0 && potionId < Potion.potionTypes.length) {
                    Potion potion = Potion.potionTypes[potionId];
                    if (potion != null) {
                        data.effects.add(new FoodData.EffectData(
                            "minecraft:" + potion.getName(),
                            duration,
                            amplifier,
                            chance
                        ));
                    }
                }
            } catch (Exception ignored) {}

            FOOD_DATA.put(id, data);
        }

        saveConfig();
        System.out.println("[FoodRebalanced] Generated default food config with " + FOOD_DATA.size() + " entries.");
    }

    /** Get FoodData for an ItemStack */
    public static FoodData getFoodData(ItemStack stack) {
        if (stack == null) return null;
        String id = GameData.getItemRegistry().getNameForObject(stack.getItem());
        return FOOD_DATA.get(id);
    }

    /** Reflection helper */
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

    /** Data structure compatible with Gson */
    public static class FoodData {
        public Integer hunger;
        public Float saturation;
        public List<EffectData> effects = new ArrayList<>();
        public List<EnchantData> enchantments = new ArrayList<>();

        public FoodData() {}

        public FoodData(int hunger, float saturation) {
            this.hunger = hunger;
            this.saturation = saturation;
        }

        public static class EffectData {
            public String id;
            public Integer duration;
            public Integer amplifier;
            public Float chance;

            public EffectData() {}
            public EffectData(String id, int duration, int amplifier, float chance) {
                this.id = id;
                this.duration = duration;
                this.amplifier = amplifier;
                this.chance = chance;
            }
        }

        public static class EnchantData {
            public String id;
            public Integer level;

            public EnchantData() {}
            public EnchantData(String id, int level) {
                this.id = id;
                this.level = level;
            }
        }
    }
}
