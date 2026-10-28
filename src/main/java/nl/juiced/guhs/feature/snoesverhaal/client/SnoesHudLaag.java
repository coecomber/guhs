package nl.juiced.guhs.feature.snoesverhaal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;
import nl.juiced.guhs.feature.snoesverhaal.SnoesHud;
import nl.juiced.guhs.feature.snoesverhaal.SnoesVerhaal.Fase;

/**
 * Playtest: Snoes' story in the top left corner (the safari quest tracker's look): a pink title, the pieces found
 * (✓liedje ✓ster · herinnering ...) and a short hint for the next step. Shown from Snoes at your bed until the finale;
 * hidden while a screen is open and with F1.
 */
public final class SnoesHudLaag {
    private static final int MAX_BREED = 200, TITEL = 0xFFF7B6CB, TEKST = 0xFFF4F0E8, ACHTER = 0x70201018;

    private SnoesHudLaag() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) ->
                event.registerAboveAll(nl.juiced.guhs.Guhs.id("snoes_hud"), SnoesHudLaag::extractRenderState));
    }

    /** The lines under the title (empty: the story is not running, nothing shows). */
    static List<Component> regels(SnoesHud.Stand s) {
        List<Component> uit = new ArrayList<>();
        Fase[] fases = Fase.values();
        Fase f = s.fase() >= 0 && s.fase() < fases.length ? fases[s.fase()] : Fase.NIETS;
        switch (f) {
            case BIJ_BED -> uit.add(Component.translatable("gui.guhs.snoes.hud.bij_bed"));
            case INGESTOPT -> uit.add(Component.translatable("gui.guhs.snoes.hud.ingestopt"));
            case DROOMT -> {
                int n = Integer.bitCount(s.stukjes());
                MutableComponent lijst = Component.translatable("gui.guhs.snoes.hud.stukjes", n, Stukje.values().length).append(" ");
                for (Stukje st : Stukje.values()) {
                    boolean heeft = (s.stukjes() & (1 << st.ordinal())) != 0;
                    lijst.append(Component.literal(heeft ? "✓" : "·").withStyle(heeft ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                            .append(Component.translatable("gui.guhs.snoes.stukje." + st.id())
                                    .withStyle(heeft ? ChatFormatting.WHITE : ChatFormatting.GRAY))
                            .append(" ");
                }
                uit.add(lijst);
                uit.add(Component.translatable(n == Stukje.values().length ? "gui.guhs.snoes.hud.finale"
                        : s.droomt() ? "gui.guhs.snoes.hud.zoek" : "gui.guhs.snoes.hud.terug").withStyle(ChatFormatting.ITALIC));
            }
            default -> {
            }
        }
        return uit;
    }

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.level == null || mc.player == null) {
            return;
        }
        List<Component> regels = regels(SnoesHud.Client.stand);
        if (regels.isEmpty()) {
            return;
        }
        Font font = mc.font;
        Component titel = Component.translatable("gui.guhs.snoes.hud.titel");
        int breed = font.width(titel);
        int hoog = 12;
        for (Component r : regels) {
            breed = Math.max(breed, Math.min(MAX_BREED, font.width(r)));
            hoog += font.split(r, MAX_BREED).size() * 10;
        }
        int x = 8, y = 6;
        g.fill(x - 4, y - 3, x + breed + 4, y + hoog + 1, ACHTER);
        g.text(font, titel, x, y, TITEL, true);
        y += 12;
        for (Component r : regels) {
            for (var lijn : font.split(r, MAX_BREED)) {
                g.text(font, lijn, x, y, TEKST, true);
                y += 10;
            }
        }
    }
}
