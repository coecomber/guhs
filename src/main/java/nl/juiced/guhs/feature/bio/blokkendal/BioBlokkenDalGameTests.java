package nl.juiced.guhs.feature.bio.blokkendal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/** biomes3 slice "blokken-dal": the behaviours of the Klaterdal blocks that can break. */
public final class BioBlokkenDalGameTests {
    private static final String EMPTY = "empty", BATCH = "bio_blokken_dal";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at, float yaw) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, yaw, 0f);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** The yaw that makes a player look this way. */
    private static float yaw(Direction d) {
        return d.toYRot();
    }

    /** What the block would become when this player puts it at `pos` by clicking `face` of the block behind that face. */
    private static BlockState plaats(GameTestHelper helper, ServerPlayer p, Block block, BlockPos pos, Direction face, double langsX, double langsZ) {
        BlockPos abs = helper.absolutePos(pos);
        BlockPos tegen = abs.relative(face.getOpposite());
        Vec3 klik = new Vec3(abs.getX() + 0.5 + langsX, abs.getY() + 0.5, abs.getZ() + 0.5 + langsZ);
        BlockPlaceContext ctx = new BlockPlaceContext(helper.getLevel(), p, InteractionHand.MAIN_HAND, new ItemStack(block),
                new BlockHitResult(klik, face, tegen, false));
        BlockState state = block.getStateForPlacement(ctx);
        helper.assertTrue(state != null, "a state to place " + block);
        helper.setBlock(pos, state);
        return helper.getBlockState(pos);
    }

    private static void klik(GameTestHelper helper, ServerPlayer p, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        helper.getBlockState(pos).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.SOUTH, abs, false));
    }

    // =================================================================================================================
    // shoji
    // =================================================================================================================
    /** Two panels beside each other become a pair that slides apart; a column opens as one door; the shapes follow. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalShoji(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 4), yaw(Direction.NORTH));
        try {
            Block shoji = BlokkenDalSlice.SHOJI.get();
            helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
            BlockPos a = new BlockPos(1, 2, 2), b = new BlockPos(2, 2, 2);      // a is west: the player's left
            BlockState sa = plaats(helper, p, shoji, a, Direction.UP, -0.3, 0);
            helper.assertTrue(sa.getValue(ShojiBlock.FACING) == Direction.NORTH && sa.getValue(ShojiBlock.KANT) == DoorHingeSide.LEFT,
                    "the first panel slides to the clicked (left) side: " + sa);
            BlockState sb = plaats(helper, p, shoji, b, Direction.UP, -0.3, 0);
            helper.assertTrue(sb.getValue(ShojiBlock.KANT) == DoorHingeSide.RIGHT, "the panel beside it slides the other way: " + sb);
            helper.assertTrue(ShojiBlock.isPartner(sa, sb), "a pair");
            // one higher: the same panels
            BlockState sa2 = plaats(helper, p, shoji, a.above(), Direction.UP, 0.3, 0);
            BlockState sb2 = plaats(helper, p, shoji, b.above(), Direction.UP, -0.3, 0);
            helper.assertTrue(sa2 == sa && sb2 == sb, "stacked panels copy the one below: " + sa2 + " " + sb2);
            // the closed panel is a thin wall across the block, the open one a sliver at its own side
            AABB dicht = sa.getCollisionShape(helper.getLevel(), helper.absolutePos(a)).bounds();
            helper.assertTrue(dicht.getXsize() > 0.99 && dicht.getZsize() < 0.2 && dicht.getYsize() > 0.99, "closed: a thin wall " + dicht);
            klik(helper, p, a.above());
            for (BlockPos pos : List.of(a, b, a.above(), b.above())) {
                helper.assertTrue(helper.getBlockState(pos).getValue(ShojiBlock.OPEN), "the whole double door opens: " + pos);
            }
            AABB links = helper.getBlockState(a).getCollisionShape(helper.getLevel(), helper.absolutePos(a)).bounds();
            AABB rechts = helper.getBlockState(b).getCollisionShape(helper.getLevel(), helper.absolutePos(b)).bounds();
            helper.assertTrue(links.getXsize() < 0.25 && links.minX < 0.01, "open: the left panel stands at the west post " + links);
            helper.assertTrue(rechts.getXsize() < 0.25 && rechts.maxX > 0.99, "open: the right panel stands at the east post " + rechts);
            helper.assertTrue(!helper.getBlockState(a).canOcclude() && helper.getBlockState(a).propagatesSkylightDown(), "light goes through");
            klik(helper, p, b);
            for (BlockPos pos : List.of(a, b, a.above(), b.above())) {
                helper.assertTrue(!helper.getBlockState(pos).getValue(ShojiBlock.OPEN), "and closes again: " + pos);
            }
            // a lone panel beside a wall slides into that wall, wherever you click
            helper.setBlock(new BlockPos(4, 2, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
            BlockState los = plaats(helper, p, shoji, new BlockPos(3, 2, 1), Direction.UP, -0.3, 0);
            helper.assertTrue(los.getValue(ShojiBlock.KANT) == DoorHingeSide.RIGHT, "into the wall on its right: " + los);
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // tatami
    // =================================================================================================================
    /** A half mat takes the direction you look; the next one pairs with it; breaking one leaves a half mat. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalTatami(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(0, 3, 2), yaw(Direction.EAST));
        try {
            Block tatami = BlokkenDalSlice.TATAMI.get();
            BlockPos a = new BlockPos(2, 2, 2), b = new BlockPos(3, 2, 2), c = new BlockPos(2, 2, 3);
            for (BlockPos vloer : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(4, 1, 4))) {
                helper.setBlock(vloer, Blocks.STONE);
            }
            BlockState sa = plaats(helper, p, tatami, a, Direction.UP, 0, 0);
            helper.assertTrue(sa.getValue(TatamiBlock.FACING) == Direction.EAST && !sa.getValue(TatamiBlock.GEKOPPELD), "a half mat, weave to the east: " + sa);
            BlockState sb = plaats(helper, p, tatami, b, Direction.UP, 0, 0);
            helper.assertTrue(sb.getValue(TatamiBlock.GEKOPPELD) && sb.getValue(TatamiBlock.FACING) == Direction.WEST, "the second points at the first: " + sb);
            sa = helper.getBlockState(a);
            helper.assertTrue(sa.getValue(TatamiBlock.GEKOPPELD) && sa.getValue(TatamiBlock.FACING) == Direction.EAST, "and the first at the second: " + sa);
            // a third finds no half mat: it is one itself; a fourth beside it pairs with it, not with the full mat
            BlockState sc = plaats(helper, p, tatami, c, Direction.UP, 0, 0);
            helper.assertTrue(!sc.getValue(TatamiBlock.GEKOPPELD), "a third is a half mat again: " + sc);
            helper.assertTrue(helper.getBlockState(a).getValue(TatamiBlock.FACING) == Direction.EAST, "the full mat is left alone");
            p.snapTo(p.getX(), p.getY(), p.getZ(), yaw(Direction.SOUTH), 0f);
            BlockState sd = plaats(helper, p, tatami, c.south(), Direction.UP, 0, 0);
            helper.assertTrue(sd.getValue(TatamiBlock.GEKOPPELD) && sd.getValue(TatamiBlock.FACING) == Direction.NORTH, "a mat the other way: " + sd);
            helper.setBlock(b, Blocks.AIR);
            helper.assertTrue(!helper.getBlockState(a).getValue(TatamiBlock.GEKOPPELD), "half a mat is left when the other half is broken");
            // sneaking keeps a half mat a half mat
            p.setShiftKeyDown(true);
            BlockState se = plaats(helper, p, tatami, b, Direction.UP, 0, 0);
            helper.assertTrue(!se.getValue(TatamiBlock.GEKOPPELD) && !helper.getBlockState(a).getValue(TatamiBlock.GEKOPPELD), "sneaking: two half mats");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // toro
    // =================================================================================================================
    /** Dusk: a random tick lights one lantern and the others nearby follow; dawn: out again. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void bioBlokkenDalToro(GameTestHelper helper) {
        ToroBlock toro = BlokkenDalSlice.TORO.get();
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 3);
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        ToroBlock.Klok.zet(box, false);
        helper.setBlock(a, toro.defaultBlockState());
        helper.setBlock(b, toro.defaultBlockState());
        helper.assertTrue(!toro.bijwerken(helper.getBlockState(a), helper.getLevel(), helper.absolutePos(a), helper.getLevel().getRandom()), "by day nothing happens");
        helper.assertTrue(helper.getBlockState(a).getLightEmission() == 0, "dark by day");
        ToroBlock.Klok.zet(box, true);
        helper.getBlockState(a).randomTick(helper.getLevel(), helper.absolutePos(a), helper.getLevel().getRandom());
        helper.assertTrue(helper.getBlockState(a).getValue(ToroBlock.LIT) && helper.getBlockState(a).getLightEmission() >= 12, "lit at dusk");
        boolean[] ochtend = {false};
        helper.succeedWhen(() -> {
            if (!ochtend[0]) {
                helper.assertTrue(helper.getBlockState(b).getValue(ToroBlock.LIT), "the lantern nearby follows by itself");
                ochtend[0] = true;
                ToroBlock.Klok.zet(box, false);
                helper.getBlockState(b).randomTick(helper.getLevel(), helper.absolutePos(b), helper.getLevel().getRandom());
            }
            boolean uit = !helper.getBlockState(a).getValue(ToroBlock.LIT) && !helper.getBlockState(b).getValue(ToroBlock.LIT);
            if (uit) {
                ToroBlock.Klok.zet(box, null);
            }
            helper.assertTrue(uit, "both out at dawn");
        });
    }

    // =================================================================================================================
    // geharkt zand
    // =================================================================================================================
    private static UseOnContext hark(GameTestHelper helper, ServerPlayer p, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new UseOnContext(helper.getLevel(), p, InteractionHand.MAIN_HAND, p.getMainHandItem(), new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    /** A hoe rakes sand and goes through the patterns; ring blocks choose their piece around a centre. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalGeharktZand(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(0, 3, 0), yaw(Direction.EAST));
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
            Block recht = BlokkenDalSlice.GEHARKT_ZAND.get(), ring = BlokkenDalSlice.GEHARKT_ZAND_RING.get();
            BlockPos m = new BlockPos(2, 2, 2);
            helper.setBlock(m.below(), Blocks.STONE);
            helper.setBlock(m, Blocks.SAND);
            Items.IRON_HOE.useOn(hark(helper, p, m));
            BlockState s = helper.getBlockState(m);
            helper.assertTrue(s.is(recht) && s.getValue(GeharktZand.Recht.AXIS) == Direction.Axis.X, "a hoe rakes sand the way you look: " + s);
            Items.IRON_HOE.useOn(hark(helper, p, m));
            helper.assertTrue(helper.getBlockState(m).getValue(GeharktZand.Recht.AXIS) == Direction.Axis.Z, "again: across");
            Items.IRON_HOE.useOn(hark(helper, p, m));
            s = helper.getBlockState(m);
            helper.assertTrue(s.is(ring) && s.getValue(GeharktZand.Ring.VORM) == GeharktZand.Vorm.ROND, "again: a round of rings: " + s);
            // ring blocks around the round take their own piece
            for (GeharktZand.Vorm v : GeharktZand.Vorm.values()) {
                if (v != GeharktZand.Vorm.ROND) {
                    BlockPos pos = m.offset(v.dx, 0, v.dz);
                    helper.setBlock(pos.below(), Blocks.STONE);
                    BlockState piece = plaats(helper, p, ring, pos, Direction.UP, 0, 0);
                    helper.assertTrue(piece.getValue(GeharktZand.Ring.VORM) == v, "the piece " + v + ": " + piece);
                }
            }
            // by hand: sneaking turns one block
            p.setShiftKeyDown(true);
            Items.IRON_HOE.useOn(hark(helper, p, m.north()));
            helper.assertTrue(helper.getBlockState(m.north()).getValue(GeharktZand.Ring.VORM) == GeharktZand.Vorm.NOORDOOST, "sneaking: the next piece");
            p.setShiftKeyDown(false);
            // the hoe on a ring block: straight again; a boulder on raked sand is a centre too
            Items.IRON_HOE.useOn(hark(helper, p, m));
            helper.assertTrue(helper.getBlockState(m).is(recht), "and back to straight");
            helper.setBlock(m.above(), BlokkenDalSlice.GLADDE_KNUFFELSTEEN.get());
            helper.assertTrue(GeharktZand.kies(helper.getLevel(), helper.absolutePos(m.east()), false) == GeharktZand.Vorm.OOST, "rings around a boulder");
            helper.assertTrue(helper.getBlockState(m.west()).getValue(GeharktZand.Ring.VORM) == GeharktZand.Vorm.WEST, "a piece keeps its place");
            helper.assertTrue(!p.getMainHandItem().isEmpty() && p.getMainHandItem().getDamageValue() > 0, "raking wears the hoe");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // guh-bamboe
    // =================================================================================================================
    /** It grows a block at a time with the right leaves, stops at its own height, and falls when the foot is cut. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void bioBlokkenDalBamboe(GameTestHelper helper) {
        GuhBamboeBlock bamboe = BlokkenDalSlice.GUH_BAMBOE.get();
        BlockPos voet = new BlockPos(2, 2, 2);
        int echt = GuhBamboeBlock.volgroeid(helper.absolutePos(voet));
        helper.assertTrue(echt >= GuhBamboeBlock.MIN_HOOGTE && echt <= GuhBamboeBlock.MAX_HOOGTE, "a height of its own: " + echt);
        GuhBamboeBlock.proefHoogte = 3;          // (the test box is four blocks high)
        helper.setBlock(voet.below(), Blocks.PINK_WOOL);
        helper.assertTrue(bamboe.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(voet)), "it stands on the Guhmensie's wool");
        helper.assertTrue(!bamboe.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(0, 3, 0))), "not in the air");
        helper.setBlock(voet, bamboe.defaultBlockState());
        int doel = GuhBamboeBlock.volgroeid(helper.absolutePos(voet));
        helper.assertTrue(bamboe.groei(helper.getLevel(), helper.absolutePos(voet)), "it grows");
        helper.assertTrue(helper.getBlockState(voet.above()).getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.SMALL
                && helper.getBlockState(voet).getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.NONE, "two high: a small tuft on top");
        // bonemeal and time do the rest, never past its own height
        for (int i = 0; i < 40; i++) {
            BlockState top = helper.getBlockState(voet);
            if (bamboe.isValidBonemealTarget(helper.getLevel(), helper.absolutePos(voet), top)) {
                bamboe.performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), helper.absolutePos(voet), top);
            }
        }
        int hoogte = 0;
        while (helper.getBlockState(voet.above(hoogte)).is(bamboe)) {
            hoogte++;
        }
        helper.assertTrue(hoogte == doel, "full-grown at " + doel + ": " + hoogte);
        helper.assertTrue(helper.getBlockState(voet.above(hoogte - 1)).getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.LARGE
                && helper.getBlockState(voet.above(hoogte - 2)).getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.SMALL
                && helper.getBlockState(voet.above(hoogte - 3)).getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.NONE, "the big tuft on top, a small one under it");
        helper.assertTrue(helper.getBlockState(voet.above(hoogte - 1)).getValue(GuhBamboeBlock.STAGE) == 1, "the top is done growing");
        helper.assertTrue(!bamboe.groei(helper.getLevel(), helper.absolutePos(voet.above(hoogte - 1))) && !helper.getBlockState(voet.above(hoogte)).is(bamboe),
                "and it stays that high");
        GuhBamboeBlock.proefHoogte = 0;
        // cut it: the stump gets a top again and grows on
        helper.setBlock(voet.above(2), Blocks.AIR);
        helper.runAfterDelay(20, () -> {
            BlockState stomp = helper.getBlockState(voet.above());
            helper.assertTrue(stomp.is(bamboe) && stomp.getValue(GuhBamboeBlock.LEAVES) == BambooLeaves.SMALL && stomp.getValue(GuhBamboeBlock.STAGE) == 0,
                    "the stump is a top again: " + stomp);
            helper.setBlock(voet.below(), Blocks.AIR);
            helper.succeedWhen(() -> helper.assertTrue(!helper.getBlockState(voet).is(bamboe) && !helper.getBlockState(voet.above()).is(bamboe),
                    "without ground it all falls"));
        });
    }

    // =================================================================================================================
    // the dakkrul
    // =================================================================================================================
    /** The krul points away from what it is stuck to and takes its corner from its neighbours. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalDakkrul(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 0), yaw(Direction.SOUTH));
        try {
            DakpanHoekBlock krul = BlokkenDalSlice.DAKPAN_HOEK.get(0).get();
            Block plaat = BlokkenDalSlice.DAKPAN_PLAAT.get(0).get();
            // a roof block at (2,3,3); the eave runs along its north side at z = 2
            BlockPos dak = new BlockPos(2, 3, 3), rand = new BlockPos(2, 3, 2);
            helper.setBlock(dak, BlokkenDalSlice.DAKPAN.get(0).get());
            BlockState s = plaats(helper, p, krul, rand, Direction.NORTH, 0, 0);
            helper.assertTrue(s.getValue(DakpanHoekBlock.FACING) == Direction.NORTH && s.getValue(DakpanHoekBlock.VORM) == DakpanHoekBlock.Vorm.RECHT,
                    "stuck to the roof's side it points away from it, straight: " + s);
            // the tip (north) is the high side (VoxelShape.max(Y, z, x))
            var shape = s.getCollisionShape(helper.getLevel(), helper.absolutePos(rand));
            helper.assertTrue(shape.max(Direction.Axis.Y, 0.05, 0.5) > 0.9 && shape.max(Direction.Axis.Y, 0.95, 0.5) < 0.6,
                    "it rises to the tip: " + shape.max(Direction.Axis.Y, 0.05, 0.5) + " / " + shape.max(Direction.Axis.Y, 0.95, 0.5));
            // a slab beside it (east): it becomes the eave's end and turns up at the free (west) side
            helper.setBlock(rand.east(), plaat);
            s = helper.getBlockState(rand);
            helper.assertTrue(s.getValue(DakpanHoekBlock.VORM) == DakpanHoekBlock.Vorm.HOEK_LINKS && DakpanHoekBlock.zijkant(s) == Direction.WEST,
                    "beside a slab: a corner to the free side: " + s);
            shape = s.getCollisionShape(helper.getLevel(), helper.absolutePos(rand));
            helper.assertTrue(shape.max(Direction.Axis.Y, 0.05, 0.05) > 0.9 && shape.max(Direction.Axis.Y, 0.95, 0.05) < 0.6
                    && shape.max(Direction.Axis.Y, 0.05, 0.95) < 0.6, "high at the outer corner only");
            // built in on both sides: straight again
            helper.setBlock(rand.west(), plaat);
            helper.assertTrue(helper.getBlockState(rand).getValue(DakpanHoekBlock.VORM) == DakpanHoekBlock.Vorm.RECHT, "between two: straight");
            helper.setBlock(rand.west(), Blocks.AIR);
            // a krul stuck onto the side of a krul joins its row, and the row's end carries the lip round the corner
            helper.setBlock(rand.east(), Blocks.AIR);
            BlockState naast = plaats(helper, p, krul, rand.east(), Direction.EAST, 0, 0);
            helper.assertTrue(naast.getValue(DakpanHoekBlock.FACING) == Direction.NORTH && naast.getValue(DakpanHoekBlock.VORM) == DakpanHoekBlock.Vorm.EIND_RECHTS,
                    "the next krul of the row: " + naast);
            helper.assertTrue(helper.getBlockState(rand).getValue(DakpanHoekBlock.VORM) == DakpanHoekBlock.Vorm.EIND_LINKS, "and the first is the other end");
            // on top of a block with a wall behind it: away from the wall, whatever the player looks at
            helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(0, 2, 1), Blocks.STONE);
            BlockState boven = plaats(helper, p, krul, new BlockPos(1, 2, 1), Direction.UP, 0, 0);
            helper.assertTrue(boven.getValue(DakpanHoekBlock.FACING) == Direction.EAST, "away from the wall behind it: " + boven);
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // bonsai, the wood set, drops, recipes, tags
    // =================================================================================================================
    /** Shears prune the bonsai into its next shape, all four round. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalBonsai(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 0), yaw(Direction.SOUTH));
        try {
            BlockPos pos = new BlockPos(2, 2, 2);
            helper.setBlock(pos.below(), Blocks.STONE);
            BlockState s = plaats(helper, p, BlokkenDalSlice.BONSAI_POT.get(), pos, Direction.UP, 0, 0);
            helper.assertTrue(s.getValue(BonsaiPotBlock.FACING) == Direction.NORTH && s.getValue(BonsaiPotBlock.VORM) == 0, "it faces the player: " + s);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            BlockPos abs = helper.absolutePos(pos);
            for (int i = 1; i <= BonsaiPotBlock.VORMEN; i++) {
                helper.getBlockState(pos).useItemOn(p.getMainHandItem(), helper.getLevel(), p, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
                helper.assertTrue(helper.getBlockState(pos).getValue(BonsaiPotBlock.VORM) == i % BonsaiPotBlock.VORMEN, "shape " + i);
            }
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    /** Every block drops itself (leaves with shears), has an item, and the sets are in the tags the game works with. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalDropsEnTags(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(BlokkenDalSlice.alles().size() == 34, "34 blocks: " + BlokkenDalSlice.alles().size());
        for (DeferredBlock<?> holder : BlokkenDalSlice.alles()) {
            Block block = holder.get();
            BlockState state = block.defaultBlockState();
            if (block instanceof DoorBlock) {
                state = state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            }
            ItemStack tool = block instanceof LeavesBlock ? new ItemStack(Items.SHEARS) : new ItemStack(Items.IRON_PICKAXE);
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), abs, null, null, tool);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1, holder.getId() + " drops itself: " + drops);
            helper.assertTrue(block.asItem() != Items.AIR, holder.getId() + " has an item");
            boolean gereedschap = state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || state.is(BlockTags.MINEABLE_WITH_HOE) || block == BlokkenDalSlice.ESDOORN_ZAAILING.get();
            helper.assertTrue(gereedschap, holder.getId() + " has a tool");
        }
        helper.assertTrue(BlokkenDalSlice.LAKHOUT_PLANKEN.get().defaultBlockState().is(BlockTags.PLANKS)
                && new ItemStack(BlokkenDalSlice.LAKHOUT_PLANKEN.get()).is(ItemTags.PLANKS), "planks");
        helper.assertTrue(BlokkenDalSlice.LAKHOUT_TRAP.get().defaultBlockState().is(BlockTags.WOODEN_STAIRS)
                && BlokkenDalSlice.LAKHOUT_PLAAT.get().defaultBlockState().is(BlockTags.WOODEN_SLABS)
                && BlokkenDalSlice.LAKHOUT_HEK.get().defaultBlockState().is(BlockTags.WOODEN_FENCES)
                && BlokkenDalSlice.LAKHOUT_POORT.get().defaultBlockState().is(BlockTags.FENCE_GATES)
                && BlokkenDalSlice.LAKHOUT_DEUR.get().defaultBlockState().is(BlockTags.WOODEN_DOORS)
                && BlokkenDalSlice.LAKHOUT_LUIK.get().defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS), "the wood set's tags");
        helper.assertTrue(BlokkenDalSlice.ESDOORN_STAM.get().defaultBlockState().is(BlockTags.LOGS_THAT_BURN)
                && BlokkenDalSlice.ESDOORN_BLADEREN_ROOD.get().defaultBlockState().is(BlockTags.LEAVES)
                && BlokkenDalSlice.ESDOORN_BLADEREN_ORANJE.get().defaultBlockState().is(BlockTags.LEAVES)
                && BlokkenDalSlice.ESDOORN_ZAAILING.get().defaultBlockState().is(BlockTags.SAPLINGS), "the esdoorn's tags");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(BlokkenDalSlice.DAKPAN_TRAP.get(i).get().defaultBlockState().is(BlockTags.STAIRS)
                    && BlokkenDalSlice.DAKPAN_PLAAT.get(i).get().defaultBlockState().is(BlockTags.SLABS), "dakpan tags " + i);
        }
        helper.assertTrue(BlokkenDalSlice.GLADDE_KNUFFELSTEEN_TRAP.get().defaultBlockState().is(BlockTags.STAIRS)
                && BlokkenDalSlice.GLADDE_KNUFFELSTEEN_PLAAT.get().defaultBlockState().is(BlockTags.SLABS), "gladde knuffelsteen tags");
        helper.assertTrue(Blocks.SAND.defaultBlockState().is(GeharktZand.HARKBAAR) && Blocks.PINK_WOOL.defaultBlockState().is(GuhBamboeBlock.GROND), "own tags");
        var features = helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        helper.assertTrue(features.containsKey(BlokkenDalSlice.ESDOORN_BOOM_ROOD.identifier()) && features.containsKey(BlokkenDalSlice.ESDOORN_BOOM_ORANJE.identifier()),
                "the sapling's two trees");
        helper.succeed();
    }

    /** Every recipe of the slice loads. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioBlokkenDalRecepten(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        List<String> ids = new java.util.ArrayList<>(List.of("roze_lakhout_planken", "roze_lakhout_planken_lakken", "roze_lakhout_balk", "roze_lakhout_trap",
                "roze_lakhout_plaat", "roze_lakhout_hek", "roze_lakhout_poort", "roze_lakhout_deur", "roze_lakhout_luik", "guh_dakpan_wit",
                "guh_dakpan_wit_steenzagen", "shoji", "tatami", "guh_bamboe_papier", "guh_bamboe_stok", "toro", "geharkt_zand", "geharkt_zand_ring",
                "geharkt_zand_uit_ring", "guh_bamboe", "bonsai_pot", "bonsai_pot_van_steen", "gladde_knuffelsteen", "gladde_knuffelsteen_trap",
                "gladde_knuffelsteen_plaat", "gladde_knuffelsteen_trap_steenzagen", "gladde_knuffelsteen_plaat_steenzagen"));
        for (String kleur : BlokkenDalSlice.DAKPAN_KLEUREN) {
            for (String s : List.of("_verven", "_trap", "_plaat", "_hoek", "_trap_steenzagen", "_plaat_steenzagen", "_hoek_steenzagen")) {
                ids.add("guh_dakpan_" + kleur + s);
            }
        }
        for (String id : ids) {
            helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), "a recipe " + id);
        }
        helper.assertTrue(ids.size() == 48, "48 recipes: " + ids.size());
        helper.succeed();
    }

    private BioBlokkenDalGameTests() {
    }
}
