package nl.juiced.guhs.feature.vadsrijk.wereld;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Viewpoints on the rock needles (design ronde 6 + 11: no teleport, only the view). The first time you reach the top platform
 * of one: a 10 s panorama scene (the camera rises and turns once round the needle, looking far out) with the needle's name
 * as a title card. Seen = {@code SpelerData.gezien "vr_w_uitkijk_<id>"}.
 */
public final class Uitkijken {
    private Uitkijken() {
    }

    static void tick(ServerLevel sl) {
        if (sl.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (RijkScenes.bezig(p)) {
                continue;
            }
            for (WereldPlekken.Uitkijk u : WereldPlekken.UITKIJKEN) {
                if (p.position().distanceToSqr(Vec3.atBottomCenterOf(u.top())) < 36) {
                    SpelerData d = Spelers.data(p);
                    if (d.gezien.add("vr_w_uitkijk_" + u.id())) {
                        panorama(p, u);
                    }
                }
            }
        }
    }

    public static void panorama(ServerPlayer p, WereldPlekken.Uitkijk u) {
        Vec3 top = Vec3.atBottomCenterOf(u.top()).add(0, 2, 0);
        RijkScene.Builder b = RijkScene.maak("vr_w_uitkijk_" + u.id(), 200);
        float start = p.getYRot();
        for (int i = 0; i <= 8; i++) {
            double a = Math.toRadians(start + 90 + i * 45);
            Vec3 dir = new Vec3(-Math.sin(a), 0, Math.cos(a));
            Vec3 cam = top.add(dir.scale(-3)).add(0, 1 + i * 0.9, 0);
            b.camera(i * 25 - (i == 8 ? 2 : 0), cam, cam.add(dir.scale(80)).add(0, -18, 0));
        }
        b.titel("vadsrijk.wereld.uitkijk." + u.id(), "vadsrijk.wereld.uitkijk.onder", 40, 90, 0xFFE3A0);
        RijkScenes.speel(p, b.bouw());
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        p.sendSystemMessage(Component.translatable("vadsrijk.wereld.uitkijk.gevonden", Component.translatable("vadsrijk.wereld.uitkijk." + u.id())));
    }

    static void markeerBinnen(ServerPlayer p) {
        if (Vadsrijk.in(p)) {
            Spelers.data(p).gezien.add(Portalen.BINNEN_GEWEEST);
        }
    }
}
