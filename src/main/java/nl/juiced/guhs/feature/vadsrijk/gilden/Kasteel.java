package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Materialen;
import nl.juiced.guhs.feature.vadsrijk.gilden.Rang.Recht;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.opslag.Materiaaltas;
import nl.juiced.guhs.feature.vadsrijk.opslag.Opslag;

/**
 * U-GILDEN: the bond's castle on its plot (V1 "een kasteeltje dat de leden samen uitbouwen, niveau 1-5"). Level 1 (de Wachttoren)
 * comes with founding; levels 2-5 need bond level, coins in the kas and profession materials in the depot (members donate,
 * {@link #doneer}), then an officer or the leader builds ({@link #bouwVolgende}).
 * <ol>
 *   <li>Wachttoren: tower with the teleport stone (lodestone) and the bond vault (chest);</li>
 *   <li>Torenhof: + a walled courtyard with a gatehouse and the Trofeeënzaal (boss trophies, placed by themselves);</li>
 *   <li>Burcht: + the smidse (smithing table) and the keuken (smoker), two corner towers;</li>
 *   <li>Vesting: four corner towers, + the naaiatelier (loom) and brouwerij (brewing stand), a garden;</li>
 *   <li>Slot: the great keep with a hall and chandeliers, spires, everything.</li>
 * </ol>
 * The blocks are pre-made templates ({@code tools/vadsrijk/gilden/kasteel.py} -> {@code guhs:vadsrijk/gilden/kasteel_<n>}, markers in
 * {@code data/guhs/vr_gilden/kasteel_<n>.json}). Clicks on the castle's lodestone / chest / workshop blocks are handled in
 * {@link #klik} (only for members of that bond).
 */
public final class Kasteel {
    private static final Logger LOG = LogUtils.getLogger();
    public static final int MAX = 5, HOOGTE = 90;
    public static final String TAG = "vr_bond_trofee";
    /** Tests: skip the world work (stamping) when false. */
    public static volatile boolean BOUWEN = true;

    /** What the next level costs: bond level, kas (brons), materials (Materialen id -> n). */
    public record Kosten(int bondLevel, long kas, Map<String, Integer> materialen) {
    }

    public static final Map<Integer, Kosten> KOSTEN = Map.of(
            2, new Kosten(2, 20000, mat("zwamsteel", 100, "pluisijzer", 60, "pluiskatoen", 40)),
            3, new Kosten(4, 50000, mat("kristalden", 120, "kandijzilver", 80, "kristalvlas", 60, "ijsbloem", 40)),
            4, new Kosten(6, 100000, mat("schemerhout", 150, "amberkoper", 100, "schemerzijde", 80, "lichtkeverkruid", 60)),
            5, new Kosten(8, 250000, mat("schemerhout", 250, "amberkoper", 200, "roze_hars", 80, "glinstergruis", 20, "bijenwas", 60)));

    private static Map<String, Integer> mat(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return m;
    }

    public static String naamKey(int level) {
        return "vadsrijk.gilden.kasteel." + Math.max(1, Math.min(MAX, level));
    }

    // ---- markers ----------------------------------------------------------------------------------------------------------------

    /** Template markers (relative to the plot corner, y 0 = ground): arrival, trophy spots. */
    public record Markers(int[] aankomst, List<int[]> trofeeen) {
    }

    private static final Map<Integer, Markers> MARKERS = new ConcurrentHashMap<>();

    public static Markers markers(int level) {
        return MARKERS.computeIfAbsent(Math.max(1, Math.min(MAX, level)), Kasteel::laadMarkers);
    }

