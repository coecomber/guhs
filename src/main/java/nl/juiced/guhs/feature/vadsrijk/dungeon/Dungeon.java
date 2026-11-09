package nl.juiced.guhs.feature.vadsrijk.dungeon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;

/**
 * W-GROT: one dungeon, as the generic door / window / queue / instance / cooldown system ({@link DungeonRij}) sees it. A dungeon
 * type's own code (the caves: {@code grotkerker.GrotDungeon}) implements it and registers through {@link Dungeons#bron}. The
 * dungeon's own run (rooms, packs, boss, loot) stays in that code; it asks {@link DungeonRij} who the group is
 * ({@link DungeonRij#lid}, {@link DungeonRij#magStarten}) and reports the clear ({@link DungeonRij#klaar}).
 */
public interface Dungeon {
    /** Stable id (also the data key of clears and cooldowns): the caves keep their cave ids ("zwamgrot", ...). */
    String id();

    DungeonType type();

    /** The name ("De Zwamgrot"). */
    Component naam();

    /** A short line under the name on the title card (the boss's subtitle for the caves). */
    Component onderTitel();

    int zone();

    int minLv();

    /** The highest level in it (the boss). */
    int maxLv();

    /** Lang key of the advised group size ("vadsrijk.dungeon.spelers.solo" / ".groep"). */
    default String aanbevolenSpelers() {
        return zone() <= 2 ? "vadsrijk.dungeon.spelers.solo" : "vadsrijk.dungeon.spelers.groep";
    }

    /** Is this position inside the dungeon (only the group may be there)? */
    boolean bevat(double x, double y, double z);

    /** Is its run going (not at rest)? */
    boolean bezig();

    /** Back to rest: everything of the run away (the group left / the instance ends). */
    void reset(ServerLevel sl);

    /** How often this player cleared it. */
    int clears(ServerPlayer p);

    /** The dungeon's own lines in the window: the boss, the loot, the rare rolls. */
    void venster(ServerPlayer p, DungeonNet.Bouw b);

    /** A door of this dungeon as placed in this build (zone 6 moves with its POIs, for example). */
    default Deur pasAan(Deur d) {
        return d;
    }
}
