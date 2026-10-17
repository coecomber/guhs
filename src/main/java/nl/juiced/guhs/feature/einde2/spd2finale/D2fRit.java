package nl.juiced.guhs.feature.einde2.spd2finale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.DavidHasselguhEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.Hasselguh;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;

/**
 * einde2 "Sponsguh", slice sp_d2_finale: step 9's ride home on David Hasselguh's back ({@link Hasselguh#draag}). The player's
 * own David (only he sees him; the resident one beside the pier is hidden for him at this step) floats at
 * {@code schelpenstad/d2f_hasselguh_start}; the player is seated first, Sponsguh and Patguh behind him
 * ({@link DavidHasselguhEntity#zetMee}), and David swims to {@code bikinibodem/hasselguh_landing} (about 24 s; his straight arc
 * is checked against both builds by the generator: einde2_sp_d2_finale_plekken). On the way: the banter in the chat (travel
 * talk) and the moment's music. The landing ({@link Hasselguh#AANGEKOMEN}) ends the step; a broken ride (a logout, a teleport)
 * puts the player back on the pier and the pier simply starts it again.
 */
final class D2fRit {
    /** Ticks after the start of each banter line. */
    static final int[] PRAAT = {50, 150, 250, 350};

    private D2fRit() {
    }

    /** The spot of the ride's start and its goal in the Guheinde (null: not known yet). */
    @Nullable
    static Vec3 start(ServerLevel g) {
        BlockPos b = Plekken.bekend(g, SpVerhaal.SCHELPENSTAD) ? Plekken.wereld(g, SpVerhaal.SCHELPENSTAD, "d2f_hasselguh_start") : null;
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    @Nullable
    static Vec3 doel(ServerLevel g) {
        BlockPos b = Plekken.bekend(g, SpVerhaal.BIKINIBODEM) ? Plekken.wereld(g, SpVerhaal.BIKINIBODEM, "hasselguh_landing") : null;
        return b == null ? null : Vec3.atBottomCenterOf(b);
    }

    /** Hop on: true when the ride started. */
    static boolean vertrek(ServerPlayer p) {
        ServerLevel g = p.level();
        Vec3 start = start(g), doel = doel(g);
        if (start == null || doel == null || Hasselguh.draagt(p)) {
            return false;
        }
        D2fStoet.Sessie s = D2fStoet.van(p);
        D2fStoet.weg(s.david);
        DavidHasselguhEntity david = D2fStoet.maak(p, E2SponsguhFeature.DAVID_HASSELGUH.get(), start, 180f);
        s.david = david;
        if (david == null) {
            return false;
        }
        p.stopRiding();
        // first on HIS back (the ride takes the nearest David: never the resident one), then off
        if (!p.startRiding(david, true, true) || !Hasselguh.draag(p, doel)) {
            p.stopRiding();
            D2fStoet.weg(david);
            s.david = null;
            p.sendSystemMessage(Component.translatable(E2SpD2Finale.G + "rit.mislukt").withStyle(ChatFormatting.GRAY));
            return false;
        }
        D2fStoet.metJe(p, true);
        if (p.getVehicle() instanceof DavidHasselguhEntity drager) {
            if (s.sponsguh != null) {
                s.sponsguh.getNavigation().stop();
                drager.zetMee(s.sponsguh);
            }
            if (s.patguh != null) {
                s.patguh.getNavigation().stop();
                drager.zetMee(s.patguh);
            }
        }
        SpAudio.moment(p, "hasselguh");
        for (int i = 0; i < PRAAT.length; i++) {
            String key = E2SpD2Finale.G + "rit." + (i + 1);
            E2SpD2Finale.later(p, PRAAT[i], q -> {
                if (Hasselguh.draagt(q)) {
                    q.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA));
                }
            });
        }
        return true;
    }

    /** Hasselguh.AANGEKOMEN: home. The step moves on, David swims off, and the scene "thuis" plays. */
    static void aangekomen(ServerPlayer p) {
        if (SpVerhaal.KROON_LIJN.stap(p) != SpVerhaal.K_HASSELGUH) {
            return;
        }
        SpAudio.stopMoment(p, 40);
        D2fStoet.Sessie s = D2fStoet.als(p);
        DavidHasselguhEntity david = s == null ? null : s.david;
        if (s != null) {
            s.david = null;
        }
        E2SpD2Finale.later(p, 50, q -> D2fStoet.weg(david));
        if (SpVerhaal.KROON_LIJN.verder(p, SpVerhaal.K_HASSELGUH)) {
            E2SpD2Finale.later(p, 12, E2SpD2Finale::thuis);
        }
    }
}
