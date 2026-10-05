package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhpixel.blok.MuurDecoBlock;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * Guhpixel slice "reisbureau": Reisbureau "De Vadsvakantie" (DESIGN_PX section 5; independent of the guhpixel dimension).
 * Send ONE of your guhs on a trip of 1, 2, 8 or 24 real hours ({@link Reizen}); it comes back with an ansichtkaart
 * ({@link KaartItem}) and a souvenir ({@link Souvenirs}: 16 common + 16 rare decoration blocks). This class owns the
 * registers: the Reisbalie ({@link BalieBlock}), the Reisstempel, seventeen ansichtkaarten, the souvenirs, the Gouden
 * koffertje and the Koffertje, the sounds; and wires the Reisagent-guh ({@link Reisagent}), the walk-off
 * ({@link Uitzwaaien}), the sunglasses ({@link Zonnebril}), the Guhdex section ({@link ReisGids}) and the dev commands
 * ({@link ReisCommando}). Resources: tools/features/guhpixel_reisbureau*.py.
 */
public final class ReisbureauSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredBlock<BalieBlock> BALIE = BLOCKS.registerBlock("reisbureau_balie", BalieBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f, 3600000.0f).noOcclusion().sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<LoreBlockItem> BALIE_ITEM = ITEMS.registerItem("reisbureau_balie", p -> new LoreBlockItem(BALIE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    /** From the Reisagent-guh after the proefreisje; the Reisbalie's recipe uses one. */
    public static final DeferredItem<Item> STEMPEL = ITEMS.registerItem("reisbureau_stempel", GuhpixelFeature.LoreItem::new,
            () -> new Item.Properties().stacksTo(16));

    /** The souvenirs and the two koffertjes by their short id ("souvenir_kaasmarkt", "zeldzaam_nomguh", "gouden_koffertje"...). */
    public static final Map<String, DeferredBlock<DecoBlock>> SOUVENIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<LoreBlockItem>> SOUVENIR_ITEMS = new LinkedHashMap<>();
    /** One ansichtkaart per destination (and one of the proefreisje). */
    public static final Map<Bestemming, DeferredItem<KaartItem>> KAARTEN = new EnumMap<>(Bestemming.class);

    static {
        for (Souvenirs.Soort s : Souvenirs.ALLE) {
            String id = "reisbureau_" + s.id();
            DeferredBlock<DecoBlock> blok = BLOCKS.registerBlock(id,
                    p -> s.muur() ? new MuurDecoBlock(p, s.vorm()) : new SouvenirBlock(p, s.vorm(), s.effect()),
                    () -> (s.muur() ? MuurDecoBlock.props() : DecoBlock.props()).lightLevel(state -> s.licht()));
            SOUVENIRS.put(s.id(), blok);
            SOUVENIR_ITEMS.put(s.id(), ITEMS.registerItem(id, p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
        for (Bestemming b : Bestemming.values()) {
            KAARTEN.put(b, ITEMS.registerItem("reisbureau_kaart_" + b.id(), p -> new KaartItem(b, p), () -> new Item.Properties().stacksTo(1)));
        }
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_VERTREK = geluid("reisbureau.vertrek");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_TERUG = geluid("reisbureau.terug");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_STEMPEL = geluid("reisbureau.stempel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELUID_BALIE = geluid("reisbureau.balie");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** A souvenir or koffertje block by its short id. */
    public static Block blok(String id) {
        return SOUVENIRS.get(id).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.REISBUREAU_AGENT, Reisagent.ROL);
        GuhHooks.tick(Uitzwaaien::tick);
        GuhHooks.tick(Zonnebril::tick);
        GuhHooks.tick(Reizen::tickGuh);
        GidsBlad.registreer(new ReisGids());
        NeoForge.EVENT_BUS.addListener(ReisCommando::register);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> Reizen.serverTick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Reizen.login(p);
            }
        });
        PxZelftest.registreer("reisbureau", ReisbureauSlice::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
        ReisbureauPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BALIE_ITEM.get()));
        output.accept(new ItemStack(STEMPEL.get()));
        SOUVENIR_ITEMS.values().forEach(i -> output.accept(new ItemStack(i.get())));
        KAARTEN.values().forEach(i -> output.accept(new ItemStack(i.get())));
    }

    /** (the dev server's /guhs px zelftest) the things that need a real server: the template, the offer, the registry. */
    private static void zelftest(net.minecraft.server.MinecraftServer server, ServerLevel level, PxZelftest.Melder meld) {
        meld.check(server.getStructureManager().get(Guhs.id("reisbureau")).isPresent(), "the template guhs:reisbureau loads");
        var aanbod = Reizen.aanbod(server);
        boolean vier = aanbod.size() == 4;
        for (int i = 0; vier && i < 4; i++) {
            vier = aanbod.get(i).minuten() == Bestemming.DUREN[i];
        }
        meld.check(vier, "today's offer: four trips, one per duration: " + aanbod);
        meld.check(SOUVENIRS.size() == 34 && KAARTEN.size() == 17, "32 souvenirs + 2 koffertjes, 17 ansichtkaarten");
        var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        meld.check(sets.get(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id("reisbureau"))).isPresent()
                && sets.get(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, Guhs.id("reisbureau_gegarandeerd"))).isPresent(),
                "the structure sets reisbureau and reisbureau_gegarandeerd exist");
    }

    private ReisbureauSlice() {
    }
}
