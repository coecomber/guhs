package nl.juiced.guhs.feature.elftocht;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;

/**
 * The Stempelguh of one of the eleven villages (STEMPELGUH). Which village: its tag {@code guhs_elftocht_dorp_<n>} (set
 * by the village template, see tools/features/elftocht_dorp_api.py), remembered in its RoleData {@code Dorp}. Its name
 * becomes "Stempelguh van &lt;dorp&gt;" (lang {@code gui.guhs.elftocht.stempelguh.<n>}; the client picks the village's own
 * hat and scarf from that key). Right-click: a stamp ({@link ElftochtTocht#stempel}).
 */
public class StempelRole implements NpcRole {
    public static final String TAG = "guhs_elftocht_dorp_";

    /** The village (1..11) of this Stempelguh, 0 when it has none. */
    public static int dorp(GuhNpcEntity npc) {
        if (npc.roleData.contains("Dorp")) {
            return npc.roleData.getInt("Dorp");
        }
        for (String tag : npc.getTags()) {
            if (tag.startsWith(TAG)) {
                try {
                    int n = Integer.parseInt(tag.substring(TAG.length()));
                    npc.roleData.putInt("Dorp", n);
                    return n;
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        ElftochtTocht.stempel(npc, player, dorp(npc));
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 40 == 1) {
            int dorp = dorp(npc);
            if (dorp >= 1 && dorp <= ElftochtTocht.VOLGORDE.length) {
                Component name = Component.translatable("gui.guhs.elftocht.stempelguh." + dorp);
                if (!name.equals(npc.getCustomName())) {
                    npc.setCustomName(name);
                }
            }
        }
    }
}
