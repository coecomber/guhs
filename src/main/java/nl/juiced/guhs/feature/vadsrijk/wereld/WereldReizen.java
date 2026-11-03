package nl.juiced.guhs.feature.vadsrijk.wereld;

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

/**
 * The airship line Knuffelkamp &lt;-&gt; De Haven (design ronde 11: luchtschip-lijnen, no fast travel otherwise).
 * Right-click the ship's bell at a dock (or let an NPC call {@link #vlieg}): a non-click flight scene, then you stand on the
 * other dock. The player is moved at the start (so the client has the chunks of the arrival around it) and the camera glides
 * in from the sea towards the arrival dock.
 * <p>
 * {@link #MAG_VLIEGEN}: the npc slice may lock the line until the camp story is done (default: always open).
 */
public final class WereldReizen {
    public enum Dok {
        KAMP, HAVEN;

        public BlockPos aankomst() {
            return this == KAMP ? WereldPlekken.KAMP_LANDING : WereldPlekken.HAVEN_LANDING;
        }

        public BlockPos bel() {
            return this == KAMP ? WereldPlekken.KAMP_BEL : WereldPlekken.HAVEN_BEL;
        }

        public Dok ander() {
            return this == KAMP ? HAVEN : KAMP;
        }
    }

    /** May this player fly from -> to? Default: yes. The npc slice may replace this. */
    public static BiPredicate<ServerPlayer, Dok> MAG_VLIEGEN = (p, naar) -> true;

    private WereldReizen() {
    }

    /** Fly this player from one dock to the other (the scene takes 9 s). */
    public static void vlieg(ServerPlayer p, Dok van, Dok naar) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return;
        }
        if (!MAG_VLIEGEN.test(p, naar)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.luchtschip.nog_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Vec3 a = Vec3.atBottomCenterOf(naar.aankomst());
        Vec3 b = Vec3.atBottomCenterOf(van.aankomst());
        Vec3 richting = a.subtract(b).multiply(1, 0, 1).normalize();
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.4f, 1.1f);
        float yaw = (float) Math.toDegrees(Math.atan2(-richting.x, richting.z));
        p.teleport(new TeleportTransition(sl, a, Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        Vec3 ver = a.subtract(richting.scale(110)).add(0, 46, 0);
        Vec3 midden = a.subtract(richting.scale(55)).add(new Vec3(-richting.z, 0, richting.x).scale(18)).add(0, 28, 0);
        RijkScene s = RijkScene.maak("vr_w_luchtschip", 180)
                .camera(0, ver, ver.add(richting.scale(40)).add(0, -6, 0))
                .camera(90, midden, a.add(0, 6, 0))
                .camera(178, a.subtract(richting.scale(6)).add(0, 3, 0), a.add(richting.scale(10)))
                .titel("vadsrijk.wereld.luchtschip.titel." + naar.name().toLowerCase(), "vadsrijk.wereld.luchtschip.onder", 120, 55, 0x9AD5FF)
                .bouw();
        RijkScenes.speel(p, s);
        sl.playSound(null, p.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 0.5f, 1.2f);
    }

    /** Right-click on a block: is it one of the two ships' bells? */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        for (Dok d : Dok.values()) {
            if (d.bel().distManhattan(pos) <= 1) {
                vlieg(p, d, d.ander());
                return true;
            }
        }
        return false;
    }
}
