package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;

/**
 * The lifts of zone 3 (ZONE3_PLAN §3): honey bucket lifts in Honingtak (deck &lt;-&gt; cafe / Nestkapel / Raattros), the big
 * Honinglift (Honingtak &lt;-&gt; Mosvoet, opens after z3_hq09) and the two wooden lifts in the Schorsstad. Right-click a block
 * next to a stop (within ~3 blocks of its stand spot): a short ride scene (camera along the shaft / cable), then you stand at the
 * other stop. Riding, never a free teleport: the camera travels the whole way.
 */
public final class Z3Liften {
    /** The big Honinglift is open (default: the flag {@code z3_honinglift}, set by the npc slice at z3_hq09; creative always). */
    public static Predicate<ServerPlayer> MAG_HONINGLIFT = p -> Questlog.vlag(p, "z3_honinglift");

    private Z3Liften() {
    }

    static boolean klik(ServerPlayer p, BlockPos pos) {
        for (Z3WereldPlekken.Lift l : Z3WereldPlekken.LIFTEN) {
            if (l.boven().distSqr(pos) <= 10) {
                return rijd(p, l, false);
            }
            if (l.onder().distSqr(pos) <= 10) {
                return rijd(p, l, true);
            }
        }
        return false;
    }

    public static Z3WereldPlekken.Lift lift(String id) {
        for (Z3WereldPlekken.Lift l : Z3WereldPlekken.LIFTEN) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /** Ride lift l up (omhoog) or down. */
    public static boolean rijd(ServerPlayer p, Z3WereldPlekken.Lift l, boolean omhoog) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || RijkScenes.bezig(p)) {
            return false;
        }
        boolean groot = l.id().equals("honinglift");
        if (groot && !p.getAbilities().instabuild && !MAG_HONINGLIFT.test(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.z3.honinglift.dicht").withStyle(ChatFormatting.GOLD));
            return true;
        }
        Vec3 van = Vec3.atBottomCenterOf(omhoog ? l.onder() : l.boven());
        Vec3 naar = Vec3.atBottomCenterOf(omhoog ? l.boven() : l.onder());
        double afstand = van.distanceTo(naar);
        int duur = (int) Math.max(60, Math.min(260, afstand * (groot ? 0.6 : 2.2)));
        RijkScene.Builder b = RijkScene.maak("vr_z3_lift_" + l.id(), duur);
        int stappen = groot ? 6 : 3;
        for (int i = 0; i <= stappen; i++) {
            double f = (double) i / stappen;
            Vec3 c = van.lerp(naar, f).add(0, 2.2 - (groot ? 10 * Math.sin(Math.PI * f) : 0), 0);
            Vec3 kijk = groot ? naar.add(0, 1, 0) : c.add(6, omhoog ? 4 : -4, 6);
            b.camera((int) Math.round(f * (duur - 2)), c.add(groot ? 3 : 2.5, 0, groot ? 3 : 2.5), kijk);
        }
        b.titel("vadsrijk.wereld.z3.lift.titel." + (groot ? "honinglift" : l.id().startsWith("schorslift") ? "schorslift" : "honing"),
                omhoog ? "vadsrijk.wereld.z3.lift.omhoog" : "vadsrijk.wereld.z3.lift.omlaag", 10, Math.min(60, duur - 20), 0xFFD27A);
        RijkScenes.speel(p, b.bouw());
        sl.playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 0.8f, 1.1f);
        Later.na(duur / 2, () -> sl.playSound(null, naar.x, naar.y, naar.z, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 1f, 0.8f));
        Z3Reizen.landOp(p, naar, duur);
        return true;
    }
}
