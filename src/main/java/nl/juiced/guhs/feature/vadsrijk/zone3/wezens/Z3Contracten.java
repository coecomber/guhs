package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;

/**
 * The 8 named hunting targets of the Jachtbord (ZONE3_PLAN §8.4, user OK on "alleen via een contract"): they NEVER spawn on their
 * own. The bounty board (z3-npc / z3-systemen) calls {@link #spawnElite(ServerPlayer, String)} when a player takes a contract:
 * the target appears at its lair ({@link Z3WezensPlekken#HOLEN}, near a Pluimpost) for that player and their party only (only
 * they see it, hit it and are attacked by it), with 2 fixed affixes and a mechanic of its own (in the creature classes), about 6x
 * an ordinary creature's health. Defeated: a rich loot roll ("z3wezens:contract_&lt;id&gt;") + its trophy per player, and
 * {@link #VERSLAGEN} fires (the board pays the Jachtpenningen, the daily chest and the 3 % Pluimguh pattern: not here). It
 * leaves on its own after 12 minutes or 30 s without its players nearby ({@link #VERTROKKEN} fires).
 */
public final class Z3Contracten {
    /** One target: its creature kind, level, the two fixed affixes. */
    public record Doelwit(String id, Supplier<? extends EntityType<? extends Z3Wezen>> type, int level, Affix a, Affix b) {
    }

    public static final Map<String, Doelwit> DOELWITTEN = new LinkedHashMap<>();

    static {
        d("ouwe_rotkeiler", Z3WezensRegistry.ROTZWIJN, 48, Affix.SNEL, Affix.GLOEIEND);              // charges twice
        d("moeder_wesp", Z3WezensRegistry.GRAUWE_WESP, 46, Affix.SCHILDEND, Affix.SNEL);             // splits her swarm at 50 %
        d("de_harsreus", Z3WezensRegistry.HARSGOLEM, 47, Affix.SCHILDEND, Affix.GLOEIEND);           // three rings of shockwaves
        d("kapitein_kaaskop", Z3WezensRegistry.KAASSCHUTTER, 46, Affix.SNEL, Affix.SCHILDEND);       // three cheese balls in a fan
        d("grootvader_rups", Z3WezensRegistry.GRAUWE_RUPS, 45, Affix.SCHILDEND, Affix.GLOEIEND);     // rolls three times
        d("de_stille_ziener", Z3WezensRegistry.DOVE_ZIENER, 50, Affix.GLOEIEND, Affix.SCHILDEND);    // hears sneaking within 6, 3 shadows
        d("slangenkoningin_sapsie", Z3WezensRegistry.WORTELSLANG, 49, Affix.SNEL, Affix.GLOEIEND);   // sap pools where she comes up
        d("bramzwijn_de_brede", Z3WezensRegistry.ROTZWIJN, 48, Affix.GLOEIEND, Affix.SCHILDEND);     // a three-wide charge + brambles
    }

    private static void d(String id, Supplier<? extends EntityType<? extends Z3Wezen>> type, int level, Affix a, Affix b) {
        DOELWITTEN.put(id, new Doelwit(id, type, level, a, b));
    }

    public static final List<String> IDS = List.copyOf(DOELWITTEN.keySet());
    /** Health on top of the elite x2.5 (so ~6x an ordinary creature: a party fight, still soloable at lv 50). */
    public static final double EXTRA_VADS = 2.4;

    /** Fires for every contract player who helped defeat it: (player, contract id). */
    public static final List<BiConsumer<ServerPlayer, String>> VERSLAGEN = new CopyOnWriteArrayList<>();
    /** Fires when a target left without being defeated (timeout / players gone): (contract id, its players). */
    public static final List<BiConsumer<String, List<UUID>>> VERTROKKEN = new CopyOnWriteArrayList<>();

    /** Active targets: "id|owner uuid" -> entity uuid. */
    private static final Map<String, UUID> ACTIEF = new HashMap<>();

    private Z3Contracten() {
    }

    /** The lair of a target (for the map trail), or null for an unknown id. */
    @Nullable
    public static BlockPos hol(String id) {
        return Z3WezensPlekken.HOLEN.get(id);
    }

