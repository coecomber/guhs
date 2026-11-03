package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * One case of the Sherlock story (the mechanics; the texts and who says them live in tools/features/erfenis_sherlock.py,
 * keys {@code sherlock.zaak.<id>.*}). See {@link Zaken} for all eleven.
 *
 * @param id          the case id ("eieren", ...), also in the lang keys
 * @param nr          the chapter 1..11
 * @param trigger     the fixed moment it waits for (besides the previous case being solved)
 * @param kaarten     the cards: clues found with the vergrootglas and testimonies of witnesses
 * @param verdachten  the suspects on the board (ids of {@link Zaken#WIE}); empty: no culprit (chapter 10, the finale)
 * @param dader       the culprit (always something sweet), or null
 * @param getuigen    the witnesses who stand in the village while the case runs
 * @param guhlden     the reward in Guhlden
 * @param beloningen  deco ids and reward flags (Sherlock.beloningen; deco also goes to ErfenisDeco.geef)
 */
public record Zaak(String id, int nr, Predicate<Voortgang> trigger, List<Kaart> kaarten, List<String> verdachten, @Nullable String dader,
                   List<Getuige> getuigen, int guhlden, List<String> beloningen) {

    /**
     * A card on the board.
     *
     * @param id        its id (lang {@code sherlock.zaak.<zaak>.kaart.<id>} and {@code .tekst})
     * @param icoon     the item drawn on it
     * @param goed      true: it points at the culprit; false: the case's red herring
     * @param wijstNaar for the red herring: the suspect it seems to point at
     * @param x         plot tile x of a clue (found with the vergrootglas), -1 for a testimony
     * @param z         plot tile z
     * @param getuige   the witness who gives this testimony, or null for a clue
     * @param kleur     the colour of the glitter (rgb)
     */
    public record Kaart(String id, String icoon, boolean goed, @Nullable String wijstNaar, int x, int z, @Nullable String getuige, int kleur) {
        public boolean glas() {
            return getuige == null;
        }
    }

    /**
     * A witness: an NPC that stands at a plot tile while the case runs. kind null = a chicken (Hendrik de Haan).
     */
    public record Getuige(String id, @Nullable GuhNpcEntity.Kind kind, int x, int z) {
    }

    public List<Kaart> glasKaarten() {
        return kaarten.stream().filter(Kaart::glas).toList();
    }

    @Nullable
    public Kaart kaart(String kaartId) {
        for (Kaart k : kaarten) {
            if (k.id().equals(kaartId)) {
                return k;
            }
        }
        return null;
    }

    @Nullable
    public Getuige getuige(String getuigeId) {
        for (Getuige g : getuigen) {
            if (g.id().equals(getuigeId)) {
                return g;
            }
        }
        return null;
    }

    /** Chapter 10: no culprit, the case is done when every testimony is collected. */
    public boolean verzamelZaak() {
        return dader == null && !kaarten.isEmpty();
    }

    /** Chapter 11: the courtroom. */
    public boolean finale() {
        return kaarten.isEmpty();
    }
}
