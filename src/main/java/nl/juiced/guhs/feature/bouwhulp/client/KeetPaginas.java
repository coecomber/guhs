package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetTabs;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.DONKER;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.GROEN;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.LICHT;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.ROOD;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.ROZE;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.TEKST;

/** The client pages of F8's tabs (see {@code keet.KeetTabs}): the "binnenkort" stand-ins, Bouwguhs, Materiaal, Niveau. */
final class KeetPaginas {
    static void register() {
        for (String id : new String[] {BouwkeetTabs.VORMEN, BouwkeetTabs.DAKEN, BouwkeetTabs.BLAUWDRUKKEN}) {
            BouwkeetScreen.tab(id, BINNENKORT);
        }
        BouwkeetScreen.tab(BouwkeetTabs.BOUWGUHS, BOUWGUHS);
        BouwkeetScreen.tab(BouwkeetTabs.MATERIAAL, MATERIAAL);
        BouwkeetScreen.tab(BouwkeetTabs.NIVEAU, NIVEAU);
    }

    /** Stand-in: what will come here; Vormen has the test hut. */
    static final BouwkeetScreen.TabPagina BINNENKORT = new BouwkeetScreen.TabPagina() {
        @Override
        public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
            if (tab.getBooleanOr("Proef", false)) {
                s.knop(Component.translatable("gui.guhs.bouwkeet.proefhuisje.knop"), x, y + h - 22, 160, () -> s.doe(s.openTab(), 1, 0, ""), s.magBewerken())
                        .setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwkeet.proefhuisje.uitleg")));
            }
        }

