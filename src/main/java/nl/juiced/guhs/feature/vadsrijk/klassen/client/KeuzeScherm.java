package nl.juiced.guhs.feature.vadsrijk.klassen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads;

/**
 * The first class choice in Knuffelkamp's klasse-tempeltje: three big cards (Knuffelridder, Pluisjager, Vadsmagier) with
 * their role, weapons and the four skills, and a "Word ...!" button under each.
 */
public final class KeuzeScherm extends Screen {
    private static final int KAART_B = 126, KAART_H = 206, GAT = 8;
    private static final String[] TOETS = {"R", "V", "G", "Z"};
    private CompoundTag t;
    private int left, top;

    public KeuzeScherm(CompoundTag t) {
        super(Component.translatable("vadsrijk.klassen.kies.kop"));
        this.t = t;
    }

    public void zet(CompoundTag t) {
        this.t = t;
    }

    private static int kleur(String id) {
        return switch (id) {
            case Klassen.JAGER -> 0xFF6FD8A8;
            case Klassen.MAGIER -> 0xFFB58CFF;
            default -> 0xFFFF7AC8;
        };
    }

    private static String[] wapens(String id) {
        return switch (id) {
            case Klassen.JAGER -> new String[] {"guhs:vr_kl_pluisboog", "guhs:vr_kl_snoepkruisboog", "guhs:vr_kl_eikelkatapult"};
            case Klassen.MAGIER -> new String[] {"guhs:vr_kl_rozenstaf", "guhs:vr_kl_zwamstokje", "guhs:vr_kl_slijmboek"};
            default -> new String[] {"guhs:vr_kussenzwaard", "guhs:vr_knuffelknots", "guhs:vr_pluishamer"};
        };
    }

    private List<Klasse> klassen() {
        List<Klasse> l = new ArrayList<>();
        for (String id : new String[] {Klassen.RIDDER, Klassen.JAGER, Klassen.MAGIER}) {
            Klasse k = Klassen.van(id);
            if (k != null) {
                l.add(k);
            }
        }
        return l;
    }

    @Override
    protected void init() {
        int n = klassen().size();
        int totaal = n * KAART_B + (n - 1) * GAT;
        left = (width - totaal) / 2;
        top = Math.max(26, (height - KAART_H) / 2 + 8);
        int i = 0;
        for (Klasse k : klassen()) {
            int x = left + i * (KAART_B + GAT);
            addRenderableWidget(Button.builder(Component.translatable("vadsrijk.klassen.kies.knop", Component.translatable(k.naamKey())).withStyle(ChatFormatting.BOLD),
                    b -> {
                        ClientPacketDistributor.sendToServer(new KlassenPayloads.Actie("kies", k.id()));
                        onClose();
                    }).bounds(x + 6, top + KAART_H - 24, KAART_B - 12, 18).build());
            i++;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        g.fillGradient(0, 0, width, height, 0xA0401A34, 0xD02A1022);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, top - 22);
        g.pose().scale(1.6f, 1.6f);
        g.centeredText(font, Component.translatable("vadsrijk.klassen.kies.kop").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFC2E8);
        g.pose().popMatrix();
        g.centeredText(font, Component.translatable("vadsrijk.klassen.kies.onderkop"), width / 2, top - 8, 0xFFD8B8CC);
        int i = 0;
        for (Klasse k : klassen()) {
            int x = left + i * (KAART_B + GAT), y = top;
            int kl = kleur(k.id());
            boolean hover = mx >= x && mx < x + KAART_B && my >= y && my < y + KAART_H;
            g.fill(x - 2, y - 2, x + KAART_B + 2, y + KAART_H + 2, hover ? 0xFFFFFFFF : kl);
            g.fill(x, y, x + KAART_B, y + KAART_H, 0xF0301424);
            g.fillGradient(x, y, x + KAART_B, y + 40, kl, 0xF0301424);
            g.pose().pushMatrix();
            g.pose().translate(x + KAART_B / 2f, y + 6);
            g.pose().scale(1.25f, 1.25f);
            g.centeredText(font, Component.translatable(k.naamKey()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF);
            g.pose().popMatrix();
            g.centeredText(font, Component.translatable("vadsrijk.klassen.kies.rol." + k.id()), x + KAART_B / 2, y + 20, 0xFFFFF0F8);
            String[] w = wapens(k.id());
            for (int j = 0; j < w.length; j++) {
                g.item(stack(w[j]), x + KAART_B / 2 - 28 + j * 20, y + 31);
            }
            int ty = y + 52;
            List<Vaardigheid> vs = k.vaardigheden();
            for (int j = 0; j < vs.size() && j < 4; j++) {
                Vaardigheid v = vs.get(j);
                g.fill(x + 5, ty, x + 15, ty + 10, j == 3 ? 0xFFFFD24A : kl);
                g.centeredText(font, TOETS[j], x + 10, ty + 1, 0xFF2A1022);
                g.text(font, Component.translatable(v.naamKey()).withStyle(ChatFormatting.BOLD), x + 18, ty + 1, 0xFFFFFFFF, false);
                ty += 11;
                g.pose().pushMatrix();
                g.pose().translate(x + 7, ty);
                g.pose().scale(0.75f, 0.75f);
                int ry = 0;
                for (FormattedCharSequence r : font.split(Component.translatable("vadsrijk.klassen.kort." + v.id()), (int) ((KAART_B - 12) / 0.75f))) {
                    g.text(font, r, 0, ry, 0xFFE8C8DA, false);
                    ry += 9;
                }
                g.pose().popMatrix();
                ty += (int) (ry * 0.75f) + 4;
            }
            i++;
        }
        super.extractRenderState(g, mx, my, pt);
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
