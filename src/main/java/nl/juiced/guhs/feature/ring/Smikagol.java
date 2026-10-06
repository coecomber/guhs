package nl.juiced.guhs.feature.ring;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-kern): Smikagol for the chapters and for afterwards (the entity: {@link SmikagolEntity}).
 * <ul>
 *   <li><b>The guide</b> (chapters 5 and 6): {@link #roep} makes him this player's guide (only their game knows about him;
 *       he is back at their side after a login, a poof to a rest point or a trip through the portal, as long as the story
 *       runs), {@link #leid} lets him scurry ahead along a route and wait, {@link #zeg} lets him talk, {@link #grijp} plays
 *       the grab, {@link #stuurWeg} ends it. {@link #BIJ_KLIK}: a scene's own answer to a click on him.</li>
 *   <li><b>The buddy</b> (the reward): {@link #maakMaatje} gives a player their own Smikagol, once. He fishes (see the
 *       entity). The Vissenbotje ({@link Botje}) calls him to you; when he is nowhere to be found any more it makes a new
 *       one and the old one retires by himself when he turns up (so there is never more than one per player).</li>
 * </ul>
 */
public final class Smikagol {
    /** Player saved data: the guide walks along; the buddy was given; the buddy's generation. */
    public static final String GIDS = "guhs_ring_smikagol_gids", MAATJE = "guhs_ring_smikagol_maatje", GENERATIE = "guhs_ring_smikagol_generatie";

    /** A scene's answer to a click on the guide (true: handled). */
    @FunctionalInterface
    public interface Klik {
        boolean klik(SmikagolEntity smikagol, ServerPlayer p);
    }

    public static final List<Klik> BIJ_KLIK = new CopyOnWriteArrayList<>();

    private static final Map<UUID, UUID> GIDSEN = new ConcurrentHashMap<>();
    /** The buddies that are loaded right now: owner -> entity. */
    private static final Map<UUID, UUID> MAATJES = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // the guide
    // =====================================================================================================================

    /** Smikagol becomes this player's guide and appears at {@code plek} (null: next to them). */
    public static SmikagolEntity roep(ServerPlayer p, @Nullable Vec3 plek) {
        GuhQuests.saved(p).putBoolean(GIDS, true);
        SmikagolEntity s = van(p);
        if (s == null) {
            wegGids(p);
            s = maak(p, SmikagolEntity.GIDS, plek == null ? Sam.naast(p) : plek, 0);
            if (s != null) {
                Zicht.alleenVoor(s, p.getUUID());
                GIDSEN.put(p.getUUID(), s.getUUID());
                p.level().addFreshEntity(s);
                GuhAdvancements.grant(p, "ring_smikagol");
            }
        } else if (plek != null) {
            s.teleportTo(plek.x, plek.y, plek.z);
        }
        return s;
    }

    /** This player's guide when he is in their world, else null. */
    @Nullable
    public static SmikagolEntity van(ServerPlayer p) {
        UUID id = GIDSEN.get(p.getUUID());
        return id != null && p.level().getEntity(id) instanceof SmikagolEntity s && s.isAlive() ? s : null;
    }

    public static boolean isGids(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(GIDS, false);
    }

    /** The guide scurries ahead along these world positions and waits for his player; at the end {@code daarna} runs. */
    public static boolean leid(ServerPlayer p, List<Vec3> route, @Nullable Consumer<ServerPlayer> daarna) {
        SmikagolEntity s = van(p);
        if (s == null) {
            s = roep(p, null);
        }
        if (s == null) {
            return false;
        }
        s.leid(route, daarna);
        return true;
    }

    /** The guide says this line (chat, in his name) to his player. */
    public static void zeg(ServerPlayer p, String key, Object... args) {
        SmikagolEntity s = van(p);
        if (s != null) {
            GuhQuests.say(p, s, key, args);
        }
    }

    /** The guide plays his grab (chapter 6). */
    public static void grijp(ServerPlayer p) {
        SmikagolEntity s = van(p);
        if (s != null) {
            s.grijp();
        }
    }

    /** Hop: the guide stands next to his player. */
    public static void kom(ServerPlayer p) {
        SmikagolEntity s = van(p);
        if (s != null) {
            Vec3 naast = Sam.naast(p);
            s.teleportTo(naast.x, naast.y, naast.z);
            s.getNavigation().stop();
        }
    }

    /** The guide leaves (the chapter is over). */
    public static void stuurWeg(ServerPlayer p) {
        GuhQuests.saved(p).remove(GIDS);
        wegGids(p);
    }

    private static void wegGids(ServerPlayer p) {
        UUID id = GIDSEN.remove(p.getUUID());
        if (id == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
    }

    /** (every second, per player) the guide is there while the story runs and he was called; else he is gone. */
    static void tick(ServerPlayer p) {
        boolean wil = isGids(p) && Ring.opReis(p) && p.isAlive() && !p.isSpectator() && Ring.verhaalWereld(p.level());
        SmikagolEntity s = van(p);
        if (!wil) {
            if (s != null || GIDSEN.containsKey(p.getUUID())) {
                wegGids(p);
            }
            if (isGids(p) && !Ring.opReis(p)) {
                GuhQuests.saved(p).remove(GIDS);
            }
            return;
        }
        if (s == null) {
            roep(p, null);
        }
    }

    // =====================================================================================================================
    // the buddy
    // =====================================================================================================================

    public static boolean heeftMaatje(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr(MAATJE, false);
    }

    /**
     * The reward at the end of the story: this player's own Smikagol, once per player (null when they have theirs already).
     * He appears next to them; the Vissenbotje (given along) calls him.
     */
    @Nullable
    public static SmikagolEntity maakMaatje(ServerPlayer p) {
        if (heeftMaatje(p)) {
            return null;
        }
        GuhQuests.saved(p).putBoolean(MAATJE, true);
        nl.juiced.guhs.feature.Minigames.give(p, new ItemStack(RingFeature.VISSENBOTJE.get()));
        GuhAdvancements.grant(p, "ring_smikagol_maatje");
        nl.juiced.guhs.feature.gids.GidsFeature.grant(p, "knabbelring/ring_maatje");
        return nieuwMaatje(p);
    }

    @Nullable
    private static SmikagolEntity nieuwMaatje(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        int generatie = saved.getIntOr(GENERATIE, 0) + 1;
        saved.putInt(GENERATIE, generatie);
        SmikagolEntity s = maak(p, SmikagolEntity.MAATJE, Sam.naast(p), generatie);
        if (s != null) {
            MAATJES.put(p.getUUID(), s.getUUID());
            p.level().addFreshEntity(s);
        }
        return s;
    }

    /** This player's buddy when he is loaded somewhere, else null. */
    @Nullable
    public static SmikagolEntity maatje(ServerPlayer p) {
        UUID id = MAATJES.get(p.getUUID());
        if (id == null) {
            return null;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            if (level.getEntity(id) instanceof SmikagolEntity s && s.isAlive()) {
                return s;
            }
        }
        return null;
    }

    /**
     * The Vissenbotje: the buddy comes to his player. When he is loaded he hops over (also from another dimension: there
     * he retires and a new one comes here with his fish); when he is nowhere to be found a new one is made.
     */
    public static boolean roepMaatje(ServerPlayer p) {
        if (!heeftMaatje(p)) {
            return false;
        }
        SmikagolEntity s = maatje(p);
        Vec3 naast = Sam.naast(p);
        if (s != null && s.level() == p.level()) {
            s.teleportTo(naast.x, naast.y, naast.z);
            s.zetBlijft(false);
            s.getNavigation().stop();
        } else {
            int vissen = s == null ? 0 : s.vissen();
            if (s != null) {
                s.discard();
            }
            s = nieuwMaatje(p);
            if (s == null) {
                return false;
            }
            s.zetVissen(vissen);
        }
        p.level().sendParticles(ParticleTypes.POOF, naast.x, naast.y + 0.5, naast.z, 10, 0.3, 0.3, 0.3, 0.01);
        GuhQuests.say(p, s, "quest.guhs.ring.smikagol.geroepen");
        return true;
    }

    @Nullable
    private static SmikagolEntity maak(ServerPlayer p, int modus, Vec3 plek, int generatie) {
        SmikagolEntity s = RingFeature.SMIKAGOL.get().create(p.level(), EntitySpawnReason.TRIGGERED);
        if (s == null) {
            return null;
        }
        s.snapTo(plek.x, plek.y, plek.z, p.getYRot() + 180f, 0f);
        s.zetEigenaar(p.getUUID(), modus, generatie);
        return s;
    }

    /**
     * (every second, every Smikagol; and when one comes back with its chunk) a guide nobody knows about is removed; a buddy
     * of an older generation than his owner's retires; the current buddy is remembered so the Vissenbotje finds him.
     */
    static void houdBij(SmikagolEntity s) {
        UUID eigenaar = s.eigenaar();
        if (eigenaar == null) {
            return;   // (a Smikagol from a spawn egg or a command: nobody's)
        }
        if (!s.isMaatje()) {
            if (!s.getUUID().equals(GIDSEN.get(eigenaar))) {
                s.discard();
            }
            return;
        }
        if (s.level().getServer().getPlayerList().getPlayer(eigenaar) instanceof ServerPlayer p) {
            if (s.generatie() != GuhQuests.saved(p).getIntOr(GENERATIE, 0)) {
                s.discard();
            } else {
                MAATJES.put(eigenaar, s.getUUID());
            }
        }
    }

    static void vergeet(ServerPlayer p) {
        wegGids(p);
        MAATJES.remove(p.getUUID());
    }

    static void wisAlles() {
        GIDSEN.clear();
        MAATJES.clear();
    }

    /** Smikagols vissenbotje: a gnawed fish bone on a string. Use it and your own Smikagol comes scurrying. */
    public static class Botje extends Item {
        public Botje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                if (!roepMaatje(p)) {
                    p.sendOverlayMessage(Component.translatable("quest.guhs.ring.smikagol.geen_maatje").withStyle(ChatFormatting.YELLOW));
                }
                p.getCooldowns().addCooldown(stack, 60);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    private Smikagol() {
    }
}
