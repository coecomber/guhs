package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterCommando;

/**
 * Guhwinter 1.10.10, slice TEKENEN (package {@code feature.guhwinter.tekenen}; ids {@code guhwinter_*}, lang {@code guhwinter.tekenen.*},
 * generator tools/features/guhwinter_tekenen.py, gametests {@code TekenenGameTests}):
 * <ul>
 *   <li>the 64x64 drawing editor (client: TekenScherm) and the storage + sync of drawings ({@link Tekeningen}, {@link TekeningenData});</li>
 *   <li>the hangable 2x2 drawing ({@link TekeningItem} {@code guhwinter_tekening}, {@link TekeningEntity});</li>
 *   <li>the kerstboom drawings ({@link KerstboomTekeningen}, the floating {@link BoomKaartje}s) and the {@link Kerstalbum}.</li>
 * </ul>
 * Registered from {@code GuhwinterFeature} (register + payloads) and {@code GuhwinterClient}. Time comes from kern ({@link Guhwinter}:
 * {@code kerstboomOpen}, {@code albumOpen}, {@code bezig}; dev date = {@code /guhs winter datum}); the tree from {@link TekenPlek}
 * (plein's ankers.json). Dev: {@code /guhs winter tekenen kerstboom|lootje|album|lijst|item <id>|kaartjes <n>}.
 */
public final class TekenenFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TekeningRef>> TEKENING_REF =
            COMPONENTS.registerComponentType("guhwinter_tekening", b -> b.persistent(TekeningRef.CODEC).networkSynchronized(TekeningRef.STREAM_CODEC));

    public static final DeferredItem<TekeningItem> TEKENING = ITEMS.registerItem("guhwinter_tekening", TekeningItem::new,
            () -> new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Kerstalbum.AlbumItem> KERSTALBUM = ITEMS.registerItem("guhwinter_kerstalbum", Kerstalbum.AlbumItem::new,
            () -> new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredHolder<EntityType<?>, EntityType<TekeningEntity>> TEKENING_ENTITY = ENTITIES.register("guhwinter_tekening",
            () -> EntityType.Builder.<TekeningEntity>of(TekeningEntity::new, MobCategory.MISC).noLootTable().sized(0.5f, 0.5f)
                    .clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwinter_tekening"))));
    public static final DeferredHolder<EntityType<?>, EntityType<BoomKaartje>> BOOMKAARTJE = ENTITIES.register("guhwinter_boomkaartje",
            () -> EntityType.Builder.<BoomKaartje>of(BoomKaartje::new, MobCategory.MISC).noLootTable().noSave().noSummon().sized(0.6f, 0.6f)
                    .clientTrackingRange(6).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwinter_boomkaartje"))));

    private TekenenFeature() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        KerstboomTekeningen.init();
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                Kerstalbum.geefAlsNodig(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Tekeningen.uitgelogd(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            Tekeningen.wis();
            KerstboomTekeningen.weg();
            TekenPlek.vergeet();
        });
        // HIGH: the tree click wins before anything else on the (protected) plaza looks at the right-click
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, KerstboomTekeningen::klik);
        nl.juiced.guhs.world.TikWacht.post("TekenenFeature", e -> KerstboomTekeningen.tick(e.getServer()));
        Guhwinter.opMoment("album:tekenen", Kerstalbum::opAlbumMoment);
        GuhwinterCommando.voegToe(ctx -> commandos());
    }

    /** (GuhwinterFeature.creative) */
    public static void creative(java.util.function.Consumer<ItemStack> output) {
        output.accept(new ItemStack(KERSTALBUM.get()));
    }

    /** (GuhwinterFeature.payloads) */
    public static void payloads(PayloadRegistrar registrar) {
        TekenPayloads.register(registrar);
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> commandos() {
        LiteralArgumentBuilder<CommandSourceStack> t = GuhwinterCommando.dev("tekenen");
        t.then(Commands.literal("kerstboom").executes(c -> {
            KerstboomTekeningen.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        t.then(Commands.literal("lootje").executes(c -> {
            Tekeningen.openEditor(c.getSource().getPlayerOrException(), Doel.LOOTJE, null,
                    tk -> c.getSource().sendSuccess(() -> Component.literal("Lootje-tekening " + tk.id() + " (v" + tk.versie() + ")"), false));
            return 1;
        }));
        t.then(Commands.literal("album").executes(c -> {
            Kerstalbum.geef(c.getSource().getPlayerOrException());
            return 1;
        }));
        t.then(Commands.literal("lijst").executes(c -> {
            TekeningenData d = TekeningenData.get(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal(d.aantal() + " tekeningen, " + d.kerstboomTekeningen().size() + " in de kerstboom; tijd "
                    + Guhwinter.nu(c.getSource().getServer()).format(Guhwinter.DATUM) + (Guhwinter.nepDatum(c.getSource().getServer()) ? " (dev)" : "")), false);
            for (Tekening tk : d.alle()) {
                c.getSource().sendSuccess(() -> Component.literal(" - " + tk.doel().id() + " " + tk.makerNaam() + " v" + tk.versie() + " " + tk.id()), false);
            }
            return d.aantal();
        }));
        t.then(Commands.literal("item").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
            UUID id;
            try {
                id = UUID.fromString(StringArgumentType.getString(c, "id"));
            } catch (IllegalArgumentException ex) {
                c.getSource().sendFailure(Component.literal("Geen geldige UUID"));
                return 0;
            }
            Tekeningen.geef(c.getSource().getPlayerOrException(), Tekeningen.maakItem(id));
            return 1;
        })));
        t.then(Commands.literal("kaartjes").then(Commands.argument("aantal", IntegerArgumentType.integer(0, 24)).executes(c -> {
            KerstboomTekeningen.KAARTJES_MAX = IntegerArgumentType.getInteger(c, "aantal");
            KerstboomTekeningen.weg();
            return 1;
        })));
        return t;
    }
}
