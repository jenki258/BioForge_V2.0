package net.jenkimods.bioforge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jenkimods.bioforge.BioForge;
import net.jenkimods.bioforge.block.LaboratoryProcessorBlock;
import net.jenkimods.bioforge.world.laboratory.LaboratoryProcessorBlockEntity;
import net.jenkimods.bioforge.world.laboratory.LaboratoryStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public final class ApothecaryBlockEntityRenderer implements BlockEntityRenderer<LaboratoryProcessorBlockEntity> {
    public static final ResourceLocation PESTLE_MODEL = ResourceLocation.tryBuild(BioForge.MODID, "block/herbal_mortar_pestle");

    public ApothecaryBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(LaboratoryProcessorBlockEntity machine, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (machine.station() != LaboratoryStation.HERBAL_MORTAR || machine.getLevel() == null) return;
        float rotation = switch (machine.getBlockState().getValue(LaboratoryProcessorBlock.FACING)) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
        double lift = 0.0D;
        if (machine.isWorking()) {
            // A slow lift followed by a quicker strike, once every sixteen ticks.
            double phase = ((machine.getLevel().getGameTime() % 16L) + partialTick) / 16.0D;
            lift = 3.0D / 16.0D * (phase < 0.7D
                    ? Math.sin(phase / 0.7D * Math.PI / 2.0D)
                    : Math.cos((phase - 0.7D) / 0.3D * Math.PI / 2.0D));
        }
        pose.pushPose();
        pose.translate(0.5D, lift, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5D, 0.0D, -0.5D);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.solid()), machine.getBlockState(),
                minecraft.getModelManager().getModel(PESTLE_MODEL),
                1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
