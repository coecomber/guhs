package nl.juiced.guhs.feature.vadsrijk.kern;

import net.minecraft.server.level.ServerPlayer;

/**
 * First-time hints (ronde 9: minimal onboarding). {@link #toon} shows the hint once per player (remembered in
 * {@link SpelerData#tips}); the client shows it as a small pink card at the top right for ~8 s. Text: the lang key itself
 * ({@code vadsrijk.<slice>.tip.<id>}) and an optional {@code .titel}.
 */
public final class Tips {
    private Tips() {
    }

    public static boolean toon(ServerPlayer p, String key) {
        if (!Vadsrijk.in(p) || !Spelers.data(p).tips.add(key)) {
            return false;
        }
        KernPayloads.stuur(p, new KernPayloads.Tip(key));
        return true;
    }

    /** Dev: forget every hint of this player. */
    public static void vergeet(ServerPlayer p) {
        Spelers.data(p).tips.clear();
    }
}
