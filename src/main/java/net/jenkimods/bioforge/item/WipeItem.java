package net.jenkimods.bioforge.item;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WipeItem extends Item {
    public WipeItem() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wipes = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        if (other.getItem() instanceof DirtyCultureVialItem) {
            if (!level.isClientSide()) {
                other.shrink(1);
                ItemStack clean = new ItemStack(BioForge.LIVE_CULTURE_VIAL.get());
                if (!player.getInventory().add(clean)) {
                    player.drop(clean, false);
                }
                wipes.shrink(1);
                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 1.2f);
                player.sendSystemMessage(Component.translatable("item.bioforge.wipes.cleaned"));
            }
            return InteractionResultHolder.success(wipes);
        }
        return InteractionResultHolder.pass(wipes);
    }
}