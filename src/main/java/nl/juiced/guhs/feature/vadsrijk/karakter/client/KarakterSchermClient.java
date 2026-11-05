package nl.juiced.guhs.feature.vadsrijk.karakter.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.karakter.Badges;
import nl.juiced.guhs.feature.vadsrijk.karakter.KarakterPayloads;
import nl.juiced.guhs.feature.vadsrijk.karakter.Modus;

/**
 * T-KARAKTER: the character screen ("Je ridders"), in the pink economie style. Three cards: name + mode badge, level, class,
 * coins, and the buttons Spelen / Naam / Makkelijker. An empty card: "Nieuw karakter" -> the creation panel (name field, the three
 * modes with their explanation, "Maken en spelen"). At the entrance it also offers "Verder als ...". The server decides; when
 * switching isn't allowed here, the reason is shown and Spelen is grey.
 */
public class KarakterSchermClient extends EcoScherm {
    private static final int KAART_W = 112, KAART_H = 150;
    /** -1 = the overview; else the slot being created / renamed / lowered. */
    private int maakSlot = -1, naamSlot = -1, modusSlot = -1;
    private Modus gekozen = Modus.NORMAAL;
    @Nullable
    private EditBox naamVeld;

    public KarakterSchermClient(CompoundTag data) {
        super("karakter", Component.translatable("vadsrijk.karakter.scherm.titel"), data, List.of(), 3 * KAART_W + 4 * 8, 214);
    }

    @Override
    protected void init() {
        super.init();
        String oud = naamVeld == null ? "" : naamVeld.getValue();
        naamVeld = new EditBox(font, left + 16, top + 44, 160, 16, Component.translatable("vadsrijk.karakter.scherm.naam"));
        naamVeld.setMaxLength(16);
        naamVeld.setHint(Component.translatable("vadsrijk.karakter.scherm.naam_hint").withStyle(ChatFormatting.GRAY));
        naamVeld.setValue(oud);
        addRenderableWidget(naamVeld);
        zichtbaar();
    }

    private void zichtbaar() {
        if (naamVeld != null) {
            naamVeld.visible = maakSlot >= 0 || naamSlot >= 0;
            naamVeld.active = naamVeld.visible;
            if (naamVeld.visible) {
                setFocused(naamVeld);
                naamVeld.setFocused(true);
            }
        }
    }

    @Override
    protected void opNieuweData() {
        maakSlot = naamSlot = modusSlot = -1;
        if (naamVeld != null) {
            naamVeld.setValue("");
        }
        zichtbaar();
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        if (!actie.equals("dicht")) {
            ClientPacketDistributor.sendToServer(new KarakterPayloads.Doe(actie, args));
        }
    }

    private List<CompoundTag> slots() {
        List<CompoundTag> l = new ArrayList<>();
        var lijst = data.getListOrEmpty("slots");
        for (int i = 0; i < lijst.size(); i++) {
            l.add(lijst.getCompoundOrEmpty(i));
        }
        return l;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (maakSlot >= 0) {
            maakPaneel(g, mx, my);
            return;
        }
        if (naamSlot >= 0) {
            naamPaneel(g, mx, my);
            return;
        }
        boolean mag = data.getBooleanOr("mag", false);
        int actief = data.getIntOr("actief", 0);
        List<CompoundTag> slots = slots();
        for (int i = 0; i < slots.size(); i++) {
            kaart(g, mx, my, left + 8 + i * (KAART_W + 8), top + 24, slots.get(i), i == actief, mag);
        }
        int y = top + 24 + KAART_H + 6;
        if (!mag) {
            g.centeredText(font, Component.translatable(data.getStringOr("waarom", "vadsrijk.karakter.niet.plek")), left + w / 2, y, ROOD);
        } else {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.uitleg"), left + w / 2, y, ZACHT);
        }
        if (data.getBooleanOr("aankomst", false)) {
            CompoundTag a = actief < slots.size() ? slots.get(actief) : new CompoundTag();
            knop(g, mx, my, left + w / 2 - 70, top + h - 20, 140, 16,
                    Component.translatable("vadsrijk.karakter.scherm.verder", a.getStringOr("naam", "?")), true, this::onClose);
        }
    }

