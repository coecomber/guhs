package nl.juiced.guhs.feature.guhwinter.lootjes.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.guhwinter.lootjes.Inhoud;

/**
 * The cached look of one gift: built ONCE (and again only when its data changes or the light around it changed), the way
 * the chunk renderer builds a section: real block models, faces between two blocks of the gift left out, ambient
 * occlusion, biome tints, the light of the spot. Kept as plain arrays per layer (solid, cutout, translucent); drawing a
 * frame only streams those arrays into the buffer (no model lookups, no quads rebuilt: lesson of the 1.5.2 FPS hunt).
 * Signs, heads, banners, beds and bells are drawn by their own vanilla renderer from a small client-only copy of their
 * block entity ({@link #extra}).
 */
final class SurpriseMesh {
    private static final Direction[] RICHTINGEN = Direction.values();

    /** A decorative block entity of the gift and where it stands from the reference point. */
    record Extra(BlockEntity be, float dx, float dy, float dz) {
    }

    private final float[][] pos = new float[3][];
    private final float[][] uv = new float[3][];
    private final int[][] kleur = new int[3][];
    private final int[][] licht = new int[3][];
    private final byte[][] richting = new byte[3][];
    private final int[] quads = new int[3];
    final List<Extra> extra;
    /** For the renderer: the data version it was built from, the (turned) gift, the light it saw, when that was checked. */
    final int versie;
    final Inhoud inhoud;
    final long lichtSom;
    long gecontroleerd;

    private SurpriseMesh(Bouwer[] b, List<Extra> extra, int versie, Inhoud inhoud, long lichtSom, long gebouwd) {
        for (int l = 0; l < 3; l++) {
            pos[l] = b[l].pos.toFloatArray();
            uv[l] = b[l].uv.toFloatArray();
            kleur[l] = b[l].kleur.toIntArray();
            licht[l] = b[l].licht.toIntArray();
            richting[l] = new byte[b[l].richting.size()];
            for (int i = 0; i < richting[l].length; i++) {
                richting[l][i] = (byte) b[l].richting.getInt(i);
            }
            quads[l] = richting[l].length;
        }
        this.extra = extra;
        this.versie = versie;
        this.inhoud = inhoud;
        this.lichtSom = lichtSom;
        this.gecontroleerd = gebouwd;
    }

    private static final class Bouwer {
        final FloatArrayList pos = new FloatArrayList(), uv = new FloatArrayList();
        final IntArrayList kleur = new IntArrayList(), licht = new IntArrayList(), richting = new IntArrayList();

        void quad(float x, float y, float z, BakedQuad q, QuadInstance in) {
            int emissie = q.materialInfo().lightEmission();
            for (int v = 0; v < 4; v++) {
                var p = q.position(v);
                pos.add(p.x() + x);
                pos.add(p.y() + y);
                pos.add(p.z() + z);
                long packed = q.packedUV(v);
                uv.add(UVPair.unpackU(packed));
                uv.add(UVPair.unpackV(packed));
                kleur.add(ARGB.multiply(in.getColor(v), q.bakedColors().color(v)));
                licht.add(in.getLightCoordsWithEmission(v, emissie));
            }
            richting.add(q.direction().ordinal());
        }
    }

