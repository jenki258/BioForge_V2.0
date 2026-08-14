package net.jenkimods.bioforge.item.protective;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.function.Supplier;

public final class BioForgeArmorMaterial implements ArmorMaterial {
    public static final BioForgeArmorMaterial PROTECTIVE = new BioForgeArmorMaterial(
            "bioforge:protective", 18, 9, 1.0F, 0.0F,
            () -> Ingredient.of(BioForge.STERILE_RUBBER.get()));
    public static final BioForgeArmorMaterial HAZCURE = new BioForgeArmorMaterial(
            "bioforge:hazcure", 24, 10, 1.5F, 0.0F,
            () -> Ingredient.of(BioForge.BLACK_STEEL_PLATE.get()));

    private final String name;
    private final int durabilityMultiplier;
    private final int enchantmentValue;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;
    private final EnumMap<ArmorItem.Type, Integer> defense =
            new EnumMap<>(ArmorItem.Type.class);

    private BioForgeArmorMaterial(String name, int durabilityMultiplier,
                                  int enchantmentValue, float toughness,
                                  float knockbackResistance,
                                  Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
        defense.put(ArmorItem.Type.BOOTS, name.endsWith("hazcure") ? 2 : 1);
        defense.put(ArmorItem.Type.LEGGINGS, name.endsWith("hazcure") ? 5 : 2);
        defense.put(ArmorItem.Type.CHESTPLATE, name.endsWith("hazcure") ? 6 : 2);
        defense.put(ArmorItem.Type.HELMET, name.endsWith("hazcure") ? 2 : 1);
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 13;
            case LEGGINGS -> 15;
            case CHESTPLATE -> 16;
            case HELMET -> 11;
        } * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense.getOrDefault(type, 0);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
