package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.schot.Schoten;

/**
 * The basic attack of the ranged classes (CONTRACT 7: "left click with a class weapon"): holding the left mouse button with a
 * bow, crossbow, catapult, staff, wand or book fires real projectiles through the kern damage pipeline. The client sends
 * {@link KlassenPayloads.Schiet} while the button is down; the server keeps the rhythm (the weapon's interval, a weapon
 * stat in kern.Aanvalstempo, faster with Aanvalssnelheid) and buffers a press that comes a few ticks early, so holding the button gives a steady stream.
 */
public final class Basisaanval {
    /**
     * How a weapon kind shoots: snelheid blocks/tick; spat = splash radius (50 % to the others). The interval is a weapon stat
     * now (fix round 1: kern.Aanvalstempo, e.g. boog 1.1 s, kruisboog 2.0 s), not part of the kind.
     */
    public record Soort(double snelheid, double zwaarte, double sturen, double spat, int doorboren, @Nullable String item,
            boolean schuin, float schaal, Schade.Soort schade) {
    }

    public static final Map<String, Soort> SOORTEN = Map.of(
            "boog", new Soort(2.6, 0.012, 0, 0, 0, "minecraft:arrow", true, 0.85f, Schade.Soort.AFSTAND),
            "kruisboog", new Soort(3.6, 0.006, 0, 0, 1, "minecraft:spectral_arrow", true, 1.0f, Schade.Soort.AFSTAND),
            "katapult", new Soort(1.9, 0.045, 0, 1.6, 0, "minecraft:cocoa_beans", false, 0.55f, Schade.Soort.AFSTAND),
            "toverstok", new Soort(2.0, 0, 0.12, 0, 0, null, false, 0, Schade.Soort.MAGIE),
            "staf", new Soort(1.5, 0, 0.05, 1.5, 0, null, false, 0, Schade.Soort.MAGIE),
            "boek", new Soort(1.3, 0, 0.35, 0, 0, "minecraft:paper", false, 0.45f, Schade.Soort.MAGIE));

    private static final Map<UUID, Long> KLAAR = new HashMap<>();
    private static final Map<UUID, Long> WACHT = new HashMap<>();
    private static final Map<UUID, Long> MELDING = new HashMap<>();
    private static final Map<UUID, Integer> TELLER = new HashMap<>();
    /** Rolsalto: the next shot until this game time is a guaranteed crit. */
    static final Map<UUID, Long> KRIT_TOT = new HashMap<>();

    private Basisaanval() {
    }

    /** Rolsalto: the next shot until {@code tot} (game time) is a guaranteed crit. */
    public static void geefSaltoKrit(ServerPlayer p, long tot) {
        KRIT_TOT.put(p.getUUID(), tot);
    }

    /** Uses up the Rolsalto crit (true when there was one). */
    public static boolean neemSaltoKrit(ServerPlayer p) {
        Long t = KRIT_TOT.remove(p.getUUID());
        return t != null && p.level().getGameTime() <= t;
    }

    /** Is this stack a ranged/magic class weapon (client and server)? */
    public static boolean isAfstandswapen(ItemStack s) {
        Uitrusting u = Rollen.van(s);
        BasisType t = u == null ? null : u.basisType();
        return t != null && SOORTEN.containsKey(t.soort());
    }

    /** Ticks until the next basic shot: the held weapon's interval (kern.Aanvalstempo) / (1 + Aanvalssnelheid%). */
    public static int interval(ServerPlayer p) {
        return Aanvalstempo.ticks(Aanvalstempo.seconden(p, Spelers.stats(p).get(Stat.AANVALSSNELHEID)));
    }

    /** The client's left mouse button is down with a ranged weapon. */
    public static void verzoek(ServerPlayer p) {
        if (!Vadsrijk.in(p) || p.isSpectator() || RijkScenes.bezig(p) || Statussen.heeft(p, Status.VERDOOFD)) {
            return;
        }
        if (!Spelers.voorAanval(p)) {   // g-klassen: weapon requirement (class level) + switch to this weapon's class
            return;
        }
        long nu = p.level().getGameTime();
        long klaar = KLAAR.getOrDefault(p.getUUID(), 0L);
        if (nu >= klaar) {
            schiet(p);
        } else if (klaar - nu <= 4) {
            WACHT.put(p.getUUID(), klaar);
        }
    }

