package com.yetgeul.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 자판과 후보 줄을 직접 그리는 뷰. */
public class KeyboardView extends View {

    public enum Type { CHAR, SHIFT, DEL, SPACE, ENTER, SYM, LANG, OLD, ABC }

    public static final class Key {
        final String label; final Type type; final char ch; final String longPress; final float weight;
        final RectF rect = new RectF();
        Key(String label, Type type, char ch, String longPress, float weight) {
            this.label = label; this.type = type; this.ch = ch; this.longPress = longPress; this.weight = weight;
        }
    }

    public interface Listener {
        void onKey(Key key, char ch);
        void onCandidate(int index);
    }

    public enum Layer { KO, EN, SYM }

    private Listener listener;
    private Layer layer = Layer.KO;
    private boolean shift = false;
    private boolean oldMode = true;
    private boolean longPressEnabled = true;
    private List<List<Key>> rows = new ArrayList<>();
    private final List<String> candidates = new ArrayList<>();
    private final List<RectF> candRects = new ArrayList<>();

    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface oldFace;
    private final float dp;
    private final float stripH, keyH, gap;

    // 색 (밝은 / 어두운 테마)
    private int cBg, cKey, cKeyFn, cInk, cInk2, cAccent, cAccentInk, cPress;

    private Key pressed;
    private boolean popupShown;
    private int popupSel;
    private final RectF popupRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = this::onLongPress;
    private final Runnable repeatRunnable = new Runnable() {
        @Override public void run() {
            if (pressed != null && pressed.type == Type.DEL && listener != null) {
                listener.onKey(pressed, '\0');
                handler.postDelayed(this, 60);
            }
        }
    };

    public KeyboardView(Context ctx) {
        super(ctx);
        dp = ctx.getResources().getDisplayMetrics().density;
        stripH = 44 * dp; keyH = 50 * dp; gap = 5 * dp;
        Typeface tf;
        try { tf = Typeface.createFromAsset(ctx.getAssets(), "fonts/yetgeul.ttf"); } catch (Exception e) { tf = Typeface.SERIF; }
        oldFace = tf;
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(oldFace);
        hintPaint.setTextAlign(Paint.Align.RIGHT);
        hintPaint.setTypeface(oldFace);
        boolean night = (ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            cBg = 0xFF23272D; cKey = 0xFF3A3F47; cKeyFn = 0xFF2C3036; cInk = 0xFFEDEEF0; cInk2 = 0xFFA2A59C;
            cAccent = 0xFF93B3DC; cAccentInk = 0xFF111418; cPress = 0xFF4A5260;
        } else {
            cBg = 0xFFD9DBDF; cKey = 0xFFFFFFFF; cKeyFn = 0xFFB9BEC6; cInk = 0xFF16181B; cInk2 = 0xFF5B5E57;
            cAccent = 0xFF26446B; cAccentInk = 0xFFF6F5F0; cPress = 0xFFDDE4EC;
        }
        buildLayout();
    }

    public void setListener(Listener l) { listener = l; }
    public Layer getLayer() { return layer; }
    public boolean isShift() { return shift; }
    public void setOldMode(boolean on) { oldMode = on; invalidate(); }
    public void setLongPressEnabled(boolean on) { longPressEnabled = on; buildLayout(); }

    public void setLayer(Layer l) { layer = l; shift = false; buildLayout(); }

    public void setShift(boolean s) { shift = s; buildLayout(); }

    public void setCandidates(List<String> c) {
        candidates.clear();
        candidates.addAll(c);
        invalidate();
    }

    // ───────────────────────── 자판 배열

    private static Key k(String s, String lp) { return new Key(s, Type.CHAR, s.charAt(0), lp, 1f); }
    private static Key k(String s) { return k(s, null); }
    private static Key f(String label, Type t, float w) { return new Key(label, t, '\0', null, w); }

