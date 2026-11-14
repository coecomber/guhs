package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * T-PUZZELS (night round T, VOORSTEL_ROADMAP 5.1): the puzzle toolbox + the puzzles and secrets of zone 1-3. Registered from
 * {@code VadsrijkFeature} (one line, marked T-PUZZELS). Blocks {@code vr_pz_<soort>}, items {@code vr_pz_bladzijde_1..9}, data
 * {@code data/guhs/vadsrijk_puzzels/*.json} (tools/features/vadsrijk_t_puzzels.py).
 * <p>Commands (op 2): {@code /guhs vadsrijk puzzel lijst | tp <id> | los <id> | reset <id> | hint <id> | plaats <id> [opnieuw]
 * | hier <id> | vergeet <id|alles> | nacht | herlaad | bladzijde <n>}.
 */
public final class PuzzelRegistry {
    public static final List<String> SOORTEN = List.of("drukplaat", "klanksteen", "voorspeelsteen", "symboolsteen", "probeerknop", "herstelknop",
            "spiegel", "lichtbron", "lichtdoel", "schuifblok", "doelvloer", "startlantaarn", "eindplaat", "scheur", "geheimdeur", "hendel",
            "fluistersteen", "sterrensteen", "sterrenkaart");
    public static final int BLADZIJDES = 9;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final Map<String, DeferredBlock<PuzzelBlok>> BLOK = new HashMap<>();
    private static final List<DeferredItem<Bladzijde>> PAGINAS = new ArrayList<>();

    static {
        for (String s : SOORTEN) {
            BLOK.put(s, BLOCKS.registerBlock("vr_pz_" + s, p -> new PuzzelBlok(p, s), () -> eigenschappen(s)));
        }
        for (int n = 1; n <= BLADZIJDES; n++) {
            int nr = n;
            PAGINAS.add(ITEMS.registerItem("vr_pz_bladzijde_" + n, p -> new Bladzijde(p, nr), () -> new Item.Properties().stacksTo(1)));
        }
    }

    private PuzzelRegistry() {
    }

    private static BlockBehaviour.Properties eigenschappen(String soort) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(switch (soort) {
            case "spiegel", "lichtdoel", "sterrensteen" -> Blocks.AMETHYST_BLOCK;
            case "schuifblok" -> Blocks.PACKED_ICE;
            case "startlantaarn" -> Blocks.LANTERN;
            case "hendel" -> Blocks.LEVER;
            case "sterrenkaart" -> Blocks.SPRUCE_PLANKS;
            default -> Blocks.STONE;
        }).strength(-1, 3600000).noLootTable();
        switch (soort) {
            case "drukplaat", "eindplaat", "probeerknop", "herstelknop", "hendel", "startlantaarn" -> p = p.noOcclusion();
            default -> {
            }
        }
        return switch (soort) {
            case "klanksteen", "lichtdoel", "doelvloer", "eindplaat", "drukplaat" -> p.lightLevel(s -> s.getValue(PuzzelBlok.AAN) ? 10 : 0);
            case "sterrensteen" -> p.lightLevel(s -> s.getValue(PuzzelBlok.AAN) ? 12 : 2);
            case "startlantaarn" -> p.lightLevel(s -> s.getValue(PuzzelBlok.AAN) ? 15 : 10);
            case "lichtbron" -> p.lightLevel(s -> 12);
            case "fluistersteen", "scheur" -> p.lightLevel(s -> 3);   // (a faint glint: findable, not obvious)
            default -> p;
        };
    }

    @Nullable
    public static PuzzelBlok blok(String soort) {
        DeferredBlock<PuzzelBlok> b = BLOK.get(soort);
        return b == null ? null : b.get();
    }

    @Nullable
    public static Item bladzijde(int n) {
        return n >= 1 && n <= PAGINAS.size() ? PAGINAS.get(n - 1).get() : null;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        PuzzelBoek.registreer();
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            Puzzels.laad(e.getServer());
            PuzzelBoek.herbouw();
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> List.copyOf(Puzzels.alleInst().keySet()).forEach(id -> Puzzels.verwijder(id)));
        nl.juiced.guhs.world.TikWacht.post("PuzzelRegistry:120", e -> Puzzels.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            if (Puzzels.zeg(e.getPlayer(), e.getRawText())) {
                e.setCanceled(true);   // (the answer is whispered: nobody else reads it)
            }
        });
        NeoForge.EVENT_BUS.addListener(PuzzelRegistry::commando);
    }

    // ---- commands ------------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> metId(String naam, Uitvoer u) {
        return Commands.literal(naam).then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Puzzels.defs().stream().map(PuzzelDef::id), b))
                .executes(c -> {
                    PuzzelDef d = Puzzels.def(StringArgumentType.getString(c, "id"));
                    if (d == null) {
                        c.getSource().sendFailure(Component.literal("Onbekende puzzel (zie /guhs vadsrijk puzzel lijst)"));
                        return 0;
                    }
                    return u.doe(c, c.getSource().getPlayerOrException(), d);
                }));
    }

    @FunctionalInterface
    interface Uitvoer {
        int doe(CommandContext<CommandSourceStack> c, ServerPlayer p, PuzzelDef d) throws CommandSyntaxException;
    }

    static void naar(ServerPlayer p, Vec3 v) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("puzzel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("lijst").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            PuzzelData data = PuzzelData.get(c.getSource().getServer());
            for (PuzzelDef d : Puzzels.defs()) {
                PuzzelData.Plek pl = data.plek(d.id());
                String waar = pl != null ? pl.origin().toShortString() : "nog niet geplaatst (anker " + (d.anker().punt() == null ? "ONTBREEKT: " + d.anker().plek()
                        : d.anker().punt().toShortString()) + ")";
                p.sendSystemMessage(Component.literal((PuzzelSpeler.opgelost(p, d.id()) ? "✔ " : "· ") + d.id() + " [z" + d.zone() + ", " + d.soort() + "] ")
                        .withStyle(PuzzelSpeler.opgelost(p, d.id()) ? ChatFormatting.GREEN : ChatFormatting.WHITE)
                        .append(Component.translatable(d.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.literal(" @ " + waar).withStyle(ChatFormatting.GRAY)));
            }
            return Puzzels.defs().size();
        }));
        k.then(metId("tp", (c, p, d) -> {
            Puzzels.Inst in = Puzzels.inst(d.id());
            if (in != null) {
                naar(p, PuzzelStempel.kijkPlek(in.origin, d));
                return 1;
            }
            BlockPos a = d.anker().punt();
            if (a == null) {
                c.getSource().sendFailure(Component.literal("Het anker " + d.anker().plek() + " bestaat niet (meer)"));
                return 0;
            }
            naar(p, Vec3.atBottomCenterOf(a).add(0, 1, 0));
            p.sendSystemMessage(Component.literal("Nog niet geplaatst: hij verschijnt zo hier in de buurt. Doe de tp nog eens om er vlak voor te staan.")
                    .withStyle(ChatFormatting.GRAY));
            return 1;
        }));
        k.then(metId("los", (c, p, d) -> {
            Puzzels.Inst in = Puzzels.inst(d.id());
            if (in == null) {
                c.getSource().sendFailure(Component.literal("Nog niet geplaatst: eerst /guhs vadsrijk puzzel tp " + d.id()));
                return 0;
            }
            if (p.level() != in.level || p.distanceToSqr(in.midden().getCenter()) > 400) {
                naar(p, PuzzelStempel.kijkPlek(in.origin, d));
            }
            boolean ok = PuzzelDev.los(in, p);
            p.sendSystemMessage(Component.literal(ok ? "Opgelost via de echte invoer." : "Niet gelukt! (zie log)").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            return ok ? 1 : 0;
        }));
        k.then(metId("reset", (c, p, d) -> {
            Puzzels.Inst in = Puzzels.inst(d.id());
            if (in != null) {
                Puzzels.herstel(in);
                in.fouten.clear();
                in.draaien.clear();
            }
            p.sendSystemMessage(Component.literal("Puzzel teruggezet (en je pogingen/tips vergeten)").withStyle(ChatFormatting.GRAY));
            return 1;
        }));
        k.then(metId("hint", (c, p, d) -> {
            p.sendSystemMessage(Component.translatable(d.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            for (String deel : List.of("uitleg", "hint1", "hint2", "aanwijzing")) {
                p.sendSystemMessage(Component.literal(deel + ": ").withStyle(ChatFormatting.GRAY).append(Component.translatable(d.key(deel))));
            }
            if (!d.woorden().isEmpty()) {
                p.sendSystemMessage(Component.literal("antwoorden: " + String.join(", ", d.woorden())).withStyle(ChatFormatting.GRAY));
            }
            return 1;
        }));
        k.then(Commands.literal("plaats").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Puzzels.defs().stream().map(PuzzelDef::id), b))
                .executes(c -> {
                    PuzzelDef d = Puzzels.def(StringArgumentType.getString(c, "id"));
                    return d == null ? 0 : plaats(c, c.getSource().getPlayerOrException(), d, false);
                }).then(Commands.literal("opnieuw").executes(c -> {
                    PuzzelDef d = Puzzels.def(StringArgumentType.getString(c, "id"));
                    return d == null ? 0 : plaats(c, c.getSource().getPlayerOrException(), d, true);
                }))));
        k.then(metId("hier", (c, p, d) -> {
            ServerLevel sl = (ServerLevel) p.level();
            BlockPos o = p.blockPosition().offset(-d.breed() / 2, 0, 2);
            PuzzelStempel.stempel(sl, d, o);
            Puzzels.Inst in = Puzzels.activeer(d, sl, o, true);
            Puzzels.herstel(in);
            p.sendSystemMessage(Component.literal("Testkopie van " + d.id() + " vóór je neergezet (telt niet als de echte plek).").withStyle(ChatFormatting.GRAY));
            return 1;
        }));
        k.then(Commands.literal("vergeet").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("alles"),
                        Puzzels.defs().stream().map(PuzzelDef::id)), b))
                .executes(c -> {
                    String id = StringArgumentType.getString(c, "id");
                    PuzzelSpeler.vergeet(c.getSource().getPlayerOrException(), id.equals("alles") ? null : id);
                    c.getSource().sendSuccess(() -> Component.literal("Vergeten: " + id + " (de beloning kan opnieuw)"), false);
                    return 1;
                })));
        k.then(Commands.literal("nacht").executes(c -> {
            Puzzels.nachtForceren = !Puzzels.nachtForceren;
            c.getSource().sendSuccess(() -> Component.literal("Sterrenpuzzels doen alsof het nacht is: " + Puzzels.nachtForceren), false);
            return 1;
        }));
        k.then(Commands.literal("herlaad").executes(c -> {
            int n = Puzzels.laad(c.getSource().getServer());
            PuzzelBoek.herbouw();
            c.getSource().sendSuccess(() -> Component.literal(n + " puzzels gelezen"), false);
            return n;
        }));
        k.then(Commands.literal("bladzijde").then(Commands.argument("n", IntegerArgumentType.integer(1, BLADZIJDES)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Bladzijde.lees(p, n);
            PuzzelSpeler.geef(p, new ItemStack(bladzijde(n)));
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int plaats(CommandContext<CommandSourceStack> c, ServerPlayer p, PuzzelDef d, boolean opnieuw) {
        MinecraftServer server = c.getSource().getServer();
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return 0;
        }
        BlockPos o = PuzzelStempel.plaats(sl, d, opnieuw);
        if (o == null) {
            c.getSource().sendFailure(Component.literal("Anker ontbreekt of het gebied is niet geladen (ga er eerst heen met tp)"));
            return 0;
        }
        p.sendSystemMessage(Component.literal(d.id() + " staat op " + o.toShortString()).withStyle(ChatFormatting.GRAY));
        return 1;
    }
}
