package nl.juiced.guhs.feature.guhpixel.grap2.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.client.GuhPop;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Payloads;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Slice;
import nl.juiced.guhs.feature.guhpixel.grap2.GuhmonGevecht;
import nl.juiced.guhs.taal.Tekst;
import org.lwjgl.glfw.GLFW;

/**
 * The battle screen of the Guhmon-gevecht: your guh in front on the left (seen from behind), the guh of Gymleider Dutjes
 * at the back on the right, each with a name box and a SLEEP bar, a text box that types the lines of the round, and the
 * four moves. The server plays the battle; this screen only shows what it sends and reports which button was pressed.
 */
public class GuhmonGevechtScherm extends Screen {
    private static final int W = 320, H = 214, VELD_H = 140;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, DONKER = 0xFF3A1C30;
    private static final int LEES_TICKS = 26;

    private record Regel(String sleutel, int wie, int a, int b) {
    }

    private CompoundTag data;
    private Component naam = Component.empty(), tegenNaam = Component.empty();
    @Nullable
    private GuhEntity popMijn, popTegen;
    private final Deque<Regel> wachtrij = new ArrayDeque<>();
    @Nullable
    private Regel nu;
    private String nuTekst = "";
    private int letters, gelezen, sprong;
    private float toonA, toonB, vorigeA, vorigeB;
    private int doelA, doelB;
    private boolean wachtOpServer, verzonden, slaaptA, slaaptB;
    private int left, top, ticks;
    private final Button[] zetten = new Button[4];
    @Nullable
    private Button verder, opnieuw;

