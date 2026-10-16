package net.jenkimods.bioforge.world.splicer;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.vaccine.VaccineResearchNotes;
import net.jenkimods.bioforge.vaccine.VaccineCorrectionNotes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import java.util.List;

public final class GeneSplicerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SAMPLE = 0, IMPRINT = 1, MEDIUM = 2, NOTES = 3, CATALYST = 4, OUTPUT = 5;
    private int progress, duration, status, selected, category;
    private int operation = -1;
    private String analyzing = "";
    private java.util.UUID operator;
    public final ItemStackHandler items = new ItemStackHandler(6) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SAMPLE -> stack.is(BioForge.VIRUS_SAMPLE.get()) || stack.is(BioForge.LIVE_CULTURE_VIAL.get()) || stack.is(BioForge.PETRI_DISH.get()) || stack.is(BioForge.SWAB.get());
                case IMPRINT -> stack.is(BioForge.GENE_IMPRINT.get());
                case MEDIUM -> stack.is(BioForge.NUTRIENT_MEDIUM.get());
                case NOTES -> stack.is(BioForge.CRISPR_NOTES.get());
                case CATALYST -> stack.is(BioForge.CATALYST_VIAL.get()) || stack.is(BioForge.SURFACTANT_CONCENTRATE.get()) || stack.is(BioForge.NUTRIENT_MEDIUM.get());
                default -> false;
            };
        }
        @Override public int getSlotLimit(int slot) { return slot == MEDIUM || slot == CATALYST ? 64 : 1; }
        @Override protected void onContentsChanged(int slot) {
            if (slot != OUTPUT) { operation = -1; progress = 0; analyzing = ""; }
            setChanged();
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            CompoundTag fixed = tag.copy(); fixed.putInt("Size", 6); super.deserializeNBT(fixed);
        }
    };
    private LazyOptional<ItemStackHandler> handler = LazyOptional.of(() -> items);
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return switch (index) { case 0 -> progress; case 1 -> duration; case 2 -> status; case 3 -> selected; case 4 -> category; case 5 -> availableActions(); default -> 0; }; }
        public void set(int index, int value) { switch (index) { case 0 -> progress = value; case 1 -> duration = value; case 2 -> status = value; case 3 -> selected = value; case 4 -> category = value; } }
        public int getCount() { return 6; }
    };
    public GeneSplicerBlockEntity(BlockPos pos, BlockState state) { super(BioForge.GENE_SPLICER_BE.get(), pos, state); }
    private ItemStack sample() { return items.getStackInSlot(SAMPLE); }
    private StrainData strain() { return StrainSampleUtil.getStrain(sample()); }
    private List<SplicerCulture.Gene> genes() { return SplicerCulture.genes(strain(), VaccineTargetCategory.values()[category]); }
    private SplicerCulture.Gene gene() { var genes = genes(); return genes.isEmpty() ? null : genes.get(Math.floorMod(selected, genes.size())); }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneSplicerBlockEntity machine) {
        if (!level.isClientSide) machine.serverTick();
    }
    private void serverTick() {
        StrainData strain = strain();
        if (strain == null || strain.getPathogenId() == null || sample().getCount() != 1) {
            status = 0; progress = 0; operation = -1; analyzing = ""; return;
        }
        if (operation == 6 || operation == 7) { advanceOperation(); return; }
        if (!SplicerCulture.analyzed(sample())) {
            String canonical = strain.toCanonicalGeneticPayload();
            if (!canonical.equals(analyzing)) { progress = 0; analyzing = canonical; }
            status = 1; duration = 600;
            ItemStack notes = items.getStackInSlot(NOTES);
            net.jenkimods.bioforge.audio.ActionAudio.work(this,"gene_splicer");
            progress += VaccineResearchNotes.matchesSample(notes, strain.toPayload())
                    || VaccineCorrectionNotes.matchesSample(notes, strain.toPayload()) ? 3 : 1;
            if (progress >= duration) { SplicerCulture.analyze(sample()); progress = 0; status = 2; net.jenkimods.bioforge.audio.ActionAudio.complete(this,"gene_splicer"); }
            setChanged();
            return;
        }
        if (operation < 0) { status = 2; return; }
        advanceOperation();
    }
    private void advanceOperation() {
        if (!canOperate(operation)) { operation = -1; progress = 0; status = 2; return; }
        status = operation == 6 ? 6 : operation == 7 ? 7 : operation + 3;
        net.jenkimods.bioforge.audio.ActionAudio.work(this,"gene_splicer");
        if (++progress >= duration) complete();
        setChanged();
    }
    private int availableActions() {
        if (operation >= 0) return 0;
        int result = 0;
        for (int id : new int[]{0, 1, 2, 6, 7}) if (canOperate(id)) result |= 1 << id;
        return result;
    }
    public boolean button(int id) {
        if (id == 3 || id == 4 || id == 5) {
            if (operation >= 0 || !SplicerCulture.analyzed(sample())) return false;
            if (id == 3) { category = (category + 1) % 3; selected = 0; }
            else { int size = genes().size(); selected = size == 0 ? 0 : Math.floorMod(selected + (id == 4 ? -1 : 1), size); }
            setChanged(); return true;
        }
        if (operation >= 0 || !canOperate(id)) return false;
        operation = id; progress = 0; duration = id == 0 ? 120 : id == 7 ? 100 : 200;
        setChanged(); return true;
    }
    public boolean canOperate(int id) {
        StrainData strain = strain();
        if (strain == null || strain.getPathogenId() == null || sample().getCount() != 1) return false;
        if (id == 6) return items.getStackInSlot(MEDIUM).is(BioForge.NUTRIENT_MEDIUM.get())
                && (SplicerCulture.biomass(sample()) < 3 || SplicerCulture.stability(sample()) < 100);
        var selectedCategory = VaccineTargetCategory.values()[category];
        if (id == 7) return SplicerCulture.analyzed(sample()) && !SplicerCulture.examined(sample(), selectedCategory)
                && items.getStackInSlot(CATALYST).is(examinationReagent(selectedCategory));
        if (id < 0 || id > 2 || !SplicerCulture.analyzed(sample()) || !items.getStackInSlot(OUTPUT).isEmpty()
                || SplicerCulture.biomass(sample()) < 1 || SplicerCulture.stability(sample()) < (id == 0 ? 30 : 60)) return false;
        ItemStack imprint = items.getStackInSlot(IMPRINT);
        var inserted = GeneImprintItem.read(imprint);
        if (!SplicerCulture.examined(sample(), id == 1 && inserted != null ? inserted.category() : selectedCategory)) return false;
        if (id == 0) return GeneImprintItem.isBlank(imprint) && gene() != null;
        if (!items.getStackInSlot(CATALYST).is(BioForge.CATALYST_VIAL.get())) return false;
        if (id == 1) return SplicerCulture.insert(strain, GeneImprintItem.read(imprint));
        return GeneImprintItem.isBlank(imprint) && gene() != null && SplicerCulture.remove(strain, gene());
    }
    public boolean button(int id, Player player) {
        if (!button(id)) return false;
        if (id == 0 || id == 1 || id == 2 || id == 6 || id == 7) operator = player.getUUID();
        return true;
    }
    public static net.minecraft.world.item.Item examinationReagent(VaccineTargetCategory category) {
        return switch (category) {
            case MUTATION -> BioForge.SURFACTANT_CONCENTRATE.get();
            case TRANSMISSION -> BioForge.CATALYST_VIAL.get();
            case SYMPTOM -> BioForge.NUTRIENT_MEDIUM.get();
        };
    }
    private void complete() {
        int action = operation;
        if (!canOperate(action)) { operation = -1; progress = 0; return; }
        ItemStack sample = sample();
        if (action == 7) {
            SplicerCulture.examine(sample, VaccineTargetCategory.values()[category]);
            items.extractItem(CATALYST, 1, false);
            net.jenkimods.bioforge.progression.MedicineMilestones.award((net.minecraft.server.level.ServerLevel) level,
                    operator, "gene_study", VaccineTargetCategory.values()[category].serializedName());
        } else if (action == 6) {
            SplicerCulture.reserves(sample, SplicerCulture.biomass(sample) + 1, SplicerCulture.stability(sample) + 25);
            items.extractItem(MEDIUM, 1, false);
        } else {
            StrainData strain = strain();
            ItemStack result = new ItemStack(BioForge.GENE_IMPRINT.get());
            if (action == 0 || action == 2) {
                SplicerCulture.Gene gene = gene();
                if (!GeneImprintItem.captureIdentified(result, strain, gene.category(), gene.target())) return;
                if (action == 2 && !SplicerCulture.remove(strain, gene)) return;
            } else if (!SplicerCulture.insert(strain, GeneImprintItem.read(items.getStackInSlot(IMPRINT)))) return;
            SplicerCulture.reserves(sample, SplicerCulture.biomass(sample) - 1, SplicerCulture.stability(sample) - (action == 0 ? 10 : 25));
            if (action != 0) { SplicerCulture.writeStrain(sample, strain); items.extractItem(CATALYST, 1, false); }
            items.extractItem(IMPRINT, 1, false);
            items.setStackInSlot(OUTPUT, result);
            if (action == 1 || action == 2) net.jenkimods.bioforge.progression.MedicineMilestones.award(
                    (net.minecraft.server.level.ServerLevel) level, operator, "engineered_pathogen", "edited");
        }
        operator = null;
        operation = -1; progress = 0; selected = 0; status = 2;
        net.jenkimods.bioforge.audio.ActionAudio.complete(this,"gene_splicer");
        setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.bioforge.gene_splicer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GeneSplicerMenu(id, inventory, this, data); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Inventory", items.serializeNBT()); tag.putInt("AnalysisProgress", operation < 0 ? progress : 0);
        net.jenkimods.bioforge.util.NbtObfuscator.writeStringDeterministic(tag, "splicer_analysis", analyzing);
        tag.remove("Analyzing");
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items.deserializeNBT(tag.getCompound("Inventory")); progress = Math.max(0, Math.min(599, tag.getInt("AnalysisProgress")));
        var encoded = net.jenkimods.bioforge.util.NbtObfuscator.readString(tag, "splicer_analysis");
        analyzing = encoded == null ? tag.getString("Analyzing") : encoded; operation = -1;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        // Manual research: avoid automating sample removal halfway through an operation.
        return cap == ForgeCapabilities.ITEM_HANDLER && side == null ? handler.cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); handler.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); handler = LazyOptional.of(() -> items); }
    public void dropContents() {
        for (int slot = 0; slot < items.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, items.getStackInSlot(slot));
    }
}
