package nl.juiced.guhs.feature.sausdieren;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.world.WildeDieren;

/**
 * bbq2 (sausdieren), dev runs only: {@code /guhs sausdieren spawnproef}. Wild Sauslopers and Sausblubjes only come with
 * the natural spawner, and that only runs around a player: on a headless dev server nobody ever saw one spawn. This puts a
 * stand-in player (a real ServerPlayer on a connection that leads nowhere, in creative so nothing bothers it) at the
 * command's spot, so the chunks around it load and the spawner does its rounds there; {@code tel} counts what walks around
 * within 128 blocks. Use it from the console: {@code execute in guhs:barbecuether positioned X Y Z run guhs sausdieren
 * spawnproef speler}, wait a few minutes, {@code ... run guhs sausdieren spawnproef tel}.
 */
final class Spawnproef {
    private static final String NAAM = "spawnproef";

    private Spawnproef() {
    }

    private static ServerPlayer vind(CommandSourceStack bron) {
        return bron.getServer().getPlayerList().getPlayerByName(NAAM);
    }

    static int speler(CommandSourceStack bron) {
        ServerLevel level = bron.getLevel();
        Vec3 plek = bron.getPosition();
        ServerPlayer p = vind(bron);
        if (p == null) {
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.nameUUIDFromBytes(NAAM.getBytes()), NAAM), false);
            p = new ServerPlayer(bron.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            bron.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        }
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = true;
        p.teleportTo(level, plek.x, plek.y, plek.z, Set.of(), 0f, 0f, false);
        ServerPlayer er = p;
        bron.sendSuccess(() -> Component.literal("spawnproef: the stand-in stands at " + er.blockPosition().toShortString() + " in " + er.level().dimension().identifier()
                + " (" + er.level().getBiome(er.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("?") + ")"), false);
        return 1;
    }

    static int tel(CommandSourceStack bron) {
        ServerPlayer p = vind(bron);
        ServerLevel level = p == null ? bron.getLevel() : p.level();
        Vec3 plek = p == null ? bron.getPosition() : p.position();
        Map<String, int[]> telling = new TreeMap<>();
        int wild = 0;
        for (Entity e : level.getEntities((Entity) null, new AABB(BlockPos.containing(plek)).inflate(128), x -> x instanceof Mob)) {
            Mob mob = (Mob) e;
            if (mob.getType().getCategory() != MobCategory.CREATURE) {
                continue;
            }
            boolean blijft = mob.isPersistenceRequired() || mob.requiresCustomPersistence();
            int[] n = telling.computeIfAbsent(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), k -> new int[3]);
            n[blijft ? 1 : 0]++;
            n[2] += WildeDieren.isKomEnGa(mob) ? 1 : 0;
            wild += blijft ? 0 : 1;
        }
        StringBuilder uit = new StringBuilder("spawnproef: creatures within 128 blocks of " + BlockPos.containing(plek).toShortString() + " at game time "
                + level.getGameTime() + " (" + wild + " count for the cap of " + MobCategory.CREATURE.getMaxInstancesPerChunk() + "):");
        telling.forEach((soort, n) -> uit.append("\n  ").append(soort).append(": ").append(n[0]).append(" wild (").append(n[2]).append(" come-and-go), ").append(n[1])
                .append(" that stay"));
        bron.sendSuccess(() -> Component.literal(uit.toString()), false);
        return 1 + wild;
    }

    static int weg(CommandSourceStack bron) {
        ServerPlayer p = vind(bron);
        if (p == null) {
            return 0;
        }
        bron.getServer().getPlayerList().remove(p);
        bron.sendSuccess(() -> Component.literal("spawnproef: the stand-in is gone"), false);
        return 1;
    }
}
