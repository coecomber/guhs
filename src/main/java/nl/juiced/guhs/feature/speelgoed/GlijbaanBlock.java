package nl.juiced.guhs.feature.speelgoed;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Het Guh-glijbaantje met klimrek (1 wide, 2 high, 3 deep): a klimrek at the back with two big guh ears on top, a
 * platform held up by a guh face, and a slide down to the front. Guhs (and players: right-click) climb up, slide down
 * "wieee!" and, when they're guhs, run round for another go ({@link ZitjeEntity} laps).
 */
public class GlijbaanBlock extends ToestelBlock {
    public static final MapCodec<GlijbaanBlock> CODEC = simpleCodec(GlijbaanBlock::new);

    /** One lap: the path (local blocks) and how long each piece takes (ticks). */
    static final Vec3 A = new Vec3(0.5, 0.0, 2.15), B = new Vec3(0.5, 1.42, 1.6), C = new Vec3(0.5, 1.42, 0.3),
            D = new Vec3(0.5, 0.55, -0.72), E = new Vec3(0.5, 0.02, -1.45), F = new Vec3(1.75, 0.0, -1.35), G = new Vec3(1.75, 0.0, 2.2);
    static final int KLIM = 24, LOOP = 10, GLIJ = 12, UIT = 5, LAP = KLIM + LOOP + GLIJ + UIT;
    static final int OM1 = 6, OM2 = 16, OM3 = 5, OM = OM1 + OM2 + OM3, PERIODE = LAP + OM;
    /** When the slide starts within a lap. */
    static final int GLIJ_START = KLIM + LOOP;

    private static final List<int[]> DELEN = List.of(new int[]{0, 0, -1}, new int[]{0, 1, -1}, new int[]{0, 1, 0}, new int[]{0, 0, 1},
            new int[]{0, 1, 1});
    private static final List<double[]> BOTSING;

    static {
        List<double[]> b = new ArrayList<>();
        b.add(new double[]{2, 0, 2, 4, 20, 4});        // the posts under the platform
        b.add(new double[]{12, 0, 2, 14, 20, 4});
        b.add(new double[]{1, 4, 0, 15, 20, 2});        // the guh face holding up the front
        b.add(new double[]{2, 0, 21, 4, 30, 24});       // the klimrek's rails
        b.add(new double[]{12, 0, 21, 14, 30, 24});
        b.add(new double[]{1, 20, 1, 15, 22, 21});      // the platform
        b.add(new double[]{1, 22, 2, 2, 28, 20});       // its railings
        b.add(new double[]{14, 22, 2, 15, 28, 20});
        for (int z = -16; z < 2; z += 3) {              // the slide: a thin ramp in steps
            double top = 8 + Math.max(0, Math.min(z + 3, 2) + 12);
            b.add(new double[]{3, top - 2, z, 13, top, z + 3});
        }
        BOTSING = List.copyOf(b);
    }

    public GlijbaanBlock(Properties properties) {
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
    public String speeltje() {
        return "glijbaantje";
    }

    @Override
    public int plekken() {
        return 1;
    }

    /** How long a ride of this many laps takes (ticks). */
    public static int duur(int rondjes) {
        return rondjes * LAP + (rondjes - 1) * OM;
    }

    private static Vec3 tussen(Vec3 a, Vec3 b, float f) {
        return a.lerp(b, Math.max(0, Math.min(1, f)));
    }

    @Nullable
    @Override
    protected Vec3 zitLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        float t = Math.max(0, zitje.rit(tijd));
        int lap = (int) (t / PERIODE);
        if (lap >= zitje.rondjes()) {
            return null;
        }
        float u = t - lap * PERIODE;
        if (u < KLIM) {
            return tussen(A, B, u / KLIM);
        }
        u -= KLIM;
        if (u < LOOP) {
            return tussen(B, C, u / LOOP);
        }
        u -= LOOP;
        if (u < GLIJ) {
            float f = u / GLIJ;
            return tussen(C, D, f * f * 0.6f + f * 0.4f);   // (faster and faster: wieee)
        }
        u -= GLIJ;
        if (u < UIT) {
            return tussen(D, E, u / UIT);
        }
        if (lap == zitje.rondjes() - 1) {
            return null;
        }
        u -= UIT;
        if (u < OM1) {
            return tussen(E, F, u / OM1);
        }
        u -= OM1;
        if (u < OM2) {
            return tussen(F, G, u / OM2);
        }
        return tussen(G, A, (u - OM2) / OM3);
    }

    @Override
    protected Vec3 kijkLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        float u = Math.max(0, zitje.rit(tijd)) % PERIODE;
        if (u >= LAP + OM1 && u < LAP + OM1 + OM2) {
            return new Vec3(0, 0, 1);    // running round to the back
        }
        return new Vec3(0, 0, -1);
    }

    @Override
    public Vec3 instapLokaal(int plek) {
        return A;
    }

    @Override
    public Vec3 uitstapLokaal(int plek) {
        return new Vec3(0.5, 0.0, -1.7);
    }

    @Override
    protected void rijdt(ServerLevel level, BlockPos pos, BlockState state, ZitjeEntity zitje, LivingEntity rijder) {
        int u = (int) (level.getGameTime() - zitje.start()) % PERIODE;
        Direction f = state.getValue(FACING);
        if (u < KLIM && u % 6 == 0) {
            level.playSound(null, rijder.blockPosition(), SpeelgoedFeature.KLIM.get(), SoundSource.NEUTRAL, 0.5f, 1.2f + level.random.nextFloat() * 0.3f);
        }
        if (u == GLIJ_START) {
            level.playSound(null, rijder.blockPosition(), SpeelgoedFeature.WIEEE.get(), SoundSource.NEUTRAL, 1f,
                    rijder instanceof GuhEntity g ? g.getVoicePitch() : 1.1f);
        }
        if (u > GLIJ_START && u < GLIJ_START + GLIJ) {
            Vec3 p = rijder.position();
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.1, p.z, 1, 0.1, 0.02, 0.1, 0.01);
            if (u % 3 == 0) {
                level.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), p.x, p.y + 0.8, p.z, 1, 0.2, 0.1, 0.2, 0);
            }
        }
        if (u == LAP - 1) {
            Vec3 p = wereld(pos, f, E);
            level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.1, p.z, 5, 0.25, 0.05, 0.25, 0.02);
        }
    }

    @Override
    protected InteractionResult gebruik(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (zitje(level, pos, 0) != null) {
            player.displayClientMessage(Component.translatable("gui.guhs.speelgoed.glijbaan.bezet").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.CONSUME;
        }
        if (ZitjeEntity.zet(level, pos, 0, player, 0, 1) != null) {
            player.displayClientMessage(Component.translatable("gui.guhs.speelgoed.glijbaan.klim").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            Spelen.spelerSpeelt(player, speeltje());
        }
        return InteractionResult.CONSUME;
    }
}
