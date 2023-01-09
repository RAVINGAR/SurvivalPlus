package com.ravingarinc.survival.command;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Pair;
import com.ravingarinc.survival.character.CharacterManager;
import com.ravingarinc.survival.character.SurvivalPlayer;
import com.ravingarinc.survival.temperature.BiomeCondition;
import com.ravingarinc.survival.temperature.TemperatureManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SurvivalPlusCommand extends BaseCommand {
    private final CharacterManager manager;
    private final SurvivalPlus plugin;

    public SurvivalPlusCommand(final SurvivalPlus plugin) {
        super("survivalplus");
        this.plugin = plugin;
        manager = plugin.getModule(CharacterManager.class);

        addOption("player", 2, (sender, args) -> false)
                .addOption("check", 2, (sender, args) -> {
                    final Player player;
                    if (args.length > 2) {
                        player = plugin.getServer().getPlayer(args[2]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + "Could not find a player called '" + args[2] + "'!");
                            return true;
                        }
                    } else {
                        if (sender instanceof Player) {
                            player = (Player) sender;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Please specify a player name!");
                            return true;
                        }
                    }
                    final SurvivalPlayer survivalPlayer = manager.getPlayer(player);
                    sender.sendMessage(ChatColor.GREEN + "Player '" + player.getName() + "' has a current temperature of " + String.format("%.2f", survivalPlayer.getTemperature()) + " degrees.");
                    return true;
                }).getParent()
                .addOption("add", 3, (sender, args) -> {
                    final Pair<SurvivalPlayer, Double> pair = applyTemperatureOption(sender, args);
                    if (pair != null) {
                        final SurvivalPlayer player = pair.getLeft();
                        player.setTemperature(player.getTemperature() + pair.getRight());
                        sender.sendMessage(ChatColor.GREEN + player.getPlayer().getName() + "'s new temperature is now " + String.format("%.2f", player.getTemperature()) + " degrees.");
                    }
                    return true;
                }).buildTabCompletions((sender, args) -> {
                    if (args.length == 3) {
                        final List<String> tabs = new ArrayList<>();
                        tabs.add("<number>");
                        return tabs;
                    }
                    return null;
                }).getParent()
                .addOption("remove", 3, (sender, args) -> {
                    final Pair<SurvivalPlayer, Double> pair = applyTemperatureOption(sender, args);
                    if (pair != null) {
                        final SurvivalPlayer player = pair.getLeft();
                        player.setTemperature(player.getTemperature() - pair.getRight());
                        sender.sendMessage(ChatColor.GREEN + player.getPlayer().getName() + "'s new temperature is now " + String.format("%.2f", player.getTemperature()) + " degrees.");
                    }
                    return true;
                }).buildTabCompletions((sender, args) -> {
                    if (args.length == 3) {
                        final List<String> tabs = new ArrayList<>();
                        tabs.add("<number>");
                        return tabs;
                    }
                    return null;
                }).getParent()
                .addOption("set", 3, (sender, args) -> {
                    final Pair<SurvivalPlayer, Double> pair = applyTemperatureOption(sender, args);
                    if (pair != null) {
                        final SurvivalPlayer player = pair.getLeft();
                        player.setTemperature(pair.getRight());
                        sender.sendMessage(ChatColor.GREEN + player.getPlayer().getName() + "'s new temperature is now " + String.format("%.2f", player.getTemperature()) + " degrees.");
                    }
                    return true;
                }).buildTabCompletions((sender, args) -> {
                    if (args.length == 3) {
                        final List<String> tabs = new ArrayList<>();
                        tabs.add("<number>");
                        return tabs;
                    }
                    return null;
                });

        addOption("environment", 2, (sender, args) -> false)
                .addOption("get", 3, (sender, args) -> {
                    if (sender instanceof Player player) {
                        final Biome biome;
                        try {
                            biome = Biome.valueOf(args[2].toUpperCase());
                        } catch (final IllegalArgumentException e) {
                            sender.sendMessage(ChatColor.RED + "Unknown biome type called '" + args[2] + '.');
                            return true;
                        }
                        final BiomeCondition condition = plugin.getModule(TemperatureManager.class).getBiomeCondition(biome);
                        if (condition == null) {
                            sender.sendMessage(ChatColor.RED + "The biome " + biome.getKey().getKey() + " is not used by this plugin!");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Biome " + ChatColor.GRAY + "| " + biome.getKey().getKey());
                        sender.sendMessage(ChatColor.GREEN + "Current Temperature " + ChatColor.GRAY + "| " + String.format("%.2f", condition.getTemperature(player.getLocation().getWorld().getName())));
                    } else {
                        sender.sendMessage(ChatColor.RED + "This command can only be used by a player!");
                    }
                    return true;
                }).buildTabCompletions((sender, args) -> {
                    if (args.length == 3) {
                        return plugin.getModule(TemperatureManager.class).getFormattedBiomeList();
                    }
                    return null;
                }).getParent()
                .addOption("check", 2, (sender, args) -> {
                    final Player player;
                    if (args.length > 2) {
                        player = plugin.getServer().getPlayer(args[2]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + "Could not find player with that name!");
                            return true;
                        }
                    } else if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please specify a player name!");
                        return true;
                    }
                    final Double temperature = plugin.getModule(TemperatureManager.class).getEnvironmentTemperatureForPlayer(player);
                    if (temperature == null) {
                        sender.sendMessage(ChatColor.RED + "Something went wrong!");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Environment Temperature for '" + player.getName() + "' " + ChatColor.GRAY + "| " + String.format("%.2f", temperature));
                    return true;
                });

        addOption("reload", 1, (sender, args) -> {
            plugin.reload();
            sender.sendMessage(ChatColor.GRAY + "SurvivalPlus has been reloaded!");
            return true;
        });

        addOption("disable", 1, (sender, args) -> {
            plugin.cancel();
            sender.sendMessage(ChatColor.GRAY + "SurvivalPlus has been disabled! Type /survivalplus reload to re-enable!");
            return true;
        });
    }

    public Pair<SurvivalPlayer, Double> applyTemperatureOption(final CommandSender sender, final String[] args) {
        if (args.length > 2) {
            final Player player;
            final double number;
            try {
                number = Double.parseDouble(args[2]);
            } catch (final NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Please specify a valid number!");
                return null;
            }

            if (args.length > 3) {
                player = plugin.getServer().getPlayer(args[3]);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "Could not find player with that name!");
                    return null;
                }
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                sender.sendMessage(ChatColor.RED + "Please specify a player name!");
                return null;
            }
            return new Pair<>(manager.getPlayer(player), number);
        } else {
            sender.sendMessage(ChatColor.RED + "Not enough arguments!");
        }
        return null;
    }
}
