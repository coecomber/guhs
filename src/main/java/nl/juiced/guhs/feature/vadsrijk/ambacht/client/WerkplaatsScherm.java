package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.items.client.SmidTabs;

/**
 * A workshop (smidse, naaiatelier, keuken, brouwerij) with its recipe book: the recipes on the left (grey = not yet), the chosen one
 * on the right with what it needs, for Maatwerk the 2 (Meesterwerk 3) bonuses you choose and the Meesterwerk property, for
 * cutting the crystal kind. "Maak" plays a short timing bar (hit the pink: better Vadsigheid); "Maak 10x" skips it (60 %).
 */
public class WerkplaatsScherm extends EcoScherm {
    private static final RandomSource R = RandomSource.create();
    private String gekozen = "";
    private final List<String> stats = new ArrayList<>();
    private String eigenschap = "", kristal = "";
    private int scroll;
    private long miniStart = -1;
    private double miniDoel, miniFase;

    public WerkplaatsScherm(CompoundTag data, List<ItemStack> items) {
        super("ambacht_werk", Component.translatable("vadsrijk.ambacht.werk.titel." + data.getStringOr("ambacht", "smeden")), data, items, 340, 236);
        gekozen = data.getStringOr("gekozen", "");
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new AmbachtPayloads.Doe(actie, args));
    }

    private CompoundTag recept() {
        ListTag l = data.getListOrEmpty("recepten");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag r = l.getCompoundOrEmpty(i);
            if (r.getStringOr("id", "").equals(gekozen)) {
                return r;
            }
        }
        return l.isEmpty() ? new CompoundTag() : l.getCompoundOrEmpty(0);
    }

    /**
     * 1.10.24 (the user: "knoppen rechts onderin vallen buiten beeld, Maak en Nu komen over de andere knoppen heen"): a roomier
     * logical size, the recipe list as tall as the panel, the chosen recipe in its own clipped box that scrolls (the Maatwerk stat
     * buttons and the Meesterwerk properties ran under the Maak bar and past the bottom), and the Maak bar fixed at the bottom.
     */
    private static final int LIJST_B = 178, RIJ = 22;
    private int detailScroll, detailH;
    private int[] lijstVak = new int[4], detailVak = new int[4];

    @Override
    protected void init() {
        w = Math.max(460, Math.min(560, width - 16));
        h = Math.max(260, Math.min(330, height - 20));
        super.init();
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.text(font, Component.translatable("vadsrijk.ambacht.werk.level", data.getIntOr("level", 1)), left + 8, top + 23, ZACHT, false);
        lijst(g, mx, my);
        detail(g, mx, my);
    }

    private void lijst(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("recepten");
        int x = left + 6, y0 = top + 35, max = Math.max(1, (top + h - 18 - y0) / RIJ);
        lijstVak = new int[] {x, y0, x + LIJST_B, y0 + max * RIJ};
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - max)));
        String nu = recept().getStringOr("id", "");
        int y = y0;
        for (int i = scroll, rij = 0; i < l.size() && rij < max; i++, rij++) {
            CompoundTag r = l.getCompoundOrEmpty(i);
            String id = r.getStringOr("id", "");
            boolean kent = r.getBooleanOr("kent", false);
            boolean hover = binnen(mx, my, x, y, LIJST_B - 6, RIJ - 1);
            g.fill(x, y, x + LIJST_B - 6, y + RIJ - 1, id.equals(nu) ? HOVER : hover ? 0xFFFFE0F0 : rij % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, x + 1, y + 1, kent ? item(r.getIntOr("item", -1)) : new ItemStack(Items.GRAY_DYE), false);
            g.text(font, SmidTabs.kort(font, Component.translatable(r.getStringOr("naam", "")), LIJST_B - 32), x + 22, y + 3, kent ? TEKST : ZACHT, false);
            int kan = r.getIntOr("kan", 0);
            g.text(font, SmidTabs.kort(font, Component.translatable("vadsrijk.ambacht.recept.lv_kan", r.getIntOr("level", 1), kan), LIJST_B - 32), x + 22, y + 12,
                    kan > 0 && kent ? GROEN : ZACHT, false);
            klikvlak(x, y, LIJST_B - 6, RIJ - 1, () -> {
                if (!gekozen.equals(id)) {
                    detailScroll = 0;
                }
                gekozen = id;
                stats.clear();
                eigenschap = "";
                miniStart = -1;
            });
            y += RIJ;
        }
        if (l.size() > max) {   // scroll bar right of the list
            int bh = lijstVak[3] - lijstVak[1], dh = Math.max(10, bh * max / l.size()), dy = (bh - dh) * scroll / Math.max(1, l.size() - max);
            g.fill(x + LIJST_B - 4, y0, x + LIJST_B - 1, y0 + bh, 0x40C0287C);
            g.fill(x + LIJST_B - 4, y0 + dy, x + LIJST_B - 1, y0 + dy + dh, 0xFFE0489A);
        }
    }

    private void detail(GuiGraphicsExtractor g, int mx, int my) {
        CompoundTag r = recept();
        if (r.isEmpty()) {
            return;
        }
        int x = left + LIJST_B + 16, rw = left + w - 8 - x;
        g.fill(x - 4, top + 31, x + rw + 4, top + h - 16, 0xFFFFF6FB);
        boolean kent = r.getBooleanOr("kent", false);
        // header (fixed): the product, its name, level and tier
        int y = top + 35;
        slot(g, mx, my, x, y, item(r.getIntOr("item", -1)), false);
        g.text(font, SmidTabs.kort(font, Component.translatable(r.getStringOr("naam", "")).copy().withStyle(ChatFormatting.BOLD), rw - 24), x + 22, y + 1, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.ambacht.recept.lv", r.getIntOr("level", 1), r.getIntOr("tier", 1)), x + 22, y + 10, kent ? ZACHT : ROOD, false);
        // the bar at the bottom (fixed): Maak / Maak 10x, or the timing bar with Nu!
        int balk = top + h - 40;
        // the middle (clipped, scrolls): what it needs, the choices
        detailVak = new int[] {x - 2, top + 57, x + rw + 2, balk - 4};
        int maxScroll = Math.max(0, detailH - (detailVak[3] - detailVak[1]));
        detailScroll = Math.min(detailScroll, maxScroll);
        int bw = rw - (maxScroll > 0 ? 6 : 0);
        zetKlip(g, detailVak[0], detailVak[1], detailVak[2], detailVak[3]);
        int y0 = top + 59;
        y = y0 - detailScroll;
        ListTag in = r.getListOrEmpty("in");
        boolean genoeg = true;
        for (int k = 0; k < in.size(); k++) {
            CompoundTag c = in.getCompoundOrEmpty(k);
            int heeft = c.getIntOr("heeft", 0), nodig = c.getIntOr("nodig", 1);
            if (heeft >= 0 && heeft < nodig) {
                genoeg = false;
            }
            slot(g, mx, my, x, y, item(c.getIntOr("item", -1)), heeft >= nodig);
            Component naam = Component.translatable(c.getStringOr("naam", ""));
            if (c.getBooleanOr("of", false)) {
                naam = naam.copy().append(Component.translatable("vadsrijk.ambacht.recept.of_kort"));
            }
            g.text(font, SmidTabs.kort(font, naam, bw - 24), x + 22, y + 1, TEKST, false);
            g.text(font, heeft < 0 ? "3x" : heeft + "/" + nodig, x + 22, y + 10, heeft >= nodig || heeft < 0 ? GROEN : ROOD, false);
            y += 20;
        }
        if (!r.getBooleanOr("hier", true)) {
            // U-AMBACHT2: T4/T5 recipes are made in Hoogvads' Ambachtsgilde (T4 also in Lampionhaven); 1.10.4: T6 in Saffraanpoort
            int tr = r.getIntOr("tier", 1);
            y = SmidTabs.regels(font, g, Component.translatable(tr >= 6 ? "vadsrijk.ambacht.werk.zijdeland" : tr >= 5 ? "vadsrijk.ambacht.werk.gilde"
                    : "vadsrijk.ambacht.werk.gilde4"), x, y + 2, bw, ROOD) + 2;
        }
        String soort = r.getStringOr("soort", "");
        int keuzes = r.getIntOr("keuzes", 0);
        if (keuzes > 0) {
            y = SmidTabs.regels(font, g, Component.translatable("vadsrijk.ambacht.werk.kies_n", keuzes, stats.size()), x, y + 2, bw, TEKST) + 2;
            ListTag st = data.getListOrEmpty("stats");
            int kw = (bw - 4) / 2;
            for (int i = 0; i < st.size(); i++) {
                String s = st.getString(i).orElse("");
                boolean aan = stats.contains(s);
                int bx = x + (i % 2) * (kw + 4), by = y + (i / 2) * 17;
                // 1.10.24: a narrow button shows the short name (before the brackets), the full one as a tooltip
                Component vol = Component.translatable("vadsrijk.kern.stat." + s);
                Component toon = vol;
                if (font.width(vol.copy().withStyle(ChatFormatting.BOLD)) > kw - 6 && vol.getString().contains(" (")) {
                    toon = Component.literal(vol.getString().substring(0, vol.getString().indexOf(" (")));
                    tip(mx, my, bx, by, kw, 15, List.of(vol));
                }
                knop(g, mx, my, bx, by, kw, 15, toon.copy().withStyle(aan ? ChatFormatting.BOLD : ChatFormatting.RESET),
                        true, () -> {
                            if (aan) {
                                stats.remove(s);
                            } else if (stats.size() < keuzes) {
                                stats.add(s);
                            }
                        });
                if (aan) {
                    g.fill(bx, by, bx + 3, by + 15, 0xFF4AC87A);
                }
            }
            y += ((st.size() + 1) / 2) * 17 + 3;
            if (soort.equals("meester")) {
                ListTag e = data.getListOrEmpty("eigenschappen");
                for (int i = 0; i < e.size(); i++) {
                    CompoundTag c = e.getCompoundOrEmpty(i);
                    String id = c.getStringOr("id", "");
                    boolean aan = id.equals(eigenschap);
                    knop(g, mx, my, x, y, bw, 15, Component.translatable(c.getStringOr("key", ""), c.getIntOr("procent", 0))
                            .withStyle(aan ? ChatFormatting.BOLD : ChatFormatting.RESET), true, () -> eigenschap = id);
                    if (aan) {
                        g.fill(x, y, x + 3, y + 15, 0xFFFFD84A);
                    }
                    y += 17;
                }
            }
        }
        if (soort.equals("slijp")) {
            int q = r.getStringOr("id", "").endsWith("stralend") ? 1 : 0;
            ListTag kr = data.getListOrEmpty("kristallen");
            int kol = Math.max(1, bw / 38);
            for (int i = 0; i < kr.size(); i++) {
                CompoundTag c = kr.getCompoundOrEmpty(i);
                String id = c.getStringOr("id", "");
                int n = c.getIntOr(q == 0 ? "ruw" : "geslepen", 0);
                int bx = x + (i % kol) * 38, by = y + (i / kol) * 21;
                slot(g, mx, my, bx, by, item(c.getIntOr("item", -1)), id.equals(kristal));
                g.text(font, String.valueOf(n), bx + 20, by + 5, n >= 3 ? GROEN : ZACHT, false);
                klikvlak(bx, by, 18, 18, () -> kristal = id);
            }
            y += ((kr.size() + kol - 1) / kol) * 21;
        }
        geenKlip(g);
        detailH = y + detailScroll - y0 + 4;
        if (maxScroll > 0) {   // scroll bar on the right edge of the box
            int bh = detailVak[3] - detailVak[1], dh = Math.max(10, bh * bh / Math.max(1, detailH)), dy = (bh - dh) * detailScroll / maxScroll;
            g.fill(detailVak[2] - 4, detailVak[1], detailVak[2] - 1, detailVak[3], 0x40C0287C);
            g.fill(detailVak[2] - 4, detailVak[1] + dy, detailVak[2] - 1, detailVak[1] + dy + dh, 0xFFE0489A);
        }
        g.fill(x - 4, balk - 3, x + rw + 4, balk - 2, 0x40C0287C);   // a thin line above the bar
        // making
        boolean klaarKeuze = stats.size() >= keuzes && (!soort.equals("meester") || !eigenschap.isEmpty()) && (!soort.equals("slijp") || !kristal.isEmpty());
        boolean kan = kent && klaarKeuze && (genoeg || soort.equals("slijp"));
        if (miniStart >= 0) {
            mini(g, mx, my, x, balk, rw);
            return;
        }
        boolean massa = soort.equals("eten") || soort.equals("drank") || soort.equals("slijp");
        int knopB = massa ? (rw - 6) / 2 : rw;
        knop(g, mx, my, x, balk, knopB, 20, Component.translatable("vadsrijk.ambacht.werk.maak").withStyle(ChatFormatting.BOLD), kan, () -> {
            miniStart = System.currentTimeMillis();
            miniDoel = 0.2 + R.nextDouble() * 0.6;
            miniFase = R.nextDouble() * Math.PI * 2;
        });
        if (massa) {
            knop(g, mx, my, x + knopB + 6, balk, rw - knopB - 6, 20, Component.translatable("vadsrijk.ambacht.werk.maak10"), kent && klaarKeuze && r.getIntOr("kan", 0) >= 10,
                    () -> maak(0, 10));
        }
    }

    private double marker() {
        double t = (System.currentTimeMillis() - miniStart) / 1000.0;
        return 0.5 + 0.5 * Math.sin(t * 1.7 * Math.PI + miniFase);
    }

    /** The timing bar (left) and Nu! (right), side by side in the fixed bar. */
    private void mini(GuiGraphicsExtractor g, int mx, int my, int x, int y, int rw) {
        int nw = Math.max(56, font.width(Component.translatable("vadsrijk.ambacht.werk.nu")) + 16);
        int bw = rw - nw - 8;
        g.fill(x, y + 4, x + bw, y + 16, 0xFF5A3A5A);
        int dx = (int) ((miniDoel - 0.09) * bw);
        g.fill(x + dx, y + 4, x + dx + (int) (0.18 * bw), y + 16, 0xFFFF6FB5);
        g.fill(x + dx + (int) (0.0675 * bw), y + 4, x + dx + (int) (0.1125 * bw), y + 16, 0xFFFFFFFF);
        int m = x + (int) (marker() * bw);
        g.fill(m - 1, y + 1, m + 1, y + 19, 0xFF4A1E46);
        knop(g, mx, my, x + bw + 8, y, nw, 20, Component.translatable("vadsrijk.ambacht.werk.nu").withStyle(ChatFormatting.BOLD), true, () -> {
            double d = Math.abs(marker() - miniDoel);
            int s = d <= 0.0225 ? 2 : d <= 0.09 ? 1 : 0;
            geluid(s == 2 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.UI_BUTTON_CLICK.value(), 1f + 0.2f * s);
            maak(s, 1);
        });
        if (System.currentTimeMillis() - miniStart > 5000) {
            maak(0, 1);
        }
    }

    private void maak(int score, int aantal) {
        CompoundTag a = new CompoundTag();
        a.putString("recept", recept().getStringOr("id", ""));
        a.putString("keuzes", String.join(",", stats));
        a.putString("eigenschap", eigenschap);
        a.putString("kristal", kristal);
        a.putInt("score", score);
        a.putInt("aantal", aantal);
        miniStart = -1;
        doe("maak", a);
    }

    @Override
    protected void opNieuweData() {
        String g = data.getStringOr("gekozen", "");
        if (!g.isEmpty()) {
            gekozen = g;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (binnen((int) mx, (int) my, detailVak[0], detailVak[1], detailVak[2] - detailVak[0], detailVak[3] - detailVak[1])) {
            detailScroll = Math.max(0, Math.min(detailScroll - (int) Math.signum(dy) * 14, Math.max(0, detailH - (detailVak[3] - detailVak[1]))));
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }
}
