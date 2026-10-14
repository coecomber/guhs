package nl.juiced.guhs.feature.park.plek;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vogels.Vogeltje;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * 1.4.2 (park-plekjes b): het Duifguhtje, a pigeon parody of Het Knabbelpark: a round blue-grey bird with a glossy
 * green-and-purple neck and guh eyes (model, texture and animations: tools/features/park_plek_b_modellen.py; drawn by the
 * birds' renderer). It is a {@link Vogeltje} (sits, hops, pecks, flies round its home, never hurts anyone) with a few park
 * manners:
 * <ul>
 *   <li>park pigeons are used to people: it only flies up when you come really close ({@link #schrikAfstand});</li>
 *   <li>hold crumbs (broodkruimels, bread or seeds) near it and it flutters round you, low, cooing ({@link #voerder});</li>
 *   <li>crumbs strewn nearby ({@link DuivenOma#strooi}: by Oma-guh or by you) bring it down there to peck ({@link #kom});</li>
 *   <li>fed from your hand: hearts, a coo, a hidden advancement.</li>
 * </ul>
 * Now and then it coos all by itself ("koer": a puffed chest and a bobbing head).
 */
public class DuifguhtjeEntity extends Vogeltje {
    /** How far it sees crumbs in a hand. */
    public static final double VOER_ZICHT = 10.0;
    /** How close it lands round strewn crumbs. */
    public static final int VOER_STRAAL = 2;

    private int koerTijd = 100;

    public DuifguhtjeEntity(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
    }

    @Override
    public String naam() {
        return "duifguhtje";
    }

    @Override
    protected double vliegSnelheid() {
        return 0.3;
    }

    @Override
    public boolean lekker(ItemStack stack) {
        return stack.is(PlekB.KRUIMELS.get()) || stack.is(Items.BREAD) || stack.is(Items.WHEAT_SEEDS);
    }

    @Override
    protected SoundEvent roep() {
        return PlekB.ROEKOE.get();
    }

    @Override
    public double schrikAfstand() {
        return 1.6;
    }

    @Override
    protected String[] extraActies() {
        return new String[]{"koer"};
    }

    /** The nearest player within {@link #VOER_ZICHT} with crumbs in a hand (not a spectator), or null. */
    @Nullable
    public Player voerder() {
        Player best = null;
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(VOER_ZICHT), p -> !p.isSpectator() && p.isAlive())) {
            if ((lekker(p.getMainHandItem()) || lekker(p.getOffhandItem())) && (best == null || p.distanceToSqr(this) < best.distanceToSqr(this))) {
                best = p;
            }
        }
        return best;
    }

    /** Roekoe: the coo animation and its sound. */
    public void koer() {
        koerTijd = 160 + random.nextInt(240);
        triggerAnim("actie", "koer");
        level().playSound(null, getX(), getY(), getZ(), roep(), SoundSource.NEUTRAL, 0.7f, 0.9f + random.nextFloat() * 0.2f);
    }

    /** Crumbs were strewn at {@code waar}: come and peck there (a bird that sits takes off for it). */
    public void kom(BlockPos waar) {
        if (!vliegt()) {
            startVliegen(null, 1);
        }
        landplek = null;
        vliegTijd = 1;
    }

    @Override
    protected void zitStap() {
        if (--koerTijd <= 0) {
            koer();
        }
        BlockPos voer = level() instanceof ServerLevel server ? DuivenOma.voerplek(server, blockPosition(), VOER_STRAAL + 2) : null;
        if (voer != null && onGround() && random.nextInt(18) == 0) {
            triggerAnim("actie", "peck");
            if (random.nextInt(4) == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
        if (tickCount % 10 == 0 && voerder() != null) {
            startVliegen(null, 160);
        }
    }

    @Override
    protected boolean vliegStap() {
        Player p = voerder();
        if (p == null) {
            return false;
        }
        // flutter round the player who holds the crumbs: low, close, a little up and down
        landplek = null;
        vliegTijd = Math.max(vliegTijd, 60);
        double a = tickCount * 0.09 + getId() * 1.9;
        double r = 1.3 + (getId() % 3) * 0.35;
        stuur(p.position().add(Math.cos(a) * r, 1.5 + 0.4 * Math.sin(getId() + tickCount * 0.15), Math.sin(a) * r), vliegSnelheid());
        if (--koerTijd <= 0) {
            koer();
        }
        return true;
    }

    /** Strewn crumbs nearby: a spot round them to land on. */
    @Override
    @Nullable
    protected BlockPos eigenLandplek() {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        BlockPos voer = DuivenOma.voerplek(server, blockPosition(), 20);
        if (voer == null) {
            return null;
        }
        for (int i = 0; i < 10; i++) {
            int x = voer.getX() + random.nextInt(VOER_STRAAL * 2 + 1) - VOER_STRAAL;
            int z = voer.getZ() + random.nextInt(VOER_STRAAL * 2 + 1) - VOER_STRAAL;
            BlockPos top = new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (Math.abs(top.getY() - voer.getY()) <= 3 && magLanden(top)) {
                return top;
            }
        }
        return null;
    }

    @Override
    protected void geland(BlockPos plek) {
        triggerAnim("actie", "peck");
    }

    @Override
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
        koer();
        player.sendOverlayMessage(Component.translatable("gui.guhs.parkb.duif.gevoerd").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(player, "parkb_duif_gevoerd");
    }

    /** (tests) where it flies to now. */
    @Nullable
    public Vec3 vliegDoel() {
        return doel;
    }
}
