package nl.juiced.guhs.feature.erfenis;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.erfenis.koppeling.Leverkisten;

/**
 * (owner: koppeling; CONTRACT_OPKNAPPER) "Naar huis sturen": the village's post for the player's Leverkist(en) in the main
 * world. {@link #verstuur} queues copies of the stacks for the owner (saved), and delivers what fits at once into their
 * loaded Leverkisten, in placing order; the rest waits and comes as soon as a Leverkist's chunk is loaded and has room.
 * The CAPS (balans 9.4) are the caller's job: see {@code koppeling.LeverkistPlafond} ({@code beperk}).
 */
public final class LeverkistPost {
    private LeverkistPost() {
    }

    public static void verstuur(MinecraftServer s, UUID eigenaar, List<ItemStack> spul) {
        Leverkisten.verstuur(s, eigenaar, spul);
    }

    /** How many items still wait for a Leverkist of this owner. */
    public static int wacht(MinecraftServer s, UUID eigenaar) {
        return Leverkisten.wacht(s, eigenaar);
    }
}