        @Override
        public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwkeet.binnenkort." + s.openTab()), x, y + 4, w, 0.875f, TEKST);
        }
    };

    // =====================================================================================================================

    static final BouwkeetScreen.TabPagina BOUWGUHS = new BouwkeetScreen.TabPagina() {
        @Override
        public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
            ListTag guhs = tab.getListOrEmpty("Guhs");
            boolean kan = s.magBewerken() && s.werkNiveau() >= BouwNiveau.GUHRAD.nummer();
            for (int i = 0; i < Math.min(8, guhs.size()); i++) {
                CompoundTag c = guhs.getCompoundOrEmpty(i);
                boolean gekozen = c.getBooleanOr("Gekozen", false);
                String id = c.read("Id", net.minecraft.core.UUIDUtil.CODEC).map(Object::toString).orElse("");
                int bx = x + (i % 2) * (w / 2), by = y + 22 + (i / 2) * 22;
                s.knop(Component.translatable(gekozen ? "gui.guhs.bouwguh.stop" : "gui.guhs.bouwguh.kies"), bx + w / 2 - 64, by, 60,
                        () -> s.doe(BouwkeetTabs.BOUWGUHS, 1, 0, id), kan);
            }
        }

        @Override
        public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
            if (s.werkNiveau() < BouwNiveau.GUHRAD.nummer()) {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwguh.vanaf"), x, y + 4, w, 0.875f, ROOD);
                return;
            }
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwguh.uitleg", tab.getIntOr("Max", 4), tab.getIntOr("Hier", 0)), x, y + 4, w, 0.75f, TEKST, false);
            ListTag guhs = tab.getListOrEmpty("Guhs");
            if (guhs.isEmpty()) {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwguh.geen").withStyle(ChatFormatting.ITALIC), x, y + 24, w, 0.875f, LICHT);
            }
            for (int i = 0; i < Math.min(8, guhs.size()); i++) {
                CompoundTag c = guhs.getCompoundOrEmpty(i);
                int bx = x + (i % 2) * (w / 2), by = y + 22 + (i / 2) * 22;
                boolean gekozen = c.getBooleanOr("Gekozen", false), weg = c.getBooleanOr("Weg", false);
                Component naam = Component.literal((gekozen ? "⛑ " : "") + c.getStringOr("Naam", "?"));
                if (weg) {
                    naam = naam.copy().append(" ").append(Component.translatable("gui.guhs.bouwguh.weg").withStyle(ChatFormatting.ITALIC));
                }
                GidsTekst.passend(g, naam, bx + 2, by + 5, w / 2 - 70, 0.75f, gekozen ? ROZE : DONKER, false);
            }
        }
    };

    // =====================================================================================================================

    static final BouwkeetScreen.TabPagina MATERIAAL = new BouwkeetScreen.TabPagina() {
        @Override
        public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
            if (tab.getBooleanOr("Bank", false)) {
                s.knop(Component.translatable("gui.guhs.bouwkeet.bank.los"), x + w - 90, y, 90, () -> s.doe(BouwkeetTabs.MATERIAAL, 3, 0, ""), s.magBewerken());
            }
            ListTag spoken = tab.getListOrEmpty("Spoken");
            int sy = y + h - Math.min(2, spoken.size()) * 20;
            for (int i = 0; i < Math.min(2, spoken.size()); i++) {
                CompoundTag c = spoken.getCompoundOrEmpty(i);
                String id = c.read("Id", net.minecraft.core.UUIDUtil.CODEC).map(Object::toString).orElse("");
                s.knop(Component.translatable("gui.guhs.bouwkeet.spoken.toon"), x + w - 124, sy + i * 20, 60, () -> s.doe(BouwkeetTabs.MATERIAAL, 1, 0, id), true);
                s.knop(Component.translatable("gui.guhs.bouwkeet.spoken.hier"), x + w - 62, sy + i * 20, 62, () -> s.doe(BouwkeetTabs.MATERIAAL, 2, 0, id),
                        s.magBewerken() && !c.getBooleanOr("Project", false) && !c.getBooleanOr("Elders", false));
            }
        }

        @Override
        public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
            Component bank = !tab.getBooleanOr("Bank", false) ? Component.translatable("gui.guhs.bouwkeet.bank.geen")
                    : tab.getBooleanOr("BankGevonden", false) ? Component.translatable("gui.guhs.bouwkeet.bank.ja")
                    : Component.translatable("gui.guhs.bouwkeet.bank.kwijt");
            GidsTekst.passend(g, bank, x, y + 4, w - 96, 0.75f, tab.getBooleanOr("BankGevonden", false) ? GROEN : LICHT, false);
            Component draadloos = tab.getBooleanOr("Draadloos", false) ? Component.translatable("gui.guhs.bouwkeet.draadloos.ja", tab.getIntOr("Kisten", 0))
                    : Component.translatable("gui.guhs.bouwkeet.draadloos.nee");
            GidsTekst.passend(g, draadloos, x, y + 14, w - 96, 0.75f, TEKST, false);
            ListTag nodig = tab.getListOrEmpty("Nodig");
            int ny = y + 28;
            if (tab.getBooleanOr("Gratis", false)) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.gratis"), x, ny, w, 0.75f, GROEN, false);
            } else if (!nodig.isEmpty()) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.nodig").withStyle(ChatFormatting.BOLD), x, ny, w, 0.75f, DONKER, false);
                for (int i = 0; i < Math.min(12, nodig.size()); i++) {
                    CompoundTag c = nodig.getCompoundOrEmpty(i);
                    var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(c.getStringOr("Id", "minecraft:air")));
                    int ix = x + (i % 6) * 52, iy = ny + 10 + (i / 6) * 20;
                    if (item.isPresent()) {
                        g.item(new ItemStack(item.get()), ix, iy);
                        long heb = c.getLongOr("Heb", 0L);
                        int n = c.getIntOr("N", 0);
                        GidsTekst.schaal(g, Component.literal(Math.min(heb, n) + "/" + n), ix + 18, iy + 5, 0.7f, heb >= n ? GROEN : ROOD, false);
                    }
                }
            }
            ListTag spoken = tab.getListOrEmpty("Spoken");
            int sy = y + h - Math.min(2, spoken.size()) * 20;
            if (!spoken.isEmpty()) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.spoken").withStyle(ChatFormatting.BOLD), x, sy - 10, w, 0.75f, DONKER, false);
            }
            for (int i = 0; i < Math.min(2, spoken.size()); i++) {
                CompoundTag c = spoken.getCompoundOrEmpty(i);
                String naam = c.getStringOr("Naam", "");
                Component regel = Component.translatable("gui.guhs.bouwkeet.spoken.regel", naam.isEmpty() ? Component.translatable("gui.guhs.bouwhulp.project")
                        : Component.literal(naam), c.getIntOr("Blokken", 0), c.getIntOr("Afstand", 0),
                        Component.translatable("gui.guhs.bouwhulp.spook.kort." + c.getIntOr("Status", 0)));
                GidsTekst.passend(g, regel, x, sy + i * 20 + 5, w - 130, 0.7f, c.getBooleanOr("Project", false) ? ROZE : TEKST, false);
            }
        }
    };

    // =====================================================================================================================

    static final BouwkeetScreen.TabPagina NIVEAU = new BouwkeetScreen.TabPagina() {
        @Override
        public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
            boolean aan = s.data().getBooleanOr("Aan", true);
            s.knop(Component.translatable(aan ? "gui.guhs.bouwkeet.zet_uit" : "gui.guhs.bouwkeet.zet_aan"), x + w - 90, y, 90,
                    () -> s.doe(BouwkeetTabs.NIVEAU, 1, aan ? 0 : 1, ""), s.magBewerken());
        }

        @Override
        public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
            int niveau = s.data().getIntOr("Niveau", 0);
            boolean kracht = s.data().getBooleanOr("Kracht", false);
            Component status = niveau == 0 ? Component.translatable("gui.guhs.bouwkeet.vk.geen")
                    : Component.translatable(kracht ? "gui.guhs.bouwkeet.vk.ja" : "gui.guhs.bouwkeet.vk.nee", tab.getIntOr("Vraag", 0));
            GidsTekst.passend(g, status, x, y + 5, w - 96, 0.75f, niveau == 0 || kracht ? GROEN : ROOD, false);
            for (BouwNiveau n : BouwNiveau.values()) {
                int ry = y + 18 + n.nummer() * 19;
                boolean heb = n.nummer() <= niveau;
                g.fill(x - 2, ry - 2, x + w, ry + 17, heb ? 0x30E07AAE : 0x10A0845E);
                GidsTekst.passend(g, Component.translatable(n.sleutel()).withStyle(ChatFormatting.BOLD), x + 2, ry, 110, 0.75f, heb ? DONKER : LICHT, false);
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.niveau.vk", n.vk), x + 2, ry + 9, 110, 0.6f, LICHT, false);
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.niveau.wat." + n.nummer()), x + 116, ry + 3, w - 120, 0.7f, heb ? TEKST : LICHT, false);
            }
            String upgrade = tab.getStringOr("Upgrade", "");
            if (!upgrade.isEmpty()) {
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(upgrade));
                int uy = y + 18 + 5 * 19 + 2;
                if (item.isPresent() && uy + 16 < y + h) {
                    ItemStack st = new ItemStack(item.get());
                    g.item(st, x, uy);
                    GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.volgende", st.getHoverName()), x + 20, uy + 4, w - 20, 0.75f, TEKST, false);
                }
            }
        }
    };

    private KeetPaginas() {
    }
}
