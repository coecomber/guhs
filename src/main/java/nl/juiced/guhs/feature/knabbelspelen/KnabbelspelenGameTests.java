package nl.juiced.guhs.feature.knabbelspelen;

import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.doolhof.Anker;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Knabbelspelen: the zeskamp points and the lintjes (+1), the role, shop and clothing sources, the loaned things,
 * the protection, and in the real building: the fields match Speelvelden, and every event works (a bite, a hop, tins
 * falling in a pyramid, the wobbling egg and its flags, the swinging spijker and a plonk, the blind pin), friends
 * joining in their own lanes, and a whole Grote Zeskamp with its lintjes and board.
 */
public class KnabbelspelenGameTests {
    private static final String EMPTY = "empty";
    private static final String GEBOUW = "knabbelspelen";

    private static GuhNpcEntity juf(GameTestHelper helper) {
        List<GuhNpcEntity> npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.SPELLEIDERGUH);
        helper.assertTrue(npcs.size() == 1, "there is one Juf Vahoegsakee: " + npcs.size());
        return npcs.get(0);
    }

    private static ServerPlayer speler(GameTestHelper helper, GuhNpcEntity npc) {
        @SuppressWarnings("removal")
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.snapTo(npc.getX(), npc.getY(), npc.getZ() + 2);
        for (Onderdeel o : Onderdeel.values()) {
            GuhQuests.saved(p).remove(Wedstrijd.BEST_KEY + o.id());
        }
        GuhQuests.saved(p).remove(Wedstrijd.BEST_KEY + "zeskamp");
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Wedstrijd.stopFor(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Opens a round of one event (null: the zeskamp) and skips the waiting and the countdown. */
    private static Wedstrijd begin(GameTestHelper helper, GuhNpcEntity npc, ServerPlayer p, Onderdeel o) {
        Wedstrijd w = Wedstrijd.start(npc, p, o);
        helper.assertTrue(w != null, "the round opened");
        w.meteen(helper.getLevel());
        helper.assertTrue(w.fase() == Wedstrijd.Fase.BEZIG, "after the whistle");
        return w;
    }

    // --- pure logic -------------------------------------------------------------------------------------------------------

    /** Zeskamp points 0..1000 (points, times, the pin), lintjes +1. */
    @GuhTest(template = EMPTY)
    public static void knabbelspelenPuntenEnLintjes(GameTestHelper helper) {
        Onderdeel zak = Onderdeel.ZAKLOPEN;
        helper.assertTrue(zak.zeskamp(zak.perfect) == 1000 && zak.zeskamp(zak.perfect / 2) == 1000, "perfect time or faster = 1000");
        helper.assertTrue(zak.zeskamp(zak.perfect * 2) == 500 && zak.zeskamp(-1) == 0, "twice as long = 500, not finished = 0");
        Onderdeel hap = Onderdeel.KNABBELHAPPEN;
        helper.assertTrue(hap.zeskamp(hap.perfect) == 1000 && hap.zeskamp(hap.perfect * 2) == 1000 && hap.zeskamp(hap.perfect / 2) == 500,
                "points: perfect = 1000 (never more), half = 500");
        helper.assertTrue(Onderdeel.GUHGUHTJE_PRIK.zeskamp(640) == 640, "the pin counts itself");
        helper.assertTrue(GuhguhtjePrik.score(0) == 1000 && GuhguhtjePrik.score(GuhguhtjePrik.MIS) == 0 && GuhguhtjePrik.score(GuhguhtjePrik.MIS / 2) == 500,
                "the pin: 1000 right on the spot, 0 from 2.5 blocks");
        helper.assertTrue(Wedstrijd.lintjes(0) == 1 + 1 && Wedstrijd.lintjes(500) == 2 + 1 && Wedstrijd.lintjes(1000) == 3 + 1, "one event: 1-3, +1");
        helper.assertTrue(Wedstrijd.zeskampLintjes(0) == 2 + 1 && Wedstrijd.zeskampLintjes(6000) == 8 + 1, "the zeskamp: 2 + per 1000, +1");
        for (Onderdeel o : Onderdeel.values()) {
            helper.assertTrue(o.board().equals("spelen_" + o.id()), "the board of " + o);
        }
        helper.succeed();
    }

    /** Juf Vahoegsakee's role and shop, the one source of the sports outfit, the loaned things. */
    @GuhTest(template = EMPTY)
    public static void knabbelspelenRolWinkelEnKleding(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.SPELLEIDERGUH) instanceof KnabbelspelenRole, "Juf Vahoegsakee has her role");
        var offers = new KnabbelspelenRole().offers(null);
        for (GuhClothes c : List.of(GuhClothes.SPELEN_ZWEETBANDJE, GuhClothes.SPELEN_FLUITJE, GuhClothes.SPELEN_SPORTSHIRTJE)) {
            boolean verkocht = false;
            for (MerchantOffer o : offers) {
                if (o.getResult().is(ModItems.clothingItem(c)) && o.getBaseCostA().is(KnabbelspelenFeature.SPELENLINTJE.get())) {
                    verkocht = true;
                }
            }
            helper.assertTrue(verkocht, c + " is sold for spelenlintjes");
            helper.assertTrue("knabbelspelen".equals(KledingBronnen.bron(c)), c + " comes from the Knabbelspelen (only)");
        }
        for (var item : List.of(KnabbelspelenFeature.GUH_ZAK.get(), KnabbelspelenFeature.KNABBELEI_LEPEL.get(), KnabbelspelenFeature.KNABBELSPIJKER.get(),
                KnabbelspelenFeature.GUHGUHTJE_STAARTJE.get(), KnabbelspelenFeature.BLIK_PLUISBAL.get())) {
            ItemStack s = new ItemStack(item);
            helper.assertTrue(Features.isLoaned(s) && KnabbelspelenFeature.geleend(s), item + " is loaned");
        }
        helper.assertTrue(!Features.isLoaned(new ItemStack(KnabbelspelenFeature.SPELENLINTJE.get())), "the lintje is yours");
        @SuppressWarnings("removal")
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(KnabbelspelenProtection.denied(p, true) && !KnabbelspelenProtection.denied(p, false), "the fields are protected");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    // --- the real building ------------------------------------------------------------------------------------------------

    /** The anchor under Juf Vahoegsakee, and every lane holds what its event needs, where Speelvelden says. */
    @GuhTest(template = GEBOUW, timeoutTicks = 100, batch = "spelen_gebouw")
    public static void knabbelspelenVeldenKloppen(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        Anker a = Speelvelden.anker(npc);
        helper.assertTrue(a != null, "the anchor is found under Juf Vahoegsakee");
        ServerLevel level = helper.getLevel();
        int G = Speelvelden.G;
        for (Onderdeel o : Onderdeel.values()) {
            for (int k = 0; k < Speelvelden.BANEN; k++) {
                int u0 = (int) Wedstrijd.spel(o).startU();
                BlockPos s = Speelvelden.blok(a, o, k, u0, 0, G + 1);
                helper.assertTrue(level.getBlockState(s).isAir() && level.getBlockState(s.above()).isAir() && !level.getBlockState(s.below()).isAir(),
                        o + ": the start of lane " + k + " is free, on a floor");
            }
        }
        for (int k = 0; k < Speelvelden.BANEN; k++) {
            for (int i = 0; i < Blikgooien.BLIKKEN.length; i++) {
                helper.assertTrue(level.getBlockState(Blikgooien.plek(a, k, i)).is(KnabbelspelenFeature.BLIK.get()), "tin " + i + " of lane " + k);
            }
            helper.assertTrue(level.getBlockState(Blikgooien.plek(a, k, 3)).equals(Blikgooien.blik(a, 3)), "the second row is shifted right");
            for (int i = 0; i < Speelvelden.FLES_U.length; i++) {
                helper.assertTrue(level.getBlockState(Spijkerpoepen.fles(a, k, i)).is(KnabbelspelenFeature.KAASMELKFLES.get()), "bottle " + i + " of lane " + k);
            }
            helper.assertTrue(level.getBlockState(Speelvelden.blok(a, Onderdeel.GUHGUHTJE_PRIK, k, Speelvelden.PRIK_BORD, 0, G + 2))
                    .is(net.minecraft.world.level.block.Blocks.MAGENTA_CONCRETE), "the tail spot on board " + k);
            helper.assertTrue(!level.getBlockState(Speelvelden.blok(a, Onderdeel.KNABBELHAPPEN, k, Speelvelden.HAP_BALK, 0, Speelvelden.HAP_Y)).isAir(),
                    "the beam over lane " + k);
        }
        helper.succeed();
    }

    /** Knabbelhappen: two swinging knabbels per lane; a bite counts (a golden one 3), then a new knabbel swings; time's up: lintjes. */
    @GuhTest(template = GEBOUW, timeoutTicks = 300, batch = "spelen_hap")
    public static void knabbelspelenKnabbelhappen(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = begin(helper, npc, p, Onderdeel.KNABBELHAPPEN);
        helper.assertTrue("knabbelspelen".equals(Minigames.playing(p)), "playing the knabbelspelen");
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        Knabbelhappen.Staat s = Knabbelhappen.staat(d);
        helper.assertTrue(s.dingen.size() == 2, "two knabbels on strings");
        SpelDing ding = s.dingen.get(0);
        ding.goud(false);
        Vec3 was = ding.position();
        Knabbelhappen.hapRaak(w, d, p, ding);
        ding.goud(true);
        Knabbelhappen.hapRaak(w, d, p, ding);
        helper.assertTrue(s.punten == 4, "1 + 3 points: " + s.punten);
        helper.runAfterDelay(5, () -> helper.assertTrue(ding.position().distanceTo(was) > 1e-3, "the knabbel swings"));
        helper.runAfterDelay(6, () -> {
            w.klaar(d, p, s.punten, false);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(Wedstrijd.of(npc) == null, "the round is over");
            helper.assertTrue(GuhQuests.count(p, KnabbelspelenFeature.SPELENLINTJE.get()) == Wedstrijd.lintjes(Onderdeel.KNABBELHAPPEN.zeskamp(4)),
                    "lintjes for the knabbelhappen");
            helper.assertTrue(Wedstrijd.best(p, Onderdeel.KNABBELHAPPEN) == 4, "the record");
            helper.assertTrue(level.getEntitiesOfClass(SpelDing.class, helper.getBounds()).isEmpty(), "the knabbels are gone");
            weg(helper, p);
        });
    }

    /** Zaklopen: a hop pushes you forward the way you look (not before the whistle); at the finish your time counts. */
    @GuhTest(template = GEBOUW, timeoutTicks = 200, batch = "spelen_zak")
    public static void knabbelspelenZaklopen(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        Wedstrijd w = begin(helper, npc, p, Onderdeel.ZAKLOPEN);
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        helper.assertTrue(p.getMainHandItem().is(KnabbelspelenFeature.GUH_ZAK.get()), "a sack in your hands");
        helper.assertTrue(p.hasEffect(MobEffects.SLOWNESS), "walking in a sack is slow");
        p.setYRot(Speelvelden.yaw(w.anker, Onderdeel.ZAKLOPEN));
        p.setXRot(0);
        Zaklopen.hop(p);
        Vec3 v = p.getDeltaMovement();
        Vec3 langs = w.anker.vector(0, Speelvelden.veld(Onderdeel.ZAKLOPEN).richting());
        helper.assertTrue(Math.abs(v.x * langs.x + v.z * langs.z - Zaklopen.HOP) < 0.02 && v.y > 0.3, "a hop along the lane: " + v);
        Vec3 finish = Speelvelden.punt(w.anker, Onderdeel.ZAKLOPEN, d.baan, Speelvelden.ZAK_FINISH + 0.5, 0, Speelvelden.G + 1);
        p.snapTo(finish.x, finish.y, finish.z);
        helper.succeedWhen(() -> {
            helper.assertTrue(d.klaar && d.score[Onderdeel.ZAKLOPEN.ordinal()] >= 0, "finished with a time");
            helper.assertTrue(d.punten[Onderdeel.ZAKLOPEN.ordinal()] == 1000, "that fast: 1000 zeskamp points");
            weg(helper, p);
        });
    }

    /** Mika-blikgooien: a hit tin falls, and every tin resting on it; all six down is 20 extra and a fresh pyramid. */
    @GuhTest(template = GEBOUW, timeoutTicks = 200, batch = "spelen_blik")
    public static void knabbelspelenBlikgooien(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = begin(helper, npc, p, Onderdeel.BLIKGOOIEN);
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        helper.assertTrue(p.getMainHandItem().is(KnabbelspelenFeature.BLIK_PLUISBAL.get()) && p.getMainHandItem().getCount() == Blikgooien.BALLEN,
                "8 pluisballen in your hand");
        Blikgooien.Staat s = Blikgooien.staat(d);
        Blikgooien.raak(w, d, p, level, 1);
        helper.assertTrue(!s.staat[1] && !s.staat[3] && !s.staat[4] && !s.staat[5] && s.staat[0] && s.staat[2], "the middle one and the three on it fall");
        helper.assertTrue(s.punten == 4 * Blikgooien.PER_BLIK, "40 points: " + s.punten);
        helper.assertTrue(level.getBlockState(Blikgooien.plek(w.anker, d.baan, 5)).isAir(), "the top tin is off the table");
        Blikgooien.raak(w, d, p, level, 0);
        Blikgooien.raak(w, d, p, level, 2);
        helper.assertTrue(s.punten == 6 * Blikgooien.PER_BLIK + Blikgooien.ALLES_OM, "all down: 60 + 20 = " + s.punten);
        helper.assertTrue(Blikgooien.gooi(p, net.minecraft.world.InteractionHand.MAIN_HAND) && s.gegooid == 1, "a throw");
        helper.succeedWhen(() -> {
            helper.assertTrue(level.getBlockState(Blikgooien.plek(w.anker, d.baan, 5)).is(KnabbelspelenFeature.BLIK.get()), "a fresh pyramid");
            weg(helper, p);
        });
    }

    /** Eierlopen: a flag counts when you pass it; running makes the egg drop, back to the last flag. */
    @GuhTest(template = GEBOUW, timeoutTicks = 200, batch = "spelen_ei")
    public static void knabbelspelenEierlopen(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = begin(helper, npc, p, Onderdeel.EIERLOPEN);
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        helper.assertTrue(p.getMainHandItem().is(KnabbelspelenFeature.KNABBELEI_LEPEL.get()), "the spoon with the egg");
        Eierlopen.Staat s = Eierlopen.staat(d);
        Vec3 vlag = Eierlopen.vlag(w, d.baan, 0);
        p.snapTo(vlag.x, vlag.y, vlag.z);
        s.vorige = vlag;
        Eierlopen.SPEL.tick(w, d, p, level, 5);
        helper.assertTrue(s.vlag == 1, "the first flag counts");
        // a sprint: way too fast
        s.vorige = p.position().add(Speelvelden.veld(Onderdeel.EIERLOPEN).richting() * -0.6, 0, 0);
        for (int i = 0; i < 6 && s.gevallen == 0; i++) {
            s.vorige = p.position().add(0.6, 0, 0);
            Eierlopen.SPEL.tick(w, d, p, level, 6 + i);
        }
        helper.assertTrue(s.gevallen == 1, "the egg dropped");
        // 1.2.7: a calm walk that arrives in bursts (nothing one tick, two steps the next) is still a calm walk
        s.rust();
        s.wiebel = 0;
        for (int i = 0; i < 80; i++) {
            s.vorige = p.position().add(i % 2 == 0 ? 0 : 0.3, 0, 0);
            s.vorigeYaw = p.getYRot();
            Eierlopen.SPEL.tick(w, d, p, level, 20 + i);
        }
        helper.assertTrue(s.gevallen == 1 && s.wiebel < 0.3, "steps in bursts (0.15 a tick on average) don't drop the egg: " + s.gevallen + " / " + s.wiebel);
        double[] b = Speelvelden.baan(w.anker, Onderdeel.EIERLOPEN, d.baan, p.position());
        helper.assertTrue(Math.abs(b[0] - Speelvelden.EI_VLAGGEN[0]) < 0.6, "back at the last flag: u " + b[0]);
        weg(helper, p);
        helper.succeed();
    }

    /** Spijkerpoepen: the spijker swings after a jolt and calms down; crouched and still over the bottle: plonk. */
    @GuhTest(template = GEBOUW, timeoutTicks = 200, batch = "spelen_spijker")
    public static void knabbelspelenSpijkerpoepen(GameTestHelper helper) {
        // the pendulum on its own
        Spijkerpoepen.Staat t = new Spijkerpoepen.Staat();
        t.riem = t.riem1 = t.riem2 = Vec3.ZERO;
        Spijkerpoepen.slinger(t, new Vec3(0.3, 0, 0));
        helper.assertTrue(Math.abs(t.dx) > 0.05, "a jolt makes it swing: " + t.dx);
        for (int i = 0; i < 300; i++) {
            Spijkerpoepen.slinger(t, new Vec3(0.3, 0, 0));
        }
        helper.assertTrue(Math.hypot(t.dx, t.dz) < 0.02, "standing still it calms down");
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = begin(helper, npc, p, Onderdeel.SPIJKERPOEPEN);
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        Spijkerpoepen.Staat s = Spijkerpoepen.staat(d);
        helper.assertTrue(s.ding != null && s.ding.soort() == SpelDing.SPIJKER && s.ding.eigenaar() == p.getId(), "the spijker hangs from your belt");
        BlockPos fles = Spijkerpoepen.fles(w.anker, d.baan, 0);
        float yaw = Speelvelden.yaw(w.anker, Onderdeel.SPIJKERPOEPEN) + 180f;       // your back to the bottle
        double r = Math.toRadians(yaw);
        Vec3 achter = new Vec3(Math.sin(r), 0, -Math.cos(r)).scale(Spijkerpoepen.ACHTER);
        Vec3 plek = Vec3.atBottomCenterOf(fles).add(0, 1, 0).subtract(achter);
        p.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        p.setShiftKeyDown(true);
        p.setPose(Pose.CROUCHING);
        s.dx = s.dz = s.vx = s.vz = 0;
        s.riem = s.riem1 = s.riem2 = Spijkerpoepen.riem(p);
        helper.succeedWhen(() -> {
            p.snapTo(plek.x, plek.y, plek.z, yaw, 0);
            p.setShiftKeyDown(true);
            p.setPose(Pose.CROUCHING);
            helper.assertTrue(level.getBlockState(fles).getValue(KnabbelspelenBlocks.KaasmelkFles.VOL), "plonk: the first bottle is full");
            helper.assertTrue(s.fles == 1, "on to bottle 2");
            weg(helper, p);
        });
    }

    /** Guhguhtje prik: blindfolded and spun; right on the spot = 1000, the tail stays on the board, the blindfold comes off. */
    @GuhTest(template = GEBOUW, timeoutTicks = 200, batch = "spelen_prik")
    public static void knabbelspelenGuhguhtjePrik(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = begin(helper, npc, p, Onderdeel.GUHGUHTJE_PRIK);
        Wedstrijd.Deelnemer d = w.deelnemer(p);
        helper.assertTrue(p.hasEffect(MobEffects.BLINDNESS), "the blindfold is on");
        helper.assertTrue(p.getMainHandItem().is(KnabbelspelenFeature.GUHGUHTJE_STAARTJE.get()), "the tail in your hand");
        GuhguhtjePrik.Staat s = GuhguhtjePrik.staat(d);
        helper.assertTrue(s.draai == GuhguhtjePrik.DRAAI_TICKS, "spinning first");
        BlockPos bord = Speelvelden.blok(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Speelvelden.PRIK_BORD, 0, Speelvelden.G + 2);
        helper.assertTrue(!GuhguhtjePrik.prik(p, Vec3.atCenterOf(bord), bord), "no pinning while spinning");
        helper.runAfterDelay(GuhguhtjePrik.DRAAI_TICKS + 3, () -> {
            helper.assertTrue(s.draai == 0, "the spinning stopped");
            BlockPos naast = Speelvelden.blok(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Speelvelden.PRIK_BORD - 3, 0, Speelvelden.G);
            helper.assertTrue(!GuhguhtjePrik.prik(p, Vec3.atCenterOf(naast), naast), "the floor isn't the board");
            Vec3 doel = Speelvelden.punt(w.anker, Onderdeel.GUHGUHTJE_PRIK, d.baan, Speelvelden.PRIK_BORD - 0.5, 0, Speelvelden.PRIK_DOEL_Y);
            helper.assertTrue(GuhguhtjePrik.prik(p, doel, bord), "pinned");
            helper.assertTrue(d.klaar && d.score[Onderdeel.GUHGUHTJE_PRIK.ordinal()] == 1000, "right on the spot: 1000");
            helper.assertTrue(!p.hasEffect(MobEffects.BLINDNESS), "the blindfold is off");
            helper.assertTrue(s.staartje != null && s.staartje.isAlive() && s.staartje.soort() == SpelDing.STAARTJE, "the tail stays on the board");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** A friend joins: two players, each in their own lane, a zeskamp all the way to the lintjes, the winner's extra and the board. */
    @GuhTest(template = GEBOUW, timeoutTicks = 1600, batch = "spelen_zeskamp")
    public static void knabbelspelenZeskampMetVriend(GameTestHelper helper) {
        GuhNpcEntity npc = juf(helper);
        ServerPlayer p = speler(helper, npc);
        ServerPlayer q = speler(helper, npc);
        ServerLevel level = helper.getLevel();
        Wedstrijd w = Wedstrijd.start(npc, p, null);
        helper.assertTrue(w != null && w.zeskamp && w.programma.size() == 6, "the zeskamp opened");
        w.meedoen(npc, q);
        helper.assertTrue(w.deelnemers().size() == 2 && Wedstrijd.isPlaying(q), "a friend joined");
        w.meteen(level);
        Wedstrijd.Deelnemer dp = w.deelnemer(p), dq = w.deelnemer(q);
        helper.assertTrue(dp.baan == 0 && dq.baan == 1, "each in their own lane");
        helper.assertTrue(p.position().distanceTo(q.position()) > 4, "a lane apart");
        int[] scores = new int[Onderdeel.values().length];
        for (Onderdeel o : Onderdeel.values()) {
            scores[o.ordinal()] = o.tijd ? o.perfect : o.perfect;
        }
        helper.onEachTick(() -> {
            if (Wedstrijd.of(npc) == w && w.fase() == Wedstrijd.Fase.BEZIG) {
                Onderdeel o = w.onderdeel();
                if (!dp.klaar) {
                    w.klaar(dp, p, scores[o.ordinal()], false);
                }
                if (!dq.klaar) {
                    w.klaar(dq, q, o.tijd ? o.perfect * 4 : o.perfect / 4, false);
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(Wedstrijd.of(npc) == null, "the zeskamp is over");
            helper.assertTrue(dp.totaal() == 6000, "a perfect zeskamp: " + dp.totaal());
            helper.assertTrue(GuhQuests.count(p, KnabbelspelenFeature.SPELENLINTJE.get()) == Wedstrijd.zeskampLintjes(6000) + 1,
                    "zeskamp lintjes, +1 for the winner");
            helper.assertTrue(GuhQuests.count(q, KnabbelspelenFeature.SPELENLINTJE.get()) == Wedstrijd.zeskampLintjes(dq.totaal()), "the friend's lintjes");
            helper.assertTrue(Wedstrijd.bestZeskamp(p) == 6000, "the zeskamp record");
            helper.assertTrue(Scorebord.top(level.getServer(), Onderdeel.ZESKAMP_BOARD).stream().anyMatch(e -> e.name().equals(p.getGameProfile().name())),
                    "on the zeskamp board");
            helper.assertTrue(!Wedstrijd.isPlaying(p) && !Wedstrijd.isPlaying(q), "nobody is still playing");
            helper.assertTrue(!KnabbelspelenFeature.heeftGeleend(p) && !KnabbelspelenFeature.heeftGeleend(q), "every loaned thing went back");
            weg(helper, p, q);
        });
    }
}
