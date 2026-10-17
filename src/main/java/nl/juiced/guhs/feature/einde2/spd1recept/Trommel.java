package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;

/**
 * einde2 "Sponsguh", slice sp_d1_recept, step 10 (CONTRACT_E2_C 7.5): the drum game on the stage of the Bellenkom. Your drum
 * is the note block on {@code podium_trommel}; you stand behind it ({@code d1r_trommel_speler}). The tune
 * {@code deuntje.trommel} runs at {@link SpAudio#BPM} 112 from its tick 0 (a soft hi-hat ticks every beat as well, so the beat is
 * always there). Octo counts in four beats (1, 2, 3, 4 big on the screen); then {@value #NOTEN} bubbles come down onto the drum,
 * one per note of {@link #PATROON}: a ring of bubbles shrinks onto the drum and POPS on its beat. Click the drum (either mouse
 * button) when it pops: within {@value #RAAM} ticks either side is a hit. A click more than that before a bubble that is
 * almost there spends it (so drumming wildly does not win). The counter is always on screen ("Raak 7 / 10 · Bel 9 / 16").
 * {@value #NODIG} hits pass. A lost round costs nothing: a soft "boing", Octo sighs, and it starts again on the spot after
 * {@value #OPNIEUW} ticks. No hidden clock: the song simply plays to its end.
 * <p>
 * {@link Ronde} is the pure core (the game test plays it with clicks at chosen ticks).
 */
final class Trommel {
    /** The notes (beats after tick 0 of the tune; beats 0-3 are Octo's count-in). */
    static final int[] PATROON = {4, 6, 8, 10, 12, 14, 15, 16, 18, 20, 22, 23, 24, 26, 28, 30};
    static final int NOTEN = 16, NODIG = 10, TEL = 4;
    /** A click this near (ticks) to a bubble's pop is a hit; this much earlier spends the bubble. */
    static final int RAAM = 5, TE_VROEG = 11;
    /** The ring of a bubble shows this many ticks before it pops. */
    static final int AANLOOP = 22;
    /** A round is over this many ticks after the last note; a lost one starts again this many ticks after that. */
    static final int UITLOOP = 16, OPNIEUW = 60;
    static final int BPM = SpAudio.BPM.getOrDefault("deuntje.trommel", 112);

    private static final DustParticleOptions RING = new DustParticleOptions(0xBFEFFF, 0.8f);
    private static final DustParticleOptions RING_DICHT = new DustParticleOptions(0xFFF07A, 1.0f);

    /** The ticks of one beat. */
    static double beat() {
        return 1200.0 / BPM;
    }

    /** The tick of note i (from tick 0 of the tune). */
    static int noot(int i) {
        return (int) Math.round(PATROON[i] * beat());
    }

    /** One round (pure). */
    static final class Ronde {
        final boolean[] raak = new boolean[NOTEN], weg = new boolean[NOTEN];
        int hits;

        /** A click at tick t of the tune: the index of the note it hit, -1 = nothing (-2 = it spent a note: too early). */
        int klik(int t) {
            for (int i = 0; i < NOTEN; i++) {
                if (weg[i]) {
                    continue;
                }
                int d = t - noot(i);
                if (Math.abs(d) <= RAAM) {
                    weg[i] = true;
                    raak[i] = true;
                    hits++;
                    return i;
                }
                if (d < -RAAM && d >= -TE_VROEG) {
                    weg[i] = true;                        // (too early: this bubble is gone)
                    return -2;
                }
            }
            return -1;
        }

        /** Notes whose window passed without a click are gone; true when the round is over at tick t. */
        boolean voorbij(int t) {
            for (int i = 0; i < NOTEN; i++) {
                if (!weg[i] && t > noot(i) + RAAM) {
                    weg[i] = true;
                }
            }
            return t > noot(NOTEN - 1) + UITLOOP;
        }

        /** How many bubbles came by so far (hit, missed or spent). */
        int geweest() {
            int n = 0;
            for (boolean b : weg) {
                n += b ? 1 : 0;
            }
            return n;
        }

        boolean gewonnen() {
            return hits >= NODIG;
        }
    }

    /** A player's game: the round, the server tick of the tune's tick 0, the round number; the pause after a lost round. */
    private static final class Spel {
        Ronde ronde = new Ronde();
        long nul;
        int rondes = 1;
        long pauzeTot = -1;
        long laatsteKlik = Long.MIN_VALUE / 2;
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Trommel() {
    }

    static boolean bezig(ServerPlayer p) {
        return SPELLEN.containsKey(p.getUUID());
    }

    static void stop(ServerPlayer p) {
        SPELLEN.remove(p.getUUID());
    }

    static void vergeet(UUID u) {
        SPELLEN.remove(u);
    }

    /** (dev, the game test) the round now; null: no game. */
    @Nullable
    static Ronde ronde(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        return s == null ? null : s.ronde;
    }

