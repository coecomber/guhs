package nl.juiced.guhs.feature.vadsrijk.hud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.opslag.client.StatusIcoon;

/**
 * U-HUD: the party frames (left, middle; spot {@code RijkHud.plek("party")}, movable in the HUD editor). Per other member: class
 * icon, name (★ = leader), level, Vads bar, spec, up to 6 small buff icons, an arrow + distance when they are far (or "elders" in
 * another world), and a grey "offline" / red "flauwgevallen" state. With the chat open you can click a frame: that member becomes
 * your party target (gold edge).
 */
public final class PartyHud {
    public static final int BREED = 114, HOOG = 29, STAP = 31;
    /** From this distance (blocks) a frame shows the arrow + distance. */
    public static final double VER = 24;

    private static List<UHudPayloads.Frame> leden = List.of();
    private static boolean nep;
    private static long tijd = -1000;
    private static long klok;
    @Nullable
    private static UUID gekozen;
    /** The frames drawn last (on screen, after the editor's move): for clicks. */
    private static final List<Object[]> VAKKEN = new ArrayList<>();

    private PartyHud() {
    }

    static void ontvang(UHudPayloads.Party p) {
        leden = p.leden();
        nep = p.nep();
        tijd = klok;
        if (gekozen != null && leden.stream().noneMatch(f -> f.id().equals(gekozen))) {
            gekozen = null;
        }
    }

    static void tick() {
        klok++;
    }

    static void wis() {
        leden = List.of();
        gekozen = null;
    }

    /** Do the frames draw the party now (then kern's simple party list stays away)? */
    static boolean actief() {
        return !leden.isEmpty() && klok - tijd <= 60;
    }

    private static final Identifier[] KLASSE_ICOON = {
            nl.juiced.guhs.Guhs.id("textures/gui/vadsrijk/skill/knuffelschild.png"),
            nl.juiced.guhs.Guhs.id("textures/gui/vadsrijk/skill/knuffelpijl.png"),
            nl.juiced.guhs.Guhs.id("textures/gui/vadsrijk/skill/roze_vuurbal.png")};

    @Nullable
    private static Identifier klasseIcoon(String klasse) {
        return switch (klasse) {
            case "knuffelridder" -> KLASSE_ICOON[0];
            case "pluisjager" -> KLASSE_ICOON[1];
            case "vadsmagier" -> KLASSE_ICOON[2];
            default -> null;
        };
    }

    static List<UHudPayloads.Frame> voorbeeld() {
        UUID a = new UUID(0, 11), b = new UUID(0, 12);
        return List.of(
                new UHudPayloads.Frame(a, "Knuffelaar", "knuffelridder", "vadsrijk.klassen.klasse.knuffelridder", 42, 820, 1000,
                        List.of(Status.VAHOEG.ordinal(), Status.KNUFFELSCHILD.ordinal()), "", 0, 0, 0, UHudPayloads.STAAT_OK, true),
                new UHudPayloads.Frame(b, "Pluisje", "pluisjager", "vadsrijk.klassen.klasse.pluisjager", 38, 260, 800,
                        List.of(Status.BRANDEND.ordinal()), "", 0, 0, 0, UHudPayloads.STAAT_OK, false),
                new UHudPayloads.Frame(new UUID(0, 13), "Slaapkop", "", "", 0, 0, 1, List.of(), "", 0, 0, 0, UHudPayloads.STAAT_OFFLINE, false));
    }

