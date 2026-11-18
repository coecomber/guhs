package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModCreativeTabs;

/**
 * Guhwinter 1.10.9, slice TEKENEN (package {@code feature.guhwinter.tekenen}; ids {@code guhwinter_*}, lang {@code guhwinter.tekenen.*},
 * generator tools/features/guhwinter_tekenen.py, gametests {@code TekenenGameTests}):
 * <ul>
 *   <li>the 64x64 drawing editor (client: TekenScherm) and the storage + sync of drawings ({@link Tekeningen}, {@link TekeningenData});</li>
 *   <li>the hangable 2x2 drawing ({@link TekeningItem} {@code guhwinter_tekening}, {@link TekeningEntity});</li>
 *   <li>the kerstboom drawings ({@link KerstboomTekeningen}, the floating {@link BoomKaartje}s) and the {@link Kerstalbum}.</li>
 * </ul>
 * Time comes from {@link TekenTijd} and the tree from {@link TekenPlek}: both local adapters until integration wires them to kern
 * and plein. Dev: {@code /guhs winter tekenen kerstboom|lootje|album|lijst|item <id>|tijd <yyyy-MM-dd HH:mm>|echt}.
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
        modBus.addListener((RegisterPayloadHandlersEvent e) -> TekenPayloads.register(e.registrar("1")));
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == ModCreativeTabs.GUHS_TAB.getKey()) {
                e.accept(new ItemStack(KERSTALBUM.get()));
            }
        });
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
        NeoForge.EVENT_BUS.addListener(KerstboomTekeningen::klik);
        nl.juiced.guhs.world.TikWacht.post("TekenenFeature", e -> {
            KerstboomTekeningen.tick(e.getServer());
            Kerstalbum.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(TekenenFeature::commandos);
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> t = Commands.literal("tekenen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
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
                    + TekenTijd.nu(c.getSource().getServer()).format(DATUM) + (TekenTijd.dev() ? " (dev)" : "")), false);
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
        t.then(Commands.literal("tijd").then(Commands.literal("echt").executes(c -> {
            TekenTijd.zetDev(null);
            c.getSource().sendSuccess(() -> Component.literal("Tekenen: echte tijd"), true);
            return 1;
        })).then(Commands.argument("moment", StringArgumentType.greedyString()).executes(c -> {
            try {
                TekenTijd.zetDev(LocalDateTime.parse(StringArgumentType.getString(c, "moment"), DATUM));
            } catch (DateTimeParseException ex) {
                c.getSource().sendFailure(Component.literal("Formaat: yyyy-MM-dd HH:mm"));
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.literal("Tekenen: tijd is nu " + TekenTijd.nu(c.getSource().getServer()).format(DATUM)), true);
            return 1;
        })));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("winter").then(t)));
    }
}
