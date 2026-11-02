package com.yetgeul.keyboard.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 현대 한글 → 15세기 중세국어 표기 변환기. 안드로이드에 의존하지 않는 순수 자바. */
public final class Converter {

    /** 켜고 끌 수 있는 규칙. 기본값은 모두 켜짐. */
    public static final class Options {
        public boolean araea = true;      // 아래아 ㆍ (하→ᄒᆞ, 둘째 음절 ㅡ→ㆍ, 물→믈)
        public boolean pansios = true;    // 반치음 ㅿ
        public boolean yesieung = true;   // 받침 ㅇ → ㆁ
        public boolean yeorin = true;     // ᄒᆞᇙ (관형형 ㄹ + ㆆ)
        public boolean beop = true;       // 순경음 비읍 ㅸ (더워→더ᄫᅥ)
        public boolean hapyong = true;    // 된소리 → 합용병서 (ㄲ→ᄭ)
        public boolean yeoneol = true;    // 이어적기 (사람이→사ᄅᆞ미)
        public boolean palJong = true;    // 8종성 (꽃→곳, 있다→잇다)
        public boolean grammar = true;    // 옛 문법·말투 (합니다→ᄒᆞᄂᆞ이다, 과 달라→에 달아)
    }

    private final Options o;

    public Converter(Options options) { this.o = options; }

    public Converter() { this(new Options()); }

    // ───────────────────────── 문장 단위

    private static final Pattern WORD = Pattern.compile("[\\uAC00-\\uD7A3\\u1100-\\u11FF\\uA960-\\uA97F\\uD7B0-\\uD7FF]+");

