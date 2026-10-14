package nl.juiced.guhs.feature.einde2.castteam;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * {@code /guhs einde2 cast_team toon}: every kind of {@link CastTeam} in a row in front of the player (5 blocks ahead, 3 apart,
 * facing the player, entity tag {@value #TAG}); {@code /guhs einde2 cast_team weg} removes that row again
 * (tools/autocheck/einde2_cast_team.txt). Dev runs only (CONTRACT_E2 11.2 I).
 */
public final class E2CastTeamCommando {
    public static final String TAG = "guhs_einde2_cast_team_toon";

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("cast_team")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon").executes(c -> toon(c.getSource())))
                .then(Commands.literal("weg").executes(c -> weg(c.getSource()))))));
    }

    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = toon(source.getLevel(), player.position(), player.getYRot());
        source.sendSuccess(() -> Component.literal("einde2 cast_team: " + n + " characters in a row"), false);
        return n;
    }

    private static int weg(CommandSourceStack source) {
        int n = weg(source.getLevel());
        source.sendSuccess(() -> Component.literal("einde2 cast_team: " + n + " removed"), false);
        return n;
    }

    /** Puts the row in front of a spot that looks at yaw; returns how many stand there (also used by the game test). */
    public static int toon(ServerLevel level, Vec3 bij, float yaw) {
        List<GuhNpcEntity.Kind> kinds = CastTeam.kinds();
        float rad = yaw * Mth.DEG_TO_RAD;
        Vec3 vooruit = new Vec3(-Mth.sin(rad), 0, Mth.cos(rad));
        Vec3 opzij = new Vec3(-vooruit.z, 0, vooruit.x);
        Vec3 midden = bij.add(vooruit.scale(5));
        int n = 0;
        for (int i = 0; i < kinds.size(); i++) {
            Vec3 at = midden.add(opzij.scale((i - (kinds.size() - 1) / 2.0) * 3));
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
            if (npc == null) {
                continue;
            }
            npc.setKind(kinds.get(i));
            npc.snapTo(Math.floor(at.x) + 0.5, Math.floor(at.y), Math.floor(at.z) + 0.5, yaw + 180f, 0f);
            npc.setYHeadRot(yaw + 180f);
            npc.setYBodyRot(yaw + 180f);
            npc.addTag(TAG);
            if (level.addFreshEntity(npc)) {
                n++;
            }
        }
        return n;
    }

    /** Removes every entity of a row; returns how many. */
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

    private E2CastTeamCommando() {
    }
}
