package nl.juiced.guhs.feature.ringh3.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.LightCoordsUtil;
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
 *   <li>His renderer: the GeckoLib model, drawn from far away (he is ten blocks tall): at full brightness while he burns,
 *       with its glow mask while he is dark (cracks and eyes shine in the dark).</li>
 *   <li>{@link #vuur}: while his flames burn he sheds fire, ash and embers, black smoke wreathes him from his feet to his
 *       wings, and he CARRIES LIGHT: a light block that only exists in this game walks along with him, so a dark hall lights
 *       up as he comes (and the first thing you see of him, before that, is two eyes and the fire in his seams).</li>
 *   <li>{@link #schok}: his steps, stomps and roars shake the camera of whoever is near (not during a scene: a scene shakes
 *       by its own script).</li>
 *   <li>The white flashes of the bridge scene ({@link Scenes#FLITSEN}: the blade on Guhdalf's shield of light, the staff on
 *       the bridge), drawn over the picture between the scene's black bars (the bars and the subtitle stay as they are).</li>
 *   <li>The lens of the bridge scene ({@link Scenes#LENZEN}) and its made-to-measure fire and light ({@link SceneVuur}).</li>
 *   <li>{@link BrugBreuk}: the span that breaks, in this game only.</li>
 * </ul>
 * Model, textures and animations: tools/features/ring_h3_modellen.py.
 */
public final class RingH3Client {
    /** The light blocks that walk along with a burning Barbecuerog: entity id -> where its light is now. */
    private static final Map<Integer, BlockPos> LICHT = new HashMap<>();
    /** The colour of his smoke (it dyes the pale smoke of a campfire): soot, a little warm. */
    private static final int ROOK = 0x1A1210;
    private static float schud;
    private static int schudTicks;
    /** Ticks the bars of a scene take to slide in and out (the engine's CutsceneSpeler.BALK_TICKS). */
    private static final float BALK_TICKS = 12f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(RingH3Feature.BARBECUEROG.get(), RogRenderer::new));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            SceneVuur.tick();
            BrugBreuk.tick();
            lichtTick();
            if (schudTicks > 0) {
                schudTicks--;
            } else {
                schud *= 0.75f;
            }
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles event) -> camera(event));
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> lens(event));
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> flits(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            LICHT.clear();
            BrugBreuk.wis();
            SceneVuur.wis();
            schud = 0;
        });
    }

    static class RogRenderer extends GeoEntityRenderer<BarbecuerogEntity, LivingEntityRenderState> {
        RogRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BarbecuerogEntity>(Guhs.id("barbecuerog")));
            this.shadowRadius = 3.0f;
            withRenderLayer(new AutoGlowingGeoLayer<BarbecuerogEntity, Void, LivingEntityRenderState>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
                    // (only while he is dark: see extractRenderState)
                    if (info.renderState().lightCoords != LightCoordsUtil.FULL_BRIGHT) {
                        super.submitRenderTask(info, collector);
                    }
                }
            });
        }

        /**
         * While his flames burn he is drawn ONCE, at full brightness: he is his own light (his skin is charcoal: black stays
         * black, the seams, the eyes and every flame shine as they do through the glow mask). The glow layer, which draws all
         * 370 cubes of him a second time, is only used while he is a shape in the dark (asleep in the Diepe Poort): measured,
         * that second pass was a tenth to a third of what he costs a frame.
         */
        @Override
        public void extractRenderState(BarbecuerogEntity rog, LivingEntityRenderState state, float partialTick) {
            super.extractRenderState(rog, state, partialTick);
            if (rog.brandt()) {
                state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            }
        }

        /**
         * He is far bigger than the box he stands in (a wing reaches eleven blocks to a side, his fire sixteen blocks up): with
         * the game's own rule he would vanish, wings and all, the moment that box leaves the picture.
         */
        @Override
        protected AABB getBoundingBoxForCulling(BarbecuerogEntity rog) {
            return rog.getBoundingBox().inflate(12.0, 7.0, 12.0);
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
        // He is WREATHED: what he is made of is fire and shadow, and both come off him all the time. Every particle is made to
        // measure (SceneVuur.deeltje: scaled, aimed, dyed, with a life of its own): the game's own smoke puff is a hand wide,
        // on a demon of ten blocks that is dust. About five a tick; only the smoke is translucent (some 70 soft puffs at
        // a time: it is what costs frames), the embers are cut-out grains.
        double m = SceneVuur.maat();
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.getMainCamera().position().distanceToSqr(x, y + 5, z) > 56 * 56) {
            m *= 0.5;                                          // (far away: half of it)
        }
        if (rog.tickCount % 2 == 0 && r.nextDouble() < m) {
            // shadow: black smoke wells up round his feet and rolls up his legs ... (the soft smoke of a campfire, dyed black:
            // the game's own smoke puff is eight pixels, at this size it is a heap of squares)
            double hoek = r.nextDouble() * Math.PI * 2, straal = 1.6 + r.nextDouble() * 2.8;
            SceneVuur.deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + Math.cos(hoek) * straal, y + 0.3 + r.nextDouble() * 1.4, z + Math.sin(hoek) * straal,
                    Math.cos(hoek) * 0.015, 0.03 + r.nextDouble() * 0.035, Math.sin(hoek) * 0.015, 0.9f + r.nextFloat() * 0.7f, 46 + r.nextInt(24), ROOK);
        } else if (r.nextDouble() < m) {
            // ... and pours off his back and his wings, up past his horns: the cloak of darkness he stands in (behind him:
            // his face and his fire stay clear)
            double kant = (r.nextBoolean() ? 1 : -1) * (0.5 + r.nextDouble() * 8.5), terug = 2.4 + r.nextDouble() * 2.4;
            SceneVuur.deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + opzij.x * kant - voor.x * terug, y + 4.5 + r.nextDouble() * 8.0,
                    z + opzij.z * kant - voor.z * terug, -voor.x * 0.02, 0.04 + r.nextDouble() * 0.05, -voor.z * 0.02, 1.0f + r.nextFloat() * 0.9f, 50 + r.nextInt(30), ROOK);
        }
        for (int i = 0; i < 3; i++) {
            if (r.nextDouble() >= m) {
                continue;
            }
            // fire: sparks stream up out of his mane, and embers drift up from all of him, far over his head (grains of fire:
            // the game's flame at its own size is a candle, and a demon hung with candles is a birthday cake)
            boolean manen = i < 2;
            double kant = (r.nextDouble() - 0.5) * (manen ? 3.4 : 17.0), ver = manen ? -3.0 + r.nextDouble() * 4.0 : -r.nextDouble() * 2.5;
            SceneVuur.deeltje(ParticleTypes.SMALL_FLAME, x + opzij.x * kant + voor.x * ver, y + (manen ? 10.0 + r.nextDouble() * 4.0 : 2.0 + r.nextDouble() * 9.0),
                    z + opzij.z * kant + voor.z * ver, (r.nextDouble() - 0.5) * 0.06, (manen ? 0.16 : 0.07) + r.nextDouble() * 0.14, (r.nextDouble() - 0.5) * 0.06,
                    0.3f + r.nextFloat() * 0.35f, 24 + r.nextInt(36), 0);
        }
        if (rog.tickCount % 4 == 0 && r.nextDouble() < m) {
            SceneVuur.deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, x - voor.x * 2.4 + (r.nextDouble() - 0.5) * 4.0, y + 11.5 + r.nextDouble() * 3.0,
                    z - voor.z * 2.4 + (r.nextDouble() - 0.5) * 4.0, 0, 0.08 + r.nextDouble() * 0.05, 0, 0.9f + r.nextFloat() * 0.8f, 44 + r.nextInt(26), ROOK);
            level.addParticle(ParticleTypes.ASH, x + (r.nextDouble() - 0.5) * 14, y + r.nextDouble() * 12, z + (r.nextDouble() - 0.5) * 14, 0, 0, 0);
            level.addParticle(ParticleTypes.WHITE_ASH, x + (r.nextDouble() - 0.5) * 14, y + r.nextDouble() * 12, z + (r.nextDouble() - 0.5) * 14, 0, 0, 0);
        }
        if (r.nextInt(12) == 0) {
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

    /** The lens of the bridge scene: a wide one where all of him must fit in the picture, a long one for a face ({@link Scenes#LENZEN}). */
    private static void lens(ViewportEvent.ComputeFov event) {
        if (CutsceneSpeler.scene() != Scenes.BRUG) {
            return;
        }
        float tijd = CutsceneSpeler.tijd() + (float) event.getPartialTick();
        for (float[] l : Scenes.LENZEN) {
            if (tijd >= l[0] && tijd < l[1]) {
                event.setFOV(l[2] + (l[3] - l[2]) * (tijd - l[0]) / Math.max(1f, l[1] - l[0]));
                return;
            }
        }
    }

    private static void flits(RenderGuiEvent.Post event) {
        Cutscene scene = CutsceneSpeler.scene();
        if (scene != Scenes.BRUG) {
            return;
        }
        float tijd = CutsceneSpeler.tijd() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float sterkst = 0;
        for (int[] f : Scenes.FLITSEN) {
            float dt = tijd - f[0];
            if (dt < 0 || dt > f[1]) {
                continue;
            }
            // up within a tick, then down fast (a flash of lightning: a veil of white that hangs for seconds is fog, not light)
            float a = dt < 1 ? dt : (float) Math.pow(1.0 - (dt - 1) / Math.max(1f, f[1] - 1f), 3.0);
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
