package nl.juiced.guhs.feature.bakkerij;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * Baking at a knabbeloven: the choice screen (dough, shape, topping), the oven with its moving pointer, and taking it
 * out at the right moment ({@link Recept.Kwaliteit}). Two ways:
 * <ul>
 *   <li><b>Your own baking</b> (any knabbeloven, not in a game): from ingredients in your pockets ({@link Nodig}): the
 *       dough needs knabbelgraan (or wheat), kaasmelk (or milk), knabbeleieren (or eggs), kaasknabbels or sugar; the
 *       topping kaasknabbels, sugar, sugar + pink dye or crumbs. Perfect: 3 pastries, good: 2, raw: 1, burnt: nothing
 *       (njeg). Fresh farm ingredients from the Knuffeldal ({@code #guhs:knus/...}) give one extra.</li>
 *   <li><b>Korstje's order game</b> ({@link BakkerijGame}): no ingredients, the pastry is for a customer.</li>
 * </ul>
 * One bake per player at a time; the server times it (the client's own count is used when it's close to the server's,
 * so a bit of lag doesn't burn your cake). Left too long it burns by itself.
 */
public final class Bakken {
    /** How far the client's own tick count may be from the server's before the server's is used instead. */
    public static final int TOLERANTIE = 8;
    /** A bake left in the oven this long after the end of the bar burns by itself. */
    public static final int VERGETEN = 40;
    /** Pastries per bake from your own oven, by quality (+1 with fresh Knuffeldal ingredients). */
    public static final int PERFECT_AANTAL = 3, GOED_AANTAL = 2, RAUW_AANTAL = 1;

    /** A bake in progress. */
    record Bak(BlockPos oven, Level level, Recept recept, long start, boolean spel, boolean vers, boolean meel) {
    }

    /**
     * 2.9 (guhpolder): knabbelmeel from a guh-molentje ({@code #guhs:knus/knabbelmeel}) goes into the dough instead of
     * knabbelgraan, and then the oven bakes {@link #MEEL_KEER} times as much.
     */
    public static final TagKey<Item> KNABBELMEEL = TagKey.create(Registries.ITEM, Guhs.id("knus/knabbelmeel"));
    public static final int MEEL_KEER = 2;

    /** What {@link #neem} took: all fresh (farm) ingredients, and knabbelmeel for the graan. */
    record Genomen(boolean vers, boolean meel) {
    }

    private static final Map<UUID, Bak> BAKKEN = new ConcurrentHashMap<>();

    private Bakken() {
    }

    // =================================================================================================================
    // ingredients
    // =================================================================================================================

    /** What a bake needs: the Knuffeldal ingredient (a tag of another feature) and what you can use instead. */
    public enum Nodig {
        GRAAN("knabbelgraan", Items.WHEAT), KAASMELK("kaasmelk", Items.MILK_BUCKET), EI("knabbelei", Items.EGG),
        KNABBELS(null, null), SUIKER(null, Items.SUGAR), ROZE(null, Items.PINK_DYE), KRUIMELS(null, Items.BREAD);

        @Nullable
        public final TagKey<Item> tag;
        @Nullable
        private final Item vervanger;

        Nodig(@Nullable String tag, @Nullable Item vervanger) {
            this.tag = tag == null ? null : TagKey.create(Registries.ITEM, Guhs.id("knus/" + tag));
            this.vervanger = vervanger;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The Knuffeldal (farm/garden) version? */
        boolean vers(ItemStack stack) {
            return tag != null && stack.is(tag);
        }

        public boolean past(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return switch (this) {
                case KNABBELS -> stack.is(ModItems.KAAS_KNABBELS.get());
                case KRUIMELS -> stack.is(Items.BREAD) || stack.is(Items.COOKIE);
                default -> vers(stack) || (this == GRAAN && stack.is(KNABBELMEEL)) || (vervanger != null && stack.is(vervanger));
            };
        }
    }

    /** Everything a combination needs (the dough, then the topping), with counts. Works on both sides. */
    public static Map<Nodig, Integer> nodig(Recept.Deeg deeg, Recept.Topping topping) {
        Map<Nodig, Integer> out = new EnumMap<>(Nodig.class);
        List<Nodig> d = switch (deeg) {
            case KNABBELDEEG -> List.of(Nodig.GRAAN, Nodig.KNABBELS);
            case KAASDEEG -> List.of(Nodig.GRAAN, Nodig.KAASMELK);
            case ZOETDEEG -> List.of(Nodig.GRAAN, Nodig.SUIKER, Nodig.EI);
            case BLADERDEEG -> List.of(Nodig.GRAAN, Nodig.KAASMELK, Nodig.EI);
        };
        List<Nodig> t = switch (topping) {
            case KAAS -> List.of(Nodig.KNABBELS);
            case SUIKER -> List.of(Nodig.SUIKER);
            case GLAZUUR -> List.of(Nodig.SUIKER, Nodig.ROZE);
            case KRUIMELS -> List.of(Nodig.KRUIMELS);
        };
        for (Nodig n : d) {
            out.merge(n, 1, Integer::sum);
        }
        for (Nodig n : t) {
            out.merge(n, 1, Integer::sum);
        }
        return out;
    }

    /** How many of each ingredient the player has (for the screen). */
    public static int[] voorraad(ServerPlayer player) {
        int[] out = new int[Nodig.values().length];
        Inventory inv = player.getInventory();
        for (Nodig n : Nodig.values()) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (n.past(s)) {
                    out[n.ordinal()] += s.getCount();
                }
            }
        }
        return out;
    }

    /** Can this be made from this stock? (Both sides.) */
    public static boolean kan(int[] voorraad, Recept.Deeg deeg, Recept.Topping topping) {
        for (var e : nodig(deeg, topping).entrySet()) {
            if (voorraad.length <= e.getKey().ordinal() || voorraad[e.getKey().ordinal()] < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes the ingredients out of the pockets (fresh Knuffeldal ones first; a milk bucket gives its bucket back).
     * Returns whether everything that can be fresh was fresh ("boerderijvers"), or null when something is missing.
     */
    @Nullable
    static Genomen neem(ServerPlayer player, Recept.Deeg deeg, Recept.Topping topping) {
        Map<Nodig, Integer> need = nodig(deeg, topping);
        if (!kan(voorraad(player), deeg, topping)) {
            return null;
        }
        boolean vers = true, versKon = false, meel = false;
        Inventory inv = player.getInventory();
        for (var e : need.entrySet()) {
            Nodig n = e.getKey();
            int left = e.getValue();
            versKon |= n.tag != null;
            for (int pass = -1; pass < 2 && left > 0; pass++) {   // (pass -1: knabbelmeel first, 2.9)
                for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                    ItemStack s = inv.getItem(i);
                    boolean isMeel = n == Nodig.GRAAN && s.is(KNABBELMEEL);
                    if (!n.past(s) || (pass == -1) != isMeel || (pass >= 0 && (pass == 0) != n.vers(s))) {
                        continue;
                    }
                    meel |= isMeel;
                    if (pass == 1 && n.tag != null) {
                        vers = false;
                    }
                    int take = Math.min(left, s.getCount());
                    boolean bucket = s.is(Items.MILK_BUCKET);
                    s.shrink(take);
                    left -= take;
                    if (bucket) {
                        Minigames.give(player, new ItemStack(Items.BUCKET, take));
                    }
                }
            }
        }
        return new Genomen(vers && versKon, meel);
    }

    // =================================================================================================================
    // the screen
    // =================================================================================================================

    /** Right-click on a knabbeloven: the baking screen (for Korstje's game when you're playing it here). */
    public static void open(ServerPlayer player, BlockPos oven) {
        BakkerijGame game = BakkerijGame.gameOf(player);
        boolean spel = game != null && game.oven(oven);
        if (game != null && !spel) {
            player.displayClientMessage(Component.translatable("gui.guhs.bakkerij.andere_oven").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        if (!spel && Minigames.playing(player) != null) {
            player.displayClientMessage(Component.translatable("quest.guhs.minigame.busy").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        CompoundTag data = status(player, spel ? game : null);
        data.putLong("Oven", oven.asLong());
        Bak bak = BAKKEN.get(player.getUUID());
        if (bak != null && bak.oven().equals(oven)) {
            data.putInt("Bakt", bak.recept().ordinal());
            data.putInt("Al", (int) (player.level().getGameTime() - bak.start()));
            data.putInt("BakTicks", bak.recept().bakTicks);
        }
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new BakkerijPayloads.Open(BakkerijPayloads.BAKSCHERM, oven.asLong(), data));
    }

    /** What the screen shows: game or own baking, the stock, the recipes you know, the orders. */
    static CompoundTag status(ServerPlayer player, @Nullable BakkerijGame game) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("Spel", game != null);
        data.putIntArray("Voorraad", voorraad(player));
        StringBuilder bekend = new StringBuilder();
        for (Recept r : Recept.BOEK) {
            if (KnusVoortgang.heeft(player, BakkerijVoortgang.RECEPTENBOEK, r.id())) {
                bekend.append(r.ordinal()).append(',');
            }
        }
        data.putString("Bekend", bekend.toString());
        if (game != null) {
            game.bestellingen(data);
        }
        return data;
    }

    // =================================================================================================================
    // baking
    // =================================================================================================================

    /** Is something baking in this oven? */
    public static boolean bakt(Level level, BlockPos oven) {
        return BAKKEN.values().stream().anyMatch(b -> b.level() == level && b.oven().equals(oven));
    }

    public static boolean bakt(ServerPlayer player) {
        return BAKKEN.containsKey(player.getUUID());
    }

    /** Slide it in: returns the lang key of why not, or null when it bakes. */
    @Nullable
    public static String start(ServerPlayer player, BlockPos oven, Recept.Deeg deeg, Recept.Vorm vorm, Recept.Topping topping) {
        ServerLevel level = player.serverLevel();
        if (!(level.getBlockState(oven).getBlock() instanceof KnabbelovenBlock) || player.distanceToSqr(oven.getCenter()) > 49) {
            return "gui.guhs.bakkerij.te_ver";
        }
        if (BAKKEN.containsKey(player.getUUID())) {
            return "gui.guhs.bakkerij.al_bezig";
        }
        Recept recept = Recept.van(deeg, vorm, topping);
        BakkerijGame game = BakkerijGame.gameOf(player);
        boolean spel = game != null && game.oven(oven);
        if (recept == null) {
            return "gui.guhs.bakkerij.geen_recept";
        }
        if (recept == Recept.ROZE_GUH_KOEK && (spel || !nl.juiced.guhs.feature.piep.ReceptItem.kent(player))) {
            return "gui.guhs.piep.recept_onbekend";   // 2.8.1 Piep: only once you learned it, and not in Korstje's game
        }
        boolean vers = false, meel = false;
        if (spel) {
            if (recept == Recept.FEESTTAART && !game.feest()) {
                return "gui.guhs.bakkerij.geheim";
            }
        } else {
            if (game != null) {
                return "gui.guhs.bakkerij.andere_oven";
            }
            if (recept == Recept.FEESTTAART) {
                return "gui.guhs.bakkerij.geheim";
            }
            Genomen v = neem(player, deeg, topping);
            if (v == null) {
                return "gui.guhs.bakkerij.te_weinig";
            }
            vers = v.vers();
            meel = v.meel();
        }
        BAKKEN.put(player.getUUID(), new Bak(oven.immutable(), level, recept, level.getGameTime(), spel, vers, meel));
        KnabbelovenBlock.aan(level, oven, recept.bakTicks + VERGETEN);
        level.playSound(null, oven, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1f);
        level.sendParticles(BakkerijFeature.MEELSTOFJE.get(), oven.getX() + 0.5, oven.getY() + 1.1, oven.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.01);
        return null;
    }

    /** The result of taking it out. */
    public record Uit(Recept recept, Recept.Kwaliteit kwaliteit, int aantal, boolean spel) {
    }

    /** Take it out: {@code clientTicks} is how long the player's screen saw it in the oven (-1: use the server's time). */
    @Nullable
    public static Uit eruit(ServerPlayer player, int clientTicks) {
        Bak bak = BAKKEN.remove(player.getUUID());
        if (bak == null) {
            return null;
        }
        int server = (int) (player.level().getGameTime() - bak.start());
        int ticks = clientTicks >= 0 && Math.abs(clientTicks - server) <= TOLERANTIE ? clientTicks : server;
        Recept.Kwaliteit k = Recept.Kwaliteit.na(ticks, bak.recept().bakTicks);
        return klaar(player, bak, k);
    }

    /** Takes a bake out with a given quality (the tests; also the burnt one that was forgotten). */
    static Uit klaar(ServerPlayer player, Bak bak, Recept.Kwaliteit k) {
        ServerLevel level = player.serverLevel();
        BlockPos oven = bak.oven();
        level.playSound(null, oven, BakkerijFeature.OVEN_DING.get(), SoundSource.BLOCKS, 1f, k == Recept.Kwaliteit.PERFECT ? 1.5f : 1.1f);
        if (k == Recept.Kwaliteit.AANGEBRAND) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, oven.getX() + 0.5, oven.getY() + 1, oven.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
        } else {
            level.sendParticles(BakkerijFeature.KNABBELWOLKJE.get(), oven.getX() + 0.5, oven.getY() + 1.1, oven.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.01);
        }
        if (k == Recept.Kwaliteit.PERFECT) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, oven.getX() + 0.5, oven.getY() + 1, oven.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0);
            GuhAdvancements.grant(player, "bakkerij_perfect");
        }
        if (bak.spel()) {
            BakkerijGame game = BakkerijGame.gameOf(player);
            if (game != null) {
                game.gebakken(player, bak.recept(), k);
            }
            return new Uit(bak.recept(), k, k == Recept.Kwaliteit.AANGEBRAND ? 0 : 1, true);
        }
        int aantal = switch (k) {
            case PERFECT -> PERFECT_AANTAL;
            case GOED -> GOED_AANTAL;
            case RAUW -> RAUW_AANTAL;
            case AANGEBRAND -> 0;
        };
        if (aantal > 0 && bak.vers()) {
            aantal++;
        }
        if (aantal > 0 && bak.meel()) {   // 2.9: baked with knabbelmeel from a guh-molentje
            aantal *= MEEL_KEER;
            player.displayClientMessage(Component.translatable("gui.guhs.guhpolder.meel_dubbel").withStyle(ChatFormatting.AQUA), true);
        }
        if (aantal > 0) {
            Minigames.give(player, new ItemStack(BakkerijFeature.bakje(bak.recept()), aantal));
            KnusVoortgang.tel(player, BakkerijVoortgang.ZELF_GEBAKKEN, aantal);
            GuhAdvancements.grant(player, "bakkerij_oven");
        }
        if (k == Recept.Kwaliteit.GOED || k == Recept.Kwaliteit.PERFECT) {
            BakkerijVoortgang.ontdek(player, bak.recept());
        }
        return new Uit(bak.recept(), k, aantal, false);
    }

    /** Close enough to the oven of the running bake to take it out (the same 7 blocks as sliding it in)? */
    static boolean bijDeOven(ServerPlayer player) {
        Bak bak = BAKKEN.get(player.getUUID());
        return bak != null && bak.level() == player.level() && player.distanceToSqr(bak.oven().getCenter()) <= 49;
    }

    /** (Tests) the running bake of this player. */
    @Nullable
    static Bak bak(ServerPlayer player) {
        return BAKKEN.get(player.getUUID());
    }

    /** A forgotten bake burns by itself; a bake whose game ended goes away. */
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 10 != 0) {
            return;
        }
        Bak bak = BAKKEN.get(p.getUUID());
        if (bak == null) {
            return;
        }
        if (bak.level() != p.level()) {
            BAKKEN.remove(p.getUUID());
            return;
        }
        if (bak.spel() && !BakkerijGame.isPlaying(p)) {
            BAKKEN.remove(p.getUUID());
            return;
        }
        if (p.level().getGameTime() - bak.start() > bak.recept().bakTicks + VERGETEN) {
            BAKKEN.remove(p.getUUID());
            klaar(p, bak, Recept.Kwaliteit.AANGEBRAND);
            p.displayClientMessage(Component.translatable("gui.guhs.bakkerij.vergeten").withStyle(ChatFormatting.GRAY), true);
            nl.juiced.guhs.network.ModNetworking.sendTo(p, new BakkerijPayloads.Status(uitStatus(p, new Uit(bak.recept(), Recept.Kwaliteit.AANGEBRAND, 0, bak.spel()))));
        }
    }

    /** The screen's update after taking something out. */
    static CompoundTag uitStatus(ServerPlayer player, Uit uit) {
        BakkerijGame game = uit.spel() ? BakkerijGame.gameOf(player) : null;
        CompoundTag data = status(player, game);
        data.putBoolean("Klaar", true);
        data.putInt("Recept", uit.recept().ordinal());
        data.putInt("Kwaliteit", uit.kwaliteit().ordinal());
        data.putInt("Aantal", uit.aantal());
        return data;
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        BAKKEN.remove(event.getEntity().getUUID());
    }

    /** (Tests / game end) forgets this player's bake. */
    static void vergeet(ServerPlayer player) {
        BAKKEN.remove(player.getUUID());
    }
}
