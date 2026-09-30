package nl.juiced.guhs.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.network.BankActionPayload;
import nl.juiced.guhs.storage.BankContents;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Bank Guh's stomach: search box, sorting, category filter, a scrollable grid of everything stored,
 * a 3x3 crafting grid and your inventory.
 * <p>
 * In the grid: left-click takes a stack, right-click half a stack, shift-click puts a stack in your inventory;
 * clicking while holding something puts it in (right-click: just one). Shift-click in your inventory stores the stack.
 */
public class BankGuhScreen extends AbstractContainerScreen<BankGuhMenu> {
    private static final int BG = 0xFF3A1F2C;
    private static final int PANEL = 0xFFF7D4E0;
    private static final int PANEL_DARK = 0xFFD99AB2;
    private static final int SLOT = 0xFF8B5A6E;
    private static final int SLOT_HOVER = 0x80FFFFFF;
    private static final int TEXT = 0xFF5A2640;

    enum Sort { NAME, COUNT, MOD }

    enum Filter { ALL, BLOCKS, TOOLS, FOOD, OTHER }

    private EditBox search;
    private Sort sort = Sort.COUNT;
    private boolean descending = true;
    private Filter filter = Filter.ALL;
    private int scrollRow;
    private List<BankContents.Entry> visible = List.of();
    private BankContents lastContents;
    private String lastQuery = "";

