package nl.juiced.guhs.feature.einde2.vesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.guheinde.GuheindeEvents;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.MagereCellen;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.reisguh.ReisguhGameTests;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.Terugkeer;
import org.slf4j.Logger;

/**
 * Game tests of the modular Mika-vesting (tools/features/einde2_vesting.py): the pieces, pools and markers the data pack
 * holds; whole vestingen the way the chunk generator starts them, sampled from the Guheinde's own generator
 * ({@link Einde2Test}: what is always there, the height, the reach, the cells); that the same chunks start a vesting as
 * before einde2; the cells per player with their count; the Guhvleugels per player.
 */
public class E2VestingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EMPTY = "empty";
    private static final String KAMER = "einde2_vesting_test_kamer";
    private static final String CEL = "einde2_vesting_test_cel";
    private static final String BATCH = "einde2_vesting";
    private static final long[] SEEDS = {21400701L, 21400702L};
    /** No block of a vesting lies higher above the floor of its start than this (tools/features/einde2_vesting_bouw.py MAX_BOVEN - 1). */
    private static final int MAX_BOVEN = 43;
    /** tools/features/einde2_vesting_bouw.py MAX_AFSTAND. */
    private static final int MAX_AFSTAND = 80;
    private static final Pattern TEMPLATE = Pattern.compile("Left\\[([^\\]]+)\\]");
    private static final List<String> ALTIJD = List.of("kern", "fundering", "keuken", "troon", "dak", "cellenblok", "steiger");
    private static final List<String> SCHEPEN = List.of("schip_groot", "schip_sloep", "schip_ballon");
    /** The structure as it was before einde2 (features/guheinde_bouw.py), without a claim on room: the old rule for "does a vesting start here". */
    private static final String OUD = """
            {"type": "guhs:guheinde_eiland", "biomes": "#guhs:has_structure/mika_vesting", "step": "surface_structures", "spawn_overrides": {},
             "terrain_adaptation": "none", "min_distance": 1100, "min_surface_y": 50, "check_radius": 10,
             "jigsaw": {"type": "minecraft:jigsaw", "biomes": "#guhs:has_structure/mika_vesting", "step": "surface_structures", "spawn_overrides": {},
                        "terrain_adaptation": "none", "start_pool": "guhs:mika_vesting/start", "size": 1, "start_height": {"absolute": 0},
                        "project_start_to_heightmap": "WORLD_SURFACE_WG", "start_jigsaw_name": "guhs:mika_vesting_midden",
                        "max_distance_from_center": 64, "use_expansion_hack": false}}""";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static boolean gedaan(ServerPlayer player, String naam) {
        var holder = player.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The names (without {@code einde2_vesting/}) of the templates the pieces of this start were made from. */
    private static List<String> stukken(StructureStart start) {
        List<String> uit = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            Identifier t = PleinSlot.template(piece);
            uit.add(t == null ? "?" : t.getPath().startsWith(E2VestingFeature.STUKKEN) ? t.getPath().substring(E2VestingFeature.STUKKEN.length()) : t.toString());
        }
        return uit;
    }

    /** The chunks the vesting's set offers, region by region outwards (the set is a plain grid: the same for every structure in it). */
    private static List<ChunkPos> kandidaten(GameTestHelper helper, long seed, int ring) {
        StructureSet set = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(Einde2Ids.MIKA_VESTING));
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
        List<ChunkPos> uit = new ArrayList<>();
        for (int r = 3; r <= ring; r++) {
            for (int rx = -r; rx <= r; rx++) {
                for (int rz = -r; rz <= r; rz++) {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) == r) {
                        uit.add(spread.getPotentialStructureChunk(seed, rx * spread.spacing(), rz * spread.spacing()));
                    }
                }
            }
        }
        return uit;
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the data: pieces, pools, markers, and what must never change
    // ------------------------------------------------------------------------------------------------------------------

    /** Every pool of the vesting exists and names templates that exist; the structure starts from them; the old ones stay. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2VestingStukkenEnPools(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Set<String> templates = new TreeSet<>();
        int aantal = 0;
        for (var e : pools.entrySet()) {
            Identifier id = e.getKey().identifier();
            if (!id.getNamespace().equals(Guhs.MODID) || !id.getPath().startsWith(E2VestingFeature.STUKKEN)) {
                continue;
            }
            aantal++;
            StructureTemplatePool pool = e.getValue();
            helper.assertTrue(pool.size() > 0, "pool " + id + " is empty");
            for (StructurePoolElement element : pool.getShuffledTemplates(RandomSource.create(1))) {
                Matcher m = TEMPLATE.matcher(element.toString());
                helper.assertTrue(m.find(), "pool " + id + " holds something that is not a template: " + element);
                Identifier t = Identifier.parse(m.group(1));
                helper.assertTrue(t.getPath().startsWith(E2VestingFeature.STUKKEN), "pool " + id + " holds a piece of somebody else: " + t);
                helper.assertTrue(level.getStructureManager().get(t).isPresent(), "pool " + id + ": no template " + t);
                templates.add(t.getPath().substring(E2VestingFeature.STUKKEN.length()));
            }
        }
        helper.assertTrue(aantal >= 20 && templates.size() >= 28, "the vesting has its pools and pieces: " + aantal + " pools, " + templates.size() + " pieces");
        helper.assertTrue(templates.containsAll(ALTIJD) && templates.containsAll(SCHEPEN), "the keep, the cellenblok, the steiger and three ships: " + templates);
        helper.assertTrue(templates.stream().filter(t -> t.startsWith("toren_")).count() >= 3 && templates.stream().filter(t -> t.startsWith("brug_")).count() >= 3,
                "several tower types and bridges: " + templates);
        for (String t : templates) {                                 // (no piece is too big for a jigsaw, none is empty)
            Vec3i size = level.getStructureManager().get(Guhs.id(E2VestingFeature.STUKKEN + t)).orElseThrow().getSize();
            helper.assertTrue(size.getX() > 0 && size.getX() <= 48 && size.getZ() <= 48 && size.getY() <= 32, t + " has size " + size);
        }
        // the structure: what a world knows it by is what it always was, only its jigsaw starts from our pool
        JsonObject s = ReisguhGameTests.json("/data/guhs/worldgen/structure/mika_vesting.json");
        JsonObject j = s.getAsJsonObject("jigsaw");
        helper.assertTrue(s.get("type").getAsString().equals("guhs:guheinde_eiland") && s.get("min_distance").getAsInt() == 1100
                && s.get("min_surface_y").getAsInt() == 50 && s.get("check_radius").getAsInt() == 10, "the ground check of the vesting is unchanged");
        helper.assertTrue(j.get("start_pool").getAsString().equals("guhs:einde2_vesting/start") && j.get("start_jigsaw_name").getAsString().equals("guhs:mika_vesting_midden")
                && j.get("size").getAsInt() == E2VestingFeature.DIEPTE && j.get("max_distance_from_center").getAsInt() == MAX_AFSTAND, "the jigsaw starts from our pool: " + j);
        helper.assertTrue(j.get("max_distance_from_center").getAsInt() + 12 <= 128 && j.get("max_distance_from_center").getAsInt() <= 96, "its reach fits");
        JsonObject p = ReisguhGameTests.json("/data/guhs/worldgen/structure_set/mika_vesting.json").getAsJsonObject("placement");
        helper.assertTrue(p.get("salt").getAsInt() == 20260926 && p.get("spacing").getAsInt() == 20 && p.get("separation").getAsInt() == 8,
                "the set of the vesting is unchanged: " + p);
        // what a vesting from before names is still there
        helper.assertTrue(pools.getValue(Guhs.id("mika_vesting/start")) != null && level.getStructureManager().get(Guhs.id("mika_vesting")).isPresent()
                && level.getStructureManager().get(Guhs.id("mika_vesting_schip")).isPresent(), "the old pool and its two templates stay");
        helper.succeed();
    }

    /** The markers of the build contract are in the sidecar, each in a piece that exists, inside that piece. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2VestingPlekken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Plekken.Blad blad = Plekken.blad(Einde2Ids.MIKA_VESTING);
        helper.assertTrue(blad.soort().equals("jigsaw"), "the vesting has a jigsaw sidecar");
        Map<String, Plekken.Plek> plekken = blad.plekken();
        Map<String, String> wil = new TreeMap<>(Map.of("kok", Plekken.NPC, "kapitein", Plekken.NPC, "keuken", Plekken.ZONE, "schatkamer", Plekken.ZONE,
                "vetbootje_opdracht", Plekken.PROP));
        for (int i = 1; i <= 8; i++) {
            wil.put("cel_" + i, Plekken.PROP);
        }
        for (Map.Entry<String, String> e : wil.entrySet()) {
            Plekken.Plek plek = plekken.get(e.getKey());
            helper.assertTrue(plek != null && plek.soort().equals(e.getValue()), "plek " + e.getKey() + " (" + e.getValue() + ") is there");
        }
        helper.assertTrue("MIKA_KOK".equals(plekken.get("kok").kind()) && "VESTINGKAPITEIN_MIKA".equals(plekken.get("kapitein").kind()),
                "the kok and the kapitein name the characters of phase B");
        helper.assertTrue(plekken.get("kok").stuk().equals(plekken.get("keuken").stuk()), "the kok stands in the kitchen piece");
        for (Plekken.Plek plek : plekken.values()) {
            helper.assertTrue(plek.stuk() != null && plek.stuk().startsWith(E2VestingFeature.STUKKEN), "plek " + plek.id() + " names its piece");
            StructureTemplate template = level.getStructureManager().get(Guhs.id(plek.stuk())).orElse(null);
            helper.assertTrue(template != null, "plek " + plek.id() + ": no template " + plek.stuk());
            BoundingBox doos = new BoundingBox(0, 0, 0, template.getSize().getX() - 1, template.getSize().getY() - 1, template.getSize().getZ() - 1);
            helper.assertTrue(doos.isInside(plek.pos()) && (plek.tot() == null || doos.isInside(plek.tot())), "plek " + plek.id() + " lies inside " + plek.stuk());
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // whole vestingen, the way the chunk generator starts them
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * Vestingen sampled from the Guheinde's generator: each has the keep up to its roof, the cellenblok, the steiger and
     * exactly one ship; nothing higher than {@link #MAX_BOVEN} above the floor of the start or above y 127, nothing further
     * than its reach or than 8 chunks from its start chunk; its cells are where the markers say; it is a modular copy
     * (so: protected).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void einde2VestingStart(GameTestHelper helper) {
        Set<String> gezien = new TreeSet<>();
        int totaal = 0, top = 0, hoogsteY = 0, meeste = 0, minste = Integer.MAX_VALUE;
        for (long seed : SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            int gevonden = 0;
            for (ChunkPos chunk : kandidaten(helper, seed, 14)) {
                StructureStart start = Einde2Test.start(w, Einde2Ids.MIKA_VESTING, chunk);
                if (start == null) {
                    continue;
                }
                List<String> namen = stukken(start);
                String wat = "seed " + seed + " " + chunk + " " + namen;
                helper.assertTrue(namen.get(0).equals("kern"), "starts with the kern: " + wat);
                for (String s : ALTIJD) {
                    helper.assertTrue(namen.stream().filter(s::equals).count() == 1, "exactly one " + s + ": " + wat);
                }
                helper.assertTrue(namen.stream().filter(SCHEPEN::contains).count() == 1, "exactly one vetschip: " + wat);
                helper.assertTrue(namen.stream().filter(n -> n.equals("slaapzaal") || n.equals("depot")).count() == 1, "the second storey: " + wat);
                helper.assertTrue(E2VestingFeature.modulair(start), "a modular copy: " + wat);
                BoundingBox kern = start.getPieces().get(0).getBoundingBox();
                int vloer = kern.minY(), mx = (kern.minX() + kern.maxX()) / 2, mz = (kern.minZ() + kern.maxZ()) / 2;
                for (StructurePiece piece : start.getPieces()) {
                    BoundingBox b = piece.getBoundingBox();
                    helper.assertTrue(b.maxY() - vloer <= MAX_BOVEN && b.maxY() <= Einde2Ids.EILANDBOUW_MAX && b.minY() > 0,
                            "a piece at y " + b.minY() + ".." + b.maxY() + " (floor " + vloer + "): " + wat);
                    helper.assertTrue(b.minX() >= mx - MAX_AFSTAND && b.maxX() <= mx + MAX_AFSTAND && b.minZ() >= mz - MAX_AFSTAND && b.maxZ() <= mz + MAX_AFSTAND,
                            "a piece outside the reach: " + b + ": " + wat);
                    helper.assertTrue((b.minX() >> 4) >= chunk.x() - 8 && (b.maxX() >> 4) <= chunk.x() + 8 && (b.minZ() >> 4) >= chunk.z() - 8
                            && (b.maxZ() >> 4) <= chunk.z() + 8, "a piece further than 8 chunks from the start chunk: " + wat);
                    top = Math.max(top, b.maxY() - vloer);
                    hoogsteY = Math.max(hoogsteY, b.maxY());
                }
                List<BlockPos> cellen = VestingCellen.cellen(start);
                long kerkers = namen.stream().filter("toren_kerker"::equals).count();
                helper.assertTrue(cellen.size() == 6 + 2 * kerkers, cellen.size() + " cells for " + kerkers + " kerkertorens: " + wat);
                for (BlockPos cel : cellen) {
                    helper.assertTrue(start.getPieces().stream().anyMatch(piece -> piece.getBoundingBox().isInside(cel)
                            && PleinSlot.template(piece) != null && (PleinSlot.template(piece).getPath().endsWith("cellenblok")
                            || PleinSlot.template(piece).getPath().endsWith("toren_kerker"))), "cell " + cel + " lies in a cell piece: " + wat);
                }
                for (String id : List.of("kok", "kapitein", "vetbootje_opdracht", "schatkamer")) {
                    helper.assertTrue(Plekken.wereld(start, Plekken.plek(Einde2Ids.MIKA_VESTING, id)) != null, "plek " + id + " has a spot in this copy: " + wat);
                }
                gezien.addAll(namen);
                meeste = Math.max(meeste, namen.size());
                minste = Math.min(minste, namen.size());
                totaal++;
                if (++gevonden >= 12) {
                    break;
                }
            }
            helper.assertTrue(gevonden >= 6, "seed " + seed + ": only " + gevonden + " vestingen in 14 rings of the grid");
        }
        LOGGER.info("einde2_vesting: {} vestingen sampled: {}-{} pieces, at most {} above the floor of the start, highest block y {}, {} different pieces: {}",
                totaal, minste, meeste, top, hoogsteY, gezien.size(), gezien);
        helper.assertTrue(gezien.stream().filter(SCHEPEN::contains).count() >= 2, "more than one kind of ship comes by: " + gezien);
        helper.assertTrue(gezien.stream().filter(n -> n.startsWith("toren_")).count() >= 2 && gezien.size() >= 18, "vestingen differ: " + gezien);
        helper.succeed();
    }

    /**
     * The server world is never reset: the chunks that start a vesting are the chunks that started one before einde2. For
     * 200 chunks of the set's grid: the structure of today starts there exactly when the structure as it was would have.
     * And what the old one makes is no modular copy (so it stays unprotected, as it is).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 6000)
    public static void einde2VestingZelfdePlekken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var access = level.registryAccess();
        Structure oud = Structure.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), JsonParser.parseString(OUD)).getOrThrow();
        Holder<Structure> houder = Holder.direct(oud);
        int samen = 0, wel = 0;
        List<String> anders = new ArrayList<>();
        boolean oudGezien = false;
        for (long seed : SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            int n = 0;
            for (ChunkPos chunk : kandidaten(helper, seed, 9)) {
                if (n++ >= 100) {
                    break;
                }
                StructureStart was = oud.generate(houder, level.dimension(), access, w.generator(), w.generator().getBiomeSource(), w.random(),
                        level.getStructureManager(), seed, chunk, 0, w.hoogte(), oud.biomes()::contains);
                StructureStart nu = Einde2Test.start(w, Einde2Ids.MIKA_VESTING, chunk);
                samen++;
                if (was.isValid()) {
                    wel++;
                    if (!oudGezien) {
                        oudGezien = true;
                        helper.assertTrue(!E2VestingFeature.modulair(was), "a vesting of the old templates is not a modular copy");
                    }
                }
                if (was.isValid() != (nu != null)) {
                    anders.add("seed " + seed + " " + chunk + ": before " + was.isValid() + ", now " + (nu != null));
                }
            }
        }
        LOGGER.info("einde2_vesting: {} chunks of the grid, a vesting in {} of them, before and now", samen, wel);
        helper.assertTrue(samen == 200 && wel >= 20, samen + " chunks sampled, " + wel + " with a vesting");
        helper.assertTrue(anders.isEmpty(), "the same chunks start a vesting as before: " + anders);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the cells
    // ------------------------------------------------------------------------------------------------------------------

    /**
     * A magere guh of a vesting cell, the way the templates hold it: feeding it frees a colourful guh and counts, once per
     * player per guh; a second player gets a turn of their own; a magere guh of a Knabbelkelder does not count here; the
     * sixth gives the second advancement.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2VestingCellenPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper), b = speler(helper);
        List<GuhEntity> extra = new ArrayList<>();
        // (a test room comes without the entities of its template: put the cell with its guh down the way the game places a piece)
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 1, 0));
        level.getStructureManager().get(Guhs.id(CEL)).orElseThrow().placeInWorld(level, hoek, hoek, new StructurePlaceSettings(), level.getRandom(), 2);
        helper.runAfterDelay(5, () -> {
            try {
                List<GuhEntity> guhs = level.getEntitiesOfClass(GuhEntity.class, new AABB(helper.absolutePos(new BlockPos(3, 2, 3))).inflate(3),
                        g -> g.getVariant() == GuhVariant.MAGER);
                helper.assertTrue(guhs.size() == 1 && VestingCellen.isCelGuh(guhs.get(0)), "the template holds one cell guh with its tag: " + guhs.size());
                GuhEntity guh = guhs.get(0);
                a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
                b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 16));
                helper.assertTrue(VestingCellen.bevrijd(a) == 0 && !gedaan(a, VestingCellen.EERSTE), "nothing freed yet");
                a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
                helper.assertTrue(VestingCellen.bevrijd(a) == 1 && gedaan(a, VestingCellen.EERSTE) && !gedaan(a, VestingCellen.ZES), "the first one counts");
                helper.assertTrue(a.getMainHandItem().getCount() == 15 && guh.isAlive() && guh.getVariant() == GuhVariant.MAGER, "it ate the knabbel and stays in its cell");
                a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
                helper.assertTrue(VestingCellen.bevrijd(a) == 1 && a.getMainHandItem().getCount() == 15, "the same guh does not count twice for one player");
                helper.assertTrue(VestingCellen.bevrijd(b) == 0, "per player");
                b.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
                helper.assertTrue(VestingCellen.bevrijd(b) == 1 && VestingCellen.bevrijd(a) == 1 && gedaan(b, VestingCellen.EERSTE), "the second player frees it too");
                // a magere guh that is not of a vesting (a Knabbelkelder's): the Guheinde counts it, we don't
                GuhEntity kelder = MagereCellen.nieuweMagere(level, helper.absolutePos(new BlockPos(1, 2, 5)));
                extra.add(kelder);
                a.interactOn(kelder, InteractionHand.MAIN_HAND, kelder.position());
                helper.assertTrue(VestingCellen.bevrijd(a) == 1 && GuhQuests.saved(a).getIntOr(GuheindeEvents.GERED, 0) == 2, "a kelder guh is not a vesting guh");
                // an empty hand asks for food and counts nothing
                GuhEntity tweede = VestingCellen.nieuwe(level, helper.absolutePos(new BlockPos(5, 2, 5)));
                extra.add(tweede);
                helper.assertTrue(VestingCellen.isCelGuh(tweede), "a new cell guh carries the tag");
                ItemStack hand = a.getMainHandItem().copy();
                a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                a.interactOn(tweede, InteractionHand.MAIN_HAND, tweede.position());
                helper.assertTrue(VestingCellen.bevrijd(a) == 1, "no knabbel, no count");
                a.setItemInHand(InteractionHand.MAIN_HAND, hand);
                a.interactOn(tweede, InteractionHand.MAIN_HAND, tweede.position());
                helper.assertTrue(VestingCellen.bevrijd(a) == 2, "the second cell guh");
                for (int i = 3; i <= VestingCellen.BEVRIJDER; i++) {
                    helper.assertTrue(!gedaan(a, VestingCellen.ZES), "not yet at " + (i - 1));
                    GuhEntity volgende = VestingCellen.nieuwe(level, helper.absolutePos(new BlockPos(1 + (i % 3) * 2, 2, 1)));
                    extra.add(volgende);
                    a.interactOn(volgende, InteractionHand.MAIN_HAND, volgende.position());
                    helper.assertTrue(VestingCellen.bevrijd(a) == i, "cell guh " + i);
                }
                helper.assertTrue(gedaan(a, VestingCellen.ZES) && !gedaan(b, VestingCellen.ZES), "six: the second advancement, for this player only");
                guh.discard();
            } finally {
                BlockPos midden = helper.absolutePos(new BlockPos(3, 2, 3));
                MagereCellen.vrij().stream().filter(g -> g.blockPosition().closerThan(midden, 24)).forEach(Entity::discard);
                extra.forEach(Entity::discard);
                weg(helper, a);
                weg(helper, b);
            }
            helper.succeed();
        });
    }

    /** An empty cell of a vesting gets a new magere guh (through the Guheinde's own cell code), and that one carries our tag. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2VestingCelVultBij(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cel = helper.absolutePos(new BlockPos(3, 2, 3));
        // (a room that was only just put down: its chunk's entities may still be loading, and then nothing comes back)
        helper.runAfterDelay(20, () -> {
            Terugkeer.zetGezien(level, MagereCellen.SOORT, cel, null);
            Terugkeer.zetGemist(level, MagereCellen.SOORT, cel, Terugkeer.BEVESTIG);
            List<GuhEntity> nieuw = VestingCellen.controleer(level, List.of(cel), cel.getCenter());
            try {
                helper.assertTrue(nieuw.size() == 1 && VestingCellen.isCelGuh(nieuw.get(0)) && cel.equals(MagereCellen.thuis(nieuw.get(0))),
                        "an empty cell gets a new cell guh with the tag: " + nieuw.size() + " (ticking " + level.isPositionEntityTicking(cel) + ", entities "
                                + level.areEntitiesLoaded(ChunkPos.pack(cel)) + ")");
                helper.assertTrue(VestingCellen.controleer(level, List.of(cel), cel.getCenter()).isEmpty(), "a cell with its guh gets no second one");
            } finally {
                nieuw.forEach(Entity::discard);
                Terugkeer.zetGezien(level, MagereCellen.SOORT, cel, null);
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------------
    // the Guhvleugels of the vetschip
    // ------------------------------------------------------------------------------------------------------------------

    /** The frame on the ship keeps its wings: every player gets a pair of their own from it, once (a click or a tap). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2VestingVleugelsPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemFrame frame = new ItemFrame(level, helper.absolutePos(new BlockPos(3, 3, 1)), Direction.SOUTH);
        frame.setItem(new ItemStack(GuheindeFeature.GUHVLEUGELS.get()), false);
        frame.addTag(VestingVleugels.TAG);
        level.addFreshEntity(frame);
        ServerPlayer a = speler(helper), b = speler(helper);
        try {
            helper.assertTrue(!VestingVleugels.heeft(a, frame), "nothing taken yet");
            a.interactOn(frame, InteractionHand.MAIN_HAND, frame.position());
            helper.assertTrue(GuhQuests.count(a, GuheindeFeature.GUHVLEUGELS.get()) == 1 && VestingVleugels.heeft(a, frame), "a click gives a pair");
            helper.assertTrue(frame.isAlive() && frame.getItem().is(GuheindeFeature.GUHVLEUGELS.get()), "and the frame keeps showing its pair");
            a.interactOn(frame, InteractionHand.MAIN_HAND, frame.position());
            a.attack(frame);
            helper.assertTrue(GuhQuests.count(a, GuheindeFeature.GUHVLEUGELS.get()) == 1, "one pair per player per ship");
            helper.assertTrue(GuhQuests.count(b, GuheindeFeature.GUHVLEUGELS.get()) == 0 && !VestingVleugels.heeft(b, frame), "per player");
            b.attack(frame);
            helper.assertTrue(GuhQuests.count(b, GuheindeFeature.GUHVLEUGELS.get()) == 1 && frame.isAlive() && !frame.getItem().isEmpty(),
                    "a tap gives the second player a pair too, the frame stays whole");
        } finally {
            frame.discard();
            weg(helper, a);
            weg(helper, b);
        }
        helper.succeed();
    }
}
