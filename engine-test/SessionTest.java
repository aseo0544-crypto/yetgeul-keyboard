import com.yetgeul.keyboard.engine.*;
public class SessionTest {
    static ComposeSession s = new ComposeSession(new Converter());
    static java.io.PrintStream out;
    static void typeKeys(String keys) {
        for (char c : keys.toCharArray()) {
            if (c == ' ') { s.space(); out.println("  [" + s.composing() + "] " + s.candidates()); }
            else if (c == '<') s.backspace();
            else s.type(c);
        }
    }
    public static void main(String[] a) throws Exception {
        out = new java.io.PrintStream(System.out, true, "UTF-8");
        // 두벌식 키: 중국과 달라 문자와 서로 통하지 아니하므로
        typeKeys("ㅈㅜㅇㄱㅜㄱㄱㅗㅏ ㄷㅏㄹㄹㅏ ㅁㅜㄴㅈㅏㅇㅗㅏ ㅅㅓㄹㅗ ㅌㅗㅇㅎㅏㅈㅣ ㅇㅏㄴㅣㅎㅏㅁㅡㄹㅗ");
        out.println("cands: " + s.candidates());
        out.println("final: " + s.finishAll());
        typeKeys("ㅅㆍㅁ ");  // 직접 넣은 옛 글자
        typeKeys("ㄷㅏㄺㅇㅣ ");
        typeKeys("<<<");
        out.println("after bs: [" + s.composing() + "]");
        out.println(HangulComposer.compose(java.util.Arrays.asList('ㄱ','ㅏ','ㅂ','ㅅ','ㅣ','ㅗ','ㅏ','ㄹ','ㄹ','ㅎ','ㅡ','ㅣ')));
    }
}