    static void tick(MinecraftServer server) {
        if (WACHT.isEmpty()) {
            return;
        }
        WACHT.entrySet().removeIf(e -> {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                return true;
            }
            if (p.level().getGameTime() >= e.getValue()) {
                schiet(p);
                return true;
            }
            return false;
        });
    }

    static void vergeet(UUID id) {
        KLAAR.remove(id);
        WACHT.remove(id);
        TELLER.remove(id);
        KRIT_TOT.remove(id);
    }

    private static void schiet(ServerPlayer p) {
        Uitrusting u = Spelers.wapen(p);
        BasisType t = u == null ? null : u.basisType();
        Soort s = t == null ? null : SOORTEN.get(t.soort());
        if (s == null || !Spelers.voorAanval(p)) {   // (g-klassen: swapped to a melee weapon meanwhile, or a weapon you can't use yet)
            return;
        }
        long nu = p.level().getGameTime();
        KLAAR.put(p.getUUID(), nu + interval(p));
        int n = TELLER.merge(p.getUUID(), 1, Integer::sum);
        boolean jachtroes = Boom.heeft(p, "j_jachtroes") && n % 4 == 0;
        Long krit = KRIT_TOT.get(p.getUUID());
        boolean salto = krit != null && nu <= krit;
        if (salto) {
            KRIT_TOT.remove(p.getUUID());
        }
        Element el = Spelers.wapenElement(p);
        double basis = Spelers.klapSchade(p);   // (one shot = damage per second x the weapon's interval)
        Vec3 kijk = p.getLookAngle();
        vuur(p, t.soort(), s, kijk, basis, el, jachtroes || salto, true);
        if (jachtroes) {
            for (int z : new int[] {-1, 1}) {
                vuur(p, t.soort(), s, kijk.yRot((float) Math.toRadians(10 * z)), basis * 0.6, el, false, false);
            }
        }
        ServerLevel sl = (ServerLevel) p.level();
        Vec3 hand = p.getEyePosition().add(kijk.scale(0.6)).add(0, -0.15, 0);
        switch (t.soort()) {
            case "boog" -> Fx.geluid(sl, hand, SoundEvents.ARROW_SHOOT, 0.7f, 1.2f + p.getRandom().nextFloat() * 0.2f);
            case "kruisboog" -> Fx.geluid(sl, hand, SoundEvents.CROSSBOW_SHOOT, 0.8f, 1.1f);
            case "katapult" -> Fx.geluid(sl, hand, SoundEvents.SNOWBALL_THROW, 0.8f, 0.7f);
            case "boek" -> Fx.geluid(sl, hand, SoundEvents.BOOK_PAGE_TURN, 1f, 1.3f);
            default -> Fx.geluid(sl, hand, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1.6f + p.getRandom().nextFloat() * 0.3f);
        }
        if (s.schade() == Schade.Soort.MAGIE) {
            sl.sendParticles(Fx.stof(Fx.kleur(el), 1.2f), hand.x, hand.y, hand.z, 6, 0.1, 0.1, 0.1, 0.01);
        }
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    /** One projectile of the basic attack (also used by the Saltosalvo). */
    public static void vuur(ServerPlayer p, String soortId, Soort s, Vec3 richting, double basis, Element el, boolean krit, boolean ketting) {
        Vec3 van = p.getEyePosition().add(richting.scale(0.5)).add(0, -0.12, 0);
        Schoten.Schot sc = Schoten.vuur(p, van, richting.normalize().scale(s.snelheid()));
        sc.zwaartekracht = s.zwaarte();
        sc.sturen = s.sturen() + (nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs.heeft(p, nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec.SCHERPSCHUTTER, 1) && s.schade() == Schade.Soort.AFSTAND ? 0.08 : 0);   // U-SPECS: Valkenblik
        sc.doorboren = s.doorboren() + (Boom.heeft(p, "j_doorboor") ? 1 : 0);
        sc.straal = s.schade() == Schade.Soort.MAGIE ? 0.45 : 0.3;
        sc.maxLeeftijd = (int) Math.ceil(40 / s.snelheid());
        sc.waarde = krit ? -1 : 1;
        if (s.item() != null) {
            Schoten.metBeeld(sc, s.item(), s.schaal(), s.schuin());
        }
        int kleur = Fx.kleur(el);
        Vec3 start = p.position();
        sc.spoor = (sl, a, b, t) -> {
            if (s.schade() == Schade.Soort.MAGIE) {
                Fx.lijn(sl, Fx.stof(kleur, 1.4f), a, b, 0.35);
                if (t % 2 == 0) {
                    sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 1, 0.05, 0.05, 0.05, 0.005);
                }
            } else {
                Fx.lijn(sl, Fx.stof(krit ? Fx.GOUD : kleur, 0.7f), a, b, 0.6);
                if (krit) {
                    sl.sendParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 2, 0.05, 0.05, 0.05, 0.05);
                }
            }
        };
        sc.opRaak = (schot, doel) -> {
            double mult = 1;
            if (Boom.heeft(p, "j_valkenoog") && doel.position().distanceTo(start) > 12) {
                mult *= 1.25;
            }
            treffer(p, doel, basis * mult, el, s.schade(), schot.waarde < 0, "basis");
            if (s.schade() == Schade.Soort.MAGIE) {
                if (Boom.heeft(p, "m_energievonk")) {
                    SpelerData d = Spelers.data(p);
                    d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + 3);
                }
                if (ketting && Boom.heeft(p, "m_kettingbolt")) {
                    ketting(p, s, doel, basis * 0.6, el);
                }
            }
            return true;
        };
        sc.opEinde = (schot, waar, blok) -> {
            ServerLevel sl = schot.level;
            if (s.spat() > 0) {
                for (LivingEntity e : Fx.mobsBij(sl, waar, s.spat())) {
                    if (!schot.geraakt.contains(e.getUUID())) {
                        treffer(p, e, basis * 0.5, el, s.schade(), false, "basis_spat");
                    }
                }
                Fx.plof(sl, waar, s.spat() * 0.6, el);
            }
            if (s.schade() == Schade.Soort.MAGIE) {
                sl.sendParticles(Fx.stof(kleur, 1.8f), waar.x, waar.y, waar.z, 10, 0.25, 0.25, 0.25, 0.02);
                Fx.geluid(sl, waar, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.4f);
            } else if (blok) {
                Fx.geluid(sl, waar, SoundEvents.ARROW_HIT, 0.5f, 1.2f);
                sl.sendParticles(ParticleTypes.POOF, waar.x, waar.y, waar.z, 2, 0.05, 0.05, 0.05, 0.01);
            }
        };
    }

    private static void ketting(ServerPlayer p, Soort s, LivingEntity van, double basis, Element el) {
        ServerLevel sl = (ServerLevel) p.level();
        LivingEntity beste = null;
        double bd = 36;
        for (LivingEntity e : Fx.mobsBij(sl, van.position(), 6)) {
            double d = e.distanceToSqr(van);
            if (e != van && d < bd) {
                bd = d;
                beste = e;
            }
        }
        if (beste == null) {
            return;
        }
        Vec3 a = van.getBoundingBox().getCenter(), b = beste.getBoundingBox().getCenter();
        Fx.lijn(sl, ParticleTypes.ELECTRIC_SPARK, a, b, 0.3);
        Fx.lijn(sl, Fx.stof(Fx.kleur(el), 1.0f), a, b, 0.4);
        Fx.geluid(sl, b, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.8f);
        treffer(p, beste, basis, el, s.schade(), false, "kettingbolt");
    }

    /**
     * One hit of a klassen skill/shot through the kern pipeline, with the ult charge. {@code krit}: a guaranteed crit (the
     * Rolsalto / Jachtroes) through kern's Klap.zekerKritiek.
     */
    public static Schade.Uitkomst treffer(ServerPlayer p, LivingEntity doel, double basis, Element el, Schade.Soort soort, boolean krit, String skill) {
        if (krit) {
            // (the crit multiplier itself is kern's: Klap.zekerKritiek, contract 13, so the number is gold too)
            if (doel.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, doel.getX(), doel.getY() + doel.getBbHeight() * 0.6, doel.getZ(), 14, 0.3, 0.3, 0.3, 0.3);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, doel.getX(), doel.getY() + doel.getBbHeight() * 0.6, doel.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
                Fx.geluid(sl, doel.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.1f);
            }
        }
        Schade.Uitkomst u = Schade.doe(new Schade.Klap(p, doel, basis, el, soort, true, skill, krit));
        Spelers.laadUlt(p, u.schade());
        if (u.schade() > 0 && doel.level() instanceof ServerLevel sl && soort == Schade.Soort.AFSTAND) {
            Fx.geluid(sl, doel.position(), SoundEvents.ARROW_HIT_PLAYER, 0.35f, 1.5f);
        }
        return u;
    }

    /** Area damage helper: everything within r of c takes basis; returns the total dealt. */
    public static double gebied(ServerPlayer p, Vec3 c, double r, double basis, Element el, Schade.Soort soort, String skill, @Nullable Set<UUID> al) {
        double tot = 0;
        for (LivingEntity e : Fx.mobsBij((ServerLevel) p.level(), c, r)) {
            if (al != null && !al.add(e.getUUID())) {
                continue;
            }
            tot += treffer(p, e, basis, el, soort, false, skill).schade();
        }
        return tot;
    }
}
