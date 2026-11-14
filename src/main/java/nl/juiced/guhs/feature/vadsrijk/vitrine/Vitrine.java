package nl.juiced.guhs.feature.vadsrijk.vitrine;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.huizen.Kwartier;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;

/**
 * Y-VITRINE (round Y): the "Vadsrijk-vitrine", the one trophy block that bridges Het Vadsrijk and your survival base.
 * <ul>
 *   <li><b>What</b>: a glass display case ({@link VitrineBlock}) that shows its OWNER's (the placer's) Vadsrijk character: class +
 *       spec + level, the six zone badges (greyed until the zone's main story is done; "?" for zones the server has not opened yet,
 *       no names), raid clears, the date of the first completed zone, and a mini statue of the owner's mount (active one; else
 *       the highest-level land mount; else the active pet; else the Knuffelorde emblem).</li>
 *   <li><b>Data</b> ({@link #foto}): a small snapshot of the owner's {@link SpelerData}, kept in the block entity (saved + synced).
 *       Every {@link #VERVERS} ticks (staggered per block) the block re-reads it when the owner is online; offline it simply keeps
 *       the last snapshot. Display only: no item, no inventory, nothing can be taken out.</li>
 *   <li><b>Where</b>: only OUTSIDE Het Vadsrijk (and outside Het Knuffelkwartier, its housing dimension): {@link #magHier}.</li>
 *   <li><b>How you get it</b>: no recipe. It is the FTB reward of "Word rekruut van De Knuffelorde" (chapter "Het Vadsrijk",
 *       slice y-ftb / integration wires item {@code guhs:vr_vitrine}); ops: {@code /guhs vadsrijk vitrine geef}.</li>
 * </ul>
 * Assets + texts: tools/features/vadsrijk_y_vitrine.py. Dev: {@code /guhs vadsrijk vitrine ...} ({@link #commando}).
 */
public final class Vitrine {
    private Vitrine() {
    }

