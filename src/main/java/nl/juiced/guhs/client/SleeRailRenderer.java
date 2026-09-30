package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.entity.SleeRailBlockEntity;
import nl.juiced.guhs.slee.SleePath;

import net.minecraft.client.renderer.rendertype.RenderTypes;
/**
 * Draws a whole sled rail piece along its path: two pink-and-white candy-stripe rails on little wooden sleepers.
 * The path is the same one the sled follows, so the sled always sits right on top of the rails.
 */
public class SleeRailRenderer implements BlockEntityRenderer<SleeRailBlockEntity> {
    public static final Identifier TEXTURE = Guhs.id("textures/block/slee_rail.png");
    /** Rails: sideways from the middle, thickness and height (the top is where the sled rides). */
    public static final double RAIL_OFFSET = 0.55, RAIL_HALF_WIDTH = 0.07, SLEEPER_HEIGHT = 0.08;
    public static final double SLEEPER_HALF_WIDTH = 0.8, SLEEPER_HALF_LENGTH = 0.14, SLEEPER_SPACING = 0.5;

    // texture swatches (u0, v0, u1, v1): pink, white, wood, dark wood
    private static final float[] PINK = {0, 0, 0.5f, 0.5f}, WHITE = {0.5f, 0, 1, 0.5f}, WOOD = {0, 0.5f, 0.5f, 1};

    public SleeRailRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SleeRailBlockEntity rail, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = rail.getBlockState();
        if (!(state.getBlock() instanceof SleeRailBlock) || rail.getLevel() == null) {
            return;
        }
        BlockPos anchor = rail.getBlockPos();
        SleePath.Shape shape = state.getValue(SleeRailBlock.SHAPE);
        SleePath.Piece piece = new SleePath.Piece(anchor, state.getValue(SleeRailBlock.FACING), shape);
        VertexConsumer vc = buffer.getBuffer(RenderTypes.entityCutout(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Vec3 origin = Vec3.atLowerCornerOf(anchor).add(0, SleePath.RIDE_HEIGHT, 0);

        double end = SleePath.railEnd(shape);
        double length = shape.length() * end;
        int segments = (int) Math.ceil(length / 0.25);
        for (int i = 0; i < segments; i++) {
            SleePath.Point a = piece.at(end * i / segments), b = piece.at(end * (i + 1) / segments);
            Vec3 pa = a.pos().subtract(origin), pb = b.pos().subtract(origin);
            int light = LevelRenderer.getLightColor(rail.getLevel(), BlockPos.containing(a.pos().add(0, 0.5, 0)));
            Vec3 heading = pb.subtract(pa).normalize();
            Vec3 side = new Vec3(-heading.z, 0, heading.x).normalize();
            Vec3 up = side.cross(heading);
            float[] tex = (i / 2) % 2 == 0 ? PINK : WHITE;
            if (Math.abs(heading.y) > 0.999) {
                continue;
            }
            for (int s = -1; s <= 1; s += 2) {
                Vec3 off = side.scale(s * RAIL_OFFSET).add(up.scale(SLEEPER_HEIGHT));
                box(vc, pose, pa.add(off), pb.add(off), side, up, RAIL_HALF_WIDTH, SleePath.RIDE_HEIGHT - SLEEPER_HEIGHT, tex, light);
            }
        }
        // sleepers
        int sleepers = Math.max(2, (int) Math.round(length / SLEEPER_SPACING));
        for (int i = 0; i < sleepers; i++) {
            SleePath.Point p = piece.at(end * (i + 0.5) / sleepers);
            Vec3 heading = p.heading();
            Vec3 side = new Vec3(-heading.z, 0, heading.x).normalize();
            Vec3 up = side.cross(heading);
            Vec3 c = p.pos().subtract(origin);
            int light = LevelRenderer.getLightColor(rail.getLevel(), BlockPos.containing(p.pos().add(0, 0.5, 0)));
            box(vc, pose, c.subtract(heading.scale(SLEEPER_HALF_LENGTH)), c.add(heading.scale(SLEEPER_HALF_LENGTH)), side, up,
                    SLEEPER_HALF_WIDTH, SLEEPER_HEIGHT, WOOD, light);
        }
    }

    /**
     * A box from a to b (the middle of its bottom face), `halfWidth` to both sides and `height` up.
     */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 side, Vec3 up, double halfWidth,
                            double height, float[] tex, int light) {
        Vec3 sw = side.scale(halfWidth), h = up.scale(height);
        Vec3 a0 = a.subtract(sw), a1 = a.add(sw), b0 = b.subtract(sw), b1 = b.add(sw);
        Vec3 a2 = a1.add(h), a3 = a0.add(h), b2 = b1.add(h), b3 = b0.add(h);
        quad(vc, pose, a3, a2, b2, b3, up, tex, light);                  // top
        quad(vc, pose, a0, a1, b1, b0, up.reverse(), tex, light);        // bottom
        quad(vc, pose, a1, b1, b2, a2, side, tex, light);                // right
        quad(vc, pose, a0, b0, b3, a3, side.reverse(), tex, light);      // left
        Vec3 fwd = b.subtract(a).normalize();
        quad(vc, pose, b0, b1, b2, b3, fwd, tex, light);                 // front
        quad(vc, pose, a0, a1, a2, a3, fwd.reverse(), tex, light);       // back
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 n, float[] tex, int light) {
        vertex(vc, pose, p0, tex[0], tex[3], n, light);
        vertex(vc, pose, p1, tex[2], tex[3], n, light);
        vertex(vc, pose, p2, tex[2], tex[1], n, light);
        vertex(vc, pose, p3, tex[0], tex[1], n, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SleeRailBlockEntity rail) {
        return new net.minecraft.world.phys.AABB(rail.getBlockPos()).inflate(4, 9, 4);
    }

    @Override
    public boolean shouldRenderOffScreen(SleeRailBlockEntity rail) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