    /**
     * Builds the look of {@code in} standing with its min corner at {@code oorsprong}; positions relative to {@code ref}
     * (the kern for a placed gift, the min corner for the preview).
     */
    static SurpriseMesh bouw(ClientLevel level, BlockPos oorsprong, Inhoud in, BlockPos ref, int versie) {
        Minecraft mc = Minecraft.getInstance();
        SurpriseWereld wereld = new SurpriseWereld(level, oorsprong, in);
        ModelBlockRenderer r = new ModelBlockRenderer(mc.options.ambientOcclusion().get(), true, mc.getBlockColors());
        boolean cutoutBladeren = mc.options.cutoutLeaves().get();
        BlockStateModelSet modellen = mc.getModelManager().getBlockStateModelSet();
        Bouwer[] b = {new Bouwer(), new Bouwer(), new Bouwer()};
        List<Extra> extra = new ArrayList<>();
        BlockPos.MutableBlockPos wp = new BlockPos.MutableBlockPos();
        for (int i = 0; i < Inhoud.V; i++) {
            BlockState s = in.get(i);
            if (s.isAir()) {
                continue;
            }
            wp.set(oorsprong.getX() + Inhoud.x(i), oorsprong.getY() + Inhoud.y(i), oorsprong.getZ() + Inhoud.z(i));
            float dx = wp.getX() - ref.getX(), dy = wp.getY() - ref.getY(), dz = wp.getZ() - ref.getZ();
            if (s.getRenderShape() == RenderShape.MODEL) {
                boolean opaak = ModelBlockRenderer.forceOpaque(cutoutBladeren, s);
                BlockPos vast = wp.immutable();
                try {
                    r.tesselateBlock((x, y, z, q, inst) -> b[opaak ? 0 : q.materialInfo().layer().ordinal()].quad(x, y, z, q, inst),
                            dx, dy, dz, wereld, vast, s, modellen.get(s), s.getSeed(vast));
                } catch (RuntimeException e) {
                    // (a model that can't be drawn this way: that block stays invisible, the rest is fine)
                }
            }
            CompoundTag tag = in.be(i);
            if (tag != null) {
                try {
                    BlockEntity be = BlockEntity.loadStatic(wp.immutable(), s, tag, level.registryAccess());
                    if (be != null) {
                        be.setLevel(level);
                        extra.add(new Extra(be, dx, dy, dz));
                    }
                } catch (RuntimeException e) {
                    // (no copy: the block shows without its text or pattern)
                }
            }
        }
        return new SurpriseMesh(b, extra, versie, in, lichtSom(level, oorsprong, in), level.getGameTime());
    }

    /** A cheap fingerprint of the light on the gift's blocks (a torch placed nearby, day turning to night in a cave...). */
    static long lichtSom(ClientLevel level, BlockPos oorsprong, Inhoud in) {
        long som = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < Inhoud.V; i++) {
            if (!in.get(i).isAir()) {
                p.set(oorsprong.getX() + Inhoud.x(i), oorsprong.getY() + Inhoud.y(i), oorsprong.getZ() + Inhoud.z(i));
                som = som * 31 + LevelRenderer.getLightCoords(level, p);
            }
        }
        return som;
    }

    boolean heeft(int laag) {
        return quads[laag] > 0;
    }

    static RenderType type(int laag) {
        return switch (ChunkSectionLayer.values()[laag]) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    /**
     * Streams one layer into the buffer. {@code tint}: ARGB multiplied into every colour (-1: none; the preview uses it for
     * its see-through pink or red); {@code vastLicht}: one light for everything (-1: the light it was built with).
     */
    void teken(PoseStack.Pose pose, VertexConsumer vc, int laag, int tint, int vastLicht) {
        int n = quads[laag];
        if (n == 0) {
            return;
        }
        Matrix4f m = pose.pose();
        Vector3f[] normalen = new Vector3f[RICHTINGEN.length];
        for (Direction d : RICHTINGEN) {
            normalen[d.ordinal()] = pose.transformNormal(d.getUnitVec3f(), new Vector3f());
        }
        float[] p = pos[laag], t = uv[laag];
        int[] k = kleur[laag], li = licht[laag];
        byte[] r = richting[laag];
        Vector3f v = new Vector3f();
        for (int q = 0; q < n; q++) {
            Vector3f nr = normalen[r[q]];
            for (int j = 0; j < 4; j++) {
                int i = q * 4 + j;
                m.transformPosition(p[i * 3], p[i * 3 + 1], p[i * 3 + 2], v);
                int c = tint == -1 ? k[i] : ARGB.multiply(k[i], tint);
                vc.addVertex(v.x, v.y, v.z, c, t[i * 2], t[i * 2 + 1], OverlayTexture.NO_OVERLAY, vastLicht == -1 ? li[i] : vastLicht, nr.x, nr.y, nr.z);
            }
        }
    }
}
