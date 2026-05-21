package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VialColorHandler {

    private static final int COLOR_UNUSED          = 0xD4C19F;
    private static final int COLOR_POSITIVE_HUMAN  = 0x91160D;
    private static final int COLOR_NEGATIVE_HUMAN  = 0xF5A7A2;
    private static final int COLOR_ANIMAL          = 0xFCF74B;

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                VialColorHandler::getVialColor,
                BioForge.ANTI_A_VIAL.get(),
                BioForge.ANTI_B_VIAL.get(),
                BioForge.ANTI_D_VIAL.get()
        );
    }

    private static int getVialColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) return 0xFFFFFF;

        CompoundTag tag = stack.getOrCreateTag();
        boolean used    = tag.getBoolean("VialUsed");
        boolean reacted = tag.getBoolean("VialReacted");

        if (!used) return COLOR_UNUSED;

        String category = tag.getString("VialBloodCategory");
        if ("NON_HUMAN".equals(category)) return COLOR_ANIMAL;

        return reacted ? COLOR_POSITIVE_HUMAN : COLOR_NEGATIVE_HUMAN;
    }
}