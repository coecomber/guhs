package nl.juiced.guhs.feature.vadsrijk.npc.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * The questlog of Het Vadsrijk (J, only in the dimension), four tabs:
 * <ul>
 *   <li>Quests: your active quests (main story gold), click one for its text and objectives; how many are done;</li>
 *   <li>Geheimen: lost knuffels, the clues to the Mika-spion, treasures;</li>
 *   <li>Wezens: every kind of creature you cheered up, with a little page (the zone's bestiary);</li>
 *   <li>Gids: the Avonturiersgids (short how-to pages: classes, loot, campfires, elements...).</li>
 * </ul>
 * Everything comes from the server ({@code NpcLog}); texts are lang keys (tools/features/vadsrijk_npc.py).
 */
public class QuestlogScherm extends Screen {
    private static final int W = 340, H = 220, LIJST = 120, RIJ = 14, PER_PAGINA = 11;
    private static final int PANEL = 0xF0301A26, RAND = 0xFFF7B6CB, TEKST = 0xFFFFE6EE, ZACHT = 0xFFB8A0B0, GOUD = 0xFFFFC94A, GROEN = 0xFF7EE08A;
    /** The Avonturiersgids topics (lang vadsrijk.npc.gids.&lt;id&gt;.titel / .tekst). */
    static final String[] GIDS = {"begin", "klassen", "vaardigheden", "loot", "sterren", "kampvuren", "flauw", "elementen", "statussen", "party",
            "quests", "munten", "markt", "rijdieren", "geheimen",
            // zone 2 (tools/features/vadsrijk_z2_npc.py GIDS)
            "z2_splinterpieken", "z2_kristalwezens", "z2_bevroren", "z2_edelstenen", "z2_reizen", "z2_titel"};
    private static final String[] TABS = {"quests", "geheimen", "wezens", "gids"};

    private CompoundTag data;
    private int tab, keuze, pagina;
    private int left, top;

    public QuestlogScherm(CompoundTag data) {
        super(Component.translatable("vadsrijk.npc.log.titel"));
        this.data = data;
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        rebuildWidgets();
    }

    private List<CompoundTag> rijen() {
        List<CompoundTag> l = new ArrayList<>();
        String lijst = switch (tab) {
            case 0 -> "Actief";
            case 2 -> "Wezens";
            default -> "";
        };
        if (tab == 3) {
            for (String g : GIDS) {
                CompoundTag t = new CompoundTag();
                t.putString("Id", g);
                l.add(t);
            }
            return l;
        }
        if (lijst.isEmpty()) {
            return l;
        }
        for (Tag t : data.getListOrEmpty(lijst)) {
            if (t instanceof CompoundTag c) {
                l.add(c);
            }
        }
        return l;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 20) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Component naam = Component.translatable("vadsrijk.npc.log.tab." + TABS[i]);
            addRenderableWidget(Button.builder(i == tab ? naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE) : naam, b -> {
                tab = t;
                keuze = 0;
                pagina = 0;
                rebuildWidgets();
            }).bounds(left + 10 + i * bw, top + 6, bw - 4, 18).build());
        }
        List<CompoundTag> rijen = rijen();
        int van = pagina * PER_PAGINA;
        for (int i = van; i < Math.min(rijen.size(), van + PER_PAGINA); i++) {
            int r = i;
            addRenderableWidget(Button.builder(rijNaam(rijen.get(i), i == keuze), b -> {
                keuze = r;
                rebuildWidgets();
            }).bounds(left + 10, top + 30 + (i - van) * RIJ, LIJST, RIJ - 1).build());
        }
        if (rijen.size() > PER_PAGINA) {
            if (pagina > 0) {
                addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
                    pagina--;
                    rebuildWidgets();
                }).bounds(left + 10, top + H - 24, 20, 16).build());
            }
            if ((pagina + 1) * PER_PAGINA < rijen.size()) {
                addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
                    pagina++;
                    rebuildWidgets();
                }).bounds(left + 10 + LIJST - 20, top + H - 24, 20, 16).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose()).bounds(left + W - 70, top + H - 24, 60, 16).build());
    }

    private Component rijNaam(CompoundTag r, boolean gekozen) {
        MutableComponent c = switch (tab) {
            case 0 -> {
                boolean hoofd = r.getStringOr("Soort", "").equals("HOOFD");
                MutableComponent m = Component.literal(r.getBooleanOr("Klaar", false) ? "✔ " : hoofd ? "★ " : "• ")
                        .append(Component.translatable("vadsrijk.npc.q." + r.getStringOr("Id", "") + ".titel"));
                yield m.withStyle(hoofd ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
            }
            case 2 -> Component.literal(r.getIntOr("N", 0) + "× ").append(wezenNaam(r.getStringOr("T", "")));
            default -> Component.translatable("vadsrijk.npc.gids." + r.getStringOr("Id", "") + ".titel");
        };
        if (gekozen) {
            c = Component.literal("» ").append(c);
        }
        String s = c.getString();
        return font.width(s) > LIJST - 8 ? Component.literal(font.plainSubstrByWidth(s, LIJST - 14) + "…").withStyle(c.getStyle()) : c;
    }

    private static Component wezenNaam(String type) {
        int d = type.indexOf(':');
        String key = "entity." + (d < 0 ? "minecraft" : type.substring(0, d)) + "." + (d < 0 ? type : type.substring(d + 1));
        return Component.translatable(key);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEL);
        int x = left + 10 + LIJST + 10, y = top + 32, w = W - (x - left) - 10;
        g.fill(x - 5, top + 28, x - 4, top + H - 28, 0x60F7B6CB);
        switch (tab) {
            case 0 -> quests(g, x, y, w);
            case 1 -> geheimen(g, left + 12, y, W - 24);
            case 2 -> wezen(g, x, y, w);
            default -> gids(g, x, y, w);
        }
    }

    private int alinea(GuiGraphicsExtractor g, Component c, int x, int y, int w, int kleur, int maxY) {
        for (FormattedCharSequence l : font.split(c, w)) {
            if (y > maxY) {
                break;
            }
            g.text(font, l, x, y, kleur, false);
            y += 10;
        }
        return y;
    }

    private void quests(GuiGraphicsExtractor g, int x, int y, int w) {
        List<CompoundTag> rijen = rijen();
        int af = data.getListOrEmpty("Af").size();
        g.text(font, Component.translatable("vadsrijk.npc.log.af", af), left + 12, top + H - 38, ZACHT, false);
        if (rijen.isEmpty()) {
            alinea(g, Component.translatable("vadsrijk.npc.log.leeg"), x, y, w, ZACHT, top + H - 30);
            return;
        }
        CompoundTag q = rijen.get(Math.min(keuze, rijen.size() - 1));
        String id = q.getStringOr("Id", "");
        boolean hoofd = q.getStringOr("Soort", "").equals("HOOFD");
        y = alinea(g, Component.translatable("vadsrijk.npc.q." + id + ".titel").withStyle(ChatFormatting.BOLD), x, y, w, hoofd ? GOUD : TEKST, top + H);
        g.text(font, Component.translatable("vadsrijk.npc.log.soort." + q.getStringOr("Soort", "ZIJ").toLowerCase(java.util.Locale.ROOT)), x, y, ZACHT, false);
        y += 13;
        y = alinea(g, Component.translatable("vadsrijk.npc.q." + id + ".tekst"), x, y, w, TEKST, top + H - 80) + 4;
        for (Tag t : q.getListOrEmpty("Doelen")) {
            CompoundTag d = (CompoundTag) t;
            int v = d.getIntOr("V", 0), n = d.getIntOr("N", 1);
            boolean klaar = v >= n;
            Component regel = Component.literal(klaar ? "✔ " : "□ ").append(Component.translatable(d.getStringOr("K", "")))
                    .append(n > 1 ? Component.literal("  " + v + "/" + n) : Component.empty());
            y = alinea(g, regel, x, y, w, klaar ? GROEN : TEKST, top + H - 40);
        }
        String bij = q.getStringOr("Bij", "");
        if (q.getBooleanOr("Klaar", false) && !bij.isEmpty()) {
            alinea(g, Component.translatable("vadsrijk.npc.log.inleveren", Component.translatable(bij)), x, top + H - 38, w, GOUD, top + H);
        }
    }

    private void geheimen(GuiGraphicsExtractor g, int x, int y, int w) {
        y = alinea(g, Component.translatable("vadsrijk.npc.log.knuffels", data.getIntOr("Knuffels", 0), data.getIntOr("KnuffelsMax", 10))
                .withStyle(ChatFormatting.BOLD), x, y, w, 0xFFFF9AD5, top + H) + 2;
        y = alinea(g, Component.translatable("vadsrijk.npc.log.knuffels.uitleg"), x, y, w, ZACHT, top + H) + 8;
        var aanw = data.getListOrEmpty("Aanwijzingen");
        y = alinea(g, Component.translatable("vadsrijk.npc.log.aanwijzingen", aanw.size(), data.getIntOr("AanwijzingenMax", 5))
                .withStyle(ChatFormatting.BOLD), x, y, w, 0xFFC9A0FF, top + H) + 2;
        for (Tag t : aanw) {
            y = alinea(g, Component.literal("• ").append(Component.translatable("vadsrijk.npc.aanwijzing.titel." + t.asString().orElse(""))), x + 6, y, w - 6, TEKST,
                    top + H);
        }
        if (aanw.isEmpty()) {
            y = alinea(g, Component.translatable("vadsrijk.npc.log.aanwijzingen.uitleg"), x, y, w, ZACHT, top + H);
        }
        y += 8;
        var schat = data.getListOrEmpty("Schatten");
        y = alinea(g, Component.translatable("vadsrijk.npc.log.schatten", schat.size(), data.getIntOr("SchattenMax", 4)).withStyle(ChatFormatting.BOLD), x, y, w,
                GOUD, top + H) + 2;
        for (Tag t : schat) {
            y = alinea(g, Component.literal("• ").append(Component.translatable("vadsrijk.npc.schat.naam." + t.asString().orElse(""))), x + 6, y, w - 6, TEKST,
                    top + H);
        }
        if (data.getBooleanOr("Zone1", false)) {
            alinea(g, Component.translatable("vadsrijk.npc.log.zone1"), x, top + H - 40, w, GOUD, top + H);
        }
    }

    private void wezen(GuiGraphicsExtractor g, int x, int y, int w) {
        List<CompoundTag> rijen = rijen();
        if (rijen.isEmpty()) {
            alinea(g, Component.translatable("vadsrijk.npc.log.wezens.leeg"), x, y, w, ZACHT, top + H - 30);
            return;
        }
        CompoundTag r = rijen.get(Math.min(keuze, rijen.size() - 1));
        String type = r.getStringOr("T", "");
        y = alinea(g, wezenNaam(type).copy().withStyle(ChatFormatting.BOLD), x, y, w, 0xFFFF9AD5, top + H) + 2;
        y = alinea(g, Component.translatable("vadsrijk.npc.log.wezens.teller", r.getIntOr("N", 0)), x, y, w, ZACHT, top + H) + 6;
        String pad = type.substring(type.indexOf(':') + 1);
        String key = "vadsrijk.npc.wezen." + pad;
        alinea(g, I18n.exists(key) ? Component.translatable(key) : Component.translatable("vadsrijk.npc.wezen.onbekend"), x, y, w, TEKST, top + H - 30);
    }

    private void gids(GuiGraphicsExtractor g, int x, int y, int w) {
        String id = GIDS[Math.min(keuze, GIDS.length - 1)];
        y = alinea(g, Component.translatable("vadsrijk.npc.gids." + id + ".titel").withStyle(ChatFormatting.BOLD), x, y, w, 0xFFFF9AD5, top + H) + 4;
        alinea(g, Component.translatable("vadsrijk.npc.gids." + id + ".tekst"), x, y, w, TEKST, top + H - 30);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
