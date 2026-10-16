package net.jenkimods.bioforge.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.crispr.VaccineTargetCategory;
import net.jenkimods.bioforge.infection.*;
import net.jenkimods.bioforge.item.crispr.GeneImprintItem;
import net.jenkimods.bioforge.util.NbtObfuscator;
import net.jenkimods.bioforge.vaccine.*;
import net.jenkimods.bioforge.world.splicer.SplicerCulture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

public final class EarlyMedicineRecipeCategory implements IRecipeCategory<EarlyMedicineRecipeCategory.Operation> {
    public enum Kind {
        FIELD("field_vaccine_maker"), DIFFUSER("medical_diffuser"), SPLICER("gene_splicer");
        final String id;
        final RecipeType<Operation> type;
        Kind(String id) { this.id = id; type = RecipeType.create(BioForge.MODID, id, Operation.class); }
        public RecipeType<Operation> type() { return type; }
        public ItemStack block() { return new ItemStack(switch (this) {
            case FIELD -> BioForge.FIELD_VACCINE_MAKER_ITEM.get();
            case DIFFUSER -> BioForge.MEDICAL_DIFFUSER_ITEM.get();
            case SPLICER -> BioForge.GENE_SPLICER_ITEM.get();
        }); }
    }
    public record Operation(List<List<ItemStack>> inputs, List<ItemStack> outputs, String description) {}
    private final Kind kind;
    private final IDrawable background, icon;
    public EarlyMedicineRecipeCategory(IGuiHelper helper, Kind kind) {
        this.kind = kind; background = helper.createBlankDrawable(212, 146);
        icon = helper.createDrawableItemStack(kind.block());
    }
    @Override public RecipeType<Operation> getRecipeType() { return kind.type; }
    @Override public Component getTitle() { return Component.translatable("block.bioforge." + kind.id); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Operation operation, IFocusGroup focuses) {
        for (int i = 0; i < operation.inputs.size(); i++) builder.addSlot(RecipeIngredientRole.INPUT, 12 + i * 38, 17)
                .addItemStacks(operation.inputs.get(i));
        for (int i = 0; i < operation.outputs.size(); i++) builder.addSlot(RecipeIngredientRole.OUTPUT, 182, 17 + i * 20)
                .addItemStack(operation.outputs.get(i));
    }
    @Override public void draw(Operation operation, IRecipeSlotsView slots, GuiGraphics g, double x, double y) {
        g.fill(0, 0, 212, 146, 0xFF162C42);
        for (int i = 0; i < operation.inputs.size(); i++) drawSlot(g, 11 + i * 38);
        drawSlot(g, 181);
        if (operation.outputs.size() > 1) { g.fill(181, 36, 199, 54, 0xFF6A99AF); g.fill(182, 37, 198, 53, 0xFF0B1929); }
        g.drawString(Minecraft.getInstance().font, "→", 148, 21, 0xFFE1F5FF, false);
        g.drawWordWrap(Minecraft.getInstance().font, Component.translatable(operation.description), 8, 60, 196, 0xFFD0E8EF);
    }
    private static void drawSlot(GuiGraphics g, int x) {
        g.fill(x, 16, x + 18, 34, 0xFF6A99AF); g.fill(x + 1, 17, x + 17, 33, 0xFF0B1929);
    }
    private static ItemStack sample(StrainData strain, Item item) {
        ItemStack stack = new ItemStack(item);
        NbtObfuscator.writeString(stack.getOrCreateTag(), strain.toPayload()); return stack;
    }
    public static List<Operation> operations(Kind kind) {
        StrainData strain = StrainData.createEmpty(); strain.setPathogen(PathogenType.VIRUS);
        strain.getSymptoms().put("temperature_plus", "1.0");
        var swab = sample(strain, BioForge.FIELD_SWAB.get());
        if (kind == Kind.FIELD) {
            List<Operation> operations = new ArrayList<>();
            for (boolean improved : new boolean[]{false, true}) {
                ItemStack result = new ItemStack(improved ? BioForge.STABILIZED_FIELD_VACCINE.get() : BioForge.FIELD_VACCINE.get());
                new VaccineProfile(strain.toPayload(), improved ? .25F : .1F, 1, .03F,
                        UUID.nameUUIDFromBytes(("bioforge:jei:" + improved).getBytes(java.nio.charset.StandardCharsets.UTF_8)), 0).write(result);
                operations.add(new Operation(List.of(List.of(swab, sample(strain, BioForge.VIRUS_SAMPLE.get()),
                        sample(strain, BioForge.LIVE_CULTURE_VIAL.get())), List.of(new ItemStack(BioForge.FIELD_VACCINE.get())),
                        List.of(new ItemStack(improved ? Items.GLISTERING_MELON_SLICE : Items.SUGAR))), List.of(result),
                        "jei.bioforge.field." + (improved ? "stabilized" : "crude")));
            }
            return operations;
        }
        if (kind == Kind.DIFFUSER) {
            List<Operation> operations = new ArrayList<>();
            for (Item medicine : new Item[]{BioForge.COUGH_TABLET.get(), BioForge.SNEEZE_TABLET.get(), BioForge.FEVER_TABLET.get(), BioForge.NAUSEA_TABLET.get()})
                operations.add(new Operation(List.of(List.of(new ItemStack(medicine)), List.of(new ItemStack(Items.WATER_BUCKET))),
                        List.of(new ItemStack(Items.BUCKET)), "jei.bioforge.field.diffuser"));
            ItemStack programmedTablet = new ItemStack(BioForge.SYMPTOM_TABLET.get());
            new SymptomTabletProfile("coughing", 2400, .75F).write(programmedTablet);
            operations.add(new Operation(List.of(List.of(programmedTablet), List.of(new ItemStack(Items.WATER_BUCKET))),
                    List.of(new ItemStack(Items.BUCKET)), "jei.bioforge.field.diffuser"));
            ItemStack vaccine = new ItemStack(BioForge.VACCINE.get());
            new VaccineProfile(strain.toPayload(), .75F, 1, 0, UUID.nameUUIDFromBytes(new byte[]{1}), 0).write(vaccine);
            ItemStack crude = new ItemStack(BioForge.FIELD_VACCINE.get()), stabilized = new ItemStack(BioForge.STABILIZED_FIELD_VACCINE.get());
            new VaccineProfile(strain.toPayload(), .1F, 1, .03F, UUID.nameUUIDFromBytes(new byte[]{2}), 0).write(crude);
            new VaccineProfile(strain.toPayload(), .25F, 1, .03F, UUID.nameUUIDFromBytes(new byte[]{3}), 0).write(stabilized);
            operations.add(new Operation(List.of(List.of(vaccine, crude, stabilized), List.of(new ItemStack(Items.WATER_BUCKET))),
                    List.of(new ItemStack(Items.BUCKET)), "jei.bioforge.field.diffuser_vaccine"));
            return operations;
        }
        ItemStack culture = sample(strain, BioForge.VIRUS_SAMPLE.get()), analyzed = culture.copy();
        SplicerCulture.analyze(analyzed);
        List<Operation> operations = new ArrayList<>();
        operations.add(new Operation(List.of(List.of(culture)), List.of(analyzed.copy()), "jei.bioforge.splicer.analyze"));
        for (var category : VaccineTargetCategory.values()) {
            var studied = analyzed.copy(); SplicerCulture.examine(studied, category);
            operations.add(new Operation(List.of(List.of(analyzed.copy()), List.of(new ItemStack(
                    net.jenkimods.bioforge.world.splicer.GeneSplicerBlockEntity.examinationReagent(category)))),
                    List.of(studied), "jei.bioforge.splicer.study_" + category.serializedName()));
        }
        SplicerCulture.examine(analyzed, VaccineTargetCategory.SYMPTOM);
        ItemStack imprint = new ItemStack(BioForge.GENE_IMPRINT.get());
        GeneImprintItem.captureIdentified(imprint, strain, VaccineTargetCategory.SYMPTOM, "temperature_plus");
        var cleanGenes = StrainData.createEmpty(); cleanGenes.setPathogen(PathogenType.VIRUS);
        var recipient = sample(cleanGenes, BioForge.VIRUS_SAMPLE.get()); SplicerCulture.analyze(recipient);
        SplicerCulture.examine(recipient, VaccineTargetCategory.SYMPTOM);
        var edited = analyzed.copy(); SplicerCulture.reserves(edited, 2, 75);
        SplicerCulture.writeStrain(edited, strain);
        var removed = recipient.copy(); SplicerCulture.reserves(removed, 2, 75);
        SplicerCulture.writeStrain(removed, cleanGenes);
        var recovered = analyzed.copy(); SplicerCulture.reserves(recovered, 2, 85);
        var weakened = analyzed.copy(); SplicerCulture.reserves(weakened, 1, 60);
        operations.addAll(List.of(
                new Operation(List.of(List.of(analyzed), List.of(new ItemStack(BioForge.GENE_IMPRINT.get()))), List.of(imprint), "jei.bioforge.splicer.extract"),
                new Operation(List.of(List.of(recipient), List.of(imprint), List.of(new ItemStack(BioForge.CATALYST_VIAL.get()))), List.of(edited), "jei.bioforge.splicer.insert"),
                new Operation(List.of(List.of(analyzed), List.of(new ItemStack(BioForge.GENE_IMPRINT.get())), List.of(new ItemStack(BioForge.CATALYST_VIAL.get()))), List.of(removed, imprint), "jei.bioforge.splicer.remove"),
                new Operation(List.of(List.of(weakened), List.of(new ItemStack(BioForge.NUTRIENT_MEDIUM.get()))), List.of(recovered), "jei.bioforge.splicer.recover")));
        return operations;
    }
}
