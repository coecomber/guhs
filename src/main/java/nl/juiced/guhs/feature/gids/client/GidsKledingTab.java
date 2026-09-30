package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.gids.GidsData;

/**
 * The Guhdex's Kleding tab (2.9): every clothing piece, grouped per source (minigames, Knuffeldal, shops, places, treasure
 * chests, jobs...), each source folding open to a grid of squares: colourful with a green border = unlocked, grey = still
 * locked (hover: name, source, price). The kapper's hairstyles come last, "bij de kapper" (they are no unlocks).
 */
public final class GidsKledingTab {
    /** The open sources ("b:&lt;source&gt;"); remembered while the game runs. */
    static final Set<String> OPEN = new HashSet<>();
    static final String KAPSELS = "kapsels";

    static final int DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, GRIJS = 0xFF9A8090, GROEN = 0xFF2A8A50;

    public static List<GidsLijst.Regel> regels(Runnable herbouw) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        List<GidsData.KledingGroep> groepen = GidsData.kledingGroepen();
        out.add(new Samenvatting());
        GidsData.Soort soort = null;
        for (GidsData.KledingGroep groep : groepen) {
            if (groep.soort() != soort) {
                soort = groep.soort();
                out.add(new SoortKop(soort.naam()));
            }
            String key = "b:" + groep.bron();
            boolean open = OPEN.contains(key);
            out.add(new BronKop(groep.naam(), groep.stukken(), open, false, () -> {
                wissel(key);
                herbouw.run();
            }));
            if (open) {
                out.add(new Rooster(groep.stukken()));
            }
        }
        List<GuhClothes> kapsels = GidsData.kapsels();
        if (!kapsels.isEmpty()) {
            out.add(new SoortKop(Component.translatable("gui.guhs.gids.soort.kapper")));
            String key = "b:" + KAPSELS;
            boolean open = OPEN.contains(key);
            out.add(new BronKop(Component.translatable("gui.guhs.gids.kapsels"), kapsels, open, true, () -> {
                wissel(key);
                herbouw.run();
            }));
            if (open) {
                out.add(new Rooster(kapsels));
            }
        }
        out.add(new GidsMinigamesTab.Ruimte(4));
        return out;
    }

    static void wissel(String key) {
        if (!OPEN.remove(key)) {
            OPEN.add(key);
        }
        GidsMinigamesTab.klik();
    }

    /** (AutoCheck) opens or shuts every source. */
    public static void alles(boolean open) {
        OPEN.clear();
        if (open) {
            for (GidsData.KledingGroep g : GidsData.kledingGroepen()) {
                OPEN.add("b:" + g.bron());
            }
            OPEN.add("b:" + KAPSELS);
        }
    }

    /** On top: how many pieces you've unlocked, and how unlocking works. */
    static final class Samenvatting implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 16 + GidsTekst.hoogte(Component.translatable("gui.guhs.gids.kleding.uitleg"), 272, 0.6f);
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            List<GuhClothes> alle = GidsData.ontgrendelbare();
            long heb = alle.stream().filter(GidsKledingIcoon::heeft).count();
            GidsTekst.passend(g, Component.translatable("gui.guhs.gids.kleding.telling", heb, alle.size()).withStyle(ChatFormatting.BOLD), x + 2, y + 2,
                    w / 2, 1f, DONKER, false);
            GidsTekst.balk(g, x + w / 2 + 6, y + 3, w / 2 - 12, 5, alle.isEmpty() ? 0 : heb / (float) alle.size());
            GidsTekst.alinea(g, Component.translatable("gui.guhs.gids.kleding.uitleg"), x + 2, y + 13, w - 4, 0.6f, GRIJS);
        }
    }

    /** A kind of source: a small heading. */
    record SoortKop(Component naam) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 14;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            int tw = GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 4, w - 20, 0.75f, 0xFF5A1838, false);
            g.fill(x + tw + 7, y + 7, x + w - 2, y + 8, 0x80D27A9C);
        }
    }

    /** A source: icon, name, x/y and a bar (hairstyles: "bij de kapper"); click: fold open / shut. */
    record BronKop(Component naam, List<GuhClothes> stukken, boolean open, boolean kapper, Runnable actie) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 21;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            long heb = kapper ? stukken.size() : stukken.stream().filter(GidsKledingIcoon::heeft).count();
            boolean vol = heb == stukken.size();
            g.fill(x + 2, y + 1, x + w, y + 20, hover ? 0x60F7B6CB : open ? 0x48F7B6CB : 0x26F7B6CB);
            g.fill(x + 2, y + 1, x + 4, y + 20, kapper ? 0xFFF3A6C4 : vol ? 0xFF4CC06A : 0xFFF7B6CB);
            GidsTekst.schaal(g, Component.literal(open ? "▼" : "▶"), x + 7, y + 8, 0.6f, ROZE, false);
            ItemStack icoon = ItemStack.EMPTY;
            for (GuhClothes c : stukken) {                   // the first piece you have, else simply the first
                if (kapper || GidsKledingIcoon.heeft(c)) {
                    icoon = GidsKledingIcoon.item(c);
                    break;
                }
            }
            if (icoon.isEmpty() && !stukken.isEmpty()) {
                icoon = GidsKledingIcoon.item(stukken.get(0));
            }
            g.item(icoon, x + 14, y + 3);
            int rechts = 86;
            GidsTekst.passend(g, naam.copy().withStyle(ChatFormatting.BOLD), x + 34, y + 7, w - 34 - rechts, 1f, DONKER, false);
            if (kapper) {
                GidsTekst.schaal(g, Component.translatable("gui.guhs.gids.kleding.bij_de_kapper").withStyle(ChatFormatting.ITALIC), x + w - 4, y + 8,
                        0.75f, 0xFFB0507A, true);
            } else {
                GidsTekst.schaal(g, Component.literal(heb + "/" + stukken.size()), x + w - 4, y + 5, 0.75f, vol ? GROEN : ROZE, true);
                GidsTekst.balk(g, x + w - 62, y + 13, 58, 3, stukken.isEmpty() ? 0 : heb / (float) stukken.size());
            }
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            actie.run();
            return true;
        }
    }

    /** The squares of one source. */
    record Rooster(List<GuhClothes> stukken) implements GidsLijst.Regel {
        static final int LINKS = 12;

        static int perRij(int w) {
            return Math.max(1, (w - LINKS - 2) / GidsKledingIcoon.CEL);
        }

        @Override
        public int hoogte() {
            int per = perRij(260);
            return 4 + ((stukken.size() + per - 1) / per) * GidsKledingIcoon.CEL;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            g.fill(x + 2, y, x + 4, y + hoogte() - 2, 0x60F7B6CB);
            int per = perRij(260);
            for (int i = 0; i < stukken.size(); i++) {
                int cx = x + LINKS + (i % per) * GidsKledingIcoon.CEL, cy = y + 2 + (i / per) * GidsKledingIcoon.CEL;
                boolean on = mx >= cx && mx < cx + GidsKledingIcoon.CEL - 2 && my >= cy && my < cy + GidsKledingIcoon.CEL - 2;
                GidsKledingIcoon.teken(g, stukken.get(i), cx, cy, on);
            }
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            int per = perRij(260);
            for (int i = 0; i < stukken.size(); i++) {
                int cx = x + LINKS + (i % per) * GidsKledingIcoon.CEL, cy = y + 2 + (i / per) * GidsKledingIcoon.CEL;
                if (mx >= cx && mx < cx + GidsKledingIcoon.CEL - 2 && my >= cy && my < cy + GidsKledingIcoon.CEL - 2) {
                    return GidsKledingIcoon.tip(stukken.get(i));
                }
            }
            return null;
        }
    }

    private GidsKledingTab() {
    }
}
