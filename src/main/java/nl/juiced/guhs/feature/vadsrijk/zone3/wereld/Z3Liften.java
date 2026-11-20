package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.ViaRoutes;
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
        // 1.10.10: the lift's own look (the chains, the fence ring round the shaft hole, the honeycomb landing) counts from further
        // away: that is what a player sees as "the lift". The chains hang ~3.6 blocks from the top stop and ~6.3 from the bottom
        // one. Nothing else to click on those blocks.
        // (holding a lead: a fence click ties a mob, leave it)
        if (p.getMainHandItem().is(Items.LEAD) || !liftBlok(p.level().getBlockState(pos))) {
            return false;
        }
        Z3WereldPlekken.Lift beste = null;
        boolean omhoog = false;
        double bd = Double.MAX_VALUE;
        for (Z3WereldPlekken.Lift l : Z3WereldPlekken.LIFTEN) {
            for (boolean onder : new boolean[] {false, true}) {
                BlockPos s = onder ? l.onder() : l.boven();
                int dy = pos.getY() - s.getY();
                double h = sq(pos.getX() - s.getX()) + sq(pos.getZ() - s.getZ());
                if (dy >= -3 && dy <= 8 && h <= KLIK_RUIM * KLIK_RUIM && h < bd) {
                    beste = l;
                    omhoog = onder;
                    bd = h;
                }
            }
        }
        return beste != null && rijd(p, beste, omhoog);
    }

    /** 1.10.10: how far (horizontally) a click on a lift block still rides the nearest stop. */
    static final double KLIK_RUIM = 7.0;

    /** The blocks the lift shafts are made of (tools/vadsrijk/zone3/bouw3.py: chains, spruce fence ring, honeycomb landing). */
    static boolean liftBlok(BlockState s) {
        return s.is(Blocks.IRON_CHAIN) || s.is(BlockTags.WOODEN_FENCES) || s.is(Blocks.HONEYCOMB_BLOCK);
    }

    private static double sq(double v) {
        return v * v;
    }

    /**
     * 1.10.10: the three honeycomb cells under the ring deck as tracker "via" routes ({@link ViaRoutes}). The Nestkapel is reached
     * ONLY by the kapellift (bereikcheck --via); the cafe and the Raattrossen can also be walked into along a branch, so their lift
     * is the route only from the deck. Cell shape from plan3.py CELLEN (angle, R, height): the cell's middle is its
     * {@code gebouw.<cel>.binnen} spot, the floor stand y; the room reaches up to just under the deck (stand 167).
     */
    public static void viaRoutes() {
        ViaRoutes.Gebied dek = ViaRoutes.cilinder(Z3WereldPlekken.STAM_X + 0.5, Z3WereldPlekken.STAM_Z + 0.5, 160, 165, 215);
        via("z3_nestkapel", "nestkapel", 10, "lift_kapel", null);
        via("z3_cafe", "cafe", 9, "lift_cafe", dek);
        via("z3_raattros", "raattros", 6, "lift_raattros", dek);
    }

    private static void via(String id, String cel, double r, String liftId, @Nullable ViaRoutes.Gebied van) {
        BlockPos m = Z3WereldPlekken.plek("gebouw." + cel + ".binnen");
        Z3WereldPlekken.Lift l = lift(liftId);
        if (m == null || l == null) {
            return;
        }
        double cx = m.getX() + 0.5, cz = m.getZ() + 0.5;
        int dekY = l.boven().getY();
        ViaRoutes.Gebied gebied = ViaRoutes.cilinder(cx, cz, r + 1, m.getY() - 2, dekY - 3);
        ViaRoutes.Gebied binnen = gebied.of(ViaRoutes.cilinder(l.onder().getX() + 0.5, l.onder().getZ() + 0.5, 4, l.onder().getY() - 2, dekY - 3));
        ViaRoutes.voegToe(new ViaRoutes.Via(id, gebied, binnen, van, l.boven(), "vadsrijk.npc.stap.via." + id));
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
