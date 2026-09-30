package nl.juiced.guhs.block;

import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A pink slime block (from guh slimeballs): bouncy, and sticky for pistons, just like a normal slime block. */
public class RozeSlijmBlock extends SlimeBlock {
    public RozeSlijmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }
}
