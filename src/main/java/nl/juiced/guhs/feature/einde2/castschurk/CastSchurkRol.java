package nl.juiced.guhs.feature.einde2.castschurk;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice cast_schurk: the default role of its Mika kinds (CONTRACT_E2 11.2 G): a click says one of the kind's two lines
 * of small talk, {@code quest.guhs.einde2_cast_schurk.praatje.<kind>.0} / {@code .1}, and nothing else. A role registered for a
 * spot ({@code NpcRollen.zet(kind, "<structuur>/<plek>", rol)}) always wins over this one: that is how phase B gives a
 * resident its story.
 */
public final class CastSchurkRol implements NpcRole {
    public static final CastSchurkRol ROL = new CastSchurkRol();
    public static final String PRAATJE = "quest.guhs.einde2_cast_schurk.praatje.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        // hook fase B: M3 of CONTRACT_E2 11.7 lets the NPC play its "praat" animation here (npc.speel("praat"))
        GuhQuests.say(player, npc, PRAATJE + npc.getKind().id() + "." + npc.level().getRandom().nextInt(2));
    }

    private CastSchurkRol() {
    }
}
