package nl.juiced.guhs.feature.bio.blokkendal;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.level.BlockEvent;
import nl.juiced.guhs.Guhs;

/**
 * Geharkt zand: the sand of a zen garden. Two blocks:
 * <ul>
 *   <li>{@link Recht} (geharkt_zand): straight grooves along its axis. The grooves sit at the same place in every block,
 *       so a whole bed lines up by itself.</li>
 *   <li>{@link Ring} (geharkt_zand_ring): rings. One ring block alone is a little round of circles ("rond"); ring blocks
 *       around a centre (another round, or the block under a boulder) are the eight pieces of a rounded square around it,
 *       each chosen by itself from where the centre is. The side pieces carry the same grooves as the straight sand, so
 *       the ring runs on into a straight bed.</li>
 * </ul>
 * A hoe is the rake: on plain sand it makes straight grooves the way you look; on raked sand it goes on to the next
 * pattern (along, across, rings, along...). Sneaking with the hoe on a ring block turns that one block by hand.
 */
public final class GeharktZand {
    /** Plain sand a hoe can rake (minecraft:sand; other slices may add theirs). */
    public static final TagKey<Block> HARKBAAR = TagKey.create(Registries.BLOCK, Guhs.id("geharkt_zand_harkbaar"));

    /** Where a ring block sits, seen from the centre it circles (ROND: it is a centre itself). */
    public enum Vorm implements StringRepresentable {
        ROND("rond", 0, 0), NOORD("noord", 0, -1), NOORDOOST("noordoost", 1, -1), OOST("oost", 1, 0), ZUIDOOST("zuidoost", 1, 1),
        ZUID("zuid", 0, 1), ZUIDWEST("zuidwest", -1, 1), WEST("west", -1, 0), NOORDWEST("noordwest", -1, -1);

        private final String naam;
        /** From the centre to this block. */
        public final int dx, dz;

        Vorm(String naam, int dx, int dz) {
            this.naam = naam;
            this.dx = dx;
            this.dz = dz;
        }

        @Override
        public String getSerializedName() {
            return naam;
        }

        static Vorm van(int dx, int dz) {
            for (Vorm v : values()) {
                if (v.dx == dx && v.dz == dz) {
                    return v;
                }
            }
            return ROND;
        }

        Vorm gedraaid(Rotation rotation) {
            return switch (rotation) {
                case CLOCKWISE_90 -> van(-dz, dx);
                case CLOCKWISE_180 -> van(-dx, -dz);
                case COUNTERCLOCKWISE_90 -> van(dz, -dx);
                default -> this;
            };
        }
    }

    /** The eight places a centre can be, the four straight ones first. */
    private static final int[][] ROND_OM = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}, {1, -1}, {1, 1}, {-1, 1}, {-1, -1}};

    static boolean isZand(BlockState state) {
        return state.getBlock() instanceof Recht || state.getBlock() instanceof Ring;
    }

    static boolean isRond(BlockState state) {
        return state.getBlock() instanceof Ring && state.getValue(Ring.VORM) == Vorm.ROND;
    }

    /** A centre to rake rings around: a round of rings, or the sand under a boulder (anything solid standing on it). */
    static boolean isKern(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isRond(state)) {
            return true;
        }
        if (!isZand(state)) {
            return false;       // (a wall beside the bed is no boulder)
        }
        BlockPos boven = pos.above();
        BlockState kei = level.getBlockState(boven);
        return !kei.isAir() && !kei.getCollisionShape(level, boven).isEmpty();
    }

    /** The shape a ring block takes here by itself: a piece around the first centre next to it, else a round of its own. */
    public static Vorm kies(BlockGetter level, BlockPos pos, boolean alleenRond) {
        for (int[] o : ROND_OM) {
            BlockPos kern = pos.offset(o[0], 0, o[1]);
            if (alleenRond ? isRond(level.getBlockState(kern)) : isKern(level, kern)) {
                return Vorm.van(-o[0], -o[1]);
            }
        }
        return Vorm.ROND;
    }

    /** Does this piece still circle something? (Anything but loose raked sand or air counts: a template may use any centre.) */
    static boolean klopt(BlockGetter level, BlockPos pos, Vorm vorm) {
        if (vorm == Vorm.ROND) {
            return true;
        }
        BlockPos kern = pos.offset(-vorm.dx, 0, -vorm.dz);
        BlockState s = level.getBlockState(kern);
        if (isRond(s) || (!s.isAir() && !isZand(s))) {
            return true;
        }
        return !level.getBlockState(kern.above()).isAir();
    }

    private static boolean hark(UseOnContext context, ItemAbility ability) {
        return ability == ItemAbilities.HOE_TILL && context.getItemInHand().canPerformAction(ability);
    }

    private static boolean sluipt(UseOnContext context) {
        return context.getPlayer() != null && context.getPlayer().isSecondaryUseActive();
    }

    /** Straight grooves along AXIS (x: they run east-west). */
    public static class Recht extends Block {
        public static final MapCodec<Recht> CODEC = simpleCodec(Recht::new);
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

        public Recht(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (!hark(context, ability)) {
                return super.getToolModifiedState(state, context, ability, simulate);
            }
            if (state.getValue(AXIS) == Direction.Axis.X || sluipt(context)) {
                return state.cycle(AXIS);
            }
            return BlokkenDalSlice.GEHARKT_ZAND_RING.get().defaultBlockState()
                    .setValue(Ring.VORM, kies(context.getLevel(), context.getClickedPos(), false));
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? state.cycle(AXIS) : state;
        }
    }

    /** Rings: a round, or one of the eight pieces around a centre. */
    public static class Ring extends Block {
        public static final MapCodec<Ring> CODEC = simpleCodec(Ring::new);
        public static final EnumProperty<Vorm> VORM = EnumProperty.create("vorm", Vorm.class);

        public Ring(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(VORM, Vorm.ROND));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VORM);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(VORM, kies(context.getLevel(), context.getClickedPos(), false));
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                BlockState neighbor, RandomSource random) {
            Vorm vorm = state.getValue(VORM);
            if (direction.getAxis().isHorizontal() && !klopt(level, pos, vorm)) {
                Vorm nieuw = kies(level, pos, true);      // my centre is gone: join a round next to me, else stay as I am
                if (nieuw != Vorm.ROND) {
                    return state.setValue(VORM, nieuw);
                }
            }
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Nullable
        @Override
        public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
            if (!hark(context, ability)) {
                return super.getToolModifiedState(state, context, ability, simulate);
            }
            if (sluipt(context)) {
                return state.cycle(VORM);            // by hand: the next piece
            }
            return BlokkenDalSlice.GEHARKT_ZAND.get().defaultBlockState();
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(VORM, state.getValue(VORM).gedraaid(rotation));
        }
    }

    /** A hoe on plain sand rakes it: straight grooves the way the player looks. */
    public static final class Harken {
        @SubscribeEvent
        public static void hark(BlockEvent.BlockToolModificationEvent event) {
            if (event.getItemAbility() != ItemAbilities.HOE_TILL || !event.getState().is(HARKBAAR)) {
                return;
            }
            UseOnContext context = event.getContext();
            if (!context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
                return;
            }
            event.setFinalState(BlokkenDalSlice.GEHARKT_ZAND.get().defaultBlockState()
                    .setValue(Recht.AXIS, context.getHorizontalDirection().getAxis()));
        }

        private Harken() {
        }
    }

    private GeharktZand() {
    }
}
