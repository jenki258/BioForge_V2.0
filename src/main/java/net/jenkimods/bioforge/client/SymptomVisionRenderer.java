package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.jenkimods.bioforge.config.BioForgeClientConfig;
import net.jenkimods.bioforge.infection.HeartRate;
import net.jenkimods.bioforge.infection.InfectionClientCache;
import net.jenkimods.bioforge.infection.symptoms.BioForgeSymptoms;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class SymptomVisionRenderer {
    private static ShaderInstance shader;
    private static RenderTarget scene, history;
    private static Object levelIdentity;
    private static Vec3 phantom;
    private static long nextPhantom, phantomStart, phantomEnd, nextEnder, enderStart, enderEnd, nextFalseEcho;
    private static long historyTick;
    private static int phantomStyle;
    private static float wither, hypoxia;
    private static boolean historyValid;

    private SymptomVisionRenderer() {}

    public static void setShader(ShaderInstance loaded) {
        shader = loaded;
        historyValid = false;
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
                && InfectionClientCache.isInfectionActive() && BioForgeClientConfig.visualIntensity() > 0;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (levelIdentity != mc.level || !active()) {
            reset();
            levelIdentity = mc.level;
        }
        if (!active() || mc.isPaused()) return;
        long now = mc.level.getGameTime();
        if (BioForgeClientConfig.paranoiaVisuals() && InfectionClientCache.hasMutation("paranoia")) {
            if (nextPhantom == 0) nextPhantom = now + 600 + mc.level.random.nextInt(1000);
            if (phantom != null) {
                Vec3 direction = phantom.subtract(mc.player.getEyePosition()).normalize();
                if (now >= phantomEnd || mc.player.getLookAngle().dot(direction) > 0.93D
                        || phantom.distanceToSqr(mc.player.position()) < 9
                        || mc.level.getMaxLocalRawBrightness(BlockPos.containing(phantom)) > 7
                        || mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()) > 9) phantom = null;
            }
            if (now >= nextPhantom) {
                phantom = findOrigin(true);
                phantomStart = now;
                phantomEnd = now + 60 + mc.level.random.nextInt(40);
                phantomStyle = mc.level.random.nextInt(3);
                nextPhantom = now + 600 + mc.level.random.nextInt(1000);
            }
        } else {
            phantom = null;
            nextPhantom = 0;
        }
        if (BioForgeClientConfig.enderVisuals() && InfectionClientCache.hasMutation("ender_disorientation")) {
            if (nextEnder == 0) nextEnder = now + 400 + mc.level.random.nextInt(500);
            if (now >= nextEnder) {
                enderStart = now;
                enderEnd = now + 30 + mc.level.random.nextInt(25);
                nextEnder = now + 400 + mc.level.random.nextInt(500);
                historyValid = false;
            }
        } else {
            nextEnder = enderEnd = 0;
            historyValid = false;
        }
        float witherTarget = BioForgeClientConfig.witherVisuals() && InfectionClientCache.hasMutation("wither_necrosis")
                ? Mth.clamp(InfectionClientCache.getSymptom(BioForgeSymptoms.INFECTION_STRENGTH), 0, 1) * 0.55F
                + (mc.player.hasEffect(MobEffects.WITHER) ? 0.4F : 0.15F) : 0;
        float hypoxiaTarget = BioForgeClientConfig.hypoxiaVisuals()
                ? Mth.clamp((0.90F - InfectionClientCache.getSymptom(BioForgeSymptoms.OXYGEN_SATURATION)) / 0.35F, 0, 1) : 0;
        wither = witherTarget == 0 ? 0 : Mth.lerp(0.12F, wither, witherTarget);
        hypoxia = hypoxiaTarget == 0 ? 0 : Mth.lerp(0.12F, hypoxia, hypoxiaTarget);
        if (BioForgeClientConfig.falseEchoes() && SculkEchoRenderer.active()) {
            if (nextFalseEcho == 0) nextFalseEcho = now + 900 + mc.level.random.nextInt(1100);
            if (now >= nextFalseEcho) {
                Vec3 origin = findOrigin(false);
                if (origin != null) SculkEchoRenderer.falseEcho(origin);
                nextFalseEcho = now + 900 + mc.level.random.nextInt(1100);
            }
        } else nextFalseEcho = 0;
        if (phantom == null && now >= enderEnd && wither < 0.001F && hypoxia < 0.001F) releaseBuffers();
    }

    private static Vec3 findOrigin(boolean peripheral) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        if (peripheral && mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()) > 9) return null;
        for (int i = 0; i < 12; i++) {
            double angle = peripheral ? Math.toRadians(mc.player.getYRot() + (mc.level.random.nextBoolean() ? 1 : -1)
                    * (35 + mc.level.random.nextInt(16))) : mc.level.random.nextDouble() * Math.PI * 2;
            double distance = 6 + mc.level.random.nextDouble() * 7;
            double x = eye.x - Math.sin(angle) * distance, z = eye.z + Math.cos(angle) * distance;
            BlockPos column = BlockPos.containing(x, mc.player.getY(), z);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos feet = column.offset(0, dy, 0);
                if (!mc.level.hasChunkAt(feet) || !mc.level.getBlockState(feet.below()).isCollisionShapeFullBlock(mc.level, feet.below())
                        || !mc.level.noCollision(new AABB(x - 0.35, feet.getY(), z - 0.35, x + 0.35, feet.getY() + 2.2, z + 0.35))
                        || mc.level.containsAnyLiquid(new AABB(x - 0.35, feet.getY(), z - 0.35, x + 0.35, feet.getY() + 2.2, z + 0.35))) continue;
                Vec3 center = new Vec3(x, feet.getY() + 1.05, z);
                if (peripheral && mc.level.getMaxLocalRawBrightness(BlockPos.containing(center)) > 7) continue;
                if (mc.level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        mc.player)).getType() == HitResult.Type.MISS) return center;
            }
        }
        return null;
    }

    private static void reset() {
        phantom = null;
        nextPhantom = phantomStart = phantomEnd = nextEnder = enderStart = enderEnd = nextFalseEcho = 0;
        wither = hypoxia = 0;
        releaseBuffers();
    }

    private static void releaseBuffers() {
        if (scene != null) { scene.destroyBuffers(); scene = null; }
        if (history != null) { history.destroyBuffers(); history = null; }
        historyValid = false;
    }

    private static void copy(RenderTarget source, RenderTarget target, boolean depth) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, source.width, source.height, 0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT | (depth ? GL11.GL_DEPTH_BUFFER_BIT : 0), GL11.GL_NEAREST);
    }

    public static void render(Matrix4f projection, Matrix4f modelView, Camera camera, float partialTick) {
        if (!active() || shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        double now = mc.level.getGameTime() + partialTick;
        float ender = now < enderEnd && now >= enderStart
                ? (float) Math.sin(Math.PI * (now - enderStart) / Math.max(1, enderEnd - enderStart)) : 0;
        float phantomAlpha = phantom == null ? 0 : (float) Math.min(1, Math.min((now - phantomStart) / 12, (phantomEnd - now) / 12));
        if (ender <= 0 && wither <= 0 && hypoxia <= 0 && phantomAlpha <= 0) return;
        RenderTarget main = mc.getMainRenderTarget();
        if (scene != null && (scene.width != main.width || scene.height != main.height)) releaseBuffers();
        if (scene == null) scene = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        copy(main, scene, true);
        if (ender > 0 && history == null) history = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        boolean updateHistory = ender > 0 && (!historyValid || mc.level.getGameTime() - historyTick >= 4);
        main.bindWrite(false);
        shader.setSampler("SceneSampler", scene.getColorTextureId());
        shader.setSampler("DepthSampler", scene.getDepthTextureId());
        shader.setSampler("HistorySampler", historyValid && history != null ? history.getColorTextureId() : scene.getColorTextureId());
        shader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        shader.safeGetUniform("InverseView").set(new Matrix4f(modelView).invert());
        shader.safeGetUniform("Time").set((float) (now % 24000 / 20));
        shader.safeGetUniform("Effects").set(ender, wither, hypoxia, BioForgeClientConfig.visualIntensity());
        HeartRate rate = InfectionClientCache.getSymptom(BioForgeSymptoms.HEART_RATE);
        shader.safeGetUniform("HeartRate").set(rate == HeartRate.TACHY ? 1.8F : rate == HeartRate.BRADY ? 0.7F : 1.1F);
        Vec3 relative = phantom == null ? Vec3.ZERO : phantom.subtract(camera.getPosition());
        shader.safeGetUniform("Phantom").set((float) relative.x, (float) relative.y, (float) relative.z, Math.max(0, phantomAlpha));
        shader.safeGetUniform("PhantomStyle").set((float) phantomStyle);
        boolean hadDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean hadDepthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean hadBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean hadCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        ShaderInstance previous = RenderSystem.getShader();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shader);
            BufferBuilder vertices = Tesselator.getInstance().getBuilder();
            vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.vertex(-1, -1, 0).uv(0, 0).endVertex();
            vertices.vertex(1, -1, 0).uv(1, 0).endVertex();
            vertices.vertex(1, 1, 0).uv(1, 1).endVertex();
            vertices.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(vertices.end());
            if (updateHistory) {
                copy(scene, history, false);
                historyValid = true;
                historyTick = mc.level.getGameTime();
            }
        } finally {
            main.bindWrite(false);
            RenderSystem.setShader(() -> previous);
            RenderSystem.depthMask(hadDepthWrite);
            if (hadDepth) RenderSystem.enableDepthTest();
            if (hadBlend) RenderSystem.enableBlend();
            if (hadCull) RenderSystem.enableCull();
        }
    }
}