    public static final String ID = "vr_vitrine";
    public static final String K = "vadsrijk.vitrine.";
    /** The player-data slice (SpelerData.extra) where the date of the first completed zone is kept. */
    public static final String SLICE = "vitrine";
    /** How often (ticks) a vitrine re-reads its online owner. */
    public static final int VERVERS = 100;
    /** How often (ticks) every online player is checked for a first completed zone (only players without a date yet). */
    static final int EERSTE_CHECK = 600;
    /** Tests: owners that count as offline even though they are on the player list. */
    public static final Set<UUID> TEST_OFFLINE = ConcurrentHashMap.newKeySet();
    /** Tests: a level that counts as Het Vadsrijk (the gametest server has no Vadsrijk dimension). */
    @Nullable
    public static volatile Level TEST_RIJK;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<VitrineBlock> BLOK = BLOCKS.registerBlock(ID, VitrineBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 6));
    public static final DeferredItem<VitrineBlock.VitrineItem> ITEM = ITEMS.registerItem(ID, p -> new VitrineBlock.VitrineItem(BLOK.get(), p),
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VitrineBlockEntity>> BE = BLOCK_ENTITIES.register(ID,
            () -> new BlockEntityType<>(VitrineBlockEntity::new, BLOK.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == nl.juiced.guhs.registry.ModCreativeTabs.GUHS_TAB.getKey()) {
                e.accept(new ItemStack(ITEM.get()));
            }
        });
        nl.juiced.guhs.world.TikWacht.post("Vitrine:117", e -> eersteCheck(e.getServer()));
        NeoForge.EVENT_BUS.addListener(Vitrine::commando);
    }

    // ---- where --------------------------------------------------------------------------------------------------------------------

    /** May a vitrine stand in this level? Everywhere except Het Vadsrijk and Het Knuffelkwartier. */
    public static boolean magHier(@Nullable Level level) {
        return level != null && level != TEST_RIJK && !Vadsrijk.in(level) && level.dimension() != Kwartier.DIM;
    }

    // ---- progress -----------------------------------------------------------------------------------------------------------------

    /** The main quests of each zone (1..6; the raid's own quests don't count), filled once. */
    private static volatile List<List<String>> hoofd;

    static List<List<String>> hoofdQuests() {
        List<List<String>> h = hoofd;
        if (h == null || h.isEmpty()) {
            h = new ArrayList<>();
            for (int z = 0; z <= 6; z++) {
                h.add(new ArrayList<>());
            }
            for (Quest q : Quests.alle().values()) {
                if (q.hoofd() && !q.id().startsWith("z6raid")) {
                    int z = ZoneBoeken.zoneVan(q.id());
                    if (z >= 1 && z <= 6) {
                        h.get(z).add(q.id());
                    }
                }
            }
            if (!h.get(1).isEmpty()) {
                hoofd = h;   // (only cache once the quests are there)
            }
        }
        return h;
    }

    /** Is the main story of this zone done (every main quest of it handed in)? A zone without main quests never counts. */
    public static boolean zoneKlaar(ServerPlayer p, int zone) {
        List<String> l = hoofdQuests().get(Math.max(0, Math.min(6, zone)));
        if (zone < 1 || l.isEmpty()) {
            return false;
        }
        for (String id : l) {
            if (!Questlog.klaar(p, id)) {
                return false;
            }
        }
        return true;
    }

    /** Bit z-1 set = zone z done. */
    public static int zonesKlaar(ServerPlayer p) {
        int m = 0;
        for (int z = 1; z <= 6; z++) {
            if (zoneKlaar(p, z)) {
                m |= 1 << (z - 1);
            }
        }
        return m;
    }

    public static int raidClears(ServerPlayer p) {
        return Spelers.data(p).extra("z6raid").getIntOr("clears", 0);
    }

    /** Real time (ms) of the first completed zone as noticed by the game; 0 = none yet. Sets it when a zone is done now. */
    public static long eersteZone(ServerPlayer p, int zones) {
        CompoundTag t = Spelers.data(p).extra(SLICE);
        long e = t.getLongOr("eerste", 0L);
        if (e == 0L && zones != 0) {
            e = System.currentTimeMillis();
            t.putLong("eerste", e);
        }
        return e;
    }

    /** Every {@link #EERSTE_CHECK} ticks: online players without a date get one as soon as a zone is done (vitrine or not). */
    static void eersteCheck(MinecraftServer server) {
        if (server.getTickCount() % EERSTE_CHECK != 17) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Spelers.data(p).extra(SLICE).getLongOr("eerste", 0L) == 0L && zoneKlaar(p, 1)) {
                eersteZone(p, 1);
            }
        }
    }

    /**
     * The snapshot a vitrine keeps of its owner: naam, klasse (id), spec (id or ""), level (generic), klvl (class level), zones
     * (bit mask), raid, eerste (ms), beeld ("r" mount / "h" pet / "" emblem), soort (mount or pet id), kleur, uiterlijk.
     */
    public static CompoundTag foto(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        CompoundTag t = new CompoundTag();
        t.putString("naam", p.getGameProfile().name());
        t.putString("klasse", d.klasse);
        Spec s = Specs.spec(p, d.klasse);
        t.putString("spec", s == null ? "" : s.id);
        t.putInt("level", d.level());
        t.putInt("klvl", d.klasseLevel());
        int zones = zonesKlaar(p);
        t.putInt("zones", zones);
        t.putInt("raid", raidClears(p));
        t.putLong("eerste", eersteZone(p, zones));
        t.putBoolean("ooit", d.introGezien || d.generiekXp > 0 || zones != 0);
        RijdierSoort r = mooisteRijdier(p);
        Huisdier h = r == null ? Huisdieren.actief(p) : null;
        if (r != null) {
            t.putString("beeld", "r");
            t.putString("soort", r.id());
            t.putString("kleur", Rijdieren.kleur(p, r).id());
            t.putString("uiterlijk", nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.uiterlijk(p, r));
        } else if (h != null) {
            t.putString("beeld", "h");
            t.putString("soort", h.id());
        } else {
            t.putString("beeld", "");
        }
        return t;
    }

    /** The mount for the statue: the active one (a land mount), else the owned land mount with the highest level; null = none. */
    @Nullable
    public static RijdierSoort mooisteRijdier(ServerPlayer p) {
        RijdierSoort a = Rijdieren.actief(p);
        if (a != null && !a.water()) {
            return a;
        }
        RijdierSoort best = null;
        int lv = -1;
        for (RijdierSoort s : RijdierSoort.values()) {
            if (!s.water() && Rijdieren.heeft(p, s) && Rijdieren.level(p, s) > lv) {
                best = s;
                lv = Rijdieren.level(p, s);
            }
        }
        return best;
    }

    /** The owner when he is online now (tests: {@link #TEST_OFFLINE} hides him). */
    @Nullable
    static ServerPlayer online(MinecraftServer server, @Nullable UUID eigenaar) {
        if (eigenaar == null || TEST_OFFLINE.contains(eigenaar)) {
            return null;
        }
        return server.getPlayerList().getPlayer(eigenaar);
    }

    // ---- the chat summary (click) ---------------------------------------------------------------------------------------------------

    public static String datum(long ms) {
        return DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms));
    }

    public static Component klasseNaam(String klasse) {
        return Component.translatable(nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.ofRidder(klasse).naamKey());
    }

    /** The lines of the summary for whoever clicks: a short overview in Dutch. Closed zones are "?" without a name. */
    public static List<Component> samenvatting(CompoundTag f, boolean ownerOnline, int open) {
        List<Component> l = new ArrayList<>();
        String naam = f.getStringOr("naam", "?");
        l.add(Component.translatable(K + "chat.kop", naam).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (!f.getBooleanOr("ooit", false)) {
            l.add(Component.translatable(K + "chat.leeg", naam).withStyle(ChatFormatting.GRAY));
            return l;
        }
        String spec = f.getStringOr("spec", "");
        MutableComponent k = spec.isEmpty()
                ? Component.translatable(K + "klasse", klasseNaam(f.getStringOr("klasse", "knuffelridder")), f.getIntOr("level", 1))
                : Component.translatable(K + "klasse.spec", klasseNaam(f.getStringOr("klasse", "knuffelridder")),
                        Component.translatable("vadsrijk.klassen.spec." + spec), f.getIntOr("level", 1));
        l.add(k.withStyle(ChatFormatting.YELLOW));
        int zones = f.getIntOr("zones", 0);
        int n = Integer.bitCount(zones & ((1 << Math.max(0, Math.min(6, open))) - 1));
        l.add(Component.translatable(K + "chat.zones", n, Math.min(6, open)).withStyle(ChatFormatting.AQUA));
        for (int z = 1; z <= 6; z++) {
            boolean klaar = (zones & (1 << (z - 1))) != 0;
            if (z > open) {
                l.add(Component.translatable(K + "chat.zone.dicht", z).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                l.add(Component.translatable(K + (klaar ? "chat.zone.klaar" : "chat.zone.bezig"), z, ZonePoort.zoneNaam(z))
                        .withStyle(klaar ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        if (open >= 6) {
            l.add(Component.translatable(K + "raid", f.getIntOr("raid", 0)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        long eerste = f.getLongOr("eerste", 0L);
        if (eerste > 0) {
            l.add(Component.translatable(K + "eerste", datum(eerste)).withStyle(ChatFormatting.GRAY));
        }
        String beeld = f.getStringOr("beeld", "");
        if (beeld.equals("r")) {
            l.add(Component.translatable(K + "chat.beeld", Component.translatable("vadsrijk.economie.rijdier." + f.getStringOr("soort", "pluimguh")))
                    .withStyle(ChatFormatting.GRAY));
        } else if (beeld.equals("h")) {
            l.add(Component.translatable(K + "chat.beeld", Component.translatable("vadsrijk.huisdier." + f.getStringOr("soort", "") + ".naam"))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            l.add(Component.translatable(K + "chat.beeld.embleem").withStyle(ChatFormatting.GRAY));
        }
        if (!ownerOnline) {
            l.add(Component.translatable(K + "chat.offline", naam).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return l;
    }

    // ---- the floating panel ------------------------------------------------------------------------------------------------

    /** Zone badge colours (done), one per zone. */
    static final int[] ZONE_KLEUR = {0xFF9AD2, 0x9AE0FF, 0xB98CFF, 0xFFB45A, 0xA6F0FF, 0xFFD27A};
    static final String[] CIJFER = {"①", "②", "③", "④", "⑤", "⑥"};

    /** The panel lines of the renderer (built client side from the synced snapshot; zones above the gate are "?" without a name). */
    public static List<Component> bordRegels(CompoundTag f, int open) {
        List<Component> l = new ArrayList<>();
        if (f.isEmpty()) {
            l.add(Component.translatable(K + "bord.vrij").withStyle(ChatFormatting.GRAY));
            return l;
        }
        String naam = f.getStringOr("naam", "?");
        l.add(Component.translatable(K + "bord.titel", naam).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (!f.getBooleanOr("ooit", false)) {
            l.add(Component.translatable(K + "bord.leeg").withStyle(ChatFormatting.GRAY));
            return l;
        }
        String spec = f.getStringOr("spec", "");
        Component klasse = klasseNaam(f.getStringOr("klasse", "knuffelridder"));
        MutableComponent k = spec.isEmpty() ? Component.translatable(K + "klasse", klasse, f.getIntOr("level", 1))
                : Component.translatable(K + "klasse.spec", klasse, Component.translatable("vadsrijk.klassen.spec." + spec), f.getIntOr("level", 1));
        l.add(k.withStyle(ChatFormatting.YELLOW));
        l.add(badges(f.getIntOr("zones", 0), open));
        if (open >= 6) {
            l.add(Component.translatable(K + "raid", f.getIntOr("raid", 0)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        long eerste = f.getLongOr("eerste", 0L);
        if (eerste > 0) {
            l.add(Component.translatable(K + "eerste", datum(eerste)).withStyle(ChatFormatting.GRAY));
        }
        return l;
    }

    /** ①..⑥: coloured = done, dark grey = not yet, "?" = a zone the server has not opened (no name anywhere). */
    public static MutableComponent badges(int zones, int open) {
        MutableComponent c = Component.empty();
        for (int z = 1; z <= 6; z++) {
            if (z > 1) {
                c.append(Component.literal(" "));
            }
            if (z > open) {
                c.append(Component.literal("?").withStyle(s -> s.withColor(0x5A5060)));
            } else if ((zones & (1 << (z - 1))) != 0) {
                int kleur = ZONE_KLEUR[z - 1];
                c.append(Component.literal(CIJFER[z - 1]).withStyle(s -> s.withColor(kleur).withBold(true)));
            } else {
                c.append(Component.literal(CIJFER[z - 1]).withStyle(s -> s.withColor(0x6E6878)));
            }
        }
        return c;
    }

    // ---- dev command ------------------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> v = Commands.literal("vitrine").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource(), c.getSource().getPlayerOrException()));
        v.then(Commands.literal("geef").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            p.getInventory().add(new ItemStack(ITEM.get()));
            c.getSource().sendSuccess(() -> Component.literal("Vadsrijk-vitrine gegeven (guhs:vr_vitrine). Zet hem neer BUITEN Het Vadsrijk."), false);
            return 1;
        }));
        // fake progress: zone <n> = every main quest of zone n handed in (+ level to that zone's end, like zones zone1klaar)
        v.then(Commands.literal("zone").then(Commands.argument("n", IntegerArgumentType.integer(1, 6)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int z = IntegerArgumentType.getInteger(c, "n");
            int n = maakZoneKlaar(p, z);
            ververs(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Zone " + z + ": " + n + " hoofdquests op klaar gezet (dev). Vitrines verversen meteen."), false);
            return n;
        })));
        v.then(Commands.literal("raid").then(Commands.argument("n", IntegerArgumentType.integer(0, 999)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Spelers.data(p).extra("z6raid").putInt("clears", n);
            ververs(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal("Raid-clears: " + n + " (dev). Zichtbaar zodra alle 6 zones open zijn."), false);
            return n;
        })));
        v.then(Commands.literal("ververs").executes(c -> {
            int n = ververs(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal(n + " geladen vitrine(s) ververst."), false);
            return n;
        }));
        v.then(Commands.literal("wisdatum").executes(c -> {
            Spelers.data(c.getSource().getPlayerOrException()).extra(SLICE).remove("eerste");
            c.getSource().sendSuccess(() -> Component.literal("Datum 'eerste zone voltooid' gewist: hij komt opnieuw zodra een zone klaar is."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(v));
    }

    /** Dev/tests: hands in every main quest of a zone (no rewards loop: Questlog.voltooi as {@code zones zone1klaar} does). */
    public static int maakZoneKlaar(ServerPlayer p, int zone) {
        int n = 0;
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && !q.id().startsWith("z6raid") && ZoneBoeken.zoneVan(q.id()) == zone && !Questlog.klaar(p, q.id())) {
                Questlog.voltooi(p, q);
                n++;
            }
        }
        if (zone == 1) {
            Questlog.zetVlag(p, nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.ZONE1_VLAG, true);
        }
        Spelers.sync(p);
        return n;
    }

    /** Every loaded vitrine re-reads its owner now (dev). */
    public static int ververs(MinecraftServer server) {
        int n = 0;
        for (VitrineBlockEntity be : VitrineBlockEntity.GELADEN) {
            if (be.getLevel() instanceof ServerLevel && !be.isRemoved()) {
                be.ververs(true);
                n++;
            }
        }
        return n;
    }

    private static int status(CommandSourceStack s, ServerPlayer p) {
        CompoundTag f = foto(p);
        for (Component c : samenvatting(f, true, ZonePoort.open())) {
            s.sendSuccess(() -> c, false);
        }
        s.sendSuccess(() -> Component.literal("(dev: zone <n> | raid <n> | ververs | wisdatum | geef; zones open: " + ZonePoort.open() + ")"), false);
        return 1;
    }
}
