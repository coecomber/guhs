package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.Markt;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/** The veilinghuis at the marktmeester: browse + filter + buy, put things up for sale, take your own back, collect proceeds. */
public class MarktScherm extends EcoScherm {
    private static final int RIJ = 21, ZICHT = 7;
    private static final String[] SLOTS = {"", "wapen", "hoofd", "borst", "benen", "voeten", "overig"};
    private static final String[] KLASSEN = {"", Klassen.RIDDER, Klassen.JAGER, Klassen.MAGIER};
    private static final int[][] LEVELS = {{0, 99}, {1, 5}, {6, 10}, {11, 15}, {16, 20}};
    private static final Pattern MUNT = Pattern.compile("(\\d+)\\s*([gzb]?)");

    private int tab, scroll, fSlot, fKlasse, fZeld = -1, fLevel, kiesSlot = -1;
    private EditBox zoek, prijs;

    public MarktScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.MARKT, Component.translatable("vadsrijk.economie.markt.titel"), data, items, 344, 244);
    }

    @Override
    protected void init() {
        super.init();
        String z = zoek == null ? "" : zoek.getValue(), pr = prijs == null ? "" : prijs.getValue();
        zoek = new EditBox(font, left + 8, top + 40, 96, 14, Component.translatable("vadsrijk.economie.markt.zoek"));
        zoek.setHint(Component.translatable("vadsrijk.economie.markt.zoek").withStyle(ChatFormatting.GRAY));
        zoek.setValue(z);
        zoek.setResponder(s -> scroll = 0);
        prijs = new EditBox(font, left + 200, top + 152, 90, 14, Component.translatable("vadsrijk.economie.markt.prijs_veld"));
        prijs.setHint(Component.literal("bv. 2z 50b").withStyle(ChatFormatting.GRAY));
        prijs.setValue(pr);
        addRenderableWidget(zoek);
        addRenderableWidget(prijs);
        zichtbaar();
    }

    private void zichtbaar() {
        if (zoek != null) {
            zoek.visible = tab == 0;
            prijs.visible = tab == 1;
        }
    }

    private void zetTab(int t) {
        tab = t;
        scroll = 0;
        zichtbaar();
    }

    @Override
    protected void opNieuweData() {
        if (tab == 1 && kiesSlot >= 0 && tas(kiesSlot) == null) {
            kiesSlot = -1;
        }
    }

    private CompoundTag tas(int slot) {
        ListTag l = data.getListOrEmpty("tas");
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getIntOr("slot", -1) == slot) {
                return l.getCompoundOrEmpty(i);
            }
        }
        return null;
    }

    /** "2z 50b", "1g", "350" (brons) -> brons; -1 when unreadable. */
    static long leesPrijs(String s) {
        Matcher m = MUNT.matcher(s.toLowerCase(Locale.ROOT).trim());
        long totaal = 0;
        boolean iets = false;
        while (m.find()) {
            iets = true;
            long n = Long.parseLong(m.group(1).length() > 12 ? "0" : m.group(1));
            totaal += switch (m.group(2)) {
                case "g" -> n * 10000;
                case "z" -> n * 100;
                default -> n;
            };
        }
        return iets ? totaal : -1;
    }

    private List<CompoundTag> gefilterd() {
        List<CompoundTag> uit = new ArrayList<>();
        ListTag l = data.getListOrEmpty("aanbod");
        String z = zoek == null ? "" : zoek.getValue().toLowerCase(Locale.ROOT).trim();
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            if (tab == 2) {
                if (t.getBooleanOr("eigen", false)) {
                    uit.add(t);
                }
                continue;
            }
            String slot = t.getStringOr("slot", "");
            if (fSlot > 0 && !(SLOTS[fSlot].equals("overig") ? slot.isEmpty() : SLOTS[fSlot].equals(slot))) {
                continue;
            }
            if (fKlasse > 0 && !KLASSEN[fKlasse].equals(t.getStringOr("klasse", ""))) {
                continue;
            }
            if (fZeld >= 0 && t.getIntOr("zeldzaamheid", -1) != fZeld) {
                continue;
            }
            int lv = t.getIntOr("level", -1);
            if (fLevel > 0 && (lv < LEVELS[fLevel][0] || lv > LEVELS[fLevel][1])) {
                continue;
            }
            if (!z.isEmpty() && !t.getStringOr("zoek", "").contains(z) && !t.getStringOr("naam", "").toLowerCase(Locale.ROOT).contains(z)) {
                continue;
            }
            uit.add(t);
        }
        return uit;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int y = top + 22;
        String[] tabs = {"vadsrijk.economie.markt.tab.kopen", "vadsrijk.economie.markt.tab.verkopen", "vadsrijk.economie.markt.tab.mijn"};
        for (int i = 0; i < 3; i++) {
            final int t = i;
            knop(g, mx, my, left + 8 + i * 74, y, 72, 14, Component.translatable(tabs[i], data.getIntOr("mijn", 0)), tab != i, () -> zetTab(t));
        }
        long op = data.getLongOr("opbrengst", 0);
        knop(g, mx, my, left + w - 108, y, 100, 14, Component.translatable("vadsrijk.economie.markt.haalop"), op > 0, () -> doe("haalop"));
        if (op > 0) {
            munten(g, left + w - 112, y + 3, op, true, false);
        }
        if (tab == 1) {
            verkopen(g, mx, my);
            return;
        }
        if (tab == 0) {
            int fx = left + 108;
            knop(g, mx, my, fx, top + 40, 56, 14, Component.translatable("vadsrijk.economie.markt.f.slot." + (fSlot == 0 ? "alle" : SLOTS[fSlot])), true, () -> {
                fSlot = (fSlot + 1) % SLOTS.length;
                scroll = 0;
            });
            knop(g, mx, my, fx + 58, top + 40, 56, 14, fKlasse == 0 ? Component.translatable("vadsrijk.economie.markt.f.klasse")
                    : Component.translatable("vadsrijk.economie.markt.f.klasse." + KLASSEN[fKlasse]), true, () -> {
                        fKlasse = (fKlasse + 1) % KLASSEN.length;
                        scroll = 0;
                    });
            Component zc = fZeld < 0 ? Component.translatable("vadsrijk.economie.markt.f.zeldzaam")
                    : Component.translatable(Zeldzaamheid.values()[fZeld].key()).withColor(Zeldzaamheid.values()[fZeld].rgb);
            knop(g, mx, my, fx + 116, top + 40, 60, 14, zc, true, () -> {
                fZeld = fZeld + 1 >= Zeldzaamheid.values().length ? -1 : fZeld + 1;
                scroll = 0;
            });
            knop(g, mx, my, fx + 178, top + 40, 50, 14, fLevel == 0 ? Component.translatable("vadsrijk.economie.markt.f.level")
                    : Component.literal("Lv " + LEVELS[fLevel][0] + "-" + LEVELS[fLevel][1]), true, () -> {
                        fLevel = (fLevel + 1) % LEVELS.length;
                        scroll = 0;
                    });
        }
        List<CompoundTag> l = gefilterd();
        int ly = top + 60;
        if (l.isEmpty()) {
            g.centeredText(font, Component.translatable(tab == 2 ? "vadsrijk.economie.markt.niks_eigen" : "vadsrijk.economie.markt.leeg"), left + w / 2, ly + 40,
                    ZACHT);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - ZICHT)));
        long munten = data.getLongOr("munten", 0);
        for (int i = scroll; i < Math.min(l.size(), scroll + ZICHT); i++) {
            CompoundTag t = l.get(i);
            int ry = ly + (i - scroll) * RIJ;
            boolean hover = binnen(mx, my, left + 6, ry, w - 12, RIJ - 1);
            g.fill(left + 6, ry, left + w - 6, ry + RIJ - 1, hover ? HOVER : (i % 2 == 0 ? RIJ_A : RIJ_B));
            ItemStack s = item(t.getIntOr("item", -1));
            slot(g, mx, my, left + 8, ry + 1, s, false);
            g.text(font, s.getHoverName(), left + 30, ry + 2, TEKST, false);
            g.text(font, Component.translatable("vadsrijk.economie.markt.verkoper", t.getStringOr("naam", "?")), left + 30, ry + 11, ZACHT, false);
            long pr = t.getLongOr("prijs", 0);
            long id = t.getLongOr("id", -1);
            int bx = left + w - 10;
            if (tab == 2 || t.getBooleanOr("eigen", false)) {
                knop(g, mx, my, bx - 56, ry + 2, 56, 16, Component.translatable("vadsrijk.economie.markt.trekin"), tab == 2, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putLong("id", id);
                    doe("trekin", a);
                });
            } else {
                knop(g, mx, my, bx - 40, ry + 2, 40, 16, Component.translatable("vadsrijk.economie.winkel.koop"), munten >= pr, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putLong("id", id);
                    doe("koop", a);
                });
            }
            munten(g, bx - 60, ry + 6, pr, true, false);
        }
        if (l.size() > ZICHT) {
            g.text(font, (scroll + 1) + "-" + Math.min(l.size(), scroll + ZICHT) + " / " + l.size(), left + 8, top + h - 26, ZACHT, false);
        }
    }

    private void verkopen(GuiGraphicsExtractor g, int mx, int my) {
        kop(g, Component.translatable("vadsrijk.economie.markt.kies_iets"), left + 8, top + 42);
        ListTag l = data.getListOrEmpty("tas");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 9) * 19, y = top + 54 + (i / 9) * 19;
            int slot = t.getIntOr("slot", -1);
            ItemStack s = item(t.getIntOr("item", -1));
            slot(g, mx, my, x, y, s, slot == kiesSlot);
            if (t.getBooleanOr("gebonden", false)) {
                g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
                tip(mx, my, x, y, 18, 18, List.of(Component.translatable("vadsrijk.economie.markt.gebonden").withStyle(ChatFormatting.RED)));
            } else {
                klikvlak(x, y, 18, 18, () -> kiesSlot = slot);
            }
        }
        int px = left + 200, py = top + 54;
        CompoundTag gekozen = kiesSlot >= 0 ? tas(kiesSlot) : null;
        if (gekozen == null) {
            g.text(font, Component.translatable("vadsrijk.economie.markt.klik_iets"), px, py, ZACHT, false);
            return;
        }
        ItemStack s = item(gekozen.getIntOr("item", -1));
        slot(g, mx, my, px, py, s, true);
        g.text(font, s.getHoverName(), px + 22, py + 5, TEKST, false);
        long waarde = gekozen.getLongOr("waarde", 0);
        if (waarde > 0) {
            g.text(font, Component.translatable("vadsrijk.economie.markt.winkelwaarde"), px, py + 26, ZACHT, false);
            munten(g, px + 130, py + 26, waarde, true, false);
        }
        g.text(font, Component.translatable("vadsrijk.economie.markt.vraagprijs"), px, top + 142, TEKST, false);
        long pr = leesPrijs(prijs.getValue());
        if (pr > 0) {
            munten(g, px, top + 170, pr, false, false);
            g.text(font, Component.translatable("vadsrijk.economie.markt.fee"), px, top + 182, ZACHT, false);
            munten(g, px + 130, top + 182, Markt.fee(pr), true, false);
        }
        boolean kan = pr > 0 && pr <= Markt.MAX_PRIJS && data.getLongOr("munten", 0) >= Markt.fee(Math.max(1, pr))
                && data.getIntOr("mijn", 0) < Markt.MAX_PER_SPELER;
        knop(g, mx, my, px, top + 196, 130, 18, Component.translatable("vadsrijk.economie.markt.zet"), kan, () -> {
            CompoundTag a = new CompoundTag();
            a.putInt("slot", kiesSlot);
            a.putLong("prijs", pr);
            doe("zet", a);
            kiesSlot = -1;
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        return true;
    }
}