    /** The RijkHud element (drawn in plek "party", so the HUD editor moves it). */
    static void teken(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        boolean bewerken = Minecraft.getInstance().screen instanceof HudBewerker;
        List<UHudPayloads.Frame> l = actief() ? leden : bewerken ? voorbeeld() : List.of();
        VAKKEN.clear();
        if (l.isEmpty()) {
            return;
        }
        int[] p = RijkHud.plek("party", w, h);
        int x = p[0], y = p[1];
        for (UHudPayloads.Frame fr : l) {
            frame(g, f, fr, x, y, pt);
            float[] a = HudIndeling.naarScherm("party", x, y, w, h), b = HudIndeling.naarScherm("party", x + BREED, y + HOOG, w, h);
            VAKKEN.add(new Object[] {fr, a[0], a[1], b[0], b[1]});
            y += STAP;
        }
        if (nep) {
            g.text(f, "(nep-party)", x, y, 0x80FFFFFF, true);
        }
    }

    private static void frame(GuiGraphicsExtractor g, Font f, UHudPayloads.Frame fr, int x, int y, float pt) {
        boolean offline = fr.staat() == UHudPayloads.STAAT_OFFLINE, flauw = fr.staat() == UHudPayloads.STAAT_FLAUW;
        boolean kies = fr.id().equals(gekozen);
        int rand = kies ? 0xFFFFD24A : flauw ? 0xFF8A2A3A : 0xFF4A2A3A;
        g.fill(x - 1, y - 1, x + BREED + 1, y + HOOG + 1, rand);
        g.fill(x, y, x + BREED, y + HOOG, offline ? 0xC0202020 : 0xC8201418);
        // class icon
        Identifier icoon = klasseIcoon(fr.klasse());
        if (icoon != null && !offline) {
            RijkHud.tekenIcoon(g, icoon, x + 2, y + 2, 9);
        } else {
            g.fill(x + 2, y + 2, x + 11, y + 11, 0xFF3A2A33);
        }
        // name + level
        String lv = offline ? "" : Component.translatable("vadsrijk.uhud.party.level", fr.level()).getString();
        String naam = (fr.leider() ? "★" : "") + fr.naam();
        int ruimte = BREED - 16 - f.width(lv) - 4;
        if (f.width(naam) > ruimte) {
            naam = f.plainSubstrByWidth(naam, ruimte - f.width("…")) + "…";
        }
        g.text(f, naam, x + 13, y + 3, offline ? 0xFF8A8A8A : 0xFFFFFFFF, true);
        g.text(f, lv, x + BREED - 2 - f.width(lv), y + 3, 0xFFFFE27A, true);
        // Vads bar
        int bx = x + 2, by = y + 13, bw = BREED - 4, bh = 5;
        g.fill(bx, by, bx + bw, by + bh, 0xFF3A2A33);
        if (!offline) {
            float frac = Mth.clamp(fr.vads() / Math.max(1, fr.max()), 0, 1);
            g.fill(bx, by, bx + (int) (bw * frac), by + bh, flauw ? 0xFF6A2A3A : frac > 0.3f ? 0xFFE0457A : 0xFFFF3A3A);
            g.fill(bx, by, bx + (int) (bw * frac), by + 1, 0x60FFFFFF);
            klein(g, f, (int) Math.ceil(fr.vads()) + " / " + (int) fr.max(), bx + bw / 2f, by + 0.5f, 0.55f, 0xFFFFFFFF, true);
        }
        // third line: spec / state left, buffs or distance right
        int ly = y + 20;
        if (offline) {
            klein(g, f, Component.translatable("vadsrijk.uhud.party.offline").getString(), x + 2, ly + 1, 0.7f, 0xFF9A9A9A, false);
            return;
        }
        if (flauw) {
            klein(g, f, Component.translatable("vadsrijk.uhud.party.flauw").getString(), x + 2, ly + 1, 0.7f, 0xFFFF6A6A, false);
        } else if (!fr.spec().isEmpty()) {
            klein(g, f, Component.translatable(fr.spec()).getString(), x + 2, ly + 1, 0.6f, 0xFFFFC2E8, false);
        }
        int rx = x + BREED - 2;
        // distance + arrow (only when far, or elsewhere)
        LocalPlayer me = Minecraft.getInstance().player;
        if (me != null) {
            boolean hier = fr.dim().isEmpty() || fr.dim().equals(me.level().dimension().identifier().toString());
            if (!hier) {
                String s = Component.translatable("vadsrijk.uhud.party.elders").getString();
                klein(g, f, s, rx - f.width(s) * 0.6f, ly + 1, 0.6f, 0xFFB0B0FF, false);
                rx -= (int) (f.width(s) * 0.6f) + 3;
            } else if (!fr.dim().isEmpty()) {
                Vec3 doel = positie(fr, pt);
                double afstand = Math.sqrt(Math.pow(doel.x - me.getX(), 2) + Math.pow(doel.z - me.getZ(), 2));
                if (afstand >= VER) {
                    String s = (int) afstand + "m";
                    klein(g, f, s, rx - f.width(s) * 0.6f, ly + 1, 0.6f, 0xFFB0E0FF, false);
                    rx -= (int) (f.width(s) * 0.6f) + 2;
                    pijl(g, rx - 4, ly + 3, hoek(me, doel, pt));
                    rx -= 10;
                }
            }
        }
        // buffs, right to left
        int z = 7;
        for (int i = fr.buffs().size() - 1; i >= 0; i--) {
            int o = fr.buffs().get(i);
            if (o < 0 || o >= Status.values().length || rx - z < x + 50) {
                continue;
            }
            StatusIcoon.teken(g, f, Status.values()[o], rx - z, ly, z, 1, -1);
            rx -= z + 1;
        }
    }

