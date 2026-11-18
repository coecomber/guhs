package nl.juiced.guhs.feature.guhwinter.lootjes.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.client.GuhKeys;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import nl.juiced.guhs.feature.guhwinter.lootjes.Inhoud;
import nl.juiced.guhs.feature.guhwinter.lootjes.LootjesFeature;
import nl.juiced.guhs.feature.guhwinter.lootjes.LootjesPayloads;
import nl.juiced.guhs.feature.guhwinter.lootjes.SurpriseBlok;
import nl.juiced.guhs.feature.guhwinter.lootjes.SurpriseData;
import org.lwjgl.glfw.GLFW;

/**
 * Holding a Surprise: a see-through preview of the real gift where you look (soft pink = it fits, red = something is in
 * the way, judged on this side; the server checks again, protections too). It turns to face you; R or shift + scroll turns
 * it further. Right-click puts it there (payload {@code guhwinter_surprise_plaats}). Shift + right-click on a placed gift
 * stays a pick-up, also with a Surprise in your hand.
 */
public final class SurpriseVoorbeeld {
    public static final KeyMapping DRAAI = new KeyMapping("key.guhs.guhwinter_surprise_draai", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, GuhKeys.CATEGORY);
    private static final double BEREIK = 10;

    private static int extra;
    @Nullable
    private static Doel doel;
    @Nullable
    private static SurpriseMesh mesh;
    @Nullable
    private static SurpriseData meshData;
    private static BlockPos meshPlek = BlockPos.ZERO;
    private static int meshDraai = -1;
    private static long hintTijd;

    /** Where the preview stands now: min corner, turn, does it fit (as far as the client can tell), which hand. */
    private record Doel(BlockPos oorsprong, Rotation draai, boolean past, InteractionHand hand, SurpriseData data) {
    }

    private SurpriseVoorbeeld() {
    }

    @Nullable
    private static InteractionHand hand(LocalPlayer p) {
        if (p.getMainHandItem().is(LootjesFeature.SURPRISE_ITEM.get())) {
            return InteractionHand.MAIN_HAND;
        }
        return p.getOffhandItem().is(LootjesFeature.SURPRISE_ITEM.get()) ? InteractionHand.OFF_HAND : null;
    }

