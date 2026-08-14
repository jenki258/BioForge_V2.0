package net.jenkimods.bioforge;

import com.mojang.logging.LogUtils;
import net.jenkimods.bioforge.block.*;
import net.jenkimods.bioforge.client.CentrifugeScreen;
import net.jenkimods.bioforge.blood.network.NetworkHandler;
import net.jenkimods.bioforge.client.IncubatorScreen;
import net.jenkimods.bioforge.client.LaboratoryProcessorScreen;
import net.jenkimods.bioforge.client.MicroscopeScreen;
import net.jenkimods.bioforge.client.VaccineMakerScreen;
import net.jenkimods.bioforge.client.render.CentrifugeBlockEntityRenderer;
import net.jenkimods.bioforge.client.render.MicroscopeBlockEntityRenderer;
import net.jenkimods.bioforge.api.vaccine.VaccineMakerPageRegistry;
import net.jenkimods.bioforge.api.guide.ResearchJournalRegistry;
import net.jenkimods.bioforge.api.behavior.BioForgeBehaviorRegistry;
import net.jenkimods.bioforge.config.BioForgeServerConfig;
import net.jenkimods.bioforge.command.BioForgeTestCommand;
import net.jenkimods.bioforge.definition.BioForgeDefinitionManager;
import net.jenkimods.bioforge.definition.BioForgeValidateCommand;
import net.jenkimods.bioforge.definition.BioForgeDefinitionCommand;
import net.jenkimods.bioforge.crispr.command.CrisprCommand;
import net.jenkimods.bioforge.infection.command.InfectCommand;
import net.jenkimods.bioforge.infection.command.InfectionInvulnerabilityCommand;
import net.jenkimods.bioforge.infection.command.DecontaminationCommand;
import net.jenkimods.bioforge.infection.command.StrainCommand;
import net.jenkimods.bioforge.infection.network.InfectionNetworkHandler;
import net.jenkimods.bioforge.infection.naming.StrainNameNetworkHandler;
import net.jenkimods.bioforge.infection.spread.AirborneReservoirManager;
import net.jenkimods.bioforge.infection.spread.TransmissionEngine;
import net.jenkimods.bioforge.item.bone_saw.BoneSawItem;
import net.jenkimods.bioforge.item.AreaContaminationScannerItem;
import net.jenkimods.bioforge.item.bones.BoneMarrowItem;
import net.jenkimods.bioforge.item.bones.SplitBoneItem;
import net.jenkimods.bioforge.item.bones.WitheredBoneMarrowItem;
import net.jenkimods.bioforge.item.bones.WitheredSplitBoneItem;
import net.jenkimods.bioforge.item.clipboard.ClipboardItem;
import net.jenkimods.bioforge.item.clipboard.MedicalReportItem;
import net.jenkimods.bioforge.item.incubating.DirtyCultureVialItem;
import net.jenkimods.bioforge.item.incubating.LiveCultureVialItem;
import net.jenkimods.bioforge.item.incubating.NutrientMediumItem;
import net.jenkimods.bioforge.item.incubating.VirusSampleItem;
import net.jenkimods.bioforge.item.crispr.CasModuleItem;
import net.jenkimods.bioforge.item.crispr.CrisprCartridgeItem;
import net.jenkimods.bioforge.item.crispr.CrisprNotesItem;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.item.reagents.EthanolItem;
import net.jenkimods.bioforge.item.reagents.WipeItem;
import net.jenkimods.bioforge.item.infection.*;
import net.jenkimods.bioforge.item.guide.ResearchJournalItem;
import net.jenkimods.bioforge.item.guide.ResearchJournalNetwork;
import net.jenkimods.bioforge.item.guide.ResearchJournalCommand;
import net.jenkimods.bioforge.item.needle.NeedleItem;
import net.jenkimods.bioforge.item.needle.SyringeItem;
import net.jenkimods.bioforge.item.otoscope.OtoscopeItem;
import net.jenkimods.bioforge.item.otoscope.OtoscopeNetworkHandler;
import net.jenkimods.bioforge.item.pulse_oximeter.PulseOximeterItem;
import net.jenkimods.bioforge.item.pulse_oximeter.PulseOximeterNetworkHandler;
import net.jenkimods.bioforge.item.protective.BioForgeArmorMaterial;
import net.jenkimods.bioforge.item.protective.ProtectiveGearItem;
import net.jenkimods.bioforge.item.reagents.CatalystVialItem;
import net.jenkimods.bioforge.item.reagents.DecalcificationFluidItem;
import net.jenkimods.bioforge.item.reagents.DiagnosticReagentItem;
import net.jenkimods.bioforge.item.reagents.DecontaminationFlaskItem;
import net.jenkimods.bioforge.item.reagents.ReagentVialItem;
import net.jenkimods.bioforge.item.reflex_hammer.ReflexHammerItem;
import net.jenkimods.bioforge.item.reflex_hammer.ReflexHammerNetworkHandler;
import net.jenkimods.bioforge.item.otoscope.MirrorItem;
import net.jenkimods.bioforge.item.samples.BloodSlideItem;
import net.jenkimods.bioforge.item.samples.CellPelletItem;
import net.jenkimods.bioforge.item.samples.PlasmaSampleItem;
import net.jenkimods.bioforge.item.samples.TubeItem;
import net.jenkimods.bioforge.item.stethoscope.StethoscopeItem;
import net.jenkimods.bioforge.item.stethoscope.StethoscopeNetworkHandler;
import net.jenkimods.bioforge.item.thermometer.ThermometerItem;
import net.jenkimods.bioforge.item.thermometer.ThermometerNetworkHandler;
import net.jenkimods.bioforge.item.vaccine.VaccineItem;
import net.jenkimods.bioforge.item.vaccine.ResistancePillItem;
import net.jenkimods.bioforge.item.vaccine.SymptomTabletItem;
import net.jenkimods.bioforge.mutation.command.MutateCommand;
import net.jenkimods.bioforge.mutation.LegacyMutationBehaviors;
import net.jenkimods.bioforge.mutation.network.MutationNetworkHandler;
import net.jenkimods.bioforge.registry.BFCreativeTabs;
import net.jenkimods.bioforge.registry.BioForgeSounds;
import net.jenkimods.bioforge.registry.BioForgeEffects;
import net.jenkimods.bioforge.vaccine.command.VaccineMakeCommand;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeBlockEntity;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeMenu;
import net.jenkimods.bioforge.world.incubator.IncubatorBlockEntity;
import net.jenkimods.bioforge.world.incubator.IncubatorMenu;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessRecipeManager;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessorBlockEntity;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessorMenu;
import net.jenkimods.bioforge.world.laboratory.LaboratoryStation;
import net.jenkimods.bioforge.world.microscope.MicroscopeBlockEntity;
import net.jenkimods.bioforge.world.microscope.MicroscopeMenu;
import net.jenkimods.bioforge.world.microscope.MicroscopeNetwork;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerBlockEntity;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerCorrectionNetwork;
import net.jenkimods.bioforge.world.vaccine.VaccineMakerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import static net.minecraftforge.registries.ForgeRegistries.MENU_TYPES;

