package net.jenkimods.bioforge.item.thermometer;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class ThermometerEventHandler {

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        tryShake(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        tryShake(event.getEntity());
    }

    private static void tryShake(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();

        boolean isMain = main.getItem() instanceof ThermometerItem;
        boolean isOff  = off.getItem() instanceof ThermometerItem;

        if (!isMain && !isOff) return;

        if (player.level().isClientSide()) {
            ThermometerNetworkHandler.sendShake(isMain);
        }

        ItemStack stack = isMain ? main : off;
        if (stack.getItem() instanceof ThermometerItem item) {
            item.onShake(player, stack);
        }
    }
}