    public BankGuhScreen(BankGuhMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 195;
        this.imageHeight = 262;
        this.inventoryLabelX = BankGuhMenu.INV_X - 1;
        this.inventoryLabelY = BankGuhMenu.INV_Y - 11;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 8, topPos + 17, 86, 12, Component.translatable("gui.guhs.bank.search"));
        search.setHint(Component.translatable("gui.guhs.bank.search").withStyle(ChatFormatting.GRAY));
        search.setBordered(true);
        search.setMaxLength(50);
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(sortLabel(), b -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            b.setMessage(sortLabel());
            refresh(true);
        }).bounds(leftPos + 97, topPos + 16, 44, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.bank.sort.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.literal(descending ? "↓" : "↑"), b -> {
            descending = !descending;
            b.setMessage(Component.literal(descending ? "↓" : "↑"));
            refresh(true);
        }).bounds(leftPos + 142, topPos + 16, 14, 14).build());
        addRenderableWidget(Button.builder(filterLabel(), b -> {
            filter = Filter.values()[(filter.ordinal() + 1) % Filter.values().length];
            b.setMessage(filterLabel());
            refresh(true);
        }).bounds(leftPos + 157, topPos + 16, 30, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.bank.filter.tooltip"))).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bank.deposit"),
                        b -> send(BankGuhMenu.Action.DEPOSIT_INVENTORY, ItemStack.EMPTY, 0, false))
                .bounds(leftPos + 126, topPos + 112, 61, 16).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.guhs.bank.deposit.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bank.clear_grid"),
                        b -> send(BankGuhMenu.Action.CLEAR_GRID, ItemStack.EMPTY, 0, false))
                .bounds(leftPos + 126, topPos + 131, 61, 16).build());
        refresh(true);
    }

    private Component sortLabel() {
        return Component.translatable("gui.guhs.bank.sort." + sort.name().toLowerCase(Locale.ROOT));
    }

    private Component filterLabel() {
        return Component.translatable("gui.guhs.bank.filter." + filter.name().toLowerCase(Locale.ROOT));
    }

    // --- building the visible list ---

    private void refresh(boolean force) {
        BankContents contents = menu.getClientContents();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (!force && contents == lastContents && query.equals(lastQuery)) {
            return;
        }
        lastContents = contents;
        lastQuery = query;
        List<BankContents.Entry> list = new ArrayList<>();
        for (BankContents.Entry e : contents.entries()) {
            if (matchesFilter(e.item()) && matchesSearch(e.item(), query)) {
                list.add(e);
            }
        }
        Comparator<BankContents.Entry> byName = Comparator.comparing(e -> e.item().getHoverName().getString().toLowerCase(Locale.ROOT));
        Comparator<BankContents.Entry> cmp = switch (sort) {
            case NAME -> byName;
            case COUNT -> Comparator.<BankContents.Entry>comparingLong(BankContents.Entry::count).thenComparing(byName.reversed());
            case MOD -> Comparator.<BankContents.Entry, String>comparing(e -> BuiltInRegistries.ITEM.getKey(e.item().getItem()).getNamespace())
                    .thenComparing(byName);
        };
        list.sort(descending ? cmp.reversed() : cmp);
        visible = list;
        scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
    }

    /** "@mod" searches by mod id, anything else by name (and tooltip-less id). */
    private static boolean matchesSearch(ItemStack stack, String query) {
        if (query.isEmpty()) {
            return true;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (query.startsWith("@")) {
            return id.getNamespace().contains(query.substring(1));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query) || id.getPath().contains(query);
    }

    private boolean matchesFilter(ItemStack stack) {
        Item item = stack.getItem();
        boolean block = item instanceof BlockItem;
        boolean tool = stack.isDamageableItem();
        boolean food = stack.has(DataComponents.FOOD);
        return switch (filter) {
            case ALL -> true;
            case BLOCKS -> block;
            case TOOLS -> tool;
            case FOOD -> food;
            case OTHER -> !block && !tool && !food;
        };
    }

    private int totalRows() {
        return (visible.size() + BankGuhMenu.GRID_COLS - 1) / BankGuhMenu.GRID_COLS;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - BankGuhMenu.GRID_ROWS);
    }

    // --- drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        refresh(false);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        renderGridTooltip(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BG);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        // storage grid
        for (int r = 0; r < BankGuhMenu.GRID_ROWS; r++) {
            for (int c = 0; c < BankGuhMenu.GRID_COLS; c++) {
                slotBox(g, x + BankGuhMenu.GRID_X + c * 18, y + BankGuhMenu.GRID_Y + r * 18);
            }
        }
        // scrollbar
        int barX = x + BankGuhMenu.GRID_X + BankGuhMenu.GRID_COLS * 18 + 3;
        int barTop = y + BankGuhMenu.GRID_Y;
        int barHeight = BankGuhMenu.GRID_ROWS * 18;
        g.fill(barX, barTop, barX + 8, barTop + barHeight, PANEL_DARK);
        int thumb = maxScroll() == 0 ? barHeight : Math.max(10, barHeight * BankGuhMenu.GRID_ROWS / Math.max(1, totalRows()));
        int thumbY = maxScroll() == 0 ? barTop : barTop + (barHeight - thumb) * scrollRow / maxScroll();
        g.fill(barX, thumbY, barX + 8, thumbY + thumb, SLOT);
        // crafting grid, arrow, result
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                slotBox(g, x + BankGuhMenu.CRAFT_X - 1 + c * 18, y + BankGuhMenu.CRAFT_Y - 1 + r * 18);
            }
        }
        g.text(font, "➜", x + 81, y + 135, TEXT, false);
        g.fill(x + BankGuhMenu.RESULT_X - 5, y + BankGuhMenu.RESULT_Y - 5, x + BankGuhMenu.RESULT_X + 21, y + BankGuhMenu.RESULT_Y + 21, PANEL_DARK);
        slotBox(g, x + BankGuhMenu.RESULT_X - 1, y + BankGuhMenu.RESULT_Y - 1);
        // inventory
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                slotBox(g, x + BankGuhMenu.INV_X - 1 + c * 18, y + BankGuhMenu.INV_Y - 1 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            slotBox(g, x + BankGuhMenu.INV_X - 1 + c * 18, y + BankGuhMenu.INV_Y + 57);
        }
    }

    private void slotBox(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFB98398);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        long total = menu.getClientContents().totalItems();
        Component summary = Component.translatable("gui.guhs.bank.summary", shortCount(total), menu.getClientContents().entries().size());
        g.text(font, summary, imageWidth - 8 - font.width(summary), titleLabelY, 0xFF8B4A68, false);

        // the stored items
        int start = scrollRow * BankGuhMenu.GRID_COLS;
        for (int i = 0; i < BankGuhMenu.GRID_COLS * BankGuhMenu.GRID_ROWS && start + i < visible.size(); i++) {
            BankContents.Entry e = visible.get(start + i);
            int sx = BankGuhMenu.GRID_X + 1 + (i % BankGuhMenu.GRID_COLS) * 18;
            int sy = BankGuhMenu.GRID_Y + 1 + (i / BankGuhMenu.GRID_COLS) * 18;
            g.item(e.item(), sx, sy);
            String count = shortCount(e.count());
            g.pose().pushMatrix();
            g.pose().translate(0, 0);
            g.pose().scale(0.66f, 0.66f);
            float tx = (sx + 17 - font.width(count) * 0.66f) / 0.66f;
            float ty = (sy + 11) / 0.66f;
            g.text(font, count, (int) tx, (int) ty, 0xFFFFFFFF, true);
            g.pose().popMatrix();
            if (isHovering(sx - 1, sy - 1, 18, 18, mouseX, mouseY)) {
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_HOVER);
            }
        }
    }

    private void renderGridTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int index = gridIndexAt(mouseX, mouseY);
        if (index < 0 || !menu.getCarried().isEmpty()) {
            return;
        }
        BankContents.Entry e = visible.get(index);
        List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(e.item()));
        lines.add(Component.translatable("gui.guhs.bank.stored", String.format(Locale.ROOT, "%,d", e.count())).withStyle(ChatFormatting.LIGHT_PURPLE));
        g.setTooltipForNextFrame(font, lines, e.item().getTooltipImage(), e.item(), mouseX, mouseY);
    }

    public static String shortCount(long n) {
        if (n < 1000) return Long.toString(n);
        if (n < 1_000_000) return trim(n / 1000.0) + "k";
        if (n < 1_000_000_000) return trim(n / 1_000_000.0) + "M";
        return trim(n / 1_000_000_000.0) + "B";
    }

    private static String trim(double v) {
        return v >= 100 ? Long.toString((long) v) : String.format(Locale.ROOT, "%.1f", v).replace(".0", "");
    }

    // --- input ---

    /** Index into {@link #visible} under the mouse, or -1 (-2 = an empty grid cell). */
    private int gridIndexAt(double mouseX, double mouseY) {
        double gx = mouseX - leftPos - BankGuhMenu.GRID_X;
        double gy = mouseY - topPos - BankGuhMenu.GRID_Y;
        if (gx < 0 || gy < 0 || gx >= BankGuhMenu.GRID_COLS * 18 || gy >= BankGuhMenu.GRID_ROWS * 18) {
            return -1;
        }
        int index = (scrollRow + (int) gy / 18) * BankGuhMenu.GRID_COLS + (int) gx / 18;
        return index < visible.size() ? index : -2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = gridIndexAt(mouseX, mouseY);
        if (index != -1 && (button == 0 || button == 1)) {
            if (!menu.getCarried().isEmpty()) {
                send(BankGuhMenu.Action.DEPOSIT_CARRIED, ItemStack.EMPTY, button, false);
            } else if (index >= 0) {
                send(BankGuhMenu.Action.TAKE, visible.get(index).item(), button, Screen.hasShiftDown());
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0 && mouseY < topPos + BankGuhMenu.GRID_Y + BankGuhMenu.GRID_ROWS * 18) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            search.keyPressed(keyCode, scanCode, modifiers);
            return true; // don't let "E" close the screen while typing
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void send(BankGuhMenu.Action action, ItemStack item, int button, boolean shift) {
        ClientPacketDistributor.sendToServer(new BankActionPayload(menu.containerId, action, item, button, shift));
    }
}
