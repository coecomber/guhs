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
                helper.assertTrue(k.schaal() > 0f && k.schaal() <= 1f, "scale in (0,1]: " + w);
                helper.assertTrue(Math.abs(k.lw() * k.schaal() - m[0]) <= 2 && Math.abs(k.lh() * k.schaal() - m[1]) <= 2, "logical size = real / scale: " + w);
                helper.assertTrue(k.left() >= 0 && k.left() + k.w() <= k.lw(), "frame inside the screen (x): " + w);
                helper.assertTrue(k.top() - MenuIndeling.TABS_H >= 0 && k.top() + k.h() <= k.lh(), "frame + tab row inside the screen (y): " + w);
                helper.assertTrue(k.w() >= MenuIndeling.MIN_W && k.h() >= Math.max(MenuIndeling.MIN_H, nodig[1]), "never smaller than a panel needs: " + w);
                helper.assertTrue(k.w() <= Math.max(MenuIndeling.MAX_W, nodig[0]) && k.h() <= Math.max(MenuIndeling.MAX_H, nodig[1]), "never larger than the max: " + w);
            }
            MenuIndeling.Kader k = MenuIndeling.kader(m[0], m[1]);
            if (m[0] == 960 && m[1] == 540) {
                eerste = k;
                helper.assertTrue(k.schaal() == 1f && k.w() == MenuIndeling.MAX_W && k.h() == MenuIndeling.MAX_H, "GUI 2 at 1080p: full size, no scaling");
            }
        }
        // every tab gets the same frame on the same screen (the frame does not depend on the tab)
        helper.assertTrue(eerste != null && eerste.equals(MenuIndeling.kader(960, 540)), "same frame for every tab");
        helper.assertTrue(MenuIndeling.kader(640, 360).schaal() == 1f, "GUI 3 at 1080p: no scaling");
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
