package nl.juiced.guhs.feature.vadsrijk.toetsen.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.toetsen.ToetsenIndeling;

/**
 * U-TOETSEN (Guhs 1.10.0, round U): in the Vadsrijk the pause menu (Esc) shows every Vadsrijk key on the RIGHT, in a pink guh
 * panel: grouped (Gevecht / Menu's / Rijden / Sociaal / Overig), key + short Dutch description, grey when unbound, red with "!"
 * on a conflict. Read live from the registered KeyMappings ({@link VadsrijkToetsen}), so rebinding shows at once and keys of
 * later branches appear on their own. Click a line = the vanilla key screen. It never overlaps the vanilla buttons: the free
 * room right of them is measured from the screen's widgets; the panel shrinks (scale 1 / 0.75 / 0.5, never below one real pixel
 * per text pixel) or goes into 2-3 columns; when even that does not fit, a "Toetsen ▸" button opens {@link ToetsenScherm}.
 * Independent of u-hud (its button sits top-left) and u-tabs. Also {@code /toetsen} opens the screen anywhere.
 */
public final class ToetsenClient {
    private static final int MARGE = 6;
    private static final ToetsenPaneel PANEEL = new ToetsenPaneel();
    private static Button knop;   // the "Toetsen ▸" fallback button of the current pause screen (null = panel mode)

    private ToetsenClient() {
    }

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> opbouw(e));
        // Foreground: inside the screen's own render, before the deferred tooltip (so our hover tooltip shows)
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Foreground e) -> {
            if (!actief(e.getScreen()) || knop != null) {
                return;
            }
            Screen s = e.getScreen();
            int[] vak = vrijeRuimte(s);
            List<VadsrijkToetsen.Toets> toetsen = VadsrijkToetsen.alle();
            ToetsenIndeling.Uitkomst u = toetsen.isEmpty() ? null : PANEEL.plan(toetsen, vak[2], vak[3], minSchaal());
            if (u == null) {
                PANEEL.vergeet();
                return;
            }
            int x = vak[0] + vak[2] - u.schermBreedte();
            int y = vak[1] + Math.max(0, (vak[3] - u.schermHoogte()) / 2);
            PANEEL.teken(e.getGuiGraphics(), toetsen, u, x, y, e.getMouseX(), e.getMouseY());
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (!actief(e.getScreen()) || knop != null || e.getButton() != 0) {
                return;
            }
            if (PANEEL.onder(e.getMouseX(), e.getMouseY()) != null) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new KeyBindsScreen(e.getScreen(), mc.options));
                e.setCanceled(true);
            } else if (PANEEL.binnen(e.getMouseX(), e.getMouseY())) {
                e.setCanceled(true);   // a click on the panel itself does nothing (and never reaches something below)
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(Commands.literal("toetsen").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            mc.schedule(() -> mc.setScreen(new ToetsenScherm(null)));   // (after the chat screen closes)
            return 1;
        })));
    }

    /** Only the real pause menu (not the F3+Esc "paused" variant), only in the Vadsrijk. */
    private static boolean actief(Screen s) {
        Minecraft mc = Minecraft.getInstance();
        return s instanceof PauseScreen p && p.showsPauseMenu() && mc.player != null && Vadsrijk.in(mc.player);
    }

    /** At GUI scale 1 the text stays at full size (half size would be unreadable); at scale 2+ down to half. */
    private static float minSchaal() {
        int gs = Minecraft.getInstance().getWindow().getGuiScale();
        return gs <= 1 ? 1f : 0.5f;
    }

    private static void opbouw(ScreenEvent.Init.Post e) {
        knop = null;
        PANEEL.vergeet();
        if (!actief(e.getScreen())) {
            return;
        }
        Screen s = e.getScreen();
        int[] vak = vrijeRuimte(s);
        List<VadsrijkToetsen.Toets> toetsen = VadsrijkToetsen.alle();
        if (toetsen.isEmpty() || PANEEL.plan(toetsen, vak[2], vak[3], minSchaal()) != null) {
            return;   // the panel fits: drawn every frame in Render.Foreground
        }
        // no room: a small button in the free strip (or, if even that is too narrow, top-right corner)
        int kb = 70;
        int kx = vak[2] >= kb ? vak[0] + vak[2] - kb : s.width - kb - 4;
        int ky = vak[2] >= kb ? vak[1] + vak[3] / 2 - 10 : 4;
        knop = Button.builder(Component.translatable("vadsrijk.toetsen.knop"), b -> Minecraft.getInstance().setScreen(new ToetsenScherm(s)))
                .bounds(kx, ky, kb, 20).build();
        e.addListener(knop);
    }

    /**
     * The free box right of the pause buttons: {x, y, w, h}. The right edge of the vanilla button column = the furthest right edge
     * of every widget that starts left of the screen centre + 110 (the centred 204-wide grid, title, other mods' centred buttons).
     */
    private static int[] vrijeRuimte(Screen s) {
        int grens = s.width / 2 + 102;
        for (GuiEventListener l : s.children()) {
            if (l instanceof AbstractWidget w && w != knop && w.visible && w.getX() < s.width / 2 + 110 && w.getX() + w.getWidth() > s.width / 2) {
                grens = Math.max(grens, w.getX() + w.getWidth());
            }
        }
        int x = grens + MARGE + 2;
        int w = s.width - MARGE - x;
        return new int[] {x, MARGE, Math.max(0, w), Math.max(0, s.height - 2 * MARGE)};
    }
}
