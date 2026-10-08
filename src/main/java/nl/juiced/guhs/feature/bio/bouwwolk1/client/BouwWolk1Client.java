package nl.juiced.guhs.feature.bio.bouwwolk1.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ballon.client.BallonClient;
import nl.juiced.guhs.feature.bio.bouwwolk1.BouwWolk1Slice;
import nl.juiced.guhs.feature.bio.bouwwolk1.Sterrenkijker;

/**
 * Client side of the biomes3 slice "bouw-wolk1":
 * <ul>
 *   <li>the haven balloon is drawn by the festival balloon's own renderer (same model, same four colours);</li>
 *   <li>the sterrenkijkerguh of the ruin wears Professor Sterretje's model; in the dark the star on his hat wobbles and
 *       he peers through his spyglass, by day his head hangs and he breathes slowly (asleep in his chair); the
 *       ballonvaarder-guh wears Kapitein Wolkje's model, his scarf flutters;</li>
 *   <li><b>more falling stars at the ruin</b>: in the dark, while a sterrenkijkerguh of a ruin is in sight (within
 *       {@value #BEREIK} blocks), stars shoot across the sky above him: the same sparkling tail as the falling stars of
 *       the sterrenregen (feature evenementen: firework and end-rod sparks along a straight slanted line at the same
 *       speed), but only light: nothing lands, nothing is picked up, no entity, nothing on the server. About one every
 *       {@value #ELKE} ticks, at most {@value #TEGELIJK} at once; where he is, is looked up once every two seconds.</li>
 * </ul>
 */
public final class BouwWolk1Client {
    public static final int BEREIK = 64, ELKE = 28, TEGELIJK = 4;
    /** Blocks per tick: the speed of a falling star of the sterrenregen (VallendeSterEntity.SPEED). */
    private static final double SNELHEID = 1.1;

    /** A shooting star: where it is, where it goes, how long still. */
    private static final class Ster {
        Vec3 plek;
        final Vec3 stap;
        int ticks;

        Ster(Vec3 plek, Vec3 stap, int ticks) {
            this.plek = plek;
            this.stap = stap;
            this.ticks = ticks;
        }
    }

    private static final List<Ster> STERREN = new ArrayList<>();
    @Nullable
    private static Vec3 ruine;
    private static int teller;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(BouwWolk1Slice.HAVEN_BALLON.get(), BallonClient.LuchtballonRenderer::new));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER, Guhs.id("entity/guh_npc_sterrenkijkerguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            if (!Sterrenkijker.wakker(npc.level())) {
                float adem = (float) Math.sin(t * 0.6f);
                return bones -> {
                    bones.ifPresent("head", b -> b.setRotX(-0.42f + adem * 0.04f));
                    bones.ifPresent("sterretje_hoedpunt", b -> b.setRotZ(0.5f + adem * 0.05f));
                    bones.ifPresent("body", b -> b.setTranslateY(adem * 0.12f));
                };
            }
            float kijk = (float) Math.max(0, Math.sin(t * 0.35f));
            return bones -> {
                bones.ifPresent("sterretje_hoedpunt", b -> b.setRotZ((float) Math.sin(t * 1.3f) * 0.12f));
                bones.ifPresent("sterretje_kijker", b -> b.setRotX(-kijk * 0.9f));
            };
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BALLONVAARDERGUH, Guhs.id("entity/guh_npc_ballonguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BALLONVAARDERGUH, (npc, tick) -> {
            float t = (float) tick * 0.25f;
            return bones -> bones.ifPresent("kapitein_sjaalpunt", b -> {
                b.setRotY((float) Math.sin(t) * 0.35f);
                b.setRotX(0.2f + (float) Math.sin(t * 1.7f) * 0.12f);
            });
        });
        NeoForge.EVENT_BUS.addListener(BouwWolk1Client::tick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            STERREN.clear();
            ruine = null;
        });
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (teller++ % 40 == 0) {
            ruine = null;
            double beste = Double.MAX_VALUE;
            for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, mc.player.getBoundingBox().inflate(BEREIK),
                    n -> n.getKind() == GuhNpcEntity.Kind.STERRENWACHT_RUINE_STERRENKIJKER)) {
                double d = npc.distanceToSqr(mc.player);
                if (d < beste) {
                    beste = d;
                    ruine = npc.position();
                }
            }
        }
        RandomSource random = level.getRandom();
        if (ruine != null && STERREN.size() < TEGELIJK && random.nextInt(ELKE) == 0 && Sterrenkijker.wakker(level)) {
            STERREN.add(nieuw(ruine, random));
        }
        for (Iterator<Ster> it = STERREN.iterator(); it.hasNext(); ) {
            Ster s = it.next();
            s.plek = s.plek.add(s.stap);
            for (int i = 0; i < 3; i++) {                        // the tail, as VallendeSterEntity draws it
                double f = random.nextDouble();
                level.addAlwaysVisibleParticle(i == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD, true, s.plek.x - s.stap.x * f,
                        s.plek.y - s.stap.y * f, s.plek.z - s.stap.z * f, 0, 0, 0);
            }
            if (--s.ticks <= 0) {
                it.remove();
            }
        }
    }

    /** A star high above the ruin, somewhere around it, on a slanted line across the sky. */
    static Ster nieuw(Vec3 boven, RandomSource random) {
        double hoek = random.nextDouble() * Math.PI * 2, ver = 12 + random.nextDouble() * 44;
        Vec3 begin = boven.add(Math.cos(hoek) * ver, 34 + random.nextDouble() * 26, Math.sin(hoek) * ver);
        double richting = random.nextDouble() * Math.PI * 2;
        Vec3 stap = new Vec3(Math.cos(richting), -0.35 - random.nextDouble() * 0.3, Math.sin(richting)).normalize().scale(SNELHEID);
        return new Ster(begin, stap, 22 + random.nextInt(18));
    }

    private BouwWolk1Client() {
    }
}
