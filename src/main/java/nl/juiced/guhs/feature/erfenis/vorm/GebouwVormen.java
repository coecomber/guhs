package nl.juiced.guhs.feature.erfenis.vorm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * De Erfenis 1.9 "Een opknapper!": the approved render designs (renders-erfenis) as real blocks. Every building has 10
 * levels (mammoetwei 3) and can be shown in any of the 5 palettes (T1..T5) and 4 rotations (quarter turns clockwise, like
 * the POC). The shapes come from {@code data/guhs/erfenis/vormen/**}, exported block for block from the render scripts by
 * {@code renders-erfenis/_bron/export_vormen.py} (see NOTITIES_VORMEN.md).
 * <p>
 * Placing goes through a small queue: up to {@link #PER_TICK} block changes right away (so almost every stamp is done
 * when the call returns), the rest over the next ticks, in order; {@link #bezig()} says whether work is still waiting.
 * The creatures of a shape ({@link DecoWezens}) appear when its blocks are done.
 */
public final class GebouwVormen {
    /** The tag of every decorative creature this class spawns. */
    public static final String TAG = DecoWezens.TAG;
    /** Stamping clears this many blocks above the ground (the tallest shape, the Hoeve L10 torentje, is ~20). */
    public static final int WIS_HOOGTE = 40;
    /** Block changes per tick before the rest waits for the next tick. */
    static final int PER_TICK = 12_000;
    /**
     * The single switch for the optional overhang layer (the renders' roof edges, ears, shutters and molen sails that stick
     * out past the footprint, up to 4 blocks for the sails). Off for now (the contract: only the footprint changes). When on,
     * a stamp also places those blocks, but only into air, never over anything else; {@link #wis} does not take them away.
     */
    public static volatile boolean OVERHANG = false;
    private static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Deque<Klus> RIJ = new ArrayDeque<>();
    private static volatile Map<String, int[]> index;
    private static volatile Map<String, JsonObject> decoIndex;
    private static volatile Map<String, BlockState[][]> paden;

    private GebouwVormen() {
    }

    // ================================================================================================= the contract API

    /**
     * Places building {@code gebouwId} at {@code level} in palette {@code palet} (1..5) with its north-west corner (after
     * turning) at {@code minHoek} (= its ground block), turned {@code rot} quarter turns clockwise: the whole footprint
     * (building + flowers and moss on the part it doesn't use yet), everything else in it cleared, its old decorative
     * creatures removed and the new ones spawned. An unknown id or level only clears the footprint.
     */
    public static void stempel(ServerLevel lvl, String gebouwId, int level, int palet, BlockPos minHoek, int rot) {
        VormBestand v = vorm(gebouwId, level);
        if (v == null) {
            int[] m = maat(gebouwId);
            wis(lvl, minHoek, (rot & 1) == 0 ? m[0] : m[1], (rot & 1) == 0 ? m[1] : m[0]);
            return;
        }
        voerUit(lvl, Klus.vorm(lvl, v, palet, minHoek, rot, true));
    }

    /**
     * (1.9 afwerking) Runs {@code klus} once all stamping queued before it is done (Opa's kelder under the Hoeve, Bertha on the
     * Muziektent): stamps are spread over ticks, so extra blocks must wait for the shape they go into.
     */
    public static void daarna(ServerLevel lvl, Runnable klus) {
        voerUit(lvl, Klus.actie(lvl, klus));
    }

    /** A footprint back to plain grass with air above (also takes away its decorative creatures). */
    public static void wis(ServerLevel lvl, BlockPos minHoek, int breedte, int diepte) {
        voerUit(lvl, Klus.wis(lvl, minHoek, breedte, diepte));
    }

    /** {breedte, diepte} of a building before turning ({0, 0} for an unknown id). */
    public static int[] maat(String gebouwId) {
        int[] i = index().get(gebouwId);
        return i == null ? new int[] {0, 0} : new int[] {i[0], i[1]};
    }

    /** The shape tier 1..5 of a building at a level (= the palette a new building starts in). */
    public static int vormTier(String gebouwId, int level) {
        int[] i = index().get(gebouwId);
        if (i == null) {
            return 1;
        }
        int n = i[2];
        return i[3 + Math.max(1, Math.min(n, level)) - 1];
    }

    /** How many levels a building has (10, the mammoetwei 3; 0 for an unknown id). */
    public static int levels(String gebouwId) {
        int[] i = index().get(gebouwId);
        return i == null ? 0 : i[2];
    }

    /** All building ids that have shapes, in the order of the contract. */
    public static List<String> gebouwen() {
        return List.copyOf(index().keySet());
    }

    /** An overgrown 8x8 patch: soort 0 struik, 1 omgevallen boom, 2 rotsblok, 3 brandnetels, 4 oude kar, 5 molshoopjes. */
    public static void stempelPlukje(ServerLevel lvl, BlockPos minHoek, int soort) {
        VormBestand v = VormBestand.laad("plukje/" + Math.floorMod(soort, 6));
        if (v == null) {
            wis(lvl, minHoek, 8, 8);
            return;
        }
        voerUit(lvl, Klus.vorm(lvl, v, 1, minHoek, 0, true));
    }

    /**
     * A deco piece at {@code pos} (its north-west corner after turning, on the ground layer), turned {@code rot} quarter turns.
     * Only its own footprint changes; hekjes connect to the hekjes around them.
     */
    public static void stempelDeco(ServerLevel lvl, String decoId, BlockPos pos, int rot) {
        VormBestand v = VormBestand.laad("deco/" + decoId);
        if (v == null) {
            return;
        }
        voerUit(lvl, Klus.vorm(lvl, v, 1, pos, rot, true).metBuren());
    }

    /** {breedte, diepte} of a deco piece before turning ({1, 1} for paths, {0, 0} when unknown). */
    public static int[] decoMaat(String decoId) {
        JsonObject o = decoIndex().get(decoId);
        if (o == null) {
            return padSoorten().contains(decoId) ? new int[] {1, 1} : new int[] {0, 0};
        }
        return new int[] {o.get("w").getAsInt(), o.get("d").getAsInt()};
    }

    /** All deco ids (paths not included: see {@link #padSoorten()}). */
    public static List<String> decoIds() {
        return List.copyOf(decoIndex().keySet());
    }

    /** The deco catalogue entry (nr, naam, w, d, soort) of a piece, or null. */
    public static JsonObject decoInfo(String decoId) {
        return decoIndex().get(decoId);
    }

    /**
     * One path tile at {@code pos} (the ground block). {@code verbindingen} = which neighbours are path too (N 1, E 2,
     * S 4, W 8): the tile picks its straight/corner/crossing/end look from that.
     */
    public static void pad(ServerLevel lvl, BlockPos pos, String padSoort, int verbindingen) {
        BlockState[][] p = paden().get(padSoort);
        if (p == null) {
            return;
        }
        BlockState[] t = p[verbindingen & 15];
        boolean om = t[2] != null && ((pos.getX() + pos.getZ()) & 1) == 1;   // stapsteentjes: every other tile is grass
        zet(lvl, pos, om ? t[2] : t[0]);
        BlockState boven = lvl.getBlockState(pos.above());
        if (t[1] != null) {
            zet(lvl, pos.above(), t[1]);
        } else if (boven.canBeReplaced() || !boven.getFluidState().isEmpty()) {
            zet(lvl, pos.above(), Blocks.AIR.defaultBlockState());   // grass and flowers on a path go away
        }
    }

    /** All path kinds. */
    public static List<String> padSoorten() {
        return List.copyOf(paden().keySet());
    }

    /** Is placing work still waiting for the next ticks? */
    public static boolean bezig() {
        return !RIJ.isEmpty();
    }

    // ================================================================================================= internals

    static VormBestand vorm(String gebouwId, int level) {
        if (!index().containsKey(gebouwId)) {
            return null;
        }
        int l = Math.max(1, Math.min(levels(gebouwId), level));
        return VormBestand.laad(gebouwId + "/L" + l);
    }

    private static void voerUit(ServerLevel lvl, Klus k) {
        if (!RIJ.isEmpty()) {
            RIJ.addLast(k);
            return;
        }
        if (!k.werk(PER_TICK)) {
            RIJ.addLast(k);
        }
    }

    /** Server tick: the waiting work, in order. */
    static void tick() {
        int budget = PER_TICK;
        while (!RIJ.isEmpty() && budget > 0) {
            Klus k = RIJ.peekFirst();
            int voor = k.gedaan;
            if (k.werk(budget)) {
                RIJ.pollFirst();
            }
            budget -= Math.max(1, k.gedaan - voor);
        }
    }

    /** Finishes all waiting work now (gametests, the dev command). */
    public static void klaarNu() {
        while (!RIJ.isEmpty()) {
            if (RIJ.peekFirst().werk(Integer.MAX_VALUE)) {
                RIJ.pollFirst();
            }
        }
    }

    static boolean zet(ServerLevel lvl, BlockPos p, BlockState st) {
        if (lvl.getBlockState(p) != st) {
            lvl.setBlock(p, st, VLAG);
            return true;
        }
        return false;
    }

    static boolean verbindt(BlockState s) {
        Block b = s.getBlock();
        return b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock;
    }

    private static Map<String, int[]> index() {
        Map<String, int[]> i = index;
        if (i == null) {
            i = new LinkedHashMap<>();
            Map<String, JsonObject> deco = new LinkedHashMap<>();
            JsonObject o = VormBestand.lees("index");
            if (o != null) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("gebouwen").entrySet()) {
                    JsonObject g = e.getValue().getAsJsonObject();
                    var vorm = g.getAsJsonArray("vorm");
                    int[] r = new int[3 + vorm.size()];
                    r[0] = g.get("w").getAsInt();
                    r[1] = g.get("d").getAsInt();
                    r[2] = g.get("levels").getAsInt();
                    for (int k = 0; k < vorm.size(); k++) {
                        r[3 + k] = vorm.get(k).getAsInt();
                    }
                    i.put(e.getKey(), r);
                }
                if (o.has("deco")) {
                    for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("deco").entrySet()) {
                        deco.put(e.getKey(), e.getValue().getAsJsonObject());
                    }
                }
            }
            decoIndex = Collections.unmodifiableMap(deco);
            index = i = Collections.unmodifiableMap(i);
        }
        return i;
    }

    private static Map<String, JsonObject> decoIndex() {
        index();
        return decoIndex;
    }

    private static Map<String, BlockState[][]> paden() {
        Map<String, BlockState[][]> p = paden;
        if (p == null) {
            p = new LinkedHashMap<>();
            JsonObject o = VormBestand.lees("pad");
            if (o != null) {
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    var a = e.getValue().getAsJsonArray();
                    BlockState[][] t = new BlockState[16][3];
                    for (int m = 0; m < 16 && m < a.size(); m++) {
                        var tegel = a.get(m).getAsJsonArray();
                        t[m][0] = VormBestand.state(tegel.get(0).getAsString());
                        t[m][1] = tegel.size() > 1 && !tegel.get(1).isJsonNull() ? VormBestand.state(tegel.get(1).getAsString()) : null;
                        t[m][2] = tegel.size() > 2 && !tegel.get(2).isJsonNull() ? VormBestand.state(tegel.get(2).getAsString()) : null;
                    }
                    p.put(e.getKey(), t);
                }
            }
            paden = p = Collections.unmodifiableMap(p);
        }
        return p;
    }

    // ================================================================================================= one job

    /** One stamp or clear: the block changes in order, then the connection pass, then the creatures. */
    static final class Klus {
        final ServerLevel lvl;
        final BlockPos o;
        final int bx, bz;          // footprint size after turning
        final VormBestand v;       // null = clear
        final BlockState[] st;
        final int rot;
        boolean buren;
        int gedaan;
        private int kolom, y;      // progress
        private final int yLaag, yHoog;
        private final List<BlockPos> verbind = new ArrayList<>();
        private int fase;
        private Runnable actie;

        private Klus(ServerLevel lvl, BlockPos o, int bx, int bz, VormBestand v, BlockState[] st, int rot, boolean wisBoven) {
            this.lvl = lvl;
            this.o = o.immutable();
            this.bx = bx;
            this.bz = bz;
            this.v = v;
            this.st = st;
            this.rot = rot & 3;
            this.yLaag = v == null ? -1 : Math.min(-1, v.ymin);
            this.yHoog = wisBoven ? Math.max(WIS_HOOGTE, v == null ? 0 : v.ymax) : v.ymax;
            this.y = yLaag;
        }

        static Klus vorm(ServerLevel lvl, VormBestand v, int palet, BlockPos o, int rot, boolean wisBoven) {
            boolean draai = (rot & 1) == 1;
            return new Klus(lvl, o, draai ? v.d : v.w, draai ? v.w : v.d, v, v.states(palet), rot, wisBoven);
        }

        static Klus wis(ServerLevel lvl, BlockPos o, int bx, int bz) {
            return new Klus(lvl, o, bx, bz, null, null, 0, true);
        }

        static Klus actie(ServerLevel lvl, Runnable r) {
            Klus k = new Klus(lvl, BlockPos.ZERO, 0, 0, null, null, 0, true);   // (v == null needs wisBoven for its height)
            k.actie = r;
            return k;
        }

        Klus metBuren() {
            buren = true;
            return this;
        }

        /** Works up to {@code budget} block changes; true when completely done. */
        boolean werk(int budget) {
            int start = gedaan;
            if (actie != null) {
                actie.run();
                gedaan++;
                return true;
            }
            if (fase == 0) {
                if (kolom == 0 && y == yLaag) {
                    DecoWezens.weg(lvl, o, bx, bz, WIS_HOOGTE);
                }
                int n = bx * bz;
                while (kolom < n) {
                    int wx = kolom % bx, wz = kolom / bx;
                    while (y <= yHoog) {
                        BlockState s = blok(wx, y, wz);
                        if (s != null) {
                            BlockPos p = o.offset(wx, y, wz);
                            if (zet(lvl, p, s)) {
                                gedaan++;
                            }
                            if (verbindt(s)) {
                                verbind.add(p);
                            }
                        }
                        y++;
                        if (gedaan - start >= budget) {
                            return false;
                        }
                    }
                    y = yLaag;
                    kolom++;
                }
                fase = 1;
            }
            if (fase == 1) {
                if (OVERHANG && v != null) {
                    int[] oh = v.overhang;
                    for (int i = 0; i + 3 < oh.length; i += 4) {
                        BlockPos p = o.offset(Draai.x(oh[i], oh[i + 2], v.w, v.d, rot), oh[i + 1], Draai.z(oh[i], oh[i + 2], v.w, v.d, rot));
                        if (lvl.getBlockState(p).isAir()) {
                            BlockState s = Draai.state(st[oh[i + 3]], rot);
                            zet(lvl, p, s);
                            if (verbindt(s)) {
                                verbind.add(p);
                            }
                        }
                    }
                }
                for (BlockPos p : verbind) {
                    BlockState s = lvl.getBlockState(p);
                    zet(lvl, p, Block.updateFromNeighbourShapes(s, lvl, p));
                }
                if (buren) {
                    for (int wx = -1; wx <= bx; wx++) {
                        for (int wz = -1; wz <= bz; wz++) {
                            if (wx >= 0 && wx < bx && wz >= 0 && wz < bz) {
                                continue;
                            }
                            for (int yy = 0; yy <= 2; yy++) {
                                BlockPos p = o.offset(wx, yy, wz);
                                BlockState s = lvl.getBlockState(p);
                                if (verbindt(s)) {
                                    zet(lvl, p, Block.updateFromNeighbourShapes(s, lvl, p));
                                }
                            }
                        }
                    }
                }
                gedaan += verbind.size();
                fase = 2;
            }
            if (fase == 2) {
                if (v != null) {
                    DecoWezens.zet(lvl, v, o, bx, bz, rot);
                }
                fase = 3;
            }
            return true;
        }

        /** The block at a turned cell, or null when this job leaves it alone. */
        BlockState blok(int wx, int y, int wz) {
            if (v == null) {
                return y < 0 ? Blocks.DIRT.defaultBlockState() : y == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            // back from the turned cell to the shape's own cell
            int lx, lz;
            switch (rot) {
                case 1 -> {
                    lx = wz;
                    lz = v.d - 1 - wx;
                }
                case 2 -> {
                    lx = v.w - 1 - wx;
                    lz = v.d - 1 - wz;
                }
                case 3 -> {
                    lx = v.w - 1 - wz;
                    lz = wx;
                }
                default -> {
                    lx = wx;
                    lz = wz;
                }
            }
            if (y < v.ymin || y > v.ymax) {
                return y < 0 ? Blocks.DIRT.defaultBlockState() : y == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            BlockState s = st[v.at(lx, y, lz)];
            if (y == 0 && s.isAir()) {
                s = Blocks.GRASS_BLOCK.defaultBlockState();
            }
            // 1.9.8: a thin layer (the T3 path, landdiertjes_steentjespad, is a carpet) in the ground layer left a sunken hole in
            // the ground (Marktplein, Hoeve, Kippenhok ...): it goes on top of a ground block instead, one higher
            if (y == 0 && dun(s)) {
                s = ONDER_DUN;
            } else if (y == 1 && s.isAir() && v.ymin <= 0 && dun(st[v.at(lx, 0, lz)])) {
                s = st[v.at(lx, 0, lz)];
            }
            return Draai.state(s, rot);
        }
    }

    /** (1.9.8) The ground under a thin layer that a shape puts in its ground layer. */
    static final BlockState ONDER_DUN = Blocks.COARSE_DIRT.defaultBlockState();

    /** (1.9.8) A thin layer (a carpet: 1/16 high) that must lie on the ground instead of being the ground. */
    static boolean dun(BlockState s) {
        return s.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock;
    }

    static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
