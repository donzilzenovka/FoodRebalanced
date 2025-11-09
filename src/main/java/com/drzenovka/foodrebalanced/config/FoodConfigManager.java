package com.drzenovka.foodrebalanced.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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

        // Generate missing vanilla entries using helper
        for (Object obj : GameData.getItemRegistry()) {
            if (!(obj instanceof ItemFood)) continue;
            ItemFood food = (ItemFood) obj;
            String id = GameData.getItemRegistry()
                .getNameForObject(food);
            if (id == null || FOOD_DATA.containsKey(id)) continue;

            FOOD_DATA.put(id, createFoodData(new ItemStack(food)));
        }

        saveConfig();
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

    public static void registerEatenItem(ItemStack stack) {
        if (stack == null) return;

        String id = GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
        if (id == null || FOOD_DATA.containsKey(id)) return;

        FOOD_DATA.put(id, createFoodData(stack));
        System.out.println("[FoodRebalanced] Registered new edible item: " + id + " at " + System.currentTimeMillis());

        saveConfig();
    }

    /** Create FoodData for an ItemStack, detecting hunger, saturation, and effects */
    private static FoodData createFoodData(ItemStack stack) {
        FoodData data = new FoodData();

        data.meta = stack.getItemDamage(); // <-- store item damage / meta

        if (stack.getItem() instanceof ItemFood food) {
            data.hunger = food.func_150905_g(stack);
            data.saturation = food.func_150906_h(stack);
            data.effects = new ArrayList<>();

            // Detect vanilla potion effects
            try {
                int potionId = food.potionId;
                int duration = food.potionDuration;
                int amplifier = food.potionAmplifier;
                float chance = food.potionEffectProbability;

                if (potionId > 0 && potionId < Potion.potionTypes.length) {
                    Potion potion = Potion.potionTypes[potionId];
                    if (potion != null) {
                        data.effects
                            .add(new FoodData.EffectData("minecraft:" + potion.getName(), duration, amplifier, chance));
                    }
                }
            } catch (Exception ignored) {}

            addNBTPotionEffects(stack, data);
        }

        if (data.effects == null) data.effects = new ArrayList<>();

        return data;
    }

    /** Get FoodData for an ItemStack */
    public static FoodData getFoodData(ItemStack stack) {
        if (stack == null) return null;
        String id = GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
        return FOOD_DATA.get(id);
    }

    /** Data structure compatible with Gson */
    public static class FoodData {

        public int meta;
        public Integer hunger;
        public Float saturation;
        public List<EffectData> effects = new ArrayList<>();

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

    private static void addNBTPotionEffects(ItemStack stack, FoodData data) {
        if (!stack.hasTagCompound()) return;
        NBTTagCompound nbt = stack.getTagCompound();

        if (!nbt.hasKey("CustomPotionEffects")) return;
        NBTTagList effectList = nbt.getTagList("CustomPotionEffects", 10); // 10 = Compound tag
        for (int i = 0; i < effectList.tagCount(); i++) {
            NBTTagCompound effectNBT = effectList.getCompoundTagAt(i);
            int id = effectNBT.getInteger("Id");
            int duration = effectNBT.getInteger("Duration");
            int amplifier = effectNBT.getInteger("Amplifier");
            float chance = effectNBT.hasKey("Chance") ? effectNBT.getFloat("Chance") : 1.0f;

            if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null) {
                data.effects.add(new FoodData.EffectData(
                    "minecraft:" + Potion.potionTypes[id].getName(),
                    duration,
                    amplifier,
                    chance
                ));
            }
        }
    }

}
