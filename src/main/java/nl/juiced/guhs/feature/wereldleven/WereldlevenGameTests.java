package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the wereldleven feature (2.8): the day rhythm (the morning yawn, the wave, the nap in a nest and on the
 * spot, the campfire with a marshmallow, the night and waking up), the kaasijsjes (on you and on a guh, the hat's flavour,
 * it wears off), IJscoguh Tingeling (his offers per season, nothing hurts him, the shop, the bell, riding off, guhs run
 * after him), the koortje (no song hides inside another, the xylofoon, the rewards +1, the koorstrikje, the fluitje), the
 * grijpmachine (a ticket, the claw, the prize + a kermisbon, the knuffelkast, one game at a time), tamed guhs cuddling a
 * plushie, and the data (tags, Knus sections, the plein piece and the kermis). Guhs of these tests take part through
 * Dagritme.TEST_MEE (on the GameTest server nobody else lives the guh day), the part of the day through TEST_DAGDEEL.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class WereldlevenGameTests {
    private static final String PLEIN = "wereldleven_test_plein", LEEG = "wereldleven_test_leeg", LANG = "wereldleven_test_lang";
    // template positions (a template block at (x, y, z) is at the helper's (x, y + 1, z))
    private static final BlockPos VUUR = new BlockPos(4, 2, 4), NEST = new BlockPos(16, 2, 4), XYLOFOON = new BlockPos(10, 2, 10),
            MACHINE = new BlockPos(16, 2, 16);

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** A guh of this test (it lives the guh day in this part of it). */
    private static GuhEntity guh(GameTestHelper helper, BlockPos at, Dagdeel dagdeel) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersonality(GuhPersonality.VADSIG);
        guh.setVariant(GuhVariant.NORMAL);
        Dagritme.TEST_MEE.add(guh.getUUID());
        Dagritme.TEST_DAGDEEL.put(guh.getUUID(), dagdeel);
        Dagritme.TEST_GEBIED.put(guh.getUUID(), helper.getBounds());
        return guh;
    }

    private static void vergeet(GuhEntity... guhs) {
        for (GuhEntity g : guhs) {
            Dagritme.TEST_MEE.remove(g.getUUID());
            Dagritme.TEST_DAGDEEL.remove(g.getUUID());
            Dagritme.TEST_GEBIED.remove(g.getUUID());
        }
    }

    /** What a guh is doing (for the messages of a test that waits in vain). */
    static String staat(GameTestHelper helper, GuhEntity guh) {
        StringBuilder b = new StringBuilder(" [at ").append(helper.relativePos(guh.blockPosition())).append(", goals:");
        guh.goalSelector.getAvailableGoals().stream().filter(w -> w.isRunning()).forEach(w -> b.append(' ').append(w.getGoal().getClass().getSimpleName()));
        return b.append(", emote ").append(guh.emotes.current()).append(", sits ").append(guh.isInSittingPose()).append(", nav done ")
                .append(guh.getNavigation().isDone()).append(", busy ").append(GuhHooks.isBezig(guh)).append(", doetMee ").append(Dagritme.doetMee(guh))
                .append(", fire ").append(Dagritme.kampvuur(guh) == null ? null : helper.relativePos(Dagritme.kampvuur(guh)))
                .append(", bounds ").append(helper.getBounds()).append(", abs ").append(guh.blockPosition())
                .append(']').toString();
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.server.getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    // =================================================================================================================
    // Dagritme
    // =================================================================================================================

    @GameTest(template = LEEG, timeoutTicks = 800)
    public static void wereldlevenDagritmeOchtendGapen(GameTestHelper helper) {
        GuhEntity guh = guh(helper, new BlockPos(6, 2, 6), Dagdeel.OCHTEND);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.getPersistentData().getLong(Dagritme.GAAP_DAG) == Dagritme.ritmeDag(guh.level()) + 1,
                        "a big yawn and a stretch in the morning"))
                .thenExecute(() -> helper.assertTrue(guh.emotes.current() == Emote.GAPEN, "the GAPEN emote: " + guh.emotes.current()))
                .thenExecute(() -> vergeet(guh))
                .thenSucceed();
    }

    @GameTest(template = PLEIN, timeoutTicks = 900, batch = "wereldleven_dutje")   // own batch: no neighbours around the nest
    public static void wereldlevenDagritmeDutjeInHetNest(GameTestHelper helper) {
        helper.setBlock(NEST, nl.juiced.guhs.feature.vadswoud.VadswoudFeature.GUHNESTJE.get());
        GuhEntity guh = guh(helper, new BlockPos(13, 2, 4), Dagdeel.DUTJE);
        BlockPos nest = helper.absolutePos(NEST);
        helper.assertTrue(nest.equals(Dagritme.nest(guh)), "it sees the guh nest: " + Dagritme.nest(guh));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!Dagritme.slaapt(guh) && guh.blockPosition().distSqr(nest) > 4 * 4) {   // (every tick: a stroll of 12+ blocks made it nap on the spot)
                        guh.moveTo(nest.getX() - 2.5, nest.getY(), nest.getZ() + 0.5);   // (it strolled off before its nap: back)
                    }
                    helper.assertTrue(Dagritme.slaapt(guh) && guh.emotes.current() == Emote.SLAPEN, "an afternoon nap: " + guh.emotes.current() + staat(helper, guh));
                })
                .thenExecute(() -> {
                    double d = guh.position().distanceTo(Vec3.atBottomCenterOf(nest));
                    helper.assertTrue(d < 1.6, "in the guh nest: " + String.format("%.1f", d) + " from it");
                    helper.assertTrue((guh.getKnusVlaggen() & Dagritme.DUTJE) == 0, "a real nest: no drawn nestje");
                    Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.DAG);
                })
                .thenWaitUntil(() -> helper.assertTrue(!Dagritme.slaapt(guh) && guh.emotes.current() != Emote.SLAPEN, "awake after the nap"))
                .thenExecute(() -> vergeet(guh))
                .thenSucceed();
    }

    @GameTest(template = LEEG, timeoutTicks = 1200, batch = "wereldleven_dagritme")   // (merge 3.0) own batch: no neighbours' nests around
    public static void wereldlevenDagritmeOpDePlekEnDeNacht(GameTestHelper helper) {
        GuhEntity guh = guh(helper, new BlockPos(6, 2, 6), Dagdeel.DUTJE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Dagritme.slaapt(guh) && (guh.getKnusVlaggen() & Dagritme.DUTJE) != 0,
                        "no nest around: it naps on the spot in a little nestje" + staat(helper, guh)))
                .thenExecute(() -> Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.NACHT))
                .thenWaitUntil(() -> helper.assertTrue(guh.getPersistentData().getString(Dagritme.SLAAP).equals("nacht")
                        && guh.emotes.current() == Emote.SLAPEN, "the night: zzz"))
                .thenExecute(() -> Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.OCHTEND))
                .thenWaitUntil(() -> helper.assertTrue(!Dagritme.slaapt(guh) && (guh.getKnusVlaggen() & Dagritme.DUTJE) == 0
                        && guh.emotes.current() == Emote.GAPEN, "morning: awake, with a yawn and a stretch (" + guh.emotes.current() + ")"))
                .thenExecute(() -> vergeet(guh))
                .thenSucceed();
    }

    @GameTest(template = PLEIN, timeoutTicks = 900)
    public static void wereldlevenDagritmeKampvuurMarshmallow(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(9, 2, 5));
        helper.setBlock(VUUR, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        GuhEntity guh = guh(helper, new BlockPos(9, 2, 8), Dagdeel.AVOND);
        BlockPos vuur = helper.absolutePos(VUUR);
        helper.assertTrue(vuur.equals(Dagritme.kampvuur(guh)) && Dagritme.plekBijVuur(guh, vuur) != null,
                "it sees the campfire: " + Dagritme.kampvuur(guh) + ", a spot: " + Dagritme.plekBijVuur(guh, vuur));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.isInSittingPose() && guh.getPersistentData().getBoolean(Dagritme.KAMPVUUR)
                        && guh.position().distanceTo(Vec3.atBottomCenterOf(vuur)) < 4.3, "sits at the campfire in the evening" + staat(helper, guh)))
                .thenExecute(() -> {
                    p.moveTo(guh.getX() + 1, guh.getY(), guh.getZ());
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 2));
                    helper.assertTrue(guh.interact(p, InteractionHand.MAIN_HAND).consumesAction(), "it takes the marshmallow");
                    helper.assertTrue(count(p, WereldlevenFeature.MARSHMALLOW_KNABBEL.get()) == 1, "one marshmallow less");
                    helper.assertTrue(KnusVoortgang.teller(p, WereldlevenVoortgang.MARSHMALLOWS) == 1 && advancement(p, "wereldleven_kampvuur"),
                            "counted, and the quest");
                    helper.assertTrue(new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get()).is(KnusTags.MARSHMALLOW), "in #guhs:knus/marshmallow");
                    Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.NACHT);
                })
                .thenWaitUntil(() -> helper.assertTrue(!guh.getPersistentData().getBoolean(Dagritme.KAMPVUUR), "bedtime: away from the fire"))
                .thenExecute(() -> {
                    vergeet(guh);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    /** A seat at the fire that one guh walks to is not picked by another guh (no two guhs in one spot). */
    @GameTest(template = PLEIN)
    public static void wereldlevenDagritmeKampvuurPlekGereserveerd(GameTestHelper helper) {
        helper.setBlock(VUUR, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        GuhEntity a = guh(helper, new BlockPos(9, 2, 8), Dagdeel.DAG);
        GuhEntity b = guh(helper, new BlockPos(9, 2, 9), Dagdeel.DAG);
        BlockPos vuur = helper.absolutePos(VUUR);
        try {
            BlockPos plek = Dagritme.plekBijVuur(a, vuur);
            helper.assertTrue(plek != null, "a spot at the fire");
            Dagritme.reserveer(a, plek, 200);
            helper.assertTrue(Dagritme.gereserveerd(b, plek) && !Dagritme.gereserveerd(a, plek), "taken, but not for itself");
            for (int i = 0; i < 40; i++) {
                BlockPos ander = Dagritme.plekBijVuur(b, vuur);
                helper.assertTrue(ander != null && !ander.equals(plek), "the other guh picks another spot: " + ander);
            }
            Dagritme.geefVrij(a, plek);
            helper.assertTrue(!Dagritme.gereserveerd(b, plek), "free again");
        } finally {
            vergeet(a, b);
        }
        helper.succeed();
    }

    /** A resident in its house walks all the way down the street to the town's campfire (much further than 16 blocks). */
    @GameTest(template = LANG, timeoutTicks = 2400)
    public static void wereldlevenDagritmeBewonerLooptNaarHetKampvuur(GameTestHelper helper) {
        BlockPos vuurRel = new BlockPos(36, 2, 5), huis = new BlockPos(4, 2, 5);
        helper.setBlock(vuurRel, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
        BlockPos vuur = helper.absolutePos(vuurRel);
        GuhEntity guh = guh(helper, huis, Dagdeel.AVOND);
        helper.assertTrue(Math.sqrt(guh.blockPosition().distSqr(vuur)) > Dagritme.VUUR_ZOEK + 8, "the fire is far away");
        helper.assertTrue(Dagritme.kampvuur(guh) == null, "a wandering guh doesn't see a fire that far away");
        GuhHooks.maakBewoner(guh, helper.absolutePos(huis));
        helper.assertTrue(vuur.equals(Dagritme.kampvuur(guh)), "a resident sees the town's campfire from its house: " + Dagritme.kampvuur(guh));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.isInSittingPose() && guh.getPersistentData().getBoolean(Dagritme.KAMPVUUR)
                        && Dagritme.aanHetVuur(guh) && guh.position().distanceTo(Vec3.atBottomCenterOf(vuur)) < 4.3,
                        "walked from its house to the campfire and sits there" + staat(helper, guh)))
                .thenExecute(() -> {
                    var range = guh.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                    helper.assertTrue(range != null && range.getValue() == range.getBaseValue(), "the long path is only for the walk there");
                    Dagritme.TEST_DAGDEEL.put(guh.getUUID(), Dagdeel.NACHT);
                })
                .thenWaitUntil(() -> helper.assertTrue(!guh.getPersistentData().getBoolean(Dagritme.KAMPVUUR), "bedtime: away from the fire"))
                .thenExecute(() -> vergeet(guh))
                .thenSucceed();
    }

    /** The campfire flag doesn't stay behind (a chunk that unloaded while the guh sat at the fire): no marshmallows anywhere. */
    @GameTest(template = LEEG, timeoutTicks = 200)
    public static void wereldlevenKampvuurVlagBlijftNietHangen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(3, 2, 6));
        GuhEntity guh = guh(helper, new BlockPos(6, 2, 6), Dagdeel.DAG);
        guh.getPersistentData().putBoolean(Dagritme.KAMPVUUR, true);   // (as saved with a guh that sat at the fire)
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 2));
        helper.assertFalse(guh.interact(p, InteractionHand.MAIN_HAND).consumesAction() && count(p, WereldlevenFeature.MARSHMALLOW_KNABBEL.get()) < 2,
                "no fire: no marshmallow");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!guh.getPersistentData().getBoolean(Dagritme.KAMPVUUR), "the old campfire flag is cleared"))
                .thenExecute(() -> helper.assertTrue(count(p, WereldlevenFeature.MARSHMALLOW_KNABBEL.get()) == 2, "the marshmallows stay in the bag"))
                .thenExecute(() -> {
                    vergeet(guh);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    @GameTest(template = LEEG, timeoutTicks = 600)
    public static void wereldlevenDagritmeZwaaien(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        GuhEntity guh = guh(helper, new BlockPos(9, 2, 9), Dagdeel.DAG);
        guh.tame(p);
        guh.setWandering(true);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        helper.startSequence()
                .thenExecute(() -> p.moveTo(guh.getX() + 2.5, guh.getY(), guh.getZ()))
                .thenWaitUntil(() -> helper.assertTrue(KnusVoortgang.teller(p, WereldlevenVoortgang.GEZWAAID) >= 1, "a tamed guh waves at you"))
                .thenExecute(() -> helper.assertTrue(advancement(p, "wereldleven_gezwaaid"), "the quest"))
                .thenExecute(() -> {
                    vergeet(guh);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // kaasijsjes
    // =================================================================================================================

    @GameTest(template = LEEG, timeoutTicks = 200)
    public static void wereldlevenKaasijsjes(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(2, 2, 2));
        GuhEntity guh = guh(helper, new BlockPos(4, 2, 2), Dagdeel.DAG);
        Kaasijsjes.eet(p, Kaasijsjes.Smaak.ROZE);
        helper.assertTrue(p.hasEffect(WereldlevenFeature.BLOSJES), "blosjes");
        Kaasijsjes.eet(p, Kaasijsjes.Smaak.CHOCO);
        helper.assertTrue(p.hasEffect(WereldlevenFeature.ZWEVERIG), "zweverig");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WereldlevenFeature.KAASIJSJES.get(Kaasijsjes.Smaak.MINT).get(), 2));
        helper.assertTrue(guh.interact(p, InteractionHand.MAIN_HAND).consumesAction(), "the guh takes the ice cream");
        helper.assertTrue(GuhHooks.heeft(guh, GuhHooks.IJSHOEDJE) && Kaasijsjes.hoedjeSmaak(guh) == Kaasijsjes.Smaak.MINT, "a mint ice-cream hat");
        helper.assertTrue(count(p, WereldlevenFeature.KAASIJSJES.get(Kaasijsjes.Smaak.MINT).get()) == 1, "one ice cream less");
        Kaasijsjes.geefGuh(guh, Kaasijsjes.Smaak.ROZE, p);
        helper.assertTrue(GuhHooks.heeft(guh, GuhHooks.BLOSJES) && guh.hasEffect(WereldlevenFeature.BLOSJES), "blushing cheeks");
        Kaasijsjes.geefGuh(guh, Kaasijsjes.Smaak.CHOCO, p);
        helper.assertTrue(guh.hasEffect(WereldlevenFeature.ZWEVERIG) && Kaasijsjes.hoedjeSmaak(guh) == Kaasijsjes.Smaak.CHOCO, "a floating guh, choco hat");
        helper.assertTrue(KnusVoortgang.teller(p, WereldlevenVoortgang.IJSJES) == 5, "five ice creams counted: " + KnusVoortgang.teller(p, WereldlevenVoortgang.IJSJES));
        helper.assertTrue(KnusVoortgang.ontdekt(p, WereldlevenVoortgang.IJSJES_BOEK).size() == 3 && advancement(p, "wereldleven_ijsje")
                && advancement(p, "knuffeldal/wereldleven_ijsje"), "three flavours in the ijsjes page, the advancement");
        for (Kaasijsjes.Smaak s : Kaasijsjes.Smaak.values()) {
            Kaasijsjes.eet(p, s);
        }
        helper.assertTrue(advancement(p, "wereldleven_alle_ijsjes"), "all seven flavours");
        // the hat and the cheeks wear off
        guh.getPersistentData().putLong(Kaasijsjes.HOEDJE_TOT, 0);
        guh.getPersistentData().putLong(Kaasijsjes.BLOSJES_TOT, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!GuhHooks.heeft(guh, GuhHooks.IJSHOEDJE) && !GuhHooks.heeft(guh, GuhHooks.BLOSJES)
                        && Kaasijsjes.hoedjeSmaak(guh) == null, "the hat and the cheeks are gone again"))
                .thenExecute(() -> {
                    vergeet(guh);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // IJscoguh Tingeling
    // =================================================================================================================

    @GameTest(template = LEEG, timeoutTicks = 200)
    public static void wereldlevenIJscoguhWinkelEnBel(GameTestHelper helper) {
        for (Seizoen s : Seizoen.values()) {
            MerchantOffers o = IJscoguhEntity.aanbod(s);
            Item seizoensijsje = WereldlevenFeature.KAASIJSJES.get(Kaasijsjes.Smaak.van(s)).get();
            helper.assertTrue(o.stream().anyMatch(offer -> offer.getResult().is(seizoensijsje)), "the " + s.id() + " ice cream in the " + s.id());
            for (Kaasijsjes.Smaak other : Kaasijsjes.Smaak.values()) {
                if (other.seizoen != null && other.seizoen != s) {
                    helper.assertTrue(o.stream().noneMatch(offer -> offer.getResult().is(WereldlevenFeature.KAASIJSJES.get(other).get())),
                            other.id() + " not in the " + s.id());
                }
            }
            for (MerchantOffer offer : o) {
                helper.assertTrue(offer.getCostA().is(ModItems.KAAS_KNABBELS.get()), "paid with kaasknabbels");
            }
        }
        ServerPlayer p = player(helper, new BlockPos(2, 2, 2));
        IJscoguhEntity ijsco = helper.spawn(WereldlevenFeature.IJSCOGUH.get(), new BlockPos(6, 2, 6));
        ijsco.setNoAi(true);
        float hp = ijsco.getHealth();
        helper.assertTrue(!ijsco.hurt(helper.getLevel().damageSources().playerAttack(p), 10f) && ijsco.getHealth() == hp, "nothing hurts him");
        int bel = ijsco.belTeller();
        ijsco.bel();
        helper.assertTrue(ijsco.belTeller() == bel + 1, "tingeling!");
        p.moveTo(ijsco.getX() + 1.5, ijsco.getY(), ijsco.getZ());
        helper.assertTrue(ijsco.interact(p, InteractionHand.MAIN_HAND).consumesAction() && ijsco.getTradingPlayer() == p, "his shop opens");
        helper.assertTrue(KnusVoortgang.teller(p, WereldlevenVoortgang.IJSCOGUH) == 1 && advancement(p, "wereldleven_ijscoguh"), "met him");
        p.closeContainer();
        helper.assertTrue(GuhVariant.IJSCOGUH.isCharacter() && GuhVariant.IJSCOGUH.npcKind() == null, "a creature page in the Guhdex");
        ijsco.setBlijft(3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(ijsco.isRemoved(), "he rides off after his day"))
                .thenExecute(() -> leave(helper, p))
                .thenSucceed();
    }

    @GameTest(template = LEEG, timeoutTicks = 600)
    public static void wereldlevenGuhsRennenDeIJscokarAchterna(GameTestHelper helper) {
        // (in the middle of the floor, facing south: the guhs' spot behind the cart is on the floor, whatever side it takes)
        IJscoguhEntity ijsco = helper.spawn(WereldlevenFeature.IJSCOGUH.get(), new BlockPos(6, 2, 7));
        ijsco.setNoAi(true);
        ijsco.setYRot(0f);
        ijsco.setYBodyRot(0f);
        ijsco.setYHeadRot(0f);
        GuhEntity guh = guh(helper, new BlockPos(11, 2, 2), Dagdeel.DAG);
        helper.startSequence()
                // (its spot is 2.6 behind the cart and up to 1.4 to the side; it stops within 1.4 of it: from 7 blocks to under 5)
                .thenWaitUntil(() -> helper.assertTrue(guh.distanceTo(ijsco) < 5.0, "the guh runs after the cart: " + String.format("%.1f", guh.distanceTo(ijsco))
                        + staat(helper, guh)))
                .thenExecute(() -> {
                    ijsco.discard();
                    vergeet(guh);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // het koortje
    // =================================================================================================================

    @GameTest(template = EMPTY_TEMPLATE)
    public static void wereldlevenLiedjesVerstoppenZichNiet(GameTestHelper helper) {
        for (Koortje.Liedje lied : Koortje.Liedje.values()) {
            Deque<Integer> gespeeld = new ArrayDeque<>();
            for (int i = 0; i < lied.noten.length; i++) {
                gespeeld.addLast(lied.noten[i]);
                Koortje.Liedje af = Koortje.herken(gespeeld);
                if (i < lied.noten.length - 1) {
                    helper.assertTrue(af == null, lied.id() + ": after " + (i + 1) + " notes already " + af);
                } else {
                    helper.assertTrue(af == lied, lied.id() + ": recognised at the end");
                }
            }
            for (int n : lied.noten) {
                helper.assertTrue(n >= 0 && n < Koortje.NOTEN, lied.id() + ": a bar that exists");
            }
        }
        helper.assertTrue(Koortje.Liedje.TOONLADDER.beloning == 2 + 1, "every reward +1");
        helper.assertTrue(Koortje.pitch(0) < Koortje.pitch(7) && Koortje.pitch(0) >= 0.5f && Koortje.pitch(7) <= 2f, "the bars go up");
        helper.succeed();
    }

    private static final String EMPTY_TEMPLATE = "empty";

    @GameTest(template = PLEIN, timeoutTicks = 200)
    public static void wereldlevenKoortjeOpDeXylofoon(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(10, 2, 13));
        BlockPos xylo = helper.absolutePos(XYLOFOON);
        GuhEntity a = guh(helper, new BlockPos(8, 2, 10), Dagdeel.DAG), b = guh(helper, new BlockPos(12, 2, 11), Dagdeel.DAG);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(a.onGround() && b.onGround(), "the guhs stand"))
                .thenExecute(() -> {
                    Koortje.vergeet(p);
                    helper.assertTrue(helper.getLevel().getBlockState(xylo).is(WereldlevenFeature.GUH_XYLOFOON.get()), "the xylofoon");
                    Koortje.Liedje af = null;
                    for (int n : Koortje.Liedje.TOONLADDER.noten) {
                        af = Koortje.noot(p, xylo, n);
                    }
                    helper.assertTrue(af == Koortje.Liedje.TOONLADDER, "the toonladder");
                    helper.assertTrue(a.emotes.current() == Emote.ZINGEN && b.emotes.current() == Emote.ZINGEN, "the guhs sing along");
                    helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == Koortje.Liedje.TOONLADDER.beloning, "the reward");
                    helper.assertTrue(KnusVoortgang.heeft(p, WereldlevenVoortgang.LIEDJESBOEK, "toonladder")
                            && KnusVoortgang.teller(p, WereldlevenVoortgang.KOORTJES) == 1 && advancement(p, "knuffeldal/wereldleven_koortje"),
                            "in the liedjesboek, a koortje");
                    for (int n : Koortje.Liedje.TOONLADDER.noten) {
                        Koortje.noot(p, xylo, n);
                    }
                    helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == Koortje.Liedje.TOONLADDER.beloning
                            && KnusVoortgang.teller(p, WereldlevenVoortgang.KOORTJES) == 2, "again: a koortje, no new reward");
                    for (Koortje.Liedje lied : Koortje.Liedje.values()) {
                        for (int n : lied.noten) {
                            Koortje.noot(p, xylo, n);
                        }
                    }
                    helper.assertTrue(KnusVoortgang.ontdekt(p, WereldlevenVoortgang.LIEDJESBOEK).size() == Koortje.Liedje.values().length,
                            "all six songs");
                    helper.assertTrue(count(p, ModItems.clothingItem(GuhClothes.KOORSTRIKJE)) == 1 && advancement(p, "knuffeldal/wereldleven_liedjesboek"),
                            "the koorstrikje");
                    helper.assertTrue(Koortje.noot(p, xylo.above(), 0) == null, "no xylofoon there: nothing");
                    // the fluitje: the guhs sing along with that too
                    a.emotes.stop();
                    b.emotes.stop();
                    p.moveTo(a.getX() + 1, a.getY(), a.getZ());
                    Koortje.fluit(p);
                    helper.assertTrue(a.emotes.current() == Emote.ZINGEN && advancement(p, "wereldleven_fluitje"), "they sing with the fluitje");
                })
                .thenExecute(() -> {
                    vergeet(a, b);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    // =================================================================================================================
    // de grijpmachine
    // =================================================================================================================

    @GameTest(template = PLEIN, timeoutTicks = 200)
    public static void wereldlevenGrijpmachine(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(16, 2, 14));
        BlockPos pos = helper.absolutePos(MACHINE);
        try {
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof GrijpmachineBlockEntity, "the machine and its block entity");
            helper.assertTrue(helper.getLevel().getBlockState(pos.above()).getValue(GrijpmachineBlock.HALF) == DoubleBlockHalf.UPPER, "two blocks tall");
            helper.assertTrue(helper.getLevel().getBlockEntity(pos.above()) instanceof GrijpmachineBlockEntity boven && boven.isBoven()
                    && boven.prijzen().isEmpty(), "the upper half has an empty block entity (no 'failed to load' warning from structures)");
            GrijpmachineBlockEntity machine = (GrijpmachineBlockEntity) helper.getLevel().getBlockEntity(pos);
            Grijpmachine.start(p, pos);
            helper.assertTrue(!Grijpmachine.speelt(p), "no ticket: no game");
            helper.assertTrue(new ItemStack(ModItems.KERMISBON.get()).is(KnusTags.GRIJPTICKETS), "a kermisbon is a ticket");
            p.getInventory().add(new ItemStack(ModItems.KERMISBON.get(), 30));
            Grijpmachine.start(p, pos);
            helper.assertTrue(Grijpmachine.speelt(p) && count(p, ModItems.KERMISBON.get()) == 29 && Minigames.GRIJPMACHINE.equals(Minigames.playing(p)),
                    "one ticket: the claw is yours");
            helper.assertTrue(machine.prijzen().size() == Grijpmachine.PRIJZEN, "a full case: " + machine.prijzen().size());
            Grijpmachine.start(p, pos);
            helper.assertTrue(count(p, ModItems.KERMISBON.get()) == 29, "clicking again doesn't cost another ticket");
            Grijpmachine.Uitslag uitslag = null;
            int beurten = 1;
            for (int i = 0; i < 30; i++) {
                GrijpmachineBlockEntity.Prijs doel = machine.prijzen().get(0);
                uitslag = Grijpmachine.grijp(p, pos, doel.x(), doel.z());
                helper.assertTrue(uitslag != null && uitslag.index() >= 0 && uitslag.knuffel().equals(doel.knuffel()), "the claw closes on it");
                if (uitslag.gepakt()) {
                    break;
                }
                Grijpmachine.start(p, pos);
                beurten++;
            }
            helper.assertTrue(uitslag != null && uitslag.gepakt(), "caught one (at last)");
            Block knuffel = WereldlevenFeature.knuffel(uitslag.knuffel());
            helper.assertTrue(knuffel != null && count(p, knuffel.asItem()) == 1, "the plushie is yours");
            helper.assertTrue(count(p, ModItems.KERMISBON.get()) == 30 - beurten + Grijpmachine.BON_ERBIJ && Grijpmachine.BON_ERBIJ == 1,
                    "and a kermisbon back (+1)");
            helper.assertTrue(KnusVoortgang.heeft(p, WereldlevenVoortgang.KNUFFELKAST, uitslag.knuffel()) && KnusVoortgang.teller(p, WereldlevenVoortgang.GRIJPEN) == 1
                    && advancement(p, "knuffeldal/wereldleven_grijpmachine"), "in the knuffelkast");
            helper.assertTrue(machine.prijzen().size() == Grijpmachine.PRIJZEN && !Grijpmachine.speelt(p), "a new plushie in the case, the turn is over");
            helper.assertTrue(Grijpmachine.grijp(p, pos, 0.5f, 0.5f) == null, "no turn: no claw");
            // grabbing next to everything, the glitter plushie's weaker grip
            List<GrijpmachineBlockEntity.Prijs> lijst = List.of(new GrijpmachineBlockEntity.Prijs("mint", 0.3f, 0.3f),
                    new GrijpmachineBlockEntity.Prijs(WereldlevenFeature.GLITTER, 0.7f, 0.7f));
            helper.assertTrue(Grijpmachine.onderKlauw(lijst, 0.9f, 0.1f) == -1 && Grijpmachine.onderKlauw(lijst, 0.32f, 0.3f) == 0, "under the claw");
            helper.assertTrue(Grijpmachine.greep(lijst.get(1), 0.7f, 0.7f) < Grijpmachine.greep(lijst.get(0), 0.3f, 0.3f), "the glitter one slips easier");
            // closing the screen ends the turn; creative plays for free
            Grijpmachine.start(p, pos);
            Grijpmachine.stop(p, pos);
            helper.assertTrue(!Grijpmachine.speelt(p), "stopped");
            p.setGameMode(GameType.CREATIVE);
            int bonnen = count(p, ModItems.KERMISBON.get());
            Grijpmachine.start(p, pos);
            helper.assertTrue(Grijpmachine.speelt(p) && count(p, ModItems.KERMISBON.get()) == bonnen, "creative: free");
            Grijpmachine.stop(p, pos);
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the plushies
    // =================================================================================================================

    @GameTest(template = LEEG, timeoutTicks = 1600)
    public static void wereldlevenTammeGuhKnuffeltKnuffel(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(8, 2, 6));
        helper.setBlock(new BlockPos(6, 2, 6), WereldlevenFeature.knuffel("pluisguh").defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        GuhEntity guh = guh(helper, new BlockPos(3, 2, 6), Dagdeel.DAG);
        guh.tame(p);
        guh.setWandering(true);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlockPos knuffel = helper.absolutePos(new BlockPos(6, 2, 6));
                    if (guh.blockPosition().distSqr(knuffel) > 7 * 7 && guh.getNavigation().isDone()) {
                        guh.moveTo(knuffel.getX() - 2.5, knuffel.getY(), knuffel.getZ() + 0.5);   // (it strolled off: back to the plushie)
                    }
                    helper.assertTrue(KnusVoortgang.teller(p, WereldlevenVoortgang.KNUFFELS) >= 1, "the tamed guh cuddles the plushie" + staat(helper, guh));
                })
                .thenExecute(() -> helper.assertTrue(advancement(p, "wereldleven_knuffel")
                        && guh.getPersistentData().getLong(Knuffels.KNUFFEL_TOT) > helper.getLevel().getGameTime(), "the quest; a rest before the next hug"))
                .thenExecute(() -> {
                    vergeet(guh);
                    leave(helper, p);
                })
                .thenSucceed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void wereldlevenKnuffelkastEnData(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        try {
            List<String> ids = WereldlevenFeature.KNUFFEL_IDS;
            // (2.9: + the Pinguh)
            helper.assertTrue(ids.size() == 22 && ids.get(ids.size() - 1).equals(WereldlevenFeature.GLITTER) && ids.contains("pluisguh")
                    && ids.contains("pinguh"), "21 variants + glitter: " + ids);
            int echte = 0;
            for (GuhVariant v : GuhVariant.values()) {
                if (!v.isCharacter() && !v.isVerhaalGuh()) {   // (3.0: the story guhs have no plush)
                    echte++;
                    helper.assertTrue(ids.contains(v.id()), "a plushie for " + v.id());
                }
            }
            helper.assertTrue(echte == 21, "21 real variants");
            for (String id : ids) {
                Block b = WereldlevenFeature.knuffel(id);
                helper.assertTrue(b != null && b.defaultBlockState().is(KnusTags.KNUFFELS) && WereldlevenFeature.knuffelId(b).equals(id), id + " in #guhs:knus/knuffels");
                helper.assertTrue(helper.getLevel().getPoiManager() != null && WereldlevenFeature.KNUFFEL_POI.get().is(b.defaultBlockState()),
                        id + " is a point of interest");
                Knuffels.ontdek(p, new ItemStack(b));
            }
            helper.assertTrue(KnusVoortgang.ontdekt(p, WereldlevenVoortgang.KNUFFELKAST).size() == 22 && advancement(p, "knuffeldal/wereldleven_knuffelkast_vol"),
                    "the full knuffelkast");
            // the Knus section
            helper.assertTrue(KnusVoortgang.mijlpalen(WereldlevenVoortgang.ONDERDEEL).size() >= 4, "milestones");
            helper.assertTrue(KnusVoortgang.verzameling(WereldlevenVoortgang.KNUFFELKAST).items().size() == 22
                    && KnusVoortgang.verzameling(WereldlevenVoortgang.LIEDJESBOEK).items().size() == 6
                    && KnusVoortgang.verzameling(WereldlevenVoortgang.IJSJES_BOEK).items().size() == 7, "the three collections");
            helper.assertTrue(p.server.getAdvancements().get(Guhs.id("quest/seen_ijscoguh")) != null
                    && GuhDex.CREATURE_RANGE > 0, "the IJscoguh's Guhdex page advancement");
            // the plein's grijpmachine piece and the kermis
            var templates = helper.getLevel().getStructureManager();
            for (String t : new String[] {"knuffeldal_stadje/grijpmachine", "guh_kermis"}) {
                var template = templates.get(Guhs.id(t));
                helper.assertTrue(template.isPresent(), "template " + t);
                var infos = template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), WereldlevenFeature.GRIJPMACHINE.get());
                helper.assertTrue(infos.size() == 2 && infos.stream().anyMatch(i -> i.state().getValue(GrijpmachineBlock.HALF) == DoubleBlockHalf.UPPER),
                        t + ": one grijpmachine (two halves)");
            }
            helper.assertTrue(ModItems.clothingItem(GuhClothes.IJSCOPETJE) != null && GuhClothes.KOORSTRIKJE.slot == GuhClothes.Slot.NECK, "the clothes");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }
}
