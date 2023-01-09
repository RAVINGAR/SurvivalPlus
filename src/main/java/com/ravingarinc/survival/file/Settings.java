package com.ravingarinc.survival.file;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    public long checkInterval = 100;
    public double minTemperature = 0;
    public double defaultTemperature = 25;

    public double maxTemperature = 60;

    public double baseBiomeTemperature = 30;

    public double calculationScale = 1.0;

    public int blockRange = 6;

    public double underGroundMultiplier = 0.5;

    public double inWater = -20;

    public double raining = 0.7;
    public double thunderStorm = 0.5;

    public List<SkillArg> mythicSkillArgs = new ArrayList<>();

}
