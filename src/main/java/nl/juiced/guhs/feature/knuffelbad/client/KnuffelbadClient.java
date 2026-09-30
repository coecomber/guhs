package nl.juiced.guhs.feature.knuffelbad.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knuffelbad.Badmeester;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadPayloads;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;
import nl.juiced.guhs.feature.knus.client.GuhRenderHooks;

/**
 * Client side of the Knuffelbad: the zwembandje and the rubber ducks, the particles, Badmeester Bubbel's own model (a
 * swim cap, a lifebuoy and a whistle he blows now and then) and his screen, the ride itself in your game (the camera
 * that banks with the slide, the panel, the splashes and sounds: {@link GlijCamera}, {@link GlijHud}, {@link GlijEffecten})
 * and the shimmer of a freshly washed guh ({@link GlansLaag}).
 */
public final class KnuffelbadClient {
    /** When each Badmeester last blew his whistle at the local player (client only, for the animation). */
    private static final java.util.Map<GuhNpcEntity, Long> FLUIT = new java.util.WeakHashMap<>();

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(KnuffelbadFeature.ZWEMBANDJE.get(), ZwembandjeRenderer::new);
            event.registerEntityRenderer(KnuffelbadFeature.BADEENDJE.get(), BadeendjeRenderer::new);
        });
        modBus.addListener(KnuffelbadClient::particles);
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("knuffelbad_hud"), GlijHud::render));
        // Badmeester Bubbel: the sitting guh with a swim cap, a lifebuoy and a whistle (tools/features/knuffelbad.py)
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BADMEESTERGUH, Guhs.id("geo/entity/guh_npc_badmeesterguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BADMEESTERGUH, (npc, state, bot) -> {
            // the whistle goes to his mouth when you run by the pool (TUUUT, like the server's Badmeester.tick), now and then
            // a little practice toot, and the lifebuoy wobbles a little
            double t = state.getAnimationTick();
            long now = npc.level().getGameTime();
            LocalPlayer me = Minecraft.getInstance().player;
            if (me != null && me.isSprinting() && me.onGround() && me.distanceToSqr(npc) < Badmeester.FLUIT_AFSTAND * Badmeester.FLUIT_AFSTAND
                    && now - FLUIT.getOrDefault(npc, -10000L) > Badmeester.FLUIT_RUST) {
                FLUIT.put(npc, now);
            }
            double sinds = now - FLUIT.getOrDefault(npc, -10000L) + state.getPartialTick();
            double cycle = (t + npc.getId() * 37) % 600;
            float blaas = sinds < 30 ? (float) Math.sin(sinds / 30 * Math.PI) : cycle < 20 ? (float) Math.sin(cycle / 20 * Math.PI) * 0.6f : 0f;
            bot.apply("badmeester_fluitje").ifPresent(b -> {
                b.setRotX(-blaas * 1.1f);
                b.setPosY(blaas * 1.5f);
            });
            bot.apply("badmeester_boei").ifPresent(b -> b.setRotZ((float) Math.sin(t * 0.07) * 0.04f));
        });
        GuhRenderHooks.laag(GlansLaag::render);
        ZwembandjeEntity.lokaal = () -> Minecraft.getInstance().player;
        ZwembandjeEntity.stuur = () -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p == null || Minecraft.getInstance().screen != null ? 0 : -p.input.leftImpulse;
        };
        ZwembandjeEntity.clientTick = GlijEffecten::tick;
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> GlijCamera.hoeken(event));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> GlijCamera.fov(event));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            GlijCamera.tick();
            GlansLaag.tick();
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> GlijHud.uit());
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KnuffelbadFeature.ZEEPBELLETJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new KnuffelbadParticles.Zeepbelletje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KnuffelbadFeature.SCHUIMVLOKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new KnuffelbadParticles.Schuimvlokje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KnuffelbadFeature.GLINSTERING.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new KnuffelbadParticles.Glinstering(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KnuffelbadFeature.PLONS.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                new KnuffelbadParticles.Plons(level, x, y, z, dx, dy, dz, sprites));
    }

    /** knuffelbad_open: Badmeester Bubbel's screen. */
    public static void open(KnuffelbadPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new KnuffelbadScherm(payload.npcId(), payload.data()));
    }

    /** knuffelbad_hud: the ride panel. */
    public static void hud(CompoundTag data) {
        GlijHud.zet(data);
    }

    /** The ring the local player rides (and runs the ride of), or null. */
    @Nullable
    public static ZwembandjeEntity eigenRing() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && p.getVehicle() instanceof ZwembandjeEntity ring && ring.eigen ? ring : null;
    }

    private KnuffelbadClient() {
    }
}
