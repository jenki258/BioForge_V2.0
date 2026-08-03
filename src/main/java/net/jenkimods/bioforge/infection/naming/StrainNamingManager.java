package net.jenkimods.bioforge.infection.naming;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.infection.InfectionData;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.vaccine.StrainFingerprint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public final class StrainNamingManager {
    public static final int MAX_NAME_LENGTH = 48;

    public enum NamingResult {
        NAMED,
        INVALID,
        DUPLICATE,
        LOCKED
    }

    private StrainNamingManager() {}

    public static void discover(LivingEntity target, InfectionData infection) {
        if (!(target.level() instanceof ServerLevel level) || infection == null
                || !infection.isInfected() || infection.getPathogenType() == null) return;
        String fingerprint = StrainFingerprint.ofPayload(
                StrainData.buildFrom(infection).toPayload());
        ServerPlayer researcher = target instanceof ServerPlayer player ? player : null;
        StrainNameStore.Discovery discovery = StrainNameStore.get(level).discover(
                fingerprint, researcher == null ? null : researcher.getUUID(),
                researcher == null ? "" : researcher.getGameProfile().getName(),
                level.getGameTime());
        if (discovery.newlyDiscovered() || discovery.researcherAssigned()) {
            StrainNameNetworkHandler.syncAll(level.getServer());
        }
        if (researcher != null && discovery.researcherAssigned()
                && !discovery.entry().isNamed()) {
            researcher.sendSystemMessage(Component.translatable(
                    "message.bioforge.strain.discovered", fingerprint)
                    .withStyle(ChatFormatting.AQUA));
            StrainNameNetworkHandler.prompt(researcher, fingerprint);
        }
    }

    public static Optional<String> getName(ServerLevel level, String fingerprint) {
        return StrainNameStore.get(level).find(fingerprint)
                .filter(StrainNameStore.Entry::isNamed)
                .map(StrainNameStore.Entry::name);
    }

    public static String displayName(ServerLevel level, String fingerprint) {
        return getName(level, fingerprint).orElse("Strain " + fingerprint);
    }

    public static Optional<String> getClientName(String fingerprint) {
        return StrainNameClientCache.find(fingerprint);
    }

    public static NamingResult submitFirstName(ServerPlayer player, String fingerprint,
                                                String input) {
        String current = currentFingerprint(player);
        if (current == null || !current.equals(fingerprint)) return NamingResult.LOCKED;
        String name = sanitizeName(input);
        if (name == null) return NamingResult.INVALID;
        StrainNameStore store = StrainNameStore.get(player.serverLevel());
        StrainNameStore.Entry entry = store.find(fingerprint).orElse(null);
        if (entry == null || entry.isNamed() || entry.researcherId() == null
                || !player.getUUID().equals(entry.researcherId())) {
            return NamingResult.LOCKED;
        }
        if (store.isNameTaken(name, fingerprint)) return NamingResult.DUPLICATE;
        if (!store.nameFirstDiscovery(fingerprint, player.getUUID(), name)) {
            return NamingResult.LOCKED;
        }
        StrainNameNetworkHandler.syncAll(player.server);
        return NamingResult.NAMED;
    }

    public static String sanitizeName(String input) {
        if (input == null) return null;
        String stripped = ChatFormatting.stripFormatting(input.replace('\u00A7', ' '));
        if (stripped == null) return null;
        String cleaned = stripped.replaceAll("[\\p{Cntrl}]", " ")
                .trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() || cleaned.length() > MAX_NAME_LENGTH
                ? null : cleaned;
    }

    private static String currentFingerprint(ServerPlayer player) {
        InfectionData infection = net.jenkimods.bioforge.infection.InfectionCapability.get(player);
        if (infection == null || !infection.isInfected() || infection.getPathogenType() == null) {
            return null;
        }
        return StrainFingerprint.ofPayload(StrainData.buildFrom(infection).toPayload());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StrainNameNetworkHandler.sync(player);
            String fingerprint = currentFingerprint(player);
            if (fingerprint == null) return;
            StrainNameStore.get(player.serverLevel()).find(fingerprint)
                    .filter(entry -> !entry.isNamed())
                    .filter(entry -> player.getUUID().equals(entry.researcherId()))
                    .ifPresent(entry -> StrainNameNetworkHandler.prompt(
                            player, entry.fingerprint()));
        }
    }
}
