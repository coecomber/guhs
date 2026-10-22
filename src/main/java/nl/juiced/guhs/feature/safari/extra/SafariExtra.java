package nl.juiced.guhs.feature.safari.extra;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.safari.gedrag.SafariHoedje;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * G1 (the gaps of the design audit): the small things that were still missing, in one place.
 * <ul>
 *   <li>own guhs wear a safari hat in the park ({@link SafariHoedje});</li>
 *   <li>the Aussie guhs of Australia (design 1.4): Bazza in the hammock under the shade tree, Shazza at the outback post
 *       office and Davo at the camp fire. They stand in the park like the lobby characters (lobby.json, written by
 *       tools/features/safari_park/zone_australie.py) and talk Aussie.</li>
 * </ul>
 * (Elsewhere, in the classes they belong to: the guhs' lines when they see animals in GuhReacties, the night eyes in
 * SafariClient and Fotograaf, the golden camera and the photo painting in camera/, the giftshop requirement in
 * SafariWinkel, the safarihek and rotswanden in SafariSpullen, the Kriebelhuis painting in SafariKoppelingen.)
 */
public final class SafariExtra {
    /** How many lines each Aussie guh has (gui.guhs.safari.aussie.<wie>.<n>). */
    static final int LIJNEN = 3;

    public static void register() {
        SafariHoedje.register();
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_AUSSIE_HANGMAT, aussie("hangmat"));
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_AUSSIE_POST, aussie("post"));
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_AUSSIE_KAMP, aussie("kamp"));
    }

    static NpcRole aussie(String wie) {
        return new NpcRole() {
            @Override
            public void talk(GuhNpcEntity npc, ServerPlayer p) {
                int n = Math.floorMod((int) (p.level().getGameTime() / 40) + p.getUUID().hashCode(), LIJNEN);
                Praat.open(p, npc, null, "gui.guhs.safari.aussie." + wie + "." + n, new Object[0],
                        new Praat.Optie(3, "gui.guhs.safari.aussie.optie"));
            }
        };
    }

    private SafariExtra() {
    }
}
