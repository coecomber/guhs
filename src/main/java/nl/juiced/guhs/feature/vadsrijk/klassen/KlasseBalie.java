package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Specs;

/**
 * The class desk: what the ordetempel NPC (npc slice) and the klasse-tempeltje in Knuffelkamp call.
 * <p>g-klassen (round 2): you are ALWAYS all 3 classes; the class is the weapon in your hand (kern {@code Spelers.voorAanval}).
 * So there is no class switch here any more:
 * <ul>
 *   <li>{@link #open}: the ordetempel desk: the trees of all 3 classes (tabs), free respec per class (cooldown 1 hour), job
 *   advancement per class, and the starter weapons of the other classes for a few coins.</li>
 *   <li>{@link #kiesEerste}: a new recruit's STARTER WEAPON (the choice screen with the 3 classes and their skills).</li>
 *   <li>{@link #boom}: the tree screen anywhere in the dimension (key K): buying nodes, tabs for the other classes.</li>
 *   <li>{@link #geefStartwapen}: for trainers/shops (npc/economie): a class's starter weapon, free the first time, else a price.</li>
 * </ul>
 * Respec and buying weapons only work in a desk session: opened by {@link #open} less than 10 minutes ago, within 12 blocks.
 * Per-player klassen data lives in {@code SpelerData.extra("klassen")}: "gekozen" (starter weapon chosen), "start_&lt;klasse&gt;".
 */
public final class KlasseBalie {
    /** The price of another class's starter weapon at the desk (brons) once you had that class's free one. */
    public static final long WAPEN_PRIJS = 30;

    private record Sessie(Vec3 pos, long tijd) {
    }

    private static final Map<UUID, Sessie> SESSIES = new HashMap<>();
    private static final Map<UUID, String> MODUS = new HashMap<>();
    /** The class whose tree the screen shows (a tab); default the active class. */
    private static final Map<UUID, String> TAB = new HashMap<>();

    private KlasseBalie() {
    }

    // ---- the API for the npc/wereld slices --------------------------------------------------------------------------------

    /** The ordetempel: opens the class desk screen for this player (call when they talk to the ordetempel NPC). */
    public static void open(ServerPlayer p) {
        SESSIES.put(p.getUUID(), new Sessie(p.position(), p.level().getGameTime()));
        TAB.remove(p.getUUID());
        stuur(p, "balie");
        Tips_.toon(p, "balie");
    }