    public String convert(String text) {
        if (o.grammar) text = phraseRules(text);
        List<String> parts = new ArrayList<>();   // 낱말과 그 사이 글자를 번갈아
        List<Boolean> isWord = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) { parts.add(text.substring(last, m.start())); isWord.add(false); }
            parts.add(m.group()); isWord.add(true);
            last = m.end();
        }
        if (last < text.length()) { parts.add(text.substring(last)); isWord.add(false); }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (!isWord.get(i)) { sb.append(parts.get(i)); continue; }
            List<Syl> syls = convertWord(parts.get(i));
            // ᄒᆞᇙ: 'ᄒᆞᆯ'로 끝나고 띄어 쓴 다음 낱말이 이어지면
            if (o.yeorin && o.araea && !syls.isEmpty() && i + 2 < parts.size()
                    && " ".equals(parts.get(i + 1)) && isWord.get(i + 2)) {
                Syl z = syls.get(syls.size() - 1);
                if (z.isHangul() && z.l == Jamo.L_H && z.v == Jamo.V_ARAEA && z.t == Jamo.T_R) z.t = Jamo.T_R_YEORIN;
            }
            for (Syl s : syls) sb.append(render(s));
        }
        return sb.toString();
    }

    private static final Pattern P_DIFFER = Pattern.compile("([가-힣]+?)(과|와)(\\s+)(다르|달라|달리|다른|다릅|다를|달랐)");
    private static final Pattern P_NEG = Pattern.compile("([가-힣]+?)지(\\s+)(않|아니)");
    private static final Pattern P_WANT = Pattern.compile("([가-힣]+?)고(\\s+)싶(습니다|어요|으면|었다|다|어|은)");
    private static final Pattern P_WILL = Pattern.compile("([가-힣]*)([가-힣])\\s+(것입니다|것이에요|거예요|것이다|거야|거다)");

    /** 낱말 경계를 넘는 옛 문법 패턴. */
    static String phraseRules(String t) {
        t = P_DIFFER.matcher(t).replaceAll("$1에$3$4");
        t = P_NEG.matcher(t).replaceAll("$1디$2$3");
        Matcher m = P_WANT.matcher(t);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String e = m.group(3), r;
            switch (e) {
                case "습니다": r = "합니다"; break;
                case "어요": case "어": r = "하노라"; break;
                case "으면": r = "하면"; break;
                case "었다": r = "하얏다"; break;
                case "은": r = "한"; break;
                default: r = "하다";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "고져" + m.group(2) + r));
        }
        m.appendTail(sb);
        t = sb.toString();
        // 갈 것이다 → 가리라
        m = P_WILL.matcher(t);
        sb = new StringBuffer();
        while (m.find()) {
            char c = m.group(2).charAt(0);
            int x = c - 0xAC00;
            if (x % 28 == 8) { // 받침 ㄹ
                String base = m.group(1) + (char) (c - 8);
                String end = m.group(3).startsWith("것입") || m.group(3).endsWith("요") ? "리이다" : "리라";
                m.appendReplacement(sb, Matcher.quoteReplacement(base + end));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ───────────────────────── 낱말 단위

    private static final Set<String> GA_EXCEPT = new HashSet<>(Arrays.asList(
            "휴가", "요가", "대가", "고가", "시가", "주가", "사가", "가가", "노가", "부가", "지가", "도가", "자가", "어디가"));

    public List<Syl> convertWord(String word) {
        List<Syl> s = Jamo.parse(word, true);
        int n0 = s.size();
        if (o.grammar) s = endings(s);
        s = dictionary(s, word, n0);
        if (o.grammar) subjectGa(s, word);
        for (int i = 0; i < s.size(); i++) {
            Syl a = s.get(i);
            if (!a.isHangul() || a.locked) continue;
            Syl prev = i > 0 ? s.get(i - 1) : null;
            Syl next = i + 1 < s.size() ? s.get(i + 1) : null;
            if (o.araea) {
                // 하→ᄒᆞ, 해→ᄒᆡ
                if (a.l == Jamo.L_H && a.v == Jamo.V_A) a.v = Jamo.V_ARAEA;
                else if (a.l == Jamo.L_H && a.v == Jamo.V_AE) a.v = Jamo.V_ARAEA_I;
                // 물→믈, 부르→브르
                if ((a.l == Jamo.L_M || a.l == Jamo.L_B || a.l == Jamo.L_P) && a.v == Jamo.V_U
                        && (a.t == Jamo.T_R || (a.t == 0 && next != null && next.isHangul() && next.l == Jamo.L_R && next.v == Jamo.V_EU)))
                    a.v = Jamo.V_EU;
                // 둘째 음절 이하: 양성모음 뒤 ㅡ→ㆍ, ㅢ→ㆎ (토씨·어미에 흔한 꼴만)
                if (prev != null && prev.isHangul() && Jamo.isYang(prev.v)
                        && (a.l == Jamo.L_R || a.l == Jamo.L_N || a.l == Jamo.L_NULL || a.l == Jamo.L_S || a.l == Jamo.L_M)
                        && (a.t == 0 || a.t == Jamo.T_N || a.t == Jamo.T_R || a.t == Jamo.T_M)) {
                    if (a.v == Jamo.V_EU) a.v = Jamo.V_ARAEA;
                    else if (a.v == Jamo.V_UI) a.v = Jamo.V_ARAEA_I;
                }
            }
            // 더워→더ᄫᅥ, 도와→도ᄫᅡ
            if (o.beop && prev != null && prev.isHangul() && prev.t == 0 && a.l == Jamo.L_NULL) {
                if (a.v == Jamo.V_WEO) { a.l = Jamo.L_BEOP; a.v = Jamo.V_EO; }
                else if (a.v == Jamo.V_WA && prev.v == Jamo.V_O) { a.l = Jamo.L_BEOP; a.v = Jamo.V_A; }
            }
        }
        if (o.yeoneol) linking(s);
        if (o.hapyong) {
            for (Syl a : s) {
                if (!a.isHangul() || a.locked) continue;
                switch (a.l) {
                    case Jamo.L_GG: a.l = Jamo.L_SG; break;
                    case Jamo.L_DD: a.l = Jamo.L_SD; break;
                    case Jamo.L_BB: a.l = Jamo.L_SB; break;
                    case Jamo.L_SS: a.l = Jamo.L_BS; break;
                    case Jamo.L_JJ: a.l = Jamo.L_SJ; break;
                    default:
                }
            }
        }
        if (o.palJong) eightFinals(s);
        if (o.yesieung) for (Syl a : s) if (a.isHangul() && a.t == Jamo.T_NG) a.t = Jamo.T_YESIEUNG;
        return s;
    }

    /** 문장 끝 말투: 합니다→ᄒᆞᄂᆞ이다, 하세요→ᄒᆞ쇼셔, 한다→ᄒᆞᄂᆞ다 등. */
    private List<Syl> endings(List<Syl> s) {
        int n = s.size();
        String w = modern(s);
        String[][] fixed = {
                {"입니다", "이{ㆁㅣ}다"}, {"입니까", "이{ㆁㅣ}잇가"},
                {"습니다", "{ㄴㆍ}이다"}, {"습니까", "{ㄴㆍ}니잇가"},
                {"으십시오", "으{ㅅㅛ}{ㅅㅕ}"}, {"십시오", "{ㅅㅛ}{ㅅㅕ}"},
                {"으세요", "으{ㅅㅛ}{ㅅㅕ}"}, {"세요", "{ㅅㅛ}{ㅅㅕ}"},
                {"으므로", "을{ㅆㆎ}"}, {"으니까", "으니"}, {"는다", "{ㄴㆍ}다"},
        };
        for (String[] f : fixed) {
            if (w.endsWith(f[0]) && w.length() > f[0].length()) return replaceTail(s, f[0].length(), f[1], null);
        }
        if (n >= 3) {
            Syl p = s.get(n - 3);
            if (p.isHangul() && !p.locked && p.t == Jamo.T_B) {
                if (w.endsWith("니다")) return replaceTail(s, 2, "{ㄴㆍ}이다", p);
                if (w.endsWith("니까")) return replaceTail(s, 2, "{ㄴㆍ}니잇가", p);
            }
        }
        if (n >= 2 && w.endsWith("므로")) {
            Syl p = s.get(n - 3 >= 0 ? n - 3 : 0);
            if (n >= 3 && p.isHangul() && p.t == 0) { p.t = Jamo.T_R; return replaceTail(s, 2, "{ㅆㆎ}", null); }
        }
        if (n >= 3 && w.endsWith("니까")) return replaceTail(s, 2, "니", null);
        // 한다/간다 → ᄒᆞᄂᆞ다/가ᄂᆞ다 (받침 ㄴ + 다)
        if (n >= 2 && w.endsWith("다")) {
            Syl p = s.get(n - 2);
            if (p.isHangul() && !p.locked && p.t == Jamo.T_N && p.l != Jamo.L_NULL) return replaceTail(s, 1, "{ㄴㆍ}다", p);
        }
        return s;
    }

    /** 끝 k음절을 spec으로 바꾸고 잠금. stripT가 있으면 그 음절의 받침을 뗌. */
    private static int groupSeq = 0;

    private static List<Syl> replaceTail(List<Syl> s, int k, String spec, Syl stripT) {
        if (stripT != null) stripT.t = 0;
        List<Syl> out = new ArrayList<>(s.subList(0, s.size() - k));
        int g = ++groupSeq;
        for (Syl x : Jamo.parseSpec(spec)) { x.group = g; out.add(x.lock()); }
        return out;
    }

    private static String modern(List<Syl> s) {
        StringBuilder sb = new StringBuilder();
        for (Syl x : s) sb.append(x.isHangul() && !x.locked && Jamo.compose(x.l, x.v, x.t).length() == 1 ? Jamo.compose(x.l, x.v, x.t) : "\u0000");
        return sb.toString();
    }

    private List<Syl> dictionary(List<Syl> s, String word, int originalLen) {
        String m = modern(s);
        List<Syl> out = new ArrayList<>();
        int i = 0;
        while (i < s.size()) {
            Dict.Entry hit = null;
            for (Dict.Entry e : Dict.ENTRIES) {
                if (!m.startsWith(e.modern, i)) continue;
                if (e.kind == Dict.Kind.ANY && e.modern.length() < 2) continue;
                if (e.kind != Dict.Kind.ANY && i != 0) continue;
                if (e.kind == Dict.Kind.NOUN) {
                    String rest = word.substring(Math.min(word.length(), e.modern.length()));
                    if (!Arrays.asList(Dict.PARTICLES).contains(rest)) continue;
                }
                hit = e;
                break;
            }
            if (hit == null) { out.add(s.get(i)); i++; continue; }
            List<Syl> rep = Jamo.parseSpec(hit.spec);
            int g = ++groupSeq;
            for (Syl x : rep) { x.group = g; out.add(x.lock()); }
            i += hit.modern.length();
        }
        return out;
    }

    /** 주격 '가' → 앞 음절에 ㅣ를 합침 (내가→내, 소리가→소리). */
    private void subjectGa(List<Syl> s, String word) {
        int n = s.size();
        if (n < 2 || GA_EXCEPT.contains(word)) return;
        Syl z = s.get(n - 1), p = s.get(n - 2);
        if (!z.isHangul() || z.locked || z.l != Jamo.L_G || z.v != Jamo.V_A || z.t != 0) return;
        if (!p.isHangul() || p.t != 0) return;
        int v = Jamo.addI(p.v);
        if (v == 0) return;
        p.v = v;
        s.remove(n - 1);
    }

    /** 이어적기: 받침 + ㅇ으로 시작하는 음절 → 받침을 다음 음절 초성으로. */
    private static void linking(List<Syl> s) {
        for (int i = 0; i + 1 < s.size(); i++) {
            Syl a = s.get(i), b = s.get(i + 1);
            if (!a.isHangul() || !b.isHangul() || a.t == 0 || b.l != Jamo.L_NULL) continue;
            if (a.t == Jamo.T_NG || a.t == Jamo.T_YESIEUNG || a.noLink) continue;
            if (a.group != 0 && a.group == b.group) continue;
            int[] sp = Jamo.splitCluster(a.t);
            if (sp != null) { a.t = sp[0]; b.l = sp[1]; continue; }
            int l = a.t == Jamo.T_SS ? Jamo.L_S : Jamo.tToL(a.t);
            if (l != 0) { b.l = l; a.t = 0; }
        }
    }

    /** 8종성법 + ㅎ 받침 거센소리되기 (둏다→됴타). */
    private static void eightFinals(List<Syl> s) {
        for (int i = 0; i < s.size(); i++) {
            Syl a = s.get(i);
            if (!a.isHangul() || a.t == 0) continue;
            Syl b = i + 1 < s.size() && s.get(i + 1).isHangul() ? s.get(i + 1) : null;
            int base = -1;
            if (a.t == Jamo.T_H) base = 0;
            else if (a.t == Jamo.T_NH) base = Jamo.T_N;
            else if (a.t == Jamo.T_RH) base = Jamo.T_R;
            if (base >= 0) {
                if (b != null) {
                    if (b.l == Jamo.L_G) b.l = Jamo.L_K;
                    else if (b.l == Jamo.L_D) b.l = Jamo.L_T;
                    else if (b.l == Jamo.L_J) b.l = Jamo.L_CH;
                    else if (b.l == Jamo.L_B) b.l = Jamo.L_P;
                }
                // 낱말 끝 ㅎ (ᄯᅡㅎ)은 토씨 없이 쓰면 적지 않음
                a.t = base;
                continue;
            }
            switch (a.t) {
                case Jamo.T_J: case Jamo.T_CH: case Jamo.T_SS: a.t = Jamo.T_S; break;
                case Jamo.T_T: a.t = Jamo.T_D; break;
                case Jamo.T_K: case Jamo.T_GG: case Jamo.T_GS: a.t = Jamo.T_G; break;
                case Jamo.T_P: case Jamo.T_BS: a.t = Jamo.T_B; break;
                case Jamo.T_NJ: a.t = Jamo.T_N; break;
                case Jamo.T_RS: case Jamo.T_RT: case Jamo.T_RP: a.t = Jamo.T_R; break;
                default:
            }
        }
    }

    // ───────────────────────── 출력 (꺼진 규칙은 여기서 현대 글자로 되돌림)

    private String render(Syl s) {
        if (!s.isHangul()) return s.raw;
        int l = s.l, v = s.v, t = s.t;
        if (!o.araea) { if (v == Jamo.V_ARAEA) v = Jamo.V_A; else if (v == Jamo.V_ARAEA_I) v = Jamo.V_AE; }
        if (!o.pansios) { if (l == Jamo.L_PANSIOS) l = Jamo.L_NULL; if (t == Jamo.T_PANSIOS) t = Jamo.T_S; }
        if (!o.yesieung) { if (l == Jamo.L_YESIEUNG) l = Jamo.L_NULL; if (t == Jamo.T_YESIEUNG) t = Jamo.T_NG; }
        if (!o.yeorin && t == Jamo.T_R_YEORIN) t = Jamo.T_R;
        if (!o.beop && l == Jamo.L_BEOP) {
            l = Jamo.L_NULL;
            if (v == Jamo.V_EO) v = Jamo.V_WEO; else if (v == Jamo.V_A) v = Jamo.V_WA;
            else if (v == Jamo.V_ARAEA) v = Jamo.V_O; else if (v == Jamo.V_EU) v = Jamo.V_U;
            else if (v == Jamo.V_ARAEA_I) v = Jamo.V_OE; else if (v == Jamo.V_I) v = Jamo.V_WI;
        }
        if (!o.hapyong) {
            switch (l) {
                case Jamo.L_SG: case 0x1122: l = Jamo.L_GG; break;
                case Jamo.L_SD: case 0x1120: case 0x1123: l = Jamo.L_DD; break;
                case Jamo.L_SB: l = Jamo.L_BB; break;
                case Jamo.L_BS: l = Jamo.L_SS; break;
                case Jamo.L_SJ: case 0x1127: l = Jamo.L_JJ; break;
                default:
            }
        }
        return Jamo.compose(l, v, t);
    }
}
