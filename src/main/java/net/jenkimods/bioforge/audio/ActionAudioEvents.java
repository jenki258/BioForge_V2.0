package net.jenkimods.bioforge.audio;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=BioForge.MODID)
public final class ActionAudioEvents {
    public static String diagnostic(ItemStack stack) {
        var item=stack.getItem();
        if(item instanceof net.jenkimods.bioforge.item.stethoscope.StethoscopeItem)return "stethoscope";
        if(item instanceof net.jenkimods.bioforge.item.otoscope.OtoscopeItem)return "otoscope";
        if(item instanceof net.jenkimods.bioforge.item.pulse_oximeter.PulseOximeterItem)return "pulse_oximeter";
        return null;
    }
    @SubscribeEvent public static void start(LivingEntityUseItemEvent.Start event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getDuration()>0) {
            var id=diagnostic(event.getItem());if(id!=null)ActionAudio.tool(player,"instrument."+id+".start");
        }
    }
    @SubscribeEvent public static void stop(LivingEntityUseItemEvent.Stop event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            var id=diagnostic(event.getItem());if(id!=null)ActionAudio.tool(player,"instrument."+id+".stop");
        }
    }
    private static boolean isMachine(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (menu instanceof net.jenkimods.bioforge.world.laboratory.LaboratoryProcessorMenu laboratory
                && (laboratory.station()==net.jenkimods.bioforge.world.laboratory.LaboratoryStation.BARREL_PRESS
                || laboratory.station()==net.jenkimods.bioforge.world.laboratory.LaboratoryStation.HERBAL_MORTAR)) return false;
        return menu.getClass().getName().startsWith("net.jenkimods.bioforge.");
    }
    @SubscribeEvent public static void open(PlayerContainerEvent.Open event) {
        if(isMachine(event.getContainer()))ActionAudio.tool(event.getEntity(),"ui.machine.open");
    }
    @SubscribeEvent public static void close(PlayerContainerEvent.Close event) {
        if(isMachine(event.getContainer()))ActionAudio.tool(event.getEntity(),"ui.machine.close");
    }
    @SubscribeEvent public static void equipment(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || net.minecraft.world.item.ItemStack.isSameItem(event.getFrom(),event.getTo()))return;
        if(protective(event.getFrom()))ActionAudio.tool(player,"instrument.protection.unequip");
        if(protective(event.getTo()))ActionAudio.tool(player,"instrument.protection.equip");
    }
    private static boolean protective(ItemStack stack) {
        return stack.getItem() instanceof net.jenkimods.bioforge.item.protective.ProtectiveGearItem
                || stack.getItem() instanceof net.jenkimods.bioforge.item.protective.HazmatSuitItem;
    }
}
