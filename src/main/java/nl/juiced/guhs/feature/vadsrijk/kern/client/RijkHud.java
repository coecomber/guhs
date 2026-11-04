package nl.juiced.guhs.feature.vadsrijk.kern.client;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The HUD inside the dimension (vanilla hearts, hunger, armour, air and the xp bar are hidden there):
 * <ul>
 *   <li>above the hotbar: the Vads bar (left) and the energy bar (right), the level + xp bar in the vanilla xp spot;</li>
 *   <li>right of the hotbar: 4 skill slots with the key, a cooldown sweep + seconds, the ult's charge; a 5th small slot for the
 *   Knuffelsprong (C);</li>
 *   <li>top: the big boss bar (name, phase); a big title card (Souls/Dungeons style) for bosses, areas, level-ups;</li>
 *   <li>left: the party; top right: a first-time hint card.</li>
 * </ul>
 * The values come from the server ({@link KernPayloads.Hud}, every 5 ticks).
 */
public final class RijkHud {
    private static final Set<Identifier> VERBORGEN = Set.of(VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaGuiLayers.VEHICLE_HEALTH);
    @Nullable
    private static KernPayloads.Hud hud;
    private static long hudTijd;
    @Nullable
    private static KernPayloads.BaasBalk baas;
    private static float baasGetoond = -1;
    private static List<KernPayloads.Lid> party = List.of();
    private static long partyTijd;
    // the title card
    @Nullable
    private static Component titel, onder;
    private static int titelDuur, titelKleur, titelWacht;
    private static long titelStart = -1;
    /** (integration fix round 1) A card that arrived while another had just started (< 1 s): shown right after it. */
    @Nullable
    private static Object[] titelWachtrij;
    // the hint card
    @Nullable
    private static String tip;
    private static long tipStart;
    private static long klok;

    private RijkHud() {
    }

    static void init(IEventBus modBus) {
        KernPayloads.hudOntvanger = h -> {
            hud = h;
            hudTijd = klok;
        };
        KernPayloads.baasOntvanger = b -> {
            baas = b.entityId() < 0 ? null : b;
            BaasMuziek.zet(baas == null ? "" : b.muziek());
        };
        KernPayloads.partyOntvanger = p -> {
            party = p.leden();
            partyTijd = klok;
        };
        KernPayloads.tipOntvanger = t -> {
            tip = t.key();
            tipStart = klok;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }
        };
        modBus.addListener((RegisterGuiLayersEvent e) -> {
            e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_hud"), RijkHud::teken);
            e.registerAboveAll(Guhs.id("vr_titel"), RijkHud::tekenTitel);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (RenderGuiLayerEvent.Pre e) -> {
            if (binnen() && (VERBORGEN.contains(e.getName()) || "appleskin".equals(e.getName().getNamespace()) || (SceneSpeler.actief() && !e.getName().equals(Guhs.id("vr_titel"))))) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            klok++;
            if (titelWacht > 0 && --titelWacht == 0) {
                titelStart = klok;
            }
            if (titelWachtrij != null && (titel == null || (titelStart >= 0 && klok - titelStart > titelDuur))) {
                Object[] q = titelWachtrij;
                titelWachtrij = null;
                zetTitel((Component) q[0], (Component) q[1], (Integer) q[2], (Integer) q[3]);
            }
            if (!binnen() && baas != null) {
                baas = null;
                BaasMuziek.zet("");
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            hud = null;
            baas = null;
            party = List.of();
            titel = null;
            titelWachtrij = null;
            tip = null;
        });
    }

    /** (npc's quest tracker, top right) Where the top of the screen is free: below the boss bar while one shows, else 0. */
    public static int bovenVrij() {
        return baas != null ? 46 : 0;
    }

    static boolean binnen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && Vadsrijk.in(mc.player) && hud != null && hud.binnen();
    }

