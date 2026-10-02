package nl.juiced.guhs.feature.guhoven.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.FurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhoven.GuhOvenMenu;

/**
 * The Guhoven's screen: the vanilla furnace screen (its background, arrow and recipe book), but where a furnace has its flame and
 * fuel slot there is a little guh wheel: grey while the oven has no guh power, bright pink while it bakes (with a tooltip).
 */
public class GuhOvenScreen extends AbstractRecipeBookScreen<GuhOvenMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier WIEL = Guhs.id("textures/gui/guh_oven_wiel.png");
    private static final Identifier WIEL_AAN = Guhs.id("textures/gui/guh_oven_wiel_aan.png");
    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");
    private static final List<RecipeBookComponent.TabInfo> TABS = List.of(
            new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.FURNACE),
            new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD),
            new RecipeBookComponent.TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
            new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC));
    /** Where the wheel sits (relative to the screen): in the column of the input slot, where the flame and fuel slot were. */
    private static final int WIEL_X = 52, WIEL_Y = 42, WIEL_GROOTTE = 24;
    /** The light grey of the vanilla container background, to paint over the furnace's flame outline and fuel slot. */
    private static final int ACHTERGROND = 0xFFC6C6C6;

    public GuhOvenScreen(GuhOvenMenu menu, Inventory inventory, Component title) {
        super(menu, new FurnaceRecipeBookComponent(menu, FILTER_NAME, TABS), inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 20, this.height / 2 - 49);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        g.fill(x + 53, y + 35, x + 76, y + 71, ACHTERGROND);   // no flame, no fuel slot
        g.blit(RenderPipelines.GUI_TEXTURED, this.menu.bakt() ? WIEL_AAN : WIEL, x + WIEL_X, y + WIEL_Y, 0.0F, 0.0F,
                WIEL_GROOTTE, WIEL_GROOTTE, WIEL_GROOTTE, WIEL_GROOTTE);
        int burn = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x + 79, y + 34, burn, 16);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (isHovering(WIEL_X, WIEL_Y, WIEL_GROOTTE, WIEL_GROOTTE, mouseX, mouseY)) {
            Component tekst = Component.translatable(this.menu.bakt() ? "gui.guhs.guh_oven.aan" : "gui.guhs.guh_oven.uit");
            g.setTooltipForNextFrame(this.font, this.font.split(tekst, 170), mouseX, mouseY);
        }
    }
}
