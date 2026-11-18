package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.annotation.Nullable;

/**
 * The pixels of a drawing: 64 x 64 ARGB ints, row by row (index {@code y * 64 + x}). A pixel with alpha 0 is "paper"
 * (nothing drawn: the eraser makes paper); every other pixel is fully opaque. On the wire and on disk the 16384 bytes
 * (big endian) are deflated: a drawing is a few hundred bytes to a few KB, never more than {@link #MAX_PAK}.
 */
public final class TekenPixels {
    public static final int W = 64, H = 64, N = W * H, BYTES = N * 4;
    /** The most a packed drawing may weigh (deflate of 16 KB worst case is ~16.4 KB): the payloads cap at this too. */
    public static final int MAX_PAK = 20000;
    /** The colour paper shows (in the world, the album, the tooltip). */
    public static final int PAPIER = 0xFFFFFAF0;

    private TekenPixels() {
    }

    public static int[] leeg() {
        return new int[N];
    }

    /** Alpha below half = paper (0), else fully opaque. */
    public static int schoon(int argb) {
        return (argb >>> 24) < 128 ? 0 : argb | 0xFF000000;
    }

    /** What to show for this pixel: paper where nothing is drawn. */
    public static int toon(int argb) {
        return (argb >>> 24) == 0 ? PAPIER : argb;
    }

    public static boolean isLeeg(int[] px) {
        for (int p : px) {
            if (p != 0) {
                return false;
            }
        }
        return true;
    }

    public static byte[] pak(int[] px) {
        byte[] raw = new byte[BYTES];
        for (int i = 0; i < N; i++) {
            int p = schoon(px[i]);
            raw[i * 4] = (byte) (p >>> 24);
            raw[i * 4 + 1] = (byte) (p >>> 16);
            raw[i * 4 + 2] = (byte) (p >>> 8);
            raw[i * 4 + 3] = (byte) p;
        }
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buf = new byte[4096];
            while (!d.finished()) {
                int n = d.deflate(buf);
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    /** The pixels of a packed drawing, or null when it is not exactly one valid 64x64 drawing (too big, too small, broken). */
    @Nullable
    public static int[] pakUit(@Nullable byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_PAK) {
            return null;
        }
        Inflater inf = new Inflater();
        try {
            inf.setInput(data);
            byte[] raw = new byte[BYTES + 1];   // one more: a bigger stream is refused, not cut off
            int n = 0;
            while (!inf.finished() && n < raw.length) {
                int k = inf.inflate(raw, n, raw.length - n);
                if (k == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                n += k;
            }
            if (n != BYTES || !inf.finished()) {
                return null;
            }
            int[] px = new int[N];
            for (int i = 0; i < N; i++) {
                int p = (raw[i * 4] & 0xFF) << 24 | (raw[i * 4 + 1] & 0xFF) << 16 | (raw[i * 4 + 2] & 0xFF) << 8 | raw[i * 4 + 3] & 0xFF;
                px[i] = schoon(p);
            }
            return px;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inf.end();
        }
    }
}
