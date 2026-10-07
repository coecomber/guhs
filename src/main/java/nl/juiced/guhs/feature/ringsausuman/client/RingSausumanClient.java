package nl.juiced.guhs.feature.ringsausuman.client;

import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.client.CastAnimaties;
import nl.juiced.guhs.feature.ring.client.CastAnimaties.Lijf;
import nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * Client side of bbq2 (ring-sausuman): Sausuman's own look (the Mika's model with his beard, hair, stained robe and fork
 * staff: tools/features/ring_sausuman_modellen.py) and how he moves.
 * <ul>
 *   <li>In the baking scene he plays the named cutscene animations of the Knabbelring's cast ({@link CastAnimaties}, on four
 *       paws), plus one of his own: {@code mok}, sulking (head down, ears flat, a slow sigh).</li>
 *   <li>Outside a scene he sulks for every viewer whose own questline has passed the baking (the synced step of
 *       {@code ring_sausuman} is 3 or more): he sits with his back to that player, facing his Mokhoek. Once that player gave
 *       him his bite (done) he peeks over his shoulder now and then. Everybody else still sees him waiting by his machine:
 *       the same entity, a different pose per viewer.</li>
 *   <li>His beard sways a little.</li>
 * </ul>
 */
public final class RingSausumanClient {
    /** The step of the questline from which he sulks (the baking scene is behind the viewer). */
    private static final int MOKT_VANAF = 3;

    public static void init(IEventBus modBus) {
        GuhNpcEntity.Kind kind = GuhNpcEntity.Kind.SAUSUMAN;
        SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_sausuman"));
        SittingGuhRenderers.NPC_ANIMATIES.put(kind, Guhs.id("entity/guh_npc_sausuman"));
        SittingGuhRenderers.NPC_ANIMATORS.put(kind, RingSausumanClient::animeer);
    }

    private static GuhRenderFrame.BoneMove animeer(GuhNpcEntity npc, double tick) {
        float t = (float) tick;
        String naam = Cutscenes.animatie(npc);
        GuhRenderFrame.BoneMove move;
        if (naam.equals("mok")) {
            move = mok(Cutscenes.animatieTicks(npc) + (t - Mth.floor(t)), t, false, false);
        } else if (!naam.isEmpty()) {
            move = CastAnimaties.maak(naam, Cutscenes.animatieTicks(npc) + (t - Mth.floor(t)), Lijf.VIERPOOT);
        } else if (!inScene() && stap() >= MOKT_VANAF) {
            move = mok(100f, t, true, stap() >= RingSausumanFeature.LIJN.stappen());
        } else {
            move = null;
        }
        // a scene walks him to his machine and back: he hops along
        double dx = npc.getX() - npc.xo, dz = npc.getZ() - npc.zo;
        float snelheid = (float) Math.sqrt(dx * dx + dz * dz);
        if (snelheid > 0.01f) {
            move = CastAnimaties.samen(move, CastAnimaties.huppel(t, snelheid));
        }
        float wuif = Mth.sin(t * 0.05f) * 0.03f;
        return CastAnimaties.samen(move, b -> b.ifPresent("sausuman_baard", s -> s.setRotX(s.getRotX() + wuif)));
    }

    /** Is the viewer watching a scene (then the actor does what the scene says, also in a replay after the questline)? */
    private static boolean inScene() {
        var speler = net.minecraft.client.Minecraft.getInstance().player;
        return speler != null && Cutscenes.bezig(speler);
    }

    /** The viewer's own step of the tower's questline (-1: not known yet). */
    private static int stap() {
        return Verhaallijn.stapClient(RingSausumanFeature.LIJN.id());
    }

    /**
     * Sulking: the head sinks (over {@code sinds} ticks), the ears lie flat, every few seconds a deep sigh lifts and drops the
     * body. {@code omgedraaid}: he has turned his back on the viewer (the whole model turns half a circle); {@code gluurt}:
     * every now and then he peeks over his shoulder, and looks away again at once.
     */
    private static GuhRenderFrame.BoneMove mok(float sinds, float t, boolean omgedraaid, boolean gluurt) {
        float k = Mth.clamp(sinds / 16f, 0f, 1f);
        k = k * k * (3 - 2 * k);
        float zucht = (float) Math.pow(Math.max(0f, Mth.sin(t * Mth.TWO_PI / 90f)), 6);
        float fase = (t % 260f) / 260f;
        float gluur = gluurt && fase > 0.8f ? Mth.sin((fase - 0.8f) / 0.2f * Mth.PI) : 0f;
        float kk = k;
        return b -> {
            if (omgedraaid) {
                b.ifPresent("root", s -> s.setRotY(s.getRotY() + Mth.PI));
            }
            b.ifPresent("head", s -> s.setRotX(s.getRotX() - 0.34f * kk + 0.05f * zucht).setRotY(s.getRotY() + 0.95f * gluur));
            b.ifPresent("body", s -> s.setScaleY(1f + 0.05f * zucht));
            b.ifPresent("ear_left", s -> s.setRotZ(s.getRotZ() - 0.6f * kk));
            b.ifPresent("ear_right", s -> s.setRotZ(s.getRotZ() + 0.6f * kk));
            b.ifPresent("tail", s -> s.setRotX(s.getRotX() - 0.25f * kk));
        };
    }

    private RingSausumanClient() {
    }
}
