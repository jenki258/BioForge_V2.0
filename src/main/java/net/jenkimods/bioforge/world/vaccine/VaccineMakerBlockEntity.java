package net.jenkimods.bioforge.world.vaccine;

import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.BioForgeResearchData;
import net.jenkimods.bioforge.crispr.CrisprGuideProfile;
import net.jenkimods.bioforge.crispr.StrainSampleUtil;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.infection.StrainData;
import net.jenkimods.bioforge.item.crispr.CrisprCartridgeItem;
import net.jenkimods.bioforge.item.crispr.CasModuleItem;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.item.vaccine.VaccineItem;
import net.jenkimods.bioforge.mutation.MutationDefinition;
import net.jenkimods.bioforge.mutation.MutationLoader;
import net.jenkimods.bioforge.mutation.network.MutationNetworkHandler;
import net.jenkimods.bioforge.mutation.network.MutationSlotPacket;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.jenkimods.bioforge.vaccine.DirectedVaccineProfile;
import net.jenkimods.bioforge.vaccine.DirectedVaccineAction;
import net.jenkimods.bioforge.vaccine.MedicalReportStrainBinding;
import net.jenkimods.bioforge.vaccine.VaccineHostProfile;
import net.jenkimods.bioforge.vaccine.VaccineProfile;
import net.jenkimods.bioforge.vaccine.VaccineResearchNotes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class VaccineMakerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CARTRIDGE_START = 0;
    public static final int CARTRIDGE_END = 15;
    public static final int CAS_SLOT = 15;
    public static final int SAMPLE_SLOT = 16;
    public static final int CARRIER_SLOT = 17;
    public static final int REAGENT_SLOT = 18;
    public static final int OUTPUT_SLOT = 19;

    public static final int REPORT_SLOT = 20;
    public static final int SLOT_COUNT = 21;
    public static final int SYNTHESIZE_BUTTON = 250;
    public static final int RESEARCH_BUTTON = 251;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot >= CARTRIDGE_START && slot < CARTRIDGE_END) {
                ItemStack stack = getStackInSlot(slot);
                if (stack.getItem() instanceof CrisprCartridgeItem) {
                    ResourceLocation profile = activeProfileId();
                    CrisprCartridgeItem.assign(stack, slot, profile);
                }
            }
            if (slot == CAS_SLOT && getStackInSlot(slot).getItem() instanceof CasModuleItem) {
                ItemStack stack = getStackInSlot(slot);
                CasModuleItem.setModuleId(stack, CasModuleItem.getModuleId(stack));
            }
            if (!processingOutput && slot != OUTPUT_SLOT) {
                progress = 0;
                craftRequested = false;
                activeRecipeId = null;
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            if (slot >= CARTRIDGE_START && slot < CARTRIDGE_END) {
                return BioForgeResearchData.recipes().stream()
                        .filter(VaccineMakerRecipe::requiresProgram)
                        .anyMatch(recipe -> recipe.cartridge().test(stack));
            }
            if (slot == CAS_SLOT) {
                return BioForgeResearchData.recipes().stream()
                        .filter(VaccineMakerRecipe::requiresProgram)
                        .anyMatch(recipe -> recipe.casModule().test(stack));
            }
            if (slot == SAMPLE_SLOT) {
                return BioForgeResearchData.recipes().stream()
                        .anyMatch(recipe -> recipe.sample().test(stack));
            }
            if (slot == CARRIER_SLOT) {
                return BioForgeResearchData.recipes().stream()
                        .anyMatch(recipe -> recipe.carrier().test(stack));
            }
            if (slot == REAGENT_SLOT) {
                return stack.getItem() instanceof GeneImprintItem
                        || BioForgeResearchData.recipes().stream()
                        .anyMatch(recipe -> recipe.reagent().test(stack));
            }
            if (slot == REPORT_SLOT) {
                return stack.is(Items.PAPER) || stack.is(Items.WRITABLE_BOOK)
                        || stack.is(BioForge.MEDICAL_REPORT.get())
                        || stack.is(BioForge.CRISPR_NOTES.get());
            }
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private LazyOptional<ItemStackHandler> lazyHandler = LazyOptional.of(() -> items);
    private int progress;
    private int maxProgress = 200;
    private int qualityPermille;
    private int status;
    private boolean craftRequested;
    private boolean processingOutput;
    private boolean redstonePowered;
    private int failureTicks;
    @Nullable private ResourceLocation activeRecipeId;
    @Nullable private UUID operatorId;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> qualityPermille;
                case 3 -> status;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> qualityPermille = value;
                case 3 -> status = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public VaccineMakerBlockEntity(BlockPos pos, BlockState state) {
        super(BioForge.VACCINE_MAKER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            VaccineMakerBlockEntity maker) {
        if (level.isClientSide()) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != maker.redstonePowered) {
            maker.redstonePowered = powered;
            if (powered) maker.requestCraft(null);
            maker.setChanged();
        }
        if (maker.failureTicks > 0) {
            maker.failureTicks--;
            maker.status = 5;
            maker.progress = 0;
            return;
        }
        Optional<VaccineMakerRecipe> found = maker.findRecipe();
        if (found.isEmpty()) {
            maker.status = 0;
            maker.progress = 0;
            maker.craftRequested = false;
            maker.activeRecipeId = null;



            Optional<VaccineMakerRecipe> research = maker.findResearchRecipe();
            if (research.isPresent()) {
                VaccineMakerRecipe preview = research.get();
                maker.assignProgram(preview.guideProfile());
                maker.qualityPermille = Math.round(
                        maker.calculateQuality(preview) * 1000.0f);
                maker.maxProgress = preview.processingTime();
            } else {
                maker.qualityPermille = 0;
            }
            return;
        }

        VaccineMakerRecipe recipe = found.get();
        maker.assignProgram(recipe.guideProfile());
        float quality = maker.calculateQuality(recipe);
        maker.qualityPermille = Math.round(quality * 1000.0f);
        maker.maxProgress = recipe.processingTime();
        if (!maker.canAcceptOutput()) {
            maker.status = 4;
            maker.progress = 0;
            maker.craftRequested = false;
            return;
        }
        if (quality < recipe.minimumQuality()) {
            maker.status = 3;
            maker.progress = 0;
            maker.craftRequested = false;
            return;
        }
        if (!maker.craftRequested) {
            maker.status = 1;
            return;
        }
        if (maker.activeRecipeId != null && !maker.activeRecipeId.equals(recipe.id())) {
            maker.progress = 0;
        }
        maker.activeRecipeId = recipe.id();
        maker.status = 2;
        maker.progress++;
        if (maker.progress >= maker.maxProgress) {
            boolean completed = maker.process(recipe, quality);
            maker.progress = 0;
            maker.craftRequested = false;
            maker.activeRecipeId = null;
            maker.status = completed ? 1 : 5;
        }
        maker.setChanged();
    }

    public boolean handleButton(Player player, int buttonId) {
        if (buttonId == RESEARCH_BUTTON) {
            return handleResearch(player);
        }
        if (buttonId == SYNTHESIZE_BUTTON) {
            return requestCraft(player.getUUID());
        }
        boolean backwards = buttonId >= 64 && buttonId < 124;
        int encoded = backwards ? buttonId - 64 : buttonId;
        if (encoded < 0 || encoded >= 60) return false;
        int cartridgeSlot = encoded / 4;
        int base = encoded % 4;
        ItemStack stack = items.getStackInSlot(cartridgeSlot);
        if (!(stack.getItem() instanceof CrisprCartridgeItem)) return false;
        CrisprGuideProfile profile = BioForgeResearchData.guideProfile(
                activeProfileId()).orElse(null);
        String alphabet = profile == null ? "ACGU" : profile.alphabet();
        CrisprCartridgeItem.cycleBase(stack, base, backwards ? -1 : 1, alphabet);
        CrisprCartridgeItem.assign(stack, cartridgeSlot, activeProfileId());
        progress = 0;
        craftRequested = false;
        activeRecipeId = null;
        setChanged();
        return true;
    }

    private boolean requestCraft(@Nullable UUID requestedBy) {
        if (craftRequested || failureTicks > 0) return false;
        Optional<VaccineMakerRecipe> recipe = findRecipe();
        if (recipe.isEmpty() || !canAcceptOutput()) return false;
        float quality = calculateQuality(recipe.get());
        if (quality < recipe.get().minimumQuality()) return false;
        craftRequested = true;
        activeRecipeId = recipe.get().id();
        operatorId = requestedBy;
        progress = 0;
        setChanged();
        return true;
    }

    private boolean handleResearch(Player player) {
        ItemStack document = items.getStackInSlot(REPORT_SLOT);
        ItemStack reagent = items.getStackInSlot(REAGENT_SLOT);
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));

        VaccineResearchNotes.Data template = VaccineResearchNotes.read(document);
        if (template != null && VaccineResearchNotes.isTemplate(document)) {
            return applyResearchTemplate(player, template);
        }

        if (VaccineResearchNotes.canRecord(document)) {
            if (strain == null) return false;
            Optional<VaccineMakerRecipe> recipe = findResearchRecipe();
            if (recipe.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.bioforge.vaccine_maker.no_research_program"), true);
                return false;
            }
            VaccineMakerRecipe selected = recipe.get();
            assignProgram(selected.guideProfile());
            float quality = calculateQuality(selected);
            ItemStack recorded = VaccineResearchNotes.record(
                    document, quality, programmedSequence(), strain.toPayload(),
                    selected.id(), level == null ? 0L : level.getGameTime());
            if (recorded.isEmpty()) return false;
            processingOutput = true;
            items.setStackInSlot(REPORT_SLOT, recorded);
            processingOutput = false;
            setChanged();
            player.level().playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.BLOCKS, 0.7f, 1.1f);
            player.displayClientMessage(Component.translatable(
                    "message.bioforge.vaccine_maker.research_recorded"), true);
            return true;
        }

        if (GeneImprintItem.isBlank(reagent)) {
            if (strain == null || !GeneImprintItem.captureUnknown(
                    reagent, strain, player.getRandom())) {
                player.displayClientMessage(Component.translatable(
                        "message.bioforge.vaccine_maker.no_gene_target"), true);
                return false;
            }
            setChanged();
            player.level().playSound(null, worldPosition, SoundEvents.BOTTLE_FILL,
                    SoundSource.BLOCKS, 0.7f, 1.35f);
            player.displayClientMessage(Component.translatable(
                    "message.bioforge.vaccine_maker.gene_extracted"), true);
            return true;
        }
        return false;
    }

    private boolean applyResearchTemplate(Player player,
                                          VaccineResearchNotes.Data template) {
        if (!hasAllCartridges() || template.sequence().length() != 60) {
            player.displayClientMessage(Component.translatable(
                    "message.bioforge.vaccine_maker.template_needs_cartridges"), true);
            return false;
        }
        ResourceLocation templateRecipe = ResourceLocation.tryParse(template.recipeId());
        ResourceLocation profile = BioForgeResearchData.recipes().stream()
                .filter(candidate -> candidate.id().equals(templateRecipe))
                .map(VaccineMakerRecipe::guideProfile)
                .findFirst()
                .orElse(ResourceLocation.tryBuild(BioForge.MODID, "default"));
        for (int slot = CARTRIDGE_START; slot < CARTRIDGE_END; slot++) {
            ItemStack cartridge = items.getStackInSlot(slot);
            if (!(cartridge.getItem() instanceof CrisprCartridgeItem)) return false;
            int start = slot * 4;
            CrisprCartridgeItem.setSequence(
                    cartridge, template.sequence().substring(start, start + 4));
            CrisprCartridgeItem.assign(cartridge, slot, profile);
        }
        progress = 0;
        craftRequested = false;
        activeRecipeId = null;
        setChanged();
        player.level().playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN,
                SoundSource.BLOCKS, 0.8f, 1.25f);
        player.displayClientMessage(Component.translatable(
                "message.bioforge.vaccine_maker.template_applied"), true);
        return true;
    }

    private boolean hasAllCartridges() {
        for (int slot = CARTRIDGE_START; slot < CARTRIDGE_END; slot++) {
            if (!(items.getStackInSlot(slot).getItem() instanceof CrisprCartridgeItem)) {
                return false;
            }
        }
        return true;
    }

    private Optional<VaccineMakerRecipe> findRecipe() {
        ItemStack sample = items.getStackInSlot(SAMPLE_SLOT);
        ItemStack carrier = items.getStackInSlot(CARRIER_SLOT);
        ItemStack reagent = items.getStackInSlot(REAGENT_SLOT);
        ItemStack report = items.getStackInSlot(REPORT_SLOT);
        ItemStack cas = items.getStackInSlot(CAS_SLOT);
        return BioForgeResearchData.recipes().stream()
                .filter(recipe -> recipe.matches(sample, carrier, reagent, report, cas))
                .filter(this::additionalRequirements)
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .findFirst();
    }

    private Optional<VaccineMakerRecipe> findResearchRecipe() {
        ItemStack sample = items.getStackInSlot(SAMPLE_SLOT);
        ItemStack cas = items.getStackInSlot(CAS_SLOT);
        return BioForgeResearchData.recipes().stream()
                .filter(recipe -> recipe.operation() == VaccineMakerOperation.FULL)
                .filter(recipe -> recipe.sample().test(sample))
                .filter(recipe -> recipe.casModule().test(cas))
                .filter(this::programRequirements)
                .sorted(Comparator.comparing(VaccineMakerRecipe::requiresReport)
                        .thenComparing(recipe -> recipe.id().toString()))
                .findFirst();
    }

    private boolean programRequirements(VaccineMakerRecipe recipe) {
        if (!recipe.requiresProgram()) return true;
        for (int slot = CARTRIDGE_START; slot < CARTRIDGE_END; slot++) {
            if (!recipe.cartridge().test(items.getStackInSlot(slot))) return false;
        }
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        if (strain == null || strain.getPathogen() == null) return false;
        return BioForgeResearchData.casModule(
                        CasModuleItem.getModuleId(items.getStackInSlot(CAS_SLOT)))
                .filter(module -> module.isCompatible(
                        recipe.guideProfile(), strain.getPathogen()))
                .isPresent();
    }

    private boolean additionalRequirements(VaccineMakerRecipe recipe) {
        if (!programRequirements(recipe)) return false;
        return switch (recipe.operation()) {
            case FULL -> fullRequirements(recipe);
            case RANDOM_MUTATION -> fullRequirements(recipe);
            case DIRECTED -> directedRequirements(recipe);
            case CLONE -> VaccineProfile.read(items.getStackInSlot(SAMPLE_SLOT)) != null
                    || DirectedVaccineProfile.read(items.getStackInSlot(SAMPLE_SLOT)) != null
                    ? isBlankCarrier(items.getStackInSlot(CARRIER_SLOT))
                    && VaccineItem.kindOf(items.getStackInSlot(SAMPLE_SLOT))
                    == VaccineItem.kindOf(items.getStackInSlot(CARRIER_SLOT))
                    : false;
        };
    }

    private boolean fullRequirements(VaccineMakerRecipe recipe) {
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        if (strain == null
                || VaccineProfile.read(items.getStackInSlot(CARRIER_SLOT)) != null
                || DirectedVaccineProfile.read(items.getStackInSlot(CARRIER_SLOT)) != null) {
            return false;
        }
        boolean requiresClinicalResearch = recipe.findingBonus() > 0.0f
                || recipe.completeBloodBonus() > 0.0f || recipe.requiresReport();
        ItemStack report = items.getStackInSlot(REPORT_SLOT);
        return !requiresClinicalResearch || MedicalReportStrainBinding.matchesSample(
                report, strain.toPayload());
    }

    private boolean directedRequirements(VaccineMakerRecipe recipe) {
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        GeneImprintItem.Data imprint =
                GeneImprintItem.read(items.getStackInSlot(REAGENT_SLOT));
        VaccineTargetCategory category = recipe.fixedDirectedCategory() != null
                ? recipe.fixedDirectedCategory()
                : imprint == null ? null : imprint.category();
        if (strain == null || imprint == null || !imprint.identified()
                || category == null || recipe.directedResult(category) == null
                || recipe.directedAction(category) == null) return false;
        DirectedVaccineAction action =
                BioForgeResearchData.action(recipe.directedAction(category)).orElse(null);
        if (action == null || !action.supports(category)) return false;
        if (recipe.fixedDirectedCategory() != null) {
            if (action.targetOverride().isBlank()
                    && (imprint == null || imprint.category() != category)) return false;
            return isBlankCarrier(items.getStackInSlot(CARRIER_SLOT))
                    && carrierMatchesCategory(items.getStackInSlot(CARRIER_SLOT), category);
        }
        StrainData imprintStrain = StrainData.parse(imprint.strainPayload());
        return strain.getPathogen() != null && strain.getPathogen() == imprintStrain.getPathogen()
                && isBlankCarrier(items.getStackInSlot(CARRIER_SLOT))
                && carrierMatchesCategory(items.getStackInSlot(CARRIER_SLOT), category);
    }

    private static boolean isBlankCarrier(ItemStack stack) {
        return VaccineProfile.read(stack) == null && DirectedVaccineProfile.read(stack) == null;
    }

    private static boolean carrierMatchesCategory(ItemStack stack,
                                                   VaccineTargetCategory category) {
        VaccineItem.Kind expected = switch (category) {
            case MUTATION -> VaccineItem.Kind.MUTATION;
            case TRANSMISSION -> VaccineItem.Kind.TRANSMISSION;
            case SYMPTOM -> VaccineItem.Kind.SYMPTOM;
        };
        return VaccineItem.kindOf(stack) == expected;
    }

    private float calculateQuality(VaccineMakerRecipe recipe) {
        if (recipe.operation() == VaccineMakerOperation.CLONE) {
            VaccineProfile full = VaccineProfile.read(items.getStackInSlot(SAMPLE_SLOT));
            if (full != null) return full.quality();
            DirectedVaccineProfile directed =
                    DirectedVaccineProfile.read(items.getStackInSlot(SAMPLE_SLOT));
            return directed == null ? 0.0f : directed.quality();
        }
        float rawQuality = calculateRawQuality(recipe);
        float cap = recipe.baseQualityCap();
        ItemStack reagent = items.getStackInSlot(REAGENT_SLOT);
        ItemStack report = items.getStackInSlot(REPORT_SLOT);
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        VaccineHostProfile medical = strain != null
                && MedicalReportStrainBinding.matchesSample(report, strain.toPayload())
                ? VaccineHostProfile.fromMedicalReport(report) : null;
        if (medical != null) {
            cap += medical.findings() * recipe.findingBonus();
            if (medical.bloodVerified()) cap += recipe.completeBloodBonus();
        }
        if (GeneImprintItem.isIdentified(reagent)) {
            cap += recipe.identifiedImprintBonus();
        }
        return Math.min(rawQuality, Math.max(0.0f, Math.min(1.0f, cap)));
    }

    private float calculateRawQuality(VaccineMakerRecipe recipe) {
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        CrisprGuideProfile profile =
                BioForgeResearchData.guideProfile(recipe.guideProfile()).orElse(null);
        if (strain == null || profile == null) return 0.0f;
        String expected = profile.deriveSequence(strain);
        float casEfficiency = BioForgeResearchData.casModule(
                        CasModuleItem.getModuleId(items.getStackInSlot(CAS_SLOT)))
                .filter(module -> module.isCompatible(
                        recipe.guideProfile(), strain.getPathogen()))
                .map(module -> module.efficiency()).orElse(0.0f);
        float points = (recipe.sample().test(items.getStackInSlot(SAMPLE_SLOT))
                ? recipe.sampleWeight() : 0.0f)
                + (recipe.carrier().test(items.getStackInSlot(CARRIER_SLOT))
                ? recipe.carrierWeight() : 0.0f)
                + (recipe.reagent().test(items.getStackInSlot(REAGENT_SLOT))
                ? recipe.reagentWeight() : 0.0f)
                + recipe.casWeight() * casEfficiency;
        for (int guide = 0; guide < 3; guide++) {
            int matches = 0;
            int startCartridge = guide * 5;
            for (int local = 0; local < 5; local++) {
                String actual = CrisprCartridgeItem.getSequence(
                        items.getStackInSlot(startCartridge + local));
                int sequenceStart = (startCartridge + local) * 4;
                for (int base = 0; base < 4; base++) {
                    if (sequenceStart + base < expected.length()
                            && actual.charAt(base) == expected.charAt(sequenceStart + base)) {
                        matches++;
                    }
                }
            }
            points += recipe.guideWeights()[guide] * matches / 20.0f;
        }
        float total = recipe.totalWeight();
        return total <= 0.0f ? 0.0f : Math.max(0.0f, Math.min(1.0f, points / total));
    }

    private String programmedSequence() {
        StringBuilder result = new StringBuilder(60);
        for (int slot = CARTRIDGE_START; slot < CARTRIDGE_END; slot++) {
            result.append(CrisprCartridgeItem.getSequence(items.getStackInSlot(slot)));
        }
        return result.toString();
    }

    private ResourceLocation activeProfileId() {
        return findRecipe().map(VaccineMakerRecipe::guideProfile)
                .or(() -> findResearchRecipe().map(VaccineMakerRecipe::guideProfile))
                .orElse(ResourceLocation.tryBuild(BioForge.MODID, "default"));
    }

    private void assignProgram(ResourceLocation profile) {
        for (int slot = CARTRIDGE_START; slot < CARTRIDGE_END; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (stack.getItem() instanceof CrisprCartridgeItem) {
                CrisprCartridgeItem.assign(stack, slot, profile);
            }
        }
    }

    private boolean canAcceptOutput() {
        return items.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    private boolean process(VaccineMakerRecipe recipe, float quality) {
        if (attemptSynthesisMutation(recipe, calculateRawQuality(recipe))) {
            return false;
        }
        ItemStack output = switch (recipe.operation()) {
            case FULL -> createFullVaccine(recipe, quality);
            case RANDOM_MUTATION -> createFullVaccine(recipe, quality);
            case DIRECTED -> createDirectedVaccine(recipe, quality);
            case CLONE -> cloneVaccine();
        };
        if (output.isEmpty()) return false;
        processingOutput = true;
        items.setStackInSlot(OUTPUT_SLOT, output);
        if (recipe.consumeSample()) items.extractItem(SAMPLE_SLOT, 1, false);
        items.extractItem(CARRIER_SLOT, 1, false);
        if (recipe.consumeReagent()) items.extractItem(REAGENT_SLOT, 1, false);
        if (recipe.consumeReport()) items.extractItem(REPORT_SLOT, 1, false);
        processingOutput = false;
        return true;
    }

    private boolean attemptSynthesisMutation(VaccineMakerRecipe recipe, float rawQuality) {
        if (level == null || rawQuality >= recipe.errorMutationThreshold()
                || recipe.errorMutationChance() <= 0.0f) {
            return false;
        }
        float threshold = Math.max(0.001f, recipe.errorMutationThreshold());
        float severity = Math.max(0.0f, Math.min(1.0f,
                (threshold - rawQuality) / threshold));
        if (level.random.nextFloat() >= recipe.errorMutationChance() * severity) return false;

        ItemStack sampleStack = items.getStackInSlot(SAMPLE_SLOT);
        StrainData strain = StrainSampleUtil.getStrain(sampleStack);
        if (strain == null || strain.getPathogen() == null) return false;
        List<MutationDefinition> candidates = MutationLoader.INSTANCE
                .getMutationsForPathogen(strain.getPathogen()).stream()
                .filter(MutationDefinition::enabled)
                .filter(definition -> definition.weight() > 0)
                .filter(definition -> definition.isCompatible(strain.getPathogen()))
                .filter(definition -> strain.getMutationIds()
                        .containsAll(definition.requiredMutations()))
                .filter(definition -> definition.conflictingMutations().stream()
                        .noneMatch(strain.getMutationIds()::contains))
                .filter(definition -> !strain.getMutationIds().contains(definition.id()))
                .toList();
        MutationDefinition selected = MutationLoader.INSTANCE.chooseWeighted(
                candidates, new Random(level.random.nextLong()));
        if (selected == null) return false;

        strain.applyMutationInVitro(selected);
        NbtObfuscator.writeString(sampleStack.getOrCreateTag(), strain.toPayload());
        if (recipe.consumeReagentOnMutation()) {
            items.extractItem(REAGENT_SLOT, 1, false);
        }
        failureTicks = 60;
        setChanged();

        if (level instanceof ServerLevel serverLevel && operatorId != null) {
            ServerPlayer player = serverLevel.getServer()
                    .getPlayerList().getPlayer(operatorId);
            if (player != null) {
                MutationNetworkHandler.sendToPlayer(
                        MutationSlotPacket.forMutation(selected.id()), player);
                player.displayClientMessage(Component.translatable(
                        "message.bioforge.vaccine_maker.sequence_mutated"), true);
            }
        }
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS, 0.9f, 0.65f);
        return true;
    }

    private ItemStack createFullVaccine(VaccineMakerRecipe recipe, float quality) {
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        if (strain == null || recipe.fullResult() == null) return ItemStack.EMPTY;
        ItemStack output = new ItemStack(recipe.fullResult());
        new VaccineProfile(strain.toPayload(), quality, recipe.uses(), recipe.defenseRisk(),
                UUID.randomUUID(), level == null ? 0L : level.getGameTime()).write(output);
        ItemStack report = items.getStackInSlot(REPORT_SLOT);
        VaccineHostProfile host = MedicalReportStrainBinding.matchesSample(
                report, strain.toPayload())
                ? VaccineHostProfile.fromMedicalReport(report) : null;
        if (host != null) host.write(output);
        return output;
    }

    private ItemStack createDirectedVaccine(VaccineMakerRecipe recipe, float quality) {
        StrainData strain = StrainSampleUtil.getStrain(items.getStackInSlot(SAMPLE_SLOT));
        GeneImprintItem.Data imprint =
                GeneImprintItem.read(items.getStackInSlot(REAGENT_SLOT));
        VaccineTargetCategory category = recipe.fixedDirectedCategory() != null
                ? recipe.fixedDirectedCategory()
                : imprint == null ? null : imprint.category();
        if (strain == null || category == null) return ItemStack.EMPTY;
        Item outputItem = recipe.directedResult(category);
        ResourceLocation actionId = recipe.directedAction(category);
        if (outputItem == null || actionId == null) return ItemStack.EMPTY;
        DirectedVaccineAction action =
                BioForgeResearchData.action(actionId).orElse(null);
        if (action == null || !action.supports(category)) return ItemStack.EMPTY;
        String target = action.targetOverride().isBlank()
                ? imprint == null ? "" : imprint.target()
                : action.targetOverride();
        if (target.isBlank()) return ItemStack.EMPTY;
        ItemStack output = new ItemStack(outputItem);
        new DirectedVaccineProfile(
                strain.toPayload(), category, target, actionId,
                quality, recipe.uses(), recipe.defenseRisk(), UUID.randomUUID(),
                level == null ? 0L : level.getGameTime()).write(output);
        return output;
    }

    private ItemStack cloneVaccine() {
        ItemStack source = items.getStackInSlot(SAMPLE_SLOT);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack output = source.copy();
        output.setCount(1);
        return output;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.bioforge.vaccine_maker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VaccineMakerMenu(id, inventory, this, data);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(capability, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", items.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putBoolean("CraftRequested", craftRequested);
        tag.putBoolean("RedstonePowered", redstonePowered);
        tag.putInt("FailureTicks", failureTicks);
        if (activeRecipeId != null) tag.putString("ActiveRecipe", activeRecipeId.toString());
        if (operatorId != null) tag.putUUID("Operator", operatorId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag inventory = tag.getCompound("Inventory").copy();


        if (inventory.getInt("Size") < SLOT_COUNT) {
            inventory.putInt("Size", SLOT_COUNT);
        }
        items.deserializeNBT(inventory);
        progress = tag.getInt("Progress");
        craftRequested = tag.getBoolean("CraftRequested");
        redstonePowered = tag.getBoolean("RedstonePowered");
        failureTicks = tag.getInt("FailureTicks");
        activeRecipeId = ResourceLocation.tryParse(tag.getString("ActiveRecipe"));
        operatorId = tag.hasUUID("Operator") ? tag.getUUID("Operator") : null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        lazyHandler = LazyOptional.of(() -> items);
    }

    public void drops() {
        if (level == null) return;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
            }
        }
    }
}
