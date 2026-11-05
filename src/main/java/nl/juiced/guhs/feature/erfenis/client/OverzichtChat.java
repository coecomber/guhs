package nl.juiced.guhs.feature.erfenis.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

/**
 * 1.9.6: the chat inside the Erfenis overview (the vanilla chat isn't drawn under a screen): what other players say (and
 * the other normal chat lines) at the left, in the middle of the screen, fading out after a while like the real chat.
 */
final class OverzichtChat {
    private record Regel(Component tekst, long tijd) {
    }

    /** How long a line stays, and how many at most. */
    private static final long BLIJFT = 20_000, VERVAAG = 3_000;
    private static final int MAX = 8;
    private static final Deque<Regel> REGELS = new ArrayDeque<>();

    private OverzichtChat() {
    }

    /** Every chat line that lands in the normal chat (player chat and the non-overlay system lines). */
    static void opChat(ClientChatReceivedEvent event) {
        if (event.isCanceled() || event instanceof ClientChatReceivedEvent.System s && s.isOverlay()) {
            return;
        }
        synchronized (REGELS) {
            REGELS.addLast(new Regel(event.getMessage(), System.currentTimeMillis()));
            while (REGELS.size() > MAX) {
                REGELS.removeFirst();
            }
        }
    }

    /** Draws the recent lines at the left, around the middle of the screen. */
    static void teken(Ui ui, Font font, int breedte, int hoogte) {
        long nu = System.currentTimeMillis();
        List<FormattedCharSequence> zinnen = new ArrayList<>();
        List<Integer> alfa = new ArrayList<>();
        int maxW = Math.max(120, Math.min(260, breedte / 3));
        synchronized (REGELS) {
            REGELS.removeIf(r -> nu - r.tijd() > BLIJFT);
            for (Regel r : REGELS) {
                long over = BLIJFT - (nu - r.tijd());
                int a = (int) (255 * Math.min(1.0, over / (double) VERVAAG));
                for (FormattedCharSequence z : font.split(r.tekst(), maxW - 8)) {
                    zinnen.add(z);
                    alfa.add(Math.max(16, a));
                }
            }
        }
        if (zinnen.isEmpty()) {
            return;
        }
        while (zinnen.size() > 12) {   // (long lines: the newest twelve)
            zinnen.remove(0);
            alfa.remove(0);
        }
        int x = 6, y = hoogte / 2 - zinnen.size() * 10 / 2;
        for (int i = 0; i < zinnen.size(); i++) {
            int a = alfa.get(i);
            ui.g.fill(x, y + i * 10 - 1, x + maxW, y + i * 10 + 9, (a * 96 / 255) << 24);
            ui.g.text(font, zinnen.get(i), x + 4, y + i * 10, (a << 24) | 0xFFFFFF, true);
        }
    }
}
