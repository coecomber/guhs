package nl.juiced.guhs.feature.grotspellen;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The hosts of the cave games: Kapitein Schepsnuit (Schatgraven), Juf Glimmie (Glimwormvangen) and Meester Knipperlicht
 * (Lichtjespuzzel). They run their game ({@link GrotSpel}) and keep a shop: their guh clothes (40-80 coins), building
 * blocks (1 coin for 4) and deco (20-64), for the game's own coin. Never sold out. And Snuffie, the snuffelguh of
 * Schatgraven ({@link #SNUFFIE}): he only says something funny when you click him.
 */
public final class GrotSpelRol implements NpcRole {
    public static final int PRIJS_PIRATENHOEDJE = 60, PRIJS_OOGKLEPJE = 40, PRIJS_ANTENNETJES = 60, PRIJS_VLINDERNETJE = 50,
            PRIJS_PADDENSTOELHOEDJE = 70;
    public static final int PRIJS_SCHATKISTJE = 40, PRIJS_GOUDEN_KAASJE = 24, PRIJS_GLIMPOTJE = 24, PRIJS_GLIMPLANT = 20,
            PRIJS_PADDENSTOELLAMPJE = 28, PRIJS_PADDENSTOELKRUKJE = 32, PRIJS_LAMPJESTEGEL = 40;
    /** Building blocks: this many for one coin. */
    public static final int BLOKKEN_PER_MUNT = 4;
    public static final int SNUFFIE_ZINNEN = 4;

    private final GrotSpel.Soort soort;

    GrotSpelRol(GrotSpel.Soort soort) {
        this.soort = soort;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        GrotSpel.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        GrotSpel spel = GrotSpel.van(npc);
        if (spel != null) {
            spel.tick(npc);
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        GrotSpel.antwoord(npc, player, optie);
    }

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        return winkel(soort);
    }

    /** The shop of a game. */
    public static MerchantOffers winkel(GrotSpel.Soort soort) {
        MerchantOffers o = new MerchantOffers();
        Item munt = soort.munt();
        switch (soort) {
            case SCHATGRAVEN -> {
                o.add(kleding(munt, PRIJS_OOGKLEPJE, GuhClothes.SCHAT_OOGKLEPJE));
                o.add(kleding(munt, PRIJS_PIRATENHOEDJE, GuhClothes.SCHAT_PIRATENHOEDJE));
                o.add(blokken(munt, GrotSpellenFeature.SCHATZANDSTEEN_ITEM.get()));
                o.add(blokken(munt, GrotSpellenFeature.SCHATKISTPLANKEN_ITEM.get()));
                bestaand(o, munt, "pluiswolblok");
                bestaand(o, munt, "viltkiezel");
                o.add(deco(munt, PRIJS_GOUDEN_KAASJE, GrotSpellenFeature.GOUDEN_KAASJE_ITEM.get()));
                o.add(deco(munt, PRIJS_SCHATKISTJE, GrotSpellenFeature.SCHATKISTJE_ITEM.get()));
            }
            case GLIMWORMVANGEN -> {
                o.add(kleding(munt, PRIJS_VLINDERNETJE, GuhClothes.GLIM_VLINDERNETJE));
                o.add(kleding(munt, PRIJS_ANTENNETJES, GuhClothes.GLIM_ANTENNETJES));
                o.add(blokken(munt, GrotSpellenFeature.NACHTVILT_ITEM.get()));
                o.add(blokken(munt, GrotSpellenFeature.GLIMMOS_ITEM.get()));
                o.add(deco(munt, PRIJS_GLIMPLANT, GrotSpellenFeature.GLIMPLANT_ITEM.get()));
                o.add(deco(munt, PRIJS_GLIMPOTJE, GrotSpellenFeature.GLIMPOTJE_ITEM.get()));
            }
            case LICHTJESPUZZEL -> {
                o.add(kleding(munt, PRIJS_PADDENSTOELHOEDJE, GuhClothes.LICHTJES_PADDENSTOELHOEDJE));
                o.add(blokken(munt, GrotSpellenFeature.PADDENSTOELSTEEN_ITEM.get()));
                o.add(blokken(munt, GrotSpellenFeature.STEELTJESSTEEN_ITEM.get()));
                o.add(deco(munt, PRIJS_PADDENSTOELLAMPJE, GrotSpellenFeature.PADDENSTOELLAMPJE_ITEM.get()));
                o.add(deco(munt, PRIJS_PADDENSTOELKRUKJE, GrotSpellenFeature.PADDENSTOELKRUKJE_ITEM.get()));
                o.add(deco(munt, PRIJS_LAMPJESTEGEL, GrotSpellenFeature.LAMPJESTEGEL_ITEM.get()));
            }
        }
        return o;
    }

    private static MerchantOffer kleding(Item munt, int prijs, GuhClothes c) {
        return new MerchantOffer(new ItemCost(munt, prijs), new ItemStack(ModItems.clothingItem(c)), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer blokken(Item munt, Item blok) {
        return new MerchantOffer(new ItemCost(munt, 1), new ItemStack(blok, BLOKKEN_PER_MUNT), Integer.MAX_VALUE, 0, 0);
    }

    private static MerchantOffer deco(Item munt, int prijs, Item ding) {
        return new MerchantOffer(new ItemCost(munt, prijs), new ItemStack(ding), Integer.MAX_VALUE, 0, 0);
    }

    /** A building block of another feature (Onder de Wol's pluiswol and viltkiezel), when it exists. */
    private static void bestaand(MerchantOffers o, Item munt, String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Guhs.id(id));
        if (item != Items.AIR) {
            o.add(blokken(munt, item));
        }
    }

    /** Snuffie: a funny line when clicked; gone when no game owns him. */
    public static final NpcRole SNUFFIE = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            GuhQuests.say(player, npc, "quest.guhs.schatgraven.snuffie." + npc.getRandom().nextInt(SNUFFIE_ZINNEN));
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if (npc.tickCount > 40 && npc.tickCount % 20 == 0 && !Schatgraven.HULPJES.contains(npc.getUUID())) {
                npc.discard();
            }
        }
    };
}
