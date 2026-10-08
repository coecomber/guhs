package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * biomes3 wereld: the air that buildings of the Wolkenweide have reserved, and what the terrain model knows about the
 * {@code guhs:bio_plek} structure sets of this server.
 * <p>
 * A {@code guhs:bio_plek} structure of kind {@code lucht} brings its own island in its template, {@code hoogte} blocks
 * above the meadow. {@link WolkTerrein} makes no island, stair, lift or cloud where such a building comes.
 * <p>
 * biomes3 fix-plaatsing: this used to keep a cylinder from the meadow to the sky free at EVERY chunk where one of six
 * structure sets could start, which cost a Wolkenweide half its natural islands. Now:
 * <ul>
 *   <li>a building in the air is "one per region" ({@code per_regio}): which Wolkenweide gets it and where it stands
 *       follows from the terrain model alone ({@link RegioKeuze#lucht}), so only the buildings that really come reserve
 *       air;</li>
 *   <li>each reserves the shape of its own template ({@link Profiel}: per height how far its blocks reach from its
 *       anchor, read from the template when the server starts) plus {@link #MARGE} blocks, not a cylinder: a castle
 *       102 blocks up leaves the sky below it to the natural islands, only its lift column stays clear;</li>
 *   <li>a structure of kind {@code lucht} WITHOUT {@code per_regio} (the test structure) still reserves a whole cylinder at
 *       every possible start of its set, but only at the starts where the meadow is whole
 *       ({@link RegioKeuze#heleWeide}: the others never start).</li>
 * </ul>
 * Filled once per server start from the structure sets ({@link #laad}); a set only counts with a
 * {@code minecraft:random_spread} placement.
 */
public final class Luchtruim {
    /** Air kept free around a building's own blocks (sideways and up/down). */
    public static final int MARGE = 5;

    /** Per height (relative to the anchor, from {@link #onder}) how far a template's blocks reach sideways from its anchor column; -1: nothing at that height. */
    public record Profiel(int onder, int[] straal) {
        /** The furthest reach between two heights relative to the anchor (both included), or -1. */
        int reik(int van, int tot) {
            int r = -1;
            for (int dy = Math.max(van, onder); dy <= Math.min(tot, onder + straal.length - 1); dy++) {
                r = Math.max(r, straal[dy - onder]);
            }
            return r;
        }
    }

    /** A {@code lucht} structure of this server. profiel: null when its template could not be read (then: the whole cylinder). */
    public record Bouwsel(BioPlekStructure structuur, String naam, RandomSpreadStructurePlacement plaatsing, Profiel profiel) {
    }

    /** Any {@code guhs:bio_plek} structure of this server with the set it is in. */
    public record Inschrijving(BioPlekStructure structuur, String naam, RandomSpreadStructurePlacement plaatsing) {
    }

    private static volatile List<Bouwsel> perRegio = List.of(), los = List.of();
    private static volatile List<Inschrijving> alle = List.of();
    private static volatile Map<BioPlekStructure, RandomSpreadStructurePlacement> plaatsingen = Map.of();
    private static volatile long seed;
    /** To measure what the buildings cost the sky: with GUHS_BIO_LUCHT=0 in the environment (or {@link #zonder}) no air is kept free at all. */
    static final boolean GEEN_GEBOUWEN = "0".equals(System.getenv("GUHS_BIO_LUCHT"));
    private static volatile boolean uit = GEEN_GEBOUWEN;

    /** For measurements only: keep no air free (true) or do (false). Models made before the call keep what they worked out. */
    public static void zonder(boolean geen) {
        uit = geen;
    }

    /** Reads the structure sets of this server (before any chunk is made). */
    public static void laad(MinecraftServer server) {
        List<Bouwsel> nieuw = new ArrayList<>(), nieuwLos = new ArrayList<>();
        List<Inschrijving> iedereen = new ArrayList<>();
        Map<BioPlekStructure, RandomSpreadStructurePlacement> sets = new IdentityHashMap<>();
        var register = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (var houder : register.listElements().toList()) {
            StructureSet set = houder.value();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry e : set.structures()) {
                if (!(e.structure().value() instanceof BioPlekStructure b) || !b.doetMee()) {
                    continue;
                }
                if (sets.putIfAbsent(b, spread) == null) {
                    iedereen.add(new Inschrijving(b, houder.key().identifier().getPath(), spread));
                }
                if (b.soort() == BioPlekken.Soort.LUCHT) {
                    Bouwsel s = new Bouwsel(b, houder.key().identifier().toString(), spread, profiel(server, b));
                    (b.perRegio().isPresent() ? nieuw : nieuwLos).add(s);
                }
            }
        }
        // (the order in which the buildings of one Wolkenweide choose their place: the widest first, then by name)
        nieuw.sort(Comparator.comparingInt((Bouwsel s) -> -s.structuur.ruimte()).thenComparing(Bouwsel::naam));
        seed = server.getWorldGenSettings().options().seed();
        plaatsingen = sets;
        iedereen.sort(Comparator.comparing(Inschrijving::naam));
        alle = List.copyOf(iedereen);
        perRegio = List.copyOf(nieuw);
        los = List.copyOf(nieuwLos);
        BioModel.vergeet();
    }

    /** The shape of a structure's start template around its start jigsaw, or null. */
    private static Profiel profiel(MinecraftServer server, BioPlekStructure b) {
        try {
            var pool = b.startPool().value();
            Profiel uit = null;
            for (var paar : pool.getTemplates()) {
                if (!(paar.getFirst() instanceof SinglePoolElement enkel)) {
                    return null;
                }
                Identifier id = enkel.getTemplateLocation();
                StructureTemplate t = server.getStructureManager().get(id).orElse(null);
                if (t == null) {
                    return null;
                }
                CompoundTag tag = t.save(new CompoundTag());
                ListTag palet = tag.getListOrEmpty("palette");
                if (palet.isEmpty()) {
                    return null;
                }
                boolean[] leeg = new boolean[palet.size()];
                for (int i = 0; i < leeg.length; i++) {
                    String naam = palet.getCompoundOrEmpty(i).getStringOr("Name", "");
                    leeg[i] = naam.equals("minecraft:air") || naam.equals("minecraft:structure_void") || naam.equals("minecraft:cave_air");
                }
                ListTag blokken = tag.getListOrEmpty("blocks");
                int ax = Integer.MIN_VALUE, ay = 0, az = 0;
                String anker = b.startJigsaw().map(Identifier::toString).orElse(null);
                for (int i = 0; i < blokken.size() && anker != null; i++) {
                    CompoundTag blok = blokken.getCompoundOrEmpty(i);
                    if (blok.getCompound("nbt").map(n -> anker.equals(n.getStringOr("name", ""))).orElse(false)) {
                        ListTag pos = blok.getListOrEmpty("pos");
                        ax = pos.getIntOr(0, 0);
                        ay = pos.getIntOr(1, 0);
                        az = pos.getIntOr(2, 0);
                        break;
                    }
                }
                if (ax == Integer.MIN_VALUE) {
                    return null;
                }
                int hoog = t.getSize().getY();
                int[] straal = uit != null ? uit.straal : new int[hoog];
                if (uit == null) {
                    java.util.Arrays.fill(straal, -1);
                } else if (uit.onder != -ay || straal.length != hoog) {
                    return null; // (variants of one building with another make: no shape to trust)
                }
                for (int i = 0; i < blokken.size(); i++) {
                    CompoundTag blok = blokken.getCompoundOrEmpty(i);
                    int staat = blok.getIntOr("state", 0);
                    if (staat < 0 || staat >= leeg.length || leeg[staat]) {
                        continue;
                    }
                    ListTag pos = blok.getListOrEmpty("pos");
                    int y = pos.getIntOr(1, 0);
                    if (y >= 0 && y < hoog) {
                        straal[y] = Math.max(straal[y], (int) Math.ceil(Math.hypot(pos.getIntOr(0, 0) - ax, pos.getIntOr(2, 0) - az)));
                    }
                }
                uit = new Profiel(-ay, straal);
            }
            return uit;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Every {@code guhs:bio_plek} structure of this server, by name. */
    public static List<Inschrijving> alle() {
        return alle;
    }

    /** The world seed the structure sets were read with. */
    public static long seed() {
        return seed;
    }

    /** The one-per-region structures of kind {@code lucht}, in the order they choose their place. */
    static List<Bouwsel> perRegio() {
        return perRegio;
    }

    /** The placement of the structure set a {@code guhs:bio_plek} structure is in, or null (no server started yet, or in no random-spread set). */
    static RandomSpreadStructurePlacement plaatsing(BioPlekStructure s) {
        return plaatsingen.get(s);
    }

    /** Is something of radius {@code straal} around (x, z), between two heights (both included), inside the room of a building in the air? */
    public static boolean bezet(BioModel m, int x, int z, int straal, int yOnder, int yBoven) {
        if (uit) {
            return false;
        }
        if (!perRegio.isEmpty()) {
            BioRegio g = m.regio(x, z, true);
            if (g != null) {
                for (RegioKeuze.Gekozen k : RegioKeuze.lucht(m, g)) {
                    double d = Math.hypot(k.x() - x, k.z() - z);
                    Bouwsel b = k.bouwsel();
                    if (d > b.structuur.ruimte() + MARGE + straal) {
                        continue;
                    }
                    if (b.profiel == null) {
                        return true;
                    }
                    int r = b.profiel.reik(yOnder - MARGE - k.y(), yBoven + MARGE - k.y());
                    if (r >= 0 && d <= r + MARGE + straal) {
                        return true;
                    }
                }
            }
        }
        for (Bouwsel p : los) {
            int s = p.plaatsing.spacing();
            int gx = Math.floorDiv(x >> 4, s), gz = Math.floorDiv(z >> 4, s);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ChunkPos c = p.plaatsing.getPotentialStructureChunk(seed, (gx + dx) * s, (gz + dz) * s);
                    double d = Math.hypot(c.getMiddleBlockX() - x, c.getMiddleBlockZ() - z);
                    if (d <= p.structuur.ruimte() + straal && RegioKeuze.heleWeide(m, c.getMiddleBlockX(), c.getMiddleBlockZ(), p.structuur.ruimte())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** How many structures reserve air (for the self test). */
    public static int aantal() {
        return perRegio.size() + los.size();
    }

    /** What is known about the structures of kind lucht (for the dev command). */
    public static String verslag() {
        StringBuilder sb = new StringBuilder();
        for (Bouwsel b : perRegio) {
            sb.append(String.format(java.util.Locale.ROOT, "%n   %s: ruimte %d, hoogte %d, kans %.2f, shape %s", b.naam, b.structuur.ruimte(), b.structuur.hoogte(),
                    b.structuur.perRegio().orElse(0.0), b.profiel == null ? "unknown (whole cylinder)" : "from " + b.profiel.onder + " to "
                            + (b.profiel.onder + b.profiel.straal.length - 1) + " around its anchor, widest " + java.util.Arrays.stream(b.profiel.straal).max().orElse(-1)));
        }
        for (Bouwsel b : los) {
            sb.append(String.format(java.util.Locale.ROOT, "%n   %s: every possible start, ruimte %d", b.naam, b.structuur.ruimte()));
        }
        return sb.toString();
    }

    private Luchtruim() {
    }
}
