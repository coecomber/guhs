package nl.juiced.guhs.client.screen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingFavorieten;
import nl.juiced.guhs.feature.kleding.KledingPayloads;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.registry.ModItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The guh's wardrobe (2.9). Tab "Kleding": a big 3D preview of your guh (drag to turn it around) that tries on
 * everything you click, per slot a scrollable list of the pieces YOU unlocked (plus "niets"), search and a filter per
 * source, a dice button (a random outfit), favourite outfits (click to try on, shift-click to save) and "Aantrekken!"
 * that dresses the real guh (payload guhs:kleding_kleed). Tab "Rugzak &amp; harnas": the armour slot, the backpack's 18
 * slots and your inventory, like a chest.
 */
public class GuhWardrobeScreen extends AbstractContainerScreen<GuhWardrobeMenu> {
    // colours: the guh palette (dark plum background, pink border, gold highlights)
    private static final int BG = 0xF0301A26, BORDER = 0xFFF7B6CB, GOLD = 0xFFF2C14E, PANEL = 0xFF4A2338, PANEL_DARK = 0xFF3A1B2C,
            ROW = 0xFF52283F, ROW_OVER = 0xFF7A4A62, ROW_SEL = 0xFF8A5A2A, SLOT_BG = 0xFF8B6A78, SLOT_IN = 0xFF3A2430,
            TEXT = 0xFFFFE6EE, TEXT_DIM = 0xFFB898A8, GREEN = 0xFF7CE08A;
    private static final int W = GuhWardrobeMenu.WIDTH, H = GuhWardrobeMenu.HEIGHT;
    // the clothes tab
    private static final int PREVIEW_X = 8, PREVIEW_Y = 32, PREVIEW_W = 114, PREVIEW_H = 164;
    private static final int LIST_X = 130, LIST_Y = 78, LIST_W = 188, ROW_H = 18, ROWS = 7;
    private static final int TAB_Y = 24, TAB_W = 30, TAB_H = 22;
    /** The favourites row and the message lines (right side, under the list). */
    private static final int FAV_Y = LIST_Y + ROWS * ROW_H + 4, MELDING_Y = FAV_Y + 22;
    private static final List<GuhClothes.Slot> SLOTS = GuhClothes.Slot.kleding();

    private boolean kledingTab = true;
    private GuhClothes.Slot slot = GuhClothes.Slot.HEAD;
    private final Map<GuhClothes.Slot, GuhClothes> preview = new EnumMap<>(GuhClothes.Slot.class);
    private final Map<GuhClothes, ItemStack> icons = new HashMap<>();
    private final List<GuhClothes> lijst = new ArrayList<>();
    private int scroll;
    private float yaw = 25, pitch = 8;
    private boolean draaien;
    @Nullable
    private String bronFilter;
    private int aantalUnlocks = -1;
    @Nullable
    private GuhEntity pop;
    private Component melding = Component.empty();
    private int meldingTicks;

    private EditBox zoek;
    private Button bronKnop, dobbel, terug, aantrekken, tabKleding, tabRugzak;
    private final List<Button> favorieten = new ArrayList<>();

