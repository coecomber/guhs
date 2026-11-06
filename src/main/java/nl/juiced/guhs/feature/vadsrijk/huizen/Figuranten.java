package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.storage.Nbt;

/**
 * U-HUIZEN: the living bits of a house, only while somebody is near (server-friendly: nothing loads or ticks for an empty street,
 * nothing is saved; {@link Regels#binnenkomen} drops any copy that comes back from disk):
 * <ul>
 *   <li>the owner's active mount idles in the stable corner (not rideable, no AI);</li>
 *   <li>up to {@value #MAX_DIEREN} of the owner's pets roam the garden (the pet entity, driven by {@link #tuinTick});</li>
 *   <li>the trophy shelf: one small figure + name per boss the owner's characters beat (from {@code SpelerData.gezien});</li>
 *   <li>Meubelmeester Mollie at the market of each ward (an NPC guh: clicks go to {@link Menus#mollie}).</li>
 * </ul>
 * What the owner has (mount, pets, trophies) is copied into the house data whenever the owner is home ({@link #bijwerkenVan}), so
 * visitors see it while the owner is away.
 */
public final class Figuranten {
    public static final String TAG = "guhs_kk_figurant", NPC_TAG = "guhs_kk_npc";
    public static final String MOLLIE = "kk_mollie";
    static final int MAX_DIEREN = 3, MAX_TROFEEEN = 8;
    private static final double BIJ = 48, WEG = 72;
    private static final Map<Integer, List<UUID>> PER_KAVEL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TUIN = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> MOLLIES = new ConcurrentHashMap<>();

    private Figuranten() {
    }

    /** Copies the owner's mount, pets and trophies into the house (the owner is home). */
    public static void bijwerkenVan(ServerPlayer p, Huis h) {
        boolean anders = false;
        RijdierSoort s = Rijdieren.actief(p);
        String r = s == null || s.water() ? "" : s.id();
        String kleur = s == null ? "" : Rijdieren.kleur(p, s).id();
        String uiterlijk = s == null ? "" : nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden.uiterlijk(p, s);
        if (!r.equals(h.rijdier) || !kleur.equals(h.rijdierKleur) || !uiterlijk.equals(h.rijdierUiterlijk)) {
            h.rijdier = r;
            h.rijdierKleur = kleur;
            h.rijdierUiterlijk = uiterlijk;
            anders = true;
        }
        List<String> dieren = new ArrayList<>();
        Huisdier actief = Huisdieren.actief(p);
        if (actief != null) {
            dieren.add(actief.id());
        }
        for (String id : Huisdieren.bezit(p)) {
            if (dieren.size() < MAX_DIEREN && !dieren.contains(id)) {
                dieren.add(id);
            }
        }
        if (!dieren.equals(h.huisdieren)) {
            h.huisdieren.clear();
            h.huisdieren.addAll(dieren);
            anders = true;
        }
        for (String g : accountTrofeeen(p)) {   // (all characters of the account together)
            if (h.trofeeen.add(g)) {
                anders = true;
            }
        }
        if (anders) {
            Kwartier.data(p.level().getServer()).setDirty();
            Integer k = Kwartier.data(p.level().getServer()).sleutelVan(p.getUUID());
            if (k != null && p.level() instanceof ServerLevel sl && sl.dimension() == Kwartier.DIM) {
                weg(sl, k);   // (the keeper puts the new ones there within two seconds)
            }
        }
    }

    /**
     * The account-wide trophy list ({@code Karakter.account(p, "huizen").trofeeen}): every boss any character of this account beat.
     * The active character's kills are merged in first (also called on login and when a character switches).
     */
    public static java.util.Set<String> accountTrofeeen(ServerPlayer p) {
        CompoundTag a = nl.juiced.guhs.feature.vadsrijk.karakter.Karakter.account(p, "huizen");
        java.util.Set<String> l = new java.util.LinkedHashSet<>();
        for (String x : a.getStringOr("trofeeen", "").split(",")) {
            if (!x.isEmpty()) {
                l.add(x);
            }
        }
        boolean nieuw = false;
        for (String g : Spelers.data(p).gezien) {
            if (g.startsWith("baaskill:") && l.add(g)) {
                nieuw = true;
            }
        }
        if (nieuw) {
            a.putString("trofeeen", String.join(",", l));
        }
        return l;
    }

