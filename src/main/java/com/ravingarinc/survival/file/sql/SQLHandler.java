package com.ravingarinc.survival.file.sql;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.api.Module;
import com.ravingarinc.survival.api.ModuleLoadException;
import com.ravingarinc.survival.character.SurvivalPlayer;
import com.ravingarinc.survival.file.Settings;
import com.ravingarinc.survival.temperature.TemperatureManager;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class SQLHandler extends Module {
    private final String DATABASE_PATH;
    private final String URL;

    private Settings settings;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(SurvivalPlus, Class)}.
     * Implementations of Managers should have one public constructor with a JavaPlugin object parameter.
     * The implementing constructor CANNOT call {@link SurvivalPlus#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param plugin The owning plugin
     */
    public SQLHandler(final SurvivalPlus plugin) {
        super(SQLHandler.class, plugin);
        DATABASE_PATH = plugin.getDataFolder() + "/players.db";
        URL = "jdbc:sqlite:" + DATABASE_PATH;
    }

    @Override
    protected void load() throws ModuleLoadException {
        final TemperatureManager temperatureManager = plugin.getModule(TemperatureManager.class);
        this.settings = temperatureManager.getSettings();

        final File file = new File(DATABASE_PATH);
        if (!file.exists()) {
            try (Connection connection = DriverManager.getConnection(URL)) {
                SurvivalPlus.log(Level.INFO, "Created new database with driver '%s'!", connection.getMetaData().getDriverName());
            } catch (final SQLException exception) {
                throw new ModuleLoadException(this, exception);
            }
            if (!execute(Schema.createTable)) {
                throw new ModuleLoadException(this, ModuleLoadException.Reason.SQL);
            }
        }
    }

    @Override
    public void cancel() {

    }

    public Optional<SurvivalPlayer> loadPlayer(final UUID uuid, final Player player) {
        return query(Schema.Player.select, (statement) -> {
            try {
                statement.setString(1, uuid.toString());
            } catch (final SQLException exception) {
                SurvivalPlus.log(Level.SEVERE, "Encountered issue preparing statement!", exception);
            }
        }, (result) -> {
            try {
                final SurvivalPlayer sPlayer = new SurvivalPlayer(uuid, player, settings);
                if (result.next()) {
                    SurvivalPlus.logIfDebug(() -> "Found player, loading now...");
                    sPlayer.setTemperature(result.getDouble(Schema.Player.TEMPERATURE));
                } else {
                    SurvivalPlus.logIfDebug(() -> "Could not find player, creating new player");
                    final double temperature = settings.defaultTemperature;
                    sPlayer.setTemperature(temperature);

                    prepareStatement(Schema.Player.insert, (statement) -> {
                        try {
                            statement.setString(1, uuid.toString());
                            statement.setDouble(2, temperature);
                        } catch (final SQLException e) {
                            SurvivalPlus.log(Level.SEVERE, "Encountered issue inserting new player data to database!", e);
                        }
                    });
                }
                return sPlayer;
            } catch (final SQLException e) {
                SurvivalPlus.log(Level.SEVERE, "Encountered issue reading player data from database!", e);
            }
            return null;
        });
    }

    /**
     * This may be executed async or sync.
     *
     * @param player The player
     */
    public void savePlayer(final SurvivalPlayer player) {
        // This method operates under the intention that a player is already in the database, since if it was loaded
        // it must be in the database
        prepareStatement(Schema.Player.update, (statement) -> {
            try {
                statement.setDouble(1, player.getTemperature());
                statement.setString(2, player.getUUID().toString());
            } catch (final SQLException e) {
                SurvivalPlus.log(Level.SEVERE, "Encountered issue updating player data in database!", e);
            }
        });
    }

    /**
     * Prepare a statement for the database and consume it. This can be used for insert/update of data
     *
     * @param request  The request
     * @param consumer The consumer to apply to the statement
     */
    private void prepareStatement(final String request, final Consumer<PreparedStatement> consumer) {
        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(request)) {
            consumer.accept(statement);
            statement.executeUpdate();
        } catch (final SQLException exception) {
            SurvivalPlus.log(Level.SEVERE, "Encountered issue inserting data into database!", exception);
        }
    }

    /**
     * Execute a query on the database.
     *
     * @param execution The query
     * @return TRUE if successful, FALSE is an exception occurred.
     */
    public boolean execute(final String execution) {
        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {
            statement.execute(execution);
            return true;
        } catch (final SQLException e) {
            SurvivalPlus.log(Level.SEVERE, "Encountered issue executing statement to database!", e);
            return false;
        }
    }

    public <T> Optional<T> query(final String query, final Consumer<PreparedStatement> consumer, final Function<ResultSet, T> cursor) {
        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(query)) {
            consumer.accept(statement);
            try (ResultSet result = statement.executeQuery()) {
                return Optional.ofNullable(cursor.apply(result));
            }
        } catch (final SQLException e) {
            SurvivalPlus.log(Level.SEVERE, "Encountered issue querying statement to database!", e);
        }
        return Optional.empty();
    }
}
