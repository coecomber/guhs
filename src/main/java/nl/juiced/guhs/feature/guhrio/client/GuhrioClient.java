package nl.juiced.guhs.feature.guhrio.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioPayloads;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;

/**
 * Super Guhrio in the player's own game. While the server has you in a level ({@link GuhrioPayloads.Start} until
 * {@link GuhrioPayloads.Stop}): the side view ({@link BaanCamera}), your keys and the lane's line ({@link BaanBesturing}),
 * the panel ({@link GuhrioHud}), and what every piece is for you ({@link #staat}: a coin you took is gone for you only).
 * Outside a level nothing here does anything.
 * <p>
 * Letting go is the one thing that must never fail: {@link #uit} puts the camera and the keys back, and it is called when
 * the server says so, when you log out or come back to life, and when the server has not been heard for three seconds
 * (the panel message doubles as a heartbeat).
 */
public final class GuhrioClient {
    /** Ticks without a word from the server before the game lets go of the lane by itself. */
    public static final int STIL_TICKS = 60;

    @Nullable
    private static List<Baan> banen;
    private static int baan;
    private static String wereld = "";
    private static final Map<BlockPos, Integer> STAAT = new HashMap<>();
    @Nullable
    private static CameraType vorigeCamera;
    private static int stil;
    private static long gevraagd;
    /** The panel. */
    static int munten, totaal, tijd, kracht;
    /** Ticks left of: the black flash after coming back to your flag, the "done" banner. */
    static int flits, klaar;
    static int klaarTijd;

