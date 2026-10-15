package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;

/**
 * The Plantagebakken as other features see them (the plantage chore of the chore slice: "plant + chop in Plantagebakken
 * only"). Everything a chore needs:
 * <pre>
 * for (PlantagebakBlockEntity bak : Plantagebakken.rond(level, huisjePos, 16)) {
 *     if (bak.heeftBoom()) {                         // a tree the bak grew stands on it
 *         BlockPos stam = bak.plantPlek();           // walk here (the trunk), swing the axe...
 *         List&lt;ItemStack&gt; buit = bak.hak(guh);       // ...and the whole tree comes down; saplings of its kind are
 *     }                                              //    back in the bak already, the rest is the harvest
 *     if (bak.stand() == PlantagebakBlockEntity.Stand.LEEG &amp;&amp; bak.voorraad().isEmpty()) {
 *         bak.plant(zaailingen);                     // puts saplings in (the stack shrinks); the bak plants one itself
 *     }
 * }
 * </pre>
 * A bak only works while it has vadskracht ({@code bak.heeftKracht()}); the tree stands 45 seconds of work after the
 * sapling was planted. {@code bak.stam()} lists the logs that still stand (for a chore that wants to look at them).
 */
public final class Plantagebakken {
    /** May this go into a Plantagebak: a sapling of any mod, a nether fungus (the sate- and worstzwammetje), a mushroom, an azalea? */
    public static boolean isZaailing(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem item && isZaailing(item.getBlock());
    }

    public static boolean isZaailing(Block block) {
        return block instanceof SaplingBlock || block instanceof NetherFungusBlock || block instanceof MushroomBlock || block instanceof AzaleaBlock
                || block.builtInRegistryHolder().is(BlockTags.SAPLINGS);
    }

    /** The bak this block belongs to (its kern or one of its eight other blocks); null when it is no Plantagebak. */
    @Nullable
    public static PlantagebakBlockEntity bij(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos kern = state.getBlock() instanceof PlantagebakDeelBlock ? MachineDeelBlock.kern(state, pos) : pos;
        return level.getBlockEntity(kern) instanceof PlantagebakBlockEntity bak ? bak : null;
    }

    /** Every Plantagebak whose kern lies within {@code straal} blocks (sideways) of this spot, in loaded chunks. */
    public static List<PlantagebakBlockEntity> rond(ServerLevel level, BlockPos midden, int straal) {
        List<PlantagebakBlockEntity> uit = new ArrayList<>();
        int x0 = (midden.getX() - straal) >> 4, x1 = (midden.getX() + straal) >> 4, z0 = (midden.getZ() - straal) >> 4, z1 = (midden.getZ() + straal) >> 4;
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof PlantagebakBlockEntity bak && !bak.isRemoved()
                            && Math.abs(be.getBlockPos().getX() - midden.getX()) <= straal && Math.abs(be.getBlockPos().getZ() - midden.getZ()) <= straal) {
                        uit.add(bak);
                    }
                }
            }
        }
        return uit;
    }

    private Plantagebakken() {
    }
}
