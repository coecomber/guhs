package nl.juiced.guhs.feature.vadsrijk.dungeon;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * W-GROT (user decision): the kinds of dungeon. Caves are the first type; outdoor dungeons, dungeons in buildings and dungeons on a
 * flying mount come later and plug into the same door / window / queue / instance / cooldown system ({@link DungeonRij}).
 */
public enum DungeonType {
    /** A cave (U-GROTKERKER's cave runs). */
    GROT,
    /** Outdoors (later). */
    BOVENGRONDS,
    /** In a building (later). */
    GEBOUW,
    /** On a flying mount (later). */
    LUCHT;

    /** The player-facing label ("Grot", "Bovengronds", "Gebouw", "In de lucht"). */
    public Component label() {
        return Component.translatable("vadsrijk.dungeon.type." + name().toLowerCase(Locale.ROOT));
    }
}
