package nl.juiced.guhs.quest;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The Guhdex: a guh variant you've seen (within {@link #SEE_RANGE} blocks, guhs and guh characters alike) fills in its
 * page, taming one gives it a star. Milestones give rewards (claimed in the Guhdex book).
 */
public final class GuhDex {
    /** 2.10: close by is close enough (3 blocks; it was 1, right next to it: a guh that keeps hopping away was hard to "see"). */
    public static final int SEE_RANGE = 3;
    /** Every guh in the Guhdex, in page order (3.0: plus every GuhVariant after ROOKGUH, in enum order: see {@link #na}). */
    public static final List<GuhVariant> ENTRIES = na(GuhVariant.ROOKGUH, List.of(GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW,
            GuhVariant.BRONTOSAURUS, GuhVariant.TECKEL, GuhVariant.GHOST, GuhVariant.STARRY, GuhVariant.RAINBOW, GuhVariant.ENDER, GuhVariant.VAHOEGE_ENDER, GuhVariant.KONING, GuhVariant.WOLK, GuhVariant.ZEEMEERGUH, GuhVariant.GOLDEN, GuhVariant.MAGER,
            GuhVariant.BROCOCOLIEF, GuhVariant.REISGUH, GuhVariant.POORTWACHTER,
            GuhVariant.SHOWGUH, GuhVariant.RACEGUH, GuhVariant.MEPGUH, GuhVariant.DJGUH, GuhVariant.GOLFGUH, GuhVariant.SMULGUH, GuhVariant.VISGUH,
            GuhVariant.MIJNGUH, GuhVariant.BIBLIOTHECARIS,
            GuhVariant.KAASMOERASGUH, GuhVariant.KIKKERGUH, GuhVariant.KAASMOT, GuhVariant.MOERASHEKS_MIKA,
            GuhVariant.BOSWACHTERGUH, GuhVariant.KNABBELPLUKKER,
            GuhVariant.GRILLGUH, GuhVariant.ASGUH, GuhVariant.ROOKGUH,   // (2.10.1: the Rookguh, with your saved-Rookguh count)
            // 2.8 (Knuffeldal)
            GuhVariant.PLUISGUH, GuhVariant.PINGUH, GuhVariant.BURGEMEESTERGUH, GuhVariant.KRUIMEL_MIKA, GuhVariant.BAKKERGUH, GuhVariant.JUF_KNUFFEL,
            GuhVariant.THEEGUH, GuhVariant.KAPPERGUH, GuhVariant.BOERINNEGUH, GuhVariant.GUHSCHAAPJE, GuhVariant.KNABBELKIPPETJE,
            GuhVariant.GUHKOE, GuhVariant.STERRENKIJKERGUH, GuhVariant.BALLONGUH, GuhVariant.OPA_GUH, GuhVariant.BADMEESTERGUH,
            GuhVariant.IJSCOGUH, GuhVariant.COCOTJE,
            // 2.9 (De Grote Guhspelen; PINGUH is right after PLUISGUH above)
            GuhVariant.SJOELGUH, GuhVariant.DOOLHOFGUH, GuhVariant.KATAPULTGUH, GuhVariant.SPELLEIDERGUH, GuhVariant.SCHAATSMEESTERGUH,
            GuhVariant.STEMPELGUH, GuhVariant.CIRCUITGUH, GuhVariant.BRANDWEERGUH, GuhVariant.POLITIEGUH, GuhVariant.APOTHEKERGUH,
            GuhVariant.BOUWVAKKERGUH));

    /** 3.0: the old list plus every variant after {@code last} in enum order (the Guhverhalen pages: all counting). */
    private static List<GuhVariant> na(GuhVariant last, List<GuhVariant> oud) {
        List<GuhVariant> all = new java.util.ArrayList<>(oud);
        for (GuhVariant v : GuhVariant.values()) {
            if (v.ordinal() > last.ordinal() && !all.contains(v)) {
                all.add(v);
            }
        }
        return List.copyOf(all);
    }
    /** The pages you can also tame (the characters can't be). */
    public static final List<GuhVariant> TAMEABLE = ENTRIES.stream().filter(v -> !v.isCharacter()).toList();
    /**
     * 2.10.1: bonus pages: in the Guhdex, but they don't count for progress (the "all pages" milestones, the maag upgrade
     * that needs a full Guhdex, the seen counter), so "alles verzameld" is exactly what it was in 2.10.0.
     */
    public static final java.util.Set<GuhVariant> EXTRA = java.util.Set.of(GuhVariant.ROOKGUH,
            GuhVariant.KRAAKGUH, GuhVariant.KRAAK_MIKA);   // (1.2.8: the Bleekwoud is rare and they only come at night)
    /** The pages that count for progress (ENTRIES without the {@link #EXTRA} ones). */
    public static final List<GuhVariant> TELLEND = ENTRIES.stream().filter(v -> !EXTRA.contains(v)).toList();

