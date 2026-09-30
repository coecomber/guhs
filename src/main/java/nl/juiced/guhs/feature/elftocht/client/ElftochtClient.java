package nl.juiced.guhs.feature.elftocht.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.elftocht.ElftochtPayloads;

/**
 * Client side of the Elf-Guhjestocht: Schaatsmeester Guhglij's model (orange pompom hat with a whistle and a pair of
 * skates round his neck; the pompom bounces), the Stempelguhs' model (each village its own hat and scarf, a stamp in
 * the paw that goes PLOF), his screen ({@link SchaatsmeesterScherm}) and the skating itself ({@link SchaatsEffecten}:
 * the skates on your feet, the skater's sway, the hiss of the blades, ice dust).
 */
public final class ElftochtClient {
    /** Stempelguh entity id -> game time of its last stamp (the PLOF animation). */
    static final Map<Integer, Long> PLOFFEN = new ConcurrentHashMap<>();
    static final String STEMPEL_NAAM = "gui.guhs.elftocht.stempelguh.";
    public static final int PLOF_TICKS = 14;

    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.SCHAATSMEESTERGUH, Guhs.id("geo/entity/guh_npc_schaatsmeesterguh.geo.json"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.STEMPELGUH, Guhs.id("geo/entity/guh_npc_stempelguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.SCHAATSMEESTERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.12f;
            bot.apply("schaatsmeester_pompon").ifPresent(b -> {
                b.setRotZ((float) Math.sin(t) * 0.25f);
                b.setRotX((float) Math.cos(t * 0.7f) * 0.15f);
            });
            bot.apply("schaatsmeester_schaatsen").ifPresent(b -> b.setRotZ((float) Math.sin(t * 0.5f) * 0.08f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.STEMPELGUH, (npc, state, bot) -> {
            int dorp = dorp(npc);
            for (int n = 1; n <= 11; n++) {
                int m = n;
                bot.apply("stempel_hoed_" + n).ifPresent(b -> b.setHidden(m != dorp));
                bot.apply("stempel_sjaal_" + n).ifPresent(b -> b.setHidden(m != dorp));
            }
            Long start = PLOFFEN.get(npc.getId());
            float p = start == null || npc.level() == null ? 1f
                    : ((npc.level().getGameTime() - start) + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) / PLOF_TICKS;
            float rot = plofHoek(p);
            bot.apply("arm_right").ifPresent(b -> b.setRotX(b.getInitialSnapshot().getRotX() + rot));
            bot.apply("stempel_stempel").ifPresent(b -> b.setRotX(-rot * 0.3f));
            if (p >= 1f && start != null) {
                PLOFFEN.remove(npc.getId());
            }
        });
        modBus.addListener(ElftochtClient::layers);
        NeoForge.EVENT_BUS.register(SchaatsEffecten.class);
    }

    /** The stamping arm: up (0..0.45), PLOF down (0.45..0.6), a little bounce back (0.6..1). Radians, 0 = at rest. */
    public static float plofHoek(float p) {
        if (p <= 0f || p >= 1f) {
            return 0f;
        }
        if (p < 0.45f) {
            return -1.3f * (p / 0.45f);
        }
        if (p < 0.6f) {
            float q = (p - 0.45f) / 0.15f;
            return -1.3f + 1.6f * q;
        }
        float q = (p - 0.6f) / 0.4f;
        return 0.3f * (1f - q);
    }

    /** The village of a Stempelguh on this side: from its name ("gui.guhs.elftocht.stempelguh.&lt;n&gt;"); 1 when unknown. */
    static int dorp(GuhNpcEntity npc) {
        Component name = npc.getCustomName();
        if (name != null && name.getContents() instanceof TranslatableContents tc && tc.getKey().startsWith(STEMPEL_NAAM)) {
            try {
                return Integer.parseInt(tc.getKey().substring(STEMPEL_NAAM.length()));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof AvatarRenderer renderer) {
                renderer.addLayer(new SchaatsLaag(renderer));
            }
        }
    }

    /** guhs:elftocht_open */
    public static void open(ElftochtPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new SchaatsmeesterScherm(payload.npcId(), payload.data()));
    }

    /** guhs:elftocht_plof */
    public static void plof(int entityId) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            PLOFFEN.put(entityId, level.getGameTime());
        }
    }

    private ElftochtClient() {
    }
}