    /** The player stands behind the drum: the band is ready, Octo counts in. */
    static void start(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        Spel s = new Spel();
        SPELLEN.put(p.getUUID(), s);
        E2SpD1Recept.titel(p, Component.translatable(E2SpD1Recept.P + "trommel.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable(E2SpD1Recept.P + "trommel.onder", NODIG, NOTEN).withStyle(ChatFormatting.YELLOW), 50);
        p.sendSystemMessage(Component.translatable(E2SpD1Recept.Q + "trommel.regel", NODIG, NOTEN).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        begin(p, s);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);          // (the step line shows the "10_b" text now)
    }

    private static void begin(ServerPlayer p, Spel s) {
        s.ronde = new Ronde();
        s.nul = p.level().getServer().getTickCount() + 20;            // (a second to read the title)
        s.pauzeTot = -1;
        D1rActeurs.speel(p, "octo", "trommel_tel");
    }

    /**
     * A click on the drum (E2SpD1Recept's click events). True = it was a game click (the caller cancels the click, so the
     * note block keeps its note).
     */
    static boolean klik(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return false;
        }
        long nu = p.level().getServer().getTickCount();
        if (nu == s.laatsteKlik || s.pauzeTot >= 0) {
            return true;                                              // (both hands in one tick; or between two rounds)
        }
        s.laatsteKlik = nu;
        int t = (int) (nu - s.nul);
        BlockPos trommel = E2SpD1Recept.blok(p.level(), "podium_trommel");
        int i = s.ronde.klik(t);
        if (trommel != null) {
            Vec3 boven = Vec3.atCenterOf(trommel).add(0, 0.9, 0);
            if (i >= 0) {
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), trommel, 1.0f, 1.0f);
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_SNARE.value(), trommel, 0.6f, 1.2f);
                Schijn.deeltjes(p, ParticleTypes.NOTE, boven.add(0, 0.4, 0), 3, 0.3, 1);
                Schijn.deeltjes(p, ParticleTypes.BUBBLE_POP, boven, 12, 0.3, 0.05);
                D1rActeurs.speel(p, "publiek", "juich");
                if (s.ronde.hits % 3 == 0) {
                    D1rActeurs.speel(p, "band", "juich");
                }
            } else {
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), trommel, 0.5f, 0.7f);
            }
        }
        return true;
    }

    /** Every tick of a game: the beat, the bubbles, the counter, the end of a round. */
    static void tik(ServerPlayer p) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        if (s.pauzeTot >= 0) {
            if (nu >= s.pauzeTot) {
                s.rondes++;
                begin(p, s);
            }
            return;
        }
        int t = (int) (nu - s.nul);
        BlockPos trommel = E2SpD1Recept.blok(p.level(), "podium_trommel");
        if (t < 0 || trommel == null) {
            return;
        }
        if (t == 0) {
            SpAudio.stinger(p, "deuntje.trommel");
        }
        // the beat: a soft hi-hat; the count-in big on the screen
        double b = beat();
        int tel = (int) Math.floor(t / b);
        if ((int) Math.round(tel * b) == t) {
            Schijn.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), trommel, 0.35f, 1.0f);
            if (tel < TEL) {
                E2SpD1Recept.titel(p, Component.literal(String.valueOf(tel + 1)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        tel == TEL - 1 ? Component.translatable(E2SpD1Recept.P + "trommel.nu").withStyle(ChatFormatting.YELLOW) : null, 8);
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), trommel, 0.5f, tel == TEL - 1 ? 1.6f : 1.2f);
            }
        }
        // the bubbles: a ring that shrinks onto the drum, a pop on the beat
        Vec3 boven = Vec3.atCenterOf(trommel).add(0, 0.9, 0);
        for (int i = 0; i < NOTEN; i++) {
            int d = noot(i) - t;
            if (s.ronde.weg[i] || d < 0 || d > AANLOOP) {
                continue;                                             // (hit, spent, gone, or not coming yet)
            }
            if (d % 2 == 0) {
                double r = 0.15 + 1.25 * d / AANLOOP;
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    Schijn.deeltjes(p, d <= RAAM ? RING_DICHT : RING, boven.add(Math.cos(a) * r, 0.35 * d / AANLOOP, Math.sin(a) * r), 1, 0, 0);
                }
                Schijn.deeltjes(p, ParticleTypes.END_ROD, boven.add(0, 0.6 * d / AANLOOP, 0), 1, 0, 0);
            }
            if (d == 0) {
                Schijn.deeltjes(p, ParticleTypes.BUBBLE_POP, boven, 8, 0.25, 0.04);
                Schijn.geluid(p, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, trommel, 0.7f, 1.3f);
            }
        }
        boolean over = s.ronde.voorbij(t);
        if (t % 5 == 0) {
            p.sendOverlayMessage(Component.translatable(E2SpD1Recept.P + "trommel.stand", s.ronde.hits, NODIG, s.ronde.geweest(), NOTEN)
                    .withStyle(s.ronde.hits >= NODIG ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        if (!over) {
            return;
        }
        if (s.ronde.gewonnen()) {
            SPELLEN.remove(p.getUUID());
            E2SpD1Recept.trommelGewonnen(p, s.ronde.hits);
            return;
        }
        // a lost round: no penalty, it starts again on the spot
        s.pauzeTot = nu + OPNIEUW;
        SpAudio.stinger(p, "stinger.mislukt");
        Schijn.geluid(p, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), trommel, 0.6f, 0.8f);
        D1rActeurs.speel(p, "octo", "zucht");
        E2SpD1Recept.titel(p, Component.translatable(E2SpD1Recept.P + "trommel.bijna").withStyle(ChatFormatting.GOLD),
                Component.translatable(E2SpD1Recept.P + "trommel.bijna_onder", s.ronde.hits, NODIG).withStyle(ChatFormatting.YELLOW), 50);
    }
}
