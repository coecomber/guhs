package nl.juiced.guhs.feature.safari.camera.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.camera.CameraPayloads;
import nl.juiced.guhs.feature.safari.camera.Foto;
import nl.juiced.guhs.feature.safari.camera.Missies;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.safari.kern.SafariSync;

/**
 * The Guhdex tab "Safari" (S1, design 1.7): your Safari-dex.
 * <ul>
 *   <li><b>Dieren</b>: every park species in zone order as a little picture (the best photo's variant) with its stars, "?"
 *   for the ones you have no photo of yet (the secret gouden dikdik only shows once you have it). Click one for its page
 *   on the right: the animal big (drag to turn it), its zone, the stars and the caption of your best photo, the zone's
 *   badge, and "Afdrukken" (a photo card for a sheet of paper).</li>
 *   <li><b>Missies</b>: today's three photo missions, done or not, and when new ones come.</li>
 *   <li><b>Geheimpjes</b>: the eight geheimpjes of the park (found ones by name).</li>
 * </ul>
 * Everything comes from the synced safari state ({@link SafariSync#stand}); the species list from {@link ParkData}.
 */
public final class SafariDexTab {
    private enum Weergave { DIEREN, MISSIES, GEHEIMPJES }

    private static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A;
    private static final int GEHEIMPJES = 8, CEL = 30;
    private static Weergave weergave = Weergave.DIEREN;
    @Nullable
    private static String gekozen;
    private static int rijScroll;
    private static float draai = 25;
    /** Bumped when a photo result or a new state arrives, so open pages redraw their pictures. */
    private static int versie;

    private final Map<String, SafariDier> poppen = new HashMap<>();
    private int poppenVersie = -1;
    private Font font;
    private int left, top, w, h;
    private int gx, gy, gw, gh, px1, px2;
    private boolean slepen;
    private double sleepX;

    public static void vernieuwd() {
        versie++;
    }

