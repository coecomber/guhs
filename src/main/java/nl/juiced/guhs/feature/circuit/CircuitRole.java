package nl.juiced.guhs.feature.circuit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Coach Vahoegvroem (CIRCUITGUH) in the Pitpaleis of the Guh-Circuit: her screen picks a track (Regenboogbaan, Vadsbaan,
 * Kaasbergbaan) and a level (makkelijk, medium, lastig), she lends you a race guh (RaceGame: one race at a time, others
 * watch from the stands), keeps the floating scoreboards at the tracks, and sells the circuit outfit for circuitbekers.
 */
public final class CircuitRole implements NpcRole {
    /** Buttons of her screen. */
    public static final int START = 0, GHOST = 1, GOUD = 2, SHOP = 3;
    /** The circuit outfit: prices in circuitbekers (helmet ~ a hat, cape ~ an accessory, the racing suit ~ a body piece). */
    public static final int PRICE_HELMPJE = 5, PRICE_VLAGCAPE = 7, PRICE_RACEPAK = 10;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        RaceGame game = RaceGame.of(npc);
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.15f);
        checkOutfit(player);
        if (game != null && game.racer().equals(player.getUUID())) {
            GuhQuests.say(player, npc, "quest.guhs.circuit.go_go");
            return;
        }
        boolean nieuw = CircuitBanen.BANEN.stream().noneMatch(b -> RaceRecords.finishedOnce(player, b.id));
        GuhQuests.say(player, npc, game != null ? "quest.guhs.circuit.hello_busy" : nieuw ? "quest.guhs.circuit.hello_new" : "quest.guhs.circuit.hello");
        CircuitPayloads.send(player, new CircuitPayloads.Open(npc.getId(), screenData(npc, player)));
    }

    /** The whole circuit outfit unlocked (or all three pieces in your pockets): the advancement. */
    static void checkOutfit(ServerPlayer player) {
        if (List.of(GuhClothes.CIRCUIT_HELMPJE, GuhClothes.CIRCUIT_RACEPAK, GuhClothes.CIRCUIT_VLAGCAPE).stream().allMatch(c -> KledingUnlocks.heeft(player, c))) {
            RaceGame.grant(player, "grote_guhspelen/circuit_kleding");
        }
    }

    /** What her screen shows: whether the circuit is free, and per track and level your records and the track record. */
    static CompoundTag screenData(GuhNpcEntity npc, ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        RaceGame game = RaceGame.of(npc);
        data.putBoolean("Busy", game != null);
        if (game != null) {
            ServerPlayer racer = ((ServerLevel) npc.level()).getServer().getPlayerList().getPlayer(game.racer());
            data.putString("Racer", racer == null ? "?" : racer.getGameProfile().getName());
            data.putInt("Lap", Math.min(game.lap() + 1, game.laps()));
            data.putString("RaceBaan", game.baan().id);
        }
        for (RaceBaan baan : CircuitBanen.BANEN) {
            for (Niveau n : Niveau.values()) {
                String key = baan.id + "_" + n.id(), rec = baan.records(n);
                data.putInt("Best_" + key, RaceRecords.best(player, rec));
                data.putInt("BestLap_" + key, RaceRecords.bestLap(player, rec));
                data.putInt("Races_" + key, RaceRecords.races(player, rec));
                List<Scorebord.Entry> top = Scorebord.top(player.server, baan.boardTotal(n));
                data.putInt("Record_" + key, top.isEmpty() ? -1 : top.get(0).score());
                data.putString("RecordName_" + key, top.isEmpty() ? "" : top.get(0).name());
            }
        }
        data.putBoolean("GhostOn", RaceRecords.ghostOn(player));
        data.putBoolean("GoudOn", RaceRecords.goudOn(player));
        return data;
    }

    /** A button on her screen (START: the chosen track and level). */
    static void action(GuhNpcEntity npc, ServerPlayer player, int action, String baanId, int niveau) {
        if (npc.getKind() != GuhNpcEntity.Kind.CIRCUITGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        switch (action) {
            case START -> {
                RaceBaan baan = CircuitBanen.baan(baanId);
                if (baan != null) {
                    RaceGame.start(npc, player, baan, Niveau.of(niveau));
                }
            }
            case GHOST -> {
                RaceRecords.toggleGhost(player);
                GuhQuests.say(player, npc, RaceRecords.ghostOn(player) ? "quest.guhs.race.ghost_on" : "quest.guhs.race.ghost_off");
                CircuitPayloads.send(player, new CircuitPayloads.Open(npc.getId(), screenData(npc, player)));
            }
            case GOUD -> {
                RaceRecords.toggleGoud(player);
                GuhQuests.say(player, npc, RaceRecords.goudOn(player) ? "quest.guhs.race.goud_on" : "quest.guhs.race.goud_off");
                CircuitPayloads.send(player, new CircuitPayloads.Open(npc.getId(), screenData(npc, player)));
            }
            case SHOP -> {
                GuhQuests.say(player, npc, "quest.guhs.circuit.shop");
                npc.openShop(player);
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        RaceGame game = RaceGame.of(npc);
        if (game != null) {
            game.checkAlive(npc);
        }
        if ((npc.tickCount + npc.getId()) % 100 == 0) {
            showScores(npc);
        }
    }

    /** The floating scoreboards at the tracks: per track the whole races and the fastest laps, all three levels. */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        for (int i = 0; i < CircuitBanen.BANEN.size(); i++) {
            RaceBaan baan = CircuitBanen.BANEN.get(i);
            List<String> totals = new ArrayList<>(), laps = new ArrayList<>();
            List<Component> headings = new ArrayList<>();
            for (Niveau n : Niveau.values()) {
                totals.add(baan.boardTotal(n));
                laps.add(baan.boardLap(n));
                headings.add(n.naam());
            }
            Vec3 a = CircuitBanen.board(level, npc, i, 0), b = CircuitBanen.board(level, npc, i, 1);
            if (a == null || b == null) {
                return;
            }
            Scorebord.show(level, a, "circuit_" + baan.id, Scorebord.text(level.getServer(),
                    Component.translatable("gui.guhs.scorebord.circuit", baan.naam()), totals, headings, RaceRecords::time));
            Scorebord.show(level, b, "circuit_" + baan.id + "_ronde", Scorebord.text(level.getServer(),
                    Component.translatable("gui.guhs.scorebord.circuit.ronde", baan.naam()), laps, headings, RaceRecords::time));
        }
    }

    /** The circuit outfit: never sold out, only here. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRICE_HELMPJE, GuhClothes.CIRCUIT_HELMPJE));
        offers.add(offer(PRICE_VLAGCAPE, GuhClothes.CIRCUIT_VLAGCAPE));
        offers.add(offer(PRICE_RACEPAK, GuhClothes.CIRCUIT_RACEPAK));
        return offers;
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(CircuitFeature.CIRCUITBEKER.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }
}
