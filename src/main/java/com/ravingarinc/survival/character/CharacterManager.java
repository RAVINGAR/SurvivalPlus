package com.ravingarinc.survival.character;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.file.sql.SQLHandler;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CharacterManager extends Module {
    private final Map<UUID, SurvivalPlayer> playerMap;

    private SQLHandler sqlHandler;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(SurvivalPlus, Class)}.
     * Implementations of Managers should have one public constructor with a JavaPlugin object parameter.
     * The implementing constructor CANNOT call {@link SurvivalPlus#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param plugin The owning plugin
     */
    public CharacterManager(final SurvivalPlus plugin) {
        super(CharacterManager.class, plugin, SQLHandler.class);
        this.playerMap = new ConcurrentHashMap<>();
    }

    /**
     * Get's the SurvivalPlayer from this Manager. If the player has not been loaded then it will be loaded.
     * The returned value will only be null if the player has not been loaded yet or if an error occurs.
     *
     * @param player The player
     * @return The returned player
     */
    public SurvivalPlayer getPlayer(final Player player) {
        return playerMap.get(player.getUniqueId());
    }

    public void loadPlayer(final Player player) {
        this.sqlHandler.loadPlayer(player.getUniqueId(), player).ifPresent(sP -> playerMap.put(sP.getUUID(), sP));
    }

    public void unloadPlayer(final Player player) {
        this.sqlHandler.savePlayer(getPlayer(player));
        this.playerMap.remove(player.getUniqueId());
    }

    public Collection<SurvivalPlayer> getAllPlayers() {
        return Collections.unmodifiableCollection(playerMap.values());
    }

    @Override
    protected void load() {
        this.sqlHandler = plugin.getModule(SQLHandler.class);

        for (final Player player : plugin.getServer().getOnlinePlayers()) {
            sqlHandler.loadPlayer(player.getUniqueId(), player).ifPresent(sP -> playerMap.put(sP.getUUID(), sP));
        }
    }

    @Override
    public void cancel() {
        playerMap.values().forEach(player -> sqlHandler.savePlayer(player));
        playerMap.clear();
    }
}
