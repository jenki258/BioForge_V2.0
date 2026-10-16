package net.jenkimods.bioforge.item.stethoscope;

import net.jenkimods.bioforge.util.NbtObfuscator;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

public enum AuscultationSite {
    HEART(true,1F,1F), LUNGS(false,1F,1F);
    private final boolean heart;
    public final float volume, pitch;
    AuscultationSite(boolean heart,float volume,float pitch) { this.heart=heart;this.volume=volume;this.pitch=pitch; }
    public boolean isHeart() { return heart; }
    public String translationKey() { return "item.bioforge.stethoscope.site."+name().toLowerCase(Locale.ROOT); }
    public AuscultationSite next() { return values()[(ordinal()+1)%values().length]; }
    public static AuscultationSite read(ItemStack stack) {
        if (!stack.hasTag()) return HEART;
        var tag=NbtObfuscator.readCompound(stack.getTag(),"auscultation_site");
        if (tag==null) return HEART;
        return switch (tag.getString("Site")) {
            case "LUNGS", "FRONT_LUNGS", "BACK_LUNGS" -> LUNGS;
            default -> HEART;
        };
    }
    public void write(ItemStack stack) {
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Site",name());
        NbtObfuscator.writeCompound(stack.getOrCreateTag(),"auscultation_site",tag);
    }
}
