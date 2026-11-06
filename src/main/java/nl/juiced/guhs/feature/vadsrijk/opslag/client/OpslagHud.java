package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.opslag.OpslagPayloads;

/**
 * T-OPSLAG: the HUD additions of §6.1, drawn through kern's {@link RijkHud#voegToe} in kern's reserved spots ({@link RijkHud#plek}):
 * <ul>
 *   <li><b>buff row</b> above the Vads/energy bars: buffs from the left, debuffs from the right, each with a time sweep + seconds,
 *   and the name of a new status flashing above it for 2 s; other slices add icons (drink cooldowns) with {@link #EXTRA};</li>
 *   <li><b>target frame</b> top centre: element, Elite, name, level, a HP bar with numbers and the target's statuses. The target is
 *   what you look at (24 blocks), else what you last hit (5 s). Not shown for a boss while its big bar shows;</li>
 *   <li><b>cast bars</b>: yours above the buff row, a boss's under the boss bar (name of the attack; a gold frame = interrupt it).</li>
 * </ul>
 */
public final class OpslagHud {
    /** An icon of another slice in the buff row (t-items: drink cooldowns): item, part left 0..1, seconds left, buff side? */
    public record ExtraIcoon(ItemStack item, float rest, int seconden, boolean buff) {
    }

    /** t-items etc.: {@code OpslagHud.EXTRA.add(() -> List.of(new OpslagHud.ExtraIcoon(stack, 0.4f, 12, true)))}. */
    public static final List<Supplier<List<ExtraIcoon>>> EXTRA = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final int DOEL_BEREIK = 24, VAK = 12, STAP = 14;

    private static long klok;
    // statuses: ticks left as sent + when it arrived; the longest seen = 100 %
    private static OpslagPayloads.HudInfo info = new OpslagPayloads.HudInfo(List.of(), -1, List.of());
    private static long infoTijd;
    private static final Map<Status, Integer> MAX = new EnumMap<>(Status.class);
    @Nullable
    private static Status nieuw;
    private static long nieuwTijd;
    // target
    private static int gekeken = -1;
    private static long gekekenTijd;
    private static int getoond = -1;
    // cast bars
    @Nullable
    private static Giet eigen, baas;

    private record Giet(int bron, Component naam, long start, int duur, int kleur, boolean onderbreekbaar, long stop, boolean onderbroken) {
        Giet gestopt(long nu, boolean onderbroken) {
            return new Giet(bron, naam, start, duur, kleur, onderbreekbaar, nu, onderbroken);
        }
    }

    private OpslagHud() {
    }

    // ---- data in -----------------------------------------------------------------------------------------------------------

    static void ontvang(OpslagPayloads.HudInfo h) {
        java.util.Set<Integer> oud = new java.util.HashSet<>();
        info.statussen().forEach(r -> oud.add(r.status()));
        for (OpslagPayloads.StatusRegel r : h.statussen()) {
            Status s = status(r.status());
            if (s == null) {
                continue;
            }
            if (!oud.contains(r.status())) {
                nieuw = s;
                nieuwTijd = klok;
                MAX.put(s, r.ticks());
            } else if (r.ticks() > MAX.getOrDefault(s, 0)) {
                MAX.put(s, r.ticks());   // (refreshed: the new length is 100 %)
            }
        }
        info = h;
        infoTijd = klok;
    }

    static void giet(OpslagPayloads.Giet g) {
        if (g.ticks() <= 0) {
            if (g.baas()) {
                baas = baas == null ? null : baas.gestopt(klok, g.onderbroken());
            } else {
                eigen = eigen == null ? null : eigen.gestopt(klok, g.onderbroken());
            }
            return;
        }
        Giet n = new Giet(g.bron(), Component.translatable(g.naamKey()), klok, g.ticks(), 0xFF000000 | g.kleur(), g.onderbreekbaar(), -1, false);
        if (g.baas()) {
            baas = n;
        } else {
            eigen = n;
        }
    }

