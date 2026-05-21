package net.jenkimods.bioforge.item.bone_saw;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class BoneSawAttackHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        LivingEntity target = event.getEntity();
        ItemStack mainHand = attacker.getMainHandItem();
        if (!(mainHand.getItem() instanceof BoneSawItem saw)) return;

        saw.tryCollectSplitBone(mainHand, target, attacker);
    }
}
