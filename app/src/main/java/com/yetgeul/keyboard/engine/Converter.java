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
        public boolean compat = false;    // 카톡 호환: 받침이 붙는 옛 글자는 현대 글자로 (받침 깨짐 방지)
    }

    private final Options o;

    public Converter(Options options) { this.o = options; }

    public Converter() { this(new Options()); }

    // ───────────────────────── 문장 단위

    private static final Pattern WORD = Pattern.compile("[\\uAC00-\\uD7A3\\u1100-\\u11FF\\uA960-\\uA97F\\uD7B0-\\uD7FF]+");

    /** 물음말(어디, 무엇...)이 있는 물음이면 -뇨/-고, 없으면 -녀/-가. */
    private static final Pattern WH = Pattern.compile("(^|[^가-힣])(어디|무엇|뭐|누구|누가|언제|어떤|어떻게|어찌|왜|몇|어느|무슨)");
    private boolean wh;

    public String convert(String text) {
        wh = WH.matcher(text).find();
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
            boolean question = i + 1 < parts.size() && parts.get(i + 1).trim().startsWith("?");
            List<Syl> syls = convertWord(parts.get(i), question);
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
    private static final Pattern P_NEG = Pattern.compile("([가-힣]+?)지(\\s+)(않|아니|못)");
    /** 아는 줄 알고 → ᄃᆞᆯ (매인이름씨 ᄃᆞᆯ) */
    private static final Pattern P_JUL = Pattern.compile("([가-힣]+)(\\s+)줄(을)?(?=\\s+(알|몰|모르))");
    private static final Pattern P_WANT = Pattern.compile("([가-힣]+?)고(\\s+)싶(습니다|어요|으면|었다|다|어|은)");
    private static final Pattern P_WILL = Pattern.compile("([가-힣]*)([가-힣])\\s+(것입니다|것이에요|거예요|것이다|거야|거다)");

    /** 낱말 경계를 넘는 옛 문법 패턴. */
    static String phraseRules(String t) {
        t = P_DIFFER.matcher(t).replaceAll("$1에$3$4");
        t = P_NEG.matcher(t).replaceAll("$1디$2$3");
        Matcher mj = P_JUL.matcher(t);
        StringBuffer sj = new StringBuffer();
        while (mj.find()) {
            char c = mj.group(1).charAt(mj.group(1).length() - 1);
            int fin = c >= 0xAC00 && c <= 0xD7A3 ? (c - 0xAC00) % 28 : 0;
            String rep = mj.group();
            if (fin == 4 || fin == 8) rep = mj.group(1) + mj.group(2) + (mj.group(3) != null ? "\u1103\u119E\u1105\u119E\u11AF" : "\u1103\u119E\u11AF");
            mj.appendReplacement(sj, Matcher.quoteReplacement(rep));
        }
        mj.appendTail(sj);
        t = sj.toString();
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

    public List<Syl> convertWord(String word) { return convertWord(word, false); }

    public List<Syl> convertWord(String word, boolean question) {
        List<Syl> s = Jamo.parse(word, true);
        int n0 = s.size();
        if (o.grammar) s = endings(s, question);
        s = dictionary(s, word, n0);
        if (o.grammar) { afterStem(s); nominal(s, word); subjectGa(s, word); particles(s); }
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
    private List<Syl> endings(List<Syl> s, boolean q) {
        int n = s.size();
        String w = modern(s);
        String polite = q ? "{ㄴㆍ}니잇가" : "{ㄴㆍ}이다";
        // 물음: 입니까/습니까 + 해요체 물음
        if (q) {
            if (w.endsWith("이에요") && n > 3) return replaceTail(s, 3, "이니잇가", null);
            // 명사 물음: 사람이냐 → 사ᄅᆞᆷ가 / (물음말 있으면) 사ᄅᆞᆷ고
            if (n > 2 && (w.endsWith("이야") || w.endsWith("이냐") || w.endsWith("이니"))) return replaceTail(s, 2, wh ? "고" : "가", null);
            for (String[] f : new String[][]{{"겠습니까", "리잇가"}, {"겠어요", "리잇가"}, {"겠니", "리여"}, {"겠냐", "리여"}, {"겠어", "리여"}})
                if (w.endsWith(f[0]) && w.length() > f[0].length()) return replaceTail(s, f[0].length(), f[1], null);
            String ny = wh ? "{ㄴㆍ}뇨" : "{ㄴㆍ}녀";
            if (n >= 3 && (w.endsWith("느냐") || w.endsWith("느니"))) return replaceTail(s, 2, ny, null);
            if (n >= 2 && (w.endsWith("니") || w.endsWith("냐")) && !w.endsWith("습니") && !w.endsWith("ㅂ니")) {
                Syl p = s.get(n - 2);
                if (p.isHangul() && !p.locked && p.t != Jamo.T_B) return replaceTail(s, 1, ny, null);
            }
        }
        String[][] fixed = new String[][]{
                {"입니다", "이{ㆁㅣ}다"}, {"입니까", "이{ㆁㅣ}잇가"},
                {"습니다", "{ㄴㆍ}이다"}, {"습니까", "{ㄴㆍ}니잇가"},
                {"으십시오", "으{ㅅㅛ}{ㅅㅕ}"}, {"십시오", "{ㅅㅛ}{ㅅㅕ}"},
                {"으세요", "으{ㅅㅛ}{ㅅㅕ}"}, {"세요", "{ㅅㅛ}{ㅅㅕ}"},
                {"으므로", "을{ㅆㆎ}"}, {"으니까", "으니"}, {"는다", "{ㄴㆍ}다"},
                {"겠습니다", "리이다"}, {"겠어요", "리이다"}, {"겠다", "리라"}, {"겠어", "리라"},
                {"시어서", "{ㅅㅑ}"}, {"시어", "{ㅅㅑ}"},
        };
        if (q) {
            fixed = new String[][]{
                    {"입니다", "이니잇가"}, {"입니까", "이니잇가"}, {"습니다", "{ㄴㆍ}니잇가"}, {"습니까", "{ㄴㆍ}니잇가"},
                    {"으세요", "으시니잇가"}, {"세요", "시니잇가"}, {"는다", "{ㄴㆍ}니"}, {"시어", "{ㅅㅑ}"}};
        }
        for (String[] f : fixed) {
            if (w.endsWith(f[0]) && w.length() > f[0].length()) return replaceTail(s, f[0].length(), f[1], null);
        }
        if (n >= 3) {
            Syl p = s.get(n - 3);
            if (p.isHangul() && !p.locked && p.t == Jamo.T_B) {
                if (w.endsWith("니다")) return replaceTail(s, 2, polite, p);
                if (w.endsWith("니까")) return replaceTail(s, 2, "{ㄴㆍ}니잇가", p);
            }
        }
        if (n >= 2 && w.endsWith("므로")) {
            Syl p = s.get(n - 3 >= 0 ? n - 3 : 0);
            if (n >= 3 && p.isHangul() && p.t == 0) { p.t = Jamo.T_R; return replaceTail(s, 2, "{ㅆㆎ}", null); }
        }
        if (n >= 3 && w.endsWith("니까")) return replaceTail(s, 2, "니", null);
        // 할까 → ᄒᆞᆯ가
        if (n >= 2 && w.endsWith("까") && s.get(n - 2).isHangul() && s.get(n - 2).t == Jamo.T_R) return replaceTail(s, 1, wh ? "고" : "가", null);
        // 해요체: 이에요/예요, 해요, 먹어요, 가요, 했어요
        if (n >= 3 && w.endsWith("이에요")) return replaceTail(s, 3, q ? "이니잇가" : "이{ㆁㅣ}다", null);
        if (n >= 2 && w.endsWith("예요")) {
            Syl p = s.get(n - 3 >= 0 ? n - 3 : 0);
            if (n >= 3 && p.isHangul() && p.t == 0 && Jamo.addI(p.v) != 0) { p.v = Jamo.addI(p.v); return replaceTail(s, 2, q ? "니잇가" : "{ㆁㅣ}다", null); }
        }
        if (n >= 2 && w.endsWith("해요")) { s.get(n - 2).v = Jamo.V_A; return replaceTail(s, 1, polite, null); }
        if (n >= 3 && (w.endsWith("어요") || w.endsWith("아요"))) {
            Syl p = s.get(n - 3);
            boolean past = p.isHangul() && p.t == Jamo.T_SS;
            return replaceTail(s, 2, past ? (q ? "니잇가" : "니이다") : polite, null);
        }
        if (n >= 2 && w.endsWith("요")) {
            Syl p = s.get(n - 2);
            if (p.isHangul() && !p.locked && p.t == 0 && (p.v == Jamo.V_A || p.v == Jamo.V_EO || p.v == Jamo.V_YEO
                    || p.v == Jamo.V_WA || p.v == Jamo.V_WEO || p.v == Jamo.V_AE || p.v == Jamo.V_E))
                return replaceTail(s, 1, polite, null);
        }
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
            int carryT = 0;
            for (Dict.Entry e : Dict.ENTRIES) {
                carryT = 0;
                if (!m.startsWith(e.modern, i)) {
                    // VERB: 마지막 음절은 받침만 달라도 맞춤 (말한 → 말하 + ㄴ)
                    if (e.kind != Dict.Kind.VERB || i != 0) continue;
                    int k = e.modern.length();
                    if (k > m.length() || !m.startsWith(e.modern.substring(0, k - 1), 0)) continue;
                    char want = e.modern.charAt(k - 1), have = m.charAt(k - 1);
                    if (want < 0xAC00 || have < 0xAC00 || have > 0xD7A3 || (want - 0xAC00) % 28 != 0
                            || (have - 0xAC00) / 28 != (want - 0xAC00) / 28) continue;
                    carryT = 0x11A7 + (have - 0xAC00) % 28;
                }
                if (e.kind == Dict.Kind.ANY && e.modern.length() < 2) continue;
                if (e.kind != Dict.Kind.ANY && i != 0) continue;
                if (carryT != 0 && e.kind != Dict.Kind.VERB) continue;
                if (e.kind == Dict.Kind.NOUN) {
                    String rest = word.substring(Math.min(word.length(), e.modern.length()));
                    if (!Arrays.asList(Dict.PARTICLES).contains(rest)) continue;
                }
                hit = e;
                break;
            }
            if (hit == null) { out.add(s.get(i)); i++; continue; }
            List<Syl> rep = Jamo.parseSpec(hit.spec);
            if (carryT != 0) {
                Syl z = rep.get(rep.size() - 1);
                if (z.t == 0) z.t = carryT;
            }
            int g = ++groupSeq;
            for (Syl x : rep) { x.group = g; out.add(x.lock()); }
            i += hit.modern.length();
        }
        return out;
    }

    /** 사전으로 바뀐 줄기 뒤 정리: 하+으니 → 하니, 에게 모음조화, ㄹ 뒤 고 → 오. */
    private void afterStem(List<Syl> s) {
        for (int i = 1; i < s.size(); i++) {
            Syl a = s.get(i - 1), b = s.get(i);
            if (!a.isHangul() || !b.isHangul()) continue;
            // 많으니 → 하니, 많음 → 함 (사전 줄기가 모음으로 끝날 때 매개모음 으를 뺌)
            if (a.locked && a.t == 0 && !b.locked && b.l == Jamo.L_NULL && b.v == Jamo.V_EU) {
                if (b.t == 0 && i + 1 < s.size()) { s.remove(i); i--; continue; }
                if (b.t == Jamo.T_M && i + 1 == s.size()) { a.t = Jamo.T_M; s.remove(i); i--; continue; }
            }
            // 에게: 음성모음 뒤 의그ᅌᅦ, 양성모음 뒤 ᄋᆡ그ᅌᅦ
            if (b.locked && b.l == Jamo.L_NULL && b.v == Jamo.V_ARAEA_I && b.t == 0 && i + 1 < s.size()) {
                Syl c = s.get(i + 1);
                if (c.isHangul() && c.l == Jamo.L_G && c.v == Jamo.V_EU && c.group == b.group && !Jamo.isYang(a.v)) b.v = Jamo.V_UI;
            }
            // 알고 → 알오, 사람이고 → 사ᄅᆞ미오
            if (b.l == Jamo.L_G && b.v == Jamo.V_O && b.t == 0 && (a.group == 0 || a.group != b.group)) {
                boolean end = i + 1 == s.size() && !b.locked;   // 할고(물음)는 그대로
                if (i + 1 < s.size()) { Syl c = s.get(i + 1); end = c.isHangul() && c.l == Jamo.L_J && c.v == Jamo.V_YEO && c.t == 0; }
                if (!end) continue;
                if (a.t == Jamo.T_R) { b.l = Jamo.L_NULL; a.noLink = true; }
                else if (i >= 2 && !a.locked && a.l == Jamo.L_NULL && a.v == Jamo.V_I && a.t == 0) b.l = Jamo.L_NULL;
            }
        }
    }

    private static final Set<String> NOMINAL_EXCEPT = new HashSet<>(Arrays.asList(
            "게임", "모임", "책임", "담임", "주임", "위임", "일임", "처임", "기름", "그믐", "고름", "거름", "소름",
            "녹음", "발음", "복음", "잡음", "탁음", "격음", "독음", "축음", "직음", "얼음"));

    /** 이름씨 꼴 -ㅁ/-음 (음슴체): 먹음 → 머굼, 좋음 → 됴홈, 있음 → 이숌, 함 → ᄒᆞ욤, 학생임 → 학ᄉᆡᇰ이롬. */
    private void nominal(List<Syl> s, String word) {
        int n = s.size();
        if (n == 0) return;
        int k = n - 1;
        Syl z = s.get(k);
        if (!z.isHangul()) return;
        // 뒤에 토씨가 붙은 꼴: 씀에, 웃음을, 학생임을
        if (n >= 2 && !z.locked && ((z.l == Jamo.L_NULL && z.t == 0 && (z.v == Jamo.V_E || z.v == Jamo.V_I || z.v == Jamo.V_UI))
                || (z.l == Jamo.L_NULL && z.v == Jamo.V_EU && (z.t == Jamo.T_R || z.t == Jamo.T_N))
                || (z.l == Jamo.L_G && z.v == Jamo.V_WA && z.t == 0) || (z.l == Jamo.L_D && z.v == Jamo.V_O && z.t == 0))) {
            k = n - 2; z = s.get(k);
        }
        String core = word.substring(0, Math.max(0, word.length() - (n - 1 - k)));
        if (NOMINAL_EXCEPT.contains(core) || !z.isHangul() || z.t == 0) return;
        Syl p = k > 0 && s.get(k - 1).isHangul() ? s.get(k - 1) : null;
        boolean yang = p != null && Jamo.isYang(p.v);
        // ᄒᆞ+옴 → ᄒᆞ욤 (함, 공부함, 편안함)
        if (z.l == Jamo.L_H && (z.v == Jamo.V_A || (z.locked && z.v == Jamo.V_ARAEA)) && z.t == Jamo.T_M && k >= 1) {
            z.t = 0; z.v = Jamo.V_ARAEA; z.locked = true;
            s.add(k + 1, new Syl(Jamo.L_NULL, Jamo.V_YO, Jamo.T_M).lock());
            return;
        }
        if (z.locked) return;
        // 됨 → ᄃᆞ외욤
        if (z.l == Jamo.L_D && z.v == Jamo.V_OE && z.t == Jamo.T_M) {
            z.v = Jamo.V_ARAEA; z.t = 0; z.locked = true;
            s.add(k + 1, new Syl(Jamo.L_NULL, Jamo.V_OE, 0).lock());
            s.add(k + 2, new Syl(Jamo.L_NULL, Jamo.V_YO, Jamo.T_M).lock());
            return;
        }
        // 아님 → 아니롬
        if (k == 1 && p != null && !p.locked && p.l == Jamo.L_NULL && p.v == Jamo.V_A && p.t == 0
                && z.l == Jamo.L_N && z.v == Jamo.V_I && z.t == Jamo.T_M) {
            z.t = 0;
            s.add(k + 1, new Syl(Jamo.L_R, Jamo.V_O, Jamo.T_M).lock());
            return;
        }
        // 학생임 → 학ᄉᆡᇰ이롬, 나무임 → 나모ㅣ롬
        if (z.l == Jamo.L_NULL && z.v == Jamo.V_I && z.t == Jamo.T_M && p != null) {
            if (p.t == 0 && Jamo.addI(p.v) != 0) { p.v = Jamo.addI(p.v); z.l = Jamo.L_R; z.v = Jamo.V_O; return; }
            z.t = 0;
            s.add(k + 1, new Syl(Jamo.L_R, Jamo.V_O, Jamo.T_M).lock());
            return;
        }
        // 삶 → 사롬, 앎 → 아롬
        if (z.t == Jamo.T_RM) {
            z.t = Jamo.T_R;
            s.add(k + 1, new Syl(Jamo.L_NULL, Jamo.isYang(z.v) ? Jamo.V_O : Jamo.V_U, Jamo.T_M));
            return;
        }
        if (z.v != Jamo.V_EU || z.t != Jamo.T_M) return;
        boolean eum = z.l == Jamo.L_NULL || z.l == Jamo.L_S;   // 음, 슴
        if (eum && p != null && p.t != 0) {
            if (p.t == Jamo.T_SS || (p.locked && p.t == Jamo.T_S)) {          // 있음 → 이숌, 했음 → ᄒᆞ야숌
                p.t = 0; z.l = Jamo.L_S; z.v = Jamo.V_YO;
                if (!p.locked && p.l == Jamo.L_H && p.v == Jamo.V_AE) {        // 망했음 → 마ᇰᄒᆞ야숌
                    p.v = Jamo.V_ARAEA; p.locked = true;
                    s.add(k, new Syl(Jamo.L_NULL, Jamo.V_YA, 0).lock());
                }
                return;
            }
            if (p.t == Jamo.T_BS) { p.t = Jamo.T_B; z.l = Jamo.L_S; z.v = Jamo.V_U; return; }   // 없음 → 업숨
            if (z.l == Jamo.L_NULL && p.t != Jamo.T_NG) z.v = yang ? Jamo.V_O : Jamo.V_U;   // 먹음 → 머굼
            return;
        }
        if (eum) return;   // 다음, 가슴 등 (받침 없는 말 + 음)
        // 씀 → ᄡᅮᆷ, 슬픔 → 슬품, 다름 → 다롬, 모름 → 몰롬
        if ("모름".equals(core) && p != null) { p.t = Jamo.T_R; z.v = Jamo.V_O; return; }
        z.v = yang ? Jamo.V_O : Jamo.V_U;
    }

    /** 주격 '가' → 앞 음절에 ㅣ를 합침 (내가→내, 소리가→소리). */
    private void subjectGa(List<Syl> s, String word) {
        int n = s.size();
        if (n < 2 || GA_EXCEPT.contains(word)) return;
        Syl z = s.get(n - 1), p = s.get(n - 2);
        if (!z.isHangul() || z.locked || z.l != Jamo.L_G || z.v != Jamo.V_A || z.t != 0) return;
        if (!p.isHangul()) return;
        if (p.t != 0) { z.l = Jamo.L_NULL; z.v = Jamo.V_I; return; }  // ㅎ 끝소리 명사: 코가 → 고히
        int v = Jamo.addI(p.v);
        if (v == 0) return;
        p.v = v;
        s.remove(n - 1);
    }

    /** 조사 고르기: 에/애/예, 에서→애셔, 받침 ㄹ 뒤 과→와, 께→ᄭᅴ. */
    private void particles(List<Syl> s) {
        int n = s.size();
        if (n < 2) return;
        Syl z = s.get(n - 1);
        if (!z.isHangul() || z.locked) return;
        // 에서 → 에셔 (앞에서 에를 고름)
        int ei = -1;
        if (z.l == Jamo.L_NULL && z.v == Jamo.V_E && z.t == 0) ei = n - 1;
        else if (n >= 3 && z.l == Jamo.L_S && z.v == Jamo.V_EO && z.t == 0) {
            Syl y = s.get(n - 2);
            if (y.isHangul() && !y.locked && y.l == Jamo.L_NULL && y.v == Jamo.V_E && y.t == 0) { ei = n - 2; z.v = Jamo.V_YEO; }
        }
        if (ei > 0) {
            Syl e = s.get(ei), p = s.get(ei - 1);
            if (p.isHangul()) {
                if (p.t == 0 && endsInI(p.v)) e.v = Jamo.V_YE;
                else if (Jamo.isYang(p.v)) e.v = Jamo.V_AE;
            }
            return;
        }
        if (n >= 3 && z.l == Jamo.L_R && z.v == Jamo.V_O && z.t == 0) {   // 뫼으로 → 뫼로
            Syl y = s.get(n - 2), x = s.get(n - 3);
            if (!y.locked && y.l == Jamo.L_NULL && y.v == Jamo.V_EU && y.t == 0 && x.isHangul() && x.locked && x.t == 0) {
                s.remove(n - 2); return;
            }
        }
        Syl p = s.get(n - 2);
        if (!p.isHangul()) return;
        // 사전으로 앞말의 받침이 바뀌었으면 조사 짝을 맞춤 (친구를 → 벋을, 여자와 → 겨집과)
        if (p.locked && n >= 2) {
            boolean cons = p.t != 0;
            if (cons) {
                if (z.l == Jamo.L_R && z.v == Jamo.V_EU && z.t == Jamo.T_R) z.l = Jamo.L_NULL;           // 를→을
                else if (z.l == Jamo.L_N && z.v == Jamo.V_EU && z.t == Jamo.T_N) z.l = Jamo.L_NULL;      // 는→은
                else if (z.l == Jamo.L_NULL && z.v == Jamo.V_WA && z.t == 0 && p.t != Jamo.T_R) z.l = Jamo.L_G; // 와→과
                else if (z.l == Jamo.L_R && z.v == Jamo.V_O && z.t == 0 && p.t != Jamo.T_R) {           // 로→으로
                    s.add(n - 1, new Syl(Jamo.L_NULL, Jamo.V_EU, 0)); n++; z = s.get(n - 1);
                }
            } else {
                if (z.l == Jamo.L_NULL && z.v == Jamo.V_EU && z.t == Jamo.T_R) z.l = Jamo.L_R;           // 을→를
                else if (z.l == Jamo.L_NULL && z.v == Jamo.V_EU && z.t == Jamo.T_N) z.l = Jamo.L_N;      // 은→는
                else if (z.l == Jamo.L_G && z.v == Jamo.V_WA && z.t == 0) z.l = Jamo.L_NULL;             // 과→와
                else if (z.l == Jamo.L_NULL && z.v == Jamo.V_I && z.t == 0 && Jamo.addI(p.v) != 0) {     // 이→ㅣ
                    p.v = Jamo.addI(p.v); s.remove(n - 1); return;
                }
            }
        }
        if (z.l == Jamo.L_G && z.v == Jamo.V_WA && z.t == 0 && (p.t == Jamo.T_R || p.t == Jamo.T_R_YEORIN)) { z.l = Jamo.L_NULL; p.noLink = true; return; }
        if (z.l == Jamo.L_GG && z.v == Jamo.V_E && z.t == 0) { z.l = Jamo.L_SG; z.v = Jamo.V_UI; z.locked = true; }
    }

    private static boolean endsInI(int v) {
        return v == Jamo.V_I || v == Jamo.V_AE || v == Jamo.V_E || v == Jamo.V_OE || v == Jamo.V_WI || v == Jamo.V_UI
                || v == Jamo.V_ARAEA_I || v == Jamo.V_YAE || v == Jamo.V_YE || v == Jamo.V_WAE || v == Jamo.V_WE;
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
        if (o.compat && t != 0 && Jamo.compose(l, v, t).length() > 1) {
            // 받침이 붙은 옛 글자는 현대 글자로 (카톡 등 받침 조합이 깨지는 앱용)
            switch (l) {
                case Jamo.L_SG: case 0x1122: l = Jamo.L_GG; break;
                case Jamo.L_SD: case 0x1120: case 0x1123: l = Jamo.L_DD; break;
                case Jamo.L_SB: l = Jamo.L_BB; break;
                case Jamo.L_BS: l = Jamo.L_SS; break;
                case Jamo.L_SJ: case 0x1127: l = Jamo.L_JJ; break;
                case Jamo.L_PANSIOS: case Jamo.L_YESIEUNG: case Jamo.L_YEORIN: l = Jamo.L_NULL; break;
                case Jamo.L_BEOP: l = Jamo.L_B; break;
                default:
            }
            if (v == Jamo.V_ARAEA) v = Jamo.V_A; else if (v == Jamo.V_ARAEA_I) v = Jamo.V_AE;
            else if (v == Jamo.V_YO_I) v = Jamo.V_YO; else if (v == Jamo.V_YU_I) v = Jamo.V_YU;
            if (t == Jamo.T_YESIEUNG) t = Jamo.T_NG; else if (t == Jamo.T_PANSIOS) t = Jamo.T_S;
            else if (t == Jamo.T_R_YEORIN) t = Jamo.T_R;
        }
        return Jamo.compose(l, v, t);
    }
}
