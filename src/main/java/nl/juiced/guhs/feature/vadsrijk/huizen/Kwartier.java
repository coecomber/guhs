package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Post;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * U-HUIZEN: Het Knuffelkwartier, the housing instance: its own dimension {@code guhs:knuffelkwartier} (flat grass, no mobs), wards of
 * {@value WijkPlan#KAVELS} plots. The heart of the slice: in and out, buying, building, moving up, selling, the mailbox.
 * <ul>
 *   <li><b>One house per account</b> (the Minecraft player, shared by all your characters): the house is a place, not a character
 *   stat. Coins and level come from the character you play when you buy.</li>
 *   <li><b>Home inventory.</b> The Knuffelkwartier is NOT the Vadsrijk: kern's inventory swap gives you your home inventory here (so
 *   you furnish with your Guhs deco from home, the Post delivers cosmetics here, and nothing of the Vadsrijk bag can leave it).
 *   Going back through the gate swaps back. While the swap is not done yet (the first second), nothing can be placed or broken.</li>
 *   <li><b>Wards scale:</b> a new ward is built the first time somebody needs a plot and every ward is full.</li>
 * </ul>
 */
public final class Kwartier {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("knuffelkwartier"));

    private Kwartier() {
    }

    public static boolean in(@Nullable Entity e) {
        return e != null && e.level().dimension() == DIM;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIM);
    }

    public static HuizenData data(MinecraftServer server) {
        return HuizenData.get(server);
    }

    /** Builds wards until {@code wijk} exists. */
    public static void zorgWijk(ServerLevel level, HuizenData d, int wijk) {
        while (d.wijken <= wijk) {
            WijkBouw.bouw(level, d.wijken, d);
            d.wijken++;
            d.setDirty();
        }
    }

    // ---- in and out -------------------------------------------------------------------------------------------------------------

    /** Remembers where the player comes from (only from outside the Knuffelkwartier). */
    static void onthoud(ServerPlayer p, HuizenData d) {
        if (in(p)) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.level().dimension().identifier().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        d.terug.put(p.getUUID(), t);
        d.setDirty();
    }

    public static boolean naarWijk(ServerPlayer p, int wijk) {
        ServerLevel l = level(p.level().getServer());
        if (l == null) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.geen_dimensie").withStyle(ChatFormatting.RED));
            return false;
        }
        HuizenData d = data(p.level().getServer());
        onthoud(p, d);
        zorgWijk(l, d, wijk);
        Vec3 a = WijkPlan.aankomst(wijk);
        p.teleport(new TeleportTransition(l, a, Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        Regels.aangekomen(p);
        p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.welkom", wijk + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    public static boolean naarHuis(ServerPlayer p, int sleutel) {
        ServerLevel l = level(p.level().getServer());
        if (l == null) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.geen_dimensie").withStyle(ChatFormatting.RED));
            return false;
        }
        HuizenData d = data(p.level().getServer());
        onthoud(p, d);
        Kavel k = WijkPlan.kavel(sleutel);
        zorgWijk(l, d, k.wijk());
        p.teleport(new TeleportTransition(l, k.voorDeur(), Vec3.ZERO, k.yawNaarBinnen(), 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        Regels.aangekomen(p);
        Huis h = d.huis(sleutel);
        if (h != null) {
            p.sendOverlayMessage(Component.translatable(h.eigenaar.equals(p.getUUID()) ? "vadsrijk.huizen.thuis" : "vadsrijk.huizen.bij", h.naam)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** Back to where the player came from (a Vadsrijk hub); else Zwamzoet; else the own respawn point. */
    public static void terug(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        HuizenData d = data(server);
        CompoundTag t = d.terug.remove(p.getUUID());
        d.setDirty();
        TeleportTransition tr = null;
        if (t != null) {
            Identifier id = Identifier.tryParse(t.getStringOr("dim", ""));
            ServerLevel l = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (l != null && l.dimension() != DIM) {
                tr = new TeleportTransition(l, new Vec3(t.getDoubleOr("x", 0), t.getDoubleOr("y", 64), t.getDoubleOr("z", 0)), Vec3.ZERO,
                        t.getFloatOr("yaw", 0), 0f, TeleportTransition.DO_NOTHING);
            }
        }
        if (tr == null) {
            ServerLevel v = Vadsrijk.level(server);
            BlockPos plein = Poorten.plek("zwamzoet");
            if (v != null && plein != null) {
                tr = new TeleportTransition(v, Vec3.atBottomCenterOf(plein), Vec3.ZERO, 0f, 0f, TeleportTransition.DO_NOTHING);
            }
        }
        if (tr == null) {
            tr = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        }
        p.teleport(tr);
        p.resetFallDistance();
    }

    // ---- buying, building, moving up, selling ---------------------------------------------------------------------------------

    /** Why this player can't buy this house now (a lang key + args), or null. Free = dev. */
    @Nullable
    public static Component magKopen(ServerPlayer p, HuizenData d, int sleutel, HuisSoort s, boolean gratis) {
        if (d.huisVan(p.getUUID()) != null) {
            return Component.translatable("vadsrijk.huizen.koop.al_een");
        }
        if (!d.vrij(sleutel)) {
            return Component.translatable("vadsrijk.huizen.koop.bezet");
        }
        if (gratis) {
            return null;
        }
        if (Spelers.level(p) < s.level) {
            return Component.translatable("vadsrijk.huizen.koop.level", s.level, Component.translatable(s.naamKey()));
        }
        if (Spelers.data(p).munten < s.prijs) {
            return Component.translatable("vadsrijk.huizen.koop.munten", Spelers.munten(s.prijs));
        }
        return null;
    }

    /** Buys plot {@code sleutel}: pays, claims, builds, celebrates. False (with a message) when not allowed. */
    public static boolean koop(ServerPlayer p, int sleutel, HuisSoort s, int variant, boolean gratis) {
        MinecraftServer server = p.level().getServer();
        HuizenData d = data(server);
        Component nee = magKopen(p, d, sleutel, s, gratis);
        if (nee != null) {
            p.sendSystemMessage(nee.copy().withStyle(ChatFormatting.RED));
            return false;
        }
        if (!gratis && !Spelers.neemMunten(p, s.prijs)) {
            return false;
        }
        d.claim(sleutel, p.getUUID(), p.getGameProfile().name(), s, variant, gratis ? 0 : s.prijs);
        ServerLevel l = level(server);
        if (l != null) {
            zorgWijk(l, d, Math.floorDiv(sleutel, WijkPlan.KAVELS));
            bouw(l, d, sleutel);
            naarHuis(p, sleutel);
            feest(l, WijkPlan.kavel(sleutel));
        }
        Huis h = d.huis(sleutel);
        if (h != null) {
            Figuranten.bijwerkenVan(p, h);
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.huizen.koop.gelukt", Component.translatable(s.naamKey()), sleutel % WijkPlan.KAVELS + 1,
                sleutel / WijkPlan.KAVELS + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** (Re)builds the plot after the data: the house (deco returned first) or the "te koop" sign. */
    public static void bouw(ServerLevel l, HuizenData d, int sleutel) {
        Kavel k = WijkPlan.kavel(sleutel);
        Huis h = d.huis(sleutel);
        Figuranten.weg(l, sleutel);
        if (h == null) {
            Ontwerp.teKoop(l, k);
        } else {
            ruimOp(l, d, h);
            Ontwerp.huis(l, k, h.soort, h.variant);
        }
    }

    /** Takes every placed deco block out and keeps it for the owner at the mailbox (never lost, never doubled). Returns how many. */
    public static int ruimOp(ServerLevel l, HuizenData d, Huis h) {
        int n = 0;
        for (long lp : List.copyOf(h.geplaatst)) {
            BlockPos pos = BlockPos.of(lp);
            BlockState s = l.getBlockState(pos);
            if (s.isAir() || tweedeHelft(s)) {
                continue;
            }
            ItemStack item = s.getCloneItemStack(l, pos, true);
            if (item.isEmpty()) {
                item = new ItemStack(s.getBlock().asItem());
            }
            l.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            if (!item.isEmpty()) {
                CompoundTag t = EcoUtil.tag(l.registryAccess(), item);
                if (t != null) {
                    d.bewaarVoorPost(h.eigenaar, t);
                    n++;
                }
            }
        }
        for (long lp : h.geplaatst) {   // (the other halves of doors/beds/tall plants)
            BlockPos pos = BlockPos.of(lp);
            if (!l.getBlockState(pos).isAir()) {
                l.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        h.geplaatst.clear();
        d.setDirty();
        return n;
    }

    /** The half of a two-block thing that does not count as the item (upper door half, bed head, upper plant half). */
    static boolean tweedeHelft(BlockState s) {
        if (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof DoublePlantBlock) {
            return s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        if (s.getBlock() instanceof BedBlock) {
            return s.getValue(BedBlock.PART) == BedPart.HEAD;
        }
        return false;
    }

    /** One size up (pay the difference). */
    public static boolean vergroot(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        HuizenData d = data(server);
        Integer k = d.sleutelVan(p.getUUID());
        Huis h = k == null ? null : d.huis(k);
        if (h == null || h.soort.groter() == null) {
            return false;
        }
        HuisSoort nieuw = h.soort.groter();
        long verschil = nieuw.prijs - h.soort.prijs;
        if (Spelers.level(p) < nieuw.level) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.koop.level", nieuw.level, Component.translatable(nieuw.naamKey())).withStyle(ChatFormatting.RED));
            return false;
        }
        if (!Spelers.neemMunten(p, verschil)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.koop.munten", Spelers.munten(verschil)).withStyle(ChatFormatting.RED));
            return false;
        }
        h.soort = nieuw;
        h.betaald += verschil;
        d.setDirty();
        herbouw(p, d, k);
        p.sendSystemMessage(Component.translatable("vadsrijk.huizen.groter.gelukt", Component.translatable(nieuw.naamKey())).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Another look (free). */
    public static boolean uiterlijk(ServerPlayer p, int variant) {
        HuizenData d = data(p.level().getServer());
        Integer k = d.sleutelVan(p.getUUID());
        Huis h = k == null ? null : d.huis(k);
        if (h == null) {
            return false;
        }
        h.variant = Math.floorMod(variant, Ontwerp.PALETTEN.size());
        d.setDirty();
        herbouw(p, d, k);
        return true;
    }

    static void herbouw(ServerPlayer p, HuizenData d, int sleutel) {
        ServerLevel l = level(p.level().getServer());
        if (l == null) {
            return;
        }
        Huis h = d.huis(sleutel);
        int n = h == null ? 0 : ruimOp(l, d, h);
        bouw(l, d, sleutel);
        feest(l, WijkPlan.kavel(sleutel));
        if (n > 0) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.deco_terug", n).withStyle(ChatFormatting.GOLD));
        }
        if (in(p) && WijkPlan.kavel(sleutel).bevat(p.getX(), p.getZ())) {
            naarHuis(p, sleutel);   // (never stuck inside a new wall)
        }
    }

    /** Sells the house: half of what was paid comes back, the deco waits at the account's mailbox (also for a new house). */
    public static boolean verkoop(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        HuizenData d = data(server);
        Integer k = d.sleutelVan(p.getUUID());
        Huis h = k == null ? null : d.huis(k);
        if (h == null) {
            return false;
        }
        ServerLevel l = level(server);
        int n = l == null ? 0 : ruimOp(l, d, h);
        long terug = h.betaald / 2;
        d.geefOp(k);
        if (l != null) {
            bouw(l, d, k);
        }
        if (terug > 0) {
            Spelers.geefMunten(p, terug);
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.huizen.verkocht", Spelers.munten(terug), n).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Dev: takes the house of {@code eigenaar} away (deco goes to the account's post box). */
    public static boolean reset(MinecraftServer server, UUID eigenaar) {
        HuizenData d = data(server);
        Integer k = d.sleutelVan(eigenaar);
        Huis h = k == null ? null : d.huis(k);
        if (h == null) {
            return false;
        }
        ServerLevel l = level(server);
        if (l != null) {
            ruimOp(l, d, h);
        }
        d.geefOp(k);
        if (l != null) {
            bouw(l, d, k);
        }
        return true;
    }

    // ---- the mailbox --------------------------------------------------------------------------------------------------------

    /** Hands out the Post (Vadsrijk cosmetics) and the deco that came out of the house. Returns how many stacks. */
    public static int haalPost(ServerPlayer p) {
        HuizenData d = data(p.level().getServer());
        int n = Post.bezorg(p, true);
        ListTag l = d.postvak.get(p.getUUID());
        if (l != null && !Spelers.data(p).binnen) {
            ListTag rest = new ListTag();
            for (int i = 0; i < l.size(); i++) {
                CompoundTag t = l.getCompoundOrEmpty(i);
                ItemStack s = EcoUtil.stack(p.registryAccess(), t);
                if (s.isEmpty()) {
                    rest.add(t);
                    continue;
                }
                p.getInventory().add(s);
                if (s.isEmpty()) {
                    n++;
                } else {
                    CompoundTag over = EcoUtil.tag(p.registryAccess(), s);
                    rest.add(over != null ? over : t);
                }
            }
            if (rest.isEmpty()) {
                d.postvak.remove(p.getUUID());
            } else {
                d.postvak.put(p.getUUID(), rest);
            }
            d.setDirty();
        }
        return n;
    }

    public static int postAantal(ServerPlayer p) {
        HuizenData d = data(p.level().getServer());
        ListTag l = d.postvak.get(p.getUUID());
        return Post.aantal(p) + (l == null ? 0 : l.size());
    }

    static void feest(ServerLevel l, Kavel k) {
        Vec3 m = k.midden();
        l.sendParticles(ParticleTypes.CHERRY_LEAVES, m.x, m.y + 4, m.z, 80, 6, 3, 6, 0.05);
        l.sendParticles(ParticleTypes.HEART, m.x, m.y + 6, m.z, 12, 3, 1, 3, 0);
        l.playSound(null, BlockPos.containing(m), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
    }
}
