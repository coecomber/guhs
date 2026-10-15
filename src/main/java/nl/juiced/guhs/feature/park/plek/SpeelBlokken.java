package nl.juiced.guhs.feature.park.plek;

import java.util.List;
import java.util.Map;

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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * 1.4.2 (Het Knabbelpark, de speeltuin): the playground blocks (models: tools/features/park_plek_a_modellen.py).
 * <ul>
 *   <li>{@link Glijbaan}: one piece of a slide, a slope from its top back edge down to its bottom front edge (FACING = the
 *       way down). Lay them one block down per block forward; whoever gets in slides down at 45 degrees and shoots out at
 *       the bottom (no fall damage). Wieee!</li>
 *   <li>{@link Schommel}: a swing seat on two ropes that go two blocks up (hang it under a bar). Right-click: you sit on it
 *       and it swings by itself ({@link ParkZitje}); the block shows the seat at the angle it is at ({@link #STAND}).</li>
 *   <li>{@link Wip}: a seesaw for two: a long plank on a trestle (FACING = along the plank). Right-click: you sit on the end
 *       nearest to you; up and down it goes ({@link #KANT} = which end is up).</li>
 *   <li>{@link Zandbak}: the sand of the park's sandbox. Right-click to dig: once per day a player digs up a kaasknabbel,
 *       the rest of the day only funny things (and sand). Part of the park: no item.</li>
 * </ul>
 * The slide, the swing and the seesaw are also craftable (and sold at the picknick kraampje): a playground of your own.
 */
public final class SpeelBlokken {
    /** The swing's angle: 0..6 = -45..45 degrees in steps of 15 (3 = hanging straight; positive = towards FACING). */
    public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 6);
    public static final int STAND_RECHT = 3, STAND_GRADEN = 15;
    public static final EnumProperty<Kant> KANT = EnumProperty.create("kant", Kant.class);

    /** Which end of a seesaw is up: none, the end towards FACING (a) or the other one (b). */
    public enum Kant implements StringRepresentable {
        VLAK, A, B;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private SpeelBlokken() {
    }

    /** A block that faces the way the player looks (the slide: downhill away from you). */
    abstract static class Gericht extends HorizontalDirectionalBlock {
        private final Map<Direction, VoxelShape> vormen;

        Gericht(Properties properties, VoxelShape noord) {
            super(properties);
            this.vormen = Shapes.rotateHorizontal(noord);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vormen.get(state.getValue(FACING));
        }

        @Override
        protected boolean isPathfindable(BlockState state, PathComputationType type) {
            return false;
        }
    }

    // =====================================================================================================================
    /** One piece of a slide (no collision: you slide through it, down the slope). */
    public static class Glijbaan extends Gericht {
        public static final MapCodec<Glijbaan> CODEC = simpleCodec(Glijbaan::new);
        /** Speed along each axis while sliding (blocks per tick): 45 degrees, the same as the slope. */
        public static final double SNEL = 0.42;
        /** The persistent data key: when this player last went "wieee" (game time). */
        public static final String WIEE = "guhs_parkplek_glij";

        public Glijbaan(Properties properties) {
            // (the outline: three steps along the slope, high at the back = south of a north-facing piece)
            super(properties, Shapes.or(Block.box(0, 0, 0, 16, 5, 6), Block.box(0, 0, 6, 16, 10, 11), Block.box(0, 0, 11, 16, 16, 16)));
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
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
            glij(state.getValue(FACING), level, entity);
        }

        /** Down the slope: forward and down at the same speed, no fall damage; a player's first slide in a while: wieee. */
        public static void glij(Direction neer, Level level, Entity entity) {
            if (!(entity instanceof LivingEntity) || entity.isPassenger() || entity.isSpectator()) {
                return;
            }
            entity.setDeltaMovement(neer.getStepX() * SNEL, -SNEL, neer.getStepZ() * SNEL);
            entity.resetFallDistance();
            if (level instanceof ServerLevel server && entity instanceof ServerPlayer sp) {
                long nu = server.getGameTime();
                if (nu - sp.getPersistentData().getLongOr(WIEE, -1000L) > 40) {
                    sp.getPersistentData().putLong(WIEE, nu);
                    server.playSound(null, sp.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.PLAYERS, 0.9f, 1.7f);
                    sp.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.glijbaan").withStyle(ChatFormatting.LIGHT_PURPLE));
                    GuhAdvancements.grant(sp, "parkplek_glijbaan");
                }
            }
        }
    }

    // =====================================================================================================================
    /** A swing seat on two ropes (two blocks up): sit on it and away you swing. */
    public static class Schommel extends Gericht {
        public static final MapCodec<Schommel> CODEC = simpleCodec(Schommel::new);

        public Schommel(Properties properties) {
            super(properties, Block.box(2, 3, 5, 14, 7, 11));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAND, STAND_RECHT));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, STAND);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player.isSecondaryUseActive() || player.isPassenger()) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                if (ParkZitje.op(server, pos, 0) != null) {
                    return InteractionResult.CONSUME;   // (somebody is swinging already)
                }
                ParkZitje.zet(server, pos, ParkZitje.Soort.SCHOMMEL, 0, player);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** A seesaw for two (FACING = along the plank): sit on the end nearest to you. */
    public static class Wip extends Gericht {
        public static final MapCodec<Wip> CODEC = simpleCodec(Wip::new);
        /** How far the seats are from the middle (blocks, along the plank). */
        public static final double ARM = 1.2;

        public Wip(Properties properties) {
            super(properties, Shapes.or(Block.box(5, 0, 5, 11, 8, 11), Block.box(6, 8, 0, 10, 10, 16)));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KANT, Kant.VLAK));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, KANT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player.isSecondaryUseActive() || player.isPassenger()) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                Direction f = state.getValue(FACING);
                double langs = (player.getX() - (pos.getX() + 0.5)) * f.getStepX() + (player.getZ() - (pos.getZ() + 0.5)) * f.getStepZ();
                int kant = langs >= 0 ? 1 : -1;
                if (ParkZitje.op(server, pos, kant) != null) {
                    kant = -kant;
                }
                if (ParkZitje.op(server, pos, kant) != null) {
                    return InteractionResult.CONSUME;   // (both ends taken)
                }
                ParkZitje.zet(server, pos, ParkZitje.Soort.WIP, kant, player);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // =====================================================================================================================
    /** The sand of the park's sandbox: dig (right-click); once a day a kaasknabbel per player. */
    public static class Zandbak extends Block {
        public static final MapCodec<Zandbak> CODEC = simpleCodec(Zandbak::new);
        /** The player's own data: the day (game time / 24000) of their last kaasknabbel from a sandbox. */
        public static final String DAG = "guhs_parkplek_zandbak_dag";
        public static final int DAG_TICKS = 24000;
        /** How many funny finds there are (gui.guhs.parkplek.zandbak.vondst.0..). */
        public static final int VONDSTEN = 6;

        public Zandbak(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp) {
                graaf(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;   // (a spade, a bucket or nothing: you dig all the same)
        }

        /** This player's dig here: the kaasknabbel of the day (true), or a funny find (false). */
        public static boolean graaf(ServerPlayer p, BlockPos pos) {
            ServerLevel level = p.level();
            level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.9f, 0.8f + level.getRandom().nextFloat() * 0.3f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 1.05,
                    pos.getZ() + 0.5, 14, 0.3, 0.1, 0.3, 0.1);
            long dag = level.getGameTime() / DAG_TICKS;
            var data = GuhQuests.saved(p);
            if (data.getLongOr(DAG, -1L) == dag) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.zandbak.vondst." + level.getRandom().nextInt(VONDSTEN))
                        .withStyle(ChatFormatting.YELLOW));
                return false;
            }
            data.putLong(DAG, dag);
            Minigames.give(p, new ItemStack(ModItems.KAAS_KNABBELS.get()));
            level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.PLAYERS, 1f, 1.3f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.05);
            p.sendSystemMessage(Component.translatable("gui.guhs.parkplek.zandbak.knabbel").withStyle(ChatFormatting.GOLD));
            GuhAdvancements.grant(p, "parkplek_zandbak");
            return true;
        }
    }

    /** (Tests, upkeep) the seats on this anchor. */
    static List<ParkZitje> zitjes(ServerLevel level, BlockPos anker) {
        return level.getEntitiesOfClass(ParkZitje.class, new AABB(anker).inflate(3), z -> z.isAlive() && anker.equals(z.anker()));
    }

    /** (ParkZitje) a seat was made: the entity type of the seats. */
    static ParkZitje nieuwZitje(ServerLevel level) {
        return PlekA.ZITJE.get().create(level, EntitySpawnReason.TRIGGERED);
    }
}
