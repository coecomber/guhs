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
 *   <li>{@link BoomKaartje}s hang on the open sides of the tree (west, then north and south) showing the submitted drawings,
 *       swapping every half minute when there are more drawings than cards.</li>
 * </ul>
 */
public final class KerstboomTekeningen {
    /** Right-clicking the tree opens the editor (switch off when another slice handles the tree's clicks). */
    public static volatile boolean boomKlik = true;
    /** The cards: how many at most (west {@link #WEST_RIJEN} x {@link #KOLOMMEN}, then one row of 5 on the north and south). */
    public static int KAARTJES_MAX = 20;
    // Fitted to plein's tree in Krijn & Corine's house (anchor = trunk base = the floor you stand on, y 32; from +2 up its
    // leaves reach 2.5 from the trunk's middle, the layer at +1 reaches 3.5 but ends at +2; the ceiling at +8). The cards hang
    // 2.95 out (just outside the leaves), facing outwards, the first row centred 2.4 above the floor, rows and columns 0.8
    // apart (a card is 0.64 square).
    // 1.10.11: NO card on the east side any more: the house's east wall is only 3.5 from the trunk, so those cards hung in a
    // 0.45 gap facing the wall and could hardly be seen (card 1 among them). Only the open sides now: WEST first (the room
    // side, where kerstboom_voor is and where you see the tree from the hearth and the table): 2 rows of 5; then NORTH and
    // SOUTH alternately, one row of 5 each (a second row there only if KAARTJES_MAX is raised above 20).
    public static double KANT = 2.95, HOOGTE_VAN = 2.08, RIJ_STAP = 0.8, KOLOM_STAP = 0.8;
    public static int KOLOMMEN = 5, WEST_RIJEN = 2;
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
                double[] plek = plek(i);
                k.snapTo(boom.getX() + 0.5 + plek[0], boom.getY() + plek[1], boom.getZ() + 0.5 + plek[2], (float) plek[3], 0f);
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

    /**
     * Card i's spot from the trunk's middle (dx, dy above the anchor, dz) and its yaw (the way its face looks; renderer: yaw
     * 90 = east, -90 = west, 0 = north, 180 = south). Cards 0 .. {@code WEST_RIJEN * KOLOMMEN - 1} hang on the west side
     * (facing west), row by row; after that they alternate north (facing north) and south (facing south). Within a row the
     * columns go middle, +1, -1, +2, -2 (along north-south on the west side, along east-west on the others).
     */
    public static double[] plek(int i) {
        int west = WEST_RIJEN * KOLOMMEN;
        int kolom, rij;
        char kant;
        if (i < west) {
            kant = 'W';
            kolom = i % KOLOMMEN;
            rij = i / KOLOMMEN;
        } else {
            int j = i - west;
            kant = j % 2 == 0 ? 'N' : 'Z';
            kolom = (j / 2) % KOLOMMEN;
            rij = (j / 2) / KOLOMMEN;
        }
        double zij = (kolom == 0 ? 0 : ((kolom + 1) / 2) * (kolom % 2 == 1 ? 1 : -1)) * KOLOM_STAP;
        double y = HOOGTE_VAN + rij * RIJ_STAP;
        return switch (kant) {
            case 'W' -> new double[] {-KANT, y, zij, -90.0};
            case 'N' -> new double[] {zij, y, -KANT, 0.0};
            default -> new double[] {zij, y, KANT, 180.0};
        };
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
