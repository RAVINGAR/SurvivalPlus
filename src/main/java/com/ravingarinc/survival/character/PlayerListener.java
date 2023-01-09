package com.ravingarinc.survival.character;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.ModuleListener;
import com.ravingarinc.survival.api.ModuleLoadException;
import com.ravingarinc.survival.file.ConfigManager;
import com.ravingarinc.survival.temperature.TemperatureDeathBehaviour;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlayerListener extends ModuleListener {
    private final Map<EntityDamageEvent.DamageCause, Consumer<SurvivalPlayer>> applicableHeatCauses;
    public TemperatureDeathBehaviour deathBehaviour = TemperatureDeathBehaviour.KEEP;
    public double respawnTemperature = 25;
    public double onFireAmount = 20;
    public double inFireAmount = 25;
    public double inLavaAmount = 35;
    private CharacterManager manager;

    public PlayerListener(final SurvivalPlus plugin) {
        super(PlayerListener.class, plugin, CharacterManager.class, ConfigManager.class);
        applicableHeatCauses = new HashMap<>();
    }

    @Override
    public void load() throws ModuleLoadException {
        this.manager = plugin.getModule(CharacterManager.class);

        applicableHeatCauses.put(EntityDamageEvent.DamageCause.FIRE, (player) -> player.addTemperature(inFireAmount));
        applicableHeatCauses.put(EntityDamageEvent.DamageCause.FIRE_TICK, (player) -> player.addTemperature(onFireAmount));
        applicableHeatCauses.put(EntityDamageEvent.DamageCause.LAVA, (player) -> player.addTemperature(inLavaAmount));

        super.load();
    }

    @Override
    public void cancel() {
        super.cancel();
        applicableHeatCauses.clear();
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.manager.loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent event) {
        this.manager.unloadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (deathBehaviour == TemperatureDeathBehaviour.RESET) {
            final SurvivalPlayer player = this.manager.getPlayer(event.getEntity());
            player.setTemperature(respawnTemperature);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) (event.getEntity());
            final Consumer<SurvivalPlayer> consumer = applicableHeatCauses.get(event.getCause());
            if (consumer != null) {
                consumer.accept(manager.getPlayer(player));
            }
        }
    }
}
