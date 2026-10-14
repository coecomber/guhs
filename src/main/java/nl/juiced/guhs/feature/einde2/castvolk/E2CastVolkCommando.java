package nl.juiced.guhs.feature.einde2.castvolk;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * {@code /guhs einde2 cast_volk toon}: every character of the slice in rows in front of the player (3 blocks apart, facing
 * the player, 12 to a row, each row 4 blocks further back), a crowd kind once for every mix set (each one placed where
 * {@link E2CastVolkFeature#mixSet} picks that set); {@code /guhs einde2 cast_volk weg} removes them again (entity tag
 * {@link #TAG}). For a look in the dev client and tools/autocheck/einde2_cast_volk.txt; it builds nothing, but it is a dev
 * tool, so only a dev run has it.
 */
public final class E2CastVolkCommando {
    public static final String TAG = "guhs_einde2_cast_volk_toon";
    static final int PER_RIJ = 12;

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("cast_volk")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon").executes(c -> toon(c.getSource())))
                .then(Commands.literal("weg").executes(c -> weg(c.getSource()))))));
    }

    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = toon(source.getLevel(), player.blockPosition(), player.getDirection());
        source.sendSuccess(() -> Component.literal("einde2 cast_volk: " + n + " characters stand in front of you (weg: /guhs einde2 cast_volk weg)"), false);
        return n;
    }

    private static int weg(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = weg(source.getLevel(), player.blockPosition(), 256);
        source.sendSuccess(() -> Component.literal("einde2 cast_volk: " + n + " characters removed"), false);
        return n;
    }

    /** One per kind (a crowd kind: one per set); the first row 4 blocks in front of {@code at} towards {@code kijk}. */
    static int toon(ServerLevel level, BlockPos at, Direction kijk) {
        Direction langs = kijk.getClockWise();
        List<Object[]> wie = new ArrayList<>();                        // {kind, set or -1}
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            int n = CastVolk.mix(kind);
            for (int i = 0; i < Math.max(1, n); i++) {
                wie.add(new Object[]{kind, n == 0 ? -1 : i});
            }
        }
        int gezet = 0;
        for (int rij = 0; rij * PER_RIJ < wie.size(); rij++) {
            List<Object[]> deze = wie.subList(rij * PER_RIJ, Math.min(wie.size(), (rij + 1) * PER_RIJ));
            BlockPos midden = at.relative(kijk, 4 + rij * 4);
            int plek = -(deze.size() - 1) * 3 / 2;
            for (Object[] w : deze) {
                GuhNpcEntity.Kind kind = (GuhNpcEntity.Kind) w[0];
                int set = (int) w[1];
                BlockPos pos = midden.relative(langs, plek);
                if (set >= 0) {
                    // nudge along the row until this spot shows the wanted set (31 and 17 are coprime with every n of 2..5)
                    for (int d = 0; d < 6 && E2CastVolkFeature.mixSet(pos, CastVolk.mix(kind)) != set; d++) {
                        pos = pos.relative(langs);
                        plek++;
                    }
                }
                GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
                if (npc != null) {
                    npc.setKind(kind);
                    float yaw = kijk.getOpposite().toYRot();
                    npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
                    npc.setYHeadRot(yaw);
                    npc.setYBodyRot(yaw);
                    npc.addTag(TAG);
                    level.addFreshEntity(npc);
                    gezet++;
                }
                plek += 3;
            }
        }
        return gezet;
    }

    /** Removes every character the command put down within {@code ruim} blocks. */
    static int weg(ServerLevel level, BlockPos at, int ruim) {
        List<Entity> weg = level.getEntities((Entity) null, new AABB(at).inflate(ruim), e -> e.entityTags().contains(TAG));
        weg.forEach(Entity::discard);
        return weg.size();
    }

    private E2CastVolkCommando() {
    }
}
