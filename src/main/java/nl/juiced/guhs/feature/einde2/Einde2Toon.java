package nl.juiced.guhs.feature.einde2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.castschurk.Wezens;
import nl.juiced.guhs.feature.einde2.castvolk.E2CastVolkFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.registry.ModEntities;

/**
 * {@code /guhs einde2 toon <groep>} (a dev run only, CONTRACT_E2 11.9): every model of a group in rows in front of the player,
 * 3 blocks apart and facing the player, so it can be looked at in peace (every entity {@code NoAI}-still and invulnerable,
 * entity tag {@value #TAG}); {@code /guhs einde2 toon weg} removes every such row again.
 * <ul>
 *   <li>{@code team}, {@code volk}, {@code schurk}: the three cast tables ({@link CastTabellen}), every kind including every
 *       look, a crowd kind once per mix set (each where the client shows that set), and for {@code schurk} every creature of
 *       11.6 in every look ({@code Soort});</li>
 *   <li>{@code sponsguh}: the cast of the Sponsguh story (phase A) in every look, and the Knabbelburgermobiel;</li>
 *   <li>{@code wezens}: every other entity type of phase A: Appa-guh, Momo-guh, the Mika's of the vesting (Ender-Mika,
 *       kaasmijt, Schuilguh), the zwevers and element animals, the vetbootje and the hot air balloon, the six biome guhs, and
 *       last, in a row of its own far behind, the Leegtewalvisguh (only seen with {@code /guhs einde2 zwevers walviszicht aan});</li>
 *   <li>{@code alles}: all groups, one after the other, each further back.</li>
 * </ul>
 * A group of more than {@link #PER_RIJ} stands in several rows, {@link #RIJ} blocks behind each other (big creatures get the
 * room they need).
 */
public final class Einde2Toon {
    public static final String TAG = "guhs_einde2_toon";
    public static final List<String> GROEPEN = List.of("team", "volk", "schurk", "sponsguh", "wezens", "geest", "verhaal");
    /** Blocks between two things of a row, things per row, blocks between two rows, and from the player to the first row. */
    static final int AFSTAND = 3, PER_RIJ = 16, RIJ = 5, VOOR = 5;
    /** The entity types of the group "wezens", in order (the whale comes last, on its own). */
    static final List<String> WEZENS = List.of("appa_guh", "momo_guh", "ender_mika", "kaasmijt", "schuilguh", "kruimelkwal",
            "vleermuis_lemurguh", "otter_pinguinguh", "das_molguh", "schildpad_eendguh", "vetbootje", "heteluchtballon");
    /**
     * integratie_b: the group "verhaal", the props of the Guhvatar story and tier 5 that are entities of their own (the
     * kaasbol of chapter 1, an airship of the fleet of the finale, the Peace Balloon of the launch); Zuka's ship and the
     * Bestelguh are not here: they go away by themselves without their chase or their counter.
     */
    static final List<String> VERHAAL = List.of("kaasbol", "einde2_h4_vuur_luchtschip", "einde2_trap5_ballon_vredesballon");
    static final List<GuhVariant> BIOMEGUHS = List.of(GuhVariant.KORSTGUH, GuhVariant.DUINGUH, GuhVariant.KRUIMELGUH, GuhVariant.SCHIMMELGUH,
            GuhVariant.KRISTALGUH, GuhVariant.KOORGUH);

    /**
     * One thing of a row: how it is made, its crowd set ({@code mix} sets, this one {@code set}; 0 and -1: none), the room
     * it takes along the row ({@code breed}) and in depth ({@code diep}, the row behind it starts at least that far back),
     * how far behind the row line it stands ({@code achter}), whether it stands in a row of its own, and how far above the
     * ground ({@code hoog}: the Leeuwschildpad-guh's origin is its nose, its beard hangs far below it).
     */
    record Ding(String naam, Function<ServerLevel, Entity> maak, int mix, int set, int breed, int diep, int achter, boolean alleen, int hoog) {
        Ding(String naam, Function<ServerLevel, Entity> maak, int mix, int set, int breed, int diep, int achter, boolean alleen) {
            this(naam, maak, mix, set, breed, diep, achter, alleen, 0);
        }

        static Ding van(String naam, Function<ServerLevel, Entity> maak) {
            return new Ding(naam, maak, 0, -1, AFSTAND, RIJ, 0, false);
        }
    }

    /**
     * The Leeuwschildpad-guh per look: the room of its head with the mane (44 blocks across at scale 1), its depth (the head
     * reaches 22 blocks behind the nose, the paw 10 before it), and the height of its nose (the beard hangs 18 below it).
     */
    static Ding schildpad(int soort) {
        float s = CastWezen.SCHILDPAD_SCHAAL[soort];
        return new Ding("leeuwschildpad_guh " + soort, level -> {
            CastWezen c = E2CastSchurkFeature.type(Wezens.LEEUWSCHILDPAD_GUH).create(level, EntitySpawnReason.COMMAND);
            if (c != null) {
                c.zetSoort(soort);
                c.zetStil(true);
            }
            return c;
        }, 0, -1, Math.round(46 * s), Math.round(36 * s), Math.round(11 * s), true, Math.round(19 * s));
    }

