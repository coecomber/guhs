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

/**
 * The class desk: what the ordetempel NPC (npc slice) and the klasse-tempeltje in Knuffelkamp call.
 * <ul>
 *   <li>{@link #open}: the ordetempel screen: switch class, the tree, free respec (cooldown 1 hour), job advancement.</li>
 *   <li>{@link #kiesEerste}: a new recruit's first class (the choice screen with the 3 classes and their skills).</li>
 *   <li>{@link #boom}: the tree screen anywhere in the dimension (key K): buying nodes only.</li>
 * </ul>
 * Respec and switching only work in a desk session: opened by {@link #open} less than 10 minutes ago, within 12 blocks.
 * Weapons belong to a class (kern checks), armour is shared; every class keeps its own level (SpelerData.xp per class).
 * Per-player klassen data lives in {@code SpelerData.extra("klassen")}: "gekozen" (first class chosen), "start_&lt;klasse&gt;".
 */
public final class KlasseBalie {
    private record Sessie(Vec3 pos, long tijd) {
    }

    private static final Map<UUID, Sessie> SESSIES = new HashMap<>();
    private static final Map<UUID, String> MODUS = new HashMap<>();

    private KlasseBalie() {
    }

    // ---- the API for the npc/wereld slices --------------------------------------------------------------------------------

    /** The ordetempel: opens the class desk screen for this player (call when they talk to the ordetempel NPC). */
    public static void open(ServerPlayer p) {
        SESSIES.put(p.getUUID(), new Sessie(p.position(), p.level().getGameTime()));
        stuur(p, "balie");
        Tips_.toon(p, "balie");
    }

    /**
     * Knuffelkamp's klasse-tempeltje: a new recruit picks their first class. Returns false (and says so) when they already
     * have one; switching is done at the ordetempel in Zwamzoet.
     */
    public static boolean kiesEerste(ServerPlayer p) {
        if (gekozen(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.klassen.kies.al", Component.translatable(Spelers.klasse(p).naamKey()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        stuur(p, "kies");
        return true;
    }

    /** Key K in the dimension: the tree of your current class (respec/switch only at the ordetempel). */
    public static void boom(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        stuur(p, aanBalie(p) ? "balie" : "boom");
        Tips_.toon(p, "boom");
    }

    public static boolean gekozen(ServerPlayer p) {
        return Spelers.data(p).extra("klassen").getBooleanOr("gekozen", false);
    }

    // ---- the logic --------------------------------------------------------------------------------------------------------

    /**
     * Switches the class (no checks on where you are: the desk session is checked by the caller). Your Vads keep their
     * percentage, energy fills up, the ult charge and cooldowns reset. The first time in a class you get its starter weapon.
     */
    public static boolean wissel(ServerPlayer p, String klasse) {
        Klasse k = Klassen.van(klasse);
        SpelerData d = Spelers.data(p);
        if (k == null || klasse.equals(d.klasse)) {
            return false;
        }
        double deel = Spelers.vads(p) / Spelers.maxVads(p);
        d.klasse = klasse;
        d.vads = Math.max(1, Spelers.maxVads(p) * deel);
        d.energie = Spelers.maxEnergie(p);
        d.ultLading = 0;
        d.klaarOp.clear();
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

    static boolean aanBalie(ServerPlayer p) {
        Sessie s = SESSIES.get(p.getUUID());
        return s != null && Vadsrijk.in(p) && p.level().getGameTime() - s.tijd() < 12000 && p.position().distanceTo(s.pos()) <= 12;
    }

    static void actie(ServerPlayer p, String soort, String arg) {
        if (!Vadsrijk.in(p)) {
            return;
        }
        ServerLevel sl = (ServerLevel) p.level();
        String modus = MODUS.getOrDefault(p.getUUID(), "boom");
        switch (soort) {
            case "koop" -> {
                String nee = Boom.koop(p, arg);
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
                String nee = Boom.respec(d, d.klasse, System.currentTimeMillis());
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
            case "wissel" -> {
                if (!aanBalie(p) || !wissel(p, arg)) {
                    return;
                }
                Fx.geluid(sl, p.position(), SoundEvents.ARMOR_EQUIP_GOLD.value(), 1f, 1.0f);
                Fx.geluid(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.5f);
                sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
                RijkScenes.titel(p, Component.translatable(Spelers.klasse(p).naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("vadsrijk.klassen.wissel.onder", Spelers.level(p)), 50, Fx.ROZE);
            }
            case "kies" -> {
                if (gekozen(p) || Klassen.van(arg) == null) {
                    return;
                }
                SpelerData d = Spelers.data(p);
                if (!arg.equals(d.klasse)) {
                    wissel(p, arg);
                } else {
                    d.extra("klassen").putBoolean("gekozen", true);
                    startwapen(p, arg);
                }
                Fx.geluid(sl, p.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
                RijkScenes.titel(p, Component.translatable("vadsrijk.klassen.kies.titel", Component.translatable(Spelers.klasse(p).naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("vadsrijk.klassen.kies.onder." + arg), 70, Fx.ROZE);
                Tips_.toon(p, "gekozen");
                // (fix gevecht) after that card: the Knuffelsprong (C), every class has it from the start
                Later.na(240, () -> nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.kern.tip.knuffelsprong"));
                return;   // (the choice screen closes itself)
            }
            case "adv" -> {
                if (!aanBalie(p) || !Advancement.VIA_BALIE || !Advancement.kanDoen(p)) {
                    return;
                }
                Advancement.doe(p);
                return;   // (the screen closes for the ceremony)
            }
            default -> {
                return;
            }
        }
        stuur(p, modus.equals("balie") && aanBalie(p) ? "balie" : modus.equals("kies") ? "kies" : "boom");
    }

    static String tijd(long sec) {
        return sec >= 60 ? (sec / 60) + " min" : sec + " s";
    }

    /** Sends the screen data. */
    static void stuur(ServerPlayer p, String modus) {
        MODUS.put(p.getUUID(), modus);
        KernPayloads.stuur(p, new KlassenPayloads.Staat(staat(p, modus)));
    }

    /** Everything the screens show. */
    public static CompoundTag staat(ServerPlayer p, String modus) {
        SpelerData d = Spelers.data(p);
        Klasse k = Spelers.klasse(p);
        CompoundTag t = new CompoundTag();
        t.putString("modus", modus);
        t.putString("klasse", k.id());
        t.putInt("level", d.level(k.id()));
        t.putInt("punten", Spelers.skillpunten(p, k.id()));
        t.putString("knopen", String.join(",", Boom.knopen(d, k.id())));
        t.putString("titel", Advancement.titelKey(k.id(), Advancement.stap(p, k.id())));
        for (Klasse x : Klassen.alle()) {
            t.putInt("lv_" + x.id(), d.level(x.id()));
            t.putInt("adv_" + x.id(), Advancement.stap(p, x.id()));
            t.putString("titel_" + x.id(), Advancement.titelKey(x.id(), Advancement.stap(p, x.id())));
        }
        t.putLong("respec", Boom.respecOver(d, System.currentTimeMillis()));
        t.putBoolean("kanAdv", Advancement.kanDoen(p));
        t.putBoolean("advBalie", Advancement.VIA_BALIE);
        return t;
    }

    static void vergeet(UUID id) {
        SESSIES.remove(id);
        MODUS.remove(id);
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
