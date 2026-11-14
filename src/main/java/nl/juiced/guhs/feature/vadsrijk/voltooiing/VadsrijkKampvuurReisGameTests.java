package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.kern.Gevecht;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.ZoneBoek;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 Kampvuurreizen ({@link KampvuurReizen}). Three test zone pages (zone 1: two campfires, zone 2 and 3: one each) on a
 * stone floor in the test area, each with one extra "the rest of the zone" entry that a flag marks done (= 100 %). Zone 1's campfire
 * category is a "later" one there, so 100 % is possible with an unlit campfire (to see the lit filter). Checks: not 100 % = no
 * travel; 100 % = the list holds only lit campfires of 100 % zones, a zone closed by the gate drops out; a trip lands next to the
 * target campfire (not in it, on the floor) and becomes the checkpoint, then the 3 s anti-spam; refused in combat, in a dungeon, too
 * far from the campfire, without a safe spot; the unlock message once per zone.
 * Run: {@code runGameTestServer -Pgt=VadsrijkKampvuurReis}.
 */
public class VadsrijkKampvuurReisGameTests {
    private static final String BATCH = "vadsrijk_kampvuurreis";

    /** The test world: campfires a1, a2 (zone 1), b1 (zone 2), c1 (zone 3) and the three "rest done" flags. */
    private static final class Wereld {
        final BlockPos a1, a2, b1, c1;
        final AtomicBoolean vol1 = new AtomicBoolean(), vol2 = new AtomicBoolean(), vol3 = new AtomicBoolean();
        final ZoneBoek z1, z2, z3;

        Wereld(GameTestHelper helper) {
            ServerLevel sl = helper.getLevel();
            a1 = helper.absolutePos(new BlockPos(2, 2, 2));
            a2 = a1.east(10);
            b1 = a1.south(10);
            c1 = a1.east(10).south(10);
            for (BlockPos v : new BlockPos[] {a1, a2, b1, c1}) {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        sl.setBlockAndUpdate(v.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                        sl.setBlockAndUpdate(v.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                        sl.setBlockAndUpdate(v.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
                    }
                }
                sl.setBlockAndUpdate(v, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            }
            z1 = pagina(1, List.of(a1, a2), vol1, true);
            z2 = pagina(2, List.of(b1), vol2, false);
            z3 = pagina(3, List.of(c1), vol3, false);
        }

        String id(BlockPos b) {
            return "vuur_" + Voltooiing.sleutel(b);
        }
    }

    private static ZoneBoek pagina(int n, List<BlockPos> vuren, AtomicBoolean vol, boolean vurenLater) {
        String id = KampvuurReizen.TEST_PREFIX + "zone" + n;
        return new ZoneBoek(id, "vadsrijk.voltooiing.zone.zone" + n, "vr_kenner_van_zwamzoet", 0, () -> {
            List<Ding> vl = vuren.stream().map(b -> new Ding("vuur_" + Voltooiing.sleutel(b), "vadsrijk.voltooiing.kampvuur.naam",
                    "vadsrijk.kern.gebied.sporenwoud#" + (vuren.indexOf(b) + 1), "", "", false,
                    p -> Voltooiing.vuurAan(p, b) ? Staat.KLAAR : Staat.NIETS, b)).toList();
            Ding rest = new Ding("rest", "vadsrijk.voltooiing.cat.geheim", "", "", "", false, p -> vol.get() ? Staat.KLAAR : Staat.NIETS, null);
            return List.of(new Categorie("kampvuur", "vadsrijk.voltooiing.cat.kampvuur", "minecraft:campfire", vl, vurenLater),
                    new Categorie("rest", "vadsrijk.voltooiing.cat.geheim", "minecraft:chest", List.of(rest)));
        });
    }

    private static ServerPlayer speler(GameTestHelper helper, Wereld w) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        SpelerData d = Spelers.data(p);
        d.extra.remove(Voltooiing.SLICE);
        d.checkpoint = null;
        Gevecht.zet(p, false);
        KampvuurReizen.vergeet(p.getUUID());
        KampvuurReizen.TEST = true;
        KampvuurReizen.TEST_ZONES = List.of(w.z1, w.z2, w.z3);
        ZonePoortConfig.TEST = 6;
        ZonePoortConfig.TEST_BOEK = 0;
        ZonePoort.TEST_STRENG = true;
        ZonePoort.zetHoogste(p, 6);
        for (BlockPos b : new BlockPos[] {w.a1, w.a2, w.b1, w.c1}) {
            Voltooiing.map(p, "vuren").putBoolean(Voltooiing.sleutel(b), true);   // every campfire lit
        }
        staBij(p, w.a1);
        return p;
    }

