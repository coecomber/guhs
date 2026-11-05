package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Groep;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen.Materiaal;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Gathering (A1: "plekken per speler die tussen posities verschuiven, met een optionele korte minigame voor kwaliteit, lichte
 * anti-AFK en uitgerust-xp; zeldzame plekken glinsteren"):
 * <ul>
 *   <li>every spot is PER PLAYER: you see your own spots (the client draws them from {@link #sync}); harvesting empties it for you
 *   only, for {@link #LEEG_MIN}-{@link #LEEG_MAX} s, after which it comes back at ANOTHER of its positions;</li>
 *   <li>the optional minigame (client, 2-4 s) gives a score 0..2 = the quality (gewoon / vadsig / extra vadsig); sneak-click skips it
 *   (gewoon);</li>
 *   <li>a refilled spot is sometimes rare ({@link #ZELDZAAM_KANS}): it glitters; 3x the yield and a chance on the rare material;</li>
 *   <li>light anti-AFK: harvesting for more than {@link #AFK_MINUTEN} min without moving {@link #AFK_AFSTAND} blocks halves the yield;</li>
 *   <li>yield grows a little with your level; Mijnen "perfect" may give a raw crystal (zone 2/3 kinds), Vissen picks the fish by
 *   time of day and weather.</li>
 * </ul>
 */
public final class Verzamelen {
    public static final int LEEG_MIN = 60, LEEG_MAX = 120, ZICHT = 64, AFK_MINUTEN = 10, AFK_AFSTAND = 5;
    public static final double ZELDZAAM_KANS = 0.05, ZELDZAAM_MATERIAAL = 0.35, BEREIK = 6.0;

    /** One spot's state for one player. */
    static final class Staat {
        int pos;
        long klaar;
        boolean zeldzaam;
    }

    private static final Map<UUID, Map<String, Staat>> STAAT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAATSTE = new ConcurrentHashMap<>();
    private static final Map<UUID, Afk> AFK = new ConcurrentHashMap<>();
    private static final Map<UUID, String> VERSTUURD = new ConcurrentHashMap<>();

    record Afk(Vec3 plek, long sinds) {
    }

    private Verzamelen() {
    }

    static void vergeet(UUID id) {
        STAAT.remove(id);
        LAATSTE.remove(id);
        AFK.remove(id);
        VERSTUURD.remove(id);
    }

    static Staat staat(ServerPlayer p, Spot s) {
        return STAAT.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).computeIfAbsent(s.id(), k -> {
            Staat st = new Staat();
            st.pos = Math.floorMod((p.getUUID().hashCode() * 31 + s.id().hashCode()), Math.max(1, s.posities().size()));
            return st;
        });
    }

    /** Where this player sees this spot now. */
    public static BlockPos positie(ServerPlayer p, Spot s) {
        Staat st = staat(p, s);
        return s.posities().get(Math.min(st.pos, s.posities().size() - 1));
    }

    public static boolean beschikbaar(ServerPlayer p, Spot s) {
        return p.level().getGameTime() >= staat(p, s).klaar;
    }

    public static boolean zeldzaam(ServerPlayer p, Spot s) {
        return staat(p, s).zeldzaam;
    }

    /** Dev: make this spot rare (glittering) for this player. */
    public static void maakZeldzaam(ServerPlayer p, Spot s) {
        Staat st = staat(p, s);
        st.zeldzaam = true;
        st.klaar = 0;
    }

    // ---- finding spots -------------------------------------------------------------------------------------------------------

    /** The spots of every group near this player (scanning the groups whose area is loaded now). */
    public static List<Spot> inBuurt(ServerPlayer p, double r) {
        List<Spot> uit = new java.util.ArrayList<>();
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(p)) {
            return uit;
        }
        PlekWereld w = PlekWereld.get(sl.getServer());
        for (Groep g : AmbachtPlekken.GROEPEN) {
            BlockPos a = g.anker();
            if (a == null) {
                continue;
            }
            double dx = a.getX() - p.getX(), dz = a.getZ() - p.getZ();
            if (dx * dx + dz * dz > (r + g.straal()) * (r + g.straal()) || Math.abs(a.getY() - p.getY()) > 90) {
                continue;
            }
            List<Spot> l = w.spots(sl, g);
            if (l != null) {
                uit.addAll(l);
            }
        }
        return uit;
    }

    @Nullable
    public static Spot spot(MinecraftServer server, String id) {
        int i = id.lastIndexOf('_');
        if (i < 0) {
            return null;
        }
        Groep g = AmbachtPlekken.groep(id.substring(0, i));
        if (g == null) {
            return null;
        }
        List<Spot> l = PlekWereld.get(server).bekend(g);
        if (l == null) {
            return null;
        }
        for (Spot s : l) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    // ---- the sync (every second) -------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 7) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            sync(p, false);
        }
    }

    /** Sends this player's spots near them + the HUD (only when something changed, or {@code altijd}). */
    public static void sync(ServerPlayer p, boolean altijd) {
        CompoundTag d = new CompoundTag();
        ListTag l = new ListTag();
        long nu = p.level().getGameTime();
        for (Spot s : inBuurt(p, ZICHT)) {
            BlockPos pos = positie(p, s);
            if (pos.distSqr(p.blockPosition()) > ZICHT * ZICHT) {
                continue;
            }
            Groep g = s.groep();
            CompoundTag e = new CompoundTag();
            e.putString("id", s.id());
            e.putLong("pos", pos.asLong());
            e.putString("a", g.ambacht().id);
            e.putInt("tier", g.tier());
            boolean leeg = nu < staat(p, s).klaar;
            e.putBoolean("leeg", leeg);
            e.putBoolean("zeldzaam", !leeg && zeldzaam(p, s));
            e.putBoolean("lvOk", g.ambacht() == Ambacht.IMKEREN || AmbachtData.level(p, g.ambacht()) >= AmbachtXp.tierLevel(g.tier()));
            if (g.ambacht() == Ambacht.IMKEREN) {
                Imkeren.syncInfo(p, s, e);
            }
            l.add(e);
        }
        d.put("spots", l);
        Eten.hud(p, d);
        d.putLong("uitgerust", AmbachtData.uitgerust(p));
        String sleutel = d.toString();
        if (!altijd && sleutel.equals(VERSTUURD.get(p.getUUID())) && p.level().getGameTime() % 100 != 7) {
            return;
        }
        VERSTUURD.put(p.getUUID(), sleutel);
        AmbachtPayloads.sync(p, d);
    }

    // ---- harvesting ----------------------------------------------------------------------------------------------------------

    /**
     * A harvest: spot id, minigame score 0..2 (ignored when {@code overgeslagen}). Returns "" or a message key (also shown).
     * Imkeren spots are hives ({@link Imkeren#klik}).
     */
    public static String oogst(ServerPlayer p, String spotId, int score, boolean overgeslagen) {
        if (!Vadsrijk.in(p) || !(p.level() instanceof ServerLevel sl)) {
            return "vadsrijk.ambacht.oogst.niet_hier";
        }
        Spot s = spot(sl.getServer(), spotId);
        if (s == null) {
            return "vadsrijk.ambacht.oogst.weg";
        }
        BlockPos pos = positie(p, s);
        if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > BEREIK * BEREIK) {
            return "vadsrijk.ambacht.oogst.te_ver";
        }
        return doeOogst(p, sl, s, overgeslagen ? 0 : score);
    }

    /** The harvest itself (no dimension/reach check: the gametests call this). */
    public static String doeOogst(ServerPlayer p, ServerLevel sl, Spot s, int score) {
        boolean overgeslagen = false;
        BlockPos pos = positie(p, s);
        Groep g = s.groep();
        if (g.ambacht() == Ambacht.IMKEREN) {
            return Imkeren.klik(p, s, score);
        }
        Staat st = staat(p, s);
        long nu = sl.getGameTime();
        if (nu < st.klaar) {
            return meld(p, "vadsrijk.ambacht.oogst.leeg");
        }
        int lv = AmbachtData.level(p, g.ambacht()), nodig = AmbachtXp.tierLevel(g.tier());
        if (lv < nodig) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.ambacht.oogst.te_laag", Component.translatable(g.ambacht().naamKey()), nodig)
                    .withStyle(ChatFormatting.RED));
            return "vadsrijk.ambacht.oogst.te_laag";
        }
        Long vorige = LAATSTE.get(p.getUUID());
        if (vorige != null && nu - vorige < 15) {
            return "vadsrijk.ambacht.oogst.rustig";
        }
        LAATSTE.put(p.getUUID(), nu);
        int q = overgeslagen ? 0 : Math.max(0, Math.min(2, score));
        boolean zeld = st.zeldzaam;
        boolean afk = afk(p);
        // the yield: 1 + a little per level above the tier, a chance on double, x3 when rare, halved when not moving
        int n = 1 + Math.max(0, lv - nodig) / 15;
        if (p.getRandom().nextDouble() < 0.05 + 0.01 * (lv / 10) + Eten.dubbelKans(p)) {
            n++;
        }
        if (zeld) {
            n *= 3;
        }
        if (afk) {
            n = Math.max(1, n / 2);
        }
        Materiaal m = kies(p, sl, g);
        if (m != null) {
            MateriaalTas.geef(p, Materialen.stack(m.id(), q, n));
            if (g.ambacht() == Ambacht.VISSEN && AmbachtData.voegToe(p, "vissen", m.id())) {
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.vis.nieuw", Component.translatable(m.naamKey())).withStyle(ChatFormatting.AQUA));
            }
        }
        // extras: the rare material on a rare spot, a raw crystal on a perfect ore swing, resin from schemerhout
        if (zeld && p.getRandom().nextDouble() < ZELDZAAM_MATERIAAL) {
            Materiaal r = Materialen.zeldzaam(g.ambacht());
            if (r != null) {
                MateriaalTas.geef(p, Materialen.stack(r.id(), q, 1));
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.oogst.zeldzaam", Component.translatable(r.naamKey())).withStyle(ChatFormatting.GOLD));
            }
        }
        if (g.ambacht() == Ambacht.MIJNEN && q == 2 && g.tier() >= 2 && p.getRandom().nextDouble() < 0.25) {
            ItemStack k = kristal(p, g.tier());
            if (!k.isEmpty()) {
                MateriaalTas.geef(p, k);
            }
        }
        if (g.ambacht() == Ambacht.HOUTHAKKEN && g.tier() == 3 && p.getRandom().nextDouble() < 0.3) {
            MateriaalTas.geef(p, Materialen.stack("roze_hars", q, 1));
        }
        // xp: the tier amount, +10 % per quality step, x3 on a rare spot, halved when AFK
        double xp = AmbachtXp.verzamel(g.tier(), lv) * (1 + 0.1 * q) * (zeld ? 3 : 1) * (afk ? 0.5 : 1);
        AmbachtData.geefXp(p, g.ambacht(), Math.round(xp), true);
        AmbachtData.voegToe(p, "plekken", g.id());
        AmbachtData.tag(p).putLong("oogsten", AmbachtData.tag(p).getLongOr("oogsten", 0L) + 1);
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Premies.meld(p, "ambacht", 1);   // ("event:ambacht" bounties, if any)
        Bestelbord.verzameld(p, g.ambacht(), n);
        // the spot empties for this player and comes back elsewhere (sometimes rare)
        st.klaar = nu + 20L * (LEEG_MIN + p.getRandom().nextInt(LEEG_MAX - LEEG_MIN + 1));
        if (s.posities().size() > 1) {
            int nieuw = p.getRandom().nextInt(s.posities().size() - 1);
            st.pos = nieuw >= st.pos ? nieuw + 1 : nieuw;
        }
        st.zeldzaam = p.getRandom().nextDouble() < ZELDZAAM_KANS;
        effect(p, sl, pos, g.ambacht(), q, zeld);
        if (afk) {
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.oogst.afk").withStyle(ChatFormatting.GRAY));
        }
        sync(p, true);
        return "";
    }

    private static String meld(ServerPlayer p, String key) {
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        return key;
    }

    /** Which material a harvest gives (fish: by time of day and weather). */
    @Nullable
    static Materiaal kies(ServerPlayer p, ServerLevel sl, Groep g) {
        List<Materiaal> l = Materialen.van(g.ambacht(), g.tier());
        if (l.isEmpty()) {
            return null;
        }
        if (g.ambacht() == Ambacht.VISSEN) {
            boolean nacht = !sl.isBrightOutside(), regen = sl.isRaining();
            Materiaal gewoon = null, n = null, r = null;
            for (Materiaal m : l) {
                switch (m.wanneer()) {
                    case "nacht" -> n = m;
                    case "regen" -> r = m;
                    default -> gewoon = m;
                }
            }
            if (nacht && n != null && p.getRandom().nextDouble() < 0.35) {
                return n;
            }
            if (regen && r != null && p.getRandom().nextDouble() < 0.35) {
                return r;
            }
            return gewoon != null ? gewoon : l.get(0);
        }
        return l.get(p.getRandom().nextInt(l.size()));
    }

    private static ItemStack kristal(ServerPlayer p, int tier) {
        List<String> soorten = tier >= 3 ? nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Kristallen.SOORTEN
                : List.of("rozekwarts", "zonnesteen", "galmkristal", "spiegelkwarts", "vonkkristal", "ijsglans");
        return nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen.stack(soorten.get(p.getRandom().nextInt(soorten.size())), 0, 1);
    }

    /** Light anti-AFK: true when this player harvested for > 10 min without walking 5 blocks away. */
    static boolean afk(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Afk a = AFK.get(p.getUUID());
        if (a == null || a.plek().distanceToSqr(p.position()) > AFK_AFSTAND * AFK_AFSTAND) {
            AFK.put(p.getUUID(), new Afk(p.position(), nu));
            return false;
        }
        return nu - a.sinds() > 20L * 60 * AFK_MINUTEN;
    }

    static void effect(ServerPlayer p, ServerLevel sl, BlockPos pos, Ambacht a, int q, boolean zeldzaam) {
        SoundEvent geluid = switch (a) {
            case MIJNEN -> SoundEvents.AMETHYST_BLOCK_HIT;
            case HOUTHAKKEN -> SoundEvents.WOOD_BREAK;
            case PLUKKEN -> SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES;
            case VISSEN -> SoundEvents.FISHING_BOBBER_SPLASH;
            default -> SoundEvents.BEEHIVE_SHEAR;
        };
        Vec3 c = Vec3.atCenterOf(pos);
        sl.playSound(null, pos, geluid, SoundSource.PLAYERS, 0.8f, 1.0f + 0.15f * q);
        // (particles only for this player: the spot is theirs)
        sl.sendParticles(p, q == 2 ? ParticleTypes.END_ROD : q == 1 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF, true, false,
                c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.02);
        if (zeldzaam) {
            sl.sendParticles(p, ParticleTypes.TOTEM_OF_UNDYING, true, false, c.x, c.y + 0.3, c.z, 20, 0.4, 0.5, 0.4, 0.15);
            sl.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }
}
