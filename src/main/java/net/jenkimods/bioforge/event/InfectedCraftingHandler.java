package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectedCraftingHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack output = event.getCrafting();
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack ingredient = event.getInventory().getItem(i);
            if (ingredient.isEmpty()) continue;

            String strain = NbtObfuscator.readString(ingredient.getOrCreateTag());
            if (strain != null && !strain.equals("CLEAN")) {
                output.setTag(null);
                NbtObfuscator.writeStringDeterministic(output.getOrCreateTag(), stripColonyId(strain));

                if (player != null) {
                    player.sendSystemMessage(Component.literal("[BioForge] Crafted item is infected."));
                }
                break;
            }
        }
    }

    private static String stripColonyId(String strain) {
        if (strain == null) return null;
        int firstPipe = strain.indexOf('|');
        return firstPipe == -1 ? strain : strain.substring(firstPipe + 1);
    }
}