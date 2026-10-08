package net.riftwatch.rift_quests.client.art;

import java.util.HashMap;
import java.util.Map;

public final class Art {
    public static final int OUTLINE = 0xFF1A100B;

    public record CogPalette(int outline, int high, int base, int shade, int hub, int hubHigh) {
    }

    public static final CogPalette COG_WOOD = new CogPalette(0xFF1B0F07, 0xFFC99560, 0xFF9A6A3C, 0xFF64401F, 0xFF9D988E, 0xFFD0CABF);
    public static final CogPalette COG_WARM = new CogPalette(0xFF1B0F07, 0xFFF6C78A, 0xFFB37B42, 0xFF6E4520, 0xFFBDB5A7, 0xFFF0E8DA);
    public static final CogPalette COG_RUST = new CogPalette(0xFF140A06, 0xFF8E5C42, 0xFF6A422C, 0xFF43281A, 0xFF6A5244, 0xFF86685A);

    public record CogSpec(String key, double radius, int teeth, double depth, int steps, double toothFraction, double ringIn, double hub,
            int spokes, double spokeWidth) {
    }

    public static final CogSpec SMALL_COG = new CogSpec("s", 5.5, 8, 1.6, 32, 0.5, 0, 1.7, 0, 0.9);
    public static final CogSpec BIG_RING = new CogSpec("b", 22, 18, 3, 72, 0.5, 16.5, 0, 0, 0.9);

    public record CasePalette(int outline, int high, int base, int shade, int speck1, int speck2, int innerLine, int boltHigh, int boltDark,
            int inner, int groove, int groove2) {
    }

    private static final CasePalette CASE_ANDESITE = new CasePalette(0xFF120C09, 0xFFCDC7BC, 0xFF9D988E, 0xFF5F5B54, 0xFFB3ADA3, 0xFF878279,
            0xFF47433E, 0xFFECE7DE, 0xFF47433E, 0xFF3B2A1C, 0xFF4A3524, 0xFF31231A);
    private static final CasePalette CASE_ANDESITE_RUST = new CasePalette(0xFF100906, 0xFFA07050, 0xFF7A523C, 0xFF4A3022, 0xFFA8622F, 0xFF5A3826,
            0xFF3A2216, 0xFFB07A52, 0xFF3A2216, 0xFF2A1D15, 0xFF33241A, 0xFF211710);
    private static final CasePalette CASE_BRASS = new CasePalette(0xFF160C04, 0xFFFFE7A0, 0xFFDBA64A, 0xFF8F5F1E, 0xFFEEBA5A, 0xFFC38F36,
            0xFF6A4210, 0xFFFFF4CF, 0xFF6A4210, 0xFF3A2416, 0xFF48301D, 0xFF2F1D11);
    private static final CasePalette CASE_BRASS_RUST = new CasePalette(0xFF120A04, 0xFFA8955E, 0xFF7E6C42, 0xFF4A3E22, 0xFF4F7A66, 0xFF5F6A46,
            0xFF33291A, 0xFFB8A874, 0xFF33291A, 0xFF271A12, 0xFF30221A, 0xFF1E150E);

    public record PanelPalette(int frame, int outline, int high, int base, int shade, int speck1, int speck2, int innerLeft, int innerRight,
            int boltHigh, int boltDark, int inner, int seam, int seamHigh, int rivetSpacing, int seed) {
    }

    public static final PanelPalette PANEL_BRASS = new PanelPalette(4, 0xFF140A04, 0xFFFFE39A, 0xFFD6A046, 0xFF7E5218, 0xFFE6B252, 0xFFBC8A34,
            0xFF5A3A12, 0xFFF0C060, 0xFFFFF4CF, 0xFF5A3A0E, 0xFF21140D, 0xFF170D08, 0xFF2B1A10, 40, 3);
    public static final PanelPalette PANEL_ANDESITE = new PanelPalette(3, 0xFF100A07, 0xFFD0CABF, 0xFF9A958B, 0xFF57534D, 0xFFACA69C, 0xFF827D75,
            0xFF3F3B36, 0xFFB8B2A8, 0xFFEEE9E0, 0xFF3D3A36, 0xFF1B140F, 0xFF140E0A, 0xFF221911, 44, 5);
    public static final PanelPalette PANEL_COPPER = new PanelPalette(4, 0xFF120703, 0xFFF6B07A, 0xFFB8643A, 0xFF5E2A12, 0xFFC8744A, 0xFF9A5230,
            0xFF3A1A0A, 0xFFD98A4F, 0xFFFFD2AA, 0xFF4A2010, 0, 0, 0, 36, 9);

