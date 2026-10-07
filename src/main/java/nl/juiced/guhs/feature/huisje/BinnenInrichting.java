package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.MijnGuhs;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;

/**
 * 1.3.2: what you see inside a Guhhuisje besides the room itself, made when somebody comes in, kept up to date while
 * somebody is there and removed when the last one leaves. Per bed (one per resident slot, in the order of the huisje's
 * resident list):
 * <ul>
 *   <li>the name sign above it (or "vrij bedje");</li>
 *   <li>the resident's favourite thing on the bedside table (a favourite the owner discovered: its knuffel, else its
 *   snack; a kaasknabbel while nothing is discovered yet);</li>
 *   <li>a resident that is asleep in its huisje ({@link Huisjes#isBinnen}, or "slaapt in huisje" in the band's records
 *   when it is not loaded): a sleeping stand-in ({@link BinnenGuh}) under a blanket, its clothes on the hook; a maatje is
 *   a little bump under the blanket;</li>
 *   <li>a resident that is away: an empty bed with a note that says where it is.</li>
 * </ul>
 * Nothing here touches the real guh, except hearts: the owner tucks a sleeper in (once per night, {@link Reden#KNUFFELEN})
 * or pets it softly ({@link Reden#AAIEN}); those go to the real guh when it is loaded and are remembered
 * ({@link Huisjes#teGoed}) until it is.
 */
public final class BinnenInrichting {
    /** Where a resident is, for the note on its bed (lang gui.guhs.huisje.binnen.briefje.&lt;id&gt;). */
    public enum Waar { SLAAPT, BUITEN, KLUS, SPEELT, VAKANTIE, MEE, KANTOOR, GUHKAMER, PARKOUR, GUHPIXEL, ZIT, WOLKJES, WEG }

    /** One resident as the room shows it. */
    public record Slot(UUID id, boolean guh, Component naam, Waar waar, Component detail, CompoundTag looks, boolean baby, boolean ingestopt,
                       ItemStack lievelings) {
    }

    /** What a room shows: who is where (as a text to compare), the slots, and how many things were put there. */
    private record Staat(String teken, List<Slot> slots, int aantal) {
    }

    /** Counts what {@link #bouw} puts in a room. */
    private static int gezet;

    private static final Map<Integer, Staat> STATEN = new HashMap<>();
    private static final Map<UUID, Integer> AAI_RUST = new ConcurrentHashMap<>();
    private static final String DEK = "guhs_huisje_binnen_dek_";
    /** The top of the mattress of a guhbedje, above the block's floor. */
    private static final double BED_HOOGTE = 7 / 16.0;

    private BinnenInrichting() {
    }

    // =====================================================================================================================
    // who is where
    // =====================================================================================================================

