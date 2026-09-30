package nl.juiced.guhs.feature.bakkerij;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Bakker Korstje's order game in the Knabbelbakkerij. Talk to him and you're the baker for two minutes: customer guhs
 * ({@link BakkerijKlant}) come in and wait at the counter with an order bubble over their heads. Bake what they want at a
 * knabbeloven ({@link Bakken}: dough, shape, topping - the screen shows the orders - and take it out of the oven at the
 * right moment), then right-click the customer to serve it.
 * <ul>
 *   <li>Points per order: 10 (a raw one 5), +10 perfect / +4 good, up to +5 for a customer who didn't wait long,
 *       times the combo: every 3 happy customers in a row one more (x2, x3, x4). A wrong pastry, a burnt one or a
 *       customer who got tired of waiting ("njeg...") breaks the combo. Nobody gets angry: guhs are always lief.</li>
 *   <li>Afterwards: bakmunten by score (+1 on every reward, like all minigames), your record, the Highscores board
 *       {@value #BOARD}, milestones on the Knus tab, and new recipes in the receptenboek.</li>
 *   <li>Grote Knusfeest: while the feesttaart task is open, a feestklant comes once you have {@value #FEEST_MIN}
 *       points: bake her Korstje's secret feesttaart and you get the real one ({@link Knusfeest#gemaakt}).</li>
 * </ul>
 * One game per Korstje at a time; a game isn't saved (after a restart it's simply over). The bakery's counter spots,
 * the door and the ovens are found once from the markers around Korstje and kept in his roleData.
 */
public final class BakkerijGame {
    /** Actions from Korstje's screen. */
    public static final int START = 0, SHOP = 1;
    public static final int COUNTDOWN_TICKS = 60, GAME_TICKS = 20 * 120;
    /** Points before the feestklant comes (while the Knusfeest's feesttaart task is open). */
    public static final int FEEST_MIN = 60;
    /** The first game ever: a few extra bakmunten (3, +1 like every minigame reward). */
    public static final int FIRST_BONUS = 3 + 1;
    /** Patience of a customer (ticks), at the start and at the end of the game. */
    public static final int GEDULD_START = 20 * 45, GEDULD_EIND = 20 * 28;
    /** Markers and ovens are found within SCAN of Korstje; the player must stay within REACH of him. */
    static final int SCAN = 14, REACH = 20;
    /** The world's top 3 (Scorebord) and the Guhdex Highscores row. */
    public static final String BOARD = "bakkerij";

    private static final Map<UUID, BakkerijGame> GAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYERS = new ConcurrentHashMap<>();

    /** The bakery: where customers wait (the counter spots), where they come in, and the ovens. */
    public record Winkel(List<BlockPos> plekken, BlockPos ingang, List<BlockPos> ovens) {
    }

    private final UUID npcId;
    private final UUID player;
    private final String playerName;
    private final Winkel winkel;
    private final boolean feest;
    private int ticks;
    private int nextKlant;
    int score, streak, bestStreak, geholpen, gemist, perfect;
    boolean feestKlantGekomen, feestGeserveerd;
    /** Customers come by themselves (the GameTests switch it off to send them in by hand). */
    boolean klanten = true;
    private final BakkerijKlant[] opPlek;
    private long lastTick;

    private BakkerijGame(UUID npcId, ServerPlayer player, Winkel winkel, boolean feest, long now) {
        this.npcId = npcId;
        this.player = player.getUUID();
        this.playerName = player.getGameProfile().name();
        this.winkel = winkel;
        this.feest = feest;
        this.lastTick = now;
        this.opPlek = new BakkerijKlant[winkel.plekken().size()];
    }

    // --- who plays where ----------------------------------------------------------------------------------------------

    public static boolean isPlaying(Player player) {
        return PLAYERS.containsKey(player.getUUID());
    }

    @Nullable
    public static BakkerijGame of(GuhNpcEntity npc) {
        return GAMES.get(npc.getUUID());
    }

    @Nullable
    static BakkerijGame byNpc(@Nullable UUID npc) {
        return npc == null ? null : GAMES.get(npc);
    }

    @Nullable
    public static BakkerijGame gameOf(Player player) {
        UUID npc = PLAYERS.get(player.getUUID());
        return npc == null ? null : GAMES.get(npc);
    }

    public int score() {
        return score;
    }

    public int streak() {
        return streak;
    }

    public boolean feest() {
        return feest;
    }

    public boolean counting() {
        return ticks < COUNTDOWN_TICKS;
    }

    public int playTicks() {
        return Math.max(0, ticks - COUNTDOWN_TICKS);
    }

    public Winkel winkel() {
        return winkel;
    }

    UUID npcId() {
        return npcId;
    }

    /** Is this one of the ovens of this game's bakery? */
    public boolean oven(BlockPos pos) {
        return winkel.ovens().contains(pos);
    }

    // --- talking to Korstje -------------------------------------------------------------------------------------------

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        BakkerijGame game = of(npc);
        boolean mine = game != null && game.player.equals(player.getUUID());
        boolean feest = Knusfeest.stap(player, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEVRAAGD;
        GuhQuests.say(player, npc, game == null ? (feest ? "quest.guhs.bakkerij.hello.feest" : "quest.guhs.bakkerij.hello")
                : mine ? "quest.guhs.bakkerij.busy_you" : "quest.guhs.bakkerij.busy");
        npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.1f);
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", game != null);
        if (game != null) {
            data.putString("Player", game.playerName);
            data.putInt("Left", Math.max(0, (COUNTDOWN_TICKS + GAME_TICKS - game.ticks) / 20));
            data.putInt("Score", game.score);
        }
        data.putInt("Best", best(player));
        data.putInt("Games", GuhQuests.saved(player).getIntOr(GAMES_KEY, 0));
        data.putBoolean("First", !GuhQuests.saved(player).getBooleanOr(PLAYED_KEY, false));
        data.putBoolean("Feest", feest);
        data.putInt("FeestMin", FEEST_MIN);
        data.putInt("Recepten", KnusVoortgang.ontdekt(player, BakkerijVoortgang.RECEPTENBOEK).size());
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new BakkerijPayloads.Open(BakkerijPayloads.KORSTJE, npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.BAKKERGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        if (action == SHOP) {
            GuhQuests.say(player, npc, "quest.guhs.bakkerij.shop");
            npc.openShop(player);
        } else if (action == START) {
            start(npc, player);
        }
    }

    // --- playing ------------------------------------------------------------------------------------------------------

    /** Starts a game for this player, if the bakery is free. */
    public static boolean start(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel world = (ServerLevel) npc.level();
        BakkerijGame running = of(npc);
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        if (running != null) {
            GuhQuests.say(player, npc, running.player.equals(player.getUUID()) ? "quest.guhs.bakkerij.busy_you" : "quest.guhs.bakkerij.busy");
            return false;
        }
        if (isPlaying(player)) {
            GuhQuests.say(player, npc, "quest.guhs.bakkerij.elsewhere");
            return false;
        }
        if (Minigames.refuse(player, npc, Minigames.BAKKERIJ)) {
            return false;
        }
        Winkel winkel = winkel(npc);
        if (winkel == null) {
            GuhQuests.say(player, npc, "quest.guhs.bakkerij.broken");
            return false;
        }
        boolean feest = Knusfeest.stap(player, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEVRAAGD;
        BakkerijGame game = new BakkerijGame(npc.getUUID(), player, winkel, feest, world.getGameTime());
        GAMES.put(npc.getUUID(), game);
        PLAYERS.put(player.getUUID(), npc.getUUID());
        Bakken.vergeet(player);
        player.closeContainer();
        player.stopRiding();
        // behind the counter, in front of the oven nearest to Korstje, looking at it
        BlockPos oven = winkel.ovens().stream().min(Comparator.comparingDouble(o -> o.distSqr(npc.blockPosition()))).orElseThrow();
        BlockState state = world.getBlockState(oven);
        Direction facing = state.hasProperty(KnabbelovenBlock.FACING) ? state.getValue(KnabbelovenBlock.FACING) : Direction.SOUTH;
        BlockPos spot = oven.relative(facing);
        teleport(player, world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, facing.getOpposite().toYRot());
        Minigames.startKeeping(player);
        GuhQuests.say(player, npc, feest ? "quest.guhs.bakkerij.go.feest" : "quest.guhs.bakkerij.go", FEEST_MIN);
        world.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        return true;
    }

    /** Every tick of Korstje while his game is on. */
    void tick(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        lastTick = world.getGameTime();
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p == null || !p.isAlive() || p.level() != world || p.distanceToSqr(npc) > REACH * REACH) {
            stop(npc, p);
            return;
        }
        Minigames.keep(p);
        ticks++;
        if (ticks <= COUNTDOWN_TICKS) {
            countdown(npc, p, world);
            return;
        }
        int t = playTicks();
        float progress = Math.min(1f, t / (float) GAME_TICKS);
        for (int i = 0; i < opPlek.length; i++) {
            if (opPlek[i] != null && (!opPlek[i].isAlive() || !opPlek[i].bezet())) {
                opPlek[i] = null;                                  // (served or gone: the spot is free)
            }
        }
        if (feest && !feestKlantGekomen && score >= FEEST_MIN && vrijePlek() >= 0) {
            klant(world, Recept.FEESTTAART);
            cheer(npc, p, "quest.guhs.bakkerij.cheer.feest");
        } else if (klanten && t >= nextKlant && vrijePlek() >= 0 && t < GAME_TICKS - 20 * 8) {
            klant(world, Recept.BOEK.get(world.getRandom().nextInt(Recept.BOEK.size())));
            nextKlant = t + Math.round(150 - 80 * progress) + world.getRandom().nextInt(30);
        }
        int left = GAME_TICKS - t;
        if (left == 20 * 60) {
            cheer(npc, p, "quest.guhs.bakkerij.cheer.half");
        } else if (left == 20 * 10) {
            cheer(npc, p, "quest.guhs.bakkerij.cheer.ten");
        }
        if (left > 0 && left <= 100 && left % 20 == 0) {
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1f, 1.6f);
        }
        if (t % 10 == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.bakkerij.bar", score, (left + 19) / 20, combo(streak), geholpen)
                    .withStyle(ChatFormatting.GOLD));
        }
        if (t >= GAME_TICKS) {
            finish(npc, p);
        }
    }

    private void countdown(GuhNpcEntity npc, ServerPlayer p, ServerLevel world) {
        if (ticks == 1 || ticks == 21 || ticks == 41) {
            int n = 3 - (ticks - 1) / 20;
            title(p, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.bakkerij.ready"), 0, 22, 2);
            world.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + 0.2f * (3 - n));
        } else if (ticks == COUNTDOWN_TICKS) {
            title(p, Component.translatable("quest.guhs.bakkerij.title.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.bakkerij.title.go.sub"), 0, 30, 10);
            world.playSound(null, p.blockPosition(), BakkerijFeature.OVEN_DING.get(), SoundSource.PLAYERS, 1f, 1.2f);
            cheer(npc, p, "quest.guhs.bakkerij.cheer.start");
        }
    }

    private int vrijePlek() {
        List<Integer> vrij = new ArrayList<>();
        for (int i = 0; i < opPlek.length; i++) {
            if (opPlek[i] == null) {
                vrij.add(i);
            }
        }
        return vrij.isEmpty() ? -1 : vrij.get(Math.floorMod((int) (lastTick * 31 + ticks), vrij.size()));
    }

    /** A new customer comes in at the door and walks to a free spot at the counter (null when all spots are taken). */
    @Nullable
    BakkerijKlant klant(ServerLevel world, Recept recept) {
        int plek = vrijePlek();
        if (plek < 0) {
            return null;
        }
        float progress = Math.min(1f, playTicks() / (float) GAME_TICKS);
        int geduld = Math.round(GEDULD_START + (GEDULD_EIND - GEDULD_START) * progress);
        if (recept == Recept.FEESTTAART) {
            geduld = GEDULD_START * 2;                               // (the feestklant waits patiently for her big cake)
            feestKlantGekomen = true;
        }
        BakkerijKlant klant = BakkerijKlant.maak(world, npcId, winkel.ingang(), winkel.plekken().get(plek), recept, geduld);
        opPlek[plek] = klant;
        world.playSound(null, winkel.ingang(), BakkerijFeature.BESTELLING.get(), SoundSource.NEUTRAL, 1f, 1f);
        return klant;
    }

    /** A pastry came out of the oven (from {@link Bakken}): into your hand, for a customer. */
    void gebakken(ServerPlayer p, Recept recept, Recept.Kwaliteit k) {
        if (k == Recept.Kwaliteit.AANGEBRAND) {
            breek(p, "quest.guhs.bakkerij.aangebrand");
            return;
        }
        if (k == Recept.Kwaliteit.PERFECT) {
            perfect++;
            KnusVoortgang.tel(p, BakkerijVoortgang.PERFECT, 1);
        }
        if (recept.inBoek() && k != Recept.Kwaliteit.RAUW) {
            BakkerijVoortgang.ontdek(p, recept);
        }
        ItemStack stack = BakjeItem.voorKlant(recept, k);
        Inventory inv = p.getInventory();
        if (inv.getItem(inv.getSelectedSlot()).isEmpty()) {
            inv.setItem(inv.getSelectedSlot(), stack);
        } else if (!inv.add(stack)) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.bakkerij.vol").withStyle(ChatFormatting.GOLD));
            return;
        }
        p.sendOverlayMessage(Component.translatable("quest.guhs.bakkerij.gebakken", recept.naam(), k.naam())
                .withStyle(k == Recept.Kwaliteit.PERFECT ? ChatFormatting.YELLOW : ChatFormatting.LIGHT_PURPLE));
    }

    /** The combo is broken (a wrong or burnt pastry, a customer who left). */
    private void breek(ServerPlayer p, String key) {
        if (streak >= 3) {
            p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.combo_weg").withStyle(ChatFormatting.GRAY));
        }
        streak = 0;
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    /** The combo multiplier: one more for every 3 happy customers in a row, up to x4. */
    public static int combo(int streak) {
        return 1 + Math.min(streak, 9) / 3;
    }

    /** Points for one order (before the combo). */
    public static int punten(Recept recept, Recept.Kwaliteit k, float geduld) {
        int base = recept == Recept.FEESTTAART ? 25 : k == Recept.Kwaliteit.RAUW ? 5 : 10;
        int bonus = k == Recept.Kwaliteit.PERFECT ? 10 : k == Recept.Kwaliteit.GOED ? 4 : 0;
        return base + bonus + Math.round(5 * Math.max(0f, Math.min(1f, geduld)));
    }

    /**
     * The player right-clicks a customer: serve what they ordered (from your hand, or anywhere in your pockets), or
     * they tell you (again) what they'd like. Returns whether something was served.
     */
    public static boolean serveer(ServerPlayer p, BakkerijKlant klant) {
        BakkerijGame game = gameOf(p);
        if (game == null || !game.npcId.equals(klant.spel())) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.bakkerij.klant.niet_jij").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        return game.serveer(p, klant, (ServerLevel) p.level());
    }

    boolean serveer(ServerPlayer p, BakkerijKlant klant, ServerLevel world) {
        Recept wil = klant.recept();
        if (wil == null || !klant.bezet()) {
            return false;
        }
        Inventory inv = p.getInventory();
        int slot = -1;
        Recept.Kwaliteit beste = null;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (BakjeItem.isSpel(s) && BakjeItem.recept(s) == wil) {
                Recept.Kwaliteit k = BakjeItem.kwaliteit(s);
                if (slot < 0 || (k != null && (beste == null || k.ordinal() > beste.ordinal()) && k != Recept.Kwaliteit.AANGEBRAND)) {
                    slot = i;
                    beste = k;
                }
            }
        }
        if (slot < 0) {
            ItemStack hand = p.getMainHandItem();
            if (BakjeItem.isSpel(hand)) {
                klant.nee();
                breek(p, "quest.guhs.bakkerij.verkeerd");
                GuhQuests.say(p, klant, "quest.guhs.bakkerij.klant.verkeerd", wil.naam());
            } else {
                GuhQuests.say(p, klant, wil == Recept.FEESTTAART ? "quest.guhs.bakkerij.klant.feestwens" : "quest.guhs.bakkerij.klant.wens",
                        wil.naam(), wil.deeg.naam(), wil.vorm.naam(), wil.topping.naam());
            }
            return false;
        }
        Recept.Kwaliteit k = beste == null ? Recept.Kwaliteit.GOED : beste;
        inv.getItem(slot).shrink(1);
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        int gained = punten(wil, k, klant.geduld()) * combo(streak);
        score += gained;
        geholpen++;
        klant.blij();
        KnusVoortgang.tel(p, BakkerijVoortgang.BESTELLINGEN, 1);
        GuhAdvancements.grant(p, "bakkerij_eerste_bestelling");
        BakkerijVoortgang.toon(p, "bakkerij_eerste_bestelling");
        world.playSound(null, klant.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.3f);
        world.playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f + Math.min(streak, 12) * 0.06f);
        world.sendParticles(ParticleTypes.HEART, klant.getX(), klant.getY() + klant.getBbHeight() + 0.3, klant.getZ(), 5, 0.3, 0.2, 0.3, 0);
        p.sendOverlayMessage(Component.translatable("quest.guhs.bakkerij.geserveerd", gained, score, combo(streak)).withStyle(ChatFormatting.GOLD));
        GuhQuests.say(p, klant, "quest.guhs.bakkerij.klant.blij" + (1 + world.getRandom().nextInt(4)), wil.naam());
        if (streak == 3 || streak == 6 || streak == 9) {
            title(p, Component.empty(), Component.translatable("quest.guhs.bakkerij.combo", combo(streak)).withStyle(ChatFormatting.LIGHT_PURPLE), 0, 20, 8);
            if (streak == 6) {
                GuhAdvancements.grant(p, "bakkerij_combo");
                BakkerijVoortgang.toon(p, "bakkerij_combo");
            }
        }
        if (wil == Recept.FEESTTAART) {
            feestGeserveerd = true;
            feesttaart(p);
        }
        return true;
    }

    /** The feestklant got her cake: Korstje packs a real one for the Grote Knusfeest. */
    private void feesttaart(ServerPlayer p) {
        if (Knusfeest.stap(p, Feesttaak.FEESTTAART) == Knusfeest.Stap.GEVRAAGD) {
            Minigames.give(p, new ItemStack(BakkerijFeature.FEESTTAART.get()));
            Knusfeest.gemaakt(p, Feesttaak.FEESTTAART);
            KnusVoortgang.tel(p, BakkerijVoortgang.FEESTTAART, 1);
            p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.feesttaart").withStyle(ChatFormatting.LIGHT_PURPLE));
            title(p, Component.translatable("quest.guhs.bakkerij.title.feesttaart").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.translatable("quest.guhs.bakkerij.title.feesttaart.sub"), 5, 50, 15);
        }
    }

    /** A customer waited too long and leaves (sad, never angry): the combo is broken. */
    void gemist(BakkerijKlant klant) {
        gemist++;
        ServerLevel world = (ServerLevel) klant.level();
        ServerPlayer p = world.getServer().getPlayerList().getPlayer(player);
        if (p != null) {
            breek(p, "quest.guhs.bakkerij.gemist");
            GuhQuests.say(p, klant, "quest.guhs.bakkerij.klant.weg");
        }
        if (klant.recept() == Recept.FEESTTAART) {
            feestKlantGekomen = false;                               // (she comes back later: the Knusfeest must go on)
        }
    }

    private void cheer(GuhNpcEntity npc, ServerPlayer p, String key) {
        GuhQuests.say(p, npc, key);
        ServerLevel world = (ServerLevel) npc.level();
        world.playSound(null, npc.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.1f, 1f + world.getRandom().nextFloat() * 0.3f);
        world.sendParticles(BakkerijFeature.MEELSTOFJE.get(), npc.getX(), npc.getY() + npc.getBbHeight() + 0.3, npc.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
    }

    /** Now and then, when nobody is playing, Korstje calls people near him. */
    static void invite(GuhNpcEntity npc) {
        if (of(npc) != null || npc.tickCount % (20 * 30) != 0 || !(npc.level() instanceof ServerLevel world)) {
            return;
        }
        Player near = world.getNearestPlayer(npc, 10);
        if (near instanceof ServerPlayer p && !isPlaying(p) && !p.isSpectator()) {
            p.sendOverlayMessage(Component.literal("<").append(npc.getDisplayName()).append("> ")
                    .append(Component.translatable("quest.guhs.bakkerij.invite" + (1 + world.getRandom().nextInt(3)))).withStyle(ChatFormatting.GOLD));
            npc.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 1.2f);
            world.sendParticles(BakkerijFeature.MEELSTOFJE.get(), npc.getX(), npc.getY() + npc.getBbHeight() + 0.3, npc.getZ(), 4, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** The orders for the baking screen: [{Recept, Geduld (0..100), Feest}]. */
    void bestellingen(CompoundTag data) {
        ListTag list = new ListTag();
        for (BakkerijKlant k : opPlek) {
            if (k != null && k.isAlive() && k.bezet() && k.recept() != null) {
                CompoundTag o = new CompoundTag();
                o.putInt("Recept", k.recept().ordinal());
                o.putInt("Geduld", Math.round(k.geduld() * 100));
                list.add(o);
            }
        }
        data.put("Bestellingen", list);
        data.putInt("Score", score);
        data.putInt("Combo", combo(streak));
        data.putBoolean("Feest", feest);
    }

    // --- the end ------------------------------------------------------------------------------------------------------

    /** Bakmunten for a score: none for doing nothing, else 1 + one per 40 points (at most 12) - and +1 like every reward. */
    public static int munten(int score) {
        if (score <= 0) {
            return 0;
        }
        return Math.min(12, 1 + score / 40) + 1;
    }

    /** The two minutes are over: bakmunten, the record, advancements, milestones. */
    void finish(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel world = (ServerLevel) npc.level();
        end(npc, p);
        CompoundTag saved = GuhQuests.saved(p);
        int munten = munten(score);
        boolean first = !saved.getBooleanOr(PLAYED_KEY, false);
        saved.putBoolean(PLAYED_KEY, true);
        saved.putInt(GAMES_KEY, saved.getIntOr(GAMES_KEY, 0) + 1);
        int best = best(p);
        boolean record = score > best;
        if (record) {
            saved.putInt(BEST_KEY, score);
        }
        int total = munten + (first ? FIRST_BONUS : 0);
        if (total > 0) {
            Minigames.give(p, new ItemStack(BakkerijFeature.BAKMUNT.get(), total));
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.done", score, geholpen, perfect, bestStreak).withStyle(ChatFormatting.GOLD));
        if (munten > 0) {
            p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.munten", munten).withStyle(ChatFormatting.YELLOW));
        }
        if (first) {
            GuhQuests.say(p, npc, "quest.guhs.bakkerij.first", FIRST_BONUS);
        }
        p.sendSystemMessage((record ? Component.translatable("quest.guhs.bakkerij.record", score)
                : Component.translatable("quest.guhs.bakkerij.best", best)).withStyle(record ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        if (score > 0) {
            Scorebord.submit(p, BOARD, score, false);
        }
        showScores(npc);
        KnusVoortgang.hoogste(p, BakkerijVoortgang.HIGHSCORE, score);
        KnusVoortgang.hoogste(p, BakkerijVoortgang.COMBO, bestStreak);
        GuhAdvancements.grant(p, "bakkerij_gespeeld");
        if (score >= 100) {
            GuhAdvancements.grant(p, "bakkerij_100");
        }
        if (score >= 250) {
            GuhAdvancements.grant(p, "bakkerij_250");
        }
        title(p, Component.translatable(record ? "quest.guhs.bakkerij.title.record" : "quest.guhs.bakkerij.title.end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("quest.guhs.bakkerij.title.points", score), 5, 60, 20);
        GuhQuests.say(p, npc, score >= 250 ? "quest.guhs.bakkerij.end.super" : score >= 100 ? "quest.guhs.bakkerij.end.good" : "quest.guhs.bakkerij.end.ok");
        world.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.2f);
        world.sendParticles(BakkerijFeature.KNABBELWOLKJE.get(), npc.getX(), npc.getY() + npc.getBbHeight() + 0.4, npc.getZ(), 10, 0.5, 0.3, 0.5, 0.02);
        world.sendParticles(ParticleTypes.HEART, npc.getX(), npc.getY() + npc.getBbHeight() + 0.4, npc.getZ(), 6, 0.5, 0.3, 0.5, 0);
    }

    /** The player walked off, logged out, died, or the bakery went away: no bakmunten. */
    void stop(GuhNpcEntity npc, @Nullable ServerPlayer p) {
        end(npc, null);
        if (p != null) {
            cleanup(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.stopped", score).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The game is over: the customers go home, the game pastries go away. */
    private void end(GuhNpcEntity npc, @Nullable ServerPlayer p) {
        GAMES.remove(npcId, this);
        PLAYERS.remove(player, npcId);
        ServerLevel world = (ServerLevel) npc.level();
        for (BakkerijKlant klant : world.getEntitiesOfClass(BakkerijKlant.class, npc.getBoundingBox().inflate(SCAN + 8), k -> npcId.equals(k.spel()))) {
            klant.naarHuis();
        }
        if (p != null) {
            cleanup(p);
        }
    }

    /** Takes back the customers' pastries (from everywhere in the inventory) and forgets a bake in the oven. */
    public static void cleanup(ServerPlayer p) {
        PLAYERS.remove(p.getUUID());
        Bakken.vergeet(p);
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (BakjeItem.isSpel(inv.getItem(i))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (BakjeItem.isSpel(p.containerMenu.getCarried())) {
            p.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    // --- events -------------------------------------------------------------------------------------------------------

    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && isPlaying(p) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    public static void onDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && isPlaying(p)) {
            stopFor(p);
        }
    }

    /** A game whose Korstje stopped ticking (his chunk unloaded) is over for its player. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !isPlaying(p)) {
            return;
        }
        UUID npc = PLAYERS.get(p.getUUID());
        BakkerijGame game = npc == null ? null : GAMES.get(npc);
        if (game == null || p.level().getGameTime() - game.lastTick > 40) {
            if (game != null) {
                GAMES.remove(npc, game);
            }
            cleanup(p);
            p.sendSystemMessage(Component.translatable("quest.guhs.bakkerij.stopped", game == null ? 0 : game.score).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        GAMES.clear();
        PLAYERS.clear();
    }

    /** Ends this player's game (without rewards), wherever it is. */
    static void stopFor(ServerPlayer p) {
        UUID npcId = PLAYERS.get(p.getUUID());
        BakkerijGame game = npcId == null ? null : GAMES.get(npcId);
        if (game != null && p.level().getServer() != null) {
            for (ServerLevel level : p.level().getServer().getAllLevels()) {
                if (level.getEntity(npcId) instanceof GuhNpcEntity npc) {
                    game.stop(npc, p);
                    return;
                }
            }
            GAMES.remove(npcId, game);
        }
        cleanup(p);
    }

    // --- the bakery ---------------------------------------------------------------------------------------------------

    /** The bakery of this Korstje: remembered in his roleData, or found by its markers. Null when there is none. */
    @Nullable
    public static Winkel winkel(GuhNpcEntity npc) {
        CompoundTag data = npc.roleData;
        if (data.contains("BakIngang")) {
            return new Winkel(posities(data.getLongArray("BakPlekken").orElse(new long[0])), BlockPos.of(data.getLongOr("BakIngang", 0L)), posities(data.getLongArray("BakOvens").orElse(new long[0])));
        }
        Winkel winkel = zoek(npc);
        if (winkel != null) {
            data.putLong("BakIngang", winkel.ingang().asLong());
            data.put("BakPlekken", new LongArrayTag(winkel.plekken().stream().mapToLong(BlockPos::asLong).toArray()));
            data.put("BakOvens", new LongArrayTag(winkel.ovens().stream().mapToLong(BlockPos::asLong).toArray()));
        }
        return winkel;
    }

    private static List<BlockPos> posities(long[] longs) {
        List<BlockPos> out = new ArrayList<>();
        for (long l : longs) {
            out.add(BlockPos.of(l));
        }
        return out;
    }

    @Nullable
    private static Winkel zoek(GuhNpcEntity npc) {
        BlockPos c = npc.blockPosition();
        List<BlockPos> plekken = new ArrayList<>(), ingangen = new ArrayList<>(), ovens = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-SCAN, -4, -SCAN), c.offset(SCAN, 8, SCAN))) {
            BlockState state = npc.level().getBlockState(pos);
            if (state.is(BakkerijFeature.KLANTPLEK.get())) {
                plekken.add(pos.immutable());
            } else if (state.is(BakkerijFeature.INGANG.get())) {
                ingangen.add(pos.immutable());
            } else if (state.getBlock() instanceof KnabbelovenBlock) {
                ovens.add(pos.immutable());
            }
        }
        if (plekken.isEmpty() || ingangen.isEmpty() || ovens.isEmpty()) {
            return null;
        }
        plekken.sort(Comparator.comparingInt((BlockPos p) -> p.getX()).thenComparingInt(p -> p.getZ()));
        BlockPos ingang = ingangen.stream().min(Comparator.comparingDouble(p -> p.distSqr(c))).orElseThrow();
        return new Winkel(plekken, ingang, ovens);
    }

    // --- records and little helpers -----------------------------------------------------------------------------------

    static final String BEST_KEY = "guhs_bakkerij_best", GAMES_KEY = "guhs_bakkerij_games", PLAYED_KEY = "guhs_bakkerij_played";

    public static void showScores(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        Scorebord.show(world, npc.position().add(0, 2.3, 0), "bakkerij", Scorebord.text(world.getServer(),
                Component.translatable("gui.guhs.scorebord.bakkerij"), List.of(BOARD),
                List.of(Component.translatable("gui.guhs.scorebord.bakkerij.punten")), s -> s + " pt"));
    }

    public static int best(Player player) {
        return GuhQuests.saved(player).getIntOr(BEST_KEY, 0);
    }

    static void teleport(ServerPlayer p, ServerLevel world, double x, double y, double z, float yRot) {
        if (p instanceof FakePlayer || p.connection == null) {
            p.snapTo(x, y, z, yRot, 0);
        } else {
            p.teleportTo(world, x, y, z, yRot, 0);
        }
    }

    static void title(ServerPlayer p, Component title, Component subtitle, int in, int stay, int out) {
        if (p instanceof FakePlayer || p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** (Tests) where the player would come out after a game: next to Korstje. */
    static Vec3 naast(GuhNpcEntity npc) {
        return npc.position().add(0, 0, 1.5);
    }

    /** (Tests) the customers of this game. */
    List<BakkerijKlant> klanten(ServerLevel world) {
        return world.getEntitiesOfClass(BakkerijKlant.class, new AABB(winkel.ingang()).inflate(SCAN + 8), k -> npcId.equals(k.spel()) && k.isAlive());
    }
}
