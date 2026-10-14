package nl.juiced.guhs.feature.ringvlucht;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2 (rookguh): the Rookguh-vlucht, server side. The test server has neither the Guhmensie nor the Barbecuether, so the
 * destinations are put in the test level ({@link Vlucht#testLevel}, {@link Vlucht#TEST_DOEL}); the field
 * {@code ringvlucht_test_veld} (tools/features/ring_vlucht.py test_veld) is a stone floor with a house that has a door south
 * (a pool where the landing would be) and west (a walled pocket there). Mock players get no packets and are not ticked by the
 * server: the tests post the player tick that ends a cutscene a mock player "watches".
 */
public final class RingVluchtGameTests {
    private static final String VELD = "ringvlucht_test_veld", BATCH = "ringvlucht";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos lokaal) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Ring.wis(p);
        Vec3 daar = Vec3.atBottomCenterOf(helper.absolutePos(lokaal));
        p.snapTo(daar.x, daar.y, daar.z);
        p.setOnGround(true);
        return p;
    }

    /** The story up to and including chapter {@code n} is done (0: only begun). */
    private static void klaarTot(ServerPlayer p, int n) {
        Ring.lijn(1).begin(p);
        for (int i = 1; i <= n; i++) {
            Verhaallijn l = Ring.lijn(i);
            l.zet(p, l.stappen());
        }
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Vlucht.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        Vlucht.testLevel = null;
        Vlucht.TEST_DOEL.clear();
    }

    private static List<String> ids(List<Haltes.Halte> haltes) {
        return haltes.stream().map(Haltes.Halte::id).toList();
    }

    /** A template position of the field (y 0 = the floor) in the world. */
    private static BlockPos veld(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y + 1, z));
    }

    private static BoundingBox huis(GameTestHelper helper) {
        return BoundingBox.fromCorners(veld(helper, 26, 1, 26), veld(helper, 37, 5, 37));
    }

    private static Vlucht.Doelplek zuid(GameTestHelper helper) {
        return new Vlucht.Doelplek(helper.getLevel(), List.of(new Landing.Ingang(veld(helper, 31, 1, 37), 0, 1)), huis(helper), 25);
    }

    private static Vlucht.Doelplek west(GameTestHelper helper) {
        return new Vlucht.Doelplek(helper.getLevel(), List.of(new Landing.Ingang(veld(helper, 26, 1, 31), -1, 0)), huis(helper), 25);
    }

    private static boolean geweigerd(ServerPlayer p, net.minecraft.world.damagesource.DamageSource bron) {
        return net.neoforged.neoforge.common.CommonHooks.onEntityIncomingDamage(p, new net.neoforged.neoforge.common.damagesource.DamageContainer(bron, 4f));
    }

    /** The stops (haltes.json), their lines and structures, the two scenes. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ringvluchtHaltes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Haltes.ALLE.size() == 8, "eight stops: " + ids(Haltes.ALLE));
        Set<String> gezien = new HashSet<>();
        for (Haltes.Halte h : Haltes.ALLE) {
            helper.assertTrue(gezien.add(h.id()), "one stop per id: " + h.id());
            helper.assertTrue(Verhaallijnen.van(h.lijn()) == h.verhaallijn(), "the line of " + h.id() + " is " + h.lijn());
            helper.assertTrue(Kopieen.structuur(level, h.structuur()) != null, "the structure of " + h.id() + " exists: " + h.structuur());
            helper.assertTrue(h.id().equals(Haltes.GOUW) || !h.ingangen().isEmpty(), "an entrance at " + h.id());
            for (Haltes.Ingang i : h.ingangen()) {
                helper.assertTrue(Math.abs(i.dx()) + Math.abs(i.dz()) == 1, "the way out of " + h.id() + " is a direction");
            }
            helper.assertTrue(h.dimensie() == (h.id().equals(Haltes.GOUW) ? ModDimensions.GUHMENSION : BarbecuetherFeature.BARBECUETHER),
                    "the dimension of " + h.id());
        }
        helper.assertTrue(Cutscene.van("ringvlucht_vlucht") != null && Cutscene.van("ringvlucht_vlucht_sam") != null, "the two flight scenes");
        helper.assertTrue(Haltes.van("guhvendel").rookguh() != null && Haltes.van(Haltes.AANLEG).rookguh() == null, "who has a Rookguh of their own");
        helper.succeed();
    }

    /**
     * Where a player may fly to, per progress: only stops their story reached (never one behind a closed sluier), the next
     * one on top, never the spot where the Rookguh stands; and who sees which Rookguh.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ringvluchtBestemmingenPerVoortgang(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            helper.assertTrue(Vlucht.bestemmingen(p, null).isEmpty(), "before the story: nowhere");
            Ring.lijn(1).begin(p);
            helper.assertTrue(ids(Vlucht.bestemmingen(p, null)).equals(List.of("gouw")), "begun: only the Gouw, " + ids(Vlucht.bestemmingen(p, null)));
            klaarTot(p, 1);
            helper.assertTrue(ids(Vlucht.bestemmingen(p, Haltes.van("gouw"))).equals(List.of("guhvendel")),
                    "chapter 1 done, at the Gouw's Rookguh: Guhvendel (in the Barbecuether), " + ids(Vlucht.bestemmingen(p, Haltes.van("gouw"))));
            klaarTot(p, 3);
            List<String> drie = ids(Vlucht.bestemmingen(p, Haltes.van("knabbelmoria")));
            helper.assertTrue(drie.equals(List.of("boomstad", "gouw", "guhvendel", "knabbelmoria")),
                    "chapter 3 done: the tree city first, then back to every place before (the mine's entrance is far from its exit): " + drie);
            for (String nee : List.of("aanleg", "sausuman", "roosterpoort", "frituurberg")) {
                helper.assertTrue(!drie.contains(nee), "not yet reached: " + nee);
            }
            klaarTot(p, 4);
            List<String> vier = ids(Vlucht.bestemmingen(p, Haltes.van("boomstad")));
            helper.assertTrue(vier.get(0).equals("roosterpoort") && vier.contains("sausuman") && vier.contains("boomstad") && !vier.contains("aanleg")
                    && !vier.contains("frituurberg"), "chapter 4 done, at the landing: the gate first, the tower too, not the landing itself: " + vier);
            klaarTot(p, 6);
            List<String> alles = ids(Vlucht.bestemmingen(p, Haltes.van("frituurberg")));
            helper.assertTrue(Vlucht.volgende(p) == null && alles.size() == 7 && !alles.contains("frituurberg") && alles.get(0).equals("gouw"),
                    "the story done: every place in story order, but not where you stand: " + alles);
            // the Rookguh of a stop is there for whoever finished it, and for nobody else
            ServerPlayer nieuw = speler(helper, new BlockPos(2, 2, 2));
            klaarTot(nieuw, 1);
            Haltes.Halte guhvendel = Haltes.van("guhvendel");
            VluchtRookguhEntity r = VluchtRookguhEntity.maak(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 2))), guhvendel, 0f);
            helper.assertTrue(r != null && !Haltes.klaar(nieuw, guhvendel) && Haltes.klaar(p, guhvendel), "Guhvendel's Rookguh: for whoever finished chapter 2");
            helper.getLevel().addFreshEntity(r);
            helper.assertTrue(!Zicht.magZien(nieuw, r) && Zicht.magZien(p, r), "the one who did not finish chapter 2 does not even see it");
            helper.assertTrue(!Vlucht.kies(nieuw, r, "gouw"), "a Rookguh that is not there for you does not fly");
            helper.assertTrue(!Vlucht.kies(p, r, "nergens") && !Vlucht.onderweg(p), "no such place");
            r.discard();
            weg(helper, p, nieuw);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }

    /** Never to a place the story did not reach, also when a (modified) client asks for it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ringvluchtNooitNaarOnbereikt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            klaarTot(p, 2);
            Vlucht.testLevel = helper.getLevel();
            for (Haltes.Halte h : Haltes.ALLE) {
                Vlucht.TEST_DOEL.put(h.id(), new Vlucht.Doelplek(helper.getLevel(), List.of(new Landing.Ingang(helper.absolutePos(new BlockPos(3, 2, 3)), 0, 1)), null, 0));
            }
            VluchtRookguhEntity r = VluchtRookguhEntity.maak(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 2))), Haltes.van("guhvendel"), 0f);
            helper.getLevel().addFreshEntity(r);
            for (String nee : List.of("boomstad", "aanleg", "sausuman", "roosterpoort", "frituurberg")) {
                helper.assertTrue(!Haltes.bereikt(p, Haltes.van(nee)) && !Vlucht.kies(p, r, nee) && !Vlucht.onderweg(p), "chapter 2 done: never to " + nee);
            }
            helper.assertTrue(!Vlucht.kies(p, r, "guhvendel"), "not to where this Rookguh stands");
            helper.assertTrue(Vlucht.kies(p, r, "knabbelmoria") && Vlucht.onderweg(p) && Cutscenes.bezig(p), "to the next chapter: the flight starts");
            helper.assertTrue(!Vlucht.kies(p, r, "gouw"), "one flight at a time");
            r.discard();
            // the grill portal's rule: into the Barbecuether only for whom it is open; the way back is free
            ServerPlayer nieuw = speler(helper, new BlockPos(2, 2, 2));
            Ring.lijn(1).begin(nieuw);
            helper.assertTrue(Vlucht.grensDicht(nieuw, ModDimensions.GUHMENSION, BarbecuetherFeature.BARBECUETHER), "chapter 1 not done: not over the portal");
            helper.assertTrue(!Vlucht.grensDicht(nieuw, BarbecuetherFeature.BARBECUETHER, ModDimensions.GUHMENSION)
                    && !Vlucht.grensDicht(nieuw, BarbecuetherFeature.BARBECUETHER, BarbecuetherFeature.BARBECUETHER), "back and within: free");
            weg(helper, p, nieuw);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }

    /** The landing: on solid ground, never in the pool, on magma, in the pocket you can't walk out of, or behind a closed sluier. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void ringvluchtLandingVeilig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vlucht.Doelplek z = zuid(helper);
        BlockPos deur = z.ingangen().get(0).plek();
        BlockPos plek = Landing.zoek(level, z.ingangen(), z.afstand(), z.doos(), c -> true);
        helper.assertTrue(plek != null && Landing.staan(level, plek) && Landing.landbaar(level, plek), "south: a place to stand " + plek);
        helper.assertTrue(level.getBlockState(plek.below()).blocksMotion() && level.getFluidState(plek.below()).isEmpty(), "solid ground under it, no water");
        helper.assertTrue(!z.doos().isInside(plek) && plek.getZ() > deur.getZ(), "in front of the door, outside the house: " + plek);
        int ver = plek.getZ() - deur.getZ();
        helper.assertTrue(ver >= 15 && ver <= 30 && Math.abs(plek.getX() - deur.getX()) <= 12, "about 25 blocks out, next to the pool: " + ver);
        helper.assertTrue(!level.getBlockState(plek.below()).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK), "not on magma");
        // the west door: 25 blocks out lies a walled pocket nobody can walk into
        Vlucht.Doelplek w = west(helper);
        BlockPos west = Landing.zoek(level, w.ingangen(), w.afstand(), w.doos(), c -> true);
        BoundingBox zak = BoundingBox.fromCorners(veld(helper, 2, 1, 25), veld(helper, 8, 1, 37));
        helper.assertTrue(west != null && !zak.isInside(west) && Landing.staan(level, west) && west.getX() < w.ingangen().get(0).plek().getX(),
                "west: never inside the closed pocket, " + west);
        // a closed sluier over the dry ground in front of the south door: the Rookguh lands elsewhere
        BoundingBox dicht = BoundingBox.fromCorners(veld(helper, 0, 1, 40), veld(helper, 63, 4, 54));
        BlockPos anders = Landing.zoek(level, z.ingangen(), z.afstand(), z.doos(), c -> !dicht.isInside(c));
        helper.assertTrue(anders != null && !dicht.isInside(anders) && Landing.staan(level, anders), "not behind the closed sluier: " + anders);
        // on the spot itself (the elf boats' landing): the entrance; and where there is no ground at all: nowhere
        BlockPos zelf = Landing.zoek(level, List.of(new Landing.Ingang(deur, 0, 1)), 0, null, c -> true);
        helper.assertTrue(deur.equals(zelf), "distance 0: the entrance itself, " + zelf);
        BlockPos lucht = veld(helper, 20, 40, 20);
        helper.assertTrue(Landing.zoek(level, List.of(new Landing.Ingang(lucht, 0, 1)), 25, null, c -> true) == null, "in thin air: nowhere");
        helper.succeed();
    }

    /**
     * The whole flight: the choice, the scene, the landing in front of the destination, the player unhurt (also the fall
     * they "made"), the rest point there, and the copy remembered for the next flight.
     */
    @GuhTest(template = VELD, batch = BATCH + "_vlucht", timeoutTicks = 400)
    public static void ringvluchtVliegtEnLandt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        klaarTot(p, 2);
        Vlucht.testLevel = level;
        Vlucht.TEST_DOEL.put("knabbelmoria", zuid(helper));
        VluchtRookguhEntity r = VluchtRookguhEntity.maak(level, Vec3.atBottomCenterOf(veld(helper, 12, 2, 10)), Haltes.van("guhvendel"), 0f);
        level.addFreshEntity(r);
        BlockPos verwacht = Landing.zoek(level, zuid(helper).ingangen(), 25, huis(helper), c -> true);
        p.fallDistance = 12;
        helper.assertTrue(Vlucht.kies(p, r, "knabbelmoria"), "the flight starts");
        helper.onEachTick(() -> NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p)));
        helper.succeedWhen(() -> {
            helper.assertTrue(!Vlucht.onderweg(p) && !Cutscenes.bezig(p), "the scene is over");
            helper.assertTrue(p.blockPosition().equals(verwacht), "the player stands on the landing spot " + verwacht + ": " + p.blockPosition());
            helper.assertTrue(p.fallDistance == 0 && Vlucht.netGeland(p), "no fall, just landed");
            helper.assertTrue(geweigerd(p, level.damageSources().fall()) && geweigerd(p, level.damageSources().lava()) && !geweigerd(p, level.damageSources().genericKill()),
                    "nothing hurts a player who was just put down (only /kill)");
            Rustpunten.Punt rust = Ring.rustpunt(p);
            helper.assertTrue(rust != null && rust.plek().distanceTo(Vec3.atBottomCenterOf(verwacht)) < 0.01, "on the trip: the landing is the rest point");
            helper.assertTrue(Cutscenes.gezien(p, "ringvlucht_vlucht_sam"), "the flight scene was seen, with Sam-guh (he walks along from chapter 2 on)");
            r.discard();
            weg(helper, p);
        });
    }

    /** The Gouw: in front of the player's OWN Guhdalf (chapter 1's saved spot), on the open side of his camp. */
    @GuhTest(template = VELD, batch = BATCH)
    public static void ringvluchtNaarDeEigenGouw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        try {
            klaarTot(p, 1);
            Vlucht.testLevel = level;
            BlockPos guhdalf = veld(helper, 50, 1, 10);
            CompoundTag saved = GuhQuests.saved(p);
            saved.putLong("guhs_ringh1_thuis", guhdalf.asLong());
            saved.putString("guhs_ringh1_thuis_dim", level.dimension().identifier().toString());
            Vlucht.Doelplek d = Vlucht.doelplek(p, Haltes.van(Haltes.GOUW));
            helper.assertTrue(d != null && d.ingangen().size() == 1 && d.ingangen().get(0).plek().equals(guhdalf), "the Gouw is the player's own Guhdalf");
            BlockPos plek = Landing.zoek(level, d.ingangen(), d.afstand(), d.doos(), c -> true);
            helper.assertTrue(plek != null && Landing.staan(level, plek) && plek.getZ() > guhdalf.getZ() + 3 && plek.distManhattan(guhdalf) <= 14,
                    "a few steps in front of Guhdalf (a camp without its table counts as unturned: south): " + plek);
            // a player whose Gouw is in another dimension: not this one
            saved.putString("guhs_ringh1_thuis_dim", "guhs:ergens_anders");
            Vlucht.Doelplek elders = Vlucht.doelplek(p, Haltes.van(Haltes.GOUW));
            helper.assertTrue(elders == null || !elders.ingangen().get(0).plek().equals(guhdalf), "a Gouw elsewhere is not this one");
            weg(helper, p);
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
        helper.succeed();
    }
}
