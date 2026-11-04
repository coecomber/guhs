package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken.Puzzel;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry;

/**
 * De Spiegelburcht's rooms (the dungeon before Vrouwe Spiegelzucht's hall):
 * <ul>
 *   <li>2 light puzzles ({@link Z2BazenPlekken#PUZZELS}: de Lichtkamer, de Prismazaal): a crystal eye shines a beam; turn the
 *       mirrors (right-click: "/", "\", flat) so the beam ends on the crystal lock(s). All locks of a room glowing at once = its
 *       crystal gate opens. The Prismazaal has two beams and two locks.</li>
 *   <li>De Spiegelgang (the gauntlet): 3 waves of crystal creatures while blades of crystal shards sweep the corridor (red strips,
 *       30 % of your Vads). Last wave beaten = its gate (poort_3) opens.</li>
 *   <li>Reset: when nobody has been in the burcht for 60 s, every mirror, lock and gate goes back (all changes are {@link Herstel}
 *       keys, so a server start also restores them).</li>
 * </ul>
 * Everything is only placed where there is air (or our own block): wereld built the shell with open-air gates (POI_Z2.md
 * poort_2..4) and marked puzzle spots (puzzel_*): we fill them. The mirrors stand on those spots.
 */
public final class Spiegelburcht {
    public static final String H_INHOUD = "z2_burcht_inhoud", H_POORT = "z2_burcht_poort_";
    public static final int LEEG_TICKS = 1200, GOLVEN = 3;
    /** Possible ids of the zone-2 crystal creatures for the gauntlet (z2-wezens); missing = kern's grey test klodder. */
    public static final List<String> GANG_MOBS = List.of("guhs:vr_z2_mob_spiegelwacht", "guhs:vr_z2_mob_scherfgeest", "guhs:vr_z2_mob_splinterling");
    /** (integration) their elements (z2-wezens: Spiegelwacht/Splinterling GEEN + versteend, Scherfgeest VUUR). */
    private static final List<Element> GANG_ELEMENT = List.of(Element.GEEN, Element.VUUR, Element.GEEN);

    private static final Set<String> OPGELOST = new HashSet<>();
    private static boolean opgezet;
    private static long leegSinds = -1;
    // the gauntlet
    private static int gangToestand, golf, golfPauze, gangLeeg, hakT;
    private static final List<UUID> GANG_LEVEND = new ArrayList<>();
    private record Hak(Vec3 a, Vec3 b, int[] t) {
    }
    private static final List<Hak> HAKKEN = new ArrayList<>();

    private Spiegelburcht() {
    }

    public static boolean binnen(double x, double y, double z) {
        return Z2BazenPlekken.inBurcht(x, y, z);
    }

    public static boolean inGang(double x, double y, double z) {
        return Math.abs(x - Z2BazenPlekken.BURCHT_X) <= Z2BazenPlekken.GANG_X + 1 && z >= Z2BazenPlekken.GANG_Z0 && z <= Z2BazenPlekken.GANG_Z1 + 1
                && y >= Z2BazenPlekken.BURCHT_Y - 2 && y <= Z2BazenPlekken.BURCHT_Y + 8;
    }

    public static boolean opgelost(String id) {
        return OPGELOST.contains(id);
    }

    public static boolean gangKlaar() {
        return gangToestand == 2;
    }

    public static int golf() {
        return golf;
    }