    private static Markers laadMarkers(int level) {
        String pad = "/data/guhs/vr_gilden/kasteel_" + level + ".json";
        try (InputStream in = Kasteel.class.getResourceAsStream(pad)) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<int[]> tr = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("trofee")) {
                    tr.add(xyz(e.getAsJsonArray()));
                }
                return new Markers(xyz(o.getAsJsonArray("aankomst")), tr);
            }
        } catch (Exception e) {
            LOG.warn("[vadsrijk gilden] markers {} not readable: {}", pad, e.toString());
        }
        return new Markers(new int[] {32, 1, 4}, List.of());
    }

    private static int[] xyz(JsonArray a) {
        return new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()};
    }

    // ---- building -----------------------------------------------------------------------------------------------------------------

    /** (Re)builds the castle of this bond at its current level: ground, template, banners, trophies. */
    public static void bouw(MinecraftServer s, Bond b) {
        ServerLevel sl = Vallei.wereld(s);
        if (!BOUWEN || sl == null || b.kavel < 0) {
            return;
        }
        Bonden d = Bonden.get(s);
        Vallei.zorgVoorKavel(sl, d, b.kavel);
        BlockPos h = Vallei.kavelHoek(b.kavel);
        leeg(sl, h);
        if (!Vallei.stempel(sl, "kasteel_" + Math.max(1, Math.min(MAX, b.kasteel)), h)) {
            return;
        }
        b.gebouwd = b.kasteel;
        d.setDirty();
        kleurBanieren(s, b);
        trofeeen(s, b);
    }

    /** Builds the castle when what stands is not the bond's level (first visit after founding in a test world, a lost plot). */
    public static void bouwAlsNodig(MinecraftServer s, Bond b) {
        if (b.gebouwd != b.kasteel) {
            bouw(s, b);
        }
    }

    /** Clears the plot above the ground and lays fresh grass (before a new level, or when a bond is disbanded). */
    static void leeg(ServerLevel sl, BlockPos h) {
        Vallei.laad(sl, h, Vallei.KAVEL, Vallei.KAVEL);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState lucht = Blocks.AIR.defaultBlockState(), gras = Blocks.GRASS_BLOCK.defaultBlockState();
        for (int x = 0; x < Vallei.KAVEL; x++) {
            for (int z = 0; z < Vallei.KAVEL; z++) {
                if (x == 0 || z == 0 || x == Vallei.KAVEL - 1 || z == Vallei.KAVEL - 1) {
                    continue;   // (the hedge + gate posts of the plot cell stay)
                }
                for (int y = 1; y < HOOGTE; y++) {
                    m.set(h.getX() + x, Vallei.Y + y, h.getZ() + z);
                    if (!sl.getBlockState(m).isAir()) {
                        sl.setBlock(m, lucht, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                m.set(h.getX() + x, Vallei.Y, h.getZ() + z);
                if (!sl.getBlockState(m).is(Blocks.GRASS_BLOCK)) {
                    sl.setBlock(m, gras, Block.UPDATE_CLIENTS);
                }
            }
        }
        for (Entity e : sl.getEntitiesOfClass(Entity.class, new AABB(h.getX(), Vallei.Y, h.getZ(), h.getX() + Vallei.KAVEL, Vallei.Y + HOOGTE, h.getZ() + Vallei.KAVEL),
                e -> e.entityTags().contains(TAG))) {
            e.discard();
        }
    }

    /** A disbanded bond: its plot becomes an empty lawn again. */
    static void ruimOp(MinecraftServer s, Bond b) {
        ServerLevel sl = Vallei.wereld(s);
        if (BOUWEN && sl != null && b.kavel >= 0) {
            leeg(sl, Vallei.kavelHoek(b.kavel));
        }
    }

    /** Every banner on the plot gets the bond's banner. */
    public static int kleurBanieren(MinecraftServer s, Bond b) {
        ServerLevel sl = Vallei.wereld(s);
        if (!BOUWEN || sl == null || b.kavel < 0) {
            return 0;
        }
        BlockPos h = Vallei.kavelHoek(b.kavel);
        if (!sl.isLoaded(h) || !sl.isLoaded(h.offset(Vallei.KAVEL - 1, 0, Vallei.KAVEL - 1))) {
            return 0;
        }
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < Vallei.KAVEL; x++) {
            for (int z = 0; z < Vallei.KAVEL; z++) {
                for (int y = 1; y < HOOGTE; y++) {
                    m.set(h.getX() + x, Vallei.Y + y, h.getZ() + z);
                    BlockState st = sl.getBlockState(m);
                    if ((st.getBlock() instanceof BannerBlock || st.getBlock() instanceof WallBannerBlock) && Banier.zet(sl, m.immutable(), b)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /** The trophy hall: per boss the bond beat, a little statue (item display) + its name, on the template's trophy spots. */
    public static void trofeeen(MinecraftServer s, Bond b) {
        ServerLevel sl = Vallei.wereld(s);
        if (!BOUWEN || sl == null || b.kavel < 0) {
            return;
        }
        BlockPos h = Vallei.kavelHoek(b.kavel);
        if (!sl.isLoaded(h)) {
            return;
        }
        for (Entity e : sl.getEntitiesOfClass(Entity.class, new AABB(h.getX(), Vallei.Y, h.getZ(), h.getX() + Vallei.KAVEL, Vallei.Y + HOOGTE, h.getZ() + Vallei.KAVEL),
                e -> e.entityTags().contains(TAG))) {
            e.discard();
        }
        List<int[]> plekken = markers(b.kasteel).trofeeen();
        int i = 0;
        for (Map.Entry<String, Long> t : b.trofeeen.entrySet()) {
            if (i >= plekken.size()) {
                break;
            }
            int[] p = plekken.get(i++);
            double x = h.getX() + p[0] + 0.5, y = Vallei.Y + p[1], z = h.getZ() + p[2] + 0.5;
            beeldje(sl, x, y + 0.55, z, i <= 3 ? Items.TOTEM_OF_UNDYING : Items.GOLDEN_APPLE);
            tekst(sl, x, y + 1.35, z, Component.translatable(t.getKey()).withStyle(ChatFormatting.GOLD));
        }
    }

    private static void beeldje(ServerLevel sl, double x, double y, double z, Item item) {
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return;
        }
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(d);
        tag.put("item", ItemStack.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), new ItemStack(item)).getOrThrow());
        tag.putString("billboard", "vertical");
        nl.juiced.guhs.storage.Nbt.load(d, tag);
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
    }

    private static void tekst(ServerLevel sl, double x, double y, double z, Component c) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return;
        }
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(d);
        tag.put("text", ComponentSerialization.CODEC.encodeStart(sl.registryAccess().createSerializationContext(NbtOps.INSTANCE), c).getOrThrow());
        tag.putInt("background", 0x40000000);
        tag.putString("billboard", "center");
        nl.juiced.guhs.storage.Nbt.load(d, tag);
        d.snapTo(x, y, z, 0, 0);
        d.addTag(TAG);
        sl.addFreshEntity(d);
    }

    /** A boss was beaten by members of this bond: a new trophy (first time only). */
    static void trofee(MinecraftServer s, Bond b, String baasKey) {
        if (b.trofeeen.containsKey(baasKey)) {
            return;
        }
        b.trofeeen.put(baasKey, BondXp.KLOK.getAsLong());
        Bonden.get(s).setDirty();
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.trofee", Component.translatable(baasKey)).withStyle(ChatFormatting.GOLD));
        if (b.kasteel >= 2) {
            trofeeen(s, b);
        }
    }

    // ---- donating + upgrading ---------------------------------------------------------------------------------------------------------

    @Nullable
    public static Kosten volgende(Bond b) {
        return KOSTEN.get(b.kasteel + 1);
    }

    @Nullable
    static Item item(String materiaal) {
        var d = Materialen.ITEM.get(materiaal);
        return d == null ? null : d.get();
    }

    /** Donates every material the next level still needs from the inventory + materiaaltas. Returns how many pieces. */
    public static BondActies.Uitslag doneer(ServerPlayer p) {
        Bond b = Bonden.van(p);
        if (b == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.geen_bond");
        }
        Kosten k = volgende(b);
        if (k == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.kasteel.max");
        }
        int totaal = 0;
        for (Map.Entry<String, Integer> e : k.materialen().entrySet()) {
            int nodig = e.getValue() - b.depot.getOrDefault(e.getKey(), 0);
            Item it = item(e.getKey());
            if (nodig <= 0 || it == null) {
                continue;
            }
            int heb = Math.min(nodig, Opslag.tel(p, it));
            if (heb > 0 && Opslag.neem(p, it, heb)) {
                b.depot.merge(e.getKey(), heb, Integer::sum);
                totaal += heb;
            }
        }
        if (totaal <= 0) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.kasteel.niets");
        }
        MinecraftServer s = p.level().getServer();
        Bonden.get(s).setDirty();
        BondXp.geef(s, b, Bonden.sleutel(p), totaal / 4, "kasteel");
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.kasteel.gedoneerd", p.getGameProfile().name(), totaal).withStyle(ChatFormatting.AQUA));
        return BondActies.Uitslag.ja("vadsrijk.gilden.kasteel.gedoneerd_ok", totaal);
    }

    /** What is still missing for the next level (lang lines), empty = ready. */
    public static List<Component> mist(Bond b) {
        List<Component> l = new ArrayList<>();
        Kosten k = volgende(b);
        if (k == null) {
            return l;
        }
        if (b.level() < k.bondLevel()) {
            l.add(Component.translatable("vadsrijk.gilden.kasteel.mist.level", k.bondLevel()));
        }
        if (b.kas < k.kas()) {
            l.add(Component.translatable("vadsrijk.gilden.kasteel.mist.kas", Spelers.munten(b.kas), Spelers.munten(k.kas())));
        }
        for (Map.Entry<String, Integer> e : k.materialen().entrySet()) {
            int heb = b.depot.getOrDefault(e.getKey(), 0);
            if (heb < e.getValue()) {
                l.add(Component.translatable("vadsrijk.gilden.kasteel.mist.mat", Component.translatable("item.guhs.vr_amb_" + e.getKey()), heb, e.getValue()));
            }
        }
        return l;
    }

    public static BondActies.Uitslag bouwVolgende(ServerPlayer p) {
        Bond b = Bonden.van(p);
        Bond.Lid l = b == null ? null : b.lid(Bonden.sleutel(p));
        if (l == null || !l.rang.mag(Recht.KASTEEL)) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.fout.recht");
        }
        Kosten k = volgende(b);
        if (k == null) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.kasteel.max");
        }
        if (!mist(b).isEmpty()) {
            return BondActies.Uitslag.nee("vadsrijk.gilden.kasteel.nog_niet");
        }
        b.kas -= k.kas();
        for (Map.Entry<String, Integer> e : k.materialen().entrySet()) {
            b.depot.merge(e.getKey(), -e.getValue(), Integer::sum);
        }
        b.depot.values().removeIf(v -> v <= 0);
        b.kasteel++;
        MinecraftServer s = p.level().getServer();
        Bonden.get(s).setDirty();
        BondBank.sluitAlles(s, b);
        bouw(s, b);
        BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.kasteel.gebouwd", Component.translatable(naamKey(b.kasteel)), b.kasteel)
                .withStyle(ChatFormatting.GOLD));
        return BondActies.Uitslag.ja("vadsrijk.gilden.kasteel.gebouwd_ok", Component.translatable(naamKey(b.kasteel)));
    }

    /** A disband: the depot goes back to the leader (bag or inventory, never lost). */
    static void geefDepotTerug(ServerPlayer p, Bond b) {
        for (Map.Entry<String, Integer> e : b.depot.entrySet()) {
            Item it = item(e.getKey());
            int n = e.getValue();
            while (it != null && n > 0) {
                int stuk = Math.min(n, 64);
                Materiaaltas.geef(p, new ItemStack(it, stuk));
                n -= stuk;
            }
        }
        b.depot.clear();
    }

    // ---- clicks in the castle -------------------------------------------------------------------------------------------------------

    /** Right-click on a block in the valley: lodestone, chest/barrel, workshop blocks of a castle. True = handled (cancel vanilla). */
    static boolean klik(ServerPlayer p, BlockPos pos, BlockState st) {
        return klik(p, pos, st, true);
    }

    /** {@code doe} false (the off hand): only says whether it is ours, so the second hand's click is cancelled too. */
    static boolean klik(ServerPlayer p, BlockPos pos, BlockState st, boolean doe) {
        MinecraftServer s = p.level().getServer();
        int cel = Vallei.celOp(pos.getX() + 0.5, pos.getZ() + 0.5);
        if (cel < 0 || !Vallei.opKavel(cel, pos)) {
            return false;
        }
        boolean steen = st.is(Blocks.LODESTONE), kist = st.is(Blocks.CHEST) || st.is(Blocks.BARREL) || st.is(Blocks.TRAPPED_CHEST);
        nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht werk = st.is(Blocks.SMITHING_TABLE) ? nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht.SMEDEN
                : st.is(Blocks.SMOKER) ? nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht.KOKEN
                : st.is(Blocks.LOOM) ? nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht.NAAIEN
                : st.is(Blocks.BREWING_STAND) ? nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht.BROUWEN : null;
        if (!steen && !kist && werk == null && !st.is(Blocks.CRAFTING_TABLE) && !st.is(Blocks.FURNACE) && !st.is(Blocks.ANVIL)) {
            return false;
        }
        if (!doe) {
            return true;
        }
        Bond eigenaar = Bonden.get(s).opKavel(cel);
        Bond mijn = Bonden.van(p);
        if (eigenaar == null || mijn != eigenaar) {
            p.sendOverlayMessage(eigenaar == null ? Component.translatable("vadsrijk.gilden.kasteel.leeg").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("vadsrijk.gilden.kasteel.van", eigenaar.naam).withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (steen) {
            BondPaneel.steen(p, mijn);
        } else if (kist) {
            BondBank.open(p, mijn, pos);
        } else if (werk != null) {
            nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtSchermen.werkplaats(p, werk, "bond_werk");
        }
        return true;
    }
}
