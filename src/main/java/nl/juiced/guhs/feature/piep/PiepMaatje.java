package nl.juiced.guhs.feature.piep;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;

/**
 * A little piep-maatje with its own right-click menu ({@link PiepMenu}) that its owner can pick up into an item
 * ({@link PiepDierItem}): the {@link PieppiepmuisjeEntity pieppiepmuisje}, {@link PoepschillyEntity Poepschilly} and
 * {@link SchillyEntity Schilly}. Implemented by the entities themselves.
 */
public interface PiepMaatje {
    /** The creature itself. */
    TamableAnimal dier();

    /** "pieppiepmuisje", "poepschilly" or "schilly" (lang keys, piepboek pages). */
    String soort();

    /** The settings its menu shows ({@link PiepInstelling#MUISJE} ...). */
    List<PiepInstelling> instellingen();

    /** The item it becomes when you pick it up. */
    Item oppakItem();

    /** The synced flags of the settings that are OFF (bit = {@link PiepInstelling#bit}). */
    int uitVlaggen();

    void setUitVlaggen(int vlaggen);

    /** Busy right now (hidden, inside a guh...): no menu, no picking up. */
    boolean isBezig();

    /** The menu's big button: the muisje climbs on your shoulder, Poepschilly/Schilly gets ready for a guh. */
    void speciaal(ServerPlayer player);

    /** Seconds it still rests before {@link #speciaal} works again (0: ready). */
    default int rustSeconden() {
        return 0;
    }

    // --- 3.0: every tameable critter can be a maatje (pick up, put down, huisje resident, "waar is hij", shoulder) ---------

    /** The sound when it is picked up / put down / hops on a shoulder (PiepDierItem.geluid asks this). */
    default net.minecraft.sounds.SoundEvent oppakGeluid() {
        return dier() instanceof PieppiepmuisjeEntity ? PiepFeature.PIEP.get() : PiepFeature.SCHILLY_PLOP.get();
    }

    /** Can it sit on your shoulder ({@link Schouder}: one at a time, left shoulder)? The muisje yes; landdiertjes: the pluiseekhoorntje. */
    default boolean kanOpSchouder() {
        return false;
    }

    /** (Client) this copy is drawn on a shoulder (sit pose). */
    default void opSchouder(boolean ja) {
    }

    /** How big it is drawn on a shoulder (x the shoulder's own 0.9). */
    default float schouderSchaal() {
        return 1f;
    }

    default boolean aan(PiepInstelling instelling) {
        TamableAnimal dier = dier();
        if (instelling == PiepInstelling.RONDVADSEN) {
            return !(dier.level().isClientSide ? dier.isInSittingPose() : dier.isOrderedToSit());
        }
        return (uitVlaggen() & (1 << instelling.bit)) == 0;
    }

    default void zet(PiepInstelling instelling, boolean aan) {
        TamableAnimal dier = dier();
        if (instelling == PiepInstelling.RONDVADSEN) {
            dier.setOrderedToSit(!aan);
            dier.setInSittingPose(!aan);
            dier.getNavigation().stop();
            dier.setJumping(false);
            return;
        }
        int v = uitVlaggen();
        setUitVlaggen(aan ? v & ~(1 << instelling.bit) : v | (1 << instelling.bit));
        instellingVeranderd(instelling);
    }

    /** (After {@link #zet}) e.g. the turtles' water malus. */
    default void instellingVeranderd(PiepInstelling instelling) {
    }

    /**
     * Rondvadsen off: it stays where it is (priority 1, MOVE: every wander/follow/tempt goal after it waits). Not while it is
     * busy with something of its own (a poetsbeurt walks by itself).
     */
    class BlijfGoal extends Goal {
        private final PiepMaatje maatje;

        public BlijfGoal(PiepMaatje maatje) {
            this.maatje = maatje;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            TamableAnimal d = maatje.dier();
            return d.isTame() && d.isOrderedToSit() && !maatje.isBezig() && !d.isLeashed() && !d.isPassenger();
        }

        @Override
        public void start() {
            maatje.dier().getNavigation().stop();
        }

        @Override
        public void tick() {
            maatje.dier().getNavigation().stop();
        }
    }
}