    private void kaart(GuiGraphicsExtractor g, int mx, int my, int x, int y, CompoundTag s, boolean isActief, boolean mag) {
        int slot = s.getIntOr("slot", 0);
        g.fill(x, y, x + KAART_W, y + KAART_H, isActief ? 0xFFFF4FA8 : 0xFFF0B8D8);
        g.fill(x + 1, y + 1, x + KAART_W - 1, y + KAART_H - 1, isActief ? 0xFFFFE6F3 : 0xFFFFF6FB);
        if (s.getBooleanOr("leeg", false)) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.leeg").withStyle(ChatFormatting.ITALIC), x + KAART_W / 2, y + 40, ZACHT);
            knop(g, mx, my, x + 8, y + KAART_H - 24, KAART_W - 16, 16, Component.translatable("vadsrijk.karakter.scherm.nieuw"), true, () -> {
                maakSlot = slot;
                gekozen = Modus.NORMAAL;
                zichtbaar();
            });
            return;
        }
        int ty = y + 6;
        Component naam = Badges.badge(s.getStringOr("badge", "")).append(Component.literal(" "))
                .append(Component.literal(s.getStringOr("naam", "?")).withStyle(ChatFormatting.BOLD));
        g.centeredText(font, naam, x + KAART_W / 2, ty, TEKST);
        ty += 12;
        Modus m = Modus.van(s.getStringOr("modus", "normaal"));
        g.centeredText(font, Component.translatable(m.naamKey()), x + KAART_W / 2, ty, m == Modus.EENVADS ? GOUD : ZACHT);
        ty += 10;
        if (s.getBooleanOr("pensioen", false)) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.pensioen"), x + KAART_W / 2, ty, GOUD);
            ty += 10;
        }
        ty += 4;
        if (s.getBooleanOr("nieuw", false)) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.nooit"), x + KAART_W / 2, ty, ZACHT);
            ty += 10;
        } else {
            g.text(font, Component.translatable("vadsrijk.karakter.scherm.level", s.getIntOr("glevel", 1)), x + 8, ty, TEKST, false);
            ty += 10;
            g.text(font, Component.translatable("vadsrijk.karakter.scherm.klasse", Component.translatable(nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.ofRidder(s.getStringOr("klasse", "knuffelridder")).naamKey()),
                    s.getIntOr("level", 1)), x + 8, ty, ZACHT, false);
            ty += 10;
            munten(g, x + 8, ty, s.getLongOr("munten", 0), false, false);
            ty += 10;
        }
        if (isActief) {
            g.centeredText(font, Component.translatable("vadsrijk.karakter.scherm.actief").withStyle(ChatFormatting.BOLD), x + KAART_W / 2, y + KAART_H - 62, GROEN);
        }
        // buttons
        int by = y + KAART_H - 62 + 12;
        if (!isActief) {
            knop(g, mx, my, x + 8, by, KAART_W - 16, 14, Component.translatable("vadsrijk.karakter.scherm.spelen"), mag, () -> {
                CompoundTag a = new CompoundTag();
                a.putInt("slot", slot);
                doe("wissel", a);
            });
        }
        by += 17;
        knop(g, mx, my, x + 8, by, KAART_W - 16, 14, Component.translatable("vadsrijk.karakter.scherm.hernoem"), true, () -> {
            naamSlot = slot;
            if (naamVeld != null) {
                naamVeld.setValue(s.getStringOr("naam", ""));
            }
            zichtbaar();
        });
        by += 17;
        if (modusSlot == slot) {
            // the lower modes, one button each
            List<Modus> lager = new ArrayList<>();
            for (Modus o : Modus.values()) {
                if (m.magNaar(o)) {
                    lager.add(o);
                }
            }
            int bw = lager.isEmpty() ? 0 : (KAART_W - 16 - (lager.size() - 1) * 2) / lager.size();
            for (int i = 0; i < lager.size(); i++) {
                Modus o = lager.get(i);
                knop(g, mx, my, x + 8 + i * (bw + 2), by, bw, 14, Component.translatable(o.naamKey() + ".kort"), true, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    a.putString("modus", o.id);
                    doe("modus", a);
                });
            }
        } else {
            boolean kan = m != Modus.KNUFFEL;
            knop(g, mx, my, x + 8, by, KAART_W - 16, 14, Component.translatable("vadsrijk.karakter.scherm.makkelijker"), kan, () -> modusSlot = slot);
            tip(mx, my, x + 8, by, KAART_W - 16, 14, List.of(Component.translatable("vadsrijk.karakter.scherm.makkelijker.tip")));
        }
    }

    private void maakPaneel(GuiGraphicsExtractor g, int mx, int my) {
        kop(g, Component.translatable("vadsrijk.karakter.scherm.maak", maakSlot + 1), left + 16, top + 26);
        g.text(font, Component.translatable("vadsrijk.karakter.scherm.naam"), left + 186, top + 48, ZACHT, false);
        int y = top + 70;
        g.text(font, Component.translatable("vadsrijk.karakter.scherm.kies_modus"), left + 16, y, TEKST, false);
        y += 12;
        int bw = (w - 32 - 8) / 3;
        Modus[] modi = {Modus.KNUFFEL, Modus.NORMAAL, Modus.EENVADS};
        for (int i = 0; i < modi.length; i++) {
            Modus o = modi[i];
            int bx = left + 16 + i * (bw + 4);
            if (o == gekozen) {
                g.fill(bx - 2, y - 2, bx + bw + 2, y + 18, 0xFFB0287C);
            }
            knop(g, mx, my, bx, y, bw, 16, Badges.badge(o == Modus.NORMAAL ? "" : o.id).append(Component.literal(" ")).append(Component.translatable(o.naamKey())),
                    true, () -> gekozen = o);
        }
        y += 24;
        for (var line : font.split(Component.translatable(gekozen.uitlegKey()), w - 32)) {
            g.text(font, line, left + 16, y, TEKST, false);
            y += 10;
        }
        g.text(font, Component.translatable("vadsrijk.karakter.scherm.alleen_omlaag"), left + 16, top + h - 44, ZACHT, false);
        String naam = naamVeld == null ? "" : naamVeld.getValue().trim();
        int by = top + h - 26;
        knop(g, mx, my, left + 16, by, 120, 18, Component.translatable("vadsrijk.karakter.scherm.maak_speel"), !naam.isEmpty() && data.getBooleanOr("mag", false),
                () -> stuurMaak(true));
        knop(g, mx, my, left + 142, by, 100, 18, Component.translatable("vadsrijk.karakter.scherm.alleen_maak"), !naam.isEmpty(), () -> stuurMaak(false));
        knop(g, mx, my, left + w - 16 - 70, by, 70, 18, Component.translatable("vadsrijk.karakter.scherm.terug"), true, () -> {
            maakSlot = -1;
            zichtbaar();
        });
    }

    private void stuurMaak(boolean spelen) {
        CompoundTag a = new CompoundTag();
        a.putInt("slot", maakSlot);
        a.putString("naam", naamVeld == null ? "" : naamVeld.getValue());
        a.putString("modus", gekozen.id);
        a.putBoolean("spelen", spelen);
        doe("maak", a);
    }

    private void naamPaneel(GuiGraphicsExtractor g, int mx, int my) {
        kop(g, Component.translatable("vadsrijk.karakter.scherm.hernoem"), left + 16, top + 26);
        String naam = naamVeld == null ? "" : naamVeld.getValue().trim();
        int by = top + 70;
        knop(g, mx, my, left + 16, by, 100, 18, Component.translatable("vadsrijk.karakter.scherm.opslaan"), !naam.isEmpty(), () -> {
            CompoundTag a = new CompoundTag();
            a.putInt("slot", naamSlot);
            a.putString("naam", naamVeld == null ? "" : naamVeld.getValue());
            doe("naam", a);
        });
        knop(g, mx, my, left + 122, by, 70, 18, Component.translatable("vadsrijk.karakter.scherm.terug"), true, () -> {
            naamSlot = -1;
            zichtbaar();
        });
    }
}
