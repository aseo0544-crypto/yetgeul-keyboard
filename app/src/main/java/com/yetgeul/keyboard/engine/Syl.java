package com.yetgeul.keyboard.engine;

/** 음절 하나 (초성 L, 중성 V, 종성 T; 첫가끝 코드). 한글이 아니면 raw에 글자를 담음. */
public final class Syl {
    public int l, v, t;
    public String raw;
    /** 사전이나 사용자가 직접 넣은 옛 음절: 일반 모음·자음 규칙을 건너뜀. */
    public boolean locked;
    /** 같은 사전 항목에서 나온 음절끼리는 같은 번호 (서로 이어적지 않음). */
    public int group;
    /** 한자음 등: 뒤 음절로 받침을 넘기지 않음. */
    public boolean noLink;

    public Syl(int l, int v, int t) { this.l = l; this.v = v; this.t = t; }

    public static Syl raw(String s) { Syl x = new Syl(0, 0, 0); x.raw = s; return x; }

    public boolean isHangul() { return raw == null; }

    public Syl lock() { locked = true; return this; }

    @Override public String toString() { return raw != null ? raw : Jamo.compose(l, v, t); }
}
