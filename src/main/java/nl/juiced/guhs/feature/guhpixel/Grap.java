package nl.juiced.guhs.feature.guhpixel;

import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * A joke game's mini questline: its id (skyblok, bedwars, vadsnite, guhmon, bzg, among), how many steps it has, its lobby
 * NPC, and the keepsake a player gets the FIRST time they finish it. Texts (owner slice): gui.guhs.&lt;id&gt;.grap.naam,
 * .grap.uitleg, .grap.stap.&lt;1..n&gt;, .grap.clou.
 */
public record Grap(String id, int stappen, GuhNpcEntity.Kind npc, Consumer<ServerPlayer> aandenken) {
}