    /** Looking at a placed gift with shift down: that's a pick-up, not a placing. */
    private static boolean kijktNaarCadeau(Minecraft mc) {
        return mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && mc.level != null
                && mc.level.getBlockState(bh.getBlockPos()).getBlock() instanceof SurpriseBlok;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        doel = null;
        if (p == null || mc.level == null) {
            return;
        }
        InteractionHand hand = hand(p);
        while (DRAAI.consumeClick()) {
            if (hand != null && mc.screen == null) {
                draai(1);
            }
        }
        if (hand == null || p.isSpectator()) {
            return;
        }
        ItemStack stack = p.getItemInHand(hand);
        SurpriseData d = stack.get(LootjesFeature.SURPRISE_DATA.get());
        if (d == null || d.leeg() || (p.isShiftKeyDown() && kijktNaarCadeau(mc))) {
            return;
        }
        HitResult hit = p.pick(BEREIK, 1.0f, false);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) {
            hint(p, "guhwinter.lootjes.surprise.hint_kijk");
            return;
        }
        Direction kant = bh.getDirection();
        BlockPos basis = bh.getBlockPos().relative(kant);
        if (kant.getAxis().isHorizontal()) {
            basis = basis.relative(kant, Inhoud.N / 2);   // against a wall: the gift's near side touches it
        } else if (kant == Direction.DOWN) {
            basis = basis.below(Inhoud.N - 1);
        }
        BlockPos oorsprong = basis.offset(-Inhoud.N / 2, 0, -Inhoud.N / 2);
        Rotation draai = naarSpeler(p.getDirection()).getRotated(Rotation.values()[extra]);
        Inhoud in = inhoud(mc.level, oorsprong, draai, d);
        doel = new Doel(oorsprong, draai, past(mc.level, oorsprong, in), hand, d);
        hint(p, "guhwinter.lootjes.surprise.hint");
    }

    /** The turn that makes the gift's front (south when it was wrapped) face a player looking {@code kijk}. */
    static Rotation naarSpeler(Direction kijk) {
        return switch (kijk) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static void hint(LocalPlayer p, String sleutel) {
        long nu = p.level().getGameTime();
        if (nu - hintTijd > 50) {
            hintTijd = nu;
            p.sendOverlayMessage(Component.translatable(sleutel).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void draai(int stap) {
        extra = Math.floorMod(extra + stap, 4);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.4f + extra * 0.1f);
        }
    }

    /** The (turned) gift, with the mesh rebuilt only when the spot, the turn or the gift changed. */
    private static Inhoud inhoud(ClientLevel level, BlockPos oorsprong, Rotation draai, SurpriseData d) {
        boolean zelfde = meshData == d || d.equals(meshData);
        if (mesh == null || !zelfde || !meshPlek.equals(oorsprong) || meshDraai != draai.ordinal()) {
            Inhoud in = mesh != null && zelfde && meshDraai == draai.ordinal() ? mesh.inhoud : d.lees().gedraaid(draai);
            mesh = SurpriseMesh.bouw(level, oorsprong, in, oorsprong, 0);
            meshData = d;
            meshPlek = oorsprong;
            meshDraai = draai.ordinal();
        }
        return mesh.inhoud;
    }

    /** Does it fit, as far as this side can tell (free spots, not on the Winterplein, nobody in the way)? */
    private static boolean past(ClientLevel level, BlockPos oorsprong, Inhoud in) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < Inhoud.V; i++) {
            if (in.get(i).isAir()) {
                continue;
            }
            p.set(oorsprong.getX() + Inhoud.x(i), oorsprong.getY() + Inhoud.y(i), oorsprong.getZ() + Inhoud.z(i));
            BlockState s = level.getBlockState(p);
            if (!level.isInWorldBounds(p) || !s.canBeReplaced() || !s.getFluidState().isEmpty() || s.getBlock() instanceof SurpriseBlok
                    || (level.dimension() == GuhwinterZone.DIMENSIE && GuhwinterZone.binnen(p))) {
                return false;
            }
            AABB b = new AABB(p);
            for (Entity e : level.getEntities((Entity) null, b, x -> (x instanceof LivingEntity || x instanceof VehicleEntity) && !x.isSpectator())) {
                return false;
            }
        }
        return true;
    }

    // --- input ------------------------------------------------------------------------------------------------------------

    static void klik(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || hand(p) != event.getHand() || mc.screen != null) {
            return;
        }
        if (p.isShiftKeyDown() && kijktNaarCadeau(mc)) {
            return;   // a pick-up: the server handles that click
        }
        Doel d = doel;
        if (d == null) {
            return;   // (not looking at the ground: the click does what it always does)
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        ClientPacketDistributor.sendToServer(new LootjesPayloads.Plaats(d.hand() == InteractionHand.MAIN_HAND, d.oorsprong(), d.draai().ordinal()));
    }

    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null || hand(p) == null || !p.isShiftKeyDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        draai(event.getScrollDeltaY() > 0 ? 1 : -1);
        event.setCanceled(true);
    }

    // --- drawing ----------------------------------------------------------------------------------------------------------

    static void teken(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Doel d = doel;
        SurpriseMesh m = mesh;
        Minecraft mc = Minecraft.getInstance();
        if (d == null || m == null || mc.level == null) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderTypes.translucentMovingBlock();
        VertexConsumer vc = buffers.getBuffer(type);
        float t = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int alfa = (int) (255 * (0.42f + 0.1f * (float) Math.sin(t * 0.15f)));
        int tint = alfa << 24 | (d.past() ? 0xFFE8F4 : 0xFF6060);
        pose.pushPose();
        pose.translate(d.oorsprong().getX() - cam.x, d.oorsprong().getY() - cam.y, d.oorsprong().getZ() - cam.z);
        pose.translate(Inhoud.N / 2.0, 0, Inhoud.N / 2.0);
        pose.scale(1.002f, 1.002f, 1.002f);
        pose.translate(-Inhoud.N / 2.0, 0, -Inhoud.N / 2.0);
        for (int laag = 0; laag < 3; laag++) {
            m.teken(pose.last(), vc, laag, tint, 0xF000F0);
        }
        pose.popPose();
        buffers.endBatch(type);
    }

    static void vergeet() {
        doel = null;
        mesh = null;
        meshData = null;
        meshDraai = -1;
        extra = 0;
    }
}
