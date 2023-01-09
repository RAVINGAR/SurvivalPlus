package com.ravingarinc.survival.character;

import com.google.common.util.concurrent.AtomicDouble;
import com.ravingarinc.survival.comp.MMOItemHandler;
import com.ravingarinc.survival.file.Settings;
import com.ravingarinc.survival.temperature.applier.Applyable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SurvivalPlayer implements Applyable {
    private final Player player;
    private final UUID uuid;

    private final AtomicDouble temperature;

    private final double minTemp;
    private final double maxTemp;
    private final double scale;

    public SurvivalPlayer(final UUID uuid, final Player player, final Settings settings) {
        this.player = player;
        this.uuid = uuid;
        this.temperature = new AtomicDouble();
        this.minTemp = settings.minTemperature;
        this.maxTemp = settings.maxTemperature;
        this.scale = settings.calculationScale;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Player getPlayer() {
        return player;
    }

    public double getTemperature() {
        return this.temperature.get();
    }

    /**
     * Forcibly set the player's temperature to a specific value.
     *
     * @param temperature The new temperature
     */
    public void setTemperature(final double temperature) {
        this.temperature.set(Math.max(Math.min(temperature, maxTemp), minTemp));
    }

    @Override
    public double apply(@Nullable final SurvivalPlayer player, final double envTemp) {
        // In this case, the previous temperature is the current environmental temperature
        final double temp = this.getTemperature();

        // The denominator will range from minimum of 1000 to maximum of 3000
        double changeInTemp = (Math.pow(envTemp - temp, 3) / (Math.max(Math.min(scale * 4000, scale * 100 * temp), scale * 500)));
        if (changeInTemp < 0) {
            // temperature decreasing
            changeInTemp *= MMOItemHandler.getModifier(MMOItemHandler.COLD_RESISTANCE, uuid);
        } else if (changeInTemp > 0) {
            // temperature increasing
            changeInTemp *= MMOItemHandler.getModifier(MMOItemHandler.HEAT_RESISTANCE, uuid);
        }
        return temp + changeInTemp;
    }

    /**
     * Used mainly for applying instant calculations. The temperature is added to the player's current temperature and
     * then is applied through normal environmental calculations.
     *
     * @param temperature The temperature
     */
    public void addTemperature(final double temperature) {
        final double envTemp = this.getTemperature() + temperature;
        setTemperature(apply(this, envTemp));
    }
}
