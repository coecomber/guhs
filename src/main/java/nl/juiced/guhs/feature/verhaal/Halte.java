package nl.juiced.guhs.feature.verhaal;

import javax.annotation.Nullable;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.5): a stop on a {@link Reiskaart}: the questline that plays there, its number on
 * the map, its pixel on the 256x160 map picture, and the structure it is (null: none; a structure behind Guhdalfs sluier
 * keeps the halte a "???").
 */
public record Halte(String lijn, int nr, int kaartX, int kaartY, @Nullable String structuur) {
}
