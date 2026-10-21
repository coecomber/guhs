package nl.juiced.guhs.feature.knabbelvelden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.hemel.HemelFeature;
import nl.juiced.guhs.feature.knabbelvelden.Geesten;
import nl.juiced.guhs.feature.knabbelvelden.Knabbelvelden;

/**
 * Client side of De Eeuwige Knabbelvelden: the retired guhs' ghosts (see-through, a golden halo, floating a little and
 * twinkling now and then), the big Knabbelportaal screen ({@link KnabbelScherm}) and the "Mee naar huis?" question
 * ({@link MeeNaarHuisScherm}).
 */
public final class KnabbelveldenClient {
    public static void init(IEventBus modBus) {
        GuhRenderer.doorzichtig(Geesten::isGeest);
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!Geesten.isGeest(guh)) {
                return;
            }
            // the halo: the bones of the hemel aureooltje, see-through and full bright
            frame.pass(RenderTypes.entityTranslucent(GuhClothes.HEMEL_AUREOOLTJE.texture()), 0xEEFFFFFF, true, GuhClothes.HEMEL_AUREOOLTJE::shows);
            float t = guh.tickCount + guh.getId() * 7 + partialTick;
            float zweef = 0.08f + 0.06f * Mth.sin(t * 0.07f);
            frame.pose(pose -> pose.translate(0, zweef, 0));
        });
        NeoForge.EVENT_BUS.addListener(KnabbelveldenClient::glinster);
    }

    /** Now and then a ghost twinkles. */
    private static void glinster(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || mc.player == null || level.dimension() != Knabbelvelden.DIM || level.getGameTime() % 5 != 0) {
            return;
        }
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof GuhEntity g && Geesten.isGeest(g) && g.distanceToSqr(mc.player) < 48 * 48 && level.getRandom().nextInt(4) == 0) {
                level.addParticle(HemelFeature.STERRETJE.get(), g.getX() + (level.getRandom().nextDouble() - 0.5) * 1.2,
                        g.getY() + 0.4 + level.getRandom().nextDouble() * 1.2, g.getZ() + (level.getRandom().nextDouble() - 0.5) * 1.2, 0, 0.01, 0);
            }
        }
    }

    /** guhs:knabbelvelden_open: the big screen (opened, or updated when it is open already). */
    public static void open(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof KnabbelScherm scherm) {
            scherm.update(data);
        } else {
            mc.setScreen(new KnabbelScherm(data));
        }
    }

    /** guhs:knabbelvelden_vraag: "Mee naar huis?" for a ghost you sneak-clicked. */
    public static void vraag(CompoundTag data) {
        Minecraft.getInstance().setScreen(new MeeNaarHuisScherm(data));
    }

    private KnabbelveldenClient() {
    }
}
