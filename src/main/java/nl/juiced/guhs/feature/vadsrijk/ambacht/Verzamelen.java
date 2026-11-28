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
 * Gathering (A1: "plekken die tussen posities verschuiven, met een optionele korte minigame voor kwaliteit, lichte anti-AFK en
 * uitgerust-xp; zeldzame plekken glinsteren"):
 * <ul>
 *   <li>1.10.4 "Grondstoffen zie je nu echt": every spot except hives is a REAL block, the same for everyone ({@link Grondstoffen},
 *   {@link GrondstofBlok}); harvesting empties it for everybody for the group's respawn time ({@link #LEEG_MIN}-{@link #LEEG_MAX} s
 *   by default), after which it comes back full, usually at ANOTHER of its positions. Hives stay per player (your own hive on a
 *   stand, drawn by the client from {@link #sync});</li>
 *   <li>the optional minigame (client, 2-4 s) gives a score 0..2 = the quality (gewoon / vadsig / extra vadsig); sneak-click skips it
 *   (gewoon);</li>
 *   <li>a refilled spot is sometimes rare ({@link #ZELDZAAM_KANS}): it glitters; 3x the yield and a chance on the rare material;</li>
 *   <li>light anti-AFK: harvesting for more than {@link #AFK_MINUTEN} min without moving {@link #AFK_AFSTAND} blocks halves the yield;</li>
 *   <li>yield grows a little with your level; Mijnen "perfect" may give a raw crystal (zone 2/3 kinds), Vissen picks the fish by
 *   time of day and weather.</li>
 * </ul>
 */
public final class Verzamelen {
    public static final int LEEG_MIN = AmbachtPlekken.LEEG_MIN, LEEG_MAX = AmbachtPlekken.LEEG_MAX, ZICHT = 64, AFK_MINUTEN = 10, AFK_AFSTAND = 5;
    public static final double ZELDZAAM_KANS = 0.05, ZELDZAAM_MATERIAAL = 0.35, BEREIK = 6.0;
    /** 1.10.30 (owner): an ORE spot gives its rare material (Glinstergruis) always when it glitters, and 1 in 5 times when it does not. */
    public static final double ERTS_GRUIS_GLINSTER = 1.0, ERTS_GRUIS_GEWOON = 0.20;

    /** The chance that a spot of this kind gives its rare material ({@code zeldzaam}: a glittering spot). Only ore gives it on a plain spot. */
    public static double zeldzaamMateriaalKans(NodeSoort soort, boolean zeldzaam) {
        if (soort == NodeSoort.ERTS) {
            return zeldzaam ? ERTS_GRUIS_GLINSTER : ERTS_GRUIS_GEWOON;
        }
        return zeldzaam ? ZELDZAAM_MATERIAAL : 0;
    }

    /** One hive stand's place for one player (1.10.4: the other spots are shared, {@link Grondstoffen}). */
    static final class Staat {
        int pos;
        long klaar;
        boolean zeldzaam;
    }

    private static final Map<UUID, Map<String, Staat>> STAAT = new ConcurrentHashMap<>();
    static final Map<UUID, Long> LAATSTE = new ConcurrentHashMap<>();   // (package: the tests reset it)
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

    /** True: a shared real block (everything but hives). */
    public static boolean echt(Spot s) {
        return s.groep().soort() != NodeSoort.KORF;
    }

    /**
     * Where this spot is now: the node block (shared; ore: the vein in the floor) or, for a hive, this player's stand position.
     */
    public static BlockPos positie(ServerPlayer p, Spot s) {
        if (echt(s) && p.level().getServer() != null) {
            return Grondstoffen.get(p.level().getServer()).positie(s);
        }
        Staat st = staat(p, s);
        return s.posities().get(Math.min(st.pos, s.posities().size() - 1));
    }

    public static boolean beschikbaar(ServerPlayer p, Spot s) {
        if (echt(s) && p.level() instanceof ServerLevel sl) {
            return Grondstoffen.get(sl.getServer()).vol(sl, s);
        }
        return p.level().getGameTime() >= staat(p, s).klaar;
    }

    public static boolean zeldzaam(ServerPlayer p, Spot s) {
        if (echt(s) && p.level().getServer() != null) {
            return Grondstoffen.get(p.level().getServer()).node(s).zeldzaam();
        }
        return staat(p, s).zeldzaam;
    }

    /** Dev: make this spot rare (glittering) and full (shared: for everyone). */
    public static void maakZeldzaam(ServerPlayer p, Spot s) {
        if (echt(s) && p.level().getServer() != null) {
            Grondstoffen.get(p.level().getServer()).maakVol(s, true);
            return;
        }
        Staat st = staat(p, s);
        st.zeldzaam = true;
        st.klaar = 0;
    }

    /** Dev: this spot is full again (shared spots for everyone; a hive stand for this player). */
    public static void maakVol(ServerPlayer p, Spot s) {
        if (echt(s) && p.level().getServer() != null) {
            Grondstoffen g = Grondstoffen.get(p.level().getServer());
            g.maakVol(s, g.node(s).zeldzaam());
            return;
        }
        staat(p, s).klaar = 0;
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
        Grondstoffen.tick(server);   // 1.10.4: the real blocks (placed lazily near players, follow their timers)
        for (ServerPlayer p : sl.players()) {
            sync(p, false);
        }
    }

    /** Sends this player's spots near them + the HUD (only when something changed, or {@code altijd}). */
    public static void sync(ServerPlayer p, boolean altijd) {
        CompoundTag d = new CompoundTag();
        ListTag l = new ListTag();
        long nu = p.level().getGameTime();
        Grondstoffen gs = p.level().getServer() == null ? null : Grondstoffen.get(p.level().getServer());
        for (Spot s : inBuurt(p, ZICHT)) {
            Groep g = s.groep();
            boolean echt = echt(s) && gs != null;
            Grondstoffen.Node node = echt ? gs.node(s) : null;
            if (echt && node.pos() == null) {
                continue;   // (1.10.4: a shared spot shows only once its block is in the world)
            }
            BlockPos pos = echt ? node.pos() : positie(p, s);
            if (pos.distSqr(p.blockPosition()) > ZICHT * ZICHT) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putString("id", s.id());
            e.putLong("pos", pos.asLong());
            e.putString("a", g.ambacht().id);
            e.putString("s", g.soort().id);   // 1.10.4: the spot kind (minigame + look)
            e.putInt("tier", g.tier());
            boolean leeg = echt ? nu < node.klaar() : nu < staat(p, s).klaar;
            e.putBoolean("leeg", leeg);
            e.putBoolean("echt", echt);   // 1.10.4: a real block (the client clicks the block itself; only hives are drawn)
            e.putBoolean("zeldzaam", !leeg && (echt ? node.zeldzaam() : zeldzaam(p, s)));
            e.putBoolean("lvOk", AmbachtData.level(p, g.ambacht()) >= nodig(g));
            if (g.zoek() == AmbachtPlekken.Zoek.KORAAL) {
                e.putBoolean("duik", true);   // U-AMBACHT2: the client draws coral and an underwater hit box
            }
            if (g.soort() == NodeSoort.KORF) {
                e.putInt("nodig", nodig(g));
                Imkeren.syncInfo(p, s, e);
            }
            l.add(e);
        }
        d.put("spots", l);
        Eten.hud(p, d);
        d.putLong("uitgerust", AmbachtData.uitgerust(p));
        String sleutel = d.toString();
        if (!altijd && sleutel.equals(VERSTUURD.get(p.getUUID())) && p.level().getServer().getTickCount() % 100 != 7) {
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
        if (g.soort() == NodeSoort.KORF) {
            return Imkeren.klik(p, s, score);
        }
        Grondstoffen gs = Grondstoffen.get(sl.getServer());
        Grondstoffen.Node st = gs.node(s);
        long nu = sl.getGameTime();
        if (nu < st.klaar()) {
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
        boolean zeld = st.zeldzaam();
        boolean afk = afk(p);
        // the yield: 1 + a little per level above the tier (1.10.4: per 8 levels, tiers are ~10 wide), a chance on double, x3 when
        // rare, halved when not moving
        int n = 1 + Math.max(0, lv - nodig) / 8;
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
            if (m.vis() && g.zone() == 4) {   // (U-AMBACHT2: coral finds are not fish)
                nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "z4_vis", "gevangen");   // T-INT: t-z4verhaal counts zone-4 fish
            }
            if (m.vis() && AmbachtData.voegToe(p, "vissen", m.id())) {
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.vis.nieuw", Component.translatable(m.naamKey())).withStyle(ChatFormatting.AQUA));
            }
        }
        // extras: the rare material on a rare spot, a raw crystal on a perfect ore swing, resin from schemerhout
        if (p.getRandom().nextDouble() < zeldzaamMateriaalKans(g.soort(), zeld)) {
            Materiaal r = Materialen.zeldzaam(g.soort());
            if (r != null) {
                MateriaalTas.geef(p, Materialen.stack(r.id(), q, 1));
                p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.oogst.zeldzaam", Component.translatable(r.naamKey())).withStyle(ChatFormatting.GOLD));
            }
        }
        if (g.soort() == NodeSoort.ERTS && q == 2 && g.tier() >= 2 && p.getRandom().nextDouble() < 0.25) {
            ItemStack k = kristal(p, g.tier());
            if (!k.isEmpty()) {
                MateriaalTas.geef(p, k);
            }
        }
        if (g.soort() == NodeSoort.HOUT && g.tier() == 3 && p.getRandom().nextDouble() < 0.3) {
            MateriaalTas.geef(p, Materialen.stack("roze_hars", q, 1));
        }
        if (g.soort() == NodeSoort.DUIK && p.getRandom().nextDouble() < 0.06) {
            MateriaalTas.geef(p, Materialen.stack("parelschelp", q, 1));   // U-AMBACHT2: divers sometimes find a pearl shell
        }
        // xp: the tier amount, +10 % per quality step, x3 on a rare spot, halved when AFK
        double xp = AmbachtXp.verzamel(g.tier(), lv) * (1 + 0.1 * q) * (zeld ? 3 : 1) * (afk ? 0.5 : 1);
        AmbachtData.geefXp(p, g.ambacht(), Math.round(xp), true);
        AmbachtData.voegToe(p, "plekken", g.id());
        AmbachtData.tag(p).putLong("oogsten", AmbachtData.tag(p).getLongOr("oogsten", 0L) + 1);
        nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Premies.meld(p, "ambacht", 1);   // ("event:ambacht" bounties, if any)
        Bestelbord.verzameld(p, g.ambacht(), n);
        // the spot empties for EVERYONE (1.10.4: a real block) for the group's time (1.10.4: per group), then it comes back full,
        // usually elsewhere (Grondstoffen.bijwerk moves it), sometimes rare
        gs.geoogst(sl, s, nu + 20L * (g.leegMin() + p.getRandom().nextInt(Math.max(1, g.leegMax() - g.leegMin() + 1))),
                p.getRandom().nextDouble() < ZELDZAAM_KANS);
        effect(p, sl, pos, g.soort(), q, zeld);
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

    /** Which material a harvest gives (fish: by time of day and weather; a dive spot gives the diving finds). */
    @Nullable
    static Materiaal kies(ServerPlayer p, ServerLevel sl, Groep g) {
        List<Materiaal> l = Materialen.van(g.soort(), g.tier());
        if (l.isEmpty()) {
            return null;
        }
        if (g.soort() == NodeSoort.VIS) {
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
        List<String> soorten = tier >= 4 ? nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot.KRISTALLEN   // U-AMBACHT2 (z4/z5 coast + sky)
                : tier >= 3 ? nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Kristallen.SOORTEN
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

    /** The minimum level for a group: the tier's start (hives: the softer {@link Imkeren#korfLevel}). */
    public static int nodig(Groep g) {
        return g.soort() == NodeSoort.KORF ? Imkeren.korfLevel(g.tier()) : AmbachtXp.tierLevel(g.tier());
    }

    static void effect(ServerPlayer p, ServerLevel sl, BlockPos pos, NodeSoort s, int q, boolean zeldzaam) {
        SoundEvent geluid = switch (s) {
            case ERTS -> SoundEvents.AMETHYST_BLOCK_HIT;
            case HOUT -> SoundEvents.WOOD_BREAK;
            case KRUID -> SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES;
            case VIS, DUIK -> SoundEvents.FISHING_BOBBER_SPLASH;
            case KORF -> SoundEvents.BEEHIVE_SHEAR;
        };
        Vec3 c = Vec3.atCenterOf(pos).add(0, s == NodeSoort.ERTS ? 0.6 : 0, 0);   // (ore: just above the vein in the floor)
        sl.playSound(null, pos, geluid, SoundSource.PLAYERS, 0.8f, 1.0f + 0.15f * q);
        // (1.10.4: everyone nearby sees a harvest, the block is shared; a hive stays the player's own)
        var deeltje = q == 2 ? ParticleTypes.END_ROD : q == 1 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.POOF;
        if (s == NodeSoort.KORF) {
            sl.sendParticles(p, deeltje, true, false, c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.02);
        } else {
            sl.sendParticles(deeltje, c.x, c.y, c.z, 8, 0.3, 0.3, 0.3, 0.02);
        }
        if (zeldzaam) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 0.3, c.z, 20, 0.4, 0.5, 0.4, 0.15);
            sl.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }
}
