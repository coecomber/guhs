package nl.juiced.guhs.feature.knuffeldal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Knuffeldal's talking screen: the character (drawn, looking at you), what they say, and underneath either the
 * answers you can give (Cocotje) or the Knusfeest list with a "Geven" button (Burgemeester Vadsema). Always a "Doei!"
 * button to walk away.
 * <p>
 * 1.4.0: a text of more than six lines makes the balloon (and the whole screen) taller. Until then the balloon showed six
 * lines and cut the rest off without a sign: half of what Steele explains, the last sentence of many characters of the
 * Guhbarbecuether. And a speaker whose head stands on the ground (the Mika-shaped characters) only looks a little down:
 * following the mouse to the answer buttons tipped the whole character over on its face.
 */
public class PraatScherm extends Screen {
    private static final int W = 300, H = 186, PIC = 70;
    /** The balloon: how wide a line of text may be, how many lines fit without growing, the height of a line. */
    private static final int REGEL_BREED = W - (10 + PIC + 8) - 10 - 10, REGELS = 6, REGEL_HOOG = 10;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    private int npcId;
    private CompoundTag data;
    private int left, top;
    /** 1.4.0: how much taller than {@link #H} the screen is now, for a text of more than {@link #REGELS} lines. */
    private int extra;
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
        boolean nieuweScene = newData.contains("Paginas") || !newData.getStringOr("Sleutel", "").equals(data.getStringOr("Sleutel", ""));
        this.npcId = newNpcId;
        this.data = newData;
        if (nieuweScene) {
            pagina = 0;
            klaarGemeld = false;
        }
        rebuildWidgets();
    }

    private int paginas() {
        return data.getListOrEmpty("Paginas").size();
    }

    private boolean laatstePagina() {
        return pagina >= paginas() - 1;
    }

    /** The page shown now (a scene), or the screen's own data. */
    private CompoundTag nu() {
        var list = data.getListOrEmpty("Paginas");
        return list.isEmpty() ? data : list.getCompoundOrEmpty(Math.min(pagina, list.size() - 1));
    }

    /** What this page says (its text with its arguments). */
    private static Component tekst(CompoundTag pg) {
        List<Object> args = new ArrayList<>();
        for (Tag t : pg.getListOrEmpty("Args")) {
            args.add(t instanceof CompoundTag a ? nl.juiced.guhs.taal.Tekst.get(a, "A") : t.asString().orElse(""));
        }
        return Component.translatable(pg.getStringOr("Tekst", ""), args.toArray());
    }

    /** The height of the screen: taller for a long text (every page of a scene alike, so nothing jumps while reading). */
    private int hoogte() {
        return H + extra;
    }

    private int meerRegels(CompoundTag pg) {
        return Math.max(0, font.split(tekst(pg), REGEL_BREED).size() - REGELS);
    }

    @Override
    protected void init() {
        int meer = meerRegels(data);
        for (Tag t : data.getListOrEmpty("Paginas")) {
            meer = Math.max(meer, meerRegels((CompoundTag) t));
        }
        // (as far as the window allows: on a very low window the last lines are cut off, as before)
        extra = Math.max(0, Math.min(meer * REGEL_HOOG, (height - 4 - H) / REGEL_HOOG * REGEL_HOOG));
        left = (width - W) / 2;
        top = (height - hoogte()) / 2;
        List<CompoundTag> opties = new ArrayList<>();
        if (laatstePagina()) {
            for (Tag t : data.getListOrEmpty("Opties")) {
                opties.add((CompoundTag) t);
            }
        } else {
            // 3.0: a scene: "Verder »" to the next page (the answers come on the last one)
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verhaal.verder"), b -> {
                pagina++;
                rebuildWidgets();
            }).bounds(left + W - 10 - 96 - 4 - 96, top + hoogte() - 26, 96, 20).build());
        }
        boolean burgemeester = data.getStringOr("Scherm", "").equals("burgemeester");
        int y = top + hoogte() - 26 - (burgemeester ? 0 : opties.size() * 22);
        int x = left + 10, w = W - 20;
        if (burgemeester) {
            // the list's "Geven" next to "Doei!"
            for (CompoundTag o : opties) {
                int id = o.getIntOr("Id", 0);
                addRenderableWidget(Button.builder(Component.translatable(o.getStringOr("Tekst", "")), b -> send(id))
                        .bounds(left + W - 10 - 96 - 4 - 96, top + hoogte() - 26, 96, 20).build());
            }
        } else {
            for (CompoundTag o : opties) {
                int id = o.getIntOr("Id", 0);
                addRenderableWidget(Button.builder(Component.literal("» ").append(Component.translatable(o.getStringOr("Tekst", ""))), b -> send(id))
                        .bounds(x, y, w, 20).build());
                y += 22;
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + W - 10 - 96, top + hoogte() - 26, 96, 20).build());
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new KnuffeldalPayloads.Action(npcId, action));
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
        int id = nu().contains("Spreker") ? nu().getIntOr("Spreker", 0) : npcId;
        return minecraft.level == null || id < 0 ? null : minecraft.level.getEntity(id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + hoogte() + 1, BORDER);
        g.fill(left, top, left + W, top + hoogte(), PANEL);
        Entity npc = spreker();
        String naamKey = nu().getStringOr("Naam", "");
        Component name = !naamKey.isEmpty() ? Component.translatable(naamKey) : npc == null ? title : npc.getDisplayName();
        g.text(font, name.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFB6D8, false);
        // the character
        g.fill(left + 10, top + 22, left + 10 + PIC, top + 22 + PIC, 0x30F7B6CB);
        if (npc instanceof LivingEntity living) {
            int kijkY = mouseY;
            if (npc instanceof nl.juiced.guhs.entity.GuhNpcEntity g2 && nl.juiced.guhs.client.SittingGuhRenderers.kopOpDeGrond(g2.getKind())) {
                // (its head turns on the ground and is most of the character: looking down at the buttons, it lay on its face)
                int midden = top + 22 + PIC / 2;
                kijkY = Math.max(midden - 30, Math.min(midden + 6, mouseY));
            }
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, left + 10, top + 22, left + 10 + PIC, top + 22 + PIC, 26, 0.0625f,
                    mouseX, kijkY, living);
        }
        // the speech balloon
        int bx = left + 10 + PIC + 8, by = top + 22, bw = W - (bx - left) - 10;
        g.fill(bx, by, bx + bw, by + PIC + extra, 0xFFFFF4F8);
        g.fill(bx - 4, by + 12, bx, by + 18, 0xFFFFF4F8);
        List<FormattedCharSequence> lines = font.split(tekst(nu()), REGEL_BREED);
        for (int i = 0; i < lines.size() && i < REGELS + extra / REGEL_HOOG; i++) {
            g.text(font, lines.get(i), bx + 5, by + 5 + i * REGEL_HOOG, 0xFF3A1C30, false);
        }
        if (paginas() > 1) {
            String n = (Math.min(pagina, paginas() - 1) + 1) + "/" + paginas();
            g.text(font, n, bx + bw - 5 - font.width(n), by + PIC + extra - 11, 0xFFB08AA0, false);
        }
        if (data.getStringOr("Scherm", "").equals("burgemeester")) {
            renderLijst(g, top + 22 + PIC + extra + 8, mouseX, mouseY);
        }
    }

    /**
     * The Knusfeest list: a line per task, with its step and (when you carry it) a little "!" to hand it in. 1.2.9: pointing
     * at a task tells you where to get it and how far you are.
     */
    private void renderLijst(GuiGraphicsExtractor g, int y, int mouseX, int mouseY) {
        var taken = data.getListOrEmpty("Taken");
        Component kop = Component.translatable(data.getLongOr("Ronde", 0L) == 0 ? "gui.guhs.knusfeest.lijst_kop" : "gui.guhs.knusfeest.lijst_kop_seizoen");
        g.text(font, kop, left + 10, y, 0xFFFFB6D8, false);
        y += 11;
        for (int i = 0; i < taken.size(); i++) {
            CompoundTag t = taken.getCompoundOrEmpty(i);
            String stap = t.getStringOr("Stap", "");
            boolean done = stap.equals("gebracht");
            int col = i % 2, row = i / 2;
            int x = left + 10 + col * ((W - 20) / 2), yy = y + row * 11;
            String mark = done ? "✔" : t.getBooleanOr("Bij", false) ? "!" : stap.equals("gestolen") ? "?" : "•";
            int colour = done ? 0xFF68D88A : t.getBooleanOr("Bij", false) ? 0xFFFFD27A : stap.equals("gestolen") ? 0xFFF7A060 : TEXT;
            Component line = Component.literal(mark + " ").append(Component.translatable("gui.guhs.knusfeest.taak." + t.getStringOr("Id", "")));
            g.text(font, font.plainSubstrByWidth(line.getString(), (W - 20) / 2 - 4), x, yy, colour, false);
            if (mouseX >= x && mouseX < x + (W - 20) / 2 && mouseY >= yy - 1 && mouseY < yy + 10) {
                String id = t.getStringOr("Id", "");
                List<FormattedCharSequence> tip = new ArrayList<>();
                tip.add(Component.translatable("gui.guhs.knusfeest.taak." + id).withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText());
                if (stap.isEmpty() || stap.equals("gevraagd")) {
                    tip.addAll(font.split(Component.translatable("gui.guhs.knusfeest.waar." + id), 190));
                }
                tip.addAll(font.split(Component.translatable("gui.guhs.knusfeest.stap." + (stap.isEmpty() ? "gevraagd" : stap))
                        .withStyle(ChatFormatting.GRAY), 190));
                g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
        int hintY = y + ((taken.size() + 1) / 2) * 11 + 2;
        if (hintY < top + hoogte() - 30) {
            g.text(font, Component.translatable("gui.guhs.knusfeest.hint").withStyle(ChatFormatting.ITALIC), left + 10, hintY, 0xFFB8A0B0, false);
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
