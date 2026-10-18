package net.jenkimods.bioforge.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.jenkimods.bioforge.infection.EchoRayTracer;
import net.jenkimods.bioforge.infection.InfectionClientCache;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class SculkEchoRenderer {
    private static final int MAX_PULSES = 24;
    private static final double SPEED = 0.65D;
    private static final double RANGE = 24.0D;
    private static final List<Pulse> PULSES = new ArrayList<>();
    private static final Deque<SoundInstance> PENDING = new ArrayDeque<>();
    private static ShaderInstance shader;
    private static RenderTarget depth;
    private static Object levelIdentity;
    private static long nextSoundTick;

    private record Pulse(Vec3 origin, double start, double range) {}

    private SculkEchoRenderer() {}

    public static void setShader(ShaderInstance loaded) { shader = loaded; }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && mc.player.isAlive() && !mc.player.isSpectator()
                && InfectionClientCache.isInfectionActive() && InfectionClientCache.hasMutation("sculk_echo")
                && mc.player.hasEffect(MobEffects.BLINDNESS);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || levelIdentity != mc.level) {
            PULSES.clear();
            PENDING.clear();
            nextSoundTick = 0L;
            levelIdentity = mc.level;
            if (!active() && depth != null) {
                depth.destroyBuffers();
                depth = null;
            }
        }
        if (active()) {
            while (!PENDING.isEmpty()) capture(PENDING.removeFirst());
        }
        if (mc.level != null) {
            long now = mc.level.getGameTime();
            PULSES.removeIf(pulse -> (now - pulse.start) * SPEED > pulse.range + 8.0D);
        }
    }

    public static void sound(SoundInstance sound) {
        if (!active() || sound == null || sound instanceof EchoSound || sound.isRelative() || sound.isLooping()
                || (sound.getSource() != SoundSource.PLAYERS && sound.getSource() != SoundSource.HOSTILE
                && sound.getSource() != SoundSource.NEUTRAL)) return;
        Minecraft mc = Minecraft.getInstance();
        Object soundLevel = mc.level;
        mc.execute(() -> {
            if (mc.level == soundLevel && active() && PENDING.size() < 32) PENDING.addLast(sound);
        });
    }

    private static void capture(SoundInstance sound) {
        if (!active() || sound.getSound() == null) return;
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level.getGameTime();
        Vec3 origin = new Vec3(sound.getX(), sound.getY() + 0.2D, sound.getZ());
        if (now < nextSoundTick || origin.distanceToSqr(mc.player.position()) > RANGE * RANGE
                || sound.getVolume() <= 0.0F) return;
        nextSoundTick = now + 5L;
        emit(origin, now, sound);
    }

    public static void falseEcho(Vec3 origin) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        emit(origin, mc.level.getGameTime(), null);
        if (net.jenkimods.bioforge.config.BioForgeClientConfig.sculkEchoAudio()) {
            mc.level.playLocalSound(origin.x, origin.y, origin.z, net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.AMBIENT, 0.16F, 0.85F, false);
        }
    }

    private static void emit(Vec3 origin, long now, SoundInstance sound) {
        Minecraft mc = Minecraft.getInstance();
        add(new Pulse(origin, now, RANGE));
        boolean audibleEcho = false;
        for (int i = 0; i < 10; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / 10.0D;
            double radius = Math.sqrt(1.0D - y * y);
            double angle = i * 2.399963229728653D + now * 0.1D;
            Vec3 direction = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            for (var reflection : EchoRayTracer.trace(mc.level, mc.player, origin, direction, RANGE, 2)) {
                add(new Pulse(reflection.position(), now + reflection.pathLength() / SPEED,
                        Math.max(1.0D, RANGE - reflection.pathLength())));
                if (sound != null && !audibleEcho && net.jenkimods.bioforge.config.BioForgeClientConfig.sculkEchoAudio()) {
                    Vec3 point = reflection.position();
                    mc.getSoundManager().playDelayed(new EchoSound(sound, point),
                            Math.max(3, Math.min(30, (int) (reflection.pathLength() / SPEED))));
                    audibleEcho = true;
                }
            }
        }
    }

    private static void add(Pulse pulse) {
        if (PULSES.size() == MAX_PULSES) PULSES.remove(0);
        PULSES.add(pulse);
    }

    public static void render(Matrix4f projection, Matrix4f modelView, Camera camera, float partialTick) {
        if (!active() || shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (depth == null) depth = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        else if (depth.width != main.width || depth.height != main.height) {
            depth.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        depth.copyDepthFrom(main);
        main.bindWrite(false);
        shader.setSampler("DepthSampler", depth.getDepthTextureId());
        shader.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
        shader.safeGetUniform("InverseView").set(new Matrix4f(modelView).invert());
        Vec3 cameraPosition = camera.getPosition();
        double now = mc.level.getGameTime() + partialTick;
        for (int i = 0; i < MAX_PULSES; i++) {
            if (i < PULSES.size()) {
                Pulse pulse = PULSES.get(i);
                Vec3 relative = pulse.origin.subtract(cameraPosition);
                float radius = (float) ((now - pulse.start) * SPEED);
                if (radius > pulse.range + 8.0D) radius = -1000.0F;
                shader.safeGetUniform("Echo" + i).set((float) relative.x, (float) relative.y,
                        (float) relative.z, radius);
            } else shader.safeGetUniform("Echo" + i).set(0.0F, 0.0F, 0.0F, -1000.0F);
        }
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
        } finally {
            RenderSystem.setShader(() -> previous);
            RenderSystem.depthMask(hadDepthWrite);
            if (hadDepth) RenderSystem.enableDepthTest();
            if (hadBlend) RenderSystem.enableBlend();
            if (hadCull) RenderSystem.enableCull();
        }
    }

    private static final class EchoSound extends SimpleSoundInstance {
        @Override public boolean canPlaySound() { return active(); }
        EchoSound(SoundInstance original, Vec3 position) {
            super(original.getLocation(), original.getSource(), original.getVolume() * 0.22F,
                    original.getPitch() * 0.94F, SoundInstance.createUnseededRandom(), false, 0,
                    SoundInstance.Attenuation.LINEAR, position.x, position.y, position.z, false);
        }
    }
}
