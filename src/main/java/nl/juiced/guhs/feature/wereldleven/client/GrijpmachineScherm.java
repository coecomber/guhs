package nl.juiced.guhs.feature.wereldleven.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.wereldleven.Grijpmachine;
import nl.juiced.guhs.feature.wereldleven.GrijpmachineBlockEntity;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.feature.wereldleven.WereldlevenPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * The grijpmachine's claw screen: the glass case seen from the front (the plushies on its floor, further back is higher
 * and smaller), a map of the floor seen from above, and the claw. Steer it with WASD or the arrow keys, drop it with the
 * space bar (or the button) - or it drops by itself when the time is up. Down it goes, the prongs close, and then the
 * server says what it caught (Grijpmachine.grijp): up it goes, over to the chute, and there... plop.
 */
public class GrijpmachineScherm extends Screen {
    private enum Fase { STUREN, ZAKKEN, DICHT, WACHT, OMHOOG, TERUG, LOS, KLAAR }

    private static final int W = 330, H = 232;
    private static final int PANEL = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    private static final float SNELHEID = 0.014f, GOOT_X = 0.1f, GOOT_Z = 0.9f;
    private final BlockPos pos;
    private List<GrijpmachineBlockEntity.Prijs> prijzen;
    private int left, top;
    private Fase fase = Fase.STUREN;
    private int faseTicks, stuurTicks;
    private float klauwX = GOOT_X + 0.08f, klauwZ = GOOT_Z - 0.08f, klauwXo = klauwX, klauwZo = klauwZ;
    private float zak, zako, dicht, dichto;
    private boolean links, rechts, achter, voor;
    private boolean verstuurd;
    // the result
    private int index = -1;
    private boolean gepakt;
    @Nullable
    private GrijpmachineBlockEntity.Prijs vast;
    private float vastHoogte;
    @Nullable
    private List<GrijpmachineBlockEntity.Prijs> daarna;
    @Nullable
    private Button grijpKnop, opnieuwKnop;

    public GrijpmachineScherm(BlockPos pos, List<GrijpmachineBlockEntity.Prijs> prijzen) {
        super(Component.translatable("block.guhs.grijpmachine"));
        this.pos = pos;
        this.prijzen = new ArrayList<>(prijzen);
    }

    public BlockPos pos() {
        return pos;
    }

    /** A new turn at the same machine (after "Nog een keer!"). */
    public void prijzen(List<GrijpmachineBlockEntity.Prijs> nieuw) {
        prijzen = new ArrayList<>(nieuw);
        fase = Fase.STUREN;
        faseTicks = stuurTicks = 0;
        klauwX = klauwXo = GOOT_X + 0.08f;
        klauwZ = klauwZo = GOOT_Z - 0.08f;
        zak = zako = dicht = dichto = 0;
        verstuurd = false;
        index = -1;
        gepakt = false;
        vast = null;
        daarna = null;
        rebuildWidgets();
    }

