package nl.juiced.guhs.feature.einde2.zij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.bouwzij1.PostgootBlock;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;

/**
 * einde2 zij: Omashguh, koning Bumi-guh's questline ({@code omashguh}, five steps, earth): "drie proeven die anders werken
 * dan je denkt" and the chute race.
 * <ol start="0">
 *   <li>talk to koning Bumi-guh before his throne (he asks for earth bending);</li>
 *   <li>trial 1, the key in the waterfall of glass ({@code proef_1}): nobody climbs it; use your EARTH inside the room and
 *       the key falls out (it opens nothing: Bumi wanted to see how you think);</li>
 *   <li>trial 2, find Flopsie ({@code proef_2}: a tiny hutch with huge paw prints): Flopsie is the giant beside the throne;
 *       click it;</li>
 *   <li>trial 3, choose your opponent ({@code proef_3}): the right one is the old king himself; then his last question,
 *       "what is my name?" (the plain answer is right);</li>
 *   <li>the chute race: from {@code glijbaan_start} down a mail chute to {@code glijbaan_finish} within {@link #RACE_TICKS}.</li>
 * </ol>
 * Reward: the kristalsnoep-ring. Failing costs nothing: try again.
 */
public final class Omashguh {
    static final String Q = Zij.Q + "omash.";
    static final String S = Einde2Ids.OMASHGUH;
    public static final int RACE_TICKS = 20 * 40;
    static final String SLEUTEL_KIES = "einde2_zij_omash_kies", SLEUTEL_NAAM = "einde2_zij_omash_naam";

    /** (per player, not saved) the chute race: game time it started (-1: armed at the start, not yet sliding). */
    private static final Map<UUID, Long> RACE = new ConcurrentHashMap<>();
    /** (per player, not saved) the last zone of Omashguh they stood in, so a hint shows once per visit. */
    private static final Map<UUID, String> ZONE = new ConcurrentHashMap<>();

    private Omashguh() {
    }

