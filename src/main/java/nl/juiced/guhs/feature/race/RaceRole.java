package nl.juiced.guhs.feature.race;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Raceguh, in the pit stop of the guh racebaan: explains the race, lends you a race guh (RaceGame) on makkelijk,
 * medium or lastig (2.9), keeps the track records, and sells the jockey outfit for raceprijsjes (only here).
 */
public final class RaceRole implements NpcRole {
    /** Buttons of the Raceguh's screen (START = a race on medium, as in 2.4). */
    public static final int START = 0, GHOST = 1, SHOP = 2, START_MAKKELIJK = 3, START_LASTIG = 4, GOUD = 5;
    /** The jockey outfit: prices in raceprijsjes. */
    public static final int PRICE_BRIL = 3, PRICE_PET = 5, PRICE_JASJE = 7;
    /** Where the floating boards of makkelijk and lastig are in the racebaan template (on the pit stop's roof; race.py). */
    public static final BlockPos BOARD_MAKKELIJK = new BlockPos(23, 11, 86), BOARD_LASTIG = new BlockPos(33, 11, 86);

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        RaceGame game = RaceGame.of(npc);
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        if (game != null && game.racer().equals(player.getUUID())) {
            GuhQuests.say(player, npc, "quest.guhs.race.go_go");
            return;
        }
        GuhQuests.say(player, npc, game != null ? "quest.guhs.race.hello_busy" : RaceRecords.races(player) == 0 ? "quest.guhs.race.hello_new" : "quest.guhs.race.hello");
        RacePayloads.send(player, new RacePayloads.Open(npc.getId(), screenData(npc, player)));
    }

    /** What the Raceguh's screen shows: per level your records and the track record. */
    static CompoundTag screenData(GuhNpcEntity npc, ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        RaceGame game = RaceGame.of(npc);
        data.putBoolean("Busy", game != null);
        if (game != null) {
            ServerPlayer racer = ((ServerLevel) npc.level()).getServer().getPlayerList().getPlayer(game.racer());
            data.putString("Racer", racer == null ? "?" : racer.getGameProfile().name());
            data.putInt("Lap", Math.min(game.lap() + 1, game.laps()));
        }
        RaceBaan baan = RaceBaan.RACEBAAN;
        boolean anyGhost = false;
        for (Niveau n : Niveau.values()) {
            String rec = baan.records(n);
            data.putInt("Best_" + n.id(), RaceRecords.best(player, rec));
            data.putInt("BestLap_" + n.id(), RaceRecords.bestLap(player, rec));
            data.putInt("Races_" + n.id(), RaceRecords.races(player, rec));
            anyGhost |= RaceRecords.ghost(player, rec).length > 0;
            List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), baan.boardTotal(n));
            data.putInt("Record_" + n.id(), top.isEmpty() ? -1 : top.get(0).score());
            data.putString("RecordName_" + n.id(), top.isEmpty() ? "" : top.get(0).name());
        }
        // (2.4 names, still read by older screens and tests)
        data.putInt("Best", RaceRecords.best(player));
        data.putInt("BestLap", RaceRecords.bestLap(player));
        data.putInt("Races", RaceRecords.races(player));
        data.putBoolean("HasGhost", anyGhost);
        data.putBoolean("GhostOn", RaceRecords.ghostOn(player));
        data.putBoolean("GoudOn", RaceRecords.goudOn(player));
        List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), BOARD_TOTAL);
        data.putInt("RecordTicks", top.isEmpty() ? -1 : top.get(0).score());
        data.putString("RecordName", top.isEmpty() ? "" : top.get(0).name());
        return data;
    }

    /** A button on the screen. */
    static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.RACEGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        switch (action) {
            case START -> RaceGame.start(npc, player, RaceBaan.RACEBAAN, Niveau.MEDIUM);
            case START_MAKKELIJK -> RaceGame.start(npc, player, RaceBaan.RACEBAAN, Niveau.MAKKELIJK);
            case START_LASTIG -> RaceGame.start(npc, player, RaceBaan.RACEBAAN, Niveau.LASTIG);
            case GHOST -> {
                RaceRecords.toggleGhost(player);
                GuhQuests.say(player, npc, RaceRecords.ghostOn(player) ? "quest.guhs.race.ghost_on" : "quest.guhs.race.ghost_off");
                RacePayloads.send(player, new RacePayloads.Open(npc.getId(), screenData(npc, player)));
            }
            case GOUD -> {
                RaceRecords.toggleGoud(player);
                GuhQuests.say(player, npc, RaceRecords.goudOn(player) ? "quest.guhs.race.goud_on" : "quest.guhs.race.goud_off");
                RacePayloads.send(player, new RacePayloads.Open(npc.getId(), screenData(npc, player)));
            }
            case SHOP -> {
                GuhQuests.say(player, npc, "quest.guhs.race.shop");
                npc.openShop(player);
            }
            default -> {
            }
        }
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        RaceGame.checkAllAlive(npc);
        if ((npc.tickCount + npc.getId()) % 100 == 0) {
            showScores(npc);
        }
    }

    /** The world's top 3 (Scorebord) at medium: the fastest races and the fastest laps; first place of the races is the track record. */
    public static final String BOARD_TOTAL = "race_total", BOARD_LAP = "race_lap";

    /** The track record (fastest race in the world, medium), or -1. */
    static int trackRecord(MinecraftServer server) {
        return RaceGame.trackRecord(server, BOARD_TOTAL);
    }

    /**
     * The floating top-3 boards: medium above the Raceguh in the pit stop (as in 2.4), makkelijk and lastig on the pit
     * stop's roof (found through the track's start marker, so they turn with the racebaan).
     */
    public static void showScores(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        Scorebord.show(level, npc.position().add(0, 2.6, 0), "race", board(level.getServer(), Niveau.MEDIUM));
        RaceTrack track = RaceTrack.ofLoaded(npc, RaceBaan.RACEBAAN);
        if (track != null) {
            for (Niveau n : new Niveau[]{Niveau.MAKKELIJK, Niveau.LASTIG}) {
                BlockPos spot = RaceTrack.templateToWorld(track.start, track.facing, n == Niveau.MAKKELIJK ? BOARD_MAKKELIJK : BOARD_LASTIG);
                Scorebord.show(level, Vec3.atBottomCenterOf(spot), "race_" + n.id(), board(level.getServer(), n));
            }
        }
    }

    private static Component board(MinecraftServer server, Niveau n) {
        RaceBaan baan = RaceBaan.RACEBAAN;
        Component title = n == Niveau.MEDIUM ? Component.translatable("gui.guhs.scorebord.race")
                : Component.translatable("gui.guhs.scorebord.race.niveau", n.naam());
        return Scorebord.text(server, title, List.of(baan.boardTotal(n), baan.boardLap(n)),
                List.of(Component.translatable("gui.guhs.race.board.total"), Component.translatable("gui.guhs.race.board.lap")), RaceRecords::time);
    }

    /** The jockey outfit: never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRICE_BRIL, GuhClothes.RACEBRIL));
        offers.add(offer(PRICE_PET, GuhClothes.JOCKEY_PET));
        offers.add(offer(PRICE_JASJE, GuhClothes.JOCKEY_JASJE));
        return offers;
    }

    private static MerchantOffer offer(int price, GuhClothes clothes) {
        return new MerchantOffer(new ItemCost(RaceFeature.RACEPRIJSJE.get(), price), new ItemStack(ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }
}
