package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

// STUB-koppeling: the koppeling slice implements this (queue for the player's Leverkist(en) in the main world); the lead
// replaces this file at the merge. Until then the items go straight into the player's pockets (or drop at their feet), so
// "Naar huis sturen" can be tested.
public final class LeverkistPost {
    private LeverkistPost() {
    }

    public static void verstuur(MinecraftServer s, UUID eigenaar, List<ItemStack> spul) {
        net.minecraft.server.level.ServerPlayer p = s.getPlayerList().getPlayer(eigenaar);
        if (p == null) {
            return;
        }
        for (ItemStack st : spul) {
            if (!p.getInventory().add(st.copy())) {
                p.drop(st.copy(), false);
            }
        }
    }
}
