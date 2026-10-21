package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A slot-sized button showing an item (the hotbar picker of a block or mix setting). */
final class ItemKnop extends AbstractButton {
    private final ItemStack stack;
    private final Runnable actie;

    ItemKnop(int x, int y, ItemStack stack, Runnable actie) {
        super(x, y, 18, 18, stack.isEmpty() ? Component.empty() : stack.getHoverName());
        this.stack = stack;
        this.actie = actie;
        this.active = !stack.isEmpty();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        actie.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(getX(), getY(), getX() + 18, getY() + 18, isHovered() && active ? 0xFFE07AAE : 0xFF8B7355);
        g.fill(getX() + 1, getY() + 1, getX() + 17, getY() + 17, 0xFFF3E7D3);
        if (!stack.isEmpty()) {
            g.item(stack, getX() + 1, getY() + 1);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
