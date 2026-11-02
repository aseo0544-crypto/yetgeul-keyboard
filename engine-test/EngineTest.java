import com.yetgeul.keyboard.engine.Converter;
public class EngineTest {
    public static void main(String[] a) throws Exception {
        java.io.PrintStream out = new java.io.PrintStream(System.out, true, "UTF-8");
        Converter c = new Converter();
        String[] in = {
            "나라의 말씀이 중국과 달라 문자와 서로 통하지 아니하므로",
            "하늘과 땅이 처음 생길 때", "사랑합니다", "안녕하세요", "내가 갈 것이다",
            "친구에게 말했어요", "지금 학교에서 배워요", "여자와 남자가 산에 갔다", "코가 아파요",
            "책을 읽어요?", "밥 먹었니?", "뭐 할까?", "물과 불", "하늘과 바다", "아이에게 말한다",
            "하나가 둘이 되었다", "아주 많은 사람이 있어요", "생각해 보니 기뻐요", "선생님께 드렸어요",
            "집에서 기다려요", "친구를 만나요", "친구는 여자와 산으로 갔다", "산이 높다", "학생이에요", "의사예요", "카톡에서 받침 깨지는 건 어쩔 수 없지?",
            "숙제 다 했음", "밥 먹음", "기분 좋음", "돈 없음", "시간 있슴", "그건 아님", "공부함", "잘 됨", "내가 범인임", "슬픔을 앎", "잘 모름",
        };
        for (String s : in) out.println(s + "\n  → " + c.convert(s));
        Converter.Options o = new Converter.Options(); o.compat = true;
        out.println("[카톡 호환] " + new Converter(o).convert("어쩔 수 없지 마음 사람이 백성"));
    }
}
