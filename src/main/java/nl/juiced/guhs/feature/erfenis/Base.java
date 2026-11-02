package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * De Erfenis (POC): one base. The same class holds the rules on the server (the authority) and the copy the client draws
 * (sent whole by {@link ErfenisPayloads.Sync}). Nothing here touches the world: stamping is {@link Stempel}, the plot's place
 * is {@link Plot}, the list of all bases {@link ErfenisData}.
 * <p>
 * <b>Owner.</b> A player's base has {@code eigenaar} = the player's uuid (and {@code id} = the same uuid). An NPC village
 * (phase 2) has {@code npc} = true, no eigenaar, a generated {@code naam} and its own random {@code id}.
 * <p>
 * <b>Time</b> is real-world millis ({@link System#currentTimeMillis()} on the server), so builds and the molens go on while
 * nobody is online. The client gets the server's clock with every sync and keeps the difference.
 * <p>
 * <b>The rules</b> ({@code kan*} return null when allowed, else a {@link Fout}; the actions check again): the stadhuis level
 * limits how many of each type and their max level ({@link GebouwType}); one builder: one build or upgrade at a time
 * ({@link #bouw}); walls and decoration are placed at once, everything else (and every upgrade) takes the builder.
 */
public final class Base {
    /** A build or upgrade in progress: building {@code gebouw} becomes level {@code naar} at real time {@code eind}. */
    public record Bouw(int gebouw, int naar, long start, long eind) {
        public float voortgang(long nu) {
            return eind <= start ? 1f : Math.max(0f, Math.min(1f, (nu - start) / (float) (eind - start)));
        }
    }

    /** Why something may not: a lang key and its arguments (a number, or another lang key, see ErfenisClient.tekst). */
    public record Fout(String key, String... args) {
    }

    public static final int START_BOUWSPUL = 150;

    public final UUID id;
    @Nullable
    public UUID eigenaar;
    public String naam;
    public boolean npc;
    public int cx, cz;
    public final List<Gebouw> gebouwen = new ArrayList<>();
    public int volgendeId = 1;
    @Nullable
    public Bouw bouw;
    public int bouwspul;
    /** The blocks in the world are behind (an upgrade finished while nobody was there, a fresh base, a reset): stamp on the next visit. */
    public boolean moetStempelen = true;

    public Base(UUID id, @Nullable UUID eigenaar, String naam, boolean npc, int cx, int cz) {
        this.id = id;
        this.eigenaar = eigenaar;
        this.naam = naam;
        this.npc = npc;
        this.cx = cx;
        this.cz = cz;
    }

    // =====================================================================================================================
    // a new base
    // =====================================================================================================================

    /** A fresh player base, as opa-guh left it: a level 1 stadhuis in the middle, a molen with a bit inside, his old fence and tree. */
    public static Base nieuw(UUID eigenaar, String naam, int cx, int cz, long nu) {
        Base b = new Base(eigenaar, eigenaar, naam, false, cx, cz);
        b.vulStart(nu);
        return b;
    }

    /** Back to the start (keeps id, owner and cell). */
    public void vulStart(long nu) {
        gebouwen.clear();
        volgendeId = 1;
        bouw = null;
        bouwspul = START_BOUWSPUL;
        nieuwGebouw(GebouwType.STADHUIS, 1, 30, 30, 0, nu);
        Gebouw molen = nieuwGebouw(GebouwType.MOLEN, 1, 23, 31, 0, nu);
        molen.voorraad = 20;
        for (int i = 0; i < 4; i++) {   // opa's old fence
            nieuwGebouw(GebouwType.MUUR, 1, 29 + i, 37, 0, nu);
        }
        nieuwGebouw(GebouwType.BOOMPJE, 1, 37, 29, 0, nu);   // and his blossom tree
        moetStempelen = true;
    }

    /**
     * (phase 2) An NPC village: a stadhuis of this level in the middle and, at random free spots, as many of every other type
     * as that level allows, at random allowed levels; walls in a ring around the middle. Deterministic for a seed.
     */
    public static Base npc(UUID id, String naam, int cx, int cz, int stadhuisLevel, long seed, long nu) {
        Base b = new Base(id, null, naam, true, cx, cz);
        Random rng = new Random(seed);
        int th = Math.max(1, Math.min(GebouwType.MAX_STADHUIS, stadhuisLevel));
        b.nieuwGebouw(GebouwType.STADHUIS, th, 30, 30, rng.nextInt(4), nu);
        // walls: a ring around the middle, as long as the limit lasts
        int muren = GebouwType.MUUR.maxAantal(th), r = 9 + th;
        int muurLevel = Math.max(1, GebouwType.MUUR.maxLevel(th) - rng.nextInt(2));
        List<int[]> ring = new ArrayList<>();
        for (int i = -r; i <= r; i++) {
            ring.add(new int[] {32 + i, 32 - r});
            ring.add(new int[] {32 + i, 32 + r});
            if (i != -r && i != r) {
                ring.add(new int[] {32 - r, 32 + i});
                ring.add(new int[] {32 + r, 32 + i});
            }
        }
        for (int[] p : ring) {
            if (b.aantal(GebouwType.MUUR) >= muren) {
                break;
            }
            if (!(p[0] == 32 && p[1] == 32 + r) && b.vrij(p[0], p[1], 1, -1)) {   // (a gate in the south)
                b.nieuwGebouw(GebouwType.MUUR, muurLevel, p[0], p[1], 0, nu);
            }
        }
        for (GebouwType t : new GebouwType[] {GebouwType.MOLEN, GebouwType.SCHUURTJE, GebouwType.BOOMPJE}) {
            int n = t.maxAantal(th);
            for (int i = 0, poging = 0; i < n && poging < 400; poging++) {
                int x = 2 + rng.nextInt(Plot.MAAT - 4 - t.maat), z = 2 + rng.nextInt(Plot.MAAT - 4 - t.maat);
                if (b.vrij(x, z, t.maat, -1)) {
                    int lvl = 1 + rng.nextInt(Math.max(1, t.maxLevel(th)));
                    Gebouw g = b.nieuwGebouw(t, lvl, x, z, rng.nextInt(4), nu);
                    g.voorraad = t == GebouwType.MOLEN ? rng.nextInt(GebouwType.molenOpslag(lvl) + 1) : 0;
                    i++;
                }
            }
        }
        b.bouwspul = rng.nextInt(b.opslag() + 1);
        return b;
    }

    /** (phase 2) One building of a hand-made or generated layout: type, level (1..top), min corner (x, z) on the plot, quarter turns. */
    public record Plan(GebouwType type, int level, int x, int z, int rot) {
    }

    /**
     * (phase 2) An NPC village from a building list. Entries that stick out of the plot or overlap an earlier entry are skipped;
     * levels are clamped to 1..top. Without a STADHUIS in the list one of level 1 is put in the middle (if free).
     */
    public static Base npc(UUID id, String naam, int cx, int cz, List<Plan> plan, int bouwspul, long nu) {
        Base b = new Base(id, null, naam, true, cx, cz);
        for (Plan p : plan) {
            if (p.type() == GebouwType.STADHUIS && b.stadhuis() != null) {
                continue;
            }
            if (b.vrij(p.x(), p.z(), p.type().maat, -1)) {
                b.nieuwGebouw(p.type(), Math.max(1, Math.min(p.type().topLevel(), p.level())), p.x(), p.z(), p.rot(), nu);
            }
        }
        if (b.stadhuis() == null && b.vrij(30, 30, GebouwType.STADHUIS.maat, -1)) {
            b.nieuwGebouw(GebouwType.STADHUIS, 1, 30, 30, 0, nu);
        }
        b.bouwspul = Math.max(0, bouwspul);
        return b;
    }

    private Gebouw nieuwGebouw(GebouwType type, int level, int x, int z, int rot, long nu) {
        Gebouw g = new Gebouw(volgendeId++, type, level, x, z, rot);
        g.bijgewerkt = nu;
        gebouwen.add(g);
        return g;
    }

    // =====================================================================================================================
    // what follows from the buildings
    // =====================================================================================================================

    @Nullable
    public Gebouw gebouw(int id) {
        for (Gebouw g : gebouwen) {
            if (g.id == id) {
                return g;
            }
        }
        return null;
    }

    /** The building on this tile, or null. */
    @Nullable
    public Gebouw op(int tx, int tz) {
        for (Gebouw g : gebouwen) {
            if (g.bevat(tx, tz)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public Gebouw stadhuis() {
        for (Gebouw g : gebouwen) {
            if (g.type == GebouwType.STADHUIS) {
                return g;
            }
        }
        return null;
    }

    public int stadhuisLevel() {
        Gebouw s = stadhuis();
        return s == null ? 1 : Math.max(1, s.level);
    }

    public int aantal(GebouwType type) {
        int n = 0;
        for (Gebouw g : gebouwen) {
            if (g.type == type) {
                n++;
            }
        }
        return n;
    }

    /** The most Bouwspul this base can hold: the stadhuis plus every schuurtje (that is built). */
    public int opslag() {
        int cap = GebouwType.stadhuisOpslag(stadhuisLevel());
        for (Gebouw g : gebouwen) {
            if (g.type == GebouwType.SCHUURTJE) {
                cap += GebouwType.schuurOpslag(g.level);
            }
        }
        return cap;
    }

    /** Knusheid: the sum of what every (built) building gives. */
    public int knusheid() {
        int k = 0;
        for (Gebouw g : gebouwen) {
            k += g.type.knus(g.level);
        }
        return k;
    }

    /** Is a footprint of {@code maat} at (x, z) inside the plot and free (ignoring building {@code negeer})? */
    public boolean vrij(int x, int z, int maat, int negeer) {
        if (x < 0 || z < 0 || x + maat > Plot.MAAT || z + maat > Plot.MAAT) {
            return false;
        }
        for (Gebouw g : gebouwen) {
            if (g.id != negeer && g.overlapt(x, z, maat)) {
                return false;
            }
        }
        return true;
    }

    public boolean binnen(int x, int z, int maat) {
        return x >= 0 && z >= 0 && x + maat <= Plot.MAAT && z + maat <= Plot.MAAT;
    }

    // =====================================================================================================================
    // the rules
    // =====================================================================================================================

    /** May one more of this type be bought now (anywhere)? Null = yes. */
    @Nullable
    public Fout kanKopen(GebouwType type) {
        int th = stadhuisLevel();
        int heeft = aantal(type);
        if (heeft >= type.maxAantal(th)) {
            int s = type.ontgrendeltAantal(heeft);
            return s > 0 ? new Fout("erfenis.fout.slot", Integer.toString(s)) : new Fout("erfenis.fout.limiet", type.naamKey());
        }
        if (!type.direct && bouw != null) {
            return new Fout("erfenis.fout.bouwer");
        }
        if (bouwspul < type.kosten(1)) {
            return new Fout("erfenis.fout.bouwspul", Integer.toString(type.kosten(1)));
        }
        return null;
    }

    /** May a new one of this type stand at (x, z)? Null = yes. */
    @Nullable
    public Fout kanPlaatsen(GebouwType type, int x, int z) {
        Fout f = kanKopen(type);
        if (f != null) {
            return f;
        }
        return plekFout(x, z, type.maat, -1);
    }

    @Nullable
    private Fout plekFout(int x, int z, int maat, int negeer) {
        if (!binnen(x, z, maat)) {
            return new Fout("erfenis.fout.buiten");
        }
        return vrij(x, z, maat, negeer) ? null : new Fout("erfenis.fout.overlap");
    }

    @Nullable
    public Fout kanVerplaatsen(int id, int x, int z) {
        Gebouw g = gebouw(id);
        return g == null ? new Fout("erfenis.fout.weg") : plekFout(x, z, g.type.maat, id);
    }

    /** May this building go one level up now? Null = yes. */
    @Nullable
    public Fout kanVerbeteren(int id) {
        Gebouw g = gebouw(id);
        if (g == null) {
            return new Fout("erfenis.fout.weg");
        }
        if (g.level >= g.type.topLevel()) {
            return new Fout("erfenis.fout.max");
        }
        int naar = g.level + 1;
        if (g.type != GebouwType.STADHUIS && naar > g.type.maxLevel(stadhuisLevel())) {
            return new Fout("erfenis.fout.slot", Integer.toString(g.type.ontgrendeltLevel(naar)));
        }
        if (bouw != null) {
            return new Fout("erfenis.fout.bouwer");
        }
        if (bouwspul < g.type.kosten(naar)) {
            return new Fout("erfenis.fout.bouwspul", Integer.toString(g.type.kosten(naar)));
        }
        return null;
    }

    @Nullable
    public Fout kanOphalen(int id, long nu) {
        Gebouw g = gebouw(id);
        if (g == null || g.type != GebouwType.MOLEN) {
            return new Fout("erfenis.fout.weg");
        }
        if (g.voorraadOp(nu) < 1) {
            return new Fout("erfenis.fout.niks");
        }
        return bouwspul >= opslag() ? new Fout("erfenis.fout.vol") : null;
    }

    // =====================================================================================================================
    // the actions (the server calls them after its checks; each checks again and returns what went wrong, or null)
    // =====================================================================================================================

    /** Builds a new one: at once (walls, decoration) or as the builder's job. {@code duurMs} &lt; 0: the type's own time. */
    @Nullable
    public Fout plaats(GebouwType type, int x, int z, int rot, long nu, long duurMs) {
        Fout f = kanPlaatsen(type, x, z);
        if (f != null) {
            return f;
        }
        bouwspul -= type.kosten(1);
        if (type.direct) {
            nieuwGebouw(type, 1, x, z, rot, nu);
        } else {
            Gebouw g = nieuwGebouw(type, 0, x, z, rot, nu);
            bouw = new Bouw(g.id, 1, nu, nu + (duurMs < 0 ? type.tijdSec(1) * 1000L : duurMs));
        }
        return null;
    }

    @Nullable
    public Fout verplaats(int id, int x, int z) {
        Fout f = kanVerplaatsen(id, x, z);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        g.x = x;
        g.z = z;
        return null;
    }

    @Nullable
    public Fout draai(int id) {
        Gebouw g = gebouw(id);
        if (g == null) {
            return new Fout("erfenis.fout.weg");
        }
        g.rot = (g.rot + 1) & 3;
        return null;
    }

    /** Starts the upgrade of this building. {@code duurMs} &lt; 0: the type's own time. */
    @Nullable
    public Fout verbeter(int id, long nu, long duurMs) {
        Fout f = kanVerbeteren(id);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        int naar = g.level + 1;
        bouwspul -= g.type.kosten(naar);
        bouw = new Bouw(id, naar, nu, nu + (duurMs < 0 ? g.type.tijdSec(naar) * 1000L : duurMs));
        return null;
    }

    /** Collects what a molen made: as much as fits in the storage, the rest stays inside. Returns the amount, or a Fout. */
    public Object ophalen(int id, long nu) {
        Fout f = kanOphalen(id, nu);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        g.telBij(nu);
        int neem = Math.min((int) Math.floor(g.voorraad), opslag() - bouwspul);
        g.voorraad -= neem;
        bouwspul += neem;
        return neem;
    }

    /** The builder is done at this time: the building goes up a level. Returns that building, or null (nothing finished). */
    @Nullable
    public Gebouw afronden(long nu) {
        Bouw b = bouw;
        if (b == null || nu < b.eind()) {
            return null;
        }
        bouw = null;
        Gebouw g = gebouw(b.gebouw());
        if (g == null) {
            return null;
        }
        g.telBij(nu);
        g.level = Math.max(g.level, b.naar());
        return g;
    }

    /** Is this building being built or upgraded right now? */
    public boolean inAanbouw(Gebouw g) {
        return bouw != null && bouw.gebouw() == g.id;
    }

    // =====================================================================================================================
    // saving (also the network form)
    // =====================================================================================================================

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        if (eigenaar != null) {
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        t.putString("Naam", naam);
        t.putBoolean("Npc", npc);
        t.putInt("Cx", cx);
        t.putInt("Cz", cz);
        t.putInt("VolgendeId", volgendeId);
        t.putInt("Bouwspul", bouwspul);
        t.putBoolean("MoetStempelen", moetStempelen);
        ListTag lijst = new ListTag();
        for (Gebouw g : gebouwen) {
            lijst.add(g.save());
        }
        t.put("Gebouwen", lijst);
        if (bouw != null) {
            CompoundTag b = new CompoundTag();
            b.putInt("Gebouw", bouw.gebouw());
            b.putInt("Naar", bouw.naar());
            b.putLong("Start", bouw.start());
            b.putLong("Eind", bouw.eind());
            t.put("Bouw", b);
        }
        return t;
    }

    @Nullable
    public static Base load(CompoundTag t) {
        UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
        if (id == null) {
            return null;
        }
        Base b = new Base(id, t.read("Eigenaar", UUIDUtil.CODEC).orElse(null), t.getStringOr("Naam", "?"), t.getBooleanOr("Npc", false),
                t.getIntOr("Cx", 0), t.getIntOr("Cz", 0));
        b.volgendeId = t.getIntOr("VolgendeId", 1);
        b.bouwspul = t.getIntOr("Bouwspul", 0);
        b.moetStempelen = t.getBooleanOr("MoetStempelen", true);
        for (Tag e : t.getListOrEmpty("Gebouwen")) {
            if (e instanceof CompoundTag c) {
                Gebouw g = Gebouw.load(c);
                if (g != null) {
                    b.gebouwen.add(g);
                    b.volgendeId = Math.max(b.volgendeId, g.id + 1);
                }
            }
        }
        CompoundTag bt = t.getCompound("Bouw").orElse(null);
        if (bt != null) {
            b.bouw = new Bouw(bt.getIntOr("Gebouw", -1), bt.getIntOr("Naar", 1), bt.getLongOr("Start", 0L), bt.getLongOr("Eind", 0L));
        }
        return b;
    }

    /** The buildings, read only (phase 2: the world map). */
    public List<Gebouw> gebouwenLijst() {
        return Collections.unmodifiableList(gebouwen);
    }
}
