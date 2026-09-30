package nl.juiced.guhs.registry;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;

/**
 * Guh villages: the "guh" villager type (Guhmension biomes, see data/neoforge/data_maps/worldgen/biome/villager_types.json)
 * and the five guh professions that replace the vanilla ones there. Guh villagers only take guh jobs.
 */
public final class ModVillagers {
    public static final DeferredRegister<VillagerType> TYPES = DeferredRegister.create(Registries.VILLAGER_TYPE, Guhs.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, Guhs.MODID);

    public static final DeferredHolder<VillagerType, VillagerType> GUH = TYPES.register("guh", () -> new VillagerType("guh"));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> VADS_TEMMER = profession("vads_temmer", ModPoiTypes.KNABBELBAK, SoundEvents.VILLAGER_WORK_SHEPHERD);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> GUH_KLEERMAKER = profession("guh_kleermaker", ModPoiTypes.NAAITAFEL, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> VADSSMID = profession("vadssmid", ModPoiTypes.VADSAAMBEELD, SoundEvents.VILLAGER_WORK_ARMORER);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> HAMSTERBOUWER = profession("hamsterbouwer", ModPoiTypes.BUIZENBANK, SoundEvents.VILLAGER_WORK_MASON);
    /** The knabbelboer: the guh farmer (kaasknabbel plants, cakes and sweets). */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> KNABBELBOER = profession("knabbelboer", ModPoiTypes.ZAADBAK, SoundEvents.VILLAGER_WORK_FARMER);
    public static final DeferredHolder<VillagerProfession, VillagerProfession> MIKA_JAGER = profession("mika_jager", ModPoiTypes.MIKATROFEE, SoundEvents.VILLAGER_WORK_WEAPONSMITH);

    private static DeferredHolder<VillagerProfession, VillagerProfession> profession(String name, DeferredHolder<PoiType, PoiType> poi,
                                                                                    net.minecraft.sounds.SoundEvent sound) {
        ResourceKey<PoiType> key = poi.getKey();
        return PROFESSIONS.register(name, () -> new VillagerProfession(name, holder -> holder.is(key), holder -> holder.is(key),
                ImmutableSet.of(), ImmutableSet.of(), sound));
    }

    private static net.minecraft.world.item.Item clothes(GuhClothes piece) {
        return ModItems.clothingItem(piece);
    }

    /** 2.9: the kleermaker's fixed full offer: his everyday set, the three ear bows, the pink ribbon and his wool/string buys. */
    public static List<VillagerTrades.ItemListing> kleermakerAanbod() {
        return List.of(
                sell(clothes(GuhClothes.RED_BOWTIE), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_STRIK, 12, 5),
                sell(clothes(GuhClothes.BLACK_BOWTIE), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_STRIK, 12, 5),
                sell(clothes(GuhClothes.SUNGLASSES), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_ZONNEBRIL, 12, 5),
                sell(clothes(GuhClothes.RAIN_HAT), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_REGENHOED, 12, 10),
                sell(clothes(GuhClothes.STRIPED_SWEATER), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_TRUI, 12, 15),
                sell(clothes(GuhClothes.RAINCOAT), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_REGENJAS, 12, 15),
                sell(clothes(GuhClothes.OORSTRIKJE_ROZE), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_OORSTRIKJE, 12, 5),
                sell(clothes(GuhClothes.OORSTRIKJE_MINT), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_OORSTRIKJE, 12, 5),
                sell(clothes(GuhClothes.OORSTRIKJE_GEEL), 1, nl.juiced.guhs.feature.kleding.KledingBronLijst.PRIJS_OORSTRIKJE, 12, 5),
                sell(ModItems.ROZE_LINT.get(), 1, 3, 12, 5), // for the Slee-guh's broken sled
                buy(Items.PINK_WOOL, 12, 1, 16, 2),
                buy(Items.STRING, 14, 1, 16, 2));
    }

    public static boolean isGuhProfession(VillagerProfession profession) {
        return Guhs.MODID.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getNamespace());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Trades (levels 1 novice ... 5 master)
    // ------------------------------------------------------------------------------------------------------------

    private static VillagerTrades.ItemListing sell(ItemLike item, int count, int emeralds, int maxUses, int xp) {
        return new BasicItemListing(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item, count), maxUses, xp, 0.05f);
    }

    private static VillagerTrades.ItemListing buy(ItemLike item, int count, int emeralds, int maxUses, int xp) {
        return new BasicItemListing(new ItemStack(item, count), new ItemStack(Items.EMERALD, emeralds), maxUses, xp, 0.05f);
    }

    private static void add(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level, VillagerTrades.ItemListing... listings) {
        trades.get(level).addAll(List.of(listings));
    }

