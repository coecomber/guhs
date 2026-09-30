package nl.juiced.guhs.feature.balto;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;

import static nl.juiced.guhs.feature.balto.BaltoVerhaal.AANGEKOMEN;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.BIJ_ROSY;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.HEEN;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.KLAAR;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.KLAAR_VOOR_TOCHT;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.NIEUW;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.ONTMOET;
import static nl.juiced.guhs.feature.balto.BaltoVerhaal.TERUG;

/**
 * The characters of Nomguh (NpcRollen, their lines: tools/features/balto_tekst.py, lang {@code gui.guhs.balto.<wie>.*}):
 * <ul>
 *   <li>{@link #BORIS}: a real goose (no guh!) with a Russian-flavoured accent who always knows what to do. Gak!</li>
 *   <li>{@link #STEELE}: Steele-Mika, the boastful champion sled leader (the one at the start line, plek "sledesprint", is
 *       balto-slee's race; every other Steele-Mika boasts here).</li>
 *   <li>{@link #MUK} and {@link #LUK}: two sweet polar-bear guhs at their igloo by the ice pond. Muk talks for two; Luk only
 *       nods (and sometimes hugs you).</li>
 *   <li>{@link #ROSY}: the sick baby guh in the ziekenhuisje (sneezes very cutely), the heart of the story.</li>
 *   <li>{@link #WITTE_WOLF}: the white wolf-guh, only at the dieptepunt of the trek; she glows and fades away after the howl.</li>
 * </ul>
 */
public final class BaltoRollen {

    /** Boris the goose, on the old boat. */
    public static final NpcRole BORIS = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.playSound(BaltoFeature.GAK.get(), 1f, 0.95f + npc.getRandom().nextFloat() * 0.15f);
            switch (BaltoVerhaal.stap(p)) {
                case NIEUW -> GuhQuests.say(p, npc, "gui.guhs.balto.boris.nieuw");
                case ONTMOET -> {
                    GuhQuests.say(p, npc, "gui.guhs.balto.boris.eerst_rosy");
                    GuhQuests.hint(p, "gui.guhs.balto.hint.rosy");
                }
                case BIJ_ROSY -> Praat.scene(p, "balto_boris", List.of(
                                BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.boris.1"),
                                BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.boris.2"),
                                BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.boris.3"),
                                BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.boris.4"),
                                BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.boris.5")),
                        new Praat.Optie(1, "gui.guhs.balto.optie.boris"));
                case KLAAR_VOOR_TOCHT -> {
                    GuhQuests.say(p, npc, "gui.guhs.balto.boris.ga");
                    GuhQuests.hint(p, "gui.guhs.balto.hint.start");
                }
                case HEEN, TERUG -> GuhQuests.say(p, npc, "gui.guhs.balto.boris.onderweg");
                case AANGEKOMEN -> GuhQuests.say(p, npc, "gui.guhs.balto.boris.naar_rosy");
                default -> GuhQuests.say(p, npc, "gui.guhs.balto.boris.klaar." + p.getRandom().nextInt(4));
            }
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            // now and then a little "gak" and a feather
            if ((npc.tickCount + npc.getId()) % 400 == 0 && npc.getRandom().nextInt(3) == 0 && npc.level() instanceof ServerLevel level
                    && level.getNearestPlayer(npc, 12) != null) {
                npc.playSound(BaltoFeature.GAK.get(), 0.6f, 1f + npc.getRandom().nextFloat() * 0.2f);
                level.sendParticles(ParticleTypes.CLOUD, npc.getX(), npc.getY() + 0.8, npc.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
    };

    /** Steele-Mika (every one without plek "sledesprint"): boasts, a bit differently at every step. */
    public static final NpcRole STEELE = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            int stap = BaltoVerhaal.stap(p);
            String key = switch (stap) {
                case NIEUW, ONTMOET -> "gui.guhs.balto.steele.begin." + p.getRandom().nextInt(3);
                case BIJ_ROSY, KLAAR_VOOR_TOCHT -> "gui.guhs.balto.steele.storm";
                case HEEN, TERUG -> "gui.guhs.balto.steele.onderweg";
                case AANGEKOMEN -> "gui.guhs.balto.steele.eer";
                default -> "gui.guhs.balto.steele.klaar." + p.getRandom().nextInt(3);
            };
            GuhQuests.say(p, npc, key);
        }
    };

