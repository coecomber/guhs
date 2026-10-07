package nl.juiced.guhs.feature.guhpixel.reisbureau.client;

import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.reisbureau.ReisbureauPayloads;
import nl.juiced.guhs.feature.guhpixel.reisbureau.ReisbureauSlice;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Client side of the guhpixel slice "reisbureau": the trip screen of the Reisbalie, the reading screen of an ansichtkaart,
 * and the little suitcase a guh carries next to it while it walks off on its trip (the synced flag PxVlaggen.KOFFER).
 */
public final class ReisbureauClient {
    public static void init(IEventBus modBus) {
        ReisbureauPayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ReisScherm s) {
                s.update(p.data());
            } else {
                mc.setScreen(new ReisScherm(p.data()));
            }
        });
        ReisbureauPayloads.kaartOpener = p -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new KaartScherm(p.data())));
        // extract: the item's render state and the numbers; submit: the pose and the item (never the entity itself)
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!GuhHooks.heeft(guh, PxVlaggen.KOFFER)) {
                return;
            }
            ItemStackRenderState item = GuhRenderer.itemState(new ItemStack(ReisbureauSlice.SOUVENIR_ITEMS.get("koffertje").get()), ItemDisplayContext.GROUND, guh);
            float yaw = Mth.rotLerp(partialTick, guh.yBodyRotO, guh.yBodyRot);
            float t = guh.tickCount + partialTick;
            double zij = guh.getBbWidth() / Math.max(0.2f, guh.getScale()) * 0.5 + 0.28;
            double hup = 0.05 + Math.abs(Math.sin(t * 0.5)) * 0.06;
            frame.layerExtra((pose, collector, light) -> {
                pose.mulPose(Axis.YP.rotationDegrees(180f - yaw));
                pose.translate(zij, hup, 0.05);
                pose.scale(1.1f, 1.1f, 1.1f);
                item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            });
        });
    }

    private ReisbureauClient() {
    }
}
