package nl.juiced.guhs.feature.klusjes;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * What a finished chore brings: hearts for the guh ({@code Reden.KLUSJE}, x1.5 when blij), the moment KLUSJE on the bus
 * (favorietjes, dagboek...), the dagboek stat KLUSJES, the first times ({@code eerste_klusje} and one per chore,
 * {@code klusjes_<id>}, each with a wist-je-datje the guh writes itself), a little sparkle and a happy "klaar!" sound. For
 * the owner: the advancements {@code lieve_vadsjes/klusjes_<id>} (the first time one of their residents does that chore),
 * {@code klusjes_alle} (all ten), {@code klusjes_honderd} (a hundred chores), {@code klusjes_bank} (sorted into a Bank Guh)
 * and {@code klusjes_zeldzaam} (a rare find). Per player: bits of the chores done in {@link #GEDAAN}, the count in {@link #TOTAAL}.
 */
public final class KlusBeloning {
    public static final String GEDAAN = "guhs_klusjes_gedaan", TOTAAL = "guhs_klusjes_totaal";
    public static final int HONDERD = 100;

    private KlusBeloning() {
    }

    public static void klaar(ServerLevel level, Huisje h, Mob mob, Klus klus, int aantal) {
        ServerPlayer owner = Band.eigenaarOnline(mob);
        Band.geefHartjes(mob, owner, Reden.KLUSJE.standaard(), Reden.KLUSJE);
        Band.moment(mob, owner, Moment.KLUSJE, klus.id());
        Dagboek.tel(mob, DagboekStat.KLUSJES, 1);
        Dagboek.eersteKeer(mob, owner, "eerste_klusje");
        if (Dagboek.eersteKeer(mob, null, "klusjes_" + klus.id())) {
            Dagboek.wistJeDat(mob, "gui.guhs.wistjedat.klusjes." + klus.id(), h.naamTekst());
        }
        level.playSound(null, mob.blockPosition(), KlusjesFeature.KLAAR.get(), SoundSource.NEUTRAL, 0.45f, 1.1f + mob.getRandom().nextFloat() * 0.3f);
        level.sendParticles(KlusjesFeature.STERRETJE.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 4, 0.25, 0.15, 0.25, 0.0);
        if (owner != null) {
            voortgang(owner, klus.id());
        }
    }

    /** The owner's chore bits, the count and the advancements. */
    public static void voortgang(ServerPlayer p, String id) {
        int i = KlusjesFeature.IDS.indexOf(id);
        if (i < 0) {
            return;
        }
        CompoundTag t = GuhQuests.saved(p);
        int bits = t.getIntOr(GEDAAN, 0);
        if ((bits & (1 << i)) == 0) {
            bits |= 1 << i;
            t.putInt(GEDAAN, bits);
            GidsFeature.grant(p, "lieve_vadsjes/klusjes_" + id);
            if (bits == (1 << KlusjesFeature.IDS.size()) - 1) {
                GidsFeature.grant(p, "lieve_vadsjes/klusjes_alle");
            }
        }
        int totaal = t.getIntOr(TOTAAL, 0) + 1;
        t.putInt(TOTAAL, totaal);
        if (totaal >= HONDERD) {
            GidsFeature.grant(p, "lieve_vadsjes/klusjes_honderd");
        }
    }

    /** How many different chores this player's residents did (0..10). */
    public static int soorten(ServerPlayer p) {
        return Integer.bitCount(GuhQuests.saved(p).getIntOr(GEDAAN, 0));
    }

    public static void bankGesorteerd(Mob mob) {
        ServerPlayer owner = Band.eigenaarOnline(mob);
        if (owner != null) {
            GidsFeature.grant(owner, "lieve_vadsjes/klusjes_bank");
        }
    }

    /** Something rare dug up or fished: the owner hears about it, the guh writes it down. */
    public static void zeldzaam(Mob mob, Huisje h, ItemStack vondst) {
        ServerPlayer owner = Band.eigenaarOnline(mob);
        Dagboek.wistJeDat(mob, "gui.guhs.wistjedat.klusjes.zeldzaam", vondst.getHoverName().copy(), h.naamTekst());
        if (owner != null) {
            GidsFeature.grant(owner, "lieve_vadsjes/klusjes_zeldzaam");
        }
        if (owner != null && h.meldingen()) {   // 1.2.5: switchable in the huisje screen
            owner.sendSystemMessage(Component.translatable("gui.guhs.klusjes.zeldzaam", mob.getName(), vondst.getHoverName(), h.naamTekst())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
