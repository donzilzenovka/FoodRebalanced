package com.drzenovka.foodrebalanced.config;

import java.util.ArrayList;
import java.util.List;

public class FoodData {

    // Use boxed types so Gson can omit fields when null
    public Integer meta;
    public Integer hunger;
    public Float saturation;
    public List<EffectData> effects;

    public FoodData() {
        this.effects = new ArrayList<>();
    }

    public FoodData(int meta, int hunger, float saturation) {
        this();
        this.meta = meta;
        this.hunger = hunger;
        this.saturation = saturation;
    }

    @Override
    public String toString() {
        return "FoodData[meta=" + meta
            +", hunger="
            + hunger
            + ", saturation="
            + saturation
            + ", effects="
            + effects
            + "]";
    }

    public static class EffectData {

        public String id; // namespaced id, e.g., minecraft:regeneration
        public Integer duration; // ticks
        public Integer amplifier;
        public Float chance; // 0.0 - 1.0

        public EffectData() {}

        public EffectData(String id, int duration, int amplifier, float chance) {
            this.id = id;
            this.duration = duration;
            this.amplifier = amplifier;
            this.chance = chance;
        }

        @Override
        public String toString() {
            return "EffectData[id=" + id
                + ", duration="
                + duration
                + ", amplifier="
                + amplifier
                + ", chance="
                + chance
                + "]";
        }
    }
}
