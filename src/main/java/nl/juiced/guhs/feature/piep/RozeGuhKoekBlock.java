package nl.juiced.guhs.feature.piep;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The roze guh koek: a pink-glazed koekje with big glossy eyes and a snoetje. They come six to a clear plastic tray, and so
 * does this block ({@link #KOEKEN} 1-6, in a 2 x 3 grid, filled row by row).
 * <ul>
 *   <li>Place a koek on the ground: a tray with one. Right-click a tray with a koek: one more (up to six, like sea pickles).</li>
 *   <li>Right-click with an empty hand: eat one (food and "Lief kijken", {@link PiepEffecten.LiefKijken}); the tray is gone at 0.
 *       Sneak + right-click with an empty hand: take one.</li>
 *   <li>Breaking it drops every koek on it (loot table blocks/roze_guh_koek).</li>
 * </ul>
 * The item ({@link KoekItem}) is food too: eat it from your hand for the same buff.
 */
public class RozeGuhKoekBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RozeGuhKoekBlock> CODEC = simpleCodec(RozeGuhKoekBlock::new);
    public static final int MAX = 6;
    public static final IntegerProperty KOEKEN = IntegerProperty.create("koeken", 1, MAX);
    private static final VoxelShape NZ = Block.box(2, 0, 0.5, 14, 3, 15.5);
    private static final VoxelShape OW = Block.box(0.5, 0, 2, 15.5, 3, 14);

    public RozeGuhKoekBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KOEKEN, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KOEKEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NZ : OW;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState there = context.getLevel().getBlockState(context.getClickedPos());
        if (there.is(this)) {
            return there.setValue(KOEKEN, Math.min(MAX, there.getValue(KOEKEN) + 1));
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(KOEKEN) < MAX
                || super.canBeReplaced(state, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP) || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (player.isSecondaryUseActive()) {
            if (!player.getInventory().add(new ItemStack(PiepFeature.ROZE_GUH_KOEK_ITEM.get()))) {
                player.drop(new ItemStack(PiepFeature.ROZE_GUH_KOEK_ITEM.get()), false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.3f);
        } else {
            player.getFoodData().eat(PiepFeature.KOEK_ETEN.nutrition(), PiepFeature.KOEK_ETEN.saturation());
            gegeten(sp);
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.1f);
            ((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(PiepFeature.ROZE_GUH_KOEK_ITEM.get())),
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.05);
        }
        eentjeMinder(level, pos, state);
        return InteractionResult.CONSUME;
    }

    /** One koek off the tray (the tray goes when it is empty). */
    public static void eentjeMinder(Level level, BlockPos pos, BlockState state) {
        int n = state.getValue(KOEKEN);
        if (n <= 1) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(KOEKEN, n - 1), 3);
        }
    }

    /** A koek eaten (from the tray or the hand): "Lief kijken", the counter, the quest. */
    public static void gegeten(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(PiepFeature.LIEF_KIJKEN, PiepEffecten.LIEF_TICKS, 0));
        player.displayClientMessage(Component.translatable("gui.guhs.piep.lief_kijken").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        PiepVoortgang.tel(player, PiepVoortgang.KOEKJES, 1, "piep_koek");
        PiepVoortgang.pagina(player, "roze_guh_koek");
    }

    /** The koek item: place it (a tray) or eat it. */
    public static class KoekItem extends BlockItem {
        public KoekItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (entity instanceof ServerPlayer player) {
                gegeten(player);
            }
            return super.finishUsingItem(stack, level, entity);
        }

        @Override
        public InteractionResult place(BlockPlaceContext context) {
            InteractionResult r = super.place(context);
            if (r.consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
                GuhAdvancements.grant(player, "piep_koek");
                PiepVoortgang.pagina(player, "roze_guh_koek");
            }
            return r;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("block.guhs.roze_guh_koek.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }
}
