package com.yetgeul.keyboard.engine;

import java.util.ArrayList;
import java.util.List;

/** 한글 자모 도구: 분해, 조합, 호환 자모 변환, 받침 나누기. */
public final class Jamo {
    private Jamo() {}

    // 자주 쓰는 첫가끝 코드
    public static final int L_G = 0x1100, L_GG = 0x1101, L_N = 0x1102, L_D = 0x1103, L_DD = 0x1104, L_R = 0x1105,
            L_M = 0x1106, L_B = 0x1107, L_BB = 0x1108, L_S = 0x1109, L_SS = 0x110A, L_NULL = 0x110B, L_J = 0x110C,
            L_JJ = 0x110D, L_CH = 0x110E, L_K = 0x110F, L_T = 0x1110, L_P = 0x1111, L_H = 0x1112;
    public static final int L_SG = 0x112D, L_SD = 0x112F, L_SB = 0x1132, L_SJ = 0x1136, L_BS = 0x1121,
            L_BEOP = 0x112B, L_PANSIOS = 0x1140, L_YESIEUNG = 0x114C, L_YEORIN = 0x1159;
    public static final int V_A = 0x1161, V_AE = 0x1162, V_YA = 0x1163, V_YAE = 0x1164, V_EO = 0x1165, V_E = 0x1166,
            V_YEO = 0x1167, V_YE = 0x1168, V_O = 0x1169, V_WA = 0x116A, V_WAE = 0x116B, V_OE = 0x116C, V_YO = 0x116D,
            V_U = 0x116E, V_WEO = 0x116F, V_WE = 0x1170, V_WI = 0x1171, V_YU = 0x1172, V_EU = 0x1173, V_UI = 0x1174,
            V_I = 0x1175, V_ARAEA = 0x119E, V_ARAEA_I = 0x11A1, V_YO_I = 0x1188, V_YU_I = 0x1194;
    public static final int T_G = 0x11A8, T_GG = 0x11A9, T_GS = 0x11AA, T_N = 0x11AB, T_NJ = 0x11AC, T_NH = 0x11AD,
            T_D = 0x11AE, T_R = 0x11AF, T_RG = 0x11B0, T_RM = 0x11B1, T_RB = 0x11B2, T_RS = 0x11B3, T_RT = 0x11B4,
            T_RP = 0x11B5, T_RH = 0x11B6, T_M = 0x11B7, T_B = 0x11B8, T_BS = 0x11B9, T_S = 0x11BA, T_SS = 0x11BB,
            T_NG = 0x11BC, T_J = 0x11BD, T_CH = 0x11BE, T_K = 0x11BF, T_T = 0x11C0, T_P = 0x11C1, T_H = 0x11C2,
            T_YESIEUNG = 0x11F0, T_R_YEORIN = 0x11D9, T_PANSIOS = 0x11EB, T_YEORIN = 0x11F9;

    public static boolean isPrecomposed(int c) { return c >= 0xAC00 && c <= 0xD7A3; }
    public static boolean isL(int c) { return (c >= 0x1100 && c <= 0x115F) || (c >= 0xA960 && c <= 0xA97C); }
    public static boolean isV(int c) { return (c >= 0x1160 && c <= 0x11A7) || (c >= 0xD7B0 && c <= 0xD7C6); }
    public static boolean isT(int c) { return (c >= 0x11A8 && c <= 0x11FF) || (c >= 0xD7CB && c <= 0xD7FB); }
    public static boolean isCompat(int c) { return c >= 0x3131 && c <= 0x318E; }

    public static int compatToL(int c) { return isCompat(c) ? JamoTables.CHO[c - JamoTables.BASE] : 0; }
    public static int compatToV(int c) { return isCompat(c) ? JamoTables.JUNG[c - JamoTables.BASE] : 0; }
    public static int compatToT(int c) { return isCompat(c) ? JamoTables.JONG[c - JamoTables.BASE] : 0; }

    /** 종성 → 같은 소리의 초성 (이어적기용). 없으면 0. */
    public static int tToL(int t) {
        for (int i = 0; i < JamoTables.JONG.length; i++) {
            if (JamoTables.JONG[i] == t && JamoTables.CHO[i] != 0) return JamoTables.CHO[i];
        }
        return 0;
    }

    /** 겹받침 나누기: {앞 종성, 뒤 초성}. 겹받침이 아니면 null. */
    public static int[] splitCluster(int t) {
        switch (t) {
            case T_GS: return new int[]{T_G, L_S};
            case T_NJ: return new int[]{T_N, L_J};
            case T_NH: return new int[]{T_N, L_H};
            case T_RG: return new int[]{T_R, L_G};
            case T_RM: return new int[]{T_R, L_M};
            case T_RB: return new int[]{T_R, L_B};
            case T_RS: return new int[]{T_R, L_S};
            case T_RT: return new int[]{T_R, L_T};
            case T_RP: return new int[]{T_R, L_P};
            case T_RH: return new int[]{T_R, L_H};
            case T_BS: return new int[]{T_B, L_S};
            case T_R_YEORIN: return new int[]{T_R, L_YEORIN};
            default: return null;
        }
    }

