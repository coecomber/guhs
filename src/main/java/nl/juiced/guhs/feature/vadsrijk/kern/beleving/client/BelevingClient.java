package nl.juiced.guhs.feature.vadsrijk.kern.beleving.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.beleving.BelevingPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.beleving.Reeks;
import nl.juiced.guhs.feature.vadsrijk.kern.client.SceneSpeler;

/**
 * The client side of the Vadsrijk combat feel:
 * <ul>
 *   <li>hit-stop: a hit entity's animation holds still for 2-4 ticks (its tick counter is held, so vanilla and GeckoLib
 *   animations both freeze), the attacker's camera kicks up a little;</li>
 *   <li>screen shake (camera yaw/pitch/roll noise), scaled by the option slider ({@link BelevingConfig}, 0 = off);</li>
 *   <li>boss moments: a camera swoop to the boss + zoom + letterbox, everything around runs at half speed (slow-mo feel),
 *   the boss freezes; phases flash the boss bar with the phase name; victories flash white;</li>
 *   <li>the Vrolijke reeks counter (right of the screen) with a rising plof note per cheer-up;</li>
 *   <li>a full-screen colour flash (Guhddelijk loot).</li>
 * </ul>
 */
public final class BelevingClient {
    private static final Map<Integer, Integer> BEVROREN = new HashMap<>();
    private static long klok;
    // camera
    private static float kickSterkte;
    private static long kickStart = -100;
    private static float schudSterkte;
    private static int schudDuur;
    private static long schudStart = -100;
    private static int focusId = -1, focusDuur;
    private static long focusStart = -100;
    @Nullable
    private static Vec3 focusPlek;
    private static int traagTot;
    // overlays
    private static int flitsKleur, flitsDuur;
    private static long flitsStart = -100;
    @Nullable
    private static Component faseTekst;
    private static long faseStart = -100;
    private static int reeksN, reeksXp, weg;
    private static long reeksTijd = -1000, mijlpaalTijd = -1000, wegTijd = -1000;
    private static final SoundEvent PIEP = SoundEvent.createVariableRangeEvent(Guhs.id("klusjes.piep"));

    private BelevingClient() {
    }

