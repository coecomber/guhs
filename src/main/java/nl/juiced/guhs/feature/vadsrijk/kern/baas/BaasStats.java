package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * 1.10.3 "Bazen" tab: per-character boss statistics, on the character ({@code Spelers.data(p).extra("baasstats")}), one compound per
 * boss id: {@code kills}, {@code eerste} (epoch ms of the first counted kill), {@code snelst} (seconds, 0 = unknown), {@code schade},
 * {@code heal}, {@code tank}, {@code lv} (level at the last kill), {@code munten}, {@code items} (other items), {@code r} (gear count
 * per rarity), {@code sp} (the special drops as component JSON, newest last, at most {@link #MAX_SP}) and {@code oud} (counted from an
 * old first-kill key: "at least once").
 * <p>Recorded from {@code VadsrijkMob.verslagen} ({@link #kill} before the rewards, {@link #metBaas} around them so that every
 * {@link BaasKist#plaats} in between counts as this boss's drop), plus {@link #speciaal} (a rare mount / pet). The boss id of an entity
 * type comes from {@link #ID} (the Bazen registry sets it; default: the type id itself).
 */
public final class BaasStats {
    public static final String DATA = "baasstats";
    public static final int MAX_SP = 16;
    /** Entity type id ("guhs:vr_baas_opperzwam") -> boss id of the Bazen tab (null: not a listed boss: its type id is used). */
    public static Function<String, String> ID = t -> null;
    @Nullable
    private static String huidig;

    private BaasStats() {
    }

    public static CompoundTag alle(ServerPlayer p) {
        return Spelers.data(p).extra(DATA);
    }

    public static CompoundTag van(ServerPlayer p, String baas) {
        CompoundTag a = alle(p);
        if (!a.contains(baas)) {
            a.put(baas, new CompoundTag());
        }
        return a.getCompoundOrEmpty(baas);
    }

    public static String idVan(String type) {
        String id = ID.apply(type);
        return id != null ? id : type;
    }

    /** The defeat of a boss: a kill (+ time, damage, healing, tanked, level) for every helper. Returns the boss id. */
    @Nullable
    public static String kill(VadsrijkBaas b, Set<ServerPlayer> helpers) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(b.getType()).toString();
        String id = idVan(type);
        Bijdrage bd = b.bijdragen();
        long tijd = b.level().getGameTime();
        for (ServerPlayer p : helpers) {
            double[] w = bd.van(p.getUUID());
            registreer(p, id, type, tijd, bd.seconden(), w[0], w[1], w[2], b.mobLevel());
        }
        return id;
    }

    /**
     * One counted kill. A boss of several parts (the three Sterrenbeelden) counts once: a kill of ANOTHER type of the same boss
     * within 30 s only adds its numbers.
     */
    public static void registreer(ServerPlayer p, String id, String type, long tijd, double sec, double schade, double heal, double tank, int lv) {
        CompoundTag t = van(p, id);
        String vorigType = t.getStringOr("lt_type", "");
        long vorig = t.getLongOr("lt", -10000L);
        boolean deel = !vorigType.isEmpty() && !vorigType.equals(type) && tijd - vorig >= 0 && tijd - vorig < 600;
        if (!deel) {
            t.putInt("kills", t.getIntOr("kills", 0) + 1);
            if (!t.contains("eerste")) {
                t.putLong("eerste", System.currentTimeMillis());
            }
            if (sec > 0.5) {
                double s = t.getDoubleOr("snelst", 0);
                t.putDouble("snelst", s <= 0 ? sec : Math.min(s, sec));
            }
        }
        t.putString("lt_type", type);
        t.putLong("lt", tijd);
        t.putDouble("schade", t.getDoubleOr("schade", 0) + schade);
        t.putDouble("heal", t.getDoubleOr("heal", 0) + heal);
        t.putDouble("tank", t.getDoubleOr("tank", 0) + tank);
        if (lv > 0) {
            t.putInt("lv", lv);
        }
    }

    /** Runs {@code r} with {@code baas} as the boss whose chests these are ({@link BaasKist#plaats} -> {@link #kist}). */
    public static void metBaas(@Nullable String baas, Runnable r) {
        String vorige = huidig;
        huidig = baas;
        try {
            r.run();
        } finally {
            huidig = vorige;
        }
    }

    /** A boss chest for this player: counted under the boss of {@link #metBaas} (nothing outside a defeat). */
    public static void kist(ServerPlayer p, LootTabel.Resultaat r) {
        if (huidig != null && r != null) {
            drops(p, huidig, r);
        }
    }

    /** The contents of a chest of boss {@code id}: coins, gear per rarity (Legendary+ also by name), other items. */
    public static void drops(ServerPlayer p, String id, LootTabel.Resultaat r) {
        CompoundTag t = van(p, id);
        t.putLong("munten", t.getLongOr("munten", 0) + Math.max(0, r.munten()));
        CompoundTag z = t.getCompoundOrEmpty("r");
        for (ItemStack s : r.items()) {
            Uitrusting u = Rollen.van(s);
            if (u == null) {
                t.putInt("items", t.getIntOr("items", 0) + s.getCount());
                continue;
            }
            z.putInt(u.zeldzaamheid().name(), z.getIntOr(u.zeldzaamheid().name(), 0) + 1);
            if (u.zeldzaamheid().ordinal() >= Zeldzaamheid.LEGENDARISCH.ordinal()) {
                speciaal(p, id, s.getHoverName());
            }
        }
        t.put("r", z);
    }

    /** A special drop (a Legendary+ piece, a rare mount or pet) of boss {@code id}, remembered by name. */
    public static void speciaal(ServerPlayer p, @Nullable String id, Component naam) {
        if (id == null) {
            return;
        }
        CompoundTag t = van(p, id);
        ListTag l = t.getListOrEmpty("sp");
        String json;
        try {
            json = net.minecraft.network.chat.ComponentSerialization.CODEC
                    .encodeStart(p.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), naam).getOrThrow().toString();
        } catch (RuntimeException e) {
            json = "\"" + naam.getString().replace("\"", "'") + "\"";
        }
        l.add(StringTag.valueOf(json));
        while (l.size() > MAX_SP) {
            l.remove(0);
        }
        t.put("sp", l);
    }

    /** Before 1.10.3 only the first kill was remembered ("baaskill:..."): such a boss counts as beaten at least once. */
    public static void zaai(ServerPlayer p, String id) {
        CompoundTag t = van(p, id);
        if (t.getIntOr("kills", 0) <= 0) {
            t.putInt("kills", 1);
            t.putBoolean("oud", true);
        }
    }
}
