package nl.juiced.guhs.feature.vadsrijk.npc;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The FTB chapter "Het Vadsrijk" (tools/features/vadsrijk_npc.py ftb()): every quest of the questlog has a hidden
 * advancement guhs:quest/vr_&lt;quest id&gt; that is granted on hand-in; secrets count through vr_knuffels_10, vr_aanwijzingen_5
 * and vr_schat_&lt;id&gt;. Nothing in the chapter is locked.
 */
public final class NpcFtb {
    private NpcFtb() {
    }

    static void aangenomen(ServerPlayer p, Quest q) {
    }

    static void klaar(ServerPlayer p, Quest q) {
        GuhAdvancements.grant(p, "vr_" + q.id());
        NpcApi.QUEST_KLAAR.forEach(h -> h.accept(p, q.id()));
    }

    static void geheim(ServerPlayer p, String soort, String id, int totaal) {
        switch (soort) {
            case "knuffels" -> {
                if (totaal >= NpcPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_knuffels_alle");
                }
            }
            case "aanwijzingen" -> {
                if (totaal >= NpcPlekken.AANWIJZING.size()) {
                    GuhAdvancements.grant(p, "vr_aanwijzingen_alle");
                }
            }
            case "schatten" -> GuhAdvancements.grant(p, "vr_schat_" + id);
            default -> {
            }
        }
    }
}
