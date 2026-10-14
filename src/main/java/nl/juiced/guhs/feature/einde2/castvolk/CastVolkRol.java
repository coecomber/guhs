package nl.juiced.guhs.feature.einde2.castvolk;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice cast_volk: the default role of every kind of {@link CastVolk} (CONTRACT_E2 11.2 G). A click says one of the
 * kind's two lines of small talk ({@code quest.guhs.einde2_cast_volk.praatje.<kind>.0} / {@code .1}, picked by the level's
 * random) and plays its {@code praat} animation (11.7 M3), nothing else: no screen, no shop, no state. A role phase B registers for a spot
 * ({@code NpcRollen.zet(kind, "<structuur>/<plek>", rol)}) always wins over this one.
 */
public final class CastVolkRol implements NpcRole {
    public static final CastVolkRol ROL = new CastVolkRol();
    public static final String PRAATJE = "quest.guhs.einde2_cast_volk.praatje.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GuhQuests.say(player, npc, PRAATJE + npc.getKind().id() + "." + npc.level().getRandom().nextInt(2));
        npc.speel("praat");                                    // (CONTRACT_E2 11.7 M3)
    }

    private CastVolkRol() {
    }
}
