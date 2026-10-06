package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;
import nl.juiced.guhs.feature.speelgoed.ZitjeEntity;

/**
 * An obstacle of the Guh-parkour ({@link Obstakel}): a toy block like the glijbaantje (a controller with invisible
 * speelgoed_deel parts, facing the player who places it), but without seats: a guh on a route takes it by itself
 * ({@link RouteStukken}). Guhs that just play (SpeelGoal, the huisje) leave it alone: it only does something in a route.
 */
public class ObstakelBlock extends ToestelBlock {
    private final Obstakel soort;
    private final MapCodec<ObstakelBlock> codec;

    public ObstakelBlock(Properties properties, Obstakel soort) {
        super(properties);
        this.soort = soort;
        this.codec = simpleCodec(p -> new ObstakelBlock(p, soort));
    }

    public Obstakel soort() {
        return soort;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected List<int[]> delen() {
        return soort.delen();
    }

    @Override
    protected List<double[]> botsing() {
        return soort.botsing();
    }

    @Override
    protected List<double[]> omlijning() {
        return soort.omlijning();
    }

    @Override
    public String speeltje() {
        return "guhparkour";
    }

    @Override
    public int plekken() {
        return 0;
    }

    @Nullable
    @Override
    protected Vec3 zitLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        return null;   // (nobody sits on an obstacle)
    }

    @Override
    public Vec3 instapLokaal(int plek) {
        return soort.baan(false).start();
    }

    @Override
    public Vec3 uitstapLokaal(int plek) {
        return soort.baan(false).eind();
    }

    @Override
    protected InteractionResult gebruik(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (player.getMainHandItem().isEmpty() && !player.isShiftKeyDown()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhparkour.hint.obstakel").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.PASS;
    }

    /** The cloth tunnel: the bump of the guh crawling through shows as a block state (0 = nobody inside, 1..5 = where). */
    public static class Kruiptunnel extends ObstakelBlock {
        public static final IntegerProperty BOBBEL = IntegerProperty.create("bobbel", 0, 5);
        private final MapCodec<Kruiptunnel> codec = simpleCodec(Kruiptunnel::new);

        public Kruiptunnel(Properties properties) {
            super(properties, Obstakel.KRUIPTUNNEL);
            registerDefaultState(defaultBlockState().setValue(BOBBEL, 0));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return codec;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(BOBBEL);
        }

        /** Shows the bump at this local z (-1..2 runs through the three blocks); anything outside: no bump. */
        public static void bobbel(ServerLevel level, BlockPos pos, double lokaalZ) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof Kruiptunnel)) {
                return;
            }
            int n = lokaalZ <= -1.0 || lokaalZ >= 2.0 ? 0 : 1 + Math.min(4, (int) ((lokaalZ + 1.0) / 0.6));
            if (state.getValue(BOBBEL) != n) {
                level.setBlock(pos, state.setValue(BOBBEL, n), Block.UPDATE_CLIENTS);
            }
        }

        /** The local z of a world point along this tunnel. */
        public static double lokaalZ(BlockPos pos, BlockState state, Vec3 wereld) {
            Direction achter = state.getValue(FACING).getOpposite();
            return 0.5 + (wereld.x - (pos.getX() + 0.5)) * achter.getStepX() + (wereld.z - (pos.getZ() + 0.5)) * achter.getStepZ();
        }
    }
}
