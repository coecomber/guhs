package nl.juiced.guhs.feature.einde2.spverhaal;

import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 8), the skelet: the state helpers of the dev command and the tests. Never called by the
 * story itself.
 */
public final class SpVerhaalDev {
    private SpVerhaalDev() {
    }

    /** The Guhvatar story done for this player (Opper-Mika won, the five chapters done), when it is not done yet. */
    public static void guhvatarKlaar(ServerPlayer p) {
        if (E2Verhaal.finaleKlaar(p)) {
            return;
        }
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
        E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
    }

    /**
     * Puts the player at this step of a Sponsguh line: the Guhvatar story done, part 1 done when the line is part 2, the line
     * wiped and set to {@code stap} (begun). Side lines are sp_zij's ({@code /guhs einde2 sponsguh zij ...}).
     */
    public static void zetTot(ServerPlayer p, String lijn, int stap) {
        guhvatarKlaar(p);
        if (lijn.equals(SpVerhaal.KROON)) {
            klaar(p, SpVerhaal.RECEPT);
        }
        Verhaallijn l = SpVerhaal.lijn(lijn);
        l.wis(p);
        l.begin(p);
        l.zet(p, stap);
    }

    /** Finishes a Sponsguh line (all its steps). */
    public static void klaar(ServerPlayer p, String lijn) {
        Verhaallijn l = SpVerhaal.lijn(lijn);
        l.begin(p);
        l.zet(p, l.stappen());
    }

    /** Forgets both Sponsguh lines of this player. */
    public static void wis(ServerPlayer p) {
        for (String lijn : SpVerhaal.LIJNEN) {
            SpVerhaal.lijn(lijn).wis(p);
        }
    }

    /** Teleports the player to the default place of a step (false: anywhere, or not worked out yet). */
    public static boolean naarPlek(ServerPlayer p, String lijn, int stap) {
        SpStappen.Plek plek = SpStappen.standIn(lijn, stap);
        ServerLevel g = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        Vec3 naar = plek == null || g == null ? null : SpStappen.wereld(g, plek);
        if (naar == null) {
            return false;
        }
        p.teleportTo(g, naar.x, naar.y + 0.1, naar.z, Set.of(), p.getYRot(), 0f, true);
        p.resetFallDistance();
        return true;
    }
}
