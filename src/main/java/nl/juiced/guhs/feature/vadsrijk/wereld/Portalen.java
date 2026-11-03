package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The way in and out (design ronde 2 + 3 + 10):
 * <ul>
 *   <li>the ruined order temple ({@code guhs:vr_w_portaaltempel}, rare, only in the Guhmension) holds a sleeping portal;
 *       touching it the first time wakes it: a 6 s camera scene round the gate, the dragon roars far away, then you are in
 *       the Vadsrijk (kern {@link VadsrijkCommando#naarBinnen}, which remembers where you were);</li>
 *   <li>awake portals take you straight in; inside the Vadsrijk a portal takes you back to where you came from
 *       ({@link VadsrijkCommando#terug});</li>
 *   <li>after the first entry ({@link #BINNEN_GEWEEST}) you can build your own: a frame of Vadsrijk-poortsteen, lit with a
 *       Vadsrijk-vonk ({@link #ontsteek}).</li>
 * </ul>
 */
public final class Portalen {
    /** {@code SpelerData.gezien} flags of this slice. */
    public static final String ONTWAAKT = "vr_w_portaal_ontwaakt", BINNEN_GEWEEST = "vr_w_binnen_geweest";
    private static final int MAX_VLAK = 21 * 21;
    private static final Map<UUID, Long> AFKOELEN = new HashMap<>();

    private Portalen() {
    }

    /** A player stands in a portal block (server, every tick while inside). */
    static void aanraking(ServerPlayer p, BlockPos pos, BlockState state) {
        long nu = p.level().getGameTime();
        Long tot = AFKOELEN.get(p.getUUID());
        if ((tot != null && nu < tot) || RijkScenes.bezig(p)) {
            return;
        }
        AFKOELEN.put(p.getUUID(), nu + 80);
        if (Vadsrijk.in(p)) {
            VadsrijkCommando.terug(p);
            return;
        }
        SpelerData d = Spelers.data(p);
        if (!state.getValue(PortaalBlok.WAKKER)) {
            ontwaak((ServerLevel) p.level(), pos);
            AFKOELEN.put(p.getUUID(), nu + 200);
            speelOntwaken(p, pos, state.getValue(PortaalBlok.AXIS));
            d.gezien.add(ONTWAAKT);
            Later.na(118, () -> {
                if (!p.isRemoved() && !Vadsrijk.in(p)) {
                    VadsrijkCommando.naarBinnen(p, false);
                }
            });
            return;
        }
        VadsrijkCommando.naarBinnen(p, false);
    }

    /** Wake every portal block connected to this one (the whole gate), with a puff of star dust. */
    public static int ontwaak(ServerLevel level, BlockPos start) {
        Set<BlockPos> gezien = new HashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start);
        int n = 0;
        while (!rij.isEmpty() && gezien.size() < 2000) {
            BlockPos b = rij.poll();
            if (!gezien.add(b)) {
                continue;
            }
            BlockState s = level.getBlockState(b);
            if (!(s.getBlock() instanceof PortaalBlok)) {
                continue;
            }
            if (!s.getValue(PortaalBlok.WAKKER)) {
                level.setBlock(b, s.setValue(PortaalBlok.WAKKER, true), 2);
                n++;
                if (n % 6 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0.02);
                }
            }
            for (Direction d : Direction.values()) {
                rij.add(b.relative(d));
            }
        }
        level.playSound(null, start, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2f, 0.7f);
        level.playSound(null, start, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.3f);
        return n;
    }

    /** The awakening: the camera circles the gate while it lights up; far away the dragon roars. 6 s, no click. */
    private static void speelOntwaken(ServerPlayer p, BlockPos poort, Direction.Axis as) {
        Vec3 m = Vec3.atCenterOf(poort);
        Vec3 voor = as == Direction.Axis.X ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        if (p.position().subtract(m).dot(voor) < 0) {
            voor = voor.scale(-1);
        }
        Vec3 opzij = new Vec3(voor.z, 0, -voor.x);
        RijkScene s = RijkScene.maak("vr_w_ontwaken", 120)
                .camera(0, m.add(voor.scale(4)).add(0, 0.5, 0), m)
                .camera(40, m.add(voor.scale(12)).add(opzij.scale(6)).add(0, 4, 0), m.add(0, 2, 0))
                .camera(80, m.add(voor.scale(18)).add(opzij.scale(-8)).add(0, 9, 0), m.add(0, 3, 0))
                .camera(118, m.add(voor.scale(2)).add(0, 1, 0), m.subtract(voor.scale(4)))
                .titel("vadsrijk.wereld.scene.ontwaken.titel", "vadsrijk.wereld.scene.ontwaken.onder", 52, 60, 0xFF9AD5)
                .bouw();
        RijkScenes.speel(p, s);
        ServerLevel l = (ServerLevel) p.level();
        Later.na(30, () -> l.playSound(null, p.blockPosition().offset(0, 30, 0), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.AMBIENT, 0.6f, 0.75f));
        Later.na(70, () -> l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 0.6f));
    }

    // ---------------------------------------------------------------------------------------------------- building your own

    /**
     * Light a self-built frame: clicked = a poortsteen block of the frame. Finds an air area in a vertical plane next to it
     * (X or Z axis) that is closed by poortsteen all round (at most 21x21), and fills it with an awake portal.
     * Returns the number of portal blocks (0 = no valid frame).
     */
    public static int ontsteek(Level level, BlockPos klik) {
        for (Direction.Axis as : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction links = as == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, links, links.getOpposite()}) {
                BlockPos start = klik.relative(d);
                if (!level.getBlockState(start).isAir()) {
                    continue;
                }
                Set<BlockPos> vlak = vlak(level, start, as);
                if (vlak != null && vlak.size() >= 2) {
                    BlockState portaal = WereldRegistry.PORTAAL.get().defaultBlockState().setValue(PortaalBlok.AXIS, as).setValue(PortaalBlok.WAKKER, true);
                    for (BlockPos b : vlak) {
                        level.setBlock(b, portaal, 2 | 16);
                    }
                    return vlak.size();
                }
            }
        }
        return 0;
    }

    /** Flood fill of air in the plane of axis {@code as}; null when it leaks out or touches something that isn't the frame. */
    static Set<BlockPos> vlak(Level level, BlockPos start, Direction.Axis as) {
        Set<BlockPos> uit = new HashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start);
        Direction l = as == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction[] richtingen = {Direction.UP, Direction.DOWN, l, l.getOpposite()};
        while (!rij.isEmpty()) {
            BlockPos b = rij.poll();
            if (uit.contains(b)) {
                continue;
            }
            BlockState s = level.getBlockState(b);
            if (s.isAir()) {
                uit.add(b);
                if (uit.size() > MAX_VLAK) {
                    return null;
                }
                for (Direction d : richtingen) {
                    rij.add(b.relative(d));
                }
            } else if (!s.is(WereldRegistry.POORTSTEEN.get())) {
                return null;
            }
        }
        return uit;
    }

    /** May this player light a portal? Only after their first visit to the Vadsrijk. */
    public static boolean magBouwen(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        return d.gezien.contains(BINNEN_GEWEEST) || d.gezien.contains(ONTWAAKT);
    }

    static void geweigerd(ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.portaal.eerst_zelf").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
