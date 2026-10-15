package nl.juiced.guhs.feature.einde2.geestwezens;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice geestmodellen: the default role of its two kinds (CONTRACT_E2 11.2 G): a click plays {@code praat} and says one
 * of the kind's two lines, {@code quest.guhs.einde2_geestmodellen.praatje.<kind>.0} / {@code .1}, nothing else. A role
 * registered for a spot ({@code NpcRollen.zet(kind, "<structuur>/<plek>", rol)}) always wins over this one: that is how
 * phase B gives the Theegeest his tea pavilion and phase C the Bestelguh his counter.
 */
public final class GeestRol implements NpcRole {
    public static final GeestRol ROL = new GeestRol();

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.speel("praat");
        GuhQuests.say(player, npc, GeestWezen.PRAATJE + npc.getKind().id() + "." + npc.level().getRandom().nextInt(2));
    }

    private GeestRol() {
    }
}
