package nl.juiced.guhs.feature.huisje;

/**
 * One running chore (or toy session) of one resident: ticked by {@link HuisjeGoal} every tick while it returns true
 * (move with the mob's navigation), stopped after {@link #maxTicks()} at the latest.
 */
public interface KlusTaak {
    /** One tick of work: true = keep going, false = done. */
    boolean tick();

    /** Called once when it ends (done, too long, night, the resident moved out...). */
    default void stop() {
    }

    default int maxTicks() {
        return 1200;
    }
}
