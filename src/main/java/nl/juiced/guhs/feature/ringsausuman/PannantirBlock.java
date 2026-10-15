package nl.juiced.guhs.feature.ringsausuman;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * De Pannantir: Sausuman's seeing pan (the palantir of this story): a frying pan on a stand, full of sauce that shows
 * things. Look into it (a click) and it shows one of {@link #BEELDEN} little visions, mostly the Eye of Sausron, who is
 * hungry too. Decoration; the one in the tower's study is his, every player gets their own at the end of the questline.
 */
public class PannantirBlock extends Block {
    public static final MapCodec<PannantirBlock> CODEC = simpleCodec(PannantirBlock::new);
    /** How many visions there are (lang gui.guhs.ringsausuman.pannantir.0 .. BEELDEN - 1). */
    public static final int BEELDEN = 8;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 6, 2, 14, 9, 14), Block.box(6, 0, 6, 10, 6, 10));

    public PannantirBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("gui.guhs.ringsausuman.pannantir.kijk",
                    Component.translatable("gui.guhs.ringsausuman.pannantir." + beeld(sp))));
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.4f);
            GuhAdvancements.grant(sp, "ring_sausuman_pannantir");
        }
        return InteractionResult.SUCCESS;
    }

    /** Which vision this player gets now: never the same one twice in a row. */
    static int beeld(ServerPlayer p) {
        int vorige = RingSausumanFeature.LIJN.teller(p, "beeld");
        int nu = (vorige + 1 + p.getRandom().nextInt(BEELDEN - 1)) % BEELDEN;
        RingSausumanFeature.LIJN.teller(p, "beeld", nu);
        return nu;
    }

    /** The sauce simmers. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.6,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0.02, 0);
        }
    }
}
