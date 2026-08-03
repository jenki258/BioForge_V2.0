package net.jenkimods.bioforge.crispr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.vaccine.DirectedVaccineAction;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BioForgeResearchData {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static volatile Map<ResourceLocation, CrisprGuideProfile> guideProfiles = Map.of();
    private static volatile Map<ResourceLocation, CrisprCasModuleDefinition> casModules = Map.of();
    private static volatile Map<ResourceLocation, CrisprAssayDefinition> assays = Map.of();
    private static volatile Map<ResourceLocation, DirectedVaccineAction> actions = Map.of();
    private static volatile List<VaccineMakerRecipe> recipes = List.of();

    private BioForgeResearchData() {}

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GuideProfileReloadListener());
        event.addListener(new CasModuleReloadListener());
        event.addListener(new AssayReloadListener());
        event.addListener(new ActionReloadListener());
        event.addListener(new RecipeReloadListener());
    }

    public static Optional<CrisprGuideProfile> guideProfile(ResourceLocation id) {
        return Optional.ofNullable(guideProfiles.get(id));
    }

    public static Optional<DirectedVaccineAction> action(ResourceLocation id) {
        return Optional.ofNullable(actions.get(id));
    }

    public static Optional<CrisprCasModuleDefinition> casModule(ResourceLocation id) {
        return Optional.ofNullable(casModules.get(id));
    }

    public static Optional<CrisprAssayDefinition> assay(ResourceLocation id) {
        return Optional.ofNullable(assays.get(id));
    }

    public static List<VaccineMakerRecipe> recipes() {
        return recipes;
    }

    public static Set<ResourceLocation> guideProfileIds() {
        return Set.copyOf(guideProfiles.keySet());
    }

    public static Set<ResourceLocation> casModuleIds() {
        return Set.copyOf(casModules.keySet());
    }

    public static Set<ResourceLocation> actionIds() {
        return Set.copyOf(actions.keySet());
    }

    private static final class GuideProfileReloadListener extends SimpleJsonResourceReloadListener {
        private GuideProfileReloadListener() {
            super(GSON, "crispr/guide_profiles");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, CrisprGuideProfile> loaded = new LinkedHashMap<>();
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> parse(entry.getKey(), entry.getValue(),
                            json -> loaded.put(entry.getKey(),
                                    CrisprGuideProfile.fromJson(entry.getKey(), json)),
                            "CRISPR guide profile"));
            guideProfiles = Map.copyOf(loaded);
            BioForge.LOGGER.info("Loaded {} CRISPR guide profiles", loaded.size());
        }
    }

    private static final class ActionReloadListener extends SimpleJsonResourceReloadListener {
        private ActionReloadListener() {
            super(GSON, "vaccine_actions");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, DirectedVaccineAction> loaded = new LinkedHashMap<>();
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> parse(entry.getKey(), entry.getValue(),
                            json -> loaded.put(entry.getKey(),
                                    DirectedVaccineAction.fromJson(entry.getKey(), json)),
                            "directed vaccine action"));
            actions = Map.copyOf(loaded);
            BioForge.LOGGER.info("Loaded {} directed vaccine actions", loaded.size());
        }
    }

    private static final class CasModuleReloadListener extends SimpleJsonResourceReloadListener {
        private CasModuleReloadListener() {
            super(GSON, "crispr/cas_modules");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, CrisprCasModuleDefinition> loaded = new LinkedHashMap<>();
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> parse(entry.getKey(), entry.getValue(),
                            json -> loaded.put(entry.getKey(),
                                    CrisprCasModuleDefinition.fromJson(entry.getKey(), json)),
                            "CRISPR Cas module"));
            casModules = Map.copyOf(loaded);
            BioForge.LOGGER.info("Loaded {} CRISPR Cas modules", loaded.size());
        }
    }

    private static final class AssayReloadListener extends SimpleJsonResourceReloadListener {
        private AssayReloadListener() {
            super(GSON, "crispr/assays");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, CrisprAssayDefinition> loaded = new LinkedHashMap<>();
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> parse(entry.getKey(), entry.getValue(),
                            json -> loaded.put(entry.getKey(),
                                    CrisprAssayDefinition.fromJson(entry.getKey(), json)),
                            "CRISPR assay"));
            assays = Map.copyOf(loaded);
            BioForge.LOGGER.info("Loaded {} CRISPR assays", loaded.size());
        }
    }

    private static final class RecipeReloadListener extends SimpleJsonResourceReloadListener {
        private RecipeReloadListener() {
            super(GSON, "vaccine_maker");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            List<VaccineMakerRecipe> loaded = new ArrayList<>();
            entries.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> parse(entry.getKey(), entry.getValue(),
                            json -> loaded.add(VaccineMakerRecipe.fromJson(entry.getKey(), json)),
                            "Vaccine Maker recipe"));
            loaded.sort(Comparator.comparing(recipe -> recipe.id().toString()));
            recipes = List.copyOf(loaded);
            BioForge.LOGGER.info("Loaded {} Vaccine Maker recipes", loaded.size());
        }
    }

    private static void parse(ResourceLocation id, JsonElement element,
                              java.util.function.Consumer<JsonObject> parser, String type) {
        try {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException(type + " must be a JSON object");
            }
            parser.accept(element.getAsJsonObject());
        } catch (RuntimeException exception) {
            BioForge.LOGGER.error("Could not load {} {}", type, id, exception);
        }
    }
}
