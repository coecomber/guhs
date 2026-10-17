package nl.juiced.guhs.feature.einde2.spd2finale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.Richting;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: step 10, Plankmika's bucket helmets. Six guhs of Bikinibodem walk about with a bucket
 * on their head (the player's own copies, {@link D2fStoet}; on the markers {@code d2f_emmer_1..6} between the stadspoort and the
 * Bellenkom, each under a golden column of light only this player sees). A puff of the bubble blaster ({@link BellenblazerItem})
 * at a bucket knocks it off: the guh blinks, cheers, says thank you and walks along with the player. A counter is always on the
 * action bar ("Emmers eraf: n/6"); when all six are free a title says so and the step's text becomes "10_b": walk to the
 * Bellenkom. Entering the Bellenkom then ends the step. No timer, no way to fail, no item lost; the freed ones are kept in the
 * line (a bit mask, teller {@value #VRIJ}), so leaving, dying or logging out never undoes one.
 */
final class D2fEmmers {
    static final int AANTAL = 6;
    /** The line's teller (CONTRACT_E2_C 2.4: a slice's own name) with a bit per freed guh. */
    static final String VRIJ = "sp_d2_finale_vrij";
    /** How far a puff carries, and how wide it is (the cosine of the half angle: forgiving). */
    static final double BEREIK = 12.0, KEGEL = 0.94;
    static final String G = E2SpD2Finale.G;

    private D2fEmmers() {
    }

    static int masker(ServerPlayer p) {
        return SpVerhaal.KROON_LIJN.teller(p, VRIJ);
    }

    static boolean isVrij(ServerPlayer p, int i) {
        return (masker(p) & (1 << i)) != 0;
    }

    static int vrij(ServerPlayer p) {
        return Integer.bitCount(masker(p) & ((1 << AANTAL) - 1));
    }

    static boolean alleVrij(ServerPlayer p) {
        return vrij(p) >= AANTAL;
    }

    /** (dev, step 11's start) all six free. */
    static void allemaalVrij(ServerPlayer p) {
        SpVerhaal.KROON_LIJN.teller(p, VRIJ, (1 << AANTAL) - 1);
    }

    // =====================================================================================================================
    // the game
    // =====================================================================================================================

    /** The marker of guh i (0-based). */
    static String marker(int i) {
        return "d2f_emmer_" + (i + 1);
    }

    /** Every half second while the step is on and the player is in Bikinibodem (not in a scene): the guhs, the columns, the counter. */
    static void tick(ServerPlayer p, ServerLevel g, long nu) {
        D2fStoet.Sessie s = D2fStoet.van(p);
        for (int i = 0; i < AANTAL; i++) {
            if (isVrij(p, i)) {
                D2fStoet.weg(s.emmers[i]);
                s.emmers[i] = null;
                continue;
            }
            if (!D2fStoet.levend(s.emmers[i], p)) {
                BlockPos b = Plekken.wereld(g, SpVerhaal.BIKINIBODEM, marker(i));
                if (b == null || b.distSqr(p.blockPosition()) > 128 * 128) {
                    continue;                                                // (made when the player comes nearer: its chunk is loaded then)
                }
                SponsguhWezen w = D2fStoet.maak(p, E2SponsguhFeature.BIKINIBODEM_GUH.get(), Vec3.atBottomCenterOf(b), (i * 67) % 360 - 180);
                if (w != null) {
                    w.zetSoort(i);
                    SpCast.zetLook(w, SpCast.EMMER, true);
                    w.zetAnimatie("loop");                                   // (marching in place, stiff as a bucket)
                }
                s.emmers[i] = w;
            }
            SponsguhWezen w = s.emmers[i];
            if (w != null && w.distanceToSqr(p) < 160 * 160) {
                // a golden column over each one that still wears a bucket (seen from far, for this player only)
                Vec3 top = w.position().add(0, w.getBbHeight() + 0.5, 0);
                g.sendParticles(p, ParticleTypes.END_ROD, true, true, top.x, top.y + 4.0, top.z, 6, 0.05, 3.5, 0.05, 0.0);
                g.sendParticles(p, ParticleTypes.WAX_ON, true, true, top.x, top.y, top.z, 2, 0.25, 0.2, 0.25, 0.0);
            }
        }
        // the freed ones walk along (the ones of an earlier visit come back as followers)
        int bij = 0;
        for (SponsguhWezen w : s.vrij) {
            if (D2fStoet.levend(w, p)) {
                bij++;
            }
        }
        s.vrij.removeIf(w -> !D2fStoet.levend(w, p));
        for (int i = bij; i < vrij(p); i++) {
            SponsguhWezen w = D2fStoet.maak(p, E2SponsguhFeature.BIKINIBODEM_GUH.get(), D2fStoet.naast(p, (i % 3 - 1) * 1.5, -2.5 - i * 0.6), p.getYRot());
            if (w != null) {
                w.zetSoort(i);
                w.zetStil(false);
                s.vrij.add(w);
            }
        }
        for (SponsguhWezen w : s.vrij) {
            D2fStoet.volg(p, w);
        }
        // the counter, once a second
        if (nu - s.laatsteTeller >= 20) {
            s.laatsteTeller = nu;
            if (alleVrij(p)) {
                BlockPos poort = Plekken.wereld(g, SpVerhaal.BIKINIBODEM, "d2f_bellenkom_poort");
                Component waar = poort == null ? Component.empty() : Component.literal(" ").append(Richting.van(p.position(), Vec3.atCenterOf(poort)));
                p.sendOverlayMessage(Component.translatable(G + "spel.naar_bellenkom").append(waar).withStyle(ChatFormatting.GOLD));
                if (poort != null) {
                    Vec3 c = Vec3.atBottomCenterOf(poort);
                    g.sendParticles(p, ParticleTypes.END_ROD, true, true, c.x, c.y + 4.0, c.z, 8, 0.05, 3.5, 0.05, 0.0);
                }
            } else {
                p.sendOverlayMessage(Component.translatable(G + "spel.teller", vrij(p), AANTAL).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The start of the game (after the scene thuis): its title and rule, and the bubble blaster in your paws. */
    static void start(ServerPlayer p) {
        E2SpD2Finale.titel(p, Component.translatable(G + "spel.titel").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable(G + "spel.regel", AANTAL).withStyle(ChatFormatting.WHITE), 60);
        SpAudio.stinger(p, "deuntje.emmers");
        geefBlazer(p, false);
    }

    /** The bubble blaster: given once, and again when it is not in the inventory any more (never lost for good). */
    static void geefBlazer(ServerPlayer p, boolean weer) {
        if (p.getInventory().hasAnyMatching(st -> st.is(E2SpD2FinaleFeature.BELLENBLAZER.get()))) {
            return;
        }
        Minigames.give(p, new ItemStack(E2SpD2FinaleFeature.BELLENBLAZER.get()));
        p.sendSystemMessage(Component.translatable(G + (weer ? "spel.blazer_weer" : "spel.blazer")).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A puff of the bubble blaster (server): the guh with a bucket in front of the player, within reach, loses it. */
    static void blaas(ServerPlayer p) {
        if (SpVerhaal.KROON_LIJN.stap(p) != SpVerhaal.K_EMMERS || Cutscenes.bezig(p)) {
            return;
        }
        D2fStoet.Sessie s = D2fStoet.als(p);
        if (s == null) {
            return;
        }
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        int beste = -1;
        double besteHoek = KEGEL;
        for (int i = 0; i < AANTAL; i++) {
            SponsguhWezen w = s.emmers[i];
            if (!D2fStoet.levend(w, p) || isVrij(p, i)) {
                continue;
            }
            Vec3 naar = w.position().add(0, w.getBbHeight() * 0.85, 0).subtract(oog);
            double d = naar.length();
            if (d > BEREIK || d < 0.01) {
                continue;
            }
            double hoek = naar.scale(1 / d).dot(kijk);
            if (hoek > besteHoek && p.hasLineOfSight(w)) {
                besteHoek = hoek;
                beste = i;
            }
        }
        if (beste >= 0) {
            bevrijd(p, beste);
        } else {
            boolean dichtbij = false;
            for (SponsguhWezen w : s.emmers) {
                dichtbij |= D2fStoet.levend(w, p) && w.distanceToSqr(p) < 24 * 24;
            }
            if (dichtbij) {
                p.sendOverlayMessage(Component.translatable(G + "spel.mis").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /**
     * Guh i is free (once): the line remembers it, its entity (when there is one) loses the bucket, blinks, cheers and walks
     * along; at six a title. True = newly freed. Works without any entity (tests, a dev command).
     */
    static boolean bevrijd(ServerPlayer p, int i) {
        if (i < 0 || i >= AANTAL || SpVerhaal.KROON_LIJN.stap(p) != SpVerhaal.K_EMMERS || isVrij(p, i)) {
            return false;
        }
        SpVerhaal.KROON_LIJN.teller(p, VRIJ, masker(p) | (1 << i));
        D2fStoet.Sessie s = D2fStoet.als(p);
        SponsguhWezen w = s == null ? null : s.emmers[i];
        if (w != null && D2fStoet.levend(w, p)) {
            s.emmers[i] = null;
            SpCast.zetLook(w, SpCast.EMMER, false);
            w.zetAnimatie("idle");
            w.zetAnimatie("schrik");
            w.zetStil(false);
            w.getLookControl().setLookAt(p);
            ServerLevel level = p.level();
            Vec3 kop = w.position().add(0, w.getBbHeight() + 0.2, 0);
            level.sendParticles(ParticleTypes.BUBBLE_POP, kop.x, kop.y, kop.z, 24, 0.4, 0.3, 0.4, 0.05);
            level.sendParticles(ParticleTypes.CRIT, kop.x, kop.y, kop.z, 14, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, w.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.35f, 1.9f);
            E2SpD2Finale.later(p, 18, q -> {
                if (w.isAlive()) {
                    w.zetAnimatie("juich");
                }
            });
            s.vrij.add(w);
        }
        p.sendSystemMessage(Component.translatable(G + "vrij." + (i + 1)).withStyle(ChatFormatting.WHITE));
        int n = vrij(p);
        if (n >= AANTAL) {
            E2SpD2Finale.titel(p, Component.translatable(G + "spel.klaar.titel").withStyle(ChatFormatting.GOLD),
                    Component.translatable(G + "spel.klaar.onder").withStyle(ChatFormatting.YELLOW), 70);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            SpAudio.stinger(p, "stinger.opgelost");
            E2SpD2Finale.naarBellenkomHint(p);
        } else {
            E2SpD2Finale.titel(p, Component.empty(), Component.translatable(G + "spel.raak").append("  ")
                    .append(Component.translatable(G + "spel.teller", n, AANTAL)).withStyle(ChatFormatting.LIGHT_PURPLE), 30);
        }
        if (s != null) {
            s.laatsteTeller = 0;                                              // (the counter at once)
        }
        return true;
    }

    /** The step ends when all six are free and the player walks into the Bellenkom. True = moved on. */
    static boolean inBellenkom(ServerPlayer p) {
        return alleVrij(p) && SpVerhaal.KROON_LIJN.verder(p, SpVerhaal.K_EMMERS);
    }

    /** The marker of the nearest guh still under a bucket (the compass of step 10), null when all are free or unknown. */
    @Nullable
    static BlockPos dichtsteEmmer(ServerPlayer p, ServerLevel g) {
        BlockPos beste = null;
        double d = Double.MAX_VALUE;
        for (int i = 0; i < AANTAL; i++) {
            if (isVrij(p, i)) {
                continue;
            }
            BlockPos b = Plekken.wereld(g, SpVerhaal.BIKINIBODEM, marker(i));
            if (b != null && b.distSqr(p.blockPosition()) < d) {
                d = b.distSqr(p.blockPosition());
                beste = b;
            }
        }
        return beste;
    }
}