    // ---- the keeper -----------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 17) {
            return;
        }
        ServerLevel l = Kwartier.level(server);
        if (l == null) {
            return;
        }
        HuizenData d = Kwartier.data(server);
        List<ServerPlayer> spelers = l.players();
        // away: the figures of plots nobody is near any more
        for (Integer k : List.copyOf(PER_KAVEL.keySet())) {
            Vec3 m = WijkPlan.kavel(k).midden();
            if (!iemandBij(spelers, m, WEG) || d.huis(k) == null) {
                weg(l, k);
            }
        }
        for (Integer w : List.copyOf(MOLLIES.keySet())) {
            if (!iemandBij(spelers, Vec3.atCenterOf(WijkPlan.midden(w)), 96)) {
                Entity e = l.getEntity(MOLLIES.remove(w));
                if (e != null) {
                    e.discard();
                }
            }
        }
        if (spelers.isEmpty()) {
            return;
        }
        // there: for every plot with a house near a player
        for (ServerPlayer p : spelers) {
            int w = WijkPlan.wijkVan(p.getX());
            if (w < 0 || w >= d.wijken) {
                continue;
            }
            for (int i = 0; i < WijkPlan.KAVELS; i++) {
                Kavel k = WijkPlan.kavel(w, i);
                Huis h = d.huis(k.sleutel());
                if (h != null && !PER_KAVEL.containsKey(k.sleutel()) && p.position().distanceTo(k.midden()) < BIJ && l.isLoaded(BlockPos.containing(k.midden()))) {
                    zet(l, k, h);
                }
            }
            UUID m = MOLLIES.get(w);
            if ((m == null || !(l.getEntity(m) instanceof RijkGuh g) || !g.isAlive()) && p.position().distanceTo(Vec3.atCenterOf(WijkPlan.midden(w))) < 64) {
                zetMollie(l, w);
            }
        }
    }

    private static boolean iemandBij(List<ServerPlayer> spelers, Vec3 m, double r) {
        for (ServerPlayer p : spelers) {
            if (p.position().distanceToSqr(m) < r * r) {
                return true;
            }
        }
        return false;
    }

    /** Removes the figures of a plot. */
    public static void weg(ServerLevel l, int sleutel) {
        List<UUID> ids = PER_KAVEL.remove(sleutel);
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            TUIN.remove(id);
            Entity e = l.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
    }

    /** Puts the figures of a house there. Returns how many. */
    static int zet(ServerLevel l, Kavel k, Huis h) {
        List<UUID> ids = new ArrayList<>();
        PER_KAVEL.put(k.sleutel(), ids);
        // the mount in the stable
        RijdierSoort s = h.rijdier.isEmpty() ? null : RijdierSoort.van(h.rijdier);
        if (s != null && !s.water()) {
            RijdierEntity r = EcoSpul.type(s).create(l, EntitySpawnReason.TRIGGERED);
            if (r != null) {
                Vec3 v = Ontwerp.stalPlek(k);
                r.snapTo(v.x, v.y, v.z, k.yawNaarBinnen() + 90, 0);
                RijdierSoort.PluimKleur kleur = RijdierSoort.PluimKleur.vanOfNull(h.rijdierKleur);
                if (kleur != null) {
                    r.zetKleur(kleur);
                }
                if (!h.rijdierUiterlijk.isEmpty()) {
                    r.zetUiterlijk(h.rijdierUiterlijk);
                }
                r.setNoAi(true);
                r.setInvulnerable(true);
                r.setSilent(true);
                r.addTag(TAG);
                l.addFreshEntity(r);
                ids.add(r.getUUID());
            }
        }
        // the pets in the garden
        int i = 0;
        for (String id : h.huisdieren) {
            Huisdier hd = Huisdieren.van(id);
            HuisdierEntity e = hd == null ? null : HuisdierenRegistry.HUISDIER.get().create(l, EntitySpawnReason.TRIGGERED);
            if (e == null) {
                continue;
            }
            Vec3 v = tuinPlek(k, l.getRandom().nextInt(1000) + i++);
            e.snapTo(v.x, v.y, v.z, l.getRandom().nextFloat() * 360, 0);
            e.zetSoort(hd.id());
            e.setCustomName(Huisdieren.naamVan(hd, Component.literal(h.naam)));
            e.setInvulnerable(true);
            e.addTag(TAG);
            TUIN.put(e.getUUID(), k.sleutel());
            l.addFreshEntity(e);
            ids.add(e.getUUID());
        }
        // the trophy shelf
        List<Vec3> plekken = Ontwerp.trofeePlekken(k, h.soort);
        int t = 0;
        for (String g : h.trofeeen) {
            if (t >= plekken.size() || t >= MAX_TROFEEEN) {
                break;
            }
            Vec3 v = plekken.get(t++);
            Display.ItemDisplay item = EntityType.ITEM_DISPLAY.create(l, EntitySpawnReason.TRIGGERED);
            if (item != null) {
                item.getSlot(0).set(trofeeItem(g));
                CompoundTag tag = new CompoundTag();
                tag.put("transformation", transformatie(0.45f));
                Nbt.load(item, tag);
                item.snapTo(v.x, v.y + 0.22, v.z, k.yawNaarBinnen(), 0);
                item.addTag(TAG);
                l.addFreshEntity(item);
                ids.add(item.getUUID());
            }
            Display.TextDisplay naam = EntityType.TEXT_DISPLAY.create(l, EntitySpawnReason.TRIGGERED);
            if (naam != null) {
                CompoundTag tag = new CompoundTag();
                tag.put("text", ComponentSerialization.CODEC.encodeStart(l.registryAccess().createSerializationContext(NbtOps.INSTANCE), trofeeNaam(g)).getOrThrow());
                tag.putString("billboard", "center");
                tag.putInt("background", 0x60241A12);
                tag.put("transformation", transformatie(0.35f));
                Nbt.load(naam, tag);
                naam.snapTo(v.x, v.y + 0.62, v.z, 0, 0);
                naam.addTag(TAG);
                l.addFreshEntity(naam);
                ids.add(naam.getUUID());
            }
        }
        return ids.size();
    }

    private static CompoundTag transformatie(float schaal) {
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(0, 0, 0));
        t.put("left_rotation", floats(0, 0, 0, 1));
        t.put("scale", floats(schaal, schaal, schaal));
        t.put("right_rotation", floats(0, 0, 0, 1));
        return t;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    /** The figure on the shelf for a boss ("baaskill:&lt;entity id&gt;"). */
    static ItemStack trofeeItem(String sleutel) {
        String id = sleutel.substring(sleutel.indexOf(':') + 1);
        if (id.contains("koning_klodder")) {
            return new ItemStack(EcoSpul.MINI_KONING_KLODDER.get());
        }
        if (id.contains("opperzwam")) {
            return new ItemStack(EcoSpul.OPPERZWAM_BEELD.get());
        }
        if (id.contains("klodderreus")) {
            return new ItemStack(EcoSpul.KLODDERREUS_KNUFFEL.get());
        }
        return new ItemStack(Items.TOTEM_OF_UNDYING);   // (a little golden figure)
    }

    static Component trofeeNaam(String sleutel) {
        Identifier id = Identifier.tryParse(sleutel.substring(sleutel.indexOf(':') + 1));
        EntityType<?> t = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
        return t == null ? Component.literal("?") : t.getDescription();
    }

    /** A random free spot in the garden (back garden first, the front garden beside the path). */
    static Vec3 tuinPlek(Kavel k, int zaad) {
        int[][] vakken = {{7, 13, 15, 18}, {14, 18, 2, 3}, {1, 6, 2, 3}};
        int[] v = vakken[Math.floorMod(zaad, vakken.length)];
        int u = v[0] + Math.floorMod(zaad * 7, v[1] - v[0] + 1), vv = v[2] + Math.floorMod(zaad * 13, v[3] - v[2] + 1);
        return k.wereld(u + 0.5, WijkPlan.VLOER, vv + 0.5);
    }

    /** {@code HuisdierEntity.ZELFSTANDIG}: our garden pets wander around in their garden. */
    static boolean tuinTick(HuisdierEntity e) {
        if (!e.entityTags().contains(TAG)) {
            return false;
        }
        Integer k = TUIN.get(e.getUUID());
        if (k == null) {
            e.discard();
            return true;
        }
        if (e.tickCount % 80 == (e.getId() & 63)) {
            Vec3 v = tuinPlek(WijkPlan.kavel(k), e.getRandom().nextInt(1000));
            e.getNavigation().moveTo(v.x, v.y, v.z, 0.7);
        }
        Player p = e.level().getNearestPlayer(e, 6);
        if (p != null) {
            e.getLookControl().setLookAt(p, 30, 30);
        }
        if (e.onGround() && e.getRandom().nextInt(300) == 0) {
            e.getJumpControl().jump();
        }
        return true;
    }

    // ---- Mollie at the market ----------------------------------------------------------------------------------------------------

    static NpcDef mollieDef(int wijk) {
        BlockPos p = WijkPlan.midden(wijk).offset(WijkBouw.MOLLIE.getX(), 0, WijkBouw.MOLLIE.getZ());
        return new NpcDef(MOLLIE, p, 90f, GuhVariant.NORMAL, List.of(GuhClothes.ZONNEHOEDJE, GuhClothes.KNUS_SJAALTJE), NpcDef.Gedrag.STAAN, null, 1f);
    }

    @Nullable
    static RijkGuh zetMollie(ServerLevel l, int wijk) {
        RijkGuh g = NpcRegistry.GUH.get().create(l, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        NpcDef d = mollieDef(wijk);
        g.snapTo(d.plek().getX() + 0.5, d.plek().getY(), d.plek().getZ() + 0.5, d.yaw(), 0);
        g.setYHeadRot(d.yaw());
        g.setYBodyRot(d.yaw());
        g.stelIn(d);
        g.addTag(NPC_TAG);
        l.addFreshEntity(g);
        MOLLIES.put(wijk, g.getUUID());
        return g;
    }

    /** (tests / dev) how many figures stand for this plot now. */
    static int aantal(int sleutel) {
        List<UUID> l = PER_KAVEL.get(sleutel);
        return l == null ? 0 : l.size();
    }

    static void wisAlles(MinecraftServer server) {
        ServerLevel l = Kwartier.level(server);
        if (l != null) {
            for (Integer k : List.copyOf(PER_KAVEL.keySet())) {
                weg(l, k);
            }
            for (UUID id : MOLLIES.values()) {
                Entity e = l.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
        }
        PER_KAVEL.clear();
        TUIN.clear();
        MOLLIES.clear();
    }
}
