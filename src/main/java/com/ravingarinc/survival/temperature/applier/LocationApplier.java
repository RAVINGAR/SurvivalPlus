package com.ravingarinc.survival.temperature.applier;

import com.ravingarinc.survival.character.SurvivalPlayer;
import com.ravingarinc.survival.comp.WorldGuardHandler;
import com.ravingarinc.survival.temperature.BiomeCondition;
import com.ravingarinc.survival.temperature.TemperatureManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LocationApplier implements Applyable {
    private final TemperatureManager manager;
    private final WorldGuardHandler handler;

    private final List<String> unapplicableBiomes;

    private final double underground;

    public LocationApplier(final TemperatureManager manager, final WorldGuardHandler handler) {
        this.manager = manager;
        this.handler = handler;
        this.underground = manager.getSettings().underGroundMultiplier;
        this.unapplicableBiomes = new ArrayList<>();
        unapplicableBiomes.add("dripstone_caves");
        unapplicableBiomes.add("lush_caves");
        unapplicableBiomes.add("deep_dark");
        unapplicableBiomes.add("nether_wastes");
        unapplicableBiomes.add("soul_sand_valley");
        unapplicableBiomes.add("crimson_forest");
        unapplicableBiomes.add("warped_forest");
        unapplicableBiomes.add("basalt_deltas");
        unapplicableBiomes.add("the_end");
        unapplicableBiomes.add("small_end_islands");
        unapplicableBiomes.add("end_midlands");
        unapplicableBiomes.add("end_barrens");
    }

    @Override
    public double apply(final SurvivalPlayer player, final double previousTemperature) {
        final Player bukkitPlayer = player.getPlayer();
        if (handler.isLoaded()) {
            final Optional<Double> opt = handler.getTemperature(bukkitPlayer.getUniqueId());
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        final Location location = bukkitPlayer.getLocation();
        final World world = Objects.requireNonNull(location.getWorld(), "World should not be null for player!");
        final Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        final BiomeCondition condition = manager.getBiomeCondition(biome);
        if (condition == null) {
            return previousTemperature;
        }
        double temperature = condition.getTemperature(world.getName());
        if (!unapplicableBiomes.contains(biome.getKey().getKey().toLowerCase())) {
            if (location.getBlockY() < 62) {
                temperature *= temperature < 0 ? (1 + 1 - underground) : underground;
            }
        }
        return temperature;
    }
}
