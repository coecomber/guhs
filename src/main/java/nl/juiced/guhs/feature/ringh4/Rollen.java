package nl.juiced.guhs.feature.ringh4;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h4): who says what in the tree city. Every character is the same entity for everybody and talks from the
 * player's own step of the chapter ({@link QuestRol}); which of them a player sees at all is the business of
 * {@code Zicht} (the template: tools/features/ring_h4_bouw.py bewoners()).
 * <ul>
 *   <li>Leguhlas at the gate (plek {@value RingH4Feature#POORT}, steps 0-1): welcomes you and sends you up the great stair.</li>
 *   <li>Guhladriel (plek {@value RingH4Feature#STAD}): in her hall (steps 0-3, and from step 6 on for ever), in the dell of the
 *       mirror (steps 4-5): the welcome, the rest, the mirror, the three gifts; later she replaces a gift you lost.</li>
 *   <li>Leguhlas and Gimguh on the quay (plek {@value RingH4Feature#STEIGER}, from step 2 on): they bicker about knabbels until
 *       it is time to sail; then Leguhlas lets you into the boat.</li>
 * </ul>
 * A player who is not in chapter 4 (a friend who walks along, somebody who is done) gets small talk and solves nothing.
 */
final class Rollen {
    private static final String Q = "quest.guhs.ringh4.";

    static void registreer() {
        NpcRollen.zet(GuhNpcEntity.Kind.LEGUHLAS, RingH4Feature.POORT, new LeguhlasPoort());
        NpcRollen.zet(GuhNpcEntity.Kind.GUHLADRIEL, RingH4Feature.STAD, new Guhladriel());
        NpcRollen.zet(GuhNpcEntity.Kind.LEGUHLAS, RingH4Feature.STEIGER, new LeguhlasSteiger());
        NpcRollen.zet(GuhNpcEntity.Kind.GIMGUH, RingH4Feature.STEIGER, new GimguhSteiger());
    }

    private static void geluid(GuhNpcEntity npc, float toon) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, toon);
    }

    /** Is chapter 4 this player's chapter right now (open and not done)? */
    private static boolean bezig(ServerPlayer p) {
        return RingH4Feature.LIJN.aanDeBeurt(p) && !RingH4Feature.LIJN.klaar(p);
    }

    static final class LeguhlasPoort extends QuestRol {
        LeguhlasPoort() {
            super(RingH4Feature.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            geluid(npc, 1.15f);
            if (!bezig(p)) {
                zeg(p, npc, Q + "leguhlas.poort.later");
                return;
            }
            if (stap == 0) {
                if (!RingH4Events.aankomst(p)) {       // (the card first; it brings step 1)
                    zeg(p, npc, Q + "leguhlas.poort.wacht");
                }
                return;
            }
            if (stap == 1) {
                zeg(p, npc, Q + "leguhlas.poort.welkom.0");
                zeg(p, npc, Q + "leguhlas.poort.welkom.1");
                if (verder(p, 1)) {
                    hint(p, Q + "hint.zaal");
                }
                return;
            }
            zeg(p, npc, Q + "leguhlas.poort.later");
        }
    }

    static final class Guhladriel extends QuestRol {
        Guhladriel() {
            super(RingH4Feature.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            geluid(npc, 1.3f);
            if (!RingH4Feature.LIJN.aanDeBeurt(p)) {
                zeg(p, npc, "quest.guhs.ring.cast.guhladriel." + nl.juiced.guhs.feature.ring.Cast.fase(p) + "." + p.getRandom().nextInt(2));
                return;
            }
            if (stap == 0) {
                if (!RingH4Events.aankomst(p)) {       // (the narrator card first, also for whoever ran past the gate)
                    zeg(p, npc, Q + "guhladriel.rust");
                }
                return;
            }
            if (stap <= 2) {
                // (whoever ran past Leguhlas is welcome all the same)
                RingH4Feature.LIJN.zet(p, 2);
                zeg(p, npc, Q + "guhladriel.welkom.0");
                zeg(p, npc, Q + "guhladriel.welkom.1");
                zeg(p, npc, Q + "guhladriel.welkom.2");
                if (verder(p, 2)) {
                    hint(p, Q + "hint.rust");
                }
                return;
            }
            switch (stap) {
                case 3 -> {
                    zeg(p, npc, Q + "guhladriel.rust");
                    hint(p, Q + "hint.rust");
                }
                case 4 -> {
                    zeg(p, npc, Q + "guhladriel.spiegel");
                    hint(p, Q + "hint.spiegel");
                }
                case 5 -> gaven(npc, p);
                case 6 -> {
                    zeg(p, npc, geefKwijte(p) ? Q + "guhladriel.opnieuw" : Q + "guhladriel.vaarwel");
                    hint(p, Q + "hint.boot");
                }
                default -> {
                    if (geefKwijte(p) && Ring.opReis(p)) {
                        zeg(p, npc, Q + "guhladriel.opnieuw");
                    } else {
                        zeg(p, npc, Q + "guhladriel.na." + p.getRandom().nextInt(3));
                    }
                }
            }
        }

        /** Step 5 -> 6: the three gifts, each with its word. */
        private void gaven(GuhNpcEntity npc, ServerPlayer p) {
            if (!verder(p, 5)) {
                return;
            }
            zeg(p, npc, Q + "guhladriel.gaven.0");
            zeg(p, npc, Q + "guhladriel.gaven.lichtflesje");
            zeg(p, npc, Q + "guhladriel.gaven.elfenmanteltje");
            zeg(p, npc, Q + "guhladriel.gaven.elfentouw");
            zeg(p, npc, Q + "guhladriel.gaven.1");
            Gaven.geef(p);
            ServerLevel level = p.level();
            level.playSound(null, npc.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.4f, 1.3f);
            level.sendParticles(p, ParticleTypes.END_ROD, false, false, p.getX(), p.getY() + 1.2, p.getZ(), 24, 0.6, 0.6, 0.6, 0.03);
            p.sendSystemMessage(Component.translatable(Q + "gaven_gekregen").withStyle(ChatFormatting.GOLD));
            hint(p, Q + "hint.boot");
        }

        /** Gives back whatever gift the player no longer carries (they are needed in chapters 5 and 6). True when one was given. */
        private static boolean geefKwijte(ServerPlayer p) {
            boolean gegeven = false;
            for (var gift : java.util.List.of(RingFeature.LICHTFLESJE.get(), RingFeature.ELFENMANTELTJE.get(), RingFeature.ELFENTOUW.get())) {
                gegeven |= Gaven.geefAlsKwijt(p, gift);
            }
            return gegeven;
        }
    }

    static final class LeguhlasSteiger extends QuestRol {
        LeguhlasSteiger() {
            super(RingH4Feature.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            geluid(npc, 1.15f);
            if (npc.isInvisible()) {
                return;                                // (he sits in the boat that is under way)
            }
            if (!RingH4Feature.LIJN.aanDeBeurt(p)) {
                zeg(p, npc, Q + "leguhlas.steiger.later");
                return;
            }
            if (stap < 6) {
                zeg(p, npc, Q + "leguhlas.steiger.wacht." + Math.min(stap, 5));
                return;
            }
            ElfenbootjeEntity boot = Vaart.boot(p.level(), npc.position(), ElfenbootjeEntity.GIDS);
            if (boot == null || boot.isWeg()) {
                zeg(p, npc, Q + "leguhlas.steiger.bootje_weg");
            } else if (!Vaart.stapIn(p, boot)) {
                zeg(p, npc, Q + "leguhlas.steiger.later");
            }
        }
    }

    static final class GimguhSteiger extends QuestRol {
        GimguhSteiger() {
            super(RingH4Feature.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            if (npc.isInvisible()) {
                return;
            }
            geluid(npc, 0.8f);
            zeg(p, npc, Q + "gimguh.steiger." + p.getRandom().nextInt(4));
        }
    }

    private Rollen() {
    }
}
