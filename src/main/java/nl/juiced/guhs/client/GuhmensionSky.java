package nl.juiced.guhs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import nl.juiced.guhs.Guhs;
import org.joml.Matrix4f;

/**
 * The Guhmension sky: like the overworld, but with a big pink moon and pink, twinkly stars.
 * (The dimension type points its "effects" at guhs:guhmension.)
 */
public class GuhmensionSky extends DimensionSpecialEffects {
    private static final Identifier SUN = Identifier.withDefaultNamespace("textures/environment/sun.png");
    private static final Identifier MOON = Guhs.id("textures/environment/pink_moon.png");

    private VertexBuffer skyBuffer;
    private VertexBuffer starBuffer;

    public GuhmensionSky() {
        super(192f, true, SkyType.NORMAL, false, false);
    }

    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Guhs.id("guhmension"), new GuhmensionSky());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94f + 0.06f, brightness * 0.94f + 0.06f, brightness * 0.91f + 0.09f);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        FogType fog = camera.getFluidInCamera();
        if (isFoggy || fog == FogType.POWDER_SNOW || fog == FogType.LAVA) {
            return true;
        }
        if (skyBuffer == null) {
            buildBuffers();
        }
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        Tesselator tesselator = Tesselator.getInstance();

        // the sky colour
        Vec3 sky = level.getSkyColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), partialTick);
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor((float) sky.x, (float) sky.y, (float) sky.z, 1f);
        var shader = RenderSystem.getShader();
        skyBuffer.bind();
        skyBuffer.drawWithShader(pose.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();
        RenderSystem.enableBlend();

        // sunrise/sunset glow
        float[] sunrise = getSunriseColor(level.getTimeOfDay(partialTick), partialTick);
        if (sunrise != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(level.getSunAngle(partialTick)) < 0 ? 180f : 0f));
            pose.mulPose(Axis.ZP.rotationDegrees(90f));
            Matrix4f m = pose.last().pose();
            BufferBuilder fan = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            fan.addVertex(m, 0f, 100f, 0f).setColor(sunrise[0], sunrise[1], sunrise[2], sunrise[3]);
            for (int j = 0; j <= 16; j++) {
                float a = j * Mth.TWO_PI / 16f;
                fan.addVertex(m, Mth.sin(a) * 120f, Mth.cos(a) * 120f, -Mth.cos(a) * 40f * sunrise[3])
                        .setColor(sunrise[0], sunrise[1], sunrise[2], 0f);
            }
            BufferUploader.drawWithShader(fan.buildOrThrow());
            pose.popPose();
        }

        // sun, pink moon, pink stars
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        pose.pushPose();
        float clear = 1f - level.getRainLevel(partialTick);
        RenderSystem.setShaderColor(1f, 1f, 1f, clear);
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360f));
        Matrix4f m = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN);
        quad(tesselator, m, 30f, 100f, 0, 0, 1, 1);
        // the moon: always full, and a bit bigger than the overworld's; drawn normally so its pink shows
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, MOON);
        quad(tesselator, m, 26f, -100f, 0, 0, 1, 1);
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        float stars = level.getStarBrightness(partialTick) * clear;
        if (stars > 0f) {
            // twinkle: the whole field breathes a little
            float twinkle = 0.85f + 0.15f * Mth.sin((ticks + partialTick) * 0.05f);
            RenderSystem.setShaderColor(stars * twinkle, stars * twinkle, stars * twinkle, stars);
            FogRenderer.setupNoFog();
            starBuffer.bind();
            starBuffer.drawWithShader(pose.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
            setupFog.run();
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        pose.popPose();
        RenderSystem.depthMask(true);
        return true;
    }

    /** The moon is drawn facing down (at y = -100 in the rotated frame), the sun facing up. */
    private static void quad(Tesselator tesselator, Matrix4f m, float size, float y, float u0, float v0, float u1, float v1) {
        BufferBuilder b = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        if (y > 0) {
            b.addVertex(m, -size, y, -size).setUv(u0, v0);
            b.addVertex(m, size, y, -size).setUv(u1, v0);
            b.addVertex(m, size, y, size).setUv(u1, v1);
            b.addVertex(m, -size, y, size).setUv(u0, v1);
        } else {
            b.addVertex(m, -size, y, size).setUv(u1, v1);
            b.addVertex(m, size, y, size).setUv(u0, v1);
            b.addVertex(m, size, y, -size).setUv(u0, v0);
            b.addVertex(m, -size, y, -size).setUv(u1, v0);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private void buildBuffers() {
        Tesselator tesselator = Tesselator.getInstance();
        // sky disc (as in the overworld)
        BufferBuilder disc = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        disc.addVertex(0f, 16f, 0f);
        for (int i = -180; i <= 180; i += 45) {
            disc.addVertex(512f * Mth.cos(i * Mth.DEG_TO_RAD), 16f, 512f * Mth.sin(i * Mth.DEG_TO_RAD));
        }
        skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        skyBuffer.bind();
        skyBuffer.upload(disc.buildOrThrow());
        // pink stars: 1800 little quads in pinks, lilacs and a few white ones
        RandomSource random = RandomSource.create(10842L);
        BufferBuilder stars = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 1800; i++) {
            float x = random.nextFloat() * 2f - 1f, y = random.nextFloat() * 2f - 1f, z = random.nextFloat() * 2f - 1f;
            float size = 0.15f + random.nextFloat() * 0.12f;
            float len = x * x + y * y + z * z;
            if (len <= 0.010f || len >= 1f) {
                continue;
            }
            org.joml.Vector3f dir = new org.joml.Vector3f(x, y, z).normalize(100f);
            float spin = (float) (random.nextDouble() * Math.PI * 2);
            org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateTo(new org.joml.Vector3f(0, 0, -1), dir).rotateZ(spin);
            int kind = random.nextInt(10);
            float r = 1f, g = kind < 6 ? 0.55f + random.nextFloat() * 0.2f : kind < 9 ? 0.7f : 1f, b = kind < 6 ? 0.8f : kind < 9 ? 1f : 1f;
            float[][] corners = {{size, -size}, {size, size}, {-size, size}, {-size, -size}};
            for (float[] c : corners) {
                org.joml.Vector3f v = new org.joml.Vector3f(c[0], c[1], 0).rotate(rot).add(dir);
                stars.addVertex(v.x, v.y, v.z).setColor(r, g, b, 1f);
            }
        }
        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        starBuffer.bind();
        starBuffer.upload(stars.buildOrThrow());
        VertexBuffer.unbind();
    }
}
