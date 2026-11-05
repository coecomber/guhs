package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.ErfenisData;

/**
 * Het Erfhekje ({@code guhs:erfhekje}, ronde 3 + 14): a pink little gate of two blocks high with a sleepy guh face on top and a
 * soft glowing doorway, for your own base in the main world. Walk through it (or right-click it) and you go to YOUR erfenis
 * ({@code Erfenis.gaNaar(player, eigen base)}); "terug" from the village puts you right in front of the hekje you used, looking
 * away from it ({@link KoppelingData.Speler#hekje}, also written as the core's way back). Any number of hekjes per player: the
 * last one used wins. Only for players who bought their erfenis (the letter); creative players may always.
 */
public class ErfhekjeBlock extends Block {
    public static final MapCodec<ErfhekjeBlock> CODEC = simpleCodec(ErfhekjeBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /** Ticks between two trips through a hekje (so you don't bounce straight back). */
    static final int RUST = 60;
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF9ACD, 1.0f), LILA = new DustParticleOptions(0xD8B4FF, 0.8f);

    // the two posts (the gate stands across the FACING axis); the upper half also has the beam with the face
    private static final VoxelShape PALEN_NZ = Shapes.or(box(0, 0, 5, 3, 16, 11), box(13, 0, 5, 16, 16, 11));
    private static final VoxelShape PALEN_OW = Shapes.or(box(5, 0, 0, 11, 16, 3), box(5, 0, 13, 11, 16, 16));
    private static final VoxelShape BOVEN_NZ = Shapes.or(PALEN_NZ, box(0, 13, 5, 16, 16, 11));
    private static final VoxelShape BOVEN_OW = Shapes.or(PALEN_OW, box(5, 13, 0, 11, 16, 16));

    /** Players who walked in, waiting for the end of the tick to travel (never teleport in the middle of a move). */
    private static final Map<UUID, BlockPos> ONDERWEG = new ConcurrentHashMap<>();
    /** Player -> server tick before which a hekje does nothing for them. */
    private static final Map<UUID, Long> RUSTIG = new ConcurrentHashMap<>();

