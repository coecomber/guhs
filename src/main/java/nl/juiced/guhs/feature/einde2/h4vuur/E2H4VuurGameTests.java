package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * einde2 phase B, slice h4_vuur: the state machine of chapter 4 and the finale for one of two players (CONTRACT_E2_B 8), and
 * the scripts of the cutscenes against the contract's lengths. The headless test server has no Guheinde, so the places
 * themselves (zones, the duel, the fleet) are played through the dev command in a real world; here the steps, the flags,
 * the rewards and the scenes.
 */
public class E2H4VuurGameTests {
    private static final String BATCH = "einde2_h4_vuur";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static boolean heeftBeeld(ServerPlayer p) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(E2H4VuurFeature.BEELD_ITEM.get()) && s.get(DataComponents.PROFILE) != null) {
                return true;
            }
        }
        return false;
    }

    /** The two lines step by step for one player: the disguise, the making-up, the dragons' flags, the rewards; the other player untouched. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void einde2H4VuurKeten(GameTestHelper helper) {
        ServerPlayer p = speler(helper), q = speler(helper);
        try {
            // chapter 4 begins (chapter 3 done): not in disguise before the arrival, in disguise from the school to the Black Sun
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_AANKOMST);
            E2VerhaalDev.zetTot(q, E2Verhaal.VUUR, E2H4Vuur.V_AANKOMST);
            helper.assertTrue(!E2H4Vuur.vermomd(p) && Look.van(p) == Look.GEEN, "no disguise before the arrival");
            E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_AANKOMST);
            helper.assertTrue(E2H4Vuur.vermomd(p) && Look.van(p) == Look.VERMOMD && !E2H4Vuur.vermomd(q), "in disguise after the arrival (only p)");
            // the dance party: won
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_DANS);
            Dansfeest.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_EENDJES && !Dansfeest.actief(p), "the party won: the turtle-ducks are next");
            // after the Black Sun: no disguise any more
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_GEVANGENIS);
            helper.assertTrue(!E2H4Vuur.vermomd(p), "no disguise after the Black Sun");
            // making it up: a wrong answer changes nothing, Toph then Katara moves the step on
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_GOEDMAKEN);
            E2VerhaalDev.zetTot(q, E2Verhaal.VUUR, E2H4Vuur.V_GOEDMAKEN);
            Goedmaken.antwoordToph(p, null, 2);
            helper.assertTrue(!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH) && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_GOEDMAKEN, "a wrong answer: nothing");
            Goedmaken.antwoordToph(p, null, 1);
            helper.assertTrue(E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_TOPH) && E2H4Vuur.VUUR.sleutel(p).equals("7_katara")
                    && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_GOEDMAKEN, "Toph made up: Katara is next");
            Goedmaken.antwoordKatara(p, null, 3);
            helper.assertTrue(!E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_KATARA), "a silly answer for Katara: nothing");
            Goedmaken.antwoordKatara(p, null, 1);
            helper.assertTrue(E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_ROTSBLOK && E2H4Vuur.VUUR.stap(q) == E2H4Vuur.V_GOEDMAKEN
                    && !E2H4Vuur.VUUR.vlag(q, E2H4Vuur.V_TOPH), "both made up: Het Kokend Rotsblok; the other player untouched");
            // the dragons: Zuka joins (zuka_team) and fire is learnt (geleerd), for p only
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_DRAKENDANS);
            helper.assertTrue(!E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR), "fire not learnt before the dragons");
            Drakendans.winVoorTest(p);
            helper.assertTrue(E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM) && E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_GELEERD)
                    && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.VUUR) && E2H4Vuur.VUUR.stap(p) == E2H4Vuur.V_TONEEL
                    && !E2Verhaal.vlag(q, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM), "the dragons: zuka_team and geleerd (p only); the theatre is next");
            // chapter 4 done: the disguise as guh clothes
            E2H4Vuur.VUUR.zet(p, E2H4Vuur.V_STAPPEN);
            helper.assertTrue(E2Verhaal.klaar(p, E2Verhaal.VUUR) && KledingUnlocks.heeft(p, GuhClothes.VUURNATIE_VERMOMMING)
                    && !KledingUnlocks.heeft(q, GuhClothes.VUURNATIE_VERMOMMING), "chapter 4 done: the Vuurnatie disguise for p");
            // the finale: the dev command's jump, the fleet and the duel as if won, then the end
            helper.assertTrue(E2H4VuurCommando.zet(p, "luchtvloot") && E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_VLOOT
                    && E2Verhaal.vlag(p, E2Verhaal.FINALE, E2Verhaal.V_WITTE_LOTUS) && E2Verhaal.klaar(p, E2Verhaal.VUUR), "the dev jump to the fleet");
            Luchtvloot.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_AGNI_KAI, "the fleet flown: the Agni Kai");
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, E2H4Vuur.F_DUEL);
            Duel.winVoorTest(p);
            helper.assertTrue(E2H4Vuur.FINALE.stap(p) == E2H4Vuur.F_KRONING && !Duel.actief(p) && Look.van(p) == Look.GEEN, "the duel won: the coronation");
            helper.assertTrue(!E2Verhaal.finaleKlaar(p) && !heeftBeeld(p), "no reward before the end");
            E2H4Vuur.FINALE.zet(p, E2H4Vuur.F_STAPPEN);
            Titels.Titel titel = Titels.van(E2H4VuurFeature.TITEL);
            helper.assertTrue(E2Verhaal.finaleKlaar(p) && titel != null && Titels.heeft(p, titel) && !Titels.heeft(q, titel), "the title Guhvatar (p only)");
            helper.assertTrue(heeftBeeld(p) && KledingUnlocks.heeft(p, GuhClothes.ZUKA_LITTEKEN) && !heeftBeeld(q), "the statue and the scar");
            int beelden = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                beelden += s.is(E2H4VuurFeature.BEELD_ITEM.get()) ? s.getCount() : 0;
            }
            E2H4Vuur.FINALE.zet(p, E2H4Vuur.F_STAPPEN);
            int daarna = 0;
            for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                daarna += s.is(E2H4VuurFeature.BEELD_ITEM.get()) ? s.getCount() : 0;
            }
            helper.assertTrue(beelden == 1 && daarna == 1, "the statue only once");
        } finally {
            for (ServerPlayer s : new ServerPlayer[] {p, q}) {
                E2H4Vuur.wisEigen(s);
                for (String l : E2Verhaal.HOOFDSTUKKEN) {
                    E2VerhaalDev.wis(s, l);
                }
                helper.getLevel().removePlayerImmediately(s, Entity.RemovalReason.DISCARDED);
            }
        }
        helper.succeed();
    }

    /**
     * fix3-h4 (the crash of playtest 2: "duplicate key BlockPos" in Drakendans.licht): the two tiles of the capital that hold the
     * dance terrace are put down in the test world, the capital's origin points there, and the Drakendans starts for real: both
     * routes are as long, never share a tile, the first step lights two different blocks, a danced tile moves the dance on, and a
     * shared tile (an old build) lights one block instead of throwing.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void einde2H4VuurDrakendans(GameTestHelper helper) {
        // the tiles never share a block, and a shared one does not throw
        Plekken.Plek jij = Plekken.plek(Plek.STRUCTUUR, "drakendans_speler"), zuka = Plekken.plek(Plek.STRUCTUUR, "drakendans_zuka"),
                zone = Plekken.plek(Plek.STRUCTUUR, "drakendans");
        helper.assertTrue(jij != null && zuka != null && zone != null && jij.punten().size() == 7 && zuka.punten().size() == 7, "two routes of seven steps");
        for (int i = 0; i < jij.punten().size(); i++) {
            for (BlockPos z : zuka.punten()) {
                helper.assertTrue(!jij.punten().get(i).equals(z), "your step " + i + " is never one of Zuka's tiles: " + z);
            }
        }
        Vec3 een = new Vec3(3.5, 70, 3.5);
        helper.assertTrue(Drakendans.tegels(een, een).size() == 1 && Drakendans.tegels(een, een.add(2, 0, 0)).size() == 2, "a shared tile lights one block");
        // the two tiles of the capital with the terrace, put down above the test's own spot
        ServerLevel level = helper.getLevel();
        BlockPos a = zone.pos(), b = zone.tot();
        BlockPos at = helper.absolutePos(new BlockPos(0, 70, 0));
        int i0 = a.getX() / 32, i1 = b.getX() / 32, j0 = a.getZ() / 32, j1 = b.getZ() / 32;
        BlockPos oorsprong = at.offset(-i0 * 32, 0, -j0 * 32);
        java.util.List<net.minecraft.world.level.ChunkPos> vast = new java.util.ArrayList<>();
        for (int cx = at.getX() >> 4; cx <= (at.getX() + (i1 - i0 + 1) * 32) >> 4; cx++) {
            for (int cz = at.getZ() >> 4; cz <= (at.getZ() + (j1 - j0 + 1) * 32) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    vast.add(new net.minecraft.world.level.ChunkPos(cx, cz));
                }
                level.getChunk(cx, cz);
            }
        }
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                StructureTemplate tegel = level.getStructureManager().get(Guhs.id(Plek.STRUCTUUR + "/stuk_" + i + "_" + j)).orElseThrow();
                BlockPos hier = oorsprong.offset(i * 32, 0, j * 32);
                tegel.placeInWorld(level, hier, hier, new StructurePlaceSettings().setKnownShape(true), level.getRandom(), 2);
            }
        }
        ServerPlayer p = speler(helper);
        var oud = Plek.testOorsprong;
        Plek.testOorsprong = q -> q == p ? oorsprong : null;
        try {
            BlockPos eerste = oorsprong.offset(jij.punten().get(0));
            helper.assertTrue(!level.getBlockState(eerste.below()).isAir(), "the terrace has a floor under the first step: " + level.getBlockState(eerste.below()));
            p.teleportTo(eerste.getX() + 0.5, eerste.getY(), eerste.getZ() + 0.5);
            helper.assertTrue(Plek.in(p, "drakendans"), "the player stands on the terrace");
            E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, E2H4Vuur.V_DRAKENDANS);
            Drakendans.start(p);
            helper.assertTrue(Drakendans.actief(p) && Drakendans.stap(p) == 0, "the dance starts (no duplicate key)");
            Map<BlockPos, BlockState> licht = Oplichten.van(p);
            helper.assertTrue(licht.size() == 2 && licht.containsValue(Blocks.SHROOMLIGHT.defaultBlockState())
                    && licht.containsValue(Blocks.SEA_LANTERN.defaultBlockState()) && licht.containsKey(eerste.below()), "two tiles lit: " + licht);
            // dancing every step (the player steps on each lit tile) ends the dance
            for (int stap = 0; stap < 6; stap++) {
                Drakendans.tick(p);
                helper.assertTrue(Drakendans.stap(p) == stap + 1, "step " + stap + " danced: " + Drakendans.stap(p));
                Vec3 volgende = Drakendans.doel(p);
                p.teleportTo(volgende.x, volgende.y, volgende.z);
                helper.assertTrue(Oplichten.van(p).size() == 2, "step " + (stap + 1) + ": two tiles lit");
            }
        } finally {
            Plek.testOorsprong = oud;
            E2H4Vuur.wisEigen(p);
            for (String l : E2Verhaal.HOOFDSTUKKEN) {
                E2VerhaalDev.wis(p, l);
            }
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            for (net.minecraft.world.level.ChunkPos c : vast) {
                level.setChunkForced(c.x(), c.z(), false);
            }
        }
        helper.succeed();
    }

    /**
     * fix4-finale (playtest 3: "I dodged the 8 blasts long ago, but round 1 of the duel never ends"): the duel runs for real in
     * the test world (the capital's origin points there; only the markers are needed, no blocks). The first blast hits the
     * player (a shove: it does not count); after that the player steps aside from every blast. Round 1 must end at the eighth
     * dodge with round 2, and Mikai must stop firing once the blasts that are still needed are in the air (one hit + eight
     * dodges = nine blasts). On the old code the round never ended (it waited for an empty sky while Mikai kept firing).
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 1400)
    public static void einde2H4VuurDuelLucht(GameTestHelper helper) {
        Plekken.Plek start = Plekken.plek(Plek.STRUCTUUR, "duel_speler"), mikai = Plekken.plek(Plek.STRUCTUUR, "duel_mikai");
        helper.assertTrue(start != null && mikai != null && Plekken.plek(Plek.STRUCTUUR, "duel_arena") != null, "the duel's markers are in the build");
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 20, 1));
        BlockPos oorsprong = at.subtract(start.pos());
        java.util.List<net.minecraft.world.level.ChunkPos> vast = new java.util.ArrayList<>();
        for (int cx = (at.getX() - 48) >> 4; cx <= (at.getX() + 48) >> 4; cx++) {
            for (int cz = (at.getZ() - 48) >> 4; cz <= (at.getZ() + 48) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) {
                    vast.add(new net.minecraft.world.level.ChunkPos(cx, cz));
                }
                level.getChunk(cx, cz);
            }
        }
        ServerPlayer p = speler(helper);
        Plek.testOorsprong = q -> q == p ? oorsprong : null;
        Duel.start(p);
        helper.assertTrue(Duel.actief(p) && Duel.ronde(p) == Duel.LUCHT && Plek.in(p, "duel_arena"), "round 1 starts, on the first pillar");
        Vec3 thuis = p.position();
        Vec3 naarMikai = Vec3.atBottomCenterOf(oorsprong.offset(mikai.pos())).subtract(thuis).multiply(1, 0, 1).normalize();
        Vec3 opzij = new Vec3(-naarMikai.z, 0, naarMikai.x);
        // per blast: the spot (-1, 0, 1: three blocks apart) the player stood on when it was fired
        Map<Integer, Integer> doel = new java.util.HashMap<>();
        java.util.Set<Integer> alle = new java.util.HashSet<>();
        int[] plek = {0}, meest = {0};
        boolean[] eersteRaak = {false};
        StringBuilder log = new StringBuilder();
        int[] tik = {0}, vorig = {0};
        int[] eerste = {-1};
        helper.onEachTick(() -> {
            if (!Duel.actief(p) || Duel.ronde(p) != Duel.LUCHT) {
                return;
            }
            Duel.tick(p);
            if (Duel.ronde(p) != Duel.LUCHT) {
                return;
            }
            tik[0]++;
            if (Duel.ontweken(p) != vorig[0]) {
                log.append(" t").append(tik[0]).append(":dodge").append(Duel.ontweken(p));
                vorig[0] = Duel.ontweken(p);
            }
            meest[0] = Math.max(meest[0], Duel.ontweken(p));
            java.util.List<VuurbalEntity> stoten = Duel.stoten(p);
            java.util.Set<Integer> nu = new java.util.HashSet<>();
            for (VuurbalEntity b : stoten) {
                nu.add(b.getId());
                alle.add(b.getId());
                if (doel.putIfAbsent(b.getId(), plek[0]) == null) {
                    log.append(" t").append(tik[0]).append(":fire@").append(plek[0]);
                }
            }
            for (Integer id : doel.keySet()) {
                if (!nu.contains(id)) {
                    log.append(" t").append(tik[0]).append(":gone");
                }
            }
            doel.keySet().retainAll(nu);
            if (!eersteRaak[0]) {
                // the first blast: stand still and take the shove
                if (eerste[0] < 0 && !alle.isEmpty()) {
                    eerste[0] = alle.iterator().next();
                }
                if (eerste[0] >= 0 && !nu.contains(eerste[0])) {
                    helper.assertTrue(Duel.ontweken(p) == 0, "a blast that hits you is not a dodge: " + Duel.ontweken(p));
                    eersteRaak[0] = true;
                }
                return;
            }
            Vec3 jij = p.position().add(0, 1, 0);
            for (VuurbalEntity b : stoten) {
                if (doel.get(b.getId()) == plek[0] && b.position().distanceTo(jij) < 7) {
                    for (int k = -1; k <= 1; k++) {
                        if (!doel.containsValue(k)) {
                            plek[0] = k;
                            Vec3 naar = thuis.add(opzij.scale(3.0 * k));
                            p.teleportTo(naar.x, naar.y, naar.z);
                            break;
                        }
                    }
                    break;
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(Duel.ronde(p) == Duel.WATER, "round 1 over: round 2 (dodged so far: " + Duel.ontweken(p) + ", blasts: " + alle.size() + ")");
            helper.assertTrue(eersteRaak[0] && meest[0] == Duel.LUCHT_STOTEN - 1 && alle.size() == Duel.LUCHT_STOTEN + 1,
                    "the first blast hit (" + eersteRaak[0] + "), the counter went up to 7 before the eighth dodge won (" + meest[0]
                            + "), and Mikai fired one hit + eight dodges = 9 blasts, not " + alle.size() + ";" + log);
            Duel.stop(p);
            Plek.testOorsprong = null;
            E2H4Vuur.wisEigen(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            for (net.minecraft.world.level.ChunkPos c : vast) {
                level.setChunkForced(c.x(), c.z(), false);
            }
        });
    }

    /** Every scene of the slice is registered, lasts what the contract allows, and only the credits run on without clicks. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void einde2H4VuurScenes(GameTestHelper helper) {
        for (Map.Entry<String, int[]> e : Scenes.LENGTES.entrySet()) {
            Cutscene s = Cutscene.van(Scenes.ID + e.getKey());
            helper.assertTrue(s != null, "scene " + e.getKey() + " registered");
            double sec = s.duur() / 20.0;
            helper.assertTrue(sec >= e.getValue()[0] && sec <= e.getValue()[1], "scene " + e.getKey() + " lasts " + sec + " s, not "
                    + e.getValue()[0] + "-" + e.getValue()[1]);
            helper.assertTrue(s.lijn() != null && (s.lijn().equals(E2Verhaal.VUUR) || s.lijn().equals(E2Verhaal.FINALE)), "scene " + e.getKey() + " replayable");
            for (Cutscene.Zeg z : s.zinnen()) {
                helper.assertTrue(!z.lied() || e.getKey().equals("aftiteling"), "only the credits run on: " + e.getKey() + "." + z.key());
                helper.assertTrue(z.spreker().isEmpty() || s.acteur(z.spreker()) != null, "a speaker is an actor: " + e.getKey() + "." + z.key());
                helper.assertTrue(z.t() + z.ticks() <= s.duur(), "the line ends in time: " + e.getKey() + "." + z.key());
            }
        }
        long eigen = Cutscene.alle().stream().filter(s -> s.id().startsWith(Scenes.ID) && !s.id().startsWith(H4Hints.KIJK)).count();
        helper.assertTrue(eigen == Scenes.LENGTES.size(), Scenes.LENGTES.size() + " scenes (18 + the six fix3-h4 why-scenes + the fix4-finale duel_begin), not " + eigen);
        helper.succeed();
    }

    /**
     * fix2-h4: every step after the arrival has a hint (where, a text, the resume spot) whose markers are in the build, and
     * the look scenes are 3.2 s with one run-on caption (no click wait).
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void einde2H4VuurHints(GameTestHelper helper) {
        for (int s = E2H4Vuur.V_SCHOOL; s < E2H4Vuur.V_STAPPEN; s++) {
            helper.assertTrue(StapHint.hint(E2Verhaal.VUUR, s) != null, "a hint for chapter 4 step " + s);
        }
        for (int s = 0; s < E2H4Vuur.F_STAPPEN; s++) {
            helper.assertTrue(StapHint.hint(E2Verhaal.FINALE, s) != null, "a hint for finale step " + s);
        }
        helper.assertTrue(H4Hints.MARKERS.size() == E2H4Vuur.V_STAPPEN - 1 + E2H4Vuur.F_STAPPEN, "one marker set per step: " + H4Hints.MARKERS.size());
        for (Map.Entry<String, String[]> e : H4Hints.MARKERS.entrySet()) {
            for (String m : e.getValue()) {
                helper.assertTrue(Plekken.plek(Plek.STRUCTUUR, m) != null, "marker " + m + " of hint " + e.getKey() + " is in the build");
            }
        }
        for (Cutscene k : new Cutscene[] {H4Hints.kijkSchool, H4Hints.kijkDans, H4Hints.kijkEendjes, H4Hints.kijkStrand, H4Hints.kijkKamp,
                H4Hints.kijkDrakendans, H4Hints.kijkToneel, H4Hints.kijkTent, H4Hints.kijkKroning, H4Hints.kijkSuki}) {
            helper.assertTrue(k != null && Cutscene.van(k.id()) == k && k.duur() == StapHint.KIJK_DUUR && k.zinnen().size() == 1
                    && k.zinnen().get(0).lied(), "look scene " + (k == null ? "?" : k.id()) + ": 3.2 s, one run-on caption");
        }
        helper.succeed();
    }
}
