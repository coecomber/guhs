package nl.juiced.guhs.feature.toveren;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.registry.ModEntities;
import org.jspecify.annotations.Nullable;

/**
 * The guh enchantments ("spreukjes"): their keys and tags, and the part of their effect that data cannot do. The
 * definitions are data (data/guhs/enchantment/*.json, tools/features/toveren.py); Mika-mepper is pure data (a damage
 * multiplier against {@code #guhs:mikas}), the rest lives here:
 * <ul>
 * <li><b>Pluisval</b> (boots): no fall damage when you land on wool, vilt or pluis ({@code #guhs:toveren/zacht}); a 'poef'
 * of fluff.</li>
 * <li><b>Glitterzicht</b> (helmet): a soft night vision, only underground (no sky above and below the surface).</li>
 * <li><b>Spaarguhtje</b> (mining tools, book only): what you mine, and what you pick up with the tool in hand, goes straight
 * into your Guhbank (the bank of a Banksleutel in your inventory) with a little 'klingel'; what the bank cannot take (no
 * key, the bank stands nowhere, full) stays a normal drop / goes into your inventory.</li>
 * <li><b>Bakvads</b> (mining tools): drops come out smelted (the smelting recipes), count for count, so fortune's extra
 * raw ore becomes extra ingots; the smelting experience comes with it.</li>
 * <li><b>Aaizacht</b> (weapons, book only): your hits never hurt your own pets (tamed guhs and every other ownable animal)
 * or villagers.</li>
 * <li><b>Vadsvlijt</b> (tools): much faster mining in wool, vilt and kaasknabbel blocks ({@code #guhs:toveren/vadsvlijt}).</li>
 * <li><b>Kaasknabbelgeur</b> (any armour piece, book only): wild guhs within 16 blocks walk to you and follow you
 * ({@link KaasknabbelGeurGoal}).</li>
 * </ul>
 * Durability enchantments are never for unbreakable gear ({@link #magOp}); for the Vahoege Vads gear and the Mika-mepper
 * the tag {@code minecraft:enchantable/durability} also drops them (a NeoForge "remove"), which keeps the vanilla table,
 * the anvil and the trade books away from them as well.
 */
public final class Spreuken {
    public static final ResourceKey<Enchantment> MIKA_MEPPER = key("mika_mepper");
    public static final ResourceKey<Enchantment> PLUISVAL = key("pluisval");
    public static final ResourceKey<Enchantment> GLITTERZICHT = key("glitterzicht");
    public static final ResourceKey<Enchantment> SPAARGUHTJE = key("spaarguhtje");
    public static final ResourceKey<Enchantment> BAKVADS = key("bakvads");
    public static final ResourceKey<Enchantment> AAIZACHT = key("aaizacht");
    public static final ResourceKey<Enchantment> VADSVLIJT = key("vadsvlijt");
    public static final ResourceKey<Enchantment> KAASKNABBELGEUR = key("kaasknabbelgeur");

    /** The Glinsteraltaar's pool. */
    public static final TagKey<Enchantment> ALTAAR = TagKey.create(Registries.ENCHANTMENT, Guhs.id("glinsteraltaar"));
    /** Every guh enchantment. */
    public static final TagKey<Enchantment> ALLE = TagKey.create(Registries.ENCHANTMENT, Guhs.id("spreukjes/alle"));
    /** The guh enchantments that only come as a Spreukjesboekje (villagers, loot): never from the altar. */
    public static final TagKey<Enchantment> BOEKJE = TagKey.create(Registries.ENCHANTMENT, Guhs.id("spreukjes/boekje"));
    /** Durability enchantments (unbreaking, mending): pointless on unbreakable gear. */
    public static final TagKey<Enchantment> DUURZAAMHEID = TagKey.create(Registries.ENCHANTMENT, Guhs.id("duurzaamheid"));
    /** Where Pluisval lands softly. */
    public static final TagKey<Block> ZACHT = TagKey.create(Registries.BLOCK, Guhs.id("toveren/zacht"));
    /** What Vadsvlijt mines fast. */
    public static final TagKey<Block> VLIJT = TagKey.create(Registries.BLOCK, Guhs.id("toveren/vadsvlijt"));

