package nl.juiced.guhs.feature.mewtwo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;

/**
 * Professor Knabbelkloon (NPC kind KNABBELKLOON), absent-minded, his little glasses always crooked. What he says follows
 * the player's questline ({@link MewtwoVoortgang}): the story in bits and the question for the 6 lab notes, where the
 * missing notes lie, the memory when all six are back (step 2), the tank parts, the big meal, and afterwards how happy
 * Guhtwo and Mieuwguh are.
 */
public class Knabbelkloon implements NpcRole {
    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        npc.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 1f, 0.8f);
        switch (MewtwoVoortgang.stap(p)) {
            case MewtwoVoortgang.NIEUW -> {
                List<Praat.Regel> pag = new ArrayList<>();
                for (int i = 1; i <= 4; i++) {
                    pag.add(new Praat.Regel(npc, "", "gui.guhs.mewtwo.prof." + i));
                }
                Praat.scene(p, MewtwoVerhaal.INTRO, pag, new Praat.Optie(1, "gui.guhs.mewtwo.prof.ja"), new Praat.Optie(2, "gui.guhs.mewtwo.prof.nee"));
            }
            case MewtwoVoortgang.NOTITIES -> {
                int n = MewtwoVoortgang.aantalNotities(p);
                if (n >= MewtwoFeature.NOTITIES) {
                    MewtwoVerhaal.notitiesKlaar(p, npc);
                } else {
                    Praat.open(p, npc, null, "gui.guhs.mewtwo.prof.zoek." + MewtwoVerhaal.eersteNotitie(p), new Object[]{n, MewtwoFeature.NOTITIES - n});
                }
            }
            case MewtwoVoortgang.ONDERDELEN -> {
                int nog = MewtwoFeature.ONDERDELEN - MewtwoVoortgang.aantalIngebouwd(p);
                int bij = 0;
                for (ItemStack s : p.getInventory().items) {
                    if (s.getItem() instanceof TankonderdeelItem && !MewtwoVoortgang.isIngebouwd(p, TankonderdeelItem.soort(s))) {
                        bij++;
                    }
                }
                if (bij > 0) {
                    Praat.open(p, npc, null, "gui.guhs.mewtwo.prof.onderdelen", new Object[]{nog, bij});
                } else {
                    Praat.open(p, npc, null, "gui.guhs.mewtwo.prof.onderdelen_zoek." + MewtwoVerhaal.eersteOnderdeel(p), new Object[]{nog});
                }
            }
            case MewtwoVoortgang.MAALTIJD -> Praat.open(p, npc, null, "gui.guhs.mewtwo.prof.maaltijd", new Object[]{
                    MewtwoFeature.PORTIE_KNABBELS - MewtwoVoortgang.knabbels(p), MewtwoFeature.PORTIE_SNACKS - MewtwoVoortgang.snacks(p)});
            default -> Praat.open(p, npc, null, VerhaalGuhs.magTemmen(p, VerhaalGuh.MEWTWO) ? "gui.guhs.mewtwo.prof.klaar_niet_getemd"
                    : "gui.guhs.mewtwo.prof.klaar", new Object[0]);
        }
    }
}