    /**
     * Knuffelkamp's klasse-tempeltje: a new recruit picks a starter weapon. Returns false (and says so) when they already did;
     * the other weapons come from the trainers, the shops or the ordetempel desk.
     */
    public static boolean kiesEerste(ServerPlayer p) {
        if (gekozen(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.kies.al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        stuur(p, "kies");
        return true;
    }

    /**
     * U-SPECS: the spec screen for {@code klasse} (the first choice after the step-1 quest, or the switch at a class NPC).
     */
    public static void vraagSpec(ServerPlayer p, String klasse) {
        if (Klassen.van(klasse) != null) {
            TAB.put(p.getUUID(), klasse);
        }
        stuur(p, "spec");
    }

    /** Key K in the dimension: the tree of your active class (respec/weapons only at the ordetempel). */
    public static void boom(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        TAB.remove(p.getUUID());
        stuur(p, aanBalie(p) ? "balie" : "boom");
        Tips_.toon(p, "boom");
    }

    /** Has this player picked their starter weapon? */
    public static boolean gekozen(ServerPlayer p) {
        return Spelers.data(p).extra("klassen").getBooleanOr("gekozen", false);
    }

    /**
     * A class's starter weapon (common, level 1, bound) for trainers/shops: free the first time per class, else {@code prijs}
     * brons (0 = always free). Returns false when the player can't pay.
     */
    public static boolean geefStartwapen(ServerPlayer p, String klasse, long prijs) {
        if (Klassen.van(klasse) == null) {
            return false;
        }
        CompoundTag x = Spelers.data(p).extra("klassen");
        boolean gratis = !x.getBooleanOr("start_" + klasse, false) || prijs <= 0;
        if (!gratis && !Spelers.neemMunten(p, prijs)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.wapen.te_duur", Spelers.munten(prijs)).withStyle(ChatFormatting.GRAY));
            return false;
        }
        x.putBoolean("start_" + klasse, false);   // (startwapen hands it out again)
        startwapen(p, klasse);
        return true;
    }

    // ---- the logic --------------------------------------------------------------------------------------------------------

    /**
     * Dev/tests: makes {@code klasse} active right away (as if you attacked with its weapon: its skills on full cooldown, ult 0)
     * and hands out its starter weapon the first time.
     */
    public static boolean wissel(ServerPlayer p, String klasse) {
        Klasse k = Klassen.van(klasse);
        SpelerData d = Spelers.data(p);
        if (k == null || klasse.equals(d.klasse)) {
            return false;
        }
        Spelers.wisselKlasse(p, klasse);
        d.extra("klassen").putBoolean("gekozen", true);
        Basisaanval.vergeet(p.getUUID());
        startwapen(p, klasse);
        Spelers.sync(p);
        return true;
    }

    /** The starter weapon of a class, once per class (bound, common, level 1). */
    static void startwapen(ServerPlayer p, String klasse) {
        CompoundTag x = Spelers.data(p).extra("klassen");
        if (x.getBooleanOr("start_" + klasse, false)) {
            return;
        }
        String type = switch (klasse) {
            case Klassen.JAGER -> Wapens.PLUISBOOG;
            case Klassen.MAGIER -> Wapens.ROZENSTAF;
            default -> "kussenzwaard";
        };
        var bt = BasisTypes.van(type);
        if (bt == null) {
            return;
        }
        Uitrusting u = Rollen.rol(bt, 1, Zeldzaamheid.GEWOON, p.getRandom()).metGebonden(true);
        ItemStack s = Rollen.stack(u);
        if (!s.isEmpty()) {
            if (!p.getInventory().add(s)) {
                p.drop(s, false);
            }
            x.putBoolean("start_" + klasse, true);
        }
    }

    public static boolean aanBalie(ServerPlayer p) {
        Sessie s = SESSIES.get(p.getUUID());
        return s != null && Vadsrijk.in(p) && p.level().getGameTime() - s.tijd() < 12000 && p.position().distanceTo(s.pos()) <= 12;
    }

    /** The class the screen shows (the tab), else the active class. */
    static String bekeken(ServerPlayer p) {
        String t = TAB.get(p.getUUID());
        return t != null && Klassen.van(t) != null ? t : Spelers.klasse(p).id();
    }

    static void actie(ServerPlayer p, String soort, String arg) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        ServerLevel sl = (ServerLevel) p.level();
        String modus = MODUS.getOrDefault(p.getUUID(), "boom");
        switch (soort) {
            case "tab" -> {
                if (Klassen.van(arg) == null) {
                    return;
                }
                TAB.put(p.getUUID(), arg);
            }
            case "koop" -> {
                String nee = Boom.koop(p, bekeken(p), arg);
                if (nee == null) {
                    Fx.geluid(sl, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
                    Fx.geluid(sl, p.position(), SoundEvents.PLAYER_LEVELUP, 0.4f, 1.8f);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1.5, p.getZ(), 16, 0.5, 0.4, 0.5, 0.02);
                } else {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.boom.nee." + nee).withStyle(ChatFormatting.GRAY));
                }
            }
            case "respec" -> {
                if (!aanBalie(p)) {
                    return;
                }
                SpelerData d = Spelers.data(p);
                String nee = Boom.respec(d, bekeken(p), System.currentTimeMillis());
                if (nee == null) {
                    Spelers.sync(p);
                    Fx.geluid(sl, p.position(), SoundEvents.BOOK_PAGE_TURN, 1f, 0.8f);
                    Fx.geluid(sl, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 1.2f);
                    p.sendSystemMessage(Component.translatable("vadsrijk.klassen.respec.klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.respec.nee." + nee, tijd(Boom.respecOver(d, System.currentTimeMillis())))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            case "wapen" -> {
                if (!aanBalie(p) || !geefStartwapen(p, arg, WAPEN_PRIJS)) {
                    return;
                }
                Fx.geluid(sl, p.position(), SoundEvents.ARMOR_EQUIP_GOLD.value(), 1f, 1.0f);
                sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
                p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.wapen.gekregen", Component.translatable(Klassen.ofRidder(arg).naamKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case "kies" -> {
                if (gekozen(p) || Klassen.van(arg) == null) {
                    return;
                }
                SpelerData d = Spelers.data(p);
                d.klasse = arg;   // (the first class: no cooldowns to reset yet)
                d.vads = -1;
                d.energie = -1;
                d.extra("klassen").putBoolean("gekozen", true);
                startwapen(p, arg);
                Spelers.sync(p);
                Fx.geluid(sl, p.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                RijkScenes.titel(p, Component.translatable("vadsrijk.klassen.kies.titel", Component.translatable(Spelers.klasse(p).naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("vadsrijk.klassen.kies.onder." + arg), 70, Fx.ROZE);
                Tips_.toon(p, "gekozen");
                // (fix gevecht) after that card: the Knuffelsprong (C), every class has it from the start
                Later.na(240, () -> nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.kern.tip.knuffelsprong"));
                return;   // (the choice screen closes itself)
            }
            // U-SPECS: the spec choice / switch
            case "spec_scherm" -> {
                stuur(p, "spec");
                return;
            }
            case "spec_kies" -> {
                Spec sp = Spec.van(arg);
                if (sp == null || !sp.klasse.equals(bekeken(p))) {
                    return;
                }
                String nee = Specs.kies(p, sp);
                if (nee != null) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.spec.nee." + nee).withStyle(ChatFormatting.GRAY));
                    stuur(p, "spec");
                }
                return;   // (the screen closes for the ceremony)
            }
            case "spec_wissel" -> {
                String kl = bekeken(p);
                long nu = System.currentTimeMillis();
                String nee = Specs.wissel(p, kl, aanBalie(p), nu);
                if (nee == null) {
                    Spec sp = Specs.spec(p, kl);
                    Fx.geluid(sl, p.position(), SoundEvents.BOOK_PAGE_TURN, 1f, 0.8f);
                    Fx.geluid(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.3f);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                    p.sendSystemMessage(Component.translatable("vadsrijk.klassen.spec.gewisseld", Component.translatable(sp.naamKey()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.spec.nee." + nee, tijd(Specs.wisselOver(p, nu)))
                            .withStyle(ChatFormatting.GRAY));
                }
                stuur(p, "spec");
                return;
            }
            case "adv" -> {
                if (!aanBalie(p) || !Advancement.VIA_BALIE || !Advancement.kanDoen(p, bekeken(p))) {
                    return;
                }
                Advancement.doe(p, bekeken(p));
                return;   // (the screen closes for the ceremony)
            }
            default -> {
                return;
            }
        }
        stuur(p, modus.equals("balie") && aanBalie(p) ? "balie" : modus.equals("kies") ? "kies" : modus.equals("spec") ? "spec" : "boom");
    }

    static String tijd(long sec) {
        return sec >= 60 ? (sec / 60) + " min" : sec + " s";
    }

    /** Sends the screen data. */
    static void stuur(ServerPlayer p, String modus) {
        MODUS.put(p.getUUID(), modus);
        KernPayloads.stuur(p, new KlassenPayloads.Staat(staat(p, modus)));
    }

    /** Everything the screens show ("klasse" = the class of the shown tree/tab, "actief" = the active class). */
    public static CompoundTag staat(ServerPlayer p, String modus) {
        SpelerData d = Spelers.data(p);
        Klasse k = Klassen.ofRidder(bekeken(p));
        CompoundTag t = new CompoundTag();
        t.putString("modus", modus);
        t.putString("klasse", k.id());
        t.putString("actief", Spelers.klasse(p).id());
        t.putInt("level", d.level(k.id()));
        t.putInt("generiek", d.level());
        t.putInt("punten", Spelers.skillpunten(p, k.id()));
        t.putString("knopen", String.join(",", Boom.knopen(d, k.id())));
        t.putString("titel", Advancement.titelKey(p, k.id(), Advancement.stap(p, k.id())));
        // U-SPECS: the spec of the shown class, the choice/switch state, and the step titles of that spec
        Spec sp = Specs.spec(p, k.id());
        long nuMs = System.currentTimeMillis();
        t.putString("spec", sp == null ? "" : sp.id);
        t.putBoolean("specOpen", Specs.open(p, k.id()));
        t.putLong("specWissel", Specs.wisselOver(p, nuMs));
        String wisselNee = Specs.waaromGeenWissel(p, k.id(), aanBalie(p), nuMs);
        t.putString("specWisselNee", wisselNee == null ? "" : wisselNee);
        for (int s = 1; s <= Advancement.MAX_STAP; s++) {
            t.putString("adv_titel_" + s, Advancement.titelKey(p, k.id(), s));
            t.putString("adv_uitleg_" + s, sp != null ? "vadsrijk.klassen.adv.uitleg." + sp.id + "." + s : "vadsrijk.klassen.adv.uitleg.geen." + s);
        }
        CompoundTag x = d.extra("klassen");
        for (Klasse kl : Klassen.alle()) {
            t.putInt("lv_" + kl.id(), d.level(kl.id()));
            t.putInt("adv_" + kl.id(), Advancement.stap(p, kl.id()));
            t.putString("titel_" + kl.id(), Advancement.titelKey(p, kl.id(), Advancement.stap(p, kl.id())));
            t.putBoolean("start_" + kl.id(), x.getBooleanOr("start_" + kl.id(), false));
        }
        t.putLong("prijs", WAPEN_PRIJS);
        t.putLong("respec", Boom.respecOver(d, System.currentTimeMillis()));
        t.putBoolean("kanAdv", Advancement.kanDoen(p, k.id()));
        t.putBoolean("advBalie", Advancement.VIA_BALIE);
        t.putInt("max", Levels.MAX);
        return t;
    }

    static void vergeet(UUID id) {
        SESSIES.remove(id);
        MODUS.remove(id);
        TAB.remove(id);
    }

    /** Hints of this slice. */
    static final class Tips_ {
        private Tips_() {
        }

        static void toon(ServerPlayer p, String id) {
            nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.klassen.tip." + id);
        }
    }
}
