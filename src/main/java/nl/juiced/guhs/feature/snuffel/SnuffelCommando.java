package nl.juiced.guhs.feature.snuffel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /guhs snuffel ...} (ops; for the AutoCheck script and dev checks; the answers are plain literals):
 * <pre>
 * ga [strand|haven|laatste]      to the island          huis                home again
 * kies &lt;ras&gt; &lt;kleur&gt; &lt;maatje&gt; [naam]   set the choice    keuze               open the choice screen
 * stand                          what the server knows about you
 * houding &lt;0-7&gt;                 hold poses (1 sniff, 2 sit, 4 wag)       blaf | graaf
 * maatje geef|weg|stout          the companion          boom &lt;0-4&gt; | groei    the tree / one step with the scene
 * geur &lt;id&gt;                      learn a scent          daad &lt;id&gt; [geur]     a good deed
 * bron &lt;id&gt; &lt;geur&gt; [graven]      a scent source at your feet           bronweg &lt;id&gt;
 * bewoner &lt;naam&gt; | hond &lt;ras&gt; &lt;kleur&gt; [pup]   a resident in front of you
 * station | kaart | boekje       open the Guhstation window / the pause menu / the snuffelboekje
 * klaar                          finish the first series (Guhstation, blossom twig)
 * wis                            forget everything of the island (never while a dog)
 * </pre>
 * In dev runs only (for a headless server): {@code proefspeler <naam>} logs a stand-in player in (or moves it to the
 * command's spot), {@code proefweg <naam>} logs it out (saved), {@code zakken} lists the executing player's inventory, size
 * and position, {@code eiland} says what stands on the island, {@code neus} what the executing dog smells and why,
 * {@code proefrespawn <naam>} presses "Respawn" for a dead stand-in.
 */
final class SnuffelCommando {
    /** (Dev) the stand-in players: nobody ticks a player whose connection is not the server's own, so we do. */
    private static final Set<String> PROEF = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static boolean proefTick;

    private SnuffelCommando() {
    }

    private static void tikProefspelers(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for (String naam : PROEF) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayerByName(naam);
            if (p == null) {
                PROEF.remove(naam);
            } else {
                p.doTick();
            }
        }
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private static int fout(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendFailure(Component.literal(tekst));
        return 0;
    }

    private static int ga(CommandContext<CommandSourceStack> c, Reis.Aankomst waar) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        return Reis.naarEiland(p, waar) ? zeg(c, "Op het eiland (" + waar + ")") : fout(c, "Dat gaat nu niet");
    }

    static void registreer(RegisterCommandsEvent event) {
        var wortel = Commands.literal("snuffel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").executes(c -> ga(c, Reis.Aankomst.LAATSTE))
                        .then(Commands.literal("strand").executes(c -> ga(c, Reis.Aankomst.STRAND)))
                        .then(Commands.literal("haven").executes(c -> ga(c, Reis.Aankomst.HAVEN)))
                        .then(Commands.literal("laatste").executes(c -> ga(c, Reis.Aankomst.LAATSTE))))
                .then(Commands.literal("huis").executes(c -> Reis.naarHuis(c.getSource().getPlayerOrException()) ? zeg(c, "Thuis") : fout(c, "Je bent geen hond")))
                .then(Commands.literal("kies").then(Commands.argument("ras", StringArgumentType.word())
                        .then(Commands.argument("kleur", StringArgumentType.word()).then(Commands.argument("maatje", StringArgumentType.word())
                                .executes(c -> kies(c, null))
                                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> kies(c, StringArgumentType.getString(c, "naam"))))))))
                .then(Commands.literal("keuze").executes(c -> {
                    Keuze.open(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Keuze k = Keuze.vanOfStandaard(p);
                    return zeg(c, "hond=" + Hondvorm.actief(p) + " eiland=" + Eiland.in(p) + " keuze=" + k + " gekozen=" + Keuze.heeft(p) + " rang="
                            + Rang.van(p).nummer() + " geuren=" + Geuren.geleerd(p) + " daden=" + Daden.van(p) + " boom=" + Boom.stap(p) + " maatje="
                            + Maatjes.heeft(p) + " kluis=" + SnuffelKluis.heeft(p) + " post=" + SnuffelKluis.postAantal(p) + " stap=" + SnuffelFeature.LIJN.stap(p)
                            + " thuis=" + Reis.thuis(p) + " laatste=" + Reis.laatste(p));
                }))
                .then(Commands.literal("houding").then(Commands.argument("vlaggen", IntegerArgumentType.integer(0, 7)).executes(c -> {
                    Hondvorm.zetHouding(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "vlaggen"));
                    return 1;
                })))
                .then(Commands.literal("blaf").executes(c -> Hondvorm.blaf(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("graaf").executes(c -> Snuffelen.graaf(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("maatje")
                        .then(Commands.literal("geef").executes(c -> Maatjes.geef(c.getSource().getPlayerOrException()) ? zeg(c, "Het maatje is er") : fout(c, "Was er al")))
                        .then(Commands.literal("weg").executes(c -> {
                            Maatjes.neemAf(c.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("stout").executes(c -> {
                            Maatjes.ondeugend(c.getSource().getPlayerOrException(), 100);
                            return 1;
                        })))
                .then(Commands.literal("boom").then(Commands.argument("stap", IntegerArgumentType.integer(0, Boom.MAX)).executes(c -> {
                    Boom.zet(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "stap"));
                    return 1;
                })))
                .then(Commands.literal("groei").executes(c -> Boom.groei(c.getSource().getPlayerOrException(), null) ? 1 : 0))
                .then(Commands.literal("geur").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
                    Geuren.alle().forEach(g -> b.suggest(g.id()));
                    return b.buildFuture();
                }).executes(c -> Geuren.leer(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")) ? 1 : 0)))
                .then(Commands.literal("daad").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> Daden.geef(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"), null) ? 1 : 0)
                        .then(Commands.argument("geur", StringArgumentType.word()).executes(c -> Daden.geef(c.getSource().getPlayerOrException(),
                                StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "geur")) ? 1 : 0))))
                .then(Commands.literal("bron").then(Commands.argument("id", StringArgumentType.word()).then(Commands.argument("geur", StringArgumentType.word())
                        .executes(c -> bron(c, true))
                        .then(Commands.argument("graven", BoolArgumentType.bool()).executes(c -> bron(c, BoolArgumentType.getBool(c, "graven")))))))
                .then(Commands.literal("bronweg").then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> Geurbronnen.haalWeg(c.getSource().getLevel(), StringArgumentType.getString(c, "id")) ? 1 : 0)))
                .then(Commands.literal("bewoner").then(Commands.argument("naam", StringArgumentType.word()).suggests((c, b) -> {
                    Honden.bewoners().forEach(x -> b.suggest(x.id()));
                    return b.buildFuture();
                }).executes(c -> {
                    CommandSourceStack s = c.getSource();
                    Vec3 voor = s.getPosition().add(Vec3.directionFromRotation(0, s.getRotation().y).scale(2.0));
                    return Bewoners.plaats(s.getLevel(), voor, s.getRotation().y + 180f, StringArgumentType.getString(c, "naam")) != null ? 1
                            : fout(c, "Die bewoner bestaat niet");
                })))
                .then(Commands.literal("hond").then(Commands.argument("ras", StringArgumentType.word()).then(Commands.argument("kleur", StringArgumentType.word())
                        .executes(c -> hond(c, false))
                        .then(Commands.argument("pup", BoolArgumentType.bool()).executes(c -> hond(c, BoolArgumentType.getBool(c, "pup")))))))
                .then(Commands.literal("station").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return GuhstationBlock.open(p, p.blockPosition()) ? 1 : 0;
                }))
                .then(Commands.literal("kaart").executes(c -> GeheugenkaartItem.open(c.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("boekje").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    nl.juiced.guhs.network.ModNetworking.sendTo(p, new SnuffelPayloads.Open(SnuffelPayloads.Open.BOEKJE, Stand.van(p)));
                    return 1;
                }))
                .then(Commands.literal("klaar").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return zeg(c, Snuffel.rondAf(p) ? "De eerste reeks is klaar" : "Was al klaar");
                }))
                .then(Commands.literal("geef").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Snuffel.geef(p, new ItemStack(SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()));
                    return 1;
                }))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (Hondvorm.actief(p)) {
                        return fout(c, "Niet terwijl je een hond bent: ga eerst naar huis");
                    }
                    nl.juiced.guhs.quest.GuhQuests.saved(p).remove(SnuffelData.SLEUTEL);
                    SnuffelFeature.LIJN.wis(p);
                    Hondvorm.sync(p);
                    Stand.stuur(p);
                    return zeg(c, "Alles van het Snuffeleiland vergeten");
                }));
        if (!FMLEnvironment.isProduction()) {
            // dev runs only: a stand-in player for a headless server (a real ServerPlayer on a connection that leads nowhere,
            // ticked like any player), so the dog form can be walked through in the REAL dimension from the console
            if (!proefTick) {
                proefTick = true;
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SnuffelCommando::tikProefspelers);
            }
            wortel.then(Commands.literal("proefspeler").then(Commands.argument("naam", StringArgumentType.word()).executes(c -> {
                CommandSourceStack s = c.getSource();
                String naam = StringArgumentType.getString(c, "naam");
                ServerPlayer p = s.getServer().getPlayerList().getPlayerByName(naam);
                PROEF.add(naam);
                if (p == null) {
                    // a login as the game does it (server/network/config/PrepareSpawnTask): the saved data decides the level and
                    // the spot, the player is loaded from its file, then placed
                    MinecraftServer server = s.getServer();
                    UUID uuid = UUID.nameUUIDFromBytes(naam.getBytes());
                    CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(uuid, naam), false);
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LogUtils.getLogger())) {
                        Optional<ValueInput> data = server.getPlayerList().loadPlayerData(new NameAndId(uuid, naam))
                                .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag));
                        ServerPlayer.SavedPosition bewaard = data.<ServerPlayer.SavedPosition>flatMap(tag -> tag.read(ServerPlayer.SavedPosition.MAP_CODEC))
                                .orElse(ServerPlayer.SavedPosition.EMPTY);
                        ServerLevel level = bewaard.dimension().map(server::getLevel).orElse(s.getLevel());
                        Vec3 plek = bewaard.position().orElse(s.getPosition());
                        Vec2 hoek = bewaard.rotation().orElse(Vec2.ZERO);
                        ServerPlayer nieuw = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
                        data.ifPresent(nieuw::load);
                        net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(nieuw, server.getPlayerList(), nieuw.getStringUUID());
                        nieuw.snapTo(plek, hoek.x, hoek.y);
                        Connection connection = new Connection(PacketFlow.SERVERBOUND);
                        new EmbeddedChannel(connection);
                        server.getPlayerList().placeNewPlayer(connection, nieuw, cookie);
                        return zeg(c, "proefspeler " + naam + " logged in (" + (data.isPresent() ? "from its file" : "new") + ") at " + nieuw.position() + " in "
                                + nieuw.level().dimension().identifier());
                    }
                }
                Vec3 plek = s.getPosition();
                p.teleportTo(s.getLevel(), plek.x, plek.y, plek.z, Set.of(), s.getRotation().y, s.getRotation().x, false);
                return zeg(c, "proefspeler " + naam + " stands at " + p.position() + " in " + p.level().dimension().identifier());
            })));
            wortel.then(Commands.literal("proefweg").then(Commands.argument("naam", StringArgumentType.word()).executes(c -> {
                ServerPlayer p = c.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(c, "naam"));
                if (p == null) {
                    return 0;
                }
                c.getSource().getServer().getPlayerList().remove(p);
                return zeg(c, "proefspeler logged out (saved)");
            })));
            wortel.then(Commands.literal("zakken").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                StringBuilder uit = new StringBuilder("zakken van " + p.getGameProfile().name() + " (gekozen " + p.getInventory().getSelectedSlot() + ", hoog "
                        + p.getBbHeight() + ", ogen " + p.getEyeHeight() + ", " + p.position() + " " + p.getYRot() + "/" + p.getXRot() + " in "
                        + p.level().dimension().identifier() + "):");
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        uit.append(' ').append(i).append('=').append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())
                                .append('x').append(stack.getCount());
                    }
                }
                return zeg(c, uit.toString());
            }));
            wortel.then(Commands.literal("proefrespawn").then(Commands.argument("naam", StringArgumentType.word()).executes(c -> {
                // (what a client's "Respawn" button asks for)
                ServerPlayer p = c.getSource().getServer().getPlayerList().getPlayerByName(StringArgumentType.getString(c, "naam"));
                if (p == null || !p.isDeadOrDying()) {
                    return fout(c, "geen dode proefspeler");
                }
                ServerPlayer nieuw = c.getSource().getServer().getPlayerList().respawn(p, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
                p.connection.player = nieuw;
                return zeg(c, "proefspeler respawned at " + nieuw.position() + " in " + nieuw.level().dimension().identifier());
            })));
            wortel.then(Commands.literal("neus").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                StringBuilder uit = new StringBuilder("neus: " + Geurbronnen.ruik(p) + " graafbaar: " + Geurbronnen.graafbaarBij(p) + " graaft: " + Snuffelen.graaft(p)
                        + " grond: " + p.onGround() + " water: " + p.isInWater() + " scene: " + nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(p) + " bronnen:");
                for (Geurbronnen.Bron b : Geurbronnen.bronnen(p)) {
                    uit.append(' ').append(b.id()).append('@').append(b.plek()).append(Geurbronnen.ruikbaar(p, b) ? "" : "(niet ruikbaar)");
                }
                return zeg(c, uit.toString());
            }));
            wortel.then(Commands.literal("eiland").executes(c -> {
                Eiland.Plaats pl = Eiland.plaats(c.getSource().getServer());
                if (pl == null) {
                    return fout(c, "geen eiland");
                }
                int bewoners = pl.level().getEntitiesOfClass(BewonerEntity.class, pl.doos().inflate(16)).size();
                int bomen = pl.level().getEntitiesOfClass(BoompjeEntity.class, pl.doos().inflate(16)).size();
                int maatjes = pl.level().getEntitiesOfClass(MaatjeEntity.class, pl.doos().inflate(16)).size();
                return zeg(c, "eiland: gebouwd versie " + Eiland.gebouwd(pl) + " (jar " + pl.opzet().versie() + "), hoek " + pl.oorsprong().toShortString() + ", spelers "
                        + Eiland.spelers(pl).size() + ", bewoners " + bewoners + ", bomen " + bomen + ", maatjes " + maatjes);
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private static int kies(CommandContext<CommandSourceStack> c, String naam) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Keuze k = new Keuze(StringArgumentType.getString(c, "ras"), StringArgumentType.getString(c, "kleur"),
                naam == null ? p.getGameProfile().name() : naam, StringArgumentType.getString(c, "maatje"));
        return Keuze.zet(p, k) ? zeg(c, "Gekozen: " + Keuze.van(p)) : fout(c, "Geen geldige keuze (ras, een kleur van dat ras, maatje a/b/c)");
    }

    private static int bron(CommandContext<CommandSourceStack> c, boolean graven) {
        CommandSourceStack s = c.getSource();
        boolean gelukt = Geurbronnen.plaats(s.getLevel(), BlockPos.containing(s.getPosition()), StringArgumentType.getString(c, "id"),
                StringArgumentType.getString(c, "geur"), graven, Geurbronnen.BEREIK);
        return gelukt ? zeg(c, "Geurbron geplaatst") : fout(c, "Die geur bestaat niet");
    }

    private static int hond(CommandContext<CommandSourceStack> c, boolean pup) {
        CommandSourceStack s = c.getSource();
        Vec3 voor = s.getPosition().add(Vec3.directionFromRotation(0, s.getRotation().y).scale(2.0));
        return Bewoners.plaatsHond(s.getLevel(), voor, s.getRotation().y + 180f, StringArgumentType.getString(c, "ras"),
                StringArgumentType.getString(c, "kleur"), pup) != null ? 1 : fout(c, "Dat ras bestaat niet");
    }
}
