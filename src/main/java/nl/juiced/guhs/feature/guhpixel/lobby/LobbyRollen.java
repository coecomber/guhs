package nl.juiced.guhs.feature.guhpixel.lobby;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The small roles of the lobby slice: the Verkoper-guh (opens the Guhpixel shop with a sales pitch), and in the
 * Guh-internetcafé the Beheerder-guh ("Heb je hem al uit en weer aan gezet, njeg?") and the guhs asleep behind their
 * computers.
 */
public final class LobbyRollen {
    public static final int ROEPEN = 8, SNURKEN = 6;

    /** The Verkoper-guh in the doorway of the shop. */
    static final NpcRole VERKOPER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 0.9f);
            GuhQuests.say(p, npc, "gui.guhs.lobby.verkoper.roep." + Math.floorMod(p.getRandom().nextInt(ROEPEN), ROEPEN));
            Winkel.open(p);
        }
    };

    /** The Beheerder-guh of the Guh-internetcafé "De Trage Verbinding". */
    static final class Beheerder implements NpcRole {
        public static final int HOE = 1, TRAAG = 2, SLAPERS = 3;

        private static Praat.Optie[] opties() {
            return new Praat.Optie[] {new Praat.Optie(HOE, "gui.guhs.internetcafe.beheerder.optie.hoe"),
                    new Praat.Optie(TRAAG, "gui.guhs.internetcafe.beheerder.optie.traag"),
                    new Praat.Optie(SLAPERS, "gui.guhs.internetcafe.beheerder.optie.slapers")};
        }

        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 0.8f);
            GuhAdvancements.grant(p, "internetcafe_beheerder");
            Praat.open(p, npc, null, Toegang.heeft(p) ? "gui.guhs.internetcafe.beheerder.terug" : "gui.guhs.internetcafe.beheerder.hallo", new Object[0], opties());
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            switch (optie) {
                case HOE -> Praat.open(p, npc, null, "gui.guhs.internetcafe.beheerder.hoe", new Object[0], opties());
                case TRAAG -> Praat.open(p, npc, null, "gui.guhs.internetcafe.beheerder.traag", new Object[0], opties());
                case SLAPERS -> Praat.open(p, npc, null, "gui.guhs.internetcafe.beheerder.slapers", new Object[0], opties());
                default -> {
                }
            }
        }
    }

    static final Beheerder BEHEERDER = new Beheerder();

    /** A guh asleep behind an old beige computer. */
    static final NpcRole SLAPER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.5f, 0.6f);
            GuhQuests.say(p, npc, "gui.guhs.internetcafe.slaper." + Math.floorMod(npc.getId() + p.tickCount / 60, SNURKEN));
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if (npc.level() instanceof ServerLevel level && (npc.tickCount + npc.getId() * 13) % 70 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + npc.getBbHeight() + 0.2, npc.getZ(), 1, 0.05, 0.05, 0.05, 0.005);
            }
        }
    };

    private LobbyRollen() {
    }
}