    static void titel(Component t, Component o, int ticks, int kleur) {
        // two cards in the same second (a level-up and the boss's "Opgevrolijkt!"): the second one waits for the first
        if (titel != null && titelStart >= 0 && klok - titelStart < 20 && titelWacht == 0) {
            titelWachtrij = new Object[]{t, o, ticks, kleur};
            return;
        }
        zetTitel(t, o, ticks, kleur);
    }

    private static void zetTitel(Component t, Component o, int ticks, int kleur) {
        titel = t;
        onder = o;
        titelDuur = ticks;
        titelKleur = kleur;
        titelWacht = 0;
        titelStart = klok;
    }

    /** A title card that starts {@code wacht} ticks from now (scenes). */
    static void titelLater(Component t, Component o, int ticks, int kleur, int wacht) {
        zetTitel(t, o, ticks, kleur);
        if (wacht > 0) {
            titelWacht = wacht;
            titelStart = -1;
        }
    }

    // ---- the HUD --------------------------------------------------------------------------------------------------------

    private static void teken(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        KernPayloads.Hud h = hud;
        if (!binnen() || h == null || mc.options.hideGui) {
            return;
        }
        Font f = mc.font;
        int w = g.guiWidth(), hh = g.guiHeight(), c = w / 2;
        // Vads and energy above the hotbar
        balk(g, f, c - 91, hh - 41, 88, 8, h.vads() / Math.max(1, h.maxVads()), 0xFFE0457A, 0xFFFF8FB8,
                Component.literal((int) Math.ceil(h.vads()) + " / " + (int) h.maxVads()));
        balk(g, f, c + 3, hh - 41, 88, 8, h.energie() / Math.max(1, h.maxEnergie()), 0xFF3C7DE0, 0xFF8CC4FF,
                Component.literal((int) h.energie() + " / " + (int) h.maxEnergie()));
        // xp in the vanilla spot
        int xpY = hh - 29;
        g.fill(c - 91, xpY, c + 91, xpY + 3, 0xC0202020);
        float xp = h.gXpNodig() <= 0 ? 1 : (float) h.gXpIn() / h.gXpNodig();   // (g-klassen: gold = generic, pink line = class)
        g.fill(c - 91, xpY, c - 91 + (int) (182 * Mth.clamp(xp, 0, 1)), xpY + 2, 0xFFFFD24A);
        float kxp = h.xpNodig() <= 0 ? 1 : (float) h.xpIn() / h.xpNodig();
        g.fill(c - 91, xpY + 2, c - 91 + (int) (182 * Mth.clamp(kxp, 0, 1)), xpY + 3, h.grijs() ? 0xFF8A7A84 : 0xFFFF7AC8);
        // skills right of the hotbar
        int sx = c + 98, sy = hh - 22;
        for (int i = 0; i < 4; i++) {
            slot(g, f, sx + i * 23, sy, i, h);
        }
        if (h.cd().length > 4) {
            sprong(g, f, sx + 4 * 23 + 1, sy + 4, h);
        }
        // small info: duizelig, opgevrolijkt, munten (left of the hotbar)
        int ly = hh - 22;
        // g-klassen: two levels. "Generiek nv X" (Vads, Verdediging, areas, quests) and "<class/title> nv Y" (the weapon in
        // your hand); without a class weapon the class line is grey (the last active class).
        String gen = Component.translatable("vadsrijk.kern.hud.generiek", h.generiek()).getString();
        g.text(f, gen, c - 98 - f.width(gen), ly - 22, 0xFFFFE27A, true);
        String titel = Component.translatable("vadsrijk.kern.hud.klasse", Component.translatable(h.klasse()), h.level()).getString();
        g.text(f, titel, c - 98 - f.width(titel), ly - 11, h.grijs() ? 0xFF8A7A84 : 0xFFFFC2E8, true);
        String munten = Spelers.munten(h.munten());
        g.text(f, munten, c - 98 - f.width(munten), ly + 2, 0xFFFFD86B, true);
        String opg = Component.translatable("vadsrijk.kern.hud.opgevrolijkt", h.opgevrolijkt()).getString();
        g.text(f, opg, c - 98 - f.width(opg), ly + 12, 0xFFFF9AD5, true);
        if (h.duizelig() > 0) {
            String d = Component.translatable("vadsrijk.kern.hud.duizelig", (h.duizelig() + 19) / 20).getString();
            g.text(f, d, c - f.width(d) / 2, hh - 52, 0xFFB98CFF, true);
        }
        if (h.bevroren() > 0) {   // (integration: zone-2 status Bevroren gets a HUD line)
            String b = Component.translatable("vadsrijk.kern.hud.bevroren", (h.bevroren() + 19) / 20).getString();
            g.text(f, b, c - f.width(b) / 2, hh - (h.duizelig() > 0 ? 62 : 52), 0xFF9EE3FF, true);
        }
        bossBalk(g, f, w, dt.getGameTimeDeltaPartialTick(false));
        party(g, f, hh);
        tip(g, f, w);
    }

