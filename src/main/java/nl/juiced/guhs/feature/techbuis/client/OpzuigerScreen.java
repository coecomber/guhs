package nl.juiced.guhs.feature.techbuis.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.feature.techbuis.OpzuigerMenu;

/** The Opzuiger's screen: its nine slots on the vanilla 3 x 3 background (a dispenser's), and your inventory. */
public class OpzuigerScreen extends AbstractContainerScreen<OpzuigerMenu> {
    private static final Identifier ACHTERGROND = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    public OpzuigerScreen(OpzuigerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, ACHTERGROND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }
}
