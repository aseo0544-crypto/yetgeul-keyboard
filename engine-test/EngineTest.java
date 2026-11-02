import com.yetgeul.keyboard.engine.Converter;
public class EngineTest {
    public static void main(String[] a) throws Exception {
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        Converter c = new Converter();
        String[] in = {
            "나라의 말씀이 중국과 달라 문자와 서로 통하지 아니하므로",
            "하늘과 땅이 처음 생길 때",
            "가을 마음", "사랑합니다", "안녕하세요", "감사합니다", "고마워", "내가 갈 것이다",
            "사람이 많이 있어", "꽃이 좋다", "좋은 사람", "물이 맑다", "모르는 것을 배우고 싶다",
            "통하지 않는다", "날마다 쓰기 편하게 하고자 할 따름이다", "누가 왔어?", "흙이 있다",
            "백성을 가르치는 바른 소리", "아이가 웃는다", "더워서 힘들다", "학교에 간다", "나는 너를 사랑해",
        };
        for (String s : in) out.println(s + "\n  → " + c.convert(s));
    }
}
