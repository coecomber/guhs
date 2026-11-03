package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * The finale, "De Grote Knabbelzaak": the courtroom of the nearest neighbour village. It is a small set stamped once in a
 * reserved cell of the erfenis dimension that no base ever gets ({@link #CEL_X}, {@link #CEL_Z}); the player goes there,
 * watches the opening (Edelachtbare Knabbelhamer, Morris' first claim), and three times picks the right evidence and shouts
 * "BEZWAAR, NJEG!" (a wrong pick: a sweet hint, try again). The third ends with the verdict and Morris' sweet ending; then
 * the rewards and back home. Leaving halfway is fine: the round is remembered; click the judge to go on.
 */
public final class Rechtbank {
    /** The reserved cell: far away from the spiral of bases around (0, 0). */
    public static final int CEL_X = 0, CEL_Z = -3000;
    static final String BEZWAAR = "sherlock_bezwaar", BEZWAAR_FOUT = "sherlock_bezwaar_fout";
    private static final int BEWIJS = 100;

    private record Later(long tick, Runnable wat) {
    }

    private static final List<Later> LATER = new ArrayList<>();

    private Rechtbank() {
    }

    /** The anchor: the middle of the courtroom floor's first air layer (the scenes are relative to it). */
    public static BlockPos anker() {
        return Plot.oorsprong(CEL_X, CEL_Z).offset(Plot.MAAT / 2, 1, Plot.MAAT / 2);
    }

    // =====================================================================================================================
    // the flow
    // =====================================================================================================================

    static void begin(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = Erfenis.level(server);
        if (level == null) {
            return;
        }
        Voortgang v = SherlockData.van(server, p.getUUID());
        bouw(level);
        zetNpcs(level);
        naarBinnen(p, level);
        p.sendSystemMessage(Component.translatable("sherlock.rechtbank.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (v.ronde == 0) {
            later(server, 30, () -> Cutscenes.speel(p, SherlockScenes.BEGIN, anker(), Rotation.NONE, Rechtbank::kies));
        } else {
            later(server, 20, () -> kies(p));
        }
    }

    static void naarBinnen(ServerPlayer p, ServerLevel level) {
        if (p.level() != level || !Erfenis.in(p)) {
            // (remember the way back to the world, like Erfenis.gaNaar does)
            Erfenis.gaNaar(p, Erfenis.eigenBase(p));
        }
        Vec3 plek = Vec3.atBottomCenterOf(anker()).add(-0.5, 0, -0.5).add(SherlockScenes.SPELER);
        p.teleport(new TeleportTransition(level, plek, Vec3.ZERO, 180f, 5f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    /** "Kies je bewijs": Morris' claim of this round, the evidence as answers. */
    static void kies(ServerPlayer p) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        if (v.ronde >= 3 || !Zaken.RECHTSZAAK.id().equals(v.actief)) {
            return;
        }
        Entity morris = Cast.van(p.level().getServer(), Cast.IEDEREEN, "rechtbank:morris");
        Praat.Optie[] opties = new Praat.Optie[Zaken.BEWIJS.size()];
        for (int i = 0; i < opties.length; i++) {
            opties[i] = new Praat.Optie(BEWIJS + i, "sherlock.bewijs." + Zaken.BEWIJS.get(i));
        }
        Praat.scene(p, BEZWAAR, List.of(new Praat.Regel(morris, morris == null ? "sherlock.wie.morris" : "", "sherlock.rechtbank.bewering." + (v.ronde + 1)),
                new Praat.Regel(Cast.van(p.level().getServer(), Cast.IEDEREEN, "rechtbank:sherlock"), "", "sherlock.rechtbank.kies")), opties);
    }

    static void opBewijs(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie < BEWIJS || optie - BEWIJS >= Zaken.BEWIJS.size()) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        Voortgang v = SherlockData.van(server, p.getUUID());
        if (v.ronde >= 3 || !Zaken.RECHTSZAAK.id().equals(v.actief)) {
            return;
        }
        String bewijs = Zaken.BEWIJS.get(optie - BEWIJS);
        Praat.sluit(p);
        if (bewijs.equals(Zaken.BEZWAAR.get(v.ronde))) {
            v.ronde++;
            SherlockData.veranderd(server);
            int r = v.ronde;
            Cutscenes.speel(p, SherlockScenes.BEZWAAR.get(r - 1), anker(), Rotation.NONE, sp -> {
                if (r < 3) {
                    kies(sp);
                } else {
                    klaar(sp, true);
                }
            });
        } else {
            Praat.scene(p, BEZWAAR_FOUT, List.of(
                    new Praat.Regel(Cast.van(server, Cast.IEDEREEN, "rechtbank:rechter"), "", "sherlock.rechtbank.fout." + bewijs),
                    new Praat.Regel(Cast.van(server, Cast.IEDEREEN, "rechtbank:sherlock"), "", "sherlock.rechtbank.hint." + (v.ronde + 1))));
        }
    }

    /** The case is won: rewards, the board in the Speurboekje, and home again (heen = from the courtroom). */
    static void klaar(ServerPlayer p, boolean heen) {
        Sherlock.losOp(p, Zaken.RECHTSZAAK);
        if (heen && Erfenis.in(p)) {
            later(p.level().getServer(), 10, () -> {
                Erfenis.gaNaar(p, Erfenis.eigenBase(p));
                p.sendSystemMessage(Component.translatable("sherlock.rechtbank.thuis").withStyle(ChatFormatting.LIGHT_PURPLE));
            });
        }
    }

    /** Clicking a character in the courtroom: the judge goes on with the case, the others say something sweet. */
    static void klik(ServerPlayer p, Entity e) {
        Voortgang v = SherlockData.van(p.level().getServer(), p.getUUID());
        String wie = e instanceof nl.juiced.guhs.entity.GuhNpcEntity n ? n.roleData.getStringOr(Cast.GETUIGE, "") : "";
        if (Zaken.RECHTSZAAK.id().equals(v.actief) && v.ronde < 3 && (wie.equals("rechter") || wie.equals("morris"))) {
            if (v.ronde == 0 && !Cutscenes.gezien(p, SherlockScenes.BEGIN.id())) {
                Cutscenes.speel(p, SherlockScenes.BEGIN, anker(), Rotation.NONE, Rechtbank::kies);
            } else {
                kies(p);
            }
            return;
        }
        if (wie.equals("rechter") && v.isOpgelost(Zaken.RECHTSZAAK.id())) {
            Praat.open(p, e, Gesprek.VERHAAL, "sherlock.rechtbank.terug", new Object[0], new Praat.Optie(8, "sherlock.optie.naar_huis"),
                    new Praat.Optie(9, "sherlock.optie.doei"));
            return;
        }
        Praat.open(p, e, Gesprek.VERHAAL, "sherlock.klik.rechtbank." + (wie.isEmpty() ? "iemand" : wie), new Object[0],
                new Praat.Optie(8, "sherlock.optie.naar_huis"), new Praat.Optie(9, "sherlock.optie.doei"));
    }

    static void opVerhaalAntwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie == 8) {
            Praat.sluit(p);
            Erfenis.gaNaar(p, Erfenis.eigenBase(p));
        } else if (optie == 9) {
            Praat.sluit(p);
        }
    }

    // =====================================================================================================================
    // the people in the room
    // =====================================================================================================================

    static void zetNpcs(ServerLevel level) {
        BlockPos a = anker();
        Vec3 o = Vec3.atBottomCenterOf(a).add(-0.5, 0, -0.5);
        zet(level, o, "rechter", Kind.KNABBELHAMER, SherlockScenes.RECHTER, 0);
        zet(level, o, "morris", Kind.MORRIS_MIKA, SherlockScenes.MORRIS, 180);
        zet(level, o, "pim", Kind.PIM_MIKA, SherlockScenes.PIM, 180);
        zet(level, o, "sherlock", Kind.SHERLOCK_GUH, SherlockScenes.SHERLOCK, 180);
        zet(level, o, "vadson", Kind.DOKTER_VADSON, SherlockScenes.VADSON, 180);
        zet(level, o, "oehoe", Kind.OEHOE, SherlockScenes.OEHOE, 180);
        zet(level, o, "guhrrit", Kind.GUHRRIT_VAKANTIE, SherlockScenes.GUHRRIT, 180);
        zet(level, o, "oma", Kind.DROOM_OMAGUH, SherlockScenes.OMA, 180);
    }

    private static void zet(ServerLevel level, Vec3 o, String wie, Kind kind, Vec3 rel, float yaw) {
        Entity e = Cast.zet(level, Cast.IEDEREEN, "rechtbank:" + wie, "rechtbank", kind, o.add(rel), yaw,
                wie.equals("oma") ? "sherlock.wie.oma" : null, false);
        if (e instanceof nl.juiced.guhs.entity.GuhNpcEntity n) {
            n.roleData.putString(Cast.GETUIGE, wie);
        }
    }

    // =====================================================================================================================
    // the set (stamped once; a gold block under the middle says it is there)
    // =====================================================================================================================

    static boolean staat(ServerLevel level) {
        return level.getBlockState(anker().below(2)).is(Blocks.GOLD_BLOCK);
    }

    static void bouw(ServerLevel level) {
        if (staat(level)) {
            return;
        }
        BlockPos a = anker();
        // the island: grass with dirt under it, a bit wider than the building
        for (int x = -14; x <= 14; x++) {
            for (int z = -16; z <= 17; z++) {
                if (x * x / 196.0 + z * z / 289.0 > 1.05) {
                    continue;
                }
                zet(level, a, x, -1, z, Blocks.GRASS_BLOCK.defaultBlockState());
                zet(level, a, x, -2, z, Blocks.DIRT.defaultBlockState());
                if (Math.abs(x) < 11 && Math.abs(z) < 13) {
                    zet(level, a, x, -3, z, Blocks.DIRT.defaultBlockState());
                }
                for (int y = 0; y <= 7; y++) {
                    zet(level, a, x, y, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
        zet(level, a, 0, -2, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        // floor, walls, roof
        for (int x = -8; x <= 8; x++) {
            for (int z = -11; z <= 11; z++) {
                boolean rand = Math.abs(x) == 8 || Math.abs(z) == 11;
                zet(level, a, x, -1, z, rand ? Blocks.PINK_TERRACOTTA.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState());
                zet(level, a, x, 6, z, rand ? Blocks.PINK_TERRACOTTA.defaultBlockState()
                        : (x % 4 == 0 && z % 4 == 0 ? Blocks.SEA_LANTERN : Blocks.STRIPPED_DARK_OAK_WOOD).defaultBlockState());
                if (rand) {
                    for (int y = 0; y <= 5; y++) {
                        BlockState s = y == 0 ? Blocks.PINK_TERRACOTTA.defaultBlockState()
                                : (Math.abs(x) == 8 && Math.abs(z) == 11) ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()
                                : Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
                        boolean raam = (y == 2 || y == 3) && (Math.abs(x) == 8 && Math.floorMod(z, 4) == 2 || Math.abs(z) == 11 && Math.abs(x) >= 3 && Math.abs(x) <= 5);
                        boolean deur = z == 11 && Math.abs(x) <= 1 && y <= 2;
                        zet(level, a, x, y, z, deur ? Blocks.AIR.defaultBlockState() : raam ? Blocks.GLASS_PANE.defaultBlockState() : s);
                    }
                }
            }
        }
        // the roof's little pink rim
        for (int x = -9; x <= 9; x++) {
            zet(level, a, x, 6, -12, Blocks.PINK_TERRACOTTA.defaultBlockState());
            zet(level, a, x, 6, 12, Blocks.PINK_TERRACOTTA.defaultBlockState());
        }
        // the aisle: a red carpet from the door to the bench
        for (int z = -6; z <= 10; z++) {
            zet(level, a, 0, 0, z, Blocks.RED_CARPET.defaultBlockState());
        }
        // the judge's platform and bench, a cheese emblem and bookcases behind it
        for (int x = -3; x <= 3; x++) {
            for (int z = -10; z <= -8; z++) {
                zet(level, a, x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            zet(level, a, x, 0, -7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            zet(level, a, x, 1, -7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        zet(level, a, 0, 1, -7, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        for (int x = -1; x <= 1; x++) {
            for (int y = 3; y <= 5; y++) {
                zet(level, a, x, y, -10, (x == 0 && y == 4 ? Blocks.YELLOW_TERRACOTTA : Blocks.HONEYCOMB_BLOCK).defaultBlockState());
            }
        }
        for (int x : new int[] {-7, -6, -5, 5, 6, 7}) {
            for (int y = 0; y <= 3; y++) {
                zet(level, a, x, y, -10, Blocks.BOOKSHELF.defaultBlockState());
            }
        }
        // the witness box (the Getuigenbank)
        for (int[] f : new int[][] {{-6, -7}, {-5, -7}, {-4, -7}, {-6, -6}, {-4, -6}}) {
            zet(level, a, f[0], 0, f[1], Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        zet(level, a, -5, 0, -6, Blocks.PINK_CARPET.defaultBlockState());
        // the two tables: ours (west) and Morris' (east)
        for (int x = 3; x <= 5; x++) {
            zet(level, a, x, 0, -4, Blocks.SPRUCE_PLANKS.defaultBlockState());
            zet(level, a, x, 1, -4, Blocks.LIGHT_GRAY_CARPET.defaultBlockState());
            zet(level, a, -x, 0, -4, Blocks.SPRUCE_PLANKS.defaultBlockState());
            zet(level, a, -x, 1, -4, Blocks.PINK_CARPET.defaultBlockState());
        }
        // the public gallery: benches facing the judge
        for (int z : new int[] {3, 5, 7, 9}) {
            for (int x = 2; x <= 6; x++) {
                BlockState bank = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
                zet(level, a, x, 0, z, bank);
                zet(level, a, -x, 0, z, bank);
            }
        }
        // lanterns at the bench and potted flowers in the corners
        zet(level, a, -3, 2, -7, Blocks.LANTERN.defaultBlockState());
        zet(level, a, 3, 2, -7, Blocks.LANTERN.defaultBlockState());
        for (int[] c : new int[][] {{-7, 10}, {7, 10}, {-7, -9}, {7, -9}}) {
            zet(level, a, c[0], 0, c[1], Blocks.POTTED_PINK_TULIP.defaultBlockState());
        }
        // two little lanterns on posts outside the door
        for (int x : new int[] {-3, 3}) {
            zet(level, a, x, 0, 13, Blocks.SPRUCE_FENCE.defaultBlockState());
            zet(level, a, x, 1, 13, Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void zet(ServerLevel level, BlockPos a, int x, int y, int z, BlockState s) {
        level.setBlock(a.offset(x, y, z), s, Block.UPDATE_CLIENTS);
    }

    // =====================================================================================================================
    // a few ticks later (after a teleport the client needs a moment)
    // =====================================================================================================================

    static synchronized void later(MinecraftServer server, int ticks, Runnable wat) {
        LATER.add(new Later(server.getTickCount() + ticks, wat));
    }

    static void tick(ServerTickEvent.Post event) {
        List<Runnable> nu = new ArrayList<>();
        synchronized (Rechtbank.class) {
            for (Iterator<Later> it = LATER.iterator(); it.hasNext(); ) {
                Later l = it.next();
                if (event.getServer().getTickCount() >= l.tick()) {
                    nu.add(l.wat());
                    it.remove();
                }
            }
        }
        nu.forEach(Runnable::run);
    }
}
