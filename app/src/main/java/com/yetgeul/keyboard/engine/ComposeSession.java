package com.yetgeul.keyboard.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 입력 중인 문장 상태. 띄어쓰기로 끝난 낱말은 옛 표기로 바꿔 보여 주고,
 * 지금 치는 낱말은 현대 한글 그대로 보여 줌. 문장 전체를 매번 다시 변환하므로
 * "중국과 달라"처럼 뒤 낱말을 보고 앞 낱말을 고치는 규칙도 적용됨.
 */
public final class ComposeSession {

    public enum SpaceResult { WORD_ADDED, FLUSH, NOTHING }

    private static final class Word {
        final List<Character> keys; boolean raw;
        Word(List<Character> k) { keys = k; }
        String modern() { return HangulComposer.compose(keys); }
    }

    /** 이만큼 낱말이 쌓이면 앞부분을 확정해 입력창에 넘김. */
    public static final int MAX_WORDS = 12;

    private final List<Word> done = new ArrayList<>();
    private List<Character> cur = new ArrayList<>();
    private Converter conv;
    private boolean oldMode = true;

    public ComposeSession(Converter conv) { this.conv = conv; }

    public void setConverter(Converter c) { conv = c; }
    public void setOldMode(boolean on) { oldMode = on; }
    public boolean isOldMode() { return oldMode; }
    public boolean isActive() { return !done.isEmpty() || !cur.isEmpty(); }
    public boolean hasCurrent() { return !cur.isEmpty(); }
    public int wordCount() { return done.size(); }

    public void clear() { done.clear(); cur = new ArrayList<>(); }

    public void type(char jamo) { cur.add(jamo); }

    /** @return false면 지울 조합 글자가 없음 (입력창 글자를 지워야 함). */
    public boolean backspace() {
        if (!cur.isEmpty()) { cur.remove(cur.size() - 1); return true; }
        if (!done.isEmpty()) {
            // 띄어쓰기를 지우면 앞 낱말이 다시 현대 한글로 돌아와 고칠 수 있게 됨
            cur = new ArrayList<>(done.remove(done.size() - 1).keys);
            return true;
        }
        return false;
    }

    public SpaceResult space() {
        if (!cur.isEmpty()) { done.add(new Word(cur)); cur = new ArrayList<>(); return SpaceResult.WORD_ADDED; }
        if (!done.isEmpty()) return SpaceResult.FLUSH;
        return SpaceResult.NOTHING;
    }

    /** 지금 낱말을 바꾸지 않고 그대로 둠 (후보 줄의 '그대로'). */
    public void keepCurrentRaw() {
        if (cur.isEmpty()) return;
        Word w = new Word(cur); w.raw = true;
        done.add(w);
        cur = new ArrayList<>();
    }

    /** 입력창에 밑줄로 보일 글자. */
    public String composing() {
        String d = render(done);
        String sep = done.isEmpty() ? "" : " ";
        return d + sep + (cur.isEmpty() ? "" : HangulComposer.compose(cur));
    }

    /** 모두 확정: 지금 낱말까지 바꿔서 돌려주고 비움. */
    public String finishAll() {
        boolean trailingSpace = cur.isEmpty() && !done.isEmpty();
        if (!cur.isEmpty()) done.add(new Word(cur));
        String s = render(done) + (trailingSpace ? " " : "");
        clear();
        return s;
    }

    /** 후보 줄: [옛 표기 미리 보기, 그대로]. */
    public List<String> candidates() {
        if (cur.isEmpty() || !oldMode) return Collections.emptyList();
        List<Word> all = new ArrayList<>(done);
        all.add(new Word(cur));
        String full = render(all);
        int sp = full.lastIndexOf(' ');
        String last = sp >= 0 ? full.substring(sp + 1) : full;
        String modern = HangulComposer.compose(cur);
        List<String> out = new ArrayList<>();
        out.add(last);
        if (!last.equals(modern)) out.add(modern + " 그대로");
        return out;
    }

    private String render(List<Word> words) {
        StringBuilder sb = new StringBuilder();
        StringBuilder seg = new StringBuilder();
        for (Word w : words) {
            if (w.raw || !oldMode) {
                flushSeg(sb, seg);
                if (sb.length() > 0) sb.append(' ');
                sb.append(w.modern());
            } else {
                if (seg.length() > 0) seg.append(' ');
                seg.append(w.modern());
            }
        }
        flushSeg(sb, seg);
        return sb.toString();
    }

    private void flushSeg(StringBuilder sb, StringBuilder seg) {
        if (seg.length() == 0) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(conv.convert(seg.toString()));
        seg.setLength(0);
    }
}