    /**
     * The bounty board's call: spawns target {@code id} at its lair for this player and their party (members online in the
     * dimension). Returns the creature, the one already out for this player, or null (unknown id / not in the Vadsrijk / no room).
     */
    @Nullable
    public static Z3Wezen spawnElite(ServerPlayer p, String id) {
        Doelwit d = DOELWITTEN.get(id);
        if (d == null || !(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        UUID bestaand = ACTIEF.get(id + "|" + p.getUUID());
        if (bestaand != null && sl.getEntity(bestaand) instanceof Z3Wezen w && w.isAlive() && !w.geplofd()) {
            return w;
        }
        BlockPos hol = hol(id);
        if (hol == null) {
            return null;
        }
        List<UUID> eigenaars = new ArrayList<>();
        eigenaars.add(p.getUUID());
        if (Partijen.party(p) != null) {
            for (ServerPlayer l : Partijen.leden(p)) {
                if (!eigenaars.contains(l.getUUID())) {
                    eigenaars.add(l.getUUID());
                }
            }
        }
        return maak(sl, d, plek(sl, hol), eigenaars);
    }

    /** Makes the target at exactly this spot (spawnElite, the dev command, the gametests). */
    @Nullable
    public static Z3Wezen maak(ServerLevel sl, Doelwit d, BlockPos plek, List<UUID> eigenaars) {
        Z3Wezen w = d.type().get().create(sl, EntitySpawnReason.EVENT);
        if (w == null) {
            return null;
        }
        w.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, sl.getRandom().nextFloat() * 360, 0);
        w.stelIn(d.level(), null, true, EnumSet.of(d.a(), d.b()));
        w.zetMaxVadsContract(EXTRA_VADS);
        w.maakContract(d.id(), eigenaars);
        sl.addFreshEntity(w);
        for (UUID u : eigenaars) {
            ACTIEF.put(d.id() + "|" + u, w.getUUID());
        }
        sl.sendParticles(ParticleTypes.POOF, w.getX(), w.getY() + 1, w.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
        sl.playSound(null, plek, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6f, 1.4f);
        return w;
    }

    /** A free standing spot at the lair: searching down from 12 above to 24 below, a few tries around it. */
    static BlockPos plek(ServerLevel sl, BlockPos hol) {
        for (int poging = 0; poging < 10; poging++) {
            int x = hol.getX() + (poging == 0 ? 0 : sl.getRandom().nextInt(9) - 4), z = hol.getZ() + (poging == 0 ? 0 : sl.getRandom().nextInt(9) - 4);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int y = hol.getY() + 12; y >= hol.getY() - 24; y--) {
                m.set(x, y - 1, z);
                if (sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() || !sl.getFluidState(m).isEmpty()) {
                    continue;
                }
                m.set(x, y, z);
                boolean vrij1 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty() && sl.getFluidState(m).isEmpty();
                m.set(x, y + 1, z);
                boolean vrij2 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
                m.set(x, y + 2, z);
                boolean vrij3 = sl.getBlockState(m).getCollisionShape(sl, m).isEmpty();
                if (vrij1 && vrij2 && vrij3) {
                    return new BlockPos(x, y, z);
                }
            }
        }
        return hol;
    }

    /** Is a target of this contract out for this player right now? */
    public static boolean actief(ServerPlayer p, String id) {
        UUID u = ACTIEF.get(id + "|" + p.getUUID());
        return u != null && p.level() instanceof ServerLevel sl && sl.getEntity(u) instanceof Z3Wezen w && w.isAlive() && !w.geplofd();
    }

    /** The trophy of a target (an empty stack for an unknown id / helpers). */
    public static ItemStack trofee(String id) {
        if (!DOELWITTEN.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse("guhs:vr_z3_mob_trofee_" + id)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** From Z3Wezen.beloon: tell the board, forget it. */
    public static void verslagen(Z3Wezen w, Set<ServerPlayer> helpers) {
        String id = w.contract();
        if (!DOELWITTEN.containsKey(id)) {
            return;
        }
        for (UUID u : w.eigenaars()) {
            ACTIEF.remove(id + "|" + u);
        }
        for (ServerPlayer p : helpers) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z3wezens.contract.verslagen", Component.translatable("vadsrijk.z3wezens.contract." + id)));
            for (BiConsumer<ServerPlayer, String> h : VERSLAGEN) {
                h.accept(p, id);
            }
        }
    }

    /** From Z3Wezen: it left (timeout / players gone). */
    public static void vertrokken(Z3Wezen w) {
        String id = w.contract();
        if (!DOELWITTEN.containsKey(id)) {
            return;
        }
        List<UUID> e = w.eigenaars();
        for (UUID u : e) {
            ACTIEF.remove(id + "|" + u);
            if (w.level() instanceof ServerLevel sl && sl.getPlayerByUUID(u) instanceof ServerPlayer p && Vadsrijk.in(p)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z3wezens.contract.vertrokken", Component.translatable("vadsrijk.z3wezens.contract." + id)));
            }
        }
        for (BiConsumer<String, List<UUID>> h : VERTROKKEN) {
            h.accept(id, e);
        }
    }

    public static void vergeet() {
        ACTIEF.clear();
    }
}
