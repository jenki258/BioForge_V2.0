package net.jenkimods.bioforge.world.incubator;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IncubatorRecipeRegistration {

    public static final ResourceLocation ID =
            Objects.requireNonNull(ResourceLocation.tryBuild(BioForge.MODID, "incubator"));
    public static final RecipeType<IncubatorRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return ID.toString();
        }
    };
    public static final RecipeSerializer<IncubatorRecipe> SERIALIZER = new IncubatorRecipeSerializer();

    private IncubatorRecipeRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.RECIPE_TYPE, helper -> helper.register(ID, TYPE));
        event.register(Registries.RECIPE_SERIALIZER, helper -> helper.register(ID, SERIALIZER));
    }
}