    public GuhmonGevechtScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhmon.gevecht.titel"));
        this.data = data;
        neem(data, data.getBooleanOr("Vers", false));   // (not "vers": the screen was opened again in the middle of a battle)
    }

    /** A new state from the server: a fresh battle starts over, a round adds its lines. */
    public void update(CompoundTag nieuw) {
        boolean vers = nieuw.getBooleanOr("Vers", false);
        this.data = nieuw;
        neem(nieuw, vers);
        wachtOpServer = false;
        if (width > 0) {
            knoppen();
        }
    }

    private void neem(CompoundTag d, boolean vers) {
        naam = Tekst.get(d, "Naam");
        tegenNaam = Tekst.get(d, "TegenNaam");
        if (vers || popMijn == null) {
            popMijn = GuhPop.van(d.getCompoundOrEmpty("Looks"));
            popTegen = GuhPop.van(d.getCompoundOrEmpty("TegenLooks"));
        }
        if (vers) {
            wachtrij.clear();
            nu = null;
            verzonden = false;
            slaaptA = slaaptB = false;
            slaap(popMijn, false);
            slaap(popTegen, false);
            doelA = d.getIntOr("SlaapSpeler", 0);
            doelB = d.getIntOr("SlaapTegen", 0);
            toonA = vorigeA = doelA;
            toonB = vorigeB = doelB;
            if (d.getIntOr("Verloren", 0) == 0) {
                wachtrij.add(new Regel("intro.1", 1, doelA, doelB));
                wachtrij.add(new Regel("intro.2", 1, doelA, doelB));
            }
            wachtrij.add(new Regel("intro.3", 0, doelA, doelB));
            speel(Grap2Slice.GELUID_GEVECHT.get(), 1f);
        }
        ListTag regels = d.getListOrEmpty("Regels");
        for (int i = 0; i < regels.size(); i++) {
            CompoundTag r = regels.getCompoundOrEmpty(i);
            wachtrij.add(new Regel(r.getStringOr("Sleutel", ""), r.getIntOr("Wie", 0), r.getIntOr("A", 0), r.getIntOr("B", 0)));
        }
        if (!vers && regels.isEmpty() && nu == null) {
            // (the screen was opened again in the middle of a battle: just the numbers)
            doelA = d.getIntOr("SlaapSpeler", 0);
            doelB = d.getIntOr("SlaapTegen", 0);
            toonA = vorigeA = doelA;
            toonB = vorigeB = doelB;
            slaaptA = uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN;
            slaaptB = uitkomst() == GuhmonGevecht.Uitkomst.VERLOREN;
            slaap(popMijn, slaaptA);
            slaap(popTegen, slaaptB);
        }
    }

    private GuhmonGevecht.Uitkomst uitkomst() {
        return GuhmonGevecht.Uitkomst.values()[Math.floorMod(data.getIntOr("Uitkomst", 0), GuhmonGevecht.Uitkomst.values().length)];
    }

    private static void slaap(@Nullable GuhEntity pop, boolean aan) {
        if (pop != null) {
            pop.setEmoteData(aan ? (Emote.SLAPEN.ordinal() + 1) | GuhEmotes.LOOP : 0);
        }
    }

    private void speel(SoundEvent geluid, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(geluid, pitch));
        }
    }

    /** Everything typed and read: the player may pick a move (or the battle is over). */
    private boolean rustig() {
        return nu == null && wachtrij.isEmpty();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bx = left + W - 6 - 2 * 78 - 2, by = top + VELD_H + 12;
        for (int i = 0; i < 4; i++) {
            GuhmonGevecht.Zet zet = GuhmonGevecht.Zet.op(i);
            zetten[i] = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhmon.zet." + zet.id()), b -> doe(zet))
                    .bounds(bx + (i % 2) * 80, by + (i / 2) * 24, 78, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.guhmon.zet." + zet.id() + ".tip"))).build());
        }
        verder = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhmon.knop.verder"), b -> {
            stuur(Grap2Payloads.VERDER, "");
            verzonden = true;
            onClose();
        }).bounds(bx + 20, by + 12, 120, 20).build());
        opnieuw = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhmon.knop.opnieuw"), b -> {
            stuur(Grap2Payloads.OPNIEUW, "");
            wachtOpServer = true;
            knoppen();
        }).bounds(bx + 20, by + 12, 120, 20).build());
        knoppen();
    }

    private void doe(GuhmonGevecht.Zet zet) {
        if (!rustig() || wachtOpServer || uitkomst() != GuhmonGevecht.Uitkomst.BEZIG) {
            return;
        }
        if (data.getBooleanOr("Demo", false)) {
            wachtrij.add(new Regel(zet.id(), 0, doelA, doelB));
        } else {
            stuur(Grap2Payloads.ZET, Integer.toString(zet.ordinal()));
            wachtOpServer = true;
        }
        knoppen();
    }

    private void stuur(int actie, String arg) {
        if (minecraft != null && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new Grap2Payloads.GuhmonDoe(actie, arg));
        }
    }

    private void knoppen() {
        GuhmonGevecht.Uitkomst u = uitkomst();
        boolean kies = rustig() && !wachtOpServer && u == GuhmonGevecht.Uitkomst.BEZIG;
        for (Button b : zetten) {
            if (b != null) {
                b.visible = kies;
            }
        }
        if (verder != null) {
            verder.visible = rustig() && u == GuhmonGevecht.Uitkomst.GEWONNEN;
        }
        if (opnieuw != null) {
            opnieuw.visible = rustig() && !wachtOpServer && u == GuhmonGevecht.Uitkomst.VERLOREN;
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (popMijn != null) {
            popMijn.tickCount++;
        }
        if (popTegen != null) {
            popTegen.tickCount++;
        }
        vorigeA = toonA;
        vorigeB = toonB;
        toonA += Mth.clamp(doelA - toonA, -2.5f, 2.5f);
        toonB += Mth.clamp(doelB - toonB, -2.5f, 2.5f);
        if (sprong > 0) {
            sprong--;
        }
        if (nu == null && !wachtrij.isEmpty()) {
            begin(wachtrij.poll());
        } else if (nu != null) {
            if (letters < nuTekst.length()) {
                letters = Math.min(nuTekst.length(), letters + 2);
            } else if (++gelezen >= LEES_TICKS) {
                nu = null;
                if (wachtrij.isEmpty()) {
                    doelA = data.getIntOr("SlaapSpeler", doelA);
                    doelB = data.getIntOr("SlaapTegen", doelB);
                }
                knoppen();
            }
        }
    }

    private void begin(Regel r) {
        nu = r;
        letters = 0;
        gelezen = 0;
        doelA = r.a();
        doelB = r.b();
        Component wie = r.wie() == 0 ? naam : tegenNaam, ander = r.wie() == 0 ? tegenNaam : naam;
        nuTekst = Component.translatable("gui.guhs.guhmon.tekst." + r.sleutel(), wie, ander).getString();
        switch (r.sleutel()) {
            case "slaapt" -> {
                if (r.wie() == 0) {
                    slaaptA = true;
                    slaap(popMijn, true);
                } else {
                    slaaptB = true;
                    slaap(popTegen, true);
                }
                speel(Grap2Slice.GELUID_SLAAPT.get(), 1f);
            }
            case "gewonnen" -> speel(Grap2Slice.GELUID_GEWONNEN.get(), 1f);
            case "verloren", "intro.1", "intro.2", "intro.3" -> {
            }
            default -> {
                sprong = 8;
                speel(Grap2Slice.GELUID_ZET.get(), r.wie() == 0 ? 1.1f : 0.8f);
            }
        }
        knoppen();
    }

    /** Space, enter or a click in the text box: the whole line at once, or on to the next one. */
    private void sneller() {
        if (nu == null) {
            return;
        }
        if (letters < nuTekst.length()) {
            letters = nuTekst.length();
        } else {
            gelezen = LEES_TICKS;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE || event.key() == GLFW.GLFW_KEY_ENTER) {
            if (nu != null) {
                sneller();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.y() >= top + VELD_H) {
            sneller();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (!verzonden && rustig() && uitkomst() == GuhmonGevecht.Uitkomst.GEWONNEN) {
            stuur(Grap2Payloads.VERDER, "");
            verzonden = true;
        } else if (!verzonden && minecraft != null && !data.getBooleanOr("Demo", false)) {
            minecraft.gui.setOverlayMessage(Component.translatable("gui.guhs.guhmon.scherm.dicht").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        // the field: a soft pink sky over a mint floor, two cushions to stand on
        g.fillGradient(left + 4, top + 4, left + W - 4, top + 86, 0xFFFFD9EC, 0xFFFFF3D6);
        g.fillGradient(left + 4, top + 86, left + W - 4, top + VELD_H, 0xFFCDEFD6, 0xFFA9DDB8);
        kussen(g, left + 240, top + 84, 58, 0xFFE8A0C8, 0xFFD27AA8);
        kussen(g, left + 86, top + 128, 78, 0xFFE8A0C8, 0xFFD27AA8);
        float pt = partialTick;
        int hopA = nu != null && nu.wie() == 0 && sprong > 0 ? -Math.round(Mth.sin(sprong / 8f * Mth.PI) * 5) : 0;
        int hopB = nu != null && nu.wie() == 1 && sprong > 0 ? -Math.round(Mth.sin(sprong / 8f * Mth.PI) * 5) : 0;
        if (popTegen != null) {
            GuhPop.teken(g, left + 188, top + 20 + hopB, left + 292, top + 92 + hopB, popTegen, -28f, -6f);
        }
        if (popMijn != null) {
            GuhPop.teken(g, left + 18, top + 44 + hopA, left + 154, top + VELD_H + hopA, popMijn, 152f, -10f);
        }
        if (slaaptB) {
            zzz(g, left + 262, top + 22);
        }
        if (slaaptA) {
            zzz(g, left + 120, top + 58);
        }
        if (nu != null && sprong > 0) {
            Component ballon = Component.translatable("gui.guhs.guhmon.ballon." + nu.sleutel());
            int x = nu.wie() == 0 ? left + 96 : left + 196, y = nu.wie() == 0 ? top + 50 : top + 12;
            int bw = font.width(ballon) + 8;
            g.fill(x - 1, y - 1, x + bw + 1, y + 13, DONKER);
            g.fill(x, y, x + bw, y + 12, 0xFFFFFFFF);
            g.text(font, ballon, x + 4, y + 2, DONKER, false);
        }
        naamvak(g, left + 10, top + 10, tegenNaam, Mth.lerp(pt, vorigeB, toonB), 0, false);
        naamvak(g, left + 166, top + 98, naam, Mth.lerp(pt, vorigeA, toonA), data.getIntOr("MaagSpeler", 0), true);
        // the text box
        int tx = left + 6, ty = top + VELD_H + 4, tw = W - 12, th = H - VELD_H - 10;
        g.fill(tx, ty, tx + tw, ty + th, 0xFFFFE6EE);
        g.fill(tx + 2, ty + 2, tx + tw - 2, ty + th - 2, 0xFF4A2440);
        g.fill(tx + 3, ty + 3, tx + tw - 3, ty + th - 3, 0xFF2A1422);
        GuhmonGevecht.Uitkomst u = uitkomst();
        boolean kies = rustig() && !wachtOpServer && u == GuhmonGevecht.Uitkomst.BEZIG;
        int breed = kies || (rustig() && u != GuhmonGevecht.Uitkomst.BEZIG) ? tw - 2 * 80 - 18 : tw - 16;
        Component tekst;
        if (nu != null) {
            tekst = Component.literal(nuTekst.substring(0, Math.min(letters, nuTekst.length())));
        } else if (kies) {
            tekst = Component.translatable("gui.guhs.guhmon.tekst.wat_nu", naam);
        } else if (rustig() && u == GuhmonGevecht.Uitkomst.GEWONNEN) {
            tekst = Component.translatable("gui.guhs.guhmon.tekst.na_winst");
        } else if (rustig() && u == GuhmonGevecht.Uitkomst.VERLOREN) {
            tekst = Component.translatable("gui.guhs.guhmon.tekst.na_verlies");
        } else {
            tekst = Component.literal("...");
        }
        List<FormattedCharSequence> regels = font.split(tekst, breed);
        for (int i = 0; i < Math.min(regels.size(), 5); i++) {
            g.text(font, regels.get(i), tx + 9, ty + 8 + i * 10, TEKST, false);
        }
        if (nu != null && letters >= nuTekst.length() && (ticks / 6) % 2 == 0) {
            g.text(font, Component.literal("▼"), tx + tw - 14, ty + th - 13, GOUD, false);
        }
        if (kies && data.getBooleanOr("DutjeMislukt", false)) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.guhmon.tekst.let_op"), tx + 9, ty + th - 13, breed, 0.75f, GOUD, false);
        }
    }

    private static void kussen(GuiGraphicsExtractor g, int cx, int cy, int w, int licht, int donker) {
        for (int i = 0; i < 7; i++) {
            int half = Math.round(w / 2f * (float) Math.sqrt(1 - Math.pow((i - 3) / 3.6, 2)));
            g.fill(cx - half, cy - 3 + i, cx + half, cy - 2 + i, i < 4 ? licht : donker);
        }
    }

    private void zzz(GuiGraphicsExtractor g, int x, int y) {
        for (int i = 0; i < 3; i++) {
            int fase = (ticks + i * 13) % 40;
            int alpha = 255 - fase * 5;
            g.text(font, Component.literal(i == 2 ? "Z" : "z"), x + i * 7 + fase / 8, y - i * 6 - fase / 4, (alpha << 24) | 0x7A5AC8, false);
        }
    }

    /** A name with its SLEEP bar; the player's own box also shows the number and a full belly. */
    private void naamvak(GuiGraphicsExtractor g, int x, int y, Component wie, float slaap, int maag, boolean eigen) {
        int w = 144, h = eigen ? 36 : 28;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, DONKER);
        g.fill(x, y, x + w, y + h, 0xFFFFF6FA);
        GidsTekst.passend(g, wie.copy().withStyle(ChatFormatting.BOLD), x + 5, y + 4, w - 10, 1f, DONKER, false);
        Component label = Component.translatable("gui.guhs.guhmon.balk.slaap");
        g.text(font, label, x + 5, y + 16, 0xFF7A5AC8, false);
        int bx = x + 9 + font.width(label), bw = x + w - 6 - bx;
        g.fill(bx - 1, y + 15, bx + bw + 1, y + 25, DONKER);
        g.fill(bx, y + 16, bx + bw, y + 24, 0xFFE4D8F4);
        int vol = Math.round(bw * Mth.clamp(slaap / GuhmonGevecht.VOL, 0f, 1f));
        if (vol > 0) {
            g.fillGradient(bx, y + 16, bx + vol, y + 24, slaap >= 75 ? 0xFF4C3CB0 : 0xFF8E78E0, slaap >= 75 ? 0xFF2E2478 : 0xFF6A54C4);
        }
        if (eigen) {
            Component getal = Component.literal(Math.round(slaap) + "/" + GuhmonGevecht.VOL);
            g.text(font, getal, x + w - 6 - font.width(getal), y + 27, DONKER, false);
            if (maag > 0) {
                GidsTekst.passend(g, Component.translatable("gui.guhs.guhmon.balk.maag", maag), x + 5, y + 27, w - 50, 0.75f, 0xFFB06A1E, false);
            }
        }
    }
}
