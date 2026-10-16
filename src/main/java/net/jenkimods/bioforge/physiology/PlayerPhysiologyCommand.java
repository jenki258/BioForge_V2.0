package net.jenkimods.bioforge.physiology;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.jenkimods.bioforge.infection.HeartRate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class PlayerPhysiologyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var targets = Commands.argument("players",EntityArgument.players());
        for (String field : List.of("temperature","oxygen","perfusion")) {
            targets.then(Commands.literal(field).then(Commands.argument("value",DoubleArgumentType.doubleArg(0,field.equals("temperature")?100:1))
                    .executes(c->set(c.getSource(),EntityArgument.getPlayers(c,"players"),field,
                            DoubleArgumentType.getDouble(c,"value"),null))));
        }
        targets.then(Commands.literal("heart").then(Commands.argument("rate",StringArgumentType.word())
                .suggests((c,b)->SharedSuggestionProvider.suggest(List.of("NORMAL","TACHY","BRADY"),b))
                .executes(c->{
                    HeartRate rate;
                    try { rate=HeartRate.valueOf(StringArgumentType.getString(c,"rate").toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException error) {
                        c.getSource().sendFailure(Component.translatable("command.bioforge.physiology.invalid_heart"));return 0;
                    }
                    return set(c.getSource(),EntityArgument.getPlayers(c,"players"),"heart",0,rate);
                })));
        dispatcher.register(Commands.literal("bioforge").requires(s->s.hasPermission(2))
                .then(Commands.literal("physiology")
                        .then(Commands.literal("get").executes(c->show(c.getSource(),c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player",EntityArgument.player())
                                        .executes(c->show(c.getSource(),EntityArgument.getPlayer(c,"player")))))
                        .then(Commands.literal("set").then(targets))
                        .then(Commands.literal("reset").then(Commands.argument("players",EntityArgument.players())
                                .executes(c->{
                                    var data=PlayerPhysiologyData.get(c.getSource().getServer());
                                    var players=EntityArgument.getPlayers(c,"players");
                                    for(var player:players)data.reset(player.getUUID());
                                    c.getSource().sendSuccess(()->Component.translatable("command.bioforge.physiology.reset",players.size()),true);
                                    return players.size();
                                })))));
    }
    private static int show(CommandSourceStack source,ServerPlayer player) {
        var p=PhysiologyReadings.profile(player);
        source.sendSuccess(()->Component.translatable("command.bioforge.physiology.values",player.getDisplayName(),
                String.format(Locale.ROOT,"%.2f",p.temperatureC()),p.heartRate().name(),
                String.format(Locale.ROOT,"%.1f",p.oxygen()*100),String.format(Locale.ROOT,"%.2f",p.perfusion())),false);
        return 1;
    }
    private static int set(CommandSourceStack source,Collection<ServerPlayer> players,String field,double value,HeartRate heart) {
        var data=PlayerPhysiologyData.get(source.getServer());
        for(var player:players) {
            var p=PhysiologyReadings.profile(player);
            data.set(player.getUUID(),new PhysiologyProfile(field.equals("temperature")?value:p.temperatureC(),
                    field.equals("heart")?heart:p.heartRate(),field.equals("oxygen")?(float)value:p.oxygen(),
                    field.equals("perfusion")?(float)value:p.perfusion()));
        }
        source.sendSuccess(()->Component.translatable("command.bioforge.physiology.updated",players.size()),true);
        return players.size();
    }
}
