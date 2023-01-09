package com.ravingarinc.survival.file;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.character.PlayerListener;
import com.ravingarinc.survival.comp.MMOItemHandler;
import com.ravingarinc.survival.temperature.TemperatureDeathBehaviour;
import com.ravingarinc.survival.temperature.TemperatureManager;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class ConfigManager extends Module {
    private final ConfigFile configFile;

    public ConfigManager(final SurvivalPlus plugin) {
        super(ConfigManager.class, plugin);
        this.configFile = new ConfigFile(plugin, "config.yml");
    }

    @Override
    protected void load() {
        final TemperatureManager manager = plugin.getModule(TemperatureManager.class);
        final Settings settings = manager.getSettings();

        final PlayerListener listener = plugin.getModule(PlayerListener.class);

        final FileConfiguration config = configFile.getConfig();
        consumeSection(config, "defaults", (child) -> {
            wrap("check-interval", child::getLong).ifPresent(v -> {
                if (v < 5) {
                    SurvivalPlus.log(Level.WARNING, "The value configured for temperature-check-interval is too low! This must be equal to or greater than 5 ticks!");
                    settings.checkInterval = 5;
                } else {
                    settings.checkInterval = v;
                }
            });
            wrap("minimum-temperature", child::getDouble).ifPresent(v -> settings.minTemperature = v);
            wrap("default-temperature", child::getDouble).ifPresent(v -> {
                settings.defaultTemperature = v;
                listener.respawnTemperature = v;
            });
            wrap("maximum-temperature", child::getDouble).ifPresent(v -> settings.maxTemperature = v);
            wrap("maximum-mmoitem-resistance", child::getDouble).ifPresent(v -> MMOItemHandler.maximumResistance = v);
            wrap("calculation-scale", child::getDouble).ifPresent(v -> settings.calculationScale = Math.max(v, 0.001));
        });

        consumeSection(config, "miscellaneous", (child) -> {
            wrap("is-underground", child::getDouble).ifPresent(v -> settings.underGroundMultiplier = v);
            wrap("on-fire", child::getDouble).ifPresent(v -> listener.onFireAmount = v);
            wrap("in-fire", child::getDouble).ifPresent(v -> listener.inFireAmount = v);
            wrap("in-lava", child::getDouble).ifPresent(v -> listener.inLavaAmount = v);
            wrap("in-water", child::getDouble).ifPresent(v -> settings.inWater = v);
            wrap("temperature-behaviour-on-death", child::getString).ifPresent(v -> {
                try {
                    listener.deathBehaviour = TemperatureDeathBehaviour.valueOf(v);
                } catch (final IllegalArgumentException exception) {
                    SurvivalPlus.log(Level.WARNING,
                            "Configuration option '%s' was invalid! " +
                                    "This must be either 'KEEP' or 'RESET'", v);
                }
            });
        });

        consumeSection(config, "temperature-effects", (child) -> wrap("values", child::getStringList).ifPresent(list -> {
            final List<SkillArg> args = settings.mythicSkillArgs;
            args.clear();
            list.forEach(entry -> {
                final String[] split = entry.split(" ");
                if (split.length > 1) {
                    final String[] range = split[1].split("to");
                    if (range.length == 2) {
                        try {
                            final double lower = Double.parseDouble(range[0]);
                            final double upper = Double.parseDouble(range[1]);
                            double percent = 100;
                            if (split.length > 2) {
                                final String str = split[2].endsWith("%")
                                        ? split[2].substring(0, split[2].length() - 1)
                                        : split[2];
                                percent = Double.parseDouble(str);
                            }
                            args.add(new SkillArg(split[0], lower, upper, percent));
                        } catch (final NumberFormatException e) {
                            SurvivalPlus.log(Level.WARNING, "Could not parse range value '%s' from line in 'temperature-effects.values'.", split[1]);
                        }
                    } else {
                        SurvivalPlus.log(Level.WARNING, "Could not parse range value '%s' from line in 'temperature-effects.values'.", split[1]);
                    }
                } else {
                    SurvivalPlus.log(Level.WARNING, "Could not parse line '%s' in 'temperature-effects.value'.", entry);
                }
            });
        }));

        consumeSection(config, "world", (child) -> {
            wrap("range", child::getInt).ifPresent(v -> settings.blockRange = v);
            wrap("blocks", child::getStringList).ifPresent(v -> {
                manager.clearTemperatureBlocks();
                for (final String entry : v) {
                    final String[] split = entry.split(" ");
                    if (split.length < 2) {
                        SurvivalPlus.log(Level.WARNING, "Not enough arguments specified for line '%s' in 'world.blocks'", entry);
                        continue;
                    }
                    final Material block = Material.matchMaterial(split[0]);
                    if (block == null) {
                        SurvivalPlus.log(Level.WARNING, "Unknown material called '%s' in 'world.blocks' list.", split[0]);
                        continue;
                    }
                    double amount = 0;
                    try {
                        amount = Double.parseDouble(split[1]);
                    } catch (final NumberFormatException exception) {
                        SurvivalPlus.log(Level.WARNING, "Could not parse '%s' as a decimal number in 'world.blocks'", split[1]);
                    }
                    if (amount == 0) {
                        continue;
                    }
                    manager.addTemperatureBlock(block, amount);
                }
            });
            consumeSection(child, "weather", (sub) -> {
                wrap("raining", sub::getDouble).ifPresent(v -> settings.raining = v);
                wrap("thunderstorm", sub::getDouble).ifPresent(v -> settings.thunderStorm = v);
            });

            wrap("base-biome-temperature", child::getDouble).ifPresent(v -> settings.baseBiomeTemperature = v);
            wrap("biomes", child::getStringList).ifPresent(v -> {
                manager.clearBiomeConditions();
                for (final String entry : v) {
                    final String[] split = entry.split(" ");
                    if (split.length < 2) {
                        SurvivalPlus.log(Level.WARNING, "Not enough arguments specified for line '%s' in 'world.biomes'", entry);
                        continue;
                    }
                    final Biome biome;
                    try {
                        biome = Biome.valueOf(split[0].toUpperCase().replaceAll("-", "_"));
                    } catch (final IllegalArgumentException exception) {
                        SurvivalPlus.log(Level.WARNING, "Unknown biome called '%s' in 'world.biomes' list.", split[0]);
                        continue;
                    }
                    double dayMulti = 0;
                    try {
                        dayMulti = Double.parseDouble(split[1]);
                    } catch (final NumberFormatException exception) {
                        SurvivalPlus.log(Level.WARNING, "Could not parse day temperature '%s' as a decimal number in 'world.biomes", split[1]);
                    }
                    if (dayMulti == 0) {
                        continue;
                    }
                    double nightMulti = dayMulti;
                    if (split.length > 2) {
                        try {
                            nightMulti = Double.parseDouble(split[2]);
                        } catch (final NumberFormatException exception) {
                            SurvivalPlus.log(Level.WARNING, "Could not parse night temperature '%s' as a decimal number in 'world.biomes", split[2]);
                        }
                    }
                    double random = 0;
                    if (split.length > 3) {
                        try {
                            random = Double.parseDouble(split[3]);
                        } catch (final NumberFormatException e) {
                            SurvivalPlus.log(Level.WARNING, "Could not parse random modifier '%s' as decimal number in 'world.biomes", split[3]);
                        }
                    }
                    manager.addBiomeCondition(biome, dayMulti, nightMulti, random);
                }
            });
        });
    }

    /**
     * Validates if a configuration section exists at the path from parent. If it does exist then it is consumed
     *
     * @param parent   The parent section
     * @param path     The path to child section
     * @param consumer The consumer
     */
    private void consumeSection(final ConfigurationSection parent, final String path, final Consumer<ConfigurationSection> consumer) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            SurvivalPlus.log(Level.WARNING, parent.getCurrentPath() + " is missing a '%s' section!", path);
        }
        consumer.accept(section);
    }

    private <V> Optional<V> wrap(final String option, final Function<String, V> wrapper) {
        final V value = wrapper.apply(option);
        if (value == null) {
            SurvivalPlus.log(Level.WARNING,
                    "Could not find configuration option '%s', please check your config! " +
                            "Using default value for now...", option);
        }
        return Optional.ofNullable(value);
    }

    @Override
    public void cancel() {
        this.configFile.reloadConfig();
    }
}
