package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;

/**
 * Het Vadsrijk, T-RIJDIER: mount looks ({@link Uiterlijk}, {@link Aankleden}), the three new land mounts (in economie's
 * {@code RijdierSoort}), and what pets do ({@link Maatjes}, {@link Snuffelen}, {@link Vahoegje}). Registers the tack items
 * ({@code vr_rijtuig_<id>}: the 3D models drawn on the mounts, and a reward token: right-click = that tack is yours), the hooks
 * (pet ticks, combat tracking, pet xp, pet passives) and the dev commands {@code /guhs vadsrijk rijdier ...} and
 * {@code /guhs vadsrijk maatje ...} (see SPIEK_T_RIJDIER.md).
 */
public final class RijdierExtraRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** The tack item per tack id (also the model the render layer draws on the mount). */
    public static final Map<String, DeferredItem<TuigItem>> TUIG = new LinkedHashMap<>();

    static {
        for (Variant v : Uiterlijk.alle()) {
            if (v.as() == As.TUIG) {
                TUIG.put(v.id(), ITEMS.registerItem("vr_rijtuig_" + v.id(), p -> new TuigItem(p, v.id()),
                        () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
            }
        }
    }

    private RijdierExtraRegistry() {
    }

    /** A tack piece: right-click to add it to your mounts' wardrobe. */
    public static class TuigItem extends Item {
        public final String tuig;

        public TuigItem(Properties p, String tuig) {
            super(p);
            this.tuig = tuig;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                if (Aankleden.geef(sp, "tuig:" + tuig)) {
                    if (!sp.isCreative()) {
                        sp.getItemInHand(hand).shrink(1);
                    }
                } else {
                    sp.sendOverlayMessage(Component.translatable("vadsrijk.economie.rijdier.al").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("vadsrijk.rijdier.tuig_item.uitleg").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        Spelers.EXTRA_STATS.add(Maatjes::stats);
        Schade.NA_KLAP.add(Maatjes::klap);
        Haken.VERSLAGEN.add(Maatjes::verslagen);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Maatjes.uitgelogd(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(RijdierExtraRegistry::commando);
    }

    static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        if (t % 20 != 7) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!Vadsrijk.in(p)) {
                continue;
            }
            Maatjes.tick(p);
            HuisdierEntity e = Huisdieren.uit(p);
            if (e != null) {
                e.zetGrootte(Vahoegje.ID.equals(e.soortId()) ? Vahoegje.SCHAAL[Vahoegje.stadium(p)] : 1f);
            }
            if (t % 200 == 7) {
                Aankleden.inhalen(p);
            }
        }
    }

    // ---- dev commands -------------------------------------------------------------------------------------------------------------

    private static ServerPlayer speler(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static void zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> r = Commands.literal("rijdier").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        // looks
        r.then(Commands.literal("garderobe")
                .then(Commands.literal("alles").executes(c -> {
                    Aankleden.alles(speler(c));
                    zeg(c, "alle vachten, patronen en tuig zijn van jou");
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    Aankleden.reset(speler(c));
                    zeg(c, "garderobe gewist (en alles uitgetrokken)");
                    return 1;
                }))
                .then(Commands.literal("inhalen").executes(c -> {
                    int n = Aankleden.inhalen(speler(c));
                    zeg(c, n + " nieuwe dingen uit prestaties/bazen/geheimen");
                    return 1;
                })));
        r.then(Commands.literal("geef").then(Commands.argument("sleutel", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Uiterlijk.alle().stream().map(Variant::sleutel), b))
                .executes(c -> {
                    boolean ok = Aankleden.geef(speler(c), StringArgumentType.getString(c, "sleutel"));
                    zeg(c, ok ? "gegeven" : "onbekend of al van jou");
                    return ok ? 1 : 0;
                })));
        r.then(Commands.literal("look").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(RijdierSoort.values()).map(RijdierSoort::id), b))
                .then(Commands.argument("as", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"vacht", "patroon", "tuig"}, b))
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
                            RijdierSoort s = RijdierSoort.van(StringArgumentType.getString(c, "soort"));
                            String as = StringArgumentType.getString(c, "as");
                            return SharedSuggestionProvider.suggest(Uiterlijk.alle().stream()
                                    .filter(v -> v.as().id().equals(as) && (s == null || v.voor(s))).map(Variant::id).distinct()
                                    .toList().toArray(new String[0]), b);
                        }).executes(c -> {
                            ServerPlayer p = speler(c);
                            RijdierSoort s = RijdierSoort.van(StringArgumentType.getString(c, "soort"));
                            String as = StringArgumentType.getString(c, "as"), id = StringArgumentType.getString(c, "id");
                            if (s == null) {
                                c.getSource().sendFailure(Component.literal("onbekend rijdier"));
                                return 0;
                            }
                            Rijdieren.geef(p, s);
                            Variant v = Uiterlijk.van(s, As.valueOf(as.toUpperCase(java.util.Locale.ROOT)), id);
                            if (v != null) {
                                Aankleden.geef(p, v, false);
                            }
                            boolean ok = Aankleden.kies(p, s, As.valueOf(as.toUpperCase(java.util.Locale.ROOT)), id.equals("geen") ? "" : id);
                            zeg(c, ok ? s.id() + " draagt nu: " + Aankleden.uiterlijk(p, s) : "kan niet");
                            return ok ? 1 : 0;
                        })))));
        r.then(Commands.literal("veren").then(Commands.argument("n", IntegerArgumentType.integer(1, 500)).executes(c -> {
            GoudenVeren.geef(speler(c), IntegerArgumentType.getInteger(c, "n"));
            return 1;
        })));
        r.then(Commands.literal("bronnen").executes(c -> {
            ServerPlayer p = speler(c);
            StringBuilder b = new StringBuilder("Bronnen (✔ = gehaald):");
            for (Variant v : Uiterlijk.alle()) {
                if (v.bron().vanzelf()) {
                    b.append("\n ").append(Aankleden.gehaald(p, v.bron()) ? "✔ " : "✘ ").append(v.sleutel()).append(" <- ").append(v.bron().soort())
                            .append(" ").append(v.bron().sleutel());
                }
            }
            for (var e : Aankleden.SOORT_BRON.entrySet()) {
                b.append("\n ").append(Aankleden.gehaald(p, e.getValue()) ? "✔ " : "✘ ").append("rijdier ").append(e.getKey().id()).append(" <- ")
                        .append(e.getValue().soort()).append(" ").append(e.getValue().sleutel()).append(e.getValue().waarde() > 0 ? " " + e.getValue().waarde() : "");
            }
            String s = b.toString();
            c.getSource().sendSuccess(() -> Component.literal(s), false);
            return 1;
        }));
        r.then(Commands.literal("schrik").executes(c -> {   // (the Zwamzwijntje test: a fake hit on your mount)
            ServerPlayer p = speler(c);
            if (!(p.getVehicle() instanceof RijdierEntity re)) {
                c.getSource().sendFailure(Component.literal("je rijdt niet"));
                return 0;
            }
            re.hurtServer(p.level(), p.level().damageSources().generic(), 1);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(r));

        // pets
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("maatje").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("test").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Huisdieren.alle().stream().map(Huisdier::id), b))
                .executes(c -> {
                    ServerPlayer p = speler(c);
                    String id = StringArgumentType.getString(c, "id");
                    if (Huisdieren.van(id) == null) {
                        c.getSource().sendFailure(Component.literal("onbekend huisdier"));
                        return 0;
                    }
                    Huisdieren.geef(p, id);
                    Huisdieren.kies(p, id);
                    Maatjes.wisCooldowns(p);
                    Maatjes.Kracht k = Maatjes.van(Huisdieren.van(id));
                    zeg(c, id + " loopt mee: " + k.rol() + " / " + k.id() + " (lv " + Maatjes.level(p, id) + ")");
                    return 1;
                })));
        m.then(Commands.literal("level").then(Commands.argument("lv", IntegerArgumentType.integer(1, Maatjes.MAX)).executes(c -> {
            ServerPlayer p = speler(c);
            Huisdier h = Huisdieren.actief(p);
            if (h == null) {
                c.getSource().sendFailure(Component.literal("geen huisdier mee"));
                return 0;
            }
            Maatjes.zetLevel(p, h.id(), IntegerArgumentType.getInteger(c, "lv"));
            zeg(c, h.id() + " is nu lv " + Maatjes.level(p, h.id()));
            return 1;
        })));
        m.then(Commands.literal("xp").then(Commands.argument("n", IntegerArgumentType.integer(1, 10000)).executes(c -> {
            ServerPlayer p = speler(c);
            Huisdier h = Huisdieren.actief(p);
            if (h == null) {
                c.getSource().sendFailure(Component.literal("geen huisdier mee"));
                return 0;
            }
            Maatjes.geefXp(p, h.id(), IntegerArgumentType.getInteger(c, "n"));
            return 1;
        })));
        m.then(Commands.literal("gevecht").then(Commands.literal("aan").executes(c -> {
            Maatjes.zetGevecht(speler(c), true);
            zeg(c, "je telt 5 s als 'in gevecht'");
            return 1;
        })).then(Commands.literal("uit").executes(c -> {
            Maatjes.zetGevecht(speler(c), false);
            return 1;
        })));
        m.then(Commands.literal("laag").executes(c -> {   // (test the Steuner abilities: Vads to 30 %, in combat, cooldowns ready)
            ServerPlayer p = speler(c);
            Spelers.data(p).vads = Spelers.maxVads(p) * 0.3;
            Spelers.data(p).energie = 0;
            Spelers.sync(p);
            Maatjes.zetGevecht(p, true);
            Maatjes.wisCooldowns(p);
            zeg(c, "Vads 30 %, energie 0, in gevecht");
            return 1;
        }));
        m.then(Commands.literal("vahoegje").then(Commands.argument("stadium", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"klein", "middel", "auto"}, b))
                .executes(c -> {
                    String s = StringArgumentType.getString(c, "stadium");
                    Vahoegje.zetDev(speler(c), s.equals("klein") ? Vahoegje.KLEIN : s.equals("middel") ? Vahoegje.MIDDEL : -1);
                    zeg(c, "Vahoegje: " + s);
                    return 1;
                })));
        m.then(Commands.literal("snuffel").executes(c -> {
            ServerPlayer p = speler(c);
            BlockPos b = Snuffelen.dichtstbij(p, 64);
            zeg(c, b == null ? "geen geheim binnen 64 blokken" : "dichtstbijzijnde geheim: " + b.toShortString() + " (" + (int) Math.sqrt(b.distToCenterSqr(p.position())) + " blokken)");
            return 1;
        }));
        m.then(Commands.literal("strooi").executes(c -> {   // (test Piepje: drops a few of your items around you)
            ServerPlayer p = speler(c);
            for (int i = 0; i < 5; i++) {
                net.minecraft.world.entity.item.ItemEntity it = new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX() + (i - 2) * 1.2, p.getY() + 0.5,
                        p.getZ() + 3, new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, 2));
                it.setTarget(p.getUUID());
                it.setPickUpDelay(10);
                p.level().addFreshEntity(it);
            }
            zeg(c, "5 hoopjes goud op 3 blokken; een Snuffelaar raapt ze op");
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }
}
