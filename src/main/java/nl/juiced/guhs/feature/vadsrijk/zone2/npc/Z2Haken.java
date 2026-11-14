package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Zone 2 on top of zone 1's quest hooks (NpcHaken already counts defeats, areas, campfires, talk objectives and the POIs we add
 * to NpcPlekken.POI). This class adds what is zone-2 specific:
 * <ul>
 *   <li>the petrified guhs at the lake (z2_hq06): a click frees them with a crystal-shatter (a talk objective of the quest), and a
 *       freed guh thanks you afterwards (per player); they stand still as statues (no AI);</li>
 *   <li>the dragon ravine (z2_hq09): arriving there starts the scene ({@link Z2Verhaal#draak});</li>
 *   <li>the zone-2 secrets: 10 knuffels under the ice, 5 clues to the Mika-boormeester (the first starts the secret quest, all
 *       five unlock the boss: z2-bazen {@code Boormeester.ontgrendel}), 5 treasures; glitter only you see, found at radius 3;</li>
 *   <li>named zone-2 bosses reported to quests ({@code meld(p, "z2_boormeester", "verslagen")});</li>
 *   <li>the haven pilot: the route to Kandijkom after zone 1 + lv 20 (an extra choice in his dialogue) and no soft-lock while
 *       z2_hq01 runs; the quest actions z2_vlieg / z2_flashback / z2_cliffhanger.</li>
 * </ul>
 * Secrets live in {@code extra("npc")}: {@code z2_knuffels}, {@code z2_aanwijzingen}, {@code z2_schatten} ({id: true}); quest objectives
 * count them with "teller:&lt;map&gt;".
 */
public final class Z2Haken {
    public static final String KNUFFELS = "z2_knuffels", AANWIJZINGEN = "z2_aanwijzingen", SCHATTEN = "z2_schatten";
    static final String HQ01 = "z2_hq01_splinterpieken", HQ06 = "z2_hq06_versteend", HQ09 = "z2_hq09_draak", HQ11 = "z2_hq11_spiegelzucht",
            HQ12 = "z2_hq12_hartsplinter";
    private static final Map<UUID, Long> DRAAK_VRAAG = new ConcurrentHashMap<>();
    static final int OPTIE_KANDIJKOM = 2, OPTIE_KAMP = 3;

    private Z2Haken() {
    }

    static void registreer() {
        Haken.VERSLAGEN.add(Z2Haken::verslagen);
        Haken.GEBIED_BETREDEN.add(Z2Haken::gebied);
        QuestMotor.KLIK_VOOR.add(Z2Haken::klikVoor);
        QuestMotor.ACTIES.put("z2_vlieg", (p, naar) -> Z2Verhaal.vlieg(p, naar.isEmpty() ? "kandijkom" : naar));
        QuestMotor.ACTIES.put("z2_flashback", (p, a) -> Z2Verhaal.flashback(p));
        QuestMotor.ACTIES.put("z2_cliffhanger", (p, a) -> Z2Verhaal.cliffhanger(p));
        // (fix round 2) the haven pilot's line to Kandijkom is one of QuestMotor's travel choices now (QuestMotor.REIZEN)
        Praat.luister(SLEUTEL_PILOOT, Z2Haken::pilootAntwoord);
        kamers();
        nl.juiced.guhs.feature.vadsrijk.npc.QuestStap.ROUTES.add(Z2Haken::geodePoort);
    }

    // ---- NPC clicks -------------------------------------------------------------------------------------------------------------

    static final String SLEUTEL_PILOOT = "vr_z2_npc_piloot";

    static boolean klikVoor(ServerPlayer p, RijkGuh g) {
        String id = g.npcId();
        if (!Z2Npcs.versteend(id)) {
            return false;
        }
        if (bevrijd(p, id)) {
            Praat.scene(p, Z2Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", "vadsrijk.z2npc.vrij." + id)));
            return true;
        }
        Quest q = Quests.van(HQ06);
        int i = doelVan(q, id);
        if (q != null && i >= 0 && Questlog.actief(p, HQ06) && !Questlog.doelKlaar(p, q, i)) {
            // free it: the crystal bursts into grey snow (the talk objective + its lines run in QuestMotor right after this)
            if (p.level() instanceof ServerLevel sl) {
                Vec3 c = g.position().add(0, 0.6, 0);
                sl.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), true, true, c.x, c.y, c.z, 80, 0.4,
                        0.5, 0.4, 0.2);
                sl.sendParticles(p, ParticleTypes.HEART, true, true, c.x, c.y + 0.8, c.z, 6, 0.4, 0.3, 0.4, 0.02);
            }
            p.level().playSound(null, g.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.2f, 0.8f);
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.bevrijd", Component.translatable("vadsrijk.npc.naam." + id))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            Tips.toon(p, "vadsrijk.npc.tip.z2_versteend");
            return false;
        }
        // not (yet) on the quest: the statue whispers
        Praat.scene(p, Z2Verhaal.SLEUTEL, List.of(new Praat.Regel(g, "", nl.juiced.guhs.feature.vadsrijk.npc.NpcDef.gedagKey(id, p.getRandom()))));
        Tips.toon(p, "vadsrijk.npc.tip.z2_versteend");
        return true;
    }

    /**
     * (fix round 2) Inside the Kandijkom geode with a target outside it: the tracker first points at the gate on that side (the
     * straight line runs into the geode wall). Gates: Z2NpcPlekken.POORT_*.
     */
    static nl.juiced.guhs.feature.vadsrijk.npc.QuestStap geodePoort(ServerPlayer p, nl.juiced.guhs.feature.vadsrijk.npc.QuestStap s) {
        BlockPos d = s.doel();
        BlockPos m = Z2NpcPlekken.GEODE_MIDDEN;
        if (d == null || Math.hypot(p.getX() - m.getX(), p.getZ() - m.getZ()) > Z2NpcPlekken.GEODE_BINNEN
                || Math.hypot(d.getX() - m.getX(), d.getZ() - m.getZ()) < Z2NpcPlekken.GEODE_BINNEN + 12 || d.getZ() > m.getZ() + 140) {
            return s;   // (not in the geode, the target is in town, or it is the dock / the airship: no gate needed)
        }
        int dx = d.getX() - m.getX(), dz = d.getZ() - m.getZ();
        String poort = Math.abs(dx) >= Math.abs(dz) || dz > 0 ? (dx >= 0 ? "oost" : "west") : "noord";
        BlockPos pos = poort.equals("oost") ? Z2NpcPlekken.POORT_OOST : poort.equals("west") ? Z2NpcPlekken.POORT_WEST : Z2NpcPlekken.POORT_NOORD;
        return new nl.juiced.guhs.feature.vadsrijk.npc.QuestStap(s.quest(), s.hoofd(), s.titel(), s.wat(),
                Component.translatable("vadsrijk.npc.stap.z2_poort", s.waar(), Component.translatable("vadsrijk.npc.plek.z2_poort_" + poort)), pos,
                s.extra());
    }

    /** (fix round 2) The Spiegelburcht's puzzle rooms: their tip stays at the bottom of the screen while you are inside. */
    static void kamers() {
        int y = nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken.BURCHT_Y;
        for (nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken.Puzzel pz : nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken.PUZZELS) {
            nl.juiced.guhs.feature.vadsrijk.npc.KamerTips.KAMERS.add(new nl.juiced.guhs.feature.vadsrijk.npc.KamerTips.Kamer("z2_" + pz.id(),
                    (x, yy, z) -> Math.abs(x) <= pz.x() + 1 && z >= pz.z0() && z <= pz.z1() + 1 && yy >= y - 2 && yy <= y + 12,
                    p -> nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelburcht.opgelost(pz.id()) ? "vadsrijk.npc.kamer.z2_opgelost"
                            : "vadsrijk.npc.kamer.z2_" + pz.id()));
        }
        nl.juiced.guhs.feature.vadsrijk.npc.KamerTips.KAMERS.add(new nl.juiced.guhs.feature.vadsrijk.npc.KamerTips.Kamer("z2_spiegelgang",
                nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelburcht::inGang,
                p -> nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel.Spiegelburcht.gangKlaar() ? "vadsrijk.npc.kamer.z2_opgelost"
                        : "vadsrijk.npc.kamer.z2_spiegelgang"));
    }

    private static void pilootAntwoord(ServerPlayer p, @Nullable net.minecraft.world.entity.Entity spreker, int optie) {
        if (optie == OPTIE_KANDIJKOM) {
            Praat.sluit(p);
            Z2Verhaal.naarKandijkom(p);
        } else if (optie == OPTIE_KAMP) {
            Praat.sluit(p);
            nl.juiced.guhs.feature.vadsrijk.npc.Verhaal.vlieg(p, "knuffelkamp");
        }
    }

    /** Is this petrified guh freed for this player? (the main quest done, or its objective counted) */
    public static boolean bevrijd(ServerPlayer p, String npc) {
        if (Questlog.klaar(p, HQ06)) {
            return true;
        }
        Quest q = Quests.van(HQ06);
        int i = doelVan(q, npc);
        return q != null && i >= 0 && Questlog.actief(p, HQ06) && Questlog.doelKlaar(p, q, i);
    }

    private static int doelVan(@Nullable Quest q, String npc) {
        if (q == null) {
            return -1;
        }
        for (int i = 0; i < q.doelen().size(); i++) {
            if (q.doelen().get(i).type() == Quest.Doel.Type.PRAAT && q.doelen().get(i).doel().equals(npc)) {
                return i;
            }
        }
        return -1;
    }

    // ---- defeats and areas -----------------------------------------------------------------------------------------------------

    static void verslagen(Haken.Verslagen v) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(v.mob().getType()).getPath();
        String baas = type.contains("boormeester") ? "z2_boormeester" : type.contains("galmwolf") ? "z2_galmwolf" : type.contains("spiegelzucht")
                ? "z2_spiegelzucht" : null;
        if (baas == null) {
            return;
        }
        Set<ServerPlayer> wie = new LinkedHashSet<>();
        for (ServerPlayer p : v.voorQuests()) {
            wie.add(p);
            wie.addAll(Partijen.ledenInBuurt(p, 48));
        }
        for (ServerPlayer p : wie) {
            NpcApi.meld(p, baas, "verslagen");
        }
    }

    static void gebied(ServerPlayer p, String gebied) {
        switch (gebied) {
            case "z2_galmhellingen", "z2_splinterkloof", "z2_spiegelburcht" -> Tips.toon(p, "vadsrijk.npc.tip.z2_kristal");
            case "z2_knuffelmeer" -> {
                Tips.toon(p, "vadsrijk.npc.tip.z2_kristal");
                Tips.toon(p, "vadsrijk.npc.tip.z2_versteend");
            }
            default -> {
            }
        }
    }

    // ---- the ticker ---------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        long t = server.getTickCount();
        if (t % 10 != 6) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        if (t % 20 == 6) {
            beelden(sl);
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || RijkScenes.bezig(p) || !nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.inZone2(p.getX(), p.getZ())) {
                continue;   // (zone 1 players skip all of this)
            }
            hogePlekken(p);
            draak(p, t);
            geheimen(p, sl);
            if (Questlog.klaar(p, HQ11) && Questlog.status(p, HQ12) == 0 && !Z2Verhaal.bezig(p)) {
                Quest q = Quests.van(HQ12);   // (the flashback's follow-up, when the scene was cut short by a log-out)
                if (q != null) {
                    Questlog.neemAan(p, q);
                }
            }
        }
    }

    /** Petrified guhs are statues: no AI, a little frost around them. */
    static void beelden(ServerLevel sl) {
        for (String id : Z2NpcPlekken.NPC.keySet()) {
            if (!Z2Npcs.versteend(id)) {
                continue;
            }
            RijkGuh g = Npcs.levend(sl, id);
            if (g != null) {
                if (!g.isNoAi()) {
                    g.setNoAi(true);
                }
                sl.sendParticles(ParticleTypes.SNOWFLAKE, g.getX(), g.getY() + 0.8, g.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    /** Zone-2 poi objectives with more height tolerance (needles, lookouts, the ravine): zone 1's ticker uses 10 blocks. */
    static void hogePlekken(ServerPlayer p) {
        for (Quest q : Questlog.actieve(p)) {
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if ((d.type() == Quest.Doel.Type.GEBIED || d.type() == Quest.Doel.Type.GEHEIM) && d.doel().startsWith("poi:z2_") && !Questlog.doelKlaar(p, q, i)) {
                    String poi = d.doel().substring(4);
                    BlockPos b = Z2NpcPlekken.poi(poi);
                    if (b != null && NpcHaken.bij(p, b, Z2NpcPlekken.straalVan(poi), Z2NpcPlekken.hoogteVan(poi))) {   // W-BEREIK: straalVan
                        Questlog.tel(p, d.type(), x -> x == d, 1);
                    }
                }
            }
        }
    }

    /** At the ravine with the dragon step open: the scene (once), afterwards only his question again. */
    static void draak(ServerPlayer p, long t) {
        if (!Z2Verhaal.draakKlaarVoorScene(p) || Praat.bezig(p)) {
            return;
        }
        BlockPos b = Z2NpcPlekken.poi("z2_drakenravijn");
        if (b == null || !NpcHaken.bij(p, b, 14, Z2NpcPlekken.NAALD_HOOGTE)) {
            return;
        }
        if (!Spelers.data(p).gezien.contains("npc:z2_draak_1")) {
            Z2Verhaal.draak(p);
        } else if (t - DRAAK_VRAAG.getOrDefault(p.getUUID(), -1000L) > 200) {
            DRAAK_VRAAG.put(p.getUUID(), t);
            Z2Verhaal.draakOpnieuw(p);
        }
    }

    // ---- secrets -------------------------------------------------------------------------------------------------------------------

    static void geheimen(ServerPlayer p, ServerLevel sl) {
        zoek(p, sl, Z2NpcPlekken.KNUFFEL, KNUFFELS, ParticleTypes.HEART);
        zoek(p, sl, Z2NpcPlekken.AANWIJZING, AANWIJZINGEN, ParticleTypes.ENCHANT);
        zoek(p, sl, Z2NpcPlekken.SCHAT, SCHATTEN, ParticleTypes.WAX_ON);
    }

    private static void zoek(ServerPlayer p, ServerLevel sl, Map<String, BlockPos> plekken, String soort, ParticleOptions glinster) {
        CompoundTag gevonden = Questlog.map(p, soort);
        for (Map.Entry<String, BlockPos> e : plekken.entrySet()) {
            if (gevonden.getBooleanOr(e.getKey(), false)) {
                continue;
            }
            BlockPos b = e.getValue();
            double dx = p.getX() - (b.getX() + 0.5), dz = p.getZ() - (b.getZ() + 0.5);
            if (dx * dx + dz * dz < 14 * 14 && Math.abs(p.getY() - b.getY()) < 12) {
                sl.sendParticles(p, glinster, false, false, b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 2, 0.25, 0.3, 0.25, 0.01);
                if (soort.equals(KNUFFELS)) {   // (under the ice: a faint pink shimmer in the ice too)
                    sl.sendParticles(p, ParticleTypes.SNOWFLAKE, false, false, b.getX() + 0.5, b.getY() + 1.0, b.getZ() + 0.5, 1, 0.3, 0.1, 0.3, 0.0);
                }
            }
            if (NpcHaken.bij(p, b, Z2NpcPlekken.VIND_STRAAL, Z2NpcPlekken.VIND_HOOGTE)) {
                vind(p, sl, soort, e.getKey(), b);
            }
        }
    }

    public static void vind(ServerPlayer p, ServerLevel sl, String soort, String id, BlockPos b) {
        CompoundTag gevonden = Questlog.map(p, soort);
        if (gevonden.getBooleanOr(id, false)) {
            return;
        }
        gevonden.putBoolean(id, true);
        int n = gevonden.size();
        Vec3 c = Vec3.atCenterOf(b);
        switch (soort) {
            case KNUFFELS -> {
                // freed from the ice: a crack of ice, then hearts
                sl.sendParticles(p, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), false, false, c.x, c.y + 0.5, c.z, 30, 0.4,
                        0.3, 0.4, 0.1);
                sl.sendParticles(p, ParticleTypes.HEART, false, false, c.x, c.y + 0.8, c.z, 8, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, b, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.4f);
                p.level().playSound(null, b, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.knuffel.gevonden", Component.translatable("vadsrijk.z2npc.knuffel." + id), n,
                        Z2NpcPlekken.KNUFFEL.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (n >= Z2NpcPlekken.KNUFFEL.size()) {
                    GuhAdvancements.grant(p, "vr_z2_knuffels_alle");
                }
            }
            case AANWIJZINGEN -> {
                p.level().playSound(null, b, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
                List<Praat.Regel> r = new ArrayList<>();
                r.add(new Praat.Regel(null, "vadsrijk.z2npc.aanwijzing.naam", "vadsrijk.z2npc.aanwijzing." + id));
                r.add(new Praat.Regel(null, "vadsrijk.z2npc.aanwijzing.naam", "vadsrijk.z2npc.aanwijzing.teller", n, Z2NpcPlekken.AANWIJZING.size()));
                Praat.scene(p, Z2Verhaal.SLEUTEL, r);
                Quest q = Quests.van("z2_geheim_boormeester");
                if (q != null && Questlog.status(p, q.id()) == 0) {
                    Questlog.neemAan(p, q);
                }
                if (n >= Z2NpcPlekken.AANWIJZING.size()) {
                    GuhAdvancements.grant(p, "vr_z2_aanwijzingen_alle");
                    p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.aanwijzing.alle").withStyle(ChatFormatting.DARK_PURPLE));
                    NpcRollen.doe("z2_boormeester", p);   // (z2-bazen: the hidden boorhal opens for this player)
                }
            }
            case SCHATTEN -> {
                p.level().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.3f);
                nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.titel(p, Component.translatable("vadsrijk.z2npc.schat.titel").withStyle(ChatFormatting.GOLD),
                        Component.translatable("vadsrijk.z2npc.schat." + id), 60, 0xFFD24A);
                BasisType bt = stukVoor(p);
                List<ItemStack> buit = bt == null ? List.of()
                        : List.of(Rollen.stack(Rollen.rol(bt, Math.max(1, Spelers.level(p)), id.equals("burchtdak") ? Zeldzaamheid.EPISCH : Zeldzaamheid.ZELDZAAM,
                                p.getRandom())));
                LootBundel.laatVallen(p, c.add(0, 0.8, 0), new LootTabel.Resultaat(buit, 6000), 2);
                GuhAdvancements.grant(p, "vr_z2_schat_" + id);
            }
            default -> {
            }
        }
        Questlog.kijkLive(p);
    }

    /** A random piece the player can use: a weapon of their class or any armour/jewellery (as zone 1's Beloningen). */
    @Nullable
    static BasisType stukVoor(ServerPlayer p) {
        return nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools.stuk(2, Spelers.data(p).klasse, p.getRandom());   // 1101-balans: only this zone's pool
    }

    /** (dev/tests) */
    static void vergeet(ServerPlayer p) {
        DRAAK_VRAAG.remove(p.getUUID());
        for (String m : new String[] {KNUFFELS, AANWIJZINGEN, SCHATTEN}) {
            Questlog.data(p).remove(m);
        }
    }

    /** (dev) the area the player stands in. */
    @Nullable
    static String gebiedVan(ServerPlayer p) {
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        return g == null ? null : g.id();
    }
}
