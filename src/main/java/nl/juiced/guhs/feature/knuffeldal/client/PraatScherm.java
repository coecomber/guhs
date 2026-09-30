package nl.juiced.guhs.feature.knuffeldal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalPayloads;

/**
 * The Knuffeldal's talking screen: the character (drawn, looking at you), what they say, and underneath either the
 * answers you can give (Cocotje) or the Knusfeest list with a "Geven" button (Burgemeester Vadsema). Always a "Doei!"
 * button to walk away.
 */
public class PraatScherm extends Screen {
    private static final int W = 300, H = 186, PIC = 70;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    private int npcId;
    private CompoundTag data;
    private int left, top;
    /** 3.0 (verhaal.Praat): the page of a scene ("Paginas"), and whether the -1 "read/closed" answer was sent. */
    private int pagina;
    private boolean klaarGemeld;

    public PraatScherm(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.knuffeldal.praat"));
        this.npcId = npcId;
        this.data = data;
    }

    public int npcId() {
        return npcId;
    }

    public void update(CompoundTag newData) {
        update(npcId, newData);
    }

    /** 3.0: a new screen from the server (a new scene starts on its first page). */
    public void update(int newNpcId, CompoundTag newData) {
        boolean nieuweScene = newData.contains("Paginas") || !newData.getString("Sleutel").equals(data.getString("Sleutel"));
        this.npcId = newNpcId;
        this.data = newData;
        if (nieuweScene) {
            pagina = 0;
            klaarGemeld = false;
        }
        rebuildWidgets();
    }

    private int paginas() {
        return data.getList("Paginas", Tag.TAG_COMPOUND).size();
    }

    private boolean laatstePagina() {
        return pagina >= paginas() - 1;
    }

