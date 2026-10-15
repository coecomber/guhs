package nl.juiced.guhs.feature.einde2.reizen;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 4.4), slice reizen: what a member of Team Guhvatar says at the campfire of a story
 * structure: two lines per member per structure, in turn ({@code gui.guhs.einde2_reizen.team.<structuur>.<lid>.<1|2>}).
 */
final class TeamRol implements NpcRole {
    /** roleData: how many times this copy has talked (the next line is the other one). */
    private static final String KEER = "guhs_einde2_reizen_keer";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        String plek = npc.roleData.getStringOr(NpcRollen.PLEK, "");
        int streep = plek.indexOf("/team_");
        if (streep < 0) {
            return;
        }
        String structuur = plek.substring(0, streep), lid = plek.substring(streep + "/team_".length());
        int keer = npc.roleData.getIntOr(KEER, 0);
        npc.roleData.putInt(KEER, keer + 1);
        GuhQuests.say(player, npc, sleutel(structuur, lid, keer % 2 + 1));
        npc.getLookControl().setLookAt(player);
    }

    static String sleutel(String structuur, String lid, int n) {
        return "gui.guhs.einde2_reizen.team." + structuur + "." + lid + "." + n;
    }
}
