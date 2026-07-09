package net.jenkimods.bioforge.world.incubator;

import com.google.gson.*;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.PathogenType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class CatalystMappingManager extends SimpleJsonResourceReloadListener {

    public static final CatalystMappingManager INSTANCE = new CatalystMappingManager();
    private Map<Item, PathogenType> itemToPathogen = new HashMap<>();
    private List<PathogenType> allPathogens = new ArrayList<>();

    private CatalystMappingManager() {
        super(new Gson(), "incubator/catalyst_mappings");
    }

    @Nullable
    public PathogenType getPathogen(Item item) {
        return itemToPathogen.get(item);
    }

    public PathogenType getRandomPathogen() {
        PathogenType[] types = PathogenType.values();
        PathogenType random;
        do {
            random = types[new Random().nextInt(types.length)];
        } while (random == PathogenType.UNIVERSAL);
        return random;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, PathogenType> map = new HashMap<>();
        Set<PathogenType> pathogens = new HashSet<>();
        objects.forEach((id, element) -> {
            try {
                JsonObject root = element.getAsJsonObject();
                if (root.has("mappings")) {
                    JsonArray arr = root.getAsJsonArray("mappings");
                    for (JsonElement e : arr) {
                        JsonObject entry = e.getAsJsonObject();
                        String itemId = entry.get("item").getAsString();
                        String pathogenName = entry.get("pathogen").getAsString();
                        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
                        PathogenType pathogen = PathogenType.fromName(pathogenName);
                        if (item != null && pathogen != null) {
                            map.put(item, pathogen);
                            pathogens.add(pathogen);
                        }
                    }
                }
            } catch (Exception ex) {
                BioForge.LOGGER.error("Error loading catalyst mapping: {}", ex.getMessage());
            }
        });
        this.itemToPathogen = map;
        this.allPathogens = new ArrayList<>(pathogens);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}