    private static List<ServerPlayer> spelers(ServerLevel sl) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && binnen(p.getX(), p.getY(), p.getZ())) {
                l.add(p);
            }
        }
        return l;
    }

    private static void zeg(ServerLevel sl, String key, ChatFormatting kleur, Object... args) {
        for (ServerPlayer p : spelers(sl)) {
            p.sendOverlayMessage(Component.translatable(key, args).withStyle(kleur));
        }
    }

    // ---- setup / reset -------------------------------------------------------------------------------------------------

    private static void zet(ServerLevel sl, String key, BlockPos p, BlockState s) {
        BlockState nu = sl.getBlockState(p);
        if (nu.isAir() || nu.getBlock() == s.getBlock() || nu.canBeReplaced()) {
            Herstel.zet(sl, key, p, s);
        }
    }

    private static void poort(ServerLevel sl, String id, int z) {
        for (int x = -Z2BazenPlekken.POORT_BREED; x <= Z2BazenPlekken.POORT_BREED; x++) {
            for (int y = 0; y <= Z2BazenPlekken.POORT_HOOG; y++) {
                zet(sl, H_POORT + id, new BlockPos(Z2BazenPlekken.BURCHT_X + x, Z2BazenPlekken.BURCHT_Y + y, z), Z2BazenRegistry.KRISTALPOORT.get().defaultBlockState());
            }
        }
    }

    public static void zetOp(ServerLevel sl) {
        for (Puzzel pz : Z2BazenPlekken.PUZZELS) {
            for (int i = 0; i < pz.bronnen().size(); i++) {
                zet(sl, H_INHOUD, pz.bronnen().get(i), Z2BazenRegistry.LICHTBRON.get().defaultBlockState()
                        .setValue(KristalBlokken.Lichtbron.FACING, KristalBlokken.richting(pz.richtingen().get(i))));
            }
            for (int i = 0; i < pz.spiegels().size(); i++) {
                zet(sl, H_INHOUD, pz.spiegels().get(i), Z2BazenRegistry.SPIEGEL.get().defaultBlockState().setValue(SpiegelBlok.STAND, pz.startStanden().get(i)));
            }
            for (BlockPos s : pz.sloten()) {
                zet(sl, H_INHOUD, s, Z2BazenRegistry.KRISTALSLOT.get().defaultBlockState());
            }
            if (!OPGELOST.contains(pz.id())) {
                poort(sl, pz.id(), pz.poortZ());
            }
        }
        if (gangToestand != 2) {
            poort(sl, "gang", Z2BazenPlekken.GANG_POORT);
        }
        opgezet = true;
    }

    public static void reset(ServerLevel sl) {
        for (String k : Herstel.sleutels(sl)) {
            if (k.startsWith("z2_burcht_")) {
                Herstel.terug(sl, k);
            }
        }
        OPGELOST.clear();
        opgezet = false;
        gangToestand = 0;
        golf = 0;
        ruimGang(sl);
    }

    private static void ruimGang(ServerLevel sl) {
        for (UUID id : GANG_LEVEND) {
            Entity e = sl.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        GANG_LEVEND.clear();
        HAKKEN.clear();
    }

    // ---- the tick --------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        List<ServerPlayer> l = spelers(sl);
        long nu = sl.getGameTime();
        if (l.isEmpty()) {
            if (opgezet && leegSinds < 0) {
                leegSinds = nu;
            }
            if (opgezet && leegSinds >= 0 && nu - leegSinds >= LEEG_TICKS) {
                reset(sl);
                leegSinds = -1;
            }
            return;
        }
        leegSinds = -1;
        if (!sl.isLoaded(Z2BazenPlekken.k(0, Z2BazenPlekken.GANG_Z1))) {
            return;
        }
        if (!opgezet) {
            zetOp(sl);
            for (ServerPlayer p : l) {
                RijkScenes.titel(p, Component.translatable("vadsrijk.z2bazen.burcht").withStyle(ChatFormatting.BOLD),
                        Component.translatable("vadsrijk.z2bazen.burcht.onder"), 60, 0xC8C8E0);
            }
        }
        gang(sl, nu);
        if (nu % 10 == 0) {
            puzzels(sl);
        }
    }

    /** Traces every beam, lights the locks, opens a gate when all its locks glow. */
    public static void puzzels(ServerLevel sl) {
        for (Puzzel pz : Z2BazenPlekken.PUZZELS) {
            Set<BlockPos> geraakt = new HashSet<>();
            for (int i = 0; i < pz.bronnen().size(); i++) {
                Lichtstraal.Pad pad = Lichtstraal.volg(sl, pz.bronnen().get(i), pz.richtingen().get(i));
                Lichtstraal.toon(sl, pad);
                if (pad.slot() && pad.eind() != null) {
                    geraakt.add(pad.eind());
                }
            }
            boolean alles = !pz.sloten().isEmpty();
            for (BlockPos s : pz.sloten()) {
                BlockState st = sl.getBlockState(s);
                boolean aan = geraakt.contains(s);
                alles &= aan;
                if (st.getBlock() instanceof KristalBlokken.Slot && st.getValue(KristalBlokken.Slot.LIT) != aan) {
                    sl.setBlock(s, st.setValue(KristalBlokken.Slot.LIT, aan), 2);
                    if (aan) {
                        sl.playSound(null, s, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 1.2f);
                    }
                }
            }
            if (alles && !OPGELOST.contains(pz.id())) {
                OPGELOST.add(pz.id());
                Herstel.terug(sl, H_POORT + pz.id());
                BlockPos g = Z2BazenPlekken.k(0, pz.poortZ());
                sl.sendParticles(ParticleTypes.END_ROD, g.getX() + 0.5, g.getY() + 2, g.getZ() + 0.5, 40, 1.2, 1.2, 0.3, 0.05);
                sl.playSound(null, g, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 1.4f);
                zeg(sl, "vadsrijk.z2bazen.burcht.open", ChatFormatting.GOLD);
            }
        }
    }

    /** Dev: turns every puzzle mirror to its solution. */
    public static void los(ServerLevel sl) {
        for (Puzzel pz : Z2BazenPlekken.PUZZELS) {
            for (int i = 0; i < pz.spiegels().size(); i++) {
                int st = pz.oplossing().get(i);
                BlockState s = sl.getBlockState(pz.spiegels().get(i));
                if (st >= 0 && s.getBlock() instanceof SpiegelBlok) {
                    sl.setBlock(pz.spiegels().get(i), s.setValue(SpiegelBlok.STAND, st), 3);
                }
            }
        }
        puzzels(sl);
    }

    public static String oplossing() {
        StringBuilder b = new StringBuilder();
        String[] namen = {"/", "\\", "plat"};
        for (Puzzel pz : Z2BazenPlekken.PUZZELS) {
            b.append(pz.id()).append(": ");
            for (int i = 0; i < pz.spiegels().size(); i++) {
                BlockPos p = pz.spiegels().get(i);
                int st = pz.oplossing().get(i);
                b.append(p.getX()).append(",").append(p.getZ()).append("=").append(st < 0 ? "-" : namen[st]).append("  ");
            }
            b.append("\n");
        }
        return b.toString();
    }

    // ---- De Splinterzaal ---------------------------------------------------------------------------------------------------

    private static void gang(ServerLevel sl, long nu) {
        List<ServerPlayer> in = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && inGang(p.getX(), p.getY(), p.getZ())) {
                in.add(p);
            }
        }
        // blades in flight
        for (Iterator<Hak> it = HAKKEN.iterator(); it.hasNext(); ) {
            Hak h = it.next();
            if (++h.t()[0] < 25) {
                continue;
            }
            it.remove();
            for (double x = h.a().x; x <= h.b().x; x += 1) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TINTED_GLASS.defaultBlockState()), x, h.a().y + 0.6, h.a().z, 3, 0.2, 0.3, 0.2,
                        0.05);
            }
            sl.playSound(null, BlockPos.containing(h.a().add(6, 0, 0)), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 1.6f);
            for (ServerPlayer p : in) {
                if (Z2Baas.inStrook(p.position(), h.a(), h.b(), 0.85)) {
                    Schade.doe(new Schade.Klap(null, p, Spelers.maxVads(p) * 0.3, Element.GEEN, Schade.Soort.WAAR, false, "z2bazen_splinterzaal"));
                }
            }
        }
        if (gangToestand == 2) {
            return;
        }
        if (gangToestand == 0) {
            if (!in.isEmpty()) {
                gangToestand = 1;
                golf = 0;
                golfPauze = 40;
                gangLeeg = 0;
                for (ServerPlayer p : in) {
                    p.sendOverlayMessage(Component.translatable("vadsrijk.z2bazen.burcht.gang").withStyle(ChatFormatting.RED));
                    Tips.toon(p, "vadsrijk.z2bazen.tip.splinterzaal");
                }
            }
            return;
        }
        // active
        gangLeeg = in.isEmpty() ? gangLeeg + 1 : 0;
        if (gangLeeg > 600) {
            ruimGang(sl);
            gangToestand = 0;
            golf = 0;
            return;
        }
        if (!in.isEmpty() && --hakT <= 0) {
            hakT = golf >= 3 ? 22 : 30;
            for (int k = 0; k < 2; k++) {
                int z = Z2BazenPlekken.GANG_Z0 + 2 + sl.getRandom().nextInt(Z2BazenPlekken.GANG_Z1 - Z2BazenPlekken.GANG_Z0 - 3);
                Vec3 a = new Vec3(Z2BazenPlekken.BURCHT_X - Z2BazenPlekken.GANG_X, Z2BazenPlekken.BURCHT_Y, z + 0.5), b = new Vec3(Z2BazenPlekken.BURCHT_X + Z2BazenPlekken.GANG_X + 1, Z2BazenPlekken.BURCHT_Y, z + 0.5);
                Telegraaf.lijn(sl, a, b, 1.7, 25, Z2Baas.ROOD);
                HAKKEN.add(new Hak(a, b, new int[1]));
            }
        }
        GANG_LEVEND.removeIf(id -> !(sl.getEntity(id) instanceof VadsrijkMob m) || m.isRemoved() || m.geplofd());
        if (GANG_LEVEND.isEmpty()) {
            if (golf >= GOLVEN) {
                gangToestand = 2;
                HAKKEN.clear();
                Herstel.terug(sl, H_POORT + "gang");
                BlockPos g = Z2BazenPlekken.k(0, Z2BazenPlekken.GANG_POORT);
                sl.sendParticles(ParticleTypes.END_ROD, g.getX() + 0.5, g.getY() + 2, g.getZ() + 0.5, 40, 1.2, 1.2, 0.3, 0.05);
                sl.playSound(null, g, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 1.2f);
                zeg(sl, "vadsrijk.z2bazen.burcht.gang_klaar", ChatFormatting.GOLD);
                return;
            }
            if (--golfPauze <= 0) {
                golf++;
                golfPauze = 60;
                int n = 2 + golf;
                for (int i = 0; i < n; i++) {
                    BlockPos p = Z2BazenPlekken.k(-5 + sl.getRandom().nextInt(11), Z2BazenPlekken.GANG_Z0 + 1 + sl.getRandom().nextInt(6));
                    VadsrijkMob m = gangMob(sl, p, golf == GOLVEN && i == 0);
                    if (m != null) {
                        GANG_LEVEND.add(m.getUUID());
                    }
                }
                zeg(sl, "vadsrijk.z2bazen.burcht.golf", ChatFormatting.RED, golf, GOLVEN);
            }
        }
    }

    private static VadsrijkMob gangMob(ServerLevel sl, BlockPos pos, boolean elite) {
        Entity e = null;
        int start = sl.getRandom().nextInt(GANG_MOBS.size()), keuze = -1;
        for (int k = 0; k < GANG_MOBS.size(); k++) {
            String id = GANG_MOBS.get((start + k) % GANG_MOBS.size());
            keuze = (start + k) % GANG_MOBS.size();
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).orElse(null);
            if (t != null) {
                e = t.create(sl, EntitySpawnReason.MOB_SUMMONED);
                if (e instanceof VadsrijkMob) {
                    break;
                }
                keuze = -1;
                if (e != null) {
                    e.discard();
                    e = null;
                }
            }
        }
        if (e == null) {
            keuze = -1;
            e = KernRegistry.PROEFKLODDER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        }
        if (!(e instanceof VadsrijkMob m)) {
            return null;
        }
        m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        m.stelIn(29, keuze >= 0 ? GANG_ELEMENT.get(keuze) : Element.WATER, elite, Set.of());
        if (m instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        sl.addFreshEntity(m);
        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 12, 0.3, 0.5, 0.3, 0.02);
        return m;
    }
}
