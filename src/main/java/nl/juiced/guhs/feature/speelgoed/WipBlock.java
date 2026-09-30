package nl.juiced.guhs.feature.speelgoed;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * De Guh-wip (1 x 1 x 3): a plank on a little guh-head stand, with a pink cushion and two guh-ear handles at each end.
 * Two guhs on it go up and down (one alone bobs a bit); right-click pushes it, sneak + right-click to sit on it yourself.
 * The plank is drawn swinging by client.ToestelRenderer (model guh_wip_plank).
 */
public class WipBlock extends SchommelToestel {
    public static final MapCodec<WipBlock> CODEC = simpleCodec(WipBlock::new);
    /** The pivot of the plank (local blocks) and where the seats sit along it. */
    public static final Vec3 SPIL = new Vec3(0.5, 0.5625, 0.5);
    public static final double ARM = 1.22, ZIT = 0.12;

    private static final List<int[]> DELEN = List.of(new int[]{0, 0, -1}, new int[]{0, 0, 1});
    private static final List<double[]> BOTSING = List.of(new double[]{4, 0, 4, 12, 9, 12});
    private static final List<double[]> OMLIJNING = List.of(new double[]{4, 0, 4, 12, 9, 12}, new double[]{4, 5, -15, 12, 12, 31});

    public WipBlock(Properties properties) {
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
        return 2;
    }

    @Override
    public float omega() {
        return (float) (2 * Math.PI / 44);
    }

    @Override
    public float verval() {
        return 180;
    }

    @Override
    public float maxHoek() {
        return 0.32f;
    }

    @Override
    protected float duwKracht() {
        return 0.12f;
    }

    @Override
    protected float basis(int rijders) {
        return rijders >= 2 ? 0.3f : 0.14f;
    }

    /** Seat 0 at the front end, seat 1 at the back end. */
    static double arm(int plek) {
        return plek == 0 ? -ARM : ARM;
    }

    @Override
    protected Vec3 zitLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        float h = hoek(level, pos, tijd);
        double d = arm(zitje.plek());
        return new Vec3(SPIL.x, SPIL.y + Math.sin(h) * d + ZIT * Math.cos(h), SPIL.z + Math.cos(h) * d);
    }

    @Override
    protected Vec3 kijkLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        return new Vec3(0, 0, zitje.plek() == 0 ? 1 : -1);   // (towards each other)
    }

    @Override
    public Vec3 instapLokaal(int plek) {
        return new Vec3(1.4, 0, SPIL.z + arm(plek));
    }

    @Override
    public Vec3 uitstapLokaal(int plek) {
        return new Vec3(1.45, 0, SPIL.z + arm(plek));
    }
}