    public ErfhekjeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF);
    }

    // =====================================================================================================================
    // two blocks high (like a door)
    // =====================================================================================================================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction richting,
                                     BlockPos buurPos, BlockState buur, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (richting.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (richting == Direction.UP)) {
            return buur.is(this) && buur.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, richting, buurPos, buur, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos onder = pos.below();
            BlockState o = level.getBlockState(onder);
            if (o.is(this) && o.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(onder, Blocks.AIR.defaultBlockState(), 35);   // (no drop from the lower half in creative)
                level.levelEvent(player, 2001, onder, Block.getId(o));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean nz = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return nz ? BOVEN_NZ : BOVEN_OW;
        }
        return nz ? PALEN_NZ : PALEN_OW;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? PALEN_NZ : PALEN_OW;   // (you walk right through the doorway)
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // =====================================================================================================================
    // going through
    // =====================================================================================================================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            BlockPos onder = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            if (mag(p, true)) {
                ONDERWEG.put(p.getUUID(), onder);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precies) {
        if (!(entity instanceof ServerPlayer p) || state.getValue(HALF) != DoubleBlockHalf.LOWER || p.isShiftKeyDown()) {
            return;
        }
        // only when the player really stands in the doorway (between the posts), not against a post
        if (!pos.equals(p.blockPosition())) {
            return;
        }
        Long rust = RUSTIG.get(p.getUUID());
        if (rust != null && rust > p.level().getServer().getTickCount()) {
            return;
        }
        if (mag(p, false)) {
            ONDERWEG.put(p.getUUID(), pos.immutable());
        } else {
            RUSTIG.put(p.getUUID(), p.level().getServer().getTickCount() + (long) RUST);   // (the message once, not every tick)
        }
    }

    /** May this player use a hekje (bought their erfenis, or creative)? Tells them why not. */
    static boolean mag(ServerPlayer p, boolean klik) {
        if (Makelaarsbrief.gekocht(p.level().getServer(), p.getUUID()) || p.isCreative() || Erfenis.in(p)) {
            return true;
        }
        p.sendOverlayMessage(Component.translatable("erfenis.koppeling.hekje.op_slot").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (klik) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        return false;
    }

    /** End of every server tick: the players who stepped into a hekje travel. */
    static void tick(ServerTickEvent.Post event) {
        if (ONDERWEG.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Map.Entry<UUID, BlockPos> e : Map.copyOf(ONDERWEG).entrySet()) {
            ONDERWEG.remove(e.getKey());
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null && p.isAlive()) {
                reis(p, e.getValue());
            }
        }
    }

    /** Through the hekje at {@code onder} (its lower half) to the player's own erfenis; the way back is in front of this hekje. */
    public static boolean reis(ServerPlayer p, BlockPos onder) {
        MinecraftServer server = p.level().getServer();
        ServerLevel van = p.level();
        BlockState state = van.getBlockState(onder);
        Direction voor = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.fromYRot(p.getYRot()).getOpposite();
        // the side of the gate the player came from (else the front): there they land when they come back
        Vec3 midden = Vec3.atBottomCenterOf(onder);
        Vec3 rel = p.position().subtract(midden);
        Direction kant = voor;
        if (rel.x * voor.getOpposite().getStepX() + rel.z * voor.getOpposite().getStepZ() > 0.05) {
            kant = voor.getOpposite();
        }
        Vec3 terug = midden.add(kant.getStepX() * 1.2, 0, kant.getStepZ() * 1.2);
        float yaw = kant.toYRot();
        RUSTIG.put(p.getUUID(), server.getTickCount() + (long) RUST);
        van.sendParticles(ROZE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        van.playSound(null, onder, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.3f);
        if (van.dimension() == nl.juiced.guhs.feature.erfenis.Plot.DIM) {
            // 1.9.7: a Terughekje (deco) inside the erfenis: back to where you came from, like "terug"
            Erfenis.terug(p);
            rust(p);
            return true;
        }
        if (!Erfenis.gaNaar(p, Erfenis.eigenBase(p))) {
            p.sendOverlayMessage(Component.translatable("erfenis.koppeling.hekje.dicht").withStyle(ChatFormatting.RED));
            return false;
        }
        KoppelingData d = KoppelingData.get(server);
        KoppelingData.Speler s = d.speler(p.getUUID());
        s.hekje = GlobalPos.of(van.dimension(), onder.immutable());
        s.hekjeYaw = yaw;
        d.setDirty();
        ErfenisData.get(server).zetTerug(p.getUUID(), new ErfenisData.Terug(van.dimension().identifier().toString(), terug.x, terug.y, terug.z, yaw, 0f));
        return true;
    }

    /** The hekje this player last went through (null: never). */
    @Nullable
    public static GlobalPos laatste(MinecraftServer server, UUID speler) {
        KoppelingData.Speler s = KoppelingData.get(server).spelerAls(speler);
        return s == null ? null : s.hekje;
    }

    /** Coming back also rests a moment (so you don't walk straight back in). */
    static void rust(ServerPlayer p) {
        RUSTIG.put(p.getUUID(), p.level().getServer().getTickCount() + (long) RUST);
    }

    // =====================================================================================================================
    // the glow
    // =====================================================================================================================

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        boolean nz = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        boolean onder = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        for (int i = 0; i < 2; i++) {
            double a = 0.25 + r.nextDouble() * 0.5, h = r.nextDouble(), d = 0.5 + (r.nextDouble() - 0.5) * 0.2;
            double x = pos.getX() + (nz ? a : d), z = pos.getZ() + (nz ? d : a);
            level.addParticle(r.nextInt(3) == 0 ? LILA : ROZE, x, pos.getY() + h, z, 0, 0.02, 0);
        }
        if (onder && r.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.2 + r.nextDouble() * 0.5, pos.getZ() + 0.5, 0, 0, 0);
        }
        if (!onder && r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.6, pos.getY() + 0.9, pos.getZ() + 0.5
                    + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        }
        if (onder && r.nextInt(80) == 0) {
            level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.3f, 1.4f + r.nextFloat() * 0.3f, false);
        }
    }
}