    private GuhrioClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(GuhrioFeature.GUHMBA.get(), GuhmbaRenderer::new);
            event.registerBlockEntityRenderer(GuhrioFeature.STUK_BE.get(), StukRenderer::new);
        });
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("guhrio_hud"), GuhrioHud::teken));
        GuhrioSpel.client = new GuhrioSpel.ClientKant() {
            @Override
            public boolean speelt(Player player) {
                return player == Minecraft.getInstance().player && GuhrioClient.speelt();
            }

            @Override
            public int staat(Player player, BlockPos pos) {
                return player == Minecraft.getInstance().player ? GuhrioClient.staat(pos) : 0;
            }

            @Override
            public boolean inPijp(Player player) {
                return player == Minecraft.getInstance().player && BaanBesturing.inPijp();
            }
        };
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Pre event) -> {
            if (event.getEntity() instanceof LocalPlayer p && p == Minecraft.getInstance().player) {
                BaanBesturing.voor(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof LocalPlayer p && p == Minecraft.getInstance().player) {
                BaanBesturing.na(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> BaanBesturing.kijkVast());
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> BaanCamera.fov(event));
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event) -> {
            if (speelt() && GuhrioHud.VERBORGEN.contains(event.getName())) {
                event.setCanceled(true);
            }
        });
        // in a level your hands do nothing: no hitting, breaking, placing or using (you look along the lane, not at things)
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (speelt()) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ExtractBlockOutlineRenderStateEvent event) -> {
            if (speelt()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> uit());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.Clone event) -> uit());
    }

    // =====================================================================================================================
    // asking
    // =====================================================================================================================

    /** Are you in a level? */
    public static boolean speelt() {
        return banen != null;
    }

    /** The lane you are on, or null. */
    @Nullable
    public static Baan baan() {
        return banen == null ? null : banen.get(Math.min(baan, banen.size() - 1));
    }

    public static String wereld() {
        return wereld;
    }

    /** What a piece is for you (0 as built). */
    public static int staat(BlockPos pos) {
        return banen == null ? 0 : STAAT.getOrDefault(pos, 0);
    }

    // =====================================================================================================================
    // from the server
    // =====================================================================================================================

    public static void start(CompoundTag data) {
        List<Baan> nieuw = GuhrioLevel.banenUit(data);
        if (nieuw.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        banen = nieuw;
        baan = Math.max(0, Math.min(data.getIntOr("Baan", 0), nieuw.size() - 1));
        wereld = data.getStringOr("Wereld", "");
        STAAT.clear();
        long[] plekken = data.getLongArray("Staat").orElse(new long[0]);
        int[] standen = data.getIntArray("Standen").orElse(new int[0]);
        for (int i = 0; i < plekken.length && i < standen.length; i++) {
            STAAT.put(BlockPos.of(plekken[i]), standen[i]);
        }
        munten = tijd = kracht = 0;
        flits = klaar = 0;
        stil = 0;
        if (vorigeCamera == null) {
            vorigeCamera = mc.options.getCameraType();
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        BaanBesturing.begin(mc.player);
        BaanCamera.begin(mc.player);
    }

    public static void stop(int reden) {
        uit();
    }

    /** Out of the level: the camera and the keys are yours again. Safe to call any time. */
    public static void uit() {
        Minecraft mc = Minecraft.getInstance();
        boolean bezig = banen != null;
        banen = null;
        STAAT.clear();
        flits = klaar = 0;
        BaanBesturing.einde();
        BaanCamera.einde();
        if (vorigeCamera != null) {
            mc.options.setCameraType(vorigeCamera);
            vorigeCamera = null;
        }
        if (mc.player != null && mc.player.input instanceof BaanBesturing.BaanInput) {
            mc.player.input = new KeyboardInput(mc.options);
        }
        if (bezig && mc.player != null) {
            mc.player.setXRot(0f);
        }
    }

    public static void staat(GuhrioPayloads.Staat payload) {
        if (banen == null) {
            // the server has us in a level but this game let go (it heard nothing for a while): ask for the level again
            long nu = net.minecraft.util.Util.getMillis();
            if (nu - gevraagd > 2000) {
                gevraagd = nu;
                net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                        new GuhrioPayloads.Actie(GuhrioPayloads.Actie.WEER, BlockPos.ZERO, 0));
            }
            return;
        }
        stil = 0;
        munten = payload.munten();
        totaal = payload.totaal();
        tijd = payload.ticks();
        kracht = payload.kracht();
        if (banen != null && payload.baan() != baan && payload.baan() < banen.size() && !BaanBesturing.inPijp()) {
            zetBaan(payload.baan());
        }
    }

    /** A piece's state from the server (your own game may have guessed it already). */
    public static void stuk(BlockPos pos, int staat) {
        if (banen == null) {
            return;
        }
        int was = STAAT.getOrDefault(pos, 0);
        if (staat == 0) {
            STAAT.remove(pos);
        } else {
            STAAT.put(pos.immutable(), staat);
        }
        if (was == 0 && staat != 0) {
            pak(pos);
        }
    }

    /** Your own game saw it first: the piece changes for you now (the server will say the same). False: it already had. */
    static boolean raad(BlockPos pos, int staat) {
        if (STAAT.getOrDefault(pos, 0) == staat) {
            return false;
        }
        STAAT.put(pos.immutable(), staat);
        pak(pos);
        return true;
    }

    /** The sound of a piece that just changed for you. */
    private static void pak(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        BlockState state = mc.level.getBlockState(pos);
        if (state.getBlock() instanceof GuhrioBlocks.MuntBlok) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.45f, 1.7f);
        } else if (state.getBlock() instanceof GuhrioBlocks.VraagBlok) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }

    public static void moment(GuhrioPayloads.Moment m) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (banen == null || p == null) {
            return;
        }
        stil = 0;
        switch (m.soort()) {
            case GuhrioPayloads.Moment.TERUG -> {
                flits = 12;
                p.setDeltaMovement(0, 0, 0);
                BaanBesturing.terug(p);
                BaanCamera.begin(p);
            }
            case GuhrioPayloads.Moment.PIJP_IN -> BaanBesturing.pijpIn(p, m.pos());
            case GuhrioPayloads.Moment.PIJP_UIT -> {
                zetBaan(m.getal());
                BaanBesturing.pijpUit(p, m.pos());
                BaanCamera.begin(p);
            }
            case GuhrioPayloads.Moment.KLAAR -> {
                klaar = GuhrioSpel.KLAAR_TICKS;
                klaarTijd = m.getal();
                BaanBesturing.klaar(p, m.pos());
            }
            case GuhrioPayloads.Moment.KRIMP -> flits = 4;
            default -> {
            }
        }
    }

    private static void zetBaan(int nieuw) {
        if (banen == null) {
            return;
        }
        baan = Math.max(0, Math.min(nieuw, banen.size() - 1));
        BaanBesturing.begin(Minecraft.getInstance().player);
    }

    // =====================================================================================================================

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (banen == null) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            uit();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        if (++stil > STIL_TICKS) {
            uit();                                             // (the server forgot us, or we missed its goodbye)
            return;
        }
        if (flits > 0) {
            flits--;
        }
        if (klaar > 0) {
            klaar--;
        } else {
            tijd++;                                            // (the server's count comes by twice a second)
        }
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);   // (F5 does nothing in a level)
        }
        BaanCamera.tick(mc.player);
    }
}
