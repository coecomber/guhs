package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.lang.reflect.Method;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Adapters to other slices that did not exist when this was written (NOTITIES_BAZEN.md lists them for integration):
 * <ul>
 *   <li>wezens: the temple mobs by entity id ({@link #RIDDER}, {@link #PRIESTER}); missing = kern's grey test klodder (Natuur).</li>
 *   <li>wereld: {@code wereld.WereldDeuren.open/sluit(ServerLevel, String)} for the temple door, by reflection; missing = false
 *       (the bazen fallback slides the door blocks away itself with {@link Herstel}).</li>
 * </ul>
 */
public final class Adapters {
    public static final String RIDDER = "guhs:vr_mob_tempelzwamridder", PRIESTER = "guhs:vr_mob_sporenpriester";
    private static final String DEUREN = "nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren";

    private Adapters() {
    }

    /** Spawns a wezens mob by id (level, element Natuur, elite), or the kern placeholder. */
    @Nullable
    public static VadsrijkMob spawn(ServerLevel sl, String id, BlockPos pos, int level, boolean elite) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
        Entity e = type == null ? null : type.create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (!(e instanceof VadsrijkMob)) {
            if (e != null) {
                e.discard();
            }
            e = KernRegistry.PROEFKLODDER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        }
        if (!(e instanceof VadsrijkMob m)) {
            return null;
        }
        m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.getRandom().nextFloat() * 360, 0);
        m.stelIn(level, Element.NATUUR, elite, Set.of());
        if (m instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        sl.addFreshEntity(m);
        return m;
    }

    private static boolean deur(String methode, ServerLevel sl, String deur) {
        try {
            Class<?> c = Class.forName(DEUREN);
            Method m = c.getMethod(methode, ServerLevel.class, String.class);
            Object r = m.invoke(null, sl, deur);
            return !(r instanceof Boolean b) || b;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    public static boolean deurOpen(ServerLevel sl, String deur) {
        return deur("open", sl, deur);
    }

    public static boolean deurDicht(ServerLevel sl, String deur) {
        return deur("sluit", sl, deur);
    }
}
