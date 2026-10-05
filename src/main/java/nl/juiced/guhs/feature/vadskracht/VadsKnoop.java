package nl.juiced.guhs.feature.vadskracht;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * A block that takes part in a vadskracht net: a source ({@link VadsBron}), a consumer ({@link VadsVerbruiker}) or storage
 * ({@link VadsOpslag}). Implemented by the BLOCK ENTITY of the controller block and handed out through the capability
 * {@link VadsKracht#KNOOP}; the part blocks of a machine bigger than one block answer with their controller's block entity.
 * One block entity may be several of the three at once.
 */
public interface VadsKnoop {
    /** The controller's position (parts report the same). */
    BlockPos vadsPlek();

    /** Does this knoop join Guhdraad or another knoop through this face (of the block that is asked)? */
    default boolean vadsVerbindt(Direction kant) {
        return true;
    }

    /** Extra hover lines of this very block (server side; send translatable Components). */
    default void vadsRegels(Consumer<Component> regels) {
    }
}
