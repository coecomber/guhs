package nl.juiced.guhs.feature.vadsrijk.vriendjes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Vahoegnir;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * N-EXTRA: the cheered-up (pink) Vahoegnir draws sweet critters. While a pink dragon is in the Vadsrijk with a player within
 * {@link #KIJKER} blocks, up to {@link #MAX} critters gather within ~{@link #STRAAL} blocks: pluisvinkjes and kaasmeesjes (fluttering
 * around him, hopping and pecking on the ground, perched on his back, neck and tail), pluiseekhoorntjes, a few guh-konijntjes,
 * pink pieppiepmuisjes and knabbelvlindertjes. They reuse the existing Guhs critter models as server-moved {@link VriendjeEntity} props
 * (no AI, no hitbox, never hurt, never saved). When the dragon is gone (or nobody is near) the birds and butterflies fly off and
 * the others hop away with a little poof.
 * <p>Pink = the zone-3 boss {@link Vahoegnir} with all 5 splinters back (his ending scene: from splinter 5 until he flies off along
 * the beam), the resting/visiting {@link RozeDraak} while he is on the ground ({@link RozeDraakBeheer}; critters are then only
 * sent to the players who see him), or any entity a later scene hands to {@link #volg}.
 */
public final class DraakVriendjes {
    /** Critters live within this radius around the dragon. */
    public static final double STRAAL = 25;
    /** Someone must be this close to the dragon for critters to come (and stay). */
    public static final double KIJKER = 48;
    public static final int MAX = 32;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<VriendjeEntity>> VRIENDJE = ENTITIES.register("vr_draakvriendje",
            () -> EntityType.Builder.<VriendjeEntity>of(VriendjeEntity::new, MobCategory.MISC).sized(0.4f, 0.4f).noSave().fireImmune()
                    .clientTrackingRange(6).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_draakvriendje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<RozeDraak>> ROZE_DRAAK = ENTITIES.register("vr_roze_draak",
            () -> EntityType.Builder.<RozeDraak>of(RozeDraak::new, MobCategory.MISC).sized(4.0f, 3.6f).noSave().fireImmune()
                    .clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_roze_draak"))));

    /** Extra pink dragons (later scenes). */
    private static final Set<Entity> EXTRA = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Entity, Groep> GROEPEN = new IdentityHashMap<>();

    private DraakVriendjes() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(DraakVriendjes::opTick);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            GROEPEN.clear();
            EXTRA.clear();
            RozeDraakBeheer.wis();
        });
        NeoForge.EVENT_BUS.addListener(DraakVriendjes::commando);
    }

    /** {@code /guhs vadsrijk vriendjes draak nest|bezoek|timer}: the resting dragon (teleports you to the nest edge), a visit now. */
    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("vriendjes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("draak").then(Commands.literal("nest").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel rijk = Vadsrijk.level(c.getSource().getServer());
            BlockPos b = RozeDraakBeheer.nestPlek();
            if (rijk == null || b == null) {
                c.getSource().sendFailure(Component.literal("Geen nest-POI gevonden"));
                return 0;
            }
            RozeDraakBeheer.DEV_ZIEN.add(p.getUUID());
            BlockPos rand = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek(RozeDraakBeheer.NEST_RAND_POI);
            BlockPos naar = rand != null ? rand : b.offset(0, 0, 14);
            p.teleportTo(rijk, naar.getX() + 0.5, naar.getY() + 1, naar.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
            c.getSource().sendSuccess(() -> Component.literal("Roze Vahoegnir rust in het nest (jij ziet hem nu ook zonder zone3_klaar)"), false);
            return 1;
        })).then(Commands.literal("bezoek").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            RozeDraakBeheer.DEV_ZIEN.add(p.getUUID());
            RozeDraak d = RozeDraakBeheer.bezoek(p);
            if (d == null) {
                c.getSource().sendFailure(Component.literal("Geen veilige landingsplek hier (ga naar een plein of kampvuur in zone 1/2)"));
                return 0;
            }
            c.getSource().sendSuccess(() -> Component.literal("Vahoegnir komt op bezoek: landt bij " + BlockPos.containing(d.plek).toShortString())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return 1;
        })).then(Commands.literal("timer").executes(c -> {
            RozeDraakBeheer.bezoekNu(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal("Volgende bezoek: zo meteen (als het mag)"), false);
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }

    /** A later scene: this entity is a pink dragon (critters gather while it exists and players are near). */
    public static void volg(Entity draak) {
        EXTRA.add(draak);
    }

    /** Is this a cheered-up (pink) dragon? */
    public static boolean roze(Entity e) {
        if (e.isRemoved()) {
            return false;
        }
        if (e instanceof RozeDraak r) {
            return r.opGrond();
        }
        return e instanceof Vahoegnir v ? v.splinters() >= 5 : EXTRA.contains(e);
    }

    /** Gametests / dev: how many critters are around this dragon now. */
    public static int aantal(Entity draak) {
        Groep g = GROEPEN.get(draak);
        return g == null ? 0 : g.vriendjes.size();
    }

    /** Gametests / dev: the critters around this dragon. */
    public static List<VriendjeEntity> rond(Entity draak) {
        Groep g = GROEPEN.get(draak);
        List<VriendjeEntity> l = new ArrayList<>();
        if (g != null) {
            for (Vriendje v : g.vriendjes) {
                l.add(v.f);
            }
        }
        return l;
    }

    private static void opTick(ServerTickEvent.Post e) {
        tick(e.getServer());
        RozeDraakBeheer.tick(e.getServer());
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            zoek(server);
        }
        if (GROEPEN.isEmpty()) {
            return;
        }
        Iterator<Groep> it = GROEPEN.values().iterator();
        while (it.hasNext()) {
            if (it.next().tick()) {
                it.remove();
            }
        }
    }

    private static void zoek(MinecraftServer server) {
        ServerLevel rijk = Vadsrijk.level(server);
        if (rijk != null) {
            for (ServerPlayer p : rijk.players()) {
                for (Vahoegnir v : rijk.getEntitiesOfClass(Vahoegnir.class, p.getBoundingBox().inflate(KIJKER), DraakVriendjes::roze)) {
                    begin(v);
                }
            }
        }
        for (Entity d : new ArrayList<>(EXTRA)) {
            if (d.isRemoved()) {
                EXTRA.remove(d);
            } else if (d.level() instanceof ServerLevel && kijkers(d)) {
                begin(d);
            }
        }
    }

    private static void begin(Entity draak) {
        if (!GROEPEN.containsKey(draak) && kijkers(draak)) {
            GROEPEN.put(draak, new Groep(draak));
        }
    }

    static boolean kijkers(Entity draak) {
        for (var p : draak.level().players()) {
            if (!p.isRemoved() && p.distanceToSqr(draak) < KIJKER * KIJKER
                    && (!(draak instanceof RozeDraak r) || (p instanceof ServerPlayer sp && r.broadcastToPlayer(sp)))) {
                return true;
            }
        }
        return false;
    }

    // ---- the kinds ------------------------------------------------------------------------------------------------------------

    /** A critter kind: model, textures, animation file and its animation names. Birds and butterflies fly. */
    enum Soort {
        VINK("pluisvinkje", "pluisvinkje", new String[] {"textures/entity/pluisvinkje.png"}, "animation.pluisvinkje.idle",
                "animation.pluisvinkje.hop", "animation.pluisvinkje.fly", new String[] {"animation.pluisvinkje.peck", "animation.pluisvinkje.blij"}, true, 1f),
        MEES("kaasmeesje", "kaasmeesje", new String[] {"textures/entity/kaasmeesje.png"}, "animation.kaasmeesje.idle",
                "animation.kaasmeesje.hop", "animation.kaasmeesje.fly", new String[] {"animation.kaasmeesje.peck"}, true, 1f),
        EEKHOORN("pluiseekhoorntje", "pluiseekhoorntje", new String[] {"textures/entity/pluiseekhoorntje.png"}, "idle", "walk", null,
                new String[] {"zit", "eet", "blij", "graaf"}, false, 1f),
        KONIJN("guh_konijntje", "guh_konijntje", new String[] {"textures/entity/guh_konijntje_roze.png", "textures/entity/guh_konijntje_wit.png",
                "textures/entity/guh_konijntje.png", "textures/entity/guh_konijntje_choco.png"}, "idle", "walk", null,
                new String[] {"zit", "eet", "blij", "binky"}, false, 1f),
        MUIS("pieppiepmuisje", "pieppiepmuisje", new String[] {"textures/vadsrijk/vriendjes/muisje_roze.png"}, "idle", "walk", null,
                new String[] {"zit", "eet", "blij", "piep"}, false, 1f),
        VLINDER("knabbelvlindertje", "knabbelvlindertje", new String[] {"textures/entity/knabbelvlindertje_roze.png",
                "textures/entity/knabbelvlindertje_lila.png", "textures/entity/knabbelvlindertje_mint.png",
                "textures/entity/knabbelvlindertje_kaasgeel.png"}, "zit", "fly", "fly", new String[] {"zit"}, true, 1f);

        final String model, anims, stil, loop, vlieg;
        final String[] texturen, bezig;
        final boolean vliegt;
        final float schaal;

        Soort(String model, String anims, String[] texturen, String stil, String loop, @Nullable String vlieg, String[] bezig, boolean vliegt, float schaal) {
            this.model = model;
            this.anims = anims;
            this.texturen = texturen;
            this.stil = stil;
            this.loop = loop;
            this.vlieg = vlieg;
            this.bezig = bezig;
            this.vliegt = vliegt;
            this.schaal = schaal;
        }

        boolean vogel() {
            return this == VINK || this == MEES;
        }
    }

    /** Who comes, in order (the first ones arrive first): mostly birds, squirrels and butterflies, a few bunnies and mice. */
    private static final Soort[] VOLGORDE = {
            Soort.VINK, Soort.EEKHOORN, Soort.VLINDER, Soort.MEES, Soort.VINK, Soort.EEKHOORN, Soort.VLINDER, Soort.KONIJN,
            Soort.VINK, Soort.MUIS, Soort.MEES, Soort.VLINDER, Soort.EEKHOORN, Soort.VINK, Soort.VLINDER, Soort.KONIJN,
            Soort.MEES, Soort.EEKHOORN, Soort.MUIS, Soort.VINK, Soort.VLINDER, Soort.MEES, Soort.EEKHOORN, Soort.VINK,
            Soort.KONIJN, Soort.VLINDER, Soort.MUIS, Soort.MEES, Soort.EEKHOORN, Soort.VINK, Soort.VLINDER, Soort.VINK};

    /**
     * Perch spots on the dragon (side, up, forward; blocks, for the o-draak model x1.6 standing, feet at y 0, 0.1 block per
     * model px): back, neck root, tail. Small critters sit there; birds flutter on and off. The wings stand in a V: no perches.
     */
    private static final double[][] ZITPLEK = {
            {0, 3.55, 0.2}, {0.55, 3.45, -0.7}, {-0.55, 3.45, 0.9}, {0, 3.75, 1.2},
            {0, 2.95, -2.2}, {0, 2.65, -3.4}, {0, 2.45, -4.6}, {0, 2.15, -5.7}};

    enum Rol {
        GROND, ZIT, FLADDER, NAAR, WEG
    }

    // ---- one dragon's crowd ------------------------------------------------------------------------------------------------------

    static final class Groep {
        final Entity draak;
        final List<Vriendje> vriendjes = new ArrayList<>();
        final boolean[] bezet = new boolean[ZITPLEK.length];
        int t, volgende;
        boolean weg;
        Vec3 vorige;
        double snelheid;

        Groep(Entity draak) {
            this.draak = draak;
            this.vorige = draak.position();
        }

        ServerLevel sl() {
            return (ServerLevel) draak.level();
        }

        RandomSource r() {
            return draak.level().getRandom();
        }

        /** World position of perch i (follows the dragon's turn). */
        Vec3 zitplek(int i) {
            double[] z = ZITPLEK[i];
            float yaw = draak instanceof net.minecraft.world.entity.LivingEntity l ? l.yBodyRot : draak.getYRot();
            double rad = Math.toRadians(yaw);
            Vec3 voor = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
            Vec3 zij = new Vec3(Math.cos(rad), 0, Math.sin(rad));
            return draak.position().add(voor.scale(z[2])).add(zij.scale(z[0])).add(0, z[1], 0);
        }

        /** A free ground spot (solid below, room above, no liquid) between 3 and STRAAL blocks from the dragon, or near {@code bij}. */
        @Nullable
        Vec3 grondplek(@Nullable Vec3 bij, double straal) {
            ServerLevel sl = sl();
            Vec3 m = bij != null ? bij : draak.position();
            for (int poging = 0; poging < 10; poging++) {
                double a = r().nextDouble() * Math.PI * 2;
                double d = bij != null ? 0.8 + r().nextDouble() * straal : 3 + r().nextDouble() * (straal - 3);
                double x = m.x + Math.cos(a) * d, z = m.z + Math.sin(a) * d;
                if (Math.hypot(x - draak.getX(), z - draak.getZ()) > STRAAL) {
                    continue;
                }
                if (draak.getBoundingBox().inflate(0.5, 2, 0.5).contains(x, draak.getY() + 0.5, z)) {
                    continue;
                }
                int top = Mth.floor(m.y) + 6, bodem = Mth.floor(m.y) - 14;
                for (int y = top; y >= bodem; y--) {
                    BlockPos b = BlockPos.containing(x, y, z);
                    BlockState s = sl.getBlockState(b);
                    if (!s.getCollisionShape(sl, b).isEmpty()) {
                        BlockPos boven = b.above();
                        if (s.getFluidState().isEmpty() && sl.getBlockState(boven).getCollisionShape(sl, boven).isEmpty()
                                && sl.getBlockState(boven.above()).getCollisionShape(sl, boven.above()).isEmpty()
                                && sl.getFluidState(boven).isEmpty()) {
                            double topY = s.getCollisionShape(sl, b).max(net.minecraft.core.Direction.Axis.Y);
                            return new Vec3(x, y + topY, z);
                        }
                        break;
                    }
                }
            }
            return null;
        }

        int vrijeZitplek() {
            List<Integer> vrij = new ArrayList<>();
            for (int i = 0; i < bezet.length; i++) {
                if (!bezet[i]) {
                    vrij.add(i);
                }
            }
            return vrij.isEmpty() ? -1 : vrij.get(r().nextInt(vrij.size()));
        }

        /** A point in the air around the dragon (fluttering). */
        Vec3 luchtplek() {
            double a = r().nextDouble() * Math.PI * 2, d = 3 + r().nextDouble() * 9;
            return draak.position().add(Math.cos(a) * d, 2.5 + r().nextDouble() * 5, Math.sin(a) * d);
        }

        /** True = done (all gone). */
        boolean tick() {
            t++;
            boolean levend = roze(draak) && draak.level() instanceof ServerLevel;
            if (!weg && (!levend || (t % 20 == 0 && !kijkers(draak)))) {
                weg = true;
                for (Vriendje v : vriendjes) {
                    v.vertrek(this);
                }
            }
            if (levend) {
                snelheid = draak.position().distanceTo(vorige);
                vorige = draak.position();
            }
            if (!weg && vriendjes.size() < MAX && t >= volgende) {
                komt(VOLGORDE[vriendjes.size() % VOLGORDE.length]);
                volgende = t + 3 + r().nextInt(6);
            }
            vriendjes.removeIf(v -> !v.tick(this));
            return weg && vriendjes.isEmpty();
        }

        /** A new critter arrives: birds and butterflies fly in from afar, the others pop out of the nest with a little puff. */
        void komt(Soort s) {
            ServerLevel sl = sl();
            String tex = s.texturen[r().nextInt(s.texturen.length)];
            Vriendje v;
            if (s.vliegt) {
                double a = r().nextDouble() * Math.PI * 2;
                Vec3 van = draak.position().add(Math.cos(a) * (STRAAL + 6), 8 + r().nextDouble() * 6, Math.sin(a) * (STRAAL + 6));
                VriendjeEntity f = VriendjeEntity.maak(sl, draak, van, 0, s.model, tex, s.anims, s.vlieg, s.schaal);
                if (f == null) {
                    return;
                }
                v = new Vriendje(s, f);
                // birds land: on him (if there's room) or on the ground; butterflies flutter
                int zit = s.vogel() && r().nextInt(3) == 0 ? vrijeZitplek() : -1;
                if (zit >= 0) {
                    v.naarZit(this, zit);
                } else if (s.vogel() && r().nextBoolean()) {
                    Vec3 g = grondplek(null, STRAAL);
                    if (g != null) {
                        v.naar(this, g, Rol.GROND, 0.35, 0.5);
                    } else {
                        v.fladder(this);
                    }
                } else {
                    v.fladder(this);
                }
            } else {
                // a few small ones sit on his back right away
                int zit = (s == Soort.MUIS || s == Soort.EEKHOORN) && r().nextInt(4) == 0 ? vrijeZitplek() : -1;
                Vec3 g = zit >= 0 ? zitplek(zit) : grondplek(null, STRAAL);
                if (g == null) {
                    return;
                }
                VriendjeEntity f = VriendjeEntity.maak(sl, draak, g, r().nextFloat() * 360, s.model, tex, s.anims, s.stil, s.schaal);
                if (f == null) {
                    return;
                }
                v = new Vriendje(s, f);
                if (zit >= 0) {
                    v.rol = Rol.ZIT;
                    v.zit = zit;
                    bezet[zit] = true;
                } else {
                    v.rol = Rol.GROND;
                }
                v.timer = 20 + r().nextInt(60);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, g.x, g.y + 0.3, g.z, 4, 0.2, 0.1, 0.2, 0.01);
                sl.sendParticles(ParticleTypes.HEART, g.x, g.y + 0.6, g.z, 1, 0.1, 0.1, 0.1, 0);
            }
            vriendjes.add(v);
        }
    }

    // ---- one critter ---------------------------------------------------------------------------------------------------------------

    static final class Vriendje {
        final Soort s;
        final VriendjeEntity f;
        Rol rol = Rol.GROND;
        /** Ticks until it decides something new. */
        int timer;
        /** Its perch (ZIT, or the perch it flies to), else -1. */
        int zit = -1;
        // a move: from, to (or a perch), progress, arc height; then this role
        Vec3 van, naar;
        int duur, k;
        double boog;
        Rol daarna = Rol.GROND;
        // fluttering: a little circle around a point
        Vec3 fladderMidden;
        double hoek, fladderStraal;

        Vriendje(Soort s, VriendjeEntity f) {
            this.s = s;
            this.f = f;
        }

        void naar(Groep g, Vec3 doel, Rol daarna, double perTick, double boog) {
            this.van = f.position();
            this.naar = doel;
            this.duur = Math.max(4, (int) Math.ceil(van.distanceTo(doel) / perTick));
            this.k = 0;
            this.boog = boog;
            this.daarna = daarna;
            this.rol = Rol.NAAR;
            f.anim(s.vliegt && (daarna != Rol.GROND || boog > 0.45 || s == Soort.VLINDER) && s.vlieg != null ? s.vlieg : s.loop);
        }

        void naarZit(Groep g, int i) {
            zit = i;
            g.bezet[i] = true;
            naar(g, g.zitplek(i), Rol.ZIT, 0.3, 0.8);
            f.anim(s.vlieg != null ? s.vlieg : s.loop);
        }

        void fladder(Groep g) {
            Vec3 p = g.luchtplek();
            fladderMidden = p;
            fladderStraal = 1 + g.r().nextDouble() * 3;
            hoek = g.r().nextDouble() * Math.PI * 2;
            Vec3 start = p.add(Math.cos(hoek) * fladderStraal, 0, Math.sin(hoek) * fladderStraal);
            naar(g, start, Rol.FLADDER, s == Soort.VLINDER ? 0.12 : 0.3, 0.3);
            timer = 100 + g.r().nextInt(200);
        }

        void laatZitplekLos(Groep g) {
            if (zit >= 0) {
                g.bezet[zit] = false;
                zit = -1;
            }
        }

        /** The dragon is gone or nobody is near: fly off / hop away and poof. */
        void vertrek(Groep g) {
            laatZitplekLos(g);
            Vec3 p = f.position();
            Vec3 uit = p.subtract(g.draak.position()).multiply(1, 0, 1);
            uit = uit.lengthSqr() < 1e-3 ? new Vec3(1, 0, 0) : uit.normalize();
            if (s.vliegt) {
                naar(g, p.add(uit.scale(30)).add(0, 14, 0), Rol.WEG, 0.4, 0);
                f.anim(s.vlieg);
                timer = duur;
            } else {
                rol = Rol.WEG;
                f.anim(s.loop);
                van = p;
                naar = p.add(uit.scale(1.5));
                duur = 10 + g.r().nextInt(30);
                k = 0;
                boog = 0.25;
                timer = duur;
            }
        }

        /** False = gone. */
        boolean tick(Groep g) {
            if (f.isRemoved()) {
                laatZitplekLos(g);
                return false;
            }
            ServerLevel sl = g.sl();
            switch (rol) {
                case NAAR -> {
                    k++;
                    Vec3 doel = daarna == Rol.ZIT && zit >= 0 ? g.zitplek(zit) : daarna == Rol.FLADDER && fladderMidden != null
                            ? fladderMidden.add(Math.cos(hoek) * fladderStraal, 0, Math.sin(hoek) * fladderStraal) : naar;
                    double x = Math.min(1, k / (double) duur);
                    Vec3 v = van.lerp(doel, x).add(0, Math.sin(x * Math.PI) * boog * Math.max(1, van.distanceTo(doel) * 0.15), 0);
                    f.zet(v);
                    if (x >= 1) {
                        rol = daarna;
                        timer = 30 + sl.getRandom().nextInt(80);
                        if (rol == Rol.ZIT || rol == Rol.GROND) {
                            f.anim(s.stil);
                            if (rol == Rol.ZIT) {
                                f.setYRot(g.draak.getYRot() + (sl.getRandom().nextBoolean() ? 90 : -90));
                            }
                        } else if (rol == Rol.FLADDER) {
                            f.anim(s.vlieg);
                        } else if (rol == Rol.WEG) {
                            f.discard();
                            return false;
                        }
                    }
                }
                case GROND -> {
                    if (--timer <= 0) {
                        int keus = sl.getRandom().nextInt(10);
                        if (s.vliegt && g.snelheid < 0.2 && keus < (s == Soort.VLINDER ? 4 : 2)) {
                            int zp = s.vogel() && sl.getRandom().nextBoolean() ? g.vrijeZitplek() : -1;
                            if (zp >= 0) {
                                naarZit(g, zp);
                            } else {
                                fladder(g);
                            }
                        } else if (keus < 6) {   // hop to a spot nearby
                            Vec3 p = g.grondplek(f.position(), s == Soort.KONIJN || s == Soort.EEKHOORN ? 3.5 : 2);
                            if (p != null && Math.abs(p.y - f.getY()) < 1.6) {
                                naar(g, p, Rol.GROND, s.vogel() ? 0.09 : 0.12, s == Soort.KONIJN ? 0.35 : 0.2);
                            } else {
                                timer = 20;
                            }
                        } else {   // peck / eat / sit / happy
                            f.anim(s.bezig[sl.getRandom().nextInt(s.bezig.length)]);
                            timer = 40 + sl.getRandom().nextInt(80);
                            if (s.vogel() && sl.getRandom().nextInt(4) == 0) {
                                sl.playSound(null, f.getX(), f.getY(), f.getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.35f, 1.6f + sl.getRandom().nextFloat() * 0.3f);
                            }
                        }
                    }
                }
                case ZIT -> {
                    if (zit < 0) {
                        rol = Rol.GROND;
                        break;
                    }
                    Vec3 p = g.zitplek(zit);
                    f.setPos(p.x, p.y, p.z);
                    // he takes off (or turns fast): everyone on him flutters off
                    if (g.snelheid > 0.25) {
                        laatZitplekLos(g);
                        if (s.vliegt) {
                            fladder(g);
                        } else {
                            Vec3 gp = g.grondplek(null, 8);
                            if (gp == null) {
                                vertrek(g);
                            } else {
                                naar(g, gp, Rol.GROND, 0.25, 1.2);
                            }
                        }
                        break;
                    }
                    if (--timer <= 0) {
                        int keus = sl.getRandom().nextInt(10);
                        if (keus < 2) {   // down again
                            laatZitplekLos(g);
                            if (s.vliegt) {
                                fladder(g);
                            } else {
                                Vec3 gp = g.grondplek(null, 6);
                                if (gp != null) {
                                    naar(g, gp, Rol.GROND, 0.18, 1.0);
                                } else {
                                    timer = 60;
                                }
                            }
                        } else {
                            f.anim(sl.getRandom().nextBoolean() ? s.stil : s.bezig[sl.getRandom().nextInt(s.bezig.length)]);
                            timer = 40 + sl.getRandom().nextInt(100);
                        }
                    }
                }
                case FLADDER -> {
                    if (fladderMidden == null) {
                        fladder(g);
                        break;
                    }
                    double v = s == Soort.VLINDER ? 0.04 : 0.09;
                    hoek += v;
                    // butterflies wobble, birds bob
                    double wob = s == Soort.VLINDER ? Math.sin(g.t * 0.3 + hoek) * 0.35 : Math.sin(g.t * 0.15 + hoek) * 0.2;
                    f.zet(fladderMidden.add(Math.cos(hoek) * fladderStraal, wob, Math.sin(hoek) * fladderStraal));
                    if (--timer <= 0) {
                        int keus = sl.getRandom().nextInt(10);
                        int zp = s.vogel() && keus < 4 && g.snelheid < 0.2 ? g.vrijeZitplek() : -1;
                        if (zp >= 0) {
                            naarZit(g, zp);
                        } else if (keus < 7) {
                            Vec3 gp = g.grondplek(null, s == Soort.VLINDER ? 12 : STRAAL);
                            if (gp != null) {
                                naar(g, gp, Rol.GROND, s == Soort.VLINDER ? 0.1 : 0.3, 0.4);
                                f.anim(s.vlieg);
                            } else {
                                fladder(g);
                            }
                        } else {
                            fladder(g);
                        }
                    }
                }
                case WEG -> {
                    k++;
                    double x = Math.min(1, k / (double) duur);
                    f.zet(van.lerp(naar, x).add(0, Math.abs(Math.sin(x * Math.PI * 3)) * boog, 0));
                    if (x >= 1) {
                        sl.sendParticles(ParticleTypes.POOF, f.getX(), f.getY() + 0.2, f.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
                        f.discard();
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /** Dev/tests: all crowds gone at once. */
    static void allesWeg() {
        for (Groep g : GROEPEN.values()) {
            for (Vriendje v : g.vriendjes) {
                v.f.discard();
            }
        }
        GROEPEN.clear();
    }

    /** Dev/tests: a crowd's box. */
    static AABB gebied(Entity draak) {
        return draak.getBoundingBox().inflate(STRAAL + 2, 16, STRAAL + 2);
    }
}
