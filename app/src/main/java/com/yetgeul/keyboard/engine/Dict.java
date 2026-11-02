package com.yetgeul.keyboard.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 옛 낱말 사전. 값의 {..} 안은 호환 자모로 쓴 옛 음절 하나 (예: {ㅎㆍ} = ᄒᆞ).
 * ANY  : 낱말 어디서나 (두 음절 이상)
 * NOUN : 낱말 맨 앞, 뒤에는 조사만 올 때 (한 음절 명사)
 * STEM : 낱말 맨 앞, 뒤에 무엇이 와도 (용언 어간)
 */
public final class Dict {
    public enum Kind { ANY, NOUN, STEM }

    public static final class Entry {
        public final String modern; public final String spec; public final Kind kind;
        Entry(String m, String s, Kind k) { modern = m; spec = s; kind = k; }
    }

    public static final List<Entry> ENTRIES = new ArrayList<>();

    private static void any(String m, String s) { ENTRIES.add(new Entry(m, s, Kind.ANY)); }
    private static void noun(String m, String s) { ENTRIES.add(new Entry(m, s, Kind.NOUN)); }
    private static void stem(String m, String s) { ENTRIES.add(new Entry(m, s, Kind.STEM)); }

    /** 한 음절 명사 뒤에 올 수 있는 조사. */
    public static final String[] PARTICLES = {
            "", "이", "가", "은", "는", "을", "를", "의", "에", "에서", "에게", "도", "만", "과", "와", "로", "으로",
            "처럼", "까지", "부터", "마다", "이다", "이고", "이며", "이나", "나", "께", "한테", "보다", "들", "들이",
            "들은", "들을", "들의", "이여", "아", "야", "입니다", "이에요", "예요", "이요"};

