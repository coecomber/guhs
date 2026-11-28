package nl.juiced.guhs.feature.vadsrijk.menu;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-TABS: the Vadsrijk-menu's tab registry (order, sub-tabs, the old CollectieTabs shim never overwrites), the frame geometry
 * (same frame for every tab, never outside the screen at GUI scale 2-4 and small windows, scales down cleanly) and the Karakter
 * tab's data. Run: {@code runGameTestServer -Pgt=VadsrijkMenu}.
 */
public class VadsrijkMenuGameTests {
    private static final String BATCH = "vadsrijk_menu";

    @GuhTest(template = "empty", batch = BATCH)
    public static void tabRegister(GameTestHelper helper) {
        String a = "vr_test_a", b = "vr_test_b", sub = "vr_test_sub";
        try {
            MenuIndeling.zet(new MenuIndeling.Tab(b, () -> ItemStack.EMPTY, () -> Component.literal("B"), () -> {
            }, 9002, null));
            MenuIndeling.zet(new MenuIndeling.Tab(a, () -> ItemStack.EMPTY, () -> Component.literal("A"), () -> {
            }, 9001, null));
            MenuIndeling.zet(new MenuIndeling.Tab(sub, () -> ItemStack.EMPTY, () -> Component.literal("S"), () -> {
            }, 10, a));
            List<String> hoofd = MenuIndeling.hoofdtabs().stream().map(MenuIndeling.Tab::id).toList();
            helper.assertTrue(hoofd.indexOf(a) >= 0 && hoofd.indexOf(a) < hoofd.indexOf(b), "main tabs sorted by order: " + hoofd);
            helper.assertTrue(!hoofd.contains(sub), "a sub-tab is not a main tab");
            List<String> subs = MenuIndeling.subtabs(a).stream().map(MenuIndeling.Tab::id).toList();
            helper.assertTrue(subs.equals(List.of(a, sub)), "sub-tabs = the main tab first, then its subs: " + subs);
            helper.assertTrue(MenuIndeling.subtabs(b).isEmpty(), "no sub-tabs = empty");
            helper.assertTrue(MenuIndeling.hoofdVan(sub).equals(a) && MenuIndeling.hoofdVan(a).equals(a) && MenuIndeling.hoofdVan("onbekend").equals("onbekend"),
                    "hoofdVan");
            // the old CollectieTabs.registreer must never replace a registered tab (MenuTabs has the icons/keys)
            MenuIndeling.alsNieuw(a, 1, () -> Component.literal("oud"), () -> {
            });
            helper.assertTrue(MenuIndeling.vind(a).volgorde() == 9001 && MenuIndeling.vind(a).naam().get().getString().equals("A"), "shim keeps the real tab");
            MenuIndeling.alsNieuw("vr_test_nieuw", 9003, () -> Component.literal("N"), () -> {
            });
            helper.assertTrue(MenuIndeling.vind("vr_test_nieuw") != null, "shim adds an unknown id");
            // same id again = replaced, not doubled
            MenuIndeling.zet(new MenuIndeling.Tab(b, () -> ItemStack.EMPTY, () -> Component.literal("B2"), () -> {
            }, 9004, null));
            helper.assertTrue(MenuIndeling.alle().stream().filter(t -> t.id().equals(b)).count() == 1, "one tab per id");
        } finally {
            for (String id : List.of(a, b, sub, "vr_test_nieuw")) {
                MenuIndeling.weg(id);
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kaderPast(GameTestHelper helper) {
        // gui-scaled sizes: 1080p at GUI 2/3/4 (also windowed), 1366x768 at 3/4, 720p at 2/3/4, tiny windows
        int[][] maten = {{960, 540}, {960, 505}, {640, 360}, {640, 336}, {480, 270}, {480, 252}, {455, 256}, {341, 192}, {640, 360}, {426, 240},
                {320, 180}, {200, 150}, {1280, 720}, {2560, 1440}};
        MenuIndeling.Kader eerste = null;
        for (int[] m : maten) {
            for (int[] nodig : new int[][] {{0, 0}, {400, 399}}) {   // (a normal tab, and a very tall skill tree)
                MenuIndeling.Kader k = MenuIndeling.kader(m[0], m[1], nodig[0], nodig[1]);
                String w = m[0] + "x" + m[1] + " " + k;
                helper.assertTrue(k.schaal() > 0f && k.schaal() <= MenuIndeling.MAX_SCHAAL, "scale in (0,2]: " + w);
                helper.assertTrue(Math.abs(k.lw() * k.schaal() - m[0]) <= 2 && Math.abs(k.lh() * k.schaal() - m[1]) <= 2, "logical size = real / scale: " + w);
                helper.assertTrue(k.left() >= 0 && k.left() + k.w() <= k.lw(), "frame inside the screen (x): " + w);
                helper.assertTrue(k.top() - MenuIndeling.TABS_H >= 0 && k.top() + k.h() <= k.lh(), "frame + tab row inside the screen (y): " + w);
                helper.assertTrue(k.w() >= MenuIndeling.MIN_W && k.h() >= Math.max(MenuIndeling.MIN_H, nodig[1]), "never smaller than a panel needs: " + w);
                helper.assertTrue(k.w() <= Math.max(MenuIndeling.MAX_W, nodig[0]) && k.h() <= Math.max(MenuIndeling.MAX_H, nodig[1]), "never larger than the max: " + w);
            }
            MenuIndeling.Kader k = MenuIndeling.kader(m[0], m[1]);
            if (m[0] == 960 && m[1] == 540) {
                eerste = k;
                helper.assertTrue(k.schaal() == 1f && k.w() == MenuIndeling.MAX_W && k.h() == MenuIndeling.MAX_H, "GUI 2 at 1080p (no GUI scale given): full size, no scaling");
            }
        }
        // every tab gets the same frame on the same screen (the frame does not depend on the tab)
        helper.assertTrue(eerste != null && eerste.equals(MenuIndeling.kader(960, 540)), "same frame for every tab");
        helper.assertTrue(MenuIndeling.kader(640, 360).schaal() == 1f, "GUI 3 at 1080p: no scaling");
        helper.assertTrue(MenuIndeling.kader(2560, 1440).schaal() == MenuIndeling.MAX_SCHAAL, "GUI 1 at 1440p: scaled up to the max");
        // 1.10.24: the scale-up keeps the real pixel scale whole (crisp font): GUI 2 at 1080p -> x1.5 (= 3 real px), GUI 3 -> x1, GUI 4 -> x1
        MenuIndeling.Kader g2 = MenuIndeling.kader(960, 540, 0, 0, true, 2);
        helper.assertTrue(g2.schaal() == 1.5f && g2.w() >= MenuIndeling.RUIM_W && g2.h() >= MenuIndeling.RUIM_H, "GUI 2 at 1080p: x1.5, still roomy: " + g2);
        // 1.10.27 ("gebruikt net wat te veel van het scherm"): about 10 % smaller than 1.10.25's 560 x 324 there, and never more than
        // the share of the window (frame of the width, tab row + frame of the height) at 1080p GUI 2 and 3
        helper.assertTrue(g2.w() >= 495 && g2.w() <= 515 && g2.h() >= 284 && g2.h() <= 296, "GUI 2 at 1080p: about 10 % smaller than 560 x 324: " + g2);
        for (int gs = 2; gs <= 3; gs++) {
            MenuIndeling.Kader k = MenuIndeling.kader(1920 / gs, 1080 / gs, 0, 0, true, gs);
            helper.assertTrue(k.w() <= k.lw() * MenuIndeling.AANDEEL_W + 0.01f && k.h() + MenuIndeling.TABS_H <= k.lh() * MenuIndeling.AANDEEL_H + 0.01f,
                    "the frame stays within its share of the window at GUI " + gs + ": " + k);
        }
        helper.assertTrue(MenuIndeling.kader(640, 360, 0, 0, true, 3).schaal() == 1f && MenuIndeling.kader(480, 270, 0, 0, true, 4).schaal() <= 1f, "GUI 3/4 at 1080p: no scale-up");
        helper.assertTrue(MenuIndeling.kader(960, 540, 0, 0, false, 2).schaal() == 1f, "a container screen is never scaled up");
        for (int gs = 1; gs <= 4; gs++) {
            float sc = MenuIndeling.kader(1920 / gs, 1080 / gs, 0, 0, true, gs).schaal();
            helper.assertTrue(sc < 1f || Math.abs(sc * gs - Math.round(sc * gs)) < 1e-4, "a scale-UP keeps a whole real pixel scale at GUI " + gs + ": " + sc);
        }
        helper.succeed();
    }

    /** 1.10.27: loose screens (smid, winkel, ...): inside the window, whole real pixels when scaled up, a smaller share than before. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void losSchermPast(GameTestHelper helper) {
        int[][] schermen = {{300, 228, 300, 228}, {330, 244, 330, 244}, {360, 250, 360, 250}, {380, 256, 380, 256}, {460, 250, 540, 320}, {460, 260, 560, 330}};
        for (int gs = 1; gs <= 4; gs++) {
            for (int[] venster : new int[][] {{1920, 1080}, {1366, 768}, {1280, 720}, {854, 480}, {2560, 1440}}) {
                int sw = venster[0] / gs, sh = venster[1] / gs;
                for (int[] m : schermen) {
                    MenuIndeling.Kader k = MenuIndeling.los(sw, sh, m[0], m[1], m[2], m[3], gs);
                    String w = sw + "x" + sh + " gui " + gs + " " + k;
                    helper.assertTrue(k.schaal() >= 0.5f && k.schaal() <= MenuIndeling.MAX_SCHAAL, "scale in [0.5, 2]: " + w);
                    helper.assertTrue(k.w() >= m[0] && k.w() <= m[2] && k.h() >= m[1] && k.h() <= m[3], "size between min and max: " + w);
                    if (k.schaal() >= 1f) {
                        helper.assertTrue(k.left() >= 0 && k.left() + k.w() <= k.lw() && k.top() >= 0 && k.top() + k.h() <= k.lh(), "panel inside the window: " + w);
                        helper.assertTrue(Math.abs(k.schaal() * gs - Math.round(k.schaal() * gs)) < 1e-4, "whole real pixel scale: " + w);
                    }
                    if (k.schaal() > 1f) {
                        helper.assertTrue(k.w() <= k.lw() * MenuIndeling.LOS_MAX + 0.01f && k.h() <= k.lh() * MenuIndeling.LOS_MAX + 0.01f,
                                "a scaled-up loose screen stays within its share: " + w);
                    }
                }
            }
        }
        // the smid at 1080p GUI 2: still x1.5, grown to its share (1.10.25: 540 x 320 = 84 % x 89 % of the window)
        MenuIndeling.Kader smid = MenuIndeling.los(960, 540, 460, 250, 540, 320, 2);
        helper.assertTrue(smid.schaal() == 1.5f && smid.w() == 512 && smid.h() == 295, "smid at 1080p GUI 2: " + smid);
        // a shop (300 x 228) keeps x2 there, a 250-high screen steps down to x1.5 (it took 93 % of the height)
        helper.assertTrue(MenuIndeling.los(960, 540, 300, 228, 300, 228, 2).schaal() == 2f && MenuIndeling.los(960, 540, 360, 250, 360, 250, 2).schaal() == 1.5f,
                "fixed loose screens at 1080p GUI 2");
        helper.succeed();
    }

    /** 1.10.27: the page maths of the Huisdieren grid (4 x 4 = 16 per page; 28 pets = 2 pages). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void paginas(GameTestHelper helper) {
        helper.assertTrue(Paginas.aantal(0, 16) == 1 && Paginas.aantal(1, 16) == 1 && Paginas.aantal(16, 16) == 1 && Paginas.aantal(17, 16) == 2
                && Paginas.aantal(28, 16) == 2 && Paginas.aantal(32, 16) == 2 && Paginas.aantal(33, 16) == 3, "page count");
        helper.assertTrue(Paginas.van(0, 16) == 0 && Paginas.van(15, 16) == 0 && Paginas.van(16, 16) == 1 && Paginas.van(27, 16) == 1 && Paginas.van(-1, 16) == 0,
                "the page of an index (the chosen pet opens on its own page)");
        helper.assertTrue(Paginas.klem(-1, 28, 16) == 0 && Paginas.klem(1, 28, 16) == 1 && Paginas.klem(2, 28, 16) == 1 && Paginas.klem(5, 0, 16) == 0,
                "a step past the first / last page stays there");
        helper.assertTrue(Paginas.eerste(0, 16) == 0 && Paginas.eerste(1, 16) == 16 && Paginas.eind(0, 28, 16) == 16 && Paginas.eind(1, 28, 16) == 28
                && Paginas.eind(0, 5, 16) == 5, "first and end index of a page");
        // every index is on exactly one page, and no page holds more than fits
        for (int n : new int[] {1, 15, 16, 17, 28, 40}) {
            int gezien = 0;
            for (int pg = 0; pg < Paginas.aantal(n, 16); pg++) {
                int van = Paginas.eerste(pg, 16), tot = Paginas.eind(pg, n, 16);
                helper.assertTrue(tot > van && tot - van <= 16, "page " + pg + " of " + n + " holds 1..16");
                for (int i = van; i < tot; i++) {
                    helper.assertTrue(Paginas.van(i, 16) == pg, "index " + i + " on page " + pg);
                    gezien++;
                }
            }
            helper.assertTrue(gezien == n, "all " + n + " shown once");
        }
        helper.assertTrue(Paginas.aantal(5, 0) == 5 && Paginas.van(3, 0) == 3, "a zero page size counts as 1 (no division by zero)");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void karakterData(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.zetLevel(p, 14);
        CompoundTag t = MenuServer.karakter(p);
        helper.assertTrue(t.getIntOr("level", 0) >= 14, "level");
        helper.assertTrue(!t.getStringOr("klasse", "").isEmpty() && !t.getStringOr("naam", "").isEmpty() && !t.getStringOr("modus", "").isEmpty(), "who: " + t);
        helper.assertTrue(t.getListOrEmpty("klassen").size() >= 3, "all three classes");
        helper.assertTrue(t.getListOrEmpty("stats").size() >= 5, "the main stats are always there");
        helper.assertTrue(t.getLongOr("xpNodig", -1) >= 0 && t.getLongOr("xp", -1) >= 0, "xp to the next level");
        helper.assertTrue(MenuServer.gear(p).size() == MenuServer.GEAR.length && MenuServer.GEAR.length == 12, "12 gear slots incl. the virtual ones");
        CompoundTag r = MenuServer.reiskaart(p);
        helper.assertTrue(!r.getListOrEmpty("plekken").isEmpty() && MenuServer.reiskaartVan(r).plekken().size() == r.getListOrEmpty("plekken").size(),
                "the travel map round-trips");
        helper.succeed();
    }
}
