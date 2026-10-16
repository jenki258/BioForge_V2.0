package net.jenkimods.bioforge.item.infection;
import net.jenkimods.bioforge.BioForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class SwabMigration {
    @SubscribeEvent public static void remap(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, BioForge.MODID))
            if (mapping.getKey().getPath().equals("field_swab")) mapping.remap(BioForge.SWAB.get());
    }
}
