package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;

/**
 * Badmeester Bubbel, in the bath hall of the Knuffelbad: explains the slides and the washing ritual (the first time he
 * gives you a bottle of guhshampoo and a föhn), shows the records of the three slides (a floating top 3 above him, and
 * yours on his screen), and runs the little shop for eendjesmunten (the swim cap and the bathrobe for your guh,
 * shampoo, a föhn, tiles and foam). Now and then: TUUUT! (no running by the pool).
 */
public final class Badmeester implements NpcRole {
    /** Buttons of his screen. */
    public static final int WINKEL = 0, TIP = 1, WASSEN = 2;
    public static final int TIPS = 8;
    /** Prices in eendjesmunten. */
    public static final int PRIJS_BADMUTSJE = 4, PRIJS_BADJASJE = 8, PRIJS_SHAMPOO = 2, PRIJS_FOHN = 5, PRIJS_TOBBE = 3, PRIJS_TEGELS = 1, PRIJS_GLIM = 2,
            PRIJS_SCHUIM = 1;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, KnuffelbadFeature.FLUIT.get(), SoundSource.NEUTRAL, 0.6f, 1.2f);
        CompoundTag data = GlijRit.data(player);
        boolean eerste = !data.getBoolean("Begroet");
        if (eerste) {
            data.putBoolean("Begroet", true);
            KnuffelbadVoortgang.gevonden(player);
        }
        GuhQuests.say(player, npc, eerste ? "quest.guhs.knuffelbad.badmeester.hallo_nieuw" : "quest.guhs.knuffelbad.badmeester.hallo");
        KnuffelbadPayloads.naar(player, new KnuffelbadPayloads.Open(npc.getId(), schermData(player)));
    }

    /** What his screen shows: your record and rides per slide, the world record, your eendjesmunten, washes. */
    static CompoundTag schermData(ServerPlayer player) {
        CompoundTag d = new CompoundTag();
        for (Glijbaan g : Glijbaan.values()) {
            CompoundTag b = new CompoundTag();
            b.putInt("Best", GlijRit.best(player, g));
            b.putInt("Ritten", GlijRit.ritten(player, g));
            List<Scorebord.Entry> top = Scorebord.top(player.server, g.board());
            b.putInt("Record", top.isEmpty() ? -1 : top.get(0).score());
            b.putString("Naam", top.isEmpty() ? "" : top.get(0).name());
            d.put(g.id(), b);
        }
        d.putInt("Munten", GuhQuests.count(player, KnuffelbadFeature.EENDJESMUNT.get()));
        d.putInt("Wassen", KnusVoortgang.teller(player, KnuffelbadVoortgang.WASSEN));
        d.putInt("Eendjes", KnusVoortgang.ontdekt(player, KnuffelbadVoortgang.BADEENDJES).size());
        d.putInt("EendjesTotaal", Eendsoort.SPECIAAL.size());
        return d;
    }

    /** A button on his screen. */
    static void actie(GuhNpcEntity npc, ServerPlayer player, int actie) {
        if (npc.getKind() != GuhNpcEntity.Kind.BADMEESTERGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        switch (actie) {
            case WINKEL -> {
                GuhQuests.say(player, npc, "quest.guhs.knuffelbad.badmeester.winkel");
                npc.openShop(player);
            }
            case TIP -> GuhQuests.say(player, npc, "quest.guhs.knuffelbad.badmeester.tip" + player.getRandom().nextInt(TIPS));
            case WASSEN -> {
                CompoundTag data = GlijRit.data(player);
                GuhQuests.say(player, npc, "quest.guhs.knuffelbad.badmeester.wassen");
                if (!data.getBoolean("WasSet")) {
                    data.putBoolean("WasSet", true);
                    nl.juiced.guhs.feature.Minigames.give(player, new ItemStack(KnuffelbadFeature.GUHSHAMPOO.get()));
                    nl.juiced.guhs.feature.Minigames.give(player, new ItemStack(KnuffelbadFeature.GUH_FOHN.get()));
                    GuhQuests.say(player, npc, "quest.guhs.knuffelbad.badmeester.cadeautje");
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if ((npc.tickCount + npc.getId()) % 100 == 0) {
            toonBord(npc);
        }
        // TUUUT, no running by the pool! (a player sprinting near him; the client blows the whistle for the same reason)
        if ((npc.tickCount + npc.getId()) % 5 != 0) {
            return;
        }
        long now = npc.level().getGameTime();
        if (now - npc.roleData.getLong("Fluit") < FLUIT_RUST) {
            return;
        }
        for (ServerPlayer p : npc.level().getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(FLUIT_AFSTAND),
                p -> p.isSprinting() && p.onGround() && !p.isSpectator() && !GlijRit.rijdt(p))) {
            npc.level().playSound(null, npc, KnuffelbadFeature.FLUIT.get(), SoundSource.NEUTRAL, 1f, 1f);
            npc.roleData.putLong("Fluit", now);
            GuhQuests.say(p, npc, "quest.guhs.knuffelbad.badmeester.niet_rennen");
            break;
        }
    }

    /** No running by the pool: within this many blocks he whistles, then not again for FLUIT_RUST ticks. */
    public static final int FLUIT_AFSTAND = 14, FLUIT_RUST = 200;

    /** The floating top 3 of the three slides above Badmeester Bubbel. */
    public static void toonBord(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<String> boards = List.of(Glijbaan.ROZE_TRECHTER.board(), Glijbaan.GLIMTUNNEL.board(), Glijbaan.GROTE_PLONS.board());
        List<Component> heads = List.of(Glijbaan.ROZE_TRECHTER.naam(), Glijbaan.GLIMTUNNEL.naam(), Glijbaan.GROTE_PLONS.naam());
        Scorebord.show(level, npc.position().add(0, 3.0, 0), "knuffelbad",
                Scorebord.text(level.getServer(), Component.translatable("gui.guhs.scorebord.knuffelbad"), boards, heads, s -> s + " pt"));
    }

    /** After a ride: the boards of the Badmeester nearby (and of the slide's start gate) are brought up to date. */
    static void toonScores(ServerLevel level, BlockPos gate) {
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(gate).inflate(96),
                n -> n.getKind() == GuhNpcEntity.Kind.BADMEESTERGUH)) {
            toonBord(npc);
        }
    }

    /** The slide's own top 3, floating over its start gate. */
    static void toonPoortBord(ServerLevel level, BlockPos gate, Glijbaan g) {
        Scorebord.show(level, Vec3.atBottomCenterOf(gate).add(0, 2.6, 0), g.board(),
                Scorebord.text(level.getServer(), g.naam(), List.of(g.board()), List.of(), s -> s + " pt"));
    }

    /** The shop: never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_BADMUTSJE, new ItemStack(ModItems.clothingItem(GuhClothes.BADMUTSJE))));
        offers.add(offer(PRIJS_BADJASJE, new ItemStack(ModItems.clothingItem(GuhClothes.BADJASJE))));
        offers.add(offer(PRIJS_SHAMPOO, new ItemStack(KnuffelbadFeature.GUHSHAMPOO.get())));
        offers.add(offer(PRIJS_FOHN, new ItemStack(KnuffelbadFeature.GUH_FOHN.get())));
        offers.add(offer(PRIJS_TOBBE, new ItemStack(KnuffelbadFeature.GUH_WASTOBBE_ITEM.get())));
        offers.add(offer(PRIJS_TEGELS, new ItemStack(KnuffelbadFeature.TRECHTERTEGEL_ITEM.get(), 8)));
        offers.add(offer(PRIJS_GLIM, new ItemStack(KnuffelbadFeature.GLIMTEGEL_ITEM.get(), 8)));
        offers.add(offer(PRIJS_SCHUIM, new ItemStack(KnuffelbadFeature.SCHUIM_ITEM.get(), 8)));
        return offers;
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        Item coin = KnuffelbadFeature.EENDJESMUNT.get();
        return new MerchantOffer(new ItemCost(coin, price), result, Integer.MAX_VALUE, 0, 0);
    }
}
