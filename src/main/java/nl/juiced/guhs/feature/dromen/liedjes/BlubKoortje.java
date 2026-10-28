package nl.juiced.guhs.feature.dromen.liedjes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubKleding;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DromenFeature;

/**
 * 1.8.0 Guhdromenland, the Slaapliedjesheuvels (BOUWPLAN_180.md part E, island "liedjes"; design: the Blub-koortje). Five
 * shy wild Blubs live on the island, each knows one note of Snoes' slaapliedje (do re mi fa sol; its strikje has the note's
 * colour, and it peeps its own note). Sneak up to one and it follows you; bring every Blub to its own klokjesheuvel (its ears
 * go up when it stands right). When all five stand on their hills the choir sings the whole lullaby, and everyone who helped
 * (and everyone on the island) gets the piece "het liedje" for Snoes ({@link DroomVoortgang.Stukje#LIEDJE}), as long as
 * {@link #VERHAAL_LOOPT} says Snoes' story is running (the story agent sets it; by default it always runs). Then the Blubs
 * are tired, hop off and are shy again a minute later.
 * <p>
 * The island's podium spots come from {@code data/guhs/dromen/eiland/liedjes.json} ("koor", relative to the island's
 * centre; written by tools/features/eiland_liedjes.py). The choir logic itself is a plain {@link Koor} over any five spots,
 * so the game test runs it in a test garden.
 */
public final class BlubKoortje {
    private static final Logger LOG = LogUtils.getLogger();

    /** The notes, low to high, and the entity tag a choir Blub carries ({@code guhs.koor.<noot>}). */
    public static final List<String> NOTEN = List.of("do", "re", "mi", "fa", "sol");
    public static final String TAG = "guhs.koor.";
    /** The strikje colour of each note (the podium colours of the island: roze, boter, aqua, lavendel, hemelsblauw). */
    static final DyeColor[] KLEUR = {DyeColor.PINK, DyeColor.YELLOW, DyeColor.LIGHT_BLUE, DyeColor.PURPLE, DyeColor.BLUE};
    /** Semitones above do (a major scale). */
    static final int[] HALVE_TONEN = {0, 2, 4, 5, 7};
    /** Is Snoes' story running for this player (so the koortje gives the piece)? The story agent may narrow it. */
    public static Predicate<ServerPlayer> VERHAAL_LOOPT = p -> true;

    /** How close a Blub must stand to a podium (horizontal / vertical), how close you sneak, how far it keeps following. */
    static final double OP_HEUVEL = 2.6, OP_HEUVEL_Y = 2.5, SLUIP = 3.0, SCHUW = 4.5, LOS = 24.0;
    /** After singing: the Blubs rest this long before they are shy again. */
    static final int RUST = 20 * 60;
    /** The island choir is checked every this many ticks. */
    static final int ELKE = 5;

    @Nullable
    private static Koor eiland;

