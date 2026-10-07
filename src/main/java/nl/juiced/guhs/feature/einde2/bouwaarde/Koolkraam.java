package nl.juiced.guhs.feature.einde2.bouwaarde;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * The koolman's stall in Ba Sing Guh: three spots (the markers {@code koolkraam_1} .. {@code koolkraam_3}, one in every
 * ring; each is the lowest corner of 5 x 4 x 4 free blocks) and two overlays of plain blocks, the stall whole
 * ({@code laag_koolkraam_heel}) and what is left of it after something came by ({@code laag_koolkraam_kapot}).
 * <p>
 * Phase A builds no stall: the spots stay free. This class puts one down for EVERYBODY (a dev command, a test, a server
 * that wants the stalls to stand).
 * hook fase B: the story shows a stall to ONE player (the sister update's Schijn takes the same two templates and the same
 * corners: {@link #hoek}), wrecks it when its scene plays and lets the koolman shout.
 */
public final class Koolkraam {
    public static final int SPOTS = 3;
    public static final String HEEL = "koolkraam_heel", KAPOT = "koolkraam_kapot";

    /** What stands on a spot. */
    public enum Staat {
        WEG, HEEL, KAPOT
    }

    /** The marker of spot nr (1 .. {@link #SPOTS}): the lowest corner of the stall, in template coordinates. */
    @Nullable
    public static Plekken.Plek plek(int nr) {
        return Plekken.plek(Einde2Ids.BA_SING_GUH, "koolkraam_" + nr);
    }

    /** The world position of that corner at the one guaranteed copy of the city (null: no such copy in this world, or no such spot). */
    @Nullable
    public static BlockPos hoek(ServerLevel guheinde, int nr) {
        return Plekken.wereld(guheinde, Einde2Ids.BA_SING_GUH, "koolkraam_" + nr);
    }

    /** Puts the stall of spot nr of the guaranteed copy in this state, for everybody. False: nothing there to change. */
    public static boolean zet(ServerLevel guheinde, int nr, Staat staat) {
        BlockPos hoek = hoek(guheinde, nr);
        return hoek != null && zet(guheinde, hoek, staat);
    }

    /** The same at any corner (a test floor, another copy): takes away what stood there and puts down the new state. */
    public static boolean zet(ServerLevel level, BlockPos hoek, Staat staat) {
        Plekken.Blad blad = Plekken.blad(Einde2Ids.BA_SING_GUH);
        Plekken.Laag heel = blad.lagen().get(HEEL), kapot = blad.lagen().get(KAPOT);
        if (heel == null || kapot == null) {
            return false;
        }
        boolean ok = Plekken.laag(level, heel, hoek, true) & Plekken.laag(level, kapot, hoek, true);
        if (staat != Staat.WEG) {
            ok &= Plekken.laag(level, staat == Staat.HEEL ? heel : kapot, hoek, false);
        }
        return ok;
    }

    private Koolkraam() {
    }
}
