package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;

/**
 * Placing a Surprise in the world and picking it up again.
 * <ul>
 *   <li>{@link #plaats}: from the client's preview (payload {@code guhwinter_surprise_plaats}: the room's min corner and the
 *       turn). Every block of the gift needs a free spot (air or something replaceable like grass, never water), in loaded
 *       ground, not on the Winterplein, not in anyone's protected area: each spot asks the same place event a normal block
 *       would (claims, guh buildings, {@link Protected}), and no one may be standing in it;</li>
 *   <li>{@link #pak}: shift + right-click by the one who placed it: the whole gift turns back into the item;</li>
 *   <li>breaking a part does nothing (only a part whose kern is gone: that leftover just goes).</li>
 * </ul>
 */
public final class SurprisePlaatsen {
    private static final String T = "guhwinter.lootjes.surprise.";
    /** How far the gift's middle may be from your eyes. */
    static final double BEREIK = 14;

    private SurprisePlaatsen() {
    }

    // =====================================================================================================================
    // placing
    // =====================================================================================================================

    public static void plaats(ServerPlayer p, boolean hoofdhand, BlockPos oorsprong, int draai) {
        InteractionHand hand = hoofdhand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = p.getItemInHand(hand);
        SurpriseData d = stack.get(LootjesFeature.SURPRISE_DATA.get());
        if (!stack.is(LootjesFeature.SURPRISE_ITEM.get()) || d == null) {
            return;
        }
        if (d.leeg()) {
            melding(p, T + "leeg");
            return;
        }
        if (Bouwkist.bouwt(p) || p.isSpectator()) {
            melding(p, T + "niet_nu");
            return;
        }
        Vec3 mid = new Vec3(oorsprong.getX() + Inhoud.N / 2.0, oorsprong.getY() + 1, oorsprong.getZ() + Inhoud.N / 2.0);
        if (mid.distanceTo(p.getEyePosition()) > BEREIK) {
            melding(p, T + "te_ver");
            return;
        }
        Rotation r = Rotation.values()[Math.floorMod(draai, 4)];
        Inhoud in = d.lees().gedraaid(r);
        ServerLevel level = p.level();
        Component fout = vrij(p, level, oorsprong, in);
        if (fout != null) {
            p.sendOverlayMessage(fout.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.4f);
            return;
        }
        zet(level, oorsprong, in, d, r.ordinal(), p.getUUID());
        stack.shrink(1);
        level.playSound(null, BlockPos.containing(mid), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
        level.playSound(null, BlockPos.containing(mid), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);
        p.sendOverlayMessage(Component.translatable(T + "geplaatst", d.maker()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Why the gift can't stand here (null: it can). Looks at every block of the (turned) gift. */
    @Nullable
    static Component vrij(ServerPlayer p, ServerLevel level, BlockPos oorsprong, Inhoud in) {
        List<AABB> dozen = new ArrayList<>();
        for (int i = 0; i < Inhoud.V; i++) {
            if (in.get(i).isAir()) {
                continue;
            }
            BlockPos pos = oorsprong.offset(Inhoud.x(i), Inhoud.y(i), Inhoud.z(i));
            if (!level.isInWorldBounds(pos)) {
                return Component.translatable(T + "fout.buiten");
            }
            if (!level.hasChunkAt(pos)) {
                return Component.translatable(T + "fout.laden");
            }
            if (level.dimension() == GuhwinterZone.DIMENSIE && GuhwinterZone.binnen(pos)) {
                return Component.translatable(T + "fout.plein");
            }
            BlockState s = level.getBlockState(pos);
            if (!s.canBeReplaced() || !s.getFluidState().isEmpty() || s.getBlock() instanceof SurpriseBlok) {
                return Component.translatable(T + "fout.in_de_weg", s.getBlock().getName());
            }
            if (!level.mayInteract(p, pos) || Protected.at(level, pos)) {
                return Component.translatable(T + "fout.beschermd");
            }
            BlockEvent.EntityPlaceEvent e = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos),
                    level.getBlockState(pos.below()), p);
            if (NeoForge.EVENT_BUS.post(e).isCanceled()) {
                return Component.translatable(T + "fout.beschermd");
            }
            dozen.add(new AABB(pos));
        }
        AABB alles = new AABB(oorsprong).expandTowards(Inhoud.N - 1, Inhoud.N - 1, Inhoud.N - 1);
        for (Entity e : level.getEntities((Entity) null, alles, x -> x instanceof LivingEntity || x instanceof VehicleEntity)) {
            if (e.isSpectator()) {
                continue;
            }
            for (AABB b : dozen) {
                if (b.intersects(e.getBoundingBox())) {
                    return Component.translatable(T + "fout.iemand", e.getDisplayName());
                }
            }
        }
        return null;
    }

    /** Puts the gift down (no checks): the kern on its first block, a part on every other block. */
    static void zet(ServerLevel level, BlockPos oorsprong, Inhoud in, SurpriseData d, int draai, UUID plaatser) {
        int eerste = in.eerste();
        if (eerste < 0) {
            return;
        }
        BlockPos kern = oorsprong.offset(Inhoud.x(eerste), Inhoud.y(eerste), Inhoud.z(eerste));
        BlockState ks = in.get(eerste);
        level.setBlock(kern, LootjesFeature.SURPRISE.get().defaultBlockState().setValue(SurpriseBlok.LICHT, licht(ks)), Block.UPDATE_ALL);
        if (level.getBlockEntity(kern) instanceof SurpriseBlokEntity be) {
            be.zetKern(ks, d, draai, oorsprong, plaatser);
            level.sendBlockUpdated(kern, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
        }
        for (int i = eerste + 1; i < Inhoud.V; i++) {
            BlockState s = in.get(i);
            if (s.isAir()) {
                continue;
            }
            BlockPos pos = oorsprong.offset(Inhoud.x(i), Inhoud.y(i), Inhoud.z(i));
            level.setBlock(pos, LootjesFeature.SURPRISE_DEEL.get().defaultBlockState().setValue(SurpriseBlok.LICHT, licht(s)), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof SurpriseBlokEntity be) {
                be.zetDeel(s, kern);
                level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static int licht(BlockState s) {
        return Math.max(0, Math.min(15, s.getLightEmission()));
    }

    // =====================================================================================================================
    // picking up, looking at, breaking
    // =====================================================================================================================

    /** The kern's block entity of the gift this block belongs to (null: not a gift, or its kern is gone). */
    @Nullable
    static SurpriseBlokEntity kern(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SurpriseBlokEntity be)) {
            return null;
        }
        if (be.isKern()) {
            return be;
        }
        BlockPos k = be.kernPos();
        return k != null && level.getBlockEntity(k) instanceof SurpriseBlokEntity kb && kb.isKern() ? kb : null;
    }

    /** Shift + right-click: the one who placed it gets the gift back as its item. */
    public static boolean pak(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        SurpriseBlokEntity k = kern(level, pos);
        if (k == null) {
            weesWeg(level, pos);
            return true;
        }
        if (k.plaatser != null && !k.plaatser.equals(p.getUUID())) {
            melding(p, T + "niet_van_jou");
            return true;
        }
        if (p.getInventory().getFreeSlot() < 0) {
            melding(p, T + "tas_vol");
            return true;
        }
        SurpriseData d = k.gift;
        haalWeg(level, k);
        ItemStack s = SurpriseItem.maak(d);
        p.getInventory().add(s);
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.sendOverlayMessage(Component.translatable(T + "opgepakt", d.maker()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Takes every block of this gift away (the kern last). */
    static void haalWeg(ServerLevel level, SurpriseBlokEntity k) {
        BlockPos o = k.oorsprong();
        BlockPos kp = k.getBlockPos();
        if (o != null && k.gift != null) {
            Inhoud in = k.gift.lees().gedraaid(k.draai());
            for (int i = 0; i < Inhoud.V; i++) {
                if (in.get(i).isAir()) {
                    continue;
                }
                BlockPos pos = o.offset(Inhoud.x(i), Inhoud.y(i), Inhoud.z(i));
                if (!pos.equals(kp) && level.getBlockEntity(pos) instanceof SurpriseBlokEntity be && kp.equals(be.kernPos())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        level.setBlock(kp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** A part whose kern is gone: it just goes (nothing to keep). */
    private static void weesWeg(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof SurpriseBlok) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** A plain right-click: whose gift is this? */
    static void toon(ServerPlayer p, BlockPos pos) {
        SurpriseBlokEntity k = kern(p.level(), pos);
        if (k == null || k.gift == null) {
            return;
        }
        boolean eigen = k.plaatser == null || k.plaatser.equals(p.getUUID());
        p.sendOverlayMessage(Component.translatable(T + (eigen ? "info.eigen" : "info"), k.gift.maker()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- events -----------------------------------------------------------------------------------------------------------

    /** Shift + right-click on any block of a gift (with or without something in your hand) = pick it up. */
    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) {
            return;
        }
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof SurpriseBlok)) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            pak(p, event.getPos());
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

    /** Nobody breaks a block out of a gift (creative neither); a leftover part without its kern may go. */
    static void opBreek(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof SurpriseBlok)) {
            return;
        }
        if (kern(level, event.getPos()) == null) {
            return;   // (a leftover)
        }
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer p) {
            melding(p, T + "niet_slopen");
        }
    }

    static void melding(ServerPlayer p, String sleutel, Object... args) {
        p.sendOverlayMessage(Component.translatable(sleutel, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