    private void buildLayout() {
        rows = new ArrayList<>();
        if (layer == Layer.KO) {
            String r1 = shift ? "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ" : "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ";
            rows.add(chars(r1));
            rows.add(chars("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ"));
            List<Key> r3 = new ArrayList<>();
            r3.add(f("⇧", Type.SHIFT, 1.5f));
            r3.addAll(chars("ㅋㅌㅊㅍㅠㅜㅡ"));
            r3.add(f("⌫", Type.DEL, 1.5f));
            rows.add(r3);
        } else if (layer == Layer.EN) {
            rows.add(chars(shift ? "QWERTYUIOP" : "qwertyuiop"));
            rows.add(chars(shift ? "ASDFGHJKL" : "asdfghjkl"));
            List<Key> r3 = new ArrayList<>();
            r3.add(f("⇧", Type.SHIFT, 1.5f));
            r3.addAll(chars(shift ? "ZXCVBNM" : "zxcvbnm"));
            r3.add(f("⌫", Type.DEL, 1.5f));
            rows.add(r3);
        } else {
            rows.add(chars("1234567890"));
            rows.add(chars("@#₩%&-+()/"));
            List<Key> r3 = new ArrayList<>();
            r3.add(new Key("〮", Type.CHAR, '〮', null, 1.25f));
            r3.add(new Key("〯", Type.CHAR, '〯', null, 1.25f));
            r3.addAll(chars("*\"':;!?"));
            r3.add(f("⌫", Type.DEL, 1.5f));
            rows.add(r3);
        }
        List<Key> r4 = new ArrayList<>();
        r4.add(f(layer == Layer.SYM ? "가" : "!#1", layer == Layer.SYM ? Type.ABC : Type.SYM, 1.4f));
        r4.add(f(layer == Layer.EN ? "한" : "A", Type.LANG, 1.1f));
        r4.add(f("옛", Type.OLD, 1.2f));
        r4.add(f("", Type.SPACE, 3.8f));
        r4.add(k(".", ",?!"));
        r4.add(f("↵", Type.ENTER, 1.5f));
        rows.add(r4);
        if (getWidth() > 0) layoutKeys(getWidth());
        requestLayout();
        invalidate();
    }

