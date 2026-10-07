package nl.juiced.guhs.feature.snuffel.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.snuffel.Boom;
import nl.juiced.guhs.feature.snuffel.GeurSoort;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Rang;

/**
 * Het snuffelboekje: everything your nose has learned. On the left who you are (your dog's name and breed, your rank as
 * "Snuffelpup (rang 1 (laagste) van 5 (hoogste))", the list of all five ranks with what each asks, your good deeds, your
 * tree, your diploma); on the right every scent you learned, with the colour of its kind. Opened with its key (N), from
 * the inventory key while you are a dog, and from the memory card's menu.
 */
public final class BoekjeScherm extends Screen {
    private static final int W = 356, H = 226;
    private static final int PAPIER = 0xFFF6ECD6, RAND = 0xFF8A6A3A, INKT = 0xFF4A3220, ZACHT = 0xFF7A5A3A, LIJN = 0xFFD9C49A, GROEN = 0xFF3F8A3A, GRIJS = 0xFF9A8E7C;
    private static final int PER_PAGINA = 8, MIDDEN = 186;

    private int left, top, pagina;

    public BoekjeScherm() {
        super(Component.translatable("gui.guhs.snuffel.boekje.titel"));
    }

    static ItemStack icoon(String id) {
        Identifier rl = Identifier.tryParse(id);
        var item = rl == null ? Items.BONE : BuiltInRegistries.ITEM.getValue(rl);
        return new ItemStack(item == Items.AIR ? Items.BONE : item);
    }

    private int paginas() {
        return Math.max(1, (EigenStand.geuren().size() + PER_PAGINA - 1) / PER_PAGINA);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        pagina = Math.min(pagina, paginas() - 1);
        if (paginas() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                pagina = Math.max(0, pagina - 1);
            }).bounds(left + W - 60, top + H - 24, 20, 18).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                pagina = Math.min(paginas() - 1, pagina + 1);
            }).bounds(left + W - 34, top + H - 24, 20, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.boekje.dicht"), b -> onClose()).bounds(left + 10, top + H - 24, 70, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        g.fill(left + MIDDEN, top + 22, left + MIDDEN + 1, top + H - 30, LIJN);
        g.centeredText(font, Component.translatable("gui.guhs.snuffel.boekje.van", EigenStand.naam()).withStyle(ChatFormatting.BOLD), left + W / 2, top + 8, INKT);

        // --- left: who you are ---
        int x = left + 10, y = top + 26;
        Honden.Ras ras = Honden.ras(EigenStand.ras());
        if (ras != null) {
            g.text(font, Component.translatable("gui.guhs.snuffel.boekje.hond", ras.naam(), ras.kleurNaam(EigenStand.kleur())), x, y, ZACHT, false);
        }
        y += 12;
        Rang rang = EigenStand.rang();
        for (var regel : font.split(rang.regel().copy().withStyle(ChatFormatting.BOLD), MIDDEN - 20)) {
            g.text(font, regel, x, y, INKT, false);
            y += 10;
        }
        g.text(font, Component.translatable("gui.guhs.snuffel.boekje.aantal", EigenStand.aantal()), x, y, ZACHT, false);
        y += 14;
        g.text(font, Component.translatable("gui.guhs.snuffel.boekje.rangen"), x, y, ZACHT, false);
        y += 11;
        for (Rang r : Rang.values()) {
            boolean heb = r.ordinal() <= rang.ordinal();
            g.text(font, Component.literal(heb ? "✔" : "•"), x, y, heb ? GROEN : GRIJS, false);
            klein(g, r.lijstRegel(), x + 10, y + 1, heb ? INKT : GRIJS);
            y += 9;
        }
        klein(g, Component.translatable("gui.guhs.snuffel.boekje.later"), x, y + 2, GRIJS);
        y += 14;
        List<String> daden = EigenStand.daden();
        g.text(font, Component.translatable("gui.guhs.snuffel.boekje.daden", daden.size()), x, y, INKT, false);
        y += 10;
        g.text(font, Component.translatable("gui.guhs.snuffel.boekje.boom", Component.translatable("gui.guhs.snuffel.boom.stap." + Math.min(Boom.MAX, EigenStand.boom()))),
                x, y, INKT, false);
        y += 10;
        if (EigenStand.heeftMaatje()) {
            g.text(font, Component.translatable("gui.guhs.snuffel.boekje.maatje", Honden.maatjeNaam(EigenStand.maatje())), x, y, INKT, false);
            y += 10;
        }
        if (EigenStand.diploma()) {
            g.text(font, Component.translatable("gui.guhs.snuffel.boekje.diploma"), x, y, GROEN, false);
        }

        // --- right: the scents ---
        int rx = left + MIDDEN + 8, ry = top + 26;
        g.text(font, Component.translatable("gui.guhs.snuffel.boekje.geuren"), rx, ry, ZACHT, false);
        // the legend: the four kinds and their colours
        int lx = rx;
        for (GeurSoort s : GeurSoort.values()) {
            g.fill(lx, top + H - 21, lx + 6, top + H - 15, s.kleur());
            Component naam = s.naam();
            g.text(font, naam, lx + 8, top + H - 22, ZACHT, false);
            lx += 12 + font.width(naam);
            if (lx > left + W - 70) {
                break;
            }
        }
        ry += 12;
        List<EigenStand.Geur> geuren = EigenStand.geuren();
        if (geuren.isEmpty()) {
            for (var regel : font.split(Component.translatable("gui.guhs.snuffel.boekje.leeg"), W - MIDDEN - 18)) {
                g.text(font, regel, rx, ry, GRIJS, false);
                ry += 10;
            }
        }
        for (int i = pagina * PER_PAGINA; i < Math.min(geuren.size(), (pagina + 1) * PER_PAGINA); i++) {
            EigenStand.Geur geur = geuren.get(i);
            int gy = ry + (i - pagina * PER_PAGINA) * 19;
            g.fill(rx, gy, rx + 3, gy + 16, geur.soort().kleur());
            g.item(icoon(geur.icoon()), rx + 5, gy);
            var naam = font.split(geur.naam(), W - MIDDEN - 40);
            for (int r = 0; r < Math.min(2, naam.size()); r++) {
                g.text(font, naam.get(r), rx + 24, gy + (naam.size() > 1 ? 0 : 4) + r * 9, INKT, false);
            }
        }
    }

    /** A line at 80 % size (the long rank names fit the column). */
    private void klein(GuiGraphicsExtractor g, Component tekst, int x, int y, int kleur) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.8f, 0.8f);
        g.text(font, tekst, 0, 0, kleur, false);
        g.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
