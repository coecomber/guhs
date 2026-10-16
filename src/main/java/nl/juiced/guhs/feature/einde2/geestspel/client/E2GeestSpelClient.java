package nl.juiced.guhs.feature.einde2.geestspel.client;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestspel.GeestSpelPayloads;

/**
 * einde2 phase B, slice geest_spel, client side (DESIGN 5 "Geestenvorm-lijf", "Geestentij", "Maannacht", "Spiegelvijvers"):
 * <ul>
 *   <li>the spirit form: in the Geestenwereld every player is drawn see-through, with a soft glow round them;</li>
 *   <li>your own body as a blue echo at the arrival ({@code lijf}): a figure with your skin and a pale blue outline, only in
 *       your own game;</li>
 *   <li>the mood of the mist: deep blue in a spirit tide, silver in a moon night (the fog colour, eased);</li>
 *   <li>Piandao-guh's duel: a click (attack or use) is a parry ({@link GeestSpelPayloads.Pareer}), not a swing;</li>
 *   <li>a mirror pond: a ripple over the screen with what the water shows.</li>
 * </ul>
 */
public final class E2GeestSpelClient {
    public static final net.minecraft.resources.Identifier LAAG_SPIEGEL = Guhs.id("einde2_geest_spel_spiegel");
    /** How long a mirror pond's picture stays (ms). */
    private static final long SPIEGEL_MS = 6000;

