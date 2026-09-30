package nl.juiced.guhs.feature.piep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.LootTable;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The fight of the kaasknabbel-nest. Step onto the golden kern in the middle of the arena: three waves of boze
 * kaasknabbels come out of the holes in the wall ({@link #GOLVEN}), then the Boze Oppernabbel. Beat him: "Hij was gewoon
 * zieli..." and a treasure chest appears on the kern (the first time with the roze_guh_koek_recept: loot table
 * guhs:chests/kaasknabbel_nest_schat; later ones only knabbels: guhs:chests/kaasknabbel_nest). After {@link #HERSTEL} ticks
 * the nest can be fought again (for knabbels). Everyone gone or dead: the fight stops and the knabbels hop back in.
 * <p>
 * A nest is found by its structure piece ({@link PiepFeature#KAASKNABBEL_NEST}); its key is the start's chunk. The kern is
 * the centre of the piece at floor height {@link #VLOER} (tools/features/piep_nest.py: G, C, the spawn holes). State per
 * nest in the SavedData {@code guhs_piep_nesten} of the level.
 */
public final class KaasknabbelNest {
    /** The knabbels per wave (plus one per extra player, at most two more). */
    public static final int[] GOLVEN = {4, 6, 8};
    /** Before the next wave, and how long after a win the nest is back. */
    public static final int PAUZE = 60, HERSTEL = 24000 * 3;
    /** The arena floor above the template's bottom, the kern radius, the spawn holes (template numbers, see piep_nest.py). */
    public static final int VLOER = 4, SPAWN_R = 9;
    public static final double KERN_R = 3.2;
    public static final int[] SPAWN_HOEKEN = {0, 60, 120, 180, 240, 300};
    /** Nobody within this many blocks of the kern for {@link #OPGEVEN} ticks: the fight stops. */
    public static final int WEG_AFSTAND = 40, OPGEVEN = 20 * 20;
    public static final ResourceKey<LootTable> SCHAT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            Guhs.id("chests/kaasknabbel_nest_schat"));
    public static final ResourceKey<LootTable> KNABBELS = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
            Guhs.id("chests/kaasknabbel_nest"));

    public enum Fase {
        RUST, GOLF, PAUZE, BAAS, GEWONNEN
    }

    /** One nest's fight. */
    public static final class Gevecht {
        final String key;
        BlockPos kern;
        Fase fase = Fase.RUST;
        int golf;
        long faseTot, gewonnenOp = -1, leegSinds = -1;
        boolean receptGegeven;
        final Set<UUID> mobs = new HashSet<>();
        final Set<UUID> spelers = new HashSet<>();

        Gevecht(String key, BlockPos kern) {
            this.key = key;
            this.kern = kern;
        }

        public Fase fase() {
            return fase;
        }

        public String key() {
            return key;
        }

        public int golf() {
            return golf;
        }

        public BlockPos kern() {
            return kern;
        }

        public boolean receptGegeven() {
            return receptGegeven;
        }

        public int levend(ServerLevel level) {
            int n = 0;
            for (UUID id : mobs) {
                Entity e = level.getEntity(id);
                if (e != null && e.isAlive()) {
                    n++;
                }
            }
            return n;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("Key", key);
            t.put("Kern", NbtUtils.writeBlockPos(kern));
            t.putLong("GewonnenOp", gewonnenOp);
            t.putBoolean("Recept", receptGegeven);
            return t;
        }

        static Gevecht load(CompoundTag t) {
            Gevecht g = new Gevecht(t.getString("Key"), NbtUtils.readBlockPos(t, "Kern").orElse(BlockPos.ZERO));
            g.gewonnenOp = t.getLong("GewonnenOp");
            g.receptGegeven = t.getBoolean("Recept");
            g.fase = g.gewonnenOp >= 0 ? Fase.GEWONNEN : Fase.RUST;   // (a fight going on when the server stopped starts over)
            return g;
        }
    }

    /** All nests of a level (saved: only when they were won and whether the recipe came out). */
    public static final class Nesten extends SavedData {
        final Map<String, Gevecht> nesten = new HashMap<>();

        static Nesten get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Nesten::new, Nesten::load, null), "guhs_piep_nesten");
        }

        static Nesten load(CompoundTag tag, HolderLookup.Provider registries) {
            Nesten n = new Nesten();
            for (Tag t : tag.getList("Nesten", Tag.TAG_COMPOUND)) {
                Gevecht g = Gevecht.load((CompoundTag) t);
                n.nesten.put(g.key, g);
            }
            return n;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Gevecht g : nesten.values()) {
                list.add(g.save());
            }
            tag.put("Nesten", list);
            return tag;
        }
    }

    /** The nest a spot is in: its fight (made the first time), or null. */
    @Nullable
    public static Gevecht nestBij(ServerLevel level, BlockPos pos) {
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structure = registry.get(PiepFeature.KAASKNABBEL_NEST);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        if (!start.isValid() || start.getPieces().isEmpty()) {
            return null;
        }
        BoundingBox box = start.getPieces().get(0).getBoundingBox();
        String key = start.getChunkPos().x + "," + start.getChunkPos().z;
        BlockPos kern = new BlockPos((box.minX() + box.maxX() + 1) / 2, box.minY() + VLOER, (box.minZ() + box.maxZ() + 1) / 2);
        return gevecht(level, key, kern);
    }

    /** The fight of a nest (by key), made with this kern the first time (tests make their own nests this way). */
    public static Gevecht gevecht(ServerLevel level, String key, BlockPos kern) {
        Nesten n = Nesten.get(level);
        Gevecht g = n.nesten.get(key);
        if (g == null) {
            g = new Gevecht(key, kern.immutable());
            n.nesten.put(key, g);
            n.setDirty();
        }
        return g;
    }

    /** Is the player standing on the kern? */
    public static boolean opKern(Gevecht g, ServerPlayer player) {
        double dx = player.getX() - (g.kern.getX() + 0.5), dz = player.getZ() - (g.kern.getZ() + 0.5);
        return dx * dx + dz * dz <= KERN_R * KERN_R && player.getY() >= g.kern.getY() && player.getY() <= g.kern.getY() + 3;
    }

    /** (Every second, per player in the Guhmensie) a player in a nest: start the fight when they step on the kern. */
    static void spelerTick(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Gevecht g = nestBij(level, player.blockPosition());
        if (g == null) {
            return;
        }
        GuhAdvancements.grant(player, "piep_nest_gevonden");
        if (!opKern(g, player)) {
            return;
        }
        if (g.fase == Fase.GEWONNEN && level.getGameTime() - g.gewonnenOp >= HERSTEL) {
            g.fase = Fase.RUST;
        }
        if (g.fase == Fase.RUST) {
            start(level, g);
        }
    }

    /** The fight starts (the first wave comes). */
    public static void start(ServerLevel level, Gevecht g) {
        g.fase = Fase.GOLF;
        g.golf = 0;
        g.mobs.clear();
        g.spelers.clear();
        g.leegSinds = -1;
        GEVECHTEN.put(level.dimension().location() + "|" + g.key, new Actief(level, g));
        verwijderKist(level, g);
        zeg(level, g, Component.translatable("gui.guhs.piep.nest_start").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        level.playSound(null, g.kern, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8f, 1.6f);
        golf(level, g);
    }

    private static void golf(ServerLevel level, Gevecht g) {
        int spelers = Math.max(1, spelersBij(level, g).size());
        int n = GOLVEN[g.golf] + Math.min(2, spelers - 1);
        zeg(level, g, Component.translatable("gui.guhs.piep.nest_golf", g.golf + 1, GOLVEN.length).withStyle(ChatFormatting.YELLOW));
        for (int i = 0; i < n; i++) {
            int hoek = SPAWN_HOEKEN[(i + g.golf) % SPAWN_HOEKEN.length];
            BlockPos at = spawnPlek(g, hoek);
            BozeKaasknabbelEntity k = PiepFeature.BOZE_KAASKNABBEL.get().create(level);
            if (k == null) {
                continue;
            }
            k.moveTo(at.getX() + 0.5 + (i / 6) * 0.3, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            k.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            k.setNest(g.key);
            level.addFreshEntity(k);
            g.mobs.add(k.getUUID());
            level.sendParticles(ParticleTypes.POOF, k.getX(), k.getY() + 0.3, k.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    public static BlockPos spawnPlek(Gevecht g, int hoek) {
        double r = Math.toRadians(hoek);
        return g.kern.offset((int) Math.round(Math.cos(r) * SPAWN_R), 1, (int) Math.round(Math.sin(r) * SPAWN_R));
    }

    private static void baas(ServerLevel level, Gevecht g) {
        g.fase = Fase.BAAS;
        BozeOppernabbelEntity baas = PiepFeature.BOZE_OPPERNABBEL.get().create(level);
        if (baas == null) {
            return;
        }
        BlockPos at = spawnPlek(g, 180).relative(net.minecraft.core.Direction.EAST, 2);
        baas.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        baas.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        baas.setNest(g.key);
        level.addFreshEntity(baas);
        g.mobs.add(baas.getUUID());
        zeg(level, g, Component.translatable("gui.guhs.piep.nest_baas").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        level.playSound(null, at, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8f, 1.8f);
    }

    /** (The Oppernabbel at half health) a few little knabbels come to help. */
    static void roepHulp(ServerLevel level, BozeOppernabbelEntity baas, int n) {
        Gevecht g = actief(level, baas.nest());
        for (int i = 0; i < n; i++) {
            BozeKaasknabbelEntity k = PiepFeature.BOZE_KAASKNABBEL.get().create(level);
            if (k == null) {
                continue;
            }
            BlockPos at = g != null ? spawnPlek(g, SPAWN_HOEKEN[i * 2 % SPAWN_HOEKEN.length]) : baas.blockPosition();
            k.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            k.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            k.setNest(baas.nest());
            level.addFreshEntity(k);
            if (g != null) {
                g.mobs.add(k.getUUID());
            }
        }
    }

    /** (The Oppernabbel died) the nest is won: the treasure chest on the kern. */
    static void bossVerslagen(ServerLevel level, BozeOppernabbelEntity baas) {
        Gevecht g = actief(level, baas.nest());
        if (g == null || g.fase != Fase.BAAS) {
            return;
        }
        gewonnen(level, g);
    }

    public static void gewonnen(ServerLevel level, Gevecht g) {
        g.fase = Fase.GEWONNEN;
        g.gewonnenOp = level.getGameTime();
        boolean eersteKeer = !g.receptGegeven;
        g.receptGegeven = true;
        Nesten.get(level).setDirty();
        // the knabbels that are left go zieli too
        for (UUID id : g.mobs) {
            Entity e = level.getEntity(id);
            if (e instanceof BozeKaasknabbelEntity k && k.isAlive()) {
                BozeKaasknabbelEntity.zieli(level, k, Component.translatable("gui.guhs.piep.zieli"), 0.8);
                k.discard();
            }
        }
        g.mobs.clear();
        BlockPos kist = g.kern.above();
        level.setBlock(kist, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        if (level.getBlockEntity(kist) instanceof ChestBlockEntity chest) {
            chest.setLootTable(eersteKeer ? SCHAT : KNABBELS, level.random.nextLong());
            chest.getPersistentData().putBoolean("guhs_piep_schat", true);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, kist.getX() + 0.5, kist.getY() + 0.8, kist.getZ() + 0.5, 20, 0.6, 0.4, 0.6, 0);
        level.playSound(null, kist, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
        for (ServerPlayer p : spelersBij(level, g)) {
            p.displayClientMessage(Component.translatable("gui.guhs.piep.gewoon_zieli").withStyle(ChatFormatting.GOLD), true);
            PiepVoortgang.tel(p, PiepVoortgang.NEST, 1, "piep_nest_gewonnen");
        }
        for (UUID id : g.spelers) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null && !spelersBij(level, g).contains(p)) {
                PiepVoortgang.tel(p, PiepVoortgang.NEST, 1, "piep_nest_gewonnen");
            }
        }
        GEVECHTEN.remove(level.dimension().location() + "|" + g.key);
    }

    /** Stops a fight (everyone gone): the knabbels hop back into their holes. */
    public static void stop(ServerLevel level, Gevecht g) {
        for (UUID id : g.mobs) {
            Entity e = level.getEntity(id);
            if (e != null) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.3, e.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                e.discard();
            }
        }
        g.mobs.clear();
        g.fase = g.gewonnenOp >= 0 ? Fase.GEWONNEN : Fase.RUST;
        if (g.fase == Fase.GEWONNEN) {
            g.gewonnenOp = Math.min(g.gewonnenOp, level.getGameTime() - HERSTEL);   // (a lost rematch may be tried again)
        }
        GEVECHTEN.remove(level.dimension().location() + "|" + g.key);
    }

    private static void verwijderKist(ServerLevel level, Gevecht g) {
        BlockPos kist = g.kern.above();
        if (level.getBlockEntity(kist) instanceof ChestBlockEntity chest && chest.getPersistentData().getBoolean("guhs_piep_schat")) {
            level.removeBlockEntity(kist);                     // (an old treasure chest: whatever is left in it goes back to the nest)
            level.setBlock(kist, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    static List<ServerPlayer> spelersBij(ServerLevel level, Gevecht g) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.isAlive() && p.distanceToSqr(g.kern.getCenter()) <= WEG_AFSTAND * WEG_AFSTAND) {
                out.add(p);
            }
        }
        return out;
    }

    private static void zeg(ServerLevel level, Gevecht g, Component text) {
        for (ServerPlayer p : spelersBij(level, g)) {
            p.sendSystemMessage(text);
        }
    }

    // --- the fights going on (ticked by PiepEvents) --------------------------------------------------------------------------

    record Actief(ServerLevel level, Gevecht gevecht) {
    }

    static final Map<String, Actief> GEVECHTEN = new java.util.concurrent.ConcurrentHashMap<>();

    @Nullable
    static Gevecht actief(ServerLevel level, String key) {
        Actief a = GEVECHTEN.get(level.dimension().location() + "|" + key);
        return a == null ? null : a.gevecht();
    }

    /** Every tick: waves, pauses, giving up. */
    static void tick() {
        for (Actief a : List.copyOf(GEVECHTEN.values())) {
            ServerLevel level = a.level();
            Gevecht g = a.gevecht();
            long now = level.getGameTime();
            if (now % 10 != 0) {
                continue;
            }
            List<ServerPlayer> spelers = spelersBij(level, g);
            spelers.forEach(p -> g.spelers.add(p.getUUID()));
            if (spelers.isEmpty()) {
                if (g.leegSinds < 0) {
                    g.leegSinds = now;
                } else if (now - g.leegSinds >= OPGEVEN) {
                    stop(level, g);
                    continue;
                }
            } else {
                g.leegSinds = -1;
            }
            if (!level.isLoaded(g.kern)) {
                continue;
            }
            switch (g.fase) {
                case GOLF -> {
                    g.mobs.removeIf(id -> {
                        Entity e = level.getEntity(id);
                        return e == null || !e.isAlive();
                    });
                    if (g.mobs.isEmpty()) {
                        g.golf++;
                        g.fase = Fase.PAUZE;
                        g.faseTot = now + PAUZE;
                        if (g.golf < GOLVEN.length) {
                            zeg(level, g, Component.translatable("gui.guhs.piep.nest_golf_klaar").withStyle(ChatFormatting.GREEN));
                        }
                    }
                }
                case PAUZE -> {
                    if (now >= g.faseTot) {
                        if (g.golf < GOLVEN.length) {
                            g.fase = Fase.GOLF;
                            golf(level, g);
                        } else {
                            baas(level, g);
                        }
                    }
                }
                case BAAS -> {
                    boolean baasLeeft = g.mobs.stream().map(level::getEntity).anyMatch(e -> e instanceof BozeOppernabbelEntity && e.isAlive());
                    if (!baasLeeft && g.mobs.stream().map(level::getEntity).noneMatch(e -> e instanceof BozeOppernabbelEntity)) {
                        // the boss is gone without dying (unloaded? /kill?): count it as beaten if it died, else start over
                        stop(level, g);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** (Server stopping, tests) forget the fights going on. */
    static void vergeet() {
        GEVECHTEN.clear();
    }

    /** Does this knabbel belong to a fight that is still going on? (Knabbels of a finished fight are removed when they load.) */
    static boolean hoortBijGevecht(ServerLevel level, BozeKaasknabbelEntity k) {
        return actief(level, k.nest()) != null;
    }

    private KaasknabbelNest() {
    }

    static ListTag strings(Set<UUID> ids) {
        ListTag l = new ListTag();
        ids.forEach(id -> l.add(StringTag.valueOf(id.toString())));
        return l;
    }
}
