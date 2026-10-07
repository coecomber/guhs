package nl.juiced.guhs.feature.guhpixel;

import java.util.function.Function;

/**
 * A kind of game: its arena, how many real players one session takes, the lobby anchor its players return to
 * ({@link LobbyPlek#voor}) and the maker of its {@link Sessie}.
 */
public record SpelSoort(String id, ArenaSoort arena, int minSpelers, int maxSpelers, LobbyPlek plek, Function<SessieStart, Sessie> maker) {
}
