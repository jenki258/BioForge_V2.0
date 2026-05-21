package net.jenkimods.bioforge.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipeManager;
import net.jenkimods.bioforge.world.decalcification.DecalcificationRecipeManager;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

@JeiPlugin
public class CentrifugeJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            Objects.requireNonNull(ResourceLocation.tryBuild(BioForge.MODID, "jei_plugin"));

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CentrifugeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new DecalcificationRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<CentrifugeRecipeWrapper> centrifugeWrapped = CentrifugeRecipeManager.INSTANCE.getRecipes()
                .stream()
                .map(CentrifugeRecipeWrapper::new)
                .toList();
        registration.addRecipes(CentrifugeRecipeCategory.RECIPE_TYPE, centrifugeWrapped);

        List<DecalcificationRecipeWrapper> decalcWrapped = DecalcificationRecipeManager.INSTANCE.getRecipes()
                .stream()
                .map(DecalcificationRecipeWrapper::new)
                .toList();
        registration.addRecipes(DecalcificationRecipeCategory.RECIPE_TYPE, decalcWrapped);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                net.jenkimods.bioforge.world.centrifuge.CentrifugeMenu.class,
                BioForge.CENTRIFUGE_MENU.get(),
                CentrifugeRecipeCategory.RECIPE_TYPE,
                0,
                8,
                8,
                36
        );
    }
}
