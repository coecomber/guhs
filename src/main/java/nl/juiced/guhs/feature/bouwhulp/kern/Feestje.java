package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The fun around building (design 2.2, ronde 29, and 2.3 Pof!): when a project is done a little pink flag stands on top for
 * ten seconds, confetti flies and a "Klaar!" sounds; Pof! is a big pink smoke cloud with "POF!".
 */
public final class Feestje {
    /** A flag stands this long (ticks). */
    public static final int VLAG_TIJD = 200;
    /** Tag of the flag entity (a flag that outlived a restart is taken away when it loads). */
    public static final String VLAG_TAG = "guhs_bouwhulp_vlag";
    private static final Map<UUID, Long> VLAGGEN = new ConcurrentHashMap<>();
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFFF08CC0, 2.5f);

    /** Done: flag, confetti, a sound, "Klaar!" for the builder. */
    public static void klaar(ServerLevel level, Spook s, @Nullable ServerPlayer bouwer) {
        BlockPos top = s.topMidden();
        Vec3 m = Vec3.atBottomCenterOf(top);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, m.x, m.y + 0.5, m.z, 60, 1.2, 0.8, 1.2, 0.35);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.x, m.y + 0.5, m.z, 30, 1.5, 0.6, 1.5, 0.1);
        level.playSound(null, top, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.9f, 1.3f);
        level.playSound(null, top, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, 1.6f);
        Display.ItemDisplay vlag = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (vlag != null) {
            vlag.getSlot(0).set(new ItemStack(Items.PINK_BANNER));
            vlag.setPos(m.x, m.y + 0.6, m.z);
            vlag.addTag(VLAG_TAG);
            VLAGGEN.put(vlag.getUUID(), level.getGameTime() + VLAG_TIJD);   // (before it joins: an unknown flag is sent away)
            level.addFreshEntity(vlag);
        }
        if (bouwer != null) {
            bouwer.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.klaar", s.naam().isEmpty()
                    ? Component.translatable("gui.guhs.bouwhulp.project") : Component.literal(s.naam())));
        }
    }

    /** Pof!: a pink cloud over the whole box, "POF!" for everybody who sees it, a puff of sound. */
    public static void pof(ServerLevel level, Spook s) {
        AABB d = s.doos();
        Vec3 m = d.getCenter();
        int n = (int) Math.min(400, 40 + d.getXsize() * d.getZsize());
        level.sendParticles(ROZE, m.x, m.y, m.z, n, d.getXsize() / 2.5, d.getYsize() / 2.5, d.getZsize() / 2.5, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, m.x, m.y, m.z, n / 2, d.getXsize() / 3, d.getYsize() / 3, d.getZsize() / 3, 0.08);
        level.playSound(null, BlockPos.containing(m), SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.BLOCKS, 1.5f, 0.6f);
        level.playSound(null, BlockPos.containing(m), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS, 1f, 1.4f);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(m) < 64 * 64) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(2, 20, 8));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("gui.guhs.bouwhulp.pof")));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("gui.guhs.bouwhulp.pof.onder")));
            }
        }
    }

    /** Takes the flags away when their time is over (every second). */
    static void tick(ServerLevel level) {
        if (VLAGGEN.isEmpty() || level.getGameTime() % 20 != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> it = VLAGGEN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            Entity v = level.getEntity(e.getKey());
            if (v == null) {
                continue;   // (another dimension's flag)
            }
            if (level.getGameTime() >= e.getValue()) {
                v.discard();
                it.remove();
            }
        }
    }

    /** A flag entity loads that nobody remembers (after a restart): away with it. */
    static boolean oudeVlag(Entity e) {
        return e.entityTags().contains(VLAG_TAG) && !VLAGGEN.containsKey(e.getUUID());
    }

    private Feestje() {
    }
}
