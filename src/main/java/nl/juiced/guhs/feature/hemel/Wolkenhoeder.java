package nl.juiced.guhs.feature.hemel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.band.Wolkjes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The wolkenhoeder (NPC WOLKENHOEDER), next to the Knuffelhart in every Hemelkapelletje. First visit: a little scene about
 * the sleeping heart and the three things it needs ({@link HemelQuest.Ding}). Each visit after that he takes what you
 * brought (one of each); with all three the heart wakes up (a scene, the outfits). After that he offers the revive screen.
 */
public class Wolkenhoeder implements NpcRole {
    public static final String INTRO = "hemel_intro", KLOPT = "hemel_klopt";
    /** Answer ids. */
    public static final int ZOEKEN = 1, WAAR = 2, WOLKJES = 3, VERTEL = 4;
    /** roleData: the Knuffelhart next to him (found once). */
    private static final String HART = "HemelHart";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.9f, 1.25f);
        GuhAdvancements.grant(p, "hemel_hoeder");
        GidsFeature.grant(p, "verhalen/hemel_hoeder");
        switch (HemelQuest.stap(p)) {
            case 0 -> {
                HemelQuest.zetStap(p, 1);
                List<Praat.Regel> regels = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    regels.add(new Praat.Regel(npc, "", "gui.guhs.hemel.intro." + i));
                }
                Praat.scene(p, INTRO, regels, new Praat.Optie(ZOEKEN, "gui.guhs.hemel.optie.zoeken"), new Praat.Optie(WAAR, "gui.guhs.hemel.optie.waar"));
            }
            case 1 -> verzamel(npc, p);
            default -> klaar(npc, p);
        }
    }

    /** Step 1: take what the player brought; all three: the heart wakes up. */
    private void verzamel(GuhNpcEntity npc, ServerPlayer p) {
        List<HemelQuest.Ding> gebracht = HemelQuest.breng(p);
        BlockPos hart = hart(npc);
        for (HemelQuest.Ding d : gebracht) {
            GuhQuests.say(p, npc, "gui.guhs.hemel.gebracht." + d.id());
            if (hart != null && npc.level() instanceof ServerLevel level) {
                level.sendParticles(HemelFeature.STERRETJE.get(), hart.getX() + 0.5, hart.getY() + 0.6, hart.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, hart, HemelFeature.STER.get(), SoundSource.BLOCKS, 0.8f, 1f);
            }
        }
        if (HemelQuest.nodig(p).isEmpty()) {
            wordtWakker(npc, p, hart);
            return;
        }
        if (gebracht.isEmpty()) {
            Praat.open(p, npc, null, nodigKey(p), new Object[0], new Praat.Optie(WAAR, "gui.guhs.hemel.optie.waar"));
        } else {
            GuhQuests.say(p, npc, nodigKey(p));
        }
        GuhQuests.hint(p, "quest.guhs.next.hemel_zoeken");
    }

    /** "Het Knuffelhart heeft nog nodig: een guhkristal en een pluisveertje" (lang key per combination: the bits still missing). */
    static String nodigKey(ServerPlayer p) {
        int mask = 0;
        for (HemelQuest.Ding d : HemelQuest.nodig(p)) {
            mask |= d.bit();
        }
        return "gui.guhs.hemel.nodig." + mask;
    }

    /** All three brought: the heart beats (for this player), a scene, the outfits. */
    static void wordtWakker(@Nullable Entity spreker, ServerPlayer p, @Nullable BlockPos hart) {
        HemelQuest.wakker(p);
        if (hart != null && p.level() instanceof ServerLevel level) {
            double x = hart.getX() + 0.5, y = hart.getY() + 0.6, z = hart.getZ() + 0.5;
            level.sendParticles(ParticleTypes.HEART, x, y + 0.4, z, 14, 0.8, 0.5, 0.8, 0.05);
            level.sendParticles(HemelFeature.STERRETJE.get(), x, y, z, 40, 1.2, 0.8, 1.2, 0.04);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 20, 0.6, 0.6, 0.6, 0.06);
            level.playSound(null, hart, HemelFeature.HARTKLOP.get(), SoundSource.BLOCKS, 1.2f, 1f);
            level.playSound(null, hart, HemelFeature.TERUG.get(), SoundSource.BLOCKS, 1f, 1f);
        }
        if (spreker != null) {
            List<Praat.Regel> regels = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                regels.add(new Praat.Regel(spreker, "", "gui.guhs.hemel.klopt." + i));
            }
            Praat.scene(p, KLOPT, regels);
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.hemel.klopt.chat").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }

    /** After the questline: how many are in the wolkjes, and the screen. */
    private void klaar(GuhNpcEntity npc, ServerPlayer p) {
        int n = Wolkjes.dood(p.server, p.getUUID()).size();
        if (n > 0) {
            Praat.open(p, npc, null, "gui.guhs.hemel.hoeder.klaar", new Object[]{n}, new Praat.Optie(WOLKJES, "gui.guhs.hemel.optie.wolkjes"),
                    new Praat.Optie(VERTEL, "gui.guhs.hemel.optie.vertel"));
        } else {
            Praat.open(p, npc, null, "gui.guhs.hemel.hoeder.klaar_leeg", new Object[0], new Praat.Optie(VERTEL, "gui.guhs.hemel.optie.vertel"));
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        switch (optie) {
            case WAAR -> Praat.open(p, npc, null, "gui.guhs.hemel.waar", new Object[0]);
            case ZOEKEN -> {
                Praat.sluit(p);
                GuhQuests.hint(p, "quest.guhs.next.hemel_zoeken");
            }
            case WOLKJES -> {
                BlockPos hart = hart(npc);
                if (hart != null && HemelQuest.klopt(p)) {
                    Praat.sluit(p);
                    Hemel.openScherm(p, hart);
                }
            }
            case VERTEL -> Praat.open(p, npc, null, "gui.guhs.hemel.vertel", new Object[0]);
            default -> {
            }
        }
    }

    /** The listeners of the two scenes (registered once from HemelFeature.register). */
    static void luisteraars() {
        Praat.luister(INTRO, (p, spreker, optie) -> {
            if (optie == WAAR && spreker != null) {
                Praat.open(p, spreker, null, "gui.guhs.hemel.waar", new Object[0]);
            } else if (optie == ZOEKEN) {
                Praat.sluit(p);
                GuhQuests.hint(p, "quest.guhs.next.hemel_zoeken");
            }
        });
        Praat.luister(KLOPT, (p, spreker, optie) -> {
            if (optie < 0) {
                GuhQuests.hint(p, "quest.guhs.next.hemel_terug");
            }
        });
    }

    /** The Knuffelhart next to this wolkenhoeder (searched once within 10 blocks, then remembered). */
    @Nullable
    static BlockPos hart(GuhNpcEntity npc) {
        if (npc.roleData.contains(HART)) {
            BlockPos pos = BlockPos.of(npc.roleData.getLong(HART));
            if (npc.level().getBlockState(pos).is(HemelFeature.KNUFFELHART.get())) {
                return pos;
            }
        }
        BlockPos found = zoekHart(npc.level(), npc.blockPosition(), 10, 4);
        if (found != null) {
            npc.roleData.putLong(HART, found.asLong());
        }
        return found;
    }

    @Nullable
    static BlockPos zoekHart(net.minecraft.world.level.Level level, BlockPos near, int r, int ry) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(near.offset(-r, -ry, -r), near.offset(r, ry, r))) {
            if (level.getBlockState(pos).is(HemelFeature.KNUFFELHART.get())) {
                double d = pos.distSqr(near);
                if (d < bestD) {
                    bestD = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }
}
