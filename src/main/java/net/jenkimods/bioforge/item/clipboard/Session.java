package net.jenkimods.bioforge.item.clipboard;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BioForge.MODID)
public class Session {

    public interface SessionCapability {
        UUID getId();
        void setId(UUID id);
    }

    public static class SessionCapabilityImpl implements SessionCapability {
        private UUID id;

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public void setId(UUID id) {
            this.id = id;
        }
    }

    public static final Capability<SessionCapability> SESSION_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation SESSION_ID =
            new ResourceLocation(BioForge.MODID, "session");

    @Mod.EventBusSubscriber(modid = BioForge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerCaps(RegisterCapabilitiesEvent event) {
            event.register(SessionCapability.class);
        }
    }

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;

        event.addCapability(SESSION_ID, new Provider());
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final SessionCapabilityImpl backend = new SessionCapabilityImpl();
        private final LazyOptional<SessionCapability> optional = LazyOptional.of(() -> backend);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == SESSION_CAP ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (backend.getId() != null) {
                tag.putUUID("SessionId", backend.getId());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            if (tag.hasUUID("SessionId")) {
                backend.setId(tag.getUUID("SessionId"));
            }
        }
    }

    @Nullable
    public static UUID get(Entity entity) {
        if (!(entity instanceof Player player)) return null;

        return player.getCapability(SESSION_CAP)
                .map(SessionCapability::getId)
                .orElse(null);
    }

    public static void set(Player player, UUID id) {
        player.getCapability(SESSION_CAP).ifPresent(cap -> cap.setId(id));
    }
}
