package nl.juiced.guhs.feature.mewtwo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.mewtwo.KloontankBlockEntity;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoStand;

/**
 * Draws the kloontank round its block (the middle of its floor): an eight-sided glass tube (3 blocks wide, almost 3 high)
 * with metal rings, the pink knabbelsap inside (it glows), and as long as YOUR questline hasn't repaired it (the client's
 * {@link MewtwoStand}): cracks in three of its panes, one pane broken down to a stump, the liquid only a puddle high, and four
 * lamps on the front of the base (red = that part is missing, green = built in). Repaired: whole glass, full to the top.
 */
public class KloontankRenderer implements BlockEntityRenderer<KloontankBlockEntity> {
    private static final int ZIJDEN = 8;
    private static final float R_GLAS = 1.38f, R_VLOEI = 1.28f, ONDER = 0.125f, BOVEN = 2.97f;

    public KloontankRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static TextureAtlasSprite sprite(String naam) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(Guhs.id("block/" + naam));
    }

    @Override
    public void render(KloontankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean heel = MewtwoStand.tankHeel();
        float tijd = (tank.getLevel() == null ? 0 : tank.getLevel().getGameTime()) + partialTick;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose last = pose.last();
        // the liquid: a glowing pink column (full and wavy when whole, a puddle when cracked)
        TextureAtlasSprite vloei = sprite("mewtwo_kloonvloeistof");
        float hoog = heel ? BOVEN - 0.12f + (float) Math.sin(tijd * 0.08) * 0.03f : 0.42f + (float) Math.sin(tijd * 0.05) * 0.02f;
        int roze = heel ? 0xE0FFFFFF : 0xD0FFFFFF;
        for (int i = 0; i < ZIJDEN; i++) {
            zijde(vc, last, i, R_VLOEI, ONDER, hoog, vloei, roze, LightTexture.FULL_BRIGHT, -1);
        }
        deksel(vc, last, R_VLOEI, hoog, vloei, roze, LightTexture.FULL_BRIGHT);
        // the glass: eight panes, three cracked and one broken while it isn't repaired
        TextureAtlasSprite glas = sprite("mewtwo_tankglas"), barst = sprite("mewtwo_tankglas_barst");
        for (int i = 0; i < ZIJDEN; i++) {
            if (!heel && i == 3) {
                zijde(vc, last, i, R_GLAS, ONDER, 0.9f, barst, 0xFFFFFFFF, light, 1);   // (only the stump of the broken pane is left)
                continue;
            }
            boolean kapot = !heel && (i == 1 || i == 2 || i == 5);
            zijde(vc, last, i, R_GLAS, ONDER, BOVEN, kapot ? barst : glas, 0xFFFFFFFF, light, 1);
        }
        // metal rings at the bottom and the top
        TextureAtlasSprite metaal = sprite("mewtwo_tankmetaal");
        for (int i = 0; i < ZIJDEN; i++) {
            zijde(vc, last, i, R_GLAS + 0.04f, 0.0f, ONDER + 0.12f, metaal, 0xFFFFFFFF, light, 1);
            zijde(vc, last, i, R_GLAS + 0.04f, BOVEN - 0.14f, BOVEN + 0.02f, metaal, 0xFFFFFFFF, light, 1);
        }
        // the four lamps on the front of the base: red = missing, green = built in
        Direction voor = tank.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? tank.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.SOUTH;
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-voor.toYRot()));
        PoseStack.Pose lamp = pose.last();
        for (int n = 1; n <= MewtwoFeature.ONDERDELEN; n++) {
            boolean in = MewtwoStand.isIngebouwd(n);
            TextureAtlasSprite s = sprite(in ? "mewtwo_lampje_groen" : "mewtwo_lampje_rood");
            float x = -0.52f + (n - 1) * 0.3f, z = R_GLAS + 0.07f;
            int flits = !in && ((int) (tijd / 10) + n) % 4 == 0 ? 0xFFFFB0B0 : 0xFFFFFFFF;
            vierkant(vc, lamp, x, 0.03f, z, x + 0.16f, 0.2f, s, flits, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
        pose.popPose();
    }

    /** One side of an eight-sided tube (radius r, from y0 to y1); dir 1 = its normal points out, -1 = in. */
    private static void zijde(VertexConsumer vc, PoseStack.Pose p, int i, float r, float y0, float y1, TextureAtlasSprite s, int argb, int light, int dir) {
        double a0 = Math.PI * 2 * i / ZIJDEN + Math.PI / ZIJDEN, a1 = Math.PI * 2 * (i + 1) / ZIJDEN + Math.PI / ZIJDEN;
        float x0 = (float) Math.cos(a0) * r, z0 = (float) Math.sin(a0) * r, x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r;
        float nx = (float) Math.cos((a0 + a1) / 2) * dir, nz = (float) Math.sin((a0 + a1) / 2) * dir;
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        // (the texture repeats once per block of height)
        for (float yb = y0; yb < y1 - 1e-3f; yb += 1f) {
            float ye = Math.min(y1, yb + 1f);
            float ve = v0 + (v1 - v0) * (ye - yb);
            vc.addVertex(p, x0, ye, z0).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
            vc.addVertex(p, x0, yb, z0).setColor(argb).setUv(u0, ve).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
            vc.addVertex(p, x1, yb, z1).setColor(argb).setUv(u1, ve).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
            vc.addVertex(p, x1, ye, z1).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, 0, nz);
        }
    }

    /** The flat top of the liquid (a fan of wedges). */
    private static void deksel(VertexConsumer vc, PoseStack.Pose p, float r, float y, TextureAtlasSprite s, int argb, int light) {
        float um = (s.getU0() + s.getU1()) / 2, vm = (s.getV0() + s.getV1()) / 2, du = (s.getU1() - s.getU0()) / 2, dv = (s.getV1() - s.getV0()) / 2;
        for (int i = 0; i < ZIJDEN; i++) {
            double a0 = Math.PI * 2 * i / ZIJDEN + Math.PI / ZIJDEN, a1 = Math.PI * 2 * (i + 1) / ZIJDEN + Math.PI / ZIJDEN;
            float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
            vc.addVertex(p, 0, y, 0).setColor(argb).setUv(um, vm).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            vc.addVertex(p, x1 * r, y, z1 * r).setColor(argb).setUv(um + x1 * du, vm + z1 * dv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            vc.addVertex(p, x0 * r, y, z0 * r).setColor(argb).setUv(um + x0 * du, vm + z0 * dv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
            vc.addVertex(p, 0, y, 0).setColor(argb).setUv(um, vm).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
        }
    }

    /** A little upright square (x0..x1, y0..y1) at depth z, facing +z. */
    private static void vierkant(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z, float x1, float y1, TextureAtlasSprite s, int argb, int light) {
        vc.addVertex(p, x0, y1, z).setColor(argb).setUv(s.getU0(), s.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x0, y0, z).setColor(argb).setUv(s.getU0(), s.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y0, z).setColor(argb).setUv(s.getU1(), s.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
        vc.addVertex(p, x1, y1, z).setColor(argb).setUv(s.getU1(), s.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 0, 1);
    }

    @Override
    public AABB getRenderBoundingBox(KloontankBlockEntity tank) {
        return new AABB(tank.getBlockPos()).inflate(1.6, 0, 1.6).expandTowards(0, 3, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(KloontankBlockEntity tank) {
        return true;
    }
}
