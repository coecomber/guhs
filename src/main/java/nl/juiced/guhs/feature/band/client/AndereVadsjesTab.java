package nl.juiced.guhs.feature.band.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * The Guhdex tab "Mijn andere vadsjes" (1.2.10): every tamed critter of yours that isn't a guh (pieppiepmuisjes, Poepschilly,
 * Schilly, the landdiertjes, the Guhxolotl...), a row each: its picture (the item it becomes when you pick it up), its name,
 * what it is and where it is (short; the full place with coordinates in the tooltip). The data comes with "Mijn guhs"
 * ({@link MijnGuhsCache#vadsjes}): the critters themselves don't have to be loaded.
 */
public final class AndereVadsjesTab {
    private static double scroll;
    private static final int RIJ = 24, KOP = 13;
    private static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848;

    private final GidsLijst lijst = new GidsLijst();
    private Font font;
    private int left, top, w;

    public void init(Font font, int left, int top, int w, int h) {
        this.font = font;
        this.left = left;
        this.top = top;
        this.w = w;
        lijst.plaats(left + 8, top + 28, w - 14, h - 34);
        List<GidsLijst.Regel> regels = new ArrayList<>();
        String vorige = null;
        for (MijnGuhsCache.Vadsje v : MijnGuhsCache.vadsjes()) {
            if (!v.soort().equals(vorige)) {
                vorige = v.soort();
                String soort = v.soort();
                long aantal = MijnGuhsCache.vadsjes().stream().filter(x -> x.soort().equals(soort)).count();
                regels.add(new Kop(soortNaam(soort).copy().append(" (" + aantal + ")")));
            }
            regels.add(new Rij(v));
        }
        lijst.zet(regels);
        lijst.scrollNaar(scroll);
    }

    public void bewaarScroll() {
        scroll = lijst.scroll();
    }

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (MijnGuhsCache.vadsjes().isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.anderevadsjes.leeg"), left + 20, top + 60, w - 40, 1f, TEKST);
        }
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        return lijst.tip(mx, my);
    }

    public boolean wiel(double mx, double my, double delta) {
        if (lijst.wiel(mx, my, delta)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public boolean klik(double mx, double my, int button) {
        if (lijst.klik(mx, my, button)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public boolean sleep(double my) {
        if (lijst.sleep(my)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public void los() {
        lijst.los();
    }

    /** "Pieppiepmuisje", "Schilly"...: the creature's own name (its entity id is its kind). */
    static Component soortNaam(String soort) {
        return Component.translatable("entity.guhs." + soort);
    }

    /** The item this kind becomes when you pick it up (its picture); a name tag when there is none. */
    static ItemStack icoon(String soort) {
        for (String id : new String[]{soort + "_item", soort + "_emmertje", soort}) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Guhs.MODID, id));
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.NAME_TAG);
    }

    /** A kind's heading: "Pieppiepmuisje (3)". */
    private final class Kop implements GidsLijst.Regel {
        private final Component tekst;

        Kop(Component tekst) {
            this.tekst = tekst;
        }

        @Override
        public int hoogte() {
            return KOP;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.text(font, tekst.copy().withStyle(ChatFormatting.BOLD), x + 1, y + 3, ROZE, false);
            g.fill(x, y + KOP - 1, x + w, y + KOP, 0x60D27A9C);
        }
    }

    /** One vadsje: picture, name, where it is. */
    private final class Rij implements GidsLijst.Regel {
        private final MijnGuhsCache.Vadsje v;
        private final ItemStack icoon;

        Rij(MijnGuhsCache.Vadsje v) {
            this.v = v;
            this.icoon = icoon(v.soort());
        }

        private Component naam() {
            return v.naam().getString().isEmpty() ? soortNaam(v.soort()) : v.naam();
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y, x + w, y + RIJ - 2, hover ? 0x40F77AB0 : 0x20F7B6CB);
            g.item(icoon, x + 3, y + 3);
            int tx = x + 24, breed = x + w - 4 - tx;
            GidsTekst.passend(g, naam().copy().withStyle(ChatFormatting.BOLD), tx, y + 2, breed, 1f, DONKER, false);
            GidsTekst.passend(g, MijnGuhsTab.kortePlek(v.plek()), tx, y + 13, breed, 0.75f, TEKST, false);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> out = new ArrayList<>();
            out.add(naam().copy().withStyle(ChatFormatting.BOLD));
            out.add(soortNaam(v.soort()).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            for (net.minecraft.network.chat.FormattedText r : font.getSplitter().splitLines(v.plek(), 200, net.minecraft.network.chat.Style.EMPTY)) {
                out.add(Component.literal(r.getString()).withStyle(ChatFormatting.GRAY));
            }
            return out;
        }
    }
}
