package com.drzenovka.foodrebalanced.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

import com.drzenovka.foodrebalanced.FoodRebalanced;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.registry.GameData;

public class FoodConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Map<String, FoodData> FOOD_DATA = new LinkedHashMap<>();

    /** Initialize / load config */
    public static void init() {
        loadConfig();
    }

    public static void loadConfig() {
        File configDir = FoodRebalanced.configDir;
        if (!configDir.exists()) configDir.mkdirs();

        File configFile = new File(configDir, "food_overrides.json");

        // Load existing JSON
        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    String id = entry.getKey();
                    FoodData data = GSON.fromJson(entry.getValue(), FoodData.class);
                    FOOD_DATA.put(id, data);
                }
                System.out.println("[FoodRebalanced] Loaded " + FOOD_DATA.size() + " food entries from JSON.");
            } catch (Exception e) {
                System.err.println("[FoodRebalanced] Failed to load food_overrides.json");
                e.printStackTrace();
            }
        }

        // Generate vanilla food entries if missing
        for (Object obj : GameData.getItemRegistry()) {
            if (!(obj instanceof ItemFood)) continue;
            ItemFood food = (ItemFood) obj;
            String id = GameData.getItemRegistry().getNameForObject(food);
            if (id == null || FOOD_DATA.containsKey(id)) continue;

            FoodData data = new FoodData();
            data.hunger = food.func_150905_g(new ItemStack(food));
            data.saturation = food.func_150906_h(new ItemStack(food));
            data.effects = new ArrayList<>();
            data.enchantments = new ArrayList<>();

            FOOD_DATA.put(id, data);
        }

        // Save merged JSON
        saveConfig();
    }


    public static void registerEatenItem(ItemStack stack, int hunger, float saturation) {
        if (stack == null) return;

        String id = GameData.getItemRegistry().getNameForObject(stack.getItem());
        if (id == null) id = stack.getUnlocalizedName(); // fallback if obfuscated

        if (FOOD_DATA.containsKey(id)) return; // already registered

        FoodData data = new FoodData(hunger, saturation);
        data.effects = new ArrayList<>();
        data.enchantments = new ArrayList<>();

        FOOD_DATA.put(id, data);
        saveConfig(); // persist immediately

        // Log registration with timestamp
        String time = java.time.LocalDateTime.now().toString();
        System.out.println("[FoodRebalanced] Registered new edible item: " + id + " at " + time
            + " (hunger=" + hunger + ", saturation=" + saturation + ")");
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

    /** Get FoodData for an ItemStack */
    public static FoodData getFoodData(ItemStack stack) {
        if (stack == null) return null;
        String id = GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
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
