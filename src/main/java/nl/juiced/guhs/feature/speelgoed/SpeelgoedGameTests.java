package nl.juiced.guhs.feature.speelgoed;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Favorieten;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the speelgoed (2.10): the four toy kinds are registered (and are the favourite-toy candidates); a guh
 * pushes the knabbelbal until the knabbel pops out and eats it; players kick and refill the ball; guhs climb and slide
 * down the glijbaantje; a guh on the schommel swings, a push makes it swing higher and gives hearts, its ride ends by
 * itself with its hearts; two guhs sit on the wip together; tunnel pieces join up (entrances at the ends), a guh runs
 * through, hides and is found by knocking; a huisje resident plays with a toy near its home. Playing always gives
 * SPEELGOED hearts (never takes any), the SPEELTJE moment, the SPEELTJES stat and "eerste speeltje".
 * (Templates speelgoed_test_tuin: 20 x 20 grass at y 0; speelgoed_test_speelkamer: 14 x 14 with a wool wall.)
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class SpeelgoedGameTests {
    private static final String TUIN = "speelgoed_test_tuin", KAMER = "speelgoed_test_speelkamer";
    private static final String BATCH = "speelgoed";
    /** Every SPEELTJE moment (guh id, toy). */
    static final List<String> MOMENTEN = new CopyOnWriteArrayList<>();

    static {
        Band.opMoment((guh, speler, m, waarde) -> {
            if (m == Moment.SPEELTJE) {
                MOMENTEN.add(guh.getUUID() + ":" + waarde);
            }
        });
    }

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static GuhEntity guh(GameTestHelper helper, ServerPlayer owner, BlockPos at, boolean speelt) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(owner);
        if (speelt) {
            SpeelGoal.TEST_ALTIJD.add(guh.getUUID());
        }
        return guh;
    }

    static long speeltjes(GuhEntity guh) {
        return Dagboek.stat(guh.getServer(), guh.getOwnerUUID(), guh.getUUID(), DagboekStat.SPEELTJES);
    }

    static boolean moment(GuhEntity guh, String speeltje) {
        return MOMENTEN.contains(guh.getUUID() + ":" + speeltje);
    }

    static void klaar(GameTestHelper helper, GuhEntity guh, ServerPlayer... players) {
        SpeelGoal.TEST_ALTIJD.remove(guh.getUUID());
        weg(helper, players);
    }

    // =====================================================================================================================

    @GameTest(template = TUIN, batch = BATCH)
    public static void speelgoedVierSoortenGeregistreerd(GameTestHelper helper) {
        Set<String> ids = Speelgoed.alle().stream().map(s -> s.id()).collect(Collectors.toSet());
        helper.assertTrue(ids.containsAll(Spelen.SPEELTJES), "the four toy kinds are registered: " + ids);
        helper.assertTrue(Set.copyOf(Favorieten.SPEELTJES).equals(Set.copyOf(Spelen.SPEELTJES)), "they are the favourite toy candidates");
        helper.succeed();
    }

    @GameTest(template = KAMER, batch = BATCH, timeoutTicks = 1400)
    public static void speelgoedKnabbelbalTotDeKnabbelEruitRolt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhEntity guh = guh(helper, p, new BlockPos(4, 2, 4), true);
        KnabbelbalEntity bal = KnabbelbalEntity.maak(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 2, 8))), true);
        Vec3 start = bal.position();
        int[] hartjes = {0};
        helper.onEachTick(() -> {
            int h = Band.hartjes(guh);
            helper.assertTrue(h >= hartjes[0], "hearts never go down");
            hartjes[0] = h;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(bal.position().distanceToSqr(start) > 0.5, "the ball got pushed around: guh " + guh.position() + " ball " + bal.position()
                    + " sessie " + sessie(guh) + " bezig " + nl.juiced.guhs.feature.knus.GuhHooks.isBezig(guh) + " nav " + guh.getNavigation().isDone());
            helper.assertFalse(bal.isVol(), "the knabbel popped out");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16),
                    i -> i.getItem().is(ModItems.KAAS_KNABBELS.get())).isEmpty(), "and the guh ate it");
            helper.assertTrue(speeltjes(guh) >= 1 && moment(guh, "knabbelbal"), "counted as playing (stat + SPEELTJE moment)");
            helper.assertTrue(Band.hartjes(guh) > 0, "playing gives hearts");
            helper.assertTrue(nl.juiced.guhs.feature.band.BandData.get(guh.getServer()).vind(p.getUUID(), guh.getUUID()).heeftEerste("eerste_speeltje"),
                    "eerste speeltje in the dagboek");
            klaar(helper, guh, p);
        });
    }

    @GameTest(template = KAMER, batch = BATCH)
    public static void speelgoedSchoppenEnVullen(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(3, 2, 3));
        GuhEntity guh = guh(helper, p, new BlockPos(9, 2, 9), false);
        KnabbelbalEntity bal = KnabbelbalEntity.maak(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(5, 2, 5))), false);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, bal.position());
        bal.skipAttackInteraction(p);
        helper.assertTrue(bal.getDeltaMovement().horizontalDistance() > 0.3, "a kick sends it rolling");
        helper.assertTrue(guh.getPersistentData().getLong(Spelen.BAL_TOT) > helper.getLevel().getGameTime(), "your guh wants to chase it");
        // an empty ball, filled with a kaasknabbel
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
        bal.interact(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(bal.isVol() && p.getMainHandItem().getCount() == 1, "filled with one kaasknabbel");
        // picked up full, the item remembers
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setShiftKeyDown(true);
        bal.interact(p, InteractionHand.MAIN_HAND);
        helper.assertTrue(bal.isRemoved(), "sneak + right-click picks it up");
        ItemStack item = p.getInventory().items.stream().filter(s -> s.is(SpeelgoedFeature.KNABBELBAL_ITEM.get())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!item.isEmpty() && KnabbelbalItem.isVol(item), "the ball item, still full");
        klaar(helper, guh, p);
        helper.succeed();
    }

    @GameTest(template = TUIN, batch = BATCH, timeoutTicks = 1200)
    public static void speelgoedGlijbaantjeKlimmenEnGlijden(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos pos = helper.absolutePos(new BlockPos(10, 2, 10));
        ToestelBlock.bouw(helper.getLevel(), pos, SpeelgoedFeature.GLIJBAANTJE.get(), Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockState(pos.north()).getBlock() instanceof SpeelDeelBlock
                && helper.getLevel().getBlockState(pos.south().above()).getBlock() instanceof SpeelDeelBlock, "the glijbaantje takes up 1 x 2 x 3 blocks");
        GuhEntity guh = guh(helper, p, new BlockPos(6, 2, 13), true);
        boolean[] opGeklommen = {false};
        helper.onEachTick(() -> {
            if (guh.getVehicle() instanceof ZitjeEntity z) {
                opGeklommen[0] = true;
                helper.assertTrue(guh.getY() < pos.getY() + 1.6, "never higher than the platform");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(opGeklommen[0], "the guh climbed on: guh " + guh.position() + " pos " + pos + " sessie " + sessie(guh));
            helper.assertFalse(guh.isPassenger(), "and slid down and got off");
            helper.assertTrue(speeltjes(guh) >= 1 && moment(guh, "glijbaantje"), "counted as playing");
            // a player can slide too
            helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            helper.assertTrue(p.getVehicle() instanceof ZitjeEntity, "right-click: you climb on yourself");
            p.stopRiding();
            klaar(helper, guh, p);
        });
    }

    @GameTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void speelgoedSchommelDuwtje(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 7));
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(10, 2, 10));
        ToestelBlock.bouw(level, pos, SpeelgoedFeature.SCHOMMEL.get(), Direction.NORTH);
        GuhEntity guh = guh(helper, p, new BlockPos(10, 2, 8), false);
        ZitjeEntity z = ZitjeEntity.zet(level, pos, 0, guh, 60, 1);
        helper.assertTrue(z != null && guh.getVehicle() == z, "the guh sits on the schommel");
        helper.assertTrue(ToestelBlock.zitje(level, pos, 0) == z && SpeelgoedFeature.SCHOMMEL.get().vrijePlek(level, pos) < 0, "its only seat is taken");
        ToestelBlockEntity be = (ToestelBlockEntity) level.getBlockEntity(pos);
        helper.runAfterDelay(5, () -> {
            float voor = be.amplitude(level.getGameTime());
            helper.assertTrue(voor > 0.3f, "a guh swings by itself: " + voor);
            int hartjes = Band.hartjes(guh);
            helper.assertTrue(SpeelgoedFeature.SCHOMMEL.get().duw(level, pos, p), "a push");
            helper.assertTrue(be.amplitude(level.getGameTime()) > voor, "higher!");
            helper.assertTrue(Band.hartjes(guh) > hartjes, "the owner's push gives a heart");
            helper.succeedWhen(() -> {
                helper.assertFalse(guh.isPassenger(), "the ride ends by itself");
                helper.assertTrue(z.isRemoved(), "the seat is gone");
                helper.assertTrue(speeltjes(guh) >= 1 && moment(guh, "wip_schommel"), "counted as playing");
                klaar(helper, guh, p);
            });
        });
    }

    @GameTest(template = TUIN, batch = BATCH, timeoutTicks = 900)
    public static void speelgoedSamenOpDeWip(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos pos = helper.absolutePos(new BlockPos(10, 2, 10));
        ToestelBlock.bouw(helper.getLevel(), pos, SpeelgoedFeature.WIP.get(), Direction.EAST);
        GuhEntity a = guh(helper, p, new BlockPos(6, 2, 6), true);
        GuhEntity b = guh(helper, p, new BlockPos(14, 2, 14), true);
        helper.succeedWhen(() -> {
            helper.assertTrue(SpeelgoedFeature.WIP.get().bezet(helper.getLevel(), pos) == 2, "two guhs on the wip together");
            helper.assertTrue(a.getVehicle() instanceof ZitjeEntity && b.getVehicle() instanceof ZitjeEntity, "both sitting");
            ZitjeEntity za = (ZitjeEntity) a.getVehicle(), zb = (ZitjeEntity) b.getVehicle();
            helper.assertTrue(za.plek() != zb.plek(), "one on each end");
            za.klaar(false);
            zb.klaar(false);
            klaar(helper, a);
            klaar(helper, b, p);
        });
    }

    @GameTest(template = TUIN, batch = BATCH, timeoutTicks = 1200)
    public static void speelgoedTunnelVerstoppertje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos[] stukken = new BlockPos[4];
        for (int i = 0; i < 4; i++) {
            stukken[i] = helper.absolutePos(new BlockPos(7 + i, 2, 10));
            BlockState s = SpeelgoedFeature.TUNNEL.get().defaultBlockState().setValue(TunnelBlock.AXIS, Direction.Axis.X);
            level.setBlock(stukken[i], TunnelBlock.vorm(s, level, stukken[i]), 3);
        }
        for (BlockPos q : stukken) {
            level.setBlock(q, TunnelBlock.vorm(level.getBlockState(q), level, q), 3);
        }
        helper.assertTrue(TunnelBlock.kant(level.getBlockState(stukken[0]), Direction.WEST) == TunnelBlock.Kant.INGANG
                && TunnelBlock.kant(level.getBlockState(stukken[3]), Direction.EAST) == TunnelBlock.Kant.INGANG, "entrances at both ends");
        helper.assertTrue(TunnelBlock.kant(level.getBlockState(stukken[1]), Direction.WEST) == TunnelBlock.Kant.OPEN
                && TunnelBlock.kant(level.getBlockState(stukken[1]), Direction.EAST) == TunnelBlock.Kant.OPEN
                && TunnelBlock.kant(level.getBlockState(stukken[1]), Direction.NORTH) == TunnelBlock.Kant.DICHT, "joined in the middle, fur on the sides");
        helper.assertTrue(TunnelBlock.netwerk(level, stukken[2], 64).size() == 4, "one tunnel of four pieces");
        GuhEntity guh = guh(helper, p, new BlockPos(4, 2, 13), true);
        boolean[] gevonden = {false};
        helper.onEachTick(() -> {
            TunnelSpel s = TunnelSpel.verstopt(guh);
            if (s != null && !gevonden[0]) {
                helper.assertTrue(guh.noPhysics, "inside the tunnel");
                BlockPos klop = stukken[1];
                level.getBlockState(klop).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(klop), Direction.UP, klop, false));
                helper.assertTrue(s.isGevonden(), "knock knock: found!");
                gevonden[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gevonden[0], "the guh hid in the tunnel and was found: guh " + guh.position() + " sessie " + sessie(guh)
                    + (sessie(guh) instanceof TunnelSpel t ? " fase " + t.fase() : ""));
            helper.assertFalse(guh.noPhysics, "it came out again");
            helper.assertTrue(speeltjes(guh) >= 1 && moment(guh, "tunnel"), "counted as playing");
            klaar(helper, guh, p);
        });
    }

    @GameTest(template = TUIN, batch = BATCH, timeoutTicks = 1400)
    public static void speelgoedBewonerSpeeltBijZijnHuisje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(18, 2, 18));
        Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(3, 2, 3)), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
        GuhEntity guh = guh(helper, p, new BlockPos(6, 2, 8), false);
        helper.assertTrue(Huisjes.trekIn(h, guh), "a resident");
        KnabbelbalEntity.maak(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(9, 2, 9))), true);
        // the huisje asks the toys for something to play with near home (what HuisjeGoal does, now and then, at random)
        KlusTaak taak = Speelgoed.willekeurig(level, guh, h.pos(), Huisjes.BEREIK);
        helper.assertTrue(taak instanceof KnabbelbalSpel, "a toy near the huisje: " + taak);
        helper.assertTrue(Speeltjes.voor(level, guh, "glijbaantje", h.pos(), Huisjes.BEREIK) == null, "no glijbaantje around: nothing for that one");
        int[] ticks = {0};
        boolean[] bezig = {true};
        helper.onEachTick(() -> {
            if (bezig[0]) {
                bezig[0] = taak.tick() && ++ticks[0] < taak.maxTicks();
                if (!bezig[0]) {
                    taak.stop();
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertFalse(bezig[0], "the play session ended");
            helper.assertTrue(speeltjes(guh) >= 1, "the resident played with the knabbelbal");
            helper.assertTrue(Huisjes.isBewoner(guh), "and still lives in its huisje");
            weg(helper, p);
        });
    }

    /** (tests) a guh's play session right now, from SpeelGoal. */
    static KlusTaak sessie(GuhEntity guh) {
        for (var w : guh.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof SpeelGoal g) {
                return g.taak();
            }
        }
        return null;
    }

    static UUID id(Entity e) {
        return e.getUUID();
    }
}
