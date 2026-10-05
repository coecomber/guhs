package nl.juiced.guhs.feature.guhpixel.bioscoop.client;

import com.mojang.math.Axis;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.bioscoop.BioscoopPayloads;
import nl.juiced.guhs.feature.guhpixel.bioscoop.BioscoopSlice;
import nl.juiced.guhs.feature.guhpixel.bioscoop.Doek;
import nl.juiced.guhs.feature.guhpixel.bioscoop.ProjectorBlockEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Client side of the guhpixel slice "bioscoop": the film on the screen ({@link ProjectorRenderer}) and its sound cues,
 * the projector screen ({@link ProjectorScherm}), and how a guh in a cinema seat looks: a bakje popcorn in front of it
 * (flag POPCORN; it nibbles) and the little hop when the film startles it (flag SCHRIK; the popcorn jumps along).
 */
public final class BioscoopClient {
    /** When each startled guh (entity id) was first seen startled (its age then), for the hop. */
    private static final Int2FloatOpenHashMap SCHRIK_SINDS = new Int2FloatOpenHashMap();

    public static void init(IEventBus modBus) {
        BioscoopPayloads.opener = p -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ProjectorScherm s && s.pos() == p.data().getLongOr("Pos", 0L)) {
                s.update(p.data());
            } else {
                mc.setScreen(new ProjectorScherm(p.data()));
            }
        });
        ProjectorBlockEntity.clientTikker = BioscoopClient::tik;
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(BioscoopSlice.PROJECTOR_BE.get(), ProjectorRenderer::new));
        modBus.addListener((AddClientReloadListenersEvent event) ->
                event.addListener(Guhs.id("guhbioscoop_films"), (ResourceManagerReloadListener) rm -> FilmData.vergeet()));
        GuhRenderer.hook((guh, partialTick, frame) -> {
            boolean schrik = GuhHooks.heeft(guh, PxVlaggen.SCHRIK);
            float hop = 0f;
            if (schrik) {
                float nu = guh.tickCount + partialTick;
                float sinds = SCHRIK_SINDS.containsKey(guh.getId()) ? SCHRIK_SINDS.get(guh.getId()) : nu;
                if (nu < sinds || nu - sinds > 40) {
                    sinds = nu;
                }
                SCHRIK_SINDS.put(guh.getId(), sinds);
                float f = Mth.clamp((nu - sinds) / 9f, 0f, 1f);
                hop = Mth.sin(f * Mth.PI) * 0.32f;
                final float h = hop;
                frame.pose(pose -> pose.translate(0, h, 0));
            } else if (!SCHRIK_SINDS.isEmpty()) {
                SCHRIK_SINDS.remove(guh.getId());
            }
            if (!GuhHooks.heeft(guh, PxVlaggen.POPCORN)) {
                return;
            }
            float t = guh.tickCount + partialTick;
            float lijf = Mth.rotLerp(partialTick, guh.yBodyRotO, guh.yBodyRot);
            float maat = Math.max(0.2f, guh.getScale());
            // in front of its snout; now and then it dips its nose in (a nibble), a fright throws the bakje up
            double hap = Math.max(0, Math.sin(t * 0.21)) * 0.03;
            double omhoog = 0.3 * maat + hap + hop * 1.6;
            double voor = 0.5 * maat;
            ItemStackRenderState item = GuhRenderer.itemState(new ItemStack(BioscoopSlice.POPCORN.get()), ItemDisplayContext.GROUND, guh);
            float kantel = schrik ? (t * 40f) % 360f : 0f;
            frame.extra((pose, collector, light) -> {
                pose.mulPose(Axis.YP.rotationDegrees(-lijf));
                pose.translate(0, omhoog, voor);
                pose.mulPose(Axis.XP.rotationDegrees(kantel));
                pose.scale(0.7f * maat, 0.7f * maat, 0.7f * maat);
                item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            });
        });
    }

    /** (Client tick of a projector) plays the sound cues of the running film at the screen, each one once. */
    private static void tik(ProjectorBlockEntity be) {
        Level level = be.getLevel();
        Doek doek = be.doek();
        if (level == null || doek == null || !be.speelt()) {
            be.geluidFilm = "";
            be.geluidTot = -1;
            return;
        }
        FilmData film = FilmData.laad(be.film());
        if (film == null) {
            return;
        }
        int t = (int) (level.getGameTime() - be.start());
        String sleutel = be.film() + "@" + be.start();
        if (!sleutel.equals(be.geluidFilm) || t < be.geluidTot) {
            be.geluidFilm = sleutel;
            be.geluidTot = t <= 12 ? -1 : t - 1;        // (who walks in half-way does not get the old sounds all at once)
        }
        if (t <= be.geluidTot) {
            return;
        }
        Vec3 m = doek.midden();
        for (FilmData.Geluid g : film.geluiden) {
            if (g.t() > be.geluidTot && g.t() <= t) {
                level.playLocalSound(m.x, m.y, m.z, geluid(g.id()), SoundSource.RECORDS, g.volume() * 1.6f, g.toon(), false);
            }
        }
        be.geluidTot = t;
    }

    private static SoundEvent geluid(Identifier id) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(id).orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
    }

    private BioscoopClient() {
    }
}
