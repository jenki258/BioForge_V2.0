package net.jenkimods.bioforge.infection.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.stream.Collectors;

public class InfectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bioforge")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("infect")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("infected", BoolArgumentType.bool())
                                        .executes(ctx -> execute(ctx.getSource(),
                                                toLiving(EntityArgument.getEntities(ctx, "targets")),
                                                BoolArgumentType.getBool(ctx, "infected"),
                                                PathogenType.UNIVERSAL, InfectionType.CONTACT_BASED, false))
                                        .then(Commands.argument("pathogen", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (PathogenType pt : PathogenType.values())
                                                        builder.suggest(pt.name().toLowerCase());
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> execute(ctx.getSource(),
                                                        toLiving(EntityArgument.getEntities(ctx, "targets")),
                                                        BoolArgumentType.getBool(ctx, "infected"),
                                                        PathogenType.fromName(StringArgumentType.getString(ctx, "pathogen")),
                                                        InfectionType.CONTACT_BASED, false))
                                                .then(Commands.argument("infectionType", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            PathogenType pt = PathogenType.fromName(
                                                                    StringArgumentType.getString(ctx, "pathogen"));
                                                            for (InfectionType it : pt.getAllowedTransmissions())
                                                                builder.suggest(it.name().toLowerCase());
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> execute(ctx.getSource(),
                                                                toLiving(EntityArgument.getEntities(ctx, "targets")),
                                                                BoolArgumentType.getBool(ctx, "infected"),
                                                                PathogenType.fromName(StringArgumentType.getString(ctx, "pathogen")),
                                                                InfectionType.fromName(StringArgumentType.getString(ctx, "infectionType")),
                                                                false))
                                                        .then(Commands.argument("persistent", BoolArgumentType.bool())
                                                                .executes(ctx -> execute(ctx.getSource(),
                                                                        toLiving(EntityArgument.getEntities(ctx, "targets")),
                                                                        BoolArgumentType.getBool(ctx, "infected"),
                                                                        PathogenType.fromName(StringArgumentType.getString(ctx, "pathogen")),
                                                                        InfectionType.fromName(StringArgumentType.getString(ctx, "infectionType")),
                                                                        BoolArgumentType.getBool(ctx, "persistent")))
                                                                .then(Commands.literal("symptoms")
                                                                        .then(Commands.argument("heartRate", StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> {
                                                                                    for (HeartRate hr : HeartRate.values())
                                                                                        builder.suggest(hr.name().toLowerCase());
                                                                                    return builder.buildFuture();
                                                                                })
                                                                                .then(Commands.argument("lungSound", StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                            for (LungSound ls : LungSound.values())
                                                                                                builder.suggest(ls.name().toLowerCase());
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .then(Commands.argument("tempPlus", BoolArgumentType.bool())
                                                                                                .then(Commands.argument("tempMinus", BoolArgumentType.bool())
                                                                                                        .then(Commands.argument("redness", FloatArgumentType.floatArg(0, 1))
                                                                                                                .then(Commands.argument("lesions", FloatArgumentType.floatArg(0, 1))
                                                                                                                        .then(Commands.argument("secretion", FloatArgumentType.floatArg(0, 1))
                                                                                                                                .then(Commands.argument("swelling", FloatArgumentType.floatArg(0, 1))
                                                                                                                                        .then(Commands.argument("reflexDelay", FloatArgumentType.floatArg(0, 2))
                                                                                                                                                .then(Commands.argument("reflexStrength", FloatArgumentType.floatArg(0, 1))
                                                                                                                                                        .then(Commands.argument("neuralDamage", FloatArgumentType.floatArg(0, 1))
                                                                                                                                                                .then(Commands.argument("oxygenSaturation", FloatArgumentType.floatArg(0, 1))
                                                                                                                                                                        .then(Commands.argument("perfusionIndex", FloatArgumentType.floatArg(0, 1))
                                                                                                                                                                                .then(Commands.argument("infectionStrength", FloatArgumentType.floatArg(0, 1))
                                                                                                                                                                                        .executes(ctx -> executeWithSymptoms(
                                                                                                                                                                                                ctx.getSource(),
                                                                                                                                                                                                toLiving(EntityArgument.getEntities(ctx, "targets")),
                                                                                                                                                                                                BoolArgumentType.getBool(ctx, "infected"),
                                                                                                                                                                                                PathogenType.fromName(StringArgumentType.getString(ctx, "pathogen")),
                                                                                                                                                                                                InfectionType.fromName(StringArgumentType.getString(ctx, "infectionType")),
                                                                                                                                                                                                BoolArgumentType.getBool(ctx, "persistent"),
                                                                                                                                                                                                HeartRate.fromName(StringArgumentType.getString(ctx, "heartRate")),
                                                                                                                                                                                                LungSound.fromName(StringArgumentType.getString(ctx, "lungSound")),
                                                                                                                                                                                                BoolArgumentType.getBool(ctx, "tempPlus"),
                                                                                                                                                                                                BoolArgumentType.getBool(ctx, "tempMinus"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "redness"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "lesions"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "secretion"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "swelling"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "reflexDelay"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "reflexStrength"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "neuralDamage"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "oxygenSaturation"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "perfusionIndex"),
                                                                                                                                                                                                FloatArgumentType.getFloat(ctx, "infectionStrength")
                                                                                                                                                                                        ))
                                                                                                                                                                                )
                                                                                                                                                                        )
                                                                                                                                                                )
                                                                                                                                                        )
                                                                                                                                                )
                                                                                                                                        )
                                                                                                                                )
                                                                                                                        )
                                                                                                                )
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("cure")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> cure(ctx.getSource(),
                                        toLiving(EntityArgument.getEntities(ctx, "targets")))))));
    }

    private static Collection<LivingEntity> toLiving(Collection<? extends Entity> entities) {
        return entities.stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .collect(Collectors.toList());
    }

    private static int execute(CommandSourceStack source, Collection<LivingEntity> targets,
                               boolean infected, PathogenType pathogenType,
                               InfectionType infectionType, boolean persistent) {
        if (!pathogenType.allows(infectionType)) {
            source.sendFailure(Component.translatable("command.bioforge.infect.incompatible",
                    pathogenType.name(), infectionType.name()));
            return 0;
        }
        for (LivingEntity entity : targets) {
            InfectionData data = InfectionCapability.get(entity);
            if (data == null) continue;
            data.clearInfection();
            if (entity instanceof ServerPlayer player) {
                InfectionStore.get(player.serverLevel()).clearInfection(player.getUUID());
            }
            if (infected) {
                data.setInfected(true);
                data.setPathogenType(pathogenType);
                data.setInfectionType(infectionType);
                InfectionEventHandler.applyDefaultSymptoms(data);
                if (persistent && entity instanceof ServerPlayer player) {
                    InfectionStore.get(player.serverLevel()).setInfection(player.getUUID(),
                            new InfectionStore.InfectionRecord(
                                    true, true, pathogenType, infectionType,
                                    data.getSymptom(BioForgeSymptoms.HEART_RATE),
                                    data.getSymptom(BioForgeSymptoms.LUNG_SOUND),
                                    data.getSymptom(BioForgeSymptoms.TEMPERATURE_PLUS),
                                    data.getSymptom(BioForgeSymptoms.TEMPERATURE_MINUS),
                                    data.getSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS),
                                    data.getSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS),
                                    data.getSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION),
                                    data.getSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING),
                                    data.getSymptom(BioForgeSymptoms.REFLEX_DELAY),
                                    data.getSymptom(BioForgeSymptoms.REFLEX_STRENGTH),
                                    data.getSymptom(BioForgeSymptoms.NEURAL_DAMAGE),
                                    data.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION),
                                    data.getSymptom(BioForgeSymptoms.PERFUSION_INDEX),
                                    data.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH)
                            ));
                }
                final String name = entity.getDisplayName().getString();
                source.sendSuccess(() -> Component.translatable("command.bioforge.infect.success",
                        name, pathogenType.name(), infectionType.name()), true);
            } else {
                final String name = entity.getDisplayName().getString();
                source.sendSuccess(() -> Component.translatable("command.bioforge.infect.cleared",
                        name), true);
            }
            if (entity instanceof ServerPlayer player) {
                InfectionEventHandler.syncToClient(player, data);
            }
        }
        return targets.size();
    }

    private static int executeWithSymptoms(CommandSourceStack source, Collection<LivingEntity> targets,
                                           boolean infected, PathogenType pathogenType,
                                           InfectionType infectionType, boolean persistent,
                                           HeartRate heartRate, LungSound lungSound,
                                           boolean tempPlus, boolean tempMinus,
                                           float redness, float lesions, float secretion, float swelling,
                                           float reflexDelay, float reflexStrength, float neuralDamage,
                                           float oxygenSaturation, float perfusionIndex,
                                           float infectionStrength) {
        if (!pathogenType.allows(infectionType)) {
            source.sendFailure(Component.translatable("command.bioforge.infect.incompatible",
                    pathogenType.name(), infectionType.name()));
            return 0;
        }
        for (LivingEntity entity : targets) {
            InfectionData data = InfectionCapability.get(entity);
            if (data == null) continue;
            data.clearInfection();
            if (entity instanceof ServerPlayer player) {
                InfectionStore.get(player.serverLevel()).clearInfection(player.getUUID());
            }
            if (infected) {
                data.setInfected(true);
                data.setPathogenType(pathogenType);
                data.setInfectionType(infectionType);
                data.setSymptom(BioForgeSymptoms.HEART_RATE, heartRate);
                data.setSymptom(BioForgeSymptoms.LUNG_SOUND, lungSound);
                data.setSymptom(BioForgeSymptoms.TEMPERATURE_PLUS, tempPlus);
                data.setSymptom(BioForgeSymptoms.TEMPERATURE_MINUS, tempMinus);
                data.setSymptom(BioForgeSymptoms.OTOSCOPE_REDNESS, redness);
                data.setSymptom(BioForgeSymptoms.OTOSCOPE_LESIONS, lesions);
                data.setSymptom(BioForgeSymptoms.OTOSCOPE_SECRETION, secretion);
                data.setSymptom(BioForgeSymptoms.OTOSCOPE_SWELLING, swelling);
                data.setSymptom(BioForgeSymptoms.REFLEX_DELAY, reflexDelay);
                data.setSymptom(BioForgeSymptoms.REFLEX_STRENGTH, reflexStrength);
                data.setSymptom(BioForgeSymptoms.NEURAL_DAMAGE, neuralDamage);
                data.setSymptom(BioForgeSymptoms.OXYGEN_SATURATION, oxygenSaturation);
                data.setSymptom(BioForgeSymptoms.PERFUSION_INDEX, perfusionIndex);
                data.setSymptom(BioForgeSymptoms.INFECTION_STRENGTH, infectionStrength);
                if (persistent && entity instanceof ServerPlayer player) {
                    InfectionStore.get(player.serverLevel()).setInfection(player.getUUID(),
                            new InfectionStore.InfectionRecord(
                                    true, true, pathogenType, infectionType,
                                    heartRate, lungSound, tempPlus, tempMinus,
                                    redness, lesions, secretion, swelling,
                                    reflexDelay, reflexStrength, neuralDamage,
                                    oxygenSaturation, perfusionIndex,
                                    infectionStrength));
                }
                final String name = entity.getDisplayName().getString();
                source.sendSuccess(() -> Component.translatable("command.bioforge.infect.success",
                        name, pathogenType.name(), infectionType.name()), true);
            } else {
                final String name = entity.getDisplayName().getString();
                source.sendSuccess(() -> Component.translatable("command.bioforge.infect.cleared",
                        name), true);
            }
            if (entity instanceof ServerPlayer player) {
                InfectionEventHandler.syncToClient(player, data);
            }
        }
        return targets.size();
    }

    private static int cure(CommandSourceStack source, Collection<LivingEntity> targets) {
        for (LivingEntity entity : targets) {
            InfectionData data = InfectionCapability.get(entity);
            if (data == null) continue;
            data.clearInfection();
            if (entity instanceof ServerPlayer player) {
                InfectionStore.get(player.serverLevel()).clearInfection(player.getUUID());
            }
            final String name = entity.getDisplayName().getString();
            source.sendSuccess(() -> Component.translatable("command.bioforge.cure.success", name), true);
            if (entity instanceof ServerPlayer player) {
                InfectionEventHandler.syncToClient(player, data);
            }
        }
        return targets.size();
    }
}