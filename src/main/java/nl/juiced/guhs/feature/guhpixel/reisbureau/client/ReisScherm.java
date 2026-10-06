package nl.juiced.guhs.feature.guhpixel.reisbureau.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.client.GuhKiezerLijst;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Reizen;
import nl.juiced.guhs.feature.guhpixel.reisbureau.ReisbureauPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The trip screen of a Reisbalie (the same for the counter in the Reisbureau and a home-made one). Left: today's trips
 * (per trip the duration, what the guh "waarschijnlijk" and "met geluk" brings, an explanation on hover). Right: pick one
 * of your guhs and send it off, or, when one is away: where it is, when it is back, and the buttons to collect it or call
 * it back early. Bottom: the reispas (stamps, trips, souvenirs). The server decides everything (Reizen.opActie) and
 * answers with a fresh state.
 */
public class ReisScherm extends Screen {
    private static final int W = 360, H = 236, KAART_H = 30, LINKS_W = 180, RECHTS_X = 196, RECHTS_W = 156;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A,
            ROZE = 0xFFFF9AC8;
    private static final String G = "gui.guhs.reisbureau.";

    private CompoundTag data;
    private final GuhKiezerLijst kiezer;
    private final List<Reis> reizen = new ArrayList<>();
    private int left, top;
    @Nullable
    private String gekozenReis;
    private long ontvangen;
    private boolean zeker, gevraagd;
    private int ticks;
    @Nullable
    private GuhEntity pop;
    private float popYaw = 25f;

    /** One trip of the offer, with its two souvenir icons (made lazily: never an ItemStack before the level is there). */
    private static final class Reis {
        final String id;
        final int minuten, kans;
        final boolean proef, open, hebS, hebZ;
        final String souvenirId, zeldzaamId;
        @Nullable
        ItemStack souvenir, zeldzaam;

        Reis(CompoundTag t) {
            id = t.getStringOr("Id", "");
            minuten = t.getIntOr("Minuten", 60);
            kans = t.getIntOr("Kans", 0);
            proef = t.getBooleanOr("Proef", false);
            open = t.getBooleanOr("Open", false);
            hebS = t.getBooleanOr("HebS", false);
            hebZ = t.getBooleanOr("HebZ", false);
            souvenirId = t.getStringOr("Souvenir", "");
            zeldzaamId = t.getStringOr("Zeldzaam", "");
        }

        ItemStack souvenir() {
            if (souvenir == null) {
                souvenir = stack(souvenirId);
            }
            return souvenir;
        }

        ItemStack zeldzaam() {
            if (zeldzaam == null) {
                zeldzaam = stack(zeldzaamId);
            }
            return zeldzaam;
        }

        private static ItemStack stack(String id) {
            Identifier key = id.isEmpty() ? null : Identifier.tryParse(id);
            return key == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(key));
        }

        Component naam() {
            return Component.translatable(G + "bestemming." + id);
        }

