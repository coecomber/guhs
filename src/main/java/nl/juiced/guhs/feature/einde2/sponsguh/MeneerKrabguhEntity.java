package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * einde2, slice sponsguh_wezens: Meneer Krabguh, the red crab guh who owns De Krokante Knabbel and counts his knabbels.
 * His flag {@code Bevroren} (NBT, synced): koning Neptuguh froze him: a block of ice around him, he stands stock-still and
 * says nothing (a click only makes the ice tinkle).
 * <p>
 * hook fase B: the second part of the Sponsguh story freezes and thaws him with {@link #setBevroren}.
 */
public class MeneerKrabguhEntity extends SponsguhWezen {
    /**
     * (fix 1.5.0) Asked for a click on a FROZEN Krabguh instead of {@link #BIJ_KLIK}: the block of ice is not the Krabguh of
     * De Krokante Knabbel, so no step that wants "click Meneer Krabguh" (e.g. part 1's job interview) may count it.
     */
    public static final List<BiFunction<SponsguhWezen, ServerPlayer, Boolean>> BIJ_IJS_KLIK = new CopyOnWriteArrayList<>();

    public MeneerKrabguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public boolean isBevroren() {
        return vlag();
    }

    public void setBevroren(boolean bevroren) {
        zetVlag(bevroren);
        if (bevroren) {
            getNavigation().stop();
        }
    }

    @Override
    protected boolean magLopen() {
        return !isBevroren() && super.magLopen();
    }

    @Override
    public void zetAnimatie(String naam) {
        if (!isBevroren()) {
            super.zetAnimatie(naam);                                         // (frozen: no animation at all)
        }
    }

    @Override
    protected String beweging(boolean beweegt) {
        return isBevroren() ? "idle" : super.beweging(beweegt);
    }

    @Override
    protected List<BiFunction<SponsguhWezen, ServerPlayer, Boolean>> verhaalKlikken() {
        return isBevroren() ? BIJ_IJS_KLIK : BIJ_KLIK;
    }

    @Override
    protected boolean eigenKlik(ServerPlayer player) {
        if (!isBevroren()) {
            return false;
        }
        playSound(SoundEvents.GLASS_HIT, 0.8f, 1.4f);
        return true;
    }

    @Override
    public boolean isNoAi() {
        return isBevroren() || super.isNoAi();
    }
}
