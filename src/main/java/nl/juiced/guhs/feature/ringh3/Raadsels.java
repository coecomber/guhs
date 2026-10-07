package nl.juiced.guhs.feature.ringh3;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h3): the three riddles of the mine. Every one is a player's OWN: it only counts for a player who is exactly at
 * that step of ring_h3 ({@link Ring#aanZet}), its progress is kept in that player's questline (counters), and what it opens is
 * a door that closes again by itself ({@link Deuren}). So any number of players solve them, side by side or years apart, and a
 * friend who walks along can't solve yours.
 * <ol>
 *   <li><b>The gate</b> (step 1): "Zeg njeg en treed binnen." Say the guh word in the chat near the gate ("njeg", or the
 *       English "nyeg"), or click the glowing inscription and pick it from three answers.</li>
 *   <li><b>The levers</b> (step 2): four levers under four emblems, to be pulled in the order of the rhyme on the stele
 *       ({@link Plekken#HENDEL_VOLGORDE}); a wrong one and you start again.</li>
 *   <li><b>Gimguh's door</b> (step 4): knock three times on the rune of what Durguh loved most (his tomb says: cheese).</li>
 * </ol>
 */
public final class Raadsels {
    /** The questline counters. */
    public static final String HEFBOOM = "hefboom", HEFBOOM_FOUT = "hefboom_fout", KLOP = "klop", KLOP_FOUT = "klop_fout", POORT_FOUT = "poort_fout";
    /** The secret word of the gate. */
    public static final List<String> WOORD = List.of("njeg", "nyeg");
    /** Within this many blocks of the gate the word counts. */
    public static final double POORT_BEREIK = 16;
    /** The Praat screen of the inscription: its sleutel and the right answer. */
    public static final String PRAAT_POORT = "ringh3_poort";
    public static final int ANTWOORD_NJEG = 2;
    /** The lang names of the eight rune signs (gui.guhs.ringh3.rune.&lt;name&gt;). */
    public static final String[] TEKEN_NAMEN = {"kaas", "worst", "saus", "knabbel", "bot", "vlam", "njeg", "trommel"};

    private Raadsels() {
    }

    private static Verhaallijn lijn() {
        return RingH3Feature.LIJN;
    }

    static void registreer() {
        Praat.luister(PRAAT_POORT, (p, spreker, optie) -> {
            Mijn m = Mijn.van(p);
            if (m == null || optie < 0) {
                return;
            }
            if (optie == ANTWOORD_NJEG) {
                poortOpen(p, m, false);
            } else {
                poortFout(p, m);
            }
        });
    }

    // =====================================================================================================================
    // the gate
    // =====================================================================================================================

    public static boolean zegtWoord(String tekst) {
        String t = tekst.toLowerCase(Locale.ROOT);
        return WOORD.stream().anyMatch(t::contains);
    }

    /** (the chat) a player said something: the word, near the gate, at the step of the gate? */
    static void chat(ServerPlayer p, String tekst) {
        if (!zegtWoord(tekst)) {
            return;
        }
        Mijn m = Mijn.van(p);
        if (m == null || !Ring.aanZet(p, lijn(), 1) || !bijPoort(p, m)) {
            return;
        }
        poortOpen(p, m, true);
    }

    static boolean bijPoort(ServerPlayer p, Mijn m) {
        return m.midden(Plekken.POORT_BUITEN).closerThan(p.position(), POORT_BEREIK);
    }

    /** A click on the inscription. */
    static void poortKlik(ServerPlayer p, Mijn m) {
        int stap = lijn().stap(p);
        if (!lijn().aanDeBeurt(p) || stap < 1) {
            GuhQuests.hint(p, "quest.guhs.ringh3.poort.lees");
            return;
        }
        if (stap > 1) {
            GuhQuests.hint(p, "quest.guhs.ringh3.poort.al_open");
            Deuren.open(m, Deuren.Deur.WEST);
            return;
        }
        Praat.scene(p, PRAAT_POORT, List.of(new Praat.Regel(null, "gui.guhs.ringh3.poort.naam", "gui.guhs.ringh3.poort.schrift")),
                new Praat.Optie(1, "gui.guhs.ringh3.poort.antwoord.vads"), new Praat.Optie(ANTWOORD_NJEG, "gui.guhs.ringh3.poort.antwoord.njeg"),
                new Praat.Optie(3, "gui.guhs.ringh3.poort.antwoord.sesam"));
    }

    private static void poortFout(ServerPlayer p, Mijn m) {
        if (!Ring.aanZet(p, lijn(), 1)) {
            return;
        }
        int fout = lijn().teller(p, POORT_FOUT) + 1;
        lijn().teller(p, POORT_FOUT, fout);
        Vec3 poort = m.midden(Plekken.POORT_BUITEN);
        m.level().playSound(null, poort.x, poort.y, poort.z, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0f, 0.5f);
        GuhQuests.hint(p, "quest.guhs.ringh3.poort.fout." + Math.min(3, fout));
    }

    /** The gate opens for this player: the riddle is solved. */
    static void poortOpen(ServerPlayer p, Mijn m, boolean gezegd) {
        if (!lijn().verder(p, 1)) {
            return;
        }
        Deuren.open(m, Deuren.Deur.WEST);
        ServerLevel level = m.level();
        Vec3 schrift = m.midden(Plekken.POORT_SCHRIFT);
        level.sendParticles(ParticleTypes.END_ROD, schrift.x, schrift.y, schrift.z, 60, 2.2, 1.6, 2.2, 0.03);
        level.playSound(null, schrift.x, schrift.y, schrift.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4f, 0.6f);
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.poort.open").withStyle(ChatFormatting.AQUA));
        Ring.behaald(p, "ring_h3_poort");
        if (gezegd) {
            Ring.behaald(p, "ring_h3_njeg");
        }
    }

    // =====================================================================================================================
    // the levers
    // =====================================================================================================================

    /** A player pulled lever nr (the block already moved and springs back by itself). */
    static void hendel(ServerPlayer p, BlockPos pos, int nr) {
        ServerLevel level = p.level();
        Mijn m = Mijn.van(p);
        if (m == null || !Plekken.HENDELS.contains(m.lokaal(pos))) {
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.7f);
            return;
        }
        if (!Ring.aanZet(p, lijn(), 2)) {
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.7f);
            GuhQuests.hint(p, lijn().stap(p) > 2 ? "quest.guhs.ringh3.hefboom.al_gedaan" : "quest.guhs.ringh3.hefboom.niet_van_jou");
            if (lijn().stap(p) > 2) {
                Deuren.open(m, Deuren.Deur.VALHEK);
            }
            return;
        }
        int n = lijn().teller(p, HEFBOOM);
        if (nr == Plekken.HENDEL_VOLGORDE[Math.min(n, Plekken.HENDEL_VOLGORDE.length - 1)]) {
            n++;
            lijn().teller(p, HEFBOOM, n);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 0.6f + n * 0.15f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            if (n >= Plekken.HENDEL_VOLGORDE.length) {
                lijn().teller(p, HEFBOOM, 0);
                if (lijn().verder(p, 2)) {
                    Deuren.open(m, Deuren.Deur.VALHEK);
                    p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.hefboom.open").withStyle(ChatFormatting.GOLD));
                    Ring.behaald(p, "ring_h3_hefbomen");
                }
            } else {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh3.hefboom.goed", n, Plekken.HENDEL_VOLGORDE.length).withStyle(ChatFormatting.GREEN));
            }
            return;
        }
        // the wrong one: everything springs back, start again (and after a few tries Sam-guh reads the stele aloud)
        lijn().teller(p, HEFBOOM, 0);
        int fout = lijn().teller(p, HEFBOOM_FOUT) + 1;
        lijn().teller(p, HEFBOOM_FOUT, fout);
        level.playSound(null, pos, RingH3Feature.KLONK.get(), SoundSource.BLOCKS, 1.0f, 0.7f);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh3.hefboom.fout").withStyle(ChatFormatting.RED));
        if (fout == 2 || fout % 4 == 0) {
            samZegt(p, "quest.guhs.ringh3.hefboom.sam");
        }
    }

    // =====================================================================================================================
    // the runes
    // =====================================================================================================================

    /** A player knocked on a rune stone with this sign. */
    static void rune(ServerPlayer p, BlockPos pos, int teken) {
        ServerLevel level = p.level();
        level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1.0f, 0.6f + teken * 0.07f);
        Mijn m = Mijn.van(p);
        if (m == null) {
            // a rune stone somebody put down at home: a knock turns it to the next sign
            int volgende = (teken + 1) % TEKEN_NAMEN.length;
            level.setBlock(pos, level.getBlockState(pos).setValue(RingH3Blocks.Rune.TEKEN, volgende), net.minecraft.world.level.block.Block.UPDATE_ALL);
            p.sendOverlayMessage(naam(volgende));
            return;
        }
        if (teken == Plekken.RUNE_NJEG) {
            poortKlik(p, m);
            return;
        }
        if (!Plekken.RUNEN.contains(m.lokaal(pos))) {
            p.sendOverlayMessage(naam(teken));
            return;
        }
        if (!Ring.aanZet(p, lijn(), 4)) {
            p.sendOverlayMessage(naam(teken));
            if (lijn().stap(p) > 4) {
                Deuren.open(m, Deuren.Deur.GEHEIM);
            }
            return;
        }
        if (teken == Plekken.RUNE_GOED) {
            int n = lijn().teller(p, KLOP) + 1;
            lijn().teller(p, KLOP, n);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 1.2f, 0.7f + n * 0.12f);
            if (n >= Plekken.RUNE_KLOPPEN) {
                lijn().teller(p, KLOP, 0);
                if (lijn().verder(p, 4)) {
                    Deuren.open(m, Deuren.Deur.GEHEIM);
                    p.sendSystemMessage(Component.translatable("quest.guhs.ringh3.rune.open").withStyle(ChatFormatting.GOLD));
                    Ring.behaald(p, "ring_h3_gang");
                }
            } else {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh3.rune.klop", n, Plekken.RUNE_KLOPPEN).withStyle(ChatFormatting.GREEN));
            }
            return;
        }
        lijn().teller(p, KLOP, 0);
        int fout = lijn().teller(p, KLOP_FOUT) + 1;
        lijn().teller(p, KLOP_FOUT, fout);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh3.rune.fout", naam(teken)).withStyle(ChatFormatting.RED));
        if (fout == 3 || fout % 6 == 0) {
            samZegt(p, "quest.guhs.ringh3.rune.sam");
        }
    }

    static Component naam(int teken) {
        return Component.translatable("gui.guhs.ringh3.rune." + TEKEN_NAMEN[Math.floorMod(teken, TEKEN_NAMEN.length)]);
    }

    /** A hint in the name of the player's own Sam-guh (or as a plain hint when he isn't around). */
    private static void samZegt(ServerPlayer p, String key) {
        var sam = Sam.van(p);
        if (sam != null) {
            GuhQuests.say(p, sam, key);
        } else {
            GuhQuests.hint(p, key);
        }
    }
}
