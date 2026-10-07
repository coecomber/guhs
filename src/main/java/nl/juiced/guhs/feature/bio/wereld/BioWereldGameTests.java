package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.gametest.GuhTest;
import org.slf4j.Logger;

/**
 * biomes3 wereld: the game tests of the terrain model. The game test server has no Guhmensie, so these build the
 * dimension's random state from its noise settings (as BleekwoudGameTests does) and ask the model, the real biome source
 * and the real final density; what needs generated chunks is the self test ({@code /guhs bio zelftest wereld}).
 */
public class BioWereldGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_wereld", EMPTY = "empty";
    private static final long[] SEEDS = {20261007L, 1L};

    private record Wereld(RandomState random, BioModel model) {
    }

    private static Wereld wereld(GameTestHelper helper, long seed) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        RandomState random = RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed);
        return new Wereld(random, BioModel.van(random));
    }

    /** Spots (x, z) of a kind of column, from a coarse scan of 24 x 24 km. */
    private static List<int[]> zoek(BioModel m, byte soort, int max) {
        List<int[]> uit = new ArrayList<>();
        for (int x = -12000; x <= 12000 && uit.size() < max; x += 96) {
            for (int z = -12000; z <= 12000 && uit.size() < max; z += 96) {
                if (m.soort(x, z) == soort && m.meng(x, z) >= 1f) {
                    boolean nieuw = true;
                    for (int[] p : uit) {
                        nieuw &= Math.abs(p[0] - x) + Math.abs(p[1] - z) > 400;
                    }
                    if (nieuw) {
                        uit.add(new int[]{x, z});
                    }
                }
            }
        }
        return uit;
    }

    private static final int N = 400, STAP = 40;

    /**
     * The shares, measured on the real biome source (two seeds, 16 x 16 km each, every 40 blocks): Klaterdal and
     * Bloesemmeertje each about as common as another Guhmensie biome, the Wolkenweide rare; the biome source and the terrain
     * model agree in every sample; and OUTSIDE our regions the biome is what it was before these biomes existed (the same
     * source without our three entries answers the same): the existing map only changes where ours are.
     */
    @GuhTest(template = EMPTY, batch = "bio_wereld_aandeel", timeoutTicks = 6000)
    public static void bioWereldAandeel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        MultiNoiseBiomeSource nieuw = (MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, source).getOrThrow();
        JsonObject zonder = source.deepCopy();
        JsonArray oudeLijst = new JsonArray();
        for (JsonElement e : source.getAsJsonArray("biomes")) {
            String b = e.getAsJsonObject().get("biome").getAsString();
            if (!b.equals("guhs:klaterdal") && !b.equals("guhs:bloesemmeertje") && !b.equals("guhs:wolkenweide")) {
                oudeLijst.add(e);
            }
        }
        zonder.add("biomes", oudeLijst);
        MultiNoiseBiomeSource oud = (MultiNoiseBiomeSource) BiomeSource.CODEC.parse(ops, zonder).getOrThrow();
        List<ResourceKey<Biome>> onze = List.of(Bio.KLATERDAL, Bio.BLOESEMMEERTJE, Bio.WOLKENWEIDE);
        long totaal = 0, anders = 0, verschoven = 0, rand = 0;
        long[] aantal = new long[4];
        float[] dal = new float[SEEDS.length * N * N], weide = new float[SEEDS.length * N * N];
        int n = 0;
        for (long seed : SEEDS) {
            Wereld w = wereld(helper, seed);
            Climate.Sampler sampler = w.random.sampler();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    int x = -8000 + i * STAP, z = -8000 + j * STAP;
                    Climate.TargetPoint punt = sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(100), QuartPos.fromBlock(z));
                    var biome = nieuw.getNoiseBiome(punt);
                    // (the sampler asks the quart's corner)
                    int qx = QuartPos.toBlock(QuartPos.fromBlock(x)), qz = QuartPos.toBlock(QuartPos.fromBlock(z));
                    byte soort = w.model.soort(qx, qz);
                    int welke = biome.is(Bio.KLATERDAL) ? Kaart.DAL : biome.is(Bio.BLOESEMMEERTJE) ? Kaart.MEER : biome.is(Bio.WOLKENWEIDE) ? Kaart.WEIDE : 0;
                    totaal++;
                    aantal[welke]++;
                    if (welke != soort) {
                        anders++;
                    }
                    if (soort == Kaart.BUITEN && !oud.getNoiseBiome(punt).equals(biome)) {
                        verschoven++;
                    }
                    if (soort != Kaart.BUITEN && w.model.meng(qx, qz) < 1f) {
                        rand++;
                    }
                    dal[n] = (float) w.model.ruis(BioModel.R_DAL, x, z);
                    weide[n++] = (float) w.model.ruis(BioModel.R_WEIDE, x, z);
                }
            }
        }
        java.util.Arrays.sort(dal);
        java.util.Arrays.sort(weide);
        StringBuilder kwantielen = new StringBuilder();
        for (double deel : new double[]{0.005, 0.01, 0.015, 0.02, 0.03, 0.04, 0.05, 0.07, 0.10, 0.13}) {
            kwantielen.append(String.format(Locale.ROOT, "%n   the top %.1f%% of the noises: dal above %.3f, weide above %.3f", 100 * deel,
                    dal[(int) (dal.length * (1 - deel))], weide[(int) (weide.length * (1 - deel))]));
        }
        double pDal = aantal[Kaart.DAL] / (double) totaal, pMeer = aantal[Kaart.MEER] / (double) totaal, pWeide = aantal[Kaart.WEIDE] / (double) totaal;
        LOGGER.info("biomes3 shares of the Guhmensie surface ({} samples): Klaterdal {}%, Bloesemmeertje {}%, Wolkenweide {}%; together {}% of the old map "
                        + "changes (of which {}% is the rim where the terrain blends); biome and model differ in {} samples; outside our regions the biome "
                        + "changed in {} samples{}", totaal, pct(pDal), pct(pMeer), pct(pWeide), pct(pDal + pMeer + pWeide), pct(rand / (double) totaal), anders,
                verschoven, kwantielen);
        helper.assertTrue(anders == 0, "the biome source and the terrain model agree everywhere (differences: " + anders + ")");
        helper.assertTrue(verschoven == 0, "outside our regions every biome is what it was (changed: " + verschoven + ")");
        helper.assertTrue(pDal >= 0.020 && pDal <= 0.060, "the Klaterdal is about as common as another biome: " + pct(pDal) + "%");
        helper.assertTrue(pMeer >= 0.012 && pMeer <= 0.045, "the Bloesemmeertje is about as common as another biome: " + pct(pMeer) + "%");
        helper.assertTrue(pWeide >= 0.004 && pWeide <= 0.016, "the Wolkenweide is rare: " + pct(pWeide) + "%");
        helper.succeed();
    }

    private static String pct(double p) {
        return String.format(Locale.ROOT, "%.2f", 100 * p);
    }

    /**
     * The valley and the lake, in a few dals of two seeds: the terraces lie at their four heights, the river's water never
     * has a lower DRY neighbour (it cannot run out of its bed), falls exist and drop a terrace, tall falls exist; the lake
     * is 1-2 deep at its shore and 5-8 in the middle, with large islands; and the real final density is solid exactly up to
     * the model's top block.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldDalEnMeer(GameTestHelper helper) {
        int dalen = 0, rivier = 0, vallen = 0, hogeVal = 0, lekken = 0, meerKolommen = 0, ondiep = 0, diep = 0, teDiep = 0, groot = 0, lip = 0, fout = 0;
        int[] terras = new int[4];
        String eerste = "";
        for (long seed : SEEDS) {
            Wereld w = wereld(helper, seed);
            BioModel m = w.model;
            DensityFunction dichtheid = w.random.router().finalDensity();
            for (int[] plek : zoek(m, Kaart.MEER, 3)) {
                dalen++;
                int c0x = (plek[0] >> 4), c0z = (plek[1] >> 4);
                for (int cx = c0x - 14; cx <= c0x + 14; cx++) {
                    for (int cz = c0z - 14; cz <= c0z + 14; cz++) {
                        Kaart k = m.kaart(cx, cz);
                        if (k.leeg) {
                            continue;
                        }
                        for (int o = 0; o < 256; o++) {
                            if (k.meng[o] < 1f || k.soort[o] == Kaart.WEIDE) {
                                continue;
                            }
                            int x = (cx << 4) + (o & 15), z = (cz << 4) + (o >> 4), h = k.hoogte[o], wat = k.water[o];
                            if (k.terras[o] >= 0) {
                                terras[k.terras[o]]++;
                                boolean rv = (k.vlag[o] & Kaart.RIVIER) != 0;
                                int top = DalTerrein.HOOGTE[k.terras[o]];
                                if (!rv && h != top || rv && wat != Kaart.GEEN && (wat != top - 1 || h < top - 3)) {
                                    fout++;
                                    eerste = eerste.isEmpty() ? "terrace height at " + x + " " + z : eerste;
                                }
                                lip += (k.vlag[o] & Kaart.LIP) != 0 ? 1 : 0;
                            } else {
                                meerKolommen++;
                                if (wat != Kaart.GEEN) {
                                    int d = wat - h;
                                    ondiep += d <= 2 ? 1 : 0;
                                    diep += d >= 5 ? 1 : 0;
                                    teDiep += d > 9 ? 1 : 0;
                                }
                                groot += (k.vlag[o] & Kaart.GROOT) != 0 ? 1 : 0;
                            }
                            if (wat != Kaart.GEEN) {
                                rivier += (k.vlag[o] & Kaart.RIVIER) != 0 ? 1 : 0;
                                for (Direction d : Direction.Plane.HORIZONTAL) {
                                    int bx = x + d.getStepX(), bz = z + d.getStepZ();
                                    int bw = m.water(bx, bz);
                                    if (bw == Kaart.GEEN && (m.meng(bx, bz) < 1f || m.hoogte(bx, bz) < wat)) {
                                        lekken++;
                                        eerste = eerste.isEmpty() ? "leak at " + x + " " + z + " to " + bx + " " + bz : eerste;
                                    }
                                    if (bw != Kaart.GEEN && bw < wat) {
                                        vallen++;
                                        hogeVal += BioPlekken.val(m, x, z, d) >= 10 ? 1 : 0;
                                        if ((k.vlag[o] & Kaart.VAL) == 0) {
                                            fout++;
                                            eerste = eerste.isEmpty() ? "fall without the flag at " + x + " " + z : eerste;
                                        }
                                    }
                                }
                            }
                            // the real terrain: solid at the top block, open above it (one column in eleven)
                            if (o % 11 == 0) {
                                double vast = dichtheid.compute(new DensityFunction.SinglePointContext(x, h, z));
                                double open = dichtheid.compute(new DensityFunction.SinglePointContext(x, h + 1, z));
                                if (!(vast > 0) || open > 0) {
                                    fout++;
                                    eerste = eerste.isEmpty() ? "density at " + x + " " + h + " " + z + ": " + vast + " / " + open : eerste;
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("biomes3 dal: {} dals; terrace columns {} / {} / {} / {} (floor..rim), river water columns {}, lips {}, fall edges {} (tall {}), "
                        + "lake columns {} (depth 1-2: {}, 5+: {}, over 9: {}), large island columns {}, leaks {}, wrong {} {}", dalen, terras[0], terras[1], terras[2],
                terras[3], rivier, lip, vallen, hogeVal, meerKolommen, ondiep, diep, teDiep, groot, lekken, fout, eerste);
        helper.assertTrue(dalen >= 2, "dals with a lake found: " + dalen);
        helper.assertTrue(lekken == 0, "river and lake water never lies next to lower dry ground: " + lekken + " " + eerste);
        helper.assertTrue(fout == 0, "heights, flags and the real density agree with the model: " + fout + " " + eerste);
        helper.assertTrue(terras[0] > 500 && terras[1] > 500 && terras[2] > 500 && terras[3] > 500, "all four terraces exist");
        helper.assertTrue(rivier > 300 && vallen >= 6, "rivers with falls: " + rivier + " water columns, " + vallen + " fall edges");
        helper.assertTrue(hogeVal >= 1, "a tall waterfall (10+) somewhere");
        helper.assertTrue(ondiep > 200 && diep > 1000 && teDiep == 0, "the lake: shallow shores, 5-8 in the middle");
        helper.assertTrue(groot > 150, "a large island in a lake: " + groot + " columns");
        helper.succeed();
    }

    /**
     * The Wolkenweide: meadow, floating islands at many heights (solid in the real density, free air under them), stairs
     * whose steps are each one block higher and {@link WolkTerrein#TRAP_STAP} further, lift columns with free air.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldWolkenweide(GameTestHelper helper) {
        int weiden = 0, eilanden = 0, laag = 0, hoog = 0, trappen = 0, liften = 0, stromen = 0, fout = 0, wolken = 0;
        boolean[] vormen = new boolean[5];
        String eerste = "";
        for (long seed : SEEDS) {
            Wereld w = wereld(helper, seed);
            BioModel m = w.model;
            DensityFunction dichtheid = w.random.router().finalDensity();
            for (int[] plek : zoek(m, Kaart.WEIDE, 3)) {
                weiden++;
                for (WolkTerrein.Eiland ei : WolkTerrein.bij(m, plek[0] - 200, plek[1] - 200, plek[0] + 200, plek[1] + 200)) {
                    eilanden++;
                    vormen[ei.vorm] = true;
                    int grond = WolkTerrein.grond(m, ei.x, ei.z);
                    laag += ei.top - grond <= 20 ? 1 : 0;
                    hoog += ei.top - grond >= 45 ? 1 : 0;
                    // the island is really there: somewhere within its outline the real density is solid at its top block
                    boolean vast = false;
                    for (int dx = -3; dx <= 3 && !vast; dx++) {
                        for (int dz = -3; dz <= 3 && !vast; dz++) {
                            if (ei.binnen(dx, dz) > 0.2 && m.meng(ei.x + dx, ei.z + dz) >= 1f) {
                                vast = dichtheid.compute(new DensityFunction.SinglePointContext(ei.x + dx, ei.top, ei.z + dz)) > 0
                                        && !(dichtheid.compute(new DensityFunction.SinglePointContext(ei.x + dx, ei.top + 1, ei.z + dz)) > 0);
                            }
                        }
                    }
                    if (!vast && ei.vorm != WolkTerrein.GAT && ei.vorm != WolkTerrein.MAAN && ei.vorm != WolkTerrein.DUBBEL && m.meng(ei.x, ei.z) >= 1f) {
                        fout++;
                        eerste = eerste.isEmpty() ? "no solid island at " + ei.x + " " + ei.top + " " + ei.z : eerste;
                    }
                    if (ei.trap != null && ei.trapStappen > 0) {
                        trappen++;
                        for (int s = 0; s < ei.trapStappen; s++) {
                            int x = ei.trapX[s], z = ei.trapZ[s], top = ei.top - (s + 1);
                            if (m.meng(x, z) >= 1f && m.luchtVrij(x, z, top, top)) {
                                fout++;
                                eerste = eerste.isEmpty() ? "stair step missing at " + x + " " + top + " " + z : eerste;
                            }
                            if (s > 0 && Math.abs(ei.trapX[s] - ei.trapX[s - 1]) + Math.abs(ei.trapZ[s] - ei.trapZ[s - 1]) != WolkTerrein.TRAP_STAP) {
                                fout++;
                            }
                        }
                        // the first step touches the island's edge, the last is at most two above the meadow
                        int laatste = ei.trapStappen - 1;
                        if (ei.top - ei.trapStappen - WolkTerrein.grond(m, ei.trapX[laatste], ei.trapZ[laatste]) > 2) {
                            fout++;
                            eerste = eerste.isEmpty() ? "stair ends too high at " + ei.trapX[laatste] + " " + ei.trapZ[laatste] : eerste;
                        }
                    }
                    if (ei.lift != null && WolkTerrein.liftVrij(m, ei.liftX, ei.liftZ, ei.liftGrond, ei.top + 2)) {
                        liften++;
                    }
                    if (ei.stroom != null && WolkTerrein.liftVrij(m, ei.stroomX, ei.stroomZ, ei.stroomGrond, ei.top + 1)) {
                        stromen++;
                    }
                }
                for (int cx = Math.floorDiv(plek[0] - 200, WolkTerrein.WOLK_CEL); cx <= Math.floorDiv(plek[0] + 200, WolkTerrein.WOLK_CEL); cx++) {
                    for (int cz = Math.floorDiv(plek[1] - 200, WolkTerrein.WOLK_CEL); cz <= Math.floorDiv(plek[1] + 200, WolkTerrein.WOLK_CEL); cz++) {
                        wolken += WolkTerrein.wolken(m, cx, cz).length > 0 ? 1 : 0;
                    }
                }
            }
        }
        LOGGER.info("biomes3 Wolkenweide: {} meadows; {} islands (within 20 of the meadow {}, 45 or more above {}), stairs {}, lifts {}, streams down {}, "
                + "cloud banks {}, wrong {} {}", weiden, eilanden, laag, hoog, trappen, liften, stromen, wolken, fout, eerste);
        helper.assertTrue(weiden >= 2, "Wolkenweides found: " + weiden);
        helper.assertTrue(fout == 0, "islands and stairs are where the model says: " + fout + " " + eerste);
        helper.assertTrue(eilanden >= 12 && laag >= 3 && hoog >= 2, "islands at many heights: " + eilanden + " (" + laag + " low, " + hoog + " high)");
        helper.assertTrue(vormen[WolkTerrein.ROND] && vormen[WolkTerrein.LANG] && vormen[WolkTerrein.MAAN] && vormen[WolkTerrein.DUBBEL]
                && vormen[WolkTerrein.GAT], "every shape occurs");
        helper.assertTrue(trappen >= 2 && liften >= 2 && wolken >= 4, "ways up and clouds: stairs " + trappen + ", lifts " + liften + ", clouds " + wolken);
        helper.succeed();
    }

    /**
     * Every kind of structure spot is found somewhere near a dal or a Wolkenweide, and is what it says: the bank looks at
     * river water, the bridge spot has a bank on both sides across its axis, the foot of a fall looks at higher falling
     * water, the island spot is flat island ground, the air spot is {@code hoogte} above the meadow.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void bioWereldPlekken(GameTestHelper helper) {
        Map<BioPlekken.Soort, Integer> gevonden = new EnumMap<>(BioPlekken.Soort.class);
        String fout = "";
        for (long seed : SEEDS) {
            Wereld w = wereld(helper, seed);
            BioModel m = w.model;
            List<int[]> plekken = new ArrayList<>(zoek(m, Kaart.MEER, 3));
            plekken.addAll(zoek(m, Kaart.WEIDE, 3));
            for (int[] plek : plekken) {
                for (int cx = (plek[0] >> 4) - 16; cx <= (plek[0] >> 4) + 16; cx++) {
                    for (int cz = (plek[1] >> 4) - 16; cz <= (plek[1] >> 4) + 16; cz++) {
                        if (m.kaart(cx, cz).leeg) {
                            continue;
                        }
                        for (BioPlekken.Soort s : BioPlekken.Soort.values()) {
                            Optional<BioPlekken.Plek> gev = BioPlekken.zoek(m, s, cx, cz, 30);
                            if (gev.isEmpty()) {
                                continue;
                            }
                            BioPlekken.Plek p = gev.get();
                            gevonden.merge(s, 1, Integer::sum);
                            int sx = p.kijk().getStepX(), sz = p.kijk().getStepZ();
                            boolean goed = (p.x() >> 4) == cx && (p.z() >> 4) == cz && switch (s) {
                                case TERRAS -> m.droog(p.x(), p.z()) && m.hoogte(p.x(), p.z()) == p.y() && m.droog(p.x() + 6, p.z() - 6);
                                case OEVER -> m.droog(p.x(), p.z()) && m.water(p.x() + sx, p.z() + sz) == p.y() - 1;
                                case OVER_RIVIER -> m.water(p.x(), p.z()) == p.y() - 1 && m.water(p.x() + sx * 3, p.z() + sz * 3) == p.y() - 1;
                                case WATERVAL -> m.droog(p.x(), p.z()) && m.terras(p.x(), p.z()) < 3;
                                case ROTS -> m.droog(p.x(), p.z()) && m.terras(p.x(), p.z()) >= 2;
                                case MEER_OEVER -> m.droog(p.x(), p.z()) && m.water(p.x() + sx * 4, p.z() + sz * 4) == MeerTerrein.WATER;
                                case MEER_EILAND, MEER_BOOM -> (m.vlag(p.x(), p.z()) & Kaart.GROOT) != 0 && m.hoogte(p.x(), p.z()) == p.y();
                                case WEIDE -> m.soort(p.x(), p.z()) == Kaart.WEIDE && m.hoogte(p.x(), p.z()) == p.y();
                                case LUCHT -> p.y() == m.hoogte(p.x(), p.z()) + 30;
                                case ZWEEFEILAND -> !m.luchtVrij(p.x(), p.z(), p.y(), p.y()) && m.luchtVrij(p.x(), p.z(), p.y() + 1, p.y() + 4);
                            };
                            if (!goed && fout.isEmpty()) {
                                fout = s + " at " + p;
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("biomes3 structure spots per kind (chunks with one, around 3 dals and 3 Wolkenweides of 2 seeds): {}", gevonden);
        helper.assertTrue(fout.isEmpty(), "a spot is what its kind says: " + fout);
        for (BioPlekken.Soort s : BioPlekken.Soort.values()) {
            helper.assertTrue(gevonden.getOrDefault(s, 0) >= 1, "a spot of kind " + s.getSerializedName() + " exists: " + gevonden);
        }
        helper.succeed();
    }

    /** The structure type: the test structures of every kind are read as guhs:bio_plek, inert on a normal server; the turn follows "kijk". */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioWereldPlekStructuur(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        int n = 0;
        for (BioPlekken.Soort s : BioPlekken.Soort.values()) {
            for (String biome : new String[]{"klaterdal", "bloesemmeertje", "wolkenweide"}) {
                var st = structures.getValue(Guhs.id(biome + "_plektest_" + s.getSerializedName()));
                if (st instanceof BioPlekStructure b) {
                    n++;
                    helper.assertTrue(b.soort() == s, "the kind of " + biome + "_plektest_" + s.getSerializedName());
                    helper.assertTrue(b.doetMee() == BioPlekStructure.TEST_AAN, "test structures only generate on a test server");
                }
            }
        }
        helper.assertTrue(n == BioPlekken.Soort.values().length, "a test structure for every kind of spot: " + n);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Rotation r = BioPlekStructure.draai(d);
            helper.assertTrue(r.rotate(Direction.NORTH) == d, "the template's north side turns to " + d);
        }
        helper.succeed();
    }
}
