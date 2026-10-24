package nl.juiced.guhs.feature.safari.quests;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpixel.Aandenken;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.registry.ModSounds;

/**
 * De vermomde guh (design 1.7): a guh sneaked into the park in a homemade zebra suit ("njeg, ik ben echt een zebra").
 * Three disguised guhs stand in the park, one per spot (next to the first home of the zebras, the reuzenpanda's and the
 * pinguïns, in the matching suit); they are kept there by {@link #bewaak}. A player finds them in that order (quest
 * {@link #QUEST}: "" / "gevraagd" -> "1" -> "2" -> "klaar"); the head ranger (S2) can start it ({@link #ranger}), but
 * finding the first one starts it too. At the end Streepje goes home with you: a tamed guh in the zebrapakje, the suit
 * unlocked for all your guhs, and 150 safarimunten.
 */
public final class VermomdeGuh {
    public static final String QUEST = "vermomde_guh";
    public static final String TAG = "guhs_safari_vermomd";
    public static final int MUNTEN = 150;

    /** The three spots: next to which species, the suit, the word for the "I'm just a ..." line. */
    record Plek(String soort, GuhClothes pak, String woord) {
    }

    static final List<Plek> PLEKKEN = List.of(
            new Plek("zebra", GuhClothes.ZEBRAPAKJE, "zebra"),
            new Plek("reuzenpanda", GuhClothes.PANDAPAKJE, "panda"),
            new Plek("pinguin", GuhClothes.PINGUINPAKJE, "pinguin"));

    /** Flag: the head ranger already thanked this player after the quest. */
    static final String BEDANKT = "vermomd_bedankt";

    /**
     * S2's head ranger (the speaker, may be null): asks for help the first time, thanks once after the quest; otherwise
     * false, so his own talk (the tip of the day...) comes (the quest tracker already shows where you are).
     */
    static boolean ranger(ServerPlayer p, @Nullable Entity ranger) {
        MinecraftServer server = p.level().getServer();
        String stand = SafariSpeler.quest(server, p.getUUID(), QUEST);
        if (stand.isEmpty()) {
            SafariSpeler.zetQuest(server, p.getUUID(), QUEST, "gevraagd");
            SafariDialoog.zeg(p, ranger, SafariDialoog.RANGER, "gui.guhs.safari.vermomd.ranger");
            SafariDialoog.zeg(p, ranger, SafariDialoog.RANGER, "gui.guhs.safari.vermomd.ranger_2");
            return true;
        }
        if (stand.equals("klaar") && !SafariSpeler.vlag(server, p.getUUID(), BEDANKT)) {
            SafariSpeler.zetVlag(server, p.getUUID(), BEDANKT, true);
            SafariDialoog.zeg(p, ranger, SafariDialoog.RANGER, "gui.guhs.safari.vermomd.ranger_klaar");
            return true;
        }
        return false;
    }

    /** A right click on an entity: true when it was one of the disguised guhs. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!e.entityTags().contains(TAG)) {
            return false;
        }
        int i = plekVan(e);
        if (i < 0) {
            return true;
        }
        vind(p, i, e);
        return true;
    }

    /** The player clicks the disguised guh at spot i (also used by the gametests, with e = null). */
    static void vind(ServerPlayer p, int i, @Nullable Entity e) {
        MinecraftServer server = p.level().getServer();
        String stand = SafariSpeler.quest(server, p.getUUID(), QUEST);
        int nu = switch (stand) {
            case "", "gevraagd" -> 0;
            case "1" -> 1;
            case "2" -> 2;
            default -> -1;
        };
        if (nu != i) {
            SafariDialoog.zeg(p, e, SafariDialoog.VERKLEED, "gui.guhs.safari.vermomd.niet_nu",
                    Component.translatable("gui.guhs.safari.vermomd." + PLEKKEN.get(i).woord()));
            return;
        }
        SafariDialoog.zeg(p, e, SafariDialoog.VERKLEED, "gui.guhs.safari.vermomd." + (i + 1));
        if (e != null && p.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 14, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, e.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        }
        if (i < 2) {
            SafariSpeler.zetQuest(server, p.getUUID(), QUEST, String.valueOf(i + 1));
        } else {
            klaar(p);
        }
    }

