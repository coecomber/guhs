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
    /** Your switch channels (bits), the big vadsmunten of this level you have (bits), the Guhshi that carries you (entity id, 0: none). */
    static int kanalen, vads, guhshi;
    /** Ticks left of Guhshi's tongue and how far it reaches (blocks, negative: back along the lane). */
    static int tong;
    static float tongVer;
    /** Set by the avatar render-state modifier: this player is drawn sitting on a level's Guhshi. */
    private static final net.minecraft.util.context.ContextKey<Boolean> OP_GUHSHI = new net.minecraft.util.context.ContextKey<>(Guhs.id("guhrio_op_guhshi"));
    /** Ticks left of: the black flash after coming back to your flag, the "done" banner. */
    static int flits, klaar;
    static int klaarTijd;

    private GuhrioClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            DoosModel.vergeet();
            event.registerEntityRenderer(GuhrioFeature.GUHMBA.get(), WezenRenderers.Guhmba::new);
            event.registerEntityRenderer(GuhrioFeature.SCHILD_MIKA.get(), WezenRenderers.SchildMika::new);
            event.registerEntityRenderer(GuhrioFeature.PLOF_MIKA.get(), WezenRenderers.PlofMika::new);
            event.registerEntityRenderer(GuhrioFeature.HAPBLOEM.get(), WezenRenderers.Hapbloem::new);
            event.registerEntityRenderer(GuhrioFeature.GUHRIO_GRILLSPIES.get(), WezenRenderers.Grillspies::new);
            event.registerEntityRenderer(GuhrioFeature.GUHRIO_KNABBEL.get(), WezenRenderers.Knabbel::new);
            event.registerEntityRenderer(GuhrioFeature.PLATFORM.get(), c -> new WezenRenderers.Dek<>(c, "guhrio_platform"));
            event.registerEntityRenderer(GuhrioFeature.VALBLOK.get(), c -> new WezenRenderers.Dek<>(c, "guhrio_valblok"));
            event.registerBlockEntityRenderer(GuhrioFeature.STUK_BE.get(), StukRenderer::new);
        });
        // a player on a level's Guhshi is drawn sitting, lifted onto his back
        modBus.addListener((net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) -> event.registerAvatarEntityModifier(
                new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
                    @Override
                    public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(
                            T avatar, net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                        boolean op = opGuhshi(avatar);
                        state.setRenderData(OP_GUHSHI, op ? Boolean.TRUE : null);
                        if (op) {
                            state.isPassenger = true;
                        }
                    }
                }));
        NeoForge.EVENT_BUS.register(Zit.class);
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

            @Override
            public int kanalen(Player player) {
                return player == Minecraft.getInstance().player && GuhrioClient.speelt() ? kanalen : 0;
            }
        };
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> {
            // Q in a level never drops what you hold: it is the way out (twice)
            Minecraft mc = Minecraft.getInstance();
            if (speelt() && mc.player != null) {
                boolean q = false;
                while (mc.options.keyDrop.consumeClick()) {
                    q = true;
                }
                if (q && mc.screen == null) {
                    BaanBesturing.stopToets();
                }
            }
        });
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
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> {
            BaanBesturing.kijkVast();
            houGuhshiBij();
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> BaanCamera.fov(event));
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event) -> {
            if (speelt() && GuhrioHud.verborgen(event.getName())) {
                event.setCanceled(true);
            }
        });
        // in a level your hands do nothing: no hitting, breaking, placing or using (you look along the lane, not at things)
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (speelt()) {
                event.setSwingHand(false);
                event.setCanceled(true);
                if (event.isAttack() || event.isUseItem()) {
                    BaanBesturing.actieToets();                // (a mouse button: a knabbel, or Guhshi's tongue)
                }
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

    /** Lifts a player who sits on a level's Guhshi onto his back (the pose is put back after the player is drawn). */
    public static final class Zit {
        private static final net.minecraft.util.context.ContextKey<Boolean> GETILD = new net.minecraft.util.context.ContextKey<>(Guhs.id("guhrio_getild"));

        private Zit() {
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void voor(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?, ?> event) {
            if (Boolean.TRUE.equals(event.getRenderState().getRenderData(OP_GUHSHI))) {
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, ZIT_HOOGTE, 0);
                event.getRenderState().setRenderData(GETILD, Boolean.TRUE);
            }
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void na(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?, ?> event) {
            if (event.getRenderState().getRenderData(GETILD) != null) {
                event.getRenderState().setRenderData(GETILD, null);
                event.getRenderState().setRenderData(OP_GUHSHI, null);
                event.getPoseStack().popPose();
            }
        }
    }

    /** Is switch channel k on for you? */
    public static boolean kanaal(int k) {
        return banen != null && (kanalen >> k & 1) != 0;
    }

    /** How far above the ground a player sits on a level's Guhshi. */
    public static final double ZIT_HOOGTE = 0.62;

    /** Is this player carried by a level's Guhshi (a guh of that kind that stands exactly where the player is)? */
    static boolean opGuhshi(net.minecraft.world.entity.Entity speler) {
        if (speler == Minecraft.getInstance().player) {
            return banen != null && guhshi != 0;
        }
        for (nl.juiced.guhs.entity.GuhEntity guh : speler.level().getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, speler.getBoundingBox().inflate(0.4),
                g -> g.getVariant() == nl.juiced.guhs.entity.GuhVariant.GUHSHI && g.isNoAi())) {
            if (guh.distanceToSqr(speler) < 0.5) {
                return true;
            }
        }
        return false;
    }

    /** Every frame: the Guhshi that carries YOU is exactly under you (the server's copy of him lags a few ticks behind). */
    private static void houGuhshiBij() {
        Minecraft mc = Minecraft.getInstance();
        if (banen == null || guhshi <= 0 || mc.player == null || mc.level == null) {
            return;
        }
        net.minecraft.world.entity.Entity guh = mc.level.getEntity(guhshi);
        if (guh == null) {
            return;
        }
        LocalPlayer p = mc.player;
        guh.setPos(p.getX(), p.getY(), p.getZ());
        guh.xo = guh.xOld = p.xo;
        guh.yo = guh.yOld = p.yo;
        guh.zo = guh.zOld = p.zo;
        guh.setYRot(p.getYRot());
        guh.yRotO = p.yRotO;
        guh.setYBodyRot(p.getYRot());
        guh.setYHeadRot(p.getYRot());
    }

    /** (dev: one line of what this game knows of the level) */
    public static String devInfo() {
        Baan b = baan();
        return b == null ? "not in a level" : "level " + wereld + " lane " + baan + " (" + b.id + ") coins " + munten + " power " + kracht
                + " time " + tijd + " pieces changed " + STAAT.size() + " camera type " + Minecraft.getInstance().options.getCameraType();
    }

    /** (dev: how far along the lane you are, or NaN) */
    public static double devS() {
        Baan b = baan();
        LocalPlayer p = Minecraft.getInstance().player;
        return b == null || p == null ? Double.NaN : b.plek(p.getX(), p.getZ()).s();
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
        kanalen = vads = guhshi = tong = 0;
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
        kanalen = vads = guhshi = tong = 0;
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
        kanalen = payload.kanalen();
        vads = payload.vads();
        guhshi = payload.guhshi();
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
        if (state.getBlock() instanceof nl.juiced.guhs.feature.guhrio.GuhrioStukken.VadsmuntBlok) {
            mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.8f);
        } else if (state.getBlock() instanceof GuhrioBlocks.MuntBlok) {
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
            case GuhrioPayloads.Moment.DEUR -> {
                zetBaan(m.getal());
                flits = 8;
                p.setDeltaMovement(0, 0, 0);
                BaanBesturing.terug(p);
                BaanCamera.begin(p);
            }
            case GuhrioPayloads.Moment.KRIMP -> flits = 4;
            case GuhrioPayloads.Moment.VADSMUNT -> mc.gui.setOverlayMessage(
                    net.minecraft.network.chat.Component.translatable("gui.guhs.guhrio.vadsmunt", m.getal()), false);
            case GuhrioPayloads.Moment.TONG -> {
                tong = 6;
                tongVer = m.getal() / 10f;
                tongDeeltjes(p);
            }
            default -> {
            }
        }
    }

    /** Guhshi's tongue: a quick pink line along the lane, the way it shot. */
    private static void tongDeeltjes(LocalPlayer p) {
        Baan b = baan();
        if (b == null) {
            return;
        }
        double s = b.plek(p.getX(), p.getZ()).s();
        int n = Math.max(2, (int) Math.abs(tongVer * 3));
        for (int i = 1; i <= n; i++) {
            net.minecraft.world.phys.Vec3 punt = b.punt(s + tongVer * i / n, p.getY() + 0.75);
            p.level().addParticle(new net.minecraft.core.particles.DustParticleOptions(0xFF7090, 1.1f), punt.x, punt.y, punt.z, 0, 0, 0);
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
        if (tong > 0) {
            tong--;
        }
        if (nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(mc.player)) {
            return;                                            // (a cutscene has the camera; the clock stands still)
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
