package nl.juiced.guhs.feature.vadsrijk.voltooiing.client;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.economie.client.RijdierScherm;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierScherm;

/**
 * The collection tabs (bookmarks on the right edge of the pink panel): Rijdieren, Huisdieren, Verkenningsboek. Drawn on top of the
 * mount and pet screens from outside (ScreenEvent; their files are not mine) and by {@link BoekScherm} itself.
 * <p>INTEGRATION NOTE: agent i-ui builds a shared {@code CollectieTabs.registreer} API for the same thing. When both are merged,
 * register the book there ({@code id "boek"}, icon writable_book, open = {@link VoltooiingClient#open}) and delete this class
 * (+ its {@code init} call in VoltooiingClient).
 */
public final class BoekTabs {
    public static final int B = 24, H = 22, AFSTAND = 25;

    record Tab(String labelKey, String icoon, Runnable open, Predicate<Screen> actief) {
    }

    static final List<Tab> TABS = List.of(
            new Tab("vadsrijk.voltooiing.tab.rijdieren", "minecraft:saddle", () -> ClientPacketDistributor.sendToServer(new EcoPayloads.RijdierToets(1)),
                    s -> s instanceof RijdierScherm),
            new Tab("vadsrijk.voltooiing.tab.huisdieren", "minecraft:lead", HuisdierClient::open, s -> s instanceof HuisdierScherm),
            new Tab("vadsrijk.voltooiing.tab.boek", "minecraft:writable_book", VoltooiingClient::open, s -> s instanceof BoekScherm));

    private BoekTabs() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> {
            if (vreemd(e.getScreen())) {
                int[] r = rand(e.getScreen());
                if (r != null) {
                    teken(e.getGuiGraphics(), e.getScreen(), r[0], r[1], e.getMouseX(), e.getMouseY());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (e.getMouseButtonEvent().button() != 0 || !vreemd(e.getScreen())) {
                return;
            }
            int[] r = rand(e.getScreen());
            if (r != null && klik(e.getScreen(), r[0], r[1], e.getMouseX(), e.getMouseY())) {
                e.setCanceled(true);
            }
        });
    }

    /** A collection screen that isn't the book (the book draws its own tabs). */
    private static boolean vreemd(Screen s) {
        return s instanceof RijdierScherm || s instanceof HuisdierScherm;
    }

    private static Field fLeft, fTop, fW;

    /** {x right of the panel, y top of the panel} of an EcoScherm (its fields are protected: read them once by reflection). */
    @Nullable
    private static int[] rand(Screen s) {
        if (!(s instanceof EcoScherm)) {
            return null;
        }
        try {
            if (fLeft == null) {
                fLeft = EcoScherm.class.getDeclaredField("left");
                fTop = EcoScherm.class.getDeclaredField("top");
                fW = EcoScherm.class.getDeclaredField("w");
                fLeft.setAccessible(true);
                fTop.setAccessible(true);
                fW.setAccessible(true);
            }
            return new int[] {fLeft.getInt(s) + fW.getInt(s) + 3, fTop.getInt(s)};
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /** Draws the tabs at x (just right of the panel), from y + 22 down. */
    static void teken(GuiGraphicsExtractor g, Screen s, int x, int y, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        if (x + B > s.width) {
            return;
        }
        for (int i = 0; i < TABS.size(); i++) {
            Tab t = TABS.get(i);
            int ty = y + 22 + i * AFSTAND;
            boolean actief = t.actief().test(s), hover = !actief && mx >= x && mx < x + B && my >= ty && my < ty + H;
            int bx = actief ? x - 3 : x;   // (the active tab sticks out of the panel a bit less: it is "open")
            g.fill(bx, ty, x + B, ty + H, EcoScherm.RAND);
            g.fill(bx, ty + 1, x + B - 1, ty + H - 1, actief ? EcoScherm.PANEEL : hover ? EcoScherm.HOVER : 0xFFFFC8E4);
            g.item(icoon(t.icoon()), x + 3, ty + 3);
            if (hover) {
                Component label = Component.translatable(t.labelKey());
                int lw = mc.font.width(label) + 8, lx = Math.min(x + B + 2, s.width - lw - 2);
                g.fill(lx, ty + 5, lx + lw, ty + 17, 0xF0FFFFFF);
                g.fill(lx, ty + 5, lx + lw, ty + 6, EcoScherm.RAND);
                g.text(mc.font, label, lx + 4, ty + 7, EcoScherm.TEKST, false);
            }
        }
    }

    static boolean klik(Screen s, int x, int y, double mx, double my) {
        for (int i = 0; i < TABS.size(); i++) {
            Tab t = TABS.get(i);
            int ty = y + 22 + i * AFSTAND;
            if (mx >= x && mx < x + B && my >= ty && my < ty + H) {
                if (!t.actief().test(s)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.1f));
                    t.open().run();
                }
                return true;
            }
        }
        return false;
    }

    static ItemStack icoon(String id) {
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            return new ItemStack(item);
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }
}