    public enum ShaftKind {
        TURN(0xFF120C08, new int[] {0xFFE6D2B2, 0xFFB6A68F, 0xFF8C8070, 0xFF5E554A}, new int[] {0xFFA29480, 0xFF827666, 0xFF655C51, 0xFF443D35}),
        IDLE(0xFF120C08, new int[] {0xFFB9B3A8, 0xFF948E84, 0xFF7A756D, 0xFF57534D}, new int[] {0xFF8A857C, 0xFF726D65, 0xFF5E5A54, 0xFF3F3C38}),
        RUST(0xFF120A06, new int[] {0xFF9A6240, 0xFF7A4A2C, 0xFF5F3822, 0xFF422515}, new int[] {0xFF7E4C30, 0xFF633A22, 0xFF4C2C19, 0xFF33190E});

        final int outline;
        final int[] rows;
        final int[] stripes;

        ShaftKind(int outline, int[] rows, int[] stripes) {
            this.outline = outline;
            this.rows = rows;
            this.stripes = stripes;
        }
    }

    public enum Lever {
        ON, MIDDLE, OFF, RUST
    }

    public static final Canvas LOCK = lock();
    public static final Canvas TEAM = team();
    public static final Canvas CHECK = check();
    public static final Canvas BANG = bang();
    public static final Canvas PLATE = plate();

    private static final Map<String, Canvas> COGS = new HashMap<>();
    private static final Map<String, Canvas> CASINGS = new HashMap<>();
    private static final Map<Lever, Canvas> LEVERS = new HashMap<>();
    private static final double[] BAYER = bayer();

    private Art() {
    }

