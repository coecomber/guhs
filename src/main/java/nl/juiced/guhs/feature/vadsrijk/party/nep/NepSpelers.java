package nl.juiced.guhs.feature.vadsrijk.party.nep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.party.DungeonWachtrij;
import nl.juiced.guhs.feature.vadsrijk.party.PartyRegistry;
import nl.juiced.guhs.feature.vadsrijk.party.Wachtrijen;

/**
 * W-PARTY: owns every nep-speler ({@link NepSpeler}): the wild ones in the mob zones, the recruited companions and the dungeon
 * fillers; the per-player setting; the fake chat.
 * <ul>
 *   <li><b>Wild</b>: every {@link #WILD_ELKE} ticks per mob zone (a non-town area with Vadsrijk mobs around a player who has them
 *       on) a small chance of a group of 2-3, never more than {@link #MAX_PER_GEBIED} per zone. They leave after 12-20 minutes or
 *       when nobody is near for a minute.</li>
 *   <li><b>Recruiting</b>: right-click -> chat "[Kom je mee?]" -> {@link #rekruteer}: in your party for 20-30 minutes
 *       ({@link #MEE_MIN}..{@link #MEE_MAX} ticks), then a goodbye line and gone. Your party must have room (max 5 with them).</li>
 *   <li><b>Setting off</b> ({@link #magZien} false): that player does not see them (client), gets no nep chat, cannot recruit them, and
 *       no nep-speler fills a queue they are in. <b>Server off</b> ({@link NepConfig}): all of them leave, nothing spawns.</li>
 * </ul>
 * Not saved: a restart sends every nep-speler home (the entity is never saved, a leftover discards itself).
 */
public final class NepSpelers {
    public static final int MAX_PER_GEBIED = 3, WILD_ELKE = 600;
    public static int MEE_MIN = 20 * 60 * 20, MEE_MAX = 30 * 60 * 20;
    /** Gametests: no Vadsrijk / distance checks. */
    public static boolean TEST;

    static final Map<UUID, NepSpeler> ACTIEF = new LinkedHashMap<>();
    private static final Map<UUID, Boolean> STAND = new HashMap<>();
    /** The mob each player hit last (their "target" for the companions). */
    private static final Map<UUID, UUID> DOEL = new HashMap<>();
    /** Wild: ticks nobody was near (in steps of 20). */
    private static final Map<UUID, Integer> EENZAAM = new HashMap<>();

    private NepSpelers() {
    }

    public static void init() {
        Schade.NA_KLAP.add(u -> {
            if (u.klap().bron() instanceof ServerPlayer p && u.klap().doel() instanceof VadsrijkMob m) {
                DOEL.put(p.getUUID(), m.getUUID());
            }
        });
        Partijen.NPC_WEG.add(id -> {
            NepSpeler n = ACTIEF.get(id);
            if (n != null && n.dungeon == null) {
                afscheid(n, "doei");
            }
        });
    }

    // ---- queries ------------------------------------------------------------------------------------------------------------

    public static boolean bekend(NepSpeler n) {
        return ACTIEF.get(n.getUUID()) == n;
    }

    public static List<NepSpeler> alle() {
        return new ArrayList<>(ACTIEF.values());
    }

    @Nullable
    public static NepSpeler van(UUID id) {
        return ACTIEF.get(id);
    }

    /** The player's own setting "Nep-spelers: aan/uit" (default on). */
    public static boolean magZien(ServerPlayer p) {
        return STAND.getOrDefault(p.getUUID(), true);
    }

    public static boolean magZien(UUID id) {
        return STAND.getOrDefault(id, true);
    }

    public static void zetStand(UUID id, boolean aan) {
        STAND.put(id, aan);
    }

