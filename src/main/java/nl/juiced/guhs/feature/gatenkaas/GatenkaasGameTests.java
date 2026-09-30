package nl.juiced.guhs.feature.gatenkaas;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of the gatenkaas caves: the sensors hear walking, chewing and digging but not sneaking or guhs:stil; the
 * third warning wakes the Vadswaker; he hears you chew, never breaks blocks and digs himself back in; stalactites fall
 * when their ceiling goes; the lore book and the two templates (with their geometry checked in gatenkaas.py).
 * (Every noise test has its own batch: a sensor of one test must not hear the player of another.)
 */
public class GatenkaasGameTests {
    private static final String EMPTY = "empty";

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                if (x + z > 0) {                        // (not on the test's own structure block)
                    helper.setBlock(x, 0, z, Blocks.STONE);
                }
            }
        }
    }

    private static ServerPlayer survivor(GameTestHelper helper, int x, int z) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(x, 1, z));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.setOnGround(true);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static boolean active(GameTestHelper helper, BlockPos rel) {
        BlockState s = helper.getBlockState(rel);
        return s.is(GatenkaasFeature.KNABBELSENSOR.get()) && s.getValue(KnabbelsensorBlock.ACTIVE);
    }

    private static void resetSensor(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, GatenkaasFeature.KNABBELSENSOR.get().defaultBlockState());
    }

    /** Walking and digging light up a sensor; creative players, sneaking steps and guhs:stil don't. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_1")
    public static void sensorsHearYouUnlessYouAreStil(GameTestHelper helper) {
        floor(helper);
        BlockPos sensor = new BlockPos(3, 1, 3);
        helper.setBlock(sensor, GatenkaasFeature.KNABBELSENSOR.get());
        ServerPlayer player = survivor(helper, 1, 1);
        BlockPos at = player.blockPosition();

        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
        helper.assertTrue(active(helper, sensor), "a sensor hears you dig");
        resetSensor(helper, sensor);

        // walking: two ticks of the step check, the player moved in between (a sneaking one makes no sound)
        player.setShiftKeyDown(true);
        step(player, 0.8);
        helper.assertTrue(!active(helper, sensor), "sneaking is safe");
        player.setShiftKeyDown(false);
        step(player, 0.8);
        helper.assertTrue(active(helper, sensor), "walking is heard");
        resetSensor(helper, sensor);

        player.addEffect(new MobEffectInstance(GatenkaasFeature.STIL, 200));
        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.KAUWEN);
        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
        helper.assertTrue(!active(helper, sensor), "with guhs:stil nobody hears you");
        player.removeEffect(GatenkaasFeature.STIL);

        player.setGameMode(GameType.CREATIVE);
        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
        helper.assertTrue(!active(helper, sensor), "creative players make no noise");
        done(helper, player);
    }

    private static void step(ServerPlayer player, double dx) {
        player.tickCount = 8;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        player.setPos(player.getX() + dx, player.getY(), player.getZ());
        player.setOnGround(true);
        player.tickCount = 16;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
    }

    /** Chewing is a noise too: every few ticks while you eat. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_2")
    public static void chewingIsHeard(GameTestHelper helper) {
        floor(helper);
        BlockPos sensor = new BlockPos(3, 1, 3);
        helper.setBlock(sensor, GatenkaasFeature.KNABBELSENSOR.get());
        ServerPlayer player = survivor(helper, 1, 1);
        ItemStack snack = new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get());
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(player, snack, 12));
        helper.assertTrue(active(helper, sensor), "the sensor hears you chew");
        done(helper, player);
    }

    /** Three warnings from a Mika-built schreeuwer wake the Vadswaker; a home-made one only screams. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_3", timeoutTicks = 240)
    public static void thirdWarningWakesTheVadswaker(GameTestHelper helper) {
        floor(helper);
        BlockPos sensor = new BlockPos(1, 1, 3), shrieker = new BlockPos(3, 1, 3);
        helper.setBlock(sensor, GatenkaasFeature.KNABBELSENSOR.get());
        helper.setBlock(shrieker, GatenkaasFeature.KNABBELSCHREEUWER.get().defaultBlockState());      // (player-placed)
        ServerPlayer player = survivor(helper, 1, 1);
        BlockPos at = player.blockPosition();
        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
        helper.assertTrue(helper.getBlockState(shrieker).getValue(KnabbelschreeuwerBlock.SHRIEKING), "it screams");
        helper.assertTrue(Knabbelgeluid.warnings(player) == 0, "but a home-made schreeuwer counts no warnings");

        for (int w = 1; w <= Knabbelgeluid.SUMMON_AT; w++) {
            resetSensor(helper, sensor);
            helper.setBlock(shrieker, GatenkaasFeature.KNABBELSCHREEUWER.get().defaultBlockState().setValue(KnabbelschreeuwerBlock.CAN_SUMMON, true));
            GuhQuests.saved(player).putLong(Knabbelgeluid.LAST_WARNING, helper.getLevel().getGameTime() - 1000);
            Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
            helper.assertTrue(Knabbelgeluid.warnings(player) == w, "warning " + w + ": " + Knabbelgeluid.warnings(player));
            // right away again: the schreeuwer has a cooldown per player
            resetSensor(helper, sensor);
            helper.setBlock(shrieker, GatenkaasFeature.KNABBELSCHREEUWER.get().defaultBlockState().setValue(KnabbelschreeuwerBlock.CAN_SUMMON, true));
            Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
            helper.assertTrue(Knabbelgeluid.warnings(player) == w, "no extra warning within the cooldown");
        }
        AABB around = new AABB(helper.absolutePos(shrieker)).inflate(12);
        List<VadswakerEntity> woken = helper.getLevel().getEntitiesOfClass(VadswakerEntity.class, around);
        helper.assertTrue(woken.size() == 1, "the Vadswaker woke up: " + woken.size());
        VadswakerEntity vadswaker = woken.get(0);
        // (he comes up within 6 blocks of the schreeuwer: keep him on the test floor, where the chunks tick)
        net.minecraft.world.phys.Vec3 onFloor = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 1.5));
        vadswaker.snapTo(onFloor.x, onFloor.y, onFloor.z, 0, 0);
        helper.assertTrue(vadswaker.isEmerging() && vadswaker.isInvulnerableTo(helper.getLevel().damageSources().playerAttack(player)),
                "he climbs out of the ground (and can't be hurt while he does)");
        helper.assertTrue(vadswaker.anger(player) > 0, "and he knows about you");
        // a fourth warning doesn't wake a second one
        GuhQuests.saved(player).putLong(Knabbelgeluid.LAST_WARNING, helper.getLevel().getGameTime() - 1000);
        resetSensor(helper, sensor);
        helper.setBlock(shrieker, GatenkaasFeature.KNABBELSCHREEUWER.get().defaultBlockState().setValue(KnabbelschreeuwerBlock.CAN_SUMMON, true));
        Knabbelgeluid.noise(player, at, Knabbelgeluid.Noise.HAKKEN);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(VadswakerEntity.class, around).size() == 1, "only one Vadswaker");
        // (polled instead of a fixed delay: in a big test batch the first ticks of a new entity can be skipped)
        boolean[] out = {false};
        helper.onEachTick(() -> {
            if (!out[0] && !vadswaker.isEmerging() && !vadswaker.isDigging()) {
                out[0] = true;
                vadswaker.discard();
                done(helper, player);
            }
        });
        helper.runAfterDelay(230, () -> helper.fail("still " + vadswaker.getPose() + " after 230 ticks: tickCount " + vadswaker.tickCount
                + ", removed " + vadswaker.isRemoved() + ", noAi " + vadswaker.isNoAi() + ", ticking " + helper.getLevel().isPositionEntityTicking(vadswaker.blockPosition())));
    }

    /** He hears you chew from far away, gets angry and goes for you; with guhs:stil you can eat right next to him. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_4", timeoutTicks = 60)
    public static void theVadswakerHearsYouChew(GameTestHelper helper) {
        floor(helper);
        VadswakerEntity vadswaker = helper.spawn(GatenkaasFeature.VADSWAKER.get(), new BlockPos(2, 1, 2));
        vadswaker.standUp();
        ServerPlayer player = survivor(helper, 0, 0);
        player.snapTo(vadswaker.getX() + 15, vadswaker.getY(), vadswaker.getZ());
        ServerPlayer quiet = survivor(helper, 4, 4);
        quiet.addEffect(new MobEffectInstance(GatenkaasFeature.STIL, 400));
        ItemStack snack = new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get());
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(quiet, snack, 12));
        helper.assertTrue(vadswaker.anger(quiet) == 0, "a stil player chews without a sound");
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(player, snack, 12));
        helper.assertTrue(vadswaker.anger(player) == Knabbelgeluid.Noise.KAUWEN.anger, "he hears chewing 15 blocks away: " + vadswaker.anger(player));
        NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(player, snack, 6));
        helper.assertTrue(vadswaker.anger(player) >= VadswakerEntity.ANGRY, "two bites and he's angry");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(vadswaker.getTarget() == player, "he goes for the chewer: " + vadswaker.getTarget());
            vadswaker.discard();
            done(helper, player, quiet);
        });
    }

    /** Angry at someone behind a wall, he still breaks nothing (even with mob griefing on). */
    @GuhTest(template = EMPTY, batch = "gatenkaas_5", timeoutTicks = 140)
    public static void theVadswakerBreaksNoBlocks(GameTestHelper helper) {
        floor(helper);
        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < 5; x++) {
                helper.setBlock(x, y, 3, Blocks.OAK_PLANKS);
            }
        }
        VadswakerEntity vadswaker = helper.spawn(GatenkaasFeature.VADSWAKER.get(), new BlockPos(2, 1, 1));
        vadswaker.standUp();
        ServerPlayer player = survivor(helper, 2, 4);
        vadswaker.hear(player, player.blockPosition(), VadswakerEntity.MAX_ANGER);
        helper.runAfterDelay(120, () -> {
            for (int y = 1; y <= 3; y++) {
                for (int x = 0; x < 5; x++) {
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(x, y, 3));
                }
            }
            vadswaker.discard();
            done(helper, player);
        });
    }

    /** After a long quiet he digs himself back into the ground and is gone. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_6", timeoutTicks = 120)
    public static void theVadswakerDigsBackIn(GameTestHelper helper) {
        floor(helper);
        VadswakerEntity vadswaker = helper.spawn(GatenkaasFeature.VADSWAKER.get(), new BlockPos(2, 1, 2));
        vadswaker.standUp();
        vadswaker.quietFor(VadswakerEntity.CALM_DIG_AFTER);
        helper.runAfterDelay(3, () -> helper.assertTrue(vadswaker.isDigging(), "he starts digging"));
        helper.runAfterDelay(VadswakerEntity.DIG_TICKS + 10, () -> {
            helper.assertTrue(vadswaker.isRemoved(), "and he's gone");
            helper.succeed();
        });
    }

    /** Stacked pieces get their thickness; when the ceiling goes, the stalactite falls (and a stalagmite breaks). */
    @GuhTest(template = EMPTY, batch = "gatenkaas_7", timeoutTicks = 60)
    public static void stalactitesFallWhenTheCeilingGoes(GameTestHelper helper) {
        var block = GatenkaasFeature.KAAS_STALACTIET.get();
        helper.setBlock(1, 3, 1, GatenkaasFeature.GATENKAAS.get());
        BlockState down = block.defaultBlockState().setValue(KaasStalactietBlock.TIP_DIRECTION, Direction.DOWN);
        for (int y = 2; y >= 0; y--) {
            BlockPos p = new BlockPos(1, y, 1);
            helper.setBlock(p, down.setValue(KaasStalactietBlock.THICKNESS,
                    KaasStalactietBlock.thickness(helper.getLevel(), helper.absolutePos(p), Direction.DOWN)));
        }
        for (int y = 2; y >= 0; y--) {                  // (let the pieces above see the ones below them)
            BlockPos p = helper.absolutePos(new BlockPos(1, y, 1));
            helper.getLevel().setBlock(p, helper.getLevel().getBlockState(p).setValue(KaasStalactietBlock.THICKNESS,
                    KaasStalactietBlock.thickness(helper.getLevel(), p, Direction.DOWN)), 3);
        }
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 0, 1)).getValue(KaasStalactietBlock.THICKNESS) == DripstoneThickness.TIP
                && helper.getBlockState(new BlockPos(1, 1, 1)).getValue(KaasStalactietBlock.THICKNESS) == DripstoneThickness.FRUSTUM
                && helper.getBlockState(new BlockPos(1, 2, 1)).getValue(KaasStalactietBlock.THICKNESS) == DripstoneThickness.BASE,
                "base, frustum, tip");
        helper.setBlock(1, 0, 1, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(new BlockPos(1, 1, 1)).getValue(KaasStalactietBlock.THICKNESS) == DripstoneThickness.TIP,
                "without its tip, the next piece is the tip");
        // a stalagmite on a block, and the ceiling of the stalactite: gone
        helper.setBlock(3, 1, 3, Blocks.STONE);
        helper.setBlock(3, 2, 3, block.defaultBlockState());
        helper.setBlock(1, 3, 1, Blocks.AIR);
        helper.setBlock(3, 1, 3, Blocks.AIR);
        helper.runAfterDelay(4, () -> {
            helper.assertBlockNotPresent(block, new BlockPos(1, 2, 1));
            helper.assertBlockNotPresent(block, new BlockPos(3, 2, 3));
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(FallingBlockEntity.class, helper.getBounds().inflate(2)).isEmpty()
                    || helper.getBlockState(new BlockPos(1, 1, 1)).isAir(), "the stalactite fell");
            helper.succeed();
        });
    }

    /** The lore book: a real guh book, in the Mika-voorraadschuur's chest every time. The biome and features exist. */
    @GuhTest(template = EMPTY, batch = "gatenkaas_8")
    public static void theLoreBookAndTheWorldgen(GameTestHelper helper) {
        Guhboek book = Guhboek.VOORRAADKELDER;
        helper.assertTrue(Guhboek.of(book.stack()) == book && !book.secret(), "the diary of the Voorraadmika is a guh book");
        var level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/voorraadschuur")));
        for (int i = 0; i < 5; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                    .create(LootContextParamSets.CHEST);
            helper.assertTrue(table.getRandomItems(params).stream().anyMatch(s -> Guhboek.of(s) == book), "the big chest always has the book");
        }
        var registries = level.registryAccess();
        helper.assertTrue(registries.lookupOrThrow(Registries.BIOME).containsKey(GatenkaasFeature.GATENKAASGROTTEN), "the biome");
        helper.assertTrue(registries.lookupOrThrow(Registries.PLACED_FEATURE).containsKey(Guhs.id("gatenkaas_holte")), "the cheese holes");
        helper.assertTrue(registries.lookupOrThrow(Registries.STRUCTURE).containsKey(GatenkaasFeature.VOORRAADKELDER)
                && registries.lookupOrThrow(Registries.STRUCTURE).containsKey(GatenkaasFeature.MIJNSCHACHT), "both structures");
        helper.assertTrue(registries.lookupOrThrow(Registries.BIOME).containsKey(Guhs.id("guh_kristalmijn")), "the crystal mine is still there");
        helper.succeed();
    }

    /** The larder as generated: the lectern with the diary, sensors and Mika-built schreeuwers, chests, the moat. */
    @GuhTest(template = "stille_voorraadkelder", batch = "gatenkaas_templates", timeoutTicks = 100)
    public static void theLarderTemplateIsComplete(GameTestHelper helper) {
        int sensors = 0, shriekers = 0, chests = 0, sauce = 0, lecterns = 0;
        var level = helper.getLevel();
        var box = helper.getBounds();
        for (BlockPos p : BlockPos.betweenClosed((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ)) {
            BlockState s = level.getBlockState(p);
            if (s.is(GatenkaasFeature.KNABBELSENSOR.get())) {
                sensors++;
            } else if (s.is(GatenkaasFeature.KNABBELSCHREEUWER.get()) && s.getValue(KnabbelschreeuwerBlock.CAN_SUMMON)) {
                shriekers++;
            } else if (s.is(Blocks.CHEST) && level.getBlockEntity(p) instanceof ChestBlockEntity) {
                chests++;
            } else if (s.is(nl.juiced.guhs.registry.ModBlocks.KAAS_SAUS.get())) {
                sauce++;
            } else if (s.is(Blocks.LECTERN) && level.getBlockEntity(p) instanceof LecternBlockEntity lectern) {
                helper.assertTrue(Guhboek.of(lectern.getBook()) == Guhboek.VOORRAADKELDER, "the diary lies on the lectern");
                lecterns++;
            }
        }
        helper.assertTrue(sensors >= 30 && shriekers >= 8, "guards: " + sensors + " sensors, " + shriekers + " schreeuwers");
        helper.assertTrue(chests >= 12 && lecterns == 1 && sauce > 100, "chests " + chests + ", lecterns " + lecterns + ", kaassaus " + sauce);
        helper.succeed();
    }

    /** The abandoned mine shaft: a chest, a chest cart on the old track, a ladder up the caved-in shaft. */
    @GuhTest(template = "gatenkaas_mijnschacht", batch = "gatenkaas_templates", timeoutTicks = 60)
    public static void theMineShaftTemplateIsComplete(GameTestHelper helper) {
        // (relative to the test's structure block, one below the template: template y + 1)
        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(13, 4, 21));
        helper.assertBlockPresent(Blocks.LADDER, new BlockPos(21, 6, 12));
        var carts = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.vehicle.MinecartChest.class, helper.getBounds().inflate(1));
        helper.assertTrue(carts.size() == 1, "a chest cart: " + carts.size());
        helper.succeed();
    }
}
