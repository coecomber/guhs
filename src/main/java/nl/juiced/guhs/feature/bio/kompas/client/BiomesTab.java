package nl.juiced.guhs.feature.bio.kompas.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bio.BiomeLijst;
import nl.juiced.guhs.feature.bio.kompas.BiomeIconen;
import nl.juiced.guhs.feature.bio.kompas.BiomeKompas;
import nl.juiced.guhs.feature.bio.kompas.BiomesMenu;
import nl.juiced.guhs.feature.bio.kompas.KompasSlice;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTabs;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3: the tab "Biomes" of the Superkompas menu ({@code SuperkompasScreen} calls in here from a few hook lines). It is
 * the last icon tab, after the category tabs. Its list: one heading per section of {@link BiomeLijst}; a locked one
 * shows a padlock and tells on hover how to get there, an unlocked one folds open and closed with a click and shows its
 * biomes in two columns (icon, name, a green tick when you have been there). Click a biome: the compass looks for it.
 * <p>
 * Also here: the width of the icon tabs. Eleven tabs of the normal width fit the menu; from the twelfth on all tabs get
 * narrower ({@link #tabW}, {@link #tabGap}), down to 18 pixels for fifteen.
 */
public final class BiomesTab {
    private static final int KNOP_H = 20, SECTIE_H = 21;
    private static final int GOUD = 0xFFF7D27A, LICHT = 0xFFFFE6EE, ZACHT = 0xFFB8A0B0, GRIJS = 0xFF8A7482;
    /** The room the row of tabs may take inside the menu (the menu is 300 wide). */
    private static final int TAB_RUIMTE = 292;
    /** The unlocked sections that are folded open (remembered while the game runs). */
    private static final Set<String> OPEN = new HashSet<>();

    // --- the row of tabs --------------------------------------------------------------------------------------------------

    /** The index of the tab Biomes: right after the category tabs. */
    public static int index() {
        return SuperkompasItem.CATEGORIES.size();
    }

    public static boolean is(int tab) {
        return tab == index();
    }

    /** Every tab of the menu, in order: the categories, then Biomes. */
    public static List<SuperkompasItem.Category> tabs() {
        List<SuperkompasItem.Category> uit = new ArrayList<>(SuperkompasItem.CATEGORIES);
        uit.add(BiomesMenu.CATEGORIE);
        return uit;
    }

    /** The gap between two tabs for this many tabs. */
    public static int tabGap(int aantal) {
        return GidsTabs.breedte(aantal, 20, GidsTabs.GAP) <= TAB_RUIMTE ? GidsTabs.GAP : 1;
    }

    /** The width of a tab for this many tabs: the normal 24 while they fit, else as wide as fits (an icon needs 18). */
    public static int tabW(int aantal) {
        int gap = tabGap(aantal);
        return Math.max(18, Math.min(GidsTabs.W, (TAB_RUIMTE - Math.max(0, aantal - 1) * gap) / Math.max(1, aantal)));
    }

    /** Where the row of tabs starts in a menu at {@code left} that is {@code w} wide. */
    public static int tabsX(int left, int w) {
        int n = index() + 1;
        return left + (w - GidsTabs.breedte(n, tabW(n), tabGap(n))) / 2;
    }

    /**
     * The tab the menu opens on: Biomes when the compass looks for a biome, or when it was the tab you had last and the
     * compass looks for nothing; else what the menu chose itself.
     */
    public static int start(InteractionHand hand, @Nullable String chosen, int tab, int laatste) {
        String biome = gekozen(hand);
        if (biome != null) {
            BiomeLijst.Sectie s = BiomesMenu.sectieVan(biome);
            if (s != null) {
                OPEN.add(s.id());
            }
            return index();
        }
        return chosen == null && laatste == index() ? index() : tab;
    }

    @Nullable
    private static String gekozen(InteractionHand hand) {
        Player p = Minecraft.getInstance().player;
        return p == null ? null : BiomeKompas.gekozen(p.getItemInHand(hand));
    }

    // --- the list ---------------------------------------------------------------------------------------------------------

    /**
     * The rows of the tab when the menu shows it; {@code lijst} is the list they go in (a click on a heading refills it),
     * {@code sluit} closes the menu. When no unlocked section is folded open, one opens by itself: the one of the
     * dimension you are in, else the first that is not locked.
     */
    public static List<GidsLijst.Regel> regels(InteractionHand hand, GidsLijst lijst, Runnable sluit) {
        Player p = Minecraft.getInstance().player;
        if (p != null && BiomeLijst.secties().stream().noneMatch(s -> OPEN.contains(s.id()) && !BiomesMenu.opSlot(p, s))) {
            BiomeLijst.Sectie hier = BiomeLijst.van(p.level().dimension());
            if (hier != null && !BiomesMenu.opSlot(p, hier)) {
                OPEN.add(hier.id());
            } else {
                BiomeLijst.secties().stream().filter(s -> !BiomesMenu.opSlot(p, s)).findFirst().ifPresent(s -> OPEN.add(s.id()));
            }
        }
        return bouw(hand, lijst, sluit);
    }

    private static List<GidsLijst.Regel> bouw(InteractionHand hand, GidsLijst lijst, Runnable sluit) {
        List<GidsLijst.Regel> uit = new ArrayList<>();
        Player p = Minecraft.getInstance().player;
        if (p == null) {
            return uit;
        }
        String gekozen = gekozen(hand);
        String nu = BiomeLijst.van(p.level().dimension()) == null ? null : BiomeLijst.van(p.level().dimension()).id();
        List<BiomesMenu.BiomeRij> wacht = new ArrayList<>();
        for (BiomesMenu.Rij r : BiomesMenu.rijen(p, OPEN::contains)) {
            if (r instanceof BiomesMenu.SectieRij s) {
                paren(uit, wacht, hand, gekozen, nu, sluit);
                uit.add(new Sectie(s, () -> {
                    if (!OPEN.remove(s.id())) {
                        OPEN.add(s.id());
                    }
                    lijst.zet(bouw(hand, lijst, sluit));
                }));
            } else if (r instanceof BiomesMenu.BiomeRij b) {
                wacht.add(b);
            }
        }
        paren(uit, wacht, hand, gekozen, nu, sluit);
        return uit;
    }

    private static void paren(List<GidsLijst.Regel> uit, List<BiomesMenu.BiomeRij> wacht, InteractionHand hand, @Nullable String gekozen,
            @Nullable String nu, Runnable sluit) {
        for (int i = 0; i < wacht.size(); i += 2) {
            uit.add(new Paar(wacht.get(i), i + 1 < wacht.size() ? wacht.get(i + 1) : null, hand, gekozen, nu, sluit));
        }
        wacht.clear();
    }

    private static void geluid(net.minecraft.sounds.SoundEvent geluid, float toon) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(geluid, toon));
    }

    /** A long text as tooltip lines of at most 190 pixels. */
    private static void regelsIn(List<Component> uit, Component tekst, ChatFormatting kleur) {
        for (var deel : Minecraft.getInstance().font.getSplitter().splitLines(tekst, 190, Style.EMPTY)) {
            uit.add(Component.literal(deel.getString()).withStyle(kleur));
        }
    }

    /** A little padlock, 7 x 9 pixels. */
    private static void hangslot(GuiGraphicsExtractor g, int x, int y, int beugel, int slot) {
        g.fill(x + 2, y, x + 5, y + 1, beugel);
        g.fill(x + 1, y + 1, x + 2, y + 4, beugel);
        g.fill(x + 5, y + 1, x + 6, y + 4, beugel);
        g.fill(x, y + 4, x + 7, y + 9, slot);
        g.fill(x, y + 4, x + 7, y + 5, 0x40FFFFFF);
        g.fill(x + 3, y + 5, x + 4, y + 7, 0xFF2A1620);
    }

    /** A section's heading: a bar over the whole width. */
    private record Sectie(BiomesMenu.SectieRij rij, Runnable wissel) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return SECTIE_H;
        }

        private boolean op(double my, int y) {
            return my >= y + 2 && my < y + SECTIE_H - 2;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            boolean on = hover && op(my, y);
            int boven = y + 2, onder = y + SECTIE_H - 2;
            Component naam = BiomeKompas.sectieNaam(rij.id());
            if (rij.opSlot()) {
                g.fill(x, boven, x + w, onder, on ? 0xFF8A7482 : 0xFF5A4652);
                g.fill(x + 1, boven + 1, x + w - 1, onder - 1, on ? 0xFF3A2632 : 0xFF2C1C26);
                hangslot(g, x + 6, boven + 4, 0xFFB8A0B0, 0xFFD8B25A);
                GidsTekst.passend(g, naam, x + 19, boven + 5, w - 26 - 12, 1f, on ? ZACHT : GRIJS, false);
                hangslot(g, x + w - 13, boven + 4, 0xFF6A5662, 0xFF6A5662);
                return;
            }
            g.fill(x, boven, x + w, onder, on ? GOUD : 0xFFB89A5A);
            g.fill(x + 1, boven + 1, x + w - 1, onder - 1, on ? 0xFF6A3E54 : 0xFF53303F);
            g.fill(x + 1, boven + 1, x + w - 1, boven + 2, 0x20FFFFFF);
            Component telling = Component.literal(rij.bezocht() + "/" + rij.totaal() + " ✔");
            int tw = Math.round(Minecraft.getInstance().font.width(telling) * 0.875f);
            GidsTekst.schaal(g, telling, x + w - 6, boven + 5, 0.875f, rij.bezocht() == rij.totaal() && rij.totaal() > 0 ? 0xFF68D88A : ZACHT, true);
            GidsTekst.passend(g, Component.literal(rij.open() ? "▼ " : "▶ ").append(naam).withStyle(ChatFormatting.BOLD), x + 6, boven + 5,
                    w - 18 - tw, 1f, GOUD, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (!op(my, y)) {
                return false;
            }
            if (rij.opSlot()) {
                geluid(SoundEvents.CHEST_LOCKED, 1.3f);
            } else {
                geluid(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f);
                wissel.run();
            }
            return true;
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            if (!op(my, y)) {
                return null;
            }
            List<Component> uit = new ArrayList<>();
            uit.add(BiomeKompas.sectieNaam(rij.id()).copy().withStyle(ChatFormatting.BOLD));
            if (rij.opSlot()) {
                uit.add(Component.translatable("gui.guhs.biokompas.op_slot").withStyle(ChatFormatting.GOLD));
                regelsIn(uit, Component.translatable("gui.guhs.superkompas.sectie." + rij.id() + ".slot"), ChatFormatting.GRAY);
            } else {
                uit.add(Component.translatable("gui.guhs.biokompas.telling", rij.bezocht(), rij.totaal()).withStyle(ChatFormatting.GREEN));
                uit.add(Component.translatable(rij.open() ? "gui.guhs.biokompas.dicht" : "gui.guhs.biokompas.open").withStyle(ChatFormatting.GRAY));
            }
            return uit;
        }
    }

    /** Two biomes side by side (the right one may be missing). */
    private record Paar(BiomesMenu.BiomeRij links, @Nullable BiomesMenu.BiomeRij rechts, InteractionHand hand, @Nullable String gekozen,
                        @Nullable String nu, Runnable sluit) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return KNOP_H + 3;
        }

        private static int bw(int w) {
            return (w - 4) / 2;
        }

        @Nullable
        private BiomesMenu.BiomeRij onder(double mx, double my, int x, int y, int w) {
            if (my < y + 1 || my >= y + 1 + KNOP_H) {
                return null;
            }
            if (mx >= x && mx < x + bw(w)) {
                return links;
            }
            if (rechts != null && mx >= x + bw(w) + 4 && mx < x + w) {
                return rechts;
            }
            return null;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            BiomesMenu.BiomeRij op = hover ? onder(mx, my, x, y, w) : null;
            knop(g, links, x, y + 1, bw(w), op == links);
            if (rechts != null) {
                knop(g, rechts, x + bw(w) + 4, y + 1, bw(w), op == rechts);
            }
        }

        private void knop(GuiGraphicsExtractor g, BiomesMenu.BiomeRij b, int x, int y, int w, boolean on) {
            boolean kies = b.biome().equals(gekozen);
            g.fill(x, y, x + w, y + KNOP_H, kies ? GOUD : on ? 0xFFE8B8CC : 0xFF6A4A5A);
            g.fill(x + 1, y + 1, x + w - 1, y + KNOP_H - 1, on ? 0xFF6A3E54 : kies ? 0xFF5A3A2A : 0xFF462838);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x20FFFFFF);
            ItemStack icoon = BiomeIconen.icoon(b.biome());
            g.pose().pushMatrix();
            g.pose().translate(x + 3, y + 3);
            g.pose().scale(0.875f, 0.875f);
            g.item(icoon, 0, 0);
            g.pose().popMatrix();
            int tx = x + 20, rechtsRuimte = 4;
            if (b.bezocht()) {
                GidsTekst.schaal(g, Component.literal("✔"), x + w - 4, y + 6, 1f, 0xFF68D88A, true);
                rechtsRuimte = 14;
            }
            Component label = BiomeKompas.biomeNaam(b.biome());
            if (kies) {
                label = Component.literal("▶ ").append(label);
            }
            GidsTekst.passend(g, label, tx, y + 6, x + w - rechtsRuimte - tx, 1f, kies ? GOUD : LICHT, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            BiomesMenu.BiomeRij b = onder(mx, my, x, y, w);
            if (b == null) {
                return false;
            }
            ClientPacketDistributor.sendToServer(new KompasSlice.BiomeKeuze(hand == InteractionHand.MAIN_HAND, b.biome()));
            geluid(SoundEvents.LODESTONE_COMPASS_LOCK, 1.2f);
            sluit.run();
            return true;
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            BiomesMenu.BiomeRij b = onder(mx, my, x, y, w);
            if (b == null) {
                return null;
            }
            List<Component> uit = new ArrayList<>();
            uit.add(BiomeKompas.biomeNaam(b.biome()).copy().withStyle(ChatFormatting.BOLD));
            uit.add(Component.translatable(b.bezocht() ? "gui.guhs.biokompas.geweest" : "gui.guhs.biokompas.niet_geweest")
                    .withStyle(b.bezocht() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (!b.sectie().equals(nu)) {
                regelsIn(uit, Component.translatable("gui.guhs.biokompas.tip_elders", BiomeKompas.sectieNaam(b.sectie())), ChatFormatting.DARK_AQUA);
            }
            if (b.biome().equals(gekozen)) {
                uit.add(Component.translatable("gui.guhs.superkompas.zoekt_al").withStyle(ChatFormatting.GOLD));
            }
            return uit;
        }
    }

    private BiomesTab() {
    }
}
