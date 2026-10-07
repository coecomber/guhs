package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.List;
import java.util.function.IntPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhSeatEntity;
import nl.juiced.guhs.feature.bio.blokkenwolk.RegenboogBlokken.Strook;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhTime;

/**
 * Game tests of the biomes3 slice "blokken-wolk": cloud is soft (you sink in a little, nothing suffocates, landing never
 * hurts, from any height), the rainbow's stripes join up (also in a turned structure), the wolkenbed is a bed (spawn point,
 * sleeping, broken while somebody sleeps), the wolkenbank is a seat, petals float on water only, every block drops itself,
 * the recipes are there, and the rule of the big waterfall.
 */
public class BioBlokkenWolkGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "bio_blokken_wolk";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static ServerPlayer player(GameTestHelper helper, BlockPos rel) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(rel);
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static double top(GameTestHelper helper, BlockState state) {
        return state.getCollisionShape(helper.getLevel(), helper.absolutePos(POS)).max(Direction.Axis.Y);
    }

    // --- cloud ------------------------------------------------------------------------------------------------------------

    /** You sink in two pixels everywhere, things can still stand on it, nothing suffocates in it, and nothing in it ticks. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkZacht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(POS);
        double zak = WolkenBlokken.ZAK / 16.0;
        for (var blok : List.of(BlokkenWolkSlice.WOLKENBLOK_WIT, BlokkenWolkSlice.WOLKENBLOK_ROZE)) {
            BlockState s = blok.get().defaultBlockState();
            helper.assertTrue(Math.abs(top(helper, s) - (1 - zak)) < 1e-6, "the block: its top is " + WolkenBlokken.ZAK + " pixels lower");
            helper.assertTrue(s.isFaceSturdy(level, abs, Direction.UP) && s.isFaceSturdy(level, abs, Direction.NORTH), "things can stand on and against it");
            helper.assertTrue(s.isSolidRender() && s.canOcclude(), "a solid cube: neighbours cull against it");
            helper.assertTrue(!s.isSuffocating(level, abs), "nothing suffocates in cloud");
            helper.assertTrue(!s.hasBlockEntity() && !s.isRandomlyTicking(), "no block entity, no ticks");
        }
        for (var plaat : List.of(BlokkenWolkSlice.WOLKENBLOK_WIT_PLAAT, BlokkenWolkSlice.WOLKENBLOK_ROZE_PLAAT)) {
            BlockState s = plaat.get().defaultBlockState();
            helper.assertTrue(Math.abs(top(helper, s.setValue(SlabBlock.TYPE, SlabType.BOTTOM)) - (0.5 - zak)) < 1e-6, "a bottom slab");
            helper.assertTrue(Math.abs(top(helper, s.setValue(SlabBlock.TYPE, SlabType.TOP)) - (1 - zak)) < 1e-6, "a top slab");
            helper.assertTrue(Math.abs(top(helper, s.setValue(SlabBlock.TYPE, SlabType.DOUBLE)) - (1 - zak)) < 1e-6, "a double slab");
            helper.assertTrue(s.setValue(SlabBlock.TYPE, SlabType.TOP).isFaceSturdy(level, abs, Direction.UP), "a top slab carries things");
            helper.assertTrue(!s.hasBlockEntity() && !s.isRandomlyTicking(), "no block entity, no ticks");
        }
        for (var trap : List.of(BlokkenWolkSlice.WOLKENBLOK_WIT_TRAP, BlokkenWolkSlice.WOLKENBLOK_ROZE_TRAP)) {
            for (BlockState s : trap.get().getStateDefinition().getPossibleStates()) {
                var vorm = s.getCollisionShape(level, abs);
                helper.assertTrue(Math.abs(vorm.max(Direction.Axis.Y) - (1 - zak)) < 1e-6, "stairs: the high step");
                // the low step of a bottom stair: nothing of the shape between its top and the middle of the block
                if (s.getValue(StairBlock.HALF) == Half.BOTTOM) {
                    double laag = vorm.toAabbs().stream().mapToDouble(b -> b.maxY).min().orElse(0);
                    helper.assertTrue(Math.abs(laag - (0.5 - zak)) < 1e-6, "stairs: the low step is lower too (" + laag + ")");
                }
                helper.assertTrue(vorm.min(Direction.Axis.Y) >= -1e-6, "stairs: nothing sticks out below the block");
            }
        }
        helper.succeed();
    }

    private static Pig val(GameTestHelper helper, BlockPos rel, int hoogte) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, rel.getX() + 0.5f, rel.getY() + 1 + hoogte, rel.getZ() + 0.5f);
        pig.setNoAi(false);     // (no AI means no gravity either)
        pig.getNavigation().stop();
        return pig;
    }

    /**
     * Landing on cloud from 20 blocks never hurts: the block, both slabs, the stairs, both colours, the rainbow, the bed.
     * On stone next to it the same fall does hurt (so the test would notice if falling stopped hurting at all).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200, skyAccess = true)     // (no barrier roof over the test: they fall in from above)
    public static void bioBlokkenWolkGeenValschade(GameTestHelper helper) {
        List<BlockState> zacht = List.of(
                BlokkenWolkSlice.WOLKENBLOK_WIT.get().defaultBlockState(),
                BlokkenWolkSlice.WOLKENBLOK_ROZE_PLAAT.get().defaultBlockState(),
                BlokkenWolkSlice.WOLKENBLOK_WIT_PLAAT.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                BlokkenWolkSlice.WOLKENBLOK_ROZE_TRAP.get().defaultBlockState(),
                BlokkenWolkSlice.REGENBOOGBLOK.get().defaultBlockState(),
                BlokkenWolkSlice.REGENBOOGBLOK_PLAAT.get().defaultBlockState(),
                BlokkenWolkSlice.REGENBOOGBLOK_TRAP.get().defaultBlockState());
        // one column each, a wall of glass panes would be nicer but a pig falls straight
        Pig[] varkens = new Pig[zacht.size()];
        BlockPos[] plek = new BlockPos[zacht.size()];
        for (int i = 0; i < zacht.size(); i++) {
            plek[i] = new BlockPos(i % 4, 1, i / 4 * 2);
            helper.setBlock(plek[i], zacht.get(i));
            varkens[i] = val(helper, plek[i], 20);
        }
        BlockPos steen = new BlockPos(4, 1, 4);
        helper.setBlock(steen, Blocks.STONE);
        Pig pechvogel = val(helper, steen, 20);
        helper.succeedWhen(() -> {
            for (int i = 0; i < varkens.length; i++) {
                helper.assertTrue(varkens[i].isAlive(), "the pig on " + zacht.get(i) + " lives");
                helper.assertTrue(varkens[i].onGround() && varkens[i].getY() < helper.absolutePos(plek[i]).getY() + 1.01, "the pig landed on " + zacht.get(i));
                helper.assertTrue(varkens[i].getHealth() == varkens[i].getMaxHealth(), "no fall damage on " + zacht.get(i)
                        + " (health " + varkens[i].getHealth() + ")");
                helper.assertTrue(Math.abs(varkens[i].getDeltaMovement().y) < 0.1, "it does not bounce");
            }
            helper.assertTrue(pechvogel.onGround() || !pechvogel.isAlive(), "the pig on stone landed");
            helper.assertTrue(!pechvogel.isAlive() || pechvogel.getHealth() < pechvogel.getMaxHealth(), "the same fall on stone does hurt");
            for (Pig p : varkens) {
                p.discard();
            }
            pechvogel.discard();
        });
    }

    /**
     * Sinking in does not trap anybody: a small guh (and a pig) standing in cloud are not hurt and not stuck, and walk off it
     * onto a normal block (a step up of two pixels) and onto a slab of cloud.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 300)
    public static void bioBlokkenWolkKleineGuhLooptErover(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), BlokkenWolkSlice.WOLKENBLOK_WIT.get());
        }
        helper.setBlock(new BlockPos(3, 1, 2), BlokkenWolkSlice.WOLKENBLOK_ROZE_TRAP.get().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.STONE_SLAB);
        GuhEntity guh = helper.spawnWithNoFreeWill(ModEntities.GUH.get(), 0.5f, 2.0f, 2.5f);
        guh.setBaby(true);
        guh.setNoAi(false);
        float gezond = guh.getHealth();
        BlockPos doel = helper.absolutePos(new BlockPos(4, 3, 2));
        int[] tik = {0};
        helper.succeedWhen(() -> {
            if (tik[0]++ % 10 == 0) {
                guh.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
            }
            helper.assertTrue(guh.isAlive() && guh.getHealth() >= gezond, "the small guh is never hurt (not suffocating in cloud)");
            helper.assertTrue(!guh.isInWall(), "never inside a wall");
            helper.assertTrue(guh.getX() > doel.getX() - 0.2 && guh.getY() > doel.getY() - 0.6,
                    "the small guh walked from the cloud, up the cloud stairs, onto the stone (at " + guh.position() + ")");
            guh.discard();
        });
    }

    // --- the rainbow ------------------------------------------------------------------------------------------------------

    private static BlockState gezet(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), 3);
        return level.getBlockState(pos);
    }

    /**
     * Pieces side by side across their stripes share one rainbow (a, b, c from the red side); alone a piece is whole. Block,
     * slab and stairs join each other. And a row that a structure turns or mirrors still has the red on the right side.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkRegenboogSluitAan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState blok = BlokkenWolkSlice.REGENBOOGBLOK.get().defaultBlockState().setValue(RegenboogBlokken.AS, Direction.Axis.X);
        BlockState plaat = BlokkenWolkSlice.REGENBOOGBLOK_PLAAT.get().defaultBlockState().setValue(RegenboogBlokken.AS, Direction.Axis.X);
        BlockState trap = BlokkenWolkSlice.REGENBOOGBLOK_TRAP.get().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockPos a = helper.absolutePos(new BlockPos(2, 1, 1)), b = a.south(), c = b.south();
        helper.assertTrue(gezet(level, b, blok).getValue(RegenboogBlokken.STROOK) == Strook.HEEL, "alone: the whole rainbow");
        gezet(level, a, plaat);
        gezet(level, c, trap);
        helper.assertTrue(level.getBlockState(a).getValue(RegenboogBlokken.STROOK) == Strook.A, "the north piece is the red third (a slab)");
        helper.assertTrue(level.getBlockState(b).getValue(RegenboogBlokken.STROOK) == Strook.B, "the middle piece (a block)");
        helper.assertTrue(level.getBlockState(c).getValue(RegenboogBlokken.STROOK) == Strook.C, "the south piece is the violet third (stairs)");
        // a piece with its stripes the other way does not join
        gezet(level, a.west(), blok.setValue(RegenboogBlokken.AS, Direction.Axis.Z));
        helper.assertTrue(level.getBlockState(a.west()).getValue(RegenboogBlokken.STROOK) == Strook.HEEL
                && level.getBlockState(a).getValue(RegenboogBlokken.STROOK) == Strook.A, "stripes the other way stay apart");
        level.removeBlock(a.west(), false);
        level.removeBlock(b, false);
        helper.assertTrue(level.getBlockState(a).getValue(RegenboogBlokken.STROOK) == Strook.HEEL
                && level.getBlockState(c).getValue(RegenboogBlokken.STROOK) == Strook.HEEL, "the middle gone: two whole rainbows");
        level.removeBlock(a, false);
        level.removeBlock(c, false);
        // faces between two pieces are not drawn; a slab hides only what it covers
        helper.assertTrue(RegenboogBlokken.verborgen(blok, blok, Direction.NORTH) && RegenboogBlokken.verborgen(plaat, blok, Direction.NORTH)
                && !RegenboogBlokken.verborgen(blok, plaat, Direction.NORTH) && !RegenboogBlokken.verborgen(blok, Blocks.GLASS.defaultBlockState(), Direction.UP),
                "hidden faces");
        // turned and mirrored like a structure template: the states say what the row then really is
        BlockPos midden = helper.absolutePos(POS);
        List<BlockPos> rij = List.of(new BlockPos(0, 0, -1), BlockPos.ZERO, new BlockPos(0, 0, 1));
        List<BlockState> staten = List.of(plaat.setValue(RegenboogBlokken.STROOK, Strook.A), blok.setValue(RegenboogBlokken.STROOK, Strook.B),
                trap.setValue(RegenboogBlokken.STROOK, Strook.C));
        for (Mirror mirror : Mirror.values()) {
            for (Rotation rotation : Rotation.values()) {
                for (int i = 0; i < 3; i++) {
                    BlockPos p = midden.offset(StructureTemplate.transform(rij.get(i), mirror, rotation, BlockPos.ZERO));
                    level.setBlock(p, staten.get(i).mirror(mirror).rotate(rotation), 2 | 16);   // (no shape updates: as the template wrote it)
                }
                for (int i = 0; i < 3; i++) {
                    BlockPos p = midden.offset(StructureTemplate.transform(rij.get(i), mirror, rotation, BlockPos.ZERO));
                    BlockState s = level.getBlockState(p);
                    Direction.Axis as = ((RegenboogBlokken.Deel) s.getBlock()).as(s);
                    helper.assertTrue(s.getValue(RegenboogBlokken.STROOK) == RegenboogBlokken.strook(level, p, as),
                            mirror + " " + rotation + ": piece " + i + " says " + s.getValue(RegenboogBlokken.STROOK) + ", it is "
                                    + RegenboogBlokken.strook(level, p, as));
                }
                for (int i = 0; i < 3; i++) {
                    level.setBlock(midden.offset(StructureTemplate.transform(rij.get(i), mirror, rotation, BlockPos.ZERO)), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
            }
        }
        helper.succeed();
    }

    // --- furniture --------------------------------------------------------------------------------------------------------

    /** The wolkenbank is a seat like the guh-bank; the wolkenlamp gives soft light and needs nothing under it. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkBankEnLamp(GameTestHelper helper) {
        helper.setBlock(POS, BlokkenWolkSlice.WOLKENBANK.get());
        BlockPos abs = helper.absolutePos(POS);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = player(helper, new BlockPos(2, 1, 3));
        level.getBlockState(abs).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        helper.assertTrue(p.getVehicle() instanceof GuhSeatEntity, "sitting on the wolkenbank");
        helper.assertTrue(Math.abs(p.getVehicle().getY() - (abs.getY() + 0.5)) < 0.01, "at seat height");
        p.stopRiding();
        BlockPos lamp = helper.absolutePos(new BlockPos(1, 3, 1));
        level.setBlock(lamp, BlokkenWolkSlice.WOLKENLAMP.get().defaultBlockState(), 3);
        BlockState s = level.getBlockState(lamp);
        helper.assertTrue(s.is(BlokkenWolkSlice.WOLKENLAMP.get()) && s.getLightEmission(level, lamp) == WolkenlampBlock.LICHT
                && WolkenlampBlock.LICHT >= 10 && WolkenlampBlock.LICHT < 15, "the lamp floats in mid-air and gives soft light");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getEntitiesOfClass(GuhSeatEntity.class, new net.minecraft.world.phys.AABB(abs).inflate(2)).isEmpty(),
                    "the seat is gone when you get up");
            helper.assertTrue(level.getBlockState(lamp).is(BlokkenWolkSlice.WOLKENLAMP.get()), "the lamp still floats");
            level.removeBlock(lamp, false);
            leave(helper, p);
            helper.succeed();
        });
    }

    /**
     * The wolkenbed: placed as two halves, a bed for the game (direction, occupied), it becomes your spawn point and you
     * respawn beside it, you sleep in it at night, and breaking it under a sleeper wakes him without a crash. By day it
     * says "you can sleep only at night" and still sets the spawn, as a vanilla bed does.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void bioBlokkenWolkBed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos voet = helper.absolutePos(new BlockPos(2, 2, 2));
        Direction naar = Direction.EAST;
        BlockPos hoofd = voet.relative(naar);
        WolkenbedBlock bed = BlokkenWolkSlice.WOLKENBED.get();
        BlockState s = bed.defaultBlockState().setValue(WolkenbedBlock.FACING, naar);
        level.setBlock(voet, s, 3);
        bed.setPlacedBy(level, voet, s, null, ItemStack.EMPTY);
        ServerPlayer p = player(helper, new BlockPos(2, 2, 1));
        long was = GuhTime.dayTime(level);
        Runnable klaar = () -> {
            GuhTime.setDayTime(level, was);
            leave(helper, p);
        };
        BlockState h = level.getBlockState(hoofd);
        try {
            helper.assertTrue(h.is(bed) && h.getValue(WolkenbedBlock.PART) == BedPart.HEAD && level.getBlockState(voet).getValue(WolkenbedBlock.PART) == BedPart.FOOT,
                    "placing the foot places the head");
            helper.assertTrue(h.isBed(level, hoofd, p) && h.getBedDirection(level, hoofd) == naar && !h.hasBlockEntity(), "a bed with a direction, no block entity");
            helper.assertTrue(h.is(net.minecraft.tags.BlockTags.BEDS), "in the beds tag");
        } catch (Throwable t) {
            klaar.run();
            throw t;
        }
        // (the game only knows whether it is dark a tick after the clock was set)
        GuhTime.setDayTime(level, 6000);
        helper.runAfterDelay(3, () -> {
            try {
                // by day: no sleep, but the spawn point is set (through the block's own click, on the foot half)
                var voor = p.getRespawnConfig();
                InteractionResult r = level.getBlockState(voet).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(voet), Direction.UP, voet, false));
                helper.assertTrue(r.consumesAction() && !p.isSleeping(), "by day you do not sleep");
                var nu = p.getRespawnConfig();
                helper.assertTrue(nu != null && !java.util.Objects.equals(nu, voor) && nu.respawnData().pos().equals(hoofd)
                        && nu.respawnData().dimension() == level.dimension(), "the wolkenbed is your spawn point now (" + nu + ")");
                var terug = h.getRespawnPosition(EntityType.PLAYER, level, hoofd, 0f);
                helper.assertTrue(terug.isPresent() && terug.get().position().distanceTo(Vec3.atCenterOf(hoofd)) < 3.0, "you respawn beside the bed");
                GuhTime.setDayTime(level, 18000);
            } catch (Throwable t) {
                klaar.run();
                throw t;
            }
        });
        helper.runAfterDelay(6, () -> {
            try {
                // at night: asleep, both halves occupied, lying the right way
                level.getBlockState(hoofd).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(hoofd), Direction.UP, hoofd, false));
                helper.assertTrue(p.isSleeping() && p.getSleepingPos().filter(hoofd::equals).isPresent(), "asleep in the wolkenbed at night");
                helper.assertTrue(level.getBlockState(hoofd).getValue(WolkenbedBlock.OCCUPIED) && level.getBlockState(voet).getValue(WolkenbedBlock.OCCUPIED),
                        "both halves are occupied");
                helper.assertTrue(p.getBedOrientation() == naar, "lying from foot to head");
                // somebody else: it is occupied
                ServerPlayer q = player(helper, new BlockPos(2, 2, 3));
                level.getBlockState(voet).useWithoutItem(level, q, new BlockHitResult(Vec3.atCenterOf(voet), Direction.UP, voet, false));
                boolean slaapt = q.isSleeping();
                leave(helper, q);
                helper.assertTrue(!slaapt, "one sleeper at a time");
                // broken under the sleeper
                level.destroyBlock(voet, false);
                helper.assertTrue(level.getBlockState(hoofd).isAir() && level.getBlockState(voet).isAir(), "one half broken: the other goes too");
            } catch (Throwable t) {
                klaar.run();
                throw t;
            }
        });
        helper.runAfterDelay(10, () -> {
            try {
                p.doTick();     // (a mock player has no connection that ticks it; this is the tick in which a sleeper looks for his bed)
                helper.assertTrue(!p.isSleeping() && p.isAlive(), "the sleeper woke up when the bed went");
            } finally {
                klaar.run();
            }
            helper.succeed();
        });
    }

    // --- petals -----------------------------------------------------------------------------------------------------------

    /**
     * Petals float on still water only: placed on water, thickened by more petals (three at most), not placed on land or on
     * flowing water; they go, and drop what they were, when the water under them goes; nothing collides with them.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkBloesemblaadjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // a basin of one water source with stone round it
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos water = helper.absolutePos(POS), erop = water.above(), land = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        BloesemblaadjesBlock blok = BlokkenWolkSlice.BLOESEMBLAADJES.get();
        BloesemblaadjesBlock.Voorwerp voorwerp = BlokkenWolkSlice.BLOESEMBLAADJES_ITEM.get();
        ServerPlayer p = player(helper, new BlockPos(2, 2, 0));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(voorwerp, 16));
            helper.assertTrue(blok.defaultBlockState().canSurvive(level, erop) && !blok.defaultBlockState().canSurvive(level, land.above()),
                    "floats on water, not on stone");
            helper.assertTrue(!voorwerp.plaats(level, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(land), Direction.UP, land, false))
                    .consumesAction() && level.getBlockState(land.above()).isAir(), "not placed on land");
            for (int n = 1; n <= BloesemblaadjesBlock.MAX; n++) {
                BlockPos raak = n == 1 ? water : erop;      // (first the water, then the petals that lie there)
                helper.assertTrue(voorwerp.plaats(level, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(raak), Direction.UP, raak, false))
                        .consumesAction(), "placed, time " + n);
                BlockState s = level.getBlockState(erop);
                helper.assertTrue(s.is(blok) && s.getValue(BloesemblaadjesBlock.DICHTHEID) == n, "density " + n + " (" + s + ")");
            }
            helper.assertTrue(p.getMainHandItem().getCount() == 16 - BloesemblaadjesBlock.MAX, "one petal item per layer");
            voorwerp.plaats(level, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(erop), Direction.UP, erop, false));
            helper.assertTrue(level.getBlockState(erop).getValue(BloesemblaadjesBlock.DICHTHEID) == BloesemblaadjesBlock.MAX
                    && level.getBlockState(erop.above()).isAir() && p.getMainHandItem().getCount() == 16 - BloesemblaadjesBlock.MAX, "a full patch takes no more");
            BlockState vol = level.getBlockState(erop);
            helper.assertTrue(vol.getCollisionShape(level, erop).isEmpty() && !vol.getShape(level, erop).isEmpty(), "nothing bumps into it, you can still point at it");
            helper.assertTrue(Block.getDrops(vol, level, erop, null).stream().mapToInt(ItemStack::getCount).sum() == BloesemblaadjesBlock.MAX,
                    "a thick patch drops three");
            // flowing water does not carry petals
            BlockPos stroom = helper.absolutePos(new BlockPos(2, 3, 4));
            level.setBlock(stroom.below(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), 2 | 16);
            helper.assertTrue(!blok.defaultBlockState().canSurvive(level, stroom), "not on flowing water");
            level.setBlock(stroom.below(), Blocks.AIR.defaultBlockState(), 2 | 16);
            // the water goes: the petals go too
            level.setBlock(water, Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(level.getBlockState(erop).isAir(), "the water is gone, the petals are gone");
            helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(erop).inflate(1.5),
                    e -> e.getItem().is(voorwerp)).isEmpty(), "and they dropped as items");
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(erop).inflate(3)).forEach(Entity::discard);
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    // --- loot and recipes -------------------------------------------------------------------------------------------------

    private static int telt(List<ItemStack> drops, Item item) {
        return drops.stream().filter(d -> d.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** Every block drops itself (a double slab two, the bed once for its two halves), and every one has an item. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkBlokkenVallenZelf(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(POS);
        for (var blok : BlokkenWolkSlice.blokken()) {
            Block b = blok.get();
            Item item = b.asItem();
            helper.assertTrue(item != net.minecraft.world.item.Items.AIR, blok.getId() + " has an item");
            BlockState s = b.defaultBlockState();
            if (b instanceof WolkenbedBlock) {
                helper.assertTrue(telt(Block.getDrops(s.setValue(WolkenbedBlock.PART, BedPart.HEAD), level, pos, null), item) == 1
                        && Block.getDrops(s.setValue(WolkenbedBlock.PART, BedPart.FOOT), level, pos, null).isEmpty(), "the bed drops once");
                continue;
            }
            List<ItemStack> drops = Block.getDrops(s, level, pos, null);
            helper.assertTrue(drops.size() == 1 && telt(drops, item) == 1, blok.getId() + " drops itself (" + drops + ")");
            if (b instanceof SlabBlock) {
                helper.assertTrue(telt(Block.getDrops(s.setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, pos, null), item) == 2, blok.getId() + " double: two");
            }
        }
        helper.succeed();
    }

    /** The recipes: fluff into cloud, white and pink both ways, slabs and stairs, the rainbow, the furniture, the petals. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkRecepten(GameTestHelper helper) {
        var recepten = helper.getLevel().getServer().getRecipeManager();
        var registers = helper.getLevel().registryAccess();
        record R(String id, Item uit, int aantal) {
        }
        for (R r : List.of(
                new R("wolkenblok_wit", BlokkenWolkSlice.WOLKENBLOK_WIT_ITEM.get(), 4),
                new R("wolkenblok_roze", BlokkenWolkSlice.WOLKENBLOK_ROZE_ITEM.get(), 4),
                new R("wolkenblok_roze_verven", BlokkenWolkSlice.WOLKENBLOK_ROZE_ITEM.get(), 8),
                new R("wolkenblok_wit_bleken", BlokkenWolkSlice.WOLKENBLOK_WIT_ITEM.get(), 8),
                new R("wolkenblok_wit_plaat", BlokkenWolkSlice.WOLKENBLOK_WIT_PLAAT_ITEM.get(), 6),
                new R("wolkenblok_wit_trap", BlokkenWolkSlice.WOLKENBLOK_WIT_TRAP_ITEM.get(), 4),
                new R("wolkenblok_roze_plaat", BlokkenWolkSlice.WOLKENBLOK_ROZE_PLAAT_ITEM.get(), 6),
                new R("wolkenblok_roze_trap", BlokkenWolkSlice.WOLKENBLOK_ROZE_TRAP_ITEM.get(), 4),
                new R("regenboogblok", BlokkenWolkSlice.REGENBOOGBLOK_ITEM.get(), 6),
                new R("regenboogblok_plaat", BlokkenWolkSlice.REGENBOOGBLOK_PLAAT_ITEM.get(), 6),
                new R("regenboogblok_trap", BlokkenWolkSlice.REGENBOOGBLOK_TRAP_ITEM.get(), 4),
                new R("wolkenbank", BlokkenWolkSlice.WOLKENBANK_ITEM.get(), 1),
                new R("wolkenbed", BlokkenWolkSlice.WOLKENBED_ITEM.get(), 1),
                new R("wolkenlamp", BlokkenWolkSlice.WOLKENLAMP_ITEM.get(), 2),
                new R("drijvende_bloesemblaadjes", BlokkenWolkSlice.BLOESEMBLAADJES_ITEM.get(), 4),
                new R("drijvende_bloesemblaadjes_uit_roze_blaadjes", BlokkenWolkSlice.BLOESEMBLAADJES_ITEM.get(), 2))) {
            var recept = recepten.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(r.id())));
            helper.assertTrue(recept.isPresent(), "a recipe " + r.id());
            ItemStack uit = recept.get().value().assemble(null);
            helper.assertTrue(uit.is(r.uit()) && uit.getCount() == r.aantal(), r.id() + " makes " + r.aantal() + " x " + r.uit() + " (" + uit + ")");
        }
        helper.assertTrue(registers != null, "registries");
        // made by hand: four fluff in a square is four white cloud
        var raster = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(new ItemStack(BlokkenWolkSlice.WOLKENPLUIS.get()),
                new ItemStack(BlokkenWolkSlice.WOLKENPLUIS.get()), new ItemStack(BlokkenWolkSlice.WOLKENPLUIS.get()), new ItemStack(BlokkenWolkSlice.WOLKENPLUIS.get())));
        var gevonden = helper.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, raster, helper.getLevel());
        helper.assertTrue(gevonden.isPresent() && gevonden.get().value().assemble(raster).is(BlokkenWolkSlice.WOLKENBLOK_WIT_ITEM.get()),
                "four wolkenpluis make wolkenblok_wit at a crafting table");
        helper.succeed();
    }

    // --- the big waterfall ------------------------------------------------------------------------------------------------

    private static IntPredicate tussen(int van, int tot) {
        return y -> y >= van && y <= tot;
    }

    /**
     * The rule: foam only where water falls 4 blocks or more. A river stepping down one or two blocks never gets it; the
     * height counts the block of the pool the water lands in. And the real thing: columns of falling water in the world.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkWatervalRegel(GameTestHelper helper) {
        IntPredicate niets = y -> false;
        // no falling water at all (still water, air)
        helper.assertTrue(Waterval.meet(niets, y -> true, 10) == null && Waterval.meet(niets, niets, 10) == null, "still water is no waterfall");
        // a river dropping one block into the next stretch: no falling block in the air at all, or one that lands in water
        Waterval.Voet een = Waterval.meet(tussen(10, 10), tussen(0, 9), 10);
        helper.assertTrue(een != null && een.hoogte() == 2 && !een.groot(), "a step of two blocks: " + een);
        Waterval.Voet opGrond = Waterval.meet(tussen(10, 10), niets, 10);
        helper.assertTrue(opGrond.hoogte() == 1 && !opGrond.groot() && !opGrond.inWater(), "one falling block onto the ground: " + opGrond);
        Waterval.Voet drie = Waterval.meet(tussen(10, 12), niets, 11);
        helper.assertTrue(drie.y() == 10 && drie.hoogte() == 3 && !drie.groot(), "three blocks onto the ground: not big, " + drie);
        // exactly at the threshold, both ways of landing
        Waterval.Voet vierGrond = Waterval.meet(tussen(10, 13), niets, 13);
        helper.assertTrue(vierGrond.y() == 10 && vierGrond.hoogte() == 4 && vierGrond.groot(), "four blocks onto the ground: big, " + vierGrond);
        Waterval.Voet vierWater = Waterval.meet(tussen(10, 12), tussen(0, 9), 10);
        helper.assertTrue(vierWater.y() == 10 && vierWater.hoogte() == 4 && vierWater.groot() && vierWater.inWater(), "three falling blocks into a pool: a drop of four, " + vierWater);
        // asked anywhere in the fall: the same foot
        for (int y = 20; y <= 31; y++) {
            Waterval.Voet v = Waterval.meet(tussen(20, 31), tussen(0, 19), y);
            helper.assertTrue(v.y() == 20 && v.hoogte() == 13 && v.groot(), "a tall fall asked at " + y + ": " + v);
        }
        helper.assertTrue(Waterval.meet(y -> true, niets, 0).hoogte() == Waterval.MAX_HOOGTE, "an endless fall is measured to " + Waterval.MAX_HOOGTE);
        helper.assertTrue(!Waterval.groot(Waterval.GROOT - 1) && Waterval.groot(Waterval.GROOT) && Waterval.GROOT == 4, "the threshold is 4");

        // in the world: falling water (level 8) set as a worldgen feature would, nothing updated
        ServerLevel level = helper.getLevel();
        BlockPos voet = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState vallend = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
        BlockState bron = Blocks.WATER.defaultBlockState();
        int geen = 2 | 16;
        // column A: 5 falling blocks onto stone; B: 2 falling blocks into a pool (a drop of 3); C: 3 into a pool (4); D: a deep pool
        BlockPos a = voet, b = voet.east(2), c = voet.south(2), d = voet.east(2).south(2);
        try {
            for (int i = 0; i < 5; i++) {
                level.setBlock(a.above(i), vallend, geen);
            }
            level.setBlock(b.below(), bron, geen);
            level.setBlock(c.below(), bron, geen);
            for (int i = 0; i < 3; i++) {
                level.setBlock(c.above(i), vallend, geen);
                if (i < 2) {
                    level.setBlock(b.above(i), vallend, geen);
                }
            }
            for (int i = -1; i < 6; i++) {
                level.setBlock(d.above(i), bron, geen);
            }
            int boven = voet.getY() + 12, onder = voet.getY() - 2;
            helper.assertTrue(Waterval.valt(level, a) && !Waterval.valt(level, d) && !Waterval.valt(level, a.above(6)), "falling water is falling water");
            Waterval.Voet va = Waterval.zoek(level, a.getX(), a.getZ(), boven, onder);
            helper.assertTrue(va != null && va.y() == a.getY() && va.hoogte() == 5 && va.groot() && !va.inWater(), "A: " + va);
            Waterval.Voet vb = Waterval.zoek(level, b.getX(), b.getZ(), boven, onder);
            helper.assertTrue(vb != null && vb.hoogte() == 3 && !vb.groot() && vb.inWater(), "B: a small drop, no foam: " + vb);
            Waterval.Voet vc = Waterval.zoek(level, c.getX(), c.getZ(), boven, onder);
            helper.assertTrue(vc != null && vc.y() == c.getY() && vc.hoogte() == 4 && vc.groot() && vc.inWater(), "C: " + vc);
            helper.assertTrue(Waterval.zoek(level, d.getX(), d.getZ(), boven, onder) == null, "D: a deep pool is no waterfall");
            helper.assertTrue(Waterval.zoek(level, a.getX() + 1, a.getZ(), boven, onder) == null, "an empty column");
        } finally {
            for (BlockPos kolom : List.of(a, b, c, d)) {
                for (int i = -1; i < 6; i++) {
                    level.setBlock(kolom.above(i), i < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), geen);
                }
            }
        }
        helper.succeed();
    }

    /** What the wolkenstroom's particles need exists on the server too (the types are registered on both sides). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenWolkDeeltjesEnGeluiden(GameTestHelper helper) {
        var deeltjes = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE;
        for (String id : List.of("waterval_schuim", "waterval_nevel", "wolkenstroom_pluis")) {
            helper.assertTrue(deeltjes.containsKey(Guhs.id(id)), "particle " + id);
        }
        var geluiden = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT;
        for (String id : List.of("waterval.ruis", "wolkenblok.stap", "wolkenblok.plaats", "wolkenblok.breek")) {
            helper.assertTrue(geluiden.containsKey(Guhs.id(id)), "sound " + id);
        }
        helper.assertTrue(BlokkenWolkSlice.WOLKENBLOK_WIT.get().defaultBlockState().getSoundType() == BlokkenWolkSlice.WOLK_GELUID
                && BlokkenWolkSlice.WOLK_GELUID.getStepSound() == BlokkenWolkSlice.WOLK_STAP.get(), "cloud sounds like cloud");
        helper.assertTrue(WolkenstroomPluis.OMHOOG > 0 && WolkenstroomPluis.OMLAAG < 0, "up is up, down is down");
        helper.succeed();
    }
}
