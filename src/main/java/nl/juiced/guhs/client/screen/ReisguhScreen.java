package nl.juiced.guhs.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.ReisVolgorde;
import nl.juiced.guhs.quest.Reisguh;
import nl.juiced.guhs.taal.Tekst;

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * A Reisguh's menu: its name (rename it) and every other Reisguh you've discovered (click to travel there).
 * 1.4.2: per row a star (favourites always on top) and arrows to move the row in your own order (also onto the next or
 * previous page: the page follows the row); a sort button (own order / distance / name). The server keeps all of it per
 * player (quest/ReisVolgorde) and sends the rows in the own order; the screen sorts by distance or name itself. The height
 * follows the window and the rows end above the paging and Done buttons, so nothing overlaps at any GUI scale.
 */
public class ReisguhScreen extends Screen {
    private static final int W = 280, MAX_H = 258, ROW = 22, LIST_Y = 72, BOTTOM = 30;
    private final int npcId;
    private CompoundTag data;
    private int left, top, h, page, perPage;
    private EditBox name;
    /** What is typed in the name box (kept when the list updates), and the name the server sent last. */
    private String typed, serverName;
    /** The row just starred or moved: the page follows it. */
    private UUID follow;

    private record Row(UUID id, Component name, int distance, boolean fav) {
    }

    public ReisguhScreen(MaagPayloads.ReisOpen open) {
        super(Component.translatable("entity.guhs.guh_npc.reisguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    public void update(MaagPayloads.ReisOpen open) {
        this.data = open.data();
        rebuildWidgets();
    }

    private void send(int action, String text) {
        ClientPacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, action, text));
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        ListTag points = data.getListOrEmpty("Points");
        for (int i = 0; i < points.size(); i++) {
            CompoundTag p = points.getCompoundOrEmpty(i);
            p.read("Id", UUIDUtil.CODEC).ifPresent(id -> rows.add(new Row(id, Tekst.get(p, "Name"), p.getIntOr("Distance", 0), p.getBooleanOr("Fav", false))));
        }
        ReisVolgorde.sorteer(rows, sort(), Row::fav, Row::distance, r -> r.name().getString());
        return rows;
    }

    private int sort() {
        int s = data.getIntOr("Sort", ReisVolgorde.EIGEN);
        return s >= 0 && s < ReisVolgorde.MODI ? s : ReisVolgorde.EIGEN;
    }

    @Override
    protected void init() {
        h = Math.min(MAX_H, height - 8);
        left = (width - W) / 2;
        top = (height - h) / 2;
        perPage = Math.max(1, (h - LIST_Y - BOTTOM + 2) / ROW);

        String nu = Tekst.get(data, "Name").getString();
        if (name != null && nu.equals(serverName)) {
            typed = name.getValue();                                // (a list update: keep what is being typed)
        } else {
            typed = nu;
        }
        serverName = nu;
        name = new EditBox(font, left + 12, top + 24, W - 90, 18, Component.translatable("gui.guhs.reis.name"));
        name.setMaxLength(Reisguh.MAX_NAME);
        name.setValue(typed);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"), b -> send(Reisguh.RENAME, name.getValue()))
                .bounds(left + W - 74, top + 23, 62, 20).tooltip(GuhScreen.tip("gui.guhs.reis.rename.tooltip")).build());

        int sort = sort();
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.reis.sort", Component.translatable("gui.guhs.reis.sort." + ReisVolgorde.MODUS_SLEUTEL[sort])),
                        b -> send(Reisguh.SORT, String.valueOf((sort + 1) % ReisVolgorde.MODI)))
                .bounds(left + W - 162, top + 48, 150, 20).tooltip(GuhScreen.tip("gui.guhs.reis.sort.tooltip")).build());

        List<Row> rows = rows();
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        if (follow != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id().equals(follow)) {
                    page = i / perPage;
                }
            }
            follow = null;
        }
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < perPage && page * perPage + i < rows.size(); i++) {
            int index = page * perPage + i;
            Row r = rows.get(index);
            String id = r.id().toString();
            int y = top + LIST_Y + i * ROW;
            addRenderableWidget(Button.builder(Component.literal(r.fav() ? "★" : "☆").withStyle(r.fav() ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
                            b -> {
                                follow = r.id();
                                send(Reisguh.FAVORITE, id);
                            })
                    .bounds(left + 12, y, 20, 20).tooltip(GuhScreen.tip(r.fav() ? "gui.guhs.reis.unfav.tooltip" : "gui.guhs.reis.fav.tooltip")).build());
            // (short "(812 bl)" in the row as in the mockup; a long name scrolls, the tooltip has it in full)
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.reis.rij", r.name(), r.distance()),
                            b -> {
                                send(Reisguh.TRAVEL, id);
                                onClose();
                            })
                    .bounds(left + 36, y, W - 36 - 12 - 48, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.reis.to", r.name(), r.distance())
                            .append("\n").append(Component.translatable("gui.guhs.reis.to.tooltip")))).build());
            boolean eigen = sort == ReisVolgorde.EIGEN;
            Button up = Button.builder(Component.literal("▲"), b -> {
                        follow = r.id();
                        send(Reisguh.UP, id);
                    })
                    .bounds(left + W - 56, y, 20, 20).tooltip(GuhScreen.tip(eigen ? "gui.guhs.reis.up.tooltip" : "gui.guhs.reis.schuif_uit.tooltip")).build();
            Button down = Button.builder(Component.literal("▼"), b -> {
                        follow = r.id();
                        send(Reisguh.DOWN, id);
                    })
                    .bounds(left + W - 34, y, 20, 20).tooltip(GuhScreen.tip(eigen ? "gui.guhs.reis.down.tooltip" : "gui.guhs.reis.schuif_uit.tooltip")).build();
            up.active = eigen && ReisVolgorde.kanSchuiven(rows, index, -1, Row::fav);
            down.active = eigen && ReisVolgorde.kanSchuiven(rows, index, 1, Row::fav);
            addRenderableWidget(up);
            addRenderableWidget(down);
        }
        int by = top + h - BOTTOM + 4;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 12, by, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 36, by, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 92, by, 80, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (name.isFocused() && (keyCode == 257 || keyCode == 335)) {
            send(Reisguh.RENAME, name.getValue());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, 0xFF8FD3F5);
        g.fill(left, top, left + W, top + h, 0xE81A2638);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFE6F6FF);
        g.text(font, Component.translatable("gui.guhs.reis.where"), left + 12, top + 54, 0xFF9FD8F0);
        int n = data.getListOrEmpty("Points").size();
        if (n == 0) {
            int y = top + LIST_Y + 2;
            for (var line : font.split(Component.translatable("gui.guhs.reis.none"), W - 24)) {
                if (y + 9 > top + h - BOTTOM) {
                    break;
                }
                g.text(font, line, left + 12, y, 0xFFB0C8D8);
                y += 11;
            }
        }
        int pages = Math.max(1, (n + perPage - 1) / perPage);
        if (pages > 1) {
            g.text(font, Component.literal((page + 1) + "/" + pages), left + 62, top + h - BOTTOM + 10, 0xFFB0C8D8);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
