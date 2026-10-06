package nl.juiced.guhs.feature.ring;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (ring-kern): what the story gives at the end, and the small party afterwards (DESIGN_130 4, "Rewards and afterwards").
 * <ul>
 *   <li>{@link #geef}: once per player, the moment chapter 6 is done (ring-kern listens to that questline itself; ring-h6
 *       may call it earlier, at the feast): the four outfits ({@link #KLEDING}, source "ring"), the Oog van Sausron statuette
 *       (ring-h5's block), a few hooks for the Elfentouw, Sam-guh free to take home ({@code VerhaalGuhs.geefVrij}: a click
 *       on him tames him, once), Smikagol as a buddy ({@link Smikagol#maakMaatje}) and the advancement that closes the tab.
 *       The title "Ringdrager" needs nothing: it looks at the questline.</li>
 *   <li>{@link #feest}: the daily party in the Knabbelgouw. ring-h1 calls it from whoever hosts the party (Guhdalf, the party
 *       table): one treat per player per day, only after the story.</li>
 * </ul>
 */
public final class RingBeloning {
    /** The four outfits: Guhdalf's hat with beard, the elf cloak with its leaf pin, hairy hobbit feet, the ring on a chain. */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.RING_GUHDALFHOED, GuhClothes.RING_ELFENMANTEL, GuhClothes.RING_HOBBITVOETEN,
            GuhClothes.RING_RINGKETTING);
    /** The KledingBronnen source of the outfits. */
    public static final String BRON = "ring";
    static final String GEGEVEN = "guhs_ring_beloond", FEEST_DAG = "guhs_ring_feest_dag";
    public static final int HAKEN = 4;

    /** Did this player get the rewards of the story? */
    public static boolean gegeven(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(GEGEVEN, false);
    }

    /** The rewards of the whole story, once per player (false: they have them already, or the story isn't done). */
    public static boolean geef(ServerPlayer p) {
        if (!Ring.klaar(p) || gegeven(p)) {
            return false;
        }
        GuhQuests.saved(p).putBoolean(GEGEVEN, true);
        for (GuhClothes c : KLEDING) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(c)));
        }
        Minigames.give(p, new ItemStack(RingH5Feature.OOG_VAN_SAUSRON_BEELDJE_ITEM.get()));
        Minigames.give(p, new ItemStack(RingFeature.ELFENTOUW_HAAK_ITEM.get(), HAKEN));
        Gaven.geefAlsKwijt(p, RingFeature.ELFENTOUW.get());
        // Sam-guh may come home for good (a click on him), Smikagol is a buddy from now on
        VerhaalGuhs.geefVrij(p, VerhaalGuh.SAM_GUH);
        GuhQuests.saved(p).putBoolean(Sam.MEE, true);
        Smikagol.stuurWeg(p);
        Smikagol.maakMaatje(p);
        GuhAdvancements.grant(p, "ring_klaar");
        GidsFeature.grant(p, "knabbelring/ring_ringdrager");
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
        p.sendSystemMessage(Component.translatable("quest.guhs.ring.beloning").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** May this player have today's treat (the story is done and they did not have one today)? */
    public static boolean magFeesten(ServerPlayer p) {
        return Ring.klaar(p) && GuhQuests.saved(p).getLongOr(FEEST_DAG, -1L) != Band.dag(p.level().getServer());
    }

    /**
     * The daily party in the Knabbelgouw: one Feestknabbel per player per day, after the story. The host (an NPC, or null)
     * says a line either way; a player who is still on the trip hears that the party waits for them. True: a treat was given.
     */
    public static boolean feest(ServerPlayer p, @Nullable Entity gastheer) {
        if (!Ring.klaar(p)) {
            zeg(p, gastheer, "quest.guhs.ring.feest.nog_niet");
            return false;
        }
        if (!magFeesten(p)) {
            zeg(p, gastheer, "quest.guhs.ring.feest.morgen");
            return false;
        }
        CompoundTag saved = GuhQuests.saved(p);
        saved.putLong(FEEST_DAG, Band.dag(p.level().getServer()));
        Minigames.give(p, new ItemStack(RingFeature.FEESTKNABBEL.get()));
        zeg(p, gastheer, "quest.guhs.ring.feest.traktatie." + p.getRandom().nextInt(3));
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1.6, p.getZ(), 30, 0.6, 0.6, 0.6, 0.12);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.7f, 1.2f);
        GuhAdvancements.grant(p, "ring_feest");
        return true;
    }

    private static void zeg(ServerPlayer p, @Nullable Entity gastheer, String key) {
        if (gastheer != null) {
            GuhQuests.say(p, gastheer, key);
        } else {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private RingBeloning() {
    }
}
