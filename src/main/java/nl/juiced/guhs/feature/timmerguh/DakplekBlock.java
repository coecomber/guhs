package nl.juiced.guhs.feature.timmerguh;

import java.util.Locale;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A see-through ghost tile on the Timmerguh's half-built guhhuisje: a piece of the oortjesdak still goes here (right-click it
 * with a {@link DakpluisjeItem}). {@link Deel} says which: the fluffy roof (becomes guhs:pluisdak), an ear (pink wool) or the
 * inside of an ear (magenta wool). Solid to walk on, can't be broken in survival.
 */
public class DakplekBlock extends Block {
    public static final MapCodec<DakplekBlock> CODEC = simpleCodec(DakplekBlock::new);
    public static final EnumProperty<Deel> DEEL = EnumProperty.create("deel", Deel.class);

    /** What part of the roof goes on this spot. */
    public enum Deel implements StringRepresentable {
        DAK, OOR, BINNENOOR;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The block that is laid here. */
        public BlockState gelegd() {
            return switch (this) {
                case DAK -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(nl.juiced.guhs.Guhs.id("pluisdak")).defaultBlockState();
                case OOR -> Blocks.PINK_WOOL.defaultBlockState();
                case BINNENOOR -> Blocks.MAGENTA_WOOL.defaultBlockState();
            };
        }
    }

    public DakplekBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEEL, Deel.DAK));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEEL);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
        return neighbour.is(this) || super.skipRendering(state, neighbour, side);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
