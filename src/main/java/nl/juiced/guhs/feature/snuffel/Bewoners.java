package nl.juiced.guhs.feature.snuffel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The residents: making them and what they do.
 * <ul>
 *   <li>On the island a resident is a line of eiland.json ({@code "bewoners"}): the kern keeps it on its spot.</li>
 *   <li>Anywhere else: {@link #plaats} (a named resident) or {@link #plaatsHond} (any dog) puts one in the world.</li>
 *   <li>What a resident says is its {@link Rol}, registered by key ({@link #zetRol}): the {@code sleutel} of its line in
 *   eiland.json, or the resident's name id for one placed by hand. Talk with the mod's talking screen
 *   ({@code Praat.open(p, npc, "<pkg>_<x>", key, args, opties)} + {@code Praat.luister}) or a chat line
 *   ({@code GuhQuests.say}). A resident without a role greets; Kapitein Zoutsnoet (key {@code kapitein}) offers the trip
 *   home until somebody gives him another role.</li>
 * </ul>
 */
public final class Bewoners {
    /** What a resident does. Everything in it is per player: the resident is the same dog for everybody. */
    public interface Rol {
        /** Right-clicked by a player (server side). */
        void praat(BewonerEntity npc, ServerPlayer p);

        /** Twice a second (server side). */
        default void tick(BewonerEntity npc) {
        }
    }

    public static final String KAPITEIN = "kapitein";
    private static final String KAPITEIN_SLEUTEL = "snuffel_kapitein";
    private static final Map<String, Rol> ROLLEN = new ConcurrentHashMap<>();

    private Bewoners() {
    }

    static void init() {
        Praat.luister(KAPITEIN_SLEUTEL, (p, spreker, optie) -> {
            if (optie == 1) {
                Reis.naarHuis(p);
            }
        });
    }

    /** The role of the resident with this key (one per key; a second call replaces the first). */
    public static void zetRol(String sleutel, Rol rol) {
        ROLLEN.put(sleutel, rol);
    }

    @Nullable
    public static Rol rol(String sleutel) {
        return ROLLEN.get(sleutel);
    }

    static void klik(BewonerEntity npc, ServerPlayer p) {
        if (Cutscenes.bezig(p)) {
            return;
        }
        Rol rol = rol(npc.sleutel());
        if (rol != null) {
            rol.praat(npc, p);
        } else if (KAPITEIN.equals(npc.sleutel()) || KAPITEIN.equals(npc.bewoner())) {
            kapitein(npc, p);
        } else {
            GuhQuests.say(p, npc, npc.pup() ? "quest.guhs.snuffel.bewoner.hallo_pup" : "quest.guhs.snuffel.bewoner.hallo");
        }
    }

    /** The harbour captain's own offer: he sails a dog home (the island slice may give him a boat scene first). */
    public static void kapitein(BewonerEntity npc, ServerPlayer p) {
        if (!Hondvorm.actief(p)) {
            GuhQuests.say(p, npc, "quest.guhs.snuffel.kapitein.geen_hond");
            return;
        }
        Praat.open(p, npc, KAPITEIN_SLEUTEL, "quest.guhs.snuffel.kapitein.vraag", new Object[0], new Praat.Optie(1, "gui.guhs.snuffel.optie.naar_huis"),
                new Praat.Optie(0, "gui.guhs.snuffel.optie.blijven"));
    }

    /** A resident for a spot of the island (not added to the level yet). */
    @Nullable
    static BewonerEntity maak(ServerLevel level, Eiland.BewonerPlek b, Vec3 plek) {
        BewonerEntity e = SnuffelFeature.SNUFFEL_BEWONER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        if (!b.bewoner().isEmpty()) {
            if (Honden.bewoner(b.bewoner()) == null) {
                return null;
            }
            e.zetBewoner(b.bewoner());
        } else {
            if (Honden.ras(b.ras()) == null) {
                return null;
            }
            e.zetHond(b.ras(), b.kleur(), b.pup());
        }
        e.zetHouding(b.houding());
        zet(e, plek, b.yaw());
        return e;
    }

    private static void zet(BewonerEntity e, Vec3 plek, float yaw) {
        e.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
    }

    /** Puts a named resident (the generator's list: "dokter", "trainer", "kapitein"...) in the world. Null: unknown name. */
    @Nullable
    public static BewonerEntity plaats(ServerLevel level, Vec3 plek, float yaw, String bewoner) {
        if (Honden.bewoner(bewoner) == null) {
            return null;
        }
        BewonerEntity e = SnuffelFeature.SNUFFEL_BEWONER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.zetBewoner(bewoner);
        zet(e, plek, yaw);
        level.addFreshEntity(e);
        return e;
    }

    /** Puts any dog in the world: a breed, one of its coats, grown or a puppy. Null: unknown breed. */
    @Nullable
    public static BewonerEntity plaatsHond(ServerLevel level, Vec3 plek, float yaw, String ras, String kleur, boolean pup) {
        if (Honden.ras(ras) == null) {
            return null;
        }
        BewonerEntity e = SnuffelFeature.SNUFFEL_BEWONER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.zetHond(ras, kleur, pup);
        zet(e, plek, yaw);
        level.addFreshEntity(e);
        return e;
    }
}
