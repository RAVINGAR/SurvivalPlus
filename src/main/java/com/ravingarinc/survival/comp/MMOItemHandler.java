package com.ravingarinc.survival.comp;

import com.ravingarinc.survival.SurvivalPlus;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.StatMap;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.StatManager;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;

import java.util.UUID;

public class MMOItemHandler {

    public static DoubleStat COLD_RESISTANCE = null;
    public static DoubleStat HEAT_RESISTANCE = null;
    public static DoubleStat TEMPERATURE_RESISTANCE = null;
    public static double maximumResistance = 20;
    private static boolean isEnabled = false;

    public static void enable(final SurvivalPlus plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("MMOItems") != null) {
            COLD_RESISTANCE = new DoubleStat("COLD_RESISTANCE", Material.BLUE_ICE, "Cold Resistance", new String[]{"Increases resistance against cold temperatures."});
            HEAT_RESISTANCE = new DoubleStat("HEAT_RESISTANCE", Material.MAGMA_CREAM, "Heat Resistance", new String[]{"Increases resistance against hot temperatures."});
            TEMPERATURE_RESISTANCE = new DoubleStat("TEMPERATURE_RESISTANCE", Material.GLOWSTONE_DUST, "Temperature Resistance", new String[]{"Increases resistance against all temperatures."});

            registerStats();
            isEnabled = true;
        }
    }

    public static void registerStats() {
        final StatManager manager = MMOItems.plugin.getStats();

        manager.register(COLD_RESISTANCE);
        manager.register(HEAT_RESISTANCE);
        manager.register(TEMPERATURE_RESISTANCE);
    }

    /**
     * Gets the modifier based on a player's heat or cold resistance. This adds on the generic temperature resistance as well
     *
     * @param stat
     * @param uuid
     * @return
     */
    public static double getModifier(final DoubleStat stat, final UUID uuid) {
        if (isEnabled) {
            final StatMap stats = MMOPlayerData.get(uuid).getStatMap();
            final double resistance = stats.getStat(stat.getId()) + stats.getStat(TEMPERATURE_RESISTANCE.getId());
            return (maximumResistance - Math.max(Math.min(resistance, maximumResistance), -maximumResistance)) / maximumResistance;
        } else {
            return 1;
        }
    }
}