    /** The residents of this huisje as the room shows them, in bed order. */
    public static List<Slot> stand(MinecraftServer s, Huisje h) {
        List<Slot> uit = new ArrayList<>();
        BandData band = BandData.get(s);
        Huisjes data = Huisjes.get(s);
        long dag = Band.dag(s);
        ServerLevel thuis = s.getLevel(h.dim);
        for (UUID id : h.bewoners) {
            boolean guh = "guh".equals(h.soort(id));
            BandData.Rec r = band.vind(h.eigenaar, id);
            Entity e = guh ? Band.zoekGeladen(s, id) : thuis == null ? null : Huisjes.zoekBewoner(thuis, h, id);
            Component naam = e != null ? e.getName() : r != null ? r.weergave() : h.naamVan(id);
            PlekSoort plek = r == null ? PlekSoort.ONBEKEND : r.plek.soort();
            Component detail = Component.empty();
            Waar waar;
            if (r != null && r.dood) {
                waar = Waar.WOLKJES;
            } else if (e != null ? Huisjes.isBinnen(e) : plek == PlekSoort.SLAAPT_IN_HUISJE) {
                waar = Waar.SLAAPT;
            } else if (e instanceof Mob mob && !e.isPassenger() && !e.isVehicle() && Huisjes.thuisVan(e) == h) {
                // loaded and at home: what it is doing right now
                String bezig = HuisjeGoal.bezigMet(mob);
                String px = e instanceof GuhEntity g ? GuhKiezer.geclaimd(g) : "";
                if (!px.isEmpty()) {
                    waar = px.contains("parkour") ? Waar.PARKOUR : Waar.GUHPIXEL;
                } else if (bezig == null) {
                    waar = e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOrderedToSit() ? Waar.ZIT : Waar.BUITEN;
                } else if (bezig.isEmpty()) {
                    waar = Waar.SPEELT;
                } else {
                    waar = Waar.KLUS;
                    detail = Component.translatable("gui.guhs.klus." + bezig);
                }
            } else {
                waar = switch (plek) {
                    case WERELD, HUISJE, SLAAPT_IN_HUISJE -> Waar.BUITEN;
                    case ZIT -> Waar.ZIT;
                    case RIJDT_OP, RIJDT_MEE, ITEM_SPELER, ITEM_RUGZAK, SCHOUDER, BIJ_JOU -> Waar.MEE;
                    case OP_VAKANTIE -> Waar.VAKANTIE;
                    case OP_KANTOOR -> Waar.KANTOOR;
                    case GUHKAMER -> Waar.GUHKAMER;
                    case IN_DE_WOLKJES -> Waar.WOLKJES;
                    default -> Waar.WEG;
                };
                if (waar == Waar.VAKANTIE && r != null) {
                    detail = r.plek.detail();
                }
            }
            CompoundTag looks = e instanceof GuhEntity g ? Band.looks(g) : r != null ? r.looks.copy() : new CompoundTag();
            boolean baby = e instanceof Mob m ? m.isBaby() : looks.getBooleanOr("Baby", false);
            uit.add(new Slot(id, guh, naam, waar, detail, looks, baby, data.ingestopt.getOrDefault(id, -1L) == dag, lievelings(r)));
        }
        return uit;
    }

