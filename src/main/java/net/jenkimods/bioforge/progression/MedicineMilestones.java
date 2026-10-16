package net.jenkimods.bioforge.progression;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.item.protective.HazmatSuitItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class MedicineMilestones {
    private MedicineMilestones() {}
    public static void award(Player player, String id, String criterion) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var advancement = serverPlayer.server.getAdvancements().getAdvancement(ResourceLocation.tryBuild(BioForge.MODID, id));
        if (advancement != null && !serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone())
            serverPlayer.getAdvancements().award(advancement, criterion);
    }
    public static void award(ServerLevel level, UUID operator, String id, String criterion) {
        if (operator != null) award(level.getServer().getPlayerList().getPlayer(operator), id, criterion);
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        var stack = event.getCrafting();
        if (stack.getItem() instanceof HazmatSuitItem suit && suit.hasCustomColor(stack))
            award(event.getEntity(), "colored_hazmat", "dyed");
    }
}
