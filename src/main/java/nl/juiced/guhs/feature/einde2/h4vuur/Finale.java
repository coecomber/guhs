package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestspel.GeestToegang;
import nl.juiced.guhs.feature.einde2.geestwezens.Fang;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice h4_vuur: the finale (line {@value E2Verhaal#FINALE}, DESIGN 5, CONTRACT_E2_B 7.5): Oom Miroh sends
 * you to meditate; Fang flies you to the Top der Voorgangers in the Geestenwereld (Roku-guh, Kyoshi-guh and the Guhvatars
 * before them), back to your body; that night you wake on the back of the Leeuwschildpad-guh; the flight through the airship
 * fleet; the Agni Kai on the palace square; the duel on the pillars; the coronation and the credits.
 */
final class Finale {
    /** Where you climb on Fang in the Geestenwereld (a marker of geest_bouw; the older name as a stand-in). */
    static final String[] OPSTAP = {"fang_opstap", "plek_fang_pad", "fang_pad"};
    /** Where Fang lands on the Top (geest_bouw adds top_landing; fang_landing is the older marker). */
    static final String[] LANDING = {"top_landing", "fang_landing", "plek_top_voorgangers"};

    private Finale() {
    }

    /** The direction of the finale in the capital (from E2H4Vuur.regie). */
    static void regie(ServerPlayer p, int stap) {
        switch (stap) {
            case E2H4Vuur.F_TOP -> {
                // Miroh's tent starts it (MIROH); a player who comes back from the Geestenwereld without the Top: Miroh again
            }
            case E2H4Vuur.F_SCHILDPAD -> nacht(p);
            case E2H4Vuur.F_VLOOT -> {
                if (Plek.in(p, "kade") || Plek.bij(p, "leeuwschildpad_kade", 6)) {
                    if (Cutscenes.gezien(p, Scenes.VLOOT_PLAN.id())) {
                        Luchtvloot.start(p);
                    } else {
                        // (fix3-h4) the morning of the comet: why the fleet, why Zuka and Katara-guh go to the palace
                        E2H4Vuur.speel(p, Scenes.VLOOT_PLAN, "leeuwschildpad_kade", Luchtvloot::start);
                    }
                }
            }
            case E2H4Vuur.F_AGNI_KAI -> {
                if (Plek.in(p, "agni_kai_plein") || Plek.in(p, "kroning")) {
                    E2Audio.moment(p, "agni_kai");
                    String anker = Plekken.plek(Plek.STRUCTUUR, "agni_kai_midden") != null ? "agni_kai_midden" : "kroning_zuka";
                    E2H4Vuur.speel(p, Scenes.AGNI_KAI, anker, q -> {
                        E2Audio.stopMoment(q, 40);
                        if (E2H4Vuur.FINALE.verder(q, E2H4Vuur.F_AGNI_KAI)) {
                            E2H4Vuur.zeg(q, "duel.naar");
                            duel(q);
                        }
                    });
                }
            }
            case E2H4Vuur.F_DUEL -> {
                if (Plek.in(p, "duel_arena")) {
                    duel(p);
                } else if (Plek.bij(p, "trap_arena", 6) || Plek.bij(p, "rustpunt_8", 6)) {
                    duel(p);
                }
            }
            case E2H4Vuur.F_KRONING -> {
                if (Plek.in(p, "kroning")) {
                    kroning(p);
                }
            }
            case E2H4Vuur.F_AFTITELING -> {
                if (Plek.in(p, "kroning")) {
                    aftiteling(p);
                }
            }
            default -> {
            }
        }
    }

    // --- step 0: the Top der Voorgangers ---------------------------------------------------------------------------------------

    /** Miroh's "yes": the White Lotus opens (flag), into the Geestenwereld at Fang, and Fang flies you to the Top. */
    static void naarDeTop(ServerPlayer p) {
        E2H4Vuur.FINALE.vlag(p, E2Verhaal.V_WITTE_LOTUS, true);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        E2H4Vuur.zeg(p, "finale.mediteer");
        String opstap = eerste(OPSTAP);
        if (opstap == null || !GeestToegang.stuur(p, opstap)) {
            E2H4Vuur.zeg(p, "finale.geen_geest");
            return;
        }
        Later.plan(p, 40, Finale::fang);
    }

    /** On Fang's back along fang_vlucht; then (a moment for the chunks) the Top. */
    static void fang(ServerPlayer p) {
        if (!Geestenwereld.in(p)) {
            return;
        }
        Plekken.Plek pad = Plekken.plek(Geestenwereld.NAAM, "fang_vlucht");
        List<Vec3> punten = pad == null ? List.of() : pad.punten().stream().map(q -> Vec3.atBottomCenterOf(Geestenwereld.OORSPRONG.offset(q))).toList();
        if (punten.isEmpty()) {
            String landing = eerste(LANDING);
            BlockPos l = landing == null ? null : Geestenwereld.wereld(landing);
            punten = l == null ? List.of() : List.of(Vec3.atBottomCenterOf(l));
        }
        if (!Fang.vlieg(p, punten, q -> Later.plan(q, 40, Finale::top))) {
            top(p);
        }
    }

    /** The Top: Roku-guh, Kyoshi-guh and the Guhvatars before them; then back to the body. */
    static void top(ServerPlayer p) {
        BlockPos anker = Geestenwereld.wereld(Scenes.TOP_ANKER);
        if (anker == null || !Geestenwereld.in(p) || E2H4Vuur.FINALE.stap(p) != E2H4Vuur.F_TOP) {
            return;
        }
        Cutscenes.speel(p, Scenes.TOP, anker, Rotation.NONE, q -> {
            if (E2H4Vuur.FINALE.verder(q, E2H4Vuur.F_TOP)) {
                Geestenwereld.terug(q);
                E2H4Vuur.zeg(q, "finale.terug");
            }
        });
    }

    /** In the Geestenwereld at finale step 0 with the White Lotus open (they logged out on the way): Fang again. */
    static void inGeestenwereld(ServerPlayer p) {
        if (E2H4Vuur.FINALE == null || E2H4Vuur.FINALE.stap(p) != E2H4Vuur.F_TOP || !E2H4Vuur.FINALE.vlag(p, E2Verhaal.V_WITTE_LOTUS)) {
            return;
        }
        if (Fang.vliegt(p)) {
            return;                                              // (fix2-h4: already on Fang's back; before, a second flight could start)
        }
        String opstap = eerste(OPSTAP);
        BlockPos b = opstap == null ? null : Geestenwereld.wereld(opstap);
        if (b == null) {
            return;
        }
        Vec3 plek = Vec3.atBottomCenterOf(b);
        if (plek.distanceToSqr(p.position()) < 36) {
            fang(p);
        } else {
            // (fix2-h4) somewhere else in the Geestenwereld at this step (back after logging out): where Fang waits
            p.level().sendParticles(p, net.minecraft.core.particles.ParticleTypes.END_ROD, true, true, plek.x, plek.y + 5, plek.z, 6, 0.1, 2.6,
                    0.1, 0.0);
            if (p.tickCount % 600 >= 10) {
                return;
            }
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("➜ ").withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(net.minecraft.network.chat.Component.translatable(E2H4Vuur.Q + "finale.fang_wacht",
                            nl.juiced.guhs.feature.einde2.verhaal.Richting.van(p.position(), plek)).withStyle(net.minecraft.ChatFormatting.YELLOW)));
        }
    }

    @Nullable
    static String eerste(String[] namen) {
        for (String n : namen) {
            if (Plekken.plek(Geestenwereld.NAAM, n) != null) {
                return n;
            }
        }
        return null;
    }

    // --- step 1: the night, the Leeuwschildpad-guh -----------------------------------------------------------------------------

    /**
     * Back in the capital after the Top: night falls, you wake on the back of the Leeuwschildpad-guh.
     * <p>
     * fix4-finale: you fall asleep (a few seconds of blindness), and while your eyes are closed you are put on the quay in
     * front of its head. The overlay of its body is sent as block updates, which a client drops for chunks it has not loaded,
     * and the scene's shots of the head are taken from the quay: from the far side of the capital neither would show.
     */
    private static void nacht(ServerPlayer p) {
        E2H4Vuur.balk(p, "schildpad.slaap");
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 110, 0, false, false, false));
        Later.plan(p, 60, q -> {
            if (E2H4Vuur.FINALE.stap(q) != E2H4Vuur.F_SCHILDPAD || !Plek.inHoofdstad(q)) {
                q.removeEffect(MobEffects.BLINDNESS);
                return;
            }
            Plek.breng(q, "leeuwschildpad_kade", 180f);
            Later.plan(q, 30, Finale::schildpad);
        });
    }

    /** The scene of the Leeuwschildpad-guh (its body shown to this player only, for as long as it lasts). */
    private static void schildpad(ServerPlayer q) {
        q.removeEffect(MobEffects.BLINDNESS);
        if (E2H4Vuur.FINALE.stap(q) != E2H4Vuur.F_SCHILDPAD || !Plek.inHoofdstad(q)) {
            return;
        }
        boolean laag = Lagen.toon(q);
        if (!E2H4Vuur.speel(q, Scenes.LEEUWSCHILDPAD, "leeuwschildpad_rug", r -> {
            Lagen.weg(r);
            if (E2H4Vuur.FINALE.verder(r, E2H4Vuur.F_SCHILDPAD)) {
                Plek.breng(r, "leeuwschildpad_kade", 180f);
                E2H4Vuur.zeg(r, "schildpad.wakker");
            }
        }) && laag) {
            Lagen.weg(q);
        }
    }

    // --- step 4: the duel ------------------------------------------------------------------------------------------------------

    /**
     * fix4-finale: into the duel. The first time, the scene before it ({@link Scenes#DUEL_BEGIN}: why four rounds, and how it
     * must end); you are put on your pillar first, so the arena is loaded around you while the scene fades in.
     */
    static void duel(ServerPlayer p) {
        if (Duel.actief(p)) {
            return;
        }
        if (Cutscenes.gezien(p, Scenes.DUEL_BEGIN.id())) {
            Duel.start(p);
            return;
        }
        Plek.breng(p, "duel_speler", 0f);
        if (!E2H4Vuur.speel(p, Scenes.DUEL_BEGIN, "duel_speler", Duel::start)) {
            Duel.start(p);
        }
    }

    // --- steps 5 and 6: the coronation and the credits ---------------------------------------------------------------------

    private static void kroning(ServerPlayer p) {
        E2Audio.moment(p, "kroning");
        E2H4Vuur.speel(p, Scenes.KRONING, "kroning_zuka", q -> {
            E2Audio.stopMoment(q, 40);
            if (!E2H4Vuur.FINALE.verder(q, E2H4Vuur.F_KRONING)) {
                return;
            }
            // wink 1 ends here: Sjokkel over the finish line (once per player), then the credits
            if (!Cutscenes.gezien(q, Scenes.KNIPOOG_SJOKKEL.id())
                    && E2H4Vuur.speel(q, Scenes.KNIPOOG_SJOKKEL, "knipoog_sjokkel_finish", Finale::aftiteling)) {
                return;
            }
            aftiteling(q);
        });
    }

    static void aftiteling(ServerPlayer p) {
        if (E2H4Vuur.FINALE.stap(p) != E2H4Vuur.F_AFTITELING) {
            return;
        }
        E2Audio.moment(p, "aftiteling");
        E2H4Vuur.speel(p, Scenes.AFTITELING, "kroning_zuka", q -> {
            E2Audio.stopMoment(q, 60);
            E2H4Vuur.FINALE.verder(q, E2H4Vuur.F_AFTITELING);
        });
    }
}