@Mod(BioForge.MODID)
public class BioForge {
    public static final String MODID = "bioforge";
    public static final String MOD_NAME = "BioForge";
    public static final String VERSION = "2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(MENU_TYPES, MODID);

    public static final RegistryObject<Item> ACTIVATED_CARBON = ITEMS.register(
            "activated_carbon", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LABORATORY_WASTE = ITEMS.register(
            "laboratory_waste", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_STEEL_BLEND = ITEMS.register(
            "black_steel_blend", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_STEEL_INGOT = ITEMS.register(
            "black_steel_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_STEEL_NUGGET = ITEMS.register(
            "black_steel_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_STEEL_PLATE = ITEMS.register(
            "black_steel_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REINFORCED_GLASS = ITEMS.register(
            "reinforced_glass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AGAR_POWDER = ITEMS.register(
            "agar_powder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFURIC_ACID = ITEMS.register(
            "sulfuric_acid", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> STERILIZING_SOLUTION = ITEMS.register(
            "sterilizing_solution", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> POLYMER_RESIN = ITEMS.register(
            "polymer_resin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STERILE_POLYMER_SHEET = ITEMS.register(
            "sterile_polymer_sheet", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LABORATORY_GLASSWARE = ITEMS.register(
            "laboratory_glassware", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STERILE_FILTER = ITEMS.register(
            "sterile_filter", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> OPTICAL_LENS = ITEMS.register(
            "optical_lens", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRECISION_MECHANISM = ITEMS.register(
            "precision_mechanism", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ELECTRONIC_CONTROL_UNIT = ITEMS.register(
            "electronic_control_unit", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LABORATORY_FRAME = ITEMS.register(
            "laboratory_frame", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BIOMEDICAL_PROCESSOR = ITEMS.register(
            "biomedical_processor", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NEUTRALIZING_AGENT = ITEMS.register(
            "neutralizing_agent", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SURFACTANT_CONCENTRATE = ITEMS.register(
            "surfactant_concentrate", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DECONTAMINATION_FLASK = ITEMS.register(
            "decontamination_flask", DecontaminationFlaskItem::new);
    public static final RegistryObject<Item> SEALED_BIOFABRIC = ITEMS.register(
            "sealed_biofabric", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STERILE_RUBBER = ITEMS.register(
            "sterile_rubber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ACTIVATED_FILTER = ITEMS.register(
            "activated_filter", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RESPIRATOR_VALVE = ITEMS.register(
            "respirator_valve", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> THERMAL_GEL = ITEMS.register(
            "thermal_gel", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INSULATED_LINING = ITEMS.register(
            "insulated_lining", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_STEEL_MESH = ITEMS.register(
            "black_steel_mesh", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHEMICAL_RESISTANT_COATING = ITEMS.register(
            "chemical_resistant_coating", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> AIRTIGHT_SEAL = ITEMS.register(
            "airtight_seal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WINE_MUST = ITEMS.register(
            "wine_must", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MEDICAL_MASK = ITEMS.register(
            "medical_mask", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.PROTECTIVE, ArmorItem.Type.HELMET,
                    new Item.Properties(), "item.bioforge.protective_gear.mask",
                    ProtectiveGearItem.WearableStyle.MEDICAL_MASK,
                    "bioforge:textures/models/armor/medical_mask.png"));
    public static final RegistryObject<Item> PROTECTIVE_GLOVES = ITEMS.register(
            "protective_gloves", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.PROTECTIVE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), "item.bioforge.protective_gear.gloves",
                    ProtectiveGearItem.WearableStyle.PROTECTIVE_GLOVES,
                    "bioforge:textures/models/armor/protective_gloves.png"));
    public static final RegistryObject<Item> ICE_BAG = ITEMS.register(
            "ice_bag", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.PROTECTIVE, ArmorItem.Type.HELMET,
                    new Item.Properties(), "item.bioforge.protective_gear.ice_bag",
                    ProtectiveGearItem.WearableStyle.THERMAL_BAG,
                    "bioforge:textures/models/armor/ice_bag.png"));
    public static final RegistryObject<Item> MAGMA_BAG = ITEMS.register(
            "magma_bag", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.PROTECTIVE, ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant(),
                    "item.bioforge.protective_gear.magma_bag",
                    ProtectiveGearItem.WearableStyle.THERMAL_BAG,
                    "bioforge:textures/models/armor/magma_bag.png"));
    public static final RegistryObject<Item> HAZCURE_HELMET = ITEMS.register(
            "hazcure_helmet", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.HAZCURE, ArmorItem.Type.HELMET,
                    new Item.Properties(), "item.bioforge.protective_gear.hazcure"));
    public static final RegistryObject<Item> HAZCURE_CHESTPLATE = ITEMS.register(
            "hazcure_chestplate", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.HAZCURE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties(), "item.bioforge.protective_gear.hazcure"));
    public static final RegistryObject<Item> HAZCURE_LEGGINGS = ITEMS.register(
            "hazcure_leggings", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.HAZCURE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties(), "item.bioforge.protective_gear.hazcure"));
    public static final RegistryObject<Item> HAZCURE_BOOTS = ITEMS.register(
            "hazcure_boots", () -> new ProtectiveGearItem(
                    BioForgeArmorMaterial.HAZCURE, ArmorItem.Type.BOOTS,
                    new Item.Properties(), "item.bioforge.protective_gear.hazcure"));
    public static final RegistryObject<Block> BLACK_STEEL_BLOCK = BLOCKS.register(
            "black_steel_block", BlackSteelBlock::new);
    public static final RegistryObject<BlockItem> BLACK_STEEL_BLOCK_ITEM = ITEMS.register(
            "black_steel_block", () -> new BlockItem(
                    BLACK_STEEL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WOODEN_NEEDLE = ITEMS.register("wooden_needle", () -> new NeedleItem(NeedleItem.Tier.WOODEN));
    public static final RegistryObject<Item> IRON_NEEDLE = ITEMS.register("iron_needle", () -> new NeedleItem(NeedleItem.Tier.IRON));
    public static final RegistryObject<Item> HARDENED_NEEDLE = ITEMS.register("hardened_needle", () -> new NeedleItem(NeedleItem.Tier.HARDENED));
    public static final RegistryObject<Item> ANTI_A_VIAL = ITEMS.register("anti_a_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_A));
    public static final RegistryObject<Item> ANTI_B_VIAL = ITEMS.register("anti_b_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_B));
    public static final RegistryObject<Item> ANTI_D_VIAL = ITEMS.register("anti_d_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_D));
    public static final RegistryObject<Item> PATHOGEN_REAGENT = ITEMS.register(
            "pathogen_reagent",
            () -> new DiagnosticReagentItem(DiagnosticReagentItem.Kind.PATHOGEN));
    public static final RegistryObject<Item> VISIBILITY_REAGENT = ITEMS.register(
            "visibility_reagent",
            () -> new DiagnosticReagentItem(DiagnosticReagentItem.Kind.VISIBILITY));
    public static final RegistryObject<Item> VACCINE = ITEMS.register("vaccine", VaccineItem::new);
    public static final RegistryObject<Item> MUTATION_VACCINE = ITEMS.register("mutation_vaccine",
            () -> new VaccineItem(VaccineItem.Kind.MUTATION));
    public static final RegistryObject<Item> TRANSMISSION_VACCINE = ITEMS.register("transmission_vaccine",
            () -> new VaccineItem(VaccineItem.Kind.TRANSMISSION));
    public static final RegistryObject<Item> SYMPTOM_VACCINE = ITEMS.register("symptom_vaccine",
            () -> new VaccineItem(VaccineItem.Kind.SYMPTOM));
    public static final RegistryObject<Item> RANDOM_MUTATION_VACCINE =
            ITEMS.register("random_mutation_vaccine",
                    () -> new VaccineItem(VaccineItem.Kind.RANDOM_MUTATION));
    public static final RegistryObject<Item> VIRAL_SUPPRESSOR_PILL =
            ITEMS.register("viral_suppressor_pill", ResistancePillItem::new);
    public static final RegistryObject<Item> VIRAL_INHIBITOR_PILL =
            ITEMS.register("viral_inhibitor_pill", ResistancePillItem::new);
    public static final RegistryObject<Item> VIRAL_BLOCKER_PILL =
            ITEMS.register("viral_blocker_pill", ResistancePillItem::new);
    public static final RegistryObject<Item> SYMPTOM_TABLET =
            ITEMS.register("symptom_tablet", SymptomTabletItem::new);
    public static final RegistryObject<Item> CRISPR_CARTRIDGE = ITEMS.register("crispr_cartridge",
            CrisprCartridgeItem::new);
    public static final RegistryObject<Item> CAS_MODULE = ITEMS.register("cas_module",
            CasModuleItem::new);
    public static final RegistryObject<Item> GENE_IMPRINT = ITEMS.register("gene_imprint",
            GeneImprintItem::new);
    public static final RegistryObject<Item> CRISPR_NOTES = ITEMS.register("crispr_notes",
            CrisprNotesItem::new);
    public static final RegistryObject<Item> DECALCIFICATION_FLUID = ITEMS.register("decalcification_fluid", DecalcificationFluidItem::new);
    public static final RegistryObject<Item> BONE_SAW = ITEMS.register("bone_saw", BoneSawItem::new);
    public static final RegistryObject<Item> WITHERED_SPLIT_BONE = ITEMS.register("withered_split_bone", WitheredSplitBoneItem::new);
    public static final RegistryObject<Item> WITHERED_BONE_MARROW = ITEMS.register("withered_bone_marrow", WitheredBoneMarrowItem::new);
    public static final RegistryObject<Item> SPLIT_BONE = ITEMS.register("split_bone", SplitBoneItem::new);
    public static final RegistryObject<Item> BONE_MARROW = ITEMS.register("bone_marrow", BoneMarrowItem::new);
    public static final RegistryObject<Item> THERMOMETER_ITEM = ITEMS.register("thermometer", ThermometerItem::new);
    public static final RegistryObject<Item> STETHOSCOPE = ITEMS.register("stethoscope", StethoscopeItem::new);
    public static final RegistryObject<Item> OTOSCOPE = ITEMS.register("otoscope", OtoscopeItem::new);
    public static final RegistryObject<Item> MIRROR = ITEMS.register("mirror", MirrorItem::new);
    public static final RegistryObject<Item> REFLEX_HAMMER = ITEMS.register("reflex_hammer", ReflexHammerItem::new);
    public static final RegistryObject<Item> PULSE_OXIMETER = ITEMS.register("pulse_oximeter", PulseOximeterItem::new);
    public static final RegistryObject<Item> CLIPBOARD = ITEMS.register("clipboard", ClipboardItem::new);
    public static final RegistryObject<Item> MEDICAL_REPORT = ITEMS.register("medical_report", MedicalReportItem::new);
    public static final RegistryObject<Item> RESEARCH_JOURNAL = ITEMS.register(
            "research_journal", ResearchJournalItem::new);
    public static final RegistryObject<Item> AREA_CONTAMINATION_SCANNER = ITEMS.register(
            "area_contamination_scanner", AreaContaminationScannerItem::new);

    public static final RegistryObject<Block> CENTRIFUGE = BLOCKS.register("centrifuge", CentrifugeBlock::new);
    public static final RegistryObject<Item> CENTRIFUGE_ITEM = ITEMS.register("centrifuge", () -> new BlockItem(CENTRIFUGE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE = BLOCK_ENTITIES.register("centrifuge", () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE.get()).build(null));
    public static final RegistryObject<MenuType<CentrifugeMenu>> CENTRIFUGE_MENU = MENUS.register("centrifuge", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(CentrifugeMenu::new));

    public static final RegistryObject<Block> VIRAL_SCANNER = BLOCKS.register("viral_scanner",
            () -> new ViralScannerBlock(ViralScannerBlock.Variant.FULL));
    public static final RegistryObject<Block> CEILING_VIRAL_SCANNER = BLOCKS.register("ceiling_viral_scanner",
            () -> new ViralScannerBlock(ViralScannerBlock.Variant.CEILING));
    public static final RegistryObject<Block> OPEN_LEFT_VIRAL_SCANNER = BLOCKS.register("open_left_viral_scanner",
            () -> new ViralScannerBlock(ViralScannerBlock.Variant.OPEN_LEFT));
    public static final RegistryObject<Block> OPEN_RIGHT_VIRAL_SCANNER = BLOCKS.register("open_right_viral_scanner",
            () -> new ViralScannerBlock(ViralScannerBlock.Variant.OPEN_RIGHT));
    public static final RegistryObject<BlockItem> VIRAL_SCANNER_ITEM = ITEMS.register("viral_scanner",
            () -> new BlockItem(VIRAL_SCANNER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> CEILING_VIRAL_SCANNER_ITEM = ITEMS.register("ceiling_viral_scanner",
            () -> new BlockItem(CEILING_VIRAL_SCANNER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OPEN_LEFT_VIRAL_SCANNER_ITEM = ITEMS.register("open_left_viral_scanner",
            () -> new BlockItem(OPEN_LEFT_VIRAL_SCANNER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> OPEN_RIGHT_VIRAL_SCANNER_ITEM = ITEMS.register("open_right_viral_scanner",
            () -> new BlockItem(OPEN_RIGHT_VIRAL_SCANNER.get(), new Item.Properties()));
    public static final RegistryObject<Block> AIR_VENT = BLOCKS.register("air_vent", AirVentBlock::new);
    public static final RegistryObject<BlockItem> AIR_VENT_ITEM = ITEMS.register("air_vent",
            () -> new BlockItem(AIR_VENT.get(), new Item.Properties()));

    public static final RegistryObject<Block> MICROBIAL_MAT = BLOCKS.register("microbial_mat", MicrobialMatBlock::new);
    public static final RegistryObject<BlockEntityType<MicrobialMatBlockEntity>> MICROBIAL_MAT_BE =
            BLOCK_ENTITIES.register("microbial_mat",
                    () -> BlockEntityType.Builder.of(MicrobialMatBlockEntity::new, MICROBIAL_MAT.get()).build(null));

    public static final RegistryObject<Block> PETRI_DISH_BLOCK = BLOCKS.register("petri_dish", PetriDishBlock::new);
    public static final RegistryObject<BlockEntityType<PetriDishBlockEntity>> PETRI_DISH_BE =
            BLOCK_ENTITIES.register("petri_dish",
                    () -> BlockEntityType.Builder.of(PetriDishBlockEntity::new, PETRI_DISH_BLOCK.get()).build(null));

    public static final RegistryObject<Block> SPOROCARP = BLOCKS.register("sporocarp", SporocarpBlock::new);
    public static final RegistryObject<BlockEntityType<SporocarpBlockEntity>> SPOROCARP_BE =
            BLOCK_ENTITIES.register("sporocarp",
                    () -> BlockEntityType.Builder.of(SporocarpBlockEntity::new, SPOROCARP.get()).build(null));
    public static final RegistryObject<Block> NECROTIC_PATCH = BLOCKS.register("necrotic_patch", NecroticPatchBlock::new);

    public static final RegistryObject<Block> CONTAMINATED_SUBSTRATE = BLOCKS.register("contaminated_substrate", ContaminatedSubstrateBlock::new);
    public static final RegistryObject<Item> CONTAMINATED_SUBSTRATE_ITEM = ITEMS.register("contaminated_substrate", ContaminatedSubstrateItem::new);

    public static final RegistryObject<Block> COLONY_CORE = BLOCKS.register("colony_core", ColonyCoreBlock::new);
    public static final RegistryObject<BlockEntityType<ColonyCoreBlockEntity>> COLONY_CORE_BE =
            BLOCK_ENTITIES.register("colony_core",
                    () -> BlockEntityType.Builder.of(ColonyCoreBlockEntity::new, COLONY_CORE.get()).build(null));

    public static final RegistryObject<Block> INFESTED_BLOCK = BLOCKS.register("infested_block", InfestedBlock::new);
    public static final RegistryObject<BlockEntityType<InfestedBlockEntity>> INFESTED_BLOCK_BE =
            BLOCK_ENTITIES.register("infested_block",
                    () -> BlockEntityType.Builder.of(InfestedBlockEntity::new, INFESTED_BLOCK.get()).build(null));

    public static final RegistryObject<Item> SWAB = ITEMS.register("swab", SwabItem::new);
    public static final RegistryObject<Item> PETRI_DISH = ITEMS.register("petri_dish", PetriDishItem::new);
    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe", SyringeItem::new);
    public static final RegistryObject<Item> BLOOD_SLIDE = ITEMS.register("blood_slide", BloodSlideItem::new);
    public static final RegistryObject<Item> TUBE = ITEMS.register("tube", TubeItem::new);
    public static final RegistryObject<Item> PLASMA_SAMPLE = ITEMS.register("plasma_sample", PlasmaSampleItem::new);
    public static final RegistryObject<Item> CELL_PELLET = ITEMS.register("cell_pellet", CellPelletItem::new);

    public static final RegistryObject<Block> MICROSCOPE = BLOCKS.register("microscope", MicroscopeBlock::new);
    public static final RegistryObject<BlockEntityType<MicroscopeBlockEntity>> MICROSCOPE_BE =
            BLOCK_ENTITIES.register("microscope", () ->
                    BlockEntityType.Builder.of(MicroscopeBlockEntity::new, MICROSCOPE.get()).build(null));
    public static final RegistryObject<MenuType<MicroscopeMenu>> MICROSCOPE_MENU  = MENUS.register("microscope", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(MicroscopeMenu::new));
    public static final RegistryObject<BlockItem> MICROSCOPE_ITEM = ITEMS.register("microscope",
            () -> new BlockItem(MICROSCOPE.get(), new Item.Properties()));

    public static final RegistryObject<Block> INCUBATOR = BLOCKS.register("incubator", IncubatorBlock::new);
    public static final RegistryObject<BlockEntityType<IncubatorBlockEntity>> INCUBATOR_BE =
            BLOCK_ENTITIES.register("incubator", () -> BlockEntityType.Builder.of(IncubatorBlockEntity::new, INCUBATOR.get()).build(null));
    public static final RegistryObject<MenuType<IncubatorMenu>> INCUBATOR_MENU =
            MENUS.register("incubator", () -> IForgeMenuType.create(IncubatorMenu::new));
    public static final RegistryObject<BlockItem> INCUBATOR_ITEM = ITEMS.register("incubator",
            () -> new BlockItem(INCUBATOR.get(), new Item.Properties()));

    public static final RegistryObject<Block> VACCINE_MAKER =
            BLOCKS.register("vaccine_maker", VaccineMakerBlock::new);
    public static final RegistryObject<BlockEntityType<VaccineMakerBlockEntity>> VACCINE_MAKER_BE =
            BLOCK_ENTITIES.register("vaccine_maker", () ->
                    BlockEntityType.Builder.of(VaccineMakerBlockEntity::new,
                            VACCINE_MAKER.get()).build(null));
    public static final RegistryObject<MenuType<VaccineMakerMenu>> VACCINE_MAKER_MENU =
            MENUS.register("vaccine_maker", () -> IForgeMenuType.create(VaccineMakerMenu::new));
    public static final RegistryObject<BlockItem> VACCINE_MAKER_ITEM =
            ITEMS.register("vaccine_maker",
                    () -> new BlockItem(VACCINE_MAKER.get(), new Item.Properties()));

    public static final RegistryObject<Block> BARREL_PRESS = BLOCKS.register(
            "barrel_press",
            () -> new LaboratoryProcessorBlock(LaboratoryStation.BARREL_PRESS));
    public static final RegistryObject<Block> CHEMICAL_SYNTHESIZER = BLOCKS.register(
            "chemical_synthesizer",
            () -> new LaboratoryProcessorBlock(LaboratoryStation.CHEMICAL_SYNTHESIZER));
    public static final RegistryObject<Block> STERILIZATION_CHAMBER = BLOCKS.register(
            "sterilization_chamber",
            () -> new LaboratoryProcessorBlock(LaboratoryStation.STERILIZATION_CHAMBER));
    public static final RegistryObject<Block> PHARMA_MIXER = BLOCKS.register(
            "pharma_mixer",
            () -> new LaboratoryProcessorBlock(LaboratoryStation.PHARMA_MIXER));
    public static final RegistryObject<BlockItem> BARREL_PRESS_ITEM = ITEMS.register(
            "barrel_press", () -> new BlockItem(BARREL_PRESS.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> CHEMICAL_SYNTHESIZER_ITEM = ITEMS.register(
            "chemical_synthesizer", () -> new BlockItem(CHEMICAL_SYNTHESIZER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> STERILIZATION_CHAMBER_ITEM = ITEMS.register(
            "sterilization_chamber", () -> new BlockItem(STERILIZATION_CHAMBER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> PHARMA_MIXER_ITEM = ITEMS.register(
            "pharma_mixer", () -> new BlockItem(PHARMA_MIXER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LaboratoryProcessorBlockEntity>> LABORATORY_PROCESSOR_BE =
            BLOCK_ENTITIES.register("laboratory_processor", () -> BlockEntityType.Builder.of(
                    LaboratoryProcessorBlockEntity::new, BARREL_PRESS.get(), CHEMICAL_SYNTHESIZER.get(),
                    STERILIZATION_CHAMBER.get(), PHARMA_MIXER.get()).build(null));
    public static final RegistryObject<MenuType<LaboratoryProcessorMenu>> LABORATORY_PROCESSOR_MENU =
            MENUS.register("laboratory_processor", () -> IForgeMenuType.create(LaboratoryProcessorMenu::new));

    public static final RegistryObject<Item> CATALYST_VIAL = ITEMS.register("catalyst_vial", CatalystVialItem::new);
    public static final RegistryObject<Item> NUTRIENT_MEDIUM = ITEMS.register("nutrient_medium", NutrientMediumItem::new);
    public static final RegistryObject<Item> VIRUS_SAMPLE = ITEMS.register("virus_sample", VirusSampleItem::new);
    public static final RegistryObject<Item> LIVE_CULTURE_VIAL = ITEMS.register("live_culture_vial", LiveCultureVialItem::new);
    public static final RegistryObject<Item> DIRTY_CULTURE_VIAL = ITEMS.register("dirty_culture_vial", DirtyCultureVialItem::new);
    public static final RegistryObject<Item> ETHANOL = ITEMS.register("ethanol", EthanolItem::new);
    public static final RegistryObject<Item> WIPES = ITEMS.register("wipes", WipeItem::new);

    public BioForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.SERVER, BioForgeServerConfig.SPEC,
                "bioforge-server.toml");
        LegacyMutationBehaviors.register();
        VaccineMakerPageRegistry.bootstrapBuiltIns();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        BioForgeSounds.SOUNDS.register(modEventBus);
        BioForgeEffects.register(modEventBus);
        BFCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            ThermometerNetworkHandler.register();
            StethoscopeNetworkHandler.register();
            OtoscopeNetworkHandler.register();
            ReflexHammerNetworkHandler.register();
            PulseOximeterNetworkHandler.register();
            InfectionNetworkHandler.register();
            StrainNameNetworkHandler.register();
            MicroscopeNetwork.register();
            VaccineMakerCorrectionNetwork.register();
            MutationNetworkHandler.register();
            ResearchJournalNetwork.register();
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        InfectCommand.register(event.getDispatcher());
        InfectionInvulnerabilityCommand.register(event.getDispatcher());
        StrainCommand.register(event.getDispatcher());
        MutateCommand.register(event.getDispatcher());
        VaccineMakeCommand.register(event.getDispatcher(), VACCINE);
        CrisprCommand.register(event.getDispatcher());
        BioForgeValidateCommand.register(event.getDispatcher());
        BioForgeDefinitionCommand.register(event.getDispatcher());
        BioForgeTestCommand.register(event.getDispatcher());
        ResearchJournalCommand.register(event.getDispatcher());
        DecontaminationCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        BioForgeDefinitionManager.freezeJavaRegistrations();
        net.jenkimods.bioforge.mutation.MutationLoader.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.infection.lifecycle.InfectionLifecycleRegistry.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.infection.natural.NaturalInfectionManager.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.crispr.BioForgeResearchData.freezeJavaRegistrations();
        net.jenkimods.bioforge.world.centrifuge.CentrifugeRecipeManager.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.world.decalcification.DecalcificationRecipeManager.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.world.incubator.CatalystMappingManager.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.world.microscope.MicroscopeSymptomConfig.INSTANCE.freezeJavaRegistrations();
        LaboratoryProcessRecipeManager.INSTANCE.freezeJavaRegistrations();
        net.jenkimods.bioforge.api.vaccine.VaccineMakerPageRegistry.freeze();
        ResearchJournalRegistry.freeze();
        BioForgeBehaviorRegistry.freeze();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        event.getServer().getAllLevels().forEach(level -> {
            AirborneReservoirManager.clear(level);
            AirVentBlock.clear(level);
        });
        TransmissionEngine.clearCaches();
        InfectionNetworkHandler.clearDefinitionSyncState();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                        BioForge.CENTRIFUGE_BE.get(),
                        CentrifugeBlockEntityRenderer::new);
                net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                        BioForge.MICROSCOPE_BE.get(),
                        MicroscopeBlockEntityRenderer::new);
                ResourceLocation filledRL = ResourceLocation.tryBuild(BioForge.MODID, "filled");
                ResourceLocation reactedRL = ResourceLocation.tryBuild(BioForge.MODID, "reacted");
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.SWAB.get(), filledRL, (stack, level, entity, seed) -> SwabItem.isContaminated(stack) ? 1.0f : 0.0f);
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.PETRI_DISH.get(), filledRL, (stack, level, entity, seed) -> {if (PetriDishItem.isInoculated(stack)) {return stack.getOrCreateTag().getInt("Growth") >= 1 ? 1.0f : 0.0f;}return 0.0f;});
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.CLIPBOARD.get(), filledRL, (stack, level, entity, seed) -> ClipboardItem.getFilledModel(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.WOODEN_NEEDLE.get(), filledRL, (stack, level, entity, seed) -> NeedleItem.getFilledPredicate(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.IRON_NEEDLE.get(), filledRL, (stack, level, entity, seed) -> NeedleItem.getFilledPredicate(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.HARDENED_NEEDLE.get(), filledRL, (stack, level, entity, seed) -> NeedleItem.getFilledPredicate(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.ANTI_A_VIAL.get(), reactedRL, (stack, level, entity, seed) -> ReagentVialItem.getReactedPredicate(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.ANTI_B_VIAL.get(), reactedRL, (stack, level, entity, seed) -> ReagentVialItem.getReactedPredicate(stack));
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.ANTI_D_VIAL.get(), reactedRL, (stack, level, entity, seed) -> ReagentVialItem.getReactedPredicate(stack));
                net.minecraft.client.gui.screens.MenuScreens.register(BioForge.CENTRIFUGE_MENU.get(), CentrifugeScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(BioForge.MICROSCOPE_MENU.get(), MicroscopeScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(BioForge.INCUBATOR_MENU.get(), IncubatorScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        BioForge.VACCINE_MAKER_MENU.get(), VaccineMakerScreen::new);
                net.minecraft.client.gui.screens.MenuScreens.register(
                        BioForge.LABORATORY_PROCESSOR_MENU.get(), LaboratoryProcessorScreen::new);
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.THERMOMETER_ITEM.get(), ResourceLocation.tryBuild(BioForge.MODID, "ready"), (stack, level, entity, seed) -> ThermometerItem.isReady(stack) ? 1.0f : 0.0f);
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.SYRINGE.get(), ResourceLocation.tryBuild(BioForge.MODID, "syringe_fill"), (stack, level, entity, seed) -> {int uses = SyringeItem.getUses(stack);return uses / 4.0f;});
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.BLOOD_SLIDE.get(), ResourceLocation.tryBuild(BioForge.MODID, "blood_slide_filled"), (stack, level, entity, seed) -> BloodSlideItem.hasBlood(stack) ? 1.0f : 0.0f);
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.TUBE.get(), ResourceLocation.tryBuild(BioForge.MODID, "tube_filled_blood"), (stack, level, entity, seed) -> TubeItem.hasBlood(stack) ? 1.0f : 0.0f);
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.LIVE_CULTURE_VIAL.get(),
                        ResourceLocation.tryBuild(BioForge.MODID, "filled"),
                        (stack, level, entity, seed) -> LiveCultureVialItem.hasStrain(stack) ? 1.0f : 0.0f);
            });
        }

        @SubscribeEvent
        public static void onRegisterAdditionalModels(
                ModelEvent.RegisterAdditional event) {
            event.register(MicroscopeBlockEntityRenderer.KNOB_MODEL);
            event.register(MicroscopeBlockEntityRenderer.LENS_WHEEL_MODEL);
            event.register(MicroscopeBlockEntityRenderer.BULB_MODEL);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel.THERMAL_BAG_LAYER,
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel::createThermalBagLayer);
            event.registerLayerDefinition(
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel.MEDICAL_MASK_LAYER,
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel::createMedicalMaskLayer);
            event.registerLayerDefinition(
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel.PROTECTIVE_GLOVES_LAYER,
                    net.jenkimods.bioforge.client.render.ProtectiveGearModel::createProtectiveGlovesLayer);
        }
    }
}