    public static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    public static Canvas outline(Canvas canvas, int colour) {
        int w = canvas.width;
        int h = canvas.height;
        boolean[] solid = new boolean[w * h];
        for (int index = 0; index < solid.length; index++) {
            solid[index] = (canvas.pixels[index] >>> 24) != 0;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                if (solid[index]) {
                    continue;
                }
                if ((x > 0 && solid[index - 1]) || (x < w - 1 && solid[index + 1]) || (y > 0 && solid[index - w]) || (y < h - 1 && solid[index + w])) {
                    canvas.pixels[index] = colour;
                }
            }
        }
        return canvas;
    }

    public static void disc(Canvas canvas, double cx, double cy, double radius, int colour) {
        for (int y = (int) Math.floor(cy - radius - 1); y <= cy + radius; y++) {
            for (int x = (int) Math.floor(cx - radius - 1); x <= cx + radius; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                if (dx * dx + dy * dy <= radius * radius) {
                    canvas.px(x, y, colour);
                }
            }
        }
    }

    public static Canvas cog(CogSpec spec, int step, CogPalette palette) {
        int frame = Math.floorMod(step, spec.steps());
        String key = spec.key() + "|" + frame + "|" + palette.hashCode();
        return COGS.computeIfAbsent(key, ignored -> cogCanvas(spec, frame * 2 * Math.PI / spec.steps(), palette));
    }

    private static Canvas cogCanvas(CogSpec spec, double angle, CogPalette palette) {
        int n = (int) Math.ceil(spec.radius()) + 1;
        int size = n * 2;
        Canvas canvas = new Canvas(size, size);
        double rootRadius = spec.radius() - spec.depth();
        int[] classes = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - n;
                double dy = y + 0.5 - n;
                double r = Math.hypot(dx, dy);
                int kind = 0;
                if (r <= rootRadius) {
                    if (r <= spec.hub()) {
                        kind = 3;
                    } else if (r < spec.ringIn()) {
                        for (int spoke = 0; spoke < spec.spokes(); spoke++) {
                            double a = angle + spoke * 2 * Math.PI / spec.spokes();
                            double ux = Math.cos(a);
                            double uy = Math.sin(a);
                            if (dx * ux + dy * uy > 0 && Math.abs(-dx * uy + dy * ux) < spec.spokeWidth()) {
                                kind = 2;
                                break;
                            }
                        }
                    } else {
                        kind = 1;
                    }
                } else if (r <= spec.radius()) {
                    double f = (Math.atan2(dy, dx) - angle) / (2 * Math.PI) * spec.teeth();
                    f -= Math.floor(f);
                    if (f < spec.toothFraction()) {
                        kind = 1;
                    }
                }
                classes[y * size + x] = kind;
            }
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int kind = at(classes, size, x, y);
                if (kind == 0) {
                    if (at(classes, size, x - 1, y) != 0 || at(classes, size, x + 1, y) != 0 || at(classes, size, x, y - 1) != 0
                            || at(classes, size, x, y + 1) != 0) {
                        canvas.set(x, y, palette.outline());
                    }
                    continue;
                }
                int colour;
                if (kind == 3) {
                    colour = at(classes, size, x, y - 1) != 3 || at(classes, size, x - 1, y) != 3 ? palette.hubHigh() : palette.hub();
                } else {
                    boolean top = at(classes, size, x, y - 1) == 0 || at(classes, size, x - 1, y) == 0;
                    boolean bottom = at(classes, size, x, y + 1) == 0 || at(classes, size, x + 1, y) == 0;
                    colour = top ? palette.high() : bottom ? palette.shade() : palette.base();
                }
                canvas.set(x, y, colour);
            }
        }
        return canvas;
    }

    private static int at(int[] classes, int size, int x, int y) {
        return x < 0 || y < 0 || x >= size || y >= size ? 0 : classes[y * size + x];
    }

    public static void rivet(Canvas canvas, int x, int y, int high, int speck, int dark) {
        canvas.set(x, y, high);
        canvas.set(x + 1, y, speck);
        canvas.set(x, y + 1, speck);
        canvas.set(x + 1, y + 1, dark);
    }

    public static Canvas casing(boolean small, boolean locked, int seed) {
        String key = (small ? "s" : "m") + (locked ? "L" : "") + seed;
        return CASINGS.computeIfAbsent(key, ignored -> {
            int size = small ? 22 : 26;
            int frame = small ? 2 : 4;
            Canvas canvas = new Canvas(size, size);
            drawCasing(canvas, 0, 0, size, frame, small ? locked ? CASE_ANDESITE_RUST : CASE_ANDESITE : locked ? CASE_BRASS_RUST : CASE_BRASS, seed);
            return canvas;
        });
    }

    private static void drawCasing(Canvas canvas, int x, int y, int size, int frame, CasePalette palette, int seed) {
        canvas.rect(x, y, size, size, palette.outline());
        int edge = size - 1;
        for (int j = 1; j < edge; j++) {
            for (int i = 1; i < edge; i++) {
                boolean inner = i > frame && j > frame && i < edge - frame && j < edge - frame;
                int colour;
                if (inner) {
                    colour = palette.inner();
                    if ((j + (int) (hash(seed, j, 5) * 3)) % 4 == 0) {
                        colour = palette.groove();
                    } else if (hash(i + seed * 31, j, 11) < 0.07) {
                        colour = palette.groove2();
                    }
                } else {
                    colour = palette.base();
                    double h = hash(i + seed * 17, j, seed);
                    if (h < 0.14) {
                        colour = palette.speck1();
                    } else if (h > 0.9) {
                        colour = palette.speck2();
                    }
                    boolean ring = i >= frame && j >= frame && i <= edge - frame && j <= edge - frame;
                    if (ring && (i == frame || j == frame)) {
                        colour = palette.innerLine();
                    } else if (ring && (i == edge - frame || j == edge - frame)) {
                        colour = palette.speck1();
                    }
                    if (i == 1 || j == 1) {
                        colour = palette.high();
                    } else if (i == edge - 1 || j == edge - 1) {
                        colour = palette.shade();
                    }
                }
                canvas.set(x + i, y + j, colour);
            }
        }
        int offset = frame == 2 ? 1 : 2;
        int far = size - offset - 2;
        int[][] bolts = {{offset, offset}, {far, offset}, {offset, far}, {far, far}};
        for (int[] bolt : bolts) {
            rivet(canvas, x + bolt[0], y + bolt[1], palette.boltHigh(), palette.speck1(), palette.boltDark());
        }
        if (frame > 2) {
            int middle = (size >> 1) - 1;
            int[][] more = {{middle, offset}, {middle, far}, {offset, middle}, {far, middle}};
            for (int[] bolt : more) {
                rivet(canvas, x + bolt[0], y + bolt[1], palette.boltHigh(), palette.speck1(), palette.boltDark());
            }
        }
    }

    public static void panel(Canvas canvas, int x, int y, int w, int h, PanelPalette palette) {
        int frame = palette.frame();
        int ex = w - 1;
        int ey = h - 1;
        canvas.rect(x, y, w, h, palette.outline());
        if (palette.inner() != 0) {
            int ix = x + frame + 1;
            int iy = y + frame + 1;
            int iw = w - 2 * frame - 2;
            int ih = h - 2 * frame - 2;
            canvas.rect(ix, iy, iw, ih, palette.inner());
            for (int i = ix + 14; i < ix + iw - 2; i += 18) {
                canvas.rect(i, iy, 1, ih, palette.seam());
                canvas.rect(i + 1, iy, 1, ih, palette.seamHigh());
            }
            for (int j = iy; j < iy + ih; j++) {
                for (int i = ix; i < ix + iw; i++) {
                    if (hash(i, j, palette.seed() + 1) < 0.03) {
                        canvas.set(i, j, palette.seam());
                    }
                }
            }
            canvas.rect(ix, iy, iw, 1, 0x59000000);
            canvas.rect(ix, iy, 1, ih, 0x59000000);
        }
        for (int j = 1; j < ey; j++) {
            for (int i = 1; i < ex; i++) {
                if (i > frame && j > frame && i < ex - frame && j < ey - frame) {
                    continue;
                }
                int colour = palette.base();
                double hh = hash(x + i, y + j, palette.seed());
                if (hh < 0.12) {
                    colour = palette.speck1();
                } else if (hh > 0.92) {
                    colour = palette.speck2();
                }
                boolean ring = i >= frame && j >= frame && i <= ex - frame && j <= ey - frame;
                if (ring && (i == frame || j == frame)) {
                    colour = palette.innerLeft();
                } else if (ring && (i == ex - frame || j == ey - frame)) {
                    colour = palette.innerRight();
                }
                if (i == 1 || j == 1) {
                    colour = palette.high();
                } else if (i == ex - 1 || j == ey - 1) {
                    colour = palette.shade();
                }
                canvas.set(x + i, y + j, colour);
            }
        }
        int offset = 1 + ((frame - 2) >> 1);
        int[] xs = {x + offset, x + w - offset - 2};
        int[] ys = {y + offset, y + h - offset - 2};
        for (int rx : xs) {
            for (int ry : ys) {
                rivet(canvas, rx, ry, palette.boltHigh(), palette.speck1(), palette.boltDark());
            }
        }
        int spacing = palette.rivetSpacing();
        for (int rx = x + offset + spacing; rx < x + w - offset - 2 - spacing / 2; rx += spacing) {
            rivet(canvas, rx, ys[0], palette.boltHigh(), palette.speck1(), palette.boltDark());
            rivet(canvas, rx, ys[1], palette.boltHigh(), palette.speck1(), palette.boltDark());
        }
        if (h > 60) {
            for (int ry = y + offset + spacing; ry < y + h - offset - 2 - spacing / 2; ry += spacing) {
                rivet(canvas, xs[0], ry, palette.boltHigh(), palette.speck1(), palette.boltDark());
                rivet(canvas, xs[1], ry, palette.boltHigh(), palette.speck1(), palette.boltDark());
            }
        }
    }

    public static void shaftH(Canvas canvas, int xa, int xb, int y, ShaftKind kind, int offset) {
        int from = Math.min(xa, xb);
        int to = Math.max(xa, xb);
        int w = to - from + 1;
        canvas.rect(from, y - 3, w, 1, kind.outline);
        canvas.rect(from, y + 2, w, 1, kind.outline);
        for (int i = 0; i < 4; i++) {
            int yy = y - 2 + i;
            canvas.rect(from, yy, w, 1, kind.rows[i]);
            for (int x = from; x <= to; x++) {
                if (Math.floorMod(x + i - offset, 5) == 0) {
                    canvas.set(x, yy, kind.stripes[i]);
                }
            }
        }
        if (kind == ShaftKind.RUST) {
            for (int x = from; x <= to; x++) {
                double h = hash(x, y, 3);
                if (h < 0.2) {
                    canvas.set(x, y - 2 + ((int) (h * 97)) % 4, h < 0.1 ? 0xFFB8743F : 0xFF2E180C);
                }
            }
        }
    }

    public static void shaftV(Canvas canvas, int x, int ya, int yb, ShaftKind kind, int offset) {
        int from = Math.min(ya, yb);
        int to = Math.max(ya, yb);
        int h = to - from + 1;
        canvas.rect(x - 3, from, 1, h, kind.outline);
        canvas.rect(x + 2, from, 1, h, kind.outline);
        for (int i = 0; i < 4; i++) {
            int xx = x - 2 + i;
            canvas.rect(xx, from, 1, h, kind.rows[i]);
            for (int y = from; y <= to; y++) {
                if (Math.floorMod(y + i - offset, 5) == 0) {
                    canvas.set(xx, y, kind.stripes[i]);
                }
            }
        }
        if (kind == ShaftKind.RUST) {
            for (int y = from; y <= to; y++) {
                double hh = hash(x, y, 4);
                if (hh < 0.2) {
                    canvas.set(x - 2 + ((int) (hh * 97)) % 4, y, hh < 0.1 ? 0xFFB8743F : 0xFF2E180C);
                }
            }
        }
    }

    public static void gearbox(Canvas canvas, int x, int y, ShaftKind kind, int tick) {
        boolean rust = kind == ShaftKind.RUST;
        canvas.rect(x - 4, y - 4, 8, 8, 0xFF120C08);
        canvas.rect(x - 3, y - 3, 6, 6, rust ? 0xFF7A523C : 0xFFA29D93);
        canvas.rect(x - 3, y - 3, 6, 1, rust ? 0xFF9A6A4C : 0xFFD6D0C5);
        canvas.rect(x - 3, y - 3, 1, 6, rust ? 0xFF9A6A4C : 0xFFD6D0C5);
        canvas.rect(x - 3, y + 2, 6, 1, rust ? 0xFF4A3022 : 0xFF625E57);
        canvas.rect(x + 2, y - 3, 1, 6, rust ? 0xFF4A3022 : 0xFF625E57);
        canvas.rect(x - 2, y - 2, 4, 4, rust ? 0xFF2A1B12 : 0xFF3B2A1C);
        canvas.rect(x - 1, y - 1, 2, 2, rust ? 0xFF6C5446 : kind == ShaftKind.TURN ? 0xFFD8CBB6 : 0xFFB5AFA4);
        if (kind == ShaftKind.TURN) {
            int[][] spin = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            int[] p = spin[Math.floorMod(tick, 4)];
            canvas.set(x - 1 + p[0], y - 1 + p[1], 0xFF4A4641);
        }
    }

    public static void ring(Canvas canvas, int x, int y, int w, int h, int colour) {
        canvas.rect(x, y, w, 1, colour);
        canvas.rect(x, y + h - 1, w, 1, colour);
        canvas.rect(x, y + 1, 1, h - 2, colour);
        canvas.rect(x + w - 1, y + 1, 1, h - 2, colour);
    }

    public static void brackets(Canvas canvas, int x, int y, int w, int h, int colour) {
        int l = 4;
        canvas.rect(x, y, l, 1, colour);
        canvas.rect(x, y, 1, l, colour);
        canvas.rect(x + w - l, y, l, 1, colour);
        canvas.rect(x + w - 1, y, 1, l, colour);
        canvas.rect(x, y + h - 1, l, 1, colour);
        canvas.rect(x, y + h - l, 1, l, colour);
        canvas.rect(x + w - l, y + h - 1, l, 1, colour);
        canvas.rect(x + w - 1, y + h - l, 1, l, colour);
    }

    private static Canvas lock() {
        Canvas c = new Canvas(9, 11);
        c.rect(3, 1, 3, 1, 0xFFB1ABA1);
        c.set(2, 2, 0xFFB1ABA1);
        c.set(6, 2, 0xFFB1ABA1);
        for (int y = 3; y <= 4; y++) {
            c.set(2, y, 0xFF8F8A81);
            c.set(6, y, 0xFF8F8A81);
        }
        c.rect(1, 5, 7, 5, 0xFFD39F3F);
        c.rect(1, 5, 7, 1, 0xFFFFF0B4);
        c.rect(1, 9, 7, 1, 0xFFA06F24);
        c.set(4, 6, 0xFF2A1608);
        c.set(4, 7, 0xFF2A1608);
        return outline(c, 0xFF140A04);
    }

    private static Canvas team() {
        Canvas c = new Canvas(14, 9);
        head(c, 7, 1, 0xFFC06B3D, 0xFFE9B68A);
        head(c, 1, 3, 0xFF4A2C18, 0xFFD99A70);
        return outline(c, 0xFF140A04);
    }

    private static void head(Canvas c, int x, int y, int hair, int skin) {
        c.rect(x, y, 5, 5, skin);
        c.rect(x, y, 5, 1, hair);
        c.set(x, y + 1, hair);
        c.set(x + 4, y + 1, hair);
        c.set(x + 1, y + 2, 0xFF2A1A40);
        c.set(x + 3, y + 2, 0xFF2A1A40);
        c.set(x + 2, y + 4, 0xFFB8744F);
    }

    private static Canvas check() {
        Canvas c = new Canvas(9, 8);
        int[][] points = {{1, 3}, {2, 4}, {3, 5}, {4, 4}, {5, 3}, {6, 2}, {7, 1}};
        for (int[] p : points) {
            c.set(p[0], p[1], 0xFFA8EC9A);
            c.set(p[0], p[1] + 1, 0xFF5AA14E);
        }
        return outline(c, 0xFF0E1A0C);
    }

    private static Canvas bang() {
        Canvas c = new Canvas(7, 11);
        c.rect(1, 1, 5, 9, 0xFFFFD23C);
        c.rect(1, 1, 5, 1, 0xFFFFF3A8);
        c.rect(1, 9, 5, 1, 0xFFC98A10);
        c.rect(3, 2, 1, 4, 0xFF4A2A08);
        c.set(3, 7, 0xFF4A2A08);
        return outline(c, 0xFF2A1404);
    }

    private static Canvas plate() {
        Canvas c = new Canvas(64, 64);
        for (int py = 0; py < 2; py++) {
            for (int px = 0; px < 2; px++) {
                int x0 = px * 32;
                int y0 = py * 32;
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) {
                        int colour = 0xFF1A110C;
                        double h = hash(x0 + x, y0 + y, 7);
                        int tx = x + py * 3;
                        if (x > 2 && x < 29 && y > 2 && y < 29 && ((tx + y) % 8 == 0 || Math.floorMod(tx - y, 8) == 0)) {
                            colour = 0xFF20160F;
                        }
                        if (h < 0.05) {
                            colour = 0xFF21160F;
                        } else if (h > 0.97) {
                            colour = 0xFF140C08;
                        }
                        if (y == 0) {
                            colour = 0xFF2A1B11;
                        }
                        if (x == 0) {
                            colour = 0xFF24180F;
                        }
                        if (y == 31 || x == 31) {
                            colour = 0xFF0B0604;
                        }
                        c.set(x0 + x, y0 + y, colour);
                    }
                }
                int[][] bolts = {{3, 3}, {27, 3}, {3, 27}, {27, 27}};
                for (int[] b : bolts) {
                    c.rect(x0 + b[0], y0 + b[1], 2, 2, 0xFF3D2B1F);
                    c.set(x0 + b[0], y0 + b[1], 0xFF62483A);
                    c.set(x0 + b[0] + 1, y0 + b[1] + 2, 0xFF0B0604);
                    c.set(x0 + b[0] + 2, y0 + b[1] + 1, 0xFF0B0604);
                }
            }
        }
        return c;
    }

    private static double[] bayer() {
        int[] order = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
        double[] values = new double[16];
        for (int i = 0; i < 16; i++) {
            values[i] = (order[i] + 0.5) / 16;
        }
        return values;
    }

    private static int dithered(double level, int x, int y, int rgb, int step) {
        double scaled = level * 3;
        int base = (int) Math.floor(scaled);
        int value = base + ((scaled - base) > BAYER[(y & 3) * 4 + (x & 3)] ? 1 : 0);
        if (value <= 0) {
            return 0;
        }
        return Math.min(255, value * step) << 24 | rgb;
    }

    public static Canvas light(int w, int h) {
        Canvas c = new Canvas(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = (x - w * 0.42) / 230;
                double dy = (y + 30) / 210.0;
                double d = Math.hypot(dx, dy);
                c.pixels[y * c.width + x] = dithered(Math.pow(Math.max(0, 1 - d), 1.4), x, y, 0xFF963C, 12);
            }
        }
        return c;
    }

    public static Canvas vignette(int w, int h) {
        Canvas c = new Canvas(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double e = Math.max(Math.abs(x - w / 2.0) / (w / 2.0), Math.abs(y - h / 2.0) / (h / 2.0));
                c.pixels[y * c.width + x] = dithered(Math.max(0, (e - 0.78) / 0.22), x, y, 0x060301, 46);
            }
        }
        return c;
    }

    public static Canvas lever(Lever kind) {
        return LEVERS.computeIfAbsent(kind, Art::leverCanvas);
    }

    private static Canvas leverCanvas(Lever kind) {
        Canvas c = new Canvas(9, 11);
        boolean rust = kind == Lever.RUST;
        int stick = rust ? 0xFF5A3A28 : 0xFF6A5040;
        if (kind == Lever.ON) {
            int[][] points = {{4, 7}, {4, 6}, {3, 5}, {3, 4}, {2, 3}};
            for (int[] p : points) {
                c.set(p[0], p[1], stick);
            }
            c.rect(1, 1, 2, 2, 0xFFFFB347);
            c.set(1, 1, 0xFFFFF0B0);
        } else if (kind == Lever.MIDDLE) {
            for (int y = 3; y <= 7; y++) {
                c.set(4, y, stick);
            }
            c.rect(4, 1, 2, 2, 0xFFD98A4F);
            c.set(4, 1, 0xFFFFD2AA);
        } else {
            int[][] points = {{4, 7}, {4, 6}, {5, 5}, {5, 4}, {6, 3}};
            for (int[] p : points) {
                c.set(p[0], p[1], stick);
            }
            c.rect(6, 1, 2, 2, rust ? 0xFF6E4A35 : 0xFFA85A2A);
            c.set(6, 1, rust ? 0xFF8A5A40 : 0xFFD98A4F);
        }
        c.rect(1, 8, 7, 2, rust ? 0xFF7A523C : 0xFFD39F3F);
        c.rect(1, 8, 7, 1, rust ? 0xFF9A6A4C : 0xFFF4CB63);
        c.set(2, 9, rust ? 0xFF4A3022 : 0xFFA06F24);
        c.set(6, 9, rust ? 0xFF4A3022 : 0xFFA06F24);
        return outline(c, 0xFF120A05);
    }

    public static void line(Canvas canvas, int x0, int y0, double x1d, double y1d, int colour) {
        int x1 = (int) Math.round(x1d);
        int y1 = (int) Math.round(y1d);
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        for (int i = 0; i < 64; i++) {
            canvas.set(x0, y0, colour);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * error;
            if (e2 >= dy) {
                error += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                error += dx;
                y0 += sy;
            }
        }
    }

    public static void dial(Canvas canvas, int ox, int oy, double fraction) {
        double cx = 15;
        double cy = 15;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 30; x++) {
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double r = Math.hypot(dx, dy);
                if (dy > 0) {
                    continue;
                }
                int colour = 0;
                if (r <= 14.9 && r > 13.6) {
                    colour = 0xFF160C04;
                } else if (r <= 13.6 && r > 12) {
                    colour = dy < -9 ? 0xFFFFE39A : dy < -4 ? 0xFFD9A447 : 0xFF9A6A24;
                } else if (r <= 12) {
                    colour = 0xFF1A0F08;
                    double t = 1 - Math.atan2(-dy, dx) / Math.PI;
                    if (r > 8.6 && r <= 10.6) {
                        colour = t <= fraction ? r > 9.8 ? 0xFFFFC070 : 0xFFFF9A3C : 0xFF3A2214;
                    } else if (r > 10.6 && r <= 11.8 && Math.abs(t * 8 - Math.round(t * 8)) < 0.14) {
                        colour = 0xFFA8866A;
                    }
                }
                if (colour != 0) {
                    canvas.set(ox + x, oy + y, colour);
                }
            }
        }
        canvas.rect(ox, oy + 15, 30, 1, 0xFF160C04);
        canvas.rect(ox + 2, oy + 15, 26, 1, 0xFF8F5F1E);
        double a = Math.PI * (1 - fraction);
        line(canvas, ox + 14, oy + 14, ox + 14.5 + Math.cos(a) * 9, oy + 14.5 - Math.sin(a) * 9, 0xFFF4EDE8);
        canvas.rect(ox + 14, oy + 13, 2, 2, 0xFFFFE39A);
    }

    public static void crest(Canvas canvas, int ox, int oy) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double px = x + 0.5 - 8;
                double py = y + 0.5 - 8;
                int colour = 0;
                if (hex(px, py, 8.2) && !hex(px, py, 7)) {
                    colour = 0xFF8A4B23;
                } else if (hex(px, py, 6) && !hex(px, py, 5)) {
                    colour = 0xFFD98A4F;
                } else if (hex(px, py, 4.1) && !hex(px, py, 3.2)) {
                    colour = 0xFFB8743F;
                } else if (hex(px, py, 3.2)) {
                    colour = 0xFF1A0F08;
                }
                if (colour != 0) {
                    canvas.set(ox + x, oy + y, colour);
                }
            }
        }
        String[] rows = {"XX.", "X.X", "XX.", "X.X", "X.X"};
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < 3; i++) {
                if (rows[j].charAt(i) == 'X') {
                    canvas.set(ox + 6 + i, oy + 6 + j, 0xFFFFD0A0);
                }
            }
        }
    }

    private static boolean hex(double x, double y, double r) {
        double ax = Math.abs(x);
        double ay = Math.abs(y);
        return ax <= r * 0.866 && ay + ax * 0.577 <= r;
    }

    public static void gauge(Canvas canvas, int x, int y, int w, int h, double fraction, boolean done) {
        canvas.rect(x, y, w, h, 0xFF140A05);
        for (int cap : new int[] {x, x + w - 3}) {
            canvas.rect(cap, y, 3, h, 0xFFC06B3D);
            canvas.rect(cap, y, 3, 1, 0xFFF0A56E);
            canvas.rect(cap, y, 1, h, 0xFFF0A56E);
            canvas.rect(cap, y + h - 1, 3, 1, 0xFF6E3018);
            canvas.rect(cap + 2, y + 1, 1, h - 1, 0xFF6E3018);
        }
        int inner = w - 6;
        int fill = (int) Math.round(inner * Math.max(0, Math.min(1, fraction)));
        canvas.rect(x + 3, y + 1, inner, h - 2, 0xFF24140C);
        if (fill > 0) {
            int[] colours = done ? new int[] {0xFFFFF0A8, 0xFFF4CB63, 0xFFD39F3F, 0xFFA06F24, 0xFF7A4E16}
                    : new int[] {0xFFFFE0A0, 0xFFFFB347, 0xFFFF9A3C, 0xFFE07A2A, 0xFFA8541C};
            for (int row = 0; row < h - 2; row++) {
                canvas.rect(x + 3, y + 1 + row, fill, 1, colours[Math.min(row, colours.length - 1)]);
            }
            if (fill < inner) {
                canvas.rect(x + 3 + fill - 1, y + 1, 1, h - 3, 0xFFFFF3CF);
            }
        }
        for (int i = 1; i < 4; i++) {
            canvas.rect(x + 3 + (int) Math.round(inner * i / 4.0), y + 1, 1, 2, 0x47FFDCB4);
        }
        canvas.rect(x + 3, y + 1, inner, 1, 0x29FFF0DC);
    }

    public static void nixieTube(Canvas canvas, int x, int y, boolean space) {
        canvas.rect(x, y, 8, 16, space ? 0xFF0D0603 : 0xFF120804);
        if (!space) {
            for (int j = 1; j < 16; j += 2) {
                for (int i = 1; i < 8; i += 2) {
                    canvas.px(x + i, y + j, 0x1AFF8C3C);
                }
            }
            canvas.rect(x + 1, y + 1, 6, 1, 0xFF24140B);
        }
        ring(canvas, x, y, 8, 16, space ? 0xFF2A180D : 0xFF3D2414);
    }

    public static void slot(Canvas canvas, int x, int y, boolean ticket) {
        if (ticket) {
            ring(canvas, x - 1, y - 1, 20, 20, 0xFF5A3608);
        }
        canvas.rect(x, y, 18, 18, ticket ? 0xFF21150A : 0xFF1D120C);
        canvas.rect(x, y, 18, 1, 0xFF0A0503);
        canvas.rect(x, y, 1, 18, 0xFF0A0503);
        canvas.rect(x, y + 17, 18, 1, ticket ? 0xFFC98A10 : 0xFF5C3E28);
        canvas.rect(x + 17, y + 1, 1, 17, ticket ? 0xFFC98A10 : 0xFF5C3E28);
    }

    public enum ButtonStyle {
        BRASS(0xFFD9A447, 0xFFECBB5C, 0xFFFFE7A0, 0xFF7A4E16, 0xFF160C04),
        ANDESITE(0xFF9D988E, 0xFFB6B0A5, 0xFFD6D0C5, 0xFF4B4743, 0xFF120C09),
        LIT(0xFFFFB347, 0xFFFFC46A, 0xFFFFE0A8, 0xFFA8541C, 0xFF160C04),
        DISABLED(0xFF5F5B54, 0xFF5F5B54, 0xFF77736B, 0xFF3A3733, 0xFF120C09);

        final int base;
        final int hover;
        final int high;
        final int shade;
        final int outline;

        ButtonStyle(int base, int hover, int high, int shade, int outline) {
            this.base = base;
            this.hover = hover;
            this.high = high;
            this.shade = shade;
            this.outline = outline;
        }
    }

    public static void button(Canvas canvas, int x, int y, int w, int h, ButtonStyle style, boolean hover, boolean pressed, boolean glow) {
        if (glow) {
            ring(canvas, x - 3, y - 3, w + 6, h + 6, 0x40FFAA3C);
            ring(canvas, x - 2, y - 2, w + 4, h + 4, 0x99FFAA3C);
        }
        ring(canvas, x - 1, y - 1, w + 2, h + 2, style.outline);
        canvas.rect(x, y, w, h, hover ? style.hover : style.base);
        int top = pressed ? style.shade : style.high;
        int bottom = pressed ? style.high : style.shade;
        canvas.rect(x, y, w, 1, top);
        canvas.rect(x, y, 1, h, top);
        canvas.rect(x, y + h - 1, w, 1, bottom);
        canvas.rect(x + w - 1, y + 1, 1, h - 1, bottom);
    }

    public static void inset(Canvas canvas, int x, int y, int w, int h, int fill, int border) {
        canvas.rect(x, y, w, h, fill);
        ring(canvas, x, y, w, h, border);
    }

    public static void recenterIcon(Canvas canvas, int x, int y) {
        canvas.rect(x + 4, y + 1, 1, 7, 0xFF241D18);
        canvas.rect(x + 1, y + 4, 7, 1, 0xFF241D18);
        canvas.rect(x + 3, y + 3, 3, 1, 0xFF241D18);
        canvas.rect(x + 3, y + 5, 3, 1, 0xFF241D18);
        canvas.rect(x + 3, y + 3, 1, 3, 0xFF241D18);
        canvas.rect(x + 5, y + 3, 1, 3, 0xFF241D18);
        canvas.set(x + 4, y + 4, 0xFFFFB347);
    }
}
