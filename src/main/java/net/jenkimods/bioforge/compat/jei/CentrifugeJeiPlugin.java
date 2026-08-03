package net.jenkimods.bioforge.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipeManager;
import net.jenkimods.bioforge.world.decalcification.DecalcificationRecipeManager;
import net.jenkimods.bioforge.world.incubator.IncubatorRecipeRegistration;
import net.jenkimods.bioforge.crispr.BioForgeResearchData;
import net.minecraft.client.Minecraft;
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
                new DecalcificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new IncubatorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new VaccineMakerRecipeCategory(registration.getJeiHelpers().getGuiHelper())
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

        if (Minecraft.getInstance().level != null) {
            List<IncubatorRecipeWrapper> incubatorWrapped = Minecraft.getInstance().level
                    .getRecipeManager()
                    .getAllRecipesFor(IncubatorRecipeRegistration.TYPE)
                    .stream()
                    .map(IncubatorRecipeWrapper::new)
                    .toList();
            registration.addRecipes(IncubatorRecipeCategory.RECIPE_TYPE, incubatorWrapped);
        }

        registration.addRecipes(VaccineMakerRecipeCategory.RECIPE_TYPE,
                BioForgeResearchData.recipes().stream()
                        .map(VaccineMakerRecipeWrapper::new)
                        .toList());
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
        registration.addRecipeTransferHandler(
                net.jenkimods.bioforge.world.incubator.IncubatorMenu.class,
                BioForge.INCUBATOR_MENU.get(),
                IncubatorRecipeCategory.RECIPE_TYPE,
                0,
                4,
                4,
                36
        );
    }
}
