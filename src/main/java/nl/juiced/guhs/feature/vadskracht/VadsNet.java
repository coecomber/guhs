package nl.juiced.guhs.feature.vadskracht;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * One vadskracht net: the Guhdraad and knopen that hang together, and the answer of its last evaluation. Other packages
 * only read it ({@link VadsKracht#net}); {@link VadsNetten} builds and evaluates it.
 */
public final class VadsNet {
    /** Why a net runs or stands still. */
    public enum Status {
        /** The machines get what they ask. */
        DRAAIT,
        /** No source gives anything (and no battery has anything left). */
        GEEN_BRON,
        /** The machines ask more than the sources give: everything stands still. */
        TE_ZWAAR,
        /** More than {@link VadsGetallen#MAX_NET} blocks: everything stands still. */
        TE_GROOT,
        /** There is no net here. */
        LEEG
    }

    /** "No net here". */
    public static final VadsNet EMPTY = new VadsNet();

    /** A source and what it gives right now (asked once per evaluation). */
    private record Gemeten(VadsBron bron, int geeft, BlockPos plek) {
    }

    private static final Comparator<Gemeten> STERKSTE_EERST = Comparator.comparingInt(Gemeten::geeft).reversed()
            .thenComparing(Gemeten::plek);

    // --- what it is made of (fixed once built) ---
    final LongArrayList draden = new LongArrayList();
    /** Every block of the net: the Guhdraad and all blocks of the knopen. */
    final LongOpenHashSet plekken = new LongOpenHashSet();
    final Long2ReferenceOpenHashMap<VadsKnoop> knoopOp = new Long2ReferenceOpenHashMap<>();
    final List<VadsKnoop> knopen = new ArrayList<>();
    private final Map<VadsKnoop, Boolean> uniek = new IdentityHashMap<>();
    final List<VadsBron> bronnen = new ArrayList<>();
    final List<VadsVerbruiker> verbruikers = new ArrayList<>();
    final List<VadsOpslag> opslag = new ArrayList<>();
    /** The chunks it lies in, and the unloaded chunks it ends at (it is rebuilt when one of those loads). */
    final LongOpenHashSet chunks = new LongOpenHashSet(), rand = new LongOpenHashSet();
    boolean teGroot;
    /** Replaced by a newer net (or about to be). */
    boolean vervallen;
    /** Its tick within the second. */
    int fase;

    // --- the last evaluation ---
    private Status status = Status.LEEG;
    private int aanbod, vraag;
    private long buffer, bufferMax;
    private final int[] aantal = new int[BronSoort.values().length], telt = new int[BronSoort.values().length];
    private final Map<VadsBron, Boolean> teltNiet = new IdentityHashMap<>();
    /** Does the Guhdraad show power (null: not set yet, every piece is checked)? */
    private Boolean draadAan;

    VadsNet() {
    }

    // =====================================================================================================================
    // the answers
    // =====================================================================================================================

    public Status status() {
        return status;
    }

    public boolean draait() {
        return status == Status.DRAAIT;
    }

    /** What the sources that count give together, in VK per second. */
    public int aanbod() {
        return aanbod;
    }

    /** What the switched-on machines ask together (their nominal demand), in VK per second. */
    public int vraag() {
        return vraag;
    }

    /** How much the machines ask more than the sources give (0 when it fits). */
    public int tekort() {
        return Math.max(0, vraag - aanbod);
    }

    /** What the batteries hold together / can hold together, in VK. */
    public long buffer() {
        return buffer;
    }

    public long bufferMax() {
        return bufferMax;
    }

    /** How many sources of this kind the net has. */
    public int aantal(BronSoort s) {
        return aantal[s.ordinal()];
    }

    /** How many of them count (at most {@link BronSoort#max}). */
    public int telt(BronSoort s) {
        return telt[s.ordinal()];
    }

    /** Does this source count (false: one too many of its kind)? */
    public boolean teltMee(VadsBron bron) {
        return !teltNiet.containsKey(bron);
    }

    /** The controller positions of the knopen. */
    public List<BlockPos> knopen() {
        List<BlockPos> out = new ArrayList<>(knopen.size());
        for (VadsKnoop k : knopen) {
            out.add(k.vadsPlek());
        }
        return Collections.unmodifiableList(out);
    }

    /** How many blocks (Guhdraad and machine blocks) the net has. */
    public int grootte() {
        return plekken.size();
    }

    /** How many pieces of Guhdraad. */
    public int draden() {
        return draden.size();
    }

    // =====================================================================================================================
    // building (VadsNetten)
    // =====================================================================================================================

    void draad(BlockPos pos) {
        long key = pos.asLong();
        plekken.add(key);
        draden.add(key);
    }

    void knoop(BlockPos pos, VadsKnoop k) {
        long key = pos.asLong();
        plekken.add(key);
        if (uniek.put(k, Boolean.TRUE) == null) {
            knopen.add(k);
            if (k instanceof VadsBron b) {
                bronnen.add(b);
            }
            if (k instanceof VadsVerbruiker v) {
                verbruikers.add(v);
            }
            if (k instanceof VadsOpslag o) {
                opslag.add(o);
            }
        }
        knoopOp.put(key, k);
    }

    // =====================================================================================================================
    // evaluating
    // =====================================================================================================================

    /**
     * Works out whether the net runs and tells its knopen. {@code seconde}: this is the net's one evaluation of this second,
     * so the batteries are charged or emptied; in between (something changed, somebody looks) nothing is moved.
     * Returns false when a knoop is gone: the net must be rebuilt.
     */
    boolean evalueer(ServerLevel level, VadsNetten beheer, boolean seconde) {
        for (VadsKnoop k : knopen) {
            if (k instanceof BlockEntity be ? be.isRemoved() : level.getCapability(VadsKracht.KNOOP, k.vadsPlek(), null) != k) {
                return false;
            }
        }
        // the sources: per kind only the strongest few count
        Arrays.fill(aantal, 0);
        Arrays.fill(telt, 0);
        teltNiet.clear();
        int geeft = 0;
        List<Gemeten> gemeten = new ArrayList<>(bronnen.size());
        for (VadsBron b : bronnen) {
            gemeten.add(new Gemeten(b, Math.max(0, b.vadsAanbod()), b.vadsPlek()));
        }
        gemeten.sort(STERKSTE_EERST);
        for (Gemeten g : gemeten) {
            BronSoort soort = g.bron().vadsSoort();
            boolean mee = true;
            if (soort != null) {
                mee = aantal[soort.ordinal()]++ < soort.max;
                if (mee) {
                    telt[soort.ordinal()]++;
                }
            }
            if (mee) {
                geeft += g.geeft();
            } else {
                teltNiet.put(g.bron(), Boolean.TRUE);
            }
        }
        int vraagt = 0;
        for (VadsVerbruiker v : verbruikers) {
            vraagt += Math.max(0, v.vadsVraag());
        }
        long inhoud = 0, ruimte = 0;
        for (VadsOpslag o : opslag) {
            inhoud += Math.max(0, o.vadsInhoud());
            ruimte += Math.max(0, o.vadsMax());
        }
        aanbod = geeft;
        vraag = vraagt;
        bufferMax = ruimte;

        // the rule
        int tekort = vraagt - geeft;
        Status nu;
        if (teGroot) {
            nu = Status.TE_GROOT;
        } else if (geeft <= 0 && inhoud <= 0) {
            nu = Status.GEEN_BRON;
        } else if (tekort <= 0 || inhoud >= tekort) {
            nu = Status.DRAAIT;
        } else {
            nu = geeft <= 0 ? Status.GEEN_BRON : Status.TE_ZWAAR;
        }
        if (seconde && nu == Status.DRAAIT && !opslag.isEmpty()) {
            if (tekort > 0) {
                long nog = tekort;
                for (VadsOpslag o : opslag) {
                    nog -= o.vadsOntlaad(nog);
                    if (nog <= 0) {
                        break;
                    }
                }
                inhoud -= tekort - Math.max(0, nog);
            } else if (tekort < 0) {
                long over = -tekort;
                for (VadsOpslag o : opslag) {
                    long erin = o.vadsLaad(over);
                    over -= erin;
                    inhoud += erin;
                    if (over <= 0) {
                        break;
                    }
                }
            }
        }
        buffer = inhoud;
        status = nu;

        // tell everybody
        boolean aan = nu == Status.DRAAIT;
        for (VadsBron b : bronnen) {
            b.vadsTelt(!teltNiet.containsKey(b));
        }
        for (VadsVerbruiker v : verbruikers) {
            beheer.vertel(v, aan);
        }
        if (draadAan == null || draadAan != aan) {
            zetDraden(level, aan);
        }
        return true;
    }

    /** Everything off (the net broke during an evaluation). */
    void stil(ServerLevel level, VadsNetten beheer) {
        status = Status.GEEN_BRON;
        for (VadsVerbruiker v : verbruikers) {
            beheer.vertel(v, false);
        }
        zetDraden(level, false);
    }

    /** The Guhdraad shows whether its net runs (and gives a redstone signal while it does, as it always did). */
    private void zetDraden(ServerLevel level, boolean aan) {
        draadAan = aan;
        if (draden.isEmpty()) {
            return;
        }
        Block draad = ModBlocks.GUH_WIRE.get();
        List<BlockPos> anders = new ArrayList<>();
        boolean was = VadsNetten.bezig;
        VadsNetten.bezig = true;
        try {
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int i = 0; i < draden.size(); i++) {
                p.set(draden.getLong(i));
                if (!VadsNetten.geladen(level, p)) {
                    continue;
                }
                BlockState state = level.getBlockState(p);
                if (state.is(draad) && state.getValue(GuhWireBlock.POWERED) != aan) {
                    level.setBlock(p, state.setValue(GuhWireBlock.POWERED, aan), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    anders.add(p.immutable());
                }
            }
            for (BlockPos pos : anders) {
                level.updateNeighborsAt(pos, draad);
                level.updateNeighborsAt(pos.below(), draad);
            }
        } finally {
            VadsNetten.bezig = was;
        }
    }
}