    private Einde2Toon() {
    }

    // --- the groups ----------------------------------------------------------------------------------------------------------

    /** Everything of one group, in the order of its row. */
    public static List<Ding> groep(String groep) {
        List<Ding> uit = new ArrayList<>();
        switch (groep) {
            case "team" -> kinds(uit, CastTabellen.Groep.TEAM);
            case "volk" -> kinds(uit, CastTabellen.Groep.VOLK);
            case "schurk" -> {
                kinds(uit, CastTabellen.Groep.SCHURK);
                for (Wezens w : Wezens.values()) {
                    if (w == Wezens.LEEUWSCHILDPAD_GUH) {
                        for (int s = 0; s < w.soorten; s++) {
                            uit.add(schildpad(s));                      // (a head of some 30 blocks: a row of its own)
                        }
                        continue;
                    }
                    for (int s = 0; s < Math.max(1, w.soorten); s++) {
                        int soort = s;
                        boolean draak = w == Wezens.DRAAKGUH;
                        int breed = draak ? 11 : Math.max(AFSTAND, (int) Math.ceil(w.breed * (w == Wezens.LUCHTBIZON_GUH ? 1.1f : 1f)) + 2);
                        uit.add(new Ding(w.id() + (w.soorten > 0 ? " " + s : ""), level -> {
                            CastWezen c = E2CastSchurkFeature.type(w).create(level, EntitySpawnReason.COMMAND);
                            if (c != null) {
                                c.zetSoort(soort);
                                c.zetStil(true);
                            }
                            return c;
                        }, 0, -1, breed, draak ? 11 : Math.max(RIJ, breed), draak ? 5 : 0, false));
                    }
                }
            }
            case "sponsguh" -> {
                for (Cast cast : Cast.values()) {
                    for (int s = 0; s < Math.max(1, cast.soorten); s++) {
                        int soort = s;
                        int breed = Math.max(AFSTAND, (int) Math.ceil(cast.breed) + 2);
                        uit.add(new Ding(cast.id() + (cast.soorten > 0 ? " " + s : ""), level -> {
                            Entity e = maakType(level, cast.id());
                            if (e instanceof SponsguhWezen w) {
                                if (cast.soorten > 0) {
                                    w.zetSoort(soort);
                                }
                                w.zetStil(true);
                            }
                            return e;
                        }, 0, -1, breed, Math.max(RIJ, breed), 0, false));
                    }
                }
                uit.add(new Ding("knabbelburgermobiel", level -> maakType(level, "knabbelburgermobiel"), 0, -1, 4, RIJ, 0, false));
            }
            case "wezens" -> {
                for (String id : WEZENS) {
                    int breed = id.equals("appa_guh") ? 5 : id.equals("vetbootje") || id.equals("heteluchtballon") ? 4 : AFSTAND;
                    uit.add(new Ding(id, level -> maakType(level, id), 0, -1, breed, RIJ, 0, false));
                }
                for (GuhVariant v : BIOMEGUHS) {
                    uit.add(Ding.van(v.id(), level -> {
                        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.COMMAND);
                        if (guh != null) {
                            guh.setVariant(v);
                        }
                        return guh;
                    }));
                }
                // the whale: about 20 x 12 x 44 blocks, in a row of its own, its snout towards the player and well behind the rest
                uit.add(new Ding("leegtewalvisguh", level -> maakType(level, "leegtewalvisguh"), 0, -1, 24, 48, 24, true));
            }
            case "verhaal" -> {
                for (String id : VERHAAL) {
                    boolean groot = !id.equals("kaasbol");   // (the airship is drawn 2.5x, the balloon is as big as the yard's overlay)
                    uit.add(new Ding(id, level -> maakType(level, id), 0, -1, groot ? 26 : 4, groot ? 26 : RIJ, groot ? 10 : 0, groot));
                }
            }
            // (geestmodellen: the creatures of the Geestenwereld in every look, the Theegeest and the Bestelguh)
            case "geest" -> nl.juiced.guhs.feature.einde2.geestwezens.GeestToon.rij()
                    .forEach(r -> uit.add(new Ding(r.naam(), r.maak(), 0, -1, r.breed(), r.diep(), r.achter(), r.alleen())));
            default -> {
            }
        }
        return uit;
    }

    /** Every kind of a table, a crowd kind once for each of its mix sets. */
    private static void kinds(List<Ding> uit, CastTabellen.Groep groep) {
        for (GuhNpcEntity.Kind kind : CastTabellen.kinds(groep)) {
            int mix = CastTabellen.mix(kind);
            for (int i = 0; i < Math.max(1, mix); i++) {
                uit.add(new Ding(kind.id() + (mix > 0 ? " mix " + i : ""), level -> {
                    GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
                    if (npc != null) {
                        npc.setKind(kind);
                    }
                    return npc;
                }, mix, mix > 0 ? i : -1, AFSTAND, RIJ, 0, false));
            }
        }
    }

    @Nullable
    private static Entity maakType(ServerLevel level, String id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Guhs.id(id));
        return type == null ? null : type.create(level, EntitySpawnReason.COMMAND);
    }

    // --- putting them down ---------------------------------------------------------------------------------------------------

    /**
     * Puts the rows of these groups in front of {@code at} (the player looking {@code kijk}), the first row {@link #VOOR}
     * blocks ahead; returns how many things stand there.
     */
    public static int toon(ServerLevel level, BlockPos at, Direction kijk, List<String> groepen) {
        Direction zij = kijk.getClockWise();
        float yaw = kijk.getOpposite().toYRot();
        int diepte = VOOR;
        int gezet = 0;
        for (String groep : groepen) {
            List<List<Ding>> rijen = new ArrayList<>();
            List<Ding> rij = new ArrayList<>();
            for (Ding d : groep(groep)) {
                if (!rij.isEmpty() && (d.alleen() || rij.size() >= PER_RIJ || rij.get(rij.size() - 1).alleen())) {
                    rijen.add(rij);
                    rij = new ArrayList<>();
                }
                rij.add(d);
            }
            if (!rij.isEmpty()) {
                rijen.add(rij);
            }
            for (List<Ding> deze : rijen) {
                int breed = 0, diep = RIJ;
                for (Ding d : deze) {
                    breed += d.breed();
                    diep = Math.max(diep, d.diep());
                }
                BlockPos midden = at.relative(kijk, diepte);
                double plek = -breed / 2.0;
                int schuif = 0;                                         // (how far the crowd sets have nudged the rest along)
                for (Ding d : deze) {
                    BlockPos pos = midden.relative(zij, (int) Math.round(plek + d.breed() / 2.0) + schuif).relative(kijk, d.achter());
                    if (d.mix() > 0) {
                        for (int k = 0; k < d.mix() && E2CastVolkFeature.mixSet(pos, d.mix()) != d.set(); k++) {
                            pos = pos.relative(zij);
                            schuif++;
                        }
                    }
                    plek += d.breed();
                    Entity e = d.maak().apply(level);
                    if (e == null) {
                        continue;
                    }
                    e.snapTo(pos.getX() + 0.5, pos.getY() + d.hoog(), pos.getZ() + 0.5, yaw, 0f);
                    if (e instanceof LivingEntity le) {
                        le.setYHeadRot(yaw);
                        le.setYBodyRot(yaw);
                    }
                    stil(e);
                    if (level.addFreshEntity(e)) {
                        gezet++;
                    }
                }
                diepte += diep;
            }
        }
        return gezet;
    }

    /** NoAI-still, invulnerable, never despawning, tagged. */
    private static void stil(Entity e) {
        e.setInvulnerable(true);
        e.addTag(TAG);
        if (e instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        } else {
            e.setNoGravity(true);                                       // (a boat, a balloon, a car: they stay in the row)
        }
        if (e instanceof CastWezen w) {
            w.zetStil(true);
        }
    }

    /** Removes every thing of every row; returns how many. */
    public static int weg(ServerLevel level) {
        List<Entity> weg = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e.entityTags().contains(TAG)) {
                weg.add(e);
            }
        }
        weg.forEach(Entity::discard);
        return weg.size();
    }

    /** How many things a group (or "alles") puts down. */
    public static int aantal(List<String> groepen) {
        int n = 0;
        for (String g : groepen) {
            n += groep(g).size();
        }
        return n;
    }

    // --- the command ---------------------------------------------------------------------------------------------------------

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> toon = Commands.literal("toon");
        for (String groep : GROEPEN) {
            toon.then(Commands.literal(groep).executes(c -> toon(c.getSource(), groep, List.of(groep))));
        }
        toon.then(Commands.literal("alles").executes(c -> toon(c.getSource(), "alles", GROEPEN)));
        toon.then(Commands.literal("weg").executes(c -> {
            int n = weg(c.getSource().getLevel());
            c.getSource().sendSuccess(() -> Component.literal("einde2 toon: " + n + " removed"), false);
            return n;
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(toon)));
    }

    private static int toon(CommandSourceStack source, String naam, List<String> groepen) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = toon(source.getLevel(), player.blockPosition(), player.getDirection(), groepen);
        source.sendSuccess(() -> Component.literal("einde2 toon " + naam + ": " + n + " in rows in front of you (away again: /guhs einde2 toon weg)"), false);
        return n;
    }
}