    private static void staBij(ServerPlayer p, BlockPos vuur) {
        p.snapTo(vuur.getX() + 0.5, vuur.getY(), vuur.getZ() + 2.5);
    }

    private static void herstel(ServerPlayer p) {
        KampvuurReizen.TEST = false;
        KampvuurReizen.TEST_ZONES = null;
        KampvuurReizen.vergeet(p.getUUID());
        ZonePoortConfig.TEST = null;
        ZonePoortConfig.TEST_BOEK = null;
        ZonePoort.TEST_STRENG = false;
        Gevecht.zet(p, false);
        Spelers.data(p).extra.remove(Voltooiing.SLICE);
        Spelers.data(p).checkpoint = null;
    }

    private static List<String> ids(ServerPlayer p) {
        return KampvuurReizen.lijst(p).stream().map(KampvuurReizen.Vuur::id).toList();
    }

    /** Not 100 %: nothing to travel to, the click does nothing extra, a forced choice is refused, no unlock message. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void nietVolGeenReis(GameTestHelper helper) {
        Wereld w = new Wereld(helper);
        ServerPlayer p = speler(helper, w);
        try {
            helper.assertTrue(KampvuurReizen.lijst(p).isEmpty(), "no zone at 100 %: no destinations, got " + ids(p));
            helper.assertTrue(!KampvuurReizen.vrij(p, w.z1) && !KampvuurReizen.vrij(p, w.z2), "travel not unlocked");
            KampvuurReizen.openVoorTest(p, w.a1);
            String r = KampvuurReizen.kies(p, w.id(w.a2));
            helper.assertTrue("niet_vrij".equals(r), "choosing anyway is refused (niet_vrij), got " + r);
            Voltooiing.controleer(p, KampvuurReizen.TEST_ZONES);
            helper.assertTrue(!KampvuurReizen.gemeld(p, w.z1.id()), "no unlock message below 100 %");
            helper.assertTrue(KampvuurReizen.zoneNr(ZoneBoeken.ZONE2) == 2 && KampvuurReizen.zoneNr(nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtBoek.BOEK) == 0,
                    "zone numbers: zone2 = 2, the crafts book = none");
        } finally {
            herstel(p);
        }
        helper.succeed();
    }

    /**
     * 100 %: only lit campfires of 100 % zones that are open for the player; a trip lands next to the target and is its checkpoint;
     * the anti-spam; "already here"; the unlock message once.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lijstEnReis(GameTestHelper helper) {
        Wereld w = new Wereld(helper);
        ServerPlayer p = speler(helper, w);
        try {
            w.vol1.set(true);
            w.vol3.set(true);   // zone 2 stays below 100 % (its campfire is lit, but that is not enough)
            ZonePoortConfig.TEST = 2;   // the server gate: zone 3 is "binnenkort"
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(ids(p).equals(List.of(w.id(w.a1), w.id(w.a2))), "zone 1 only (zone 2 < 100 %, zone 3 closed): " + ids(p));
            ZonePoortConfig.TEST = 6;
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(ids(p).equals(List.of(w.id(w.a1), w.id(w.a2), w.id(w.c1))), "zone 3 open again: its campfire is back: " + ids(p));
            Voltooiing.map(p, "vuren").remove(Voltooiing.sleutel(w.a2));
            helper.assertTrue(ids(p).equals(List.of(w.id(w.a1), w.id(w.c1))), "an unlit campfire is no destination: " + ids(p));

            // the unlock message: once per 100 % zone
            Voltooiing.controleer(p, KampvuurReizen.TEST_ZONES);
            helper.assertTrue(KampvuurReizen.gemeld(p, w.z1.id()) && KampvuurReizen.gemeld(p, w.z3.id()) && !KampvuurReizen.gemeld(p, w.z2.id()),
                    "unlock announced for zones 1 and 3, not 2");

            // the trip: from a1 to c1
            KampvuurReizen.openVoorTest(p, w.a1);
            String r = KampvuurReizen.kies(p, w.id(w.c1));
            helper.assertTrue(r == null, "the trip goes, got " + r);
            BlockPos staat = p.blockPosition();
            ServerLevel sl = helper.getLevel();
            helper.assertTrue(staat.distSqr(w.c1) <= 8 && !staat.equals(w.c1), "lands next to the target campfire, not in it: " + staat + " campfire " + w.c1);
            helper.assertTrue(!(sl.getBlockState(staat).getBlock() instanceof CampfireBlock) && sl.getBlockState(staat.below()).is(Blocks.STONE)
                    && sl.getBlockState(staat).isAir() && sl.getBlockState(staat.above()).isAir(), "on the floor, room for feet and head");
            helper.assertTrue(staat.equals(Spelers.data(p).checkpoint), "the target campfire is the checkpoint now");

            // anti-spam, then "already here"
            KampvuurReizen.openVoorTest(p, w.c1);
            r = KampvuurReizen.kies(p, w.id(w.a1));
            helper.assertTrue("wacht".equals(r), "a second trip right away waits (3 s), got " + r);
            KampvuurReizen.vergeet(p.getUUID());
            KampvuurReizen.openVoorTest(p, w.c1);
            r = KampvuurReizen.kies(p, w.id(w.c1));
            helper.assertTrue("hier".equals(r), "the campfire you stand at: already here, got " + r);
            r = KampvuurReizen.kies(p, w.id(w.b1));
            helper.assertTrue("onbekend".equals(r), "a campfire of a zone below 100 % is no destination, got " + r);
            r = KampvuurReizen.kies(p, w.id(w.a1));
            helper.assertTrue(r == null && p.blockPosition().distSqr(w.a1) <= 8, "back to a1, got " + r + " at " + p.blockPosition());
            // without a valid screen: silently nothing
            r = KampvuurReizen.kies(p, w.id(w.c1));
            helper.assertTrue("dicht".equals(r), "no open screen: ignored, got " + r);
        } finally {
            herstel(p);
        }
        helper.succeed();
    }

    /** Refused in combat, in a dungeon, too far from the campfire, and when the target has no safe spot. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void geblokkeerd(GameTestHelper helper) throws Exception {
        Wereld w = new Wereld(helper);
        ServerPlayer p = speler(helper, w);
        String sleutel = "kampreistest_" + UUID.randomUUID();
        try {
            w.vol1.set(true);
            w.vol2.set(true);
            KampvuurReizen.openVoorTest(p, w.a1);
            Gevecht.zet(p, true);
            String r = KampvuurReizen.kies(p, w.id(w.b1));
            helper.assertTrue("gevecht".equals(r), "in combat: refused, got " + r);
            Gevecht.zet(p, false);

            // a dungeon run (an instance with this player as member)
            var maak = DungeonRij.Instantie.class.getDeclaredConstructor(String.class);
            maak.setAccessible(true);
            DungeonRij.Instantie inst = maak.newInstance(sleutel);
            inst.leden.add(p.getUUID());
            DungeonRij.BEZET.put(sleutel, inst);
            r = KampvuurReizen.kies(p, w.id(w.b1));
            helper.assertTrue("kerker".equals(r), "in a dungeon: refused, got " + r);
            DungeonRij.BEZET.remove(sleutel);

            // riding: refused (the mock player has no vehicle, so only the reason exists; see weiger)
            // too far from the campfire the screen was opened at
            p.snapTo(w.a1.getX() + 20.5, w.a1.getY(), w.a1.getZ() + 0.5);
            r = KampvuurReizen.kies(p, w.id(w.b1));
            helper.assertTrue("ver".equals(r), "walked away from the campfire: refused, got " + r);
            staBij(p, w.a1);

            // no safe spot at the target: stone all around it at feet height
            ServerLevel sl = helper.getLevel();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        if (dx != 0 || dz != 0 || dy != 0) {
                            sl.setBlockAndUpdate(w.b1.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
                        }
                    }
                }
            }
            r = KampvuurReizen.kies(p, w.id(w.b1));
            helper.assertTrue("geen_plek".equals(r), "no safe spot next to the target: refused, got " + r);
            helper.assertTrue(p.blockPosition().distSqr(w.a1) <= 9, "and the player stays where he was");
            // the safe-spot rule itself: never in or on the campfire
            helper.assertTrue(KampvuurReizen.veiligePlek(sl, w.b1) == null, "walled in: no spot (never the campfire itself or above it)");
            BlockPos plek = KampvuurReizen.veiligePlek(sl, w.a1);
            helper.assertTrue(plek != null && !plek.equals(w.a1) && !plek.equals(w.a1.above()), "an open campfire has a spot beside it: " + plek);
        } finally {
            DungeonRij.BEZET.remove(sleutel);
            herstel(p);
        }
        helper.succeed();
    }
}
