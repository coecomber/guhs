package nl.juiced.guhs.feature.kaasmoeras;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.ModDimensions;

/**
 * The kaasmoeras outside its own blocks and creatures:
 * <ul>
 *   <li>a wild guh born in the kaasmoeras is often a Kaasmoerasguh ({@link #MOERASGUH_CHANCE}; only there, decided once
 *       per guh when it first joins the world);</li>
 *   <li>the Guhdex pages of the kikkerguh, the kaasmot and the Moerasheks-Mika fill in when you're next to one;</li>
 *   <li>the quest/advancement hooks: bouncing on borrelende kaassaus, opening a knabbelvlotje, all three motknabbels.</li>
 * </ul>
 */
public final class KaasmoerasEvents {
    /** Out of the wild guhs born in the kaasmoeras, this many are a Kaasmoerasguh. */
    public static final float MOERASGUH_CHANCE = 0.4f;
    /** How close you have to be to a kikkerguh, kaasmot or Moerasheks-Mika for its Guhdex page. */
    public static final double SEE_RANGE = 3.0;
    static final String CHECKED = "guhs_kaasmoeras_checked";
    public static final ResourceKey<LootTable> VLOTJE_LOOT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            Guhs.id("chests/knabbelvlotje"));

    /** The Guhdex pages of the kaasmoeras creatures (their GuhVariant id is also their entity id). */
    public static Map<GuhVariant, EntityType<?>> creaturePages() {
        return Map.of(GuhVariant.KIKKERGUH, KaasmoerasFeature.KIKKERGUH.get(), GuhVariant.KAASMOT, KaasmoerasFeature.KAASMOT.get(),
                GuhVariant.MOERASHEKS_MIKA, KaasmoerasFeature.MOERASHEKS_MIKA.get());
    }

    // --- the Kaasmoerasguh ----------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof GuhEntity guh) {
            maybeMoerasguh(level, guh);
        }
    }

    /**
     * Decides (once per guh) whether a new wild guh in the kaasmoeras becomes a Kaasmoerasguh. Only plain wild grown-up
     * guhs (not tamed, no variant, no name) in the Guhmension. Returns true when it did.
     */
    public static boolean maybeMoerasguh(ServerLevel level, GuhEntity guh) {
        if (guh.getClass() != GuhEntity.class || guh.getPersistentData().getBooleanOr(CHECKED, false)) {
            return false;
        }
        return decide(guh, level.dimension() == ModDimensions.GUHMENSION && inKaasmoeras(level, guh.blockPosition()));
    }

    /** The decision itself (once per guh): in the kaasmoeras, a plain wild grown-up guh may become a Kaasmoerasguh. */
    public static boolean decide(GuhEntity guh, boolean inKaasmoeras) {
        CompoundTag data = guh.getPersistentData();
        if (data.getBooleanOr(CHECKED, false)) {
            return false;
        }
        data.putBoolean(CHECKED, true);
        if (!inKaasmoeras || guh.isTame() || guh.isBaby() || guh.hasCustomName() || guh.getVariant() != GuhVariant.NORMAL) {
            return false;
        }
        if (guh.getRandom().nextFloat() < MOERASGUH_CHANCE) {
            guh.setVariant(GuhVariant.KAASMOERASGUH);
            return true;
        }
        return false;
    }

    public static boolean inKaasmoeras(Level level, BlockPos pos) {
        return level.getBiome(pos).is(KaasmoerasFeature.KAASMOERAS);
    }

    // --- the Guhdex, quests and advancements ------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (player.tickCount % 10 == 0) {
            seeCreatures(player);
        }
        if (player.tickCount % 40 == 0 && hasAllMotknabbels(player)) {
            GuhAdvancements.grant(player, "kaasmoeras_motknabbels");
            grantShown(player, "guhmension/kaasmoeras_regenboog");
        }
    }

    /** Fills in the Guhdex pages of the kaasmoeras creatures right next to the player. */
    public static void seeCreatures(ServerPlayer player) {
        Map<GuhVariant, EntityType<?>> pages = creaturePages();
        Set<GuhVariant> near = EnumSet.noneOf(GuhVariant.class);
        for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(SEE_RANGE))) {
            for (var page : pages.entrySet()) {
                if (e.getType() == page.getValue()) {
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
            if (p.seen.add(v)) {
                data.setDirty();
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhdex.new", Component.translatable("entity.guhs." + v.id()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.2f);
            }
        }
    }

    public static boolean hasAllMotknabbels(ServerPlayer player) {
        Set<MotknabbelBlock.Kleur> kleuren = EnumSet.noneOf(MotknabbelBlock.Kleur.class);
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(KaasmoerasFeature.MOTKNABBEL_ITEM.get())) {
                kleuren.add(MotknabbelBlock.kleur(stack));
            }
        }
        return kleuren.size() == MotknabbelBlock.Kleur.values().length;
    }

    /** Boing: somebody bounced on borrelende kaassaus. */
    public static void bounced(ServerPlayer player) {
        GuhAdvancements.grant(player, "kaasmoeras_stuiter");
        grantShown(player, "guhmension/kaasmoeras_stuiter");
    }

    /** Barrel data: this is the barrel of a knabbelvlotje (stays when the loot is gone). */
    public static final String VLOTJE_TON = "guhs_vlotje_ton";

    /**
     * Opening the barrel of a knabbelvlotje. 1.2.7: for everybody, also when somebody else took the loot already (the
     * barrel is marked the first time; one that was emptied before 1.2.7 is known by the raft around it).
     */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getLevel().getBlockEntity(event.getPos()) instanceof RandomizableContainerBlockEntity container
                && isVlotjeTon(player.level(), event.getPos(), container)) {
            GuhAdvancements.grant(player, "kaasmoeras_vlotje");
            grantShown(player, "guhmension/kaasmoeras_vlotje");
        }
    }

    /** Is this the loot barrel of a knabbelvlotje (with or without its loot)? */
    public static boolean isVlotjeTon(net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomizableContainerBlockEntity container) {
        if (container.getPersistentData().getBooleanOr(VLOTJE_TON, false)) {
            return true;
        }
        boolean ja = VLOTJE_LOOT.equals(container.getLootTable()) || lijktOpVlotje(level, pos);
        if (ja) {
            container.getPersistentData().putBoolean(VLOTJE_TON, true);
            container.setChanged();
        }
        return ja;
    }

    /** How many parts of a knabbelvlotje have to be around a barrel (out of the seven {@link #lijktOpVlotje} looks at). */
    public static final int VLOTJE_DELEN = 3;

    /**
     * The raft of KaasmoerasPoelFeature.vlotje around this barrel, in the Guhmensie: the spruce slab under it, the floater
     * log next to that, the mast (two spruce fences at +2, +1) with its pink sail, the lampion post at +3, +2 and the
     * block of kaasknabbels at 0, +2. A few of them are enough: in a generated world a pool next door now and then
     * takes a bite out of a raft (kaasmodder where the slab under the barrel was).
     */
    public static boolean lijktOpVlotje(net.minecraft.server.level.ServerLevel level, BlockPos ton) {
        return level.dimension() == nl.juiced.guhs.world.ModDimensions.GUHMENSION && vlotDelen(level, ton) >= VLOTJE_DELEN;
    }

    /** How many of the seven parts of a knabbelvlotje stand around this barrel (0: no barrel). */
    public static int vlotDelen(net.minecraft.world.level.BlockGetter level, BlockPos ton) {
        if (!level.getBlockState(ton).is(net.minecraft.world.level.block.Blocks.BARREL)) {
            return 0;
        }
        var fence = net.minecraft.world.level.block.Blocks.SPRUCE_FENCE;
        int delen = 0;
        delen += level.getBlockState(ton.below()).is(net.minecraft.world.level.block.Blocks.SPRUCE_SLAB) ? 1 : 0;
        delen += level.getBlockState(ton.offset(0, -1, -1)).is(net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_LOG) ? 1 : 0;
        delen += level.getBlockState(ton.offset(2, 0, 1)).is(fence) ? 1 : 0;
        delen += level.getBlockState(ton.offset(2, 1, 1)).is(fence) ? 1 : 0;
        delen += level.getBlockState(ton.offset(1, 1, 1)).is(net.minecraft.world.level.block.Blocks.PINK_WOOL) ? 1 : 0;
        delen += level.getBlockState(ton.offset(3, 0, 2)).is(fence) ? 1 : 0;
        delen += level.getBlockState(ton.offset(0, 0, 2)).is(nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get()) ? 1 : 0;
        return delen;
    }

    /** Grants one of our shown advancements (with an impossible trigger: the mod decides when). */
    public static void grantShown(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private KaasmoerasEvents() {
    }
}