    /** How far the fog has turned to the tide's blue / the moon's silver (0..1, eased per tick). */
    private static float tij, maan;
    @Nullable
    private static Echo echo;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterRenderStateModifiersEvent event) -> event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar.level().dimension() == Geestenwereld.DIM && !avatar.isSpectator()) {
                    state.isInvisible = true;               // drawn see-through (the game's own ghost look)
                    state.isInvisibleToPlayer = false;
                }
            }
        }));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(LAAG_SPIEGEL, E2GeestSpelClient::spiegel));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(E2GeestSpelClient::mist);
        NeoForge.EVENT_BUS.addListener(E2GeestSpelClient::klik);
    }

    private static boolean hier(Minecraft mc) {
        return mc.level != null && mc.level.dimension() == Geestenwereld.DIM;
    }

    // =====================================================================================================================
    // the tick: the mood eases, the glow of the spirit form, the echo of your body
    // =====================================================================================================================

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean hier = hier(mc);
        tij += ((hier && GeestSpelPayloads.CLIENT_TIJ ? 1f : 0f) - tij) * 0.03f;
        maan += ((hier && GeestSpelPayloads.CLIENT_MAAN ? 1f : 0f) - maan) * 0.03f;
        if (!hier || mc.player == null || mc.isPaused()) {
            weg();
            if (!hier) {
                GeestSpelPayloads.CLIENT_RITME = false;
            }
            return;
        }
        ClientLevel level = mc.level;
        if (level.getGameTime() % 6 == 0) {
            for (var speler : level.players()) {
                if (!speler.isSpectator() && speler.distanceToSqr(mc.player) < 48 * 48) {
                    level.addParticle(ParticleTypes.GLOW, speler.getX() + (level.getRandom().nextDouble() - 0.5) * 0.8, speler.getY() + level.getRandom().nextDouble() * 1.8,
                            speler.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
                }
            }
        }
        echo(mc, level);
    }

    /** Your body as a blue echo at the arrival, while you are near it. */
    private static void echo(Minecraft mc, ClientLevel level) {
        BlockPos lijf = Geestenwereld.wereld("lijf");
        LocalPlayer speler = mc.player;
        boolean wil = lijf != null && speler.blockPosition().closerThan(lijf, 40);
        if (!wil) {
            weg();
            return;
        }
        if (echo == null || echo.isRemoved() || echo.level() != level) {
            echo = new Echo(level, speler);
            echo.setId(-4_500_000);
            Vec3 at = Vec3.atBottomCenterOf(lijf);
            echo.snapTo(at.x, at.y, at.z, 180f, 0f);
            echo.setYHeadRot(180f);
            echo.setYBodyRot(180f);
            echo.setOldPosAndRot();
            echo.setPose(Pose.CROUCHING);
            echo.setShiftKeyDown(true);
            level.addEntity(echo);
        }
        if (level.getGameTime() % 10 == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, echo.getX() + (level.getRandom().nextDouble() - 0.5) * 0.6, echo.getY() + 0.4 + level.getRandom().nextDouble(),
                    echo.getZ() + (level.getRandom().nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        }
    }

    private static void weg() {
        if (echo != null) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && !echo.isRemoved()) {
                level.removeEntity(echo.getId(), Entity.RemovalReason.DISCARDED);
            }
            echo = null;
        }
    }

    /** The echo: another player in this client's level with your skin, crouched as if meditating, a pale blue outline. */
    private static final class Echo extends RemotePlayer {
        private final LocalPlayer echt;

        Echo(ClientLevel level, LocalPlayer echt) {
            super(level, new GameProfile(java.util.UUID.randomUUID(), echt.getGameProfile().name()));
            this.echt = echt;
        }

        @Override
        public PlayerSkin getSkin() {
            return echt.getSkin();
        }

        @Override
        public boolean shouldShowName() {
            return false;
        }

        @Override
        public boolean isCurrentlyGlowing() {
            return true;
        }

        @Override
        public int getTeamColor() {
            return 0x7FD4FF;
        }

        @Override
        public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) {
            return echt.isModelPartShown(part);
        }

        @Override
        public void tick() {
            // (it does not move: a body that meditates)
        }
    }

    // =====================================================================================================================
    // the mist's colour
    // =====================================================================================================================

    private static void mist(ViewportEvent.ComputeFogColor event) {
        if (tij < 0.01f && maan < 0.01f || !hier(Minecraft.getInstance())) {
            return;
        }
        float r = event.getRed(), g = event.getGreen(), b = event.getBlue();
        r += (0.10f - r) * tij * 0.85f;
        g += (0.16f - g) * tij * 0.85f;
        b += (0.55f - b) * tij * 0.85f;
        r += (0.86f - r) * maan * 0.7f;
        g += (0.88f - g) * maan * 0.7f;
        b += (0.95f - b) * maan * 0.7f;
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    // =====================================================================================================================
    // the duel
    // =====================================================================================================================

    private static void klik(InputEvent.InteractionKeyMappingTriggered event) {
        if (!GeestSpelPayloads.CLIENT_RITME || !hier(Minecraft.getInstance()) || Minecraft.getInstance().screen != null) {
            return;
        }
        if (event.isAttack() || event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(true);
            ClientPacketDistributor.sendToServer(new GeestSpelPayloads.Pareer());
        }
    }

    // =====================================================================================================================
    // the mirror pond
    // =====================================================================================================================

    private static void spiegel(GuiGraphicsExtractor g, DeltaTracker delta) {
        long t = System.currentTimeMillis() - GeestSpelPayloads.CLIENT_SPIEGEL_SINDS;
        Minecraft mc = Minecraft.getInstance();
        if (t < 0 || t > SPIEGEL_MS || !hier(mc) || mc.options.hideGui) {
            return;
        }
        float in = Math.min(1f, t / 600f), uit = Math.min(1f, (SPIEGEL_MS - t) / 900f);
        float a = Math.min(in, uit);
        int sw = g.guiWidth(), sh = g.guiHeight();
        int cx = sw / 2, cy = sh / 2;
        // the ripple: rings of pale light that slowly grow
        for (int ring = 0; ring < 5; ring++) {
            double fase = ((t / 1400.0) + ring / 5.0) % 1.0;
            int rad = (int) (30 + fase * Math.min(sw, sh) * 0.45);
            int alpha = (int) (a * (1 - fase) * 90);
            cirkel(g, cx, cy, rad, (alpha << 24) | 0xBFE8FF);
        }
        g.fill(0, 0, sw, sh, ((int) (a * 70) << 24) | 0x1A2A55);
        Font font = mc.font;
        Component kop = Component.translatable("gui.guhs.einde2_geest_spel.spiegel.kop").withStyle(ChatFormatting.ITALIC);
        Component wat = GeestSpelPayloads.CLIENT_SPIEGEL_ZELF ? Component.translatable("gui.guhs.einde2_geest_spel.spiegel.jij")
                : Component.translatable("structure.guhs." + GeestSpelPayloads.CLIENT_SPIEGEL);
        int kleur = ((int) (a * 255) << 24) | 0xDDF4FF;
        g.text(font, kop, cx - font.width(kop) / 2, cy - 14, kleur, true);
        g.text(font, wat, cx - font.width(wat) / 2, cy, ((int) (a * 255) << 24) | 0xFFFFFF, true);
    }

    /** A thin ring of dots. */
    private static void cirkel(GuiGraphicsExtractor g, int cx, int cy, int r, int kleur) {
        int stappen = Math.max(24, r);
        for (int i = 0; i < stappen; i++) {
            double h = Math.PI * 2 * i / stappen;
            int x = cx + (int) Math.round(Math.cos(h) * r), y = cy + (int) Math.round(Math.sin(h) * r * 0.55);
            g.fill(x, y, x + 2, y + 1, kleur);
        }
    }

    private E2GeestSpelClient() {
    }
}
