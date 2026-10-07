package nl.juiced.guhs.feature.einde2.bouwzij2;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Ontdekt;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the five builds of bouw_zij2 (tools/features/einde2_bouw_zij2.py; the builds check themselves when they are
 * generated: walkable, lit, nothing floating, every marker reachable): what the game makes of them. Each has its guaranteed
 * copy in the high band for three seeds ({@link Einde2Test#controleerLeegteBouw}), every marker of the build contract is in its
 * sidecar with the right kind, the rings of the race are really hoops of ring blocks in the saved tiles, and the block exists.
 */
public class E2BouwZij2GameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_zij2";

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwZij2KyoshiDorp(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, Einde2Ids.KYOSHI_DORP);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwZij2Bibliotheek(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwZij2Leeuwschildpad(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, Einde2Ids.LEEUWSCHILDPAD_EILAND);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwZij2Leegtehaven(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, Einde2Ids.LEEGTEHAVEN);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwZij2Ringenrace(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, Einde2Ids.GUHVLEUGEL_RINGENRACE);
        helper.succeed();
    }

    private static void wil(GameTestHelper helper, String structuur, String soort, String... ids) {
        for (String id : ids) {
            Plekken.Plek plek = Plekken.plek(structuur, id);
            helper.assertTrue(plek != null && plek.soort().equals(soort), structuur + "/" + id + " is a " + soort + ": " + plek);
        }
    }

    private static void wilKind(GameTestHelper helper, String structuur, String id, String kind) {
        Plekken.Plek plek = Plekken.plek(structuur, id);
        helper.assertTrue(plek != null && plek.soort().equals(Plekken.NPC) && kind.equals(plek.kind()), structuur + "/" + id + " is the spot of " + kind + ": " + plek);
    }

    private static boolean binnen(Plekken.Plek zone, BlockPos p) {
        return p.getX() >= zone.pos().getX() && p.getX() <= zone.tot().getX() && p.getY() >= zone.pos().getY() && p.getY() <= zone.tot().getY()
                && p.getZ() >= zone.pos().getZ() && p.getZ() <= zone.tot().getZ();
    }

    /** Every marker the build contract names for these five places (6.7), with its kind, and the numbered groups. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwZij2Plekken(GameTestHelper helper) {
        for (String id : Zij2Plekken.PLEKKEN) {
            wil(helper, id, Plekken.ZONE, "aankomst");
            wil(helper, id, Plekken.PROP, "aankomst_speler", "uitzicht");
            Plekken.Plek aankomst = Plekken.plek(id, "aankomst");
            helper.assertTrue(aankomst.tot().getX() - aankomst.pos().getX() >= 8 && aankomst.tot().getZ() - aankomst.pos().getZ() >= 8, id + ": a landing of 9 x 9");
            helper.assertTrue(Einde2Ontdekt.inKompas(id) && Einde2Ids.ZIJ_STRUCTUREN.contains(id), id + " is a side place of the compass");
        }
        String dorp = Einde2Ids.KYOSHI_DORP;
        wil(helper, dorp, Plekken.PROP, "kyoshi_beeld", "dans_speler", "schminktafel");
        wil(helper, dorp, Plekken.ZONE, "dojo", "dansvloer");
        wilKind(helper, dorp, "krijgster_leider", "KYOSHI_KRIJGSTERGUH");
        wilKind(helper, dorp, "fan_guh", "FANGUH");
        wilKind(helper, dorp, "dorpsoudste", "KYOSHI_DORPSOUDSTE");
        Plekken.Plek vloer = Plekken.plek(dorp, "dansvloer");
        helper.assertTrue(vloer.tot().getX() - vloer.pos().getX() >= 8 && vloer.tot().getZ() - vloer.pos().getZ() >= 8, "the dance floor is at least 9 x 9");
        helper.assertTrue(binnen(Plekken.plek(dorp, "dojo"), vloer.pos()) && binnen(vloer, Plekken.plek(dorp, "dans_speler").pos().above()),
                "the dance floor lies in the dojo and the player stands on it");
        helper.assertTrue(Zij2Plekken.groep(dorp, "krijgster").size() >= 3 && Zij2Plekken.groep(dorp, "bewoner").size() >= 4, "three warriors, four villagers");
        List<Plekken.Plek> otters = Zij2Plekken.groep(dorp, "otter");
        helper.assertTrue(otters.size() >= 2 && otters.stream().allMatch(p -> p.soort().equals(Plekken.WEZEN) && "guhs:otter_pinguinguh".equals(p.entity())),
                "two otter-pinguinguhs: " + otters);

        String bieb = Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH;
        wil(helper, bieb, Plekken.PROP, "ingang_top", "kennis_balie", "planetarium_wiel", "knipoog_mew_plank");
        wil(helper, bieb, Plekken.ZONE, "uilenzaal", "planetarium");
        wil(helper, bieb, Plekken.PAD, "knipoog_mew_zweef", "knipoog_mew_camera");
        wilKind(helper, bieb, "wan_shi_guh", "WAN_SHI_GUH");
        wilKind(helper, bieb, "professor", "PROFESSOR_ZEI_GUH");
        helper.assertTrue(Zij2Plekken.groep(bieb, "vosguh").size() >= 3 && Zij2Plekken.groep(bieb, "zaal").size() >= 5 && Zij2Plekken.guhboekKasten().size() >= 6,
                "three fox-guhs, five halls, six cabinets");
        helper.assertTrue(Zij2Plekken.groep(bieb, "vosguh").stream().allMatch(p -> "VOSGUH".equals(p.kind())), "the kind of the fox-guhs");
        Plekken.Plek uil = Plekken.plek(bieb, "wan_shi_guh"), plank = Plekken.plek(bieb, "knipoog_mew_plank"), zaal = Plekken.plek(bieb, "uilenzaal");
        Plekken.Plek camera = Plekken.plek(bieb, "knipoog_mew_camera");
        helper.assertTrue(plank.pos().getZ() < uil.pos().getZ() && Math.abs(plank.pos().getX() - uil.pos().getX()) <= 3, "the shelf of the wink is behind the owl");
        double afstand = Math.sqrt(camera.punten().get(0).distSqr(uil.pos()));
        helper.assertTrue(camera.punten().size() == 2 && camera.punten().get(0).getZ() > uil.pos().getZ() && afstand >= 6 && afstand <= 14,
                "the camera of the wink looks at the owl from 6 to 14 blocks in front of him: " + camera.punten());
        for (BlockPos p : List.of(uil.pos(), plank.pos(), camera.punten().get(0))) {
            helper.assertTrue(binnen(zaal, p), "the wink plays in the uilenzaal: " + p);
        }
        List<Plekken.Plek> zalen = Zij2Plekken.groep(bieb, "zaal");
        for (int i = 1; i < zalen.size(); i++) {
            helper.assertTrue(zalen.get(i).pos().getY() < zalen.get(i - 1).pos().getY(), "the halls go down: zaal_" + (i + 1));
        }
        helper.assertTrue(zaal.pos().getY() < zalen.get(zalen.size() - 1).pos().getY(), "the uilenzaal is the bottom");
        helper.assertTrue(binnen(Plekken.plek(bieb, "planetarium"), Plekken.plek(bieb, "planetarium_wiel").pos()), "the wheel stands in the planetarium");

        String pad = Einde2Ids.LEEUWSCHILDPAD_EILAND;
        wil(helper, pad, Plekken.PROP, "kop", "zegen_plek", "schild_top");
        wil(helper, pad, Plekken.ZONE, "bos");
        wilKind(helper, pad, "leeuwschildpad", "LEEUWSCHILDPAD_GUH");
        helper.assertTrue(Math.sqrt(Plekken.plek(pad, "leeuwschildpad").pos().distSqr(Plekken.plek(pad, "kop").pos())) <= 8, "the talk spot is in front of the head");
        helper.assertTrue(Plekken.plek(pad, "schild_top").pos().getY() > Plekken.plek(pad, "zegen_plek").pos().getY() + 10, "the top of the shell is high above the palm");

        String haven = Einde2Ids.LEEGTEHAVEN;
        wil(helper, haven, Plekken.ZONE, "steiger_1", "steiger_2", "vetschip_1", "vetschip_2", "markt", "veerpier", "veer_dek", "werf", "kroeg", "kapitein_hoek");
        wil(helper, haven, Plekken.PROP, "prikbord", "helling", "walvis_bewijs");
        for (Map.Entry<String, String> e : Map.of("veerman", "VEERGUH", "scheepsbouwer", "SCHEEPSBOUWERGUH", "kroegbaas", "KROEGBAASGUH", "kapitein", "KAPITEINGUH",
                "mika_kok", "MIKA_KOK").entrySet()) {
            wilKind(helper, haven, e.getKey(), e.getValue());
        }
        List<Plekken.Plek> handel = Zij2Plekken.handelaars();
        Plekken.Plek markt = Plekken.plek(haven, "markt");
        helper.assertTrue(handel.size() >= 4 && handel.stream().allMatch(p -> p.soort().equals(Plekken.WEZEN) && "guhs:ender_mika".equals(p.entity())
                && binnen(markt, p.pos())), "four Ender-Mika traders on the market: " + handel);
        Plekken.Plek dek = Plekken.plek(haven, "veer_dek"), hoek = Plekken.plek(haven, "kapitein_hoek"), kroeg = Plekken.plek(haven, "kroeg");
        helper.assertTrue(Math.min(dek.tot().getX() - dek.pos().getX(), dek.tot().getZ() - dek.pos().getZ()) >= 6
                && Math.max(dek.tot().getX() - dek.pos().getX(), dek.tot().getZ() - dek.pos().getZ()) >= 8, "the ferry deck is at least 7 x 9");
        helper.assertTrue(binnen(kroeg, hoek.pos()) && binnen(kroeg, hoek.tot()) && binnen(hoek, Plekken.plek(haven, "kapitein").pos())
                && binnen(hoek, Plekken.plek(haven, "walvis_bewijs").pos()), "the corner of the captain is in the pub, he and the proof are in it");
        helper.assertTrue(binnen(Plekken.plek(haven, "werf"), Plekken.plek(haven, "helling").pos().above()), "the slip lies on the shipyard");

        String race = Einde2Ids.GUHVLEUGEL_RINGENRACE;
        wil(helper, race, Plekken.ZONE, "start", "finish");
        wil(helper, race, Plekken.PROP, "startlijn", "finishlijn", "scorebord");
        wilKind(helper, race, "racemeester", "RINGENRACEGUH");
        List<Plekken.Plek> ringen = Zij2Plekken.ringen();
        helper.assertTrue(ringen.size() >= 16, "a course of at least sixteen rings: " + ringen.size());
        BlockPos vorige = Plekken.plek(race, "startlijn").pos();
        for (Plekken.Plek ring : ringen) {
            helper.assertTrue(ring.soort().equals(Plekken.RING) && ring.straal() >= 4 && ring.as() != null && ring.as() != Direction.Axis.Y,
                    ring.id() + ": a standing ring, wide enough for an Enderguh");
            double d = Math.sqrt(ring.pos().distSqr(vorige));
            helper.assertTrue(d >= 12 && d <= 40, ring.id() + " is " + (int) d + " blocks from the one before it");
            vorige = ring.pos();
        }
        helper.assertTrue(Math.sqrt(Plekken.plek(race, "finishlijn").pos().distSqr(vorige)) <= 40, "the last ring is in reach of the finish");
        helper.succeed();
    }

    /** The saved tiles of the race: every ring marker is a closed hoop of ring blocks with a free opening. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwZij2Ringen(GameTestHelper helper) {
        String race = Einde2Ids.GUHVLEUGEL_RINGENRACE;
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), race);
        helper.assertTrue(bouw != null, "the race is a guhs:leegte_bouw");
        Set<BlockPos> ringblokken = new HashSet<>(), anders = new HashSet<>();
        StructurePlaceSettings settings = new StructurePlaceSettings();
        int tegels = 0;
        for (int i = 0; i < bouw.tilesX(); i++) {
            for (int j = 0; j < bouw.tilesZ(); j++) {
                StructureTemplate t = helper.getLevel().getStructureManager().get(bouw.tile(i, j)).orElse(null);
                if (t == null) {
                    continue;
                }
                tegels++;
                helper.assertTrue(t.getSize().getX() <= 32 && t.getSize().getZ() <= 32 && t.getSize().getY() == bouw.size().getY(), "tile " + i + "," + j + ": " + t.getSize());
                BlockPos hoek = new BlockPos(i * 32, 0, j * 32);
                t.filterBlocks(hoek, settings, E2BouwZij2Feature.RINGENRACE_RING.get()).forEach(info -> ringblokken.add(info.pos()));
                for (String naam : List.of("kruimelwolk", "dichte_kruimelwolk", "kruimelrots", "kruimelrots_stenen")) {
                    t.filterBlocks(hoek, settings, BuiltInRegistries.BLOCK.getValue(Guhs.id(naam))).forEach(info -> anders.add(info.pos()));
                }
            }
        }
        helper.assertTrue(tegels >= 10 && !anders.isEmpty(), "the tiles of the race are saved: " + tegels);
        for (Plekken.Plek ring : Zij2Plekken.ringen()) {
            int hoepel = 0, dicht = 0;
            for (int u = -ring.straal() - 2; u <= ring.straal() + 2; u++) {
                for (int v = -ring.straal() - 2; v <= ring.straal() + 2; v++) {
                    BlockPos p = ring.as() == Direction.Axis.X ? ring.pos().offset(0, v, u) : ring.pos().offset(u, v, 0);
                    if (ringblokken.contains(p)) {
                        hoepel++;
                        helper.assertTrue(Math.hypot(u, v) > ring.straal(), ring.id() + ": a ring block inside the free opening at " + p);
                    } else if (Math.hypot(u, v) <= ring.straal() && anders.contains(p)) {
                        dicht++;
                    }
                }
            }
            helper.assertTrue(hoepel >= 30 && dicht == 0, ring.id() + ": " + hoepel + " ring blocks, " + dicht + " blocks in its opening");
        }
        helper.succeed();
    }

    /** The ring block exists and glows; every place is the real build with its tiles, its sidecar and its sets. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwZij2Blok(GameTestHelper helper) {
        var ring = E2BouwZij2Feature.RINGENRACE_RING.get();
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id("ringenrace_ring")) && !new ItemStack(ring).isEmpty(), "the ring block has its item");
        helper.assertTrue(ring.defaultBlockState().getLightEmission() == 12 && ring != Blocks.AIR, "the ring block glows");
        var access = helper.getLevel().registryAccess();
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        for (String id : Zij2Plekken.PLEKKEN) {
            LeegteBouwStructure bouw = Plekken.leegteBouw(access, id);
            helper.assertTrue(bouw != null && bouw.rand() == 0, id + " is a guhs:leegte_bouw without a rim");
            helper.assertTrue(bouw.size().getX() > 64 && bouw.size().getZ() > 64, id + " is the real build, not the stand-in platform: " + bouw.size());
            helper.assertTrue(Plekken.blad(id).grootte().equals(bouw.size()), id + ": the sidecar has the size of the build");
            int tegels = 0;
            for (int i = 0; i < bouw.tilesX(); i++) {
                for (int j = 0; j < bouw.tilesZ(); j++) {
                    tegels += helper.getLevel().getStructureManager().get(bouw.tile(i, j)).isPresent() ? 1 : 0;
                }
            }
            helper.assertTrue(tegels >= 6, id + ": " + tegels + " tiles");
            helper.assertTrue(sets.containsKey(Guhs.id(id + "_gegarandeerd")), id + " has its guaranteed set");
        }
        // only the two places with residents have a very rare random copy; the harbour, the turtle and the race exist once
        helper.assertTrue(sets.containsKey(Guhs.id(Einde2Ids.KYOSHI_DORP)) && sets.containsKey(Guhs.id(Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH)), "the rare copies");
        helper.assertTrue(!sets.containsKey(Guhs.id(Einde2Ids.LEEGTEHAVEN)) && !sets.containsKey(Guhs.id(Einde2Ids.LEEUWSCHILDPAD_EILAND))
                && !sets.containsKey(Guhs.id(Einde2Ids.GUHVLEUGEL_RINGENRACE)), "the harbour, the turtle and the race exist once");
        helper.succeed();
    }
}