        Component duur() {
            return Reizen.tijd(minuten * 60_000L);
        }
    }

    public ReisScherm(CompoundTag data) {
        super(Component.translatable(G + "titel"));
        this.kiezer = new GuhKiezerLijst(id -> herbouw());
        zet(data);
    }

    private void zet(CompoundTag nieuw) {
        this.data = nieuw;
        this.ontvangen = System.currentTimeMillis();
        this.gevraagd = false;
        this.zeker = false;
        reizen.clear();
        ListTag aanbod = nieuw.getListOrEmpty("Aanbod");
        boolean nogDaar = false;
        for (int i = 0; i < aanbod.size(); i++) {
            Reis r = new Reis(aanbod.getCompoundOrEmpty(i));
            reizen.add(r);
            nogDaar |= r.open && r.id.equals(gekozenReis);
        }
        if (!nogDaar) {
            gekozenReis = null;
            for (Reis r : reizen) {
                if (r.open && r.proef) {
                    gekozenReis = r.id;   // (the proefreisje is the only one to pick)
                }
            }
        }
        pop = null;
    }

    public void update(CompoundTag nieuw) {
        zet(nieuw);
        herbouw();
    }

    private void herbouw() {
        if (width > 0) {
            rebuildWidgets();
        }
    }

    private boolean heeftReis() {
        return data.contains("Reis");
    }

    private CompoundTag reis() {
        return data.getCompoundOrEmpty("Reis");
    }

    private long rest() {
        return Math.max(0L, reis().getLongOr("Rest", 0L) - (System.currentTimeMillis() - ontvangen));
    }

    private boolean klaar() {
        return reis().getBooleanOr("Weg", false) && rest() <= 0;
    }

    private int stap() {
        return data.getIntOr("Stap", 0);
    }

    private void stuur(int actie, String bestemming, String guh) {
        if (minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new ReisbureauPayloads.Actie(BlockPos.of(data.getLongOr("Pos", 0L)), actie, bestemming, guh));
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int rx = left + RECHTS_X;
        if (heeftReis()) {
            boolean weg = reis().getBooleanOr("Weg", false);
            if (weg && klaar()) {
                addRenderableWidget(Button.builder(Component.translatable(G + "knop.ophalen"), b -> stuur(ReisbureauPayloads.OPHALEN, "", ""))
                        .bounds(rx + 18, top + 168, RECHTS_W - 36, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.translatable(zeker ? G + "knop.eerder_zeker" : G + "knop.eerder"), b -> {
                    if (zeker) {
                        stuur(ReisbureauPayloads.EERDER, "", "");
                    } else {
                        zeker = true;
                        herbouw();
                    }
                }).bounds(rx + 18, top + 168, RECHTS_W - 36, 20).tooltip(Tooltip.create(Component.translatable(G + "tip.eerder"))).build());
            }
        } else if (stap() >= Reizen.PROEF) {
            kiezer.plaats(rx, top + 34, RECHTS_W, 112);
            kiezer.zet(data.getListOrEmpty("Guhs"));
            UUID guh = kiezer.gekozen();
            Button boek = Button.builder(Component.translatable(G + "knop.boek"), b -> {
                UUID g = kiezer.gekozen();
                if (g != null && gekozenReis != null) {
                    stuur(ReisbureauPayloads.BOEK, gekozenReis, g.toString());
                }
            }).bounds(rx + 18, top + 168, RECHTS_W - 36, 20).build();
            boek.active = guh != null && gekozenReis != null;
            addRenderableWidget(boek);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 68, top + H - 24, 60, 18).build());
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (heeftReis()) {
            // the moment it is back (or stored): ask the server once, it answers with the "Ophalen" state
            boolean weg = reis().getBooleanOr("Weg", false);
            if (!gevraagd && ((weg && !reis().getBooleanOr("Klaar", false) && rest() <= 0) || (!weg && ticks % 40 == 0))) {
                gevraagd = weg;
                stuur(ReisbureauPayloads.VERVERS, "", "");
            }
        } else if (ticks % 100 == 0) {
            stuur(ReisbureauPayloads.VERVERS, "", "");   // (a guh may have walked up to the balie)
        }
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    private int kaartY(int i) {
        return top + 34 + i * (KAART_H + 2);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        // ---- left: today's trips ----
        int lx = left + 8;
        g.text(font, Component.translatable(G + "vandaag").withStyle(ChatFormatting.BOLD), lx, top + 22, ROZE, false);
        GidsTekst.passend(g, Component.translatable(G + "nieuw_over", Reizen.tijd(Math.max(0L, data.getLongOr("Morgen", 0L) - (System.currentTimeMillis() - ontvangen)))),
                left + W - 8, top + 16, 150, 0.625f, DOF, true);   // (a line of its own under the title: beside the heading it ran into the Dutch one)
        for (int i = 0; i < reizen.size(); i++) {
            tekenReis(g, reizen.get(i), lx, kaartY(i), mouseX, mouseY);
        }
        // ---- right: who is away, or who may go ----
        int rx = left + RECHTS_X;
        if (heeftReis()) {
            tekenWeg(g, rx);
        } else if (stap() >= Reizen.PROEF) {
            g.text(font, Component.translatable(G + "kies_guh").withStyle(ChatFormatting.BOLD), rx, top + 22, ROZE, false);
            kiezer.teken(g, mouseX, mouseY);
            Reis r = gekozen();
            GidsTekst.passend(g, r == null ? Component.translatable(G + "kies_reis") : r.naam().copy().append(" · ").append(r.duur()),
                    rx, top + 152, RECHTS_W, 0.875f, r == null ? DOF : GOUD, false);
        } else {
            g.text(font, Component.translatable(G + "slot.kop").withStyle(ChatFormatting.BOLD), rx, top + 22, ROZE, false);
            GidsTekst.alinea(g, Component.translatable(G + "slot." + Math.max(0, Math.min(1, stap()))), rx, top + 38, RECHTS_W, 0.875f, TEKST);
        }
        // ---- bottom: the reispas ----
        int py = top + 198;
        g.fill(left + 8, py - 3, left + W - 8, py - 2, 0x60F7B6CB);
        g.text(font, Component.translatable(G + "reispas").withStyle(ChatFormatting.BOLD), lx, py + 2, ROZE, false);
        int stempels = data.getIntOr("Stempels", 0);
        int sx = lx + font.width(Component.translatable(G + "reispas").withStyle(ChatFormatting.BOLD)) + 8;
        for (int i = 0; i < Reizen.STEMPELS_VOL; i++) {
            int x = sx + i * 13;
            g.fill(x, py, x + 11, py + 11, 0xFF7A2848);
            g.fill(x + 1, py + 1, x + 10, py + 10, i < stempels ? 0xFFF77AB0 : 0xFF3A1C30);
            if (i < stempels) {
                // a little guh face as the stamp
                g.fill(x + 2, py + 1, x + 4, py + 3, 0xFFFFC6E0);
                g.fill(x + 7, py + 1, x + 9, py + 3, 0xFFFFC6E0);
                g.fill(x + 3, py + 4, x + 4, py + 6, 0xFF3A1C30);
                g.fill(x + 7, py + 4, x + 8, py + 6, 0xFF3A1C30);
                g.fill(x + 5, py + 7, x + 6, py + 8, 0xFF7A2848);
            }
        }
        Component tel = Component.translatable(G + "reispas.reizen", data.getIntOr("Reizen", 0)).append("  ·  ")
                .append(Component.translatable(G + "reispas.souvenirs", data.getIntOr("Souvenirs", 0), 32));
        GidsTekst.passend(g, tel, left + W - 8, py + 2, W - 16 - (sx - lx) - Reizen.STEMPELS_VOL * 13 - 8, 0.875f, GOUD, true);
        Component melding = Tekst.get(data, "Melding");
        if (!Tekst.empty(melding)) {
            GidsTekst.passend(g, melding, lx, top + H - 19, W - 16 - 68, 0.875f, TEKST, false);
        }
    }

    @Nullable
    private Reis gekozen() {
        for (Reis r : reizen) {
            if (r.id.equals(gekozenReis)) {
                return r;
            }
        }
        return null;
    }

    private void tekenReis(GuiGraphicsExtractor g, Reis r, int x, int y, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + LINKS_W && mouseY >= y && mouseY < y + KAART_H;
        boolean ik = r.id.equals(gekozenReis) && !heeftReis();
        g.fill(x, y, x + LINKS_W, y + KAART_H, ik ? GROEN : hover && r.open ? RAND : 0xFF5A2A48);
        g.fill(x + 1, y + 1, x + LINKS_W - 1, y + KAART_H - 1, ik ? 0xFF3A4A36 : 0xFF3A1C30);
        // the duration as a coloured tab on the left
        int tab = r.proef ? 0xFF8ACBF0 : r.minuten <= 60 ? 0xFF9BE08A : r.minuten <= 120 ? 0xFFFFE27A : r.minuten <= 480 ? 0xFFFFA860 : 0xFFF77AB0;
        g.fill(x + 1, y + 1, x + 4, y + KAART_H - 1, tab);
        int tw = LINKS_W - 8 - (r.proef ? 22 : 44);
        GidsTekst.passend(g, r.naam(), x + 8, y + 4, tw, 1f, r.open ? TEKST : DOF, false);
        GidsTekst.schaal(g, Component.translatable(G + "duur", r.duur()), x + 8, y + 17, 0.75f, r.open ? GOUD : DOF, false);
        int ix = x + LINKS_W - (r.proef ? 21 : 41);
        g.item(r.souvenir(), ix, y + 3);
        if (r.hebS) {
            g.fill(ix + 12, y + 2, ix + 16, y + 6, GROEN);
        }
        if (!r.proef) {
            g.item(r.zeldzaam(), ix + 20, y + 3);
            if (r.hebZ) {
                g.fill(ix + 32, y + 2, ix + 36, y + 6, GROEN);
            }
            GidsTekst.schaal(g, Component.literal(r.kans + "%"), ix + 37, y + 21, 0.625f, ROZE, true);
        }
        if (!r.open) {
            g.fill(x + 1, y + 1, x + LINKS_W - 1, y + KAART_H - 1, 0x70301A26);
        }
    }

    private void tekenWeg(GuiGraphicsExtractor g, int rx) {
        CompoundTag reis = reis();
        Component naam = Tekst.get(reis, "Naam");
        Component plek = Component.translatable(G + "bestemming." + reis.getStringOr("Bestemming", ""));
        boolean weg = reis.getBooleanOr("Weg", false);
        g.text(font, Component.translatable(G + "weg.kop").withStyle(ChatFormatting.BOLD), rx, top + 22, ROZE, false);
        g.fill(rx, top + 34, rx + RECHTS_W, top + 100, 0x30F7B6CB);
        if (weg && !klaar()) {
            // away: an empty spot with a little suitcase label
            GidsTekst.alinea(g, Component.translatable(G + "weg.een_tegelijk"), rx + 6, top + 60, RECHTS_W - 12, 0.75f, DOF);
        } else {
            if (pop == null) {
                pop = GuhPop.van(reis.getCompoundOrEmpty("Looks"));
            }
            if (pop != null) {
                popYaw += 0.4f;
                pop.tickCount++;
                GuhPop.teken(g, rx, top + 34, rx + RECHTS_W, top + 100, pop, popYaw, -8f);
            }
        }
        long rest = rest();
        long duur = Math.max(1L, reis.getLongOr("Duur", 1L));
        Component regel = !weg ? Component.translatable(G + "weg.vertrekt", naam) : klaar() ? Component.translatable(G + "weg.klaar", naam)
                : Component.translatable(G + "weg.onderweg", naam, plek);
        int y = top + 104;
        y += GidsTekst.alinea(g, regel, rx, y, RECHTS_W, 0.875f, klaar() ? GROEN : TEKST) + 4;
        GidsTekst.balk(g, rx + 1, Math.min(y, top + 148), RECHTS_W - 2, 5, weg ? 1f - rest / (float) duur : 0f);
        if (weg && !klaar()) {
            GidsTekst.passend(g, Component.translatable(G + "weg.terug_over", Reizen.tijd(rest)), rx, Math.min(y, top + 148) + 9, RECHTS_W, 0.875f, GOUD, false);
        }
    }

    // =====================================================================================================================
    // tooltips and the mouse
    // =====================================================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = tip(mouseX, mouseY);
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> tip(int mx, int my) {
        int lx = left + 8;
        for (int i = 0; i < reizen.size(); i++) {
            Reis r = reizen.get(i);
            int y = kaartY(i);
            if (mx < lx || mx >= lx + LINKS_W || my < y || my >= y + KAART_H) {
                continue;
            }
            List<Component> tip = new ArrayList<>();
            int ix = lx + LINKS_W - (r.proef ? 21 : 41);
            if (!r.proef && mx >= ix && mx < ix + 17) {
                tip.add(Component.translatable(G + "waarschijnlijk").withStyle(ChatFormatting.GOLD));
                tip.add(r.souvenir().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                wikkel(tip, Component.translatable(G + "tip.waarschijnlijk"), ChatFormatting.GRAY);
                tip.add(Component.translatable(r.hebS ? G + "tip.heb_je" : G + "tip.nieuw").withStyle(r.hebS ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE));
            } else if (!r.proef && mx >= ix + 20) {
                tip.add(Component.translatable(G + "met_geluk", r.kans).withStyle(ChatFormatting.GOLD));
                tip.add(r.zeldzaam().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                wikkel(tip, Component.translatable(G + "tip.met_geluk"), ChatFormatting.GRAY);
                tip.add(Component.translatable(r.hebZ ? G + "tip.heb_je" : G + "tip.nieuw").withStyle(r.hebZ ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE));
            } else {
                tip.add(r.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
                wikkel(tip, Component.translatable(G + "bestemming." + r.id + ".uitleg"), ChatFormatting.WHITE);
                tip.add(Component.translatable(G + "duur", r.duur()).withStyle(ChatFormatting.GOLD));
                wikkel(tip, Component.translatable(r.proef ? G + "tip.proef" : G + "tip.echte_tijd"), ChatFormatting.GRAY);
                if (!r.open) {
                    wikkel(tip, Component.translatable(G + "slot." + Math.max(0, Math.min(2, stap()))), ChatFormatting.GOLD);
                }
            }
            return tip;
        }
        int py = top + 198;
        if (my >= py - 2 && my < py + 13 && mx >= lx && mx < left + W - 8) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(G + "reispas.stempels", data.getIntOr("Stempels", 0), Reizen.STEMPELS_VOL).withStyle(ChatFormatting.LIGHT_PURPLE));
            wikkel(tip, Component.translatable(G + "reispas.uitleg"), ChatFormatting.WHITE);
            return tip;
        }
        return heeftReis() || stap() < Reizen.PROEF ? null : kiezer.tip(mx, my);
    }

    private static void wikkel(List<Component> tip, Component tekst, ChatFormatting kleur) {
        for (var regel : Minecraft.getInstance().font.getSplitter().splitLines(tekst, 200, Style.EMPTY)) {
            tip.add(Component.literal(regel.getString()).withStyle(kleur));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mx = event.x(), my = event.y();
        int lx = left + 8;
        if (!heeftReis()) {
            for (int i = 0; i < reizen.size(); i++) {
                Reis r = reizen.get(i);
                int y = kaartY(i);
                if (r.open && mx >= lx && mx < lx + LINKS_W && my >= y && my < y + KAART_H) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    gekozenReis = r.id;
                    herbouw();
                    return true;
                }
            }
            return stap() >= Reizen.PROEF && kiezer.klik(mx, my, event.button());
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return (!heeftReis() && kiezer.wiel(mouseX, mouseY, scrollY)) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return (!heeftReis() && kiezer.sleep(event.x(), event.y(), dragX, dragY)) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        kiezer.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
