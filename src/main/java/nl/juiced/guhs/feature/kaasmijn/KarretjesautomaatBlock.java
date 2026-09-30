package nl.juiced.guhs.feature.kaasmijn;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cart dispenser at the stations of the Kaasexpress: press it and a minecart appears on the rails next to it
 * (one at a time). The carts can't be broken (no free minecarts) and go away by themselves when nobody has been in
 * them for a minute (the Mijnguh cleans them up, see {@link #cleanupCarts}).
 */
public class KarretjesautomaatBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KarretjesautomaatBlock> CODEC = simpleCodec(KarretjesautomaatBlock::new);
    /** Carts from a dispenser (the ones built into the mine have "guhs_kaasmijn" and stay). */
    public static final String CART_TAG = "guhs_kaasmijn_kar";
    public static final String IDLE_KEY = "guhs_kaasmijn_idle";
    public static final int IDLE_TICKS = 20 * 60;

    public KarretjesautomaatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            dispense(server, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Puts a cart on the nearest rail within 2 blocks, unless one is already waiting there. True if it made one. */
    public static boolean dispense(ServerLevel level, BlockPos pos, @Nullable ServerPlayer player) {
        BlockPos rail = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (BaseRailBlock.isRail(level.getBlockState(p)) && (rail == null || p.distSqr(pos) < rail.distSqr(pos))) {
                rail = p.immutable();
            }
        }
        if (rail == null) {
            tell(player, "quest.guhs.kaasmijn.cart_norail");
            return false;
        }
        if (!level.getEntitiesOfClass(AbstractMinecart.class, new AABB(rail).inflate(1.5)).isEmpty()) {
            tell(player, "quest.guhs.kaasmijn.cart_ready");
            return false;
        }
        Minecart cart = new Minecart(level, rail.getX() + 0.5, rail.getY() + 0.0625, rail.getZ() + 0.5);
        cart.setInvulnerable(true);
        cart.addTag(CART_TAG);
        level.addFreshEntity(cart);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        level.playSound(null, rail, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.3f, 1.4f);
        tell(player, "quest.guhs.kaasmijn.cart_new");
        return true;
    }

    /** Called every so many ticks: dispenser carts without anyone in them for {@link #IDLE_TICKS} go away. Returns how many. */
    public static int cleanupCarts(ServerLevel level, AABB area, int ticksPassed) {
        int removed = 0;
        for (AbstractMinecart cart : level.getEntitiesOfClass(AbstractMinecart.class, area, c -> c.getTags().contains(CART_TAG))) {
            var data = cart.getPersistentData();
            if (cart.isVehicle()) {
                data.putInt(IDLE_KEY, 0);
                continue;
            }
            int idle = data.getInt(IDLE_KEY) + ticksPassed;
            data.putInt(IDLE_KEY, idle);
            if (idle >= IDLE_TICKS) {
                cart.discard();
                removed++;
            }
        }
        return removed;
    }

    private static void tell(@Nullable ServerPlayer player, String key) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), true);
        }
    }
}
