package nl.juiced.guhs.feature.verhaal;

import java.util.List;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.5): the travel map of a group of questlines in the Guhdex tab Verhalen: a drawn
 * picture ({@code textures/gui/verhaal/reiskaart_<id>.png}, 256x160) with the haltes on it in travel order. Name and
 * picture: tools/features/verhaal_motor.py {@code reiskaart(h, id, naam, kaart)} ({@code gui.guhs.verhaal.reiskaart.<id>}).
 */
public record Reiskaart(String id, String groep, List<Halte> haltes) {
    public Reiskaart {
        haltes = List.copyOf(haltes);
    }

    public String naamKey() {
        return "gui.guhs.verhaal.reiskaart." + id;
    }
}
