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
            // 1.10.10: the Banksleutel slot (click: your key goes in / the key comes back out)
            ItemStack sleutel = BuiltInRegistries.ITEM.getOptional(Identifier.parse(tab.getStringOr("Sleutel", "minecraft:air")))
                    .map(ItemStack::new).orElse(ItemStack.EMPTY);
            ItemKnop vak = s.voegToe(new ItemKnop(x, y, sleutel, () -> s.doe(BouwkeetTabs.MATERIAAL, 4, 0, "")));
            vak.active = s.magBewerken();
            vak.setTooltip(Tooltip.create(Component.translatable(sleutel.isEmpty() ? "gui.guhs.bouwkeet.sleutel.vak.leeg" : "gui.guhs.bouwkeet.sleutel.vak.vol")));
            if (tab.getBooleanOr("BankOud", false)) {
                s.knop(Component.translatable("gui.guhs.bouwkeet.bank.los"), x + w - 90, y, 90, () -> s.doe(BouwkeetTabs.MATERIAAL, 3, 0, ""), s.magBewerken());
            }
            // test round 5: what is still missing as a written book
            boolean mist = !s.data().getCompoundOrEmpty("Project").getListOrEmpty("Tekort").isEmpty();
            s.knop(Component.translatable("gui.guhs.bouwhulp.lijstje.knop"), x + w - 90, y + 22, 90,
                    () -> s.doe("", nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetPayloads.Actie.LIJSTJE.ordinal(), 0, ""), mist)
                    .setTooltip(Tooltip.create(Component.translatable(mist ? "gui.guhs.bouwhulp.lijstje.uitleg" : "gui.guhs.bouwhulp.lijstje.niets.uitleg")));
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
        public java.util.List<Component> tip(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h, int mx, int my) {
            return materiaalTip(tab, x, y, mx, my);
        }

        @Override
        public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
            GidsTekst.passend(g, bankTekst(tab), x + 22, y + 1, w - 118, 0.75f, tab.getBooleanOr("BankGevonden", false) ? GROEN : LICHT, false);
            Component draadloos = tab.getBooleanOr("Draadloos", false) ? Component.translatable("gui.guhs.bouwkeet.draadloos.ja", tab.getIntOr("Kisten", 0))
                    : Component.translatable("gui.guhs.bouwkeet.draadloos.nee");
            GidsTekst.passend(g, draadloos, x + 22, y + 10, w - 118, 0.75f, TEKST, false);
            long[] som = tab.getLongArray("Som").orElse(new long[0]);
            if (som.length == 3 && tab.getBooleanOr("Bank", false) && !tab.getBooleanOr("Gratis", false)) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.bank.deel", som[1], som[0], som[2]), x, y + 22, w - 96, 0.7f,
                        som[2] > 0 ? ROOD : GROEN, false);
            }
            ListTag nodig = tab.getListOrEmpty("Nodig");
            int ny = y + 32;
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
                        int n = c.getIntOr("N", 0), maak = c.getIntOr("Maak", 0);
                        // 1.10.10: amber = the keet crafts the rest
                        int kleur = heb >= n ? GROEN : c.getIntOr("Mist", 0) == 0 && maak > 0 ? 0xFFC8862A : ROOD;
                        GidsTekst.schaal(g, Component.literal(Math.min(heb, n) + "/" + n), ix + 18, iy + 1, 0.7f, kleur, false);
                        if (maak > 0) {
                            GidsTekst.schaal(g, Component.literal("+" + maak), ix + 18, iy + 9, 0.6f, 0xFFC8862A, false);
                        }
                    }
                }
            }
            ListTag spoken = tab.getListOrEmpty("Spoken");
            int sy = y + h - Math.min(2, spoken.size()) * 20;
            if (!nodig.isEmpty() && !tab.getBooleanOr("Gratis", false)) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.bouwkeet.nodig.hover").withStyle(ChatFormatting.ITALIC), x, ny + 10 + 2 * 20 + 2, w,
                        0.7f, LICHT, false);
            }
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

    /** 1.10.10: which Bank Guh the keet is linked to (the key in the slot, or the old click-link). */
    static Component bankTekst(CompoundTag tab) {
        if (!tab.getBooleanOr("Bank", false)) {
            return Component.translatable("gui.guhs.bouwkeet.bank.geen");
        }
        if (!tab.getBooleanOr("BankGevonden", false)) {
            return Component.translatable("gui.guhs.bouwkeet.bank.kwijt");
        }
        int[] p = tab.getIntArray("BankPlek").orElse(new int[3]);
        Component dim = nl.juiced.guhs.feature.band.GuhVolger.dimensie(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                Identifier.parse(tab.getStringOr("BankDim", "minecraft:overworld"))));
        return Component.translatable(tab.getBooleanOr("BankOud", false) ? "gui.guhs.bouwkeet.bank.oud" : "gui.guhs.bouwkeet.bank.bij", p[0], p[1], p[2], dim);
    }

    /** (Materiaal) the needed item under the mouse: its name, "12 / 30", the bank's share, what the keet crafts, what is missing. */
    static java.util.List<Component> materiaalTip(CompoundTag tab, int x, int y, int mx, int my) {
        ListTag nodig = tab.getListOrEmpty("Nodig");
        int ny = y + 32;
        for (int i = 0; i < Math.min(12, nodig.size()); i++) {
            int ix = x + (i % 6) * 52, iy = ny + 10 + (i / 6) * 20;
            if (mx >= ix && mx < ix + 50 && my >= iy && my < iy + 18) {
                CompoundTag c = nodig.getCompoundOrEmpty(i);
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(c.getStringOr("Id", "minecraft:air")));
                if (item.isPresent()) {
                    java.util.List<Component> uit = new java.util.ArrayList<>();
                    uit.add(new ItemStack(item.get()).getHoverName());
                    uit.add(Component.translatable("gui.guhs.bouwhulp.tip.heb", Math.min(c.getLongOr("Heb", 0L), c.getIntOr("N", 0)), c.getIntOr("N", 0))
                            .withStyle(ChatFormatting.GRAY));
                    if (tab.getBooleanOr("Bank", false)) {
                        uit.add(Component.translatable("gui.guhs.bouwkeet.tip.bank", c.getLongOr("Bank", 0L)).withStyle(ChatFormatting.GRAY));
                    }
                    if (c.getIntOr("Maak", 0) > 0) {
                        net.minecraft.network.chat.MutableComponent van = Component.empty();
                        ListTag l = c.getListOrEmpty("Van");
                        for (int j = 0; j < l.size(); j++) {
                            CompoundTag v = l.getCompoundOrEmpty(j);
                            var vi = BuiltInRegistries.ITEM.getOptional(Identifier.parse(v.getStringOr("Id", "minecraft:air")));
                            if (vi.isPresent()) {
                                van.append(j == 0 ? "" : ", ").append(v.getIntOr("N", 0) + "× ").append(new ItemStack(vi.get()).getHoverName());
                            }
                        }
                        uit.add((l.isEmpty() ? Component.translatable("gui.guhs.bouwkeet.tip.maak.rest", c.getIntOr("Maak", 0))
                                : Component.translatable("gui.guhs.bouwkeet.tip.maak", c.getIntOr("Maak", 0), van)).withStyle(ChatFormatting.GOLD));
                    }
                    if (c.getIntOr("Mist", 0) > 0) {
                        uit.add(Component.translatable("gui.guhs.bouwkeet.tip.mist", c.getIntOr("Mist", 0)).withStyle(ChatFormatting.RED));
                    }
                    if (item.get() == net.minecraft.world.item.Items.WATER_BUCKET) {
                        uit.add(Component.translatable("gui.guhs.bouwhulp.water.regel", c.getIntOr("N", 0)).withStyle(ChatFormatting.AQUA));
                    }
                    return uit;
                }
            }
        }
        return null;
    }

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
