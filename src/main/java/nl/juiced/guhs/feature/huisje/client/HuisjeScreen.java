package nl.juiced.guhs.feature.huisje.client;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.band.client.MijnGuhsTab;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;
import nl.juiced.guhs.feature.huisje.Huisjes;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The screen of a Guhhuisje (2.10, owner only): its name (renamable), the residents (a little picture, name, hearts level)
 * with "Uit huis", per resident the chores it can do as on/off toggles with a tip what each chore needs nearby,
 * "Nieuwe bewoner" (your own guhs and maatjes around), where the chores' spoils go, and the toggle "laat klus-area zien"
 * (the blue dome, {@link HuisjeKoepel}).
 * 3.0 (timmerguh): someone else's huisje opens read-only (data "MagBewerken" false): "Dit is het huisje van X" at the top,
 * the name can't be edited and every button that changes something is grey (only "Klus-area" and "Klaar" work).
 * 1.2.8: the little "?" button next to the name opens the dialog "Wat kan hier?" on top of this screen
 * ({@link HuisjeOverzichtPaneel}: which chores can be done around this huisje right now and why, which toys stand there);
 * while it is open this screen is dimmed and only the dialog takes the mouse and the keys (Esc closes the dialog only).
 */
public class HuisjeScreen extends Screen {
    private static final int W = 320, H = 230, RIJ = 28;
    private static final int PANEEL = 0xF0FFF4F8, RAND = 0xFFF7B6CB, DONKER = 0xFF3A1C30, TEKST = 0xFF5A3A4A, LICHT = 0xFFB0708A, ROZE = 0xFF7A2848;

    private CompoundTag data;
    private BlockPos pos;
    private int left, top;
    private EditBox naam;
    /** The selected resident (band id), or null. */
    @Nullable
    private String gekozen;
    private boolean nieuw;
    private final GidsLijst bewoners = new GidsLijst(), rechts = new GidsLijst();
    private final Map<String, LivingEntity> poppen = new HashMap<>();
    /** 1.2.8: the open "Wat kan hier?" dialog, or null. */
    @Nullable
    private HuisjeOverzichtPaneel overzicht;

    /** 3.0: may this viewer change the huisje (its owner or an op)? Otherwise the screen is read-only. */
    private boolean mag() {
        return !data.contains("MagBewerken") || data.getBooleanOr("MagBewerken", false);
    }

    private static net.minecraft.client.gui.components.Tooltip alleenKijken() {
        return net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.timmerguh.alleen_kijken"));
    }

    public HuisjeScreen(CompoundTag data) {
        super(Component.translatable("gui.guhs.huisje.titel"));
        this.data = data;
        this.pos = BlockPos.of(data.getLongOr("Pos", 0L));
    }

    /** New data from the server (after a button). */
    public void update(CompoundTag nieuw) {
        String oudeNaam = naam == null ? null : naam.getValue();
        boolean focus = naam != null && naam.isFocused();
        this.data = nieuw;
        this.pos = BlockPos.of(nieuw.getLongOr("Pos", 0L));
        poppen.clear();
        rebuildWidgets();
        if (focus && oudeNaam != null) {
            naam.setValue(oudeNaam);
            naam.setFocused(true);
        }
    }

    public BlockPos pos() {
        return pos;
    }

    /** 1.2.8: the server's answer for the "Wat kan hier?" dialog (ignored when it was closed meanwhile). */
    public void overzicht(CompoundTag antwoord) {
        if (overzicht != null) {
            overzicht.zet(antwoord);
        }
    }

    /** 1.2.8: is the "Wat kan hier?" dialog open? */
    public boolean overzichtOpen() {
        return overzicht != null;
    }

    /** 1.2.8: opens the "Wat kan hier?" dialog over this screen and asks the server for it. */
    public void openOverzicht() {
        if (overzicht != null || !mag()) {
            return;
        }
        overzicht = new HuisjeOverzichtPaneel(pos, this::sluitOverzicht);
        rebuildWidgets();
        overzicht.vraag();
    }

    private void sluitOverzicht() {
        overzicht = null;
        rebuildWidgets();
    }

