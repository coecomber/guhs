package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.client.screen.GuhDexScreen;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.gids.GidsData;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.Highscores;

/**
 * The Guhdex's Minigames tab (2.9): the eras (Klassiekers, Knuffeldal, De Grote Guhspelen), each with its buildings. A
 * building opens into: where to find it and whether you've been there, its clothing (x/y as icons), and per level /
 * track / event / song your best and the server record with its holder. Everything folds open and shut; what is open is
 * remembered while the game runs.
 */
public final class GidsMinigamesTab {
    /** What is open: "t:&lt;era&gt;" and "g:&lt;group&gt;". The eras start open, the buildings shut. */
    static final Set<String> OPEN = new HashSet<>(Set.of("t:klassiekers", "t:knuffeldal", "t:grote_guhspelen"));

    static final int DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, FEL = 0xFFD0306A, GRIJS = 0xFF9A8090, GOUD = 0xFFC08A10, GROEN = 0xFF2A8A50;

    /** The rows of the tab; herbouw: called after something folded open or shut. */
    public static List<GidsLijst.Regel> regels(Runnable herbouw) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(new Samenvatting());
        for (SpelGroepen.Tijdperk t : SpelGroepen.Tijdperk.values()) {
            List<SpelGroepen.Groep> groepen = SpelGroepen.van(t);
            if (groepen.isEmpty()) {
                continue;
            }
            String tk = "t:" + t.id();
            out.add(new TijdperkKop(t, groepen, OPEN.contains(tk), () -> {
                wissel(tk);
                herbouw.run();
            }));
            if (!OPEN.contains(tk)) {
                continue;
            }
            for (SpelGroepen.Groep groep : groepen) {
                String gk = "g:" + groep.id();
                boolean open = OPEN.contains(gk);
                out.add(new GroepKop(groep, open, () -> {
                    wissel(gk);
                    herbouw.run();
                }));
                if (open) {
                    out.add(new GroepInfo(groep));
                    List<GuhClothes> kleding = SpelGroepen.kleding(groep.id());
                    if (!kleding.isEmpty()) {
                        out.add(new KledingRij(kleding));
                    }
                    List<Highscores.Game> rijen = GidsData.rijen(groep);
                    if (rijen.isEmpty()) {
                        out.add(new Leeg());
                    } else {
                        out.add(new ScoreKop());
                        for (int i = 0; i < rijen.size(); i++) {
                            out.add(new ScoreRij(rijen.get(i), i % 2 == 0));
                        }
                    }
                    out.add(new Ruimte(5));
                }
            }
            out.add(new Ruimte(2));
        }
        return out;
    }

    static void wissel(String key) {
        if (!OPEN.remove(key)) {
            OPEN.add(key);
        }
        klik();
    }

    /** (AutoCheck) opens or shuts everything. */
    public static void alles(boolean open) {
        OPEN.clear();
        for (SpelGroepen.Tijdperk t : SpelGroepen.Tijdperk.values()) {
            OPEN.add("t:" + t.id());
        }
        if (open) {
            for (SpelGroepen.Groep g : SpelGroepen.alle()) {
                OPEN.add("g:" + g.id());
            }
        }
    }

    static void klik() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.3f));
    }

    static MaagPayloads.HighscoreRow rij(String game) {
        for (MaagPayloads.HighscoreRow r : GuhDexScreen.highscores) {
            if (r.game().equals(game)) {
                return r;
            }
        }
        return new MaagPayloads.HighscoreRow(game, false, "", "", "");
    }

    static String jij() {
        var p = Minecraft.getInstance().player;
        return p == null ? "" : p.getGameProfile().getName();
    }

    static Component label(String gameId) {
        String key = GidsData.rijLabel(gameId);
        return I18n.exists(key) ? Component.translatable(key) : Component.translatable("gui.guhs.highscores.game." + gameId);
    }

    // --- the rows -----------------------------------------------------------------------------------------------------

    /** On top: how many buildings you've found and how many records are yours. */
    static final class Samenvatting implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 24;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            int gebouwen = 0, bezocht = 0;
            for (SpelGroepen.Groep gr : SpelGroepen.alle()) {
                if (gr.structuur() != null) {
                    gebouwen++;
                    if (SpelGroepen.Client.bezocht(gr.id())) {
                        bezocht++;
                    }
                }
            }
            int records = 0;
            String jij = jij();
            for (MaagPayloads.HighscoreRow r : GuhDexScreen.highscores) {
                if (r.hasRecord() && r.holder().equals(jij)) {
                    records++;
                }
            }
            GidsTekst.passend(g, Component.translatable("gui.guhs.gids.minigames.bezocht", bezocht, gebouwen).withStyle(ChatFormatting.BOLD),
                    x + 2, y + 2, w / 2 - 4, 1f, DONKER, false);
            GidsTekst.balk(g, x + 3, y + 14, w / 2 - 10, 4, gebouwen == 0 ? 0 : bezocht / (float) gebouwen);
            Component rec = records == 0 ? Component.translatable("gui.guhs.gids.minigames.geen_records")
                    : Component.translatable("gui.guhs.gids.minigames.records", records);
            GidsTekst.passend(g, rec, x + w - 2, y + 4, w / 2 - 4, 0.75f, records == 0 ? GRIJS : GOUD, true);
            GidsTekst.passend(g, Component.translatable("gui.guhs.gids.minigames.klik"), x + w - 2, y + 13, w / 2 - 4, 0.6f, GRIJS, true);
        }
    }

    /** An era: a band with its name and how many of its buildings you've found; click: fold open / shut. */
    record TijdperkKop(SpelGroepen.Tijdperk tijdperk, List<SpelGroepen.Groep> groepen, boolean open, Runnable actie) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 16;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x, y + 1, x + w, y + 15, hover ? 0xFFF9CFDE : 0xFFF3B8CD);
            g.fill(x, y + 1, x + w, y + 2, 0x70FFFFFF);
            g.fill(x, y + 14, x + w, y + 15, 0xFFD27A9C);
            GidsTekst.schaal(g, Component.literal(open ? "▼" : "▶"), x + 4, y + 4, 0.75f, ROZE, false);
            GidsTekst.passend(g, tijdperk.naam().copy().withStyle(ChatFormatting.BOLD), x + 14, y + 4, w - 90, 1f, 0xFF5A1838, false);
            long met = groepen.stream().filter(gr -> gr.structuur() != null).count();
            long gevonden = groepen.stream().filter(gr -> gr.structuur() != null && SpelGroepen.Client.bezocht(gr.id())).count();
            GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.gevonden", gevonden, met), x + w - 4, y + 5, 0.75f,
                    gevonden == met ? GROEN : ROZE, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            actie.run();
            return true;
        }
    }

    /** A building: icon, name, its guh, clothing x/y and whether you've been there; click: fold open / shut. */
    record GroepKop(SpelGroepen.Groep groep, boolean open, Runnable actie) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 24;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y + 1, x + w, y + 23, hover ? 0x60F7B6CB : open ? 0x48F7B6CB : 0x26F7B6CB);
            g.fill(x + 2, y + 1, x + 4, y + 23, open ? FEL : 0xFFF7B6CB);
            GidsTekst.schaal(g, Component.literal(open ? "▼" : "▶"), x + 7, y + 9, 0.6f, ROZE, false);
            g.renderItem(groep.icoon().get(), x + 14, y + 4);
            boolean bezocht = SpelGroepen.Client.bezocht(groep.id());
            int rechts = 74;
            GidsTekst.passend(g, groep.naam().copy().withStyle(ChatFormatting.BOLD), x + 34, y + 4, w - 34 - rechts, 1f, DONKER, false);
            Component sub = groep.npc() != null
                    ? Component.translatable("gui.guhs.gids.minigames.bij", Component.translatable("entity.guhs.guh_npc." + groep.npc().name().toLowerCase(java.util.Locale.ROOT)))
                    : Component.translatable("gui.guhs.gids.minigames.zelf");
            GidsTekst.passend(g, sub, x + 34, y + 14, w - 34 - rechts, 0.6f, GRIJS, false);
            List<GuhClothes> kleding = SpelGroepen.kleding(groep.id());
            if (!kleding.isEmpty()) {
                long heb = kleding.stream().filter(GidsKledingIcoon::heeft).count();
                GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.kleding_telling", heb, kleding.size()), x + w - 4, y + 4, 0.6f,
                        heb == kleding.size() ? GROEN : ROZE, true);
            }
            if (groep.structuur() != null) {
                GidsTekst.schaal(g, Component.translatable(bezocht ? "gui.guhs.gids.minigames.kort_bezocht" : "gui.guhs.gids.minigames.kort_niet"),
                        x + w - 4, y + 14, 0.6f, bezocht ? GROEN : GRIJS, true);
            }
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            actie.run();
            return true;
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(groep.naam().copy().withStyle(ChatFormatting.BOLD), groep.waar().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** Where to find the building (biome, building, guh), and whether you've been there. */
    record GroepInfo(SpelGroepen.Groep groep) implements GidsLijst.Regel {
        static final float S = 0.75f;

        @Override
        public int hoogte() {
            int w = 250;
            return 6 + GidsTekst.hoogte(groep.waar(), w - 28, S) + (groep.structuur() != null ? 9 : 0);
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + hoogte(), 0x60F7B6CB);
            GidsTekst.schaal(g, Component.literal("⌂"), x + 10, y + 3, S, ROZE, false);
            int h = GidsTekst.alinea(g, groep.waar(), x + 20, y + 3, Math.min(w, 250) - 28, S, 0xFF5A3A4A);
            if (groep.structuur() != null) {
                boolean bezocht = SpelGroepen.Client.bezocht(groep.id());
                GidsTekst.schaal(g, Component.translatable(bezocht ? "gui.guhs.spelgroep.bezocht" : "gui.guhs.spelgroep.niet_bezocht"),
                        x + 20, y + 4 + h, S, bezocht ? GROEN : GRIJS, false);
            }
        }
    }

    /** The clothing of the building: squares, green = yours, grey = still locked; hover for name, price and source. */
    record KledingRij(List<GuhClothes> kleding) implements GidsLijst.Regel {
        static final int LABEL = 62, PER_RIJ = 9;

        @Override
        public int hoogte() {
            return 3 + ((kleding.size() + PER_RIJ - 1) / PER_RIJ) * GidsKledingIcoon.CEL;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + hoogte(), 0x60F7B6CB);
            long heb = kleding.stream().filter(GidsKledingIcoon::heeft).count();
            GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.kleding", heb, kleding.size()), x + 10, y + 7, 0.75f,
                    heb == kleding.size() ? GROEN : ROZE, false);
            for (int i = 0; i < kleding.size(); i++) {
                int cx = x + LABEL + (i % PER_RIJ) * GidsKledingIcoon.CEL, cy = y + 1 + (i / PER_RIJ) * GidsKledingIcoon.CEL;
                boolean on = mx >= cx && mx < cx + GidsKledingIcoon.CEL - 2 && my >= cy && my < cy + GidsKledingIcoon.CEL - 2;
                GidsKledingIcoon.teken(g, kleding.get(i), cx, cy, on);
            }
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            for (int i = 0; i < kleding.size(); i++) {
                int cx = x + LABEL + (i % PER_RIJ) * GidsKledingIcoon.CEL, cy = y + 1 + (i / PER_RIJ) * GidsKledingIcoon.CEL;
                if (mx >= cx && mx < cx + GidsKledingIcoon.CEL - 2 && my >= cy && my < cy + GidsKledingIcoon.CEL - 2) {
                    return GidsKledingIcoon.tip(kleding.get(i));
                }
            }
            return null;
        }
    }

    /** The column headings of the score table. */
    static final class ScoreKop implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 11;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + 11, 0x60F7B6CB);
            int[] c = kolommen(x, w);
            GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.kolom.spel"), c[0], y + 3, 0.6f, GRIJS, false);
            GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.kolom.jij"), c[1], y + 3, 0.6f, GRIJS, false);
            GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.kolom.record"), c[2], y + 3, 0.6f, GRIJS, false);
            g.fill(c[0], y + 10, x + w - 2, y + 11, 0x40D27A9C);
        }
    }

    /** x of the columns: label, your best, the record (with holder). */
    static int[] kolommen(int x, int w) {
        return new int[]{x + 10, x + 10 + (int) ((w - 12) * 0.47f), x + 10 + (int) ((w - 12) * 0.67f)};
    }

    /** One level / track / event / song: your best and the server record with its holder (gold when it's yours). */
    record ScoreRij(Highscores.Game game, boolean even) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 11;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + 11, 0x60F7B6CB);
            int[] c = kolommen(x, w);
            if (even || hover) {
                g.fill(c[0] - 3, y, x + w - 2, y + 11, hover ? 0x40F7B6CB : 0x18F7B6CB);
            }
            MaagPayloads.HighscoreRow row = rij(game.id());
            GidsTekst.passend(g, label(game.id()), c[0], y + 2, c[1] - c[0] - 7, 0.75f, DONKER, false);
            if (row.played()) {
                GidsTekst.passend(g, Component.literal(row.best()).withStyle(ChatFormatting.BOLD), c[1], y + 2, c[2] - c[1] - 4, 0.75f, FEL, false);
            } else {
                GidsTekst.schaal(g, Component.literal("-"), c[1], y + 2, 0.75f, GRIJS, false);
            }
            if (row.hasRecord()) {
                boolean mine = row.holder().equals(jij());
                Component rec = Component.literal(row.record()).withStyle(ChatFormatting.BOLD)
                        .append(Component.literal(" " + (mine ? "★ " : "") + row.holder()).withStyle(ChatFormatting.RESET));
                GidsTekst.passend(g, rec, c[2], y + 2, x + w - 4 - c[2], 0.75f, mine ? GOUD : ROZE, false);
            } else {
                GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.minigames.geen_record"), c[2], y + 2, 0.75f, GRIJS, false);
            }
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            MaagPayloads.HighscoreRow row = rij(game.id());
            List<Component> out = new ArrayList<>();
            out.add(Component.translatable("gui.guhs.highscores.game." + game.id()).withStyle(ChatFormatting.BOLD));
            out.add(Component.translatable(game.lowerIsBetter() ? "gui.guhs.highscores.lower" : "gui.guhs.highscores.higher")
                    .withStyle(ChatFormatting.GRAY));
            out.add(row.played() ? Component.translatable("gui.guhs.gids.minigames.tip.jij", row.best()).withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("gui.guhs.highscores.never").withStyle(ChatFormatting.DARK_GRAY));
            out.add(!row.hasRecord() ? Component.translatable("gui.guhs.highscores.no_record").withStyle(ChatFormatting.DARK_GRAY)
                    : row.holder().equals(jij()) ? Component.translatable("gui.guhs.highscores.record_you", row.record()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.guhs.highscores.record", row.record(), row.holder()).withStyle(ChatFormatting.YELLOW));
            return out;
        }
    }

    /** A building without scores (the kermis, the grijpmachine). */
    static final class Leeg implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 12;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + 12, 0x60F7B6CB);
            GidsTekst.passend(g, Component.translatable("gui.guhs.gids.minigames.geen_scores").withStyle(ChatFormatting.ITALIC), x + 10, y + 3,
                    w - 14, 0.75f, GRIJS, false);
        }
    }

    /** Empty room. */
    record Ruimte(int h) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return h;
        }

        @Override
        public void teken(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hover) {
        }
    }

    private GidsMinigamesTab() {
    }
}