    public static void init(IEventBus modBus) {
        net.neoforged.fml.ModList.get().getModContainerById(Guhs.MODID)
                .ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, BelevingConfig.SPEC, "guhs-vadsrijk-client.toml"));
        BelevingPayloads.ontvanger = BelevingClient::ontvang;
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(Guhs.id("vr_beleving"), BelevingClient::teken));
        NeoForge.EVENT_BUS.addListener(BelevingClient::tick);
        NeoForge.EVENT_BUS.addListener(BelevingClient::hoeken);
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov e) -> {
            float w = focusGewicht((float) e.getPartialTick());
            if (w > 0) {
                e.setFOV(e.getFOV() * (1 - 0.16f * w));
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            BEVROREN.clear();
            reeksN = 0;
            faseTekst = null;
            focusId = -1;
            traagTot = 0;
        });
    }

    private static void ontvang(BelevingPayloads.Moment m) {
        Minecraft mc = Minecraft.getInstance();
        var d = m.d();
        float s = BelevingConfig.sterkte();
        switch (m.soort()) {
            case "klap" -> {
                if (s > 0) {
                    BEVROREN.merge(d.getIntOr("id", -1), d.getIntOr("ticks", 2), Math::max);
                    float kick = d.getFloatOr("kick", 0);
                    if (kick > 0) {
                        kickSterkte = Math.max(kickSterkte * Math.max(0, 1 - (klok - kickStart) / 4f), kick);
                        kickStart = klok;
                    }
                }
            }
            case "schud" -> schud(d.getFloatOr("sterkte", 0.5f), d.getIntOr("ticks", 10));
            case "reeks" -> {
                int n = d.getIntOr("n", 1);
                reeksN = n;
                reeksXp = d.getIntOr("xp", 0);
                reeksTijd = klok;
                if (d.getBooleanOr("mijlpaal", false)) {
                    mijlpaalTijd = klok;
                }
                if (mc.player != null && mc.level != null) {
                    float toon = Reeks.toon(n);
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), PIEP, SoundSource.PLAYERS, 0.6f, toon, false);
                    mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                            0.35f, toon, false);
                }
            }
            case "reeks_weg" -> {
                weg = d.getIntOr("n", 0);
                wegTijd = klok;
                reeksN = 0;
            }
            case "fase" -> {
                int id = d.getIntOr("id", -1);
                faseTekst = Component.translatableWithFallback(d.getStringOr("key", ""), d.getStringOr("terugval", "Nieuwe fase!"));
                faseStart = klok;
                focus(id, 22);
                BEVROREN.merge(id, 12, Math::max);
                traagTot = Math.max(traagTot, 20);
            }
            case "overwinning" -> {
                int id = d.getIntOr("id", -1);
                focus(id, 34);
                BEVROREN.merge(id, 14, Math::max);
                traagTot = Math.max(traagTot, 30);
                flits(0xFFFFFF, 8);
            }
            case "flits" -> flits(d.getIntOr("kleur", 0xFF7AC8), d.getIntOr("ticks", 12));
            default -> {
            }
        }
    }

    private static void schud(float sterkte, int ticks) {
        float nu = huidigeSchud(0);
        schudSterkte = Math.max(nu, sterkte);
        schudDuur = Math.max(4, ticks);
        schudStart = klok;
    }

    private static void focus(int id, int duur) {
        focusId = id;
        focusDuur = duur;
        focusStart = klok;
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(id);
        focusPlek = e == null ? null : e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    private static void flits(int kleur, int ticks) {
        flitsKleur = kleur;
        flitsDuur = ticks;
        flitsStart = klok;
    }

    // ---- ticking: the freezes and the slow-mo -----------------------------------------------------------------------------

    private static void tick(ClientTickEvent.Post e) {
        klok++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        Iterator<Map.Entry<Integer, Integer>> it = BEVROREN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> b = it.next();
            Entity ent = mc.level.getEntity(b.getKey());
            if (ent != null) {
                ent.tickCount--;   // (it just ticked: hold its animation clock)
            }
            if (b.getValue() <= 1 || ent == null) {
                it.remove();
            } else {
                b.setValue(b.getValue() - 1);
            }
        }
        if (traagTot > 0) {
            traagTot--;
            if (klok % 2 == 0 && mc.player != null) {
                for (Entity ent : mc.level.entitiesForRendering()) {
                    if (ent instanceof LivingEntity && ent != mc.player && !BEVROREN.containsKey(ent.getId()) && ent.distanceToSqr(mc.player) < 48 * 48) {
                        ent.tickCount--;   // half speed
                    }
                }
            }
        }
        if (focusId >= 0) {
            Entity f = mc.level.getEntity(focusId);
            if (f != null && !f.isRemoved()) {
                focusPlek = f.position().add(0, f.getBbHeight() * 0.6, 0);
            }
            if (klok - focusStart > focusDuur) {
                focusId = -1;
            }
        }
    }

    // ---- the camera -------------------------------------------------------------------------------------------------------

    private static float huidigeSchud(float pt) {
        float t = klok - schudStart + pt;
        return t < 0 || t > schudDuur ? 0 : schudSterkte * (1 - t / schudDuur);
    }

    private static float focusGewicht(float pt) {
        if (focusId < 0 && klok - focusStart > focusDuur) {
            return 0;
        }
        float s = BelevingConfig.sterkte();
        if (s <= 0 || SceneSpeler.actief()) {
            return 0;
        }
        float u = (klok - focusStart + pt) / Math.max(1, focusDuur);
        if (u < 0 || u > 1) {
            return 0;
        }
        float w = u < 0.2f ? u / 0.2f : u > 0.7f ? (1 - u) / 0.3f : 1;
        w = w * w * (3 - 2 * w);
        return w * Math.min(1, s * 1.5f);
    }

    private static void hoeken(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player) || SceneSpeler.actief()) {
            return;
        }
        float pt = (float) e.getPartialTick();
        float s = BelevingConfig.sterkte();
        float yaw = e.getYaw(), pitch = e.getPitch(), roll = e.getRoll();
        // the swoop to the boss
        float w = focusGewicht(pt);
        if (w > 0 && focusPlek != null) {
            Vec3 c = e.getCamera().position();
            Vec3 d = focusPlek.subtract(c);
            double hor = Math.sqrt(d.x * d.x + d.z * d.z);
            float doelYaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float doelPitch = (float) (-Mth.atan2(d.y, hor) * Mth.RAD_TO_DEG);
            yaw += Mth.wrapDegrees(doelYaw - yaw) * w;
            pitch += (doelPitch - pitch) * w;
        }
        if (s > 0) {
            // the kick of a heavy hit (up, then back)
            float kt = klok - kickStart + pt;
            if (kt >= 0 && kt < 5) {
                float k = kickSterkte * s * (float) Math.sin(Math.PI * Math.min(1, kt / 5f)) * (1 - kt / 5f) * 2.4f;
                pitch -= k;
                roll += k * 0.35f;
            }
            float a = huidigeSchud(pt) * s * 2.6f;
            if (a > 0.01f) {
                float t = klok + pt;
                yaw += a * (float) (Math.sin(t * 2.7) * 0.6 + Math.sin(t * 5.3 + 1.3) * 0.4);
                pitch += a * (float) (Math.sin(t * 3.1 + 0.7) * 0.6 + Math.sin(t * 6.1 + 2.1) * 0.4);
                roll += a * 0.6f * (float) Math.sin(t * 4.3 + 0.4);
            }
        }
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.setRoll(roll);
    }

    // ---- the overlays -----------------------------------------------------------------------------------------------------

    private static void teken(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Vadsrijk.in(mc.player)) {
            return;
        }
        float pt = dt.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        Font f = mc.font;
        // letterbox while a boss moment holds the camera
        float fw = focusGewicht(pt);
        if (fw > 0) {
            int band = (int) (h * 0.07f * fw);
            g.fill(0, 0, w, band, 0xE0000000);
            g.fill(0, h - band, w, h, 0xE0000000);
        }
        if (!mc.options.hideGui) {
            fase(g, f, w, pt);
            reeks(g, f, w, h, pt);
        }
        // the flash
        float ft = klok - flitsStart + pt;
        if (ft >= 0 && ft < flitsDuur) {
            int a = (int) (150 * (1 - ft / flitsDuur));
            g.fill(0, 0, w, h, (a << 24) | (flitsKleur & 0xFFFFFF));
        }
    }

    private static void fase(GuiGraphicsExtractor g, Font f, int w, float pt) {
        Component t = faseTekst;
        float tijd = klok - faseStart + pt;
        if (t == null || tijd < 0 || tijd > 70) {
            return;
        }
        // the boss bar flashes (RijkHud draws it at y 20..29, width min(300, w - 40))
        if (tijd < 30) {
            int bw = Math.min(300, w - 40), x = w / 2 - bw / 2;
            float puls = (float) (Math.sin(tijd * 1.3) * 0.5 + 0.5) * (1 - tijd / 30f);
            g.fill(x - 3, 17, x + bw + 3, 33, ((int) (puls * 200) << 24) | 0xFFFFFF);
        }
        float in = Mth.clamp(tijd / 5f, 0, 1) * Mth.clamp((70 - tijd) / 12f, 0, 1);
        if (in <= 0.05f) {
            return;
        }
        int a = (int) (in * 255) << 24;
        g.pose().pushMatrix();
        g.pose().translate(w / 2f, 44);
        float s = 1.5f + 0.4f * (1 - Math.min(1, tijd / 8f));
        g.pose().scale(s, s);
        g.text(f, t, -f.width(t) / 2, 0, a | 0xFF6A6A, true);
        g.pose().popMatrix();
    }

    private static void reeks(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        float tijd = klok - reeksTijd + pt;
        int rx = w - 10, ry = h / 2 - 34;
        if (reeksN >= 2 && tijd <= Reeks.VENSTER + 4) {
            String kop = Component.translatableWithFallback("vadsrijk.kern.beleving.reeks", "Vrolijke reeks").getString();
            g.text(f, kop, rx - f.width(kop), ry, 0xFFFF9AD5, true);
            String getal = "×" + reeksN + (reeksN >= 10 ? "!" : "");
            float pop = 1 + 0.15f * Math.max(0, 1 - tijd / 5f) + (klok - mijlpaalTijd < 20 ? 0.25f * (1 - (klok - mijlpaalTijd + pt) / 20f) : 0);
            float s = 2.0f * Math.min(1.25f, pop);
            g.pose().pushMatrix();
            g.pose().translate(rx, ry + 20 - 4.5f * s);   // (grows around its middle: never into the title or the bar)
            g.pose().scale(s, s);
            int kleur = reeksN >= 50 ? 0xFFFF5FC8 : reeksN >= 10 ? 0xFFFFD24A : 0xFFFFFFFF;
            g.text(f, getal, -f.width(getal), 0, kleur, true);
            g.pose().popMatrix();
            int by = ry + 11 + (int) (9 * 2.0f) + 3, bw = 70;
            float rest = Mth.clamp(1 - tijd / Reeks.VENSTER, 0, 1);
            g.fill(rx - bw, by, rx, by + 3, 0xA0301828);
            g.fill(rx - (int) (bw * rest), by, rx, by + 3, rest < 0.3f ? 0xFFFF7A7A : 0xFFFF9AD5);
            if (reeksXp > 0) {
                String xp = "+" + reeksXp + "% xp";
                g.text(f, xp, rx - f.width(xp), by + 6, 0xFFFFE27A, true);
            }
        } else {
            float wt = klok - wegTijd + pt;
            if (weg >= 5 && wt >= 0 && wt < 36) {
                int a = (int) (255 * (1 - wt / 40f)) << 24;
                String s = Component.translatableWithFallback("vadsrijk.kern.beleving.reeks_weg", "Reeks voorbij: ×%s", weg).getString();
                if ((a >>> 24) > 12) {
                    g.text(f, s, rx - f.width(s), ry + 11, a | 0xBBBBBB, true);
                }
            }
        }
    }
}
