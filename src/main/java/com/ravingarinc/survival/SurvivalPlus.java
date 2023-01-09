package com.ravingarinc.survival;

import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.api.ModuleLoadException;
import com.ravingarinc.survival.character.CharacterManager;
import com.ravingarinc.survival.character.PlayerListener;
import com.ravingarinc.survival.command.SurvivalPlusCommand;
import com.ravingarinc.survival.comp.MMOItemHandler;
import com.ravingarinc.survival.comp.PlaceholderAPIHandler;
import com.ravingarinc.survival.comp.WorldGuardHandler;
import com.ravingarinc.survival.file.ConfigManager;
import com.ravingarinc.survival.file.sql.SQLHandler;
import com.ravingarinc.survival.temperature.TemperatureManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SurvivalPlus extends JavaPlugin {
    public static boolean debug;
    private static Logger logger;
    private Map<Class<? extends Module>, Module> modules;

    /**
     * Expects a message where %s will be replaced by the provided terms
     *
     * @param level        The log level
     * @param message      The message
     * @param replacements The replacements
     */
    public static void log(final Level level, final String message, final Object... replacements) {
        log(level, message, null, replacements);
    }

    public static void log(final Level level, final String message, @Nullable final Throwable throwable, final Object... replacements) {
        String format = message;
        for (final Object replacement : replacements) {
            format = format.replaceFirst("%s", replacement.toString());
        }
        if (throwable == null) {
            logger.log(level, format);
        } else {
            logger.log(level, format, throwable);
        }
    }

    public static void log(final Level level, final String message, final Throwable throwable) {
        logger.log(level, message, throwable);
    }

    public static void logIfDebug(final Supplier<String> message, final Object... replacements) {
        if (debug) {
            log(Level.WARNING, message.get(), replacements);
        }
    }

    public static void runIfDebug(final Runnable runnable) {
        if (debug) {
            runnable.run();
        }
    }

    @Override
    public void onLoad() {
        logger = this.getLogger();
        modules = new LinkedHashMap<>();

        MMOItemHandler.enable(this);

        addModule(WorldGuardHandler.class);
        addModule(PlaceholderAPIHandler.class);
    }

    @Override
    public void onEnable() {
        // add managers
        addModule(ConfigManager.class);
        addModule(SQLHandler.class);
        addModule(CharacterManager.class);
        addModule(TemperatureManager.class);

        // add listeners
        addModule(PlayerListener.class);

        load();

        new SurvivalPlusCommand(this).register(this);
    }

    public void load() {
        modules.values().forEach(manager -> {
            manager.setLoaded(false);
            try {
                manager.initialise();
            } catch (final ModuleLoadException e) {
                SurvivalPlus.log(Level.SEVERE, e.getMessage());
            }
        });
        int loaded = 0;
        for (final Module module : modules.values()) {
            if (module.isLoaded()) {
                loaded++;
            }
        }
        if (loaded > 1) {
            if (loaded == modules.size()) {
                SurvivalPlus.log(Level.INFO, "%s has been enabled successfully!", getName());
            } else {
                SurvivalPlus.log(Level.INFO, "%s has been partially enabled successfully!", getName());
                SurvivalPlus.log(Level.WARNING, "%s module/s have failed to load!", (modules.size() - loaded));
            }
        } else {
            SurvivalPlus.log(Level.INFO, "No modules were loaded succesfully! %s will now shutdown...", getName());
            this.onDisable();
        }
    }

    public void reload() {
        cancel();
        load();
    }

    public void cancel() {
        final List<Module> reverseOrder = new ArrayList<>(modules.values());
        Collections.reverse(reverseOrder);
        reverseOrder.forEach(module -> {
            if (module.isLoaded()) {
                try {
                    module.cancel();
                } catch (final Exception e) {
                    SurvivalPlus.log(Level.SEVERE, "Encountered issue shutting down module '%s'!", e, module.getName());
                }
            }
        });
    }

    private <T extends Module> void addModule(final Class<T> module) {
        final Optional<? extends Module> opt = Module.initialise(this, module);
        opt.ifPresent(t -> modules.put(module, t));
    }

    /**
     * Get the manager of the specified type otherwise an IllegalArgumentException is thrown.
     *
     * @param type The manager type
     * @param <T>  The type
     * @return The manager
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(final Class<T> type) {
        final Module module = modules.get(type);
        if (module == null) {
            throw new IllegalArgumentException("Could not find module of type " + type.getName() + ". Contact developer! Most likely JavaPlugin.getManager() has been called from a Module's constructor!");
        }
        return (T) module;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void onDisable() {
        cancel();
        this.getServer().getScheduler().cancelTasks(this);

        log(Level.INFO, getName() + " is disabled.");
    }
}
