package com.ravingarinc.survival.temperature.applier;

import com.ravingarinc.survival.SurvivalPlus;
import com.ravingarinc.survival.character.SurvivalPlayer;
import com.ravingarinc.survival.file.SkillArg;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.SkillExecutor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

public class MythicEffectApplier implements Applyable {
    private final BukkitAPIHelper helper;

    private final List<SkillArg> skillArgs;

    private final Random random;

    public MythicEffectApplier(final List<SkillArg> args) {
        this.random = new Random(System.currentTimeMillis());
        this.helper = MythicBukkit.inst().getAPIHelper();
        this.skillArgs = new ArrayList<>();

        final SkillExecutor executor = MythicBukkit.inst().getSkillManager();
        args.forEach(arg -> {
            final Optional<Skill> optSkill = executor.getSkill(arg.id());
            if (optSkill.isPresent()) {
                skillArgs.add(arg);
            } else {
                SurvivalPlus.log(Level.WARNING, "Could not find mythic skill called '%s' specified in config.yml/temperature-effects.values!", arg.id());
            }
        });
    }

    @Override
    public double apply(final SurvivalPlayer player, final double previousTemperature) {
        final Player bukkitPlayer = player.getPlayer();
        final Collection<Entity> entity = new HashSet<>();
        entity.add(bukkitPlayer);
        final Collection<Location> location = new HashSet<>();
        location.add(bukkitPlayer.getLocation());
        skillArgs.forEach((arg) -> {
            if (previousTemperature >= arg.lower() && previousTemperature < arg.upper()) {
                if (arg.percent() >= 100 || arg.percent() >= random.nextInt(100)) {
                    helper.castSkill(bukkitPlayer, arg.id(), bukkitPlayer.getLocation(), entity, location, 1);
                }
            }
        });

        return previousTemperature;
    }
}