    static Verhaallijn ln() {
        return ZijLijnen.OMASH;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.BUMI_GUH, S + "/bumi", new Rol());
        CastWezen.BIJ_KLIK.add(Omashguh::klikWezen);
        Krachten.NA_GEBRUIK.add(Omashguh::kracht);
        Praat.luister(SLEUTEL_KIES, (p, spreker, optie) -> kies(p, optie));
        Praat.luister(SLEUTEL_NAAM, (p, spreker, optie) -> naam(p, optie));
    }

    // --- koning Bumi-guh ---------------------------------------------------------------------------------------------------

    static final class Rol extends QuestRol {
        Rol() {
            super(ZijLijnen.OMASH);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("praat");
            switch (stap) {
                case 0 -> {
                    if (!Zij.element(p, ZijLijnen.OMASHGUH, npc)) {
                        npc.speel("giechel");
                        return;
                    }
                    ln().begin(p);
                    scherm(p, npc, Q + "welkom", new Praat.Optie(1, Q + "optie.doe_mee"), new Praat.Optie(2, Q + "optie.waarom"));
                }
                case 1 -> {
                    zeg(p, npc, Q + "proef1_nog");
                    hint(p, Q + "hint.proef1");
                }
                case 2 -> {
                    zeg(p, npc, Q + "proef2_nog");
                    hint(p, Q + "hint.proef2");
                }
                case 3 -> {
                    zeg(p, npc, Q + "proef3_nog");
                    hint(p, Q + "hint.proef3");
                }
                case 4 -> {
                    zeg(p, npc, Q + "race");
                    hint(p, Q + "hint.race");
                }
                default -> {
                    npc.speel(p.getRandom().nextInt(3) == 0 ? "kristalsnoep" : "giechel");
                    zeg(p, npc, Q + "dank." + p.getRandom().nextInt(4));
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap != 0) {
                return;
            }
            if (optie == 2) {
                scherm(p, npc, Q + "uitleg", new Praat.Optie(1, Q + "optie.doe_mee"));
            } else if (optie == 1 && verder(p, 0)) {
                npc.speel("spierbal");
                zeg(p, npc, Q + "proef1");
                hint(p, Q + "hint.proef1");
            }
        }
    }

    // --- trial 1: the key in the waterfall -------------------------------------------------------------------------------

    /** An earth power used inside the room of trial 1 sets the key free. */
    static void kracht(ServerPlayer p, Krachten.Kracht k) {
        if (k.element() != Einde2Ids.Element.AARDE || ln().stap(p) != 1) {
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 8);
        if (kopie == null || !kopie.in("proef_1", p.position(), 1.5)) {
            return;
        }
        ServerLevel level = p.level();
        AABB zone = kopie.zone("proef_1");
        Vec3 val = zone == null ? p.position().add(0, 3, 0) : new Vec3((zone.minX + zone.maxX) / 2, zone.maxY - 2, (zone.minZ + zone.maxZ) / 2);
        level.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), true, false, val.x, val.y, val.z, 60, 1.2, 1.5, 1.2, 0.2);
        level.sendParticles(p, ParticleTypes.WAX_ON, true, false, val.x, val.y, val.z, 20, 0.6, 1.0, 0.6, 0.1);
        Zij.geluid(p, SoundEvents.GLASS_BREAK, 1f, 0.8f);
        Zij.geluid(p, SoundEvents.CHAIN_FALL, 1f, 1.2f);
        if (ln().verder(p, 1)) {
            Zij.stem(p, naam(), Q + "proef1_klaar");
            Zij.hint(p, Q + "hint.proef2");
            giechel(level, kopie);
        }
    }

    // --- trial 2: find Flopsie ---------------------------------------------------------------------------------------------

    static boolean klikWezen(CastWezen w, ServerPlayer p) {
        if (!w.wie().equals("flopsie") || !Zij.plek(w).startsWith(S + "/") || ln().stap(p) != 2) {
            return false;
        }
        w.zetAnimatie("knuffel");
        w.level().playSound(null, w.blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.4f);
        ((ServerLevel) w.level()).sendParticles(ParticleTypes.HEART, w.getX(), w.getY() + 2.2, w.getZ(), 6, 0.6, 0.4, 0.6, 0);
        if (ln().verder(p, 2)) {
            Zij.stem(p, naam(), Q + "proef2_klaar");
            Zij.hint(p, Q + "hint.proef3");
        }
        return true;
    }

    // --- trial 3: choose your opponent, and his name -------------------------------------------------------------------------

    static void kiesScherm(ServerPlayer p, String tekst) {
        Praat.scene(p, SLEUTEL_KIES, List.of(new Praat.Regel(null, Q + "naam.bumi", tekst)),
                new Praat.Optie(1, Q + "kies.1"), new Praat.Optie(2, Q + "kies.2"), new Praat.Optie(3, Q + "kies.3"));
    }

    private static void kies(ServerPlayer p, int optie) {
        if (ln().stap(p) != 3 || optie < 0) {
            return;
        }
        if (optie != 3) {
            Zij.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.7f);
            kiesScherm(p, Q + "kies_fout." + optie);
            return;
        }
        Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.6f);
        naamScherm(p, Q + "naam_vraag");
    }

    static void naamScherm(ServerPlayer p, String tekst) {
        Praat.scene(p, SLEUTEL_NAAM, List.of(new Praat.Regel(null, Q + "naam.bumi", Q + "kies_goed"), new Praat.Regel(null, Q + "naam.bumi", tekst)),
                new Praat.Optie(1, Q + "naam_optie.1"), new Praat.Optie(2, Q + "naam_optie.2"), new Praat.Optie(3, Q + "naam_optie.3"));
    }

    private static void naam(ServerPlayer p, int optie) {
        if (ln().stap(p) != 3 || optie < 0) {
            return;
        }
        if (optie != 3) {
            Zij.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.7f);
            Praat.scene(p, SLEUTEL_NAAM, List.of(new Praat.Regel(null, Q + "naam.bumi", Q + "naam_fout." + optie)),
                    new Praat.Optie(1, Q + "naam_optie.1"), new Praat.Optie(2, Q + "naam_optie.2"), new Praat.Optie(3, Q + "naam_optie.3"));
            return;
        }
        if (ln().verder(p, 3)) {
            Praat.sluit(p);
            Zij.stem(p, naam(), Q + "proef3_klaar");
            Zij.hint(p, Q + "hint.race");
            Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
        }
    }

    // --- every few ticks: zones and the race ---------------------------------------------------------------------------------

    static void tick(ServerPlayer p) {
        int stap = ln().stap(p);
        if (stap < 1 || stap > 4) {
            RACE.remove(p.getUUID());
            return;
        }
        ZijPlek.Kopie kopie = ZijPlek.bij(p, S, 8);
        if (kopie == null) {
            RACE.remove(p.getUUID());
            ZONE.remove(p.getUUID());
            return;
        }
        Vec3 pos = p.position();
        String zone = kopie.in("proef_1", pos, 0.5) ? "proef_1" : kopie.in("proef_2", pos, 0.5) ? "proef_2" : kopie.in("proef_3", pos, 0.5) ? "proef_3" : "";
        String was = ZONE.put(p.getUUID(), zone);
        if (!zone.equals(was) && !zone.isEmpty()) {
            if (stap == 1 && zone.equals("proef_1")) {
                Zij.melding(p, Q + "zone.1");
            } else if (stap == 2 && zone.equals("proef_2")) {
                Zij.melding(p, Q + "zone.2");
            } else if (stap == 3 && zone.equals("proef_3")) {
                kiesScherm(p, Q + "kies");
            }
        }
        if (stap == 4) {
            race(p, kopie);
        }
    }

    private static void race(ServerPlayer p, ZijPlek.Kopie kopie) {
        UUID id = p.getUUID();
        long nu = p.level().getGameTime();
        Long start = RACE.get(id);
        if (start == null) {
            if (kopie.bij("glijbaan_start", p.position(), 5)) {
                RACE.put(id, -1L);
                Zij.melding(p, Q + "race_klaar");
            }
            return;
        }
        if (start < 0) {
            if (PostgootBlock.opGoot(p)) {
                RACE.put(id, nu);
                Zij.titel(p, Component.translatable(Q + "race_titel").withStyle(ChatFormatting.GOLD), null, 2, 20, 6);
                p.level().playSound(null, p.blockPosition(), E2Audio.geluid("deuntje.glijbaan").get(), net.minecraft.sounds.SoundSource.RECORDS, 1f, 1f);
            } else if (!kopie.bij("glijbaan_start", p.position(), 12)) {
                RACE.remove(id);
            }
            return;
        }
        long tijd = nu - start;
        if (tijd % 10 == 0) {
            p.sendOverlayMessage(Component.translatable(Q + "race_tijd", nl.juiced.guhs.quest.Highscores.tijd((int) tijd)).withStyle(ChatFormatting.YELLOW));
        }
        if (kopie.bij("glijbaan_finish", p.position(), 6)) {
            RACE.remove(id);
            if (tijd <= RACE_TICKS) {
                gewonnen(p, kopie, (int) tijd);
            } else {
                Zij.stem(p, naam(), Q + "race_te_laat", nl.juiced.guhs.quest.Highscores.tijd((int) tijd));
            }
        } else if (tijd > RACE_TICKS + 20 * 20) {
            RACE.remove(id);
            Zij.melding(p, Q + "race_kwijt");
        }
    }

    private static void gewonnen(ServerPlayer p, ZijPlek.Kopie kopie, int tijd) {
        if (!ln().verder(p, 4)) {
            return;
        }
        ServerLevel level = p.level();
        Zij.titel(p, Component.translatable(Q + "race_gewonnen").withStyle(ChatFormatting.GOLD), Component.translatable(Q + "race_tijd",
                nl.juiced.guhs.quest.Highscores.tijd(tijd)), 4, 50, 10);
        level.sendParticles(p, ParticleTypes.FIREWORK, true, false, p.getX(), p.getY() + 1, p.getZ(), 40, 0.8, 0.8, 0.8, 0.15);
        Zij.geluid(p, SoundEvents.PLAYER_LEVELUP, 1f, 1f);
        Zij.stem(p, naam(), Q + "klaar");
        Zij.eenmalig(ln(), p, "beloning", new ItemStack(E2ZijFeature.KRISTALSNOEP_RING.get()),
                new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 16));
        giechel(level, kopie);
    }

    /** The old king laughs, wherever he stands in this copy. */
    private static void giechel(ServerLevel level, ZijPlek.Kopie kopie) {
        BlockPos bumi = kopie.pos("bumi");
        if (bumi != null) {
            GuhNpcEntity npc = Zij.npc(level, Vec3.atCenterOf(bumi), GuhNpcEntity.Kind.BUMI_GUH, 10);
            if (npc != null) {
                npc.speel("giechel");
            }
        }
    }

    static String naam() {
        return Q + "naam.bumi";
    }

    /** (dev, tests) forgets the race. */
    static void vergeet(UUID id) {
        RACE.remove(id);
        ZONE.remove(id);
    }

    /** (tests) is this player's race running (true) or armed (false); null: none. */
    static Boolean race(UUID id) {
        Long s = RACE.get(id);
        return s == null ? null : s >= 0;
    }
}
