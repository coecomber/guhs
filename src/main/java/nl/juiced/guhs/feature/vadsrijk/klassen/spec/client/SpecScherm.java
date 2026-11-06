package nl.juiced.guhs.feature.vadsrijk.klassen.spec.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;

/**
 * U-SPECS: the spec screen. Two big cards (spec A and B of the class shown): the style, what each advancement step gives (with
 * the spec's titles), and one button: "Word ...!" for the first choice, "Wissel naar ..." at a class NPC (once per 10 min, not
 * in a fight), or a grey "Jouw specialisatie". Same pink style as the class choice ({@code KeuzeScherm}).
 */
public final class SpecScherm extends Screen {
    private static final int KAART_B = 170, KAART_H = 214, GAT = 12;
    private CompoundTag t;
    private int left, top;

    public SpecScherm(CompoundTag t) {
        super(Component.translatable("vadsrijk.klassen.spec.kop"));
        this.t = t;
    }

    public void zet(CompoundTag t) {
        this.t = t;
        rebuildWidgets();
    }

    private String klasse() {
        return t.getStringOr("klasse", Klassen.RIDDER);
    }

    private List<Spec> specs() {
        return Spec.vanKlasse(klasse());
    }

    @Override
    protected void init() {
        left = (width - (2 * KAART_B + GAT)) / 2;
        top = Math.max(30, (height - KAART_H) / 2 + 8);
        String mijn = t.getStringOr("spec", "");
        boolean open = t.getBooleanOr("specOpen", false);
        String nee = t.getStringOr("specWisselNee", "");
        int i = 0;
        for (Spec s : specs()) {
            int x = left + i * (KAART_B + GAT);
            Component naam = Component.translatable(s.naamKey());
            Button b;
            if (open) {
                b = Button.builder(Component.translatable("vadsrijk.klassen.spec.word", naam).withStyle(ChatFormatting.BOLD), x2 -> {
                    ClientPacketDistributor.sendToServer(new KlassenPayloads.Actie("spec_kies", s.id));
                    onClose();
                }).bounds(x + 8, top + KAART_H - 24, KAART_B - 16, 18).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.spec.word.tip"))).build();
            } else if (s.id.equals(mijn)) {
                b = Button.builder(Component.translatable("vadsrijk.klassen.spec.jouw"), x2 -> {
                }).bounds(x + 8, top + KAART_H - 24, KAART_B - 16, 18).build();
                b.active = false;
            } else if (!mijn.isEmpty()) {
                long sec = t.getLongOr("specWissel", 0);
                Component tip = nee.isEmpty() ? Component.translatable("vadsrijk.klassen.spec.wissel.tip")
                        : Component.translatable("vadsrijk.klassen.spec.nee." + nee, sec >= 60 ? (sec / 60 + 1) + " min" : sec + " s");
                b = Button.builder(Component.translatable("vadsrijk.klassen.spec.wissel", naam), x2 ->
                        ClientPacketDistributor.sendToServer(new KlassenPayloads.Actie("spec_wissel", "")))
                        .bounds(x + 8, top + KAART_H - 24, KAART_B - 16, 18).tooltip(Tooltip.create(tip)).build();
                b.active = nee.isEmpty();
            } else {
                b = Button.builder(Component.translatable("vadsrijk.klassen.spec.later"), x2 -> {
                }).bounds(x + 8, top + KAART_H - 24, KAART_B - 16, 18)
                        .tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.spec.later.tip"))).build();
                b.active = false;
            }
            addRenderableWidget(b);
            i++;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 40, top + KAART_H + 8, 80, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        g.fillGradient(0, 0, width, height, 0xA0401A34, 0xD02A1022);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, top - 26);
        g.pose().scale(1.5f, 1.5f);
        g.centeredText(font, Component.translatable("vadsrijk.klassen.spec.kop").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFC2E8);
        g.pose().popMatrix();
        g.centeredText(font, Component.translatable(t.getBooleanOr("specOpen", false) ? "vadsrijk.klassen.spec.onderkop.kies" : "vadsrijk.klassen.spec.onderkop",
                Component.translatable(Klassen.ofRidder(klasse()).naamKey())), width / 2, top - 11, 0xFFD8B8CC);
        String mijn = t.getStringOr("spec", "");
        int stap = t.getIntOr("adv_" + klasse(), 0);
        int i = 0;
        for (Spec s : specs()) {
            int x = left + i * (KAART_B + GAT), y = top;
            int kl = 0xFF000000 | s.kleur;
            boolean hover = mx >= x && mx < x + KAART_B && my >= y && my < y + KAART_H;
            boolean jouw = s.id.equals(mijn);
            g.fill(x - 2, y - 2, x + KAART_B + 2, y + KAART_H + 2, hover || jouw ? 0xFFFFFFFF : kl);
            g.fill(x, y, x + KAART_B, y + KAART_H, 0xF0301424);
            g.fillGradient(x, y, x + KAART_B, y + 40, kl, 0xF0301424);
            g.item(stack(s.icoon), x + 6, y + 6);
            g.pose().pushMatrix();
            g.pose().translate(x + KAART_B / 2f, y + 7);
            g.pose().scale(1.3f, 1.3f);
            g.centeredText(font, Component.translatable(s.naamKey()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF);
            g.pose().popMatrix();
            g.centeredText(font, Component.translatable("vadsrijk.klassen.spec." + s.id + ".rol"), x + KAART_B / 2, y + 23, 0xFFFFF0F8);
            int ty = y + 42;
            ty = tekst(g, Component.translatable("vadsrijk.klassen.spec." + s.id + ".kort"), x + 7, ty, KAART_B - 14, 0xFFE8C8DA, 0.8f) + 4;
            for (int st = 1; st <= 3; st++) {
                boolean heeft = jouw && stap >= st;
                g.fill(x + 6, ty, x + 16, ty + 10, heeft ? 0xFFFFD24A : kl);
                g.centeredText(font, String.valueOf(st), x + 11, ty + 1, 0xFF2A1022);
                g.text(font, Component.translatable(s.titelKey(st)).withStyle(ChatFormatting.BOLD), x + 19, ty + 1, heeft ? 0xFFFFD24A : 0xFFFFFFFF, false);
                Component lv = Component.translatable("vadsrijk.klassen.spec.stap_lv", st == 1 ? 17 : st == 2 ? 35 : 58);
                g.text(font, lv, x + KAART_B - 6 - font.width(lv), ty + 1, 0xFF8A6A7E, false);
                ty += 11;
                ty = tekst(g, Component.translatable("vadsrijk.klassen.adv.kort." + s.id + "." + st), x + 9, ty, KAART_B - 16, 0xFFE8C8DA, 0.7f) + 3;
            }
            i++;
        }
        super.extractRenderState(g, mx, my, pt);
    }

    private int tekst(GuiGraphicsExtractor g, Component c, int x, int y, int breed, int kleur, float schaal) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(schaal, schaal);
        int ry = 0;
        for (FormattedCharSequence r : font.split(c, (int) (breed / schaal))) {
            g.text(font, r, 0, ry, kleur, false);
            ry += 9;
        }
        g.pose().popMatrix();
        return y + (int) Math.ceil(ry * schaal);
    }

    private static ItemStack stack(String id) {
        Item it = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return new ItemStack(it == null ? Items.PINK_DYE : it);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
