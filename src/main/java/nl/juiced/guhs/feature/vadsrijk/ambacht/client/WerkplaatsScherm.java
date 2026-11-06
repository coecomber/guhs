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

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.text(font, Component.translatable("vadsrijk.ambacht.werk.level", data.getIntOr("level", 1)), left + 8, top + 22, ZACHT, false);
        lijst(g, mx, my);
        detail(g, mx, my);
    }

    private void lijst(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("recepten");
        int max = 9, y = top + 34;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - max)));
        String nu = recept().getStringOr("id", "");
        for (int i = scroll, rij = 0; i < l.size() && rij < max; i++, rij++) {
            CompoundTag r = l.getCompoundOrEmpty(i);
            String id = r.getStringOr("id", "");
            boolean kent = r.getBooleanOr("kent", false);
            g.fill(left + 6, y, left + 150, y + 20, id.equals(nu) ? HOVER : rij % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 7, y + 1, kent ? item(r.getIntOr("item", -1)) : new ItemStack(Items.GRAY_DYE), false);
            g.text(font, font.plainSubstrByWidth(Component.translatable(r.getStringOr("naam", "")).getString(), 118), left + 28, y + 2, kent ? TEKST : ZACHT, false);
            int kan = r.getIntOr("kan", 0);
            g.text(font, Component.translatable("vadsrijk.ambacht.recept.lv_kan", r.getIntOr("level", 1), kan), left + 28, y + 11, kan > 0 && kent ? GROEN : ZACHT, false);
            klikvlak(left + 6, y, 144, 20, () -> {
                gekozen = id;
                stats.clear();
                eigenschap = "";
                miniStart = -1;
            });
            y += 21;
        }
    }

    private void detail(GuiGraphicsExtractor g, int mx, int my) {
        CompoundTag r = recept();
        if (r.isEmpty()) {
            return;
        }
        int x = left + 158, y = top + 34;
        boolean kent = r.getBooleanOr("kent", false);
        slot(g, mx, my, x, y, item(r.getIntOr("item", -1)), false);
        kop(g, Component.translatable(r.getStringOr("naam", "")), x + 22, y + 1);
        g.text(font, Component.translatable("vadsrijk.ambacht.recept.lv", r.getIntOr("level", 1), r.getIntOr("tier", 1)), x + 22, y + 10, kent ? ZACHT : ROOD, false);
        y += 22;
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
            g.text(font, naam, x + 22, y + 1, TEKST, false);
            g.text(font, heeft < 0 ? "3x" : heeft + "/" + nodig, x + 22, y + 10, heeft >= nodig || heeft < 0 ? GROEN : ROOD, false);
            y += 20;
        }
        String soort = r.getStringOr("soort", "");
        int keuzes = r.getIntOr("keuzes", 0);
        if (keuzes > 0) {
            g.text(font, Component.translatable("vadsrijk.ambacht.werk.kies_n", keuzes, stats.size()), x, y + 1, TEKST, false);
            y += 11;
            ListTag st = data.getListOrEmpty("stats");
            for (int i = 0; i < st.size(); i++) {
                String s = st.getString(i).orElse("");
                boolean aan = stats.contains(s);
                int bx = x + (i % 2) * 88, by = y + (i / 2) * 12;
                knop(g, mx, my, bx, by, 86, 11, Component.translatable("vadsrijk.kern.stat." + s).withStyle(aan ? ChatFormatting.BOLD : ChatFormatting.RESET),
                        true, () -> {
                            if (aan) {
                                stats.remove(s);
                            } else if (stats.size() < keuzes) {
                                stats.add(s);
                            }
                        });
                if (aan) {
                    g.fill(bx, by, bx + 2, by + 11, 0xFF4AC87A);
                }
            }
            y += ((st.size() + 1) / 2) * 12 + 2;
            if (soort.equals("meester")) {
                ListTag e = data.getListOrEmpty("eigenschappen");
                for (int i = 0; i < e.size(); i++) {
                    CompoundTag c = e.getCompoundOrEmpty(i);
                    String id = c.getStringOr("id", "");
                    boolean aan = id.equals(eigenschap);
                    knop(g, mx, my, x, y, 174, 11, Component.translatable(c.getStringOr("key", ""), c.getIntOr("procent", 0))
                            .withStyle(aan ? ChatFormatting.BOLD : ChatFormatting.RESET), true, () -> eigenschap = id);
                    if (aan) {
                        g.fill(x, y, x + 2, y + 11, 0xFFFFD84A);
                    }
                    y += 12;
                }
            }
        }
        if (soort.equals("slijp")) {
            int q = r.getStringOr("id", "").endsWith("stralend") ? 1 : 0;
            ListTag kr = data.getListOrEmpty("kristallen");
            for (int i = 0; i < kr.size(); i++) {
                CompoundTag c = kr.getCompoundOrEmpty(i);
                String id = c.getStringOr("id", "");
                int n = c.getIntOr(q == 0 ? "ruw" : "geslepen", 0);
                int bx = x + (i % 5) * 35, by = y + (i / 5) * 20;
                slot(g, mx, my, bx, by, item(c.getIntOr("item", -1)), id.equals(kristal));
                g.text(font, String.valueOf(n), bx + 20, by + 5, n >= 3 ? GROEN : ZACHT, false);
                klikvlak(bx, by, 18, 18, () -> kristal = id);
            }
            if (!kristal.isEmpty()) {
                genoeg &= true;
            }
        }
        // making
        boolean klaarKeuze = stats.size() >= keuzes && (!soort.equals("meester") || !eigenschap.isEmpty()) && (!soort.equals("slijp") || !kristal.isEmpty());
        boolean kan = kent && klaarKeuze && (genoeg || soort.equals("slijp"));
        int by = top + h - 34;
        if (miniStart >= 0) {
            mini(g, mx, my, x, by);
            return;
        }
        knop(g, mx, my, x, by, 84, 16, Component.translatable("vadsrijk.ambacht.werk.maak"), kan, () -> {
            miniStart = System.currentTimeMillis();
            miniDoel = 0.2 + R.nextDouble() * 0.6;
            miniFase = R.nextDouble() * Math.PI * 2;
        });
        boolean massa = soort.equals("eten") || soort.equals("drank") || soort.equals("slijp");
        if (massa) {
            knop(g, mx, my, x + 90, by, 84, 16, Component.translatable("vadsrijk.ambacht.werk.maak10"), kent && klaarKeuze && r.getIntOr("kan", 0) >= 10,
                    () -> maak(0, 10));
        }
    }

    private double marker() {
        double t = (System.currentTimeMillis() - miniStart) / 1000.0;
        return 0.5 + 0.5 * Math.sin(t * 1.7 * Math.PI + miniFase);
    }

    private void mini(GuiGraphicsExtractor g, int mx, int my, int x, int y) {
        int bw = 120;
        g.fill(x, y + 3, x + bw, y + 13, 0xFF5A3A5A);
        int dx = (int) ((miniDoel - 0.09) * bw);
        g.fill(x + dx, y + 3, x + dx + (int) (0.18 * bw), y + 13, 0xFFFF6FB5);
        g.fill(x + dx + (int) (0.0675 * bw), y + 3, x + dx + (int) (0.1125 * bw), y + 13, 0xFFFFFFFF);
        int m = x + (int) (marker() * bw);
        g.fill(m - 1, y, m + 1, y + 16, 0xFF4A1E46);
        knop(g, mx, my, x + bw + 6, y, 48, 16, Component.translatable("vadsrijk.ambacht.werk.nu"), true, () -> {
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
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }
}
