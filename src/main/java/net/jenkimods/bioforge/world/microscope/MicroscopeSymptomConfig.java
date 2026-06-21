package net.jenkimods.bioforge.world.microscope;

import com.google.gson.*;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.MicroscopeVisibility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class MicroscopeSymptomConfig extends SimpleJsonResourceReloadListener {

    public static final MicroscopeSymptomConfig INSTANCE = new MicroscopeSymptomConfig();
    private List<MicroscopeSymptomEntry> entries = new ArrayList<>();

    private MicroscopeSymptomConfig() {
        super(new Gson(), "microscope");
    }

    public List<MicroscopeSymptomEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        List<MicroscopeSymptomEntry> list = new ArrayList<>();
        objects.forEach((id, element) -> {
            try {
                JsonObject root = element.getAsJsonObject();
                if (root.has("entries")) {
                    JsonArray arr = root.getAsJsonArray("entries");
                    for (JsonElement e : arr) {
                        JsonObject obj = e.getAsJsonObject();
                        addEntry(list, obj);
                    }
                } else {
                    addEntry(list, root);
                }
            } catch (Exception e) {
                BioForge.LOGGER.error("Invalid microscope entry {}: {}", id, e.getMessage());
            }
        });
        this.entries = list;
    }

    private void addEntry(List<MicroscopeSymptomEntry> list, JsonObject json) {
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
            list.add(new MicroscopeSymptomEntry(key, ResourceLocation.tryParse(icon), stateIcons, minVis));
        } else {
            boolean bool = type.equals("boolean");
            list.add(new MicroscopeSymptomEntry(key, ResourceLocation.tryParse(icon), bool, minVis));
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