package nl.juiced.guhs.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * 1.5.2 (client FPS): the guh model (guh.geo.json) has ~415 bones, almost all of them variant or outfit bones. Which of
 * them a guh shows used to be worked out by bone name, with streams, for every bone of every guh, every frame (and once
 * more per worn piece of clothing). Bases with a few dozen dressed guhs dropped to 10-15 fps.
 * <p>
 * Now every baked model gets one {@link Masks}: its bones in a fixed order, the child links, and per (variant, worn slots)
 * and per clothing piece a ready visibility {@link Mask}. A mask also knows which bones have nothing visible below them,
 * so an extra pass (clothes, hooks) skips those whole subtrees instead of walking all bones for one hat.
 */
public final class GuhBoneMasks {
    private static final Map<BakedGeoModel, Masks> MODELS = Collections.synchronizedMap(new WeakHashMap<>());

    private GuhBoneMasks() {
    }

    public static Masks of(BakedGeoModel model) {
        return MODELS.computeIfAbsent(model, Masks::new);
    }

    /** Per bone (in {@link Masks#bones} order): whether its own cubes show, and whether anything below it shows. */
    public record Mask(boolean[] own, boolean[] below) {
    }

    public static final class Masks {
        /** All bones; a parent always comes before its children. */
        public final GeoBone[] bones;
        private final int[][] children;
        private final boolean[] variantBone;
        private final boolean[] kruin;
        private final boolean[][] covered = new boolean[GuhClothes.Slot.values().length][];
        private final Map<Integer, boolean[]> hidden = new ConcurrentHashMap<>();
        private final Map<Integer, Mask> clothes = new ConcurrentHashMap<>();

        Masks(BakedGeoModel model) {
            Map<GeoBone, Integer> index = new IdentityHashMap<>();
            java.util.List<GeoBone> order = new java.util.ArrayList<>();
            for (GeoBone top : model.topLevelBones()) {
                collect(top, order, index);
            }
            for (GeoBone bone : model.boneLookup().get().values()) {   // (in case the lookup knows a bone the tree walk did not)
                if (!index.containsKey(bone)) {
                    collect(bone, order, index);
                }
            }
            bones = order.toArray(GeoBone[]::new);
            int n = bones.length;
            children = new int[n][];
            variantBone = new boolean[n];
            kruin = new boolean[n];
            for (int i = 0; i < n; i++) {
                GeoBone[] kids = bones[i].children();
                children[i] = new int[kids.length];
                for (int k = 0; k < kids.length; k++) {
                    children[i][k] = index.get(kids[k]);
                }
                String name = bones[i].name();
                variantBone[i] = GuhVariant.isVariantBone(name);
                kruin[i] = name.endsWith("_kruin");
            }
            for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
                boolean[] c = covered[slot.ordinal()] = new boolean[n];
                for (int i = 0; i < n; i++) {
                    c[i] = variantBone[i] && GuhClothes.slotCovers(slot, bones[i].name());
                }
            }
        }

        private static void collect(GeoBone bone, java.util.List<GeoBone> order, Map<GeoBone, Integer> index) {
            if (index.putIfAbsent(bone, order.size()) != null) {
                return;
            }
            order.add(bone);
            for (GeoBone child : bone.children()) {
                collect(child, order, index);
            }
        }

        public boolean variantBone(int i) {
            return variantBone[i];
        }

        /**
         * The main pass: which variant/outfit bones hide their own cubes (covered by a worn slot, or not this variant's).
         * Non-variant bones are false here; the renderer leaves them alone.
         */
        public boolean[] hidden(GuhVariant variant, Iterable<GuhClothes.Slot> worn) {
            int slots = 0;
            for (GuhClothes.Slot slot : worn) {
                slots |= 1 << slot.ordinal();
            }
            int wornSlots = slots;
            return hidden.computeIfAbsent(variant.ordinal() << 8 | wornSlots, k -> {
                boolean[] h = new boolean[bones.length];
                for (int i = 0; i < bones.length; i++) {
                    if (!variantBone[i]) {
                        continue;
                    }
                    boolean cover = false;
                    for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
                        if ((wornSlots & 1 << slot.ordinal()) != 0 && covered[slot.ordinal()][i]) {
                            cover = true;
                            break;
                        }
                    }
                    h[i] = cover || !variant.shows(bones[i].name());
                }
                return h;
            });
        }

        /** The pass of one worn piece; {@code kruinToo} false leaves out a hairstyle's high "*_kruin" part (under a hat). */
        public Mask clothes(GuhClothes piece, boolean kruinToo) {
            return clothes.computeIfAbsent(piece.ordinal() << 1 | (kruinToo ? 1 : 0), k -> {
                boolean[] own = new boolean[bones.length];
                for (int i = 0; i < bones.length; i++) {
                    own[i] = piece.shows(bones[i].name()) && (kruinToo || !kruin[i]);
                }
                return mask(own);
            });
        }

        /** A pass with its own bone test (feature hooks): tested once per bone, not cached. */
        public Mask of(Predicate<String> shows) {
            boolean[] own = new boolean[bones.length];
            for (int i = 0; i < bones.length; i++) {
                own[i] = shows.test(bones[i].name());
            }
            return mask(own);
        }

        private Mask mask(boolean[] own) {
            boolean[] below = new boolean[bones.length];
            for (int i = bones.length - 1; i >= 0; i--) {   // children come after their parent
                for (int c : children[i]) {
                    if (own[c] || below[c]) {
                        below[i] = true;
                        break;
                    }
                }
            }
            return new Mask(own, below);
        }
    }
}
