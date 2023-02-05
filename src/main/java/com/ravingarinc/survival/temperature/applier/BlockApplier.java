package com.ravingarinc.survival.temperature.applier;

import com.ravingarinc.survival.character.SurvivalPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;

import java.util.Map;

public class BlockApplier implements Applyable {

    private final Map<Material, Double> temperatureBlocks;

    private final int radius;

    public BlockApplier(final Map<Material, Double> temperatureBlocks, final int radius) {
        this.temperatureBlocks = temperatureBlocks;
        this.radius = radius;
    }

    @Override
    public double apply(final SurvivalPlayer player, final double previousTemperature) {
        final Location location = player.getPlayer().getLocation();
        final World world = location.getWorld();
        final int dX = location.getBlockX();
        final int dY = location.getBlockY();
        final int dZ = location.getBlockZ();

        double i = 0;
        double temperatureTotal = 0.0;
        for (int x = dX - radius; x < dX + radius; x++) {
            for (int y = dY - radius; y < dY + radius; y++) {
                for (int z = dZ - radius; z < dZ + radius; z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    final Material type = block.getType();
                    final Double temperature = temperatureBlocks.get(type);
                    if (temperature == null) {
                        continue;
                    }
                    if (block.getBlockData() instanceof Lightable) {
                        if (!((Lightable) block.getBlockData()).isLit()) {
                            continue;
                        }
                    }
                    i++;
                    final double distanceFactor = 1 - (((double) Math.max(Math.abs(x - dX), Math.abs(z - dZ))) / radius);
                    temperatureTotal += (temperature * distanceFactor);
                }
            }
        }
        temperatureTotal = temperatureTotal / (i > 1.5 ? i / 1.5 : 1.0);

        return previousTemperature + temperatureTotal;
    }
}
