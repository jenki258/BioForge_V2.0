package net.jenkimods.bioforge.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** All linear machine gauges use the Vaccine Maker's existing artwork. */
public final class MachineProgress {
    private static final ResourceLocation TRACK = ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/progress_track.png");
    private static final ResourceLocation FILL = ResourceLocation.tryBuild("bioforge", "textures/gui/vaccine_maker/progress_fill.png");
    private MachineProgress() {}
    public static void draw(GuiGraphics g, int x, int y, int width, int height, int value, int maximum) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(width / 99F, height / 4F, 1);
        GuiRenderCompat.prepare(TRACK);
        g.blit(TRACK, 0, 0, 0, 0, 99, 4, 99, 4);
        int filled = maximum <= 0 ? 0 : Math.round(99 * Mth.clamp(value / (float) maximum, 0F, 1F));
        if (filled > 0) {
            GuiRenderCompat.prepare(FILL);
            g.blit(FILL, 0, 0, 0, 0, filled, 4, 99, 4);
        }
        g.pose().popPose();
    }
    public static void vertical(GuiGraphics g, int x, int y, int width, int height, int value, int maximum) {
        g.pose().pushPose();
        g.pose().translate(x, y + height, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        draw(g, 0, 0, height, width, value, maximum);
        g.pose().popPose();
    }
}
