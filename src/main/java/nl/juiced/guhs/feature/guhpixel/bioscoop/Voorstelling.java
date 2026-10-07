package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhSeatEntity;

/**
 * One film that is running on one projector right now (server side, in memory only: the projector's block entity is what
 * is saved, and it makes a new Voorstelling when it ticks without one). It knows the screen, the seats around the
 * projector and which guh sits where; the guhs' {@link BioscoopGoal} looks here for a film to go to.
 */
public final class Voorstelling {
    /** Seats count within this many blocks of the projector (and 4 up or down). */
    public static final int STOEL_BEREIK = 12;
    /** A popcornmachine this close to the projector gives the audience popcorn. */
    public static final int POPCORN_BEREIK = 12;

    private static final Map<GlobalPos, Voorstelling> ALLE = new HashMap<>();

    public final GlobalPos plek;
    public final FilmInfo film;
    public long start;
    public Doek doek;
    private final List<BlockPos> stoelen = new ArrayList<>();
    private final Map<BlockPos, UUID> bezet = new HashMap<>();
    @Nullable
    private BlockPos machine;
    private long gescand = Long.MIN_VALUE;
    private long gezien;
    /** Over: the film ran to its end (the audience cheers) or it was switched off. */
    private boolean afgelopen, uitgezet;

    private Voorstelling(GlobalPos plek, FilmInfo film, long start, Doek doek) {
        this.plek = plek;
        this.film = film;
        this.start = start;
        this.doek = doek;
    }

    // --- the registry ---------------------------------------------------------------------------------------------------

    static Voorstelling begin(ServerLevel level, BlockPos projector, FilmInfo film, long start, Doek doek) {
        GlobalPos plek = GlobalPos.of(level.dimension(), projector.immutable());
        Voorstelling oud = ALLE.get(plek);
        if (oud != null) {
            oud.uitgezet = true;
            oud.afgelopen = true;
        }
        Voorstelling v = new Voorstelling(plek, film, start, doek);
        v.gezien = level.getGameTime();
        ALLE.put(plek, v);
        v.scan(level);
        return v;
    }

    @Nullable
    static Voorstelling van(Level level, BlockPos projector) {
        return ALLE.get(GlobalPos.of(level.dimension(), projector));
    }

    /** The projector switched off (or broke): the audience leaves without cheering. */
    static void einde(Level level, BlockPos projector, boolean uitgespeeld) {
        Voorstelling v = ALLE.remove(GlobalPos.of(level.dimension(), projector));
        if (v != null) {
            v.afgelopen = true;
            v.uitgezet = !uitgespeeld;
        }
    }

    /** Is there any film at all in this level? (The cheap first question of every guh.) */
    static boolean iets(ServerLevel level) {
        if (ALLE.isEmpty()) {
            return false;
        }
        long nu = level.getGameTime();
        boolean iets = false;
        for (Iterator<Voorstelling> it = ALLE.values().iterator(); it.hasNext(); ) {
            Voorstelling v = it.next();
            if (!v.plek.dimension().equals(level.dimension())) {
                continue;
            }
            if (nu - v.gezien > 60) {       // its projector does not tick any more (unloaded, removed)
                v.afgelopen = true;
                v.uitgezet = true;
                it.remove();
                continue;
            }
            iets = true;
        }
        return iets;
    }

    /** The running films in this level whose projector is within straal of this spot. */
    static List<Voorstelling> bij(ServerLevel level, Vec3 waar, double straal) {
        List<Voorstelling> uit = new ArrayList<>();
        for (Voorstelling v : ALLE.values()) {
            if (v.plek.dimension().equals(level.dimension()) && !v.afgelopen && v.plek.pos().distToCenterSqr(waar) <= straal * straal) {
                uit.add(v);
            }
        }
        return uit;
    }

    /** The guh that has this seat during a running film, or null. */
    @Nullable
    static UUID opStoel(Level level, BlockPos stoel) {
        for (Voorstelling v : ALLE.values()) {
            if (v.plek.dimension().equals(level.dimension())) {
                UUID wie = v.bezet.get(stoel);
                if (wie != null) {
                    return wie;
                }
            }
        }
        return null;
    }

    static void wisAlles() {
        ALLE.clear();
    }

    static int aantal() {
        return ALLE.size();
    }

    // --- one film -------------------------------------------------------------------------------------------------------

    public BlockPos projector() {
        return plek.pos();
    }

    /** Ticks since the film began. */
    public int tijd(long nu) {
        return (int) Math.max(0, nu - start);
    }

    public boolean afgelopen() {
        return afgelopen;
    }

    /** Switched off before the end (no applause). */
    public boolean uitgezet() {
        return uitgezet;
    }

    public boolean popcorn() {
        return machine != null;
    }

    @Nullable
    public BlockPos machine() {
        return machine;
    }

    public List<BlockPos> stoelen() {
        return stoelen;
    }

    public int publiek() {
        return bezet.size();
    }

    /** (The projector's tick) still running; looks for seats and the popcornmachine again now and then. */
    void leeft(ServerLevel level) {
        gezien = level.getGameTime();
        if (gezien - gescand >= 100) {
            scan(level);
        }
    }

    private void scan(ServerLevel level) {
        gescand = level.getGameTime();
        stoelen.clear();
        machine = null;
        BlockPos p = projector();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -STOEL_BEREIK; dx <= STOEL_BEREIK; dx++) {
                for (int dz = -STOEL_BEREIK; dz <= STOEL_BEREIK; dz++) {
                    q.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    if (!level.isLoaded(q)) {
                        continue;
                    }
                    var blok = level.getBlockState(q).getBlock();
                    if (blok instanceof StoeltjeBlock) {
                        stoelen.add(q.immutable());
                    } else if (blok instanceof PopcornmachineBlock && machine == null) {
                        machine = q.immutable();
                    }
                }
            }
        }
        bezet.keySet().removeIf(s -> !stoelen.contains(s));
    }

    /** The nearest free seat for this guh; it is the guh's from now on (until {@link #geefStoel}). */
    @Nullable
    BlockPos neemStoel(ServerLevel level, Entity guh) {
        BlockPos beste = null;
        double besteD = Double.MAX_VALUE;
        for (BlockPos s : stoelen) {
            if (bezet.containsKey(s) || !(level.getBlockState(s).getBlock() instanceof StoeltjeBlock)) {
                continue;
            }
            double d = s.distToCenterSqr(guh.position());
            if (d < besteD && d <= 20 * 20 && level.getEntitiesOfClass(GuhSeatEntity.class, new AABB(s)).isEmpty()) {
                beste = s;
                besteD = d;
            }
        }
        if (beste != null) {
            bezet.put(beste, guh.getUUID());
        }
        return beste;
    }

    void geefStoel(BlockPos stoel, UUID wie) {
        bezet.remove(stoel, wie);
    }

    boolean heeftStoel(BlockPos stoel, UUID wie) {
        return wie.equals(bezet.get(stoel));
    }
}
