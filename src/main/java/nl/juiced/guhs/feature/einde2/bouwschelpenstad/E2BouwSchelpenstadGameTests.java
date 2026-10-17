package nl.juiced.guhs.feature.einde2.bouwschelpenstad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Ontdekt;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the slice bouw_schelpenstad: the placement of {@code guhs:schelpenstad} next to Bikinibodem, its markers
 * (build contract 6.7), the road (the seam with Bikinibodem block for block, and that a car can drive it from the seam to
 * the west rim of the loopgraaf and from the east rim into town), the path through the loopgraaf, the chests, the eight
 * decorative blocks, protection and the advancement of the end of the road. The build is read from its tile templates
 * (tools/features/einde2_bouw_schelpenstad_bouw.py): the headless test server has no Guheinde to put it in.
 */
public class E2BouwSchelpenstadGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_schelpenstad";
    private static final String ID = Einde2Ids.SCHELPENSTAD;
    private static final String WINKEL = "einde2_bouw_schelpenstad_test_winkel";
    private static final String P = "guhs:einde2_bouw_schelpenstad_";
    // the numbers of the build (the generator's self-check reads these lines)
    private static final int GROOTTE_X = 256, GROOTTE_Y = 114, GROOTTE_Z = 96;
    private static final int WEST_EIND = 89, OOST_BEGIN = 129, OOST_EIND = 175;
    private static final int TEST_X = 180, TEST_Y = 13, TEST_Z = 13;
    // the road at the seam (build contract 6.11)
    private static final int NAAD_Y = 15, NAAD_Z0 = 44, NAAD_Z1 = 52, NAAD_LANG = 8, VRIJ = 6;
    private static final String DEK = "guhs:kruimelrots_stenen", STREEP = "guhs:gebarsten_korst", STOEP = "guhs:kruimelrots_stenen_muur",
            GROND = "guhs:kaaskorst", PLAAT = "guhs:kruimelrots_stenen_plaat";

    /** The whole build: block name per template cell, and the block entity data of the cells that have any. */
    private record Bouw(Map<BlockPos, String> blok, Map<BlockPos, CompoundTag> nbt) {
        String op(int x, int y, int z) {
            return blok.get(new BlockPos(x, y, z));
        }

        boolean vrij(int x, int y, int z) {
            return !blok.containsKey(new BlockPos(x, y, z));
        }

        /** The y of the highest block of this column at or below {@code van} (-1: none). */
        int top(int x, int z, int van) {
            for (int y = van; y >= 0; y--) {
                if (!vrij(x, y, z)) {
                    return y;
                }
            }
            return -1;
        }
    }

    private static Bouw gelezen;

    private static Bouw bouw(GameTestHelper helper) {
        if (gelezen != null) {
            return gelezen;
        }
        Map<BlockPos, String> blok = new HashMap<>();
        Map<BlockPos, CompoundTag> nbt = new HashMap<>();
        for (int i = 0; i < GROOTTE_X / 32; i++) {
            for (int j = 0; j < GROOTTE_Z / 32; j++) {
                StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id(ID + "/stuk_" + i + "_" + j)).orElse(null);
                helper.assertTrue(t != null, "tile " + i + "," + j + " exists");
                helper.assertTrue(t.getSize().getX() == 32 && t.getSize().getY() == GROOTTE_Y && t.getSize().getZ() == 32, "tile " + i + "," + j + ": " + t.getSize());
                CompoundTag tag = t.save(new CompoundTag());
                ListTag palette = tag.getListOrEmpty("palette");
                for (Tag raw : tag.getListOrEmpty("blocks")) {
                    CompoundTag b = (CompoundTag) raw;
                    int[] pos = b.getListOrEmpty("pos").stream().mapToInt(x -> ((NumericTag) x).intValue()).toArray();
                    BlockPos at = new BlockPos(pos[0] + i * 32, pos[1], pos[2] + j * 32);
                    blok.put(at, palette.getCompoundOrEmpty(b.getIntOr("state", 0)).getStringOr("Name", ""));
                    b.getCompound("nbt").ifPresent(data -> nbt.put(at, data));
                }
            }
        }
        gelezen = new Bouw(blok, nbt);
        return gelezen;
    }

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Where the build lands: next to the one Bikinibodem, in the deep band, once, and not in the compass. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwSchelpenstadPlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, ID);
        LeegteBouwStructure schelp = Plekken.leegteBouw(helper.getLevel().registryAccess(), ID);
        helper.assertTrue(schelp != null && schelp.size().getX() == GROOTTE_X && schelp.size().getY() == GROOTTE_Y && schelp.size().getZ() == GROOTTE_Z,
                "256 x 114 x 96");
        helper.assertTrue(schelp.y() == Einde2Ids.LAAG_MIN && schelp.anchor().equals(new BlockPos(120, 0, 40)) && schelp.ruim() && schelp.rand() == 0,
                "y 4, anchor [120, 0, 40], its tiles empty their box, no rim");
        helper.assertTrue(schelp.tilesX() == 8 && schelp.tilesZ() == 3, "8 x 3 tiles");
        var bij = schelp.bij().orElse(null);
        helper.assertTrue(bij != null && bij.structuur().equals(Guhs.id(Einde2Ids.BIKINIBODEM)) && bij.dx() == 224 && bij.dz() == 64,
                "it stands 224 east and 64 south of Bikinibodem's origin");
        for (long seed : Einde2Test.SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            ChunkPos b = Einde2Test.gegarandeerd(w, Einde2Ids.BIKINIBODEM), s = Einde2Test.gegarandeerd(w, ID);
            LeegteBouwStructure bikini = Plekken.leegteBouw(helper.getLevel().registryAccess(), Einde2Ids.BIKINIBODEM);
            helper.assertTrue(b != null && s != null && schelp.oorsprong(s).equals(bikini.oorsprong(b).offset(224, 0, 64)),
                    "seed " + seed + ": the copy stands exactly at the offset");
            BoundingBox doos = schelp.doos(s);
            helper.assertTrue(doos.minY() == 4 && doos.maxY() == 4 + GROOTTE_Y - 1 && doos.maxY() <= Einde2Ids.LAAG_MAX, "seed " + seed + ": in the deep band: " + doos);
            // the same world y on both sides of the seam: the road is at template y 15 in both builds
            helper.assertTrue(schelp.oorsprong(s).getY() == bikini.oorsprong(b).getY(), "both builds have y 4");
        }
        helper.assertTrue(!Einde2Ontdekt.inKompas(ID), "Schelpenstad is not in the Superkompas (the road leads there)");
        helper.assertTrue(Einde2Ids.SPONS_STRUCTUREN.contains(ID), "it is one of the two places of the Sponsguh story (protected, discovered)");
        helper.succeed();
    }

    private static final Map<String, String> PLEKKEN = Map.ofEntries(
            Map.entry("aankomst", Plekken.ZONE), Map.entry("aankomst_speler", Plekken.PROP), Map.entry("uitzicht", Plekken.PROP),
            Map.entry("weg_naad", Plekken.PROP), Map.entry("weg_west", Plekken.PAD), Map.entry("weg_oost", Plekken.PAD),
            Map.entry("ruige_kroeg", Plekken.ZONE), Map.entry("kroeg_parkeer", Plekken.PROP), Map.entry("kroeg_podium", Plekken.ZONE),
            Map.entry("bellenblaas_plek", Plekken.PROP), Map.entry("barman", Plekken.NPC), Map.entry("uitsmijter", Plekken.NPC),
            Map.entry("slapende_mika", Plekken.NPC), Map.entry("sleutel", Plekken.PROP), Map.entry("kroeg_sluip", Plekken.PAD),
            Map.entry("loopgraaf", Plekken.ZONE), Map.entry("loopgraaf_rand_west", Plekken.PROP), Map.entry("loopgraaf_rand_oost", Plekken.PROP),
            Map.entry("pad_omlaag", Plekken.PAD), Map.entry("pad_omhoog", Plekken.PAD), Map.entry("loopgraaf_bodem", Plekken.ZONE),
            Map.entry("mindy", Plekken.NPC), Map.entry("snor_plek", Plekken.PROP), Map.entry("dennis", Plekken.NPC), Map.entry("dennis_route", Plekken.PAD),
            Map.entry("schelpenstad", Plekken.ZONE), Map.entry("strand", Plekken.ZONE), Map.entry("souvenirwinkel", Plekken.ZONE),
            Map.entry("cycloop", Plekken.NPC), Map.entry("droogtafel", Plekken.PROP), Map.entry("droogtafel_sponsguh", Plekken.PROP),
            Map.entry("droogtafel_patguh", Plekken.PROP), Map.entry("droogtafel_speler", Plekken.PROP), Map.entry("drooglamp", Plekken.PROP),
            Map.entry("stopcontact", Plekken.PROP), Map.entry("sproeier", Plekken.PROP), Map.entry("kroon", Plekken.PROP),
            Map.entry("kust", Plekken.ZONE), Map.entry("hasselguh", Plekken.NPC), Map.entry("hasselguh_opstap", Plekken.PROP),
            Map.entry("mindy_strand", Plekken.NPC), Map.entry("knipoog_appa_vlucht", Plekken.PAD), Map.entry("knipoog_appa_sponsguh", Plekken.NPC),
            Map.entry("knipoog_appa_patguh", Plekken.NPC), Map.entry("knipoog_appa_camera", Plekken.PAD));
    /** The numbered groups: name, how many at least, the kind. */
    private static final List<Object[]> GROEPEN = List.of(new Object[]{"rustpunt", 3, Plekken.PROP}, new Object[]{"stoere_mika", 6, Plekken.NPC},
            new Object[]{"monster", 6, Plekken.NPC}, new Object[]{"souvenirplank", 6, Plekken.PROP}, new Object[]{"bewoner", 4, Plekken.NPC});
    /** Who stands where: marker -> the entity type of the character (a later phase puts it there). */
    private static final Map<String, String> WIE = Map.of("cycloop", "guhs:cycloop_mika", "hasselguh", "guhs:david_hasselguh", "mindy", "guhs:mindy_guh",
            "mindy_strand", "guhs:mindy_guh", "dennis", "guhs:dennis_mika", "knipoog_appa_sponsguh", "guhs:sponsguh", "knipoog_appa_patguh", "guhs:patguh",
            "monster_1", "guhs:loopgraafmonster", "barman", "guhs:mika", "bewoner_1", "guhs:bikinibodem_guh");
    /** The markers of the Sponsguh story slices' build hooks (CONTRACT_E2_C 4.1: d1s_, d1t_, d1r_, d2h_, d2w_, d2f_, zij_). */
    private static final java.util.regex.Pattern HAAK = java.util.regex.Pattern.compile("(d1s|d1t|d1r|d2h|d2w|d2f|zij)_");

    /** Spots that are apart on purpose: the monsters high on the walls, David in the void, things on the table and the ceiling. */
    private static final Set<String> LOS = Set.of("monster_1", "monster_2", "monster_3", "monster_4", "hasselguh", "droogtafel_sponsguh",
            "droogtafel_patguh", "drooglamp", "sproeier");

    /** The sidecar: every marker of build contract 6.7, of the right kind, where a character can stand. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwSchelpenstadPlekken(GameTestHelper helper) {
        Bouw b = bouw(helper);
        Plekken.Blad blad = Plekken.blad(ID);
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().equals(new BlockPos(GROOTTE_X, GROOTTE_Y, GROOTTE_Z))
                && blad.anker().equals(new BlockPos(120, 0, 40)) && blad.lagen().isEmpty(), "the sidecar: " + blad.grootte() + " " + blad.anker());
        for (Map.Entry<String, String> e : PLEKKEN.entrySet()) {
            Plekken.Plek p = blad.plekken().get(e.getKey());
            helper.assertTrue(p != null && p.soort().equals(e.getValue()), "marker " + e.getKey() + " is a " + e.getValue() + ": " + p);
        }
        int geteld = PLEKKEN.size();
        for (Object[] g : GROEPEN) {
            int n = 0;
            while (blad.plekken().containsKey(g[0] + "_" + (n + 1))) {
                n++;
                helper.assertTrue(blad.plekken().get(g[0] + "_" + n).soort().equals(g[2]), g[0] + "_" + n + " is a " + g[2]);
            }
            helper.assertTrue(n >= (int) g[1], g[0] + "_*: " + n + " of at least " + g[1]);
            geteld += n;
        }
        // (merge_sp1: the Sponsguh story slices add their own markers through the build hook, each id with its slice's short
        // name, CONTRACT_E2_C 4.1; those are theirs, not this list's)
        long eigen = blad.plekken().keySet().stream().filter(k -> !HAAK.matcher(k).lookingAt()).count();
        helper.assertTrue(eigen == geteld, "no marker beyond the list: " + eigen + " vs " + geteld);
        for (Map.Entry<String, String> e : WIE.entrySet()) {
            helper.assertTrue(e.getValue().equals(blad.plekken().get(e.getKey()).entity()), e.getKey() + " is " + e.getValue());
        }
        for (Plekken.Plek p : blad.plekken().values()) {
            helper.assertTrue(p.kind() == null, p.id() + ": phase A reserves no GuhNpcEntity.Kind here (the cast are entity types)");
            if (p.soort().equals(Plekken.NPC) && !LOS.contains(p.id())) {
                BlockPos at = p.pos();
                helper.assertTrue(b.vrij(at.getX(), at.getY(), at.getZ()) && b.vrij(at.getX(), at.getY() + 1, at.getZ())
                        && !b.vrij(at.getX(), at.getY() - 1, at.getZ()), p.id() + ": two free blocks on a floor at " + at);
            }
        }
        // the big ones have their room: the car, David, the crown, a monster (2.1 high)
        BlockPos park = blad.plekken().get("kroeg_parkeer").pos(), david = blad.plekken().get("hasselguh").pos(), kroon = blad.plekken().get("kroon").pos();
        helper.assertTrue(leeg(b, park.offset(-2, 0, -3), park.offset(2, 3, 3)), "5 x 4 x 7 free for the Knabbelburgermobiel");
        helper.assertTrue(leeg(b, david.offset(-6, 0, -6), david.offset(6, 5, 6)), "13 x 6 x 13 free for David Hasselguh");
        helper.assertTrue(leeg(b, kroon, kroon.above()) && (P + "kroonkussen").equals(b.op(kroon.getX(), kroon.getY() - 1, kroon.getZ())),
                "the crown display: the cushion with 1 x 2 x 1 free on it");
        for (int i = 1; i <= 6; i++) {
            BlockPos m = blad.plekken().get("monster_" + i).pos();
            helper.assertTrue(leeg(b, m, m.above(2)) && !b.vrij(m.getX(), m.getY() - 1, m.getZ()), "monster_" + i + ": three free on its perch");
        }
        // the props that are real blocks
        helper.assertTrue((P + "drooglamp").equals(op(b, blad, "drooglamp")) && (P + "stopcontact").equals(op(b, blad, "stopcontact"))
                && (P + "sproeier").equals(op(b, blad, "sproeier")) && "guhs:bellenblaas".equals(op(b, blad, "bellenblaas_plek"))
                && "guhs:guh_tafel".equals(op(b, blad, "sleutel")), "the lamp, the socket, the sprinkler, the soap and the key's table are built");
        BlockPos lamp = blad.plekken().get("drooglamp").pos(), tafel = blad.plekken().get("droogtafel").pos();
        helper.assertTrue(lamp.getX() == tafel.getX() && lamp.getZ() == tafel.getZ() && lamp.getY() - tafel.getY() >= 3, "the lamp hangs over the table");
        for (int i = 1; i <= 6; i++) {
            BlockPos s = blad.plekken().get("souvenirplank_" + i).pos();
            helper.assertTrue((P + "souvenir").equals(b.op(s.getX(), s.getY() + 1, s.getZ())), "souvenirplank_" + i + " holds dried souvenirs");
        }
        // Appa's line over the loopgraaf: at least 12 above the rims, 9 wide free
        List<BlockPos> vlucht = blad.plekken().get("knipoog_appa_vlucht").punten();
        int rand = blad.plekken().get("loopgraaf_rand_west").pos().getY();
        helper.assertTrue(vlucht.size() == 2 && vlucht.get(0).getY() >= rand + 12 && vlucht.get(0).getY() == vlucht.get(1).getY(), "the flight line: " + vlucht);
        helper.assertTrue(leeg(b, new BlockPos(vlucht.get(0).getX(), vlucht.get(0).getY() - 2, Math.min(vlucht.get(0).getZ(), vlucht.get(1).getZ()) - 4),
                new BlockPos(vlucht.get(1).getX(), vlucht.get(0).getY() + 4, Math.max(vlucht.get(0).getZ(), vlucht.get(1).getZ()) + 4)), "9 wide free along it");
        helper.succeed();
    }

    private static String op(Bouw b, Plekken.Blad blad, String plek) {
        BlockPos p = blad.plekken().get(plek).pos();
        return b.op(p.getX(), p.getY(), p.getZ());
    }

    private static boolean leeg(Bouw b, BlockPos van, BlockPos tot) {
        for (BlockPos p : BlockPos.betweenClosed(van, tot)) {
            if (b.blok.containsKey(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The road. At the seam: block for block the profile both builds agreed on (build contract 6.11). And all of it: from
     * the seam to the west rim of the loopgraaf and from its east rim into town a deck at least 7 wide at one height, never
     * a step above 1 (half a block first), 6 free above it.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwSchelpenstadWeg(GameTestHelper helper) {
        Bouw b = bouw(helper);
        int midden = (NAAD_Z0 + NAAD_Z1) / 2;
        for (int x = 0; x < NAAD_LANG; x++) {
            for (int z = NAAD_Z0; z <= NAAD_Z1; z++) {
                boolean stoep = z == NAAD_Z0 || z == NAAD_Z1;
                helper.assertTrue((z == midden ? STREEP : DEK).equals(b.op(x, NAAD_Y, z)), "seam " + x + "," + z + ": the deck is " + b.op(x, NAAD_Y, z));
                helper.assertTrue(GROND.equals(b.op(x, NAAD_Y - 1, z)) && GROND.equals(b.op(x, NAAD_Y - 2, z)), "seam " + x + "," + z + ": two of kaaskorst under it");
                for (int d = 1; d <= VRIJ; d++) {
                    if (stoep && d == 1) {
                        helper.assertTrue(STOEP.equals(b.op(x, NAAD_Y + 1, z)), "seam " + x + "," + z + ": the kerb");
                    } else {
                        helper.assertTrue(b.vrij(x, NAAD_Y + d, z), "seam " + x + "," + z + ": " + b.op(x, NAAD_Y + d, z) + " above the road");
                    }
                }
            }
        }
        Plekken.Blad blad = Plekken.blad(ID);
        BlockPos naad = blad.plekken().get("weg_naad").pos();
        helper.assertTrue(naad.equals(new BlockPos(0, NAAD_Y + 1, midden)), "weg_naad is the middle of the road at x 0: " + naad);
        List<String> fouten = new ArrayList<>();
        rijd(b, blad.plekken().get("weg_west").punten(), 0, WEST_EIND, fouten);
        rijd(b, blad.plekken().get("weg_oost").punten(), OOST_BEGIN, OOST_EIND, fouten);
        helper.assertTrue(fouten.isEmpty(), "the road is drivable: " + fouten);
        // it ends at the rims, and between them there is no bridge: only the path down and the path up
        BlockPos west = blad.plekken().get("loopgraaf_rand_west").pos(), oost = blad.plekken().get("loopgraaf_rand_oost").pos();
        helper.assertTrue(west.getX() == WEST_EIND && oost.getX() == OOST_BEGIN && west.getY() == oost.getY(), "the rims: " + west + " " + oost);
        for (int x = WEST_EIND + 10; x <= OOST_BEGIN - 10; x++) {
            helper.assertTrue(b.top(x, (west.getZ() + oost.getZ()) / 2, west.getY() + 6) < west.getY() - 8, "no bridge over the loopgraaf at x " + x);
        }
        for (String pad : List.of("pad_omlaag", "pad_omhoog")) {
            List<BlockPos> punten = blad.plekken().get(pad).punten();
            int laagste = Integer.MAX_VALUE, hoogste = 0;
            for (int i = 0; i < punten.size(); i++) {
                BlockPos p = punten.get(i);
                helper.assertTrue(b.vrij(p.getX(), p.getY(), p.getZ()) || "minecraft:soul_lantern".equals(b.op(p.getX(), p.getY(), p.getZ())),
                        pad + " " + p + ": free");
                helper.assertTrue(b.vrij(p.getX(), p.getY() + 1, p.getZ()) && !b.vrij(p.getX(), p.getY() - 1, p.getZ()), pad + " " + p + ": head room and a floor");
                if (i > 0) {
                    BlockPos q = punten.get(i - 1);
                    int ver = Math.max(Math.abs(p.getX() - q.getX()), Math.abs(p.getZ() - q.getZ()));
                    helper.assertTrue(Math.abs(p.getY() - q.getY()) <= ver, pad + ": no step above 1 between " + q + " and " + p);
                }
                laagste = Math.min(laagste, p.getY());
                hoogste = Math.max(hoogste, p.getY());
            }
            helper.assertTrue(hoogste == west.getY() && hoogste - laagste >= 16, pad + " goes from the rim (" + hoogste + ") down to the floor (" + laagste + ")");
        }
        helper.succeed();
    }

    /** Drives along a pad of the road from x0 to x1: at every x a deck of 7 at one height near the stripe, 6 free above it. */
    private static void rijd(Bouw b, List<BlockPos> punten, int x0, int x1, List<String> fouten) {
        if (punten.get(0).getX() != x0 || punten.get(punten.size() - 1).getX() != x1) {
            fouten.add("the pad runs from x " + punten.get(0).getX() + " to " + punten.get(punten.size() - 1).getX() + ", not " + x0 + ".." + x1);
            return;
        }
        int vorige = -1, k = 0;
        for (int x = x0; x <= x1 && fouten.size() < 12; x++) {
            while (k + 1 < punten.size() - 1 && punten.get(k + 1).getX() <= x) {
                k++;
            }
            BlockPos van = punten.get(k), tot = punten.get(k + 1);
            double t = tot.getX() == van.getX() ? 0 : (x - van.getX()) / (double) (tot.getX() - van.getX());
            int zc = (int) Math.round(van.getZ() + (tot.getZ() - van.getZ()) * t);
            int hoog = Math.max(van.getY(), tot.getY()) + 1;
            int beste = 0, dek = -1;
            for (int z = zc - 6; z <= zc + 6; z++) {            // the widest run of deck at one height around the stripe
                int y = b.top(x, z, hoog), lang = 0;
                while (y >= 0 && isDek(b.op(x, y, z + lang)) && b.top(x, z + lang, hoog) == y && leeg(b, new BlockPos(x, y + 1, z + lang), new BlockPos(x, y + VRIJ, z + lang))) {
                    lang++;
                }
                if (lang > beste) {
                    beste = lang;
                    dek = y;
                }
            }
            if (beste < 7) {
                fouten.add("x " + x + ": the deck is only " + beste + " wide");
            } else if (vorige >= 0 && Math.abs(dek - vorige) > 1) {
                fouten.add("x " + x + ": a step of " + Math.abs(dek - vorige));
            }
            vorige = dek;
        }
    }

    private static boolean isDek(String blok) {
        return DEK.equals(blok) || STREEP.equals(blok) || PLAAT.equals(blok);
    }

    /** Every chest of the build has a loot table that exists; no entity is in the templates (the cast comes later). */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwSchelpenstadKisten(GameTestHelper helper) {
        Bouw b = bouw(helper);
        int kisten = 0;
        for (Map.Entry<BlockPos, CompoundTag> e : b.nbt.entrySet()) {
            String tabel = e.getValue().getStringOr("LootTable", "");
            if (tabel.isEmpty()) {
                continue;
            }
            kisten++;
            helper.assertTrue(tabel.startsWith("guhs:chests/einde2_bouw_schelpenstad_"), "chest " + e.getKey() + ": " + tabel);
            LootTable table = helper.getLevel().getServer().reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id(tabel.substring("guhs:".length()))));
            helper.assertTrue(table != LootTable.EMPTY, "loot table " + tabel + " exists");
        }
        helper.assertTrue(kisten == 3, "three modest chests: " + kisten);
        for (int i = 0; i < GROOTTE_X / 32; i++) {
            for (int j = 0; j < GROOTTE_Z / 32; j++) {
                CompoundTag tag = helper.getLevel().getStructureManager().get(Guhs.id(ID + "/stuk_" + i + "_" + j)).orElseThrow().save(new CompoundTag());
                helper.assertTrue(tag.getListOrEmpty("entities").isEmpty(), "tile " + i + "," + j + " holds no entities");
            }
        }
        // every block of the templates is a block the game knows (a stand-in of another slice counts)
        for (String naam : Set.copyOf(b.blok.values())) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.Identifier.parse(naam)), "block " + naam + " exists");
        }
        helper.succeed();
    }

    /**
     * The drying corner of the souvenir shop, placed for real: the table under the lamp, the socket, a sprinkler; our
     * blocks look and shine as they should; and inside a protected box nobody breaks them (as in the real place, where the
     * skeleton protects the whole build).
     */
    @GuhTest(template = WINKEL, batch = BATCH, timeoutTicks = 200)
    public static void einde2BouwSchelpenstadWinkel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Plekken.Blad blad = Plekken.blad(ID);
        BlockPos lamp = lokaal(blad, "drooglamp"), tafel = lokaal(blad, "droogtafel"), contact = lokaal(blad, "stopcontact"), sproeier = lokaal(blad, "sproeier");
        BlockState lampState = helper.getBlockState(lamp);
        helper.assertTrue(lampState.is(E2BouwSchelpenstadFeature.DROOGLAMP.get()) && lampState.getLightEmission(level, helper.absolutePos(lamp)) == 15,
                "the drying lamp hangs there and is on: " + lampState);
        helper.assertTrue(helper.getBlockState(tafel).is(net.minecraft.world.level.block.Blocks.SMOOTH_STONE_SLAB), "the table under it: " + helper.getBlockState(tafel));
        BlockState contactState = helper.getBlockState(contact);
        helper.assertTrue(contactState.is(E2BouwSchelpenstadFeature.STOPCONTACT.get())
                && contactState.getValue(SchelpenstadBlokken.Gericht.FACING) == Direction.SOUTH, "the socket on the north wall, facing the room: " + contactState);
        helper.assertTrue(contactState.getCollisionShape(level, helper.absolutePos(contact)).isEmpty()
                && contactState.getShape(level, helper.absolutePos(contact)).bounds().maxZ <= 0.07, "it is a plate on the wall behind it");
        helper.assertTrue(helper.getBlockState(sproeier).is(E2BouwSchelpenstadFeature.SPROEIER.get()), "the sprinkler under the ceiling");
        helper.assertTrue(!helper.getBlockState(lokaal(blad, "droogtafel_sponsguh")).isSolid() && !helper.getBlockState(lokaal(blad, "droogtafel_speler")).isSolid(),
                "room on the table and in front of it");
        // a souvenir that is put down takes its look from where it stands
        helper.assertTrue(SchelpenstadBlokken.Souvenir.soortOp(new BlockPos(0, 0, 0)) != SchelpenstadBlokken.Souvenir.soortOp(new BlockPos(1, 0, 0))
                && SchelpenstadBlokken.Souvenir.soortOp(new BlockPos(-3, 7, -9)) >= 0, "souvenirs next to each other differ");
        helper.assertTrue(E2BouwSchelpenstadFeature.BLOKKEN.size() == 8 && E2BouwSchelpenstadFeature.NEON.get().defaultBlockState().getLightEmission() == 10,
                "eight decorative blocks; the neon glows");
        // protection, as the skeleton gives the whole build
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(9, 7, 7)));
        Einde2Bescherming.TEST_AREAS.add(box);
        ServerPlayer player = speler(helper);
        try {
            BlockPos at = helper.absolutePos(lamp);
            helper.assertTrue(Einde2Bescherming.beschermd(level, at), "the corner is protected");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, at, lampState, player)).isCanceled(), "nobody takes the drying lamp");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, helper.absolutePos(tafel), helper.getBlockState(tafel), player)).isCanceled(),
                    "nor the table");
            // the end of the road: out on the head of the pier
            BlockPos kop = new BlockPos(100, 18, -40);
            helper.assertTrue(Schelpenstad.opDePier(kop, kop.offset(3, 1, -4)) && !Schelpenstad.opDePier(kop, kop.offset(5, 0, 0))
                    && !Schelpenstad.opDePier(kop, kop.offset(0, -4, 0)), "the head of the pier: near, not somewhere on the beach");
            helper.assertTrue(!GidsFeature.heeft(player, Schelpenstad.ZEE), "the advancement of the end of the road is not there yet");
            Schelpenstad.bereikt(player);
            helper.assertTrue(GidsFeature.heeft(player, Schelpenstad.ZEE), "and now it is");
        } finally {
            Einde2Bescherming.TEST_AREAS.remove(box);
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    /** A marker of the build in the test room (the room is a cut of the build: its y 0 is helper y 1). */
    private static BlockPos lokaal(Plekken.Blad blad, String plek) {
        BlockPos p = blad.plekken().get(plek).pos();
        return new BlockPos(p.getX() - TEST_X, p.getY() - TEST_Y + 1, p.getZ() - TEST_Z);
    }
}
