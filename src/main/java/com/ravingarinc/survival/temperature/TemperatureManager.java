package com.ravingarinc.survival.temperature;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.character.CharacterManager;
import com.ravingarinc.survival.file.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemperatureManager extends Module {
    private final Settings settings;

    private final Map<Material, Double> temperatureBlocks;

    private final Map<Biome, BiomeCondition> biomeConditions;

    private final List<World> validWorlds;

    private TemperatureRunner temperatureRunner;

    private TimeChecker timeChecker;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(SurvivalPlus, Class)}.
     * Implementations of Managers should have one public constructor with a JavaPlugin object parameter.
     * The implementing constructor CANNOT call {@link SurvivalPlus#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param plugin The owning plugin
     */
    public TemperatureManager(final SurvivalPlus plugin) {
        super(TemperatureManager.class, plugin, CharacterManager.class);
        this.settings = new Settings();
        this.temperatureBlocks = new HashMap<>();
        this.biomeConditions = new HashMap<>();
        this.validWorlds = new ArrayList<>();
    }

    @Nullable
    public BiomeCondition getBiomeCondition(final Biome biome) {
        return biomeConditions.get(biome);
    }

    public List<String> getFormattedBiomeList() {
        final List<String> list = new ArrayList<>();
        biomeConditions.keySet().forEach(biome -> list.add(biome.name().toLowerCase()));
        return list;
    }

    @Nullable
    public Double getEnvironmentTemperatureForPlayer(final Player player) {
        if (temperatureRunner != null) {
            return temperatureRunner.getEnvironmentTemperature(plugin.getModule(CharacterManager.class).getPlayer(player));
        }
        return null;
    }

    protected Map<Material, Double> getTemperatureBlocks() {
        return temperatureBlocks;
    }

    public void clearTemperatureBlocks() {
        this.temperatureBlocks.clear();
    }

    public void addTemperatureBlock(final Material material, final double temperature) {
        this.temperatureBlocks.put(material, temperature);
    }

    public void clearBiomeConditions() {
        this.biomeConditions.clear();
    }

    public void addBiomeCondition(final Biome biome, final double dayTempMulti, final double nightTempMulti, final double random) {
        this.biomeConditions.put(biome,
                new BiomeCondition(
                        dayTempMulti * settings.baseBiomeTemperature,
                        nightTempMulti * settings.baseBiomeTemperature, random));
    }

    @Override
    protected void load() {
        for (final World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                validWorlds.add(world);
            }
        }
        validWorlds.forEach(world -> this.biomeConditions.values().forEach(biome -> biome.registerWorld(world.getName())));

        this.timeChecker = new TimeChecker(biomeConditions.values(), validWorlds);
        this.timeChecker.runTaskTimer(plugin, 0, 500);

        this.temperatureRunner = new TemperatureRunner(plugin);
        this.temperatureRunner.runTaskTimer(plugin, settings.checkInterval, settings.checkInterval);
    }

    @Override
    public void cancel() {
        this.timeChecker.cancel();
        this.temperatureRunner.cancel();

        this.validWorlds.clear();
    }

    public Settings getSettings() {
        return settings;
    }
}