    /** What lies on the bedside table: a discovered favourite (the knuffel, else the snack), else a kaasknabbel. */
    private static ItemStack lievelings(@Nullable BandData.Rec r) {
        if (r != null) {
            for (FavorietSoort soort : List.of(FavorietSoort.KNUFFEL, FavorietSoort.ETEN)) {
                String waarde = r.fav.get(soort);
                if (waarde == null || !r.ontdekt.contains(soort)) {
                    continue;
                }
                Identifier id = soort == FavorietSoort.KNUFFEL ? Guhs.id("knuffel_" + waarde) : Identifier.tryParse(waarde);
                Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item != null) {
                    return new ItemStack(item);
                }
            }
        }
        return new ItemStack(ModItems.KAAS_KNABBELS.get());
    }

    private static String teken(List<Slot> slots) {
        StringBuilder b = new StringBuilder();
        for (Slot s : slots) {
            b.append(s.id()).append('|').append(s.waar()).append('|').append(s.detail().getString()).append('|').append(s.naam().getString())
                    .append('|').append(s.looks().hashCode()).append('|').append(s.ingestopt()).append('|')
                    .append(BuiltInRegistries.ITEM.getKey(s.lievelings().getItem())).append(';');
        }
        return b.toString();
    }

    /** What the room shows now (the last {@link #ververs}), or what it would show. */
    public static List<Slot> slots(MinecraftServer s, Huisje h) {
        Staat st = STATEN.get(h.cel);
        return st != null ? st.slots() : stand(s, h);
    }

    // =====================================================================================================================
    // building and removing
    // =====================================================================================================================

    /** Brings the room up to date: rebuilt when something changed since the last look (or always with {@code altijd}). */
    static void ververs(ServerLevel level, Huisje h, BinnenKamer k, boolean altijd) {
        if (!level.isPositionEntityTicking(BlockPos.containing(Binnen.mat(h, k)))) {
            return;   // (the room's chunk is not ready for entities yet, a tick after somebody arrived: Binnen.tick asks again)
        }
        List<Slot> slots = stand(level.getServer(), h);
        String teken = teken(slots);
        Staat oud = STATEN.get(h.cel);
        // unchanged, and everything that was put there still stands (whatever got lost is simply made again)
        if (!altijd && oud != null && oud.teken().equals(teken) && dingen(level, h).size() == oud.aantal()) {
            return;
        }
        gezet = 0;
        bouw(level, h, k, slots);
        STATEN.put(h.cel, new Staat(teken, slots, gezet));
    }

    /** Was this cell's room furnished since somebody came in? */
    static boolean gebouwd(int cel) {
        return STATEN.containsKey(cel);
    }

    private static void bouw(ServerLevel level, Huisje h, BinnenKamer k, List<Slot> slots) {
        BlockPos o = Binnen.oorsprong(h.cel);
        ruim(level, o);
        for (int i = 0; i < k.bedden().size(); i++) {
            BinnenKamer.Bed b = k.bedden().get(i);
            Slot s = i < slots.size() ? slots.get(i) : null;
            if (level.getBlockEntity(o.offset(b.bord())) instanceof SignBlockEntity bord) {
                SignText tekst = new SignText();
                tekst = s == null ? tekst.setMessage(1, Component.translatable("gui.guhs.huisje.binnen.bordje.vrij").withStyle(ChatFormatting.GRAY))
                        : tekst.setMessage(1, kort(s.naam()));
                bord.setText(tekst, true);
                bord.setWaxed(true);
            }
            if (s == null) {
                continue;
            }
            float yaw = b.kijk().toYRot();
            Vec3 bed = Vec3.atBottomCenterOf(o.offset(b.bed())).add(0, BED_HOOGTE, 0);
            Vec3 kastje = Vec3.atBottomCenterOf(o.offset(b.kastje())).add(0, 0.5, 0);
            item(level, s.lievelings(), kastje, yaw, "ground", 0.8f);
            if (s.waar() == Waar.SLAAPT) {
                if (s.guh()) {
                    slaper(level, s, bed, yaw);
                    kleren(level, s, o.offset(b.haak()), b.kijk());
                }
                dek(level, bed, yaw, i, s.ingestopt() || !s.guh());
            } else {
                dek(level, bed, yaw, i, false);
                briefje(level, bed.add(0, 0.42, 0), Component.translatable("gui.guhs.huisje.binnen.briefje." + s.waar().name().toLowerCase(java.util.Locale.ROOT),
                        s.detail()));
            }
        }
    }

    /** A name for a sign line (about 15 characters fit). */
    private static Component kort(Component naam) {
        String tekst = naam.getString();
        return tekst.length() <= 15 ? naam : Component.literal(tekst.substring(0, 14) + "…");
    }

    private static void slaper(ServerLevel level, Slot s, Vec3 bed, float yaw) {
        BinnenGuh g = HuisjeFeature.SLAPER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (g == null) {
            return;
        }
        CompoundTag looks = s.looks().copy();
        looks.remove("Clothes");   // (asleep: its clothes hang on the hook)
        g.looks(looks);
        g.setGuhScale(Math.max(0.5f, Math.min(1.1f, looks.getFloatOr("Scale", 1f))));   // (it has to fit its little bed)
        if (s.baby()) {
            g.setBaby(true);
        }
        g.bewoner(s.id());
        g.setCustomName(s.naam());
        g.setCustomNameVisible(false);
        g.setSilent(true);
        g.snapTo(bed.x, bed.y, bed.z, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.slaap(true);
        g.addTag(Binnen.TAG);
        if (level.addFreshEntity(g)) {
            gezet++;
        }
    }

    /** At most three pieces of clothing on the hook beside the bed. */
    private static void kleren(ServerLevel level, Slot s, BlockPos haak, Direction kijk) {
        CompoundTag clothes = s.looks().getCompoundOrEmpty("Clothes");
        Vec3 midden = Vec3.atCenterOf(haak).add(-kijk.getStepX() * 0.32, -0.2, -kijk.getStepZ() * 0.32);
        Direction zij = kijk.getClockWise();
        int n = 0;
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(clothes.getStringOr(slot.name(), ""));
            if (c == null || n >= 3) {
                continue;
            }
            double opzij = (n == 0 ? 0 : n == 1 ? -0.24 : 0.24);
            item(level, new ItemStack(ModItems.clothingItem(c)), midden.add(zij.getStepX() * opzij, n == 0 ? 0 : -0.08, zij.getStepZ() * opzij),
                    kijk.toYRot(), "fixed", 0.3f);
            n++;
        }
    }

    /** The blanket of bed i: folded at the foot end, or pulled up over the sleeper. */
    private static void dek(ServerLevel level, Vec3 bed, float yaw, int i, boolean op) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        CompoundTag blok = new CompoundTag();
        blok.putString("Name", "minecraft:pink_carpet");
        tag.put("block_state", blok);
        CompoundTag tf = new CompoundTag();
        tf.put("translation", op ? floats(-0.52f, 0f, -0.54f) : floats(-0.42f, 0f, 0.08f));
        tf.put("scale", op ? floats(1.04f, 8.5f, 0.8f) : floats(0.84f, 1.6f, 0.4f));
        tf.put("left_rotation", floats(0f, 0f, 0f, 1f));
        tf.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", tf);
        tag.put("Tags", tags(Binnen.TAG, DEK + i));
        spawn(level, tag, bed, yaw);
    }

    private static void briefje(ServerLevel level, Vec3 pos, Component tekst) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:text_display");
        tag.put("text", ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                tekst.copy().withStyle(ChatFormatting.DARK_PURPLE)).getOrThrow());
        tag.putString("billboard", "center");
        tag.putString("alignment", "center");
        tag.putInt("line_width", 110);
        tag.putInt("background", 0xF0FFF6E0);
        CompoundTag tf = new CompoundTag();
        tf.put("translation", floats(0f, 0f, 0f));
        tf.put("scale", floats(0.45f, 0.45f, 0.45f));
        tf.put("left_rotation", floats(0f, 0f, 0f, 1f));
        tf.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", tf);
        tag.put("Tags", tags(Binnen.TAG));
        spawn(level, tag, pos, 0f);
    }

    private static void item(ServerLevel level, ItemStack stack, Vec3 pos, float yaw, String houding, float schaal) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_display");
        tag.put("item", Nbt.saveStack(level.registryAccess(), stack));
        tag.putString("item_display", houding);
        CompoundTag tf = new CompoundTag();
        tf.put("translation", floats(0f, "ground".equals(houding) ? 0.12f : 0f, 0f));
        tf.put("scale", floats(schaal, schaal, schaal));
        tf.put("left_rotation", floats(0f, 0f, 0f, 1f));
        tf.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", tf);
        tag.put("Tags", tags(Binnen.TAG));
        spawn(level, tag, pos, yaw);
    }

    private static void spawn(ServerLevel level, CompoundTag tag, Vec3 pos, float yaw) {
        Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, x -> {
            x.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            return x;
        });
        if (e != null && level.addFreshEntity(e)) {
            gezet++;
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    private static ListTag tags(String... v) {
        ListTag l = new ListTag();
        for (String s : v) {
            l.add(StringTag.valueOf(s));
        }
        return l;
    }

    /** (The zelftest) one blanket, one note and one thing on a table at this spot; returns how many of them stand there. */
    static int proef(ServerLevel level, Vec3 pos) {
        dek(level, pos, 0f, 0, true);
        briefje(level, pos.add(0, 0.5, 0), Component.translatable("gui.guhs.huisje.binnen.briefje.buiten"));
        item(level, new ItemStack(ModItems.KAAS_KNABBELS.get()), pos.add(1, 0, 0), 0f, "ground", 1.1f);
        return level.getEntities((Entity) null, new AABB(pos, pos).inflate(3), e -> e.entityTags().contains(Binnen.TAG)).size();
    }

    /** Removes everything this class put in the room of this huisje. */
    static void ruim(ServerLevel level, Huisje h) {
        if (h.cel >= 0) {
            ruim(level, Binnen.oorsprong(h.cel));
        }
    }

    /** Removes everything this class put in the room at this min corner; returns how many things. */
    static int ruim(ServerLevel level, BlockPos oorsprong) {
        AABB doos = Stempel.doos(oorsprong, BinnenKamer.MAX).inflate(1);
        List<Entity> weg = level.getEntities((Entity) null, doos, e -> e.entityTags().contains(Binnen.TAG));
        for (Entity e : weg) {
            e.discard();
        }
        return weg.size();
    }

    static void vergeet(int cel) {
        STATEN.remove(cel);
    }

    static void vergeetAlles() {
        STATEN.clear();
        AAI_RUST.clear();
    }

    /** What stands in this huisje's room right now (tests, the zelftest): everything with the room tag. */
    public static List<Entity> dingen(ServerLevel level, Huisje h) {
        return h.cel < 0 ? List.of() : level.getEntities((Entity) null, Stempel.doos(Binnen.oorsprong(h.cel), BinnenKamer.MAX).inflate(1),
                e -> e.entityTags().contains(Binnen.TAG));
    }

    /** While somebody is inside: soft snoring now and then, a zzz above a sleeping maatje. */
    static void tick(ServerLevel level, Huisje h, BinnenKamer k, int nu) {
        Staat st = STATEN.get(h.cel);
        if (st == null || (nu + h.cel * 7) % 50 != 0) {
            return;
        }
        BlockPos o = Binnen.oorsprong(h.cel);
        List<Vec3> slapers = new ArrayList<>();
        for (int i = 0; i < Math.min(st.slots().size(), k.bedden().size()); i++) {
            Slot s = st.slots().get(i);
            if (s.waar() != Waar.SLAAPT) {
                continue;
            }
            Vec3 bed = Vec3.atBottomCenterOf(o.offset(k.bedden().get(i).bed())).add(0, BED_HOOGTE, 0);
            slapers.add(bed);
            if (!s.guh()) {
                level.sendParticles(EmotesFeature.GUH_ZZZ.get(), bed.x, bed.y + 0.55, bed.z, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
        if (!slapers.isEmpty() && (nu / 50 + h.cel) % 4 == 0) {
            Vec3 wie = slapers.get(level.getRandom().nextInt(slapers.size()));
            level.playSound(null, BlockPos.containing(wie), HuisjeFeature.SNURK_GELUID.get(), SoundSource.NEUTRAL, 0.35f,
                    0.9f + level.getRandom().nextFloat() * 0.3f);
        }
    }

    // =====================================================================================================================
    // what the owner can do
    // =====================================================================================================================

    private static boolean eigenaar(ServerPlayer p, Huisje h) {
        if (h.eigenaar.equals(p.getUUID())) {
            return true;
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.van_wie", h.eigenaarNaam.isEmpty() ? "?" : h.eigenaarNaam)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return false;
    }

    /** A click on a bed, its sign, its bedside table or its hook: the owner sees that resident's dagboekje. */
    static void bedKlik(ServerPlayer p, Huisje h, int bed) {
        if (!eigenaar(p, h)) {
            return;
        }
        List<Slot> slots = slots(p.level().getServer(), h);
        if (bed >= slots.size()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.leeg_bed").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Slot s = slots.get(bed);
        if (!s.guh()) {
            p.sendOverlayMessage(Component.translatable(s.waar() == Waar.SLAAPT ? "gui.guhs.huisje.binnen.maatje" : "gui.guhs.huisje.binnen.maatje_weg",
                    s.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        dagboek(p, s.id());
    }

    /** The Guhdex opens on this guh's page of "Mijn guhs" (hearts, favourites, dagboekje). */
    static void dagboek(ServerPlayer p, UUID id) {
        MijnGuhs.stuur(p, id);
        GuhDex.open(p);
    }

    /** A click on a sleeping stand-in: the owner tucks it in (once per night), after that pets it softly. */
    static void guhKlik(ServerPlayer p, BinnenGuh g) {
        Huisje h = Binnen.binnenIn(p);
        UUID id = g.bewoner();
        if (h == null || id == null || !h.bewoners.contains(id) || !eigenaar(p, h)) {
            return;
        }
        MinecraftServer s = p.level().getServer();
        ServerLevel level = (ServerLevel) g.level();
        Huisjes data = Huisjes.get(s);
        long dag = Band.dag(s);
        if (data.ingestopt.getOrDefault(id, -1L) != dag) {
            data.ingestopt.put(id, dag);
            data.setDirty();
            hartjes(s, h, id, Reden.KNUFFELEN);
            // the blanket goes up (only this bed changes: the sleeper stays as it is)
            int bed = h.bewoners.indexOf(id);
            for (Entity e : dingen(level, h)) {
                if (e.entityTags().contains(DEK + bed)) {
                    e.discard();
                }
            }
            dek(level, g.position(), g.getYRot(), bed, true);
            Staat oud = STATEN.remove(h.cel);
            List<Slot> slots = stand(s, h);
            STATEN.put(h.cel, new Staat(teken(slots), slots, oud == null ? dingen(level, h).size() : oud.aantal()));
            level.sendParticles(BandFeature.GROOT_HARTJE.get(), g.getX(), g.getY() + 0.9, g.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, g.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.6f, 1.3f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.instoppen", g.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int nu = s.getTickCount();
        Integer vorige = AAI_RUST.get(p.getUUID());
        if (vorige != null && nu >= vorige && nu - vorige < 30) {
            return;
        }
        AAI_RUST.put(p.getUUID(), nu);
        hartjes(s, h, id, Reden.AAIEN);
        level.sendParticles(BandFeature.HARTJE.get(), g.getX(), g.getY() + 0.85, g.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.huisje.binnen.aaien", g.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Hearts for the REAL guh: now when it is loaded, else kept until it is ({@link #teGoed}). */
    private static void hartjes(MinecraftServer s, Huisje h, UUID id, Reden reden) {
        Mob echt = Band.zoekGeladen(s, id);
        if (echt != null && Band.isBandGuh(echt)) {
            Band.geefHartjes(echt, s.getPlayerList().getPlayer(h.eigenaar), reden.standaard(), reden);
            return;
        }
        Huisjes data = Huisjes.get(s);
        int[] n = data.teGoed.computeIfAbsent(id, k -> new int[2]);
        if (reden == Reden.KNUFFELEN) {
            n[0] = Math.min(3, n[0] + 1);
        } else {
            n[1] = Math.min(Reden.AAIEN.dagMax(), n[1] + 1);
        }
        data.setDirty();
    }

    /** A real guh is loaded: it gets the hearts it was given in its room while it was away. */
    static void teGoed(GuhEntity guh) {
        MinecraftServer s = guh.level().getServer();
        if (s == null || !Band.isBandGuh(guh)) {
            return;
        }
        Huisjes data = Huisjes.get(s);
        if (data.teGoed.isEmpty()) {
            return;
        }
        int[] n = data.teGoed.remove(Band.id(guh));
        if (n == null) {
            return;
        }
        data.setDirty();
        ServerPlayer eigenaar = Band.eigenaarOnline(guh);
        if (n[0] > 0) {
            Band.geefHartjes(guh, eigenaar, n[0] * Reden.KNUFFELEN.standaard(), Reden.KNUFFELEN);
        }
        if (n[1] > 0) {
            Band.geefHartjes(guh, eigenaar, n[1] * Reden.AAIEN.standaard(), Reden.AAIEN);
        }
    }

    /** The prikbord: who does which chore (the owner reads it in the chat). */
    static void prikbord(ServerPlayer p, Huisje h) {
        if (!eigenaar(p, h)) {
            return;
        }
        MinecraftServer s = p.level().getServer();
        if (h.bewoners.isEmpty()) {
            p.sendSystemMessage(Component.translatable("gui.guhs.huisje.binnen.prikbord.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.huisje.binnen.prikbord.kop", h.naamTekst()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        ServerLevel thuis = s.getLevel(h.dim);
        for (Slot slot : stand(s, h)) {
            Entity e = slot.guh() ? Band.zoekGeladen(s, slot.id()) : thuis == null ? null : Huisjes.zoekBewoner(thuis, h, slot.id());
            MutableComponent klussen = Component.empty();
            int n = 0;
            if (!slot.baby()) {
                for (Klus k : Klusjes.alle()) {
                    boolean kan;
                    try {
                        kan = e instanceof Mob m ? k.kan(m) && h.klusAan(m, k.id()) : slot.guh() && h.klusAan(slot.id(), k.id());
                    } catch (RuntimeException ex) {
                        kan = false;
                    }
                    if (kan) {
                        klussen.append(n++ == 0 ? Component.empty() : Component.literal(", ")).append(Component.translatable("gui.guhs.klus." + k.id()));
                    }
                }
            }
            Component wat = slot.baby() ? Component.translatable("gui.guhs.huisje.binnen.prikbord.baby")
                    : n == 0 ? Component.translatable("gui.guhs.huisje.binnen.prikbord.geen") : klussen;
            p.sendSystemMessage(Component.translatable("gui.guhs.huisje.binnen.prikbord.regel", slot.naam().copy().withStyle(ChatFormatting.GOLD),
                    wat.copy().withStyle(ChatFormatting.WHITE)));
        }
    }
}
