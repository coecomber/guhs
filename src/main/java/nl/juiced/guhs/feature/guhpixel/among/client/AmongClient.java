package nl.juiced.guhs.feature.guhpixel.among.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhpixel.among.AmongGuhEntity;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.feature.guhpixel.among.AmongSlice;
import nl.juiced.guhs.feature.guhpixel.client.GuhpixelClient;
import nl.juiced.guhs.taal.Tekst;

/**
 * Client side of Among Guhs: the guh NPCs in their coloured space suits (a GuhRenderer hook: the suit bones again in the
 * participant's colour, with a pale visor), the HUD of a round (role, the crew's task bar, the own tasks, sabotage,
 * cooldowns) and the screens: the queue ({@link WachtrijScherm}), the meeting ({@link VergaderScherm}), the task
 * mini-games ({@link TaakSpellen}; {@link TaakScherm} is the waiting panel for a kind without one), the ship map
 * ({@link KaartScherm}: sabotage, vents) and the personal logbook ({@link CijfersScherm}).
 */
public final class AmongClient {
    private static final Identifier PAK = Guhs.id("textures/entity/among_pakje.png");
    private static final Identifier VIZIER = Guhs.id("textures/entity/among_vizier.png");
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xD8301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A,
            ROOD = 0xFFFF6B6B;

    @Nullable
    private static CompoundTag hud;
    private static long hudTick;
    private static final Map<String, Function<CompoundTag, Screen>> TAAK_SCHERMEN = new HashMap<>();

