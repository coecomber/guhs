package nl.juiced.guhs.feature.erfenis.client;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhKeys;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads;
import nl.juiced.guhs.feature.erfenis.Plot;
import org.lwjgl.glfw.GLFW;

/**
 * De Erfenis, the client: the overview key (O by default: B is taken by the Snuffeleiland bark and JourneyMap's
 * waypoint key), the client's copy of the base it looks at, and the hooks of the overview camera, world drawing and screen.
 * The key on a plot asks the server for the overview; outside the dimension the server answers with "Naar je erfenis?".
 */
public final class ErfenisClient {
    public static final KeyMapping TOETS = new KeyMapping("key.guhs.erfenis", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, GuhKeys.CATEGORY);

    /** The base on screen (null: none yet), owned by this player?, the server clock minus ours, dev "snel". */
    @Nullable
    static Base base;
    static boolean eigen, snel;
    /** The Guhpension window's lists (from the server), or null. */
    @Nullable
    static net.minecraft.nbt.CompoundTag pension;
    static long klokVerschil;

    private ErfenisClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(TOETS));
        ErfenisPayloads.syncOntvanger = ErfenisClient::opSync;
        ErfenisPayloads.vraagOntvanger = v -> Minecraft.getInstance().setScreen(new VraagScherm());
        ErfenisPayloads.meldingOntvanger = ErfenisClient::opMelding;
        ErfenisPayloads.pensionOntvanger = d -> {
            pension = d.data();
            if (Minecraft.getInstance().screen instanceof OverzichtScherm s && !(s.venster() instanceof Vensters.Pension)) {
                s.open(new Vensters.Pension());
            }
        };
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            while (TOETS.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ClientPacketDistributor.sendToServer(ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.OPEN));
                }
            }
            if (Overzicht.actief() && !(mc.screen instanceof OverzichtScherm) && mc.screen == null) {
                Overzicht.stop();   // (the screen went away some other way)
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            Minecraft mc = Minecraft.getInstance();
            Overzicht.frame(mc.screen instanceof OverzichtScherm s && s.toetsenVrij());
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> Overzicht.hoeken(event));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> Overzicht.fov(event));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterTranslucentBlocks event) -> OverzichtWereld.teken(event));
        // The whole HUD goes while the overview is open. hideGui (see Overzicht.start) only hides the vanilla layers: other mods'
        // HUDs (the JourneyMap minimap in the dev client) draw from RenderGuiEvent.Pre, and cancelling that skips them all.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (RenderGuiEvent.Pre event) -> {
            if (Overzicht.actief()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderHandEvent event) -> {
            if (Overzicht.actief()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            OverzichtScherm.sluitAlles();
            base = null;
        });
    }

    private static void opSync(ErfenisPayloads.Sync s) {
        Base b = s.base() == null ? null : Base.load(s.base());
        if (b == null) {
            return;
        }
        base = b;
        eigen = s.eigen();
        snel = s.snel();
        klokVerschil = s.serverNu() - System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof OverzichtScherm scherm) {
            scherm.vernieuwd();
        } else if (s.open() && mc.player != null) {
            Overzicht.start(Plot.oorsprong(b.cx, b.cz));
            mc.setScreen(new OverzichtScherm());
        }
    }

    private static void opMelding(ErfenisPayloads.Melding m) {
        if ("erfenis.dev.ui".equals(m.key())) {
            if (Minecraft.getInstance().screen instanceof OverzichtScherm scherm && !m.args().isEmpty()) {
                scherm.devUi(m.args().get(0), m.args().size() > 1 ? m.args().get(1) : "");
            }
            return;
        }
        Component t = tekst(m.key(), m.args());
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof OverzichtScherm scherm) {
            scherm.toast(t, m.goed());
        } else if (mc.player != null) {
            mc.player.sendOverlayMessage(t);
        }
    }

    /** A lang key with arguments that are numbers/text or other lang keys. */
    static Component tekst(String key, List<String> args) {
        Object[] a = new Object[args.size()];
        for (int i = 0; i < a.length; i++) {
            String s = args.get(i);
            a[i] = I18n.exists(s) ? Component.translatable(s) : s;
        }
        return Component.translatable(key, a);
    }

    /** The server's clock now (millis). */
    static long nu() {
        return System.currentTimeMillis() + klokVerschil;
    }

    static BlockPos oorsprong() {
        return base == null ? BlockPos.ZERO : Plot.oorsprong(base.cx, base.cz);
    }

    static void stuur(ErfenisPayloads.Actie a) {
        ClientPacketDistributor.sendToServer(a);
    }
}
