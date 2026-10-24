package nl.juiced.guhs.feature.grotspellen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Glimwormvangen with Juf Glimmie: 60 seconds in her dark hall, with a net and a jar on your back. Glimwormpjes flutter
 * about ({@link VangGlimwormEntity}); some are quick, some hide in the glimplanten; a golden one is worth 5 points. Every
 * glimworm you catch goes in the jar, and the jar and the hall's glimpotten get brighter ({@link #lichtStand}); a caught
 * (not golden) glimworm is replaced by a new one after two seconds. On the harder levels there are more of them, they
 * are quicker, and the hall starts darker.
 */
public final class Glimwormvangen extends GrotSpel {
    /** Every glimworm that belongs to a game right now. */
    static final Set<UUID> WORMEN = ConcurrentHashMap.newKeySet();
    public static final int SPEELTICKS = 60 * 20;
    public static final int GOUD_PUNTEN = 5, ZWAAI_PAUZE = 8, NIEUW_NA = 40, PER_ZWAAI = 3;
    public static final double VANG_AFSTAND = 3.2;
    /** How high the glimworms fly: from the floor of the hall up to this many blocks. */
    public static final double VLIEG_HOOGTE = 4.5;

    // --- per level --------------------------------------------------------------------------------------------------
    public static int aantal(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 10;
            case MEDIUM -> 14;
            case LASTIG -> 18;
        };
    }

    public static int snelle(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 1;
            case MEDIUM -> 4;
            case LASTIG -> 7;
        };
    }

    public static int verstoppers(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 2;
            case MEDIUM -> 3;
            case LASTIG -> 4;
        };
    }

    public static int gouden(Niveau n) {
        return n == Niveau.LASTIG ? 1 : 2;
    }

    public static double snelheid(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 0.05;
            case MEDIUM -> 0.07;
            case LASTIG -> 0.09;
        };
    }

    /** How bright the hall is at the start (0-4): darker on the harder levels. */
    public static int startLicht(Niveau n) {
        return switch (n) {
            case MAKKELIJK -> 2;
            case MEDIUM -> 1;
            case LASTIG -> 0;
        };
    }

    /** The light of the jar and the hall after this many caught glimworms: one stage brighter per 4. */
    public static int lichtStand(Niveau n, int gevangen) {
        return Math.min(4, startLicht(n) + gevangen / 4);
    }

    /** Glimmertjes: 0.9 / 1.7 / 2.3 per point (a good game on medium ~30-40, on lastig ~50-60), +5 for a record. */
    public static int munten(Niveau n, int punten, boolean record) {
        int pct = switch (n) {
            case MAKKELIJK -> 90;
            case MEDIUM -> 170;
            case LASTIG -> 230;
        };
        return GrotSpel.munten(n, punten * pct / 100, record);
    }

    // --- the game ---------------------------------------------------------------------------------------------------
    @Nullable
    private AABB zaal;
    private final List<BlockPos> planten = new ArrayList<>();
    private final List<BlockPos> potten = new ArrayList<>();
    private final List<UUID> wormen = new ArrayList<>();
    private final List<Integer> wacht = new ArrayList<>();
    private int gevangen, goudGevangen, punten, licht = -1, goudOver, gestuurd = -1;

    Glimwormvangen(UUID npcId) {
        super(npcId);
    }

    @Override
    public Soort soort() {
        return Soort.GLIMWORMVANGEN;
    }

    public int gevangen() {
        return gevangen;
    }

    public int goudGevangen() {
        return goudGevangen;
    }

    @Nullable
    public AABB zaal() {
        return zaal;
    }

    public List<BlockPos> potten() {
        return potten;
    }

    public List<UUID> wormen() {
        return wormen;
    }

    @Override
    protected boolean zoek(ServerLevel world, GuhNpcEntity npc) {
        planten.clear();
        potten.clear();
        BlockPos c = npc.blockPosition();
        java.util.Set<BlockPos> lucht = zaalLucht(world, npc);
        java.util.Set<BlockPos> pot = new java.util.HashSet<>();
        for (BlockPos pos : lucht) {
            if (world.getBlockState(pos).is(GrotSpellenFeature.GLIMPLANT.get())) {
                planten.add(pos);
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos q = pos.relative(d);
                if (world.getBlockState(q).is(GrotSpellenFeature.GLIMPOT.get()) && pot.add(q)) {
                    potten.add(q);
                }
            }
        }
        if (potten.size() < 4) {
            return false;
        }
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : potten) {
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxZ = Math.max(maxZ, p.getZ());
        }
        double vloer = c.getY();
        zaal = new AABB(minX + 1.5, vloer + 0.4, minZ + 1.5, maxX - 0.5, vloer + VLIEG_HOOGTE, maxZ - 0.5);
        return true;
    }

    @Override
    protected boolean heel(ServerLevel world) {
        int n = 0;
        for (BlockPos p : potten) {
            n += world.getBlockState(p).is(GrotSpellenFeature.GLIMPOT.get()) ? 1 : 0;
        }
        return zaal != null && n >= 4;
    }

    @Override
    protected int speelTicks() {
        return SPEELTICKS;
    }

    @Override
    protected void begin(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        weg(world);
        gevangen = goudGevangen = punten = 0;
        wacht.clear();
        goudOver = gouden(niveau);
        gestuurd = -1;
        int n = aantal(niveau), snel = snelle(niveau), verstop = Math.min(verstoppers(niveau), planten.size());
        for (int i = 0; i < n; i++) {
            maak(world, i < snel, i >= snel && i < snel + verstop, false);
        }
        licht = -1;
        zetLicht(world, startLicht(niveau));
    }

    /** A new glimworm somewhere in the hall (a hider starts in its plant). */
    @Nullable
    private VangGlimwormEntity maak(ServerLevel world, boolean snel, boolean verstopper, boolean goud) {
        if (zaal == null) {
            return null;
        }
        VangGlimwormEntity w = GrotSpellenFeature.VANGGLIMWORM.get().create(world, EntitySpawnReason.TRIGGERED);
        if (w == null) {
            return null;
        }
        RandomSource r = world.getRandom();
        BlockPos plant = verstopper && !planten.isEmpty() ? planten.get(r.nextInt(planten.size())) : null;
        double v = snelheid(niveau) * (snel ? 1.8 : 1.0) * (goud ? 1.3 : 1.0);
        double schrik = goud ? 4.5 : snel ? 4.0 : verstopper ? 2.5 : 3.0;
        w.zet(zaal, v, schrik, goud, plant);
        Vec3 plek = plant != null ? Vec3.atCenterOf(plant).add(0, -0.15, 0)
                : new Vec3(zaal.minX + r.nextDouble() * (zaal.maxX - zaal.minX), zaal.minY + r.nextDouble() * (zaal.maxY - zaal.minY),
                        zaal.minZ + r.nextDouble() * (zaal.maxZ - zaal.minZ));
        w.snapTo(plek.x, plek.y, plek.z, r.nextFloat() * 360f, 0f);
        WORMEN.add(w.getUUID());
        wormen.add(w.getUUID());
        world.addFreshEntity(w);
        return w;
    }

    @Override
    protected void speelTick(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        // the gold ones come out after a few seconds, one at a time
        if (goudOver > 0 && over % 300 == 150) {
            goudOver--;
            maak(world, false, false, true);
            flits(p, Component.translatable("gui.guhs.glimwormvangen.goud_daar").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
        for (int i = wacht.size() - 1; i >= 0; i--) {
            int t = wacht.get(i) - 1;
            if (t <= 0) {
                wacht.remove(i);
                maak(world, world.getRandom().nextInt(aantal(niveau)) < snelle(niveau), !planten.isEmpty() && world.getRandom().nextInt(4) == 0, false);
            } else {
                wacht.set(i, t);
            }
        }
        if (over % 10 == 0) {
            wormen.removeIf(id -> {
                boolean weg = !(world.getEntity(id) instanceof VangGlimwormEntity);
                if (weg) {
                    WORMEN.remove(id);
                }
                return weg;
            });
            potje(p);
        }
    }

    /** The jar on the player's back shows the light stage (and everyone around sees it, see GrotSpellenPayloads). */
    private void potje(ServerPlayer p) {
        int stand = lichtStand(niveau, gevangen);
        if (stand != gestuurd || over % 100 == 0) {
            gestuurd = stand;
            GrotSpellenPayloads.stuur(p, stand);
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(GrotSpellenFeature.GLIMPOTJE_RUG.get()) && GrotSpelItems.stand(s) != stand) {
                GrotSpelItems.zetStand(s, stand);
            }
        }
    }

    private void zetLicht(ServerLevel world, int stand) {
        if (stand == licht) {
            return;
        }
        licht = stand;
        for (BlockPos pos : potten) {
            BlockState s = world.getBlockState(pos);
            if (s.is(GrotSpellenFeature.GLIMPOT.get()) && s.getValue(GrotSpellenBlocks.LICHT) != stand) {
                world.setBlock(pos, s.setValue(GrotSpellenBlocks.LICHT, stand), 2);
            }
        }
    }

    /** A swing of the net: catches up to {@value #PER_ZWAAI} glimworms right in front of you. */
    public static int zwaai(ServerPlayer p) {
        if (!(spelVan(p) instanceof Glimwormvangen spel) || spel.fase != Fase.SPELEN) {
            return 0;
        }
        ServerLevel world = p.level();
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        world.playSound(null, p.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
        List<VangGlimwormEntity> raak = new ArrayList<>();
        for (VangGlimwormEntity w : world.getEntitiesOfClass(VangGlimwormEntity.class, p.getBoundingBox().inflate(VANG_AFSTAND + 1))) {
            if (!spel.wormen.contains(w.getUUID())) {
                continue;
            }
            Vec3 naar = w.position().add(0, 0.2, 0).subtract(oog);
            double d = naar.length();
            if (d <= 1.3 || d <= VANG_AFSTAND && naar.normalize().dot(kijk) >= 0.55) {
                raak.add(w);
            }
        }
        raak.sort(java.util.Comparator.comparingDouble(w -> w.distanceToSqr(oog)));
        int n = 0;
        for (VangGlimwormEntity w : raak) {
            if (n >= PER_ZWAAI) {
                break;
            }
            spel.vang(world, p, w);
            n++;
        }
        return n;
    }

    /** Caught: in the jar it goes. */
    public void vang(ServerLevel world, ServerPlayer p, VangGlimwormEntity w) {
        if (!wormen.remove(w.getUUID())) {
            return;
        }
        WORMEN.remove(w.getUUID());
        boolean goud = w.isGoud();
        gevangen++;
        punten += goud ? GOUD_PUNTEN : 1;
        if (goud) {
            goudGevangen++;
        } else {
            wacht.add(NIEUW_NA);
        }
        world.sendParticles(goud ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.GLOW, w.getX(), w.getY() + 0.2, w.getZ(), goud ? 16 : 6, 0.2, 0.2, 0.2, 0.05);
        world.playSound(null, w.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, goud ? 2.0f : 1.2f + world.getRandom().nextFloat() * 0.4f);
        w.discard();
        flits(p, goud ? Component.translatable("gui.guhs.glimwormvangen.goud", GOUD_PUNTEN).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : Component.translatable("gui.guhs.glimwormvangen.vang", punten).withStyle(ChatFormatting.GREEN));
        zetLicht(world, lichtStand(niveau, gevangen));
        potje(p);
    }

    @Override
    protected boolean klaar() {
        return false;
    }

    @Override
    protected Component balk() {
        return Component.translatable("gui.guhs.glimwormvangen.balk", gevangen, goudGevangen, klok(), punten).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected int punten() {
        return punten;
    }

    @Override
    protected int munten(boolean record) {
        return munten(niveau, punten, record);
    }

    @Override
    protected List<Component> uitslagRegels() {
        return List.of(Component.translatable("quest.guhs.glimwormvangen.uitslag", gevangen - goudGevangen),
                Component.translatable(goudGevangen > 0 ? "quest.guhs.glimwormvangen.uitslag_goud" : "quest.guhs.glimwormvangen.uitslag_geen_goud",
                        goudGevangen, goudGevangen * GOUD_PUNTEN));
    }

    private void weg(ServerLevel world) {
        for (UUID id : wormen) {
            WORMEN.remove(id);
            if (world.getEntity(id) instanceof VangGlimwormEntity w) {
                w.discard();
            }
        }
        wormen.clear();
        wacht.clear();
    }

    /** Every glimworm away, the glimpotten back to their quiet glow. */
    @Override
    protected void opruimen(ServerLevel world) {
        weg(world);
        licht = -1;
        zetLicht(world, 1);
    }

    @Override
    protected List<ItemStack> leen() {
        return List.of(new ItemStack(GrotSpellenFeature.GLIMNETJE.get()), new ItemStack(GrotSpellenFeature.GLIMPOTJE_RUG.get()));
    }

    /** (tests) Catches this glimworm as if the net got it. */
    public void testVang(ServerLevel world, ServerPlayer p, VangGlimwormEntity w) {
        vang(world, p, w);
    }

    public int licht() {
        return licht;
    }
}