    private BlubKoortje() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel level = e.getServer().getLevel(DromenFeature.DIM);
            if (level == null || level.players().isEmpty() || level.getGameTime() % ELKE != 0) {
                return;
            }
            Koor k = eilandKoor();
            if (k != null) {
                k.zorgVoorBlubs(level);
                k.tick(level);
            }
        });
        // a choir Blub sings first; it can't be tamed (it would walk off with you)
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getTarget() instanceof BlubGuhEntity b && noot(b) != null && !b.isTame() && b.isVoer(e.getItemStack())) {
                if (e.getEntity() instanceof ServerPlayer p) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.koortje.niet_temmen", b.getDisplayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                e.setCancellationResult(InteractionResult.SUCCESS);
                e.setCanceled(true);
            }
        });
    }

    /** The note of a choir Blub (null: it isn't one). */
    @Nullable
    public static String noot(BlubGuhEntity b) {
        for (String t : b.entityTags()) {
            if (t.startsWith(TAG)) {
                return t.substring(TAG.length());
            }
        }
        return null;
    }

    /** Makes a Blub the choir Blub of this note: its tag and the note's strikje. */
    public static void maakKoorBlub(BlubGuhEntity b, String noot) {
        b.addTag(TAG + noot);
        b.setPersistenceRequired();
        BlubKleding.Stuk strik = BlubKleding.stuk("strik_" + KLEUR[NOTEN.indexOf(noot)].getName());
        if (strik != null) {
            b.kleedAan(BlubKleding.Plek.STRIK, strik);
        }
    }

    /** The pitch of a note (a bell, around a lullaby's soft middle). */
    static float toon(String noot) {
        return 0.75f * (float) Math.pow(2, HALVE_TONEN[NOTEN.indexOf(noot)] / 12.0);
    }

    /** The island's choir (its spots from liedjes.json); null when the json has none. */
    @Nullable
    static synchronized Koor eilandKoor() {
        if (eiland == null) {
            Map<String, Vec3> heuvels = new LinkedHashMap<>();
            try (InputStream in = BlubKoortje.class.getResourceAsStream("/data/guhs/dromen/eiland/liedjes.json")) {
                if (in == null) {
                    return null;
                }
                JsonObject koor = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("koor");
                for (String n : NOTEN) {
                    var a = koor.getAsJsonArray(n);
                    heuvels.put(n, DroomEilanden.wereld(DroomEilanden.LIEDJES,
                            new Vec3(a.get(0).getAsDouble() + 0.5, a.get(1).getAsDouble(), a.get(2).getAsDouble() + 0.5)));
                }
            } catch (Exception ex) {
                LOG.error("Guhdromenland: the Blub-koortje spots in liedjes.json are broken: {}", ex.toString());
                return null;
            }
            BlockPos m = DroomEilanden.midden(DroomEilanden.LIEDJES);
            eiland = new Koor(heuvels, new Vec3(m.getX() + 0.5, m.getY(), m.getZ() + 0.5), 46);
        }
        return eiland;
    }

    // =====================================================================================================================

    /** One choir: five podium spots (feet positions in the world) and the Blubs around them. */
    public static final class Koor {
        final Map<String, Vec3> heuvels;
        final Vec3 midden;
        final double straal;
        final Map<UUID, Staat> staten = new HashMap<>();
        /** Who led a Blub onto its hill this round (they get the piece first). */
        final Set<UUID> helpers = new HashSet<>();
        int zingt, rust;

        static final class Staat {
            @Nullable
            UUID leider;
            boolean opPlek;
            long schuwTot, meldTot;
        }

        public Koor(Map<String, Vec3> heuvels, Vec3 midden, double straal) {
            this.heuvels = heuvels;
            this.midden = midden;
            this.straal = straal;
        }

        public int zingt() {
            return zingt;
        }

        public boolean opPlek(BlubGuhEntity b) {
            Staat s = staten.get(b.getUUID());
            return s != null && s.opPlek;
        }

        /** This Blub follows this player (as when the player sneaks up to it). */
        public void volg(BlubGuhEntity b, ServerPlayer p) {
            staat(b).leider = p.getUUID();
        }

        Staat staat(BlubGuhEntity b) {
            return staten.computeIfAbsent(b.getUUID(), u -> new Staat());
        }

        List<BlubGuhEntity> blubs(ServerLevel level) {
            return level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(midden, midden).inflate(straal, 40, straal),
                    b -> b.isAlive() && noot(b) != null && !b.isTame());
        }

        List<ServerPlayer> spelers(ServerLevel level) {
            List<ServerPlayer> uit = new ArrayList<>();
            for (ServerPlayer p : level.players()) {
                if (p.position().distanceToSqr(midden.x, p.getY(), midden.z) < straal * straal) {
                    uit.add(p);
                }
            }
            return uit;
        }

        /** One check (every {@value BlubKoortje#ELKE} ticks): follow, place, shy, sing. */
        public void tick(ServerLevel level) {
            List<BlubGuhEntity> blubs = blubs(level);
            if (zingt > 0) {
                zingt -= ELKE;
                if (zingt % 10 == 0) {
                    for (BlubGuhEntity b : blubs) {
                        if (opPlek(b)) {
                            level.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + 1.1, b.getZ(), 0,
                                    NOTEN.indexOf(noot(b)) / 24.0 * 4, 0, 0, 1);
                            b.emotie(BlubGuhEntity.RECHTOP, 40);
                        }
                    }
                }
                if (zingt <= 0) {
                    afronden(level);
                }
                return;
            }
            if (rust > 0) {
                rust -= ELKE;
                return;
            }
            long t = level.getGameTime();
            Set<String> goed = new HashSet<>();
            for (BlubGuhEntity b : blubs) {
                String noot = noot(b);
                Staat s = staat(b);
                Vec3 thuis = heuvels.get(noot);
                if (s.opPlek) {
                    if (b.position().distanceToSqr(thuis) > 1.5) {
                        b.getNavigation().moveTo(thuis.x, thuis.y, thuis.z, 1.0);
                    } else {
                        b.getNavigation().stop();
                    }
                    goed.add(noot);
                    continue;
                }
                ServerPlayer leider = s.leider == null ? null : level.getPlayerByUUID(s.leider) instanceof ServerPlayer sp ? sp : null;
                if (leider != null && (!leider.isAlive() || leider.distanceToSqr(b) > LOS * LOS)) {
                    s.leider = null;
                    leider = null;
                }
                if (leider != null) {
                    String hier = heuvelVan(b.position());
                    if (noot.equals(hier)) {
                        s.opPlek = true;
                        helpers.add(leider.getUUID());
                        b.getNavigation().stop();
                        b.snapTo(thuis.x, thuis.y, thuis.z, b.getYRot(), b.getXRot());
                        b.emotie(BlubGuhEntity.RECHTOP, 20 * 120);
                        piep(level, b, noot, 1f);
                        goed.add(noot);
                        meld(level, Component.translatable("gui.guhs.dromen.koortje.goed", b.getDisplayName(),
                                Component.translatable("gui.guhs.dromen.koortje." + noot), goed.size()), ChatFormatting.LIGHT_PURPLE);
                        continue;
                    }
                    if (hier != null && t >= s.meldTot) {
                        s.meldTot = t + 60;
                        b.oortjes(BlubGuhEntity.PLAT);
                        leider.sendOverlayMessage(Component.translatable("gui.guhs.dromen.koortje.verkeerd",
                                Component.translatable("gui.guhs.dromen.koortje." + noot)).withStyle(ChatFormatting.GRAY));
                    }
                    if (b.distanceToSqr(leider) > 2.5 * 2.5) {
                        b.getNavigation().moveTo(leider, 1.15);
                    } else {
                        b.getNavigation().stop();
                    }
                    continue;
                }
                // nobody leads it: shy, unless you sneak
                Player p = level.getNearestPlayer(b, SCHUW);
                if (p instanceof ServerPlayer sp && !sp.isSpectator()) {
                    if (sp.isShiftKeyDown() && sp.distanceToSqr(b) <= SLUIP * SLUIP) {
                        s.leider = sp.getUUID();
                        b.oortjes(BlubGuhEntity.KRUL);
                        piep(level, b, noot, 0.8f);
                        sp.sendOverlayMessage(Component.translatable("gui.guhs.dromen.koortje.volgt", b.getDisplayName(),
                                Component.translatable("gui.guhs.dromen.koortje." + noot)).withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else if (!sp.isShiftKeyDown() && t >= s.schuwTot) {
                        s.schuwTot = t + 40;
                        Vec3 weg = b.position().subtract(sp.position()).multiply(1, 0, 1).normalize().scale(6).add(b.position());
                        b.getNavigation().moveTo(weg.x, b.getY(), weg.z, 1.3);
                        b.oortjes(BlubGuhEntity.PLAT);
                        if (t >= s.meldTot) {
                            s.meldTot = t + 200;
                            sp.sendOverlayMessage(Component.translatable("gui.guhs.dromen.koortje.schuw", b.getDisplayName())
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
            }
            if (goed.size() == NOTEN.size()) {
                zing(level, blubs);
            }
        }

        /** The hill this spot stands on (null: none). */
        @Nullable
        String heuvelVan(Vec3 pos) {
            for (Map.Entry<String, Vec3> e : heuvels.entrySet()) {
                Vec3 h = e.getValue();
                if (Mth.square(pos.x - h.x) + Mth.square(pos.z - h.z) <= OP_HEUVEL * OP_HEUVEL && Math.abs(pos.y - h.y) <= OP_HEUVEL_Y) {
                    return e.getKey();
                }
            }
            return null;
        }

        /** All five stand right: the do Blub starts the lullaby (one stream), the others peep along with notes. */
        void zing(ServerLevel level, List<BlubGuhEntity> blubs) {
            zingt = BlubGuhEntity.LIEDJE_TICKS;
            for (BlubGuhEntity b : blubs) {
                if ("do".equals(noot(b)) && opPlek(b)) {
                    b.slaapliedje(null);
                    break;
                }
            }
            meld(level, Component.translatable("gui.guhs.dromen.koortje.zingt"), ChatFormatting.LIGHT_PURPLE);
        }

        /** The song is over: the piece for everyone who helped or listened (while Snoes' story runs), then a rest. */
        public void afronden(ServerLevel level) {
            zingt = 0;
            Set<ServerPlayer> krijgen = new HashSet<>(spelers(level));
            for (UUID u : helpers) {
                if (level.getPlayerByUUID(u) instanceof ServerPlayer sp) {
                    krijgen.add(sp);
                }
            }
            for (ServerPlayer p : krijgen) {
                if (VERHAAL_LOOPT.test(p) && DroomVoortgang.geef(p, DroomVoortgang.Stukje.LIEDJE)) {
                    p.sendSystemMessage(Component.translatable("gui.guhs.dromen.koortje.stukje").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            meld(level, Component.translatable("gui.guhs.dromen.koortje.klaar"), ChatFormatting.GRAY);
            helpers.clear();
            staten.clear();
            rust = RUST;
        }

        /** Keeps one choir Blub of every note on the island (spawned on a free spot of its top). */
        void zorgVoorBlubs(ServerLevel level) {
            if (level.getGameTime() % 100 != 0 || spelers(level).isEmpty()) {
                return;
            }
            Set<String> er = new HashSet<>();
            for (BlubGuhEntity b : blubs(level)) {
                if (!er.add(noot(b))) {
                    b.discard();                                       // (two of the same note: one is enough)
                }
            }
            for (String n : NOTEN) {
                if (er.contains(n)) {
                    continue;
                }
                for (int poging = 0; poging < 12; poging++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2, r = 6 + level.getRandom().nextDouble() * 22;
                    int x = Mth.floor(midden.x + Math.cos(a) * r), z = Mth.floor(midden.z + Math.sin(a) * r);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y < midden.y - 4 || y > midden.y + 20 || heuvelVan(new Vec3(x + 0.5, y, z + 0.5)) != null) {
                        continue;
                    }
                    BlubGuhEntity b = BlubFeature.BLUBGUH.get().spawn(level, new BlockPos(x, y, z), EntitySpawnReason.EVENT);
                    if (b != null) {
                        maakKoorBlub(b, n);
                    }
                    break;
                }
            }
        }

        void piep(ServerLevel level, BlubGuhEntity b, String noot, float volume) {
            level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, volume, toon(noot));
            level.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + 1.0, b.getZ(), 0, NOTEN.indexOf(noot) / 24.0 * 4, 0, 0, 1);
        }

        void meld(ServerLevel level, Component c, ChatFormatting kleur) {
            for (ServerPlayer p : spelers(level)) {
                p.sendOverlayMessage(c.copy().withStyle(kleur));
            }
        }
    }
}
