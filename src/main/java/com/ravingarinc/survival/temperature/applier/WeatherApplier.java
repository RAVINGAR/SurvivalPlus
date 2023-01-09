package com.ravingarinc.survival.temperature.applier;

import com.ravingarinc.survival.character.SurvivalPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;

public class WeatherApplier implements Applyable {

    private final double raining;
    private final double thunderStorm;
    private final double baseTemperature;

    public WeatherApplier(final double raining, final double thunderStorm, final double baseTemperature) {
        this.raining = raining;
        this.thunderStorm = thunderStorm;
        this.baseTemperature = baseTemperature;
    }

    @Override
    public double apply(final SurvivalPlayer player, final double previousTemperature) {
        final Player bukkitPlayer = player.getPlayer();
        final Location location = bukkitPlayer.getLocation();
        final World world = Objects.requireNonNull(location.getWorld(), "Player's world should not be null!");

        double temperature = previousTemperature;
        final int y = location.getBlockY();
        if (y >= 62 && y < world.getHighestBlockYAt(location)) {
            temperature = (temperature + baseTemperature) / 2;
        } else {
            final boolean isNegative = temperature < 0;
            if (world.isThundering()) {
                temperature *= isNegative ? (1 + 1 - thunderStorm) : thunderStorm;
            } else if (world.hasStorm()) {
                temperature *= isNegative ? (1 + 1 - raining) : raining;
            }
        }


        return temperature;
    }
}
