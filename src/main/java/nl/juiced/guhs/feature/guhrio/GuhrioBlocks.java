package nl.juiced.guhs.feature.guhrio;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The blocks of Super Guhrio. A level is simply built with them in its lanes; none of them ever changes in the world:
 * what a block is for one player (a coin taken, a ?-block empty, a brick broken) lives in that player's
 * {@link GuhrioSpel.Sessie} and is drawn per player (client.StukRenderer), so any number of players have their own level.
 * <ul>
 *     <li>{@link StartBlok}: the way in (walk into it or click it); it knows which level (its block entity) and which way
 *     the level runs (its facing).</li>
 *     <li>{@link VraagBlok}: bump it from below: a coin or a power-up, once per run per player.</li>
 *     <li>{@link SteenBlok}: a brick; it hops when you bump it, and breaks (for you) when you carry the Superknabbel.</li>
 *     <li>{@link MuntBlok}: a coin floating in the lane.</li>
 *     <li>{@link VlagBlok}: a flag: your spot to come back to. {@link MastBlok}: the flagpole, the end of the level.</li>
 *     <li>{@link PijpBlok} (the mouth) and {@link PijpLijfBlok}: a green pipe; duck on the mouth and you come out of the
 *     other mouth with the same {@link PijpBlok#KANAAL} in this level.</li>
 *     <li>{@link DeurBlok}: a door (two high); press W in it and you step out of the other door with the same channel.</li>
 *     <li>{@link GuhmbaPlek}: where a Guhmba lives (invisible).</li>
 * </ul>
 */
public final class GuhrioBlocks {
    private GuhrioBlocks() {
    }

    /** What a ?-block holds. */
    public enum Inhoud implements StringRepresentable {
        MUNT, SUPERKNABBEL, VUURPEPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Inhoud> INHOUD = EnumProperty.create("inhoud", Inhoud.class);
    /** Only for the look (the empty ?-block is this state's model): in the world it is always false. */
    public static final BooleanProperty LEEG = BooleanProperty.create("leeg");

    @Nullable
    private static Player speler(CollisionContext context) {
        return context instanceof EntityCollisionContext e && e.getEntity() instanceof Player p ? p : null;
    }

    // =====================================================================================================================
    // the way in
    // =====================================================================================================================

    /** Which level a start block starts; for a gate ({@link PoortBlok}) also where that level's start block is. */
    public static class StartBlockEntity extends BlockEntity {
        private String level = "";
        /** A gate: the level's start block, in the gate's own frame (+x the way the gate faces, +z its right hand). */
        private BlockPos naar = BlockPos.ZERO;

        public BlockPos naar() {
            return naar;
        }

        public void zetNaar(BlockPos naar) {
            this.naar = naar.immutable();
            setChanged();
        }

        public StartBlockEntity(BlockPos pos, BlockState state) {
            super(GuhrioFeature.START_BE.get(), pos, state);
        }

        public String level() {
            return level;
        }

        public void zetLevel(String level) {
            this.level = level == null ? "" : level;
            setChanged();
        }

        @Override
        protected void saveAdditional(ValueOutput tag) {
            super.saveAdditional(tag);
            tag.putString("Level", level);
            tag.putInt("NaarX", naar.getX());
            tag.putInt("NaarY", naar.getY());
            tag.putInt("NaarZ", naar.getZ());
        }

        @Override
        protected void loadAdditional(ValueInput tag) {
            super.loadAdditional(tag);
            level = tag.getStringOr("Level", "");
            naar = new BlockPos(tag.getIntOr("NaarX", 0), tag.getIntOr("NaarY", 0), tag.getIntOr("NaarZ", 0));
        }
    }

    /**
     * The start of a level: a little arch in the lane. Walk into it (or click it) and you are in the level it names,
     * which runs the way it faces. You have to step out of it before it takes you again.
     */
    public static class StartBlok extends HorizontalDirectionalBlock implements EntityBlock {
        public static final MapCodec<StartBlok> CODEC = simpleCodec(StartBlok::new);
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

        public StartBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST));
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
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StartBlockEntity(pos, state);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            if (!level.isClientSide() && entity instanceof ServerPlayer player && GuhrioSpel.magStarten(player)) {
                GuhrioSpel.start(player, pos);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp && GuhrioSpel.sessie(sp) == null) {
                GuhrioSpel.start(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /**
     * A gate to a level whose lanes are somewhere else (the level hall of the castle): walk into it (or click it) and you
     * are in the level of the start block its block entity points at ({@link StartBlockEntity#naar}, counted in the gate's
     * own frame, so it turns with the building). Refused levels (an earlier one must be done first) say so.
     */
    public static class PoortBlok extends HorizontalDirectionalBlock implements EntityBlock {
        public static final MapCodec<PoortBlok> CODEC = simpleCodec(PoortBlok::new);
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

        public PoortBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST));
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
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StartBlockEntity(pos, state);
        }

        /** The start block this gate leads to, or null. */
        @Nullable
        public static BlockPos start(Level level, BlockPos pos, BlockState state) {
            if (!(level.getBlockEntity(pos) instanceof StartBlockEntity be) || be.naar().equals(BlockPos.ZERO)) {
                return null;
            }
            return GuhrioLevel.wereld(pos, state.getValue(FACING), be.naar());
        }

        private static void ga(ServerPlayer player, BlockPos pos, BlockState state) {
            BlockPos start = start(player.level(), pos, state);
            if (start != null) {
                GuhrioSpel.start(player, start);
            }
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
            if (!level.isClientSide() && entity instanceof ServerPlayer player && GuhrioSpel.magStarten(player)) {
                ga(player, pos, state);
                GuhrioSpel.rust(player, 30);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp && GuhrioSpel.sessie(sp) == null) {
                ga(sp, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    // the pieces that are drawn per player
    // =====================================================================================================================

    /** A ?-block, a brick or a coin: drawn per player by client.StukRenderer; remembers only when it last hopped (client). */
    public static class StukBlockEntity extends BlockEntity {
        /** Client: the game time it was last bumped (it hops for a few ticks), or far in the past. */
        public long bots = Long.MIN_VALUE / 2;

        public StukBlockEntity(BlockPos pos, BlockState state) {
            super(GuhrioFeature.STUK_BE.get(), pos, state);
        }
    }

    /** The shared part of the per-player pieces: invisible in the world's own drawing, a block entity for the real drawing. */
    public abstract static class GetekendStuk extends Block implements EntityBlock, GuhrioStuk {
        protected GetekendStuk(Properties properties) {
            super(properties);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
            return 1f;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StukBlockEntity(pos, state);
        }
    }

    /** The ?-block: bump it from below. A coin or a power-up comes out, once per run for every player. */
    public static class VraagBlok extends GetekendStuk {
        public static final MapCodec<VraagBlok> CODEC = simpleCodec(VraagBlok::new);

        public VraagBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(INHOUD, Inhoud.MUNT).setValue(LEEG, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(INHOUD, LEEG);
        }

        @Override
        public void bots(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) != 0) {
                return;
            }
            GuhrioSpel.zetStaat(player, sessie, pos, 1);
            ServerLevel level = player.level();
            Vec3 boven = Vec3.atBottomCenterOf(pos.above());
            Inhoud inhoud = state.getValue(INHOUD);
            if (inhoud != Inhoud.MUNT) {
                // (the Vuurpeper is never taken away by a Superknabbel)
                if (inhoud == Inhoud.VUURPEPER || sessie.kracht == GuhrioSpel.Kracht.GEEN) {
                    GuhrioSpel.zetKracht(player, sessie, inhoud == Inhoud.VUURPEPER ? GuhrioSpel.Kracht.VUUR : GuhrioSpel.Kracht.SUPER);
                }
                GuhrioSpel.geluid(level, boven, SoundEvents.PLAYER_LEVELUP, 0.6f, inhoud == Inhoud.VUURPEPER ? 1.9f : 1.6f);
                level.sendParticles(player, inhoud == Inhoud.VUURPEPER ? ParticleTypes.FLAME : ParticleTypes.HAPPY_VILLAGER, false, false,
                        boven.x, boven.y + 0.3, boven.z, 10, 0.3, 0.3, 0.3, 0.02);
            } else {
                GuhrioSpel.muntVan(player, sessie, pos);
                GuhrioSpel.geluidAnderen(player, boven, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                level.sendParticles(player, ParticleTypes.WAX_ON, false, false, boven.x, boven.y + 0.4, boven.z, 6, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }

    /**
     * A brick. It hops when you bump it; with the Superknabbel it breaks, for you alone: you (and only you) walk through
     * where it was until you start the level again.
     */
    public static class SteenBlok extends GetekendStuk {
        public static final MapCodec<SteenBlok> CODEC = simpleCodec(SteenBlok::new);

        public SteenBlok(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            Player p = speler(context);
            return p != null && GuhrioSpel.staat(p, pos) == 1 ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
        }

        @Override
        public void bots(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) != 0 || !sessie.kracht.groot()) {
                return;
            }
            GuhrioSpel.zetStaat(player, sessie, pos, 1);
            ServerLevel level = player.level();
            GuhrioSpel.geluid(level, Vec3.atCenterOf(pos), SoundEvents.DECORATED_POT_SHATTER, 0.7f, 1.0f);
            level.sendParticles(player, new BlockParticleOption(ParticleTypes.BLOCK, state), false, false,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /** A coin in the lane: walk or jump through it. Back when you start the level again; everybody has their own. */
    public static class MuntBlok extends GetekendStuk {
        public static final MapCodec<MuntBlok> CODEC = simpleCodec(MuntBlok::new);
        private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

        public MuntBlok(Properties properties) {
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
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) != 0) {
                return;
            }
            GuhrioSpel.zetStaat(player, sessie, pos, 1);
            GuhrioSpel.muntVan(player, sessie, pos);
            GuhrioSpel.geluidAnderen(player, Vec3.atCenterOf(pos), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.45f, 1.7f);
        }
    }

    // =====================================================================================================================
    // flags
    // =====================================================================================================================

    /** A flag on a little pole: touch it and this is where you come back to. */
    public static class VlagBlok extends Block implements GuhrioStuk {
        public static final MapCodec<VlagBlok> CODEC = simpleCodec(VlagBlok::new);
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

        public VlagBlok(Properties properties) {
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
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            GuhrioSpel.vlag(player, sessie, pos);
        }
    }

    /** The flagpole at the end of a level (stack them; the top one carries the flag): touch it anywhere and you are done. */
    public static class MastBlok extends Block implements GuhrioStuk {
        public static final MapCodec<MastBlok> CODEC = simpleCodec(MastBlok::new);
        public static final BooleanProperty TOP = BooleanProperty.create("top");
        private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

        public MastBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(TOP, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TOP);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            // the top one carries the flag (placing one on top of another moves the flag up: see neighbour updates below)
            return defaultBlockState().setValue(TOP, !(context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() instanceof MastBlok));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            GuhrioSpel.klaar(player, sessie, pos);
        }
    }

    // =====================================================================================================================
    // the green pipe
    // =====================================================================================================================

    /** A player who is going through a pipe walks (sinks) right through the pipe's blocks; everybody else stands on them. */
    private static VoxelShape pijpVorm(Block blok, VoxelShape vorm, CollisionContext context) {
        Player p = speler(context);
        return p != null && GuhrioSpel.inPijp(p) ? Shapes.empty() : vorm;
    }

    /** The body of a green pipe (behind its mouth): a pillar along its axis (y under an upward mouth). */
    public static class PijpLijfBlok extends net.minecraft.world.level.block.RotatedPillarBlock {
        public static final MapCodec<PijpLijfBlok> CODEC = simpleCodec(PijpLijfBlok::new);

        public PijpLijfBlok(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<? extends net.minecraft.world.level.block.RotatedPillarBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return pijpVorm(this, super.getCollisionShape(state, level, pos, context), context);
        }
    }

    /**
     * The mouth of a green pipe. It opens the way it faces: up (stand on it and duck, S), down (it hangs from above: a way
     * out only) or sideways (walk into it; a sideways mouth is two blocks high: the upper block has {@link #BOVEN} and is
     * only a look). You come out of the other mouth of this level with the same {@link #KANAAL} (with more than two: the
     * next one in the level). {@link #INGANG} false: a way out only (a one-way pipe).
     */
    public static class PijpBlok extends Block implements GuhrioStuk {
        public static final MapCodec<PijpBlok> CODEC = simpleCodec(PijpBlok::new);
        public static final IntegerProperty KANAAL = IntegerProperty.create("kanaal", 0, 15);
        public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
        public static final BooleanProperty INGANG = BooleanProperty.create("ingang");
        public static final BooleanProperty BOVEN = BooleanProperty.create("boven");
        /** How far inside a sideways or hanging mouth you are while you travel. */
        public static final double ZIJ_DIEP = 1.3;

        public PijpBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KANAAL, 0).setValue(FACING, Direction.UP).setValue(INGANG, true).setValue(BOVEN, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KANAAL, FACING, INGANG, BOVEN);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getClickedFace());
        }

        @Override
        protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return pijpVorm(this, super.getCollisionShape(state, level, pos, context), context);
        }

        /** Click it with an empty hand (creative): the next channel, so builders can pair pipes without commands. */
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!player.isCreative() || GuhrioSpel.speelt(player)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                BlockState nieuw = state.cycle(KANAAL);
                level.setBlock(pos, nieuw, 3);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guhrio.pijp.kanaal", nieuw.getValue(KANAAL)));
            }
            return InteractionResult.SUCCESS;
        }

        private static Direction kant(BlockState state) {
            return state.getBlock() instanceof PijpBlok ? state.getValue(FACING) : Direction.UP;
        }

        /** Where you stand (your feet) when you are out of the mouth at {@code pos}. */
        public static Vec3 buiten(BlockGetter level, BlockPos pos, BlockState state) {
            Direction f = kant(state);
            return switch (f) {
                case UP -> new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                case DOWN -> new Vec3(pos.getX() + 0.5, pos.getY() - GuhrioSpel.PIJP_DIEP, pos.getZ() + 0.5);
                default -> new Vec3(pos.getX() + 0.5 + f.getStepX(), pos.getY(), pos.getZ() + 0.5 + f.getStepZ());
            };
        }

        /** Where you are when you are deepest inside the mouth at {@code pos}. */
        public static Vec3 binnen(BlockGetter level, BlockPos pos, BlockState state) {
            Direction f = kant(state);
            Vec3 uit = buiten(level, pos, state);
            return switch (f) {
                case UP -> uit.add(0, -GuhrioSpel.pijpDiepte(level, pos), 0);
                case DOWN -> new Vec3(uit.x, pos.getY(), uit.z);
                default -> uit.add(-f.getStepX() * ZIJ_DIEP, 0, -f.getStepZ() * ZIJ_DIEP);
            };
        }

        /** The other mouth of this pipe in the level, or null. */
        @Nullable
        public static BlockPos andere(GuhrioSpel.Actief actief, BlockPos pos, BlockState state) {
            List<GuhrioSpel.Stuk> monden = actief.stukken.stream()
                    .filter(s -> s.state().getBlock() instanceof PijpBlok && !s.state().getValue(BOVEN)
                            && s.state().getValue(KANAAL).equals(state.getValue(KANAAL))).toList();
            for (int i = 0; i < monden.size(); i++) {
                if (monden.get(i).pos().equals(pos) && monden.size() > 1) {
                    return monden.get((i + 1) % monden.size()).pos();
                }
            }
            return null;
        }

        @Override
        public void duik(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            ServerLevel level = player.level();
            if (state.getValue(BOVEN)) {
                pos = pos.below();
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof PijpBlok)) {
                    return;
                }
            }
            Direction f = state.getValue(FACING);
            if (!state.getValue(INGANG) || f == Direction.DOWN) {
                return;                                        // (a way out only)
            }
            BlockPos naar = andere(sessie.actief, pos, state);
            if (naar == null || !level.isLoaded(naar)) {
                return;
            }
            // a Hapbloem that is out of its pipe: not now
            if (f == Direction.UP && sessie.actief.wezens.get(pos.above()) instanceof HapbloemEntity bloem && bloem.buiten()) {
                return;
            }
            // (you must really stand on it / in front of it)
            Vec3 sta = buiten(level, pos, state);
            if (Math.abs(player.getY() - sta.y) > 0.6 || Math.abs(player.getX() - sta.x) > 0.8 || Math.abs(player.getZ() - sta.z) > 0.8) {
                return;
            }
            GuhrioSpel.pijp(player, sessie, pos, naar);
        }
    }

    /**
     * A door in the lane, two blocks high (half lower / upper, like any door). Stand in it and press W: you step out of the
     * other door of this level with the same {@link PijpBlok#KANAAL} (it may be on another lane: a room behind the wall).
     */
    public static class DeurBlok extends Block implements GuhrioStuk {
        public static final MapCodec<DeurBlok> CODEC = simpleCodec(DeurBlok::new);
        public static final EnumProperty<net.minecraft.world.level.block.state.properties.DoubleBlockHalf> HALF =
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;

        public DeurBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(PijpBlok.KANAAL, 0)
                    .setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(PijpBlok.KANAAL, HALF);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState onder = context.getLevel().getBlockState(context.getClickedPos().below());
            return onder.getBlock() instanceof DeurBlok
                    ? onder.setValue(HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) : defaultBlockState();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        private static boolean onderste(BlockState state) {
            return state.getBlock() instanceof DeurBlok && state.getValue(HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;
        }

        /** The other door (its lower block) of this one in the level, or null. */
        @Nullable
        public static BlockPos andere(GuhrioSpel.Actief actief, BlockPos pos, BlockState state) {
            List<GuhrioSpel.Stuk> deuren = actief.stukken.stream()
                    .filter(s -> onderste(s.state()) && s.state().getValue(PijpBlok.KANAAL).equals(state.getValue(PijpBlok.KANAAL))).toList();
            for (int i = 0; i < deuren.size(); i++) {
                if (deuren.get(i).pos().equals(pos) && deuren.size() > 1) {
                    return deuren.get((i + 1) % deuren.size()).pos();
                }
            }
            return null;
        }

        @Override
        public void deur(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            BlockPos hier = onderste(state) ? pos : pos.below();
            BlockState onder = player.level().getBlockState(hier);
            if (!onderste(onder)) {
                return;
            }
            BlockPos naar = andere(sessie.actief, hier, onder);
            if (naar == null || !player.level().isLoaded(naar)) {
                return;
            }
            // (you must really stand in it)
            if (Math.abs(player.getY() - hier.getY()) > 1.2 || Math.abs(player.getX() - (hier.getX() + 0.5)) > 0.8
                    || Math.abs(player.getZ() - (hier.getZ() + 0.5)) > 0.8) {
                return;
            }
            GuhrioSpel.deur(player, sessie, hier, naar);
        }

        /** Click it with an empty hand (creative): the next channel. */
        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!player.isCreative() || GuhrioSpel.speelt(player)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                BlockState nieuw = state.cycle(PijpBlok.KANAAL);
                level.setBlock(pos, nieuw, 3);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guhrio.pijp.kanaal", nieuw.getValue(PijpBlok.KANAAL)));
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    // creatures
    // =====================================================================================================================

    /**
     * Where a creature of the level lives: invisible, you walk through it (only somebody holding the block sees it). While
     * somebody plays the level its creature is there ({@link #maak}); it is shared by everybody in the level.
     */
    public abstract static class WezenPlek extends Block implements GuhrioStuk {
        protected WezenPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            // (only somebody holding the block sees and clicks it, like a structure void)
            return context.isHoldingItem(asItem()) ? Shapes.block() : Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        /** The creature of this spot (not yet in the world), or null. */
        @Nullable
        protected abstract Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk);

        @Override
        public void wek(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            Entity nu = actief.wezens.get(stuk.pos());
            if ((nu != null && !nu.isRemoved()) || !level.isPositionEntityTicking(stuk.pos())) {
                return;
            }
            Entity nieuw = maak(level, actief, stuk);
            if (nieuw != null) {
                level.addFreshEntity(nieuw);
                actief.wezens.put(stuk.pos(), nieuw);
            }
        }
    }

    /** Where a Guhmba lives: invisible, you walk through it. While somebody plays the level there is a Guhmba for it. */
    public static class GuhmbaPlek extends Block implements GuhrioStuk {
        public static final MapCodec<GuhmbaPlek> CODEC = simpleCodec(GuhmbaPlek::new);

        public GuhmbaPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            // (only somebody holding the block sees and clicks it, like a structure void)
            return context.isHoldingItem(asItem()) ? Shapes.block() : Shapes.empty();
        }

        @Override
        protected boolean propagatesSkylightDown(BlockState state) {
            return true;
        }

        @Override
        public void wek(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            Entity nu = actief.wezens.get(stuk.pos());
            if (nu != null && !nu.isRemoved()) {
                return;
            }
            BlockPos pos = stuk.pos();
            if (!level.isPositionEntityTicking(pos)) {
                return;
            }
            GuhmbaEntity guhmba = GuhrioFeature.GUHMBA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guhmba == null) {
                return;
            }
            guhmba.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
            guhmba.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            level.addFreshEntity(guhmba);
            actief.wezens.put(pos, guhmba);
        }
    }
}
