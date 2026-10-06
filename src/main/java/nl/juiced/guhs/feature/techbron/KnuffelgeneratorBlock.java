package nl.juiced.guhs.feature.techbron;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De Knuffelgenerator: a big pink cushion of 2 x 2 blocks (the kern is the quarter you place: the cushion grows to your left
 * and away from you; the other three are {@code guhs:techbron_kussen_deel}). Tamed guhs within 8 blocks come and lie on it by themselves, and every guh
 * that lies there gives vadskracht ({@link KnuffelgeneratorBlockEntity}). The embroidered face sleeps while the cushion is
 * empty, smiles with a guh on it and looks surprised when it is full.
 */
public class KnuffelgeneratorBlock extends BronBlock {
    public static final MapCodec<KnuffelgeneratorBlock> CODEC = simpleCodec(KnuffelgeneratorBlock::new);
    /** How high the cushion is, in pixels (guhs sink a little into the fluff: the model is two pixels higher). */
    public static final int HOOGTE = 6;
    private static final VoxelShape VORM = Block.box(0, 0, 0, 16, HOOGTE, 16);

    public KnuffelgeneratorBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int breed() {
        return 2;
    }

    @Override
    public int diep() {
        return 2;
    }

    @Override
    public Block deel() {
        return TechbronFeature.KUSSEN_DEEL.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KnuffelgeneratorBlockEntity(pos, state);
    }
}