    public GuhWardrobeScreen(GuhWardrobeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.inventoryLabelX = GuhWardrobeMenu.INV_X;
        this.inventoryLabelY = GuhWardrobeMenu.INV_Y - 11;
        GuhEntity guh = menu.guh();
        for (GuhClothes.Slot s : SLOTS) {
            preview.put(s, guh == null ? null : guh.getClothes(s));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // widgets
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        favorieten.clear();                                 // (init runs again when the window is resized)
        int x = leftPos, y = topPos;
        tabKleding = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kleding.tab.kleding"), b -> zetTab(true))
                .bounds(x + W - 190, y + 4, 90, 18).build());
        tabRugzak = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kleding.tab.rugzak"), b -> zetTab(false))
                .bounds(x + W - 98, y + 4, 92, 18).build());
        zoek = addRenderableWidget(new EditBox(font, x + LIST_X, y + 59, 96, 14, Component.translatable("gui.guhs.kleding.zoek")));
        zoek.setHint(Component.translatable("gui.guhs.kleding.zoek").withStyle(ChatFormatting.DARK_GRAY));
        zoek.setMaxLength(30);
        zoek.setResponder(s -> {
            scroll = 0;
            vulLijst();
        });
        bronKnop = addRenderableWidget(Button.builder(Component.empty(), b -> volgendeBron(hasShiftDown() ? -1 : 1))
                .bounds(x + LIST_X + 100, y + 57, LIST_W - 100 + 6, 18).build());
        dobbel = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kleding.dobbel"), b -> dobbel())
                .bounds(x + PREVIEW_X, y + PREVIEW_Y + PREVIEW_H + 4, 56, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.kleding.dobbel.tooltip"))).build());
        terug = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kleding.terug"), b -> terug())
                .bounds(x + PREVIEW_X + 58, y + PREVIEW_Y + PREVIEW_H + 4, 56, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.kleding.terug.tooltip"))).build());
        for (int i = 0; i < KledingFavorieten.AANTAL; i++) {
            final int index = i;
            favorieten.add(addRenderableWidget(Button.builder(Component.literal(String.valueOf(i + 1)), b -> favoriet(index))
                    .bounds(x + LIST_X + 50 + i * 20, y + FAV_Y, 18, 18).build()));
        }
        aantrekken = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kleding.aantrekken").withStyle(ChatFormatting.GOLD),
                        b -> aantrekken())
                .bounds(x + PREVIEW_X, y + PREVIEW_Y + PREVIEW_H + 28, PREVIEW_W, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.kleding.aantrekken.tooltip"))).build());
        maakPop();
        vulLijst();
        zetTab(kledingTab);
    }

    private void zetTab(boolean kleding) {
        kledingTab = kleding;
        menu.setToonVakjes(!kleding);
        tabKleding.active = !kleding;
        tabRugzak.active = kleding;
        for (var w : List.of(zoek, bronKnop, dobbel, terug, aantrekken)) {
            w.visible = kleding;
        }
        favorieten.forEach(b -> b.visible = kleding);
        if (!kleding) {
            zoek.setFocused(false);
        }
        werkKnoppenBij();
    }

    /** The preview guh: a copy of the real one (look, size, hair) that tries the clothes on without touching the real one. */
    private void maakPop() {
        GuhEntity guh = menu.guh();
        if (guh == null || minecraft == null || minecraft.level == null) {
            pop = null;
            return;
        }
        GuhEntity copy = (GuhEntity) guh.getType().create(minecraft.level);
        if (copy == null) {
            pop = null;
            return;
        }
        try {
            copy.load(guh.saveWithoutId(new CompoundTag()));
        } catch (RuntimeException e) {
            copy.setVariant(guh.getVariant());
        }
        copy.setOrderedToSit(false);
        copy.setInSittingPose(false);
        copy.setInvisible(false);
        copy.hideName = true;                              // (2.9 visual QA: no name tag floating over the preview)
        pop = copy;
        kleedPop();
    }

    private void kleedPop() {
        if (pop == null) {
            return;
        }
        for (GuhClothes.Slot s : SLOTS) {
            GuhClothes c = preview.get(s);
            if (c == null) {
                pop.takeOff(s);
            } else {
                pop.wear(c);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // the list
    // ---------------------------------------------------------------------------------------------------------------

    /** The pieces shown for the chosen slot: your unlocks, filtered by source and search. (Row 0 is always "niets".) */
    private void vulLijst() {
        lijst.clear();
        String q = zoek == null ? "" : zoek.getValue().strip().toLowerCase(Locale.ROOT);
        for (GuhClothes c : GuhClothes.values()) {
            if (c.slot != slot || !KledingUnlocks.Client.heeft(c)) {
                continue;
            }
            if (bronFilter != null && !bronFilter.equals(KledingBronnen.bron(c))) {
                continue;
            }
            if (!q.isEmpty() && !icon(c).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q) && !c.id().contains(q)) {
                continue;
            }
            lijst.add(c);
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        aantalUnlocks = KledingUnlocks.Client.alle().size();
        werkKnoppenBij();
    }

    private int maxScroll() {
        return Math.max(0, lijst.size() + 1 - ROWS);
    }

    private ItemStack icon(GuhClothes c) {
        return icons.computeIfAbsent(c, k -> new ItemStack(ModItems.clothingItem(k)));
    }

    /** The sources that have at least one of your unlocks (for the filter button). */
    private List<String> bronnen() {
        List<String> out = new ArrayList<>();
        for (String b : KledingBronnen.bronnen()) {
            if (KledingBronnen.van(b).stream().anyMatch(KledingUnlocks.Client::heeft)) {
                out.add(b);
            }
        }
        return out;
    }

    private void volgendeBron(int stap) {
        List<String> opties = new ArrayList<>();
        opties.add(null);
        opties.addAll(bronnen());
        int i = Math.max(0, opties.indexOf(bronFilter));
        bronFilter = opties.get(Math.floorMod(i + stap, opties.size()));
        scroll = 0;
        vulLijst();
    }

    private void werkKnoppenBij() {
        if (bronKnop == null) {
            return;
        }
        Component naam = bronFilter == null ? Component.translatable("gui.guhs.kleding.alle_bronnen")
                : Component.translatable("gui.guhs.kledingbron." + bronFilter);
        String kort = font.width(naam) <= bronKnop.getWidth() - 8 ? naam.getString()
                : font.plainSubstrByWidth(naam.getString(), bronKnop.getWidth() - 8 - font.width("…")).stripTrailing() + "…";
        bronKnop.setMessage(Component.literal(kort));
        bronKnop.setTooltip(Tooltip.create(Component.translatable("gui.guhs.kleding.bron_filter.tooltip", naam)));
        aantrekken.active = kledingTab && anders();
        GuhEntity guh = menu.guh();
        boolean eigen = guh != null && minecraft != null && minecraft.player != null && guh.isOwnedBy(minecraft.player);
        aantrekken.active &= eigen;
        for (int i = 0; i < favorieten.size(); i++) {
            List<GuhClothes> fav = KledingFavorieten.Client.get(i);
            Button b = favorieten.get(i);
            b.setMessage(Component.literal(String.valueOf(i + 1)).withStyle(fav == null ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));
            b.setTooltip(Tooltip.create(Component.translatable(fav == null ? "gui.guhs.kleding.favoriet.leeg" : "gui.guhs.kleding.favoriet", i + 1)));
        }
    }

    /** Does the preview differ from what the guh wears? */
    private boolean anders() {
        GuhEntity guh = menu.guh();
        if (guh == null) {
            return false;
        }
        for (GuhClothes.Slot s : SLOTS) {
            if (preview.get(s) != guh.getClothes(s)) {
                return true;
            }
        }
        return false;
    }

    /** May the preview change this slot? (Not away from a backpack that still holds things.) */
    private boolean magWissel(GuhClothes.Slot s, @Nullable GuhClothes nieuw) {
        GuhEntity guh = menu.guh();
        return !(s == GuhClothes.Slot.BACK && guh != null && guh.getClothes(s) == GuhClothes.GUH_BACKPACK && nieuw != GuhClothes.GUH_BACKPACK
                && !menu.backpackIsEmpty());
    }

    private void probeer(GuhClothes.Slot s, @Nullable GuhClothes c) {
        if (!magWissel(s, c)) {
            meld(Component.translatable("gui.guhs.kleding.nee.rugzak_niet_leeg").withStyle(ChatFormatting.RED));
            return;
        }
        preview.put(s, c);
        kleedPop();
        werkKnoppenBij();
        klik(1.3f);
    }

    private void dobbel() {
        var random = minecraft.level.getRandom();
        for (GuhClothes.Slot s : SLOTS) {
            List<GuhClothes> opties = new ArrayList<>();
            for (GuhClothes c : GuhClothes.values()) {
                if (c.slot == s && KledingUnlocks.Client.heeft(c) && c != GuhClothes.GUH_BACKPACK) {
                    opties.add(c);
                }
            }
            if (!magWissel(s, null)) {
                continue;                                  // (the full backpack stays on)
            }
            float niets = s == GuhClothes.Slot.HEAD || s == GuhClothes.Slot.BODY ? 0.15f : 0.4f;
            preview.put(s, opties.isEmpty() || random.nextFloat() < niets ? null : opties.get(random.nextInt(opties.size())));
        }
        kleedPop();
        werkKnoppenBij();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BONE_BLOCK_BREAK, 1.6f));
        meld(Component.translatable("gui.guhs.kleding.gedobbeld").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void terug() {
        GuhEntity guh = menu.guh();
        for (GuhClothes.Slot s : SLOTS) {
            preview.put(s, guh == null ? null : guh.getClothes(s));
        }
        kleedPop();
        werkKnoppenBij();
        klik(1.0f);
    }

    private void favoriet(int index) {
        if (hasShiftDown()) {
            List<GuhClothes> outfit = new ArrayList<>();
            SLOTS.forEach(s -> outfit.add(preview.get(s)));
            PacketDistributor.sendToServer(new KledingPayloads.Bewaar(index, KledingPayloads.ids(outfit)));
            KledingFavorieten.Client.set(bewaardLokaal(index, outfit));
            meld(Component.translatable("gui.guhs.kleding.favoriet.bewaard", index + 1).withStyle(ChatFormatting.YELLOW));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f));
            werkKnoppenBij();
            return;
        }
        List<GuhClothes> fav = KledingFavorieten.Client.get(index);
        if (fav == null) {
            meld(Component.translatable("gui.guhs.kleding.favoriet.leeg", index + 1).withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < SLOTS.size(); i++) {
            GuhClothes c = i < fav.size() ? fav.get(i) : null;
            if ((c == null || KledingUnlocks.Client.heeft(c)) && magWissel(SLOTS.get(i), c)) {
                preview.put(SLOTS.get(i), c);
            }
        }
        kleedPop();
        werkKnoppenBij();
        klik(1.2f);
    }

    private static List<String> bewaardLokaal(int index, List<GuhClothes> outfit) {
        List<String> alle = new ArrayList<>();
        for (int i = 0; i < KledingFavorieten.AANTAL; i++) {
            List<GuhClothes> f = KledingFavorieten.Client.get(i);
            alle.add(i == index ? KledingFavorieten.codeer(outfit) : f == null ? "" : KledingFavorieten.codeer(f));
        }
        return alle;
    }

    private void aantrekken() {
        List<GuhClothes> outfit = new ArrayList<>();
        SLOTS.forEach(s -> outfit.add(preview.get(s)));
        PacketDistributor.sendToServer(new KledingPayloads.Kleed(menu.getGuhId(), KledingPayloads.ids(outfit)));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.2f));
    }

    private void meld(Component tekst) {
        melding = tekst;
        meldingTicks = 60;
    }

    private void klik(float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (meldingTicks > 0) {
            meldingTicks--;
        }
        if (menu.guh() == null) {
            onClose();
            return;
        }
        if (aantalUnlocks != KledingUnlocks.Client.alle().size()) {
            vulLijst();                                    // (an unlock arrived)
        }
        werkKnoppenBij();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // drawing
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, GOLD);
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, BORDER);
        g.fill(x, y, x + W, y + H, BG);
        if (kledingTab) {
            renderKleding(g, mouseX, mouseY);
        } else {
            renderRugzak(g, mouseX, mouseY);
        }
    }

    private void renderKleding(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // the preview: a little stage with a spotlight
        g.fill(x + PREVIEW_X - 1, y + PREVIEW_Y - 1, x + PREVIEW_X + PREVIEW_W + 1, y + PREVIEW_Y + PREVIEW_H + 1, BORDER);
        g.fillGradient(x + PREVIEW_X, y + PREVIEW_Y, x + PREVIEW_X + PREVIEW_W, y + PREVIEW_Y + PREVIEW_H, 0xFF5A2A48, PANEL_DARK);
        g.fill(x + PREVIEW_X + 18, y + PREVIEW_Y + PREVIEW_H - 22, x + PREVIEW_X + PREVIEW_W - 18, y + PREVIEW_Y + PREVIEW_H - 14, 0x55F7B6CB);
        if (pop != null) {
            renderPop(g, x + PREVIEW_X, y + PREVIEW_Y + 4, x + PREVIEW_X + PREVIEW_W, y + PREVIEW_Y + PREVIEW_H - 8, pop);
        }
        g.drawCenteredString(font, Component.translatable("gui.guhs.kleding.draai"), x + PREVIEW_X + PREVIEW_W / 2,
                y + PREVIEW_Y + PREVIEW_H - 10, TEXT_DIM);
        if (anders()) {
            g.drawCenteredString(font, Component.translatable("gui.guhs.kleding.pas_aan"), x + PREVIEW_X + PREVIEW_W / 2, y + PREVIEW_Y + 4, GOLD);
        }
        // the slot tabs: the piece the preview has in each slot
        for (int i = 0; i < SLOTS.size(); i++) {
            GuhClothes.Slot s = SLOTS.get(i);
            int tx = x + LIST_X + i * (TAB_W + 2), ty = y + TAB_Y;
            boolean over = mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H;
            g.fill(tx, ty, tx + TAB_W, ty + TAB_H, s == slot ? GOLD : over ? BORDER : SLOT_BG);
            g.fill(tx + 1, ty + 1, tx + TAB_W - 1, ty + TAB_H - (s == slot ? 0 : 1), s == slot ? PANEL : SLOT_IN);
            GuhClothes c = preview.get(s);
            if (c != null) {
                g.renderItem(icon(c), tx + (TAB_W - 16) / 2, ty + 3);
            } else {
                g.drawCenteredString(font, slotLetter(s), tx + TAB_W / 2, ty + 7, TEXT_DIM);
            }
        }
        g.drawString(font, Component.translatable("gui.guhs.menu.clothes." + slot.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD),
                x + LIST_X, y + TAB_Y + TAB_H + 3, BORDER, false);
        // (the search box and the source filter sit under it: widgets)
        // the list: "niets" first, then your unlocks
        g.fill(x + LIST_X - 1, y + LIST_Y - 1, x + LIST_X + LIST_W + 7, y + LIST_Y + ROWS * ROW_H + 1, PANEL_DARK);
        GuhEntity guh = menu.guh();
        for (int r = 0; r < ROWS; r++) {
            int i = r + scroll;
            if (i > lijst.size()) {
                break;
            }
            GuhClothes c = i == 0 ? null : lijst.get(i - 1);
            int ry = y + LIST_Y + r * ROW_H;
            boolean over = mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            boolean gekozen = preview.get(slot) == c;
            boolean kan = magWissel(slot, c);
            g.fill(x + LIST_X, ry, x + LIST_X + LIST_W, ry + ROW_H - 1, gekozen ? ROW_SEL : over && kan ? ROW_OVER : ROW);
            if (c == null) {
                g.drawString(font, Component.translatable("gui.guhs.kleding.niets"), x + LIST_X + 22, ry + 5, kan ? TEXT_DIM : 0xFF806070, false);
            } else {
                g.renderItem(icon(c), x + LIST_X + 2, ry + 1);
                int ruimte = LIST_W - 26 - (guh != null && guh.getClothes(slot) == c ? font.width(Component.translatable("gui.guhs.kleding.draagt")) + 8 : 4);
                String naam = font.plainSubstrByWidth(icon(c).getHoverName().getString(), ruimte);
                g.drawString(font, naam, x + LIST_X + 22, ry + 5, kan ? TEXT : 0xFF806070, false);
            }
            if (guh != null && guh.getClothes(slot) == c) {
                g.drawString(font, Component.translatable("gui.guhs.kleding.draagt"), x + LIST_X + LIST_W - 4 - font.width(
                        Component.translatable("gui.guhs.kleding.draagt")), ry + 5, GREEN, false);
            }
        }
        if (lijst.isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> regels = font.split(Component.translatable(
                    bronFilter == null && zoek.getValue().isBlank() ? "gui.guhs.kleding.leeg" : "gui.guhs.kleding.niks_gevonden"), LIST_W - 12);
            for (int i = 0; i < regels.size() && i < 5; i++) {
                g.drawString(font, regels.get(i), x + LIST_X + 6, y + LIST_Y + ROW_H + 6 + i * 10, TEXT_DIM, false);
            }
        }
        // the scrollbar
        int barX = x + LIST_X + LIST_W + 2, barTop = y + LIST_Y, barH = ROWS * ROW_H;
        g.fill(barX, barTop, barX + 4, barTop + barH, SLOT_IN);
        if (maxScroll() > 0) {
            int knob = Math.max(12, barH * ROWS / (lijst.size() + 1));
            int ky = barTop + (barH - knob) * scroll / maxScroll();
            g.fill(barX, ky, barX + 4, ky + knob, BORDER);
        }
        // favourites label and the message line
        g.drawString(font, Component.translatable("gui.guhs.kleding.favorieten"), x + LIST_X, y + FAV_Y + 5, TEXT_DIM, false);
        Component regel = meldingTicks > 0 ? melding
                : Component.translatable("gui.guhs.kleding.ontgrendeld_aantal", aantalUnlocks, KledingBronnen.aantalOntgrendelbaar());
        List<net.minecraft.util.FormattedCharSequence> regels = font.split(regel, W - LIST_X - 8);
        for (int i = 0; i < regels.size() && i < 2; i++) {
            g.drawCenteredString(font, regels.get(i), x + (LIST_X + W) / 2, y + MELDING_Y + i * 10, meldingTicks > 0 ? TEXT : TEXT_DIM);
        }
    }

    private static String slotLetter(GuhClothes.Slot s) {
        return switch (s) {
            case HEAD -> "♛";
            case EYES -> "◎";
            case BODY -> "♣";
            case NECK -> "❀";
            case BACK -> "⌂";
            case OREN -> "♪";
            default -> "?";
        };
    }

    private void renderRugzak(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        // the real guh on the left
        g.fill(x + 8, y + 30, x + GuhWardrobeMenu.OFFSET_X - 6, y + 140, 0x40FFFFFF);
        GuhEntity guh = menu.guh();
        if (guh != null) {
            // (2.9 visual QA: no name tag over the little guh; client-side only, restored right after)
            boolean naam = guh.hideName;
            guh.hideName = true;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 8, y + 30, x + GuhWardrobeMenu.OFFSET_X - 6, y + 140, 30, 0.0625f,
                    mouseX, mouseY, guh);
            guh.hideName = naam;
        }
        g.drawString(font, Component.translatable("gui.guhs.wardrobe.armor"), x + GuhWardrobeMenu.ARMOR_X - 60, y + GuhWardrobeMenu.ARMOR_Y + 4,
                TEXT, false);
        g.drawString(font, Component.translatable(menu.hasBackpack() ? "gui.guhs.wardrobe.backpack" : "gui.guhs.wardrobe.no_backpack"),
                x + GuhWardrobeMenu.PACK_X, y + GuhWardrobeMenu.PACK_Y - 11, menu.hasBackpack() ? TEXT : 0xFF8A6A7A, false);
        for (var s : menu.slots) {
            if (s.isActive()) {
                g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_BG);
                g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, SLOT_IN);
            }
        }
        if (!menu.hasBackpack()) {
            for (int row = 0; row < GuhWardrobeMenu.PACK_ROWS; row++) {
                for (int col = 0; col < GuhWardrobeMenu.PACK_COLS; col++) {
                    int sx = x + GuhWardrobeMenu.PACK_X + col * 18, sy = y + GuhWardrobeMenu.PACK_Y + row * 18;
                    g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0x30FFFFFF);
                }
            }
            // (over the empty places of the backpack)
            List<net.minecraft.util.FormattedCharSequence> regels = font.split(Component.translatable("gui.guhs.kleding.geen_rugzak"), 156);
            for (int i = 0; i < regels.size() && i < 3; i++) {
                g.drawCenteredString(font, regels.get(i), x + GuhWardrobeMenu.PACK_X + 80, y + GuhWardrobeMenu.PACK_Y + 4 + i * 10, TEXT_DIM);
            }
        } else if (!menu.backpackIsEmpty()) {
            Component label = Component.translatable("gui.guhs.wardrobe.backpack");
            g.drawString(font, Component.translatable("gui.guhs.kleding.rugzak_vol"), x + GuhWardrobeMenu.PACK_X + font.width(label) + 6,
                    y + GuhWardrobeMenu.PACK_Y - 11, TEXT_DIM, false);
        }
    }

    /** Draws the preview guh turned by {@link #yaw} (you turn it by dragging), big enough to fill the stage. */
    private void renderPop(GuiGraphics g, int x1, int y1, int x2, int y2, GuhEntity e) {
        g.enableScissor(x1, y1, x2, y2);
        float cx = (x1 + x2) / 2f, cy = (y1 + y2) / 2f + 6;
        // (2.9 visual QA: the hitbox is much smaller than the model, so sizing by the hitbox showed only a huge head. A guh
        // model with tail, paws and a tall hat takes ~2.2 x 1.6 blocks at scale 1 (measured in-game): fit that, turned any way.)
        float groot = Math.max(0.3f, e.getScale());
        float schaal = Math.min((y2 - y1) * 0.7f / (1.6f * groot), (x2 - x1) * 0.9f / (2.2f * groot));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float bodyRot = e.yBodyRot, yRot = e.getYRot(), xRot = e.getXRot(), headO = e.yHeadRotO, head = e.yHeadRot, bodyO = e.yBodyRotO;
        e.yBodyRot = 180 + yaw;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(180 + yaw);
        e.setXRot(0);
        e.yHeadRot = e.getYRot();
        e.yHeadRotO = e.getYRot();
        InventoryScreen.renderEntityInInventory(g, cx, cy, schaal, new Vector3f(0, 0.42f * groot, 0), pose, camera, e);
        e.yBodyRot = bodyRot;
        e.yBodyRotO = bodyO;
        e.setYRot(yRot);
        e.setXRot(xRot);
        e.yHeadRotO = headO;
        e.yHeadRot = head;
        g.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), W - 200), 8, 9, TEXT, false);
        if (!kledingTab) {
            // (2.9 visual QA: our own Dutch label instead of vanilla's "Inventory")
            g.drawString(font, Component.translatable("gui.guhs.kleding.jouw_spullen"), inventoryLabelX, inventoryLabelY, TEXT, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (kledingTab) {
            tooltips(g, mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    private void tooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        for (int i = 0; i < SLOTS.size(); i++) {
            int tx = x + LIST_X + i * (TAB_W + 2), ty = y + TAB_Y;
            if (mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H) {
                GuhClothes.Slot s = SLOTS.get(i);
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable("gui.guhs.menu.clothes." + s.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD));
                GuhClothes c = preview.get(s);
                tip.add(c == null ? Component.translatable("gui.guhs.kleding.niets").withStyle(ChatFormatting.GRAY) : icon(c).getHoverName());
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }
        GuhClothes c = rijOnder(mouseX, mouseY);
        if (c != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(icon(c).getHoverName().copy().withStyle(ChatFormatting.GOLD));
            String bron = KledingBronnen.bron(c);
            if (bron != null) {
                tip.add(Component.translatable("gui.guhs.kleding.bron", Component.translatable("gui.guhs.kledingbron." + bron))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (!magWissel(slot, c)) {
                tip.add(Component.translatable("gui.guhs.kleding.nee.rugzak_niet_leeg").withStyle(ChatFormatting.RED));
            } else {
                tip.add(Component.translatable("gui.guhs.kleding.klik_passen").withStyle(ChatFormatting.DARK_GRAY));
            }
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    /** The piece in the list row under the mouse (null: none, or the "niets" row). */
    @Nullable
    private GuhClothes rijOnder(double mouseX, double mouseY) {
        int r = rijIndex(mouseX, mouseY);
        return r <= 0 || r > lijst.size() ? null : lijst.get(r - 1);
    }

    /** The list index (0 = "niets") under the mouse, or -1. */
    private int rijIndex(double mouseX, double mouseY) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (mouseX < x || mouseX >= x + LIST_W || mouseY < y || mouseY >= y + ROWS * ROW_H) {
            return -1;
        }
        int i = (int) ((mouseY - y) / ROW_H) + scroll;
        return i <= lijst.size() ? i : -1;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // input
    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (kledingTab) {
            int x = leftPos, y = topPos;
            for (int i = 0; i < SLOTS.size(); i++) {
                int tx = x + LIST_X + i * (TAB_W + 2), ty = y + TAB_Y;
                if (mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H) {
                    slot = SLOTS.get(i);
                    scroll = 0;
                    vulLijst();
                    klik(1.4f);
                    return true;
                }
            }
            int r = rijIndex(mouseX, mouseY);
            if (r >= 0) {
                probeer(slot, r == 0 ? null : lijst.get(r - 1));
                return true;
            }
            if (mouseX >= x + PREVIEW_X && mouseX < x + PREVIEW_X + PREVIEW_W && mouseY >= y + PREVIEW_Y && mouseY < y + PREVIEW_Y + PREVIEW_H) {
                draaien = true;
                return true;
            }
            if (zoek.isFocused() && !zoek.isMouseOver(mouseX, mouseY)) {
                zoek.setFocused(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draaien) {
            yaw -= (float) dragX * 2.2f;
            pitch = Mth.clamp(pitch + (float) dragY * 0.8f, -25, 35);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draaien) {
            draaien = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (kledingTab && scrollY != 0) {
            int x = leftPos, y = topPos;
            if (mouseX >= x + PREVIEW_X && mouseX < x + PREVIEW_X + PREVIEW_W && mouseY >= y + PREVIEW_Y && mouseY < y + PREVIEW_Y + PREVIEW_H) {
                yaw += (float) scrollY * 15;
                return true;
            }
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (kledingTab && zoek.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            zoek.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        pop = null;
        super.onClose();
    }
}
