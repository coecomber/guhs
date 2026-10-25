package nl.juiced.guhs.feature.ringvlucht;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ringh1.Gouw;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2 (rookguh): de Rookguh-vlucht of "In de ban van de Knabbelring". The structures of the story lie far apart; once a
 * player finished the story part of one, a Reis-Rookguh hovers at its exit ({@link VluchtRookguhEntity}) and flies them to any
 * place of the story they reached ({@link Vlucht}, {@link Haltes}, {@link Landing}, {@link VluchtScenes}). Resources, texts,
 * spots and their check: tools/features/ring_vlucht.py. Dev commands: {@link RingVluchtCommands}.
 * <p>
 * The Rookguhs come through {@code Bezetting}: one per copy of every chapter structure (and at Guhdalf's camp in every
 * Knabbelgouw and at every old big pit), made the first time a player comes near, so the copies generated before 1.4.2 have
 * one too. Each only exists for players whose story part of that place is done.
 */
public final class RingVluchtFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** 1.7.0: the Rookguh-platform, your own landing place for the Rookguh ({@link RookPlatform}). */
    public static final net.neoforged.neoforge.registries.DeferredBlock<RookPlatformBlok> PLATFORM = BLOCKS.registerBlock("rookguh_platform",
            RookPlatformBlok::new, () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.COLOR_PINK).strength(1.5f, 6f)
                    .sound(net.minecraft.world.level.block.SoundType.STONE).lightLevel(s -> 4));
    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> PLATFORM_ITEM = ITEMS.registerItem(
            "rookguh_platform", p -> new nl.juiced.guhs.feature.balto.BaltoBlocks.LoreBlock(PLATFORM.get(), p),
            () -> new net.minecraft.world.item.Item.Properties().useBlockDescriptionPrefix().rarity(net.minecraft.world.item.Rarity.RARE));

    public static final DeferredHolder<EntityType<?>, EntityType<VluchtRookguhEntity>> ROOKGUH = ENTITY_TYPES.register("ringvlucht_rookguh",
            () -> EntityType.Builder.of(VluchtRookguhEntity::new, MobCategory.MISC).sized(1.6f, 1.8f).eyeHeight(1.3f).fireImmune()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringvlucht_rookguh"))));
    /** The chunks of a destination, asked for while the flight scene plays (gone 20 s after; not saved). */
    public static final DeferredHolder<TicketType, TicketType> TICKET = TICKETS.register("ringvlucht_landing",
            () -> new TicketType(400L, TicketType.FLAG_LOADING | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED));

    /** The Bezetting ids of the Rookguhs: ringvlucht_&lt;stop&gt;, and the two of the Gouw. */
    public static final String GOUW_ID = "ringvlucht_gouw", KAMP_ID = "ringvlucht_gouw_kamp", GUHRIO_ID = "ringvlucht_guhrio";

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        TICKETS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        RookPlatform.registreer();
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(ROOKGUH.get(), RookguhEntity.createAttributes().build()));
        NeoForge.EVENT_BUS.register(RingVluchtEvents.class);
        NeoForge.EVENT_BUS.addListener(RingVluchtCommands::register);
        VluchtScenes.registreer();
        bewoners();
        aankondiging();
    }

    /**
     * The moment a stop's story part is done its Rookguh is there: the player is told so in the chat (the Gouw's at the end
     * of chapter 1; not at the end of chapter 6: the Rookguhs just flew them home).
     */
    private static void aankondiging() {
        java.util.Set<String> gedaan = new java.util.HashSet<>();
        for (Haltes.Halte h : Haltes.ALLE) {
            if (h.rookguh() == null && !h.id().equals(Haltes.GOUW) || h.hoofdstuk() == 6 || !gedaan.add(h.lijn())) {
                continue;
            }
            String key = h.id().equals(Haltes.GOUW) ? "quest.guhs.ringvlucht.er_is_er_een.gouw" : "quest.guhs.ringvlucht.er_is_er_een";
            h.verhaallijn().opStap((p, oud, nieuw) -> {
                if (oud < nieuw && nieuw >= h.verhaallijn().stappen()) {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.translatable(key).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                }
            });
        }
    }

    /** A Rookguh at the exit of every copy of every stop that has one, and at Guhdalf's camps (the Gouw). */
    private static void bewoners() {
        for (Haltes.Halte h : Haltes.ALLE) {
            if (h.rookguh() == null) {
                continue;
            }
            String id = "ringvlucht_" + h.id();
            Bezetting.wezen(id, h.structuur(), null, h.rookguh(), (level, plek, draai) -> VluchtRookguhEntity.maak(level, plek, h, 0f));
            Bezetting.alleenIn(id, BarbecuetherFeature.BARBECUETHER);
        }
        Haltes.Halte gouw = Haltes.van(Haltes.GOUW);
        if (gouw == null) {
            throw new IllegalStateException("ringvlucht: no stop " + Haltes.GOUW);
        }
        // the Knabbelgouw: its camp stands unturned in its template
        Bezetting.wezen(GOUW_ID, Gouw.STRUCTUUR, null, Gouw.GOUW_KAMP.offset(Haltes.KAMP_ROOKGUH),
                (level, plek, draai) -> VluchtRookguhEntity.maak(level, plek, gouw, 0f));
        // an old big pit: the camp that ring-h1 put down next to it (looked for around the pit's middle, as Guhdalf is)
        Bezetting.wezen(KAMP_ID, Gouw.PUT, Gouw.PUT_STUK, Gouw.PUT_MIDDEN, (level, plek, draai) -> {
            Gouw.Plek kamp = Gouw.kampBijPut(level, BlockPos.containing(plek));
            return kamp == null ? null : VluchtRookguhEntity.maak(level, Vec3.atBottomCenterOf(kamp.wereld(Haltes.KAMP_ROOKGUH)), gouw, 0f);
        }, Gouw.KAMP_ZOEK);
        Bezetting.alleenIn(GOUW_ID, ModDimensions.GUHMENSION);
        Bezetting.alleenIn(KAMP_ID, ModDimensions.GUHMENSION);
        // 1.7.1: the Super Guhrio castle has its own Reis-Rookguh on the forecourt (there for everyone)
        Bezetting.wezen(GUHRIO_ID, nl.juiced.guhs.feature.guhrio.GuhrioKasteel.STRUCTUUR, null, RookPlatform.GUHRIO_ROOKGUH,
                (level, plek, draai) -> VluchtRookguhEntity.maakExtra(level, plek, RookPlatform.GUHRIO, null));
        Bezetting.alleenIn(GUHRIO_ID, BarbecuetherFeature.BARBECUETHER);
    }

    public static void payloads(PayloadRegistrar registrar) {
        VluchtPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PLATFORM_ITEM.get()));
    }

    private RingVluchtFeature() {
    }
}
