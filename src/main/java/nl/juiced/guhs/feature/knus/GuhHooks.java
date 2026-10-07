package nl.juiced.guhs.feature.knus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Hooks into every {@link GuhEntity} (2.8), so features never have to edit GuhEntity itself.
 * <ul>
 *   <li>{@link #doelen}: extra goals, added at the end of GuhEntity.registerGoals (register in your Feature.register:
 *       goals are made when a guh is created).</li>
 *   <li>{@link #tick}: every tick of every guh, server side only (keep it cheap: spread work with
 *       {@code (guh.tickCount + guh.getId()) % N}).</li>
 *   <li>{@link #klik}: a right-click on a guh, before its own handling (taming, feeding, clothes...). Called on both sides;
 *       the first hook that doesn't return PASS wins (return {@code InteractionResult.SUCCESS}
 *       when you handled it).</li>
 *   <li>Visual flags ({@link #GLANZEND}, {@link #PYJAMA}, {@link #IJSHOEDJE}, {@link #BLOSJES}): synced to the client and
 *       saved ("KnusVlaggen"); the owner keeps its own timers in {@code guh.getPersistentData()} keys {@code guhs_<pkg>_*}
 *       and draws them with a {@code client.GuhRenderHooks} layer.</li>
 *   <li>{@link #bezig}: an activity (theekransje, feestbuffet, kampvuur, tuintje, ijscokar) claims a guh for a while; the
 *       day rhythm (wereldleven) leaves a busy guh alone.</li>
 *   <li>Residents of the Knuffeldal town (placed by phase 1): {@link #isBewoner}, {@link #thuis}.</li>
 * </ul>
 */
public final class GuhHooks {
    /** Visual flags (bits of the synced int "KnusVlaggen"): knuffelbad, kamperen, wereldleven, wereldleven. */
    public static final int GLANZEND = 1, PYJAMA = 2, IJSHOEDJE = 4, BLOSJES = 8;

    /** Persistent data keys of the residents (set by the town template, see tools/features/knuffeldal_stadje.py). */
    public static final String BEWONER = "guhs_knus_bewoner", THUIS = "guhs_knus_thuis", BEWONER_NAAM = "guhs_knus_bewoner_naam";
    private static final String BEZIG = "guhs_knus_bezig_tot";

    @FunctionalInterface
    public interface Klik {
        InteractionResult klik(GuhEntity guh, Player player, InteractionHand hand);
    }

    private static final List<BiConsumer<GuhEntity, GoalSelector>> DOELEN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<GuhEntity>> TICKS = new CopyOnWriteArrayList<>();
    private static final List<Klik> KLIKKEN = new CopyOnWriteArrayList<>();
    private static final List<BiPredicate<GuhEntity, net.minecraft.world.item.ItemStack>> ITEMS = new CopyOnWriteArrayList<>();

    private GuhHooks() {
    }

    public static void doelen(BiConsumer<GuhEntity, GoalSelector> adder) {
        DOELEN.add(adder);
    }

    public static void tick(Consumer<GuhEntity> tick) {
        TICKS.add(tick);
    }

    public static void klik(Klik klik) {
        KLIKKEN.add(klik);
    }

    /**
     * (1.2.9) An item your {@link #klik} hook takes on a guh. The client swallows a right-click on your own tamed guh (a tap
     * is a pet, holding opens the menu) unless the item in your hand does something of its own: without this the click
     * never reaches the hook. Asked on the client: only use what the client knows.
     */
    public static void item(BiPredicate<GuhEntity, net.minecraft.world.item.ItemStack> neemt) {
        ITEMS.add(neemt);
    }

    /** (GuhEntity.heeftEigenKlik, client) */
    public static boolean neemtItem(GuhEntity guh, net.minecraft.world.item.ItemStack stack) {
        for (BiPredicate<GuhEntity, net.minecraft.world.item.ItemStack> neemt : ITEMS) {
            if (neemt.test(guh, stack)) {
                return true;
            }
        }
        return false;
    }

    // --- called by GuhEntity ----------------------------------------------------------------------------------------------

    /** (GuhEntity.registerGoals) */
    public static void runDoelen(GuhEntity guh, GoalSelector goals) {
        for (BiConsumer<GuhEntity, GoalSelector> adder : DOELEN) {
            adder.accept(guh, goals);
        }
    }

    /** (GuhEntity.tick, server) */
    public static void runTick(GuhEntity guh) {
        for (Consumer<GuhEntity> tick : TICKS) {
            tick.accept(guh);
        }
    }

    /** (GuhEntity.mobInteract) PASS: nobody handled it. */
    public static InteractionResult runKlik(GuhEntity guh, Player player, InteractionHand hand) {
        for (Klik klik : KLIKKEN) {
            InteractionResult r = klik.klik(guh, player, hand);
            if (r != InteractionResult.PASS) {
                return r;
            }
        }
        return InteractionResult.PASS;
    }

    // --- flags ------------------------------------------------------------------------------------------------------------

    public static boolean heeft(GuhEntity guh, int vlag) {
        return (guh.getKnusVlaggen() & vlag) != 0;
    }

    public static void zet(GuhEntity guh, int vlag, boolean aan) {
        int now = guh.getKnusVlaggen();
        guh.setKnusVlaggen(aan ? now | vlag : now & ~vlag);
    }

    // --- busy ---------------------------------------------------------------------------------------------------------------

    /** Busy for this many ticks (0: not busy any more). */
    public static void bezig(GuhEntity guh, int ticks) {
        if (ticks <= 0) {
            guh.getPersistentData().remove(BEZIG);
        } else {
            guh.getPersistentData().putLong(BEZIG, guh.level().getGameTime() + ticks);
        }
    }

    public static boolean isBezig(GuhEntity guh) {
        return guh.getPersistentData().getLongOr(BEZIG, 0L) > guh.level().getGameTime();
    }

    // --- residents ----------------------------------------------------------------------------------------------------------

    /** A guh that lives in a Knuffeldal town (placed by the town template). */
    public static boolean isBewoner(GuhEntity guh) {
        return guh.getPersistentData().getBooleanOr(BEWONER, false);
    }

    /** Where a resident lives (its spot when it first came into the world), or null. */
    @Nullable
    public static BlockPos thuis(GuhEntity guh) {
        return guh.getPersistentData().contains(THUIS) ? BlockPos.of(guh.getPersistentData().getLongOr(THUIS, 0L)) : null;
    }

    /** Makes a guh a resident living at this spot (the town does it for its own guhs; tests too). */
    public static void maakBewoner(GuhEntity guh, BlockPos thuis) {
        guh.getPersistentData().putBoolean(BEWONER, true);
        guh.getPersistentData().putLong(THUIS, thuis.asLong());
        guh.setPersistenceRequired();
    }
}
