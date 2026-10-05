package nl.juiced.guhs.feature.wereld;

import net.minecraft.server.level.ServerPlayer;

/**
 * bbq2: the per-player steps of a questline, as the world helpers see them (CONTRACT_130 §5.1; complete, nobody edits it).
 * {@code nl.juiced.guhs.feature.verhaal.Verhaallijn} implements it; {@code QuestRol} takes one.
 */
public interface Stappen {
    int stap(ServerPlayer p);

    boolean verder(ServerPlayer p, int vanStap);

    boolean eenmalig(ServerPlayer p, String naam);
}
