package nl.juiced.guhs.feature.vadsrijk.bazenboek;

import static nl.juiced.guhs.feature.vadsrijk.bazenboek.Bazenboek.K;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.bazenboek.Bazenboek.Toegang;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;

/**
 * 1.10.3 "Bazen" tab: the bosses' own access rules ({@link Bazenboek#REGELS}), each read from the gate the game itself uses. A rule
 * answers null when it has no objection: the arena respawn / dungeon cooldown / "Toegankelijk" follows.
 */
final class Regels {
    private Regels() {
    }

    static void register() {
        // the Zwamtempel's door: Zwamtempel.waaromDicht (level 18 always, from 16 with the temple quest)
        Bazenboek.REGELS.put("opperzwam", p -> {
            if (nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Zwamtempel.waaromDicht(p) == null) {
                return null;
            }
            return Toegang.nee(Component.translatable(K + "toegang.tempel", nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam.Zwamtempel.TEMPEL_ALTIJD_LEVEL,
                    Spelers.level(p), nl.juiced.guhs.feature.vadsrijk.bazen.BazenPlekken.TEMPEL_MIN_LEVEL));
        });
        // the Spiegelburcht's front door (WereldDeuren.SLEUTELS, 1.10.3): Spiegelzucht's quest from Prisma opens it
        Bazenboek.REGELS.put("spiegelzucht", p -> nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren.ontgrendeld("spiegelburcht", p) ? null
                : Toegang.nee(Component.translatable(K + "toegang.spiegelburcht", questNaam("z2_hq11_spiegelzucht"))));
        // the Mika-boormeester sleeps until the 5 clues of the Splinterpieken woke him (Boormeester.ontgrendel); then a right-click
        Bazenboek.REGELS.put("boormeester", Regels::boormeester);
        // story fights that only exist during their quest
        Bazenboek.REGELS.put("mika_spion", p -> Bazenboek.alleenTijdensQuest(p, "geheim_spion"));
        Bazenboek.REGELS.put("grauwe_droom", p -> Bazenboek.alleenTijdensQuest(p, nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Haken.HQ08));
        // the raid: four floors of De Sterrendovende Toren, only in a raid group
        for (String r : new String[] {"z6raid_zandwachter", "z6raid_spiegelvizier", "z6raid_sterrenbeelden", "z6raid_ringeling"}) {
            Bazenboek.REGELS.put(r, p -> Toegang.ja(Component.translatable(K + "toegang.raid")));
        }
    }

    static Toegang boormeester(ServerPlayer p) {
        if (nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor.Boormeester.MAG_STARTEN.test(p)) {
            return Toegang.ja(Component.translatable(K + "toegang.boormeester.wakker"));
        }
        int heb = Questlog.map(p, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Haken.AANWIJZINGEN).size();
        int nodig = nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken.AANWIJZING.size();
        return Toegang.nee(Component.translatable(K + "toegang.boormeester.slaapt", nodig, Math.min(heb, nodig), nodig));
    }

    static Component questNaam(String quest) {
        Quest q = Quests.van(quest);
        return q == null ? Component.literal(quest) : Component.translatable(q.titelKey());
    }
}
