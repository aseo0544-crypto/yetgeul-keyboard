package com.yetgeul.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

import com.yetgeul.keyboard.engine.Converter;

/** 설정 저장소. 키보드와 설정 화면이 함께 씀. */
final class Prefs {
    static final String FILE = "yetgeul";

    /** {키, 화면 이름, 설명} — 설정 화면이 이 순서대로 보여 줌. */
    static final String[][] RULES = {
            {"araea", "아래아 ㆍ", "하늘 → ᄒᆞᄂᆞᆯ"},
            {"pansios", "반치음 ㅿ", "마음 → ᄆᆞᅀᆞᆷ"},
            {"yesieung", "옛이응 ㆁ", "백성 → ᄇᆡᆨ셔ᇰ"},
            {"beop", "순경음 비읍 ㅸ", "더워 → 더ᄫᅥ"},
            {"hapyong", "합용병서", "땅이 → ᄯᅡ히"},
            {"yeoneol", "이어적기", "사람이 → 사ᄅᆞ미"},
            {"palJong", "8종성", "꽃 → 곳, 있다 → 잇다"},
            {"yeorin", "여린히읗 ㆆ", "할 바 → ᄒᆞᇙ 바"},
            {"grammar", "옛 문법·말투", "중국과 달라 → 듕귁에 달아, 합니다 → ᄒᆞᄂᆞ이다"},
            {"compat", "카톡 호환", "받침 붙는 옛 글자는 현대 글자로 (어ᄶᅥᆯ → 어쩔), 받침 깨짐 방지"},
    };

    static SharedPreferences get(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }

    static Converter.Options options(Context c) {
        SharedPreferences p = get(c);
        Converter.Options o = new Converter.Options();
        o.araea = p.getBoolean("araea", true);
        o.pansios = p.getBoolean("pansios", true);
        o.yesieung = p.getBoolean("yesieung", true);
        o.beop = p.getBoolean("beop", true);
        o.hapyong = p.getBoolean("hapyong", true);
        o.yeoneol = p.getBoolean("yeoneol", true);
        o.palJong = p.getBoolean("palJong", true);
        o.yeorin = p.getBoolean("yeorin", true);
        o.grammar = p.getBoolean("grammar", true);
        o.compat = p.getBoolean("compat", false);
        o.letter = p.getBoolean("letter", false);
        return o;
    }

    static boolean oldMode(Context c) { return get(c).getBoolean("oldMode", true); }
    static void setOldMode(Context c, boolean on) { get(c).edit().putBoolean("oldMode", on).apply(); }
    static boolean longPress(Context c) { return get(c).getBoolean("longPress", true); }

    private Prefs() {}
}
