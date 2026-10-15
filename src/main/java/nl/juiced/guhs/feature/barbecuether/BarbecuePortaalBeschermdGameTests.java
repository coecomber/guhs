package nl.juiced.guhs.feature.barbecuether;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.ringh1.Gouw;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.4.2 (fixes, bug 3): in survival a barbecue portal can be built and lit where the story expects it, the pit of a
 * Knabbelgouw, although the Knabbelgouw is a protected building. Before 1.4.2 the fire guard of the protected buildings
 * ({@link Protected}: no flint and steel within 3 blocks of one) refused the Aanmaakblokje (it is a flint and steel).
 * A test copy of the Knabbelgouw lies so that its pit covers the test room (the real template is not placed: the
 * protection only reads the copy's box).
 */
public class BarbecuePortaalBeschermdGameTests {
    private static final String ROOM = "bbq_testkamer";
    /** Where in the Knabbelgouw (template coordinates) the test room's corner lies: inside the pit (22..58, 42..78). */
    private static final BlockPos IN_DE_PUT = new BlockPos(36, 0, 56);

    private static BlockHitResult klik(BlockPos abs, Direction kant) {
        return new BlockHitResult(Vec3.atCenterOf(abs).relative(kant, 0.5), kant, abs, false);
    }

    /** Right-click with this stack, as the game asks the protections: was it let through? */
    private static boolean doorgelaten(ServerPlayer p, ItemStack stack, BlockPos abs, Direction kant) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, abs, klik(abs, kant));
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getUseItem() != TriState.FALSE;
    }

    @GuhTest(template = ROOM, batch = "bbq_portaal_beschermd", timeoutTicks = 200)
    public static void bbqPortaalInDeBeschermdePut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure gouw = Kopieen.structuur(level, Gouw.STRUCTUUR);
        helper.assertTrue(gouw != null, "the structure guhs:knabbelgouw exists");
        BlockPos hoek = helper.absolutePos(new BlockPos(0, 0, 0)).subtract(IN_DE_PUT).below(2);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + Gouw.STRUCTUUR).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), hoek, Rotation.NONE);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, hoek, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(gouw, ChunkPos.containing(hoek), 0, new PiecesContainer(List.of(piece)));
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos staan = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(staan.getX() + 0.5, staan.getY(), staan.getZ() + 0.5);
        try {
            Kopieen.test(level, start);
            for (int x = 0; x < 9; x++) {
                for (int z = 0; z < 9; z++) {
                    helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                }
            }
            BlockPos inPut = helper.absolutePos(new BlockPos(3, 1, 4));
            helper.assertTrue(Bescherming.beschermd(level, inPut) && Protected.at(level, inPut) && Gouw.inPut(level, inPut),
                    "the room lies in the pit of a protected Knabbelgouw");
            BlockPos buiten = hoek.offset(5, 6, 5);
            helper.assertTrue(Bescherming.beschermd(level, buiten) && !Bescherming.mag(p, buiten), "outside the pit the Knabbelgouw stays whole");

            // the frame (inside 2 x 3), built block by block as a survival player does it
            ItemStack kool = new ItemStack(BarbecuetherFeature.GRILLKOOL_ITEM.get(), 64);
            p.setItemInHand(InteractionHand.MAIN_HAND, kool);
            for (int i = 0; i < 4; i++) {                    // the bottom row, on the floor
                p.gameMode.useItemOn(p, level, kool, InteractionHand.MAIN_HAND, klik(helper.absolutePos(new BlockPos(2 + i, 0, 4)), Direction.UP));
            }
            for (int j = 1; j < 5; j++) {                    // the sides, each on the block below
                for (int x : new int[]{2, 5}) {
                    p.gameMode.useItemOn(p, level, kool, InteractionHand.MAIN_HAND, klik(helper.absolutePos(new BlockPos(x, j, 4)), Direction.UP));
                }
            }
            // the top row, against the tops of the sides
            p.gameMode.useItemOn(p, level, kool, InteractionHand.MAIN_HAND, klik(helper.absolutePos(new BlockPos(2, 5, 4)), Direction.EAST));
            p.gameMode.useItemOn(p, level, kool, InteractionHand.MAIN_HAND, klik(helper.absolutePos(new BlockPos(5, 5, 4)), Direction.WEST));
            for (int j = 1; j <= 5; j++) {
                for (int i = 0; i < 4; i++) {
                    boolean rand = i == 0 || i == 3 || j == 1 || j == 5;
                    if (rand) {
                        helper.assertBlockPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(2 + i, j, 4));
                    }
                }
            }
            BlockPos binnen = helper.absolutePos(new BlockPos(3, 2, 4));
            helper.assertTrue(GrillPortalShape.findEmptyShape(level, binnen).isPresent(), "a whole grillkool frame");

            // lighting it: the Aanmaakblokje passes the fire guard and the protection; flint and steel does not
            BlockPos onderrand = helper.absolutePos(new BlockPos(3, 1, 4));
            helper.assertTrue(doorgelaten(p, new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get()), onderrand, Direction.UP),
                    "the Aanmaakblokje may light the frame in the protected pit");
            helper.assertTrue(!doorgelaten(p, new ItemStack(Items.FLINT_AND_STEEL), onderrand, Direction.UP), "flint and steel is still no fire here");
            BlockPos steen = helper.absolutePos(new BlockPos(7, 0, 7));
            helper.assertTrue(!doorgelaten(p, new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get()), steen, Direction.UP),
                    "and an Aanmaakblokje without a frame is a lighter: no fire here either");
            var shape = GrillPortalShape.findEmptyShape(level, binnen).orElseThrow();
            AanmaakblokjeItem.light(level, shape, binnen, p);      // (what its useOn does in the Guhmensie; the test server has none)
            helper.assertBlockPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(3, 2, 4));
            helper.assertBlockPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(4, 4, 4));

            // and taking it down again: a frame block (the portal goes out with it)
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(p.gameMode.destroyBlock(helper.absolutePos(new BlockPos(2, 3, 4))), "the own frame block can be broken");
            helper.assertBlockNotPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(2, 3, 4));
        } finally {
            Kopieen.testWissen(level);
            level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeedWhen(() -> helper.assertBlockNotPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(3, 2, 4)));
    }
}
