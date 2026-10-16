package net.jenkimods.bioforge.world.medicine;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.item.infection.SwabItem;
import net.jenkimods.bioforge.item.vaccine.VaccineItem;
import net.jenkimods.bioforge.vaccine.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;
import java.util.*;

public final class FieldMedicineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CRAFT_TICKS = 800, RADIUS = 12, CYCLE_TICKS = 2400, TABLET_CYCLE_TICKS = 3600, DATA_COUNT = 6;
    public static final String TREATMENT_UNTIL = "BioForgeMassTreatmentUntil";
    private int progress, status, water, treated;
    private ItemStack cycleMedicine = ItemStack.EMPTY;
    private ItemStack cycleTablet = ItemStack.EMPTY;
    private int tabletTicks, vaccineTicks;
    public final ItemStackHandler items = new ItemStackHandler(4) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (isDiffuser()) return switch (slot) {
                case 0 -> isMedicine(stack);
                case 1 -> stack.is(Items.WATER_BUCKET);
                default -> false;
            };
            return switch (slot) {
                case 0 -> isSample(stack);
                case 1 -> stack.is(BioForge.FIELD_VACCINE.get()) && VaccineProfile.read(stack) == null;
                case 2 -> stack.is(Items.SUGAR) || stack.is(Items.GLISTERING_MELON_SLICE);
                default -> false;
            };
        }
        @Override protected void onContentsChanged(int slot) {
            if (!isDiffuser() && slot < 3) progress = 0;
            setChanged();
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            var fixed = tag.copy(); fixed.putInt("Size", 4); super.deserializeNBT(fixed);
        }
    };
    private LazyOptional<IItemHandler> inventory = LazyOptional.of(() -> items);
    private LazyOptional<IItemHandler> automation = LazyOptional.of(this::automationHandler);
    public final ContainerData data = new ContainerData() {
        public int get(int i) { return switch (i) { case 0 -> progress; case 1 -> status; case 2 -> water; case 3 -> treated;
            case 4 -> tabletTicks > 0 ? tabletTicks : vaccineTicks;
            case 5 -> tabletTicks > 0 ? TABLET_CYCLE_TICKS : CYCLE_TICKS; default -> 0; }; }
        public void set(int i, int value) { switch (i) { case 0 -> progress = value; case 1 -> status = value; case 2 -> water = value; case 3 -> treated = value; } }
        public int getCount() { return DATA_COUNT; }
    };
    public FieldMedicineBlockEntity(BlockPos pos, BlockState state) { super(BioForge.FIELD_MEDICINE_BE.get(), pos, state); }
    public boolean isDiffuser() { return getBlockState().is(BioForge.MEDICAL_DIFFUSER.get()); }
    public static boolean isSample(ItemStack stack) {
        return stack.getItem() instanceof SwabItem || stack.is(BioForge.VIRUS_SAMPLE.get())
                || stack.is(BioForge.LIVE_CULTURE_VIAL.get()) || stack.is(BioForge.PETRI_DISH.get());
    }
    public static boolean isMedicine(ItemStack stack) {
        return SymptomTreatment.profile(stack) != null || VaccineItem.kindOf(stack) == VaccineItem.Kind.FULL
                && VaccineProfile.read(stack) != null && DirectedVaccineProfile.read(stack) == null;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, FieldMedicineBlockEntity machine) {
        if (level.isClientSide) return;
        if (machine.isDiffuser()) {
            if (machine.water > 0 && !level.hasNeighborSignal(pos)) {
                if (machine.tabletTicks>0 || machine.vaccineTicks>0) net.jenkimods.bioforge.audio.ActionAudio.work(machine,"medical_diffuser");
                machine.water--;
                if (machine.tabletTicks > 0) {
                    if (--machine.tabletTicks == 0) { machine.cycleTablet = ItemStack.EMPTY; net.jenkimods.bioforge.audio.ActionAudio.complete(machine,"medical_diffuser"); }
                } else if (machine.vaccineTicks > 0 && --machine.vaccineTicks == 0) { machine.cycleMedicine = ItemStack.EMPTY; net.jenkimods.bioforge.audio.ActionAudio.complete(machine,"medical_diffuser"); }
                machine.setChanged();
                if (level.getGameTime() % 20 == 0) ((ServerLevel) level).sendParticles(
                        ParticleTypes.CLOUD, pos.getX() + .5, pos.getY() + 1.2, pos.getZ() + .5,
                        3, .12, .08, .12, .015);
            }
            if (level.getGameTime() % 20 == 0) machine.dispense();
        } else machine.craft();
    }
    private void craft() {
        var sample = items.getStackInSlot(0);
        var strain = isSample(sample) ? StrainSampleUtil.getStrain(sample) : null;
        if (strain == null || strain.getPathogenId() == null) { idle(0); return; }
        if (!items.isItemValid(1, items.getStackInSlot(1))) { idle(1); return; }
        if (!items.isItemValid(2, items.getStackInSlot(2))) { idle(2); return; }
        if (!items.getStackInSlot(3).isEmpty()) { idle(3); return; }
        net.jenkimods.bioforge.audio.ActionAudio.work(this,"field_vaccine_maker");
        status = 4;
        if (++progress >= CRAFT_TICKS) {
            boolean improved = items.getStackInSlot(2).is(Items.GLISTERING_MELON_SLICE);
            ItemStack result = new ItemStack(improved ? BioForge.STABILIZED_FIELD_VACCINE.get() : BioForge.FIELD_VACCINE.get());
            new VaccineProfile(strain.toPayload(), improved ? .25F : .10F, 1, .03F,
                    UUID.randomUUID(), level.getGameTime()).write(result);
            for (int slot = 0; slot < 3; slot++) items.extractItem(slot, 1, false);
            items.setStackInSlot(3, result);
            net.jenkimods.bioforge.audio.ActionAudio.complete(this,"field_vaccine_maker");
            progress = 0;
        }
        setChanged();
    }
    private void idle(int reason) {
        if (progress != 0 || status != reason) { progress = 0; status = reason; setChanged(); }
    }
    /** One tablet broadcasts for three minutes; vaccine mist is suspended while a tablet is active. */
    private void dispense() {
        if (level.hasNeighborSignal(worldPosition)) { idle(8); return; }
        ItemStack pending = items.getStackInSlot(0);
        // Tablets can provide symptom relief during an already active vaccine cycle.
        ItemStack medicine = !cycleTablet.isEmpty() ? cycleTablet : SymptomTreatment.profile(pending) != null ? pending
                : cycleMedicine.isEmpty() ? pending : cycleMedicine;
        if (!isMedicine(medicine)) { idle(water > 0 ? 9 : 5); return; }
        if (!startCycle(SymptomTreatment.profile(medicine) != null ? TABLET_CYCLE_TICKS : CYCLE_TICKS)) { idle(6); return; }
        Vec3 origin = Vec3.atBottomCenterOf(worldPosition).add(0, 1.05, 0);
        var room = net.jenkimods.bioforge.infection.spread.AirRoomScanner.scanLimited(
                (ServerLevel) level, worldPosition.above(), RADIUS);
        var tablet = SymptomTreatment.profile(medicine);
        if (tablet != null) {
            // Capture once, including programmed NBT; further pulses reuse this dose.
            if (cycleTablet.isEmpty()) {
                cycleTablet = medicine.copy(); cycleTablet.setCount(1);
                tabletTicks = TABLET_CYCLE_TICKS;
                items.extractItem(0, 1, false);
            }
            medicine = cycleTablet;
            for (LivingEntity target : treatmentTargets(medicine, room)) {
                if (SymptomTreatment.apply(target, tablet) > 0) recordTreatment(target);
            }
            status = 9;
            setChanged();
            return;
        }
        var vaccine = VaccineProfile.read(medicine);
        if (cycleMedicine.isEmpty()) {
            cycleMedicine = medicine.copy(); cycleMedicine.setCount(1);
            vaccineTicks = CYCLE_TICKS;
            vaccine.withRemainingUses(1).write(cycleMedicine);
            VaccineProfile.consumeDose(medicine);
            medicine = cycleMedicine;
            vaccine = VaccineProfile.read(medicine);
            setChanged();
        }
        var targets = treatmentTargets(medicine, room);
        targets.sort(Comparator.comparingLong((LivingEntity e) -> e.getPersistentData().getLong(TREATMENT_UNTIL))
                .thenComparingDouble(e -> e.distanceToSqr(origin)));
        if (targets.isEmpty()) { idle(9); return; }
        LivingEntity target = targets.get(0);
        var attempt = VaccineManager.attemptVaccination(target, vaccine, VaccineHostProfile.read(medicine),
                net.jenkimods.bioforge.item.vaccine.FieldVaccineItem.effectiveness(medicine), false);
        if (!attempt.consumesDose()) { idle(9); return; }
        if (medicine.getItem() instanceof net.jenkimods.bioforge.item.vaccine.FieldVaccineItem)
            net.jenkimods.bioforge.item.vaccine.FieldVaccineItem.markTreatment(target);
        target.getPersistentData().putLong(TREATMENT_UNTIL, level.getGameTime() + 20);
        recordTreatment(target); status = 9;
        setChanged();
    }
    private void recordTreatment(LivingEntity target) {
        treated = Math.min(32767, treated + 1);
        ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + .7, target.getZ(), 5, .2, .2, .2, .01);
    }
    private boolean startCycle(int duration) {
        if (water > 0) return true;
        var bucket = items.getStackInSlot(3);
        if (!items.getStackInSlot(1).is(Items.WATER_BUCKET)
                || !bucket.isEmpty() && (!bucket.is(Items.BUCKET) || bucket.getCount() >= bucket.getMaxStackSize())) return false;
        items.extractItem(1, 1, false);
        if (bucket.isEmpty()) items.setStackInSlot(3, new ItemStack(Items.BUCKET)); else bucket.grow(1);
        water = duration;
        setChanged();
        return true;
    }
    private List<LivingEntity> treatmentTargets(ItemStack medicine,
            net.jenkimods.bioforge.infection.spread.AirRoomScanner.Room room) {
        var tablet = SymptomTreatment.profile(medicine);
        var vaccine = VaccineProfile.read(medicine);
        return level.getEntitiesOfClass(LivingEntity.class, room.bounds(), entity -> {
            if (!entity.isAlive() || entity.isSpectator() || entity instanceof Player p && p.isCreative()
                    || !InfectionCapability.canHostInfection(entity)
                    || !(room.contains(entity.blockPosition()) || room.contains(BlockPos.containing(entity.getEyePosition())))) return false;
            if (tablet != null) return true;
            var data = InfectionCapability.get(entity);
            return data != null && data.isInfected()
                    && entity.getPersistentData().getLong(TREATMENT_UNTIL) <= level.getGameTime()
                    && net.jenkimods.bioforge.item.vaccine.FieldVaccineItem.canTreat(entity, medicine)
                    && (tablet != null ? SymptomTreatment.needed(entity, tablet) : vaccine != null && compatible(vaccine, data));
        });
    }
    private static boolean compatible(VaccineProfile profile, InfectionData data) {
        var match = VaccineMatch.compare(profile.strain(), StrainData.buildFrom(data));
        return match.pathogenCompatible() && match.totalSimilarity() >= VaccineManager.getRules().minimumSimilarity();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.bioforge." + (isDiffuser() ? "medical_diffuser" : "field_vaccine_maker")); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new FieldMedicineMenu(id, inv, this, data); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Inventory", items.serializeNBT()); tag.putInt("Progress", progress);
        tag.putInt("CycleTicks", water); tag.putInt("Treated", treated);
        net.jenkimods.bioforge.util.NbtObfuscator.writeCompound(tag, "diffuser_cycle", cycleMedicine.save(new CompoundTag()));
        var runtime = new CompoundTag(); runtime.putInt("TabletTicks", tabletTicks); runtime.putInt("VaccineTicks", vaccineTicks);
        runtime.put("Tablet", cycleTablet.save(new CompoundTag()));
        net.jenkimods.bioforge.util.NbtObfuscator.writeCompound(tag, "diffuser_runtime", runtime);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items.deserializeNBT(tag.getCompound("Inventory"));
        progress = Math.max(0, Math.min(CRAFT_TICKS - 1, tag.getInt("Progress")));
        // Preserve the proportion of an old sixteen-treatment water reserve.
        water = Math.max(0, Math.min(TABLET_CYCLE_TICKS, tag.contains("CycleTicks")
                ? tag.getInt("CycleTicks") : tag.getInt("Water") * CYCLE_TICKS / 16));
        treated = Math.max(0, Math.min(32767, tag.getInt("Treated")));
        var cycle = net.jenkimods.bioforge.util.NbtObfuscator.readCompound(tag, "diffuser_cycle");
        cycleMedicine = cycle == null ? ItemStack.EMPTY : ItemStack.of(cycle);
        if (VaccineProfile.read(cycleMedicine) == null) cycleMedicine = ItemStack.EMPTY;
        var runtime = net.jenkimods.bioforge.util.NbtObfuscator.readCompound(tag, "diffuser_runtime");
        vaccineTicks = cycleMedicine.isEmpty() ? 0 : Math.max(0, Math.min(CYCLE_TICKS,
                runtime == null ? water : runtime.getInt("VaccineTicks")));
        if (vaccineTicks == 0) cycleMedicine = ItemStack.EMPTY;
        tabletTicks = runtime == null ? 0 : Math.max(0, Math.min(TABLET_CYCLE_TICKS, runtime.getInt("TabletTicks")));
        cycleTablet = runtime == null || tabletTicks == 0 ? ItemStack.EMPTY : ItemStack.of(runtime.getCompound("Tablet"));
        if (SymptomTreatment.profile(cycleTablet) == null) { cycleTablet = ItemStack.EMPTY; tabletTicks = 0; }
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? (side == null ? inventory : automation).cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); inventory.invalidate(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); inventory = LazyOptional.of(() -> items); automation = LazyOptional.of(this::automationHandler); }
    private IItemHandler automationHandler() {
        return new RangedWrapper(items, 0, 4) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 3 ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
        };
    }
    public void dropContents() {
        for (int i = 0; i < items.getSlots(); i++) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, items.getStackInSlot(i));
    }
}