    /** Hooks on a player's setting change (integration: w-grot's own "geenNpc" flag follows it). */
    public static final List<java.util.function.BiConsumer<ServerPlayer, Boolean>> STAND_HAAK = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The setting from the client (at login and on change); hides nothing server side, see {@link #magZien}. */
    public static void zetStand(ServerPlayer p, boolean aan) {
        zetStand(p.getUUID(), aan);
        STAND_HAAK.forEach(h -> h.accept(p, aan));
        if (!aan) {
            for (NepSpeler n : alle()) {
                if (p.getUUID().equals(n.eigenaar) && n.dungeon == null) {
                    afscheid(n, "doei");   // (switched off: your companions go home)
                }
            }
        }
    }

    /** The nep-spelers that belong to this player's group (party companions + dungeon fillers with them), for frames and the tab. */
    public static List<NepSpeler> metgezellen(ServerPlayer p) {
        List<NepSpeler> uit = new ArrayList<>();
        if (!magZien(p)) {
            return uit;
        }
        for (NepSpeler n : ACTIEF.values()) {
            if (!n.wild() && (n.groep.contains(p.getUUID()) || p.getUUID().equals(n.eigenaar))) {
                uit.add(n);
            }
        }
        return uit;
    }

    @Nullable
    public static LivingEntity doelVan(ServerPlayer p) {
        UUID id = DOEL.get(p.getUUID());
        return id != null && p.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof LivingEntity l ? l : null;
    }

    // ---- spawning -----------------------------------------------------------------------------------------------------------

    /** A new nep-speler at this spot (a free name, a random skin). {@code id}: the UUID to use (dungeon fillers keep their queue id). */
    public static NepSpeler spawn(ServerLevel sl, Vec3 pos, NepRol rol, int level, @Nullable String naam, @Nullable UUID id) {
        NepSpeler n = PartyRegistry.NEPSPELER.get().create(sl, EntitySpawnReason.EVENT);
        if (n == null) {
            throw new IllegalStateException("nep-speler entity type missing");
        }
        if (id != null) {
            n.setUUID(id);
        }
        Set<String> bezet = new HashSet<>();
        for (NepSpeler x : ACTIEF.values()) {
            bezet.add(x.naam());
        }
        RandomSource r = sl.getRandom();
        n.stelIn(naam != null ? naam : NepNamen.naam(r, bezet), r.nextInt(NepSpeler.SKINS), rol, level);
        n.snapTo(pos.x, pos.y, pos.z, r.nextFloat() * 360f, 0f);
        n.setYHeadRot(n.getYRot());
        n.thuis = BlockPos.containing(pos);
        ACTIEF.put(n.getUUID(), n);
        sl.addFreshEntity(n);
        return n;
    }

    /** A group of wild nep-spelers (mixed roles, a level of the zone). */
    public static List<NepSpeler> spawnWild(ServerLevel sl, BlockPos plek, String gebied, int minLv, int maxLv, int aantal) {
        List<NepSpeler> uit = new ArrayList<>();
        RandomSource r = sl.getRandom();
        NepRol[] rollen = {NepRol.SCHADE, NepRol.TANK, NepRol.HELER};
        int start = r.nextInt(3);
        long nu = sl.getServer().overworld().getGameTime();
        for (int i = 0; i < aantal; i++) {
            Vec3 pos = Vec3.atBottomCenterOf(plek).add(r.nextDouble() * 3 - 1.5, 0, r.nextDouble() * 3 - 1.5);
            int lv = minLv + (maxLv > minLv ? r.nextInt(maxLv - minLv + 1) : 0);
            NepSpeler n = spawn(sl, pos, rollen[(start + i) % 3], lv, null, null);
            n.gebied = gebied;
            n.thuis = plek;
            n.tot = nu + 20 * 60 * (12 + r.nextInt(9));
            uit.add(n);
        }
        return uit;
    }

    /** Areas where wild nep-spelers may walk: no towns, no caves/dungeons/raid/valley, no coast catch-alls. */
    public static boolean mobZone(@Nullable Gebieden.Gebied g) {
        if (g == null || g.stad()) {
            return false;
        }
        String id = g.id().toLowerCase(Locale.ROOT);
        for (String nee : new String[] {"ring", "proeftuin", "grot", "kerker", "dungeon", "raid", "vallei", "droom", "arena", "toren", "kamp"}) {
            if (id.contains(nee)) {
                return false;
            }
        }
        return true;
    }

    /** A spot 20-40 blocks from {@code rond} in the same zone, on solid ground with 2 free blocks above (null: none found). */
    @Nullable
    public static BlockPos vindPlek(ServerLevel sl, BlockPos rond, @Nullable String gebied, RandomSource r) {
        for (int poging = 0; poging < 16; poging++) {
            double hoek = r.nextDouble() * Math.PI * 2, afstand = 20 + r.nextDouble() * 20;
            int x = rond.getX() + (int) (Math.cos(hoek) * afstand), z = rond.getZ() + (int) (Math.sin(hoek) * afstand);
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos p = new BlockPos(x, rond.getY() + dy, z);
                if (!sl.isLoaded(p)) {
                    break;
                }
                BlockState onder = sl.getBlockState(p.below());
                if (onder.isFaceSturdy(sl, p.below(), Direction.UP) && sl.getBlockState(p).getCollisionShape(sl, p).isEmpty()
                        && sl.getBlockState(p.above()).getCollisionShape(sl, p.above()).isEmpty() && sl.getFluidState(p).isEmpty()
                        && sl.getFluidState(p.above()).isEmpty()) {
                    Gebieden.Gebied g = Gebieden.op(p);
                    if (gebied == null || (g != null && g.id().equals(gebied))) {
                        return p;
                    }
                    break;
                }
            }
        }
        return null;
    }