    /** The page shown now (a scene), or the screen's own data. */
    private CompoundTag nu() {
        var list = data.getList("Paginas", Tag.TAG_COMPOUND);
        return list.isEmpty() ? data : list.getCompound(Math.min(pagina, list.size() - 1));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        List<CompoundTag> opties = new ArrayList<>();
        if (laatstePagina()) {
            for (Tag t : data.getList("Opties", Tag.TAG_COMPOUND)) {
                opties.add((CompoundTag) t);
            }
        } else {
            // 3.0: a scene: "Verder »" to the next page (the answers come on the last one)
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verhaal.verder"), b -> {
                pagina++;
                rebuildWidgets();
            }).bounds(left + W - 10 - 96 - 4 - 96, top + H - 26, 96, 20).build());
        }
        boolean burgemeester = data.getString("Scherm").equals("burgemeester");
        int y = top + H - 26 - (burgemeester ? 0 : opties.size() * 22);
        int x = left + 10, w = W - 20;
        if (burgemeester) {
            // the list's "Geven" next to "Doei!"
            for (CompoundTag o : opties) {
                int id = o.getInt("Id");
                addRenderableWidget(Button.builder(Component.translatable(o.getString("Tekst")), b -> send(id))
                        .bounds(left + W - 10 - 96 - 4 - 96, top + H - 26, 96, 20).build());
            }
        } else {
            for (CompoundTag o : opties) {
                int id = o.getInt("Id");
                addRenderableWidget(Button.builder(Component.literal("» ").append(Component.translatable(o.getString("Tekst"))), b -> send(id))
                        .bounds(x, y, w, 20).build());
                y += 22;
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + W - 10 - 96, top + H - 26, 96, 20).build());
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new KnuffeldalPayloads.Action(npcId, action));
    }

    /** 3.0: closing a screen with a sleutel tells the server (-1: read to the end / closed). */
    @Override
    public void onClose() {
        if (data.contains("Sleutel") && !klaarGemeld) {
            klaarGemeld = true;
            send(-1);
        }
        super.onClose();
    }

    /** The speaker of the page shown now (a scene page's own speaker, else the screen's). */
    private Entity spreker() {
        int id = nu().contains("Spreker") ? nu().getInt("Spreker") : npcId;
        return minecraft.level == null || id < 0 ? null : minecraft.level.getEntity(id);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        Entity npc = spreker();
        String naamKey = nu().getString("Naam");
        Component name = !naamKey.isEmpty() ? Component.translatable(naamKey) : npc == null ? title : npc.getDisplayName();
        g.drawString(font, name.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFB6D8, false);
        // the character
        g.fill(left + 10, top + 22, left + 10 + PIC, top + 22 + PIC, 0x30F7B6CB);
        if (npc instanceof LivingEntity living) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 10, top + 22, left + 10 + PIC, top + 22 + PIC, 26, 0.0625f,
                    mouseX, mouseY, living);
        }
        // the speech balloon
        int bx = left + 10 + PIC + 8, by = top + 22, bw = W - (bx - left) - 10;
        g.fill(bx, by, bx + bw, by + PIC, 0xFFFFF4F8);
        g.fill(bx - 4, by + 12, bx, by + 18, 0xFFFFF4F8);
        List<Object> args = new ArrayList<>();
        for (Tag t : nu().getList("Args", Tag.TAG_STRING)) {
            args.add(t.getAsString());
        }
        Component tekst = Component.translatable(nu().getString("Tekst"), args.toArray());
        List<FormattedCharSequence> lines = font.split(tekst, bw - 10);
        for (int i = 0; i < lines.size() && i < 6; i++) {
            g.drawString(font, lines.get(i), bx + 5, by + 5 + i * 10, 0xFF3A1C30, false);
        }
        if (paginas() > 1) {
            String n = (Math.min(pagina, paginas() - 1) + 1) + "/" + paginas();
            g.drawString(font, n, bx + bw - 5 - font.width(n), by + PIC - 11, 0xFFB08AA0, false);
        }
        if (data.getString("Scherm").equals("burgemeester")) {
            renderLijst(g, top + 22 + PIC + 8);
        }
    }

    /** The Knusfeest list: a line per task, with its step and (when you carry it) a little "!" to hand it in. */
    private void renderLijst(GuiGraphics g, int y) {
        var taken = data.getList("Taken", Tag.TAG_COMPOUND);
        Component kop = Component.translatable(data.getLong("Ronde") == 0 ? "gui.guhs.knusfeest.lijst_kop" : "gui.guhs.knusfeest.lijst_kop_seizoen");
        g.drawString(font, kop, left + 10, y, 0xFFFFB6D8, false);
        y += 11;
        for (int i = 0; i < taken.size(); i++) {
            CompoundTag t = taken.getCompound(i);
            String stap = t.getString("Stap");
            boolean done = stap.equals("gebracht");
            int col = i % 2, row = i / 2;
            int x = left + 10 + col * ((W - 20) / 2), yy = y + row * 11;
            String mark = done ? "✔" : t.getBoolean("Bij") ? "!" : stap.equals("gestolen") ? "?" : "•";
            int colour = done ? 0xFF68D88A : t.getBoolean("Bij") ? 0xFFFFD27A : stap.equals("gestolen") ? 0xFFF7A060 : TEXT;
            Component line = Component.literal(mark + " ").append(Component.translatable("gui.guhs.knusfeest.taak." + t.getString("Id")));
            g.drawString(font, font.plainSubstrByWidth(line.getString(), (W - 20) / 2 - 4), x, yy, colour, false);
        }
        int hintY = y + ((taken.size() + 1) / 2) * 11 + 2;
        if (hintY < top + H - 30) {
            g.drawString(font, Component.translatable("gui.guhs.knusfeest.hint").withStyle(ChatFormatting.ITALIC), left + 10, hintY, 0xFFB8A0B0, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (npcId < 0) {
            return;   // (3.0: a scene without a speaker in the world)
        }
        Entity npc = minecraft.level == null ? null : minecraft.level.getEntity(npcId);
        if (npc == null || minecraft.player == null || minecraft.player.distanceTo(npc) > 12) {
            onClose();
        }
    }
}
