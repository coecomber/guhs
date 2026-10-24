package nl.juiced.guhs.feature.onderwol.bouw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.eilanden.EilandenFeature;
import nl.juiced.guhs.feature.eilanden.WolkenliftBlock;
import nl.juiced.guhs.feature.eilanden.WolkenstroomBlock;

/**
 * The shaft from an entrance hut on the surface down to an underground building ({@code guhs:grot_bouw} with
 * {@code "ingang"}). Seen from above it is 3 x 1 inside, across the direction {@code richting} (where the hut's floor opening
 * lets you off at the top): a ladder on the left, a wolkenlift going up in the middle and one going down on the right
 * (pads at the bottom, a column of wolkenstroom up to the top), with a lining of bricks around it (viltsteen bricks in the
 * stone, pastel bricks in the wool) and a crystal lamp now and then.
 * <p>
 * The inside is always made, from the bottom floor to the top. The lining only replaces solid ground and fluids, never
 * air (where the shaft passes through the building's own open room it stays open) and never above {@code voeringTot}
 * (the hut's own blocks stay). Placed after the building's pieces and after the hut, so its column always runs through.
 */
public class GrotSchachtPiece extends StructurePiece {
    /** The middle column (x, z), the y of the pads (the floor layer at the bottom) and of the top of the column. */
    private final int x, z, onder, boven, voeringTot;
    private final Direction richting;

    public GrotSchachtPiece(int x, int z, int onder, int boven, int voeringTot, Direction richting) {
        super(OnderWolBouw.SCHACHT.get(), 0, doos(x, z, onder, boven, richting));
        this.x = x;
        this.z = z;
        this.onder = onder;
        this.boven = boven;
        this.voeringTot = voeringTot;
        this.richting = richting.getAxis().isHorizontal() ? richting : Direction.SOUTH;
    }

    public GrotSchachtPiece(CompoundTag tag) {
        super(OnderWolBouw.SCHACHT.get(), tag);
        this.x = tag.getIntOr("x", 0);
        this.z = tag.getIntOr("z", 0);
        this.onder = tag.getIntOr("onder", 0);
        this.boven = tag.getIntOr("boven", 0);
        this.voeringTot = tag.getIntOr("voeringTot", 0);
        Direction d = Direction.from2DDataValue(tag.getIntOr("richting", 0));
        this.richting = d.getAxis().isHorizontal() ? d : Direction.SOUTH;
    }

    static BoundingBox doos(int x, int z, int onder, int boven, Direction richting) {
        return BoundingBox.fromCorners(new BlockPos(x - 2, onder - 1, z - 2), new BlockPos(x + 2, boven + 1, z + 2));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("x", x);
        tag.putInt("z", z);
        tag.putInt("onder", onder);
        tag.putInt("boven", boven);
        tag.putInt("voeringTot", voeringTot);
        tag.putInt("richting", richting.get2DDataValue());
    }

    private static BlockState blok(String id, Block anders) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return (b == Blocks.AIR ? anders : b).defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
                            ChunkPos chunkPos, BlockPos reference) {
        Direction dwars = richting.getClockWise();
        BlockState steen = blok("viltsteen_bakstenen", Blocks.STONE_BRICKS), wol = blok("pastelbaksteen_roze", Blocks.PINK_TERRACOTTA),
                lamp = blok("guh_kristal_lamp", Blocks.GLOWSTONE);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, richting);
        BlockState padOp = EilandenFeature.WOLKENLIFT.get().defaultBlockState().setValue(WolkenliftBlock.FACING, richting).setValue(WolkenliftBlock.DOWN, false);
        BlockState padNeer = padOp.setValue(WolkenliftBlock.DOWN, true);
        BlockState stroomOp = EilandenFeature.WOLKENSTROOM.get().defaultBlockState().setValue(WolkenstroomBlock.FACING, richting)
                .setValue(WolkenstroomBlock.DOWN, false);
        BlockState stroomNeer = stroomOp.setValue(WolkenstroomBlock.DOWN, true);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = onder; y <= boven; y++) {
            // the lining: the 5 x 3 ring around the inside, where there is ground or a fluid
            if (y <= voeringTot) {
                for (int a = -2; a <= 2; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if (b == 0 && a >= -1 && a <= 1) {
                            continue;
                        }
                        p.set(x + dwars.getStepX() * a + richting.getStepX() * b, y, z + dwars.getStepZ() * a + richting.getStepZ() * b);
                        if (!chunk.isInside(p)) {
                            continue;
                        }
                        BlockState s = level.getBlockState(p);
                        if (s.isAir() && s.getFluidState().isEmpty()) {
                            continue;
                        }
                        boolean lampje = Math.floorMod(y - onder, 7) == 4 && b == -1 && a == 0;
                        level.setBlock(p, lampje ? lamp : y >= GrotSchilProcessor.VILT_Y ? wol : steen, Block.UPDATE_CLIENTS);
                    }
                }
            }
            // the inside: ladder, lift up, lift down (the pads in the floor layer at the bottom)
            for (int a = -1; a <= 1; a++) {
                p.set(x + dwars.getStepX() * a, y, z + dwars.getStepZ() * a);
                if (!chunk.isInside(p)) {
                    continue;
                }
                BlockState s;
                if (a == -1) {
                    s = y == onder ? steen : ladder;
                } else if (a == 0) {
                    s = y == onder ? padOp : stroomOp;
                } else {
                    s = y == onder ? padNeer : stroomNeer;
                }
                level.setBlock(p, s, Block.UPDATE_CLIENTS);
            }
            // (the ladder needs its wall: the block behind it)
            p.set(x - dwars.getStepX() - richting.getStepX(), y, z - dwars.getStepZ() - richting.getStepZ());
            if (y > onder && chunk.isInside(p) && !GrotSchilProcessor.vast(level.getBlockState(p))) {
                level.setBlock(p, y >= GrotSchilProcessor.VILT_Y ? wol : steen, Block.UPDATE_CLIENTS);
            }
        }
        // (one more rung above the top, in the hut, so you can step off the ladder onto its floor)
        p.set(x - dwars.getStepX(), boven + 1, z - dwars.getStepZ());
        if (chunk.isInside(p) && level.getBlockState(p).isAir()) {
            level.setBlock(p, ladder, Block.UPDATE_CLIENTS);
        }
    }

    public int onder() {
        return onder;
    }

    public int boven() {
        return boven;
    }

    public Direction richting() {
        return richting;
    }
}
