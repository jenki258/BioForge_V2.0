package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class InfectedFoodHandler {

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack item = event.getItem();
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        String strain = NbtObfuscator.readString(item.getOrCreateTag());
        if (strain == null || strain.equals("CLEAN")) return;

        String[] parts = strain.split(";");
        if (parts.length == 0) return;
        String[] header = parts[0].split("\\|");
        PathogenType pathogen;
        InfectionType infectionType;
        if (header.length >= 3) {
            pathogen = PathogenType.fromName(header[1]);
            infectionType = InfectionType.fromName(header[2]);
        } else if (header.length == 2) {
            pathogen = PathogenType.fromName(header[0]);
            infectionType = InfectionType.fromName(header[1]);
        } else {
            return;
        }

        if (pathogen != null && pathogen.allows(InfectionType.FOOD_BORNE)) {
            InfectionData data = InfectionCapability.get(entity);
            if (data != null && !data.isInfected()) {
                data.setInfected(true);
                data.setPathogenType(pathogen);
                data.setInfectionType(infectionType != null ? infectionType : InfectionType.FOOD_BORNE);
                InfectionEventHandler.applyDefaultSymptoms(data);

                if (entity instanceof ServerPlayer sp) {
                    InfectionEventHandler.syncToClient(sp, data);
                }
            }
        }
    }
}