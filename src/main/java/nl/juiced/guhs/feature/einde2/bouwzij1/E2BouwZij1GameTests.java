package nl.juiced.guhs.feature.einde2.bouwzij1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * Game tests of einde2 bouw_zij1: the three side places really start in the Guheinde's generator (one guaranteed copy
 * each, in its own biome, inside the high band, every tile there), their sidecars hold every marker the build contract
 * names (kind, count and character), the chutes of Omashguh lead from the platform to the post square, the five fire
 * doors hold their locks, and the postgoot carries a rider along a little test chute with a drop and a corner
 * ({@code einde2_bouw_zij1_test_goot}). The walk-through checks of the builds themselves (reachable, lit, nothing
 * floating, every chute ridden block by block) run in the generator: tools/features/einde2_bouw_zij1_*.py.
 */
public class E2BouwZij1GameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_zij1";

    private record Wil(String id, String soort, int aantal, String kind) {
    }

    private static Wil een(String id, String soort) {
        return new Wil(id, soort, 0, null);
    }

    private static Wil npc(String id, String kind) {
        return new Wil(id, Plekken.NPC, 0, kind);
    }

    private static Wil reeks(String id, String soort, int aantal) {
        return new Wil(id, soort, aantal, null);
    }

    private static final List<Wil> STANDAARD = List.of(een("aankomst", Plekken.ZONE), een("aankomst_speler", Plekken.PROP), een("uitzicht", Plekken.PROP));

    /** The markers of build contract 6.7, per place. */
    private static final Map<String, List<Wil>> PLEKKEN = Map.of(
            E2BouwZij1Feature.OMASHGUH, List.of(een("troonzaal", Plekken.ZONE), npc("bumi", "BUMI_GUH"), npc("flopsie", "FLOPSIE"),
                    reeks("proef", Plekken.ZONE, 3), een("proef_1_start", Plekken.PROP), een("proef_2_start", Plekken.PROP), een("proef_3_start", Plekken.PROP),
                    een("postkantoor", Plekken.ZONE), npc("postguh", "POSTGUH"), een("glijbaan_start", Plekken.PROP), een("glijbaan_finish", Plekken.PROP),
                    reeks("glijbaan_route", Plekken.PAD, 3), een("kristalsnoep", Plekken.PROP), new Wil("bewoner", Plekken.NPC, 5, "OMASHGUH_BURGER"),
                    reeks("das_molguh", Plekken.WEZEN, 2)),
            E2BouwZij1Feature.LUCHTTEMPEL, List.of(npc("luchtnon", "LUCHTNON_GUH"), reeks("windgang", Plekken.ZONE, 5), reeks("windfluit", Plekken.PROP, 5),
                    een("bizonstal", Plekken.ZONE), reeks("bizon", Plekken.PROP, 5), reeks("kudde", Plekken.PROP, 5), een("fontein", Plekken.PROP),
                    reeks("lemur", Plekken.WEZEN, 3)),
            E2BouwZij1Feature.VUURTEMPEL, List.of(new Wil("vuurwijze", Plekken.NPC, 5, "VUURWIJZE_MIKA"), npc("shyu", "VUURWIJZE_SHYU"),
                    reeks("vuurdeur", Plekken.PROP, 5), een("heiligdom", Plekken.ZONE), een("roku_beeld", Plekken.PROP), npc("roku", "ROKU_GUH"),
                    een("zonnestraal", Plekken.PROP)));

    private static void controleer(List<String> fouten, String structuur, Plekken.Plek plek, String id, Wil wil) {
        if (plek == null) {
            fouten.add(structuur + "/" + id + " is missing");
        } else if (!plek.soort().equals(wil.soort())) {
            fouten.add(structuur + "/" + id + " is a " + plek.soort() + ", not a " + wil.soort());
        } else if (wil.kind() != null && !wil.kind().equals(plek.kind())) {
            fouten.add(structuur + "/" + id + " has kind " + plek.kind() + ", not " + wil.kind());
        }
    }

    /** Every marker of the contract is in the sidecars, with its kind; the wezen spots name the right creatures. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwZij1Plekken(GameTestHelper helper) {
        List<String> fouten = new ArrayList<>();
        for (Map.Entry<String, List<Wil>> e : PLEKKEN.entrySet()) {
            String structuur = e.getKey();
            Plekken.Blad blad = Plekken.blad(structuur);
            if (!"leegte_bouw".equals(blad.soort()) || blad.grootte().getX() < 100) {
                fouten.add(structuur + ": no real sidecar (still the stand-in platform?): " + blad.grootte());
                continue;
            }
            List<Wil> alle = new ArrayList<>(STANDAARD);
            alle.addAll(e.getValue());
            for (Wil wil : alle) {
                if (wil.aantal() == 0) {
                    controleer(fouten, structuur, blad.plekken().get(wil.id()), wil.id(), wil);
                    continue;
                }
                for (int i = 1; i <= wil.aantal(); i++) {
                    controleer(fouten, structuur, blad.plekken().get(wil.id() + "_" + i), wil.id() + "_" + i, wil);
                }
            }
        }
        helper.assertTrue(fouten.isEmpty(), "markers: " + fouten);
        helper.assertTrue("guhs:das_molguh".equals(Plekken.plek(E2BouwZij1Feature.OMASHGUH, "das_molguh_1").entity())
                && "guhs:vleermuis_lemurguh".equals(Plekken.plek(E2BouwZij1Feature.LUCHTTEMPEL, "lemur_1").entity()), "the creatures of the wezen spots");
        // the residents who give a side quest ask for the element of their place, and all three are in the Superkompas
        helper.assertTrue(Einde2Ids.ELEMENT_VAN.get(E2BouwZij1Feature.OMASHGUH) == Einde2Ids.Element.AARDE
                && Einde2Ids.ELEMENT_VAN.get(E2BouwZij1Feature.LUCHTTEMPEL) == Einde2Ids.Element.LUCHT
                && Einde2Ids.ELEMENT_VAN.get(E2BouwZij1Feature.VUURTEMPEL) == Einde2Ids.Element.VUUR, "the element of each place");
        for (String structuur : PLEKKEN.keySet()) {
            helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.structures().contains(structuur)), structuur + " is in the Superkompas");
        }
        helper.succeed();
    }

    /** Omashguh, the Westelijke Luchttempel and the Vuurtempel start for three seeds: one copy, own biome, high band, all tiles. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 4800)
    public static void einde2BouwZij1Omashguh(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, E2BouwZij1Feature.OMASHGUH);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 4800)
    public static void einde2BouwZij1Luchttempel(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, E2BouwZij1Feature.LUCHTTEMPEL);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 4800)
    public static void einde2BouwZij1Vuurtempel(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, E2BouwZij1Feature.VUURTEMPEL);
        helper.succeed();
    }

    /** Where in the tiles of a build does this block stand? (template coordinates of the whole build) */
    private static List<BlockPos> zoek(GameTestHelper helper, String structuur, Block block) {
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), structuur);
        List<BlockPos> uit = new ArrayList<>();
        if (bouw == null) {
            return uit;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (int i = 0; i < bouw.tilesX(); i++) {
            for (int j = 0; j < bouw.tilesZ(); j++) {
                StructureTemplate t = helper.getLevel().getStructureManager().get(bouw.tile(i, j)).orElse(null);
                if (t == null) {
                    continue;
                }
                BlockPos hoek = new BlockPos(i * 32, 0, j * 32);
                t.filterBlocks(hoek, settings, block).forEach(info -> uit.add(info.pos()));
            }
        }
        return uit;
    }

    /** The three chutes of Omashguh: real chute blocks from the platform down to the post square, never uphill. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwZij1Glijbanen(GameTestHelper helper) {
        String o = E2BouwZij1Feature.OMASHGUH;
        List<BlockPos> goten = zoek(helper, o, E2BouwZij1Feature.POSTGOOT.get());
        helper.assertTrue(goten.size() > 250, "Omashguh has its chute blocks: " + goten.size());
        BlockPos start = Plekken.plek(o, "glijbaan_start").pos(), finish = Plekken.plek(o, "glijbaan_finish").pos();
        helper.assertTrue(start.getY() - finish.getY() >= 20, "the chutes go down a long way: " + start + " -> " + finish);
        for (int i = 1; i <= 3; i++) {
            List<BlockPos> punten = Plekken.plek(o, "glijbaan_route_" + i).punten();
            BlockPos eerste = punten.get(0), laatste = punten.get(punten.size() - 1);
            helper.assertTrue(punten.size() >= 4 && eerste.distManhattan(start) <= 8 && eerste.getY() == start.getY(), "chute " + i + " starts at the platform: " + eerste);
            helper.assertTrue(Math.abs(laatste.getX() - finish.getX()) <= 2 && Math.abs(laatste.getZ() - finish.getZ()) <= 2 && laatste.getY() == finish.getY(),
                    "chute " + i + " ends at the post square: " + laatste);
            helper.assertTrue(goten.contains(eerste.below()), "chute " + i + " begins on a chute block");
            for (int k = 1; k < punten.size(); k++) {
                helper.assertTrue(punten.get(k).getY() <= punten.get(k - 1).getY(), "chute " + i + " never goes up: " + punten.get(k));
            }
        }
        helper.succeed();
    }

    /** The five fire doors: each lock is an unlit lamp in the wall, side by side, and the sanctum lies behind them. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwZij1Vuurdeuren(GameTestHelper helper) {
        String v = E2BouwZij1Feature.VUURTEMPEL;
        List<BlockPos> lampen = zoek(helper, v, Blocks.REDSTONE_LAMP);
        Plekken.Plek heiligdom = Plekken.plek(v, "heiligdom");
        for (int i = 1; i <= 5; i++) {
            BlockPos deur = Plekken.plek(v, "vuurdeur_" + i).pos();
            helper.assertTrue(lampen.contains(deur), "fire door " + i + " has its lock at " + deur);
            helper.assertTrue(deur.getZ() == heiligdom.tot().getZ() + 1 && deur.getX() > heiligdom.pos().getX() && deur.getX() < heiligdom.tot().getX(),
                    "fire door " + i + " stands in the wall of the sanctum");
        }
        helper.assertTrue(lampen.size() == 5, "five locks, no more: " + lampen.size());
        BlockPos roku = Plekken.plek(v, "roku").pos();
        helper.assertTrue(roku.getZ() <= heiligdom.tot().getZ() && roku.getZ() >= heiligdom.pos().getZ(), "Roku-guh appears inside the sanctum");
        helper.succeed();
    }

    /** The stable of the Luchttempel hangs inside the island: five pens in a row, under the grass and above the hall. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwZij1Bizonstal(GameTestHelper helper) {
        String l = E2BouwZij1Feature.LUCHTTEMPEL;
        Plekken.Plek stal = Plekken.plek(l, "bizonstal");
        int grond = Plekken.plek(l, "aankomst_speler").pos().getY(), hal = Plekken.plek(l, "luchtnon").pos().getY();
        for (int i = 1; i <= 5; i++) {
            BlockPos hok = Plekken.plek(l, "bizon_" + i).pos();
            helper.assertTrue(hok.getX() >= stal.pos().getX() && hok.getX() <= stal.tot().getX() && hok.getZ() >= stal.pos().getZ() && hok.getZ() <= stal.tot().getZ()
                    && hok.getY() < grond && hok.getY() > hal, "pen " + i + " lies in the stable, inside the island: " + hok);
            helper.assertTrue(i == 1 || hok.getX() - Plekken.plek(l, "bizon_" + (i - 1)).pos().getX() == 8, "the pens stand side by side");
        }
        // the temple hangs: the hall and every wind flute lie below the island's top
        for (int i = 1; i <= 5; i++) {
            helper.assertTrue(Plekken.plek(l, "windfluit_" + i).pos().getY() < grond - 20, "wind flute " + i + " hangs under the island");
        }
        helper.succeed();
    }

    /** The postgoot: an armour stand set on a little chute slides down a step and round a corner to the end. */
    @GuhTest(template = "einde2_bouw_zij1_test_goot", batch = BATCH, timeoutTicks = 300)
    public static void einde2BouwZij1Postgoot(GameTestHelper helper) {
        BlockState eerste = helper.getBlockState(new BlockPos(2, 4, 1));
        helper.assertTrue(eerste.getBlock() instanceof PostgootBlock, "the test chute starts with a chute block: " + eerste);
        ArmorStand rijder = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 5, 1));
        BlockPos begin = rijder.blockPosition();
        helper.succeedWhen(() -> {
            BlockPos nu = rijder.blockPosition();
            helper.assertTrue(rijder.isAlive() && nu.getY() == begin.getY() - 2 && nu.getX() - begin.getX() >= 6 && nu.getZ() - begin.getZ() >= 5,
                    "the rider slid to the end of the chute: " + begin + " -> " + nu);
            helper.assertTrue(!PostgootBlock.opGoot(rijder), "and stands on the landing, not on the chute");
        });
    }

    /** Sneaking is braking, and the block has its item. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwZij1Remmen(GameTestHelper helper) {
        ArmorStand stil = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        stil.setShiftKeyDown(true);
        helper.assertTrue(!PostgootBlock.glijdt(stil), "a sneaking rider is left alone");
        stil.setShiftKeyDown(false);
        helper.assertTrue(PostgootBlock.glijdt(stil), "and slides again when it stands up");
        stil.discard();
        helper.assertTrue(new ItemStack(E2BouwZij1Feature.POSTGOOT.get()).getItem() == E2BouwZij1Feature.POSTGOOT.get().asItem()
                && !new ItemStack(E2BouwZij1Feature.POSTGOOT.get()).isEmpty(), "the chute block has an item");
        helper.succeed();
    }
}
