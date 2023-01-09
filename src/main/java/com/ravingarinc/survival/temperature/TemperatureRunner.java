package com.ravingarinc.survival.temperature;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.character.CharacterManager;
import com.ravingarinc.survival.character.SurvivalPlayer;
import com.ravingarinc.survival.comp.WorldGuardHandler;
import com.ravingarinc.survival.file.Settings;
import com.ravingarinc.survival.temperature.applier.Applyable;
import com.ravingarinc.survival.temperature.applier.BlockApplier;
import com.ravingarinc.survival.temperature.applier.LocationApplier;
import com.ravingarinc.survival.temperature.applier.MythicEffectApplier;
import com.ravingarinc.survival.temperature.applier.WeatherApplier;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TemperatureRunner extends BukkitRunnable {
    private final CharacterManager characterManager;

    private final List<Applyable> applyables;
    private final List<Applyable> resultApplyables;

    private final double baseTemperature;

    private final double inWater;

    private final List<GameMode> validGameModes;

    public TemperatureRunner(final SurvivalPlus plugin) {
        final TemperatureManager temperature = plugin.getModule(TemperatureManager.class);
        final CharacterManager characters = plugin.getModule(CharacterManager.class);
        final Settings settings = temperature.getSettings();

        this.baseTemperature = settings.baseBiomeTemperature;
        this.inWater = settings.inWater;
        this.characterManager = characters;
        this.applyables = new ArrayList<>();
        this.resultApplyables = new ArrayList<>();
        this.validGameModes = new ArrayList<>();
        this.validGameModes.add(GameMode.ADVENTURE);
        this.validGameModes.add(GameMode.SURVIVAL);

        applyables.add(new LocationApplier(temperature, plugin.getModule(WorldGuardHandler.class)));
        applyables.add(new WeatherApplier(settings.raining, settings.thunderStorm, settings.defaultTemperature));

        if (!temperature.getTemperatureBlocks().isEmpty()) {
            applyables.add(new BlockApplier(temperature.getTemperatureBlocks(), temperature.getSettings().blockRange));
        }

        applyables.add((player, previousTemperature) -> {
            final Player bukkitPlayer = player.getPlayer();
            if (bukkitPlayer.isInWater()) {
                return previousTemperature + inWater;
            }
            return previousTemperature;
        });

        resultApplyables.add((player, previousTemperature) -> {
            final double temp = player.apply(player, previousTemperature);
            player.setTemperature(temp);
            return temp;
        });

        if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            resultApplyables.add(new MythicEffectApplier(settings.mythicSkillArgs));
        }
    }

    @Override
    public void run() {
        characterManager.getAllPlayers().forEach(player -> {
            final Player bukkitPlayer = player.getPlayer();
            if (!bukkitPlayer.isDead() && validGameModes.contains(bukkitPlayer.getGameMode())) {
                applyTemperatureToApply(player, getEnvironmentTemperature(player));
            }
        });
    }

    public double getEnvironmentTemperature(final SurvivalPlayer player) {
        double temperature = baseTemperature;
        for (final Applyable applyable : applyables) {
            temperature = applyable.apply(player, temperature);
        }
        return temperature;
    }

    public void applyTemperatureToApply(final SurvivalPlayer player, double temperature) {
        for (final Applyable applyable : resultApplyables) {
            temperature = applyable.apply(player, temperature);
        }
    }
}
