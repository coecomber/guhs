package nl.juiced.guhs.feature.erfenis.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads.Actie;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Goed;
import nl.juiced.guhs.feature.erfenis.Prijzen;
import nl.juiced.guhs.taal.Tekst;

/**
 * De Erfenis, the overview's windows (drawn in the middle over the world, the camera stays): the storage, "Naar huis
 * sturen", the Loket, the Bouwkeet block choice of a Bouwblokkenwerkplaats, and the Guhpension. Esc or the cross closes one.
 */
final class Vensters {
    private Vensters() {
    }

    /** One window: its size, title and contents. */
    abstract static class Venster {
        int scroll;

        abstract int breed();

        abstract int hoog();

        abstract Component titel();

        /** The contents inside (x0, y0)-(x1, y1) (under the title). */
        abstract void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu);

        void scrol(double stappen) {
            scroll = (int) Math.max(0, scroll - stappen * 20);
        }

        /** Draws the frame and the contents; returns false when it wants to close. */
        final void teken(Ui ui, OverzichtScherm s, Base b, int sw, int sh, long nu) {
            int w = Math.min(breed(), sw - 12), h = Math.min(hoog(), sh - 12);
            int x0 = (sw - w) / 2, y0 = (sh - h) / 2;
            ui.g.fill(0, 0, sw, sh, 0x40301020);
            ui.vlak(0, 0, sw, sh, null);
            Teken.paneel(ui.g, x0, y0, x0 + w, y0 + h);
            ui.vlak(x0, y0, x0 + w, y0 + h, null);
            ui.tekst(titel().copy().withStyle(ChatFormatting.BOLD), x0 + 10, y0 + 8, Teken.DONKER);
            ui.knop(Component.literal("x"), x0 + w - 18, y0 + 5, x0 + w - 5, y0 + 18, true, Teken.ROZE_DONKER, s::sluitVenster,
                    List.of(Component.translatable("erfenis.ui.sluiten")));
            ui.g.fill(x0 + 8, y0 + 21, x0 + w - 8, y0 + 22, 0x40D27A9C);
            inhoud(ui, b, x0 + 10, y0 + 26, x0 + w - 10, y0 + h - 8, nu);
        }
    }

    // =====================================================================================================================
    // the storage
    // =====================================================================================================================

    static final class Opslag extends Venster {
        @Override
        int breed() {
            return 330;
        }

        @Override
        int hoog() {
            return 250;
        }

        @Override
        Component titel() {
            return Component.translatable("erfenis.venster.opslag");
        }

        @Override
        void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu) {
            int vrij = b.opslag();
            ui.tekst(Component.translatable("erfenis.ui.opslag_regel", Ui.getal(b.opslagGebruikt()), Ui.getal(vrij)), x0, y0, Teken.TEKST);
            Teken.balk(ui.g, x0, y0 + 10, x1, y0 + 15, vrij <= 0 ? 0 : b.opslagGebruikt() / (float) vrij,
                    b.opslagGebruikt() >= vrij ? Teken.ROOD : Teken.ROZE_DONKER);
            int y = y0 + 20;
            int kol = Math.max(1, (x1 - x0) / 52), i = 0;
            ui.g.enableScissor(x0, y, x1, y1);
            int top = y - scroll;
            for (Goed g : Goed.values()) {
                int n = b.heeft(g);
                if (n <= 0) {
                    continue;
                }
                int cx = x0 + i % kol * 52, cy = top + i / kol * 20;
                if (cy + 18 > y && cy < y1) {
                    Teken.rond(ui.g, cx, cy, cx + 50, cy + 18, 0xFFF6E9EF, 2);
                    ui.item(Ui.icoon(g), cx + 1, cy + 1);
                    ui.tekst(Ui.getal(n), cx + 19, cy + 5, Teken.DONKER);
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.translatable(g.naamKey()).withStyle(ChatFormatting.BOLD));
                    tip.add(Component.translatable(g.naarHuis() ? "erfenis.ui.mag_naar_huis" : "erfenis.ui.blijft_in_dorp").withColor(Teken.ZACHT));
                    ui.klik(cx, cy, cx + 50, cy + 18, null, tip);
                }
                i++;
            }
            if (i == 0) {
                ui.tekst(Component.translatable("erfenis.ui.opslag_leeg"), x0, top + 2, Teken.ZACHT);
                i = 1;
            }
            int yy = top + ((i + kol - 1) / kol) * 20 + 6;
            yy = lijst(ui, x0, yy, x1, Component.translatable("erfenis.ui.bouwblokken"), b.bouwblokSoorten);
            yy = lijst(ui, x0, yy, x1, Component.translatable("erfenis.ui.decoschuurtje"), b.decoOpslag);
            Map<String, Integer> vondsten = new LinkedHashMap<>();
            for (String v : b.vondsten) {
                vondsten.merge(v, 1, Integer::sum);
            }
            yy = lijst(ui, x0, yy, x1, Component.translatable("erfenis.ui.vondstenkistje"), vondsten);
            ui.g.disableScissor();
            scroll = Math.max(0, Math.min(scroll, yy + scroll - y1));
        }

        /** A heading and "name x n" lines (Bouwkeet blocks by their item name, deco and finds by lang key). */
        private static int lijst(Ui ui, int x0, int y, int x1, Component kop, Map<String, Integer> m) {
            if (m.isEmpty()) {
                return y;
            }
            ui.tekst(kop.copy().withStyle(ChatFormatting.BOLD), x0, y, Teken.DONKER);
            y += 11;
            MutableComponent regel = Component.empty();
            boolean eerste = true;
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                if (!eerste) {
                    regel.append(", ");
                }
                eerste = false;
                Item it = e.getKey().contains(":") ? Goed.item(e.getKey()) : null;
                regel.append(it != null ? Component.translatable(it.getDescriptionId()) : Component.translatable(sleutel(e.getKey())))
                        .append(" x" + e.getValue());
            }
            for (FormattedCharSequence s : ui.font.split(regel, x1 - x0)) {
                ui.tekst(s, x0, y, Teken.TEKST);
                y += 9;
            }
            return y + 5;
        }

        private static String sleutel(String id) {
            return id.startsWith("deco_") || !id.contains("_") && !id.startsWith("ansicht") ? "erfenis.deco." + id.replace("deco_", "")
                    : "erfenis.vondst." + id;
        }
    }

    // =====================================================================================================================
    // Naar huis sturen
    // =====================================================================================================================

    static final class NaarHuis extends Venster {
        @Nullable
        private Goed gekozen;
        private int aantal = 16;

        @Override
        int breed() {
            return 340;
        }

        @Override
        int hoog() {
            return 230;
        }

        @Override
        Component titel() {
            return Component.translatable("erfenis.venster.naar_huis");
        }

        @Override
        void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu) {
            for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.naar_huis_uitleg"), x1 - x0)) {
                ui.tekst(s, x0, y0, Teken.ZACHT);
                y0 += 9;
            }
            y0 += 3;
            int lijstB = 150;
            ui.g.enableScissor(x0, y0, x0 + lijstB, y1);
            int y = y0 - scroll, n = 0;
            for (Goed g : Goed.values()) {
                if (!g.naarHuis()) {
                    continue;
                }
                int heeft = b.heeft(g);
                if (y + 16 > y0 && y < y1) {
                    boolean is = g == gekozen;
                    Teken.rond(ui.g, x0, y, x0 + lijstB - 4, y + 16, is ? Teken.GOUD : ui.op(x0, y, x0 + lijstB - 4, y + 16) ? 0xFFF7D9E5 : 0xFFF6EEF2, 2);
                    ui.item(Ui.icoon(g), x0 + 1, y, 1f);
                    ui.passend(Component.translatable(g.naamKey()), x0 + 19, y + 4, 80, heeft > 0 ? Teken.DONKER : Teken.ZACHT);
                    String t = Ui.getal(heeft);
                    ui.tekst(t, x0 + lijstB - 8 - ui.font.width(t), y + 4, heeft > 0 ? Teken.DONKER : Teken.ZACHT);
                    ui.klik(x0, y, x0 + lijstB - 4, y + 16, () -> {
                        gekozen = g;
                        aantal = Math.max(1, Math.min(aantal, Math.max(1, b.magNaarHuis(g, ErfenisClient.nu()))));
                    }, null);
                }
                y += 18;
                n++;
            }
            ui.g.disableScissor();
            scroll = Math.max(0, Math.min(scroll, n * 18 - (y1 - y0)));
            int rx = x0 + lijstB + 8;
            if (gekozen == null) {
                for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.kies_goed"), x1 - rx)) {
                    ui.tekst(s, rx, y0 + 4, Teken.ZACHT);
                    y0 += 9;
                }
                return;
            }
            Goed g = gekozen;
            int mag = b.magNaarHuis(g, nu);
            ui.item(Ui.icoon(g), rx, y0);
            ui.tekst(Component.translatable(g.naamKey()).withStyle(ChatFormatting.BOLD), rx + 20, y0 + 4, Teken.DONKER);
            int yy = y0 + 22;
            ui.tekst(Component.translatable("erfenis.ui.in_opslag", b.heeft(g)), rx, yy, Teken.TEKST);
            yy += 10;
            if (g.capPerUur > 0) {
                ui.tekst(Component.translatable("erfenis.ui.leverkist_cap", g.capPerUur, mag), rx, yy, Teken.TEKST);
            } else {
                ui.tekst(Component.translatable(g == Goed.WOL ? "erfenis.ui.wol_vrij" : "erfenis.ui.geen_cap"), rx, yy, Teken.GROEN_DONKER);
            }
            yy += 14;
            aantal = Math.max(1, Math.min(aantal, Math.max(1, mag)));
            String t = Integer.toString(aantal);
            ui.tekst(Component.translatable("erfenis.ui.aantal").append(": "), rx, yy + 3, Teken.TEKST);
            Teken.rond(ui.g, rx + 50, yy, rx + 100, yy + 14, 0xFFFFFFFF, 2);
            ui.tekst(t, rx + 75 - ui.font.width(t) / 2, yy + 3, Teken.DONKER);
            yy += 18;
            int[] stap = {-64, -10, -1, 1, 10, 64};
            int bx = rx, bw = (x1 - rx - 10) / 6;
            for (int st : stap) {
                ui.knop(Component.literal((st > 0 ? "+" : "") + st), bx, yy, bx + bw, yy + 14, true, Teken.ROZE_DONKER,
                        () -> aantal = Math.max(1, aantal + st), null);
                bx += bw + 2;
            }
            yy += 18;
            ui.knop(Component.translatable("erfenis.ui.max_knop"), rx, yy, rx + 60, yy + 14, mag > 0, Teken.ROZE_DONKER, () -> aantal = mag, null);
            yy += 20;
            Base.Fout f = b.kanNaarHuis(g, aantal, nu);
            ui.knop(Component.translatable("erfenis.ui.versturen", aantal), rx, yy, x1, yy + 16, f == null, Teken.GROEN,
                    () -> ErfenisClient.stuur(new Actie(Actie.NAAR_HUIS, 0, aantal, 0, 0, 0, 0, g.id)),
                    f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args()))));
        }
    }

    // =====================================================================================================================
    // the Loket
    // =====================================================================================================================

    static final class Loket extends Venster {
        @Override
        int breed() {
            return 360;
        }

        @Override
        int hoog() {
            return 240;
        }

        @Override
        Component titel() {
            return Component.translatable("erfenis.venster.loket");
        }

        /** What the next upgrades ask for: item id -> (total needed, which upgrades). */
        static Map<String, Object[]> nodig(Base b) {
            Map<String, Object[]> m = new LinkedHashMap<>();
            for (Gebouw g : b.gebouwen) {
                if (g.level < g.type.topLevel() && g.level + 1 <= Math.max(g.type.maxLevel(b.hoeveLevel()), g.type == GebouwType.HOEVE ? 10 : 0)) {
                    voeg(m, Prijzen.gebouw(g.type, g.level + 1).hoofdwereld(), Component.translatable(g.type.naamKey()).append(" " + (g.level + 1)));
                }
            }
            for (GebouwType t : GebouwType.values()) {
                if (t != GebouwType.HOEVE && b.aantal(t) < t.maxAantal(b.hoeveLevel())) {
                    voeg(m, Prijzen.gebouw(t, 1).hoofdwereld(), Component.translatable(t.naamKey()).append(" 1"));
                }
            }
            return m;
        }

        @SuppressWarnings("unchecked")
        private static void voeg(Map<String, Object[]> m, Map<String, Integer> hw, Component waarvoor) {
            for (Map.Entry<String, Integer> e : hw.entrySet()) {
                if (Goed.item(e.getKey()) == null) {
                    continue;
                }
                Object[] o = m.computeIfAbsent(e.getKey(), k -> new Object[] {0, new ArrayList<Component>()});
                o[0] = (Integer) o[0] + e.getValue();
                ((List<Component>) o[1]).add(waarvoor);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu) {
            for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.loket_uitleg"), x1 - x0)) {
                ui.tekst(s, x0, y0, Teken.ZACHT);
                y0 += 9;
            }
            y0 += 4;
            Map<String, Object[]> nodig = nodig(b);
            if (nodig.isEmpty()) {
                ui.tekst(Component.translatable("erfenis.ui.loket_niks"), x0, y0 + 4, Teken.TEKST);
                return;
            }
            var inv = Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory();
            ui.g.enableScissor(x0, y0, x1, y1);
            int y = y0 - scroll;
            for (Map.Entry<String, Object[]> e : nodig.entrySet()) {
                Item it = Goed.item(e.getKey());
                int need = (Integer) e.getValue()[0], heeft = b.loket.getOrDefault(e.getKey(), 0);
                int zakken = 0;
                if (inv != null) {
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        if (inv.getItem(i).is(it)) {
                            zakken += inv.getItem(i).getCount();
                        }
                    }
                }
                if (y + 18 > y0 && y < y1) {
                    boolean klaar = heeft >= need;
                    Teken.rond(ui.g, x0, y, x1, y + 18, klaar ? 0xFFE3F4E8 : 0xFFF6EEF2, 2);
                    ui.item(Ui.item(e.getKey()), x0 + 1, y + 1);
                    ui.passend(Component.translatable(it.getDescriptionId()), x0 + 20, y + 5, 120, Teken.DONKER);
                    ui.tekst(heeft + "/" + need, x0 + 148, y + 5, klaar ? Teken.GROEN_DONKER : Teken.ROOD);
                    ui.tekst(Component.translatable("erfenis.ui.in_zakken", zakken), x0 + 192, y + 5, Teken.ZACHT);
                    int geef = Math.min(zakken, Math.max(0, need - heeft));
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.translatable("erfenis.ui.nodig_voor").withStyle(ChatFormatting.BOLD));
                    tip.addAll((List<Component>) e.getValue()[1]);
                    String id = e.getKey();
                    ui.knop(Component.translatable("erfenis.ui.inleveren"), x1 - 62, y + 2, x1 - 2, y + 16, geef > 0, Teken.GROEN,
                            () -> ErfenisClient.stuur(new Actie(Actie.LOKET, 0, geef, 0, 0, 0, 0, id)), tip);
                }
                y += 20;
            }
            ui.g.disableScissor();
            scroll = Math.max(0, Math.min(scroll, nodig.size() * 20 - (y1 - y0)));
        }
    }

    // =====================================================================================================================
    // the Bouwkeet block choice
    // =====================================================================================================================

    static final class Bouwkeet extends Venster {
        private final int gebouw;
        @Nullable
        private Set<String> keuze;

        Bouwkeet(int gebouw) {
            this.gebouw = gebouw;
        }

        @Override
        int breed() {
            return 380;
        }

        @Override
        int hoog() {
            return 270;
        }

        @Override
        Component titel() {
            return Component.translatable("erfenis.venster.bouwkeet");
        }

        @Override
        void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu) {
            Gebouw g = b.gebouw(gebouw);
            if (g == null) {
                return;
            }
            if (keuze == null) {
                keuze = new LinkedHashSet<>(g.selectie);
            }
            List<String> mag = nl.juiced.guhs.feature.erfenis.Bouwkeet.beschikbaar(g, b);
            for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.bouwkeet_uitleg"), x1 - x0)) {
                ui.tekst(s, x0, y0, Teken.ZACHT);
                y0 += 9;
            }
            y0 += 3;
            int onder = y1 - 20;
            ui.g.enableScissor(x0, y0, x1, onder);
            int y = y0 - scroll;
            List<List<String>> sets = nl.juiced.guhs.feature.erfenis.Bouwkeet.sets();
            int kol = (x1 - x0) / 19;
            for (int s = 0; s < sets.size(); s++) {
                List<String> set = sets.get(s);
                boolean open = !set.isEmpty() && mag.contains(set.get(0));
                if (y + 10 > y0 && y < onder) {
                    Component kop = Component.translatable("erfenis.bouwkeet.set." + (s + 1)).withStyle(ChatFormatting.BOLD);
                    ui.tekst(open ? kop : kop.copy().append(" ").append(Component.translatable(s >= 3 ? "erfenis.ui.set_vk" : "erfenis.ui.set_level", 2 * s + 1)
                            .withStyle(ChatFormatting.ITALIC)), x0, y, open ? Teken.DONKER : Teken.ZACHT);
                }
                y += 11;
                for (int i = 0; i < set.size(); i++) {
                    String id = set.get(i);
                    int cx = x0 + i % kol * 19, cy = y + i / kol * 19;
                    if (cy + 18 > y0 && cy < onder) {
                        boolean aan = keuze.contains(id);
                        Teken.rond(ui.g, cx, cy, cx + 18, cy + 18, aan ? Teken.GOUD : ui.op(cx, cy, cx + 18, cy + 18) && open ? Teken.ROZE_DONKER : 0xFFE9D6DF, 2);
                        Teken.rond(ui.g, cx + 1, cy + 1, cx + 17, cy + 17, open ? Teken.CREME_VOL : 0xFFD9D0D4, 2);
                        ui.item(Ui.item(id), cx + 1, cy + 1);
                        if (!open) {
                            ui.g.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x90D9D0D4);
                        }
                        Item it = Goed.item(id);
                        ui.klik(cx, cy, cx + 18, cy + 18, open ? () -> {
                            if (!keuze.remove(id)) {
                                keuze.add(id);
                            }
                        } : null, it == null ? null : List.of(Component.translatable(it.getDescriptionId())));
                    }
                }
                y += ((set.size() + kol - 1) / kol) * 19 + 4;
            }
            ui.g.disableScissor();
            scroll = Math.max(0, Math.min(scroll, y + scroll - onder));
            ui.tekst(Component.translatable("erfenis.ui.gekozen", keuze.size()), x0, y1 - 14, Teken.TEKST);
            ui.knop(Component.translatable("erfenis.ui.alles_uit"), x1 - 170, y1 - 16, x1 - 92, y1, !keuze.isEmpty(), Teken.ROZE_DONKER, keuze::clear, null);
            Set<String> k = keuze;
            ui.knop(Component.translatable("erfenis.ui.opslaan"), x1 - 86, y1 - 16, x1, y1, !keuze.isEmpty(), Teken.GROEN,
                    () -> ErfenisClient.stuur(new Actie(Actie.SELECTIE, gebouw, 0, 0, 0, 0, 0, String.join(",", k))),
                    keuze.isEmpty() ? List.of(Component.translatable("erfenis.fout.selectie_leeg")) : null);
        }
    }

    // =====================================================================================================================
    // the Guhpension
    // =====================================================================================================================

    static final class Pension extends Venster {
        private int scrollRechts;
        private final Map<UUID, LivingEntity> poppen = new HashMap<>();

        @Override
        int breed() {
            return 420;
        }

        @Override
        int hoog() {
            return 260;
        }

        @Override
        Component titel() {
            return Component.translatable("erfenis.venster.pension");
        }

        @Override
        void scrol(double stappen) {
            OverzichtScherm s = OverzichtScherm.huidig();
            if (s != null && s.muisX() > s.width / 2) {
                scrollRechts = (int) Math.max(0, scrollRechts - stappen * 20);
            } else {
                super.scrol(stappen);
            }
        }

        private LivingEntity pop(UUID id, CompoundTag looks) {
            return poppen.computeIfAbsent(id, k -> GuhPop.van(looks));
        }

        @Override
        void inhoud(Ui ui, Base b, int x0, int y0, int x1, int y1, long nu) {
            CompoundTag data = ErfenisClient.pension;
            if (data == null) {
                ui.tekst(Component.translatable("erfenis.ui.laden"), x0, y0, Teken.ZACHT);
                return;
            }
            int mid = (x0 + x1) / 2;
            int plekken = data.getIntOr("Plekken", 0);
            List<CompoundTag> thuis = lijst(data, "Thuis"), loges = lijst(data, "Loges");
            ui.tekst(Component.translatable("erfenis.ui.jouw_guhs", thuis.size()).withStyle(ChatFormatting.BOLD), x0, y0, Teken.DONKER);
            ui.tekst(Component.translatable("erfenis.ui.logeren_hier", loges.size(), plekken).withStyle(ChatFormatting.BOLD), mid + 6, y0, Teken.DONKER);
            int ly = y0 + 12;
            // left: your guhs
            ui.g.enableScissor(x0, ly, mid - 2, y1);
            int y = ly - scroll;
            for (CompoundTag t : thuis) {
                if (y + 26 > ly && y < y1) {
                    UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
                    String niet = t.getStringOr("Niet", "");
                    Teken.rond(ui.g, x0, y, mid - 4, y + 26, 0xFFF6EEF2, 2);
                    if (id != null) {
                        GuhPop.teken(ui.g, x0 + 1, y + 1, x0 + 25, y + 25, pop(id, t.getCompoundOrEmpty("Looks")), 200f, 0f);
                    }
                    ui.passend(Tekst.get(t, "Naam"), x0 + 28, y + 3, mid - x0 - 96, Teken.DONKER);
                    ui.passend(Tekst.get(t, "Plek"), x0 + 28, y + 14, mid - x0 - 96, Teken.ZACHT);
                    boolean kan = niet.isEmpty() && loges.size() < plekken && id != null;
                    List<Component> tip = !niet.isEmpty() ? List.of(Component.translatable("erfenis.pension.niet." + niet))
                            : loges.size() >= plekken ? List.of(Component.translatable("erfenis.fout.pension_vol", plekken)) : null;
                    ui.knop(Component.translatable("erfenis.ui.logeren"), mid - 64, y + 6, mid - 6, y + 20, kan, Teken.GROEN,
                            () -> ErfenisClient.stuur(new Actie(Actie.PENSION, 0, Actie.PENSION_STUUR, 0, 0, 0, 0, id.toString())), tip);
                }
                y += 28;
            }
            if (thuis.isEmpty()) {
                for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.geen_guhs"), mid - x0 - 8)) {
                    ui.tekst(s, x0, y, Teken.ZACHT);
                    y += 9;
                }
            }
            ui.g.disableScissor();
            scroll = Math.max(0, Math.min(scroll, thuis.size() * 28 - (y1 - ly)));
            // right: staying here
            ui.g.enableScissor(mid + 4, ly, x1, y1);
            y = ly - scrollRechts;
            List<Gebouw> helpbaar = new ArrayList<>();
            for (Gebouw g : b.gebouwen) {
                if (g.type.maakt() && g.level > 0) {
                    helpbaar.add(g);
                }
            }
            for (CompoundTag t : loges) {
                if (y + 40 > ly && y < y1) {
                    UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
                    Teken.rond(ui.g, mid + 6, y, x1, y + 40, 0xFFFFF0F6, 2);
                    if (id != null) {
                        GuhPop.teken(ui.g, mid + 7, y + 2, mid + 37, y + 38, pop(id, t.getCompoundOrEmpty("Looks")), 200f, 0f);
                    }
                    ui.passend(Tekst.get(t, "Naam"), mid + 40, y + 3, x1 - mid - 110, Teken.DONKER);
                    int geb = huidigGebouw(b, id, t.getIntOr("Gebouw", -1));
                    Gebouw hg = b.gebouw(geb);
                    Component helpt = hg == null ? Component.translatable("erfenis.ui.helpt_niks")
                            : Component.translatable("erfenis.ui.helpt", Component.translatable(hg.type.naamKey()));
                    int volgende = volgendGebouw(b, helpbaar, geb, id);
                    ui.knop(helpt, mid + 40, y + 20, x1 - 70, y + 34, id != null, 0xFFB98AA6,
                            () -> ErfenisClient.stuur(new Actie(Actie.PENSION, volgende, Actie.PENSION_HELPT, 0, 0, 0, 0, id.toString())),
                            List.of(Component.translatable("erfenis.ui.helpt_uitleg")));
                    ui.knop(Component.translatable("erfenis.ui.naar_huis_guh"), x1 - 66, y + 20, x1 - 4, y + 34, id != null, Teken.ROZE_DONKER,
                            () -> ErfenisClient.stuur(new Actie(Actie.PENSION, 0, Actie.PENSION_HAAL, 0, 0, 0, 0, id.toString())), null);
                }
                y += 42;
            }
            if (loges.isEmpty()) {
                for (FormattedCharSequence s : ui.font.split(Component.translatable("erfenis.ui.pension_leeg"), x1 - mid - 10)) {
                    ui.tekst(s, mid + 6, y, Teken.ZACHT);
                    y += 9;
                }
            }
            ui.g.disableScissor();
            scrollRechts = Math.max(0, Math.min(scrollRechts, loges.size() * 42 - (y1 - ly)));
        }

        /** The base's own copy (it changes at once with every sync) over the window's data. */
        private static int huidigGebouw(Base b, @Nullable UUID id, int uitData) {
            for (Base.Loge l : b.loges) {
                if (l.id.equals(id)) {
                    return l.gebouw;
                }
            }
            return uitData;
        }

        /** The next building this logé could help (max 2 per building), after the current one; -1 at the end. */
        private static int volgendGebouw(Base b, List<Gebouw> helpbaar, int nu, @Nullable UUID id) {
            int start = -1;
            for (int i = 0; i < helpbaar.size(); i++) {
                if (helpbaar.get(i).id == nu) {
                    start = i;
                }
            }
            for (int i = start + 1; i < helpbaar.size(); i++) {
                Gebouw g = helpbaar.get(i);
                if (b.logesBij(g.id) < 2) {
                    return g.id;
                }
            }
            return -1;
        }

        private static List<CompoundTag> lijst(CompoundTag data, String key) {
            List<CompoundTag> l = new ArrayList<>();
            for (Tag e : data.getListOrEmpty(key)) {
                if (e instanceof CompoundTag c) {
                    l.add(c);
                }
            }
            return l;
        }
    }
}
