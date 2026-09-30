package nl.juiced.guhs.feature.disco;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A disco tile of the Guhdisco dance floor: one block per colour (roze, blauw, geel, groen). The DJ-guh lights them up
 * ({@link #LIT}) when he plays a colour, and when you step on them during a game. Lit they glow at full brightness.
 */
public class DiscoTileBlock extends Block {
    /** The four colours of the Simon-says floor, with their colour (for particles) and the pitch of their note. */
    public enum Kleur {
        ROZE(0xFF6EC7, 0.707f),
        BLAUW(0x4FB6FF, 0.891f),
        GEEL(0xFFE14D, 1.059f),
        GROEN(0x5CF07A, 1.414f);

        public final int rgb;
        public final float pitch;

        Kleur(int rgb, float pitch) {
            this.rgb = rgb;
            this.pitch = pitch;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public final Kleur kleur;

    public DiscoTileBlock(Kleur kleur, Properties properties) {
        super(properties);
        this.kleur = kleur;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(p -> new DiscoTileBlock(kleur, p));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }
}
