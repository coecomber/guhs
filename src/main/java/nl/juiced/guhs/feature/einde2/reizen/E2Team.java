package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice reizen. Team Guhvatar as it travels with a player: Aang + Momo after
 * {@code guhvatar_lucht}, Katara + Sokka after {@code guhvatar_water}, Toph after {@code guhvatar_aarde}, Zuka from the flag
 * {@code zuka_team}, each in the look of CONTRACT_E2_B 7.4. Skelet stand-in: nobody, and nothing is placed.
 */
public final class E2Team {
    private E2Team() {
    }

    /** The members of this player's team. */
    public static List<GuhNpcEntity.Kind> team(ServerPlayer p) {
        return List.of();
    }

    /** The team at the {@code team_*} markers of this structure, seen only by p and their guests. */
    public static void zetInStructuur(ServerPlayer p, String structuur) {
    }

    /** Takes p's team away from this structure. */
    public static void weg(ServerPlayer p, String structuur) {
    }
}