    /** How many of these seen pages count for progress (the {@link #EXTRA} pages don't). */
    public static int geteld(java.util.Collection<GuhVariant> seen) {
        return (int) seen.stream().filter(v -> !EXTRA.contains(v)).count();
    }

    /** A full Guhdex (the maag upgrade that asks for it): every counting page seen; bonus pages not needed. */
    public static boolean vol(java.util.Collection<GuhVariant> seen) {
        return geteld(seen) >= TELLEND.size();
    }

    /** The same, for the client's page ids. */
    public static int geteldIds(java.util.Collection<String> seenIds) {
        return (int) seenIds.stream().filter(id -> EXTRA.stream().noneMatch(v -> v.id().equals(id))).count();
    }

    /** A milestone: how many seen, how many tamed, and the reward. */
    public record Milestone(int seen, int tamed, java.util.function.Supplier<Item> reward) {
    }

    public static final List<Milestone> MILESTONES = List.of(
            new Milestone(5, 0, () -> ModItems.clothingItem(GuhClothes.HEART_GLASSES)),
            new Milestone(8, 3, () -> ModItems.clothingItem(GuhClothes.MONOCLE)),
            new Milestone(TELLEND.size(), 0, () -> ModItems.GUH_KRISTAL_VERREKIJKER.get()),
            new Milestone(TELLEND.size(), TAMEABLE.size(), () -> ModItems.clothingItem(GuhClothes.ROYAL_CROWN)));

    /** Creature pages (2.8): a page that isn't a guh or a guh character, filled in by standing near that entity. */
    private static final java.util.Map<GuhVariant, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<?>>> CREATURES =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** 3.0: a creature page's own range (birds, fireflies: 8), else {@link #CREATURE_RANGE}. */
    private static final java.util.Map<GuhVariant, Double> RANGES = new java.util.concurrent.ConcurrentHashMap<>();
    /** How close you have to be to a creature of a creature page. */
    public static final double CREATURE_RANGE = 3.0;

    /**
     * Registers a creature page (2.8; call from your Feature.register): within {@link #CREATURE_RANGE} blocks of an
     * entity of that type the page is seen and {@code guhs:quest/seen_<page id>} is granted. The page's id is also the
     * entity id (the Guhdex draws that entity; lang entity.guhs.&lt;id&gt;).
     */
    public static void creaturePage(GuhVariant page, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<?>> type) {
        CREATURES.put(page, type);
    }

    /** 3.0: the same, seen within {@code range} blocks (little birds and fireflies are hard to get right next to: 8). */
    public static void creaturePage(GuhVariant page, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<?>> type, double range) {
        CREATURES.put(page, type);
        RANGES.put(page, range);
    }

    /** Is this a registered creature page (its entity type exists)? */
    public static boolean isCreaturePage(GuhVariant page) {
        return CREATURES.containsKey(page);
    }