    /** Muk: talks for two. */
    public static final NpcRole MUK = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            GuhQuests.say(p, npc, "gui.guhs.balto.muk." + (BaltoVerhaal.stap(p) >= KLAAR ? "klaar." : "") + p.getRandom().nextInt(3));
            mukluk(p, 1);
        }
    };

    /** Luk: never says a word (Muk does that), only nods, waves or hugs. */
    public static final NpcRole LUK = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            GuhQuests.say(p, npc, "gui.guhs.balto.luk." + p.getRandom().nextInt(4));
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.8, npc.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
            }
            mukluk(p, 2);
        }
    };

    /** Talked to both polar-bear guhs: the little side quest. */
    static void mukluk(ServerPlayer p, int bit) {
        var saved = GuhQuests.saved(p);
        int oud = saved.getInt(BaltoVerhaal.MUKLUK);
        int nu = oud | bit;
        saved.putInt(BaltoVerhaal.MUKLUK, nu);
        if (nu == 3 && oud != 3) {
            BaltoVerhaal.grant(p, "balto_mukluk");
        }
    }

    /** Rosy, in her little bed in the ziekenhuisje. */
    public static final NpcRole ROSY = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            switch (BaltoVerhaal.stap(p)) {
                case NIEUW -> {
                    niesje(npc);
                    GuhQuests.say(p, npc, "gui.guhs.balto.rosy.nieuw");
                }
                case ONTMOET -> {
                    niesje(npc);
                    Praat.scene(p, "balto_rosy", List.of(
                                    BaltoVerhaal.verteller("gui.guhs.balto.scene.rosy.1"),
                                    BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.rosy.2"),
                                    BaltoVerhaal.regel(null, "gui.guhs.balto.zuster", "gui.guhs.balto.scene.rosy.3"),
                                    BaltoVerhaal.regel(npc, "", "gui.guhs.balto.scene.rosy.4"),
                                    BaltoVerhaal.verteller("gui.guhs.balto.scene.rosy.5")),
                            new Praat.Optie(1, "gui.guhs.balto.optie.rosy"));
                }
                case BIJ_ROSY, KLAAR_VOOR_TOCHT, HEEN, TERUG -> {
                    niesje(npc);
                    GuhQuests.say(p, npc, "gui.guhs.balto.rosy.wachten." + p.getRandom().nextInt(2));
                }
                case AANGEKOMEN -> BaltoVerhaal.feest(p, npc);
                default -> {
                    GuhQuests.say(p, npc, "gui.guhs.balto.rosy.beter." + p.getRandom().nextInt(3));
                    if (npc.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.0, npc.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
                    }
                }
            }
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if ((npc.tickCount + npc.getId()) % 160 == 0 && npc.getRandom().nextInt(3) == 0 && npc.level() instanceof ServerLevel level
                    && level.getNearestPlayer(npc, 16) != null) {
                niesje(npc);
            }
        }
    };

    /** A tiny sneeze: "hatsjoe-njeg!" and a little puff. Nothing hurts. */
    static void niesje(GuhNpcEntity npc) {
        if (npc.level() instanceof ServerLevel level) {
            level.playSound(null, npc.blockPosition(), BaltoFeature.HATSJOE.get(), SoundSource.NEUTRAL, 0.6f, 1.25f + npc.getRandom().nextFloat() * 0.2f);
            var look = npc.getLookAngle();
            level.sendParticles(ParticleTypes.CLOUD, npc.getX() + look.x * 0.4, npc.getEyeY() - 0.1, npc.getZ() + look.z * 0.4, 3, 0.05, 0.05, 0.05, 0.02);
        }
    }

    /** The white wolf-guh: a whisper; she sparkles, and fades away when her time is over. */
    public static final NpcRole WITTE_WOLF = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            GuhQuests.say(p, npc, "gui.guhs.balto.wolf." + p.getRandom().nextInt(3));
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if (!(npc.level() instanceof ServerLevel level)) {
                return;
            }
            if (npc.tickCount % 4 == 0) {
                level.sendParticles(BaltoFeature.WOLFGLANS.get(), npc.getX(), npc.getY() + 0.9, npc.getZ(), 2, 0.5, 0.6, 0.5, 0.01);
            }
            if (npc.roleData.contains(BaltoVerhaal.WOLF_TOT) && level.getGameTime() > npc.roleData.getLong(BaltoVerhaal.WOLF_TOT)) {
                level.sendParticles(BaltoFeature.WOLFGLANS.get(), npc.getX(), npc.getY() + 1, npc.getZ(), 50, 0.6, 0.9, 0.6, 0.05);
                npc.discard();
            }
        }
    };

    private BaltoRollen() {
    }
}
