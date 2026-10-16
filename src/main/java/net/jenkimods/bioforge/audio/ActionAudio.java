package net.jenkimods.bioforge.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ActionAudio {
    private static final Map<BlockEntity,Map<String,Long>> WORK=new WeakHashMap<>();
    private static final Map<Player,Map<String,Long>> TOOLS=new WeakHashMap<>();
    public static boolean due(Long last,long now,int interval) { return last==null || now<last || now-last>=interval; }
    public static void play(Level level,BlockPos pos,String id,SoundSource source,float volume) {
        if(level==null || level.isClientSide())return;
        var sound=BioForgeActionSounds.EVENTS.get(id);
        if(sound==null)throw new IllegalArgumentException("Unknown action sound: "+id);
        level.playSound(null,pos,sound.get(),source,volume,1F);
    }
    public static boolean machineEvent(BlockEntity machine,String event,int interval) {
        var level=machine.getLevel();if(level==null || level.isClientSide())return false;
        var times=WORK.computeIfAbsent(machine,ignored->new HashMap<>());long now=level.getGameTime();
        if(!due(times.get(event),now,interval))return false;
        times.put(event,now);play(level,machine.getBlockPos(),event,SoundSource.BLOCKS,event.endsWith(".work")?.35F:.75F);return true;
    }
    public static boolean work(BlockEntity machine,String id) {
        // These supplied recordings last 2.07 seconds; allow them to finish before repeating.
        return machineEvent(machine,"machine."+id+".work",42);
    }
    public static void complete(BlockEntity machine,String id) { machineEvent(machine,"machine."+id+".complete",1); }
    public static boolean tool(Player player,String id) { return tool(player,id,20); }
    public static boolean tool(Player player,String id,int interval) {
        if(!(player instanceof ServerPlayer server) || server.connection==null)return false;
        var times=TOOLS.computeIfAbsent(player,ignored->new HashMap<>());long now=player.level().getGameTime();
        if(!due(times.get(id),now,interval))return false;
        var sound=BioForgeActionSounds.EVENTS.get(id);
        if(sound==null)throw new IllegalArgumentException("Unknown tool sound: "+id);
        times.put(id,now);server.playNotifySound(sound.get(),SoundSource.PLAYERS,.65F,1F);return true;
    }
}
