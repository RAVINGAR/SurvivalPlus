package com.ravingarinc.survival.comp;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.api.ModuleLoadException;
import com.ravingarinc.survival.character.CharacterManager;
import com.ravingarinc.survival.temperature.BiomeCondition;
import com.ravingarinc.survival.temperature.TemperatureManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderAPIHandler extends Module {
    private CharacterManager characterManager;
    private TemperatureManager temperatureManager;
    private Expansion expansion;

    public PlaceholderAPIHandler(final SurvivalPlus plugin) {
        super(PlaceholderAPIHandler.class, plugin);
    }

    @Override
    protected void load() throws ModuleLoadException {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            throw new ModuleLoadException(this, "PlaceholderAPI is not enabled!");
        } else {
            characterManager = plugin.getModule(CharacterManager.class);
            temperatureManager = plugin.getModule(TemperatureManager.class);
            expansion = new Expansion();
            expansion.register();
        }
    }

    @Override
    public void cancel() {
        if (expansion != null) {
            expansion.unregister();
        }
        expansion = null;
    }

    public class Expansion extends PlaceholderExpansion {
        private final Map<String, Biome> biomeNames;

        public Expansion() {
            biomeNames = new HashMap<>();
            for (final Biome biome : Biome.values()) {
                biomeNames.put(biome.name(), biome);
            }
        }

        @Override
        public @NotNull String getIdentifier() {
            return "SurvivalPlusExpansion";
        }

        @Override
        public @NotNull String getAuthor() {
            return "RAVINGAR";
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean persist() {
            return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
        }

        @Override
        public String onRequest(final OfflinePlayer player, final @NotNull String params) {
            if (player != null) {
                if (params.equalsIgnoreCase("sp_player_temp") || params.equalsIgnoreCase("sp_player_temperature")) {
                    final Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer == null) {
                        return null;
                    }
                    return String.format("%.2f", characterManager.getPlayer(onlinePlayer).getTemperature());
                } else if (params.equalsIgnoreCase("sp_player_env") || params.equalsIgnoreCase("sp_player_environment")) {
                    final Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer == null) {
                        return null;
                    }
                    return String.format("%.2f", temperatureManager.getEnvironmentTemperatureForPlayer(onlinePlayer));
                }
            }

            String biome = null;
            if (params.startsWith("sp_biome_temp_")) {
                biome = params.substring(14);
            }
            if (params.startsWith("sp_biome_temperature_")) {
                biome = params.substring(21);
            }

            if (biome != null) {
                final Biome b = biomeNames.get(biome);
                if (b != null) {
                    final BiomeCondition condition = temperatureManager.getBiomeCondition(b);
                    if (condition != null) {
                        final String worldName;
                        if (player == null || player.getPlayer() == null) {
                            worldName = Bukkit.getWorlds().get(0).getName();
                        } else {
                            worldName = player.getPlayer().getWorld().getName();
                        }
                        return String.format("%.2f", condition.getTemperature(worldName));
                    }
                }
            }
            return null;
        }
    }
}