    /** Fills in a page for this player (2.10.1: the Rookguh page when you saved one), with the "new page" message. */
    public static void zie(ServerPlayer player, GuhVariant v) {
        if (!ENTRIES.contains(v)) {
            return;
        }
        GuhAdvancements.grant(player, "seen_" + v.id());
        nl.juiced.guhs.feature.verhaal.VerhaalFeature.paginaGezien(player, v);   // 3.0: the Diertjes tab
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        if (data.player(player.getUUID()).seen.add(v)) {
            data.setDirty();
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhdex.new", Component.translatable("entity.guhs." + v.id()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Fills in the creature pages of the creatures right next to the player (every 10 ticks, from the player tick). */
    public static void seeCreatures(ServerPlayer player) {
        if (CREATURES.isEmpty()) {
            return;
        }
        java.util.Set<GuhVariant> near = java.util.EnumSet.noneOf(GuhVariant.class);
        double max = RANGES.values().stream().mapToDouble(Double::doubleValue).max().orElse(CREATURE_RANGE);
        for (net.minecraft.world.entity.Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(Math.max(max, CREATURE_RANGE)))) {
            for (var page : CREATURES.entrySet()) {
                double range = RANGES.getOrDefault(page.getKey(), CREATURE_RANGE);
                if (e.getType() == page.getValue().get() && (range <= CREATURE_RANGE
                        || player.getBoundingBox().inflate(range).intersects(e.getBoundingBox()))
                        && (range > CREATURE_RANGE || player.getBoundingBox().inflate(CREATURE_RANGE).intersects(e.getBoundingBox()))) {
                    near.add(page.getKey());
                }
            }
        }
        if (near.isEmpty()) {
            return;
        }
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        for (GuhVariant v : near) {
            GuhAdvancements.grant(player, "seen_" + v.id());
            nl.juiced.guhs.feature.verhaal.VerhaalFeature.paginaGezien(player, v);   // 3.0: the Diertjes tab
            if (p.seen.add(v)) {
                data.setDirty();
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhdex.new", Component.translatable("entity.guhs." + v.id()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
            }
        }
    }

    public static void onPlayerTick(ServerPlayer player, GuhWorldData data) {
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        for (nl.juiced.guhs.entity.GuhNpcEntity npc : player.level().getEntitiesOfClass(nl.juiced.guhs.entity.GuhNpcEntity.class,
                player.getBoundingBox().inflate(SEE_RANGE))) {
            GuhVariant v = GuhVariant.ofCharacter(npc.getKind());
            if (v != null && ENTRIES.contains(v)) {
                GuhAdvancements.grant(player, "seen_" + v.id());
                if (p.seen.add(v)) {
                    data.setDirty();
                    player.sendOverlayMessage(Component.translatable("gui.guhs.guhdex.new", Component.translatable("entity.guhs.guh_npc." + v.id()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
                }
            }
        }
        for (GuhEntity guh : player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(SEE_RANGE))) {
            GuhVariant v = guh.getVariant();
            if (ENTRIES.contains(v)) {
                GuhAdvancements.grant(player, "seen_" + v.id());
            }
            if (ENTRIES.contains(v) && p.seen.add(v)) {
                data.setDirty();
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhdex.new", guh.getVariant().displayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
            }
        }
    }

    /** The other guh creatures: right next to one counts as having found it (for the FTB Quests chapter). */
    public static void findCreatures(ServerPlayer player) {
        for (net.minecraft.world.entity.Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(3))) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            if (java.util.Set.of("guh_bee", "guh_slime", "guh_vis", "nether_mika", "mika", "vadswaker").contains(id)) {
                GuhAdvancements.grant(player, "found_" + id);
            }
        }
    }

    /**
     * Stepping into the Guhmensie through a portal (from the Overworld, the Barbecuether or the Guheinde, not by command or
     * respawn): hangs after the portal's own arrival steps (TeleportTransition.then) and gives a Guhdex.
     */
    public static final net.minecraft.world.level.portal.TeleportTransition.PostTeleportTransition GIVE_ON_ARRIVAL = entity -> {
        if (entity instanceof ServerPlayer player) {
            giveOnArrival(player);
        }
    };

    /** A Guhdex for a player entering the Guhmensie, unless they carry one already (full pockets: it drops in front of them). */
    public static boolean giveOnArrival(ServerPlayer player) {
        if (player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.GUHDEX.get()))
                || player.containerMenu.getCarried().is(ModItems.GUHDEX.get())) {
            return false;
        }
        nl.juiced.guhs.feature.Minigames.give(player, new net.minecraft.world.item.ItemStack(ModItems.GUHDEX.get()));
        player.sendSystemMessage(Component.translatable("gui.guhs.guhdex.welcome").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static void onTamed(ServerPlayer player, GuhEntity guh) {
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        p.seen.add(guh.getVariant());
        if (p.tamed.add(guh.getVariant())) {
            data.setDirty();
        }
    }

    public static void open(ServerPlayer player) {
        Highscores.sync(player);                                 // (first, so the Highscores tab is filled right away)
        nl.juiced.guhs.feature.knus.KnusVoortgang.sync(player);   // (and the Knus tab)
        nl.juiced.guhs.feature.spelen.SpelGroepen.sync(player);     // (2.9: the Minigames tab's visited buildings)
        nl.juiced.guhs.feature.kleding.KledingUnlocks.sync(player); // (2.9: the Kleding tab's unlocks)
        nl.juiced.guhs.feature.band.BandPayloads.sync(player);      // (2.10: the Mijn guhs tab)
        nl.juiced.guhs.feature.gids.VerhalenPayloads.sync(player);  // (the Verhalen tab: every questline's step)
        nl.juiced.guhs.feature.titels.TitelsPayloads.sync(player);  // (1.2.6: the Titels tab)
        nl.juiced.guhs.network.ModNetworking.sendTo(player, MaagPayloads.GuhDexData.of(GuhWorldData.get(player.level().getServer()).player(player.getUUID())));
    }

    public static void claim(ServerPlayer player, int milestone) {
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        if (milestone < 0 || milestone >= MILESTONES.size() || p.rewards.contains(milestone)) {
            return;
        }
        Milestone m = MILESTONES.get(milestone);
        if (geteld(p.seen) >= m.seen() && p.tamed.size() >= m.tamed()) {
            p.rewards.add(milestone);
            data.setDirty();
            GuhQuests.give(player, m.reward().get());
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.1f);
            open(player);
        }
    }

    private GuhDex() {
    }
}
