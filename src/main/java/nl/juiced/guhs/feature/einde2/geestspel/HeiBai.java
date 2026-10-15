package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: Hei Bai-guh's acorn ({@value GeestLijnen#HEI_BAI}, DESIGN 5 "Het Gefluisterwoud").
 * Until a player calmed him, Hei Bai-guh is the big angry forest spirit for them (their own actor at {@code hei_bai}; the
 * little resident there is hidden for them by {@link Zicht#alleenBij}). A click: his wood in the real world burnt down;
 * bring a knabbelkoor acorn from the Guheinde (crafted from a knabbelkoorvrucht and a kaasknabbel). Plant it at
 * {@code eikel_plek} (a click with the acorn in hand): {@link GeestScenes#HEI_BAI}, he becomes small and kind again, the
 * resident shows, and the wood blooms for that player ({@link GeestLagen#WOUD}).
 */
public final class HeiBai {
    private static final String REUS = "hei_bai_reus", SLEUTEL = "einde2_geest_spel_hei_bai";

    private HeiBai() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.heiBai();
    }

    static void register() {
        Bewoners.BIJ_MAKEN.add((e, naam) -> {
            if ((Geestenwereld.NAAM + "/hei_bai").equals(naam)) {
                Zicht.alleenBij(e, GeestLijnen.HEI_BAI, GeestLijnen.H_KLAAR, 99);
            }
        });
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie == 1 && lijn().stap(p) == GeestLijnen.H_ONTMOET) {
                lijn().zet(p, GeestLijnen.H_EIKEL);
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.hei_bai.opdracht");
            }
        });
    }

    static boolean mag(ServerPlayer p) {
        return GeestToegang.zoneOpen(p, "gefluisterwoud");
    }

    static void tick(ServerPlayer p) {
        Vec3 plek = GeestPlek.midden("hei_bai");
        boolean wil = plek != null && mag(p) && !lijn().klaar(p) && p.position().closerThan(plek, 56);
        if (wil) {
            Entity reus = GeestActeurs.zorg(p, REUS, "guhs:hei_bai_guh", plek, 180f, t -> {
                t.putInt("Soort", 1);
                t.putBoolean("Stil", true);
            });
            if (reus != null && p.position().closerThan(plek, 14) && p.tickCount % 60 < 10) {
                GeestSpel.geluid(p, net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 0.25f, 1.6f);
            }
            if (lijn().stap(p) == GeestLijnen.H_EIKEL) {
                Vec3 e = GeestPlek.midden("eikel_plek");
                if (e != null && p.position().closerThan(e, 24)) {
                    p.level().sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, e.x, e.y + 0.3, e.z, 2, 0.2, 0.1, 0.2, 0.0);
                }
            }
        } else if (GeestActeurs.heeft(p, REUS)) {
            GeestActeurs.weg(p, REUS, false);
        }
    }

    /** A click on the angry giant (this player's own). */
    static boolean klikWezen(ServerPlayer p, GeestWezen w) {
        if (!REUS.equals(GeestActeurs.naam(w)) || !p.getUUID().equals(GeestActeurs.eigenaar(w))) {
            return false;
        }
        w.zetAnimatie("groei");
        if (lijn().stap(p) == GeestLijnen.H_ONTMOET) {
            lijn().begin(p);
            GeestSpel.praat(p, SLEUTEL, List.of(GeestSpel.verteller("gui.guhs.einde2_geest_spel.hei_bai.1"),
                    GeestSpel.verteller("gui.guhs.einde2_geest_spel.hei_bai.2"), GeestSpel.verteller("gui.guhs.einde2_geest_spel.hei_bai.3")),
                    new Praat.Optie(1, "gui.guhs.einde2_geest_spel.hei_bai.help"));
        } else {
            GeestSpel.verteller(p, GuhQuests.count(p, E2GeestSpelFeature.EIKEL.get()) > 0 ? "gui.guhs.einde2_geest_spel.hei_bai.plant"
                    : "gui.guhs.einde2_geest_spel.hei_bai.nog");
        }
        return true;
    }

    /** A click on the acorn spot. */
    static boolean klik(ServerPlayer p, String prop) {
        if (!"eikel_plek".equals(prop)) {
            return false;
        }
        if (!mag(p) || lijn().klaar(p)) {
            GeestSpel.verteller(p, lijn().klaar(p) ? "gui.guhs.einde2_geest_spel.hei_bai.groeit" : "gui.guhs.einde2_geest_spel.hei_bai.stronk");
            return true;
        }
        if (lijn().stap(p) == GeestLijnen.H_ONTMOET) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.hei_bai.stronk");
            return true;
        }
        if (GuhQuests.count(p, E2GeestSpelFeature.EIKEL.get()) <= 0) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.hei_bai.nog");
            return true;
        }
        GuhQuests.take(p, E2GeestSpelFeature.EIKEL.get(), 1);
        plant(p);
        return true;
    }

    /** The acorn goes in: the scene, then the line done, the giant goes, the little one shows, the wood blooms. */
    static void plant(ServerPlayer p) {
        GeestActeurs.weg(p, REUS, false);
        GeestScenes.speel(p, GeestScenes.HEI_BAI, speler -> {
            if (!lijn().klaar(speler)) {
                lijn().zet(speler, GeestLijnen.H_KLAAR);
                GuhAdvancements.grant(speler, "einde2_geest_spel_hei_bai");
                GeestSpel.verteller(speler, "gui.guhs.einde2_geest_spel.hei_bai.klaar");
                Zicht.kijkOpnieuw(speler);
                nl.juiced.guhs.feature.bestaand.Schijn.straks(speler);
                Mysterie.bloeiCheck(speler);
            }
        });
    }
}
