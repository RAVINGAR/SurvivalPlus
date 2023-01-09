package com.ravingarinc.survival.temperature.applier;

import com.ravingarinc.survival.character.SurvivalPlayer;

public interface Applyable {

    double apply(SurvivalPlayer player, double previousTemperature);
}
