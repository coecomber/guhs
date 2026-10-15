package nl.juiced.guhs.feature.ringh6;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;

/**
 * bbq2 (ring-h6): chapter 6 of the Knabbelring: De Frituurberg (DESIGN_130 4). Resources: tools/features/ring_h6.py
 * (+ ring_h6_bouw.py: the mountain; ring_h6_tekst.py: the texts).
 * <ul>
 *   <li>The structure {@code guhs:frituurberg}: a volcano in the Rookdelta with a crust of fried batter, the last of the
 *       story chain (one per world). Hidden and protected by Guhdalfs sluier until the player's story gets there
 *       ({@link Ring#sluier}); where its spots are: {@link Berg}.</li>
 *   <li>The climb ({@link Klim}): the Kronkelpad with falling coals ({@link Kolen}, {@link ValkoolEntity}: a shove, never
 *       damage), three caged Rookguhjes to free ({@link KooislotBlock}, {@link GekooideRookguhEntity}), the Elfentouw up the
 *       west face, the ring that gets heavier with every block, Smikagol who grabs at it, and Sam-guh who carries you the
 *       last stretch.</li>
 *   <li>The end ({@link Finale}, {@link Thuis}): Smikagol takes the ring, falls into the frituur and comes out golden brown
 *       with a fried ring that everybody shares, the Eye gets a piece and naps, the Rookguhs fly you home, Araguh is
 *       crowned at the feast in the Gouw. Finishing {@link #LIJN} makes ring-kern hand out the rewards of the story.</li>
 * </ul>
 * Everything is per player; nothing in the world is used up; nothing here ever hurts a player.
 */
public final class RingH6Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);
    /**
     * 1.4.1: loads the chunks of a player's home in the Guhmensie in the background while the Rookguhs fly them there
     * ({@link Thuis#warm}; asked again every second of the flight, gone 20 seconds after the last time; not saved).
     */
    public static final DeferredHolder<TicketType, TicketType> THUIS_TICKET = TICKETS.register("ringh6_thuis",
            () -> new TicketType(400L, TicketType.FLAG_LOADING | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED));

    /** The lock of a Rookguh cage (a quest prop: it can't be broken, moved or blown up). */
    public static final DeferredBlock<KooislotBlock> KOOISLOT = BLOCKS.registerBlock("ringh6_kooislot", KooislotBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f).sound(SoundType.CHAIN).noLootTable()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> KOOISLOT_ITEM = ITEMS.registerSimpleBlockItem(KOOISLOT);
    /** Everybody's own piece of the fried Knabbelring: a keepsake you may also eat. */
    public static final DeferredItem<Stukje> STUKJE = ITEMS.registerItem("ringh6_stukje_knabbelring", Stukje::new,
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).fireResistant()
                    .food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.9f).alwaysEdible().build()));

    public static final DeferredHolder<EntityType<?>, EntityType<GekooideRookguhEntity>> ROOKGUH = ENTITY_TYPES.register("ringh6_rookguh",
            () -> EntityType.Builder.<GekooideRookguhEntity>of(GekooideRookguhEntity::new, MobCategory.MISC).sized(1.3f, 1.3f).eyeHeight(0.9f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringh6_rookguh"))));
    public static final DeferredHolder<EntityType<?>, EntityType<ValkoolEntity>> VALKOOL = ENTITY_TYPES.register("ringh6_valkool",
            () -> EntityType.Builder.<ValkoolEntity>of(ValkoolEntity::new, MobCategory.MISC).sized(0.7f, 0.7f).fireImmune().noSave().noSummon()
                    .clientTrackingRange(6).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringh6_valkool"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KrokanteSmikagolEntity>> KROKANTE_SMIKAGOL = ENTITY_TYPES.register(
            "ringh6_krokante_smikagol", () -> EntityType.Builder.<KrokanteSmikagolEntity>of(KrokanteSmikagolEntity::new, MobCategory.MISC)
                    .sized(0.7f, 0.75f).eyeHeight(0.5f).fireImmune().noSave().clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringh6_krokante_smikagol"))));

    /**
     * The questline of the chapter (texts: tools/features/ring_h6.py; the steps: {@link Klim}). ring-kern refers to this
     * field ({@code Ring.lijn(6)}): finishing it is the end of the story.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h6", "knabbelring").stappen(7).na("ring_h5").icoon("guhs:gloeiend_kooltje")
            .nodig(RingH6Feature::nodig).beloningen(RingH6Feature::beloningen)
            .doel((p, stap) -> stap >= 6 ? Thuis.doel(p) : Ring.doel(6))
            .registreer();

    private static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        List<VerhaalStand.Nodig> lijst = new ArrayList<>();
        if (stap <= 5 && !LIJN.vlag(p, Finale.GEFRITUURD)) {
            lijst.add(Verhaallijn.nodig("guhs:knabbelring", Ring.heeft(p) ? 1 : 0, 1));
        }
        if (stap == 2 || stap == 3) {
            lijst.add(Verhaallijn.nodig("guhs:elfentouw", Gaven.heeft(p, RingFeature.ELFENTOUW.get()) ? 1 : 0, 1));
        }
        return lijst;
    }

    private static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        boolean klaar = LIJN.klaar(p);
        return List.of(Verhaallijn.beloning("guhs:ringh6_stukje_knabbelring", klaar || LIJN.vlag(p, Finale.GEFRITUURD)),
                Verhaallijn.beloning("guhs:ring_vissenbotje", klaar), Verhaallijn.beloning("guhs:elfentouw_haak", klaar));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        TICKETS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(ROOKGUH.get(), RookguhEntity.createAttributes().build());
            event.put(KROKANTE_SMIKAGOL.get(), SmikagolEntity.createAttributes().build());
        });
        NeoForge.EVENT_BUS.register(RingH6Events.class);
        NeoForge.EVENT_BUS.addListener(RingH6Commands::register);

        // the mountain: hidden and whole until the story gets there (no rim: you bridge or dig up to the box and step in)
        Ring.sluier(Berg.STRUCTUUR, 0, 6);
        // a Rookguhje in every cage of every copy, and the same three living free over their old cages for whoever is done
        // (they also come with the template: this only repairs)
        Berg.Gegevens berg = Berg.echt();
        for (int nr = 1; nr <= 3; nr++) {
            int kooi = nr;
            Bezetting.wezen("ringh6_rookguh_" + nr, Berg.STRUCTUUR, null, berg.plek("kooi_" + nr),
                    (level, plek, draai) -> GekooideRookguhEntity.maak(level, plek, kooi), 6);
            Bezetting.wezen("ringh6_vrije_rookguh_" + nr, Berg.STRUCTUUR, null, berg.plek("thuis_" + nr),
                    (level, plek, draai) -> GekooideRookguhEntity.thuis(level, plek, kooi), 6);
        }
        // the story
        Verteller.registreer(Klim.KAART, 4, LIJN.id());
        Finale.registreer();
        Sam.BIJ_KLIK.add(Klim::samKlik);
        Gaven.BIJ_LICHT.add(Klim::licht);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STUKJE.get()));
        output.accept(new ItemStack(KOOISLOT_ITEM.get()));
    }

    /** The piece of fried Knabbelring: food with a line of lore. */
    public static class Stukje extends Item {
        public Stukje(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private RingH6Feature() {
    }
}
