package com.yetgeul.keyboard.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 두벌식 자판 오토마타. 누른 자모(호환 자모, 옛 자모 포함) 목록을 글자로 조합. */
public final class HangulComposer {
    private HangulComposer() {}

    private static final Map<String, Character> VOWEL_PAIR = new HashMap<>();
    private static final Map<String, Character> FINAL_PAIR = new HashMap<>();
    private static final Map<Character, char[]> FINAL_SPLIT = new HashMap<>();

    static {
        String[][] vp = {{"ㅗㅏ", "ㅘ"}, {"ㅗㅐ", "ㅙ"}, {"ㅗㅣ", "ㅚ"}, {"ㅜㅓ", "ㅝ"}, {"ㅜㅔ", "ㅞ"}, {"ㅜㅣ", "ㅟ"},
                {"ㅡㅣ", "ㅢ"}, {"ㆍㅣ", "ㆎ"}, {"ㅛㅣ", "ㆉ"}, {"ㅠㅣ", "ㆌ"}};
        for (String[] p : vp) VOWEL_PAIR.put(p[0], p[1].charAt(0));
        String[][] fp = {{"ㄱㅅ", "ㄳ"}, {"ㄴㅈ", "ㄵ"}, {"ㄴㅎ", "ㄶ"}, {"ㄹㄱ", "ㄺ"}, {"ㄹㅁ", "ㄻ"}, {"ㄹㅂ", "ㄼ"},
                {"ㄹㅅ", "ㄽ"}, {"ㄹㅌ", "ㄾ"}, {"ㄹㅍ", "ㄿ"}, {"ㄹㅎ", "ㅀ"}, {"ㅂㅅ", "ㅄ"}, {"ㄹㆆ", "ㅭ"}};
        for (String[] p : fp) {
            FINAL_PAIR.put(p[0], p[1].charAt(0));
            FINAL_SPLIT.put(p[1].charAt(0), new char[]{p[0].charAt(0), p[0].charAt(1)});
        }
    }

    public static boolean isVowel(char c) { return Jamo.compatToV(c) != 0; }

    public static boolean isConsonant(char c) { return Jamo.isCompat(c) && !isVowel(c); }

    /** 키 목록 → 화면에 보일 글자. */
    public static String compose(List<Character> keys) {
        StringBuilder out = new StringBuilder();
        char cho = 0, jung = 0, jong = 0;
        for (char k : keys) {
            if (isVowel(k)) {
                if (cho != 0 && jung == 0) { jung = k; continue; }
                if (jung != 0 && jong == 0) {
                    Character pair = VOWEL_PAIR.get("" + jung + k);
                    if (pair != null) { jung = pair; continue; }
                }
                if (jong != 0) {
                    char[] sp = FINAL_SPLIT.get(jong);
                    char moved;
                    if (sp != null) { jong = sp[0]; moved = sp[1]; } else { moved = jong; jong = 0; }
                    out.append(syllable(cho, jung, jong));
                    cho = moved; jung = k; jong = 0;
                    continue;
                }
                out.append(syllable(cho, jung, jong));
                cho = 0; jung = k; jong = 0;
            } else if (isConsonant(k)) {
                if (cho != 0 && jung != 0 && jong == 0 && Jamo.compatToT(k) != 0) { jong = k; continue; }
                if (jong != 0) {
                    Character pair = FINAL_PAIR.get("" + jong + k);
                    if (pair != null) { jong = pair; continue; }
                }
                out.append(syllable(cho, jung, jong));
                cho = k; jung = 0; jong = 0;
            } else {
                out.append(syllable(cho, jung, jong)).append(k);
                cho = 0; jung = 0; jong = 0;
            }
        }
        out.append(syllable(cho, jung, jong));
        return out.toString();
    }

    private static String syllable(char cho, char jung, char jong) {
        if (cho == 0 && jung == 0) return "";
        if (jung == 0) return String.valueOf(cho);
        if (cho == 0) return String.valueOf(jung);
        return Jamo.compose(Jamo.compatToL(cho), Jamo.compatToV(jung), jong == 0 ? 0 : Jamo.compatToT(jong));
    }
}
