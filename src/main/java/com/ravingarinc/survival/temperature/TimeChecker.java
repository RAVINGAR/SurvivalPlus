package com.ravingarinc.survival.temperature;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TimeChecker extends BukkitRunnable {
    private final Collection<BiomeCondition> biomeConditions;

    private final Map<String, Integer> currentTimes;

    private final List<World> validWorlds;

    private final Random random;

    public TimeChecker(final Collection<BiomeCondition> biomeConditions, final List<World> validWorlds) {
        this.biomeConditions = biomeConditions;
        this.currentTimes = new HashMap<>();
        this.validWorlds = validWorlds;
        this.random = new Random(System.currentTimeMillis());
    }

    @Override
    public void run() {
        for (final World world : validWorlds) {
            final long time = world.getTime();
            final int halfHour = (int) (time / 500);

            final String worldName = world.getName();
            final Integer lastHalfHour = currentTimes.get(worldName);
            if (lastHalfHour == null || lastHalfHour != halfHour) {
                currentTimes.put(world.getName(), lastHalfHour);
                if (halfHour == 0) {
                    // This isn't per world so it may be recalculated multiple times!
                    biomeConditions.forEach(biome -> biome.recalculateRandomModifier(random));
                }
                biomeConditions.forEach(biome -> biome.recalculateTemperature(worldName, time));
            }
        }
    }
}
