package nl.juiced.guhs.feature.knabbelvelden.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.knabbelvelden.KnabbelPayloads;
import nl.juiced.guhs.feature.knabbelvelden.Pensioen;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Knabbelportaal's big screen (about 80 % of the window): two grids of portraits side by side, "In je Guhdex" and
 * "In de Knabbelvelden" (each card: the guh itself, its number, name and fur), a search box and a sort button (number,
 * name, fur, date), click cards to select several, then → sends them into retirement and ← brings them home (as a
 * picked-up guh). Busy guhs (in a huisje, in a Guhrad...) are marked and you are asked first. On the right the last
 * card you clicked: name, personality, clothes, since when. At the bottom "Naar de Knabbelvelden" (or, there,
 * "Terug naar huis"). Soft pink and cheese yellow; closes when you walk away from the portal.
 */
public class KnabbelScherm extends Screen {
    static final int PANEEL = 0xF6FFF8F0, RAND = 0xFFF2C14E, KOP = 0xFFFFE08A, ROZE = 0xFFE0629E, DONKER = 0xFF5A2A48, GRIJS = 0xFF9A7890,
            ORANJE = 0xFFD9822B, VAK = 0xFFFFFBFD, VAK_RAND = 0xFFF4C6DC, GEKOZEN = 0xFFFFD6EA;
    private static final int CW = 58, CH = 72;

    enum Sorteer { NUMMER, NAAM, VACHT, DATUM }

    /** One card. */
    record Kaart(UUID id, int nr, Component naam, CompoundTag looks, long sinds, String bezig, String niet, boolean onderweg, boolean velden,
                 String soort, Component plek) {
        /** 1.6.2: a maatje of "Mijn andere vadsjes" (soort = its kind), not a guh. */
        boolean maatje() {
            return !"guh".equals(soort);
        }

        /** A guh's fur, a maatje's kind. */
        String vacht() {
            return maatje() ? nl.juiced.guhs.feature.band.client.AndereVadsjesTab.soortNaam(soort).getString()
                    : GuhVariant.byId(looks.getStringOr("Variant", "")).displayName().getString();
        }
    }

    /** One of the two grids. */
    private final class Kolom {
        final boolean velden;
        final List<Kaart> alle = new ArrayList<>(), zichtbaar = new ArrayList<>();
        final Set<UUID> gekozen = new LinkedHashSet<>();
        int x, y, w, h, scroll;

        Kolom(boolean velden) {
            this.velden = velden;
        }

        int perRij() {
            return Math.max(1, (w - 4) / CW);
        }

        int rijen() {
            return Math.max(1, (h - 2) / CH);
        }

        int maxScroll() {
            return Math.max(0, (zichtbaar.size() + perRij() - 1) / perRij() - rijen());
        }

        void filter() {
            String z = zoek == null ? "" : zoek.getValue().trim().toLowerCase(Locale.ROOT);
            zichtbaar.clear();
            for (Kaart k : alle) {
                if (!soortFilter.isEmpty() && !soortFilter.equals(k.soort())) {
                    continue;   // 1.6.2: only one kind (guhs, pieppiepmuisjes...)
                }
                if (z.isEmpty() || k.naam().getString().toLowerCase(Locale.ROOT).contains(z) || k.vacht().toLowerCase(Locale.ROOT).contains(z)
                        || ("#" + k.nr()).equals(z) || String.valueOf(k.nr()).equals(z)) {
                    zichtbaar.add(k);
                }
            }
            Comparator<Kaart> op = switch (sorteer) {
                case NUMMER -> Comparator.comparingInt(Kaart::nr);
                case NAAM -> Comparator.comparing((Kaart k) -> k.naam().getString().toLowerCase(Locale.ROOT)).thenComparingInt(Kaart::nr);
                case VACHT -> Comparator.comparing(Kaart::vacht).thenComparingInt(Kaart::nr);
                case DATUM -> Comparator.comparingLong(Kaart::sinds).reversed().thenComparingInt(Kaart::nr);
            };
            zichtbaar.sort(op);
            gekozen.removeIf(id -> alle.stream().noneMatch(k -> k.id().equals(id)));
            scroll = Mth.clamp(scroll, 0, maxScroll());
        }