    public void init(Font font, int left, int top, int w, int h, Consumer<AbstractWidget> knop, Runnable herbouw) {
        this.font = font;
        this.left = left;
        this.top = top;
        this.w = w;
        this.h = h;
        if (poppenVersie != versie) {
            poppen.clear();
            poppenVersie = versie;
        }
        int bx = left + 8;
        for (Weergave v : Weergave.values()) {
            Component naam = Component.translatable("gui.guhs.safari.dex.weergave." + v.name().toLowerCase(java.util.Locale.ROOT));
            int bw = Math.max(54, font.width(naam) + 10);
            Button b = Button.builder(naam, btn -> {
                weergave = v;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2f));
                herbouw.run();
            }).bounds(bx, top + 28, bw, 13).build();
            b.active = weergave != v;
            knop.accept(b);
            bx += bw + 3;
        }
        gx = left + 8;
        gy = top + 46;
        gw = (int) ((w - 16) * 0.52f) / CEL * CEL;
        gh = h - 46 - 8;
        px1 = gx + gw + 8;
        px2 = left + w - 8;
        if (weergave == Weergave.DIEREN) {
            List<String> lijst = soorten();
            if (gekozen == null || !lijst.contains(gekozen)) {
                gekozen = lijst.stream().filter(s -> sterren(s) > 0).findFirst().orElse(lijst.isEmpty() ? null : lijst.get(0));
            }
            rijScroll = Math.max(0, Math.min(rijScroll, maxScroll()));
            if (gekozen != null) {
                String soort = gekozen;
                Button print = Button.builder(Component.translatable("gui.guhs.safari.dex.afdrukken"),
                        b -> ClientPacketDistributor.sendToServer(new CameraPayloads.Afdrukken(soort))).bounds(px1, top + h - 22, px2 - px1, 14).build();
                print.active = sterren(soort) > 0;
                print.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.safari.dex.afdrukken.tip")));
                knop.accept(print);
            }
        }
    }

    // =====================================================================================================================
    // data (from the synced safari state)
    // =====================================================================================================================

    private static CompoundTag stand() {
        return SafariSync.stand;
    }

    static CompoundTag dexRegel(String soort) {
        return stand().getCompoundOrEmpty("Dex").getCompoundOrEmpty(soort);
    }

    static int sterren(String soort) {
        return dexRegel(soort).getIntOr("Sterren", 0);
    }

    /** The dex species in zone order (the park's zones, then the species' order in soorten.json); the gouden dikdik last,
     *  only once photographed. */
    static List<String> soorten() {
        ParkData d = ParkData.get();
        List<String> uit = new ArrayList<>();
        for (String zone : d.zones) {
            for (ParkData.Soort s : d.soorten.values()) {
                if (zone.equals(s.zone()) && SafariSpeler.telt(s.naam())) {
                    uit.add(s.naam());
                }
            }
        }
        if (sterren("gouden_dikdik") > 0) {
            uit.add("gouden_dikdik");
        }
        return uit;
    }

    static int geteld() {
        int n = 0;
        for (String s : soorten()) {
            if (SafariSpeler.telt(s) && sterren(s) > 0) {
                n++;
            }
        }
        return n;
    }

    static int totaal() {
        return (int) ParkData.get().soorten.keySet().stream().filter(SafariSpeler::telt).count();
    }

    private int kolommen() {
        return Math.max(1, gw / CEL);
    }

    private int rijenZichtbaar() {
        return Math.max(1, gh / CEL);
    }

    private int maxScroll() {
        int rijen = (soorten().size() + kolommen() - 1) / kolommen();
        return Math.max(0, rijen - rijenZichtbaar());
    }

    @Nullable
    private SafariDier pop(String soort) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return poppen.computeIfAbsent(soort, s -> {
            SafariDier d = SafariFeature.DIER.get().create(mc.level, EntitySpawnReason.TRIGGERED);
            if (d != null) {
                String variant = dexRegel(s).getCompoundOrEmpty("Foto").getStringOr("Variant", "vrouw");
                d.zet(s, variant.isEmpty() ? "vrouw" : variant, BlockPos.ZERO, 8, -1, 0, 0);
                d.refreshDimensions();
            }
            return d;
        });
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // the counter and the coins at the right of the button row
        Component kop = Component.translatable("gui.guhs.safari.dex.teller", geteld(), totaal(), stand().getIntOr("Munten", 0));
        GidsTekst.passend(g, kop, left + w - 8, top + 31, 160, 0.75f, ROZE, true);
        g.fill(left + 8, top + 43, left + w - 8, top + 44, 0x60F7B6CB);
        switch (weergave) {
            case MISSIES -> tekenMissies(g);
            case GEHEIMPJES -> tekenGeheimpjes(g);
            default -> tekenDieren(g, mouseX, mouseY);
        }
    }

    private void tekenDieren(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<String> lijst = soorten();
        int kol = kolommen(), rijen = rijenZichtbaar();
        int start = rijScroll * kol;
        for (int i = start; i < Math.min(lijst.size(), start + kol * rijen); i++) {
            int k = i - start;
            int cx = gx + (k % kol) * CEL, cy = gy + (k / kol) * CEL;
            String soort = lijst.get(i);
            int st = sterren(soort);
            boolean hover = mouseX >= cx && mouseX < cx + CEL - 2 && mouseY >= cy && mouseY < cy + CEL - 2;
            boolean kies = soort.equals(gekozen);
            int kleur = kies ? 0xFFF7B6CB : st > 0 ? (hover ? 0x70F7B6CB : 0x40F7B6CB) : (hover ? 0xFF4A2A3A : 0xFF2A1420);
            g.fill(cx, cy, cx + CEL - 2, cy + CEL - 2, kleur);
            if (st > 0) {
                SafariDier pop = pop(soort);
                if (pop != null) {
                    GuhPop.teken(g, cx + 1, cy, cx + CEL - 3, cy + CEL - 7, pop, 25, -8);
                }
                GidsTekst.schaal(g, Component.literal("★".repeat(st)).withStyle(ChatFormatting.GOLD), cx + (CEL - 2) / 2 - Math.round(font.width("★".repeat(st)) * 0.25f),
                        cy + CEL - 8, 0.5f, 0xFFD89A10, false);
            } else {
                g.centeredText(font, "?", cx + (CEL - 2) / 2, cy + (CEL - 2) / 2 - 4, 0xFF8A6A7A);
            }
        }
        int max = maxScroll();
        if (max > 0) {   // a thin scrollbar at the right of the grid
            int bx = gx + gw + 1, bh = gh;
            g.fill(bx, gy, bx + 3, gy + bh, 0x30D27A9C);
            int dh = Math.max(10, bh * rijen / (rijen + max));
            int dy = gy + (bh - dh) * rijScroll / max;
            g.fill(bx, dy, bx + 3, dy + dh, 0xFFD27A9C);
        }
        tekenPagina(g, mouseX, mouseY);
    }

    /** The page of the chosen species, right of the grid. */
    private void tekenPagina(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (gekozen == null) {
            return;
        }
        String soort = gekozen;
        int st = sterren(soort);
        int y = gy;
        int bw = px2 - px1;
        int beeldH = Math.max(50, (int) ((h - 46) * 0.42f));
        g.fill(px1, y, px2, y + beeldH, st > 0 ? 0x30F7B6CB : 0xFF2A1420);
        if (st > 0) {
            SafariDier pop = pop(soort);
            if (pop != null) {
                GuhPop.teken(g, px1, y, px2, y + beeldH, pop, draai, -8);
            }
        } else {
            g.centeredText(font, "???", (px1 + px2) / 2, y + beeldH / 2 - 4, 0xFF8A6A7A);
        }
        y += beeldH + 4;
        Component naam = st > 0 ? Component.translatable("entity.guhs.safari." + soort).withStyle(ChatFormatting.BOLD) : Component.literal("???");
        y += GidsTekst.passend(g, naam, px1, y, bw, 1f, DONKER, false) + 1;
        ParkData.Soort info = ParkData.get().soort(soort);
        String zone = info == null || info.zone() == null ? "" : info.zone();
        if (!zone.isEmpty()) {
            y += GidsTekst.passend(g, Component.translatable("biome.guhs.safari_" + zone), px1, y, bw, 0.75f, LICHT, false) + 2;
        }
        Component ster = Component.literal("★".repeat(st) + "☆".repeat(3 - st)).withStyle(ChatFormatting.GOLD);
        y += GidsTekst.passend(g, ster, px1, y, bw, 1f, 0xFFD89A10, false) + 2;
        int onder = top + h - 26;
        if (st > 0) {
            CompoundTag foto = dexRegel(soort).getCompoundOrEmpty("Foto");
            y += GidsTekst.alinea(g, Foto.onderschrift(foto), px1, y, bw, 0.75f, TEKST) + 3;
        } else {
            y += GidsTekst.alinea(g, Component.translatable("gui.guhs.safari.dex.nog_niet"), px1, y, bw, 0.75f, 0xFF9A8090) + 3;
        }
        if (!zone.isEmpty() && !"entree".equals(zone) && y < onder - 8) {
            GidsTekst.alinea(g, badge(zone), px1, y, bw, 0.625f, ROZE);
        }
    }

    /** The zone badge line: claimed, ready to claim, or how many species of the zone still need a photo. */
    private Component badge(String zone) {
        if (stand().getCompoundOrEmpty("Badges").getBooleanOr(zone, false)) {
            return Component.translatable("gui.guhs.safari.dex.badge.ja", Component.translatable("biome.guhs.safari_" + zone));
        }
        int nog = 0;
        for (ParkData.Soort s : ParkData.get().soorten.values()) {
            if (zone.equals(s.zone()) && SafariSpeler.telt(s.naam()) && sterren(s.naam()) == 0) {
                nog++;
            }
        }
        return nog == 0 ? Component.translatable("gui.guhs.safari.dex.badge.klaar")
                : Component.translatable("gui.guhs.safari.dex.badge.nog", nog, Component.translatable("biome.guhs.safari_" + zone));
    }

    private void tekenMissies(GuiGraphicsExtractor g) {
        long dag = stand().getLongOr("DagNr", Klok.dag());
        List<Missies.Missie> missies = Missies.van(dag);
        CompoundTag vandaag = stand().getLongOr("DagNr", Long.MIN_VALUE) == dag ? stand().getCompoundOrEmpty("Dag") : new CompoundTag();
        int y = gy + 2, x = left + 12, bw = w - 24;
        y += GidsTekst.passend(g, Component.translatable("gui.guhs.safari.dex.missies.kop").withStyle(ChatFormatting.BOLD), x, y, bw, 1f, DONKER, false) + 4;
        for (int i = 0; i < missies.size(); i++) {
            boolean klaar = vandaag.getIntOr("missie" + i, 0) > 0;
            g.fill(x - 2, y - 2, x + bw + 2, y + 18, klaar ? 0x3868D88A : 0x22F7B6CB);
            Component regel = Component.literal(klaar ? "✔ " : "○ ").withStyle(klaar ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY)
                    .append(missies.get(i).tekst());
            GidsTekst.passend(g, regel, x + 2, y + 1, bw - 60, 1f, klaar ? 0xFF2A7A48 : DONKER, false);
            GidsTekst.passend(g, Component.translatable("gui.guhs.safari.dex.missies.loon", Missies.MUNTEN), x + bw - 2, y + 3, 56, 0.75f, 0xFFD89A10, true);
            y += 22;
        }
        long ms = Math.max(0, Klok.totMorgen());
        long uur = ms / Klok.UUR, min = (ms % Klok.UUR) / 60_000L;
        y += 2;
        y += GidsTekst.passend(g, Component.translatable("gui.guhs.safari.dex.missies.nieuw", uur, min), x, y, bw, 0.75f, LICHT, false) + 8;
        g.fill(x, y - 4, x + bw, y - 3, 0x60F7B6CB);
        y += GidsTekst.passend(g, Component.translatable("gui.guhs.safari.dex.sterren.kop").withStyle(ChatFormatting.BOLD), x, y, bw, 0.875f, ROZE, false) + 3;
        GidsTekst.alinea(g, Component.translatable("gui.guhs.safari.dex.sterren.uitleg"), x, y, bw, 0.75f, TEKST);
    }

    private void tekenGeheimpjes(GuiGraphicsExtractor g) {
        List<String> gevonden = new ArrayList<>(stand().getCompoundOrEmpty("Geheimpjes").keySet());
        int x = left + 12, y = gy + 2, bw = w - 24;
        y += GidsTekst.passend(g, Component.translatable("gui.guhs.safari.dex.geheimpjes.kop", gevonden.size(), GEHEIMPJES).withStyle(ChatFormatting.BOLD),
                x, y, bw, 1f, DONKER, false) + 4;
        int kol = 2, cw = (bw - 6) / kol, rij = Math.min(24, Math.max(16, (top + h - 30 - y) / (GEHEIMPJES / kol)));
        for (int i = 0; i < GEHEIMPJES; i++) {
            int cx = x + (i % kol) * (cw + 6), cy = y + (i / kol) * rij;
            boolean heeft = i < gevonden.size();
            g.fill(cx, cy, cx + cw, cy + rij - 3, heeft ? 0x40F7B6CB : 0xFF2A1420);
            Component naam;
            if (heeft) {
                String key = "gui.guhs.safari.geheimpje." + gevonden.get(i);
                naam = I18n.exists(key) ? Component.translatable(key) : Component.literal(gevonden.get(i));
            } else {
                naam = Component.literal("???");
            }
            GidsTekst.passend(g, Component.literal(heeft ? "✿ " : "").append(naam), cx + 4, cy + (rij - 3) / 2 - 3, cw - 8, 0.875f,
                    heeft ? DONKER : 0xFF8A6A7A, false);
        }
        GidsTekst.alinea(g, Component.translatable("gui.guhs.safari.dex.geheimpjes.uitleg"), x, top + h - 22, bw, 0.625f, LICHT);
    }

    // =====================================================================================================================
    // input
    // =====================================================================================================================

    public List<Component> tip(double mouseX, double mouseY) {
        if (weergave != Weergave.DIEREN) {
            return List.of();
        }
        String s = onder(mouseX, mouseY);
        if (s == null) {
            return List.of();
        }
        int st = sterren(s);
        if (st == 0) {
            return List.of(Component.literal("???"), Component.translatable("gui.guhs.safari.dex.nog_niet").withStyle(ChatFormatting.GRAY));
        }
        ParkData.Soort info = ParkData.get().soort(s);
        List<Component> uit = new ArrayList<>();
        uit.add(Component.translatable("entity.guhs.safari." + s).withStyle(ChatFormatting.BOLD));
        uit.add(Component.literal("★".repeat(st) + "☆".repeat(3 - st)).withStyle(ChatFormatting.GOLD));
        if (info != null && info.zone() != null && !info.zone().isEmpty()) {
            uit.add(Component.translatable("biome.guhs.safari_" + info.zone()).withStyle(ChatFormatting.GRAY));
        }
        return uit;
    }

    @Nullable
    private String onder(double mouseX, double mouseY) {
        if (mouseX < gx || mouseY < gy || mouseX >= gx + gw || mouseY >= gy + rijenZichtbaar() * CEL) {
            return null;
        }
        int k = (int) ((mouseY - gy) / CEL) * kolommen() + (int) ((mouseX - gx) / CEL);
        int i = rijScroll * kolommen() + k;
        List<String> lijst = soorten();
        return i >= 0 && i < lijst.size() ? lijst.get(i) : null;
    }

    public boolean klik(double mouseX, double mouseY, int button, Runnable herbouw) {
        if (weergave != Weergave.DIEREN || button != 0) {
            return false;
        }
        String s = onder(mouseX, mouseY);
        if (s != null) {
            gekozen = s;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.4f));
            herbouw.run();
            return true;
        }
        if (mouseX >= px1 && mouseX < px2 && mouseY >= gy && mouseY < gy + (h - 46) * 0.42f) {
            slepen = true;
            sleepX = mouseX;
            return true;
        }
        return false;
    }

    public boolean sleep(double mouseX) {
        if (!slepen) {
            return false;
        }
        draai += (float) (mouseX - sleepX) * 2f;
        sleepX = mouseX;
        return true;
    }

    public void los() {
        slepen = false;
    }

    public boolean wiel(double mouseX, double mouseY, double dy) {
        if (weergave != Weergave.DIEREN || mouseX < gx || mouseX >= gx + gw + 4 || mouseY < gy || mouseY >= gy + gh || dy == 0) {
            return false;
        }
        rijScroll = Math.max(0, Math.min(maxScroll(), rijScroll + (dy > 0 ? -1 : 1)));
        return true;
    }
}
