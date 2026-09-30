package nl.juiced.guhs.feature.sjoelen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.resources.Identifier;
/**
 * Guh-sjoelen (2.9, De Grote Guhspelen): the Sjoelhuisje in the Guhweides, a wooden house with a giant sjoelbak as its
 * roof, round puck windows and a guh face at the front. Inside, Opoe Njegschuif (SJOELGUH) lets you slide 20 real
 * sjoelschijven over a huge sjoelbak into the gates 2-3-4-1 ({@link SjoelGame}, physics in {@link SjoelBak}).
 * Sjoelschijfjes buy her knitted sjoel outfit: the sjoelpetje, the sjoelvestje and the sjoelbroche.
 * Resources: tools/features/sjoelen.py (+ sjoelen_bouw.py for the building).
 */
public final class SjoelenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final ResourceKey<Structure> SJOELHUISJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("sjoelhuisje"));

    private static BlockBehaviour.Properties bakOnly(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    /** The waxed sjoelbak wood (also a nice building block). */
    public static final DeferredBlock<Block> BAKPLANK = BLOCKS.registerSimpleBlock("sjoelen_bakplank", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS));
    /** The head of the sjoelbak: you slide from here (Opoe finds the bak by it). */
    public static final DeferredBlock<SjoelenBlocks.Deel> KOP = BLOCKS.registerBlock("sjoelen_kop", SjoelenBlocks.Deel::new, () -> bakOnly(Blocks.BIRCH_PLANKS));
    /** The gate bar with its four openings 2-3-4-1. */
    public static final DeferredBlock<SjoelenBlocks.Deel> POORT = BLOCKS.registerBlock("sjoelen_poort", SjoelenBlocks.Deel::new,
            () -> bakOnly(Blocks.BIRCH_PLANKS).noOcclusion());
    /** The lanes behind the gates, with their dividers. */
    public static final DeferredBlock<SjoelenBlocks.Deel> VAK = BLOCKS.registerBlock("sjoelen_vak", SjoelenBlocks.Deel::new,
            () -> bakOnly(Blocks.BIRCH_PLANKS).noOcclusion());
    /** A stack of sjoelschijven (decoration). */
    public static final DeferredBlock<SjoelenBlocks.Stapel> STAPEL = BLOCKS.registerBlock("sjoelen_stapel", SjoelenBlocks.Stapel::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion());

    public static final DeferredItem<BlockItem> BAKPLANK_ITEM = ITEMS.registerSimpleBlockItem(BAKPLANK);
    public static final DeferredItem<BlockItem> STAPEL_ITEM = ITEMS.registerSimpleBlockItem(STAPEL);
    /** The sjoel currency: earned per turn, spent in Opoe's shop. */
    public static final DeferredItem<Item> SJOELSCHIJFJE = ITEMS.registerSimpleItem("sjoelschijfje", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    /** The 20 loaned pucks (only while you play). */
    public static final DeferredItem<SjoelSchijvenItem> SCHIJVEN = ITEMS.registerItem("sjoelen_schijven", SjoelSchijvenItem::new,
            () -> new Item.Properties().stacksTo(SjoelGame.PUCKS).rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<EntityType<?>, EntityType<SjoelSchijfEntity>> SCHIJF = ENTITIES.register("sjoelschijf",
            () -> EntityType.Builder.<SjoelSchijfEntity>of(SjoelSchijfEntity::new, MobCategory.MISC)
                    .sized(SjoelSchijfEntity.SIZE, SjoelSchijfEntity.HEIGHT).clientTrackingRange(6).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("sjoelschijf"))));

    /** Opoe's knitted sjoel outfit and its price in sjoelschijfjes (a good turn earns 4-7). */
    public static final int PRIJS_PETJE = 4, PRIJS_BROCHE = 6, PRIJS_VESTJE = 10;
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.SJOELEN_PETJE, GuhClothes.SJOELEN_VESTJE, GuhClothes.SJOELEN_BROCHE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        Minigames.registerGame(Minigames.SJOELEN, SjoelGame::isPlaying);
        NeoForge.EVENT_BUS.register(SjoelenEvents.class);
        NeoForge.EVENT_BUS.register(SjoelenProtection.class);
        Protected.add(SjoelenProtection::inHuisje);
        KledingBronnen.bron(GuhClothes.SJOELEN_PETJE, "sjoelen", PRIJS_PETJE + " sjoelschijfjes");
        KledingBronnen.bron(GuhClothes.SJOELEN_BROCHE, "sjoelen", PRIJS_BROCHE + " sjoelschijfjes");
        KledingBronnen.bron(GuhClothes.SJOELEN_VESTJE, "sjoelen", PRIJS_VESTJE + " sjoelschijfjes");
    }

    public static void payloads(PayloadRegistrar registrar) {
        SjoelenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SJOELSCHIJFJE.get()));
        output.accept(new ItemStack(BAKPLANK_ITEM.get()));
        output.accept(new ItemStack(STAPEL_ITEM.get()));
    }

    /** Opoe Njegschuif's role. */
    @Nullable
    public static NpcRole role() {
        return SjoelenRole.INSTANCE;
    }

    /**
     * Grants an advancement of this game: the hidden quest/&lt;name&gt; (for FTB) and the shown grote_guhspelen/&lt;name&gt;
     * (all its criteria), whichever exist.
     */
    public static void advancement(ServerPlayer player, String name) {
        for (String path : new String[] {"quest/" + name, "grote_guhspelen/" + name}) {
            AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(path));
            if (holder == null) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            if (!progress.isDone()) {
                List<String> remaining = new ArrayList<>();
                progress.getRemainingCriteria().forEach(remaining::add);
                for (String criterion : remaining) {
                    player.getAdvancements().award(holder, criterion);
                }
            }
        }
    }

    /** Has this player got the advancement (quest/&lt;name&gt;)? */
    public static boolean has(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The whole sjoel outfit (unlocked, or all three in your pockets): the advancement. */
    public static void checkKleding(ServerPlayer player) {
        boolean all = true;
        for (GuhClothes c : KLEDING) {
            all &= KledingUnlocks.heeft(player, c) || player.getInventory().countItem(ModItems.clothingItem(c)) > 0;
        }
        if (all) {
            advancement(player, "sjoelen_kleding");
        }
    }

    private SjoelenFeature() {
    }
}
