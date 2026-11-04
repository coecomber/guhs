package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;

/**
 * The Gouden Veren (ronde 18: the gold Pluimguh = 2 % from Vahoeg dungeon runs, or guaranteed after {@link #NODIG} feathers).
 * A counter in {@link Z3Data}; the item {@code vr_z3_gouden_veer} goes onto it with a right-click.
 * <p>API:
 * <ul>
 *   <li>z3-bazen: {@link #geef}(p, n) for a Vahoeg boss; {@link #rolGoudenPluimguh}(p) for the 2 % drop (Baron Bleu in Vahoeg form,
 *   or any Vahoeg run: the bazen slice decides where it rolls);</li>
 *   <li>z3-npc (stalmeester Veertje): {@link #kanRuilen}(p) / {@link #ruil}(p) = 25 feathers for the gold Pluimguh;</li>
 *   <li>the weekly bounty chest and hunting contracts give feathers here too.</li>
 * </ul>
 */
public final class GoudenVeren {
    public static final int NODIG = 25;
    public static final double DROPKANS = 0.02;

    private GoudenVeren() {
    }

    public static int aantal(ServerPlayer p) {
        return Z3Data.veren(p);
    }

    public static void geef(ServerPlayer p, int n) {
        if (n <= 0) {
            return;
        }
        Z3Data.zetVeren(p, Z3Data.veren(p) + n);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
        p.sendSystemMessage(Component.translatable(heeftGoud(p) ? "vadsrijk.z3.systemen.veer.erbij_al" : "vadsrijk.z3.systemen.veer.erbij",
                n, Z3Data.veren(p), NODIG).withStyle(ChatFormatting.GOLD));
    }

    /** The feather item (for loot tables / chests). */
    public static ItemStack item(int n) {
        return new ItemStack(Z3Spul.GOUDEN_VEER.get(), n);
    }

    public static boolean heeftGoud(ServerPlayer p) {
        return Rijdieren.heeftKleur(p, PluimKleur.GOUD);
    }

    public static boolean kanRuilen(ServerPlayer p) {
        return !heeftGoud(p) && Z3Data.veren(p) >= NODIG;
    }

    /** At Veertje: 25 feathers -> the gold Pluimguh (bound, with a server message). False when not enough / already have it. */
    public static boolean ruil(ServerPlayer p) {
        if (!kanRuilen(p)) {
            p.sendSystemMessage(Component.translatable(heeftGoud(p) ? "vadsrijk.z3.systemen.veer.al_goud" : "vadsrijk.z3.systemen.veer.te_weinig",
                    Z3Data.veren(p), NODIG).withStyle(ChatFormatting.GRAY));
            return false;
        }
        Z3Data.zetVeren(p, Z3Data.veren(p) - NODIG);
        geefGoud(p);
        return true;
    }

    /** The 2 % roll (true = it dropped and was given). */
    public static boolean rolGoudenPluimguh(ServerPlayer p) {
        if (heeftGoud(p) || p.getRandom().nextDouble() >= DROPKANS) {
            return false;
        }
        geefGoud(p);
        return true;
    }

    static void geefGoud(ServerPlayer p) {
        if (!Rijdieren.geefKleur(p, PluimKleur.GOUD)) {
            return;
        }
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.9f);
        var server = p.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z3.systemen.veer.melding", p.getDisplayName())
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        }
    }
}
