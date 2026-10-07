package nl.juiced.guhs.feature.einde2.vetbootje.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.vetbootje.Vetbootje;
import nl.juiced.guhs.feature.einde2.vetbootje.WerfMenu;
import nl.juiced.guhs.feature.einde2.vetbootje.Zeil;

/**
 * The scheepswerf: paint your vetbootje. Sixteen pots of paint, six sails, a name (or let the werf make one up); on the
 * left the sail as it will look. "Verf het!" sends the choice ({@link WerfMenu.Kies}); closing the screen changes nothing.
 */
public class WerfScreen extends Screen implements MenuAccess<WerfMenu> {
    private static final int W = 276, H = 196;
    private static final int BG = 0xF02A1A12, RAND = 0xFFF6B42E, TEKST = 0xFFFFF0D0, ZACHT = 0xFFD8B890;
    /** The size of the small sail pictures (textures/gui/einde2_vetbootje_zeil_*.png). */
    private static final int ZEIL_B = 36, ZEIL_H = 42;
    /** How many names the werf can make up (gui.guhs.einde2_vetbootje.naam.idee.N). */
    private static final int IDEEEN = 12;

    private final WerfMenu menu;
    private int kleur, zeil, left, top, idee;
    private EditBox naamVak;

    public WerfScreen(WerfMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.kleur = Math.floorMod(menu.kleur, 16);
        this.zeil = Math.floorMod(menu.zeil, Zeil.values().length);
        this.idee = (int) (Math.random() * IDEEEN);
    }

    @Override
    public WerfMenu getMenu() {
        return menu;
    }

    private static Identifier plaatje(int zeil) {
        return Guhs.id("textures/gui/einde2_vetbootje_zeil_" + Zeil.values()[zeil].id() + ".png");
    }

    private static int verf(int kleur) {
        return 0xFF000000 | DyeColor.byId(kleur).getTextureDiffuseColor();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        String getypt = naamVak == null ? menu.naam : naamVak.getValue();
        // the sixteen pots of paint: two rows of eight
        for (int i = 0; i < 16; i++) {
            int k = i;
            addRenderableWidget(Button.builder(Component.empty(), b -> kleur = k)
                    .bounds(left + 96 + (i % 8) * 21, top + 34 + (i / 8) * 21, 19, 19).build());
        }
        // the six sails
        Zeil[] zeilen = Zeil.values();
        for (int i = 0; i < zeilen.length; i++) {
            int z = i;
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_vetbootje.zeil." + zeilen[i].id()), b -> zeil = z)
                    .bounds(left + 96 + (i % 3) * 57, top + 94 + (i / 3) * 22, 55, 20).build());
        }
        naamVak = new EditBox(font, left + 97, top + 152, 108, 18, Component.translatable("gui.guhs.einde2_vetbootje.werf.naam"));
        naamVak.setHint(Component.translatable("gui.guhs.einde2_vetbootje.werf.naam_hint").withStyle(ChatFormatting.GRAY));
        naamVak.setMaxLength(Vetbootje.NAAM_MAX);
        naamVak.setValue(getypt);
        addRenderableWidget(naamVak);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_vetbootje.werf.verzin"), b -> {
            idee = (idee + 1) % IDEEEN;
            naamVak.setValue(I18n.get("gui.guhs.einde2_vetbootje.naam.idee." + idee));
        }).bounds(left + 209, top + 151, 57, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_vetbootje.werf.verf").withStyle(ChatFormatting.GOLD), b -> {
            ClientPacketDistributor.sendToServer(new WerfMenu.Kies(kleur, zeil, naamVak.getValue()));
            onClose();
        }).bounds(left + 10, top + 151, 76, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, BG);
        g.text(font, title, left + 10, top + 8, TEKST);
        g.text(font, Component.translatable("gui.guhs.einde2_vetbootje.werf.kleur"), left + 96, top + 23, RAND);
        g.text(font, Component.translatable("gui.guhs.einde2_vetbootje.werf.zeil"), left + 96, top + 83, RAND);
        g.text(font, Component.translatable("gui.guhs.einde2_vetbootje.werf.naam"), left + 96, top + 141, RAND);
        g.text(font, Component.translatable("gui.guhs.einde2_vetbootje.werf.voet"), left + 10, top + H - 14, ZACHT);
        // the mast with the sail as it will look, twice as big as its picture
        int zx = left + 12, zy = top + 30;
        g.fill(zx + ZEIL_B - 2, zy - 6, zx + ZEIL_B + 2, zy + ZEIL_H * 2 + 22, 0xFF48301F);
        g.fill(zx - 4, zy - 2, zx + ZEIL_B * 2 + 4, zy + 2, 0xFF48301F);
        g.fill(zx, zy + 2, zx + ZEIL_B * 2, zy + 2 + ZEIL_H * 2, verf(kleur));
        g.blit(RenderPipelines.GUI_TEXTURED, plaatje(zeil), zx, zy + 2, 0.0F, 0.0F, ZEIL_B * 2, ZEIL_H * 2, ZEIL_B * 2, ZEIL_H * 2);
        g.fill(zx - 2, zy + ZEIL_H * 2 + 20, zx + ZEIL_B * 2 + 2, zy + ZEIL_H * 2 + 24, 0xFFD47A36);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // the paint in the pots (over the buttons), a honey ring round the chosen one
        for (int i = 0; i < 16; i++) {
            int x = left + 96 + (i % 8) * 21, y = top + 34 + (i / 8) * 21;
            if (i == kleur) {
                g.fill(x - 1, y - 1, x + 20, y + 20, RAND);
            }
            g.fill(x + 3, y + 3, x + 16, y + 16, verf(i));
        }
        Zeil[] zeilen = Zeil.values();
        int x = left + 96 + (zeil % 3) * 57, y = top + 94 + (zeil / 3) * 22;
        g.outline(x - 1, y - 1, 57, 22, RAND);
        g.centeredText(font, Component.translatable("gui.guhs.einde2_vetbootje.zeil." + zeilen[zeil].id()), left + 12 + ZEIL_B, top + 30 + ZEIL_H * 2 + 8, TEKST);
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
