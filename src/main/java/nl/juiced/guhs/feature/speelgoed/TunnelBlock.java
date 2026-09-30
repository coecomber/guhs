package nl.juiced.guhs.feature.speelgoed;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De Pluizige tunnel: pink fur tunnel pieces that join up by themselves (straight, bends, crossings). Every side is
 * closed (fur wall), open (the next piece) or an entrance ("ingang": a round hole with a guh face around it: ears on top,
 * two eyes, a snoet). Guhs run through it and hide inside (verstoppertje with the muisje; or knock on the tunnel with a
 * right-click to find them). A piece on its own is open along the way you placed it.
 */
public class TunnelBlock extends Block {
    public static final MapCodec<TunnelBlock> CODEC = simpleCodec(TunnelBlock::new);

    public enum Kant implements StringRepresentable {
        DICHT, OPEN, INGANG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Kant> NOORD = EnumProperty.create("north", Kant.class);
    public static final EnumProperty<Kant> OOST = EnumProperty.create("east", Kant.class);
    public static final EnumProperty<Kant> ZUID = EnumProperty.create("south", Kant.class);
    public static final EnumProperty<Kant> WEST = EnumProperty.create("west", Kant.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final Map<BlockState, VoxelShape> VORMEN = new java.util.concurrent.ConcurrentHashMap<>();

    public TunnelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NOORD, Kant.INGANG).setValue(ZUID, Kant.INGANG).setValue(OOST, Kant.DICHT)
                .setValue(WEST, Kant.DICHT).setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NOORD, OOST, ZUID, WEST, AXIS);
    }

    public static EnumProperty<Kant> kant(Direction d) {
        return switch (d) {
            case EAST -> OOST;
            case SOUTH -> ZUID;
            case WEST -> WEST;
            default -> NOORD;
        };
    }

    public static Kant kant(BlockState state, Direction d) {
        return state.getValue(kant(d));
    }

    /** The sides worked out from the neighbours: joined to a tunnel = open, the ends = entrances, the rest fur. */
    public static BlockState vorm(BlockState state, BlockGetter level, BlockPos pos) {
        List<Direction> buren = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).getBlock() instanceof TunnelBlock) {
                buren.add(d);
            }
        }
        Direction.Axis as = state.getValue(AXIS);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Kant k;
            if (buren.contains(d)) {
                k = Kant.OPEN;
            } else if (buren.isEmpty()) {
                k = d.getAxis() == as ? Kant.INGANG : Kant.DICHT;
            } else if (buren.size() == 1) {
                k = d == buren.get(0).getOpposite() ? Kant.INGANG : Kant.DICHT;
            } else {
                k = Kant.DICHT;
            }
            state = state.setValue(kant(d), k);
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState s = defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        return vorm(s, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? vorm(state, level, pos) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORMEN.computeIfAbsent(state, s -> {
            VoxelShape v = Block.box(0, 12, 0, 16, 16, 16);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (kant(s, d) == Kant.DICHT) {
                    v = Shapes.or(v, switch (d) {
                        case NORTH -> Block.box(0, 0, 0, 16, 12, 2);
                        case SOUTH -> Block.box(0, 0, 14, 16, 12, 16);
                        case WEST -> Block.box(0, 0, 0, 2, 12, 16);
                        default -> Block.box(14, 0, 0, 16, 12, 16);
                    });
                }
            }
            return v.optimize();
        });
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Knock on the tunnel: a guh hiding in it is found (verstoppertje). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1f, 1.4f);
        level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1f, 1.2f);
        if (!TunnelSpel.klop(sl, pos, sp)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.speelgoed.tunnel.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.CONSUME;
    }

    // =====================================================================================================================
    // the tunnel as a network (for the guhs running through)
    // =====================================================================================================================

    /** An entrance: the tunnel piece and the side the hole is on. */
    public record Ingang(BlockPos pos, Direction kant) {
        /** Just outside the hole (where you walk in). */
        public Vec3 buiten() {
            return Vec3.atBottomCenterOf(pos).add(kant.getStepX() * 0.95, 0, kant.getStepZ() * 0.95);
        }
    }

    /** All the pieces joined to this one (at most max). */
    public static Set<BlockPos> netwerk(BlockGetter level, BlockPos start, int max) {
        Set<BlockPos> gezien = new LinkedHashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start.immutable());
        gezien.add(start.immutable());
        while (!rij.isEmpty() && gezien.size() < max) {
            BlockPos p = rij.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof TunnelBlock)) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (kant(s, d) == Kant.OPEN && level.getBlockState(n).getBlock() instanceof TunnelBlock && gezien.add(n.immutable())) {
                    rij.add(n.immutable());
                }
            }
        }
        return gezien;
    }

    public static List<Ingang> ingangen(BlockGetter level, Set<BlockPos> netwerk) {
        List<Ingang> out = new ArrayList<>();
        for (BlockPos p : netwerk) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TunnelBlock) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (kant(s, d) == Kant.INGANG && !level.getBlockState(p.relative(d)).isSolidRender(level, p.relative(d))) {
                        out.add(new Ingang(p, d));
                    }
                }
            }
        }
        return out;
    }

    /** The way through the tunnel from one piece to another (both included), or an empty list. */
    public static List<BlockPos> route(BlockGetter level, BlockPos van, BlockPos naar, int max) {
        Map<BlockPos, BlockPos> terug = new HashMap<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(van);
        terug.put(van, van);
        while (!rij.isEmpty() && terug.size() < max) {
            BlockPos p = rij.poll();
            if (p.equals(naar)) {
                List<BlockPos> out = new ArrayList<>();
                for (BlockPos q = naar; ; q = terug.get(q)) {
                    out.add(q);
                    if (q.equals(van)) {
                        break;
                    }
                }
                Collections.reverse(out);
                return out;
            }
            BlockState s = level.getBlockState(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (kant(s, d) == Kant.OPEN && !terug.containsKey(n) && level.getBlockState(n).getBlock() instanceof TunnelBlock) {
                    terug.put(n, p);
                    rij.add(n);
                }
            }
        }
        return List.of();
    }
}