    static {
        // 훈민정음 서문과 자주 쓰는 낱말
        any("나라의", "나랏");
        any("말씀", "말{ㅆㆍㅁ}");
        any("하늘", "{ㅎㆍ}{ㄴㆍㄹ}");
        any("사람", "사{ㄹㆍㅁ}");
        any("중국", "{ㄷㅠㆁ}{ㄱㅟㄱ}|");
        any("백성", "{ㅂㆎㄱ}{ㅅㅕㆁ}|");
        any("따름", "{ㅼㆍ}{ㄹㆍㅁ}");
        any("마음", "{ㅁㆍ}{ㅿㆍㅁ}");
        any("가을", "{ㄱㆍ}{ㅿㆍㄹ}");
        any("겨울", "겨{ㅿㅡㄹ}");
        any("여우", "여{ㅿㆍ}");
        any("아우", "아{ㅿㆍ}");
        any("사이", "{ㅅㆍ}{ㅿㅣ}");
        any("마을", "{ㅁㆍ}{ㅿㆍㄹ}");
        any("처음", "{ㅊㆍ}{ㅿㆍㅁ}");
        any("어버이", "어버{ㅿㅣ}");
        any("글자", "글{ㅉㆍ}");
        any("문자", "문{ㅉㆍ}");
        any("세상", "{ㅅㅖ}{ㅅㅑㆁ}");
        any("서로", "서르");
        any("아이", "{ㅇㆍ}{ㅎㆎ}");
        any("아들", "아{ㄷㆍㄹ}");
        any("바다", "바{ㄹㆍㄹ}");
        any("바람", "{ㅂㆍ}{ㄹㆍㅁ}");
        any("사랑", "{ㅅㆍ}랑");
        any("생각", "{ㅅㆎㆁ}각");
        any("아침", "아{ㅊㆍㅁ}");
        any("가슴", "가{ㅅㆍㅁ}");
        any("다섯", "다{ㅅㆍㅅ}");
        any("여덟", "여{ㄷㅡㄼ}");
        any("스물", "스믈");
        any("하나", "{ㅎㆍ}나");
        any("하루", "{ㅎㆍ}{ㄹㆍ}");
        any("부처", "부텨");
        any("천지", "{ㅌㅕㄴ}디");
        any("임금", "님금");
        any("이름", "일훔");
        any("여름", "녀름");
        any("어머니", "어마님");
        any("아버지", "아바님");
        any("노래", "놀애");
        any("모래", "몰애");
        any("구름", "구룸");
        any("가난", "가{ㄴㆍㄴ}");
        any("고양이", "{ㄱㅗ}{ㅸㆎ}");
        any("매우", "{ㅁㆎ}오");
        any("함께", "{ㅎㆍㄴ}{ㅴㅢ}");
        any("까닭", "{ㄱㆍ}{ㄷㆍㄺ}");
        any("나무", "나모");
        any("여기", "{ㅇㅕ}{ㄱㅢ}");
        any("거기", "거{ㄱㅢ}");
        any("저기", "{ㄷㅕ}{ㄱㅢ}");
        any("무엇", "므슥");
        any("누가", "{ㄴㅟ}");
        any("누구", "누");
        any("거울", "거우루");
        any("어찌", "엇뎨");
        any("어떻게", "엇뎨");
        any("왜냐하면", "엇뎨어뇨 {ㅎㆍ}란{ㄷㆎ}");
        any("부디", "모{ㄷㆍ}매");
        any("편안", "{ㅍㅕㄴ}안");
        any("편하", "{ㅍㅕㄴ}안{ㅎㆍ}");
        any("통하", "{ㅅㆍ}{ㅁㆍㅅ}");
        any("만들", "{ㅁㆎㆁ}{ㄱㆍㄹ}");
        any("만드", "{ㅁㆎㆁ}{ㄱㆍ}");
        any("가르치", "{ㄱㆍ}{ㄹㆍ}치");
        any("가르", "{ㄱㆍ}{ㄹㆍ}");
        any("달라", "달아");
        any("있어", "이셔");
        any("있으", "이시");
        any("하여", "{ㅎㆍ}야");
        any("해서", "{ㅎㆍ}야");
        any("하므로", "{ㅎㆍㄹ}{ㅆㆎ}");
        any("고자", "고져");
        any("지어", "지{ㅿㅓ}");
        any("지으", "지{ㅿㅡ}");
        any("나아", "나{ㅿㅏ}");
        any("이어", "니{ㅿㅓ}");
        any("쉬운", "쉬{ㅸㅡㄴ}");
        any("어려운", "어려{ㅸㅜㄴ}");
        any("고운", "고{ㅸㆍㄴ}");
        any("더운", "더{ㅸㅡㄴ}");
        any("추운", "추{ㅸㅡㄴ}");
        any("아름다운", "아{ㄹㆍㅁ}다{ㅸㆍㄴ}");
        any("가까운", "{ㄱㆍ}{ㅺㆍ}{ㅸㆍㄴ}");
        any("어리석", "어리");
        // 조사
        any("에게서", "{ㅇㆎ}그{ㆁㅔ}셔");
        any("에게", "{ㅇㆎ}그{ㆁㅔ}");
        any("한테", "{ㅇㆎ}그{ㆁㅔ}");
        any("께서", "{ㅺㅢ}셔");
        any("에서", "{ㅇㆎ}셔");
        any("처럼", "{ㄱㆍ}티");
        any("까지", "{ㅺㆍ}지");
        // 한 음절 명사
        noun("꽃", "곶");
        noun("뜻", "{ㅳㅡㄷ}");
        noun("땅", "{ㅼㅏㅎ}");
        noun("딸", "{ㅼㆍㄹ}");
        noun("떡", "{ㅼㅓㄱ}");
        noun("쌀", "{ㅄㆍㄹ}");
        noun("뿔", "{ㅽㅡㄹ}");
        noun("꿈", "{ㅺㅜㅁ}");
        noun("달", "{ㄷㆍㄹ}");
        noun("때", "{ㅵㅐ}");
        noun("흙", "{ㅎㆍㄺ}");
        noun("닭", "{ㄷㆍㄺ}");
        noun("해", "{ㅎㆎ}");
        noun("개", "가히");
        noun("잠", "{ㅈㆍㅁ}");
        noun("섬", "{ㅅㅕㅁ}");
        noun("소", "{ㅅㅛ}");
        noun("돌", "{ㄷㅗㅀ}");
        // 용언 어간
        stem("생", "{ㅅㆎㆁ}");
        stem("좋", "{ㄷㅛㅎ}");
        stem("같", "{ㄱㆍㅌ}");
        stem("젊", "{ㅈㅕㅁ}");
        stem("않", "아니{ㅎㆍ}");
        stem("했", "{ㅎㆍ}{ㅇㅑㅅ}");
        stem("쓰", "{ㅄㅡ}");
        stem("이르", "니르");

        // 긴 것부터 맞추도록 정렬
        Collections.sort(ENTRIES, (a, b) -> b.modern.length() - a.modern.length());
    }

    private Dict() {}
}
