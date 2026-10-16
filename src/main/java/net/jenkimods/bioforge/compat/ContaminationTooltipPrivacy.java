package net.jenkimods.bioforge.compat;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.spread.ItemStrainData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Presentation only: never use the returned stack for inventory or infection logic. */
public final class ContaminationTooltipPrivacy {
    private ContaminationTooltipPrivacy() {}

    // Called before vanilla and Forge tooltip providers, including Jade's dropped-item tooltip.
    public static ItemStack forTooltip(ItemStack original) {
        if (original.isEmpty() || !original.hasTag()
                || BioForge.MODID.equals(BuiltInRegistries.ITEM.getKey(original.getItem()).getNamespace())
                || ItemStrainData.read(original) == null) return original;
        ItemStack display = original.copy();
        ItemStrainData.clear(display);
        return display;
    }
}
