package nl.juiced.guhs.feature.einde2.castteam;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice cast_team: the default role of every kind of {@link CastTeam} (CONTRACT_E2 11.2 G): a click says one of the
 * kind's two lines of small talk ({@code quest.guhs.einde2_cast_team.praatje.<kind>.0} / {@code .1}) and plays its {@code praat} animation (11.7 M3), nothing else.
 * A role registered for a spot ({@code NpcRollen.zet(kind, "<structuur>/<plek>", rol)}) wins over this one.
 */
public final class CastTeamRol implements NpcRole {
    public static final CastTeamRol ROL = new CastTeamRol();
    public static final String PRAATJE = "quest.guhs.einde2_cast_team.praatje.";

    /** The key of line i (0 or 1) of a kind. */
    public static String praatje(GuhNpcEntity.Kind kind, int i) {
        return PRAATJE + kind.id() + "." + i;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GuhQuests.say(player, npc, praatje(npc.getKind(), npc.level().getRandom().nextInt(2)));
        npc.speel("praat");                                    // (CONTRACT_E2 11.7 M3)
    }

    private CastTeamRol() {
    }
}
