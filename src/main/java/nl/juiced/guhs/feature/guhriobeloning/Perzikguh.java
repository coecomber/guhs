package nl.juiced.guhs.feature.guhriobeloning;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Prinses Perzikguh in the tower room behind the duel. She was "taken for a piece of cake" and is having a lovely time:
 * the cake is very good. You can walk up the stairs to her at any time; what she says follows the player:
 * <ul>
 *     <li>before you won the duel: she whispers that she is not really a prisoner, and that the Grote Nether-Mika must
 *     not hear it ("win the duel first, he so likes to play");</li>
 *     <li>after the duel, once: her thanks, the princess crown and a cake to take home, and she points at Guhshi, who
 *     wants to come along ({@link Guhshi});</li>
 *     <li>afterwards: a chat (how is Guhshi?), and the crown again when you lost it before you ever wore it.</li>
 * </ul>
 */
public final class Perzikguh implements NpcRole {
    /** Questline flags: she thanked this player (crown + cake given). */
    public static final String BEDANKT = "perzik_bedankt";
    private static final String T = "quest.guhs.guhriobeloning.perzik.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        Verhaallijn lijn = GuhrioBeloningFeature.LIJN;
        if (!GuhrioKasteel.duelGewonnen(p)) {
            Praat.open(p, npc, null, T + (GuhrioKasteel.aantalGehaald(p) >= GuhrioKasteel.LEVELS.size() ? "bijna" : "wacht"), new Object[0]);
            return;
        }
        GuhClothes kroon = GuhClothes.GUHRIOBELONING_PRINSESSENKROON;
        if (!lijn.vlag(p, BEDANKT)) {
            lijn.vlag(p, BEDANKT, true);
            Minigames.give(p, new ItemStack(ModItems.clothingItem(kroon)));
            Minigames.give(p, new ItemStack(Items.CAKE));
            GuhAdvancements.grant(p, "guhrio_beloning_kroon");
            p.level().playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7f, 1.5f);
            p.level().sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + 1.6, npc.getZ(), 8, 0.4, 0.3, 0.4, 0.05);
            Praat.scene(p, "guhriobeloning_perzik", List.of(new Praat.Regel(npc, "", T + "dank.1"), new Praat.Regel(npc, "", T + "dank.2"),
                    new Praat.Regel(npc, "", T + (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI) ? "dank.3_getemd" : "dank.3"))));
            return;
        }
        if (!KledingUnlocks.heeft(p, kroon) && GuhQuests.count(p, ModItems.clothingItem(kroon)) == 0) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(kroon)));
            Praat.open(p, npc, null, T + "kroon_kwijt", new Object[0]);
            return;
        }
        Praat.open(p, npc, null, T + (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI) ? "dag_guhshi" : "dag"), new Object[0]);
    }
}