        @Nullable
        Kaart onder(double mx, double my) {
            if (mx < x || mx >= x + w || my < y || my >= y + h) {
                return null;
            }
            int per = perRij(), off = (w - per * CW) / 2;
            int c = (int) ((mx - x - off) / CW), r = (int) ((my - y - 1) / CH);
            if (c < 0 || c >= per || mx - x - off < 0) {
                return null;
            }
            int i = (scroll + r) * per + c;
            return i >= 0 && i < zichtbaar.size() && r < rijen() ? zichtbaar.get(i) : null;
        }
    }

    private CompoundTag data;
    private final Kolom thuis = new Kolom(false), velden = new Kolom(true);
    private Sorteer sorteer = Sorteer.NUMMER;
    /** 1.6.2: "" = every kind, else "guh" or a maatje's kind. */
    private String soortFilter = "";
    private Button soortKnop;
    private EditBox zoek;
    private Button stuurKnop, haalKnop, reisKnop, sorteerKnop, jaKnop, neeKnop;
    private int left, top, w, h, detailX, detailW, kolomY;
    @Nullable
    private Kaart focus;
    private final Map<UUID, GuhEntity> poppen = new HashMap<>();
    private Component melding;
    private int meldingTijd, tijd;
    private float draai = 25;
    /** The question "some of them are busy: send them anyway?" (null: no question open). */
    @Nullable
    private List<Kaart> vraag;

