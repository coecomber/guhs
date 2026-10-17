package nl.juiced.guhs.feature.einde2.spcast;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The dev commands of sp_cast (dev run only, operators), under {@code /guhs einde2 sponsguh cast}:
 * <ul>
 *   <li>{@code look <look> <aan|uit>}: a story look on the cast character you look at;</li>
 *   <li>{@code anim <naam>}: an animation on the cast character or Kroeg Mika you look at;</li>
 *   <li>{@code rij}: the whole cast in a row in front of you, and behind it every look on every character that has it and
 *       the five Kroeg Mikas (entity tag {@value #TAG}); {@code weg} removes them;</li>
 *   <li>{@code kroeg}: the residents of De Ruige Kroeg of the Schelpenstad near you, at once.</li>
 * </ul>
 */
final class E2SpCastCommando {
    static final String TAG = "guhs_einde2_sp_cast_rij";

    private E2SpCastCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        SpCommando.groep(event, Commands.literal("cast")
                .then(Commands.literal("look").then(Commands.argument("look", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpCast.LOOKS, b))
                        .then(Commands.literal("aan").executes(c -> look(c.getSource(), StringArgumentType.getString(c, "look"), true)))
                        .then(Commands.literal("uit").executes(c -> look(c.getSource(), StringArgumentType.getString(c, "look"), false)))))
                .then(Commands.literal("anim").then(Commands.argument("naam", StringArgumentType.word())
                        .executes(c -> anim(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("rij").executes(c -> rij(c.getSource())))
                .then(Commands.literal("weg").executes(c -> weg(c.getSource())))
                .then(Commands.literal("kroeg").executes(c -> kroeg(c.getSource()))));
    }

    /** The cast character or Kroeg Mika the player looks at (within 8 blocks, the smallest angle), or null. */
    static Entity bekeken(ServerPlayer p) {
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        Entity best = null;
        double bestHoek = 0.97;
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(8),
                e -> e instanceof SponsguhWezen || (e instanceof GuhNpcEntity n && KroegMikas.rij(n.getKind()) != null))) {
            Vec3 naar = e.getBoundingBox().getCenter().subtract(oog).normalize();
            double hoek = naar.dot(kijk);
            if (hoek > bestHoek) {
                bestHoek = hoek;
                best = e;
            }
        }
        return best;
    }

    private static int look(CommandSourceStack source, String look, boolean aan) throws CommandSyntaxException {
        Entity e = bekeken(source.getPlayerOrException());
        if (e == null || !SpCast.LOOKS.contains(look)) {
            source.sendFailure(Component.literal("sp_cast: look at a cast character; looks: " + SpCast.LOOKS));
            return 0;
        }
        if (!SpCast.kan(e, look)) {
            source.sendFailure(Component.literal("sp_cast: " + e.getType().getDescriptionId() + " has no look " + look));
            return 0;
        }
        SpCast.zetLook(e, look, aan);
        source.sendSuccess(() -> Component.literal("sp_cast: " + look + (aan ? " on" : " off") + " for " + e.getName().getString()), false);
        return 1;
    }

    private static int anim(CommandSourceStack source, String naam) throws CommandSyntaxException {
        Entity e = bekeken(source.getPlayerOrException());
        if (e instanceof SponsguhWezen w) {
            if (!w.cast().heeft(naam)) {
                source.sendFailure(Component.literal("sp_cast: " + w.cast().id() + " has no animation " + naam + "; it has " + w.cast().animaties()));
                return 0;
            }
            w.zetAnimatie(naam);
        } else if (e instanceof GuhNpcEntity npc) {
            KroegMikas r = KroegMikas.rij(npc.getKind());
            if (r == null || (r.duur(naam) < 0 && !naam.isEmpty())) {
                source.sendFailure(Component.literal("sp_cast: " + npc.getKind().id() + " has " + (r == null ? "[]" : r.animaties()) + " (\"\" stops a pose)"));
                return 0;
            }
            npc.speel(naam);
        } else {
            source.sendFailure(Component.literal("sp_cast: look at a cast character or a Kroeg Mika"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("sp_cast: " + naam + " on " + e.getName().getString()), false);
        return 1;
    }

    private static int rij(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int n = zet(source.getLevel(), p.blockPosition(), p.getDirection());
        source.sendSuccess(() -> Component.literal("sp_cast: " + n + " placed (row 1: the cast, row 2: every look, row 3: the Kroeg Mikas)"), false);
        return n;
    }

    /** Three rows in front of {@code waar}; returns how many were placed. */
    static int zet(ServerLevel level, BlockPos waar, Direction kijk) {
        Direction zij = kijk.getClockWise();
        float yaw = kijk.getOpposite().toYRot();
        List<Entity> rij1 = new ArrayList<>(), rij2 = new ArrayList<>(), rij3 = new ArrayList<>();
        for (Cast c : Cast.values()) {
            SponsguhWezen w = wezen(level, c);
            if (w != null) {
                rij1.add(w);
            }
            for (String look : c.looks()) {
                SponsguhWezen l = wezen(level, c);
                if (l != null) {
                    l.zetLook(look, true);
                    rij2.add(l);
                }
            }
        }
        for (KroegMikas k : KroegMikas.values()) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
            if (npc != null) {
                npc.setKind(k.kind);
                rij3.add(npc);
            }
        }
        int n = 0;
        List<List<Entity>> rijen = List.of(rij1, rij2, rij3);
        for (int r = 0; r < rijen.size(); r++) {
            List<Entity> rij = rijen.get(r);
            BlockPos midden = waar.relative(kijk, 5 + r * 5);
            for (int i = 0; i < rij.size(); i++) {
                Entity e = rij.get(i);
                BlockPos plek = midden.relative(zij, (i - rij.size() / 2) * 3);
                e.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, yaw, 0f);
                e.setYHeadRot(yaw);
                e.addTag(TAG);
                e.setInvulnerable(true);
                level.addFreshEntity(e);
                n++;
            }
        }
        return n;
    }

    private static SponsguhWezen wezen(ServerLevel level, Cast c) {
        if (!(E2SponsguhFeature.type(c).create(level, EntitySpawnReason.COMMAND) instanceof SponsguhWezen w)) {
            return null;
        }
        w.zetStil(true);
        return w;
    }

    private static int weg(CommandSourceStack source) {
        List<Entity> lijst = new ArrayList<>();
        for (Entity e : source.getLevel().getAllEntities()) {
            if (e.entityTags().contains(TAG)) {
                lijst.add(e);
            }
        }
        lijst.forEach(Entity::discard);
        int n = lijst.size(), weg = n;
        source.sendSuccess(() -> Component.literal("sp_cast: " + weg + " removed"), false);
        return n;
    }

    private static int kroeg(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        StructureStart start = Plekken.bij(p.level(), KroegBewoners.STRUCTUUR, p.blockPosition(), 128);
        if (start == null) {
            source.sendFailure(Component.literal("sp_cast: no Schelpenstad within 128 blocks"));
            return 0;
        }
        int n = KroegBewoners.nu(p.level(), start).size();
        source.sendSuccess(() -> Component.literal("sp_cast: " + n + " Kroeg Mikas made"), false);
        return n;
    }
}
