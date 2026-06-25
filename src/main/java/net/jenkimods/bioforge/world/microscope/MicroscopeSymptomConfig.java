package net.jenkimods.bioforge.world.microscope;

import com.google.gson.*;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.MicroscopeVisibility;
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

import java.util.*;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class MicroscopeSymptomConfig extends SimpleJsonResourceReloadListener {

    public static final MicroscopeSymptomConfig INSTANCE = new MicroscopeSymptomConfig();
    private Map<Item, List<MicroscopeSymptomEntry>> itemEntries = new HashMap<>();

    private MicroscopeSymptomConfig() {
        super(new Gson(), "microscope");
    }

    public List<MicroscopeSymptomEntry> getEntriesFor(ItemStack stack) {
        return itemEntries.getOrDefault(stack.getItem(), Collections.emptyList());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, List<MicroscopeSymptomEntry>> map = new HashMap<>();
        objects.forEach((id, element) -> {
            try {
                JsonObject root = element.getAsJsonObject();
                if (root.has("items")) {
                    JsonObject itemsObj = root.getAsJsonObject("items");
                    for (String itemId : itemsObj.keySet()) {
                        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));
                        if (item == null) {
                            BioForge.LOGGER.warn("Unknown item in microscope config: {}", itemId);
                            continue;
                        }
                        JsonElement itemElement = itemsObj.get(itemId);
                        List<MicroscopeSymptomEntry> entries = new ArrayList<>();

                        if (itemElement.isJsonObject()) {
                            JsonObject itemObj = itemElement.getAsJsonObject();
                            if (itemObj.has("entries")) {
                                JsonArray arr = itemObj.getAsJsonArray("entries");
                                for (JsonElement e : arr) {
                                    entries.add(parseEntry(e.getAsJsonObject()));
                                }
                            }
                        } else if (itemElement.isJsonArray()) {
                            JsonArray arr = itemElement.getAsJsonArray();
                            for (JsonElement e : arr) {
                                entries.add(parseEntry(e.getAsJsonObject()));
                            }
                        }
                        map.put(item, entries);
                    }
                }
                else if (root.has("entries")) {
                    List<MicroscopeSymptomEntry> entries = new ArrayList<>();
                    JsonArray arr = root.getAsJsonArray("entries");
                    for (JsonElement e : arr) {
                        entries.add(parseEntry(e.getAsJsonObject()));
                    }
                    for (Item item : ForgeRegistries.ITEMS.getValues()) {
                        map.put(item, entries);
                    }
                }
            } catch (Exception e) {
                BioForge.LOGGER.error("Invalid microscope config {}: {}", id, e.getMessage());
            }
        });
        this.itemEntries = map;
    }

    private MicroscopeSymptomEntry parseEntry(JsonObject json) {
        String key = json.get("symptom").getAsString();
        String icon = json.get("icon").getAsString();
        String type = json.get("type").getAsString();
        MicroscopeVisibility minVis = parseVisibility(json);

        if ("enum".equals(type)) {
            Map<String, ResourceLocation> stateIcons = new LinkedHashMap<>();
            if (json.has("states")) {
                JsonObject states = json.getAsJsonObject("states");
                for (String stateName : states.keySet()) {
                    stateIcons.put(stateName, ResourceLocation.tryParse(states.get(stateName).getAsString()));
                }
            }
            return new MicroscopeSymptomEntry(key, ResourceLocation.tryParse(icon), stateIcons, minVis);
        } else {
            boolean bool = type.equals("boolean");
            return new MicroscopeSymptomEntry(key, ResourceLocation.tryParse(icon), bool, minVis);
        }
    }

    private MicroscopeVisibility parseVisibility(JsonObject json) {
        if (json.has("min_visibility")) {
            return MicroscopeVisibility.fromName(json.get("min_visibility").getAsString());
        }
        return MicroscopeVisibility.NONE;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }
}