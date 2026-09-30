package nl.juiced.guhs.feature.sterrenwacht;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Professor Sterretje (STERRENKIJKERGUH), in the Guh-Sterrenwacht: tells you about the guh constellations (a tip each
 * time you talk to him; at night: "kijk door de telescoop!"), reminds you of the Knusfeest's sterrenlantaarns, and runs
 * a little shop for wenssterren.
 */
public final class SterrenwachtRole implements NpcRole {
    public static final SterrenwachtRole INSTANCE = new SterrenwachtRole();
    public static final int TIPS = 7;
    /** Prices in wenssterren. */
    public static final int PRIJS_MUTS = 4, PRIJS_CAPE = 6, PRIJS_LANTAARNS = 1, PRIJS_TELESCOOP = 8;
    private static final String GESPROKEN = "guhs_sterrenwacht_gesproken";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.95f);
        var saved = GuhQuests.saved(player);
        int keer = saved.getInt(GESPROKEN);
        saved.putInt(GESPROKEN, keer + 1);
        if (keer == 0) {
            GuhQuests.say(player, npc, "quest.guhs.sterrenwacht.hallo");
            KnusVoortgang.hoogste(player, Sterrenkijken.GEVONDEN, 1);
            GuhAdvancements.grant(player, "sterrenwacht_sterretje");
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(SterrenwachtFeature.WENSSTER_DEELTJE.get(), npc.getX(), npc.getY() + 1.8, npc.getZ(), 12, 0.5, 0.3, 0.5, 0.02);
            }
        } else if (Knusfeest.open(player, Feesttaak.STERRENLANTAARNS) && !Knusfeest.gebracht(player, Feesttaak.STERRENLANTAARNS)) {
            GuhQuests.say(player, npc, "quest.guhs.sterrenwacht.knusfeest", Sterrenkijken.LANTAARNS);
        } else if (Sterrenkijken.sterrenregen(player)) {
            GuhQuests.say(player, npc, "quest.guhs.sterrenwacht.regen");
            if (npc.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 2, npc.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
            }
        } else if (Sterrenkijken.donker(npc.level())) {
            GuhQuests.say(player, npc, Sterrenkijken.vannacht(player) >= Sterrenkijken.MAX_PER_NACHT ? "quest.guhs.sterrenwacht.nacht_moe"
                    : "quest.guhs.sterrenwacht.nacht");
        } else {
            GuhQuests.say(player, npc, "quest.guhs.sterrenwacht.tip" + (keer % TIPS));
        }
        npc.openShop(player);
    }

    /** For wenssterren: the sterrenkijker outfit, sterrenlantaarns and your own guh telescope. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_LANTAARNS, new ItemStack(SterrenwachtFeature.STERRENLANTAARN_ITEM.get(), 2)));
        offers.add(offer(PRIJS_MUTS, new ItemStack(ModItems.clothingItem(GuhClothes.STERRENKIJKERSMUTS))));
        offers.add(offer(PRIJS_CAPE, new ItemStack(ModItems.clothingItem(GuhClothes.STERRENCAPE))));
        offers.add(offer(PRIJS_TELESCOOP, new ItemStack(SterrenwachtFeature.TELESCOOP_ITEM.get())));
        return offers;
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(SterrenwachtFeature.WENSSTER.get(), price), result, Integer.MAX_VALUE, 0, 0);
    }

    private SterrenwachtRole() {
    }
}
