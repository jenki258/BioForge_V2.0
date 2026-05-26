package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectedCraftingHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack output = event.getCrafting();
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (!ingredient.isEmpty()) {
                String strain = NbtObfuscator.readString(ingredient.getOrCreateTag());
                if (strain != null && !strain.equals("CLEAN")) {
                    NbtObfuscator.writeString(output.getOrCreateTag(), stripColonyId(strain));
                    break;
                }
            }
        }
    }

    private static String stripColonyId(String strain) {
        if (strain == null) return null;
        int firstPipe = strain.indexOf('|');
        if (firstPipe == -1) return strain;
        return strain.substring(firstPipe + 1);
    }
}