package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Lantaarnlicht;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;

/**
 * The effects of the zone-6 named boss items (the "Effect:" line, lang {@code vadsrijk.bazen.effect.z6baas_*}):
 * <ul>
 *   <li>Wormelientand: every 4th hit does +40 % damage.</li>
 *   <li>Duinzwemmerslaarzen: on sand you run 15 % faster (Snelpootjes).</li>
 *   <li>Wimperwaaier: in Het Zijdeland you take 8 % less damage.</li>
 *   <li>Zingend Zoutzwaard: +20 % damage to constructs (Zoutkolos, Spiegelslang, Zandgeestje, Zandvlooi, Zoutslang, Astrolabiumwachter).</li>
 *   <li>Zoutsplinterboog: +15 % damage to foes more than 10 blocks away.</li>
 *   <li>Zoutkristalkroon: below 30 % Vads a salt shield (Knuffelschild 15 % of your max Vads; 45 s cooldown).</li>
 *   <li>Astrolabiumring: every 11 s your next hit does +30 %.</li>
 *   <li>Glimmertjes Dover: your hits make foes dizzy (10 % chance, 1 s).</li>
 *   <li>Sterrenkijkerkruisboog: at night +20 % damage.</li>
 *   <li>Sterrenhartamulet: at night you give light yourself (the zone-3 lantern rule) and your elements hit 6 % harder.</li>
 * </ul>
 */
public final class Z6BazenEffecten {
    private static final Map<String, Long> AFKOEL = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TELLER = new ConcurrentHashMap<>();

    private Z6BazenEffecten() {
    }

    public static void register() {
        Schade.VOOR_KLAP.add(Z6BazenEffecten::voorKlap);
        Schade.NA_KLAP.add(Z6BazenEffecten::naKlap);
        Lantaarnlicht.DRAGERS.add(p -> p instanceof ServerPlayer sp && !sp.level().isBrightOutside() && "z6baas_sterrenhartamulet".equals(sieraad(sp, Slot.KETTING)));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0 && Vadsrijk.in(p) && "z6baas_duinzwemmerslaarzen".equals(gedragen(p, EquipmentSlot.FEET))
                    && opZand(p)) {
                Statussen.geef(p, Status.SNELPOOTJES, 30, 0.15, p);
            }
        });
    }

    /** On sand / sandstone / the zone-6 sand blocks? */
    public static boolean opZand(LivingEntity e) {
        BlockState b = e.level().getBlockState(e.getBlockPosBelowThatAffectsMyMovement());
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock()).getPath();
        return b.is(BlockTags.SAND) || id.contains("sand") || id.contains("zand");
    }

    @Nullable
    static String gedragen(ServerPlayer p, EquipmentSlot slot) {
        Uitrusting u = Rollen.van(p.getItemBySlot(slot));
        return u == null || u.verpluisd() || u.level() > Spelers.level(p) ? null : u.type();
    }

    @Nullable
    static String wapen(ServerPlayer p) {
        Uitrusting u = Spelers.wapen(p);
        return u == null || u.verpluisd() ? null : u.type();
    }

    @Nullable
    static String sieraad(ServerPlayer p, Slot s) {
        Uitrusting u = Rollen.van(Sieraden.get(p, s));
        return u == null || u.verpluisd() ? null : u.type();
    }

    private static boolean heeftRing(ServerPlayer p, String type) {
        return type.equals(sieraad(p, Slot.RING)) || type.equals(sieraad(p, Slot.RING2));
    }

    /** A construct (no cheer-up): the constructs of zone 6. */
    public static boolean construct(LivingEntity e) {
        return e instanceof Z6Wezen w && w.afloop() == Z6Wezen.Afloop.VERSLAAN || e instanceof Zoutkolos || e instanceof Astrolabiumwachter;
    }

    private static boolean klaar(String key, long nu, long ticks) {
        if (nu < AFKOEL.getOrDefault(key, 0L)) {
            return false;
        }
        AFKOEL.put(key, nu + ticks);
        return true;
    }

    private static double voorKlap(Schade.Klap k, double n) {
        String skill = k.skillId() == null ? "" : k.skillId();
        if (skill.equals("status")) {
            return n;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob m && p.level() instanceof ServerLevel sl) {
            String w = wapen(p);
            long nu = sl.getGameTime();
            if ("z6baas_wormelientand".equals(w) && TELLER.merge(p.getUUID() + "tand", 1, Integer::sum) % 4 == 0) {
                n *= 1.4;
            }
            if ("z6baas_zingend_zoutzwaard".equals(w) && construct(m)) {
                n *= 1.2;
            }
            if ("z6baas_zoutsplinterboog".equals(w) && m.distanceTo(p) > 10) {
                n *= 1.15;
            }
            if ("z6baas_sterrenkijkerkruisboog".equals(w) && !sl.isBrightOutside()) {
                n *= 1.2;
            }
            if ("z6baas_glimmertjes_dover".equals(w) && p.getRandom().nextDouble() < 0.1) {
                Statussen.geef(m, Status.DUIZELIG, 20, 1, p);
            }
            if (heeftRing(p, "z6baas_astrolabiumring") && klaar(p.getUUID() + "astro", nu, 220)) {
                n *= 1.3;
            }
            if ("z6baas_sterrenhartamulet".equals(sieraad(p, Slot.KETTING)) && !sl.isBrightOutside() && k.element() != null
                    && k.element() != nl.juiced.guhs.feature.vadsrijk.kern.Element.GEEN) {
                n *= 1.06;
            }
        }
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && Z6WezensPlekken.inZone(p.getX(), p.getZ()) && "z6baas_wimperwaaier".equals(sieraad(p, Slot.OORBEL))) {
            n *= 0.92;
        }
        return n;
    }

    private static void naKlap(Schade.Uitkomst u) {
        if (u.schade() <= 0 || !(u.klap().doel() instanceof ServerPlayer p) || !Vadsrijk.in(p) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        if ("z6baas_zoutkristalkroon".equals(gedragen(p, EquipmentSlot.HEAD)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3
                && klaar(p.getUUID() + "kroon", sl.getGameTime(), 900)) {
            Statussen.geef(p, Status.KNUFFELSCHILD, 120, Spelers.maxVads(p) * 0.15, p);
        }
    }
}