    private List<Key> chars(String s) {
        List<Key> list = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            list.add(k(c, longPressEnabled ? archaicFor(c) : null));
        }
        return list;
    }

    /** 길게 누르면 나오는 옛 자모. */
    static String archaicFor(String c) {
        switch (c) {
            case "ㅅ": return "ㅿㅺㅼㅽ";
            case "ㅇ": return "ㆁㆆ";
            case "ㅎ": return "ㆆ";
            case "ㅂ": return "ㅸㅄㅳ";
            case "ㄱ": return "ㅺ";
            case "ㄷ": return "ㅼ";
            case "ㅏ": case "ㅡ": return "ㆍ";
            case "ㅐ": return "ㆎ";
            case "ㅆ": return "ㅄ";
            case "ㄸ": return "ㅳ";
            default: return null;
        }
    }

    // ───────────────────────── 크기와 그리기

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = (int) (stripH + rows.size() * (keyH + gap) + gap * 2);
        setMeasuredDimension(w, h);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { layoutKeys(w); }

    private void layoutKeys(int w) {
        float y = stripH + gap;
        float maxUnits = 10f;
        float unit = (w - gap) / maxUnits;
        for (List<Key> row : rows) {
            float units = 0;
            for (Key key : row) units += key.weight;
            float scale = units > maxUnits ? maxUnits / units : 1f;
            float x = (w - units * unit * scale) / 2f + gap / 2f;
            for (Key key : row) {
                float kw = key.weight * unit * scale;
                key.rect.set(x + gap / 2f, y, x + kw - gap / 2f, y + keyH);
                x += kw;
            }
            y += keyH + gap;
        }
    }

    @Override protected void onDraw(Canvas c) {
        if (rows.isEmpty()) return;
        if (rows.get(0).get(0).rect.isEmpty()) layoutKeys(getWidth());
        c.drawColor(cBg);
        drawStrip(c);
        float r = 6 * dp;
        for (List<Key> row : rows) {
            for (Key key : row) {
                boolean fn = key.type != Type.CHAR && key.type != Type.SPACE;
                int bg = fn ? cKeyFn : cKey;
                int ink = cInk;
                if (key.type == Type.OLD && oldMode) { bg = cAccent; ink = cAccentInk; }
                if (key.type == Type.ENTER) { bg = cAccent; ink = cAccentInk; }
                if (key.type == Type.SHIFT && shift) { bg = cAccent; ink = cAccentInk; }
                if (key == pressed && !popupShown) bg = cPress;
                keyPaint.setColor(bg);
                c.drawRoundRect(key.rect, r, r, keyPaint);
                String label = key.type == Type.SPACE ? (oldMode ? "옛글 · 스페이스" : "스페이스") : key.label;
                textPaint.setColor(key.type == Type.SPACE ? cInk2 : ink);
                textPaint.setTextSize((key.type == Type.SPACE || key.label.length() > 1 ? 14 : 21) * dp);
                drawCentered(c, label, key.rect.centerX(), key.rect.centerY(), textPaint);
                if (key.longPress != null) {
                    hintPaint.setColor(cAccent);
                    hintPaint.setTextSize(10 * dp);
                    c.drawText(key.longPress.substring(0, 1), key.rect.right - 4 * dp, key.rect.top + 12 * dp, hintPaint);
                }
            }
        }
        if (popupShown && pressed != null) drawPopup(c);
    }

    private void drawStrip(Canvas c) {
        candRects.clear();
        float x = 8 * dp;
        float cy = stripH / 2f;
        for (int i = 0; i < candidates.size(); i++) {
            String s = candidates.get(i);
            textPaint.setTextSize((i == 0 ? 18 : 15) * dp);
            float tw = textPaint.measureText(s);
            RectF rr = new RectF(x, 6 * dp, x + tw + 24 * dp, stripH - 4 * dp);
            if (rr.right > getWidth()) break;
            if (i == 0) {
                keyPaint.setColor(cKey);
                c.drawRoundRect(rr, 6 * dp, 6 * dp, keyPaint);
            }
            textPaint.setColor(i == 0 ? cInk : cInk2);
            drawCentered(c, s, rr.centerX(), cy + 1 * dp, textPaint);
            candRects.add(rr);
            x = rr.right + 6 * dp;
        }
        if (candidates.isEmpty()) {
            textPaint.setTextSize(13 * dp);
            textPaint.setColor(cInk2);
            String hint = oldMode ? "옛글 켜짐 · 띄어쓰면 옛 표기로 바뀝니다" : "옛글 꺼짐 · [옛] 키로 켜기";
            drawCentered(c, hint, getWidth() / 2f, cy, textPaint);
        }
    }

    private void drawPopup(Canvas c) {
        String opts = pressed.label + pressed.longPress;
        float cw = 40 * dp, ch = 52 * dp;
        float w = cw * opts.length() + 8 * dp;
        float left = Math.max(4 * dp, Math.min(pressed.rect.left - 4 * dp, getWidth() - w - 4 * dp));
        float top = Math.max(2 * dp, pressed.rect.top - ch - 10 * dp);
        popupRect.set(left, top, left + w, top + ch);
        keyPaint.setColor(cKey);
        keyPaint.setShadowLayer(8 * dp, 0, 2 * dp, 0x55000000);
        c.drawRoundRect(popupRect, 8 * dp, 8 * dp, keyPaint);
        keyPaint.clearShadowLayer();
        for (int i = 0; i < opts.length(); i++) {
            RectF cell = new RectF(left + 4 * dp + i * cw, top + 4 * dp, left + 4 * dp + (i + 1) * cw, top + ch - 4 * dp);
            if (i == popupSel) {
                keyPaint.setColor(cAccent);
                c.drawRoundRect(cell, 6 * dp, 6 * dp, keyPaint);
            }
            textPaint.setColor(i == popupSel ? cAccentInk : cInk);
            textPaint.setTextSize(22 * dp);
            drawCentered(c, String.valueOf(opts.charAt(i)), cell.centerX(), cell.centerY(), textPaint);
        }
    }

    private static void drawCentered(Canvas c, String s, float cx, float cy, Paint p) {
        Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, p);
    }

    // ───────────────────────── 터치

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (y < stripH) {
                    for (int i = 0; i < candRects.size(); i++) {
                        if (candRects.get(i).contains(x, y) && listener != null) { listener.onCandidate(i); return true; }
                    }
                    return true;
                }
                pressed = find(x, y);
                popupShown = false;
                if (pressed != null) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (pressed.longPress != null) handler.postDelayed(longPressRunnable, 380);
                    if (pressed.type == Type.DEL) {
                        listener.onKey(pressed, '\0');
                        handler.postDelayed(repeatRunnable, 400);
                    }
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (popupShown) {
                    int n = pressed.label.length() + pressed.longPress.length();
                    int sel = (int) ((x - popupRect.left - 4 * dp) / (40 * dp));
                    popupSel = Math.max(0, Math.min(n - 1, sel));
                    invalidate();
                } else if (pressed != null && !pressed.rect.contains(x, y) && pressed.type != Type.DEL) {
                    Key k2 = find(x, y);
                    if (k2 != pressed) {
                        handler.removeCallbacks(longPressRunnable);
                        pressed = k2;
                        if (pressed != null && pressed.longPress != null) handler.postDelayed(longPressRunnable, 380);
                        invalidate();
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(repeatRunnable);
                Key k = pressed;
                if (k != null && listener != null) {
                    if (popupShown) {
                        String opts = k.label + k.longPress;
                        listener.onKey(k, opts.charAt(popupSel));
                    } else if (k.type != Type.DEL) {
                        listener.onKey(k, k.ch);
                    }
                }
                pressed = null;
                popupShown = false;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(repeatRunnable);
                pressed = null; popupShown = false; invalidate();
                return true;
            default:
                return true;
        }
    }

    private void onLongPress() {
        if (pressed == null || pressed.longPress == null) return;
        popupShown = true;
        popupSel = 1;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        invalidate();
    }

    private Key find(float x, float y) {
        Key best = null;
        float bestD = Float.MAX_VALUE;
        for (List<Key> row : rows) {
            for (Key k : row) {
                if (y < k.rect.top - gap || y > k.rect.bottom + gap) continue;
                if (k.rect.contains(x, y)) return k;
                float d = Math.abs(k.rect.centerX() - x);
                if (d < bestD) { bestD = d; best = k; }
            }
        }
        return best;
    }
}
