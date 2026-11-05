package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
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

/**
 * The effects of the zone-4 named boss items (the line under "Effect:" on the item, lang {@code vadsrijk.bazen.effect.z4baas_*}):
 * <ul>
 *   <li>Slurfstaf: +15 % schade op wezens in het water.</li>
 *   <li>Zeekoeienhuid: in het water krijg je 15 % minder schade.</li>
 *   <li>Lagunelampion: je geeft zelf licht in het donker (the zone-3 lantern rule).</li>
 *   <li>Oorlogsvliegerboog: +20 % schade op vliegende wezens.</li>
 *   <li>Kootjes Enterhaak: +15 % schade op wezens die Plakkerig zijn.</li>
 *   <li>Kaasdoek: onder 30 % Vads 3 s Snelpootjes (30 s afkoelen).</li>
 * </ul>
 */
public final class Z4BazenEffecten {
    private static final Map<String, Long> AFKOEL = new HashMap<>();

    private Z4BazenEffecten() {
    }

    static void register() {
        Schade.VOOR_KLAP.add(Z4BazenEffecten::voorKlap);
        Schade.NA_KLAP.add(Z4BazenEffecten::naKlap);
        Lantaarnlicht.DRAGERS.add(p -> p instanceof ServerPlayer sp && "z4baas_lagunelampion".equals(sieraad(sp, Slot.OORBEL)));
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

    private static boolean eigen(Schade.Klap k) {
        String s = k.skillId() == null ? "" : k.skillId();
        return s.equals("status");
    }

    private static double voorKlap(Schade.Klap k, double n) {
        if (eigen(k)) {
            return n;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob m) {
            String w = wapen(p);
            if ("z4baas_slurfstaf".equals(w) && m.isInWater()) {
                n *= 1.15;
            }
            if ("z4baas_oorlogsvliegerboog".equals(w) && !m.onGround() && m.isNoGravity()) {
                n *= 1.2;
            }
            if ("z4baas_enterhaak".equals(w) && Statussen.heeft(m, Status.PLAKKERIG)) {
                n *= 1.15;
            }
        }
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && p.isInWater() && "z4baas_zeekoeienhuid".equals(gedragen(p, EquipmentSlot.CHEST))) {
            n *= 0.85;
        }
        return n;
    }

    private static void naKlap(Schade.Uitkomst u) {
        if (u.schade() <= 0 || !(u.klap().doel() instanceof ServerPlayer p) || !Vadsrijk.in(p) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        if ("z4baas_kaasdoek".equals(gedragen(p, EquipmentSlot.HEAD)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3) {
            String key = p.getUUID() + "kaasdoek";
            long nu = sl.getGameTime();
            if (nu >= AFKOEL.getOrDefault(key, 0L)) {
                AFKOEL.put(key, nu + 600);
                Statussen.geef(p, Status.SNELPOOTJES, 60, 0.3, p);
            }
        }
    }
}
