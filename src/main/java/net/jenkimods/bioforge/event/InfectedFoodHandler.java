package net.jenkimods.bioforge.event;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.util.NbtObfuscator;
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

        String strainRaw = NbtObfuscator.readString(item.getOrCreateTag());
        if (strainRaw == null || strainRaw.equals("CLEAN")) return;

        StrainData strain = StrainData.parse(strainRaw);
        if (strain.getInfectionTypes().contains(InfectionType.FOOD_BORNE) && strain.getPathogen() != null) {
            InfectionData data = InfectionCapability.get(entity);
            if (data != null) {
                strain.applyToEntity(data, entity);
            }
        }
    }
}