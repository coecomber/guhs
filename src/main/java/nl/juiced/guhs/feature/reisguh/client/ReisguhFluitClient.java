package nl.juiced.guhs.feature.reisguh.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.reisguh.ReisguhFluit;

/**
 * The Reisguh on the client: his own model (the conducteurspetje and the fluitje on a cord: geo guh_npc_reisguh) and,
 * when he blows his whistle ({@link ReisguhFluit.Fluit}), the whistle goes up into his mouth, he puffs his cheeks, leans
 * back a little and waves a paw ("Instappen!").
 */
@EventBusSubscriber(modid = Guhs.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReisguhFluitClient {
    public static final net.minecraft.resources.ResourceLocation MODEL = Guhs.id("geo/entity/guh_npc_reisguh.geo.json");
    /** Entity id -> client game time when he started blowing. */
    private static final Map<Integer, Long> START = new ConcurrentHashMap<>();
    /** Where the whistle goes (model units, from its place on his chest) and how far it turns (mouthpiece into the mouth). */
    static final float OMHOOG = 6.6f, NAAR_VOREN = 3.75f, DRAAI = Mth.HALF_PI;

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.REISGUH, MODEL);
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.REISGUH, (npc, state, bot) -> {
            float k = blaas(npc.getId(), state.getPartialTick());
            bot.apply("reis_fluitje").ifPresent(b -> {
                b.setPosY(OMHOOG * k);
                b.setPosZ(-NAAR_VOREN * k);
                b.setRotX(DRAAI * k);
            });
            bot.apply("head").ifPresent(b -> b.setScaleX(1 + 0.05f * k));   // puffed cheeks (absolute: nothing else scales it)
            if (k <= 0) {
                return;
            }
            bot.apply("head").ifPresent(b -> {
                b.setRotY(b.getRotY() * (1 - k));                    // he looks straight ahead while blowing
                b.setRotX(b.getRotX() * (1 - k) - 0.12f * k);        // and leans back a little
            });
            bot.apply("arm_left").ifPresent(b -> {                   // "Instappen!": a paw up high
                b.setRotX(-2.5f * k);
                b.setRotZ(-0.35f * k + 0.15f * k * Mth.sin((float) state.getAnimationTick() * 0.6f));
            });
        });
    }

    /** A whistle message came in: start the animation. */
    public static void fluit(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            START.put(entityId, level.getGameTime());
        }
    }

    /** 0 = whistle on his chest, 1 = in his mouth: up in 5 ticks, blowing, down in the last 7. */
    static float blaas(int entityId, double partialTick) {
        Long start = START.get(entityId);
        var level = Minecraft.getInstance().level;
        if (start == null || level == null) {
            return 0;
        }
        float t = (float) (level.getGameTime() - start + partialTick);
        if (t < 0 || t > ReisguhFluit.DUUR) {
            if (t > ReisguhFluit.DUUR + 40) {
                START.remove(entityId);
            }
            return 0;
        }
        float up = Math.min(1, t / 5f), down = Math.min(1, (ReisguhFluit.DUUR - t) / 7f);
        float k = Math.min(up, down);
        return k * k * (3 - 2 * k);
    }

    private ReisguhFluitClient() {
    }
}