    static void tick() {
        klok++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || klok % 2 != 0) {
            return;
        }
        Entity e = kijkDoel(mc);
        if (e != null) {
            gekeken = e.getId();
            gekekenTijd = klok;
        }
    }

    static void wis() {
        info = new OpslagPayloads.HudInfo(List.of(), -1, List.of());
        MAX.clear();
        eigen = null;
        baas = null;
        gekeken = -1;
        getoond = -1;
        nieuw = null;
    }

    /** Pixels the boss cast bar takes below the boss bar (kern's quest tracker moves down by this). */
    static int bovenExtra() {
        return actief(baas) ? 12 : 0;
    }

    @Nullable
    private static Status status(int i) {
        return i >= 0 && i < Status.values().length ? Status.values()[i] : null;
    }

    /** What the crosshair points at within {@link #DOEL_BEREIK} blocks (a living Vadsrijk mob, not through walls). */
    @Nullable
    private static Entity kijkDoel(Minecraft mc) {
        Entity cam = mc.getCameraEntity();
        if (cam == null) {
            return null;
        }
        Vec3 oog = cam.getEyePosition(1f), kijk = cam.getViewVector(1f);
        double bereik = DOEL_BEREIK;
        HitResult blok = cam.pick(bereik, 1f, false);
        if (blok.getType() != HitResult.Type.MISS) {
            bereik = blok.getLocation().distanceTo(oog);
        }
        Vec3 eind = oog.add(kijk.scale(bereik));
        AABB vak = cam.getBoundingBox().expandTowards(kijk.scale(bereik)).inflate(1);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(cam, oog, eind, vak, x -> x instanceof VadsrijkMob m && m.isAlive() && !m.geplofd(), bereik * bereik);
        return r == null ? null : r.getEntity();
    }

    // ---- drawing -----------------------------------------------------------------------------------------------------------

    static void tekenBuffs(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        int[] p = RijkHud.plek("buffs", w, h);
        int links = p[0], y = p[1], rechts = p[2], z = p[3];
        int perKant = Math.min(3, Math.max(1, (w / 2 - 45 - links) / STAP));   // (3: the centre stays free for kern's Duizelig/Bevroren line)
        float verstreken = klok - infoTijd + pt;
        int nb = 0, nd = 0;
        boolean verouderd = klok - infoTijd > 40;   // (the server sends every 5 ticks while there is something: stale = gone)
        if (!verouderd) {
            for (OpslagPayloads.StatusRegel r : info.statussen()) {
                Status s = status(r.status());
                if (s == null) {
                    continue;
                }
                float ticks = Math.max(0, r.ticks() - verstreken);
                float rest = ticks / Math.max(1, MAX.getOrDefault(s, r.ticks()));
                int sec = (int) Math.ceil(ticks / 20f);
                int[] xy = s.buff ? vak(links, y, nb++, perKant, true) : vak(rechts - z, y, nd++, perKant, false);
                StatusIcoon.teken(g, f, s, xy[0], xy[1], z, rest, sec);
                if (s == nieuw && klok - nieuwTijd < 40) {
                    String n = Component.translatable(s.key()).getString();
                    int a = (int) (255 * Mth.clamp((40 - (klok - nieuwTijd)) / 10f, 0, 1)) << 24;
                    int tx = s.buff ? xy[0] : xy[0] + z - f.width(n);
                    g.text(f, n, tx, xy[1] - 10, a | (StatusIcoon.kleur(s) & 0xFFFFFF), true);
                }
            }
        }
        for (Supplier<List<ExtraIcoon>> bron : EXTRA) {
            for (ExtraIcoon e : bron.get()) {
                int[] xy = e.buff() ? vak(links, y, nb++, perKant, true) : vak(rechts - z, y, nd++, perKant, false);
                g.fill(xy[0] - 1, xy[1] - 1, xy[0] + z + 1, xy[1] + z + 1, e.buff() ? 0xFF2E7A3A : 0xFF8A2A3A);
                g.fill(xy[0], xy[1], xy[0] + z, xy[1] + z, 0xE0201418);
                g.pose().pushMatrix();
                g.pose().translate(xy[0], xy[1]);
                g.pose().scale(z / 16f, z / 16f);
                g.item(e.item(), 0, 0);
                g.pose().popMatrix();
                if (e.rest() < 1) {
                    g.fill(xy[0], xy[1], xy[0] + z, xy[1] + (int) (z * (1 - Math.max(0, e.rest()))), 0x90000000);
                }
            }
        }
    }

    /** The n-th icon on one side: up to perKant in a row, then a row higher. */
    private static int[] vak(int x0, int y0, int n, int perKant, boolean naarRechts) {
        int rij = n / perKant, kol = n % perKant;
        return new int[] {naarRechts ? x0 + kol * STAP : x0 - kol * STAP, y0 - rij * STAP};
    }

    static void tekenDoel(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int id = klok - gekekenTijd <= 6 ? gekeken : (klok - infoTijd <= 40 ? info.doel() : -1);
        if (id < 0 && getoond >= 0 && klok - gekekenTijd < 100) {
            id = getoond;   // (keep the last one a moment)
        }
        Entity e = id < 0 ? null : mc.level.getEntity(id);
        if (!(e instanceof VadsrijkMob m) || !m.isAlive() || (m instanceof VadsrijkBaas && RijkHud.baasActief())) {
            getoond = -1;
            return;
        }
        getoond = id;
        int[] p = RijkHud.plek("doelframe", w, h);
        int x = p[0], y = p[1], bw = p[2];
        g.fill(x - 1, y - 1, x + bw + 1, y + 21, m.elite() ? 0xFFD8A23A : 0xFF4A2A3A);
        g.fill(x, y, x + bw, y + 20, 0xD0201418);
        MutableComponent naam = m.element().icoon();
        if (m.elite()) {
            naam.append(Component.translatable("vadsrijk.kern.elite").withStyle(ChatFormatting.GOLD)).append(" ");
        }
        naam.append(m.getName().copy().withStyle(ChatFormatting.WHITE));
        String lv = Component.translatable("vadsrijk.opslag.doel.level", m.mobLevel()).getString();
        int ruimte = bw - 8 - f.width(lv);
        g.pose().pushMatrix();
        float s = Math.min(1f, ruimte / (float) Math.max(1, f.width(naam)));
        g.pose().translate(x + 3, y + 2);
        g.pose().scale(s, s);
        g.text(f, naam, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        g.text(f, lv, x + bw - 3 - f.width(lv), y + 2, 0xFFFFE27A, true);
        float frac = (float) (m.vads() / Math.max(1, m.maxVads()));
        int by = y + 12, bh = 6;
        g.fill(x + 3, by, x + bw - 3, by + bh, 0xFF3A1A22);
        g.fill(x + 3, by, x + 3 + (int) ((bw - 6) * Mth.clamp(frac, 0, 1)), by + bh, frac > 0.5f ? 0xFF5AC85A : frac > 0.2f ? 0xFFE0B040 : 0xFFE04A4A);
        String getal = (int) Math.ceil(m.vads()) + " / " + (int) m.maxVads();
        g.pose().pushMatrix();
        g.pose().translate(x + bw / 2f, by + 0.5f);
        g.pose().scale(0.6f, 0.6f);
        g.text(f, getal, -f.width(getal) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        // the target's statuses (only known for what you hit: the server sends those)
        if (id == info.doel() && klok - infoTijd <= 40) {
            int ix = x;
            for (OpslagPayloads.StatusRegel r : info.doelStatussen()) {
                Status st = status(r.status());
                if (st != null) {
                    StatusIcoon.teken(g, f, st, ix, y + 23, 9, 1, -1);
                    ix += 11;
                }
            }
        }
    }

    static void tekenGiet(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        if (actief(eigen)) {
            int[] p = RijkHud.plek("gietbalk", w, h);
            balk(g, f, eigen, p[0], p[1], p[2], p[3], pt);
        }
        if (actief(baas)) {
            int[] p = RijkHud.plek("baasgiet", w, h);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getEntity(baas.bron()) == null && baas.stop() < 0) {
                baas = baas.gestopt(klok, false);   // (the caster is gone)
            }
            balk(g, f, baas, p[0], p[1], p[2], p[3], pt);
        }
    }

    private static boolean actief(@Nullable Giet b) {
        return b != null && (b.stop() < 0 ? klok - b.start() <= b.duur() + 10 : klok - b.stop() <= 15);
    }

    private static void balk(GuiGraphicsExtractor g, Font f, Giet b, int x, int y, int bw, int bh, float pt) {
        float t = (b.stop() < 0 ? klok - b.start() + pt : b.stop() - b.start());
        float frac = b.onderbroken() ? t / Math.max(1, b.duur()) : (b.stop() >= 0 ? 1 : t / Math.max(1, b.duur()));
        g.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, b.onderbreekbaar() ? 0xFFFFD24A : 0xFF120A0E);
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF120A0E);
        g.fill(x, y, x + bw, y + bh, 0xFF3A2A33);
        int kleur = b.onderbroken() ? 0xFF8A3A3A : b.kleur();
        g.fill(x, y, x + (int) (bw * Mth.clamp(frac, 0, 1)), y + bh, kleur);
        g.fill(x, y, x + (int) (bw * Mth.clamp(frac, 0, 1)), y + 2, 0x60FFFFFF);
        Component tekst = b.onderbroken() ? Component.translatable("vadsrijk.opslag.giet.onderbroken") : b.naam();
        g.pose().pushMatrix();
        g.pose().translate(x + bw / 2f, y + 1);
        g.pose().scale(0.75f, 0.75f);
        g.text(f, tekst, -f.width(tekst) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        if (b.stop() < 0) {
            String s = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, (b.duur() - t) / 20f));
            g.pose().pushMatrix();
            g.pose().translate(x + bw + 4, y + 1);
            g.pose().scale(0.75f, 0.75f);
            g.text(f, s, 0, 0, 0xFFE8D6DF, true);
            g.pose().popMatrix();
        }
    }
}
