package nl.juiced.guhs.feature.ringh3.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.ringh3.BarbecuerogEntity;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;
import nl.juiced.guhs.feature.ringh3.Scenes;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * Client side of bbq2 (ring-h3): the Barbecuerog as you see, hear and feel him, and the two things the bridge scene needs
 * that the verhaal engine does not have.
 * <ul>
 *   <li>His renderer: the GeckoLib model with its glow mask (cracks, eyes, embers and every flame shine in the dark), drawn
 *       from far away (he is ten blocks tall).</li>
 *   <li>{@link #vuur}: while his flames burn he sheds fire, ash and embers, black smoke wreathes him from his feet to his
 *       wings, and he CARRIES LIGHT: a light block that only exists in this game walks along with him, so a dark hall lights
 *       up as he comes (and the first thing you see of him, before that, is two eyes and the fire in his seams).</li>
 *   <li>{@link #schok}: his steps, stomps and roars shake the camera of whoever is near (not during a scene: a scene shakes
 *       by its own script).</li>
 *   <li>The white flashes of the bridge scene ({@link Scenes#FLITSEN}: the blade on Guhdalf's shield of light, the staff on
 *       the bridge), drawn over the picture between the scene's black bars (the bars and the subtitle stay as they are).</li>
 *   <li>{@link BrugBreuk}: the span that breaks, in this game only.</li>
 * </ul>
 * Model, textures and animations: tools/features/ring_h3_modellen.py.
 */
public final class RingH3Client {
    /** The light blocks that walk along with a burning Barbecuerog: entity id -> where its light is now. */
    private static final Map<Integer, BlockPos> LICHT = new HashMap<>();
    private static float schud;
    private static int schudTicks;
    /** Ticks the bars of a scene take to slide in and out (the engine's CutsceneSpeler.BALK_TICKS). */
    private static final float BALK_TICKS = 12f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(RingH3Feature.BARBECUEROG.get(), RogRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            BrugBreuk.tick();
            lichtTick();
            if (schudTicks > 0) {
                schudTicks--;
            } else {
                schud *= 0.75f;
            }
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> camera(event));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> flits(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            LICHT.clear();
            BrugBreuk.wis();
            schud = 0;
        });
    }

    static class RogRenderer extends GeoEntityRenderer<BarbecuerogEntity, LivingEntityRenderState> {
        RogRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BarbecuerogEntity>(Guhs.id("barbecuerog")));
            this.shadowRadius = 3.0f;
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        /**
         * He is far bigger than the box he stands in (a wing reaches eleven blocks to a side, his fire thirteen blocks up): with
         * the game's own rule he would vanish, wings and all, the moment that box leaves the picture.
         */
        @Override
        protected AABB getBoundingBoxForCulling(BarbecuerogEntity rog) {
            return rog.getBoundingBox().inflate(10.0, 5.0, 10.0);
        }
    }

    // =====================================================================================================================
    // fire, embers, light
    // =====================================================================================================================

    /** (every client tick of every Barbecuerog) what burns on him, and the light he carries. */
    public static void vuur(BarbecuerogEntity rog) {
        if (!(rog.level() instanceof ClientLevel level)) {
            return;
        }
        RandomSource r = level.getRandom();
        Vec3 voor = Vec3.directionFromRotation(0, rog.yBodyRot);
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        double x = rog.getX(), y = rog.getY(), z = rog.getZ();
        if (!rog.brandt()) {
            // asleep in the dark: a thread of smoke now and then, nothing more
            if (rog.tickCount % 12 == 0) {
                level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 3, y + 5 + r.nextDouble() * 3, z + (r.nextDouble() - 0.5) * 3, 0, 0.02, 0);
            }
            doof(level, rog.getId());
            return;
        }
        // the mane: flames from his crown and down his back (the model's own fire stands up to thirteen blocks high)
        for (int i = 0; i < 3; i++) {
            double hoog = 9.0 + r.nextDouble() * 3.6, terug = -2.2 + r.nextDouble() * 3.4;
            level.addAlwaysVisibleParticle(i == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, true, x + voor.x * terug + (r.nextDouble() - 0.5) * 2.4,
                    y + hoog, z + voor.z * terug + (r.nextDouble() - 0.5) * 2.4, 0, 0.03 + r.nextDouble() * 0.05, 0);
        }
        // black smoke wreathes him: it wells up round his feet, rolls off his back and his wings and climbs past his head
        // (he never stands clean against a wall: there is always his own darkness round him)
        double hoek = r.nextDouble() * Math.PI * 2, straal = 2.0 + r.nextDouble() * 2.4;
        level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x + Math.cos(hoek) * straal, y + 0.2 + r.nextDouble() * 1.2, z + Math.sin(hoek) * straal,
                0, 0.03 + r.nextDouble() * 0.04, 0);
        double kant = (r.nextBoolean() ? 1 : -1) * (1.5 + r.nextDouble() * 7.5), terug = 1.2 + r.nextDouble() * 1.6;
        level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x + opzij.x * kant - voor.x * terug, y + 4.0 + r.nextDouble() * 7.5,
                z + opzij.z * kant - voor.z * terug, 0, 0.04 + r.nextDouble() * 0.04, 0);
        if (rog.tickCount % 2 == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x - voor.x * 1.8 + (r.nextDouble() - 0.5) * 3.0, y + 10.0 + r.nextDouble() * 3.0,
                    z - voor.z * 1.8 + (r.nextDouble() - 0.5) * 3.0, 0, 0.07, 0);
            level.addParticle(ParticleTypes.ASH, x + (r.nextDouble() - 0.5) * 12, y + r.nextDouble() * 11, z + (r.nextDouble() - 0.5) * 12, 0, 0, 0);
            level.addParticle(ParticleTypes.WHITE_ASH, x + (r.nextDouble() - 0.5) * 12, y + r.nextDouble() * 11, z + (r.nextDouble() - 0.5) * 12, 0, 0, 0);
        }
        if (rog.tickCount % 5 == 0) {
            // the blade of fire (in his right hand, reaching forward) and the lash (left) drip fire
            double ver = 2.0 + r.nextDouble() * 5.0;
            level.addParticle(ParticleTypes.FLAME, x - opzij.x * 3.0 + voor.x * ver, y + 2.6 + r.nextDouble() * 0.8, z - opzij.z * 3.0 + voor.z * ver, 0, 0.02, 0);
            level.addParticle(ParticleTypes.SMALL_FLAME, x + opzij.x * 3.0 + voor.x * (r.nextDouble() * 3.0), y + 0.4 + r.nextDouble() * 2.4,
                    z + opzij.z * 3.0 + voor.z * (r.nextDouble() * 3.0), 0, 0.01, 0);
        }
        if (r.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.LAVA, x + (r.nextDouble() - 0.5) * 3, y + 4.5 + r.nextDouble() * 2, z + (r.nextDouble() - 0.5) * 3, 0, 0, 0);
        }
        licht(level, rog);
    }

    /** His light: a light block of this game alone, in the air at his chest, moved when he moves. */
    private static void licht(ClientLevel level, BarbecuerogEntity rog) {
        BlockPos wil = null;
        BlockPos borst = BlockPos.containing(rog.getX(), rog.getY() + 5.5, rog.getZ());
        for (int dy = 0; dy <= 4 && wil == null; dy++) {
            for (BlockPos p : new BlockPos[] {borst.above(dy), borst.below(dy)}) {
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s.is(Blocks.LIGHT)) {
                    wil = p;
                    break;
                }
            }
        }
        BlockPos nu = LICHT.get(rog.getId());
        if (wil == null || wil.equals(nu)) {
            return;
        }
        // (moving a light of fifteen relights and redraws every chunk section round it: he takes it along in strides of three
        // blocks, not at every block he crosses)
        if (nu != null && level.getBlockState(nu).is(Blocks.LIGHT) && nu.distSqr(wil) < 9) {
            return;
        }
        if (nu != null && level.getBlockState(nu).is(Blocks.LIGHT)) {
            level.setBlock(nu, Blocks.AIR.defaultBlockState(), 19);
        }
        level.setBlock(wil, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 19);
        LICHT.put(rog.getId(), wil.immutable());
    }

    private static void doof(ClientLevel level, int id) {
        BlockPos nu = LICHT.remove(id);
        if (nu != null && level.getBlockState(nu).is(Blocks.LIGHT)) {
            level.setBlock(nu, Blocks.AIR.defaultBlockState(), 19);
        }
    }

    /** (every client tick) lights whose Barbecuerog is gone go out. */
    private static void lichtTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            LICHT.clear();
            return;
        }
        for (Iterator<Map.Entry<Integer, BlockPos>> it = LICHT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, BlockPos> e = it.next();
            Entity rog = level.getEntity(e.getKey());
            if (!(rog instanceof BarbecuerogEntity b) || b.isRemoved()) {
                if (level.getBlockState(e.getValue()).is(Blocks.LIGHT)) {
                    level.setBlock(e.getValue(), Blocks.AIR.defaultBlockState(), 19);
                }
                it.remove();
            }
        }
    }

    // =====================================================================================================================
    // the ground shakes
    // =====================================================================================================================

    /** (the entity, when its synced shock counter changes) a step, a stomp or a roar of this strength (0..15). */
    public static void schok(BarbecuerogEntity rog, int kracht) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CutsceneSpeler.actief()) {
            return;
        }
        double afstand = Math.sqrt(mc.player.distanceToSqr(rog));
        float k = (float) (kracht / 15.0 * 3.2 * Mth.clamp(1.0 - afstand / 56.0, 0, 1));
        if (k > schud) {
            schud = k;
            schudTicks = 4 + kracht;
        }
    }

    private static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (schud < 0.02f || CutsceneSpeler.actief()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double tijd = (mc.level == null ? 0 : mc.level.getGameTime()) + event.getPartialTick();
        float k = schud * schud * 0.5f + schud * 0.4f;
        event.setYaw(event.getYaw() + (float) (Math.sin(tijd * 1.9) * k * 0.9));
        event.setPitch(event.getPitch() + (float) (Math.cos(tijd * 2.6) * k * 0.8));
        event.setRoll(event.getRoll() + (float) (Math.sin(tijd * 1.3) * k * 0.7));
    }

    // =====================================================================================================================
    // the flashes of the bridge scene
    // =====================================================================================================================

    private static void flits(RenderGuiEvent.Post event) {
        Cutscene scene = CutsceneSpeler.scene();
        if (scene != Scenes.BRUG) {
            return;
        }
        float tijd = CutsceneSpeler.tijd() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float sterkst = 0;
        for (int[] f : Scenes.FLITSEN) {
            float dt = tijd - f[0];
            if (dt < 0 || dt > f[1] * 2.5f) {
                continue;
            }
            // up in a tick, down slowly
            float a = dt < 1 ? dt : (float) Math.pow(1.0 - (dt - 1) / (f[1] * 2.5f - 1), 1.6);
            sterkst = Math.max(sterkst, a * f[2] / 100f);
        }
        if (sterkst > 0.01f) {
            int alpha = Mth.clamp((int) (sterkst * 255), 0, 255);
            // only the picture: the scene's black bars (VerhaalHud: 13 % of the height each, sliding in and out in BALK_TICKS)
            // and the subtitle in the lower one are not lit up
            int breed = event.getGuiGraphics().guiWidth(), hoog = event.getGuiGraphics().guiHeight();
            float in = Mth.clamp(Math.min(tijd / BALK_TICKS, (scene.duur() - tijd) / BALK_TICKS), 0f, 1f);
            int balk = Math.round(hoog * 0.13f * in);
            event.getGuiGraphics().fill(0, balk, breed, hoog - balk, (alpha << 24) | 0xFFF8E6);
        }
    }

    private RingH3Client() {
    }
}
