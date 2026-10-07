package nl.juiced.guhs.feature.snuffel;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import nl.juiced.guhs.network.ModNetworking;

/**
 * Het Guhstation ({@code guhs:guhstation}): a grey-black little console box with a guh snoet as its logo, the reward of
 * the first series of Het Snuffeleiland. A right-click opens a SMALL window (client.GuhstationScherm): a white field on
 * which your dog and your little brother or sister run in and play under the title "HET SNUFFEL EILAND", with "Druk op
 * start", your sniffing rank and number of scents, and the button "Nee ik wil even niet snuffelen, njeg". Start brings you
 * to the LAST spot you stood on the island, as a dog ({@link Reis#naarEiland}).
 * <p>
 * Anybody who has been on the island may use any Guhstation (it is a block: one per home is plenty). The server only
 * accepts "start" from a player it opened the window for, at that block, a moment ago.
 */
public class GuhstationBlock extends HorizontalDirectionalBlock {
    /** How long after opening the window "start" is accepted, and how far the player may have walked. */
    static final int GELDIG_TICKS = 20 * 120;
    static final double GELDIG_AFSTAND = 8;
    private static final Map<Direction, VoxelShape> VORM = new EnumMap<>(Direction.class);

    static {
        // the box (12 x 5 x 10 pixels) with its lid, seen from the front (north)
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VORM.put(d, d.getAxis() == Direction.Axis.Z ? Block.box(2, 0, 3, 14, 5, 13) : Block.box(3, 0, 2, 13, 5, 14));
        }
    }

    private record Open(GlobalPos plek, long tick) {
    }

    private static final Map<UUID, Open> OPEN = new ConcurrentHashMap<>();

    public GuhstationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(GuhstationBlock::new);
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
        return VORM.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            open(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Opens the little window for this player (or says why not). */
    public static boolean open(ServerPlayer p, BlockPos pos) {
        if (!Reis.bezocht(p) || !Keuze.heeft(p)) {
            // (never been there: the Guhstation is what the island gives you, the dock is the way in)
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.guhstation.onbekend").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (Hondvorm.actief(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.snuffel.reis.al_daar").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        OPEN.put(p.getUUID(), new Open(GlobalPos.of(p.level().dimension(), pos.immutable()), p.level().getServer().getTickCount()));
        CompoundTag data = Stand.van(p);
        ModNetworking.sendTo(p, new SnuffelPayloads.Open(SnuffelPayloads.Open.GUHSTATION, data));
        p.level().playSound(null, pos, SnuffelFeature.GUHSTATION_GELUID.get(), SoundSource.BLOCKS, 0.6f, 1f);
        return true;
    }

    /** "Druk op start": off to the island, when the window was opened for this player at a Guhstation nearby. */
    static boolean start(ServerPlayer p) {
        Open o = OPEN.remove(p.getUUID());
        if (o == null || o.plek().dimension() != p.level().dimension() || p.level().getServer().getTickCount() - o.tick() > GELDIG_TICKS
                || o.plek().pos().distToCenterSqr(p.position()) > GELDIG_AFSTAND * GELDIG_AFSTAND
                || !(p.level().getBlockState(o.plek().pos()).getBlock() instanceof GuhstationBlock)) {
            return false;
        }
        return Reis.naarEiland(p, Reis.Aankomst.LAATSTE);
    }

    static void vergeet(UUID speler) {
        OPEN.remove(speler);
    }

    /** (Tests) is "start" waiting for this player? */
    static boolean wachtOpStart(ServerPlayer p) {
        return OPEN.containsKey(p.getUUID());
    }
}
