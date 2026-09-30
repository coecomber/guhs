package nl.juiced.guhs.feature.boerderij;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Boerin Hooibaal (NPC kind BOERINNEGUH), in the big guh barn of the Guhboerderij. Every (Guhmensie) day she has one
 * little chore ({@link Klus}) for you: pet, brush or feed a few animals, fill a voerbak, or bring her pluiswol,
 * knabbeleitjes, kaasmelk or something from your tuintje. Done: kaasknabbels, knabbelvoer and seeds. After talking her
 * shop opens (brush, feed, voerbak, nests, seeds, pots, the gieter, her hat and neckerchief), and she buys products too.
 * Data per player in {@link BoerderijVoortgang#data}: Dag, Klus, Stand, Klaar, Ontmoet.
 */
public final class Hooibaal implements NpcRole {
    /** The chores; the doing ones count as you go, the bringing ones are checked when you talk to her. */
    public enum Klus {
        AAIEN(3), BORSTELEN(3), VOEREN(3), VOERBAK(2), WOL(4), EIEREN(3), MELK(2), OOGST(4);

        public final int doel;

        Klus(int doel) {
            this.doel = doel;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        boolean brengen() {
            return ordinal() >= WOL.ordinal();
        }

        @Nullable
        static Klus byId(String id) {
            for (Klus k : values()) {
                if (k.id().equals(id)) {
                    return k;
                }
            }
            return null;
        }
    }

    public static final int BELONING_KNABBELS = 6, BELONING_VOER = 4, BELONING_ZAADJES = 2;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.95f);
        CompoundTag d = BoerderijVoortgang.data(player);
        if (!d.getBoolean("Ontmoet")) {
            d.putBoolean("Ontmoet", true);
            GuhQuests.say(player, npc, "quest.guhs.boerderij.hallo");
            GuhAdvancements.grant(player, "boerderij_hooibaal");
        }
        long dag = Seizoen.dag(player.level());
        Klus klus = Klus.byId(d.getString("Klus"));
        if (d.getLong("Dag") != dag || klus == null || !d.contains("Dag")) {
            klus = nieuweKlus(player, dag);
            GuhQuests.say(player, npc, "quest.guhs.boerderij.klus." + klus.id(), klus.doel);
            return;                                                // (first read the chore; the shop opens next time)
        }
        if (d.getBoolean("Klaar")) {
            GuhQuests.say(player, npc, "quest.guhs.boerderij.al_klaar");
        } else if (probeerAf(player, klus)) {
            beloon(player, npc);
        } else {
            int stand = klus.brengen() ? teller(player, klus) : d.getInt("Stand");
            GuhQuests.say(player, npc, "quest.guhs.boerderij.nog." + klus.id(), Math.min(stand, klus.doel), klus.doel);
        }
        npc.openShop(player);
    }

    /** Today's chore for this player (the same all day; another one tomorrow). */
    public static Klus nieuweKlus(ServerPlayer player, long dag) {
        CompoundTag d = BoerderijVoortgang.data(player);
        Klus[] all = Klus.values();
        int i = Math.floorMod((int) (dag * 7919L) + player.getUUID().hashCode() + d.getInt("Totaal"), all.length);
        Klus klus = all[i];
        d.putLong("Dag", dag);
        d.putString("Klus", klus.id());
        d.putInt("Stand", 0);
        d.putBoolean("Klaar", false);
        return klus;
    }

    /** (Tests) set today's chore. */
    public static void zetKlus(ServerPlayer player, Klus klus) {
        CompoundTag d = BoerderijVoortgang.data(player);
        d.putLong("Dag", Seizoen.dag(player.level()));
        d.putString("Klus", klus.id());
        d.putInt("Stand", 0);
        d.putBoolean("Klaar", false);
    }

    @Nullable
    public static Klus vandaag(ServerPlayer player) {
        CompoundTag d = BoerderijVoortgang.data(player);
        return d.getLong("Dag") == Seizoen.dag(player.level()) && !d.getBoolean("Klaar") ? Klus.byId(d.getString("Klus")) : null;
    }

    public static boolean klaarVandaag(ServerPlayer player) {
        CompoundTag d = BoerderijVoortgang.data(player);
        return d.getLong("Dag") == Seizoen.dag(player.level()) && d.getBoolean("Klaar");
    }

    // --- the doing chores count while you work ---------------------------------------------------------------------------

    /** A care step by this player (BoerderijDier.verzorg). */
    static void gedaan(ServerPlayer player, BoerderijDier.Zorg zorg, BoerderijDier dier) {
        Klus klus = vandaag(player);
        if (klus != null && klus.name().equals(zorg.name())) {
            stap(player, klus);
        }
    }

    /** A voerbak got a portion from this player. */
    static void voerbakGevuld(ServerPlayer player) {
        Klus klus = vandaag(player);
        if (klus == Klus.VOERBAK) {
            stap(player, klus);
        }
    }

    private static void stap(ServerPlayer player, Klus klus) {
        CompoundTag d = BoerderijVoortgang.data(player);
        int stand = d.getInt("Stand") + 1;
        d.putInt("Stand", stand);
        if (stand == klus.doel) {
            player.displayClientMessage(Component.translatable("gui.guhs.boerderij.klus_klaar").withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
        } else if (stand < klus.doel) {
            player.displayClientMessage(Component.translatable("gui.guhs.boerderij.klus_stand", stand, klus.doel).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // --- the bringing chores ---------------------------------------------------------------------------------------------

    @Nullable
    private static TagKey<Item> wat(Klus klus) {
        return switch (klus) {
            case WOL -> KnusTags.PLUISWOL;
            case EIEREN -> KnusTags.KNABBELEI;
            case MELK -> KnusTags.KAASMELK;
            case OOGST -> KnusTags.OOGST;
            default -> null;
        };
    }

    private static int teller(ServerPlayer player, Klus klus) {
        TagKey<Item> tag = wat(klus);
        int n = 0;
        if (tag != null) {
            for (ItemStack s : player.getInventory().items) {
                if (s.is(tag)) {
                    n += s.getCount();
                }
            }
        }
        return n;
    }

    /** Is the chore done (a bringing chore: the things are taken now)? */
    static boolean probeerAf(ServerPlayer player, Klus klus) {
        if (!klus.brengen()) {
            return BoerderijVoortgang.data(player).getInt("Stand") >= klus.doel;
        }
        TagKey<Item> tag = wat(klus);
        if (tag == null || teller(player, klus) < klus.doel) {
            return false;
        }
        int left = klus.doel;
        for (ItemStack s : player.getInventory().items) {
            if (left > 0 && s.is(tag)) {
                int take = Math.min(left, s.getCount());
                if (s.hasCraftingRemainingItem()) {
                    for (int i = 0; i < take; i++) {
                        Minigames.give(player, s.getCraftingRemainingItem());
                    }
                }
                s.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    /** The chore is done: the reward, the counters. */
    static void beloon(ServerPlayer player, @Nullable GuhNpcEntity npc) {
        CompoundTag d = BoerderijVoortgang.data(player);
        d.putBoolean("Klaar", true);
        d.putInt("Totaal", d.getInt("Totaal") + 1);
        if (npc != null) {
            GuhQuests.say(player, npc, "quest.guhs.boerderij.bedankt" + player.getRandom().nextInt(4));
        }
        Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), BELONING_KNABBELS));
        Minigames.give(player, new ItemStack(BoerderijFeature.KNABBELVOER.get(), BELONING_VOER));
        Item[] zaadjes = {TuintjesFeature.KNABBELZAADJES.get(), TuintjesFeature.THEEKRUIDZAADJES.get(), TuintjesFeature.GUHBLOEMZAADJES.get()};
        Minigames.give(player, new ItemStack(zaadjes[player.getRandom().nextInt(zaadjes.length)], BELONING_ZAADJES));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
        KnusVoortgang.tel(player, BoerderijVoortgang.KLUSJES, 1);
        GuhAdvancements.grant(player, "boerderij_klusje");
        // 2.9: her straw hat after your first chore, the overalls after a few more (their only source, feature.kleding)
        int totaal = d.getInt("Totaal");
        if (totaal == nl.juiced.guhs.feature.kleding.KledingFeature.STROOHOED_KLUSJES) {
            Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.STRAW_HAT)));
            player.sendSystemMessage(Component.translatable("gui.guhs.kleding.hooibaal.strohoed").withStyle(ChatFormatting.GOLD));
        } else if (totaal == nl.juiced.guhs.feature.kleding.KledingFeature.OVERALL_KLUSJES) {
            Minigames.give(player, new ItemStack(ModItems.clothingItem(GuhClothes.OVERALLS)));
            player.sendSystemMessage(Component.translatable("gui.guhs.kleding.hooibaal.overall").withStyle(ChatFormatting.GOLD));
        }
    }

    /** (Tests) finish today's chore now if it is done. */
    public static boolean rondAf(ServerPlayer player) {
        Klus klus = vandaag(player);
        if (klus != null && probeerAf(player, klus)) {
            beloon(player, null);
            return true;
        }
        return false;
    }

    // --- the shop ----------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        Item knabbels = ModItems.KAAS_KNABBELS.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(knabbels, 4, BoerderijFeature.GUHBORSTEL.get(), 1));
        offers.add(offer(knabbels, 2, BoerderijFeature.KNABBELVOER.get(), 8));
        offers.add(offer(knabbels, 6, BoerderijFeature.GUH_VOERBAK.get(), 1));
        offers.add(offer(knabbels, 4, BoerderijFeature.KIPPENNESTJE.get(), 1));
        offers.add(offer(knabbels, 2, TuintjesFeature.KNABBELZAADJES.get(), 4));
        offers.add(offer(knabbels, 2, TuintjesFeature.THEEKRUIDZAADJES.get(), 4));
        offers.add(offer(knabbels, 3, TuintjesFeature.GUHBLOEMZAADJES.get(), 4));
        offers.add(offer(knabbels, 3, TuintjesFeature.GUH_BLOEMPOT.get(), 2));
        offers.add(offer(knabbels, 4, TuintjesFeature.GUH_MOESTUINBAK.get(), 1));
        offers.add(offer(knabbels, 5, TuintjesFeature.GUH_GIETER.get(), 1));
        offers.add(offer(knabbels, 10, ModItems.clothingItem(GuhClothes.BOERDERIJ_HOEDJE), 1));
        offers.add(offer(knabbels, 8, ModItems.clothingItem(GuhClothes.BOERDERIJ_ZAKDOEK), 1));
        offers.add(offer(knabbels, 10, ModItems.clothingItem(GuhClothes.TUINHOEDJE), 1));
        offers.add(offer(knabbels, 12, ModItems.clothingItem(GuhClothes.TUINSCHORTJE), 1));
        // she buys the products too (for the bakery and the tea house in the Knuffeldal)
        offers.add(new MerchantOffer(new ItemCost(BoerderijFeature.PLUISWOL.get(), 6), Optional.empty(), new ItemStack(knabbels, 2), Integer.MAX_VALUE, 0, 0));
        offers.add(new MerchantOffer(new ItemCost(BoerderijFeature.KNABBELEI.get(), 4), Optional.empty(), new ItemStack(knabbels, 2), Integer.MAX_VALUE, 0, 0));
        return offers;
    }

    static MerchantOffer offer(ItemLike cost, int price, ItemLike result, int count) {
        return new MerchantOffer(new ItemCost(cost, price), Optional.empty(), new ItemStack(result, count), Integer.MAX_VALUE, 0, 0);
    }
}
