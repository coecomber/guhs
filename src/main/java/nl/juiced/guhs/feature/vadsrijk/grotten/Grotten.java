package nl.juiced.guhs.feature.vadsrijk.grotten;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.puzzels.PuzzelSpeler;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;

/**
 * U-GROTTEN (round U, FEEDBACK_RONDE4 V2): the five hand-made atmosphere caves, one per zone. The caves are built into the map tiles
 * (tools/vadsrijk/grotten); this class gives them their game side, all from {@link GrotData}:
 * <ul>
 * <li>an <b>area</b> per cave ({@code grot_<id>}, with a y range so the surface above stays its own area; pushed in front of every
 * zone list after server start) and a <b>biome</b> per cave ({@code guhs:vadsrijk_grot_<id>}, "Het Vadsrijk - &lt;grot&gt;", via
 * {@link #biome});</li>
 * <li>a <b>discovery card</b> the first time you enter (like a viewpoint: title card + chime + chat line), counted in the
 * Verkenningsboek category "Grotten" of the zone page (discovered, elite, puzzle, treasure);</li>
 * <li><b>Ambacht spots</b> (2 groups per cave, {@link AmbachtPlekken#GROEPEN}; POI ids {@code grot_<id>_amb_n} resolve here);</li>
 * <li>an <b>elite</b> (its own spawner, 1 at a time, 5 min respawn) + a few cave mobs, through the kern {@link Spawners}; a small
 * bonus (coins, and a rare piece the first time) for everyone who helped;</li>
 * <li>a <b>puzzle</b> (t-puzzels toolbox, id {@code grot_<id>}, anchored to {@code grot_<id>_puzzel}) and a <b>treasure chest</b>
 * you open by walking up to it, once per player, only after the puzzle is solved;</li>
 * <li>a <b>sound streek</b> for t-geluid ({@link GeluidPlan#EXTRA}: drip/echo cave bed + a fitting theme); the client adds the
 * particles and the drips ({@code client/GrotSfeerClient}).</li>
 * </ul>
 * Dev: {@code /guhs vadsrijk grot lijst | tp <id> [plek] | wis <id> | elite <id>} (U-GROTKERKER adds run | reset | baas | kist).
 */
public final class Grotten {
    public static final String DATA = "grotten";
    private static final Map<UUID, List<long[]>> KAART_WACHT = new HashMap<>();
    private static int herhaal = 0;

    private Grotten() {
    }

    // ---- data lookups (pure, also used by the biome source and the client) ----------------------------------------------------

    @Nullable
    public static GrotData.Grot grot(String id) {
        for (GrotData.Grot g : GrotData.ALLE) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    /** The cave at a position (null: none). */
    @Nullable
    public static GrotData.Grot op(double x, double y, double z) {
        for (GrotData.Grot g : GrotData.ALLE) {
            if (g.bevat(x, y, z)) {
                return g;
            }
        }
        return null;
    }

    /** VadsrijkBiomen hook: the biome id ("grot_zwamgrot") at block x/y/z, or null outside every cave. */
    @Nullable
    public static String biome(int x, int y, int z) {
        GrotData.Grot g = op(x + 0.5, y, z + 0.5);
        return g == null ? null : g.biome();
    }

    /** A cave POI by its full id {@code grot_<cave>_<key>} (null when unknown). */
    @Nullable
    public static BlockPos plek(String id) {
        if (!id.startsWith("grot_")) {
            return null;
        }
        for (GrotData.Grot g : GrotData.ALLE) {
            String pre = "grot_" + g.id() + "_";
            if (id.startsWith(pre)) {
                return g.plek(id.substring(pre.length()));
            }
        }
        return null;
    }

    public static String gebiedId(GrotData.Grot g) {
        return "grot_" + g.id();
    }

    public static String puzzelId(GrotData.Grot g) {
        return "grot_" + g.id();
    }

    // ---- registration --------------------------------------------------------------------------------------------------------

    public static void register(IEventBus modBus) {
        for (GrotData.Amb a : GrotData.AMBACHT) {
            AmbachtPlekken.GROEPEN.add(new AmbachtPlekken.Groep("grot_" + a.grot() + "_" + a.poi().substring(a.poi().lastIndexOf("amb")), a.zone(),
                    a.poi(), Ambacht.valueOf(a.ambacht()), a.tier(), a.aantal(), a.straal(), AmbachtPlekken.Zoek.valueOf(a.zoek()), ""));
        }
        for (GrotData.Grot g : GrotData.ALLE) {
            GeluidPlan.EXTRA.put(gebiedId(g), new GeluidPlan.Streek(gebiedId(g), List.of(thema(g)), g.id().equals("wortelgrot") ? "wortel" : "grot"));
            // U-GROTKERKER: no more free elite/mob spawners: the cave run (grotkerker.GrotKerker) spawns the packs + the mid-elite
            // (spawner id "grot:<id>#elite" stays, so the elite bonus + book entry below keep working)
        }
        for (int zone = 1; zone <= 5; zone++) {
            int z = zone;
            Voltooiing.extraCategorie("zone" + zone, () -> categorie(z));
        }
        Haken.GEBIED_BETREDEN.add(Grotten::betreden);
        Haken.VERSLAGEN.add(Grotten::verslagen);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            gebieden();
            herhaal = 40;
        });
        nl.juiced.guhs.world.TikWacht.post("Grotten:168", e -> tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(Grotten::commando);
    }

