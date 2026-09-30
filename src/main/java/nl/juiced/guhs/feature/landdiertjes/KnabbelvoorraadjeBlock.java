package nl.juiced.guhs.feature.landdiertjes;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;
/**
 * A knabbelvoorraadje: a little mound of earth and leaves where a pluiseekhoorntje hid kaasknabbels (a knabbel peeks out).
 * Right-click it: you dig it up and get the knabbels ({@link #KNABBELS}); breaking it drops them too. Only squirrels make
 * them (no item): wild ones bury what they find, a tamed one now and then digs one up right next to its owner.
 */
public class KnabbelvoorraadjeBlock extends Block {
    public static final MapCodec<KnabbelvoorraadjeBlock> CODEC = simpleCodec(KnabbelvoorraadjeBlock::new);
    public static final int MAX = 8;
    public static final IntegerProperty KNABBELS = IntegerProperty.create("knabbels", 1, MAX);
    private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 4, 14);

    public KnabbelvoorraadjeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KNABBELS, 3));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KNABBELS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos otherPos, BlockState other, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, ticks, pos, dir, otherPos, other, random);
    }

    /** Can a stash go here (air or grass, on sturdy ground)? */
    public static boolean past(Level level, BlockPos pos) {
        BlockState hier = level.getBlockState(pos);
        return (hier.isAir() || hier.canBeReplaced() && hier.getFluidState().isEmpty())
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Puts a stash with this many knabbels here (or adds them to the one that is already here). */
    public static boolean verstop(ServerLevel level, BlockPos pos, int knabbels) {
        BlockState hier = level.getBlockState(pos);
        if (hier.getBlock() instanceof KnabbelvoorraadjeBlock) {
            level.setBlock(pos, hier.setValue(KNABBELS, Math.min(MAX, hier.getValue(KNABBELS) + knabbels)), 3);
        } else if (past(level, pos)) {
            level.setBlock(pos, LanddiertjesFeature.KNABBELVOORRAADJE.get().defaultBlockState().setValue(KNABBELS, Math.max(1, Math.min(MAX, knabbels))), 3);
        } else {
            return false;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.2,
                pos.getZ() + 0.5, 10, 0.25, 0.05, 0.25, 0.05);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            int n = state.getValue(KNABBELS);
            ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get(), n);
            if (!sp.getInventory().add(knabbels)) {
                sp.drop(knabbels, false);
            }
            sl.removeBlock(pos, false);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.2,
                    pos.getZ() + 0.5, 14, 0.25, 0.05, 0.25, 0.08);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
            sl.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
            sp.sendOverlayMessage(Component.translatable("gui.guhs.landdiertjes.voorraadje_gevonden", n).withStyle(ChatFormatting.GOLD));
            GidsFeature.grant(sp, "diertjes/landdiertjes_voorraadje");
        }
        return InteractionResult.SUCCESS;
    }
}
