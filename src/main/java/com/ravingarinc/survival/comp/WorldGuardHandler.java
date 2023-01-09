package com.ravingarinc.survival.comp;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.api.ModuleLoadException;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldGuardHandler extends Module {
    public static DoubleFlag TEMPERATURE_FLAG = null;
    private final Map<UUID, Double> regionTemperatureMap;
    private final Factory factory;
    private boolean loaded = false;

    public WorldGuardHandler(final SurvivalPlus plugin) {
        super(WorldGuardHandler.class, plugin);

        this.regionTemperatureMap = new ConcurrentHashMap<>();
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            try {
                final DoubleFlag flag = new DoubleFlag("temperature-flag");
                registry.register(flag);
                TEMPERATURE_FLAG = flag;
            } catch (final FlagConflictException e) {
                final Flag<?> existing = registry.get("temperature-flag");
                if (existing instanceof DoubleFlag doubleFlag) {
                    TEMPERATURE_FLAG = doubleFlag;
                }
            }
            this.factory = new Factory(this);
        } else {
            this.factory = null;
        }
    }

    public void add(final UUID uuid, final double temperature) {
        regionTemperatureMap.put(uuid, temperature);
    }

    public void remove(final UUID uuid) {
        regionTemperatureMap.remove(uuid);
    }

    public Optional<Double> getTemperature(final UUID uuid) {
        return Optional.ofNullable(regionTemperatureMap.get(uuid));
    }

    @Override
    public void load() throws ModuleLoadException {
        if (!loaded) {
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                throw new ModuleLoadException(this, "WorldGuard is not enabled!");
            } else {
                final SessionManager manager = WorldGuard.getInstance().getPlatform().getSessionManager();
                manager.registerHandler(factory, null);
                loaded = true;
            }
        }
    }

    @Override
    public void cancel() {
    }


    public static class TemperatureFlagHandler extends FlagValueChangeHandler<Double> {
        private final WorldGuardHandler handler;

        public TemperatureFlagHandler(final Session session, final WorldGuardHandler handler) {
            super(session, TEMPERATURE_FLAG);
            this.handler = handler;
        }

        @Override
        protected void onInitialValue(final LocalPlayer player, final ApplicableRegionSet set, final Double value) {
            if (value != null) {
                handler.add(player.getUniqueId(), value);
            }
        }

        @Override
        protected boolean onSetValue(final LocalPlayer player, final Location from, final Location to, final ApplicableRegionSet toSet, final Double currentValue, final Double lastValue, final MoveType moveType) {
            handler.add(player.getUniqueId(), currentValue);
            return true;
        }

        @Override
        protected boolean onAbsentValue(final LocalPlayer player, final Location from, final Location to, final ApplicableRegionSet toSet, final Double lastValue, final MoveType moveType) {
            handler.remove(player.getUniqueId());
            return true;
        }
    }

    public static class Factory extends Handler.Factory<TemperatureFlagHandler> {
        private final WorldGuardHandler handler;

        public Factory(final WorldGuardHandler handler) {
            this.handler = handler;
        }

        @Override
        public TemperatureFlagHandler create(final Session session) {
            return new TemperatureFlagHandler(session, handler);
        }
    }
}
