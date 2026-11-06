package nl.juiced.guhs.feature.vadsrijk.vint.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.menu.client.MenuPaneel;
import nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu;
import nl.juiced.guhs.feature.vadsrijk.toetsen.client.VadsrijkToetsen;

/**
 * V-INT (round U integration), client glue:
 * <ul>
 * <li>Key H: u-tabs' Vadsrijk-menu ({@code key.guhs.vr_menu}) only fires IN the Vadsrijk, einde2's power key
 * ({@code key.guhs.einde2_kracht}) only OUTSIDE it. Two conflict contexts that never clash with each other (NeoForge only presses a
 * mapping whose context is active), so both keep H and the key screen shows no false conflict.</li>
 * <li>Menu tabs: Knuffelbond (u-gilden, 115), Mijn huis (u-huizen, 117), sub-tab "HUD aanpassen" under Instellingen (u-hud),
 * sub-tab "Specialisatie" under Skillboom (u-specs).</li>
 * <li>u-toetsen: the menu key in the "Menu's" group.</li>
 * </ul>
 */
public final class VIntClient {
    private static final String K = "vadsrijk.vint.";

    private VIntClient() {
    }

    /** Active in-game inside (VADSRIJK) or outside (ELDERS) the Vadsrijk; the two never conflict with each other. */
    public enum Context implements IKeyConflictContext {
        VADSRIJK, ELDERS;

        @Override
        public boolean isActive() {
            if (!KeyConflictContext.IN_GAME.isActive()) {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            boolean binnen = mc.player != null && Vadsrijk.in(mc.player);
            return this == VADSRIJK ? binnen : !binnen;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            if (other instanceof Context c) {
                return c == this;
            }
            return other == KeyConflictContext.IN_GAME || other == KeyConflictContext.UNIVERSAL;
        }
    }

    public static void init(IEventBus modBus) {
        KeyMapping menu = nl.juiced.guhs.feature.vadsrijk.menu.client.MenuClient.MENU;
        menu.setKeyConflictContext(Context.VADSRIJK);
        nl.juiced.guhs.feature.einde2.zij.client.E2ZijClient.KRACHT.setKeyConflictContext(Context.ELDERS);
        VadsrijkToetsen.registreer(menu, K + "toets.menu", VadsrijkToetsen.Groep.MENUS);

        VadsrijkMenu.scherm("bond", () -> new ItemStack(Items.PINK_BANNER), BondPaneel::new, 115);
        VadsrijkMenu.scherm("huis", () -> new ItemStack(Items.CHERRY_DOOR), HuisPaneel::new, 117);
        VadsrijkMenu.subtab("instellingen", "hud", () -> Component.translatable(K + "tab.hud"), () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new nl.juiced.guhs.feature.vadsrijk.hud.client.HudBewerker(mc.screen));
        }, 10);
        VadsrijkMenu.subtab("skillboom", "specs", () -> Component.translatable(K + "tab.specs"),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads.Actie("spec_scherm", "")), 10);
    }

    /** Runs a chat command (closes the menu first, so the chat answer is visible). */
    static void commando(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand(cmd);
        }
    }

    /** Wrapped lines of text; returns the next y. */
    static int alinea(MenuPaneel s, GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, Component c, int x, int y, int breed, int kleur) {
        for (var regel : font.split(c, breed)) {
            g.text(font, regel, x, y, kleur, false);
            y += 10;
        }
        return y + 4;
    }

    /** The Knuffelbond tab: what a bond is + buttons that open u-gilden's chat panel parts. */
    static final class BondPaneel extends MenuPaneel {
        BondPaneel() {
            super("bond", Component.translatable(K + "bond.titel"), new CompoundTag());
        }

        @Override
        protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
            int x = left + 12, y = top + 26, breed = w - 24;
            for (int i = 1; i <= 3; i++) {
                y = alinea(this, g, font, Component.translatable(K + "bond.uitleg." + i), x, y, breed, TEKST);
            }
            String[][] knoppen = {{"bond", "vadsrijk.gilden.knop.inventaris"}, {"bond leden", "vadsrijk.gilden.knop.leden"},
                    {"bond taken", "vadsrijk.gilden.knop.taken"}, {"bond perks", "vadsrijk.gilden.knop.perks"},
                    {"bond kasteel", "vadsrijk.gilden.knop.kasteel"}, {"bond thuis", "vadsrijk.gilden.knop.thuis"},
                    {"bond bord", "vadsrijk.gilden.knop.bord"}, {"bond ranglijst", "vadsrijk.gilden.knop.ranglijst"},
                    {"bond uitleg", "vadsrijk.gilden.knop.uitleg"}};
            int bw = (breed - 8) / 3, by = Math.max(y + 4, top + h - 3 * 22 - 22);
            for (int i = 0; i < knoppen.length; i++) {
                String cmd = knoppen[i][0];
                knop(g, mx, my, x + (i % 3) * (bw + 4), by + (i / 3) * 22, bw, 18, Component.translatable(knoppen[i][1]), true, () -> commando(cmd));
            }
            g.text(font, Component.translatable(K + "bond.voet"), x, top + h - 14, ZACHT, false);
        }
    }

    /** The Mijn huis tab: how the Knuffelkwartier works and where to find the Kwartiermeester. */
    static final class HuisPaneel extends MenuPaneel {
        HuisPaneel() {
            super("huis", Component.translatable(K + "huis.titel"), new CompoundTag());
        }

        @Override
        protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
            int x = left + 12, y = top + 26, breed = w - 24;
            y = alinea(this, g, font, Component.translatable(K + "huis.intro"), x, y, breed, TEKST);
            for (int i = 1; i <= 3; i++) {
                y = alinea(this, g, font, Component.translatable("vadsrijk.huizen.uitleg." + i), x, y, breed, TEKST);
            }
            alinea(this, g, font, Component.translatable(K + "huis.waar"), x, y, breed, GOUD);
        }
    }
}