    /** The server's verdict. */
    public void uitslag(int index, boolean gepakt, String knuffel, List<GrijpmachineBlockEntity.Prijs> nieuw) {
        this.index = index;
        this.gepakt = gepakt;
        this.daarna = new ArrayList<>(nieuw);
        this.vast = index >= 0 && index < prijzen.size() ? prijzen.get(index) : null;
        if (vast != null) {
            prijzen.remove(index);
        }
        vastHoogte = 0;
        zet(Fase.OMHOOG);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        grijpKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.wereldleven.grijp_knop"), b -> laatZakken())
                .bounds(left + 234, top + 124, 86, 20).build());
        opnieuwKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.wereldleven.grijp_opnieuw"),
                b -> PacketDistributor.sendToServer(new WereldlevenPayloads.GrijpOpnieuw(pos))).bounds(left + 234, top + 148, 86, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knuffeldal.doei"), b -> onClose())
                .bounds(left + 234, top + H - 26, 86, 20).build());
        knoppen();
    }

    private void knoppen() {
        if (grijpKnop != null) {
            grijpKnop.active = fase == Fase.STUREN;
        }
        if (opnieuwKnop != null) {
            opnieuwKnop.visible = fase == Fase.KLAAR;
        }
    }

    private void zet(Fase f) {
        fase = f;
        faseTicks = 0;
        knoppen();
    }

    private void laatZakken() {
        if (fase == Fase.STUREN) {
            zet(Fase.ZAKKEN);
            geluid(1.0f);
        }
    }

    private void geluid(float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WereldlevenFeature.GRIJPKLAUW.get(), pitch, 0.6f));
        }
    }

    private void verstuur() {
        if (!verstuurd) {
            verstuurd = true;
            PacketDistributor.sendToServer(new WereldlevenPayloads.Grijp(pos, klauwX, klauwZ));
        }
    }

    // --- keys ------------------------------------------------------------------------------------------------------------------

    private boolean stuur(int key, boolean down) {
        switch (key) {
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> links = down;
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> rechts = down;
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> achter = down;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> voor = down;
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (stuur(key, true)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            laatZakken();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        if (stuur(key, false)) {
            return true;
        }
        return super.keyReleased(key, scan, modifiers);
    }

    // --- the animation -----------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        klauwXo = klauwX;
        klauwZo = klauwZ;
        zako = zak;
        dichto = dicht;
        faseTicks++;
        switch (fase) {
            case STUREN -> {
                stuurTicks++;
                float dx = (rechts ? 1 : 0) - (links ? 1 : 0), dz = (voor ? 1 : 0) - (achter ? 1 : 0);
                klauwX = Mth.clamp(klauwX + dx * SNELHEID, 0.04f, 0.96f);
                klauwZ = Mth.clamp(klauwZ + dz * SNELHEID, 0.04f, 0.96f);
                if (stuurTicks >= Grijpmachine.STUURTIJD) {
                    laatZakken();
                }
            }
            case ZAKKEN -> {
                zak = Math.min(1f, faseTicks / 24f);
                if (faseTicks >= 24) {
                    zet(Fase.DICHT);
                }
            }
            case DICHT -> {
                dicht = Math.min(1f, faseTicks / 8f);
                if (faseTicks >= 8) {
                    verstuur();
                    zet(Fase.WACHT);
                }
            }
            case WACHT -> {
                if (faseTicks > 80) {    // (no answer: the server wasn't told, or it's gone)
                    zet(Fase.KLAAR);
                }
            }
            case OMHOOG -> {
                zak = Math.max(0f, 1f - faseTicks / 24f);
                if (vast != null && !gepakt) {
                    vastHoogte = faseTicks < 10 ? faseTicks / 10f * 0.3f : Math.max(0f, 0.3f - (faseTicks - 10) / 5f * 0.3f);
                    if (faseTicks == 12) {
                        dicht = 0.4f;
                        geluid(0.7f);
                    }
                }
                if (faseTicks >= 24) {
                    if (vast != null && !gepakt) {
                        prijzen.add(vast);    // (it slipped back onto the pile)
                        vast = null;
                    }
                    zet(gepakt ? Fase.TERUG : Fase.KLAAR);
                    if (!gepakt) {
                        dicht = 0;
                        klaarZetten();
                    }
                }
            }
            case TERUG -> {
                float t = Math.min(1f, faseTicks / 30f);
                klauwX = Mth.lerp(0.15f + 0.15f * t, klauwX, GOOT_X);
                klauwZ = Mth.lerp(0.15f + 0.15f * t, klauwZ, GOOT_Z);
                if (faseTicks >= 30) {
                    klauwX = GOOT_X;
                    klauwZ = GOOT_Z;
                    zet(Fase.LOS);
                    geluid(1.3f);
                }
            }
            case LOS -> {
                dicht = Math.max(0f, 1f - faseTicks / 6f);
                if (faseTicks >= 14) {
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.6f, 0.5f));
                    }
                    vast = null;
                    klaarZetten();
                    zet(Fase.KLAAR);
                }
            }
            default -> {
            }
        }
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > 8 * 8) {
            onClose();
        }
    }

    private void klaarZetten() {
        if (daarna != null) {
            prijzen = new ArrayList<>(daarna);
        }
    }

    @Override
    public void onClose() {
        if (fase == Fase.STUREN || fase == Fase.ZAKKEN || fase == Fase.DICHT) {
            verstuur();    // (walking away drops the claw where it is: your ticket isn't wasted)
        }
        super.onClose();
    }

    // --- drawing --------------------------------------------------------------------------------------------------------------

    private int kastL() {
        return left + 12;
    }

    private int kastR() {
        return left + 222;
    }

    private int railY() {
        return top + 32;
    }

    private int vloerAchter() {
        return top + 132;
    }

    private int vloerVoor() {
        return top + 196;
    }

    private float diepte(float z) {
        return 0.62f + 0.38f * z;
    }

    private float schermX(float x, float z) {
        float mid = (kastL() + kastR()) / 2f;
        return mid + (x - 0.5f) * (kastR() - kastL() - 24) * diepte(z);
    }

    private float schermY(float z) {
        return vloerAchter() + z * (vloerVoor() - vloerAchter());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFFFB6D8, false);
        // the case: glass, a pink frame, the floor (a trapezium, lighter at the back)
        int l = kastL(), r = kastR();
        g.fill(l - 3, top + 22, r + 3, top + 206, 0xFFE88AB4);
        g.fill(l, top + 26, r, top + 202, 0xFF2A3A55);
        for (int y = vloerAchter(); y < vloerVoor(); y++) {
            float z = (y - vloerAchter()) / (float) (vloerVoor() - vloerAchter());
            int half = (int) ((r - l - 8) / 2f * diepte(z));
            int mid = (l + r) / 2;
            g.fill(mid - half, y, mid + half, y + 1, (y / 3) % 2 == 0 ? 0xFFFFC6DF : 0xFFFFB3D4);
        }
        // the chute (front left) with a guh face over it
        float gx = schermX(GOOT_X, GOOT_Z), gy = schermY(GOOT_Z);
        g.fill((int) gx - 16, (int) gy - 10, (int) gx + 16, (int) gy + 8, 0xFF3A1C30);
        g.fill((int) gx - 14, (int) gy - 8, (int) gx + 14, (int) gy + 6, 0xFF1A0C18);
        g.centeredText(font, Component.translatable("gui.guhs.wereldleven.grijp_goot"), (int) gx, (int) gy - 22, 0xFFFFE6EE);
        // the rail at the top
        g.fill(l + 4, railY() - 2, r - 4, railY() + 2, 0xFFB0B8C8);
        float kx = Mth.lerp(partialTick, klauwXo, klauwX), kz = Mth.lerp(partialTick, klauwZo, klauwZ);
        float z1 = Mth.lerp(partialTick, zako, zak), d1 = Mth.lerp(partialTick, dichto, dicht);
        // the shadow of the claw on the floor (where it will land)
        int sx = (int) schermX(kx, kz), sy = (int) schermY(kz);
        int sw = (int) (12 * diepte(kz));
        g.fill(sx - sw, sy - 2, sx + sw, sy + 2, 0x60000000);
        // the plushies, back ones first; the claw between them at its depth
        List<GrijpmachineBlockEntity.Prijs> sorted = new ArrayList<>(prijzen);
        sorted.sort(Comparator.comparingDouble(GrijpmachineBlockEntity.Prijs::z));
        boolean klauwGetekend = false;
        for (GrijpmachineBlockEntity.Prijs p : sorted) {
            if (!klauwGetekend && p.z() > kz) {
                klauw(g, kx, kz, z1, d1);
                klauwGetekend = true;
            }
            knuffel(g, p, schermX(p.x(), p.z()), schermY(p.z()), 0f);
        }
        if (!klauwGetekend) {
            klauw(g, kx, kz, z1, d1);
        }
        // the map (seen from above) and the time
        kaart(g, kx, kz);
        if (fase == Fase.STUREN) {
            int left2 = left + 234, w2 = 86;
            float rest = 1f - stuurTicks / (float) Grijpmachine.STUURTIJD;
            g.fill(left2, top + 115, left2 + w2, top + 120, 0xFF5A2E44);
            g.fill(left2, top + 115, left2 + (int) (w2 * rest), top + 120, rest < 0.25f ? 0xFFFF6F7D : 0xFF7ED957);
        }
        Component info = switch (fase) {
            case STUREN -> Component.translatable("gui.guhs.wereldleven.grijp_stuur");
            case KLAAR -> Component.translatable(gepakt ? "gui.guhs.wereldleven.grijp_gewonnen" : index >= 0 ? "gui.guhs.wereldleven.grijp_glipt"
                    : "gui.guhs.wereldleven.grijp_mis");
            default -> Component.translatable("gui.guhs.wereldleven.grijp_spannend");
        };
        // what's going on, under the case
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(info, r - l);
        int y = top + 209;
        for (var line : lines.subList(0, Math.min(2, lines.size()))) {
            g.centeredText(font, line, (l + r) / 2, y, TEXT);
            y += 10;
        }
    }

    private void knuffel(GuiGraphicsExtractor g, GrijpmachineBlockEntity.Prijs p, float x, float y, float lift) {
        Block block = WereldlevenFeature.knuffel(p.knuffel());
        if (block == null) {
            return;
        }
        float s = 1.5f * diepte(p.z());
        g.pose().pushMatrix();
        g.pose().translate(x - 8 * s, y - 14 * s - lift);
        g.pose().scale(s, s);
        g.item(new ItemStack(block), 0, 0);
        g.pose().popMatrix();
    }

    /** The claw: the rope from the rail, the head, three prongs (open or closed), and what it holds. */
    private void klauw(GuiGraphicsExtractor g, float kx, float kz, float zak, float dicht) {
        float f = diepte(kz);
        int x = (int) schermX(kx, kz);
        int grond = (int) (schermY(kz) - 26 * f);
        int y = (int) Mth.lerp(zak, railY() + 16, grond);
        g.fill(x - 1, railY(), x + 1, y, 0xFFD8DCE6);
        int hw = (int) (7 * f);
        g.fill(x - hw, y, x + hw, y + (int) (6 * f), 0xFFB0B8C8);
        g.fill(x - hw + 1, y + 1, x + hw - 1, y + 2, 0xFFFFFFFF);
        int open = (int) ((1f - dicht) * 7 * f);
        int pl = (int) (12 * f);
        for (int side = -1; side <= 1; side++) {
            int px = x + side * (hw - 1) + side * open / 2;
            int bottomX = x + side * (int) (2 * f) + side * open;
            for (int i = 0; i < pl; i++) {
                int cx = Math.round(Mth.lerp(i / (float) pl, px, bottomX));
                g.fill(cx - 1, y + (int) (6 * f) + i, cx + 1, y + (int) (6 * f) + i + 1, 0xFF8C94A6);
            }
        }
        if (vast != null) {
            float lift = gepakt ? 0 : vastHoogte;
            if (gepakt) {
                knuffel(g, vast, x, y + 22 * f, 0);
            } else if (fase == Fase.OMHOOG) {
                knuffel(g, vast, schermX(vast.x(), vast.z()), schermY(vast.z()), lift * (schermY(vast.z()) - railY()));
            }
        }
    }

    /** The floor seen from above: the plushies as dots, the chute, the claw as a cross. */
    private void kaart(GuiGraphicsExtractor g, float kx, float kz) {
        int x0 = left + 234, y0 = top + 22, s = 80;
        g.fill(x0 - 1, y0 - 1, x0 + s + 1, y0 + s + 1, 0xFFF7B6CB);
        g.fill(x0, y0, x0 + s, y0 + s, 0xFFFFE6F0);
        g.fill(x0, (int) (y0 + (GOOT_Z - 0.12f) * s), (int) (x0 + (GOOT_X + 0.12f) * s), y0 + s, 0xFF3A1C30);
        for (GrijpmachineBlockEntity.Prijs p : prijzen) {
            int px = (int) (x0 + p.x() * s), py = (int) (y0 + p.z() * s);
            int c = p.knuffel().equals(WereldlevenFeature.GLITTER) ? 0xFFFFD84E : 0xFFE0609A;
            g.fill(px - 3, py - 3, px + 3, py + 3, 0xFF5A2E44);
            g.fill(px - 2, py - 2, px + 2, py + 2, c);
        }
        int cx = (int) (x0 + kx * s), cy = (int) (y0 + kz * s);
        int reach = (int) (Grijpmachine.GRIJP_BEREIK * s);
        g.outline(cx - reach, cy - reach, reach * 2, reach * 2, 0x805B9DFF);
        g.fill(cx - 5, cy, cx + 6, cy + 1, 0xFF2A3A55);
        g.fill(cx, cy - 5, cx + 1, cy + 6, 0xFF2A3A55);
        g.text(font, Component.translatable("gui.guhs.wereldleven.grijp_kaart"), x0, y0 + s + 3, 0xFFB8A0B0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
