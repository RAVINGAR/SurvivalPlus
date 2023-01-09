package com.ravingarinc.survival.temperature;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public class BiomeCondition {
    public static final long duskStart = 12000;
    public static final long duskEnd = 13500;
    public static final long dawnStart = 23000;
    public static final long dawnEnd = 1000;
    private final double dayTemp;
    private final double nightTemp;
    private final double randomMod;
    private final Map<String, AtomicDouble> temperatureMap;
    private double lastRandomMod;

    /**
     * The biome applier, day and night temperatures should already be multiplied by the base-biome-temperature
     */
    public BiomeCondition(final double dayTemp, final double nightTemp, final double randomMod) {
        this.dayTemp = dayTemp;
        this.nightTemp = nightTemp;
        this.randomMod = randomMod;
        this.lastRandomMod = 0;
        this.temperatureMap = new HashMap<>();
    }

    public void registerWorld(final String worldName) {
        temperatureMap.put(worldName, new AtomicDouble(dayTemp));
    }

    /**
     * Recalculate the temperature for a given world given a specific time.
     *
     * @param worldName
     * @param time
     */
    public void recalculateTemperature(final String worldName, final long time) {
        final AtomicDouble atomicDouble = temperatureMap.get(worldName);
        if (atomicDouble == null) {
            return;
        }
        final double temperature = BiomeTime.getCurrentTemperature(time, dayTemp, nightTemp);
        atomicDouble.set(temperature * (1 + lastRandomMod));
    }

    public double getTemperature(final String worldName) {
        final AtomicDouble atomicDouble = temperatureMap.get(worldName);
        if (atomicDouble == null) {
            return dayTemp;
        } else {
            return atomicDouble.get();
        }
    }

    public void recalculateRandomModifier(final Random random) {
        if (randomMod == 0) {
            return;
        }
        final boolean positive = randomMod > 0;
        double newRandom = random.nextDouble(Math.abs(randomMod));
        if (positive) {
            newRandom *= -1;
        }
        lastRandomMod = newRandom;
    }

    private enum BiomeTime {
        SUNSET((time) -> time >= duskStart && time < duskEnd) {
            @Override
            public double calculateTemperature(final long time, final double dayTemp, final double nightTemp) {
                return ((nightTemp - dayTemp) / (duskEnd - duskStart)) * (time - duskStart) + dayTemp;
            }
        },
        SUNRISE((time) -> time >= dawnStart || (time >= 0 && time < dawnEnd)) {
            @Override
            public double calculateTemperature(final long time, final double dayTemp, final double nightTemp) {
                final long x = time < 24000 && time >= dawnStart
                        ? (time - dawnStart)
                        : 24000 - dawnStart + time;
                return ((dayTemp - nightTemp) / (24000 - dawnStart + dawnEnd)) * x + nightTemp;
            }
        },
        NIGHT((time) -> time >= duskEnd) {
            @Override
            public double calculateTemperature(final long time, final double dayTemp, final double nightTemp) {
                return nightTemp;
            }
        },
        DAY((time) -> time >= dawnEnd) {
            @Override
            public double calculateTemperature(final long time, final double dayTemp, final double nightTemp) {
                return dayTemp;
            }
        };

        private final Predicate<Long> predicate;

        BiomeTime(final Predicate<Long> predicate) {
            this.predicate = predicate;
        }

        public static double getCurrentTemperature(final long time, final double dayTemp, final double nightTemp) {
            if (dayTemp == nightTemp) {
                return dayTemp;
            }
            for (final BiomeTime biome : values()) {
                if (biome.predicate.test(time)) {
                    return biome.calculateTemperature(time, dayTemp, nightTemp);
                }
            }
            return dayTemp;
        }

        protected abstract double calculateTemperature(long time, double dayTemp, double nightTemp);
    }
}
