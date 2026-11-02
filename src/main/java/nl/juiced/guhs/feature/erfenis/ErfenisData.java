package nl.juiced.guhs.feature.erfenis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * De Erfenis (POC): every base of the world (players and NPC villages), saved with the overworld
 * ({@code data/guhs/erfenis.dat}). The phase-2 entry point:
 * <ul>
 *   <li>{@link #alleBases()} every base; {@link #van(UUID)} by base id (= the owner's uuid for a player);
 *   {@link #opCel(int, int)} by cell.</li>
 *   <li>{@link #vanSpeler(UUID, String, long)} a player's base, made on first use in the next free cell.</li>
 *   <li>{@link #maakNpcBase(String, int, long, long)} a generated NPC village in the next free cell (stamped on the first visit).</li>
 *   <li>After changing a base call {@link #setDirty()}; to put it in the world {@link Erfenis#stempel(MinecraftServer, Base)}.</li>
 * </ul>
 * Also: where every player was before they went to the dimension ({@link Terug}), and the dev switch "snel".
 */
public final class ErfenisData extends SavedData {
    /** Where a player stood before going to the erfenis dimension. */
    public record Terug(String dim, double x, double y, double z, float yaw, float pitch) {
    }

    static final SavedDataType<ErfenisData> TYPE = GuhSavedData.tagType("erfenis", ErfenisData::new, ErfenisData::load, ErfenisData::save);

    private final Map<UUID, Base> bases = new LinkedHashMap<>();
    private final Map<Long, UUID> cellen = new HashMap<>();
    private final Map<UUID, Terug> terug = new HashMap<>();
    /** The next spiral index to try for a new cell. */
    private int volgendeSpiraal;
    /** Dev: builds and upgrades finish at once. */
    public boolean snel;

    public static ErfenisData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis");
    }

    public Collection<Base> alleBases() {
        return Collections.unmodifiableCollection(bases.values());
    }

    @Nullable
    public Base van(UUID baseId) {
        return bases.get(baseId);
    }

    @Nullable
    public Base opCel(int cx, int cz) {
        UUID id = cellen.get(Plot.sleutel(cx, cz));
        return id == null ? null : bases.get(id);
    }

    /** This player's base; made (a fresh start in the next free cell) if they have none yet. */
    public Base vanSpeler(UUID speler, String naam, long nu) {
        Base b = bases.get(speler);
        if (b == null) {
            int[] c = vrijeCel();
            b = Base.nieuw(speler, naam, c[0], c[1], nu);
            voegToe(b);
        } else if (!b.naam.equals(naam)) {
            b.naam = naam;
            setDirty();
        }
        return b;
    }

    /** Phase 2: a generated NPC village with this name and stadhuis level in the next free cell. */
    public Base maakNpcBase(String naam, int stadhuisLevel, long seed, long nu) {
        int[] c = vrijeCel();
        Base b = Base.npc(new UUID(seed ^ 0x6775686572666E73L, ((long) c[0] << 32) ^ c[1]), naam, c[0], c[1], stadhuisLevel, seed, nu);
        voegToe(b);
        return b;
    }

    /** Phase 2: an NPC village from a building list (see {@link Base#npc(UUID, String, int, int, java.util.List, int, long)}) in the next free cell. */
    public Base maakNpcBase(String naam, java.util.List<Base.Plan> plan, int bouwspul, long nu) {
        int[] c = vrijeCel();
        Base b = Base.npc(UUID.randomUUID(), naam, c[0], c[1], plan, bouwspul, nu);
        voegToe(b);
        return b;
    }

    /**
     * Phase 2 (the world map): the NPC village with this id in THIS cell (the map picks its own cells and ids), made, or, when
     * it exists already, given this building list in place ({@link Base#vervangPlan}). Null (nothing changed): the cell
     * belongs to another base, or the id is a player's base / an NPC base in another cell. Call {@link #veranderd} after.
     */
    @Nullable
    public Base npcBase(UUID id, String naam, int cx, int cz, java.util.List<Base.Plan> plan, long nu) {
        Base b = bases.get(id);
        if (b == null) {
            if (cellen.containsKey(Plot.sleutel(cx, cz))) {
                return null;
            }
            b = Base.npc(id, naam, cx, cz, plan, 0, nu);
            b.bouwspul = b.opslag() / 2;
            voegToe(b);
            return b;
        }
        if (!b.npc || b.cx != cx || b.cz != cz) {
            return null;
        }
        b.naam = naam;
        b.vervangPlan(plan, nu);
        setDirty();
        return b;
    }

    /** Every cell a base stands on (the world map keeps its new NPC villages off these; the player spiral skips them too). */
    public java.util.Set<Long> bezetteCellen() {
        return Collections.unmodifiableSet(cellen.keySet());
    }

    /** Phase 2: a changed NPC base (it grew): mark it for stamping on the next visit, or stamp now if somebody is there. */
    public void veranderd(MinecraftServer server, Base b) {
        b.moetStempelen = true;
        setDirty();
        if (Erfenis.bezocht(server, b)) {
            Erfenis.stempel(server, b);
            Erfenis.syncKijkers(server, b);
        }
    }

    private void voegToe(Base b) {
        bases.put(b.id, b);
        cellen.put(Plot.sleutel(b.cx, b.cz), b.id);
        setDirty();
    }

    private int[] vrijeCel() {
        while (true) {
            int[] c = Plot.spiraal(volgendeSpiraal++);
            if (!cellen.containsKey(Plot.sleutel(c[0], c[1]))) {
                setDirty();
                return c;
            }
        }
    }

    @Nullable
    public Terug terug(UUID speler) {
        return terug.get(speler);
    }

    public void zetTerug(UUID speler, @Nullable Terug t) {
        if (t == null) {
            terug.remove(speler);
        } else {
            terug.put(speler, t);
        }
        setDirty();
    }

    // =====================================================================================================================

    private static ErfenisData load(CompoundTag t) {
        ErfenisData d = new ErfenisData();
        d.volgendeSpiraal = t.getIntOr("VolgendeSpiraal", 0);
        d.snel = t.getBooleanOr("Snel", false);
        for (Tag e : t.getListOrEmpty("Bases")) {
            if (e instanceof CompoundTag c) {
                Base b = Base.load(c);
                if (b != null) {
                    d.bases.put(b.id, b);
                    d.cellen.put(Plot.sleutel(b.cx, b.cz), b.id);
                }
            }
        }
        for (Tag e : t.getListOrEmpty("Terug")) {
            if (e instanceof CompoundTag c) {
                c.read("Speler", UUIDUtil.CODEC).ifPresent(id -> d.terug.put(id, new Terug(c.getStringOr("Dim", "minecraft:overworld"),
                        c.getDoubleOr("X", 0), c.getDoubleOr("Y", 80), c.getDoubleOr("Z", 0), c.getFloatOr("Yaw", 0f), c.getFloatOr("Pitch", 0f))));
            }
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("VolgendeSpiraal", volgendeSpiraal);
        t.putBoolean("Snel", snel);
        ListTag lijst = new ListTag();
        for (Base b : bases.values()) {
            lijst.add(b.save());
        }
        t.put("Bases", lijst);
        ListTag tl = new ListTag();
        for (Map.Entry<UUID, Terug> e : terug.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.store("Speler", UUIDUtil.CODEC, e.getKey());
            Terug r = e.getValue();
            c.putString("Dim", r.dim());
            c.putDouble("X", r.x());
            c.putDouble("Y", r.y());
            c.putDouble("Z", r.z());
            c.putFloat("Yaw", r.yaw());
            c.putFloat("Pitch", r.pitch());
            tl.add(c);
        }
        t.put("Terug", tl);
        return t;
    }
}
