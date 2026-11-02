import com.yetgeul.keyboard.engine.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

/**
 * data/ 자료 검사.
 *  1) words.tsv 옛말 표기가 바르게 읽히는지, Dict.java에 들어 있는지
 *  2) sentences.tsv 현대어를 변환해서 정답 옛말과 몇 % 같은지 (낱자 기준)
 * 표기가 틀리면 실패(종료 코드 1). 문장 점수는 보고만 함.
 */
public class DataCheck {
    static java.io.PrintStream out;

    static List<String[]> rows(Path p) throws Exception {
        List<String[]> r = new ArrayList<>();
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            r.add(line.split("\t", -1));
        }
        return r;
    }

    static String render(String spec) {
        StringBuilder sb = new StringBuilder();
        for (Syl s : Jamo.parseSpec(spec)) sb.append(s);
        return sb.toString();
    }

    /** 비교용: 낱자로 풀어 쓴 문자열의 코드 포인트들. */
    static int[] jamo(String s) { return Normalizer.normalize(s, Normalizer.Form.NFD).codePoints().toArray(); }

    static int distance(int[] a, int[] b) {
        int[] prev = new int[b.length + 1], cur = new int[b.length + 1];
        for (int j = 0; j <= b.length; j++) prev[j] = j;
        for (int i = 1; i <= a.length; i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length; j++)
                cur[j] = Math.min(Math.min(cur[j - 1], prev[j]) + 1, prev[j - 1] + (a[i - 1] == b[j - 1] ? 0 : 1));
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length];
    }

    public static void main(String[] a) throws Exception {
        out = new java.io.PrintStream(System.out, true, "UTF-8");
        Path dir = Paths.get(a.length > 0 ? a[0] : "data");
        int bad = 0;

        Set<String> inDict = new HashSet<>();
        for (Dict.Entry e : Dict.ENTRIES) inDict.add(e.modern + "\t" + e.spec + "\t" + e.kind.name().toLowerCase());
        int words = 0, missing = 0;
        for (String[] r : rows(dir.resolve("words.tsv"))) {
            words++;
            if (r.length < 3) { out.println("줄 모양이 틀림: " + String.join(" | ", r)); bad++; continue; }
            try { render(r[1]); } catch (RuntimeException ex) { out.println("표기 오류: " + r[0] + " → " + r[1]); bad++; continue; }
            if (!inDict.contains(r[0] + "\t" + r[1] + "\t" + r[2])) { out.println("사전에 없음: " + r[0] + " → " + render(r[1]) + " (" + r[2] + ")"); missing++; }
        }
        out.println("낱말 " + words + "개, 사전에 없는 것 " + missing + "개");

        Converter c = new Converter();
        int n = 0, exact = 0; double sum = 0;
        for (String[] r : rows(dir.resolve("sentences.tsv"))) {
            String gold;
            try { gold = render(r[1]); } catch (RuntimeException ex) { out.println("표기 오류: " + r[1]); bad++; continue; }
            String got = c.convert(r[0]);
            int[] g = jamo(gold), h = jamo(got);
            double score = 1.0 - (double) distance(g, h) / Math.max(g.length, 1);
            if (score < 0) score = 0;
            n++; sum += score;
            if (Arrays.equals(g, h)) exact++;
            out.printf("%3.0f%%  %s%n      정답 %s%n      변환 %s   [%s]%n", score * 100, r[0], gold, got, r.length > 2 ? r[2] : "");
        }
        out.printf("문장 %d개: 완전 일치 %d개, 평균 일치율 %.1f%%%n", n, exact, n == 0 ? 0 : sum * 100 / n);
        if (bad > 0) { out.println("표기 오류 " + bad + "개"); System.exit(1); }
    }
}
