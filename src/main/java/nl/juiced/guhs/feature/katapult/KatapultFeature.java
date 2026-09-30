package nl.juiced.guhs.feature.katapult;

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
 * De Knabbelkatapult (2.9, De Grote Guhspelen): on the Vadskliffen a cheerful guh castle with a big catapult on its wall
 * looks across a gorge at a crooked Mika fort on a cliff ledge. Kapitein Floepguh (KATAPULTGUH) lends you pluisballen;
 * shoot the 12 Mika forts to bits and free the stolen kaasknabbels ({@link KatapultGame}, the forts in
 * {@link KatapultFort}, the balls {@link PluisbalEntity}, the falling pieces {@link KatapultBrokjeEntity}). Katapultsterren
 * buy his outfit: the katapulthelmpje (with a feather), the katapultriem and the pluisbal-oorbelletjes.
 * Resources: tools/features/katapult.py (+ katapult_bouw.py for the castle, katapult_forten.py for the 12 forts).
 */
public final class KatapultFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final ResourceKey<Structure> KNABBELKATAPULT = ResourceKey.create(Registries.STRUCTURE, Guhs.id("knabbelkatapult"));

    private static BlockBehaviour.Properties castleOnly(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    /** The catapult's bucket (FACING = where it shoots). */
    public static final DeferredBlock<KatapultBlocks.Gericht> WERPER = BLOCKS.registerBlock("katapult_werper", KatapultBlocks::werper,
            () -> castleOnly(Blocks.SPRUCE_PLANKS).noOcclusion());
    /** The foundation of the Mika fort (FACING = towards the catapult). */
    public static final DeferredBlock<KatapultBlocks.Gericht> FORTPLEK = BLOCKS.registerBlock("katapult_fortplek", KatapultBlocks.Gericht::new,
            () -> castleOnly(Blocks.POLISHED_BLACKSTONE));
    /** A Mika figure in a fort. */
    public static final DeferredBlock<KatapultBlocks.Gericht> MIKA = BLOCKS.registerBlock("katapult_mika", KatapultBlocks::mika,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).strength(-1f, 3600000f).noLootTable().noOcclusion());
    /** A crate of stolen kaasknabbels. */
    public static final DeferredBlock<KatapultBlocks.Gericht> KNABBELKIST = BLOCKS.registerBlock("katapult_knabbelkist", KatapultBlocks::kist,
            () -> castleOnly(Blocks.BARREL).noOcclusion());
    /** The Mika's crooked building planks (a nice purple wood for building). */
    public static final DeferredBlock<Block> MIKAPLANK = BLOCKS.registerSimpleBlock("katapult_mikaplank", () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS));

    public static final DeferredItem<BlockItem> MIKAPLANK_ITEM = ITEMS.registerSimpleBlockItem(MIKAPLANK);
    /** The katapult currency: stars for a run of forts, spent in the Kapitein's shop. */
    public static final DeferredItem<Item> KATAPULTSTER = ITEMS.registerSimpleItem("katapultster", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    /** The loaned pluisballen (only while you play). */
    public static final DeferredItem<PluisballenItem> PLUISBALLEN = ITEMS.registerItem("katapult_pluisballen", PluisballenItem::new,
            () -> new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<EntityType<?>, EntityType<PluisbalEntity>> PLUISBAL = ENTITIES.register("pluisbal",
            () -> EntityType.Builder.<PluisbalEntity>of(PluisbalEntity::new, MobCategory.MISC)
                    .sized(PluisbalEntity.SIZE, PluisbalEntity.SIZE).clientTrackingRange(10).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("pluisbal"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KatapultBrokjeEntity>> BROKJE = ENTITIES.register("katapult_brokje",
            () -> EntityType.Builder.<KatapultBrokjeEntity>of(KatapultBrokjeEntity::new, MobCategory.MISC)
                    .sized(0.98f, 0.98f).clientTrackingRange(10).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("katapult_brokje"))));

    /** The Kapitein's outfit and its price in katapultsterren (a run earns 1-6, lastig more). */
    public static final int PRIJS_HELMPJE = 4, PRIJS_OORBELLETJES = 6, PRIJS_RIEM = 10;
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.KATAPULT_HELMPJE, GuhClothes.KATAPULT_RIEM, GuhClothes.KATAPULT_OORBELLETJES);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        Minigames.registerGame(Minigames.KATAPULT, KatapultGame::isPlaying);
        NeoForge.EVENT_BUS.register(KatapultEvents.class);
        NeoForge.EVENT_BUS.register(KatapultProtection.class);
        Protected.add(KatapultProtection::inKasteel);
        KledingBronnen.bron(GuhClothes.KATAPULT_HELMPJE, "katapult", PRIJS_HELMPJE + " katapultsterren");
        KledingBronnen.bron(GuhClothes.KATAPULT_OORBELLETJES, "katapult", PRIJS_OORBELLETJES + " katapultsterren");
        KledingBronnen.bron(GuhClothes.KATAPULT_RIEM, "katapult", PRIJS_RIEM + " katapultsterren");
    }

    public static void payloads(PayloadRegistrar registrar) {
        KatapultPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KATAPULTSTER.get()));
        output.accept(new ItemStack(MIKAPLANK_ITEM.get()));
    }

    /** Kapitein Floepguh's role. */
    @Nullable
    public static NpcRole role() {
        return KatapultRole.INSTANCE;
    }

    /** Grants quest/&lt;name&gt; and grote_guhspelen/&lt;name&gt; (all criteria), whichever exist. */
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

    public static boolean has(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The whole katapult outfit (unlocked, or all three in your pockets): the advancement. */
    public static void checkKleding(ServerPlayer player) {
        boolean all = true;
        for (GuhClothes c : KLEDING) {
            all &= KledingUnlocks.heeft(player, c) || player.getInventory().countItem(ModItems.clothingItem(c)) > 0;
        }
        if (all) {
            advancement(player, "katapult_kleding");
        }
    }

    private KatapultFeature() {
    }
}
