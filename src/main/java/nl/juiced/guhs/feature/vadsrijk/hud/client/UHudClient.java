package nl.juiced.guhs.feature.vadsrijk.hud.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;

/**
 * U-HUD client: the party frames ({@link PartyHud}), the emote wheel (key {@code ;}, {@link EmoteWiel}) and poses
 * ({@link EmotePose}), the HUD editor ({@link HudBewerker}, {@link HudIndeling}): pause menu button "HUD aanpassen" (in the
 * Vadsrijk), the options screen button, or the client command {@code /hudbewerker}.
 */
public final class UHudClient {
    public static final KeyMapping EMOTES = new KeyMapping("key.guhs.vr_emotes", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, KernClient.CATEGORIE);

    private UHudClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(EMOTES));
        modBus.addListener(UHudClient::renderStates);
        UHudPayloads.partyOntvanger = PartyHud::ontvang;
        UHudPayloads.emoteOntvanger = EmotePose::ontvang;
        UHudPayloads.bewerkerOntvanger = () -> Minecraft.getInstance().setScreen(new HudBewerker(null));
        delen();
        HudIndeling.init();
        RijkHud.EIGEN_PARTY = PartyHud::actief;
        RijkHud.voegToe("u_hud_party", 90, PartyHud::teken);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            PartyHud.tick();
            EmotePose.tick();
            Minecraft mc = Minecraft.getInstance();
            while (EMOTES.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    mc.setScreen(new EmoteWiel());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            PartyHud.wis();
            EmotePose.wis();
        });
        // the editor's buttons: pause menu + options (only in the Vadsrijk, where the HUD is)
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if ((e.getScreen() instanceof PauseScreen || e.getScreen() instanceof OptionsScreen) && mc.player != null && Vadsrijk.in(mc.player)) {
                var scherm = e.getScreen();
                e.addListener(Button.builder(Component.translatable("vadsrijk.uhud.bewerker.knop"), b -> mc.setScreen(new HudBewerker(scherm)))
                        .bounds(4, 4, 96, 20).build());
            }
        });
        // click-to-target: a click on a party frame while the chat is open
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (e.getScreen() instanceof ChatScreen && e.getButton() == 0 && PartyHud.klik(e.getMouseX(), e.getMouseY())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(Commands.literal("hudbewerker").executes(c -> {
            Minecraft mc = Minecraft.getInstance();
            mc.schedule(() -> mc.setScreen(new HudBewerker(null)));   // (after the chat screen closes)
            return 1;
        })));
    }

    private static void renderStates(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar instanceof Player p) {
                    EmotePose.extract(p, state);
                }
            }
        });
    }

    /** Every HUD part of the Vadsrijk that the editor can move (plek spots register themselves the first time they are used). */
    private static void delen() {
        String n = "vadsrijk.uhud.deel.";
        HudIndeling.registreer("balken", n + "balken", (w, h) -> new int[] {w / 2 - 92, h - 42, 184, 10});
        HudIndeling.registreer("xp", n + "xp", (w, h) -> new int[] {w / 2 - 91, h - 30, 182, 5});
        HudIndeling.registreer("skills", n + "skills", (w, h) -> new int[] {w / 2 + 97, h - 23, 4 * 23 + 18, 22});
        HudIndeling.registreer("info", n + "info", (w, h) -> new int[] {w / 2 - 98 - 104, h - 45, 104, 44});
        HudIndeling.registreer("baasbalk", n + "baasbalk", (w, h) -> {
            int bw = Math.min(300, w - 40);
            return new int[] {w / 2 - bw / 2 - 2, 4, bw + 4, 38};
        });
        HudIndeling.registreer("tip", n + "tip", (w, h) -> new int[] {8, 8, 190, 56});
        for (String plek : new String[] {"party", "buffs", "doelframe", "gietbalk", "baasgiet", "drankjesriem"}) {
            HudIndeling.registreer(plek, n + plek, (w, h) -> HudIndeling.plekBox(plek, w, h));
        }
        // other slices' own layers, moved as a whole
        HudIndeling.registreerLaag(Guhs.id("vr_t_items_riem"), "drankjesriem", n + "drankjesriem", (w, h) -> HudIndeling.plekBox("drankjesriem", w, h));
        HudIndeling.registreerLaag(Guhs.id("vr_npc_volg"), "questvolger", n + "questvolger",
                (w, h) -> new int[] {w - 172 - 6, 6 + RijkHud.bovenVrij(), 172, 64});
        HudIndeling.registreerLaag(Guhs.id("vr_vfx_cast"), "baasspreuk", n + "baasspreuk", (w, h) -> {
            int bw = Math.min(200, w - 60);
            return new int[] {w / 2 - bw / 2 - 2, 4, bw + 4, 26};
        });
        HudIndeling.registreerLaag(Guhs.id("vr_economie_munten"), "munten", n + "munten", (w, h) -> new int[] {w / 2 + 96, h - 43, 80, 16});
        HudIndeling.registreerLaag(Guhs.id("vr_rijdier_hud"), "rijdier", n + "rijdier", (w, h) -> new int[] {w / 2 + 98 + 4 * 23 + 21, h - 23, 22, 22});
        HudIndeling.registreerLaag(Guhs.id("vr_z4boot_wind"), "wind", n + "wind", (w, h) -> new int[] {w - 80, h / 2 - 40, 74, 44});
    }
}