    /** The cave areas, in front of every other area (the zone lists also push theirs at server start: we repeat after them). */
    static void gebieden() {
        for (GrotData.Grot g : GrotData.ALLE) {
            Gebieden.voegVooraan(gebied(g));
        }
    }

    public static Gebieden.Gebied gebied(GrotData.Grot g) {
        return new Gebieden.Gebied(gebiedId(g), g.minX(), g.minZ(), g.maxX(), g.maxZ(), g.minLv(), g.maxLv(), false).metY(g.minY(), g.maxY());
    }

    private static String thema(GrotData.Grot g) {
        return switch (g.zone()) {
            case 1 -> "druipgrotten";
            case 2 -> "splinterkloof";
            case 3 -> "wortelduister";
            case 4 -> "lampionkust_avond";
            default -> "hoogvads";
        };
    }

    private static void spawners(GrotData.Grot g) {
        BlockPos hal = g.plek("hal"), elite = g.plek("elite");
        java.util.function.Supplier<? extends EntityType<? extends VadsrijkMob>> mob, baas;
        switch (g.zone()) {
            case 1 -> {
                mob = WezensRegistry.GLIMZWAM;
                baas = WezensRegistry.ZWAMWACHTER;
            }
            case 2 -> {
                mob = Z2WezensRegistry.KRISTALNAALDKRUIPER;
                baas = Z2WezensRegistry.VERSTEENDE_WACHT;
            }
            case 3 -> {
                mob = Z3WezensRegistry.WORTELSCHIM;
                baas = Z3WezensRegistry.WORTELSLANG;
            }
            case 4 -> {
                mob = Z4WezensRegistry.KORAALKOBOLD;
                baas = Z4WezensRegistry.MOPPERKRAB;
            }
            default -> {
                mob = Z5WezensRegistry.MISTSLIER;
                baas = Z5WezensRegistry.GRAUWE_WOLKDUIVEL;
            }
        }
        Spawners.registreer(new Spawners.Spawner("grot:" + g.id() + "#mob", hal, 9, mob, 3, 600, g.minLv(), g.maxLv(), null, 0.03)
                .grot(hal.getY() - 2, hal.getY() + 4));
        Spawners.registreer(new Spawners.Spawner("grot:" + g.id() + "#elite", elite, 3, baas, 1, 6000, g.eliteLv(), g.eliteLv(), null, 1.0)
                .grot(elite.getY() - 2, elite.getY() + 3));
    }

    // ---- per player data -------------------------------------------------------------------------------------------------------

