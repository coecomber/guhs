package nl.juiced.guhs.feature.speelgoed;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * De Guh-schommel (3 wide, 2 high): a pink A-frame with a beam that wears guh ears and a face, and a cushioned seat on
 * two ropes. A guh on it swings by itself; right-click pushes it higher ("Duw!"), sneak + right-click to swing yourself.
 * The seat is drawn swinging by client.ToestelRenderer (model guh_schommel_zitje).
 */
public class SchommelBlock extends SchommelToestel {
    public static final MapCodec<SchommelBlock> CODEC = simpleCodec(SchommelBlock::new);
    /** Where the ropes hang from (local blocks) and how long they are. */
    public static final Vec3 SPIL = new Vec3(0.5, 1.75, 0.5);
    public static final double TOUW = 1.12;

    private static final List<int[]> DELEN = List.of(new int[]{-1, 0, 0}, new int[]{1, 0, 0}, new int[]{-1, 1, 0}, new int[]{0, 1, 0},
            new int[]{1, 1, 0});
    private static final List<double[]> BOTSING = List.of(new double[]{-14, 0, 2, -12, 28, 14}, new double[]{28, 0, 2, 30, 28, 14},
            new double[]{-14, 27, 6, 30, 30, 10});
    private static final List<double[]> OMLIJNING = List.of(new double[]{-14, 0, 2, -12, 28, 14}, new double[]{28, 0, 2, 30, 28, 14},
            new double[]{-14, 27, 6, 30, 32, 10}, new double[]{3, 6, 3, 13, 11, 13});

    public SchommelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected List<int[]> delen() {
        return DELEN;
    }

    @Override
    protected List<double[]> botsing() {
        return BOTSING;
    }

    @Override
    protected List<double[]> omlijning() {
        return OMLIJNING;
    }

    @Override
    public String speeltje() {
        return "wip_schommel";
    }

    @Override
    public int plekken() {
        return 1;
    }

    @Override
    public float omega() {
        return (float) (2 * Math.PI / 52);
    }

    @Override
    public float verval() {
        return 260;
    }

    @Override
    public float maxHoek() {
        return 0.95f;
    }

    @Override
    protected float duwKracht() {
        return 0.22f;
    }

    @Override
    protected float basis(int rijders) {
        return 0.42f;
    }

    @Override
    protected Vec3 zitLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        float h = hoek(level, pos, tijd);
        return new Vec3(SPIL.x, SPIL.y - TOUW * Math.cos(h), SPIL.z + TOUW * Math.sin(h));
    }

    @Override
    public Vec3 instapLokaal(int plek) {
        return new Vec3(0.5, 0, -0.9);
    }

    @Override
    public Vec3 uitstapLokaal(int plek) {
        return new Vec3(0.5, 0, -1.15);
    }
}
