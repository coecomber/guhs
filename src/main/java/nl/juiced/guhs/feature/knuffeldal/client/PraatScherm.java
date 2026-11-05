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
 * <p>
 * 1.6.0 (test round 5): the answers always start under the balloon and the screen grows for them. They were stacked up
 * from the bottom, so four or more (IJsverkoper Smeltje's ice creams) covered the text.
 */
public class PraatScherm extends Screen {
    private static final int W = 300, H = 186, PIC = 70;
    /** The balloon: how wide a line of text may be, how many lines fit without growing, the height of a line. */
    private static final int REGEL_BREED = W - (10 + PIC + 8) - 10 - 10, REGELS = 6, REGEL_HOOG = 10;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    protected int npcId;   // (T-DIALOOG: protected for RijkPraatScherm)
    protected CompoundTag data;
    private int left, top;
    /** 1.4.0: how much taller than {@link #H} the screen is now, for a text of more than {@link #REGELS} lines. */
    private int extra;
    /** 1.6.0 test round 5: the height without the extra text lines: taller when the answers need room under the balloon. */
    private int basis = H;
    /** 3.0 (verhaal.Praat): the page of a scene ("Paginas"), and whether the -1 "read/closed" answer was sent. */
    protected int pagina;
    protected boolean klaarGemeld;

    public PraatScherm(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.knuffeldal.praat"));
        this.npcId = npcId;
        this.data = data;
    }

    /**
     * T-DIALOOG: another look for some conversations (Het Vadsrijk's RijkPraatScherm for payloads with "Rijk"); null = this one.
     * Set once by a client init.
     */
    @javax.annotation.Nullable
    public static java.util.function.BiFunction<Integer, CompoundTag, PraatScherm> stijl;

    /** T-DIALOOG: the screen for this payload (a style's own screen, else this plain one). */
    public static PraatScherm maak(int npcId, CompoundTag data) {
        PraatScherm s = stijl == null ? null : stijl.apply(npcId, data);
        return s != null ? s : new PraatScherm(npcId, data);
    }

    /** T-DIALOOG: may this open screen show that payload in place (same style)? The plain screen takes all but "Rijk" ones. */
    public boolean past(CompoundTag newData) {
        return stijl == null || !newData.getBooleanOr("Rijk", false);
    }

    public int npcId() {
        return npcId;
    }

    /** q-dialoog: the sleutel of what this screen shows ("" = none). */
    public String sleutel() {
        return data.getStringOr("Sleutel", "");
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

    protected int paginas() {
        return data.getListOrEmpty("Paginas").size();
    }

    protected boolean laatstePagina() {
        return pagina >= paginas() - 1;
    }

    /** The page shown now (a scene), or the screen's own data. */
    protected CompoundTag nu() {
        var list = data.getListOrEmpty("Paginas");
        return list.isEmpty() ? data : list.getCompoundOrEmpty(Math.min(pagina, list.size() - 1));
    }

    /** What this page says (its text with its arguments). */
    protected static Component tekst(CompoundTag pg) {
        List<Object> args = new ArrayList<>();
        for (Tag t : pg.getListOrEmpty("Args")) {
            args.add(t instanceof CompoundTag a ? nl.juiced.guhs.taal.Tekst.get(a, "A") : t.asString().orElse(""));
        }
        return Component.translatable(pg.getStringOr("Tekst", ""), args.toArray());
    }

    /** The height of the screen: taller for a long text (every page of a scene alike, so nothing jumps while reading). */
    private int hoogte() {
        return basis + extra;
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
        List<CompoundTag> opties = new ArrayList<>();
        if (laatstePagina()) {
            for (Tag t : data.getListOrEmpty("Opties")) {
                opties.add((CompoundTag) t);
            }
        }
        boolean burgemeester = data.getStringOr("Scherm", "").equals("burgemeester");
        // the balloon, then a row per answer, then the "Doei!" row
        basis = Math.max(H, 22 + PIC + 6 + (burgemeester ? 0 : opties.size() * 22) + 30);
        // (as far as the window allows: on a very low window the last lines are cut off, as before)
        extra = Math.max(0, Math.min(meer * REGEL_HOOG, (height - 4 - basis) / REGEL_HOOG * REGEL_HOOG));
        left = (width - W) / 2;
        top = Math.max(2, (height - hoogte()) / 2);   // (q-dialoog: never above the window on a big GUI scale)
        if (!laatstePagina()) {
            // 3.0: a scene: "Verder »" to the next page (the answers come on the last one)
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verhaal.verder"), b -> {
                pagina++;
                rebuildWidgets();
            }).bounds(left + W - 10 - 96 - 4 - 96, top + hoogte() - 26, 96, 20).build());
        }
        int y = top + 22 + PIC + extra + 6;
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

    protected void send(int action) {
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

    /** T-DIALOOG: only the screen's own (blurred/dimmed) background, for a style that draws its own panel. */
    protected final void schermAchtergrond(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
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
        LivingEntity portret = portret();
        if (minecraft.options.advancedItemTooltips) {
            // q-dialoog dev aid (F3+H): which text, which speaker, where the portrait came from
            String bron = portret == null ? "geen" : portret == minecraft.player ? "speler" : STAND_INS.containsValue(portret) ? "stand-in" : "entity";
            String info = data.getStringOr("Sleutel", "-") + " | " + nu().getStringOr("Tekst", "") + " | spreker "
                    + (nu().contains("Spreker") ? nu().getIntOr("Spreker", 0) : npcId) + " | " + bron;
            g.text(font, font.plainSubstrByWidth(info, W), left, top + hoogte() + 3, 0xFFB0B0B0, true);
        }
        if (portret != null) {
            tekenPortret(g, portret, left + 10, top + 22, left + 10 + PIC, top + 22 + PIC, mouseX, mouseY, partialTick);
        } else {
            // (a narrator, a sign, a note: a little book instead of an empty frame)
            g.pose().pushMatrix();
            g.pose().translate(left + 10 + PIC / 2f - 16, top + 22 + PIC / 2f - 16);
            g.pose().scale(2f, 2f);
            g.item(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), 0, 0);
            g.pose().popMatrix();
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

    /** q-dialoog: stand-ins for speakers that are not on this client, by their portrait (built once, kept while in this world). */
    protected static final java.util.Map<String, LivingEntity> STAND_INS = new java.util.HashMap<>();

    /**
     * q-dialoog: who is drawn for the page shown now. The player's own lines ("Speler"): you. Else the speaker when this
     * client has it, else a stand-in built from the page's "Portret" (a far NPC, one that was never sent to this client, a
     * page that only names an NPC). Null: nobody (a narrator).
     */
    @javax.annotation.Nullable
    protected LivingEntity portret() {
        CompoundTag pg = nu();
        if (pg.getBooleanOr("Speler", false)) {
            return minecraft.player;
        }
        if (spreker() instanceof LivingEntity living && !living.isRemoved()) {
            return living;
        }
        return standIn(pg.getCompoundOrEmpty("Portret"));
    }

    @javax.annotation.Nullable
    private LivingEntity standIn(CompoundTag t) {
        if (t.isEmpty() || minecraft.level == null) {
            return null;
        }
        String sleutel = t.toString();
        LivingEntity e = STAND_INS.get(sleutel);
        if (e != null && e.level() == minecraft.level) {
            return e;
        }
        if (STAND_INS.size() > 32 || e != null) {
            STAND_INS.clear();   // (another world: build them anew)
        }
        try {
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getOptional(net.minecraft.resources.Identifier.parse(t.getStringOr("Type", ""))).orElse(null);
            if (type == null || !(type.create(minecraft.level, net.minecraft.world.entity.EntitySpawnReason.LOAD) instanceof LivingEntity living)) {
                return null;
            }
            if (living instanceof nl.juiced.guhs.entity.GuhEntity guh) {
                guh.setVariant(nl.juiced.guhs.entity.GuhVariant.valueOf(t.getStringOr("Variant", "NORMAL")));
                for (Tag k : t.getListOrEmpty("Kleren")) {
                    guh.wear(nl.juiced.guhs.entity.GuhClothes.valueOf(k.asString().orElse("")));
                }
                boolean zit = t.getBooleanOr("Zit", false);
                guh.setOrderedToSit(zit);
                guh.setInSittingPose(zit);
            }
            if (living instanceof nl.juiced.guhs.entity.GuhNpcEntity npc && t.contains("Soort")) {
                npc.setKind(nl.juiced.guhs.entity.GuhNpcEntity.Kind.valueOf(t.getStringOr("Soort", "")));
            }
            living.setYRot(180f);
            living.yBodyRot = 180f;
            living.yHeadRot = 180f;
            STAND_INS.put(sleutel, living);
            return living;
        } catch (RuntimeException ex) {
            return null;   // (an unknown type, clothes or colour from a newer server: no portrait rather than a crash)
        }
    }

    /**
     * q-dialoog: the portrait in its frame. A guh close (its head and body fill the frame), a person from the chest up, the
     * rest whole; turned a little toward the mouse (not more: following the answer buttons tipped characters over) and
     * swaying gently.
     */
    private void tekenPortret(GuiGraphicsExtractor g, LivingEntity living, int x0, int y0, int x1, int y1, int mouseX, int mouseY, float pt) {
        if (living instanceof nl.juiced.guhs.entity.GuhNpcEntity g2) {
            // (the story NPCs keep their tuned framing; one whose head stands on the ground only looks a little down)
            int kijkY = mouseY;
            if (nl.juiced.guhs.client.SittingGuhRenderers.kopOpDeGrond(g2.getKind())) {
                int midden = (y0 + y1) / 2;
                kijkY = Math.max(midden - 30, Math.min(midden + 6, mouseY));
            }
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, 26, 0.0625f, mouseX, kijkY, living);
            return;
        }
        float hoog = living.getBbHeight() / Math.max(0.01f, living.getScale());
        float breed = living.getBbWidth() / Math.max(0.01f, living.getScale());
        float vak = y1 - y0;
        float midden, span;
        if (living instanceof nl.juiced.guhs.entity.GuhEntity) {
            midden = 0.62f;   // (the guh model: feet at 0, the head's top at ~0.95, hats up to ~1.4)
            span = 1.65f;
        } else if (hoog > 1.2f && breed < hoog * 0.7f) {
            midden = hoog * 0.68f;   // (upright and tall, like the player: head and chest)
            span = hoog * 0.68f;
        } else {
            midden = hoog * 0.5f;
            span = Math.max(hoog, breed) * 1.35f;
        }
        int maat = Math.max(8, Math.round(vak / span));
        float tijd = ((minecraft.level == null ? 0 : minecraft.level.getGameTime()) + pt) / 20f;
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f;
        float xHoek = clamp((float) Math.atan((cx - mouseX) / 40f), 0.55f) + (float) Math.sin(tijd * 0.9f) * 0.08f;
        float yHoek = clamp((float) Math.atan((cy - mouseY) / 40f), 0.35f) + (float) Math.sin(tijd * 0.6f + 1f) * 0.03f;
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x0, y0, x1, y1, maat, midden - hoog / 2f, xHoek, yHoek, living);
    }

    private static float clamp(float v, float max) {
        return Math.max(-max, Math.min(max, v));
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
        for (LivingEntity e : STAND_INS.values()) {
            e.tickCount++;   // (q-dialoog: a stand-in is not in the world: its idle animation runs on this)
        }
        if (npcId < 0) {
            return;   // (3.0: a scene without a speaker in the world)
        }
        Entity npc = minecraft.level == null ? null : minecraft.level.getEntity(npcId);
        if (npc == null || minecraft.player == null || minecraft.player.distanceTo(npc) > 12) {
            onClose();
        }
    }
}