    private static void balk(GuiGraphicsExtractor g, Font f, int x, int y, int bw, int bh, float frac, int kleur, int licht, Component tekst) {
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xFF1A1018);
        g.fill(x, y, x + bw, y + bh, 0xFF3A2A33);
        int v = (int) (bw * Mth.clamp(frac, 0, 1));
        g.fill(x, y, x + v, y + bh, kleur);
        g.fill(x, y, x + v, y + 2, licht);
        String s = tekst.getString();
        g.pose().pushMatrix();
        g.pose().translate(x + bw / 2f, y + 1);
        g.pose().scale(0.75f, 0.75f);
        g.text(f, s, -f.width(s) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    private static void slot(GuiGraphicsExtractor g, Font f, int x, int y, int i, KernPayloads.Hud h) {
        boolean ult = i == 3;
        int rand = ult ? (h.ult() >= 100 ? 0xFFFFD24A : 0xFF8A5A2A) : 0xFF6A3A5A;
        g.fill(x - 1, y - 1, x + 21, y + 21, rand);
        g.fill(x, y, x + 20, y + 20, 0xE0281A24);
        String naam = i < h.skills().size() ? Component.translatable(h.skills().get(i)).getString() : "";
        String letters = naam.isEmpty() ? "?" : naam.substring(0, Math.min(2, naam.length()));
        g.text(f, letters, x + 10 - f.width(letters) / 2, y + 6, 0xFFFFC2E8, true);
        int cd = h.cd()[i], max = Math.max(1, h.cdMax()[i]);
        if (ult && h.ult() < 100) {
            int vol = (int) (20 * h.ult() / 100f);
            g.fill(x, y + 20 - vol, x + 20, y + 20, 0x60FFD24A);
            String p = (int) h.ult() + "%";
            g.pose().pushMatrix();
            g.pose().translate(x + 10, y + 14);
            g.pose().scale(0.6f, 0.6f);
            g.text(f, p, -f.width(p) / 2, 0, 0xFFFFE27A, true);
            g.pose().popMatrix();
        }
        if (cd > 0) {
            int hoog = (int) Math.ceil(20f * cd / max);
            g.fill(x, y + 20 - hoog, x + 20, y + 20, 0xB0000000);
            String s = String.valueOf((cd + 19) / 20);
            g.text(f, s, x + 10 - f.width(s) / 2, y + 6, 0xFFFFFFFF, true);
        }
        KeyMapping k = KernClient.SKILLS[i];
        String toets = k.getTranslatedKeyMessage().getString();
        if (toets.length() > 3) {
            toets = toets.substring(0, 3);
        }
        g.pose().pushMatrix();
        g.pose().translate(x + 1, y + 1);
        g.pose().scale(0.6f, 0.6f);
        g.text(f, toets, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    /** The 5th, smaller slot: the Knuffelsprong (every class), with its key and cooldown. */
    private static void sprong(GuiGraphicsExtractor g, Font f, int x, int y, KernPayloads.Hud h) {
        int z = 16;
        int cd = h.cd()[4], max = Math.max(1, h.cdMax()[4]);
        g.fill(x - 1, y - 1, x + z + 1, y + z + 1, cd > 0 ? 0xFF4A3A4A : 0xFF8FD4FF);
        g.fill(x, y, x + z, y + z, 0xE0281A24);
        // a little arc with a heart on top: the leap
        int[][] boog = {{2, 12}, {3, 10}, {4, 8}, {5, 7}, {6, 6}, {7, 5}, {8, 5}, {9, 5}, {10, 6}, {11, 7}, {12, 8}, {13, 10}};
        for (int[] b : boog) {
            g.fill(x + b[0], y + b[1], x + b[0] + 1, y + b[1] + 1, 0xFFFFC2E8);
        }
        g.fill(x + 7, y + 2, x + 8, y + 3, 0xFFFF6AA8);
        g.fill(x + 9, y + 2, x + 10, y + 3, 0xFFFF6AA8);
        g.fill(x + 7, y + 3, x + 10, y + 4, 0xFFFF6AA8);
        g.fill(x + 8, y + 4, x + 9, y + 5, 0xFFFF6AA8);
        if (cd > 0) {
            int hoog = (int) Math.ceil(z * (float) cd / max);
            g.fill(x, y + z - hoog, x + z, y + z, 0xB0000000);
            String s = String.valueOf((cd + 19) / 20);
            g.text(f, s, x + z / 2 - f.width(s) / 2, y + 4, 0xFFFFFFFF, true);
        }
        String toets = KernClient.SKILLS[4].getTranslatedKeyMessage().getString();
        if (toets.length() > 3) {
            toets = toets.substring(0, 3);
        }
        g.pose().pushMatrix();
        g.pose().translate(x + 1, y + 1);
        g.pose().scale(0.6f, 0.6f);
        g.text(f, toets, 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    private static void bossBalk(GuiGraphicsExtractor g, Font f, int w, float pt) {
        KernPayloads.BaasBalk b = baas;
        if (b == null) {
            baasGetoond = -1;
            return;
        }
        float frac = b.vads() / Math.max(1, b.max());
        baasGetoond = baasGetoond < 0 ? frac : Mth.lerp(0.08f, baasGetoond, frac);
        int bw = Math.min(300, w - 40), x = w / 2 - bw / 2, y = 22;
        g.pose().pushMatrix();
        g.pose().translate(w / 2f, 6);
        g.pose().scale(1.25f, 1.25f);
        Component naam = b.naam();
        g.text(f, naam, -f.width(naam) / 2, 0, 0xFFFF6A6A, true);
        g.pose().popMatrix();
        g.fill(x - 2, y - 2, x + bw + 2, y + 9, 0xFF120A0E);
        g.fill(x, y, x + bw, y + 7, 0xFF3A1A22);
        g.fill(x, y, x + (int) (bw * baasGetoond), y + 7, 0xFFFFE0A0);
        g.fill(x, y, x + (int) (bw * frac), y + 7, 0xFFC23A3A);
        g.fill(x, y, x + (int) (bw * frac), y + 2, 0xFFFF7A7A);
        for (float t : new float[] {0.5f, 0.2f}) {
            g.fill(x + (int) (bw * t), y - 1, x + (int) (bw * t) + 1, y + 8, 0xFFFFFFFF);
        }
        String fase = Component.translatable("vadsrijk.kern.hud.fase", b.fase()).getString();
        g.text(f, fase, x + bw - f.width(fase), y + 11, 0xFFFFC2C2, true);
        String getal = (int) b.vads() + " / " + (int) b.max();
        g.text(f, getal, x, y + 11, 0xFFDDDDDD, true);
    }

    private static void party(GuiGraphicsExtractor g, Font f, int hh) {
        if (party.isEmpty() || klok - partyTijd > 60) {
            return;
        }
        int y = hh / 2 - party.size() * 14 / 2;
        for (KernPayloads.Lid l : party) {
            String n = (l.leider() ? "★ " : "") + l.naam() + " Nv" + l.level();
            g.text(f, n, 6, y, 0xFFFFFFFF, true);
            g.fill(6, y + 9, 86, y + 12, 0xFF3A2A33);
            g.fill(6, y + 9, 6 + (int) (80 * Mth.clamp(l.vads() / Math.max(1, l.max()), 0, 1)), y + 12, 0xFFE0457A);
            y += 16;
        }
    }

    private static void tip(GuiGraphicsExtractor g, Font f, int w) {
        if (tip == null) {
            return;
        }
        // fix round 1: top left (the quest tracker owns the top right), below the boss bar when one shows; longer hints stay longer
        int bw = 190, x = 8, y = 8 + bovenVrij();
        String titelKey = tip + ".titel";
        List<FormattedCharSequence> regels = f.split(Component.translatable(tip), bw - 12);
        int duur = 200 + 30 * Math.max(0, regels.size() - 3);
        if (klok - tipStart > duur) {
            return;
        }
        int hoog = 20 + regels.size() * 10;
        float in = Mth.clamp((klok - tipStart) / 6f, 0, 1) * Mth.clamp((duur - (klok - tipStart)) / 10f, 0, 1);
        int a = (int) (in * 235) << 24;
        g.fill(x, y, x + bw, y + hoog, a | 0x2A1424);
        g.fill(x, y, x + 2, y + hoog, a | 0xFF7AC8);
        if (in > 0.1f) {
            g.text(f, net.minecraft.client.resources.language.I18n.exists(titelKey) ? Component.translatable(titelKey) : Component.literal("Tip"), x + 7, y + 5,
                    0xFFFF9AD5, true);
            int ry = y + 17;
            for (FormattedCharSequence r : regels) {
                g.text(f, r, x + 7, ry, 0xFFEFE6EC, true);
                ry += 10;
            }
            // a thin bar that runs out: how long the hint stays
            int rest = (int) ((bw - 4) * Mth.clamp(1 - (klok - tipStart) / (float) duur, 0, 1));
            g.fill(x + 2, y + hoog - 2, x + 2 + rest, y + hoog - 1, a | 0xFF9AD5);
        }
    }

    // ---- title cards and the scene bars -----------------------------------------------------------------------------------

    private static void tekenTitel(GuiGraphicsExtractor g, DeltaTracker dt) {
        float pt = dt.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), hh = g.guiHeight();
        float b = SceneSpeler.balk(pt);
        if (b > 0) {
            int bar = (int) (hh * 0.12f * b);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, hh - bar, w, hh, 0xFF000000);
        }
        Component t = titel;
        if (t == null || titelStart < 0) {
            return;
        }
        float tijd = klok - titelStart + pt;
        if (tijd > titelDuur) {
            titel = null;
            return;
        }
        float in = Mth.clamp(tijd / 8f, 0, 1) * Mth.clamp((titelDuur - tijd) / 10f, 0, 1);
        if (in <= 0.02f) {
            return;
        }
        Font f = Minecraft.getInstance().font;
        int a = (int) (in * 255) << 24;
        int y = (int) (hh * 0.38f);
        // dark band, the accent line, the big title, the subtitle
        g.fillGradient(0, y - 22, w, y + 34, 0, 0);
        g.fill(0, y - 18, w, y + 30, (int) (in * 140) << 24);
        int lijn = (int) (Math.min(1, tijd / 12f) * w * 0.35f);
        g.fill(w / 2 - lijn, y + 17, w / 2 + lijn, y + 18, a | (titelKleur & 0xFFFFFF));
        g.pose().pushMatrix();
        g.pose().translate(w / 2f, y - 10);
        float s = 2.4f + 0.15f * (1 - Math.min(1, tijd / 15f));
        g.pose().scale(s, s);
        g.text(f, t, -f.width(t) / 2, 0, a | 0xFFFFFF, true);
        g.pose().popMatrix();
        if (onder != null) {
            g.centeredText(f, onder, w / 2, y + 21, a | 0xE8D6DF);
        }
    }
}
