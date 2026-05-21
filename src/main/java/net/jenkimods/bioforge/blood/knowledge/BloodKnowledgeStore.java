package net.jenkimods.bioforge.blood.knowledge;

import net.jenkimods.bioforge.world.data.ReagentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class BloodKnowledgeStore extends SavedData {

    private static final String DATA_NAME   = "bioforge_blood_knowledge";
    private static final int    MAX_ENTRIES = 2000;

    private final Map<UUID, LinkedHashMap<UUID, BloodKnowledge>> data = new HashMap<>();

    public static BloodKnowledgeStore get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
                BloodKnowledgeStore::load,
                BloodKnowledgeStore::new,
                DATA_NAME
        );
    }

    public BloodKnowledgeStore() {}

    public BloodKnowledge getOrCreate(UUID playerUUID, UUID subjectUUID,
                                      String subjectName, String subjectType, boolean isSubjectPlayer) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap =
                data.computeIfAbsent(playerUUID, k -> new LinkedHashMap<>());
        return playerMap.computeIfAbsent(subjectUUID,
                k -> new BloodKnowledge(subjectUUID, subjectName, subjectType, isSubjectPlayer));
    }

    public Optional<BloodKnowledge> find(UUID playerUUID, UUID subjectUUID) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return Optional.empty();
        return Optional.ofNullable(playerMap.get(subjectUUID));
    }

    public Collection<BloodKnowledge> getAllForPlayer(UUID playerUUID) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return List.of();
        return List.copyOf(playerMap.values());
    }

    public int clearAllForPlayer(UUID playerUUID) {
        LinkedHashMap<UUID, BloodKnowledge> removed = data.remove(playerUUID);
        int count = removed != null ? removed.size() : 0;
        if (removed != null) {
            setDirty();
        }
        return count;
    }

    public boolean removeForPlayer(UUID playerUUID, UUID subjectUUID) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return false;

        BloodKnowledge removed = playerMap.remove(subjectUUID);
        if (removed == null) return false;

        if (playerMap.isEmpty()) {
            data.remove(playerUUID);
        }
        setDirty();
        return true;
    }

    public List<String> getSubjectNamesForPlayer(UUID playerUUID) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return List.of();

        return playerMap.values().stream()
                .map(BloodKnowledge::getSubjectName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public int removeBySubjectNameForPlayer(UUID playerUUID, String subjectName) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return 0;

        int before = playerMap.size();
        playerMap.entrySet().removeIf(e -> e.getValue().getSubjectName().equalsIgnoreCase(subjectName));
        int removed = before - playerMap.size();

        if (removed > 0) {
            if (playerMap.isEmpty()) data.remove(playerUUID);
            setDirty();
        }

        return removed;
    }

    public void recordReagent(UUID playerUUID, UUID subjectUUID,
                              String subjectName, String subjectType, boolean isSubjectPlayer,
                              ReagentType reagentType, boolean reacted) {
        BloodKnowledge k = getOrCreate(
                playerUUID, subjectUUID, subjectName, subjectType, isSubjectPlayer);

        switch (reagentType) {
            case ANTI_A -> k.setAntiA(reacted);
            case ANTI_B -> k.setAntiB(reacted);
            case ANTI_D -> k.setAntiD(reacted);
        }

        enforceLimit(playerUUID);
        setDirty();
    }

    private void enforceLimit(UUID playerUUID) {
        LinkedHashMap<UUID, BloodKnowledge> playerMap = data.get(playerUUID);
        if (playerMap == null) return;

        while (playerMap.size() > MAX_ENTRIES) {
            UUID oldest = playerMap.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().getLastUpdated()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest != null) playerMap.remove(oldest);
        }
    }

    @Override
    public CompoundTag save(CompoundTag out) {
        ListTag playersList = new ListTag();
        data.forEach((playerUUID, knowledgeMap) -> {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("PlayerUUID", playerUUID);
            ListTag entriesList = new ListTag();
            knowledgeMap.values().forEach(k -> entriesList.add(k.serialize()));
            playerTag.put("Entries", entriesList);
            playersList.add(playerTag);
        });
        out.put("Players", playersList);
        return out;
    }

    public static BloodKnowledgeStore load(CompoundTag tag) {
        BloodKnowledgeStore store = new BloodKnowledgeStore();
        ListTag playersList = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playersList.size(); i++) {
            CompoundTag playerTag = playersList.getCompound(i);
            UUID playerUUID = playerTag.getUUID("PlayerUUID");
            LinkedHashMap<UUID, BloodKnowledge> knowledgeMap = new LinkedHashMap<>();
            ListTag entriesList = playerTag.getList("Entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entriesList.size(); j++) {
                BloodKnowledge k = BloodKnowledge.deserialize(entriesList.getCompound(j));
                knowledgeMap.put(k.getSubjectUUID(), k);
            }
            store.data.put(playerUUID, knowledgeMap);
        }
        return store;
    }
}