    /** The reward: Streepje comes home with you, in his zebrapakje. */
    static void klaar(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        SafariSpeler.zetQuest(server, p.getUUID(), QUEST, "klaar");
        if (p.level() instanceof ServerLevel level) {
            GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guh != null) {
                guh.setVariant(GuhVariant.NORMAL);
                guh.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, p.getYRot() + 180f, 0f);
                guh.setCustomName(Component.translatable("gui.guhs.safari.vermomd.naam"));
                guh.tame(p);
                guh.wear(GuhClothes.ZEBRAPAKJE);
                guh.setPersistenceRequired();
                level.addFreshEntity(guh);
            }
        }
        Aandenken.kleding(p, GuhClothes.ZEBRAPAKJE, GuhClothes.PANDAPAKJE, GuhClothes.PINGUINPAKJE);   // (all three: none stays out of reach)
        SafariMunten.verdienEens(p, "quest_" + QUEST, MUNTEN, "vermomde guh");
        SafariDialoog.zeg(p, null, SafariDialoog.VERKLEED, "gui.guhs.safari.vermomd.klaar", MUNTEN);
        Beloningen.adv(p, "safari_vermomde_guh");
    }

    private static int plekVan(Entity e) {
        for (int i = 0; i < PLEKKEN.size(); i++) {
            if (e.entityTags().contains(TAG + "_" + i)) {
                return i;
            }
        }
        return -1;
    }

    /** Where the disguised guh of spot i stands: a few blocks from the first home of its species (null: none in the park). */
    @Nullable
    static BlockPos plek(ServerLevel level, int i) {
        String soort = PLEKKEN.get(i).soort();
        for (ParkData.Plek p : ParkData.get().plekken) {
            if (p.soort().equals(soort)) {
                int x = p.centrum().getX() + 3, z = p.centrum().getZ() + 2;
                // (1.7.0 fix: on the real ground, not on top of a bamboo stalk or a bush: the players must reach him)
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                while (pos.getY() > level.getMinY() && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                    pos = pos.below();
                }
                return pos;
            }
        }
        return null;
    }

    /** Every few seconds: each spot with a loaded chunk and a player near has exactly one disguised guh. */
    static void bewaak(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        for (int i = 0; i < PLEKKEN.size(); i++) {
            BlockPos pos = plek(level, i);
            if (pos == null || !level.hasChunkAt(pos) || level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 96, false) == null) {
                continue;
            }
            final int nr = i;
            List<Entity> er = level.getEntities((Entity) null, new AABB(pos).inflate(24), e -> e.entityTags().contains(TAG + "_" + nr));
            for (int k = 1; k < er.size(); k++) {
                er.get(k).discard();
            }
            if (er.isEmpty()) {
                zet(level, i, pos);
            }
            ruimOp(level, pos);
        }
    }

    /**
     * 1.7.0 fix: tall grass (or a flower, or bamboo) on his spot catches the click before it reaches him, and in the park
     * nothing may be broken: plants on his spot and just above it go.
     */
    static void ruimOp(ServerLevel level, BlockPos pos) {
        for (BlockPos p : new BlockPos[]{pos, pos.above()}) {
            net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
            if (!s.isAir() && (s.canBeReplaced() || s.is(net.minecraft.world.level.block.Blocks.BAMBOO)) && s.getFluidState().isEmpty()) {
                level.removeBlock(p, false);
            }
        }
    }

    static GuhEntity zet(ServerLevel level, int i, BlockPos pos) {
        GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.NORMAL);
        ruimOp(level, pos);
        guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        guh.wear(PLEKKEN.get(i).pak());
        guh.setNoAi(true);
        guh.setInvulnerable(true);
        guh.setPersistenceRequired();
        guh.addTag(TAG);
        guh.addTag(TAG + "_" + i);
        level.addFreshEntity(guh);
        return guh;
    }

    private VermomdeGuh() {
    }
}
