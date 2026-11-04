package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A frozen pillar block of the Galmwolf arena: BARST 0 = whole, 1 = cracked (the next howl shatters it). Unbreakable by hand. */
public class IJspilaarBlok extends Block {
    public static final MapCodec<IJspilaarBlok> CODEC = simpleCodec(IJspilaarBlok::new);
    public static final IntegerProperty BARST = IntegerProperty.create("barst", 0, 1);

    public IJspilaarBlok(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BARST, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BARST);
    }
}
