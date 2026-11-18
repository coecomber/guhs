package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The kerstboom drawings: every player hangs ONE drawing in the tree and may keep editing it until 25 Dec 23:59
 * ({@link Guhwinter#kerstboomOpen}); on 26 Dec they all go into the Kerstalbum ({@link Kerstalbum}).
 * <ul>
 *   <li>{@link #open(ServerPlayer)}: the story slice calls this when you click the tree; the editor opens with your drawing
 *       preloaded (or empty the first time);</li>
 *   <li>fallback: right-clicking the tree itself (leaves/logs within 3 blocks of the plein anchor {@code kerstboom}) does the
 *       same, unless {@link #boomKlik} is switched off (when the story handles the tree);</li>
 *   <li>a few {@link BoomKaartje}s float around the tree showing the submitted drawings, swapping every half minute.</li>
 * </ul>
 */
public final class KerstboomTekeningen {
    /** Right-clicking the tree opens the editor (switch off when another slice handles the tree's clicks). */
    public static volatile boolean boomKlik = true;
    /** The cards: how many at most, their ring around the anchor (blocks), their height above it, swap time (ticks). */
    public static int KAARTJES_MAX = 8;
    public static double RING = 2.4, HOOGTE_VAN = 1.6, HOOGTE_STAP = 0.55;
    private static final int WISSEL_TICKS = 600, KIJK_TICKS = 40, BIJ = 48;

    private static final List<BoomKaartje> KAARTJES = new ArrayList<>();
    private static int ronde;
    private static long tik;

    private KerstboomTekeningen() {
    }

    /** Opens the kerstboom editor for this player (with their own drawing preloaded), or tells them it's too late. */
    public static void open(ServerPlayer speler) {
        if (!Guhwinter.kerstboomOpen(speler.level().getServer())) {
            speler.sendOverlayMessage(Tekeningen.melding("guhwinter.tekenen.kerstboom.dicht"));
            return;
        }
        Tekeningen.openEditor(speler, Doel.KERSTBOOM, TekeningenData.get(speler.level().getServer()).kerstboomVan(speler.getUUID()));
    }

    /** All kerstboom drawings (oldest first). */
    public static List<Tekening> alle(MinecraftServer server) {
        return TekeningenData.get(server).kerstboomTekeningen();
    }

    static void init() {
        Doel.KERSTBOOM.magOpslaan((speler, bestaande) ->
                Guhwinter.kerstboomOpen(speler.level().getServer()) ? null : "guhwinter.tekenen.kerstboom.dicht");
        Doel.KERSTBOOM.opOpslaan((speler, t) -> speler.sendSystemMessage(Tekeningen.melding(
                t.versie() > 1 ? "guhwinter.tekenen.kerstboom.bijgewerkt" : "guhwinter.tekenen.kerstboom.opgehangen")));
    }

    // ------------------------------------------------------------------------------------------------ the tree

    static void klik(PlayerInteractEvent.RightClickBlock e) {
        if (!boomKlik || e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer sp)
                || e.getLevel().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        BlockPos boom = TekenPlek.kerstboom(sp.level().getServer());
        if (boom == null || !isBoom(boom, e.getPos(), e.getLevel().getBlockState(e.getPos()))) {
            return;
        }
        open(sp);
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** Leaves or logs (or a block with "kerst" in its id) within 3 blocks of the anchor, up to 10 above it. */
    static boolean isBoom(BlockPos anker, BlockPos pos, BlockState state) {
        int dx = Math.abs(pos.getX() - anker.getX()), dz = Math.abs(pos.getZ() - anker.getZ()), dy = pos.getY() - anker.getY();
        if (dx > 3 || dz > 3 || dy < -1 || dy > 10) {
            return false;
        }
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)
                || state.typeHolder().getRegisteredName().contains("kerst");
    }

    // ------------------------------------------------------------------------------------------------ the cards

    static void tick(MinecraftServer server) {
        if (++tik % KIJK_TICKS != 0) {
            return;
        }
        ServerLevel level = server.getLevel(ModDimensions.GUHMENSION);
        BlockPos boom = level == null ? null : TekenPlek.kerstboom(server);
        KAARTJES.removeIf(Entity::isRemoved);
        if (level == null || boom == null || !Guhwinter.bezig(server) || !level.isLoaded(boom) || !iemandBij(level, boom)) {
            weg();
            return;
        }
        List<Tekening> alle = alle(server);
        int n = Math.min(KAARTJES_MAX, alle.size());
        while (KAARTJES.size() > n) {
            KAARTJES.remove(KAARTJES.size() - 1).discard();
        }
        if (n == 0) {
            return;
        }
        if (tik % WISSEL_TICKS < KIJK_TICKS && alle.size() > n) {
            ronde++;
        }
        for (int i = 0; i < n; i++) {
            BoomKaartje k = i < KAARTJES.size() ? KAARTJES.get(i) : null;
            if (k == null || k.level() != level) {
                k = TekenenFeature.BOOMKAARTJE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (k == null) {
                    return;
                }
                double hoek = i * 2.39996;   // the golden angle: spread around the tree
                double x = boom.getX() + 0.5 + Math.cos(hoek) * RING, z = boom.getZ() + 0.5 + Math.sin(hoek) * RING;
                k.snapTo(x, boom.getY() + HOOGTE_VAN + i * HOOGTE_STAP, z, (float) Math.toDegrees(hoek) - 90f, 0f);
                level.addFreshEntity(k);
                if (i < KAARTJES.size()) {
                    KAARTJES.set(i, k);
                } else {
                    KAARTJES.add(k);
                }
            }
            k.zetTekening(alle.get((ronde * n + i) % alle.size()).id());
        }
    }

    private static boolean iemandBij(ServerLevel level, BlockPos boom) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(boom, BIJ)) {
                return true;
            }
        }
        return false;
    }

    static void weg() {
        for (BoomKaartje k : KAARTJES) {
            k.discard();
        }
        KAARTJES.clear();
    }

    @Nullable
    static BlockPos anker(MinecraftServer server) {
        return TekenPlek.kerstboom(server);
    }
}
