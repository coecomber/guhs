package nl.juiced.guhs.feature.landdiertjes;

import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.piep.Schouder;

/** Game events of the landdiertjes: the visible "seen" advancements, and the op commands for testing and the autocheck. */
public final class LanddiertjesEvents {
    /** Guhdex page (quest/seen_&lt;id&gt;) -> the visible advancement of the Diertjes tab. */
    static final Map<String, String> GEZIEN = Map.of(
            "quest/seen_pluisegeltje", "diertjes/landdiertjes_egeltje",
            "quest/seen_guh_konijntje", "diertjes/landdiertjes_konijntje",
            "quest/seen_pluiseekhoorntje", "diertjes/landdiertjes_eekhoorntje",
            "quest/seen_shuckle", "diertjes/landdiertjes_shuckle");

    private LanddiertjesEvents() {
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        Identifier id = event.getAdvancement().id();
        if (Guhs.MODID.equals(id.getNamespace()) && event.getEntity() instanceof ServerPlayer p) {
            String zichtbaar = GEZIEN.get(id.getPath());
            if (zichtbaar != null) {
                GidsFeature.grant(p, zichtbaar);
            }
        }
    }

    /**
     * /guhs landdiertjes rij &lt;pos&gt; (a tame row of all four, and a bunny of every colour), schouder (a tame eekhoorntje on
     * your shoulder), plekje &lt;pos&gt; &lt;tries&gt; (try the shuckle-plekje there), voorraadje &lt;pos&gt; (a stash). Op 2.
     */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var dier = Commands.literal("landdiertjes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("rij").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                    return rij(p, pos);
                })))
                .then(Commands.literal("schouder").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    PluiseekhoorntjeEntity e = LanddiertjesFeature.PLUISEEKHOORNTJE.get().create(p.level(), EntitySpawnReason.TRIGGERED);
                    if (e == null || Schouder.heeft(p)) {
                        return 0;
                    }
                    e.tame(p);
                    return PluiseekhoorntjeEntity.opSchouder(p, e) ? 1 : 0;
                }))
                .then(Commands.literal("plekje").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("keer", IntegerArgumentType.integer(1, 100)).executes(c -> {
                            ServerLevel level = c.getSource().getLevel();
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                            int n = 0;
                            for (int i = IntegerArgumentType.getInteger(c, "keer"); i > 0; i--) {
                                if (ShucklePlekjeBlock.probeer(level, pos, level.getRandom()) != null) {
                                    n++;
                                }
                            }
                            return n;
                        }))))
                .then(Commands.literal("voorraadje").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c ->
                        KnabbelvoorraadjeBlock.verstop(c.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(c, "pos"), 5) ? 1 : 0)));
        event.getDispatcher().register(Commands.literal("guhs").then(dier));
    }

    /** A tame row: egeltje, the four bunnies, eekhoorntje, Sjokkel (facing south), 1.5 blocks apart along x. */
    static int rij(ServerPlayer owner, BlockPos pos) {
        ServerLevel level = owner.level();
        int x = 0;
        for (Landdiertje d : new Landdiertje[]{LanddiertjesFeature.PLUISEGELTJE.get().create(level, EntitySpawnReason.TRIGGERED), maakKonijn(level, GuhKonijntjeEntity.Kleur.ROZE),
                maakKonijn(level, GuhKonijntjeEntity.Kleur.WIT), maakKonijn(level, GuhKonijntjeEntity.Kleur.CHOCO),
                maakKonijn(level, GuhKonijntjeEntity.Kleur.GRIJS), LanddiertjesFeature.PLUISEEKHOORNTJE.get().create(level, EntitySpawnReason.TRIGGERED),
                LanddiertjesFeature.SHUCKLE.get().create(level, EntitySpawnReason.TRIGGERED)}) {
            if (d == null) {
                continue;
            }
            d.snapTo(pos.getX() + 0.5 + x * 1.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
            d.setYHeadRot(0f);
            d.setYBodyRot(0f);
            d.tame(owner);
            d.setOrderedToSit(true);
            d.setInSittingPose(false);
            d.setPersistenceRequired();
            level.addFreshEntity(d);
            x++;
        }
        return x;
    }

    private static GuhKonijntjeEntity maakKonijn(ServerLevel level, GuhKonijntjeEntity.Kleur kleur) {
        GuhKonijntjeEntity k = LanddiertjesFeature.GUH_KONIJNTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (k != null) {
            k.finalizeSpawn(level, level.getCurrentDifficultyAt(k.blockPosition()), EntitySpawnReason.COMMAND, null);
            k.setKleur(kleur);
        }
        return k;
    }
}