    public static void onTrades(VillagerTradesEvent event) {
        var t = event.getTrades();
        if (event.getType() == VADS_TEMMER.get()) {
            add(t, 1, buy(ModItems.KAAS_KNABBELS.get(), 16, 1, 16, 2), sell(ModItems.GUH_SPAWN_EGG.get(), 1, 8, 6, 5));
            add(t, 2, sell(Items.SADDLE, 1, 6, 6, 10), buy(ModBlocks.BLOCK_OF_KAASKNABBELS.get(), 2, 1, 12, 10),
                    sell(ModItems.HEILIGDOM_KOMPAS.get(), 1, 12, 3, 10), // to Moeder Vadsig
                    sell(ModItems.SUPERKOMPAS.get(), 1, 10, 3, 10)); // finds any guh structure you choose
            add(t, 3, sell(ModItems.IRON_GUH_ARMOR.get(), 1, 12, 3, 15), sell(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 4, 3, 12, 10));
            add(t, 4, sell(ModItems.DIAMOND_GUH_ARMOR.get(), 1, 28, 3, 20), sell(ModItems.GUH_SPAWN_EGG.get(), 3, 20, 4, 20));
            add(t, 5, sell(ModItems.NETHERITE_GUH_ARMOR.get(), 1, 48, 2, 30));
        } else if (event.getType() == GUH_KLEERMAKER.get()) {
            // 2.9: the kleermaker only sells his own everyday set, and always all of it (a fixed full offer, see
            // kleermakerAanbod and feature.kleding.KledingKleermaker); he doesn't learn other trades when he levels up
            add(t, 1, kleermakerAanbod().toArray(VillagerTrades.ItemListing[]::new));
        } else if (event.getType() == VADSSMID.get()) {
            add(t, 1, buy(ModItems.VAHOEGE_VADS.get(), 2, 3, 12, 5), sell(ModItems.VAHOEGE_VADS_INGOT.get(), 1, 12, 6, 5));
            add(t, 2, sell(ModItems.VADS_SHOVEL.get(), 1, 18, 3, 10), sell(ModItems.VADS_HOE.get(), 1, 18, 3, 10));
            add(t, 3, sell(ModItems.VADS_PICKAXE.get(), 1, 36, 3, 15), sell(ModItems.VADS_AXE.get(), 1, 36, 3, 15),
                    sell(ModItems.VADS_BOOTS.get(), 1, 30, 3, 15));
            add(t, 4, sell(ModItems.VADS_SWORD.get(), 1, 30, 3, 20), sell(ModItems.VADS_HELMET.get(), 1, 36, 3, 20),
                    sell(ModItems.VADS_LEGGINGS.get(), 1, 48, 3, 20));
            add(t, 5, sell(ModItems.VADS_CHESTPLATE.get(), 1, 58, 2, 30), sell(ModItems.VADS_PAXEL.get(), 1, 64, 2, 30));
        } else if (event.getType() == HAMSTERBOUWER.get()) {
            add(t, 1, buy(Items.YELLOW_DYE, 12, 1, 16, 2), sell(Items.YELLOW_STAINED_GLASS, 8, 1, 16, 2));
            add(t, 2, sell(ModBlocks.GUH_WIRE.get(), 8, 3, 12, 10), buy(Items.REDSTONE, 12, 1, 16, 10));
            add(t, 3, sell(ModBlocks.GUH_WHEEL.get(), 1, 10, 4, 15), sell(Items.PINK_STAINED_GLASS, 8, 1, 16, 10));
            add(t, 4, sell(ModBlocks.GUH_WIRE.get(), 24, 7, 8, 20), sell(ModBlocks.FRYING_PAN.get(), 1, 8, 4, 20));
            add(t, 5, sell(ModBlocks.GUH_SPAWNER.get(), 1, 60, 1, 30));
        } else if (event.getType() == KNABBELBOER.get()) {
            add(t, 1, buy(ModItems.KAAS_KNABBELS.get(), 20, 1, 16, 2), sell(ModItems.KAASKNABBELZAADJES.get(), 4, 1, 16, 2));
            add(t, 2, sell(ModItems.GUH_CUPCAKE.get(), 3, 2, 12, 10), buy(Items.SUGAR, 16, 1, 16, 10),
                    sell(ModItems.MACARONS.get("roze").get(), 4, 2, 12, 10), sell(ModItems.MACARONS.get("mint").get(), 4, 2, 12, 10));
            add(t, 3, sell(ModItems.KAASKNABBEL_MILKSHAKE.get(), 1, 3, 8, 15), sell(ModItems.MACARONS.get("citroen").get(), 4, 2, 12, 15),
                    sell(ModItems.MACARONS.get("choco").get(), 4, 2, 12, 15));
            add(t, 4, sell(ModItems.KAASFONDUE.get(), 1, 5, 6, 20), sell(ModBlocks.KNABBELKORF.get(), 1, 8, 4, 20));
            add(t, 5, sell(ModItems.GUH_TAART.get(), 1, 12, 4, 30), sell(ModItems.KAASHONING.get(), 2, 6, 6, 30));
        } else if (event.getType() == MIKA_JAGER.get()) {
            add(t, 1, buy(ModItems.MIKA_VET.get(), 2, 1, 16, 2), sell(Items.ARROW, 16, 1, 12, 2));
            add(t, 2, sell(ModItems.MIKA_MEPPER.get(), 1, 12, 3, 10), sell(Items.SHIELD, 1, 5, 6, 10));
            add(t, 3, sell(ModItems.SUPERKOMPAS.get(), 1, 12, 3, 15), buy(ModItems.MIKA_VET.get(), 1, 1, 16, 10));
            add(t, 4, sell(Items.GOLDEN_APPLE, 1, 8, 4, 20));
            add(t, 5, sell(Items.TOTEM_OF_UNDYING, 1, 40, 1, 30));
        }
    }

    /** Guh villagers only take guh jobs: a guh villager that grabbed a vanilla workstation (and never traded) lets go of it. */
    public static void onVillagerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide || villager.tickCount % 40 != 0) {
            return;
        }
        VillagerData data = villager.getVillagerData();
        VillagerProfession profession = data.getProfession();
        if (data.getType() != GUH.get() || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT
                || isGuhProfession(profession) || villager.getVillagerXp() > 0) {
            return;
        }
        villager.setVillagerData(data.setProfession(VillagerProfession.NONE));
        villager.setOffers(null);
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
    }

    private ModVillagers() {
    }
}