    static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(DATA);
    }

    public static boolean heeft(ServerPlayer p, GrotData.Grot g, String wat) {
        return data(p).getBooleanOr(g.id() + "." + wat, false);
    }

    static void zet(ServerPlayer p, GrotData.Grot g, String wat) {
        data(p).putBoolean(g.id() + "." + wat, true);
    }

    // ---- discovery ------------------------------------------------------------------------------------------------------------

    static void betreden(ServerPlayer p, String gebied) {
        for (GrotData.Grot g : GrotData.ALLE) {
            if (gebiedId(g).equals(gebied) && !heeft(p, g, "ontdekt")) {
                zet(p, g, "ontdekt");
                // after kern's own area title (same tick): our card a second later, like a viewpoint
                KAART_WACHT.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(new long[] {GrotData.ALLE.indexOf(g), 25});
            }
        }
    }

    public static void kaart(ServerPlayer p, GrotData.Grot g) {
        RijkScenes.titel(p, Component.translatable("vadsrijk.grotten.naam." + g.id()).withStyle(ChatFormatting.GOLD),
                Component.translatable("vadsrijk.grotten.ontdekt.titel").append(" ").append(Component.translatable("vadsrijk.grotten.onder." + g.id())),
                90, 0xFFE3A0);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.8f);
        p.sendSystemMessage(Component.translatable("vadsrijk.grotten.ontdekt.chat", Component.translatable("vadsrijk.grotten.naam." + g.id()))
                .withStyle(ChatFormatting.YELLOW));
    }

    // ---- elite ----------------------------------------------------------------------------------------------------------------

    static void verslagen(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (m.spawner == null || !m.spawner.startsWith("grot:") || !m.spawner.endsWith("#elite")) {
            return;
        }
        GrotData.Grot g = grot(m.spawner.substring(5, m.spawner.indexOf('#')));
        if (g == null) {
            return;
        }
        for (ServerPlayer p : v.helpers()) {
            boolean eerste = !heeft(p, g, "elite");
            zet(p, g, "elite");
            List<ItemStack> buit = new ArrayList<>();
            if (eerste) {
                BasisType t = stukVoor(p, g.zone());
                if (t != null) {
                    buit.add(Rollen.stack(Rollen.rol(t, g.eliteLv(), Zeldzaamheid.ZELDZAAM, p.getRandom())));
                }
            }
            LootBundel.laatVallen(p, m.position().add(0, 0.6, 0), new LootTabel.Resultaat(buit, 150L * g.zone() * g.zone() + 100), 1);
            p.sendSystemMessage(Component.translatable("vadsrijk.grotten.elite.verslagen", Component.translatable("vadsrijk.grotten.elite.naam." + g.id()))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** A random piece the player can use: a weapon of their class or any armour (as the zone-1 treasures do). */
    @Nullable
    static BasisType stukVoor(ServerPlayer p, int zone) {
        return nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools.stuk(zone, Spelers.data(p).klasse, p.getRandom());   // 1101-balans: only this zone's pool
    }

    // ---- treasure + tick ------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (herhaal > 0 && --herhaal % 20 == 0) {
            gebieden();   // (again after every other registry's server-start pass: the caves must win over the zone areas)
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        // discovery cards waiting for their moment
        if (!KAART_WACHT.isEmpty()) {
            for (var it = KAART_WACHT.entrySet().iterator(); it.hasNext();) {
                var e = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p == null) {
                    it.remove();
                    continue;
                }
                e.getValue().removeIf(w -> {
                    if (--w[1] > 0) {
                        return false;
                    }
                    kaart(p, GrotData.ALLE.get((int) w[0]));
                    return true;
                });
                if (e.getValue().isEmpty()) {
                    it.remove();
                }
            }
        }
        if (sl.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            for (GrotData.Grot g : GrotData.ALLE) {
                BlockPos s = g.plek("schat");
                double dx = p.getX() - (s.getX() + 0.5), dz = p.getZ() - (s.getZ() + 0.5), dy = p.getY() - s.getY();
                double d2 = dx * dx + dz * dz;
                if (d2 > 14 * 14 || Math.abs(dy) > 8 || heeft(p, g, "schat")) {
                    continue;
                }
                sl.sendParticles(p, ParticleTypes.WAX_ON, false, false, s.getX() + 0.5, s.getY() + 0.8, s.getZ() + 0.5, 3, 0.4, 0.3, 0.4, 0.01);
                if (d2 <= 2.0 * 2.0 && Math.abs(dy) <= 2) {
                    schat(p, g, false);
                }
            }
        }
    }

    /** Walk up to the chest: the treasure, once, after the cave's puzzle. {@code dev} skips the puzzle check. Returns true when given. */
    public static boolean schat(ServerPlayer p, GrotData.Grot g, boolean dev) {
        if (heeft(p, g, "schat")) {
            return false;
        }
        if (!dev && !PuzzelSpeler.opgelost(p, puzzelId(g))) {
            CompoundTag d = data(p);
            long nu = p.level().getGameTime();
            if (nu - d.getLongOr(g.id() + ".slotmelding", -1000L) > 200) {
                d.putLong(g.id() + ".slotmelding", nu);
                p.sendSystemMessage(Component.translatable("vadsrijk.grotten.schat.slot").withStyle(ChatFormatting.GRAY));
                p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_LOCKED, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
            return false;
        }
        zet(p, g, "schat");
        BlockPos s = g.plek("schat");
        p.level().playSound(null, s, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1f, 1f);
        p.level().playSound(null, s, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
        RijkScenes.titel(p, Component.translatable("vadsrijk.grotten.schat.titel").withStyle(ChatFormatting.GOLD),
                Component.translatable("vadsrijk.grotten.schat.onder", Component.translatable("vadsrijk.grotten.naam." + g.id())), 70, 0xFFD24A);
        List<ItemStack> buit = new ArrayList<>();
        BasisType t = stukVoor(p, g.zone());
        if (t != null) {
            buit.add(Rollen.stack(Rollen.rol(t, g.eliteLv(), Zeldzaamheid.EPISCH, p.getRandom())));
        }
        LootBundel.laatVallen(p, Vec3.atCenterOf(s).add(0, 0.8, 0), new LootTabel.Resultaat(buit, 600L * g.zone() * g.zone() + 400), 2);
        return true;
    }

    // ---- Verkenningsboek ------------------------------------------------------------------------------------------------------

    static Categorie categorie(int zone) {
        List<Ding> l = new ArrayList<>();
        for (GrotData.Grot g : GrotData.ALLE) {
            if (g.zone() != zone) {
                continue;
            }
            String naam = "vadsrijk.grotten.naam." + g.id();
            l.add(new Ding("grot_" + g.id() + "_ontdekt", "vadsrijk.grotten.boek.ontdekt." + g.id(), "", "vadsrijk.grotten.boek.hint.ontdekt", naam,
                    false, p -> heeft(p, g, "ontdekt") ? Staat.KLAAR : Staat.NIETS, g.plek("ingang")));
            l.add(new Ding("grot_" + g.id() + "_elite", "vadsrijk.grotten.boek.elite." + g.id(), "", "vadsrijk.grotten.boek.hint.elite", naam,
                    false, p -> heeft(p, g, "elite") ? Staat.KLAAR : Staat.NIETS, g.plek("elite")));
            l.add(new Ding("grot_" + g.id() + "_puzzel", "vadsrijk.grotten.boek.puzzel." + g.id(), "", "vadsrijk.grotten.boek.hint.puzzel", naam,
                    false, p -> PuzzelSpeler.opgelost(p, puzzelId(g)) ? Staat.KLAAR : Staat.NIETS, g.plek("puzzel")));
            l.add(new Ding("grot_" + g.id() + "_schat", "vadsrijk.grotten.boek.schat." + g.id(), "", "vadsrijk.grotten.boek.hint.schat", naam,
                    false, p -> heeft(p, g, "schat") ? Staat.KLAAR : PuzzelSpeler.opgelost(p, puzzelId(g)) ? Staat.BEZIG : Staat.NIETS, g.plek("schat")));
            l.add(nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.boekDing(g.id(), g.plek("elite")));   // U-GROTKERKER: "grotbaas verslagen"
        }
        return new Categorie("grotten", "vadsrijk.grotten.boek.cat", "minecraft:glow_lichen", l);
    }

    // ---- dev commands ---------------------------------------------------------------------------------------------------------

    static void naar(ServerPlayer p, BlockPos b) {
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, java.util.Set.<Relative>of(),
                TeleportTransition.DO_NOTHING));
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("grot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("lijst").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (GrotData.Grot g : GrotData.ALLE) {
                BlockPos i = g.plek("ingang");
                p.sendSystemMessage(Component.literal("z" + g.zone() + " " + g.id() + " (lv " + g.minLv() + "-" + g.maxLv() + ", elite " + g.eliteLv() + ") "
                        + (heeft(p, g, "ontdekt") ? "ontdekt " : "") + (heeft(p, g, "elite") ? "elite " : "")
                        + (PuzzelSpeler.opgelost(p, puzzelId(g)) ? "puzzel " : "") + (heeft(p, g, "schat") ? "schat " : "")
                        + "@ " + i.getX() + " " + i.getY() + " " + i.getZ()).withStyle(ChatFormatting.YELLOW));
            }
            return 1;
        }));
        k.then(Commands.literal("tp").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(GrotData.ALLE.stream().map(GrotData.Grot::id), b))
                .executes(c -> tp(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"), "ingang"))
                .then(Commands.argument("plek", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("ingang", "binnen", "hal", "puzzel", "elite", "schat", "amb_1", "amb_2",
                                "uitgang"), b))
                        .executes(c -> tp(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "plek"))))));
        // U-GROTKERKER: was "reset" (that is the cave run now: grotkerker.GrotKerker)
        k.then(Commands.literal("wis").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(GrotData.ALLE.stream().map(GrotData.Grot::id), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GrotData.Grot g = grot(StringArgumentType.getString(c, "id"));
                    if (g == null) {
                        return 0;
                    }
                    for (String w : List.of("ontdekt", "elite", "schat", "slotmelding")) {
                        data(p).remove(g.id() + "." + w);
                    }
                    p.sendSystemMessage(Component.literal("Grot " + g.id() + " gereset (ontdekt/elite/schat; de puzzel reset je met /guhs vadsrijk puzzel reset "
                            + puzzelId(g) + ")").withStyle(ChatFormatting.GRAY));
                    return 1;
                })));
        k.then(Commands.literal("elite").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(GrotData.ALLE.stream().map(GrotData.Grot::id), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GrotData.Grot g = grot(StringArgumentType.getString(c, "id"));
                    ServerLevel sl = Vadsrijk.level(p.level().getServer());
                    if (g == null || sl == null) {
                        return 0;
                    }
                    VadsrijkMob m = nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.devElite(sl, g.id());   // U-GROTKERKER: the run's elite
                    p.sendSystemMessage(Component.literal(m == null ? "Geen plek gevonden (is de grot geladen?)" : "Elite gespawnd in " + g.id())
                            .withStyle(ChatFormatting.GRAY));
                    return 1;
                })));
        k.then(Commands.literal("kaart").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(GrotData.ALLE.stream().map(GrotData.Grot::id), b))
                .executes(c -> {
                    GrotData.Grot g = grot(StringArgumentType.getString(c, "id"));
                    if (g != null) {
                        kaart(c.getSource().getPlayerOrException(), g);
                    }
                    return 1;
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int tp(ServerPlayer p, String id, String plek) {
        GrotData.Grot g = grot(id);
        BlockPos b = g == null ? null : g.plek(plek);
        if (b == null) {
            p.sendSystemMessage(Component.literal("Onbekend (zie /guhs vadsrijk grot lijst)").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (plek.equals("puzzel")) {
            b = b.above(4);
        } else if (plek.startsWith("amb_")) {
            b = b.above(3);
        }
        if (!plek.equals("ingang") && !plek.equals("uitgang")) {
            nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij.devToegang(p, id);   // W-GROT: inside the closed cave = the dev is its group
        }
        naar(p, b);
        return 1;
    }
}
