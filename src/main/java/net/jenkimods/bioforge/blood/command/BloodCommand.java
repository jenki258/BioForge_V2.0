package net.jenkimods.bioforge.blood.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.blood.BloodCapability;
import net.jenkimods.bioforge.blood.BloodData;
import net.jenkimods.bioforge.blood.BloodEventHandler;
import net.jenkimods.bioforge.blood.BloodType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class BloodCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("bioforge")
                        .then(Commands.literal("get_blood")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(ctx -> executeGet(ctx,
                                                EntityArgument.getEntity(ctx, "entity")))))
                        .then(Commands.literal("set_blood")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("amount")
                                        .then(Commands.argument("entities", EntityArgument.entities())
                                                .then(Commands.argument("amount",
                                                                IntegerArgumentType.integer(0, BloodData.MAX_BLOOD))
                                                        .executes(ctx -> executeSetAmount(ctx,
                                                                EntityArgument.getEntities(ctx, "entities"),
                                                                IntegerArgumentType.getInteger(ctx, "amount"))))))


                                .then(Commands.literal("type")
                                        .then(Commands.argument("entities", EntityArgument.entities())
                                                .then(Commands.argument("type",
                                                                StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            for (BloodType t : BloodType.values())
                                                                builder.suggest(t.name().toLowerCase());
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> executeSetType(ctx,
                                                                EntityArgument.getEntities(ctx, "entities"),
                                                                StringArgumentType.getString(ctx, "type")))))))

                        .then(Commands.literal("reset")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("entities", EntityArgument.entities())
                                        .executes(ctx -> executeReset(ctx,
                                                EntityArgument.getEntities(ctx, "entities")))))
        );
    }

    private static int executeGet(CommandContext<CommandSourceStack> ctx, Entity entity) {
        BloodData data = BloodCapability.get(entity);
        if (data == null) {
            ctx.getSource().sendFailure(Component.literal(
                    entity.getName().getString() + " has no blood data."));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("[BioForge] %s - Blood: %d/%d  Type: %s  Phase: %s",
                        entity.getName().getString(),
                        data.getBlood(),
                        BloodData.MAX_BLOOD,
                        data.getBloodType().getDisplayName(),
                        data.getPhase().name())), false);
        return 1;
    }

    private static int executeSetAmount(CommandContext<CommandSourceStack> ctx,
                                        Collection<? extends Entity> entities, int amount) {
        int count = 0;
        for (Entity entity : entities) {
            BloodData data = BloodCapability.get(entity);
            if (data == null) continue;
            data.setBlood(amount);
            if (entity instanceof ServerPlayer sp) {
                BloodEventHandler.syncToClient(sp, data);
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "[BioForge] Set blood of " + entity.getName().getString()
                            + " to " + amount), true);
            count++;
        }
        return count;
    }

    private static int executeSetType(CommandContext<CommandSourceStack> ctx,
                                      Collection<? extends Entity> entities, String typeName) {
        BloodType type;
        try {
            type = BloodType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal(
                    "[BioForge] Unknown blood type: " + typeName
                            + ". Valid: " + getTypeList()));
            return 0;
        }

        int count = 0;
        for (Entity entity : entities) {
            BloodData data = BloodCapability.get(entity);
            if (data == null) continue;

            data.setBloodType(type);

            if (entity instanceof ServerPlayer sp) {
                BloodEventHandler.syncToClient(sp, data);
            }

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "[BioForge] Set blood type of " + entity.getName().getString()
                            + " to " + type.getDisplayName()), true);
            count++;
        }
        return count;
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx,
                                    Collection<? extends Entity> entities) {
        int count = 0;
        for (Entity entity : entities) {
            BloodData data = BloodCapability.get(entity);
            if (data == null) continue;

            data.setBlood(BloodData.MAX_BLOOD);

            if (entity instanceof LivingEntity living) {
                BloodType fresh = (living instanceof net.minecraft.world.entity.player.Player)
                        ? BloodType.randomHuman(new java.util.Random())
                        : BloodType.randomNonHuman(new java.util.Random());
                data.setBloodType(fresh);
            }

            if (entity instanceof ServerPlayer sp) {
                BloodEventHandler.syncToClient(sp, data);
            }

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "[BioForge] Reset blood for " + entity.getName().getString()), true);
            count++;
        }
        return count;
    }



    private static String getTypeList() {
        StringBuilder sb = new StringBuilder();
        for (BloodType t : BloodType.values()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(t.name().toLowerCase());
        }
        return sb.toString();
    }
}