    /** Vadsvlijt: the mining speed is multiplied by 1 + this x the level. */
    static final float VLIJT_PER_NIVEAU = 2.0f;
    /** Glitterzicht's night vision (ambient, no particles): refreshed while it has less than this left. */
    static final int ZICHT_DUUR = 300, ZICHT_VERNIEUW = 240;

    private Spreuken() {
    }

    private static ResourceKey<Enchantment> key(String naam) {
        return ResourceKey.create(Registries.ENCHANTMENT, Guhs.id(naam));
    }

    static void register(IEventBus bus) {
        bus.addListener(Spreuken::onFall);
        bus.addListener(Spreuken::onPlayerTick);
        bus.addListener(Spreuken::onDrops);
        bus.addListener(Spreuken::onPickup);
        bus.addListener(Spreuken::onDamage);
        bus.addListener(Spreuken::onBreakSpeed);
        bus.addListener(Spreuken::onJoin);
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    public static Optional<Holder.Reference<Enchantment>> holder(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
    }

    /** The level of this enchantment on this stack (0: none). */
    public static int niveau(Level level, ResourceKey<Enchantment> key, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return holder(level, key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
    }

    /** The highest level of this enchantment in the slots it works in (armour, hands), on this entity. */
    public static int niveau(LivingEntity entity, ResourceKey<Enchantment> key) {
        return holder(entity.level(), key).map(h -> EnchantmentHelper.getEnchantmentLevel(h, entity)).orElse(0);
    }

    /** May this enchantment go on this stack? Never a durability enchantment on unbreakable gear. */
    public static boolean magOp(ItemStack stack, Holder<Enchantment> enchantment) {
        return !(stack.has(DataComponents.UNBREAKABLE) && enchantment.is(DUURZAAMHEID));
    }

    // =====================================================================================================================
    // Pluisval
    // =====================================================================================================================

    static void onFall(LivingFallEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || niveau(e, PLUISVAL) <= 0 || !zachtOnder(e)) {
            return;
        }
        event.setDamageMultiplier(0f);
        if (event.getDistance() > 1.5) {
            poef(level, e);
        }
    }

    /** Is the entity standing on (or in: a carpet) something soft? */
    static boolean zachtOnder(Entity e) {
        return e.getBlockStateOn().is(ZACHT) || e.level().getBlockState(e.blockPosition()).is(ZACHT);
    }

    static void poef(ServerLevel level, Entity e) {
        level.sendParticles(ToverenFeature.PLUISPOEF.get(), e.getX(), e.getY() + 0.1, e.getZ(), 14, 0.35, 0.05, 0.35, 0.03);
        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    // =====================================================================================================================
    // Glitterzicht
    // =====================================================================================================================

    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 7) {
            return;
        }
        MobEffectInstance nu = player.getEffect(MobEffects.NIGHT_VISION);
        boolean vanOns = nu != null && nu.isAmbient() && !nu.isVisible() && nu.getDuration() <= ZICHT_DUUR;
        if (niveau(player, GLITTERZICHT) > 0 && onderGrond(level, player.blockPosition())) {
            if (nu == null || vanOns && nu.getDuration() < ZICHT_VERNIEUW) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ZICHT_DUUR, 0, true, false, true));
            }
        } else if (vanOns) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /** Underground: no sky above, and a few blocks below the surface of this column. */
    public static boolean onderGrond(Level level, BlockPos pos) {
        return !level.canSeeSky(pos.above()) && pos.getY() < level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 3;
    }

    // =====================================================================================================================
    // Bakvads + Spaarguhtje (drops), Spaarguhtje (pickup)
    // =====================================================================================================================

    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = event.getLevel();
        ItemStack tool = event.getTool();
        if (niveau(level, BAKVADS, tool) > 0) {
            float xp = 0;
            boolean gebakken = false;
            for (ItemEntity drop : event.getDrops()) {
                Gebakken g = bak(level, drop.getItem());
                if (g != null) {
                    drop.setItem(g.stack());
                    xp += g.xp();
                    gebakken = true;
                }
            }
            int heel = Mth.floor(xp);
            if (level.getRandom().nextFloat() < xp - heel) {
                heel++;
            }
            if (heel > 0) {
                event.setDroppedExperience(event.getDroppedExperience() + heel);
            }
            if (gebakken) {
                BlockPos p = event.getPos();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
            }
        }
        if (niveau(level, SPAARGUHTJE, tool) > 0) {
            ResourceHandler<ItemResource> bank = bank(player);
            if (bank == null) {
                return;
            }
            boolean iets = false;
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemEntity drop = it.next();
                ItemStack rest = Kisten.stop(bank, drop.getItem());
                if (rest.getCount() != drop.getItem().getCount()) {
                    iets = true;
                }
                if (rest.isEmpty()) {
                    it.remove();
                } else {
                    drop.setItem(rest);
                }
            }
            if (iets) {
                klingel(player);
            }
        }
    }

    record Gebakken(ItemStack stack, float xp) {
    }

    /** The stack smelted (count for count), or null when it does not smelt. */
    static @Nullable Gebakken bak(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        Optional<RecipeHolder<SmeltingRecipe>> recept = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
        if (recept.isEmpty()) {
            return null;
        }
        ItemStack uit = recept.get().value().assemble(input);
        if (uit.isEmpty()) {
            return null;
        }
        return new Gebakken(uit.copyWithCount(Math.min(uit.getMaxStackSize(), uit.getCount() * stack.getCount())),
                recept.get().value().experience() * stack.getCount());
    }

    static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.canPickup().isFalse()) {
            return;
        }
        ItemEntity item = event.getItemEntity();
        if (item.hasPickUpDelay() || item.isRemoved() || niveau(player.level(), SPAARGUHTJE, player.getMainHandItem()) <= 0) {
            return;
        }
        ResourceHandler<ItemResource> bank = bank(player);
        if (bank == null) {
            return;
        }
        ItemStack stack = item.getItem();
        ItemStack rest = Kisten.stop(bank, stack);
        int erin = stack.getCount() - rest.getCount();
        if (erin <= 0) {
            return;
        }
        player.take(item, erin);
        klingel(player);
        if (rest.isEmpty()) {
            item.discard();
            event.setCanPickup(TriState.FALSE);
        } else {
            item.setItem(rest);   // (the rest goes into the inventory the normal way)
        }
    }

    /**
     * The player's Guhbank: the bank of the first Banksleutel in their inventory that remembers one (its chunk is loaded
     * for a little while if it was not, like a Hapluikje's delivery). Null: no key, or the bank stands nowhere.
     */
    static @Nullable ResourceHandler<ItemResource> bank(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.is(BankFeature.BANK_SLEUTEL.get())) {
                continue;
            }
            UUID id = BankSleutelItem.bank(s);
            if (id == null) {
                continue;
            }
            BankGuhBlockEntity be = BankAdressen.zoek(player.level().getServer(), id, true);
            if (be != null && be.getLevel() != null) {
                ResourceHandler<ItemResource> h = Kisten.van(be.getLevel(), be.getBlockPos(), null);
                if (h != null) {
                    return h;
                }
            }
        }
        return null;
    }

    static void klingel(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.35f,
                1.7f + player.getRandom().nextFloat() * 0.2f);
    }

    // =====================================================================================================================
    // Aaizacht
    // =====================================================================================================================

    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack wapen = event.getSource().getWeaponItem();
        if (wapen == null) {
            wapen = player.getMainHandItem();
        }
        if (niveau(player.level(), AAIZACHT, wapen) > 0 && beschermd(player, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Never hit by an Aaizacht weapon of this player: their own pets (tamed guhs too) and every villager. */
    public static boolean beschermd(Player player, LivingEntity doel) {
        if (doel instanceof AbstractVillager) {
            return true;
        }
        return doel instanceof OwnableEntity pet && pet.getOwner() == player;
    }

    // =====================================================================================================================
    // Vadsvlijt
    // =====================================================================================================================

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (!state.is(VLIJT)) {
            return;
        }
        int n = niveau(event.getEntity().level(), VADSVLIJT, event.getEntity().getMainHandItem());
        if (n > 0) {
            event.setNewSpeed(event.getNewSpeed() * (1 + VLIJT_PER_NIVEAU * n));
        }
    }

    // =====================================================================================================================
    // Kaasknabbelgeur
    // =====================================================================================================================

    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof GuhEntity guh && guh.getType() == ModEntities.GUH.get()) {
            KaasknabbelGeurGoal.zorgVoor(guh);
        }
    }
}
