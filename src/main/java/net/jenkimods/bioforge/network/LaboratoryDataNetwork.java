package net.jenkimods.bioforge.network;

import com.google.gson.*;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.BioForgeResearchData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.io.Reader;
import java.util.*;
import java.util.function.Supplier;

/** Synchronizes legacy data directories that vanilla RecipeManager does not send to remote clients. */
@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class LaboratoryDataNetwork {
    public static final List<String> DIRECTORIES = List.of("crispr/guide_profiles", "crispr/cas_modules", "crispr/assays", "vaccine_actions", "vaccine_correction_profiles", "vaccine_maker", "laboratory_processing", "centrifuge", "decalcification", "incubator/catalyst_mappings", "microscope", "mutations");
    private static final int MAX_JSON = 1_000_000;
    private static SimpleChannel channel;
    private static volatile boolean remoteDataLoaded;
    private LaboratoryDataNetwork() {}
    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(ResourceLocation.tryBuild(BioForge.MODID, "laboratory_data"), () -> "1", "1"::equals, "1"::equals);
        channel.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        if (channel == null) return;
        var manager = event.getPlayerList().getServer().getResourceManager();
        for (String directory : DIRECTORIES) {
            JsonObject entries = new JsonObject();
            manager.listResources(directory, id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
                String path = file.getPath();
                ResourceLocation id = ResourceLocation.tryBuild(file.getNamespace(), path.substring(directory.length() + 1, path.length() - 5));
                try (Reader reader = resource.openAsReader()) { entries.add(id.toString(), JsonParser.parseReader(reader)); }
                catch (Exception exception) { BioForge.LOGGER.error("Cannot sync laboratory resource {}", file, exception); }
            });
            Snapshot packet = new Snapshot(directory, entries.toString());
            if (packet.json.length() > MAX_JSON) throw new IllegalStateException("Laboratory data exceeds sync limit: " + directory);
            if (event.getPlayer() != null) send(event.getPlayer(), packet);
            else for (ServerPlayer player : event.getPlayerList().getPlayers()) send(player, packet);
        }
    }
    private static void send(ServerPlayer player, Snapshot packet) { channel.send(PacketDistributor.PLAYER.with(() -> player), packet); }
    public static void applyRemote(String directory, Map<ResourceLocation, JsonElement> entries) {
        switch (directory) {
            case "laboratory_processing" -> net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipeManager.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            case "centrifuge" -> net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipeManager.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            case "decalcification" -> net.jenkimods.bioforge.world.decalcification.DecalcificationRecipeManager.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            case "incubator/catalyst_mappings" -> net.jenkimods.bioforge.world.incubator.CatalystMappingManager.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            case "microscope" -> net.jenkimods.bioforge.world.microscope.MicroscopeSymptomConfig.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            case "mutations" -> net.jenkimods.bioforge.mutation.MutationLoader.INSTANCE.apply(entries, null, net.minecraft.util.profiling.InactiveProfiler.INSTANCE);
            default -> BioForgeResearchData.applyRemoteData(directory, entries);
        }
    }
    public static void clearRemote() {
        // LoggingOut also fires while a new integrated world is being created,
        // after its datapacks loaded but before ServerLifecycleHooks sees it.
        if (!remoteDataLoaded || net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) return;
        remoteDataLoaded = false;
        for (String directory : DIRECTORIES) applyRemote(directory, Map.of());
    }
    @SubscribeEvent public static void beginServerReload(net.minecraftforge.event.AddReloadListenerEvent event) {
        beginServerReload();
    }
    public static void beginServerReload() { remoteDataLoaded = false; }
    public record Snapshot(String directory, String json) {
        public static void encode(Snapshot packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.directory, 64); buffer.writeUtf(packet.json, MAX_JSON); }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            String directory = buffer.readUtf(64);
            if (!DIRECTORIES.contains(directory)) throw new IllegalArgumentException("Unknown laboratory directory");
            return new Snapshot(directory, buffer.readUtf(MAX_JSON));
        }
        private static void handle(Snapshot packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                // Integrated server and client share static registries; never rewrite server data from its client thread.
                if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) return;
                Map<ResourceLocation, JsonElement> entries = new LinkedHashMap<>();
                JsonObject root = JsonParser.parseString(packet.json).getAsJsonObject();
                if (root.size() > 4096) throw new IllegalArgumentException("Too many laboratory definitions");
                root.entrySet().forEach(entry -> entries.put(java.util.Objects.requireNonNull(ResourceLocation.tryParse(entry.getKey())), entry.getValue()));
                applyRemote(packet.directory, entries);
                remoteDataLoaded = true;
            });
            context.setPacketHandled(true);
        }
    }
}
