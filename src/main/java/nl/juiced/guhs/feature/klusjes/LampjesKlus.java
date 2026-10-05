package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeGoal;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.knus.Dagdeel;

/**
 * Lampjes 's avonds: in the evening (Dagdeel AVOND, by the overworld clock like the huisje itself) the resident walks
 * round the lamps of the home base (#guhs:klusjes/lampjes: guhlampjes and candles) and switches them on, a few per
 * trip; in the morning (OCHTEND) it switches them off again, with a big yawn at the first one. Guhs only.
 */
public class LampjesKlus extends BasisKlus {
    public static final int PER_KEER = 6;

    LampjesKlus() {
        super("lampjes", () -> new ItemStack(KlusjesFeature.GUHLAMPJE_ITEM.get()), 100);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    /** The day part of this huisje (the tests' override, else the overworld clock). */
    static Dagdeel dagdeel(ServerLevel level, Huisje h) {
        Dagdeel test = HuisjeGoal.TEST_DAGDEEL.get(h.pos());
        return test != null ? test : Dagdeel.van(nl.juiced.guhs.world.GuhTime.dayTime(level.getServer().overworld()));
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<BlockPos> lampen = KlusGebied.van(level, huisje, KlusGebied.Soort.LAMP);
        if (lampen.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        Dagdeel d = dagdeel(level, huisje);
        if (d != Dagdeel.AVOND && d != Dagdeel.OCHTEND) {
            return KlusStand.straks("tijd", lampen.size());
        }
        boolean aan = d == Dagdeel.AVOND;
        int n = (int) lampen.stream().filter(p -> level.isLoaded(p) && moet(level, p, aan)).count();
        return n > 0 ? KlusStand.ja(aan ? "aan" : "uit", n) : KlusStand.straks("klaar", lampen.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        Dagdeel d = dagdeel(level, huisje);
        if (d != Dagdeel.AVOND && d != Dagdeel.OCHTEND) {
            return null;
        }
        boolean aan = d == Dagdeel.AVOND;
        List<BlockPos> lampen = new ArrayList<>();
        for (BlockPos p : KlusGebied.van(level, huisje, KlusGebied.Soort.LAMP)) {
            if (moet(level, p, aan) && !KlusGebied.geclaimd(level, p)) {
                lampen.add(p);
            }
        }
        if (lampen.isEmpty()) {
            return null;
        }
        // a little round: the nearest, then each time the nearest to that one
        List<BlockPos> rondje = new ArrayList<>();
        BlockPos bij = bewoner.blockPosition();
        while (!lampen.isEmpty() && rondje.size() < PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(bewoner)) {
            BlockPos van = bij;
            BlockPos volgende = lampen.stream().min(Comparator.comparingDouble(p -> p.distSqr(van))).get();
            lampen.remove(volgende);
            rondje.add(volgende);
            bij = volgende;
        }
        return new Taak(level, huisje, bewoner, rondje, aan);
    }

    /** Does this lamp still need switching (on in the evening, off in the morning)? */
    static boolean moet(ServerLevel level, BlockPos p, boolean aan) {
        BlockState s = level.getBlockState(p);
        if (!KlusGebied.lampje(s) || s.getValue(BlockStateProperties.LIT) == aan) {
            return false;
        }
        return !aan || !s.hasProperty(BlockStateProperties.WATERLOGGED) || !s.getValue(BlockStateProperties.WATERLOGGED);
    }

    private class Taak extends StappenTaak {
        private final List<BlockPos> rondje;
        private final boolean aan;
        private boolean gegaapt;

        Taak(ServerLevel level, Huisje huisje, Mob mob, List<BlockPos> rondje, boolean aan) {
            super(level, huisje, mob, LampjesKlus.this);
            this.rondje = rondje;
            this.aan = aan;
        }

        @Override
        protected void begin() {
            for (BlockPos p : rondje) {
                claim(p, 600);
                erbij(loop(p, 1.8));
                erbij(werk(8, Vec3.atCenterOf(p), null));
                erbij(doe(() -> knip(p)));
            }
        }

        private void knip(BlockPos p) {
            if (!moet(level, p, aan) || !GuhlampjeBlock.zet(level, p, aan)) {
                return;
            }
            aantal++;
            gelukt();
            sprankel(Vec3.atCenterOf(p).add(0, 0.3, 0), aan ? 4 : 1);
            if (!aan && !gegaapt && mob instanceof GuhEntity g) {
                gegaapt = true;                                      // (morning: the first lamp off with a big yawn)
                g.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF);
            }
        }
    }
}