    /** The member's entity when it is loaded (smooth), else the sent spot. */
    private static Vec3 positie(UHudPayloads.Frame fr, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player p = mc.level.getPlayerByUUID(fr.id());
            if (p != null) {
                return p.getPosition(pt);
            }
        }
        return new Vec3(fr.x(), fr.y(), fr.z());
    }

    /** Screen angle of the arrow: 0 = straight ahead (up), clockwise. */
    private static float hoek(LocalPlayer me, Vec3 doel, float pt) {
        double dx = doel.x - me.getX(), dz = doel.z - me.getZ();
        double naar = Math.toDegrees(Math.atan2(dz, dx)) - 90;   // (minecraft yaw of the direction)
        return (float) Math.toRadians(Mth.wrapDegrees(naar - me.getViewYRot(pt)));
    }

    /** A small arrow (7x7) around (cx, cy), turned by {@code rad}. */
    private static void pijl(GuiGraphicsExtractor g, int cx, int cy, float rad) {
        g.pose().pushMatrix();
        g.pose().translate(cx + 0.5f, cy + 0.5f);
        g.pose().rotate(rad);
        int k = 0xFFB0E0FF;
        g.fill(0, -3, 1, 4, k);
        g.fill(-1, -2, 2, -1, k);
        g.fill(-2, -1, 3, 0, k);
        g.pose().popMatrix();
    }

    private static void klein(GuiGraphicsExtractor g, Font f, String s, float x, float y, float schaal, int kleur, boolean midden) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(schaal, schaal);
        g.text(f, s, midden ? -f.width(s) / 2 : 0, 0, kleur, true);
        g.pose().popMatrix();
    }

    /** A click with the mouse free (chat open): on a frame -> that member becomes your party target. */
    static boolean klik(double mx, double my) {
        for (Object[] v : VAKKEN) {
            UHudPayloads.Frame fr = (UHudPayloads.Frame) v[0];
            if (mx >= (float) v[1] && mx < (float) v[3] && my >= (float) v[2] && my < (float) v[4]) {
                if (fr.staat() == UHudPayloads.STAAT_OFFLINE || nep) {
                    return true;
                }
                gekozen = fr.id().equals(gekozen) ? null : fr.id();
                if (gekozen != null) {
                    ClientPacketDistributor.sendToServer(new UHudPayloads.Doel(gekozen));
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.6f);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** The member you clicked last (client side), or null. */
    @Nullable
    public static UUID gekozen() {
        return gekozen;
    }
}
