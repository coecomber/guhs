package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;

/**
 * einde2 phase B, slice h4_vuur, step 2: the secret dance party in the cave behind the school (DESIGN 5, chapter 4). A rhythm
 * game on the 9 x 9 floor ({@code dansvloer}) on the beat of {@code deuntje.dansfeest} ({@link E2Audio#BPM}): every
 * {@link #TELLEN} beats one of the nine squares of 3 x 3 lights up (only for this player, {@link Oplichten}) and on the last
 * beat of the bar you must stand in it. {@link #RONDES} bars; {@link #NODIG} hits win. Missing costs nothing: the music
 * simply goes on, and a lost party starts again when you step back on the floor.
 */
public final class Dansfeest {
    /** (fix2-h4: 7 of the 12 bars win, was 8: kinder, as the h1 lessons ask) */
    public static final int RONDES = 12, NODIG = 7, TELLEN = 4;
    /** The floor: template x/z of the corner, the floor blocks' y (one under where you stand) and its size. */
    private static final String VLOER = "dansvloer";
    private static final BlockState[] LICHT = {Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Blocks.OCHRE_FROGLIGHT.defaultBlockState(),
            Blocks.VERDANT_FROGLIGHT.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState()};

    private static final class Spel {
        final BlockPos hoek;            // the world block of the floor's north-west corner (the floor block, under your feet)
        final int tick;                 // ticks per beat
        int t;                          // ticks since the first beat (a count-in of three beats first)
        int ronde = -1, vak = 4, raak;

        Spel(BlockPos hoek, int tick) {
            this.hoek = hoek;
            this.tick = tick;
            this.t = -3 * tick;
        }
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Dansfeest() {
    }

    public static boolean actief(ServerPlayer p) {
        return SPELLEN.containsKey(p.getUUID());
    }

    /** The ticks of one beat at the tune's BPM (100 BPM: 12 ticks). */
    static int beatTicks() {
        return Math.max(4, Math.round(1200f / E2Audio.BPM.getOrDefault("deuntje.dansfeest", 100)));
    }

    /** The player steps on the floor: the music starts and the count-in. */
    static void start(ServerPlayer p) {
        BlockPos o = Plek.oorsprong(p);
        Plekken.Plek vloer = Plekken.plek(Plek.STRUCTUUR, VLOER);
        if (o == null || vloer == null || actief(p)) {
            return;
        }
        BlockPos hoek = o.offset(vloer.pos()).below();
        SPELLEN.put(p.getUUID(), new Spel(hoek, beatTicks()));
        Schijn.geluid(p, E2Audio.geluid("deuntje.dansfeest").get(), p.blockPosition(), 1.0f, 1.0f);
        E2H4Vuur.zeg(p, "dans.start", NODIG, RONDES);
        E2H4Vuur.titel(p, "dans.titel", "dans.sub");
    }

    static void tick(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        if (!Plek.in(p, VLOER)) {
            E2H4Vuur.balk(p, "dans.weg");
            stop(p, false);
            return;
        }
        s.t++;
        if (s.t < 0) {
            if (-s.t % s.tick == 0) {
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), p.blockPosition(), 0.8f, 1.6f);
                E2H4Vuur.balk(p, "dans.tel", -s.t / s.tick);
            }
            return;
        }
        if (s.t % s.tick != 0) {
            return;
        }
        int tel = (s.t / s.tick) % TELLEN;
        if (tel == 0) {
            // the bar is over: were you in the lit square? then the next square lights up
            if (s.ronde >= 0) {
                beoordeel(p, s);
            }
            s.ronde++;
            if (s.ronde >= RONDES) {
                einde(p, s);
                return;
            }
            int nieuw;
            do {
                nieuw = p.getRandom().nextInt(9);
            } while (nieuw == s.vak);
            s.vak = nieuw;
            Oplichten.zet(p, vak(s, s.vak, LICHT[s.ronde % LICHT.length]));
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), p.blockPosition(), 0.7f, 0.8f + 0.1f * (s.vak % 5));
            E2H4Vuur.balk(p, "dans.ronde", s.ronde + 1, RONDES, s.raak, NODIG);
        } else {
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), p.blockPosition(), 0.6f, 1.0f);
        }
    }

    private static Map<BlockPos, BlockState> vak(Spel s, int vak, BlockState licht) {
        Map<BlockPos, BlockState> m = new HashMap<>();
        int vx = vak % 3, vz = vak / 3;
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                m.put(s.hoek.offset(vx * 3 + dx, 0, vz * 3 + dz), licht);
            }
        }
        return m;
    }

    private static void beoordeel(ServerPlayer p, Spel s) {
        int vx = s.vak % 3, vz = s.vak / 3;
        int x = p.getBlockX() - s.hoek.getX(), z = p.getBlockZ() - s.hoek.getZ();
        boolean raak = x >= vx * 3 && x < vx * 3 + 3 && z >= vz * 3 && z < vz * 3 + 3;
        if (raak) {
            s.raak++;
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), p.blockPosition(), 1.0f, 1.2f + 0.05f * s.raak);
            Schijn.deeltjes(p, ParticleTypes.NOTE, p.position().add(0, 2.1, 0), 4, 0.4, 1.0);
        } else {
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), p.blockPosition(), 0.7f, 0.7f);
        }
    }

    private static void einde(ServerPlayer p, Spel s) {
        boolean gewonnen = s.raak >= NODIG;
        stop(p, false);
        if (!gewonnen) {
            E2H4Vuur.zeg(p, "dans.bijna", s.raak, NODIG);
            return;
        }
        Schijn.deeltjes(p, ParticleTypes.FIREWORK, p.position().add(0, 2, 0), 40, 1.5, 0.1);
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 0.8f, 1.3f);
        if (E2H4Vuur.VUUR.verder(p, E2H4Vuur.V_DANS)) {
            E2H4Vuur.titel(p, "dans.titel_gewonnen", "dans.sub_gewonnen", s.raak, RONDES);
            E2H4Vuur.zeg(p, "dans.gewonnen");
            E2H4Vuur.zeg(p, "dans.juf");
        }
    }

    /** Ends this player's party (the lit tiles go back to the real floor). */
    static void stop(ServerPlayer p, boolean gewonnen) {
        if (SPELLEN.remove(p.getUUID()) != null) {
            Oplichten.wis(p);
        }
    }

    /** (dev, tests) the party as if every bar was danced in the right square. */
    static void winVoorTest(ServerPlayer p) {
        Spel s = new Spel(BlockPos.ZERO, beatTicks());
        s.raak = NODIG;
        SPELLEN.put(p.getUUID(), s);
        einde(p, s);
    }
}
