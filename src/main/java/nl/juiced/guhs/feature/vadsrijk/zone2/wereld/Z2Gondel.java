package nl.juiced.guhs.feature.vadsrijk.zone2.wereld;

import java.util.function.BiPredicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;

/**
 * The gondola of Kandijkom: from the valley station in town (NE) up to the top station on the Galmtop shoulder in the
 * Galmhellingen, and back. A ride scene (non-click camera gliding along the real cable, past the two pylons and the cabins
 * hanging on it); the player is moved to the other station at the start, like the airship.
 * Start: right-click the bell at a station (or {@link #rijd}). {@link #MAG_RIJDEN}: an npc/quest gate (default: always).
 */
public final class Z2Gondel {
    public static final int DUUR = 240;
    public static BiPredicate<ServerPlayer, Boolean> MAG_RIJDEN = (p, omhoog) -> true;

    private Z2Gondel() {
    }

    /** Ride up (true: valley -&gt; Galmtop) or down. */
    public static boolean rijd(ServerPlayer p, boolean omhoog) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return false;
        }
        if (!MAG_RIJDEN.test(p, omhoog)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.z2.gondel.dicht").withStyle(ChatFormatting.AQUA));
            return false;
        }
        Vec3 van = Vec3.atCenterOf(omhoog ? Z2WereldPlekken.KABEL_DAL : Z2WereldPlekken.KABEL_BERG);
        Vec3 naar = Vec3.atCenterOf(omhoog ? Z2WereldPlekken.KABEL_BERG : Z2WereldPlekken.KABEL_DAL);
        BlockPos aankomst = omhoog ? Z2WereldPlekken.GONDEL_BERG : Z2WereldPlekken.GONDEL_DAL;
        Vec3 richting = naar.subtract(van).normalize();
        Vec3 opzij = new Vec3(-richting.z, 0, richting.x).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-richting.x, richting.z));
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.2f, 1.4f);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(aankomst), Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        // the camera hangs in a cabin under the cable: 5 m below, slightly to the side, looking ahead and a bit down
        RijkScene.Builder b = RijkScene.maak("vr_z2_gondel", DUUR);
        for (int i = 0; i <= 6; i++) {
            double t = i / 6.0;
            int tick = (int) Math.round(t * (DUUR - 2));
            Vec3 c = van.lerp(naar, t).add(0, -6, 0).add(opzij.scale(1.5));
            Vec3 kijk = c.add(richting.scale(30)).add(opzij.scale(i % 2 == 0 ? 6 : -6)).add(0, -6, 0);
            if (i == 6) {
                kijk = Vec3.atBottomCenterOf(aankomst).add(richting.scale(8)).add(0, 1, 0);
            }
            b.camera(tick, c, kijk);
        }
        b.titel("vadsrijk.wereld.z2.gondel.titel", omhoog ? "vadsrijk.wereld.z2.gondel.omhoog" : "vadsrijk.wereld.z2.gondel.omlaag", 30, 80, 0xBFE8FF);
        RijkScenes.speel(p, b.bouw());
        for (int t = 0; t < DUUR; t += 40) {
            Later.na(t, () -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.MINECART_INSIDE, SoundSource.AMBIENT, 0.25f, 0.7f));
        }
        Later.na(DUUR - 10, () -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1f, 1.6f));
        return true;
    }

    /** Right-click on a station bell. */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        if (Z2WereldPlekken.GONDEL_DAL_KNOP.distManhattan(pos) <= 1) {
            rijd(p, true);
            return true;
        }
        if (Z2WereldPlekken.GONDEL_BERG_KNOP.distManhattan(pos) <= 1) {
            rijd(p, false);
            return true;
        }
        return false;
    }
}