    public static void init(IEventBus modBus) {
        AmongPayloads.ontvanger = p -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> ontvang(mc, p));
        };
        TaakSpellen.registreer();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(AmongSlice.AMONG_GUH.get(), GuhRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("among_hud"), AmongClient::tekenHud));
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (guh instanceof AmongGuhEntity among) {
                frame.pass(PAK, 0xFF000000 | among.kleur().rgb, bone -> bone.startsWith("outfit_suit"));
                frame.pass(VIZIER, 0xFFFFFFFF, bone -> bone.startsWith("outfit_glasses"));
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            hud = null;
            GuhpixelClient.hudVerborgen = false;
        });
    }

    /**
     * The screen of a kind of task (the id of TaakSoorten): the screen gets the tag of the "taak" payload (Paneel, Soort,
     * Duur, Stap, Stappen, Kamer, Herstel) and answers with {@code new AmongPayloads.Actie(TAAK_KLAAR, paneel, 0, result)}
     * when the task is done, or TAAK_STOP when it is closed early. Kinds without a screen get the waiting panel.
     */
    public static void taakScherm(String soort, Function<CompoundTag, Screen> maker) {
        TAAK_SCHERMEN.put(soort, maker);
    }

    private static void ontvang(Minecraft mc, AmongPayloads.Scherm p) {
        CompoundTag data = p.data();
        switch (p.soort()) {
            case AmongPayloads.HUD -> {
                hud = data.isEmpty() ? null : data;
                hudTick = mc.level == null ? 0 : mc.level.getGameTime();
                GuhpixelClient.hudVerborgen = hud != null;
            }
            case AmongPayloads.WACHTRIJ -> {
                if (data.getBooleanOr("Dicht", false)) {
                    if (mc.screen instanceof WachtrijScherm) {
                        mc.setScreen(null);
                    }
                } else if (mc.screen instanceof WachtrijScherm s) {
                    s.update(data);
                } else if (data.getBooleanOr("Open", false)) {
                    mc.setScreen(new WachtrijScherm(data));
                }
            }
            case AmongPayloads.VERGADERING -> {
                if (data.getBooleanOr("Dicht", false)) {
                    if (mc.screen instanceof VergaderScherm) {
                        mc.setScreen(null);
                    }
                } else if (mc.screen instanceof VergaderScherm s) {
                    s.update(data);
                } else if (data.getBooleanOr("Open", false)) {
                    mc.setScreen(new VergaderScherm(data));
                }
            }
            case AmongPayloads.TAAK -> mc.setScreen(TAAK_SCHERMEN.getOrDefault(data.getStringOr("Soort", ""), TaakScherm::new).apply(data));
            case AmongPayloads.KAART -> mc.setScreen(new KaartScherm(data));
            case AmongPayloads.CIJFERS -> mc.setScreen(new CijfersScherm(data));
            default -> {
            }
        }
    }

    static Component seconden(int ticks) {
        return Component.translatable("gui.guhs.among.hud.seconden", Math.max(0, (ticks + 19) / 20));
    }

    /** Top left while a round runs: the role, the crew's task bar, the own tasks, what is sabotaged, the Mika's cooldowns. */
    private static void tekenHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        CompoundTag h = hud;
        if (h == null || mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        int verstreken = (int) Math.max(0, mc.level.getGameTime() - hudTick);
        boolean mika = h.getBooleanOr("Mika", false), wakker = h.getBooleanOr("Wakker", true);
        ListTag taken = h.getListOrEmpty("Taken");
        int x = 4, y = 4, w = 168, regel = 10;
        int sabotage = h.getIntOr("Sabotage", 0);
        // the task lines first: the panel is as wide as the longest one
        Component[] regels = new Component[taken.size()];
        boolean[] af = new boolean[taken.size()];
        for (int i = 0; i < taken.size(); i++) {
            CompoundTag t = taken.getCompoundOrEmpty(i);
            int stap = t.getIntOr("Stap", 0), stappen = Math.max(1, t.getIntOr("Stappen", 1));
            af[i] = stap >= stappen;
            Component naam = Component.translatable("gui.guhs.among.taak." + t.getStringOr("Soort", ""));
            Component tekst = stappen > 1
                    ? Component.translatable("gui.guhs.among.hud.taak_stappen", naam, Tekst.get(t, "Kamer"), Math.min(stap, stappen), stappen)
                    : Component.translatable("gui.guhs.among.hud.taak", naam, Tekst.get(t, "Kamer"));
            regels[i] = Component.literal(af[i] ? "✔ " : "□ ").append(tekst);
            w = Math.max(w, mc.font.width(regels[i]) + 12);
        }
        int hoogte = 30 + taken.size() * regel + (mika ? 2 * regel + 2 : 0) + (sabotage != 0 ? regel + 4 : 0) + 4;
        g.fill(x - 1, y - 1, x + w + 1, y + hoogte + 1, RAND);
        g.fill(x, y, x + w, y + hoogte, PANEEL);
        Component rol = Component.translatable(mika ? "gui.guhs.among.hud.mika" : "gui.guhs.among.hud.crew").withStyle(ChatFormatting.BOLD);
        g.text(mc.font, rol, x + 5, y + 4, mika ? ROOD : 0xFF7FDBFF, false);
        if (!wakker) {
            Component droom = Component.translatable("gui.guhs.among.hud.droomguh");
            g.text(mc.font, droom, x + w - 5 - mc.font.width(droom), y + 4, DOF, false);
        } else if (h.getBooleanOr("Lastig", false)) {
            Component lastig = Component.translatable("gui.guhs.among.niveau.lastig");
            g.text(mc.font, lastig, x + w - 5 - mc.font.width(lastig), y + 4, GOUD, false);
        }
        int klaar = h.getIntOr("Klaar", 0), totaal = Math.max(1, h.getIntOr("Totaal", 1));
        g.fill(x + 5, y + 16, x + w - 5, y + 23, 0xFF1A0E15);
        g.fill(x + 5, y + 16, x + 5 + (w - 10) * klaar / totaal, y + 23, GROEN);
        g.centeredText(mc.font, Component.translatable("gui.guhs.among.hud.balk", klaar, totaal), x + w / 2, y + 15, TEKST);
        int ry = y + 28;
        for (int i = 0; i < regels.length; i++) {
            g.text(mc.font, regels[i], x + 5, ry, af[i] ? GROEN : mika ? DOF : TEKST, false);
            ry += regel;
        }
        if (mika) {
            ry += 2;
            int duw = h.getIntOr("DuwAfkoel", 0) - verstreken, sab = h.getIntOr("SaboteerAfkoel", 0) - verstreken;
            g.text(mc.font, Component.translatable("gui.guhs.among.hud.duwen", duw > 0 ? seconden(duw) : Component.translatable("gui.guhs.among.hud.klaar")),
                    x + 5, ry, duw > 0 ? DOF : GOUD, false);
            ry += regel;
            g.text(mc.font, Component.translatable("gui.guhs.among.hud.saboteren",
                    sabotage != 0 ? Component.translatable("gui.guhs.among.hud.bezig") : sab > 0 ? seconden(sab) : Component.translatable("gui.guhs.among.hud.klaar")),
                    x + 5, ry, sabotage != 0 || sab > 0 ? DOF : GOUD, false);
            ry += regel;
        }
        if (sabotage != 0) {
            ry += 3;
            int over = h.getIntOr("SabotageOver", 0) - verstreken;
            Component s = switch (sabotage) {
                case 1 -> Component.translatable("gui.guhs.among.hud.licht");
                case 2 -> Component.translatable("gui.guhs.among.hud.alarm", seconden(over), Integer.bitCount(h.getIntOr("AlarmVast", 0)));
                default -> Component.translatable("gui.guhs.among.hud.deuren", Tekst.get(h, "DeurKamer"), seconden(over));
            };
            g.text(mc.font, s, x + 5, ry, (mc.level.getGameTime() / 10) % 2 == 0 ? ROOD : GOUD, false);
        }
    }

    private AmongClient() {
    }
}
