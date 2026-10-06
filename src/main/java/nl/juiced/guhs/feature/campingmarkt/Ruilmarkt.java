package nl.juiced.guhs.feature.campingmarkt;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.spiesburcht.NetherMikaRuil;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (camping-markt): the Nether-Mika-ruilmarkt. Questline {@link CampingmarktFeature#RUILMARKT} (per player):
 * <ol start="0">
 *   <li>talk to the Marktmeester-Mika;</li>
 *   <li>learn haggling: bring his price down from {@link #BEGINPRIJS} to {@link #DOELPRIJS} knabbels or less
 *       ({@link #zet}); you get his Keurstempel;</li>
 *   <li>unmask the fake vads: one of the five stacks on the counter of the Waag is lighter. Click two stacks to weigh them
 *       against each other ({@link #klikStapel}), then stamp the fake one;</li>
 *   <li>tell the Marktmeester: the scales (a deco block), and from now on the Mika's give a little extra when you barter.</li>
 * </ol>
 * <b>Haggling</b> is a game of reading his mood. Every round the Marktmeester brags, sighs or growls, and one answer fits:
 * a compliment for a bragger, a low offer for one who sighs, pretending to walk away from one who growls. The right answer
 * takes {@link #KORTING} off the price, a wrong one costs one of his {@link #GEDULD} patience. Out of patience: he shoves
 * you off the market square (a nudge, {@link Duwtje}) and you simply start again. The state is not saved.
 * <p>
 * <b>The fake vads</b> is every player's own puzzle: which stack is the fake is drawn per player ({@link #nep}), the stacks
 * themselves never change. A wrong stamp costs nothing, but the swindler swaps the stacks: weigh again.
 * <p>
 * <b>Better barter rates</b> ({@link #klant}, {@link #extraatje}): for whoever finished the questline a Nether-Mika throws
 * a second present for every vads bar, and the stall holders of the market do the same ({@link #kraam}).
 */
public final class Ruilmarkt {
    public static final int STAPELS = 5;
    public static final int BEGINPRIJS = 30, DOELPRIJS = 10, KORTING = 7, GEDULD = 3, TIPS = 4;
    /** The moods of the Marktmeester; the answer that fits mood i is option i + 1. */
    public static final int OPSCHEPPEN = 0, ZUCHTEN = 1, GROMMEN = 2;
    public static final int COMPLIMENT = 1, LAAG_BIEDEN = 2, WEGLOPEN = 3;
    /** The player's own counters in the questline: the number of the fake stack (0: not drawn yet), weighings since the last stamp. */
    public static final String NEP = "nep", WEGINGEN = "wegingen", KOOPJE_DAG = "koopje_dag";
    /** What the Marktmeester's bargain of the day is drawn from, and what a Mika gives for a vads bar. */
    public static final ResourceKey<LootTable> KOOPJE = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("gameplay/campingmarkt_koopje"));
    /** (a Nether-Mika's own data) the certified customer it is bartering with, and when it last gave an extra. */
    static final String KLANT = "guhs_campingmarkt_klant", EXTRA_TIJD = "guhs_campingmarkt_extra_tijd";
    private static final String Q = "quest.guhs.campingmarkt.marktmeester.", A = "quest.guhs.campingmarkt.afdingen.";

    /** A haggle that is going on (not saved: a player who leaves starts again). */
    public static final class Afdingen {
        public int prijs = BEGINPRIJS, geduld = GEDULD, stemming;

        Afdingen(int stemming) {
            this.stemming = stemming;
        }
    }

    private static final Map<UUID, Afdingen> BEZIG = new ConcurrentHashMap<>();
    /** The stack a player laid on the left pan (waiting for the second one). */
    private static final Map<UUID, Integer> LINKS = new ConcurrentHashMap<>();

    private Ruilmarkt() {
    }

    static void vergeet(UUID speler) {
        BEZIG.remove(speler);
        LINKS.remove(speler);
    }

    // =================================================================================================================
    // haggling
    // =================================================================================================================

    /** The haggle this player is in (null: none). */
    @Nullable
    public static Afdingen bezig(ServerPlayer p) {
        return BEZIG.get(p.getUUID());
    }

    /** Starts (or goes on with) a haggle with the Marktmeester and shows his screen. */
    public static Afdingen begin(GuhNpcEntity npc, ServerPlayer p) {
        Afdingen a = BEZIG.computeIfAbsent(p.getUUID(), u -> new Afdingen(p.getRandom().nextInt(3)));
        toon(npc, p, a, "begin");
        return a;
    }

    private static void toon(GuhNpcEntity npc, ServerPlayer p, Afdingen a, String wat) {
        Praat.open(p, npc, null, A + wat + "." + a.stemming, new Object[]{a.prijs, a.geduld}, new Praat.Optie(COMPLIMENT, A + "optie.compliment"),
                new Praat.Optie(LAAG_BIEDEN, A + "optie.laag"), new Praat.Optie(WEGLOPEN, A + "optie.weglopen"));
    }

    /** The outcome of a move: still haggling, the price is low enough (the deal screen), or sent away. */
    public enum Zet { VERDER, GEWONNEN, WEGGESTUURD, DEAL, NIETS }

    /**
     * The player picked an answer in the haggling screen. The fitting answer (mood + 1) lowers the price, another one costs
     * patience; at {@link #DOELPRIJS} or less the deal screen comes, where option 1 closes the deal.
     */
    public static Zet zet(GuhNpcEntity npc, ServerPlayer p, int optie) {
        Afdingen a = BEZIG.get(p.getUUID());
        if (a == null || optie < 1 || optie > 3) {
            return Zet.NIETS;
        }
        if (a.prijs <= DOELPRIJS) {
            if (optie != 1) {
                return Zet.NIETS;
            }
            deal(npc, p);
            return Zet.DEAL;
        }
        boolean goed = optie == a.stemming + 1;
        if (goed) {
            a.prijs -= KORTING;
            npc.level().playSound(null, npc, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 0.7f);
        } else {
            a.geduld--;
            npc.level().playSound(null, npc, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.8f, 0.7f);
        }
        if (a.geduld <= 0) {
            BEZIG.remove(p.getUUID());
            Praat.sluit(p);
            GuhQuests.say(p, npc, A + "weggestuurd");
            GuhQuests.hint(p, "quest.guhs.campingmarkt.hint.afdingen");
            npc.level().playSound(null, npc, ModSounds.MIKA_HURT.get(), SoundSource.NEUTRAL, 1f, 0.8f);
            if (Duwtje.mag(p)) {
                Duwtje.duw(p, p.position().subtract(npc.position()), 0.9);   // a shove off his rostrum: no damage, nothing lost
            }
            return Zet.WEGGESTUURD;
        }
        if (a.prijs <= DOELPRIJS) {
            Praat.open(p, npc, null, A + "gewonnen", new Object[]{a.prijs}, new Praat.Optie(1, A + "optie.deal"));
            return Zet.GEWONNEN;
        }
        a.stemming = (a.stemming + 1 + p.getRandom().nextInt(2)) % 3;   // (never the same mood twice in a row)
        toon(npc, p, a, goed ? "goed" : "fout");
        return Zet.VERDER;
    }

    /** The deal is closed: the lesson of the questline is learnt, or (afterwards) the bargain of the day is yours. */
    private static void deal(GuhNpcEntity npc, ServerPlayer p) {
        BEZIG.remove(p.getUUID());
        Praat.sluit(p);
        Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
        ServerLevel level = p.level();
        level.playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7f, 1.3f);
        if (lijn.stap(p) == 1) {
            lijn.verder(p, 1);
            nep(p);
            GuhQuests.say(p, npc, Q + "geleerd1");
            GuhQuests.say(p, npc, Q + "geleerd2");
            Minigames.give(p, new ItemStack(CampingmarktFeature.KEURSTEMPEL.get()));
            GuhQuests.hint(p, "quest.guhs.campingmarkt.hint.wegen");
            GuhAdvancements.grant(p, "camping_markt_afgedongen");
            return;
        }
        if (lijn.klaar(p) && lijn.teller(p, KOOPJE_DAG) != dag(p)) {
            lijn.teller(p, KOOPJE_DAG, dag(p));
            GuhQuests.say(p, npc, Q + "koopje");
            for (ItemStack stack : rol(level, npc, KOOPJE)) {
                Minigames.give(p, stack);
            }
            GuhAdvancements.grant(p, "camping_markt_koopje");
        }
    }

    /** The day number haggling for the bargain of the day goes by (the overworld's clock, never 0). */
    static int dag(ServerPlayer p) {
        return (int) (p.level().getServer().overworld().getGameTime() / 24000L) + 1;
    }

    /** May this player haggle for the bargain of the day now (the questline done, not yet today)? */
    public static boolean koopjeVrij(ServerPlayer p) {
        Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
        return lijn.klaar(p) && lijn.teller(p, KOOPJE_DAG) != dag(p);
    }

    private static List<ItemStack> rol(ServerLevel level, Entity wie, ResourceKey<LootTable> tabel) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tabel);
        return table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, wie).create(LootContextParamSets.PIGLIN_BARTER));
    }

    // =================================================================================================================
    // the fake vads
    // =================================================================================================================

    /** The number (1..5) of the stack that is the fake for this player; drawn the first time it is asked. */
    public static int nep(ServerPlayer p) {
        Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
        int n = lijn.teller(p, NEP);
        if (n < 1 || n > STAPELS) {
            n = 1 + p.getRandom().nextInt(STAPELS);
            lijn.teller(p, NEP, n);
        }
        return n;
    }

    /** How the scales tip with stack {@code links} on the left pan and {@code rechts} on the right one (the fake is lighter). */
    public static CampingmarktBlocks.Stand weeg(int nep, int links, int rechts) {
        return links == nep ? CampingmarktBlocks.Stand.RECHTS : rechts == nep ? CampingmarktBlocks.Stand.LINKS : CampingmarktBlocks.Stand.MIDDEN;
    }

    /**
     * A player clicked a stack of vads. With the Keurstempel: this is the fake, says the player. Otherwise: the first click
     * lays the stack on the left pan, the second (another stack) on the right one, and the scales show which is lighter.
     */
    public static void klikStapel(ServerPlayer p, BlockPos pos, int nummer, ItemStack stack) {
        Verhaallijn lijn = CampingmarktFeature.RUILMARKT;
        ServerLevel level = p.level();
        int stap = lijn.stap(p);
        if (stap != 2) {
            Kamperen.meld(p, stap < 2 ? "stapel.eerst_afdingen" : "stapel.klaar", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        int nep = nep(p);
        if (stack.is(CampingmarktFeature.KEURSTEMPEL.get())) {
            if (lijn.teller(p, WEGINGEN) == 0) {
                Kamperen.meld(p, "stapel.eerst_wegen", ChatFormatting.LIGHT_PURPLE);
                return;
            }
            LINKS.remove(p.getUUID());
            double x = pos.getX() + 0.5, y = pos.getY() + 0.7, z = pos.getZ() + 0.5;
            if (nummer != nep) {
                // wrong: nothing lost, but the swindler swaps the stacks, so guessing does not get you there
                lijn.teller(p, NEP, 1 + (nummer - 1 + 1 + p.getRandom().nextInt(STAPELS - 1)) % STAPELS);
                lijn.teller(p, WEGINGEN, 0);
                Kamperen.meld(p, "stapel.echt", ChatFormatting.LIGHT_PURPLE, nummer);
                level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8f, 1.3f);
                level.sendParticles(p, ParticleTypes.SMOKE, false, false, x, y, z, 8, 0.25, 0.15, 0.25, 0.01);
                return;
            }
            lijn.verder(p, 2);
            Kamperen.meld(p, "stapel.ontmaskerd", ChatFormatting.GOLD, nummer);
            GuhQuests.hint(p, "quest.guhs.campingmarkt.hint.marktmeester");
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            level.playSound(null, pos, ModSounds.GUH_EAT.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
            // (only this player sees their own fake fall apart: purple wrapping paper and the kaasknabbel that was in it)
            level.sendParticles(p, new ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get()), false, false, x, y, z, 14, 0.25, 0.2, 0.25, 0.08);
            level.sendParticles(p, ParticleTypes.POOF, false, false, x, y, z, 10, 0.3, 0.2, 0.3, 0.02);
            return;
        }
        Integer links = LINKS.get(p.getUUID());
        if (links == null) {
            LINKS.put(p.getUUID(), nummer);
            Kamperen.meld(p, "stapel.links", ChatFormatting.YELLOW, nummer);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7f, 1.2f);
            return;
        }
        if (links == nummer) {
            LINKS.remove(p.getUUID());
            Kamperen.meld(p, "stapel.terug", ChatFormatting.YELLOW, nummer);
            return;
        }
        LINKS.remove(p.getUUID());
        lijn.teller(p, WEGINGEN, lijn.teller(p, WEGINGEN) + 1);
        CampingmarktBlocks.Stand stand = weeg(nep, links, nummer);
        BlockPos schaal = schaalBij(level, pos);
        if (schaal != null) {
            CampingmarktBlocks.Weegschaal.tik(level, schaal, stand);
        }
        Kamperen.meld(p, "stapel.weeg." + stand.getSerializedName(), ChatFormatting.GOLD, links, nummer);
    }

    /** The scales near these stacks (the Waag has one in the middle), or null. */
    @Nullable
    static BlockPos schaalBij(ServerLevel level, BlockPos pos) {
        BlockPos best = null;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-6, -2, -6), pos.offset(6, 2, 6))) {
            if (level.getBlockState(q).getBlock() instanceof CampingmarktBlocks.Weegschaal && (best == null || q.distSqr(pos) < best.distSqr(pos))) {
                best = q.immutable();
            }
        }
        return best;
    }

    /** A click on the scales while a player is weighing for the questline: how it works. False: not weighing (the scales weigh hands). */
    public static boolean klikSchaal(ServerPlayer p, BlockPos pos) {
        if (CampingmarktFeature.RUILMARKT.stap(p) != 2) {
            return false;
        }
        Kamperen.meld(p, "schaal.uitleg", ChatFormatting.YELLOW);
        return true;
    }

    // =================================================================================================================
    // the Marktmeester-Mika
    // =================================================================================================================

    public static final class MarktmeesterRol extends QuestRol {
        public MarktmeesterRol() {
            super(CampingmarktFeature.RUILMARKT);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.level().playSound(null, npc, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 0.8f);
            switch (stap) {
                case 0 -> {
                    zeg(p, npc, Q + "hallo1");
                    zeg(p, npc, Q + "hallo2");
                    verder(p, 0);
                    hint(p, "quest.guhs.campingmarkt.hint.afdingen");
                }
                case 1 -> begin(npc, p);
                case 2 -> {
                    if (geefAlsKwijt(p, CampingmarktFeature.KEURSTEMPEL.get())) {
                        zeg(p, npc, Q + "stempel_weer");
                    } else {
                        zeg(p, npc, Q + "weeg");
                    }
                }
                case 3 -> {
                    if (verder(p, 3)) {
                        zeg(p, npc, Q + "klaar1");
                        zeg(p, npc, Q + "klaar2");
                        GuhQuests.take(p, CampingmarktFeature.KEURSTEMPEL.get(), 64);
                        geefEenmalig(p, "beloning", new ItemStack(CampingmarktFeature.WEEGSCHAAL_ITEM.get()));
                        zichtbaar(p, "barbecuether/camping_markt_ruilmarkt");
                        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
                        if (npc.level() instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1.2, npc.getZ(), 16, 0.6, 0.5, 0.6, 0.0);
                        }
                    }
                }
                default -> {
                    if (koopjeVrij(p)) {
                        zeg(p, npc, Q + "koopje_vraag");
                        begin(npc, p);
                    } else {
                        zeg(p, npc, Q + "tip" + npc.getRandom().nextInt(TIPS));
                        npc.openShop(p);
                    }
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            zet(npc, p, optie);
        }

        /** After the questline: honest prices for the things of the Guhbarbecuether. */
        @Nullable
        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            MerchantOffers offers = new MerchantOffers();
            offers.add(Kamperen.offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 12), new ItemStack(BarbecuetherFeature.GRILLKOOL.get(), 2)));
            offers.add(Kamperen.offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 6), new ItemStack(BarbecuetherFeature.PINDASCHEUTJES.get(), 4)));
            offers.add(Kamperen.offer(new ItemCost(ModItems.VAHOEGE_VADS_INGOT.get(), 1), new ItemStack(ModItems.KAAS_KNABBELS.get(), 24)));
            offers.add(Kamperen.offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 32), new ItemStack(CampingmarktFeature.WEEGSCHAAL_ITEM.get())));
            for (String kleur : List.of("rood", "creme")) {
                offers.add(Kamperen.offer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 6), new ItemStack(CampingmarktFeature.TENTDOEK.get(kleur).blok().get(), 8)));
            }
            return offers;
        }
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        Afdingen a = bezig(p);
        return switch (stap) {
            case 1 -> List.of(Verhaallijn.nodig("guhs:kaas_knabbels", "gui.guhs.campingmarkt.nodig.afdingen", a == null ? 0 : BEGINPRIJS - Math.max(DOELPRIJS, a.prijs),
                    BEGINPRIJS - DOELPRIJS));
            case 2 -> List.of(Verhaallijn.nodig("guhs:campingmarkt_keurstempel", GuhQuests.count(p, CampingmarktFeature.KEURSTEMPEL.get()), 1));
            default -> List.of();
        };
    }

    // =================================================================================================================
    // better barter rates
    // =================================================================================================================

    /**
     * A certified customer (the questline done) holds out a vads bar to a wild Nether-Mika that is going to take it (called
     * before the Mika's own handler): the Mika remembers who it is bartering with.
     */
    static void klant(ServerPlayer p, Entity target, ItemStack stack) {
        if (CampingmarktFeature.RUILMARKT.klaar(p) && NetherMikaRuil.isNetherMika(target) && stack.is(ModItems.VAHOEGE_VADS_INGOT.get())
                && target instanceof MikaEntity mika && !NetherMikaRuil.isAngryAt(mika, p) && !NetherMikaRuil.isAdmiring(mika)) {
            mika.getPersistentData().putString(KLANT, p.getUUID().toString());
        }
    }

    /**
     * A Nether-Mika threw something (not the vads bar itself: that is a barter that was broken off): when it was bartering
     * with a certified customer, or one stands right next to it, it throws a second present. Once per barter.
     */
    static void gegooid(ServerLevel level, MikaEntity mika, ItemStack wat) {
        CompoundTag data = mika.getPersistentData();
        String klant = data.getStringOr(KLANT, "");
        data.remove(KLANT);
        long nu = level.getGameTime();
        if (wat.is(ModItems.VAHOEGE_VADS_INGOT.get()) || Math.abs(nu - data.getLongOr(EXTRA_TIJD, -100L)) < 20) {
            return;
        }
        ServerPlayer p = null;
        if (!klant.isEmpty()) {
            try {
                p = level.getServer().getPlayerList().getPlayer(UUID.fromString(klant));
            } catch (IllegalArgumentException ignored) {
                // (not a uuid: nobody)
            }
        } else {
            // (the bar was thrown on the ground for it: the certified customer who stands closest)
            for (ServerPlayer q : level.getEntitiesOfClass(ServerPlayer.class, mika.getBoundingBox().inflate(8), CampingmarktFeature.RUILMARKT::klaar)) {
                if (p == null || q.distanceToSqr(mika) < p.distanceToSqr(mika)) {
                    p = q;
                }
            }
        }
        if (p == null || p.level() != level || p.distanceToSqr(mika) > 16 * 16 || !CampingmarktFeature.RUILMARKT.klaar(p)) {
            return;
        }
        data.putLong(EXTRA_TIJD, nu);
        extraatje(level, mika, p, NetherMikaRuil.barter(level, mika));
    }

    /** The second present of a barter, thrown to the certified customer. */
    static void extraatje(ServerLevel level, LivingEntity mika, ServerPlayer p, List<ItemStack> loot) {
        for (ItemStack stack : loot) {
            BehaviorUtils.throwItem(mika, stack, p.position().add(0, 1, 0));
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mika.getX(), mika.getY() + mika.getBbHeight() + 0.2, mika.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        Kamperen.meld(p, "ruil.extraatje", ChatFormatting.GOLD);
        GuhAdvancements.grant(p, "camping_markt_extraatje");
    }

    /**
     * A click on a stall holder of the market. With a vads bar: a barter on the spot (no sniffing, no chasing: this is a
     * market), with the extra present for a certified customer. Otherwise it praises its wares.
     */
    public static void kraam(ServerPlayer p, KraamMikaEntity mika, InteractionHand hand) {
        ServerLevel level = p.level();
        ItemStack stack = p.getItemInHand(hand);
        long nu = level.getGameTime();
        CompoundTag data = mika.getPersistentData();
        if (!stack.is(ModItems.VAHOEGE_VADS_INGOT.get())) {
            if (nu - data.getLongOr(EXTRA_TIJD, -100L) > 15) {
                data.putLong(EXTRA_TIJD, nu);
                GuhQuests.say(p, mika, "quest.guhs.campingmarkt.kraam." + mika.getKraam() + "." + level.getRandom().nextInt(KraamMikaEntity.ZINNEN));
                mika.playSound(ModSounds.MIKA_AMBIENT.get(), 0.8f, 1.1f);
            }
            return;
        }
        if (nu - data.getLongOr(EXTRA_TIJD, -100L) < 10) {
            return;
        }
        data.putLong(EXTRA_TIJD, nu);
        stack.consume(1, p);
        mika.blij();
        mika.playSound(SoundEvents.PIGLIN_CELEBRATE, 0.8f, 1.3f);
        for (ItemStack loot : rol(level, mika, NetherMikaRuil.RUIL_LOOT)) {
            BehaviorUtils.throwItem(mika, loot, p.position().add(0, 1, 0));
        }
        Kamperen.meld(p, "ruil.kraam", ChatFormatting.GOLD);
        GuhAdvancements.grant(p, "camping_markt_kraam");
        if (CampingmarktFeature.RUILMARKT.klaar(p)) {
            extraatje(level, mika, p, rol(level, mika, NetherMikaRuil.RUIL_LOOT));
        }
    }
}
