package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of the slice geestenwereld: the dimension's data (type, biome, the feature, the version), every zone and its
 * markers in the tiles of the build (read from the templates: the test server has no datapack dimensions), the chunks the
 * build covers, protection, the way back (meditatiesteen), the void that puts you back at the arrival, the puzzle, the lore
 * and the residents (the shared Bewoners rule). A test box of the test level counts as Geestenwereld
 * ({@link Geestenwereld#testGebied}).
 */
public class E2GeestenwereldGameTests {
    private static final String BATCH = "einde2_geestenwereld";
    private static final String KAMER = "einde2_geestenwereld_test_kamer";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static Geestenwereld.TestGebied gebied(GameTestHelper helper, Vec3 aankomst, int leegteY) {
        AABB doos = helper.getBounds().inflate(0, 8, 0);
        Geestenwereld.TestGebied g = new Geestenwereld.TestGebied(helper.getLevel().dimension(), doos, aankomst, leegteY);
        Geestenwereld.testGebied(g);
        return g;
    }

    // --- the data ---------------------------------------------------------------------------------------------------------------

    /** The dimension type, the biome, the feature that puts the build down, its version and its sidecar are there. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2GeestenwereldDimensie(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(access.lookupOrThrow(Registries.DIMENSION_TYPE).get(ResourceKey.create(Registries.DIMENSION_TYPE, Guhs.id("geestenwereld"))).isPresent(),
                "the dimension type guhs:geestenwereld");
        var type = access.lookupOrThrow(Registries.DIMENSION_TYPE).getValue(Guhs.id("geestenwereld"));
        helper.assertTrue(type != null && type.hasFixedTime() && !type.hasCeiling(), "no day and night, no ceiling");
        helper.assertTrue(access.lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, Guhs.id("geestenwereld"))).isPresent(),
                "the biome guhs:geestenwereld");
        helper.assertTrue(access.lookupOrThrow(Registries.PLACED_FEATURE).get(ResourceKey.create(Registries.PLACED_FEATURE, Guhs.id("einde2_geestenwereld_stukken"))).isPresent(),
                "the placed feature that puts the build down");
        helper.assertTrue(BuiltInRegistries.FEATURE.getValue(Guhs.id("einde2_geestenwereld_stukken")) instanceof GeestBouw, "the feature type");
        helper.assertTrue(GeestBouw.versie() > 1, "the build has a version: " + GeestBouw.versie());
        Plekken.Blad blad = Plekken.blad(Geestenwereld.NAAM);
        helper.assertTrue(blad.soort().equals("vast") && blad.grootte().getX() == Geestenwereld.BREEDTE && blad.grootte().getY() == Geestenwereld.HOOGTE,
                "the sidecar of a build at a fixed spot: " + blad.soort() + " " + blad.grootte());
        helper.assertTrue(Math.floorMod(Geestenwereld.OORSPRONG.getX(), 16) == 0 && Math.floorMod(Geestenwereld.OORSPRONG.getZ(), 16) == 0,
                "the tiles line up with the chunks");
        helper.assertTrue(GeestBouw.inBouw(ChunkPos.containing(Geestenwereld.OORSPRONG), Geestenwereld.OORSPRONG)
                && GeestBouw.inBouw(new ChunkPos(18, 18), Geestenwereld.OORSPRONG) && !GeestBouw.inBouw(new ChunkPos(19, 0), Geestenwereld.OORSPRONG)
                && !GeestBouw.inBouw(new ChunkPos(-20, 0), Geestenwereld.OORSPRONG), "the build covers chunks -19..18");
        helper.succeed();
    }

    // --- the build ---------------------------------------------------------------------------------------------------------------

    /** The blocks of the build at its markers, read from the tile templates (block id per template position). */
    static final class Tegels {
        private final StructureTemplateManager manager;
        private final Map<String, Map<BlockPos, String>> cache = new HashMap<>();

        Tegels(ServerLevel level) {
            this.manager = level.getServer().getStructureManager();
        }

        /** The block at this template position of the whole build ("" when the tile does not exist; air when nothing is there). */
        String blok(BlockPos p) {
            int i = Math.floorDiv(p.getX(), GeestBouw.TILE), j = Math.floorDiv(p.getZ(), GeestBouw.TILE);
            Map<BlockPos, String> tile = cache.computeIfAbsent(i + "_" + j, k -> lees(i, j));
            if (tile == null) {
                return "";
            }
            return tile.getOrDefault(new BlockPos(p.getX() - i * GeestBouw.TILE, p.getY(), p.getZ() - j * GeestBouw.TILE), "minecraft:air");
        }

        String staat(BlockPos p, String prop) {
            String b = blok(p);
            int at = b.indexOf(prop + "=");
            if (at < 0) {
                return "";
            }
            int eind = b.indexOf(',', at);
            int haak = b.indexOf(']', at);
            int tot = eind < 0 ? haak : Math.min(eind, haak);
            return b.substring(at + prop.length() + 1, tot);
        }

        boolean bestaat(BlockPos p) {
            return !blok(p).isEmpty();
        }

        private Map<BlockPos, String> lees(int i, int j) {
            Optional<StructureTemplate> t = manager.get(GeestBouw.tile(i, j));
            if (t.isEmpty()) {
                return null;
            }
            CompoundTag nbt = t.get().save(new CompoundTag());
            ListTag palette = nbt.getListOrEmpty("palette");
            String[] namen = new String[palette.size()];
            for (int k = 0; k < palette.size(); k++) {
                CompoundTag e = palette.getCompoundOrEmpty(k);
                StringBuilder s = new StringBuilder(e.getStringOr("Name", ""));
                CompoundTag props = e.getCompoundOrEmpty("Properties");
                if (!props.isEmpty()) {
                    s.append('[');
                    List<String> keys = props.keySet().stream().sorted().toList();
                    for (int n = 0; n < keys.size(); n++) {
                        s.append(n > 0 ? "," : "").append(keys.get(n)).append('=').append(props.getStringOr(keys.get(n), ""));
                    }
                    s.append(']');
                }
                namen[k] = s.toString();
            }
            Map<BlockPos, String> uit = new HashMap<>();
            ListTag blokken = nbt.getListOrEmpty("blocks");
            for (int k = 0; k < blokken.size(); k++) {
                CompoundTag b = blokken.getCompoundOrEmpty(k);
                ListTag pos = b.getListOrEmpty("pos");
                uit.put(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), namen[b.getIntOr("state", 0)]);
            }
            return uit;
        }
    }

    private static boolean is(String blok, String id) {
        return blok.equals(id) || blok.startsWith(id + "[");
    }

    /** Every zone: its markers, its meditation stone and memorial stones in the tiles; the residents' spots are free. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2GeestenwereldZones(GameTestHelper helper) {
        Map<String, Plekken.Plek> plekken = Plekken.van(Geestenwereld.NAAM);
        Tegels t = new Tegels(helper.getLevel());
        for (String zone : Geestenwereld.ZONES) {
            for (String m : List.of("zone_" + zone, "plek_" + zone, "meditatiesteen_" + zone)) {
                helper.assertTrue(plekken.containsKey(m), "marker " + m);
            }
            BlockPos steen = plekken.get("meditatiesteen_" + zone).pos();
            helper.assertTrue(is(t.blok(steen), "guhs:meditatiesteen"), "a meditation stone in " + zone + ": " + t.blok(steen));
            BlockPos plek = plekken.get("plek_" + zone).pos();
            helper.assertTrue(t.blok(plek).equals("minecraft:air") && !t.blok(plek.below()).equals("minecraft:air"),
                    "you can stand at plek_" + zone + ": " + t.blok(plek.below()));
        }
        helper.assertTrue(plekken.containsKey("aankomst") && plekken.containsKey(Geestenwereld.AANKOMST) && plekken.containsKey("uitzicht"),
                "the standard markers");
        int lore = 0, lantaarns = 0, haken = 0, tegels = 0;
        for (Plekken.Plek p : plekken.values()) {
            if (!p.soort().equals(Plekken.PAD) && !p.soort().equals(Plekken.ZONE) && !p.id().equals("uitzicht") && !p.id().startsWith("geestreus_")) {
                helper.assertTrue(t.bestaat(p.pos()), "the tile of " + p.id() + " exists");
            }
            if (p.id().startsWith("lore_")) {
                lore++;
                helper.assertTrue(is(t.blok(p.pos()), "guhs:gedenksteen"), "a memorial stone at " + p.id() + ": " + t.blok(p.pos()));
            } else if (p.id().startsWith("verdwaalde_lantaarn_")) {
                lantaarns++;
            } else if (p.id().startsWith("lantaarnboom_haak_")) {
                haken++;
                // (the template says minecraft:chain; 26.1 reads it as minecraft:iron_chain)
                helper.assertTrue(t.blok(p.pos().above()).replace("iron_chain", "chain").startsWith("minecraft:chain"),
                        "an empty hook (a chain) over " + p.id() + ": " + t.blok(p.pos().above()));
            } else if (p.id().startsWith("tegel_") && !p.id().startsWith("tegel_voetstuk_")) {
                tegels++;
                helper.assertTrue(is(t.blok(p.pos()), "guhs:lotuspuzzeltegel"), "a puzzle tile at " + p.id());
            } else if (p.soort().equals(Plekken.NPC) && !"guhs:geest_koi".equals(p.entity()) && !"guhs:kijkmasker".equals(p.entity())) {
                helper.assertTrue(t.blok(p.pos()).equals("minecraft:air") && t.blok(p.pos().above()).equals("minecraft:air")
                        && !t.blok(p.pos().below()).equals("minecraft:air"), "the spot of " + p.id() + " is free, on a floor: " + t.blok(p.pos().below()));
            }
        }
        helper.assertTrue(lore >= 19 && lantaarns == 8 && haken == 8 && tegels == 9,
                "lore " + lore + ", lost lanterns " + lantaarns + ", hooks " + haken + ", tiles " + tegels);
        // the puzzle starts wrong (the middle may lie any way)
        BlockState basis = E2GeestenwereldFeature.LOTUSPUZZELTEGEL.get().defaultBlockState();
        helper.assertTrue(!Lotuspuzzel.opgelost(n -> basis.setValue(GeestBlokken.Lotuspuzzeltegel.DRAAI,
                Integer.parseInt(t.staat(plekken.get("tegel_" + n).pos(), "draai")))), "the puzzle is not solved when you arrive");
        // the residents' creatures and characters: the same names as the slice geestmodellen
        for (String[] r : new String[][]{{"koh", "guhs:koh"}, {"hei_bai", "guhs:hei_bai_guh"}, {"fang", "guhs:fang"}, {"koi_tui", "guhs:geest_koi"},
                {"koi_la", "guhs:geest_koi"}, {"lantaarngeest_1", "guhs:lantaarngeest"}, {"schaduwgeest_1", "guhs:schaduwgeest"},
                {"libelkonijnguh_1", "guhs:libelkonijnguh"}}) {
            helper.assertTrue(r[1].equals(plekken.get(r[0]).entity()), r[0] + " is " + r[1]);
        }
        helper.assertTrue("THEEGEEST_GUH".equals(plekken.get("theegeest").kind()) && "ROKU_GUH".equals(plekken.get("roku").kind())
                && "KYOSHI_GUH".equals(plekken.get("kyoshi").kind()), "the Theegeest, Roku-guh and Kyoshi-guh");
        helper.succeed();
    }

    // --- protection, the way back, the void -------------------------------------------------------------------------------------

    /** Nothing can be broken or built, nothing hurts; creative may; outside the box everything is as usual. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2GeestenwereldBescherming(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Geestenwereld.TestGebied g = gebied(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4))), helper.absolutePos(BlockPos.ZERO).getY() - 4);
        ServerPlayer p = speler(helper);
        try {
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            BlockState state = level.getBlockState(pos);
            helper.assertTrue(Geestenwereld.in(level, pos) && !Geestenwereld.in(level, pos.offset(40, 0, 0)), "the box is Geestenwereld");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, p)).isCanceled(), "no breaking");
            var place = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos.above()), Blocks.STONE.defaultBlockState(), p);
            helper.assertTrue(NeoForge.EVENT_BUS.post(place).isCanceled(), "no building");
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos.offset(40, 0, 0), state, p)).isCanceled(), "outside: as usual");
            var binnen = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            var buiten = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            buiten.snapTo(buiten.getX() + 40, buiten.getY(), buiten.getZ());
            float gezond = binnen.getHealth();
            helper.assertTrue(!binnen.hurtServer(level, level.damageSources().generic(), 2f) && binnen.getHealth() == gezond, "nothing hurts in there");
            helper.assertTrue(buiten.hurtServer(level, level.damageSources().generic(), 2f), "outside it does");
            binnen.discard();
            buiten.discard();
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, p)).isCanceled(), "creative may (to fix something)");
        } finally {
            Geestenwereld.testGebiedWeg(g);
            weg(helper, p);
        }
        helper.succeed();
    }

    /** A meditatiesteen sends you back to where your body was; with no spot remembered you wake up at your respawn point. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2GeestenwereldTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Geestenwereld.TestGebied g = gebied(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4))), helper.absolutePos(BlockPos.ZERO).getY() - 4);
        ServerPlayer p = speler(helper);
        try {
            Vec3 lijf = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 7)));
            p.snapTo(lijf.x, lijf.y, lijf.z, 45f, 0f);
            Geestenwereld.onthoud(p);
            helper.assertTrue(GuhQuests.saved(p).contains(Geestenwereld.TERUG), "the body's spot is remembered");
            p.snapTo(lijf.x - 5, lijf.y, lijf.z - 5, 0f, 0f);
            BlockPos steen = helper.absolutePos(new BlockPos(2, 2, 2));
            BlockState s = level.getBlockState(steen);
            helper.assertTrue(s.is(E2GeestenwereldFeature.MEDITATIESTEEN.get()), "the meditation stone of the test room");
            s.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(steen), Direction.UP, steen, false));
            helper.assertTrue(p.position().distanceTo(lijf) < 0.01 && Math.abs(p.getYRot() - 45f) < 0.01, "back at the body: " + p.position());
            helper.assertTrue(!GuhQuests.saved(p).contains(Geestenwereld.TERUG), "and forgotten");
            // outside the Geestenwereld the stone is just a stone
            Geestenwereld.testGebiedWeg(g);
            Geestenwereld.onthoud(p);
            p.snapTo(lijf.x - 5, lijf.y, lijf.z - 5, 0f, 0f);
            s.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(steen), Direction.UP, steen, false));
            helper.assertTrue(p.position().distanceTo(lijf) > 3 && GuhQuests.saved(p).contains(Geestenwereld.TERUG), "outside: nothing happens");
            GuhQuests.saved(p).remove(Geestenwereld.TERUG);
            // nothing remembered: the respawn point (the test world's spawn), never stuck
            Geestenwereld.testGebied(g);
            helper.assertTrue(Geestenwereld.terug(p), "no remembered spot: still out");
        } finally {
            Geestenwereld.testGebiedWeg(g);
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Falling below the void line puts you back at the arrival, softly; above it nothing happens. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2GeestenwereldLeegte(GameTestHelper helper) {
        BlockPos nul = helper.absolutePos(BlockPos.ZERO);
        Vec3 aankomst = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        Geestenwereld.TestGebied g = gebied(helper, aankomst, nul.getY() - 4);
        ServerPlayer p = speler(helper);
        try {
            p.snapTo(aankomst.x + 2, aankomst.y, aankomst.z + 1, 0f, 0f);
            helper.assertTrue(!Geestenwereld.vangOp(p), "standing on the island: nothing");
            p.snapTo(aankomst.x + 2, nul.getY() - 7, aankomst.z + 1, 0f, 0f);
            p.fallDistance = 30;
            helper.assertTrue(Geestenwereld.vangOp(p), "below the void line: caught");
            helper.assertTrue(p.position().distanceTo(aankomst) < 0.01 && p.fallDistance == 0, "back at the arrival, without a fall: " + p.position());
            Geestenwereld.testGebiedWeg(g);
            p.snapTo(aankomst.x + 2, nul.getY() - 7, aankomst.z + 1, 0f, 0f);
            helper.assertTrue(!Geestenwereld.vangOp(p), "outside the Geestenwereld the void is the void");
        } finally {
            Geestenwereld.testGebiedWeg(g);
            weg(helper, p);
        }
        helper.succeed();
    }

    // --- geest_bouw: the revamp ---------------------------------------------------------------------------------------------------

    /** CONTRACT_E2_B 4.5: id -> at least this many (0: the id itself). */
    private static final Map<String, Integer> REVAMP = new java.util.LinkedHashMap<>();

    static {
        for (String id : List.of("lotus_hof", "heiligdom", "stenen_lotus", "reiger", "lotusarchief", "archief_ingang", "lid_stichter",
                "trap_der_meesters", "poort_doorgang", "lotuspuzzel", "ijs_stroom", "verlovingsketting", "ritme_plek", "smidse", "meteoriet",
                "theetuin_tafel", "roku", "kyoshi", "top_landing", "top_speler", "top_camera", "koh_gang", "tui_la_eilandje", "tui_la_speler",
                "theetuin", "eiland_8", "pad_8_anker", "wan_onthulling", "wan_camera", "moeras", "moeras_visioen", "geestenklok", "klok",
                "guru_pathik")) {
            REVAMP.put(id, 0);
        }
        for (String m : List.of("bumi", "pakguh", "piandao", "jeong_jeong", "miroh")) {
            for (String id : List.of("vleugel_" + m, "echo_" + m, "proef_" + m + "_start", "proef_" + m + "_einde")) {
                REVAMP.put(id, 0);
            }
        }
        Object[][] rijen = {{"lid", 3}, {"meesterbeeld", 5}, {"bumi_kamer", 3}, {"bumi_draai", 3}, {"ijs_spiegel", 6}, {"ijs_platform", 4},
                {"kaars", 8}, {"lotusblad", 8}, {"geest_gast", 3}, {"theepot", 3}, {"tegel_voetstuk", 6}, {"bezienswaardigheid", 8},
                {"roddel", 4}, {"wan", 5}, {"spiegelvijver", 4}, {"voetstappen", 3}, {"geheim", 3}, {"kijkmasker", 12}, {"chakra", 7},
                {"kennisvos", 4}, {"mistreiger", 6}, {"geestreus", 4}};
        for (Object[] r : rijen) {
            REVAMP.put((String) r[0], (Integer) r[1]);
        }
        for (String z : List.of("gefluisterwoud", "koh_grot", "lotus_ruines", "top_voorgangers", "hemelvijver", "theepaviljoen", "fang_pad")) {
            REVAMP.put("padlantaarn_" + z, 3);
        }
    }

    /**
     * geest_bouw: every marker of CONTRACT_E2_B 4.5 is in the sidecar; the echo masters, old members and Guru Pathik are their
     * Kinds; the stone lotus, the pedestals, the path lanterns and the bell's rope stand in the tiles; the three overlays are
     * there, hold blocks and only fill air; the watching masks are residents, each with its own face.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2GeestenwereldRevamp(GameTestHelper helper) {
        Map<String, Plekken.Plek> plekken = Plekken.van(Geestenwereld.NAAM);
        for (Map.Entry<String, Integer> e : REVAMP.entrySet()) {
            if (e.getValue() == 0) {
                helper.assertTrue(plekken.containsKey(e.getKey()), "marker " + e.getKey());
            } else {
                long n = plekken.keySet().stream().filter(k -> k.matches(java.util.regex.Pattern.quote(e.getKey()) + "_\\d+")).count();
                helper.assertTrue(n >= e.getValue(), e.getKey() + "_*: " + n + " of at least " + e.getValue());
            }
        }
        Map<String, String> kinds = Map.of("echo_bumi", "BUMI_GUH_ECHO", "echo_pakguh", "PAKGUH_ECHO", "echo_piandao", "PIANDAO_GUH_ECHO",
                "echo_jeong_jeong", "JEONG_JEONG_GUH_ECHO", "echo_miroh", "OOM_MIROH_ECHO", "lid_stichter", "LOTUS_STICHTER_ECHO", "lid_1", "LOTUS_LID_ECHO",
                "guru_pathik", "GURU_PATHIK_GUH");
        kinds.forEach((id, kind) -> helper.assertTrue(kind.equals(plekken.get(id).kind()), id + " is " + kind));
        helper.assertTrue("guhs:oude_reigerguh".equals(plekken.get("reiger").entity()) && "guhs:kijkmasker".equals(plekken.get("kijkmasker_1").entity()),
                "the heron and the masks");
        Tegels t = new Tegels(helper.getLevel());
        helper.assertTrue(is(t.blok(plekken.get("stenen_lotus").pos()), "minecraft:quartz_pillar"), "the stone lotus: " + t.blok(plekken.get("stenen_lotus").pos()));
        for (int i = 1; i <= 6; i++) {
            BlockPos v = plekken.get("tegel_voetstuk_" + i).pos();
            helper.assertTrue(is(t.blok(v), "minecraft:chiseled_quartz_block"), "pedestal " + i + ": " + t.blok(v));
        }
        int lampen = 0;
        for (Plekken.Plek p : plekken.values()) {
            if (p.id().startsWith("padlantaarn_")) {
                lampen++;
                helper.assertTrue(is(t.blok(p.pos()), "guhs:geestlantaarn"), "a lantern at " + p.id() + ": " + t.blok(p.pos()));
            }
        }
        helper.assertTrue(lampen >= 21, "path lanterns: " + lampen);
        helper.assertTrue(t.blok(plekken.get("geestenklok").pos()).replace("iron_chain", "chain").startsWith("minecraft:chain"), "the bell's rope");
        // the overlays: there, with blocks, only where the build is air
        ServerLevel level = helper.getLevel();
        for (String naam : Geestenwereld.LAGEN) {
            Plekken.Laag l = Geestenwereld.laag(naam);
            helper.assertTrue(l != null, "overlay " + naam);
            Map<BlockPos, BlockState> blokken = Geestenwereld.laagBlokken(level, naam);
            helper.assertTrue(blokken.size() > 20, "overlay " + naam + " holds blocks: " + blokken.size());
            int n = 0;
            for (BlockPos w : blokken.keySet()) {
                if (n++ % 7 == 0) {
                    BlockPos lokaal = w.subtract(Geestenwereld.OORSPRONG);
                    helper.assertTrue(t.blok(lokaal).equals("minecraft:air") || t.blok(lokaal).isEmpty(), naam + " only fills air: " + lokaal + " " + t.blok(lokaal));
                }
            }
        }
        // the watching masks: residents, each its own face
        List<Bewoners.Bewoner> rij = Bewoners.vasteTabel().getOrDefault(Geestenwereld.NAAM, List.of());
        for (int i = 1; i <= 12; i++) {
            String id = "kijkmasker_" + i;
            int soort = i - 1;
            helper.assertTrue(rij.stream().anyMatch(b -> b.plek().equals(id) && b.soort() == soort && b.stil()), id + ": a resident with face " + soort);
        }
        helper.succeed();
    }

    // --- the puzzle, the lore, the residents ---------------------------------------------------------------------------------

    /** A click turns a tile a quarter; the table of the right turns; the lore of a memorial stone comes from its marker. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2GeestenwereldPuzzel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        try {
            BlockPos tegel = helper.absolutePos(new BlockPos(4, 1, 6));
            BlockState s = level.getBlockState(tegel);
            helper.assertTrue(s.is(E2GeestenwereldFeature.LOTUSPUZZELTEGEL.get()) && s.getValue(GeestBlokken.Lotuspuzzeltegel.DRAAI) == 1, "the tile, turned once");
            s.useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(tegel), Direction.UP, tegel, false));
            helper.assertTrue(level.getBlockState(tegel).getValue(GeestBlokken.Lotuspuzzeltegel.DRAAI) == 2, "a click: a quarter more");
            BlockState basis = E2GeestenwereldFeature.LOTUSPUZZELTEGEL.get().defaultBlockState();
            helper.assertTrue(Lotuspuzzel.opgelost(n -> basis.setValue(GeestBlokken.Lotuspuzzeltegel.DRAAI, Math.max(0, Lotuspuzzel.GOED[n - 1]))),
                    "the right turns make the lotus whole");
            helper.assertTrue(Lotuspuzzel.opgelost(n -> basis.setValue(GeestBlokken.Lotuspuzzeltegel.DRAAI, n == 5 ? 3 : Lotuspuzzel.GOED[n - 1])),
                    "the middle may lie any way");
            helper.assertTrue(!Lotuspuzzel.opgelost(n -> basis.setValue(GeestBlokken.Lotuspuzzeltegel.DRAAI, n == 3 ? 0 : Math.max(0, Lotuspuzzel.GOED[n - 1]))),
                    "one corner wrong: not whole");
            for (String id : List.of("koh", "aang", "leeg", "meditatiecirkel", "hemelvijver")) {
                BlockPos at = Geestenwereld.wereld("lore_" + id);
                helper.assertTrue(at != null && id.equals(GeestBlokken.Gedenksteen.lore(at)), "the stone of " + id);
            }
            helper.assertTrue(GeestBlokken.Gedenksteen.lore(Geestenwereld.OORSPRONG.offset(1, 1, 1)) == null, "no stone there");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** The residents of the build come from the shared rule: Roku-guh and Kyoshi-guh always; a creature only when its type exists. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2GeestenwereldBewoners(GameTestHelper helper) {
        List<Bewoners.Bewoner> rij = Bewoners.vasteTabel().getOrDefault(Geestenwereld.NAAM, List.of());
        helper.assertTrue(rij.stream().anyMatch(b -> b.kind() == GuhNpcEntity.Kind.ROKU_GUH && b.plek().equals("roku"))
                && rij.stream().anyMatch(b -> b.kind() == GuhNpcEntity.Kind.KYOSHI_GUH && b.plek().equals("kyoshi")), "Roku-guh and Kyoshi-guh at the top");
        for (Plekken.Plek p : Plekken.van(Geestenwereld.NAAM).values()) {
            if (p.entity() == null || !p.soort().equals(Plekken.NPC)) {
                continue;
            }
            boolean bestaat = BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.Identifier.parse(p.entity()));
            boolean bewoner = rij.stream().anyMatch(b -> b.plek().equals(p.id()));
            helper.assertTrue(bestaat == bewoner, p.id() + ": a resident exactly when " + p.entity() + " exists (" + bestaat + ")");
        }
        for (Bewoners.Bewoner b : rij) {
            if (b.plek().startsWith("lantaarngeest_")) {
                helper.assertTrue(b.soort() == Integer.parseInt(b.plek().substring("lantaarngeest_".length())) - 1, "every lantern its own colour");
            }
            if (b.plek().equals("koi_la")) {
                helper.assertTrue(b.soort() == 1 && !b.stil(), "La: the black koi, swimming");
            }
        }
        helper.assertTrue(Bewoners.tabel().get(Geestenwereld.NAAM) == null, "not one of the structures (their count stays as it was)");
        helper.succeed();
    }
}
