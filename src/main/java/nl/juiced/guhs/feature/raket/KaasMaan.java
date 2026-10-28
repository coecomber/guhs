package nl.juiced.guhs.feature.raket;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The Kaasmaan (dimension guhs:kaasmaan, data/guhs/dimension(_type)/kaasmaan.json: a flat cheese moon, ground at y {@link #GROND}).
 * <ul>
 *   <li>Low gravity for every player on it (a GRAVITY modifier, put on and taken off by {@link #tick}).</li>
 *   <li>The moon base, built once around (0, {@link #GROND}, 0) the first time someone lands ({@link #zorg}: whenever the
 *       return capsule at {@link #CAPSULE} is missing): a glass dome on a cheese-tile floor, the {@link TerugkeercapsuleBlock},
 *       lights, and craters with kaasmaankristal ore round it.</li>
 *   <li>Astronaut Kaasje (a guh in the astronaut clothes, tag {@link #TAG_KAASJE}): trades kaasmaankristallen for souvenirs
 *       ({@link #opKlik}).</li>
 *   <li>Planting a maanvlaggetje on the moon: the advancement quest/kaasmaan_vlag ({@link #opNeerzetten}).</li>
 * </ul>
 * Getting there: the Lanceerplatform after the launch ({@link #naarMaan}, remembers where you came from); back: the capsule
 * ({@link #terug}).
 */
public final class KaasMaan {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("kaasmaan"));
    /** The top block of the ground (the flat layers: bedrock 1, kaaskorst 50, gatenkaas 4). */
    public static final int GROND = 54;
    public static final BlockPos CAPSULE = new BlockPos(0, GROND + 1, -5);
    public static final Vec3 AANKOMST = new Vec3(0.5, GROND + 1, 4.5);
    public static final String TAG_KAASJE = "guhs_kaasmaan_kaasje", TAG_BEMANNING = "guhs_raket_bemanning";
    private static final String TERUG = "guhs_kaasmaan_terug";
    private static final Identifier ZWAARTE = Guhs.id("kaasmaan_zwaarte"), ZACHT = Guhs.id("kaasmaan_zacht");
    /** How much lighter (GRAVITY x (1 + this)) and how much farther you may fall unhurt on the moon. */
    static final double LICHTER = -0.7, VAL_EXTRA = 12;
    /** The trades: kristallen -> souvenir (the reisbureau's Kaasmaan souvenirs and our flag). */
    static final int PRIJS_VLAG = 4, PRIJS_MAANSTEEN = 8, PRIJS_RAKETJE = 16;
    private static final String G = "gui.guhs.kaasmaan.";

    private KaasMaan() {
    }

    public static boolean isMaan(Level level) {
        return level.dimension() == DIM;
    }

    // =====================================================================================================================
    // going and coming back
    // =====================================================================================================================

    /** To the Kaasmaan; remembers where to come back to (next to the platform {@code platform}). */
    public static void naarMaan(ServerPlayer p, BlockPos platform) {
        ServerLevel maan = p.level().getServer().getLevel(DIM);
        if (maan == null) {
            return;
        }
        CompoundTag t = new CompoundTag();
        Vec3 terug = Vec3.atBottomCenterOf(platform).add(4, 1, 4);
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putDouble("X", terug.x);
        t.putDouble("Y", terug.y);
        t.putDouble("Z", terug.z);
        p.getPersistentData().put(TERUG, t);
        zorg(maan);
        p.teleport(new TeleportTransition(maan, AANKOMST, Vec3.ZERO, 180f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable(G + "aankomst").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(p, "kaasmaan_bezocht");
        maan.playSound(null, BlockPos.containing(AANKOMST), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    /** Back to where you came from (else your respawn point). */
    public static void terug(ServerPlayer p) {
        CompoundTag t = p.getPersistentData().getCompoundOrEmpty(TERUG);
        TeleportTransition tr = null;
        if (t.contains("Dim")) {
            Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
            ServerLevel level = dim == null ? null : p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (level != null && level.dimension() != DIM) {
                tr = new TeleportTransition(level, new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0)), Vec3.ZERO,
                        p.getYRot(), 0f, TeleportTransition.DO_NOTHING);
            }
        }
        if (tr == null) {
            tr = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        }
        zwaarte(p, false);
        p.teleport(tr);
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable(G + "terug").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // the moon base (built once)
    // =====================================================================================================================

    private static BlockState guhs(String id, Block anders) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return (b == Blocks.AIR ? anders : b).defaultBlockState();
    }

    /** Builds the moon base if its capsule is missing, and puts astronaut Kaasje there if he is gone. */
    public static void zorg(ServerLevel maan) {
        if (!maan.getBlockState(CAPSULE).is(RaketFeature.TERUGKEERCAPSULE.get())) {
            bouwBasis(maan);
        }
        if (kaasje(maan) == null) {
            zetKaasje(maan);
        }
    }

    static void bouwBasis(ServerLevel maan) {
        BlockState vloer = guhs("belegen_kaas_tegels", Blocks.YELLOW_TERRACOTTA), koepel = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState(), rand = guhs("gatenkaas_stenen", Blocks.SMOOTH_SANDSTONE);
        int r = 8;
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int z = -r - 1; z <= r + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= r + 0.5) {
                    maan.setBlock(new BlockPos(x, GROND, z), (x + z) % 4 == 0 && d < r - 1 && d > 2 ? lamp : vloer, 3);
                } else if (d <= r + 1.5) {
                    maan.setBlock(new BlockPos(x, GROND, z), rand, 3);
                }
                for (int y = 1; y <= r + 1; y++) {
                    double dd = Math.sqrt(x * x + z * z + y * y);
                    BlockPos p = new BlockPos(x, GROND + y, z);
                    boolean deur = z > 0 && Math.abs(x) <= 1 && y <= 3;
                    if (dd >= r - 0.5 && dd <= r + 0.5 && !deur) {
                        maan.setBlock(p, koepel, 3);
                    } else if (dd < r - 0.5 || deur && dd <= r + 0.5) {
                        maan.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        maan.setBlock(CAPSULE, RaketFeature.TERUGKEERCAPSULE.get().defaultBlockState(), 3);
        maan.setBlock(CAPSULE.below(), lamp, 3);
        kraters(maan);
    }

    /** Eight craters round the base (fixed spots: the same on every server), with kaasmaankristal ore in their floors. */
    static void kraters(ServerLevel maan) {
        RandomSource rnd = RandomSource.create(180_2026L);
        BlockState rand = guhs("gatenkaas_stenen", Blocks.SMOOTH_SANDSTONE), erts = RaketFeature.KAASMAANKRISTAL_ERTS.get().defaultBlockState();
        for (int i = 0; i < 8; i++) {
            double hoek = i * Math.PI / 4 + rnd.nextDouble() * 0.5;
            int afstand = 18 + rnd.nextInt(22), straal = 4 + rnd.nextInt(4);
            int cx = (int) Math.round(Math.cos(hoek) * afstand), cz = (int) Math.round(Math.sin(hoek) * afstand);
            for (int x = -straal - 1; x <= straal + 1; x++) {
                for (int z = -straal - 1; z <= straal + 1; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > straal + 1.2) {
                        continue;
                    }
                    BlockPos top = new BlockPos(cx + x, GROND, cz + z);
                    if (d > straal) {
                        maan.setBlock(top.above(), rand, 3);                          // the rim, one high
                        continue;
                    }
                    int diepte = (int) Math.round(Math.sqrt(Math.max(0, straal * straal - d * d)) * 0.6);
                    for (int y = 0; y < diepte; y++) {
                        maan.setBlock(top.below(y), Blocks.AIR.defaultBlockState(), 3);
                    }
                    BlockPos bodem = top.below(diepte);
                    if (rnd.nextInt(6) == 0) {
                        maan.setBlock(bodem, erts, 3);
                    }
                }
            }
        }
    }

    @Nullable
    static GuhEntity kaasje(ServerLevel maan) {
        for (GuhEntity g : maan.getEntitiesOfClass(GuhEntity.class, new net.minecraft.world.phys.AABB(CAPSULE).inflate(12))) {
            if (g.entityTags().contains(TAG_KAASJE)) {
                return g;
            }
        }
        return null;
    }

    static void zetKaasje(ServerLevel maan) {
        GuhEntity g = ModEntities.GUH.get().create(maan, EntitySpawnReason.STRUCTURE);
        if (g == null) {
            return;
        }
        g.snapTo(3.5, GROND + 1, -2.5, 200f, 0f);
        g.wear(GuhClothes.ASTRONAUTENPAKJE);
        g.wear(GuhClothes.ASTRONAUTENHELM);
        g.setCustomName(Component.translatable(G + "kaasje"));
        g.setCustomNameVisible(true);
        g.setNoAi(true);
        g.setInvulnerable(true);
        g.setPersistenceRequired();
        g.addTag(TAG_KAASJE);
        maan.addFreshEntity(g);
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 3) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            zwaarte(p, isMaan(p.level()));
        }
        ServerLevel maan = event.getServer().getLevel(DIM);
        if (maan != null && !maan.players().isEmpty() && event.getServer().getTickCount() % 200 == 3) {
            zorg(maan);
        }
    }

    /** Low gravity on (on the moon) or off (everywhere else). */
    static void zwaarte(ServerPlayer p, boolean aan) {
        AttributeInstance z = p.getAttribute(Attributes.GRAVITY), v = p.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (z == null || v == null) {
            return;
        }
        if (aan && !z.hasModifier(ZWAARTE)) {
            z.addTransientModifier(new AttributeModifier(ZWAARTE, LICHTER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            v.addTransientModifier(new AttributeModifier(ZACHT, VAL_EXTRA, AttributeModifier.Operation.ADD_VALUE));
        } else if (!aan && z.hasModifier(ZWAARTE)) {
            z.removeModifier(ZWAARTE);
            v.removeModifier(ZACHT);
        }
    }

    /** Astronaut Kaasje: kaasmaankristallen for souvenirs (the dearest you can pay; sneaking: the flag). */
    static void opKlik(PlayerInteractEvent.EntityInteract event) {
        if (!event.getTarget().entityTags().contains(TAG_KAASJE) || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack hand = p.getMainHandItem();
        int n = hand.is(RaketFeature.KAASMAANKRISTAL.get()) ? hand.getCount() : 0;
        Item ruil = null;
        int prijs = 0;
        if (n >= PRIJS_RAKETJE && !p.isShiftKeyDown()) {
            ruil = item("reisbureau_zeldzaam_kaasmaan");
            prijs = PRIJS_RAKETJE;
        } else if (n >= PRIJS_MAANSTEEN && !p.isShiftKeyDown()) {
            ruil = item("reisbureau_souvenir_kaasmaan");
            prijs = PRIJS_MAANSTEEN;
        } else if (n >= PRIJS_VLAG) {
            ruil = RaketFeature.MAANVLAGGETJE.get().asItem();
            prijs = PRIJS_VLAG;
        }
        if (ruil == null || ruil == net.minecraft.world.item.Items.AIR) {
            p.sendSystemMessage(Component.translatable(G + "kaasje.praat." + p.getRandom().nextInt(3)).withStyle(ChatFormatting.YELLOW));
            p.sendSystemMessage(Component.translatable(G + "kaasje.ruil", PRIJS_VLAG, PRIJS_MAANSTEEN, PRIJS_RAKETJE).withStyle(ChatFormatting.GRAY));
            return;
        }
        hand.shrink(prijs);
        ItemStack s = new ItemStack(ruil);
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
        GuhAdvancements.grant(p, "kaasmaan_ruil");
        p.sendSystemMessage(Component.translatable(G + "kaasje.gekocht", s.getHoverName()).withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6f, 1.6f);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Guhs.id(id));
    }

    /** A maanvlaggetje planted on the moon. */
    static void opNeerzetten(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().is(RaketFeature.MAANVLAGGETJE.get()) && event.getEntity() instanceof ServerPlayer p
                && p.level() instanceof ServerLevel sl && isMaan(sl)) {
            GuhAdvancements.grant(p, "kaasmaan_vlag");
            BlockPos pos = event.getPos();
            sl.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 30, 0.6, 0.8, 0.6, 0.05);
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            p.sendSystemMessage(Component.translatable(G + "vlag").withStyle(ChatFormatting.GOLD));
        }
    }
}
