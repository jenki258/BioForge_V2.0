package net.jenkimods.bioforge;

import com.mojang.logging.LogUtils;
import net.jenkimods.bioforge.block.*;
import net.jenkimods.bioforge.client.CentrifugeScreen;
import net.jenkimods.bioforge.blood.network.NetworkHandler;
import net.jenkimods.bioforge.infection.command.InfectCommand;
import net.jenkimods.bioforge.infection.network.InfectionNetworkHandler;
import net.jenkimods.bioforge.item.bone_saw.BoneSawItem;
import net.jenkimods.bioforge.item.bones.BoneMarrowItem;
import net.jenkimods.bioforge.item.bones.SplitBoneItem;
import net.jenkimods.bioforge.item.bones.WitheredBoneMarrowItem;
import net.jenkimods.bioforge.item.bones.WitheredSplitBoneItem;
import net.jenkimods.bioforge.item.clipboard.ClipboardItem;
import net.jenkimods.bioforge.item.clipboard.MedicalReportItem;
import net.jenkimods.bioforge.item.infection.*;
import net.jenkimods.bioforge.item.needle.NeedleItem;
import net.jenkimods.bioforge.item.otoscope.OtoscopeItem;
import net.jenkimods.bioforge.item.otoscope.OtoscopeNetworkHandler;
import net.jenkimods.bioforge.item.pulse_oximeter.PulseOximeterItem;
import net.jenkimods.bioforge.item.pulse_oximeter.PulseOximeterNetworkHandler;
import net.jenkimods.bioforge.item.reagents.DecalcificationFluidItem;
import net.jenkimods.bioforge.item.reagents.ReagentVialItem;
import net.jenkimods.bioforge.item.reflex_hammer.ReflexHammerItem;
import net.jenkimods.bioforge.item.reflex_hammer.ReflexHammerNetworkHandler;
import net.jenkimods.bioforge.item.otoscope.MirrorItem;
import net.jenkimods.bioforge.item.stethoscope.StethoscopeItem;
import net.jenkimods.bioforge.item.stethoscope.StethoscopeNetworkHandler;
import net.jenkimods.bioforge.item.thermometer.ThermometerItem;
import net.jenkimods.bioforge.item.thermometer.ThermometerNetworkHandler;
import net.jenkimods.bioforge.registry.BFCreativeTabs;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeBlockEntity;
import net.jenkimods.bioforge.world.centrifuge.CentrifugeMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(BioForge.MODID)
public class BioForge {
    public static final String MODID = "bioforge";
    public static final String MOD_NAME = "BioForge";
    public static final String VERSION = "2.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<Item> WOODEN_NEEDLE = ITEMS.register("wooden_needle", () -> new NeedleItem(NeedleItem.Tier.WOODEN));
    public static final RegistryObject<Item> IRON_NEEDLE = ITEMS.register("iron_needle", () -> new NeedleItem(NeedleItem.Tier.IRON));
    public static final RegistryObject<Item> HARDENED_NEEDLE = ITEMS.register("hardened_needle", () -> new NeedleItem(NeedleItem.Tier.HARDENED));
    public static final RegistryObject<Item> ANTI_A_VIAL = ITEMS.register("anti_a_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_A));
    public static final RegistryObject<Item> ANTI_B_VIAL = ITEMS.register("anti_b_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_B));
    public static final RegistryObject<Item> ANTI_D_VIAL = ITEMS.register("anti_d_vial", () -> new ReagentVialItem(ReagentVialItem.Type.ANTI_D));
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

    public static final RegistryObject<Block> CENTRIFUGE = BLOCKS.register("centrifuge", CentrifugeBlock::new);
    public static final RegistryObject<Item> CENTRIFUGE_ITEM = ITEMS.register("centrifuge", () -> new BlockItem(CENTRIFUGE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE = BLOCK_ENTITIES.register("centrifuge", () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE.get()).build(null));
    public static final RegistryObject<MenuType<CentrifugeMenu>> CENTRIFUGE_MENU = MENUS.register("centrifuge", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(CentrifugeMenu::new));

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

    public BioForge(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
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
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        InfectCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
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
                net.minecraft.client.renderer.item.ItemProperties.register(BioForge.THERMOMETER_ITEM.get(), ResourceLocation.tryBuild(BioForge.MODID, "ready"), (stack, level, entity, seed) -> ThermometerItem.isReady(stack) ? 1.0f : 0.0f);
            });
        }
    }
}