    // ---- recruiting / talking -----------------------------------------------------------------------------------------------

    /** Right-click: a hello and the choices in chat. */
    public static void praat(ServerPlayer p, NepSpeler n) {
        if (!NepConfig.aan() || !magZien(p) || !bekend(n)) {
            return;
        }
        n.getLookControl().setLookAt(p, 30f, 30f);
        if (p.getUUID().equals(n.eigenaar)) {
            long rest = Math.max(0, n.tot - p.level().getServer().overworld().getGameTime());
            zeg(n, NepNamen.zeg("hoi", n.getRandom()));
            p.sendSystemMessage(Component.translatable("vadsrijk.party.nep.nog", n.naam(), n.tot < 0 ? "?" : String.valueOf((rest + 1199) / 1200))
                    .withStyle(ChatFormatting.GRAY).append(" ").append(knop("vadsrijk.party.nep.knop.weg", "/guhs vadsrijk party nepweg " + n.getUUID(),
                            ChatFormatting.RED)));
            return;
        }
        if (!n.wild()) {
            zeg(n, NepNamen.zeg("nee", n.getRandom()));
            return;
        }
        zeg(n, NepNamen.zeg("hoi", n.getRandom()));
        p.sendSystemMessage(Component.literal("» ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(knop("vadsrijk.party.nep.knop.mee", "/guhs vadsrijk party nepmee " + n.getUUID(), ChatFormatting.GREEN)).append(" ")
                .append(Component.translatable("vadsrijk.party.nep.knop.laat").withStyle(ChatFormatting.DARK_GRAY)));
    }

    static MutableComponent knop(String key, String cmd, ChatFormatting kleur) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(kleur)
                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand(cmd)).withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".uitleg"))));
    }

    /** "Kom je mee?": the nep-speler joins p's party for 20-30 minutes. Returns a lang key ({@code vadsrijk.party.nep.mee} = yes). */
    public static String rekruteer(ServerPlayer p, NepSpeler n) {
        if (!NepConfig.aan()) {
            return "vadsrijk.party.nep.server_uit";
        }
        if (!magZien(p)) {
            return "vadsrijk.party.nep.zelf_uit";
        }
        if (!bekend(n) || n.isRemoved()) {
            return "vadsrijk.party.nep.weg";
        }
        if (!n.wild()) {
            zeg(n, NepNamen.zeg("nee", n.getRandom()));
            return "vadsrijk.party.nep.bezet";
        }
        if (!TEST && (n.level() != p.level() || n.distanceTo(p) > 10)) {
            return "vadsrijk.party.nep.te_ver";
        }
        String r = Partijen.voegNpcToe(p, n.getUUID());
        if (!r.endsWith("erbij")) {
            zeg(n, "vadsrijk.party.nep.zeg.nee.2");
            return r;
        }
        long nu = p.level().getServer().overworld().getGameTime();
        int duur = MEE_MIN + (MEE_MAX > MEE_MIN ? n.getRandom().nextInt(MEE_MAX - MEE_MIN + 1) : 0);
        n.eigenaar = p.getUUID();
        n.tot = nu + duur;
        n.gebied = "";
        n.groep.clear();
        Partijen.Party party = Partijen.party(p);
        if (party != null) {
            n.groep.addAll(party.leden);
        }
        zeg(n, NepNamen.zeg("ja", n.getRandom()), (duur + 1199) / 1200);
        return "vadsrijk.party.nep.mee";
    }

    // ---- chat ---------------------------------------------------------------------------------------------------------------

    /** A fake chat line: above its head + "[Nep] name: ..." in the chat of everyone near (and its group) who has them on. */
    public static void zeg(NepSpeler n, String key, Object... args) {
        n.zetZeg(key + (args.length > 0 ? "|" + args[0] : ""));
        if (!(n.level() instanceof ServerLevel sl)) {
            return;
        }
        Component regel = Component.literal("[Nep] ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(n.naam()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
        for (ServerPlayer p : sl.players()) {
            if (magZien(p) && (p.distanceTo(n) < 32 || n.groep.contains(p.getUUID()))) {
                p.sendSystemMessage(regel);
            }
        }
    }

    /** Says goodbye ({@code soort}: "doei" or "gg"), then leaves. */
    public static void afscheid(NepSpeler n, String soort) {
        if (!bekend(n)) {
            return;
        }
        zeg(n, NepNamen.zeg(soort, n.getRandom()));
        weg(n);
    }

    /** Gone at once (a poof), out of its party. */
    public static void weg(NepSpeler n) {
        if (ACTIEF.remove(n.getUUID()) == null && n.isRemoved()) {
            return;
        }
        EENZAAM.remove(n.getUUID());
        Partijen.verwijderNpc(n.getUUID());
        if (n.level() instanceof ServerLevel sl && !n.isRemoved()) {
            sl.sendParticles(ParticleTypes.POOF, n.getX(), n.getY() + 1, n.getZ(), 16, 0.3, 0.6, 0.3, 0.02);
            sl.playSound(null, n.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6f, 0.7f);
        }
        n.discard();
    }

    /** Everyone leaves (server switched off, or a player's dev command). */
    public static void allemaalWeg(boolean zeggen) {
        for (NepSpeler n : alle()) {
            if (zeggen && !n.wild()) {
                afscheid(n, "doei");
            } else {
                weg(n);
            }
        }
    }

    // ---- the tick -----------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        long nu = server.overworld().getGameTime();
        if (!NepConfig.aan()) {
            if (!ACTIEF.isEmpty()) {
                allemaalWeg(true);
            }
            return;
        }
        onderhoud(server, nu);
        if (server.getTickCount() % WILD_ELKE == 0) {
            ServerLevel sl = Vadsrijk.level(server);
            if (sl != null) {
                wild(sl);
            }
        }
    }

    /** Timers, groups, cleanup (also called by the gametests with a chosen time). */
    public static void onderhoud(MinecraftServer server, long nu) {
        DungeonWachtrij rij = Wachtrijen.DUNGEON;
        for (NepSpeler n : alle()) {
            if (n.isRemoved()) {
                weg(n);
                continue;
            }
            if (n.tot >= 0 && nu >= n.tot) {
                afscheid(n, "doei");
                continue;
            }
            if (n.wild()) {
                boolean iemand = false;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p.level() == n.level() && p.distanceTo(n) < 96) {
                        iemand = true;
                        break;
                    }
                }
                int e = iemand ? 0 : EENZAAM.getOrDefault(n.getUUID(), 0) + 1;
                EENZAAM.put(n.getUUID(), e);
                if (e >= 60 && !TEST) {
                    weg(n);
                }
                continue;
            }
            if (n.dungeon != null) {
                boolean buiten = rij == null || n.eigenaar == null || !n.dungeon.equals(rij.dungeonVan(n.eigenaar));
                if (n.buitenDungeon(buiten) >= 10) {
                    afscheid(n, "gg");
                }
                continue;
            }
            Partijen.Party party = Partijen.partyVanNpc(n.getUUID());
            if (party == null) {
                afscheid(n, "doei");
                continue;
            }
            n.groep.clear();
            n.groep.addAll(party.leden);
        }
    }

    /** One round of wild spawning: per mob zone with a player who has them on, a small chance of a small group. */
    static void wild(ServerLevel sl) {
        RandomSource r = sl.getRandom();
        Map<String, ServerPlayer> zones = new LinkedHashMap<>();
        for (ServerPlayer p : sl.players()) {
            if (!magZien(p) || p.isSpectator()) {
                continue;
            }
            Gebieden.Gebied g = Gebieden.op(p.blockPosition());
            if (mobZone(g) && !zones.containsKey(g.id())) {
                zones.put(g.id(), p);
            }
        }
        for (var e : zones.entrySet()) {
            String id = e.getKey();
            ServerPlayer p = e.getValue();
            int al = 0;
            for (NepSpeler n : ACTIEF.values()) {
                if (n.wild() && id.equals(n.gebied)) {
                    al++;
                }
            }
            if (al >= 2 || r.nextFloat() > 0.25f) {
                continue;
            }
            if (sl.getEntitiesOfClass(VadsrijkMob.class, p.getBoundingBox().inflate(48), m -> !m.geplofd()).size() < 2) {
                continue;   // (no mobs around: not a place where players grind)
            }
            BlockPos plek = vindPlek(sl, p.blockPosition(), id, r);
            Gebieden.Gebied g = Gebieden.van(id);
            if (plek == null || g == null) {
                continue;
            }
            int n = Math.min(MAX_PER_GEBIED - al, 2 + r.nextInt(2));
            spawnWild(sl, plek, id, g.minLevel(), g.maxLevel(), n);
        }
    }

    /** Server start/stop: nothing is kept. */
    public static void wisAlles() {
        for (NepSpeler n : alle()) {
            n.discard();
        }
        ACTIEF.clear();
        DOEL.clear();
        EENZAAM.clear();
    }

    public static void uitgelogd(UUID id) {
        DOEL.remove(id);
    }
}
