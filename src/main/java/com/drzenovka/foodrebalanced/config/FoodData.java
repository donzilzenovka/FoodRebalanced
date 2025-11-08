package com.drzenovka.foodrebalanced.config;

import java.util.ArrayList;
import java.util.List;

public class FoodData {

    // Use boxed types so Gson can omit fields when null
    public Integer hunger;
    public Float saturation;
    public List<EffectData> effects;
    public List<EnchantData> enchantments;

    public FoodData() {
        this.effects = new ArrayList<>();
        this.enchantments = new ArrayList<>();
    }

    public FoodData(int hunger, float saturation) {
        this();
        this.hunger = hunger;
        this.saturation = saturation;
    }

    @Override
    public String toString() {
        return "FoodData[hunger=" + hunger + ", saturation=" + saturation +
            ", effects=" + effects + ", enchantments=" + enchantments + "]";
    }

    public static class EffectData {
        public String id;          // namespaced id, e.g., minecraft:regeneration
        public Integer duration;   // ticks
        public Integer amplifier;
        public Float chance;       // 0.0 - 1.0

        public EffectData() {}

        public EffectData(String id, int duration, int amplifier, float chance) {
            this.id = id;
            this.duration = duration;
            this.amplifier = amplifier;
            this.chance = chance;
        }

        @Override
        public String toString() {
            return "EffectData[id=" + id + ", duration=" + duration +
                ", amplifier=" + amplifier + ", chance=" + chance + "]";
        }
    }

    public static class EnchantData {
        public String id;       // namespaced id or numeric id as string
        public Integer level;

        public EnchantData() {}

        public EnchantData(String id, int level) {
            this.id = id;
            this.level = level;
        }

        @Override
        public String toString() {
            return "EnchantData[id=" + id + ", level=" + level + "]";
        }
    }
}