    private ListTag lijst(String key) {
        return data.getListOrEmpty(key);
    }

    @Nullable
    private CompoundTag bewoner(String id) {
        ListTag list = lijst("Bewoners");
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompoundOrEmpty(i).getStringOr("Id", "").equals(id)) {
                return list.getCompoundOrEmpty(i);
            }
        }
        return null;
    }

    /** 1.2.0: renaming with the shown name unchanged keeps a default name translatable (it isn't frozen into one language). */
    private void hernoem() {
        Component nu = nl.juiced.guhs.taal.Tekst.get(data, "Naam");
        if (!nl.juiced.guhs.taal.Tekst.literal(nu) && naam.getValue().strip().equals(nu.getString())) {
            return;
        }
        stuur(HuisjePayloads.Actie.NAAM, "", naam.getValue(), false, 0);
    }

    private void stuur(HuisjePayloads.Actie actie, String id, String tekst, boolean aan, int entity) {
        ClientPacketDistributor.sendToServer(new HuisjePayloads.Doe(pos, actie.ordinal(), id, tekst, aan, entity));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        naam = new EditBox(font, left + 10, top + 22, 192, 16, Component.translatable("gui.guhs.huisje.naam"));
        naam.setMaxLength(Huisjes.MAX_NAAM);
        naam.setValue(nl.juiced.guhs.taal.Tekst.get(data, "Naam").getString());
        boolean mag = mag();
        if (!mag) {
            nieuw = false;
            naam.setEditable(false);
            naam.setTooltip(alleenKijken());
        }
        addRenderableWidget(naam);
        Button hernoem = Button.builder(Component.translatable("gui.guhs.huisje.hernoem"),
                b -> hernoem()).bounds(left + 206, top + 21, 62, 18).build();
        hernoem.active = mag;
        if (!mag) {
            hernoem.setTooltip(alleenKijken());
        }
        addRenderableWidget(hernoem);
        // 1.2.5: chat messages about rare finds on/off
        boolean meld = data.getBooleanOr("Meldingen", true);
        Button melding = Button.builder(meldLabel(meld), b -> {
            data.putBoolean("Meldingen", !data.getBooleanOr("Meldingen", true));
            stuur(HuisjePayloads.Actie.MELDINGEN, "", "", data.getBooleanOr("Meldingen", true), 0);
            b.setMessage(meldLabel(data.getBooleanOr("Meldingen", true)));
            b.setTooltip(meldTip(data.getBooleanOr("Meldingen", true)));
        }).bounds(left + 294, top + 21, 18, 18).tooltip(mag ? meldTip(meld) : alleenKijken()).build();
        melding.active = mag;
        addRenderableWidget(melding);
        // 1.2.8: "Wat kan hier?" (which chores and toys the area offers)
        Button wat = Button.builder(Component.literal("?").withStyle(ChatFormatting.BOLD), b -> openOverzicht())
                .bounds(left + 272, top + 21, 18, 18).tooltip(mag ? net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.guhs.huisje.overzicht.knop.tooltip")) : alleenKijken()).build();
        wat.active = mag;
        addRenderableWidget(wat);
        ListTag lijst = lijst("Bewoners");
        if (gekozen == null || bewoner(gekozen) == null) {
            gekozen = lijst.isEmpty() ? null : lijst.getCompoundOrEmpty(0).getStringOr("Id", "");
        }
        // the residents on the left, the chores (or who could move in) on the right
        bewoners.plaats(left + 8, top + 64, 132, H - 64 - 30);
        List<GidsLijst.Regel> rijen = new ArrayList<>();
        for (int i = 0; i < lijst.size(); i++) {
            rijen.add(new BewonerRij(lijst.getCompoundOrEmpty(i)));
        }
        bewoners.zet(rijen);
        rechts.plaats(left + 146, top + 64, W - 146 - 8, H - 64 - 30);
        rechts.zet(nieuw ? kandidaten() : klussen());
        int by = top + H - 25;
        Button nieuwKnop = Button.builder(Component.translatable(nieuw ? "gui.guhs.huisje.terug_klusjes" : "gui.guhs.huisje.nieuwe_bewoner"), b -> {
            nieuw = !nieuw;
            rebuildWidgets();
        }).bounds(left + 8, by, 100, 18).tooltip(mag ? net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.huisje.nieuwe_bewoner.tooltip")) : alleenKijken()).build();
        nieuwKnop.active = mag;
        addRenderableWidget(nieuwKnop);
        Button uit = Button.builder(Component.translatable("gui.guhs.huisje.uit_huis"), b -> {
            if (gekozen != null) {
                stuur(HuisjePayloads.Actie.UIT, gekozen, "", false, 0);
                gekozen = null;
            }
        }).bounds(left + 112, by, 60, 18).build();
        uit.active = mag && gekozen != null;
        if (!mag) {
            uit.setTooltip(alleenKijken());
        }
        addRenderableWidget(uit);
        addRenderableWidget(Button.builder(koepelLabel(), b -> {
            HuisjeKoepel.zet(pos, !HuisjeKoepel.aan(pos));
            b.setMessage(koepelLabel());
        }).bounds(left + 176, by, 96, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.huisje.koepel.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 276, by, 36, 18).build());
        if (overzicht != null && !mag) {
            overzicht = null;   // (no longer allowed to change this huisje: the server wouldn't answer any more)
        }
        if (overzicht != null) {
            // the dialog lies over the screen: everything under it rests until it closes
            overzicht.plaats(left + 12, top + 8, W - 24, H - 16);
            for (var kind : children()) {
                if (kind instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                    widget.active = false;
                    widget.setFocused(false);
                }
            }
            naam.setEditable(false);
            setFocused(null);
        }
    }

    /** "Meldingen van zeldzame vondsten: aan" + what it does. */
    private static net.minecraft.client.gui.components.Tooltip meldTip(boolean aan) {
        return net.minecraft.client.gui.components.Tooltip.create(Component.empty()
                .append(Component.translatable("gui.guhs.huisje.meldingen",
                        Component.translatable(aan ? "gui.guhs.huisje.aan" : "gui.guhs.huisje.uit")).withStyle(ChatFormatting.BOLD))
                .append(Component.literal("\n")).append(Component.translatable("gui.guhs.huisje.meldingen.tooltip").withStyle(ChatFormatting.GRAY)));
    }

    private static Component meldLabel(boolean aan) {
        return aan ? Component.literal("!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.literal("!").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH);
    }

    private Component koepelLabel() {
        return Component.translatable("gui.guhs.huisje.koepel", Component.translatable(HuisjeKoepel.aan(pos) ? "gui.guhs.huisje.aan" : "gui.guhs.huisje.uit"));
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, 0xFFFBE0EA);
        g.fill(left, top + 18, left + W, top + 19, 0xFFD27A9C);
        Component titel = Component.translatable("gui.guhs.huisje.titel_maat", Component.translatable("block.guhs.guhhuisje_" + data.getStringOr("Maat", "")))
                .withStyle(ChatFormatting.BOLD);
        g.text(font, titel, left + 8, top + 5, ROZE, false);
        String bezet = lijst("Bewoners").size() + " / " + data.getIntOr("Plekken", 0);
        Component plekken = Component.translatable("gui.guhs.huisje.bewoners", bezet);
        GidsTekst.passend(g, plekken, left + W - 8, top + 5, 110, 1f, ROZE, true);
        if (!mag()) {
            // 3.0: someone else's huisje: whose it is (you may only look)
            String eigenaar = data.getStringOr("EigenaarNaam", "");
            Component van = Component.translatable("gui.guhs.huisje.van_wie", eigenaar.isEmpty() ? "?" : eigenaar, nl.juiced.guhs.taal.Tekst.get(data, "Naam"))
                    .withStyle(ChatFormatting.BOLD);
            g.fill(left + 6, top + 40, left + W - 6, top + 51, 0x40D27A9C);
            GidsTekst.passend(g, van.copy().append(Component.literal("  ")).append(Component.translatable("gui.guhs.timmerguh.alleen_kijken_kort")
                    .withStyle(ChatFormatting.ITALIC)), left + 9, top + 42, W - 18, 0.75f, ROZE, false);
        } else {
            // where the chores' spoils go
            Component opslag = Component.translatable(data.getBooleanOr("Bank", false) ? "gui.guhs.huisje.opslag.bank"
                    : data.getBooleanOr("Kist", false) ? "gui.guhs.huisje.opslag.kist" : "gui.guhs.huisje.opslag.deur");
            GidsTekst.passend(g, opslag, left + 8, top + 42, W - 16, 0.75f, LICHT, false);
        }
        GidsTekst.schaal(g, Component.translatable("gui.guhs.huisje.kop.bewoners").withStyle(ChatFormatting.BOLD), left + 8, top + 53, 0.875f, ROZE, false);
        Component kop = nieuw ? Component.translatable("gui.guhs.huisje.kop.nieuw")
                : gekozen != null && bewoner(gekozen) != null ? Component.translatable("gui.guhs.huisje.kop.klusjes_van", nl.juiced.guhs.taal.Tekst.get(bewoner(gekozen), "Naam"))
                : Component.translatable("gui.guhs.huisje.kop.klusjes");
        GidsTekst.passend(g, kop.copy().withStyle(ChatFormatting.BOLD), left + 146, top + 53, W - 146 - 8, 0.875f, ROZE, false);
        if (lijst("Bewoners").isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.huisje.leeg"), left + 12, top + 68, 124, 0.75f, LICHT);
        }
        int mx = overzicht != null ? -1 : mouseX, my = overzicht != null ? -1 : mouseY;   // (no hover under the dialog)
        bewoners.teken(g, mx, my, 0xFFD27A9C, 0x30D27A9C);
        rechts.teken(g, mx, my, 0xFFD27A9C, 0x30D27A9C);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (overzicht != null) {
            super.extractRenderState(g, -1, -1, partialTick);   // (the widgets under the dialog: no hover, no tooltips)
            overzicht.teken(g, mouseX, mouseY, partialTick);
            return;
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = bewoners.tip(mouseX, mouseY);
        if (tip == null) {
            tip = rechts.tip(mouseX, mouseY);
        }
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (overzicht != null) {
            return overzicht.klik(event, doubleClick);
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return bewoners.klik(mx, my, button) || rechts.klik(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overzicht != null) {
            return overzicht.wiel(mx, my, sy);
        }
        return bewoners.wiel(mx, my, sy) || rechts.wiel(mx, my, sy) || super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (overzicht != null) {
            return overzicht.sleep(my);
        }
        return bewoners.sleep(my) || rechts.sleep(my) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        bewoners.los();
        rechts.los();
        if (overzicht != null) {
            overzicht.los();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
        if (overzicht != null) {
            // Esc (or the inventory key) closes only the dialog; nothing reaches the screen under it
            if (keyCode == 256 || minecraft != null && minecraft.options.keyInventory.matches(event)) {
                sluitOverzicht();
            } else {
                overzicht.toets(keyCode);
            }
            return true;
        }
        if (naam != null && naam.isFocused() && mag() && (keyCode == 257 || keyCode == 335)) {
            hernoem();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(pos.getCenter()) > 24 * 24) {
            onClose();
        } else if (overzicht != null) {
            overzicht.tick();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private LivingEntity pop(CompoundTag b) {
        return poppen.computeIfAbsent(b.getStringOr("Id", ""), k -> "guh".equals(b.getStringOr("Soort", ""))
                ? (b.contains("Looks") ? GuhPop.van(b.getCompoundOrEmpty("Looks")) : null) : GuhPop.maatje(b.getStringOr("Soort", "")));
    }

    // =====================================================================================================================
    // rows
    // =====================================================================================================================

    /** A resident: click to see (and switch) its chores. */
    private final class BewonerRij implements GidsLijst.Regel {
        private final CompoundTag b;

        BewonerRij(CompoundTag b) {
            this.b = b;
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean kies = b.getStringOr("Id", "").equals(gekozen);
            g.fill(x, y, x + w, y + RIJ - 2, kies ? 0x50F77AB0 : hover ? 0x30F77AB0 : 0x18F7B6CB);
            LivingEntity pop = pop(b);
            if (pop != null) {
                GuhPop.teken(g, x + 1, y + 1, x + 29, y + RIJ - 3, pop, 30, -5);
            }
            GidsTekst.passend(g, nl.juiced.guhs.taal.Tekst.get(b, "Naam").copy().withStyle(ChatFormatting.BOLD), x + 32, y + 4, w - 36, 0.875f, DONKER, false);
            Component onder;
            int kleur = LICHT;
            if (b.getIntOr("Niveau", 0) >= 0) {
                BandNiveau n = BandNiveau.byIndex(b.getIntOr("Niveau", 0));
                onder = MijnGuhsTab.hartje(n).append(" ").append(MijnGuhsTab.niveauNaam(n));
                kleur = MijnGuhsTab.kleur(n);
            } else {
                onder = Component.translatable("entity.guhs." + b.getStringOr("Soort", ""));
            }
            if (b.getBooleanOr("Binnen", false)) {
                onder = Component.translatable("gui.guhs.huisje.slaapt").append(" · ").append(onder);
            }
            GidsTekst.passend(g, onder, x + 32, y + 16, w - 36, 0.625f, kleur, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            gekozen = b.getStringOr("Id", "");
            nieuw = false;
            rebuildWidgets();
            return true;
        }
    }

    /** The chores of the selected resident: a toggle per chore, with its tip. */
    private List<GidsLijst.Regel> klussen() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        ListTag alle = lijst("Klusjes");
        if (alle.isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.huisje.geen_klusjes"), LICHT, 0.875f));
            return out;
        }
        CompoundTag b = gekozen == null ? null : bewoner(gekozen);
        if (b == null) {
            out.add(new Tekst(Component.translatable("gui.guhs.huisje.kies_bewoner"), LICHT, 0.75f));
            for (int i = 0; i < alle.size(); i++) {
                String id = alle.getCompoundOrEmpty(i).getStringOr("Id", "");
                out.add(new KlusRij(null, id, icoon(alle.getCompoundOrEmpty(i).getStringOr("Icoon", "")), false, false));
            }
            return out;
        }
        if (b.getBooleanOr("Baby", false)) {
            out.add(new Tekst(Component.translatable("gui.guhs.huisje.baby"), LICHT, 0.75f));
        }
        ListTag k = b.getListOrEmpty("Klussen");
        for (int i = 0; i < k.size(); i++) {
            CompoundTag c = k.getCompoundOrEmpty(i);
            String icoon = "";
            for (int j = 0; j < alle.size(); j++) {
                if (alle.getCompoundOrEmpty(j).getStringOr("Id", "").equals(c.getStringOr("Id", ""))) {
                    icoon = alle.getCompoundOrEmpty(j).getStringOr("Icoon", "");
                }
            }
            out.add(new KlusRij(b.getStringOr("Id", ""), c.getStringOr("Id", ""), icoon(icoon), c.getBooleanOr("Aan", false), c.getBooleanOr("Kan", false)));
        }
        return out;
    }

    private static ItemStack icoon(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(rl));
    }

    /** One chore: [icon] name [on/off], its tip below. */
    private final class KlusRij implements GidsLijst.Regel {
        @Nullable
        private final String bewoner;
        private final String klus;
        private final ItemStack icoon;
        private final boolean aan, kan;

        KlusRij(@Nullable String bewoner, String klus, ItemStack icoon, boolean aan, boolean kan) {
            this.bewoner = bewoner;
            this.klus = klus;
            this.icoon = icoon;
            this.aan = aan;
            this.kan = kan;
        }

        private Component tip() {
            return Component.translatable("gui.guhs.klus." + klus + ".tip");
        }

        @Override
        public int hoogte() {
            return 20 + GidsTekst.hoogte(tip(), rechts.rijBreedte() - 24, 0.625f);
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean mag = mag();
            g.fill(x, y, x + w, y + hoogte() - 2, hover && bewoner != null && kan && mag ? 0x30F77AB0 : 0x18F7B6CB);
            if (!icoon.isEmpty()) {
                g.item(icoon, x + 2, y + 2);
            }
            GidsTekst.passend(g, Component.translatable("gui.guhs.klus." + klus).withStyle(ChatFormatting.BOLD), x + 22, y + 6, w - 70, 0.875f,
                    kan || bewoner == null ? DONKER : LICHT, false);
            if (bewoner != null) {
                Component staat = !kan ? Component.translatable("gui.guhs.huisje.kan_niet") : Component.translatable(aan ? "gui.guhs.huisje.aan" : "gui.guhs.huisje.uit");
                int kleur = !kan ? LICHT : !mag ? 0xFF9A8A92 : aan ? 0xFF3A9A5A : 0xFFB04060;
                if (kan) {
                    g.fill(x + w - 44, y + 3, x + w - 4, y + 15, !mag ? 0x40A0A0A0 : aan ? 0x6068D88A : 0x60F77AB0);   // (grey: only looking)
                }
                // (centred in the pill; the "kan niet" text is right-aligned to the row edge)
                int tw = Math.round(font.width(staat) * 0.75f);
                GidsTekst.passend(g, staat, kan ? x + w - 24 - Math.min(tw, 38) / 2 : x + w - 4 - Math.min(tw, 60), y + 5, kan ? 38 : 60,
                        0.75f, kleur, false);
            }
            GidsTekst.alinea(g, tip(), x + 22, y + 18, w - 24, 0.625f, TEKST);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (bewoner == null || !kan || !mag()) {
                return false;
            }
            stuur(HuisjePayloads.Actie.KLUS, bewoner, klus, !aan, 0);
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return bewoner == null ? null : !mag() ? List.of(Component.translatable("gui.guhs.timmerguh.alleen_kijken"))
                    : List.of(Component.translatable(aan ? "gui.guhs.huisje.klus_uit" : "gui.guhs.huisje.klus_aan"));
        }
    }

    /** Who could move in: your own guhs and maatjes around you. */
    private List<GidsLijst.Regel> kandidaten() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        ListTag k = mag() ? lijst("Kandidaten") : new ListTag();
        if (k.isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.huisje.geen_kandidaten"), LICHT, 0.75f));
        }
        for (int i = 0; i < k.size(); i++) {
            CompoundTag c = k.getCompoundOrEmpty(i);
            out.add(new GidsLijst.Regel() {
                @Override
                public int hoogte() {
                    return 22;
                }

                @Override
                public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
                    g.fill(x, y, x + w, y + 20, hover ? 0x40F77AB0 : 0x18F7B6CB);
                    GidsTekst.passend(g, nl.juiced.guhs.taal.Tekst.get(c, "Naam").copy().withStyle(ChatFormatting.BOLD), x + 4, y + 3, w - 8, 0.875f, DONKER, false);
                    Component onder = nl.juiced.guhs.taal.Tekst.empty(nl.juiced.guhs.taal.Tekst.get(c, "Woont")) ? Component.translatable("gui.guhs.huisje.trek_in")
                            : Component.translatable("gui.guhs.huisje.verhuis", nl.juiced.guhs.taal.Tekst.get(c, "Woont"));
                    GidsTekst.passend(g, onder, x + 4, y + 13, w - 8, 0.625f, LICHT, false);
                }

                @Override
                public boolean klik(double mx, double my, int x, int y, int w) {
                    stuur(HuisjePayloads.Actie.TREK_IN, "", "", false, c.getIntOr("Entity", 0));
                    nieuw = false;
                    return true;
                }
            });
        }
        return out;
    }

    /** A wrapped text. */
    private final class Tekst implements GidsLijst.Regel {
        private final Component tekst;
        private final int kleur;
        private final float schaal;

        Tekst(Component tekst, int kleur, float schaal) {
            this.tekst = tekst;
            this.kleur = kleur;
            this.schaal = schaal;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, rechts.rijBreedte() - 4, schaal) + 4;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 2, y + 2, w - 4, schaal, kleur);
        }
    }
}
