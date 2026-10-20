package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.safari.rit.RitData;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The moments of Safaripark Vadse Bergen (S3, design ronde 26), run by the server from {@link MomentSchema}:
 * <ul>
 *   <li>a notice to everybody in the park a minute before and at the start ("Over 1 minuut: voedertijd bij de pinguïns!");</li>
 *   <li>voedertijd: a ranger guh stands at the zone's feeding spot, the fed animals come to him, the shy animals of the zone
 *       stop hiding ({@link #voedertijd}: also the 3-star chance of the camera);</li>
 *   <li>rangerpraatje: a ranger at a truck stop tells four facts; whoever listened to the end gets a few Safarimunten;</li>
 *   <li>nachtsafari: the trucks carry lanterns ({@link #nachtsafari});</li>
 *   <li>dierenverjaardag: the birthday species of the real day gets a cake and garlands at its homes, and its photos pay
 *       double ({@link #muntFactor}).</li>
 * </ul>
 * The public static methods are the API for the other slices (S1's camera: {@link #noorderlicht}, {@link #muntFactor},
 * {@link #voedertijd}).
 */
public final class Momenten {
    public static final String RANGER_TAG = "guhs_safari_ranger";
    private static final int PRAATJE_ELKE = 200, PRAATJE_MUNTEN = 5, PRAATJE_DAGMAX = 10;
    /** Server: the ranger of each running moment ("voeder:zone" / "praatje:s") and who listened from the start. */
    private static final Map<String, UUID> RANGERS = new HashMap<>();
    private static final Map<String, Set<UUID>> LUISTERAARS = new HashMap<>();
    private static final Set<String> GEMELD = new HashSet<>();
    private static final Set<String> JARIG_GEMELD = new HashSet<>();

    // --- the API -----------------------------------------------------------------------------------------------------------------

    public static long dag(Level level) {
        return nl.juiced.guhs.world.GuhTime.day(level);
    }

    public static int tijd(Level level) {
        return (int) nl.juiced.guhs.world.GuhTime.timeOfDay(level);
    }

    /** Is it feeding time in this zone now (in the safari park)? */
    public static boolean voedertijd(Level level, String zone) {
        if (!Safari.echt(level)) {
            return false;
        }
        int v = MomentSchema.voederStart(zone);
        int t = tijd(level);
        return t >= v && t < v + MomentSchema.DUUR;
    }

    /** Are the lantern trucks out (a night safari night, in its hours)? */
    public static boolean nachtsafari(Level level) {
        return Safari.echt(level) && MomentSchema.nachtsafari(dag(level), tijd(level));
    }

    /** Northern lights over this spot now: the IJskust at night, on most nights (a photo with it gets a bonus star). */
    public static boolean noorderlicht(Level level, BlockPos pos) {
        if (!Safari.echt(level) || !MomentSchema.nacht(tijd(level)) || !MomentSchema.noorderlichtNacht(dag(level))) {
            return false;
        }
        int zone = ParkData.get().zone(pos.getX(), pos.getZ());
        return zone >= 0 && "ijskust".equals(ParkData.get().zoneNaam(zone));
    }

    /** The birthday species of today (real days), "" before the park is known. */
    public static String jarigeSoort() {
        List<String> soorten = new ArrayList<>();
        for (String s : ParkData.get().soorten.keySet()) {
            if (SafariSpeler.telt(s)) {
                soorten.add(s);
            }
        }
        return MomentSchema.jarig(Klok.dag(), soorten);
    }

    /** 2 for the birthday species of today (photos and coins pay double), else 1. */
    public static int muntFactor(MinecraftServer server, String soort) {
        return soort != null && soort.equals(jarigeSoort()) ? 2 : 1;
    }

    // --- the ticker --------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return;
        }
        verjaardag(level);
        if (level.players().isEmpty()) {
            return;
        }
        long dag = dag(level);
        int t = tijd(level);
        for (MomentSchema.Moment m : MomentSchema.dag(dag)) {
            String sleutel = m.soort() + ":" + m.zone() + ":" + m.van();
            if (t >= m.van() - MomentSchema.MELDING && t < m.van() - MomentSchema.MELDING + 20) {
                melding(level, dag + ":vooraf:" + sleutel, voorafTekst(m));
            }
            if (m.bezig(t)) {
                melding(level, dag + ":begin:" + sleutel, beginTekst(m));
                switch (m.soort()) {
                    case VOEDERTIJD -> voeren(level, m, t);
                    case PRAATJE -> praatje(level, m, t, dag);
                    default -> {
                    }
                }
            } else if (RANGERS.containsKey(sleutel)) {
                eindig(level, sleutel, m, dag);
            }
        }
        if (server.getTickCount() % 200 == 13) {
            opruimen(level);
        }
        if (GEMELD.size() > 512) {
            GEMELD.clear();
        }
    }

    private static void melding(ServerLevel level, String sleutel, @Nullable Component tekst) {
        if (tekst == null || !GEMELD.add(sleutel)) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(tekst);
        }
    }

    private static Component dieren(String zone) {
        return Component.translatable("gui.guhs.safari.voeder.dieren." + zone);
    }

    @Nullable
    private static Component voorafTekst(MomentSchema.Moment m) {
        return switch (m.soort()) {
            case VOEDERTIJD -> Component.translatable("gui.guhs.safari.moment.voeder_straks", dieren(m.zone()),
                    Component.translatable("gui.guhs.safari.zone." + m.zone())).withStyle(ChatFormatting.YELLOW);
            case PRAATJE -> Component.translatable("gui.guhs.safari.moment.praatje_straks",
                    Component.translatable("gui.guhs.safari.zone." + m.zone())).withStyle(ChatFormatting.YELLOW);
            case NACHTSAFARI -> Component.translatable("gui.guhs.safari.moment.nacht_straks").withStyle(ChatFormatting.LIGHT_PURPLE);
        };
    }

    @Nullable
    private static Component beginTekst(MomentSchema.Moment m) {
        return switch (m.soort()) {
            case VOEDERTIJD -> Component.translatable("gui.guhs.safari.moment.voeder_nu", dieren(m.zone()),
                    Component.translatable("gui.guhs.safari.zone." + m.zone())).withStyle(ChatFormatting.GOLD);
            case PRAATJE -> Component.translatable("gui.guhs.safari.moment.praatje_nu",
                    Component.translatable("gui.guhs.safari.zone." + m.zone())).withStyle(ChatFormatting.GOLD);
            case NACHTSAFARI -> Component.translatable("gui.guhs.safari.moment.nacht_nu").withStyle(ChatFormatting.LIGHT_PURPLE);
        };
    }

    // --- voedertijd --------------------------------------------------------------------------------------------------------------

    @Nullable
    private static ParkData.Plek voederPlek(String zone) {
        String soort = MomentSchema.VOEDER_SOORT.get(zone);
        ParkData.Plek best = null;
        for (ParkData.Plek p : ParkData.get().plekken) {
            if (p.soort().equals(soort) && p.zone().equals(zone) && (best == null || p.id() < best.id())) {
                best = p;
            }
        }
        return best;
    }

    private static void voeren(ServerLevel level, MomentSchema.Moment m, int t) {
        ParkData.Plek plek = voederPlek(m.zone());
        if (plek == null) {
            return;
        }
        String sleutel = m.soort() + ":" + m.zone() + ":" + m.van();
        BlockPos c = plek.centrum();
        if (!level.isLoaded(c)) {
            return;
        }
        GuhNpcEntity ranger = ranger(level, sleutel, grond(level, c.offset(plek.water() ? 0 : 2, 0, 0)), "gui.guhs.safari.ranger.voeder");
        if (ranger == null) {
            return;
        }
        // the fed animals come to the ranger, happy
        String soort = MomentSchema.VOEDER_SOORT.get(m.zone());
        for (SafariDier d : level.getEntitiesOfClass(SafariDier.class, new AABB(c).inflate(plek.straal() + 12, 12, plek.straal() + 12),
                d -> d.soort().equals(soort))) {
            if (d.distanceToSqr(ranger) > 9 && level.getGameTime() % 40 < 20) {
                d.getNavigation().moveTo(ranger.getX(), ranger.getY(), ranger.getZ(), 1.1);
            } else if (d.distanceToSqr(ranger) <= 9 && level.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, d.getX(), d.getY() + d.getBbHeight() + 0.3, d.getZ(), 2, 0.3, 0.2, 0.3, 0);
            }
        }
    }

    // --- rangerpraatje -----------------------------------------------------------------------------------------------------------

    @Nullable
    private static BlockPos haltePlek(ServerLevel level, String zone) {
        RitData d = RitData.get();
        if (d.route == null) {
            return null;
        }
        for (RitData.Halte h : d.haltes) {
            if (h.zone().equals(zone)) {
                Vec3 p = d.route.op(h.afstand());
                Vec3 opzij = d.route.op(h.afstand() + 2).subtract(p).normalize().yRot((float) Math.PI / 2).scale(3);
                return grond(level, BlockPos.containing(p.add(opzij)));
            }
        }
        return null;
    }

    private static void praatje(ServerLevel level, MomentSchema.Moment m, int t, long dag) {
        String sleutel = m.soort() + ":" + m.zone() + ":" + m.van();
        BlockPos plek = haltePlek(level, m.zone());
        if (plek == null || !level.isLoaded(plek)) {
            return;
        }
        GuhNpcEntity ranger = ranger(level, sleutel, plek, "gui.guhs.safari.ranger.praatje");
        if (ranger == null) {
            return;
        }
        int verstreken = t - m.van();
        Set<UUID> luisteraars = LUISTERAARS.computeIfAbsent(sleutel, k -> new HashSet<>());
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(ranger) < 14 * 14 && verstreken < MomentSchema.DUUR / 3) {
                luisteraars.add(p.getUUID());
            }
        }
        int regel = verstreken / PRAATJE_ELKE;
        if (regel < 4 && verstreken % PRAATJE_ELKE < 20) {
            Component tekst = Component.translatable("gui.guhs.safari.ranger.zegt", Component.translatable("gui.guhs.safari.feit." + m.zone() + "." + regel))
                    .withStyle(ChatFormatting.GREEN);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(ranger) < 16 * 16) {
                    p.sendSystemMessage(tekst);
                }
            }
        }
    }

    private static void eindig(ServerLevel level, String sleutel, MomentSchema.Moment m, long dag) {
        UUID id = RANGERS.remove(sleutel);
        Entity e = id == null ? null : level.getEntity(id);
        if (m.soort() == MomentSchema.Soort.PRAATJE) {
            Set<UUID> luisteraars = LUISTERAARS.remove(sleutel);
            if (luisteraars != null && e != null) {
                for (ServerPlayer p : level.players()) {
                    if (luisteraars.contains(p.getUUID()) && p.distanceToSqr(e) < 16 * 16) {
                        int n = SafariMunten.verdienDagelijks(p, "praatje", PRAATJE_MUNTEN, PRAATJE_DAGMAX, "praatje");
                        p.sendSystemMessage(Component.translatable(n > 0 ? "gui.guhs.safari.ranger.bedankt" : "gui.guhs.safari.ranger.bedankt_vol")
                                .withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }
        if (e != null) {
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            e.discard();
        }
    }

    @Nullable
    private static GuhNpcEntity ranger(ServerLevel level, String sleutel, BlockPos plek, String naam) {
        UUID id = RANGERS.get(sleutel);
        if (id != null && level.getEntity(id) instanceof GuhNpcEntity g && !g.isRemoved()) {
            return g;
        }
        GuhNpcEntity g = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.setKind(GuhNpcEntity.Kind.BOSWACHTERGUH);
        g.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        g.setCustomName(Component.translatable(naam));
        g.setCustomNameVisible(true);
        g.addTag(RANGER_TAG);
        level.addFreshEntity(g);
        level.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 0.5, g.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        RANGERS.put(sleutel, g.getUUID());
        return g;
    }

    /** Rangers of moments that are over (a restart in the middle of one) go home. */
    private static void opruimen(ServerLevel level) {
        Set<UUID> nu = new HashSet<>(RANGERS.values());
        for (Entity e : level.getAllEntities()) {
            if (e instanceof GuhNpcEntity && e.entityTags().contains(RANGER_TAG) && !nu.contains(e.getUUID())) {
                e.discard();
            }
        }
    }

    private static BlockPos grond(ServerLevel level, BlockPos p) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
    }

    /** A ranger of a moment only talks: no screens, no trading. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget().entityTags().contains(RANGER_TAG)) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide() && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                event.getEntity().sendSystemMessage(Component.translatable("gui.guhs.safari.ranger.hoi").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // --- dierenverjaardag --------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getTo() == Safari.DIM) {
            String soort = jarigeSoort();
            if (!soort.isEmpty() && JARIG_GEMELD.add(p.getUUID() + ":" + Klok.dag())) {
                p.sendSystemMessage(Component.translatable("gui.guhs.safari.moment.jarig", Component.translatable("entity.guhs.safari." + soort))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Once per real day: the birthday decorations move to the new birthday species' homes. */
    private static void verjaardag(ServerLevel level) {
        Opslag o = Opslag.get(level.getServer());
        long vandaag = Klok.dag();
        if (o.dag == vandaag) {
            return;
        }
        Block taart = blok("guhs:guh_taart", Blocks.CAKE);
        Block slinger = blok("guhs:feestslingers", null);
        // away with yesterday's (only what is still ours)
        for (long l : o.blokken) {
            BlockPos b = BlockPos.of(l);
            if (level.isLoaded(b)) {
                BlockState s = level.getBlockState(b);
                if (s.is(taart) || (slinger != null && s.is(slinger)) || s.is(Blocks.DARK_OAK_FENCE)) {
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        List<Long> nieuw = new ArrayList<>();
        String soort = jarigeSoort();
        int n = 0;
        for (ParkData.Plek p : ParkData.get().plekken) {
            if (!p.soort().equals(soort) || p.water() || n >= 3) {
                continue;
            }
            BlockPos c = p.centrum();
            if (!level.isLoaded(c)) {
                continue;
            }
            BlockPos g = grond(level, c.offset(1, 0, 1));
            if (!level.getBlockState(g).isAir() || !level.getBlockState(g.below()).isFaceSturdy(level, g.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(g).isEmpty()) {
                continue;
            }
            level.setBlock(g, taart.defaultBlockState(), Block.UPDATE_CLIENTS);
            nieuw.add(g.asLong());
            if (slinger != null) {
                for (int dx : new int[]{-2, 2}) {
                    BlockPos paal = grond(level, g.offset(dx, 0, 0));
                    if (level.getBlockState(paal).isAir() && level.getBlockState(paal.above()).isAir() && Math.abs(paal.getY() - g.getY()) <= 1) {
                        level.setBlock(paal, Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        level.setBlock(paal.above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
                        nieuw.add(paal.asLong());
                        nieuw.add(paal.above().asLong());
                    }
                }
                for (int dx = -1; dx <= 1; dx++) {
                    BlockPos s = g.offset(dx, 1, 0);
                    if (level.getBlockState(s).isAir()) {
                        BlockState st = slinger.defaultBlockState();
                        level.setBlock(s, st, Block.UPDATE_CLIENTS);
                        nieuw.add(s.asLong());
                    }
                }
            }
            n++;
        }
        if (n == 0) {
            return;          // its homes not loaded yet: try again later today
        }
        o.dag = vandaag;
        o.blokken = nieuw;
        o.setDirty();
    }

    @Nullable
    private static Block blok(String id, @Nullable Block anders) {
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(anders);
    }

    // --- the schemabord --------------------------------------------------------------------------------------------------------

    /** Today's plan as chat text (the schemabord, /guhs safari schema). */
    public static Component schema(Level level) {
        long dag = dag(level);
        int t = tijd(level);
        MutableComponent uit = Component.translatable("gui.guhs.safari.schema.titel", MomentSchema.klok(t)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (MomentSchema.Moment m : MomentSchema.dag(dag)) {
            String status = m.bezig(t) ? "nu" : t >= m.tot() ? "geweest" : "straks";
            Component wat = switch (m.soort()) {
                case VOEDERTIJD -> Component.translatable("gui.guhs.safari.schema.voeder", dieren(m.zone()), Component.translatable("gui.guhs.safari.zone." + m.zone()));
                case PRAATJE -> Component.translatable("gui.guhs.safari.schema.praatje", Component.translatable("gui.guhs.safari.zone." + m.zone()));
                case NACHTSAFARI -> Component.translatable("gui.guhs.safari.schema.nacht");
            };
            ChatFormatting kleur = status.equals("nu") ? ChatFormatting.GREEN : status.equals("geweest") ? ChatFormatting.GRAY : ChatFormatting.WHITE;
            uit.append("\n").append(Component.literal(MomentSchema.klok(m.van()) + "  ").withStyle(kleur)).append(wat.copy().withStyle(kleur))
                    .append(Component.translatable("gui.guhs.safari.schema." + status).withStyle(kleur, ChatFormatting.ITALIC));
        }
        String soort = jarigeSoort();
        if (!soort.isEmpty()) {
            uit.append("\n").append(Component.translatable("gui.guhs.safari.schema.jarig", Component.translatable("entity.guhs.safari." + soort))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return uit;
    }

    // --- storage -------------------------------------------------------------------------------------------------------------------

    /** The birthday decorations of the current real day (SavedData {@code guhs:safari_momenten}). */
    static final class Opslag extends SavedData {
        static final SavedDataType<Opslag> TYPE = GuhSavedData.tagType("safari_momenten", Opslag::new, Opslag::load, Opslag::save);
        long dag = Long.MIN_VALUE;
        List<Long> blokken = new ArrayList<>();

        static Opslag get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("Dag", dag);
            ListTag l = new ListTag();
            for (long b : blokken) {
                l.add(LongTag.valueOf(b));
            }
            t.put("Blokken", l);
            return t;
        }

        private static Opslag load(CompoundTag t) {
            Opslag o = new Opslag();
            o.dag = t.getLongOr("Dag", Long.MIN_VALUE);
            for (net.minecraft.nbt.Tag b : t.getListOrEmpty("Blokken")) {
                if (b instanceof LongTag lt) {
                    o.blokken.add(lt.longValue());
                }
            }
            return o;
        }
    }

    private Momenten() {
    }
}
