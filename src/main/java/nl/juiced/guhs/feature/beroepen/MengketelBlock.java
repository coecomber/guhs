package nl.juiced.guhs.feature.beroepen;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The mengketel (the Apotheekje): a round pink pot with a little guh face, always gently bubbling. Right-click it with
 * {@link Apotheek#KRUIDJES} snotkruidjes and a kaasmelk in your pockets: blub blub... a kaasmelkdrankje.
 */
public class MengketelBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MengketelBlock> CODEC = simpleCodec(MengketelBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 13, 15);

    public MengketelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            meng((ServerLevel) level, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.85,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }

    /** Mixes a kaasmelkdrankje from the player's snotkruidjes and kaasmelk: true when it worked. */
    public static boolean meng(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int kruidjes = GuhQuests.count(player, BeroepenFeature.SNOTKRUIDJE.get());
        ItemStack melk = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(KnusTags.KAASMELK)) {
                melk = s;
                break;
            }
        }
        if (kruidjes < Apotheek.KRUIDJES || melk.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.mengketel.nodig", Math.min(kruidjes, Apotheek.KRUIDJES),
                    Apotheek.KRUIDJES, melk.isEmpty() ? 0 : 1).withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        GuhQuests.take(player, BeroepenFeature.SNOTKRUIDJE.get(), Apotheek.KRUIDJES);
        melk.shrink(1);                                               // (its bottle goes into the drankje)
        Minigames.give(player, new ItemStack(BeroepenFeature.KAASMELKDRANKJE.get()));
        double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
        level.playSound(null, pos, BeroepenFeature.BUBBEL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 20, 0.25, 0.1, 0.25, 0.05);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.3, z, 10, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.2, z, 6, 0.2, 0.2, 0.2, 0.02);
        player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.mengketel.klaar").withStyle(ChatFormatting.GOLD));
        return true;
    }
}
