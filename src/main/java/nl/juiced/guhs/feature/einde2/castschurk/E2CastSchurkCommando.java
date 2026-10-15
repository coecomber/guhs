package nl.juiced.guhs.feature.einde2.castschurk;

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
import net.minecraft.world.entity.Mob;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * {@code /guhs einde2 cast_schurk toon}: every character of the slice in a row in front of the player, 3 blocks apart and
 * facing the player (the twelve Mika kinds, the Vuurnatie-Mika once per mix set), and behind it a row of every creature in
 * every look; {@code /guhs einde2 cast_schurk weg} removes both rows (entity tag {@value #TAG}). For a look at the models
 * (tools/autocheck/einde2_cast_schurk.txt). It builds in the world, so only a dev run has it.
 */
final class E2CastSchurkCommando {
    static final String TAG = "guhs_einde2_cast_schurk_toon";

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("cast_schurk")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon").executes(c -> toon(c.getSource())))
                .then(Commands.literal("weg").executes(c -> weg(c.getSource()))))));
    }

    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int n = zet(source.getLevel(), player.blockPosition(), player.getDirection());
        source.sendSuccess(() -> Component.literal("einde2 cast_schurk: " + n + " in two rows"), false);
        return n;
    }

    /** Both rows in front of {@code waar}, the player looking {@code kijk}; returns how many were placed. */
    static int zet(ServerLevel level, BlockPos waar, Direction kijk) {
        Direction zij = kijk.getClockWise();
        float yaw = kijk.getOpposite().toYRot();
        BlockPos rij = waar.relative(kijk, 5);
        List<GuhNpcEntity.Kind> kinds = new ArrayList<>();
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            for (int i = 0; i < Math.max(1, CastSchurk.mix(kind)); i++) {
                kinds.add(kind);
            }
        }
        int n = 0;
        int set = 0;
        for (int i = 0; i < kinds.size(); i++) {
            GuhNpcEntity.Kind kind = kinds.get(i);
            BlockPos at = rij.relative(zij, (i - kinds.size() / 2) * 3);
            int sets = CastSchurk.mix(kind);
            if (sets > 0) {                                        // (shift a crowd kind sideways until it shows set i)
                for (int k = 0; k < sets && Math.floorMod(at.getX() * 31 + at.getZ() * 17, sets) != set % sets; k++) {
                    at = at.relative(zij, 1);
                }
                set++;
            }
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
            if (npc == null) {
                continue;
            }
            npc.setKind(kind);
            npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
            npc.setYHeadRot(yaw);
            npc.addTag(TAG);
            level.addFreshEntity(npc);
            n++;
        }
        List<CastWezen> wezens = new ArrayList<>();
        for (Wezens w : Wezens.values()) {
            for (int s = 0; s < Math.max(1, w.soorten); s++) {
                CastWezen wezen = E2CastSchurkFeature.type(w).create(level, EntitySpawnReason.COMMAND);
                if (wezen != null) {
                    wezen.zetSoort(s);
                    wezens.add(wezen);
                }
            }
        }
        BlockPos rij2 = waar.relative(kijk, 16);
        double breed = 0;
        for (CastWezen w : wezens) {
            breed += ruimte(w);
        }
        double plek = -breed / 2;
        for (CastWezen w : wezens) {
            double r = ruimte(w);
            BlockPos at = rij2.relative(zij, (int) Math.round(plek + r / 2));
            plek += r;
            double hoog = w.wezens() == Wezens.LEEUWSCHILDPAD_GUH ? Math.round(19 * w.getScale()) : 0;   // (its beard hangs below its nose)
            w.snapTo(at.getX() + 0.5, at.getY() + hoog, at.getZ() + 0.5, yaw, 0f);
            w.setYHeadRot(yaw);
            w.zetStil(true);
            w.addTag(TAG);
            level.addFreshEntity(w);
            n++;
        }
        return n;
    }

    /**
     * Room a creature gets in its row (the dragon's coil is 9 blocks wide). The lion turtle's head (44 across with its mane)
     * overlaps its neighbours here: this slice row must stay in the loaded chunks of its test; /guhs einde2 toon schurk gives
     * it rows of its own.
     */
    private static double ruimte(Mob w) {
        if (w instanceof CastWezen c && c.wezens() == Wezens.DRAAKGUH) {
            return 11;
        }
        return Math.max(3, Math.ceil(w.getBbWidth()) + 2);
    }

    private static int weg(CommandSourceStack source) {
        int n = weg(source.getLevel());
        source.sendSuccess(() -> Component.literal("einde2 cast_schurk: " + n + " removed"), false);
        return n;
    }

    static int weg(ServerLevel level) {
        List<Entity> weg = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e.entityTags().contains(TAG)) {
                weg.add(e);
            }
        }
        weg.forEach(Entity::discard);
        return weg.size();
    }

    private E2CastSchurkCommando() {
    }
}