    /** 현대 음절로 합칠 수 있으면 완성형 한 글자, 아니면 첫가끝 자모열. */
    public static String compose(int l, int v, int t) {
        StringBuilder sb = new StringBuilder();
        if (l >= L_G && l <= L_H && v >= V_A && v <= V_I && (t == 0 || (t >= T_G && t <= T_H))) {
            int code = 0xAC00 + ((l - L_G) * 21 + (v - V_A)) * 28 + (t == 0 ? 0 : t - 0x11A7);
            sb.appendCodePoint(code);
        } else {
            sb.appendCodePoint(l == 0 ? 0x115F : l);
            sb.appendCodePoint(v == 0 ? 0x1160 : v);
            if (t != 0) sb.appendCodePoint(t);
        }
        return sb.toString();
    }

    /** 문자열을 음절 단위로 나눔. 한글이 아닌 글자는 raw 음절로. */
    public static List<Syl> parse(String s, boolean lockArchaic) {
        List<Syl> out = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            int c = s.codePointAt(i);
            int len = Character.charCount(c);
            if (isPrecomposed(c)) {
                int x = c - 0xAC00;
                out.add(new Syl(L_G + x / 588, V_A + (x % 588) / 28, x % 28 == 0 ? 0 : 0x11A7 + x % 28));
                i += len;
            } else if (isL(c)) {
                int l = c, v = 0, t = 0;
                i += len;
                if (i < n && isV(s.codePointAt(i))) { v = s.codePointAt(i); i += Character.charCount(v); }
                if (i < n && isT(s.codePointAt(i))) { t = s.codePointAt(i); i += Character.charCount(t); }
                Syl sy = new Syl(l == 0x115F ? 0 : l, v == 0x1160 ? 0 : v, t);
                sy.locked = lockArchaic;
                out.add(sy);
            } else {
                out.add(Syl.raw(new String(Character.toChars(c))));
                i += len;
            }
        }
        return out;
    }

    /** 사전 표기 해석: 현대 글자는 그대로, {ㅅㆍㅁ}처럼 중괄호 안의 호환 자모는 옛 음절 하나. */
    public static List<Syl> parseSpec(String spec) {
        List<Syl> out = new ArrayList<>();
        int i = 0;
        while (i < spec.length()) {
            char ch = spec.charAt(i);
            if (ch == '{') {
                int end = spec.indexOf('}', i);
                String g = spec.substring(i + 1, end);
                int l = compatToL(g.charAt(0));
                int k = 1, v = 0, t = 0;
                if (k < g.length() && compatToV(g.charAt(k)) != 0) { v = compatToV(g.charAt(k)); k++; }
                if (k < g.length()) { t = compatToT(g.charAt(k)); k++; }
                if (l == 0 || v == 0 || k != g.length()) throw new IllegalArgumentException("bad spec: " + spec);
                out.add(new Syl(l, v, t));
                i = end + 1;
            } else if (ch == '|') {
                if (!out.isEmpty()) out.get(out.size() - 1).noLink = true;
                i++;
                continue;
            } else {
                out.addAll(parse(String.valueOf(ch), false));
                i++;
            }
        }
        return out;
    }

    public static boolean isYang(int v) {
        return v == V_A || v == V_O || v == V_YA || v == V_YO || v == V_ARAEA || v == V_WA || v == V_AE
                || v == V_OE || v == V_ARAEA_I || v == V_WAE || v == V_YAE || v == V_YO_I;
    }

    /** 모음 뒤에 주격 ㅣ를 합친 모음. 이미 ㅣ로 끝나면 그대로. 합칠 수 없으면 0. */
    public static int addI(int v) {
        switch (v) {
            case V_A: return V_AE;
            case V_EO: return V_E;
            case V_O: return V_OE;
            case V_U: return V_WI;
            case V_EU: return V_UI;
            case V_ARAEA: return V_ARAEA_I;
            case V_YA: return V_YAE;
            case V_YEO: return V_YE;
            case V_YO: return V_YO_I;
            case V_YU: return V_YU_I;
            case V_WA: return V_WAE;
            case V_WEO: return V_WE;
            case V_I: case V_AE: case V_E: case V_OE: case V_WI: case V_UI: case V_YAE: case V_YE:
            case V_WAE: case V_WE: case V_ARAEA_I: case V_YO_I: case V_YU_I:
                return v;
            default: return 0;
        }
    }
}