    public KnabbelScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.knabbelvelden.scherm.titel"));
        update(data);
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        lees(thuis, nieuw.getListOrEmpty("Thuis"), false);
        lees(velden, nieuw.getListOrEmpty("Velden"), true);
        if (nieuw.contains("Melding")) {
            melding = Tekst.get(nieuw, "Melding");
            meldingTijd = 120;
        }
        if (focus != null) {
            Kaart f = focus;
            focus = null;
            for (Kaart k : f.velden() ? velden.alle : thuis.alle) {
                if (k.id().equals(f.id())) {
                    focus = k;
                }
            }
        }
        vraag = null;
        filter();
    }

    private static void lees(Kolom kolom, List<Tag> lijst, boolean inVelden) {
        kolom.alle.clear();
        for (Tag raw : lijst) {
            CompoundTag t = (CompoundTag) raw;
            UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
            if (id != null) {
                kolom.alle.add(new Kaart(id, t.getIntOr("Nr", 0), Tekst.get(t, "Naam"), t.getCompoundOrEmpty("Looks"), t.getLongOr("Sinds", -1L),
                        t.getStringOr("Bezig", ""), t.getStringOr("Niet", ""), t.getBooleanOr("Onderweg", false), inVelden,
                        t.getStringOr("Soort", "guh"), t.contains("Plek") ? Tekst.get(t, "Plek") : Component.empty()));
            }
        }
    }

    private void filter() {
        thuis.filter();
        velden.filter();
        werkKnoppenBij();
    }

    private BlockPos portaal() {
        return BlockPos.of(data.getLongOr("Pos", 0L));
    }

    // --- layout --------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        w = Math.min(width - 8, Math.max(340, (int) (width * 0.8f)));
        h = Math.min(height - 8, Math.max(214, (int) (height * 0.8f)));
        left = (width - w) / 2;
        top = (height - h) / 2;
        detailW = Mth.clamp((int) (w * 0.22f), 110, 170);
        int mid = 30;
        int kolomW = (w - 20 - detailW - 8 - mid) / 2;
        kolomY = top + 52;
        int kolomH = h - (kolomY - top) - 12 - 34;
        thuis.x = left + 10;
        velden.x = thuis.x + kolomW + mid;
        thuis.w = velden.w = kolomW;
        thuis.y = velden.y = kolomY + 12;
        thuis.h = velden.h = kolomH - 12;
        detailX = velden.x + kolomW + 8;

        zoek = new EditBox(font, left + 10, top + 33, Math.min(150, kolomW - 4), 14, Component.translatable("gui.guhs.knabbelvelden.scherm.zoek"));
        zoek.setHint(Component.translatable("gui.guhs.knabbelvelden.scherm.zoek").withStyle(ChatFormatting.GRAY));
        zoek.setMaxLength(40);
        zoek.setResponder(s -> filter());
        addRenderableWidget(zoek);
        sorteerKnop = addRenderableWidget(Button.builder(sorteerTekst(), b -> {
            sorteer = Sorteer.values()[(sorteer.ordinal() + 1) % Sorteer.values().length];
            b.setMessage(sorteerTekst());
            filter();
        }).bounds(zoek.getX() + zoek.getWidth() + 6, top + 32, 110, 16).build());
        // 1.6.2: which kind: all, guhs, or one kind of "Mijn andere vadsjes" (only kinds you have)
        soortKnop = addRenderableWidget(Button.builder(soortTekst(), b -> {
            List<String> soorten = soorten();
            int i = soorten.indexOf(soortFilter);
            soortFilter = soorten.get((i + 1) % soorten.size());
            b.setMessage(soortTekst());
            filter();
        }).bounds(sorteerKnop.getX() + sorteerKnop.getWidth() + 4, top + 32, 120, 16).build());

        int mx = thuis.x + kolomW + (mid - 22) / 2, my = thuis.y + thuis.h / 2;
        stuurKnop = addRenderableWidget(Button.builder(Component.literal("→"), b -> stuur())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.knabbelvelden.scherm.stuur")))
                .bounds(mx, my - 24, 22, 20).build());
        haalKnop = addRenderableWidget(Button.builder(Component.literal("←"), b -> haal())
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.knabbelvelden.scherm.haal")))
                .bounds(mx, my + 4, 22, 20).build());

        boolean inVelden = data.getBooleanOr("InVelden", false);
        reisKnop = addRenderableWidget(Button.builder(Component.translatable(inVelden ? "gui.guhs.knabbelvelden.scherm.naar_huis"
                : "gui.guhs.knabbelvelden.scherm.naar_velden"), b -> reis(inVelden)).bounds(left + (w - 200) / 2, top + h - 28, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + 10, top + h - 28, 70, 20).build());

        int vx = left + (w - 240) / 2, vy = top + (h - 90) / 2;
        jaKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knabbelvelden.scherm.vraag.ja"), b -> {
            List<Kaart> v = vraag;
            vraag = null;
            if (v != null) {
                verstuur(v, true);
            }
            werkKnoppenBij();
        }).bounds(vx + 10, vy + 62, 105, 20).build());
        neeKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knabbelvelden.scherm.vraag.nee"), b -> {
            vraag = null;
            werkKnoppenBij();
        }).bounds(vx + 125, vy + 62, 105, 20).build());
        filter();
    }

    /** "" (all) first, then guhs, then every maatje kind you have here or at home. */
    private List<String> soorten() {
        List<String> uit = new ArrayList<>(List.of(""));
        java.util.TreeSet<String> rest = new java.util.TreeSet<>();
        for (Kolom k : List.of(thuis, velden)) {
            for (Kaart c : k.alle) {
                if (c.maatje()) {
                    rest.add(c.soort());
                } else if (!uit.contains("guh")) {
                    uit.add("guh");
                }
            }
        }
        uit.addAll(rest);
        if (!uit.contains(soortFilter)) {
            soortFilter = "";
        }
        return uit;
    }

    private Component soortTekst() {
        Component wat = soortFilter.isEmpty() ? Component.translatable("gui.guhs.knabbelvelden.scherm.soort.alles")
                : "guh".equals(soortFilter) ? Component.translatable("gui.guhs.knabbelvelden.scherm.soort.guh")
                : nl.juiced.guhs.feature.band.client.AndereVadsjesTab.soortNaam(soortFilter);
        return Component.translatable("gui.guhs.knabbelvelden.scherm.soort", wat);
    }

    private Component sorteerTekst() {
        return Component.translatable("gui.guhs.knabbelvelden.scherm.sorteer", Component.translatable("gui.guhs.knabbelvelden.sorteer."
                + sorteer.name().toLowerCase(Locale.ROOT)));
    }

    private void werkKnoppenBij() {
        if (stuurKnop == null) {
            return;
        }
        boolean v = vraag != null;
        stuurKnop.active = !v && thuis.gekozen.stream().anyMatch(id -> kaart(thuis, id) != null && kaart(thuis, id).niet().isEmpty());
        haalKnop.active = !v && !velden.gekozen.isEmpty();
        jaKnop.visible = neeKnop.visible = v;
        reisKnop.active = !v;
        sorteerKnop.active = !v;
        if (soortKnop != null) {
            soortKnop.active = !v;
        }
    }

    @Nullable
    private static Kaart kaart(Kolom k, UUID id) {
        for (Kaart c : k.alle) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }

    // --- actions -------------------------------------------------------------------------------------------------------

    private void stuur() {
        List<Kaart> kies = new ArrayList<>();
        for (UUID id : thuis.gekozen) {
            Kaart k = kaart(thuis, id);
            if (k != null && k.niet().isEmpty() && !k.onderweg()) {
                kies.add(k);
            }
        }
        if (kies.isEmpty()) {
            return;
        }
        if (kies.stream().anyMatch(k -> !k.bezig().isEmpty())) {
            vraag = kies;   // some are busy: ask first
            werkKnoppenBij();
            return;
        }
        verstuur(kies, false);
    }

    private void verstuur(List<Kaart> kies, boolean bevestigd) {
        List<UUID> ids = new ArrayList<>();
        for (Kaart k : kies) {
            ids.add(k.id());
        }
        thuis.gekozen.clear();
        ClientPacketDistributor.sendToServer(new KnabbelPayloads.Actie(portaal().asLong(), Pensioen.STUUR, ids, bevestigd));
    }

    private void haal() {
        if (velden.gekozen.isEmpty()) {
            return;
        }
        List<UUID> ids = new ArrayList<>(velden.gekozen);
        velden.gekozen.clear();
        ClientPacketDistributor.sendToServer(new KnabbelPayloads.Actie(portaal().asLong(), Pensioen.HAAL, ids, false));
    }

    private void reis(boolean naarHuis) {
        ClientPacketDistributor.sendToServer(new KnabbelPayloads.Actie(portaal().asLong(), naarHuis ? Pensioen.NAAR_HUIS : Pensioen.NAAR_VELDEN,
                List.of(), false));
        onClose();
    }

    // --- input ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (vraag == null) {
            for (Kolom k : List.of(thuis, velden)) {
                Kaart c = k.onder(event.x(), event.y());
                if (c != null) {
                    if (!k.gekozen.remove(c.id()) && (k.velden || c.niet().isEmpty())) {
                        k.gekozen.add(c.id());
                    }
                    focus = c;
                    werkKnoppenBij();
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 1.6f, 0.25f));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.x() >= detailX && event.x() < detailX + detailW && event.y() >= kolomY && event.y() < kolomY + 100) {
            draai += (float) dx * 2.5f;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (Kolom k : List.of(thuis, velden)) {
            if (mx >= k.x && mx < k.x + k.w && my >= k.y && my < k.y + k.h) {
                k.scroll = Mth.clamp(k.scroll - (int) Math.signum(sy), 0, k.maxScroll());
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (zoek != null && zoek.isFocused() && event.key() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            zoek.keyPressed(event);
            return true;   // (typing an "e" doesn't close anything)
        }
        return super.keyPressed(event);
    }

    // --- drawing -------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + w + 2, top + h + 2, RAND);
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFFFFE9A8);
        g.fill(left, top, left + w, top + h, PANEEL);
        g.fillGradient(left, top, left + w, top + 30, 0xFFFFD6EA, PANEEL);
        // little cheese holes and clouds along the bottom
        for (int i = 0; i * 34 + 20 < w; i++) {
            int cx = left + 18 + i * 34, cy = top + h - 3;
            g.fill(cx - 11, cy - 5, cx + 11, cy, 0x40FFFFFF);
            g.fill(cx - 6, cy - 9, cx + 6, cy - 5, 0x40FFFFFF);
        }
        Component titel = Component.literal("☁ ").append(title).append(" ☁").withStyle(ChatFormatting.BOLD);
        g.text(font, titel, left + (w - font.width(titel)) / 2, top + 7, ROZE, false);
        Component sub = Component.translatable("gui.guhs.knabbelvelden.scherm.sub");
        GidsTekst.passend(g, sub, left + (w - Math.min(font.width(sub), w - 20)) / 2, top + 19, w - 20, 1f, GRIJS, false);
        for (Kolom k : List.of(thuis, velden)) {
            Component kop = Component.translatable(k.velden ? "gui.guhs.knabbelvelden.scherm.velden" : "gui.guhs.knabbelvelden.scherm.thuis", k.alle.size());
            GidsTekst.passend(g, kop.copy().withStyle(ChatFormatting.BOLD), k.x + 2, kolomY + 1, k.w - 4, 1f, k.velden ? 0xFFC0901A : DONKER, false);
            g.fill(k.x - 1, k.y - 1, k.x + k.w + 1, k.y + k.h + 1, VAK_RAND);
            g.fill(k.x, k.y, k.x + k.w, k.y + k.h, k.velden ? 0xFFFFF8E2 : VAK);
        }
        g.fill(detailX - 1, kolomY - 1, detailX + detailW + 1, thuis.y + thuis.h + 1, VAK_RAND);
        g.fillGradient(detailX, kolomY, detailX + detailW, thuis.y + thuis.h, 0xFFE8F4FF, 0xFFFFE6F2);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Kaart over = null;
        for (Kolom k : List.of(thuis, velden)) {
            Kaart o = kolom(g, k, mouseX, mouseY);
            if (o != null) {
                over = o;
            }
        }
        detail(g);
        if (melding != null && meldingTijd > 0) {
            int mw = font.width(melding);
            int mx = Math.max(sorteerKnop.getX() + sorteerKnop.getWidth() + 8, left + w - 12 - mw);
            GidsTekst.passend(g, melding, mx, top + 36, left + w - 10 - mx, 1f, 0xFFB07A10, false);
        }
        if (vraag != null) {
            vraagPaneel(g);
            jaKnop.extractRenderState(g, mouseX, mouseY, partialTick);
            neeKnop.extractRenderState(g, mouseX, mouseY, partialTick);
        } else if (over != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(over.naam().copy().withStyle(ChatFormatting.BOLD));
            if (!over.niet().isEmpty()) {
                tip.add(Component.translatable("gui.guhs.knabbelvelden.niet." + over.niet()).withStyle(ChatFormatting.GOLD));
            } else if (!over.bezig().isEmpty()) {
                tip.add(Component.translatable("gui.guhs.knabbelvelden.bezig." + over.bezig()).withStyle(ChatFormatting.GOLD));
                if ("opgepakt".equals(over.bezig()) && !over.plek().getString().isEmpty()) {
                    tip.add(over.plek().copy().withStyle(ChatFormatting.GRAY));
                }
            }
            if (over.onderweg()) {
                tip.add(Component.translatable("gui.guhs.knabbelvelden.scherm.onderweg").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Nullable
    private Kaart kolom(GuiGraphicsExtractor g, Kolom k, int mouseX, int mouseY) {
        if (k.zichtbaar.isEmpty()) {
            Component leeg = Component.translatable(k.alle.isEmpty() ? (k.velden ? "gui.guhs.knabbelvelden.scherm.leeg_velden"
                    : "gui.guhs.knabbelvelden.scherm.leeg_thuis") : "gui.guhs.knabbelvelden.scherm.niks_gevonden");
            GidsTekst.passend(g, leeg, k.x + 6, k.y + k.h / 2 - 4, k.w - 12, 0.85f, GRIJS, false);
            return null;
        }
        Kaart over = vraag == null ? k.onder(mouseX, mouseY) : null;
        int per = k.perRij(), off = (k.w - per * CW) / 2;
        g.enableScissor(k.x, k.y, k.x + k.w, k.y + k.h);
        for (int r = 0; r < k.rijen(); r++) {
            for (int c = 0; c < per; c++) {
                int i = (k.scroll + r) * per + c;
                if (i >= k.zichtbaar.size()) {
                    break;
                }
                kaart(g, k, k.zichtbaar.get(i), k.x + off + c * CW, k.y + 1 + r * CH, k.zichtbaar.get(i) == over);
            }
        }
        g.disableScissor();
        if (k.maxScroll() > 0) {
            int n = k.maxScroll() + k.rijen();
            int bar = Math.max(12, k.h * k.rijen() / n);
            int by = k.y + (int) ((k.h - bar) * (k.scroll / (float) k.maxScroll()));
            g.fill(k.x + k.w - 3, by, k.x + k.w - 1, by + bar, 0xFFE8A0C4);
        }
        return over;
    }

    private void kaart(GuiGraphicsExtractor g, Kolom k, Kaart kaart, int x, int y, boolean over) {
        boolean gekozen = k.gekozen.contains(kaart.id());
        boolean kanNiet = !kaart.niet().isEmpty();
        int x2 = x + CW - 3, y2 = y + CH - 3;
        g.fill(x, y, x2, y2, gekozen ? ROZE : over ? 0xFFF4B8D4 : VAK_RAND);
        g.fill(x + 1, y + 1, x2 - 1, y2 - 1, gekozen ? GEKOZEN : over ? 0xFFFFF0F7 : 0xFFFFFFFF);
        if (kaart.maatje()) {
            icoon(g, kaart, x + (CW - 3) / 2, y + 23, 2f);
        } else {
            GuhEntity pop = pop(kaart);
            if (pop != null) {
                pop.tickCount = tijd;
                GuhPop.teken(g, x + 2, y + 2, x2 - 2, y + 44, pop, 30, -5);
            }
            GidsTekst.passend(g, Component.literal("#" + kaart.nr()), x + 3, y + 3, 30, 0.7f, ROZE, false);
        }
        GidsTekst.passend(g, kaart.naam(), x + 3, y + 46, CW - 9, 0.75f, kanNiet ? GRIJS : DONKER, false);
        GidsTekst.passend(g, Component.literal(kaart.vacht()), x + 3, y + 55, CW - 9, 0.6f, GRIJS, false);
        if (!kaart.bezig().isEmpty() && !kanNiet) {
            g.fill(x2 - 9, y + 2, x2 - 2, y + 9, ORANJE);
            g.text(font, "!", x2 - 7, y + 2, 0xFFFFFFFF, false);
        }
        if (kaart.onderweg()) {
            GidsTekst.passend(g, Component.literal("…"), x2 - 12, y + 34, 10, 1f, ROZE, false);
        }
        if (kanNiet) {
            g.fill(x + 1, y + 1, x2 - 1, y2 - 1, 0x70FFFFFF);
        }
    }

    @Nullable
    private GuhEntity pop(Kaart k) {
        return k.maatje() ? null : poppen.computeIfAbsent(k.id(), id -> GuhPop.van(k.looks()));
    }

    /** 1.6.2: a maatje's picture (its pick-up item), centred on cx, cy at this scale. */
    private static void icoon(GuiGraphicsExtractor g, Kaart k, int cx, int cy, float schaal) {
        g.pose().pushMatrix();
        g.pose().translate(cx - 8 * schaal, cy - 8 * schaal);
        g.pose().scale(schaal, schaal);
        g.item(nl.juiced.guhs.feature.band.client.AndereVadsjesTab.icoon(k.soort()), 0, 0);
        g.pose().popMatrix();
    }

    private void detail(GuiGraphicsExtractor g) {
        int x = detailX, y = kolomY, dw = detailW;
        if (focus == null) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.kies"), x + 6, y + 40, dw - 12, 0.85f, GRIJS, false);
            return;
        }
        Kaart k = focus;
        g.fill(x + 14, y + 84, x + dw - 14, y + 92, 0x70FFFFFF);
        if (k.maatje()) {
            icoon(g, k, x + dw / 2, y + 50, 3f);
        } else {
            GuhEntity pop = pop(k);
            if (pop != null) {
                pop.tickCount = tijd;
                GuhPop.teken(g, x + 2, y + 4, x + dw - 2, y + 92, pop, draai, 8);
            }
        }
        int ty = y + 96;
        GidsTekst.passend(g, k.naam().copy().withStyle(ChatFormatting.BOLD), x + 6, ty, dw - 12, 1f, DONKER, false);
        ty += 11;
        GidsTekst.passend(g, Component.literal(k.maatje() ? k.vacht() : "#" + k.nr() + " · " + k.vacht()), x + 6, ty, dw - 12, 0.8f, ROZE, false);
        ty += 10;
        if (!k.velden() && !k.plek().getString().isEmpty()) {
            // 1.6.2: where it is now (a picked-up one: in which chest...)
            GidsTekst.passend(g, k.plek(), x + 6, ty, dw - 12, 0.7f, GRIJS, false);
            ty += 9;
        }
        GuhPersonality aard = k.maatje() ? null : GuhPersonality.byId(k.looks().getStringOr("Personality", ""));
        if (aard != null) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.aard", aard.displayName()), x + 6, ty, dw - 12, 0.75f, DONKER, false);
            ty += 9;
        }
        CompoundTag kleren = k.looks().getCompoundOrEmpty("Clothes");
        List<Component> stukken = new ArrayList<>();
        boolean kleding = !k.maatje();
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(kleren.getStringOr(slot.name(), ""));
            if (c != null) {
                stukken.add(new ItemStack(ModItems.clothingItem(c)).getHoverName());
            }
        }
        if (kleding) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.kleding"), x + 6, ty, dw - 12, 0.75f, DONKER, false);
            ty += 9;
        }
        if (kleding && stukken.isEmpty()) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.geen_kleding"), x + 10, ty, dw - 16, 0.7f, GRIJS, false);
            ty += 8;
        }
        for (Component c : stukken) {
            if (ty > thuis.y + thuis.h - 26) {
                break;
            }
            GidsTekst.passend(g, Component.literal("• ").append(c), x + 10, ty, dw - 16, 0.7f, GRIJS, false);
            ty += 8;
        }
        ty += 3;
        long dag = data.getLongOr("Dag", 0L);
        Component sinds = k.velden() ? (k.sinds() >= dag ? Component.translatable("gui.guhs.knabbelvelden.scherm.pensioen_vandaag")
                : Component.translatable("gui.guhs.knabbelvelden.scherm.pensioen_sinds", k.sinds()))
                : k.sinds() < 0 ? Component.translatable("gui.guhs.knabbelvelden.scherm.al_lang")
                : Component.translatable("gui.guhs.knabbelvelden.scherm.van_jou_sinds", k.sinds());
        GidsTekst.passend(g, sinds, x + 6, ty, dw - 12, 0.75f, 0xFFC0901A, false);
        ty += 10;
        String status = !k.niet().isEmpty() ? "niet." + k.niet() : !k.bezig().isEmpty() ? "bezig." + k.bezig() : null;
        if (status != null) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden." + status), x + 6, ty, dw - 12, 0.7f, ORANJE, false);
        }
    }

    private void vraagPaneel(GuiGraphicsExtractor g) {
        int vx = left + (w - 240) / 2, vy = top + (h - 90) / 2;
        g.fill(left, top, left + w, top + h, 0x80FFFFFF);
        g.fill(vx - 2, vy - 2, vx + 242, vy + 92, RAND);
        g.fill(vx, vy, vx + 240, vy + 90, PANEEL);
        List<String> namen = new ArrayList<>();
        for (Kaart k : vraag) {
            if (!k.bezig().isEmpty()) {
                namen.add(k.naam().getString());
            }
        }
        String lijst = String.join(", ", namen.subList(0, Math.min(4, namen.size()))) + (namen.size() > 4 ? ", …" : "");
        GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.vraag.titel").withStyle(ChatFormatting.BOLD), vx + 8, vy + 8, 224, 1f, ROZE, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.vraag.tekst", lijst), vx + 8, vy + 24, 224, 0.8f, DONKER, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.knabbelvelden.scherm.vraag.uitleg"), vx + 8, vy + 40, 224, 0.7f, GRIJS, false);
    }

    // --- ticking -------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        tijd++;
        if (meldingTijd > 0) {
            meldingTijd--;
        }
        if (minecraft.player == null || minecraft.player.position().distanceTo(Vec3.atCenterOf(portaal())) > Pensioen.BEREIK + 2) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
