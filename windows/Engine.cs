// 옛글 키보드 변환 엔진 (C#) — 안드로이드 Java 엔진(app/src/main/java/.../engine)을 옮긴 것.
// 사전과 자모 표는 windows/gen.py가 Java 소스에서 자동으로 채움. 직접 고치지 말 것.
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace Yetgeul
{
    public class Syl
    {
        public int L, V, T;
        public string Raw;
        public bool Locked, NoLink;
        public int Group;
        public Syl(int l, int v, int t) { L = l; V = v; T = t; }
        public static Syl MakeRaw(string s) { Syl x = new Syl(0, 0, 0); x.Raw = s; return x; }
        public bool IsHangul { get { return Raw == null; } }
    }

    public static class Jamo
    {
        public const int L_G = 0x1100, L_GG = 0x1101, L_N = 0x1102, L_D = 0x1103, L_DD = 0x1104, L_R = 0x1105,
            L_M = 0x1106, L_B = 0x1107, L_BB = 0x1108, L_S = 0x1109, L_SS = 0x110A, L_NULL = 0x110B, L_J = 0x110C,
            L_JJ = 0x110D, L_CH = 0x110E, L_K = 0x110F, L_T = 0x1110, L_P = 0x1111, L_H = 0x1112;
        public const int L_SG = 0x112D, L_SD = 0x112F, L_SB = 0x1132, L_SJ = 0x1136, L_BS = 0x1121,
            L_BEOP = 0x112B, L_PANSIOS = 0x1140, L_YESIEUNG = 0x114C, L_YEORIN = 0x1159;
        public const int V_A = 0x1161, V_AE = 0x1162, V_YA = 0x1163, V_YAE = 0x1164, V_EO = 0x1165, V_E = 0x1166,
            V_YEO = 0x1167, V_YE = 0x1168, V_O = 0x1169, V_WA = 0x116A, V_WAE = 0x116B, V_OE = 0x116C, V_YO = 0x116D,
            V_U = 0x116E, V_WEO = 0x116F, V_WE = 0x1170, V_WI = 0x1171, V_YU = 0x1172, V_EU = 0x1173, V_UI = 0x1174,
            V_I = 0x1175, V_ARAEA = 0x119E, V_ARAEA_I = 0x11A1, V_YO_I = 0x1188, V_YU_I = 0x1194;
        public const int T_G = 0x11A8, T_GG = 0x11A9, T_GS = 0x11AA, T_N = 0x11AB, T_NJ = 0x11AC, T_NH = 0x11AD,
            T_D = 0x11AE, T_R = 0x11AF, T_RG = 0x11B0, T_RM = 0x11B1, T_RB = 0x11B2, T_RS = 0x11B3, T_RT = 0x11B4,
            T_RP = 0x11B5, T_RH = 0x11B6, T_M = 0x11B7, T_B = 0x11B8, T_BS = 0x11B9, T_S = 0x11BA, T_SS = 0x11BB,
            T_NG = 0x11BC, T_J = 0x11BD, T_CH = 0x11BE, T_K = 0x11BF, T_T = 0x11C0, T_P = 0x11C1, T_H = 0x11C2,
            T_YESIEUNG = 0x11F0, T_R_YEORIN = 0x11D9, T_PANSIOS = 0x11EB;

        const int BASE = 0x3131;
        static readonly int[] CHO = { 0x1100,0x1101,0x0,0x1102,0x115c,0x115d,0x1103,0x1104,0x1105,0xa964,0xa968,0xa969,0xa96c,0x0,0x0,0x111a,0x1106,0x1107,0x1108,0x1121,0x1109,0x110a,0x110b,0x110c,0x110d,0x110e,0x110f,0x1110,0x1111,0x1112,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x1114,0x1115,0x115b,0x0,0x0,0xa966,0x0,0x0,0x0,0x111c,0xa971,0x0,0x111d,0x111e,0x1120,0x1122,0x1123,0x1127,0x1129,0x112b,0x112c,0x112d,0x112e,0x112f,0x1132,0x1136,0x1140,0x1147,0x114c,0x0,0x0,0x1157,0x1158,0x1159,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0 };
        static readonly int[] JUNG = { 0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x1161,0x1162,0x1163,0x1164,0x1165,0x1166,0x1167,0x1168,0x1169,0x116a,0x116b,0x116c,0x116d,0x116e,0x116f,0x1170,0x1171,0x1172,0x1173,0x1174,0x1175,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x1184,0x1185,0x1188,0x1191,0x1192,0x1194,0x119e,0x11a1 };
        static readonly int[] JONG = { 0x11a8,0x11a9,0x11aa,0x11ab,0x11ac,0x11ad,0x11ae,0xd7cd,0x11af,0x11b0,0x11b1,0x11b2,0x11b3,0x11b4,0x11b5,0x11b6,0x11b7,0x11b8,0xd7e6,0x11b9,0x11ba,0x11bb,0x11bc,0x11bd,0xd7f9,0x11be,0x11bf,0x11c0,0x11c1,0x11c2,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x11ff,0x11c6,0x11c7,0x11c8,0x11cc,0x11ce,0x11d3,0x11d7,0x11d9,0x11dc,0x11dd,0x11df,0x11e2,0x0,0xd7e3,0x0,0xd7e7,0xd7e8,0x0,0x11e6,0x0,0x11e7,0x0,0x11e8,0x11ea,0xd7ef,0x11eb,0x11ee,0x11f0,0x11f1,0x11f2,0x11f4,0x0,0x11f9,0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0 };

        public static bool IsPre(int c) { return c >= 0xAC00 && c <= 0xD7A3; }
        public static bool IsL(int c) { return (c >= 0x1100 && c <= 0x115F) || (c >= 0xA960 && c <= 0xA97C); }
        public static bool IsV(int c) { return (c >= 0x1160 && c <= 0x11A7) || (c >= 0xD7B0 && c <= 0xD7C6); }
        public static bool IsT(int c) { return (c >= 0x11A8 && c <= 0x11FF) || (c >= 0xD7CB && c <= 0xD7FB); }
        public static bool IsCompat(int c) { return c >= 0x3131 && c <= 0x318E; }
        public static int CToL(int c) { return IsCompat(c) ? CHO[c - BASE] : 0; }
        public static int CToV(int c) { return IsCompat(c) ? JUNG[c - BASE] : 0; }
        public static int CToT(int c) { return IsCompat(c) ? JONG[c - BASE] : 0; }

        public static int TToL(int t)
        {
            for (int i = 0; i < JONG.Length; i++) if (JONG[i] == t && CHO[i] != 0) return CHO[i];
            return 0;
        }

        public static int[] SplitCluster(int t)
        {
            switch (t)
            {
                case T_GS: return new[] { T_G, L_S };
                case T_NJ: return new[] { T_N, L_J };
                case T_NH: return new[] { T_N, L_H };
                case T_RG: return new[] { T_R, L_G };
                case T_RM: return new[] { T_R, L_M };
                case T_RB: return new[] { T_R, L_B };
                case T_RS: return new[] { T_R, L_S };
                case T_RT: return new[] { T_R, L_T };
                case T_RP: return new[] { T_R, L_P };
                case T_RH: return new[] { T_R, L_H };
                case T_BS: return new[] { T_B, L_S };
                case T_R_YEORIN: return new[] { T_R, L_YEORIN };
                default: return null;
            }
        }

        public static string Compose(int l, int v, int t)
        {
            if (l >= L_G && l <= L_H && v >= V_A && v <= V_I && (t == 0 || (t >= T_G && t <= T_H)))
                return char.ConvertFromUtf32(0xAC00 + ((l - L_G) * 21 + (v - V_A)) * 28 + (t == 0 ? 0 : t - 0x11A7));
            StringBuilder sb = new StringBuilder();
            sb.Append((char)(l == 0 ? 0x115F : l));
            sb.Append((char)(v == 0 ? 0x1160 : v));
            if (t != 0) sb.Append((char)t);
            return sb.ToString();
        }

        public static List<Syl> Parse(string s, bool lockArchaic)
        {
            List<Syl> o = new List<Syl>();
            int i = 0, n = s.Length;
            while (i < n)
            {
                int c = s[i];
                if (IsPre(c))
                {
                    int x = c - 0xAC00;
                    o.Add(new Syl(L_G + x / 588, V_A + (x % 588) / 28, x % 28 == 0 ? 0 : 0x11A7 + x % 28));
                    i++;
                }
                else if (IsL(c))
                {
                    int l = c, v = 0, t = 0; i++;
                    if (i < n && IsV(s[i])) { v = s[i]; i++; }
                    if (i < n && IsT(s[i])) { t = s[i]; i++; }
                    Syl sy = new Syl(l == 0x115F ? 0 : l, v == 0x1160 ? 0 : v, t);
                    sy.Locked = lockArchaic;
                    o.Add(sy);
                }
                else { o.Add(Syl.MakeRaw(s[i].ToString())); i++; }
            }
            return o;
        }

        public static List<Syl> ParseSpec(string spec)
        {
            List<Syl> o = new List<Syl>();
            int i = 0;
            while (i < spec.Length)
            {
                char ch = spec[i];
                if (ch == '{')
                {
                    int end = spec.IndexOf('}', i);
                    string g = spec.Substring(i + 1, end - i - 1);
                    int l = CToL(g[0]), k = 1, v = 0, t = 0;
                    if (k < g.Length && CToV(g[k]) != 0) { v = CToV(g[k]); k++; }
                    if (k < g.Length) { t = CToT(g[k]); k++; }
                    if (l == 0 || v == 0 || k != g.Length) throw new ArgumentException("bad spec: " + spec);
                    o.Add(new Syl(l, v, t));
                    i = end + 1;
                }
                else if (ch == '|') { if (o.Count > 0) o[o.Count - 1].NoLink = true; i++; }
                else { o.AddRange(Parse(ch.ToString(), false)); i++; }
            }
            return o;
        }

        public static bool IsYang(int v)
        {
            return v == V_A || v == V_O || v == V_YA || v == V_YO || v == V_ARAEA || v == V_WA || v == V_AE
                || v == V_OE || v == V_ARAEA_I || v == V_WAE || v == V_YAE || v == V_YO_I;
        }

        public static int AddI(int v)
        {
            switch (v)
            {
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

    public class Options
    {
        public bool Araea = true, Pansios = true, Yesieung = true, Yeorin = true, Beop = true,
            Hapyong = true, Yeoneol = true, PalJong = true, Grammar = true, Compat = false, Letter = false;
    }

    public class Converter
    {
        enum Kind { ANY, NOUN, STEM, VERB }
        class Entry { public string M, S; public Kind K; public Entry(string m, string s, Kind k) { M = m; S = s; K = k; } }

        static readonly List<Entry> ENTRIES = new List<Entry>();
        static readonly List<Entry> LETTER = new List<Entry>();
        static readonly List<Entry> LETTER_FIRST = new List<Entry>();
        static readonly HashSet<string> PARTICLES = new HashSet<string>(new[] { "", "이", "가", "은", "는", "을", "를", "의", "에", "에서", "에게", "도", "만", "과", "와", "로", "으로", "처럼", "까지", "부터", "마다", "이다", "이고", "이며", "이나", "나", "께", "한테", "보다", "들", "들이", "들은", "들을", "들의", "이여", "아", "야", "입니다", "이에요", "예요", "이요" });
        static readonly HashSet<string> GA_EXCEPT = new HashSet<string>(new[] {
            "휴가", "요가", "대가", "고가", "시가", "주가", "사가", "가가", "노가", "부가", "지가", "도가", "자가", "어디가" });

        static void any(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.ANY)); }
        static void noun(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.NOUN)); }
        static void stem(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.STEM)); }
        static void verb(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.VERB)); }
        static void lany(string m, string s) { LETTER.Add(new Entry(m, s, Kind.ANY)); }
        static void lnoun(string m, string s) { LETTER.Add(new Entry(m, s, Kind.NOUN)); }
        static void lstem(string m, string s) { LETTER.Add(new Entry(m, s, Kind.STEM)); }
        static void lverb(string m, string s) { LETTER.Add(new Entry(m, s, Kind.VERB)); }

        static Converter()
        {
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
            noun("하나", "{ㅎㆍ}{ㄴㅏㅎ}");
            noun("둘", "{ㄷㅜㅀ}");
            noun("셋", "{ㅅㅔㅎ}");
            noun("넷", "{ㄴㅔㅎ}");
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
            any("나무는", "남{ㄱㆍㄴ}");
            any("나무를", "남{ㄱㆍㄹ}");
            any("나무가", "남기");
            any("나무에", "남{ㄱㆎ}");
            any("나무의", "남{ㄱㆎ}");
            any("그렇게", "그리");
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
            verb("편하", "{ㅍㅕㄴ}안{ㅎㆍ}");
            verb("통하", "{ㅅㆍ}{ㅁㆍㅅ}");
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
            any("어리석은", "어린");
            any("어리석", "어리");
            any("에게서", "{ㅇㆎ}그{ㆁㅔ}셔");
            any("에게", "{ㅇㆎ}그{ㆁㅔ}");
            any("한테", "{ㅇㆎ}그{ㆁㅔ}");
            any("께서", "{ㅺㅢ}셔");
            any("처럼", "{ㄱㆍ}티");
            any("까지", "{ㅺㆍ}장");
            any("부터", "브터");
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
            stem("생", "{ㅅㆎㆁ}");
            stem("좋", "{ㄷㅛㅎ}");
            stem("같", "{ㄱㆍㅌ}");
            stem("젊", "{ㅈㅕㅁ}");
            stem("않", "아니{ㅎㆍ}");
            stem("했", "{ㅎㆍ}{ㅇㅑㅅ}");
            stem("쓰", "{ㅄㅡ}");
            stem("이르", "니르");
            any("친구", "벋");
            any("여자", "겨집");
            any("남자", "{ㅅㅗ}나{ㅎㆎ}");
            any("사나이", "{ㅅㅗ}나{ㅎㆎ}");
            any("어른", "얼운");
            any("바위", "바회");
            any("구멍", "구무");
            any("얼굴", "{ㄴㆍㅊ}");
            any("벌레", "벌에");
            any("지금", "이제");
            any("아주", "{ㄱㆍ}장");
            any("가장", "{ㄱㆍ}장");
            any("정말", "진실로");
            any("진짜", "진실로");
            any("너무", "너므");
            any("어디", "어{ㄷㅢ}");
            any("언제", "어느 제");
            any("어떤", "엇던");
            any("모두", "다");
            any("모든", "모{ㄷㆍㄴ}");
            any("많은", "한");
            any("많아", "하야");
            any("예쁜", "고{ㅸㆍㄴ}");
            any("말해", "닐어");
            any("말했", "닐엇");
            any("생각해", "{ㅅㆍ}랑{ㅎㆎ}");
            any("사랑해", "괴여");
            any("사랑했", "괴엿");
            any("배워", "{ㅂㆎ}화");
            any("배웠", "{ㅂㆎ}홧");
            any("싸워", "사화");
            any("기다려", "기드려");
            any("아파", "알파");
            any("배고파", "{ㅂㆎ}골파");
            any("기뻐", "깃버");
            any("빨라", "{ㅽㆍㄹ}라");
            noun("산", "뫼");
            noun("강", "가{ㄹㆍㅁ}");
            noun("코", "{ㄱㅗㅎ}");
            noun("배", "{ㅂㆎ}");
            noun("곳", "곧");
            noun("뼈", "{ㅽㅕ}");
            verb("말하", "니{ㄹㆍ}");
            verb("생각하", "{ㅅㆍ}랑{ㅎㆍ}");
            verb("사랑하", "괴");
            verb("일어나", "니러나");
            verb("기다리", "기드리");
            verb("만나", "맛나");
            verb("배우", "{ㅂㆎ}호");
            verb("싸우", "사호");
            verb("아프", "알{ㅍㆍ}");
            verb("배고프", "{ㅂㆎ}골{ㅍㆍ}");
            verb("기쁘", "깃브");
            verb("빠르", "{ㅽㆍ}{ㄹㆍ}");
            stem("많", "하");
            stem("읽", "{ㄴㅣㄺ}");
            stem("작", "{ㅈㅕㄱ}");
            stem("예쁘", "곱");
            stem("가깝", "{ㄱㆍㅅ}갑");
            stem("닮", "{ㄷㆍㄻ}");
            any("뿌리", "불휘");
            any("열매", "여름");
            noun("샘", "{ㅅㆎㅁ}");
            any("가뭄", "{ㄱㆍ}{ㅁㆍㄹ}");
            any("움직여", "뮈여");
            verb("움직이", "뮈");
            any("마침내", "{ㅁㆍ}{ㅊㆍㅁ}내");
            any("끝내", "{ㅁㆍ}{ㅊㆍㅁ}내");
            any("불쌍히", "어엿비");
            any("불쌍해", "어엿버");
            verb("불쌍하", "어엿브");
            any("여겨", "너겨");
            any("익혀", "니겨");
            verb("익히", "니기");
            stem("못", "몯");
            any("나쁜", "모딘");
            any("귀신", "귓것");
            any("친척", "아{ㅿㆍㅁ}");
            any("재물", "쳔{ㄹㅑㆁ}");
            any("많이", "만히");
            any("모아", "뫼호아");
            verb("모으", "뫼호");
            any("지켜", "딕{ㅎㅢ}여");
            verb("지키", "딕{ㅎㅢ}");
            any("마지못해", "모지마라");
            any("베어", "버혀");
            any("가둬", "가도아");
            any("가두어", "가도아");
            verb("가두", "가도");
            any("아까운", "앗가{ㅸㆍㄴ}");
            any("아깝", "앗갑");
            any("아껴", "앗겨");
            verb("아끼", "앗기");
            any("괴롭혀", "보채여");
            verb("괴롭히", "보채");
            any("무릇", "믈읫");
            noun("만약", "{ㅎㆍ}다가");
            noun("만일", "{ㅎㆍ}다가");
            noun("잘", "이대");
            any("남편", "남진");
            any("항상", "{ㅅㅑㆁ}{ㄴㅖ}");
            noun("늘", "{ㅅㅑㆁ}{ㄴㅖ}");
            any("빨리", "{ㅽㆍㄹ}리");
            any("천천히", "날호야");
            any("갑자기", "믄득");
            any("문득", "믄득");
            any("반드시", "반{ㄷㆍ}기");
            noun("자주", "{ㅈㆍ}조");
            noun("이미", "{ㅎㆍ}마");
            noun("벌써", "{ㅎㆍ}마");
            noun("아직", "안직");
            noun("혼자", "{ㅎㆍ}오{ㅿㅏ}");
            noun("홀로", "{ㅎㆍ}오{ㅿㅏ}");
            any("가득", "{ㄱㆍ}{ㄷㆍㄱ}");
            noun("겨우", "계오");
            noun("조금", "{ㅈㅛ}곰");
            noun("얼마", "언마");
            noun("왜", "엇뎨");
            any("저것", "{ㄷㅕ}것");
            any("가운데", "가온{ㄷㆎ}");
            noun("끝", "{ㄱㆍㅅ}");
            noun("밖", "밧");
            noun("옆", "{ㄴㅕㅂ}");
            any("웃음", "우{ㅅㅜㅁ}");
            any("입술", "입시울");
            noun("이마", "니마");
            noun("털", "터럭");
            any("머리카락", "머리터럭");
            any("그림자", "그르메");
            any("무지개", "므지게");
            any("새벽", "새{ㅂㆎ}");
            noun("잎", "닙");
            any("나비", "나{ㅂㆎ}");
            any("호랑이", "범");
            any("토끼", "톳기");
            any("거북", "거붑");
            noun("뱀", "{ㅂㆎ}얌");
            any("돼지", "{ㄷㅗㅌ}");
            any("가위", "{ㄱㆍ}{ㅿㆎ}");
            any("수레", "술위");
            any("시장", "{ㅈㅕ}{ㅈㆎ}");
            any("서울", "{ㅅㅕ}{ㅸㅡㄹ}");
            any("도둑", "도{ㅈㆍㄱ}");
            noun("칼", "{ㄱㅏㅀ}");
            any("할아버지", "한아비");
            any("할머니", "한어미");
            any("누나", "누의");
            any("누이", "누의");
            any("아내", "갓");
            any("여섯", "여{ㅅㅡㅅ}");
            any("일곱", "닐굽");
            noun("열", "{ㅇㅕㅀ}");
            any("서른", "셜흔");
            any("마흔", "마{ㅿㆍㄴ}");
            any("어두운", "어드{ㅸㅡㄴ}");
            any("즐거운", "즐거{ㅸㅜㄴ}");
            any("무거운", "므거{ㅸㅜㄴ}");
            any("무거", "므거");
            any("무겁", "므겁");
            any("무서운", "므{ㅿㅢ}여{ㅸㅡㄴ}");
            any("두려운", "두리{ㅸㅡㄴ}");
            any("두렵", "두립");
            any("깨끗", "{ㅺㆎㅅ}긋");
            any("착한", "어딘");
            any("어진", "어딘");
            any("짧은", "{ㄷㅕ}른");
            stem("싫", "{ㅅㅡㅀ}");
            stem("잊", "닞");
            stem("씻", "싯");
            any("버려", "{ㅂㆍ}려");
            verb("버리", "{ㅂㆍ}리");
            any("고쳐", "고텨");
            verb("고치", "고티");
            any("다퉈", "다토아");
            verb("다투", "다토");
            any("사귀어", "사괴여");
            verb("사귀", "사괴");
            any("나눠", "{ㄴㆍㄴ}화");
            any("나누어", "{ㄴㆍㄴ}화");
            verb("나누", "{ㄴㆍㄴ}호");
            any("바라", "{ㅂㆍ}라");
            noun("벗", "벋");
            any("수달", "러울");
            any("노루", "노로");
            any("원숭이", "납");
            any("두꺼비", "두텁");
            any("파리", "{ㅍㆍㄹ}");
            noun("팔", "{ㅂㆍㄹ}");
            any("새우", "사{ㅸㅣ}");
            any("종이", "{ㅈㅛ}{ㅎㆎ}");
            any("채찍", "채");
            any("부엉이", "부{ㅎㅕㆁ}");
            any("병아리", "비육");
            any("우박", "무뤼");
            any("얼음", "어름");
            noun("턱", "{ㅌㆍㄱ}");
            noun("팥", "{ㅍㆍㅅ}");
            any("기러기", "그력");
            any("발꿈치", "발측");
            any("벼루", "벼로");
            any("사슴", "사{ㅅㆍㅁ}");
            noun("숯", "숫");
            any("부엌", "브{ㅿㅓㅂ}");
            any("우산", "{ㅅㅠ}룹");
            any("제비", "{ㅈㅕ}비");
            any("반딧불이", "반{ㄷㅚ}");
            any("반딧불", "반{ㄷㅚ}");
            any("올챙이", "올창");
            any("굼벵이", "굼{ㅂㅔㆁ}");
            any("호미", "호{ㅁㆎ}");
            any("율무", "율{ㅁㅢ}");
            noun("절", "{ㄷㅕㄹ}");
            any("울타리", "울");
            lany("걱정", "념녀");
            lany("염려", "념녀");
            lany("소식", "긔별");
            lany("연락", "긔별");
            lnoun("잘", "{ㄷㅛ}히");
            lany("건강히", "평안이");
            lany("건강", "평안");
            lany("편안", "평안");
            lany("안부", "문안");
            lany("그동안", "그{ㅅㆍ}이");
            lany("그사이", "그{ㅅㆍ}이");
            lany("어머님", "어마님");
            lany("어머니", "어마님");
            lany("엄마", "어마님");
            lany("아버님", "아바님");
            lany("아버지", "아바님");
            lany("아빠", "아바님");
            lany("자식", "{ㅈㆍ}식");
            lany("자세히", "{ㅈㆍ}시");
            lany("자주", "{ㅈㆍ}조");
            lany("무사히", "무{ㅅㆍ}히");
            lany("무사", "무{ㅅㆍ}");
            lany("부디", "브{ㄷㆎ}");
            lany("더욱", "더옥");
            lany("날씨", "일긔");
            lany("마음", "{ㅁㆍ}{ㅇㆍㅁ}");
            lany("서울", "셔울");
            lany("문안", "문|안");
            lany("줄인다", "그치노라");
            lany("줄입니다", "그치{ㅇㆍㅂ}{ㄴㆍ}이다");
            lany("줄여요", "그치{ㅇㆍㅂ}{ㄴㆍ}이다");
            lany("어떻게", "엇디");
            lany("어떠", "엇더");
            lany("어떤", "엇던");
            lany("편지", "편지");
            lverb("지내", "디내");
            lstem("좋", "{ㄷㅛㅎ}");
            lstem("싶", "시브");
            lverb("바라", "{ㅂㆍ}라");
            lverb("드리", "드리");
            ENTRIES.Sort(delegate(Entry a, Entry b) { return b.M.Length - a.M.Length; });
            LETTER.Sort(delegate(Entry a, Entry b) { return b.M.Length - a.M.Length; });
            LETTER_FIRST.AddRange(LETTER); LETTER_FIRST.AddRange(ENTRIES);
        }

        readonly Options o;
        int groupSeq = 0;

        public Converter(Options options)
        {
            if (options.Letter)
            {
                Options c = new Options();
                c.Araea = options.Araea; c.Hapyong = options.Hapyong; c.Yeoneol = options.Yeoneol; c.PalJong = options.PalJong;
                c.Grammar = options.Grammar; c.Compat = options.Compat; c.Letter = true;
                c.Pansios = false; c.Yesieung = false; c.Beop = false; c.Yeorin = false;
                options = c;
            }
            o = options;
        }
        public Converter() : this(new Options()) { }

        static readonly Regex WORD = new Regex("[가-힣ᄀ-ᇿꥠ-꥿ힰ-퟿]+");
        static readonly Regex P_DIFFER = new Regex("([가-힣]+?)(과|와)(\\s+)(다르|달라|달리|다른|다릅|다를|달랐)");
        static readonly Regex P_NEG = new Regex("([가-힣]+?)지(\\s+)(않|아니|못|마(?=세|라|십|요|시|[^가-힣]|$))");
        static readonly Regex P_JUL = new Regex("([가-힣]+)(\\s+)줄(을)?(?=\\s+(알|몰|모르))");
        static readonly Regex WH = new Regex("(^|[^가-힣])(어디|무엇|뭐|누구|누가|언제|어떤|어떻게|어찌|왜|몇|어느|무슨)");
        bool wh;
        static readonly Regex P_WANT = new Regex("([가-힣]+?)고(\\s+)싶(습니다|어요|으면|었다|다|어|은)");
        static readonly Regex P_WILL = new Regex("([가-힣]*)([가-힣])\\s+(것입니다|것이에요|거예요|것이다|거야|거다)");

        public string Convert(string text)
        {
            wh = WH.IsMatch(text);
            if (o.Grammar) text = PhraseRules(text, o.Letter);
            List<string> parts = new List<string>();
            List<bool> isWord = new List<bool>();
            int last = 0;
            foreach (Match m in WORD.Matches(text))
            {
                if (m.Index > last) { parts.Add(text.Substring(last, m.Index - last)); isWord.Add(false); }
                parts.Add(m.Value); isWord.Add(true);
                last = m.Index + m.Length;
            }
            if (last < text.Length) { parts.Add(text.Substring(last)); isWord.Add(false); }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.Count; i++)
            {
                if (!isWord[i]) { sb.Append(parts[i]); continue; }
                bool question = i + 1 < parts.Count && parts[i + 1].Trim().StartsWith("?");
                List<Syl> syls = ConvertWord(parts[i], question);
                if (o.Yeorin && o.Araea && syls.Count > 0 && i + 2 < parts.Count && parts[i + 1] == " " && isWord[i + 2])
                {
                    Syl z = syls[syls.Count - 1];
                    if (z.IsHangul && z.L == Jamo.L_H && z.V == Jamo.V_ARAEA && z.T == Jamo.T_R) z.T = Jamo.T_R_YEORIN;
                }
                foreach (Syl s in syls) sb.Append(Render(s));
            }
            return sb.ToString();
        }

        static string PhraseRules(string t, bool letter)
        {
            t = P_DIFFER.Replace(t, "$1에$3$4");
            t = P_NEG.Replace(t, "$1디$2$3");
            t = P_JUL.Replace(t, delegate(Match m)
            {
                string g1 = m.Groups[1].Value;
                char c = g1[g1.Length - 1];
                int fin = c >= 0xAC00 && c <= 0xD7A3 ? (c - 0xAC00) % 28 : 0;
                if (fin != 4 && fin != 8) return m.Value;
                return g1 + m.Groups[2].Value + (m.Groups[3].Success ? "\u1103\u119E\u1105\u119E\u11AF" : "\u1103\u119E\u11AF");
            });
            if (!letter) t = P_WANT.Replace(t, delegate(Match m)
            {
                string e = m.Groups[3].Value, r;
                switch (e)
                {
                    case "습니다": r = "합니다"; break;
                    case "어요": case "어": r = "하노라"; break;
                    case "으면": r = "하면"; break;
                    case "었다": r = "하얏다"; break;
                    case "은": r = "한"; break;
                    default: r = "하다"; break;
                }
                return m.Groups[1].Value + "고져" + m.Groups[2].Value + r;
            });
            t = P_WILL.Replace(t, delegate(Match m)
            {
                char c = m.Groups[2].Value[0];
                int x = c - 0xAC00;
                if (x % 28 != 8) return m.Value;
                string b = m.Groups[1].Value + (char)(c - 8);
                string g3 = m.Groups[3].Value;
                return b + (g3.StartsWith("것입") || g3.EndsWith("요") ? "리이다" : "리라");
            });
            return t;
        }

        public List<Syl> ConvertWord(string word, bool question)
        {
            List<Syl> s = Jamo.Parse(word, true);
            if (o.Grammar) s = o.Letter ? LetterEndings(s, question) : Endings(s, question);
            s = Dictionary(s, word);
            if (o.Grammar) { AfterStem(s); Copula(s); Nominal(s, word); SubjectGa(s, word); Particles(s); }
            if (o.Letter) LetterFix(s);
            for (int i = 0; i < s.Count; i++)
            {
                Syl a = s[i];
                if (!a.IsHangul || a.Locked) continue;
                Syl prev = i > 0 ? s[i - 1] : null;
                Syl next = i + 1 < s.Count ? s[i + 1] : null;
                if (o.Araea)
                {
                    if (a.L == Jamo.L_H && a.V == Jamo.V_A) a.V = Jamo.V_ARAEA;
                    else if (a.L == Jamo.L_H && a.V == Jamo.V_AE) a.V = Jamo.V_ARAEA_I;
                    if ((a.L == Jamo.L_M || a.L == Jamo.L_B || a.L == Jamo.L_P) && a.V == Jamo.V_U
                        && (a.T == Jamo.T_R || (a.T == 0 && next != null && next.IsHangul && next.L == Jamo.L_R && next.V == Jamo.V_EU)))
                        a.V = Jamo.V_EU;
                    if (prev != null && prev.IsHangul && Jamo.IsYang(prev.V)
                        && (a.L == Jamo.L_R || a.L == Jamo.L_N || a.L == Jamo.L_NULL || a.L == Jamo.L_S || a.L == Jamo.L_M)
                        && (a.T == 0 || a.T == Jamo.T_N || a.T == Jamo.T_R || a.T == Jamo.T_M))
                    {
                        if (a.V == Jamo.V_EU) a.V = Jamo.V_ARAEA;
                        else if (a.V == Jamo.V_UI) a.V = Jamo.V_ARAEA_I;
                    }
                }
                if (o.Beop && prev != null && prev.IsHangul && prev.T == 0 && a.L == Jamo.L_NULL)
                {
                    if (a.V == Jamo.V_WEO) { a.L = Jamo.L_BEOP; a.V = Jamo.V_EO; }
                    else if (a.V == Jamo.V_WA && prev.V == Jamo.V_O) { a.L = Jamo.L_BEOP; a.V = Jamo.V_A; }
                }
            }
            if (o.Yeoneol) Linking(s);
            if (o.Hapyong)
            {
                foreach (Syl a in s)
                {
                    if (!a.IsHangul || a.Locked) continue;
                    if (a.L == Jamo.L_GG) a.L = Jamo.L_SG;
                    else if (a.L == Jamo.L_DD) a.L = Jamo.L_SD;
                    else if (a.L == Jamo.L_BB) a.L = Jamo.L_SB;
                    else if (a.L == Jamo.L_SS) a.L = Jamo.L_BS;
                    else if (a.L == Jamo.L_JJ) a.L = Jamo.L_SJ;
                }
            }
            if (o.PalJong) EightFinals(s);
            if (o.PalJong && o.Letter) foreach (Syl a in s) if (a.IsHangul && a.T == Jamo.T_D) a.T = Jamo.T_S;
            if (o.Yesieung) foreach (Syl a in s) if (a.IsHangul && a.T == Jamo.T_NG) a.T = Jamo.T_YESIEUNG;
            return s;
        }

        List<Syl> Endings(List<Syl> s, bool q)
        {
            int n = s.Count;
            string w = Modern(s);
            string polite = q ? "{ㄴㆍ}니잇가" : "{ㄴㆍ}이다";
            if (q)
            {
                if (w.EndsWith("이에요") && n > 3) return ReplaceTail(s, 3, "이니잇가", null);
                if (n > 2 && (w.EndsWith("이야") || w.EndsWith("이냐") || w.EndsWith("이니"))) return ReplaceTail(s, 2, wh ? "고" : "가", null);
                string ny = wh ? "{ㄴㆍ}뇨" : "{ㄴㆍ}녀";
                if (n >= 3 && (w.EndsWith("느냐") || w.EndsWith("느니"))) return ReplaceTail(s, 2, ny, null);
                if (n >= 1 && w.EndsWith("해") && !s[n - 1].Locked) { s[n - 1].V = Jamo.V_A; return ReplaceTail(s, 0, ny, null); }
                if (n >= 2)
                {
                    Syl p = s[n - 2], z = s[n - 1];
                    if (p.IsHangul && !p.Locked && z.IsHangul && !z.Locked && z.T == 0 && !w.EndsWith("겠어"))
                    {
                        if (w.EndsWith("야") && p.T == 0) return ReplaceTail(s, 1, wh ? "고" : "가", null);
                        if (z.L == Jamo.L_NULL && (z.V == Jamo.V_EO || z.V == Jamo.V_A)) return ReplaceTail(s, 1, ny, null);
                    }
                }
                if (n >= 2 && (w.EndsWith("니") || w.EndsWith("냐")) && !w.EndsWith("습니"))
                {
                    Syl pq = s[n - 2];
                    if (pq.IsHangul && !pq.Locked && pq.T != Jamo.T_B) return ReplaceTail(s, 1, ny, null);
                }
            }
            string[,] get = q ? new string[,] { {"겠습니까", "리잇가"}, {"겠어요", "리잇가"}, {"겠니", "리여"}, {"겠냐", "리여"}, {"겠어", "리여"} }
                : new string[,] { {"겠습니다", "리이다"}, {"겠어요", "리이다"}, {"겠다", "리라"}, {"겠어", "리라"}, {"겠음", "리로다"}, {"겠슴", "리로다"} };
            for (int i = 0; i < get.GetLength(0); i++)
            {
                if (!w.EndsWith(get[i, 0]) || w.Length <= get[i, 0].Length) continue;
                Syl b = s[n - get[i, 0].Length - 1];
                bool cons = b.IsHangul && b.T != 0 && b.T != Jamo.T_R;
                return ReplaceTail(s, get[i, 0].Length, (cons ? "으" : "") + get[i, 1], null);
            }
            string[,] fix = q ? new string[,] {
                {"입니다", "이니잇가"}, {"입니까", "이니잇가"}, {"습니다", "{ㄴㆍ}니잇가"}, {"습니까", "{ㄴㆍ}니잇가"},
                {"으세요", "으시니잇가"}, {"세요", "시니잇가"}, {"는다", "{ㄴㆍ}니"}, {"시어", "{ㅅㅑ}"} } : new string[,] {
                {"입니다", "이{ㆁㅣ}다"}, {"입니까", "이{ㆁㅣ}잇가"},
                {"습니다", "{ㄴㆍ}이다"}, {"습니까", "{ㄴㆍ}니잇가"},
                {"으십시오", "으{ㅅㅛ}{ㅅㅕ}"}, {"십시오", "{ㅅㅛ}{ㅅㅕ}"},
                {"으세요", "으{ㅅㅛ}{ㅅㅕ}"}, {"세요", "{ㅅㅛ}{ㅅㅕ}"},
                {"으므로", "을{ㅆㆎ}"}, {"으니까", "으니"}, {"는다", "{ㄴㆍ}다"},
                {"시어서", "{ㅅㅑ}"}, {"시어", "{ㅅㅑ}"} };
            for (int i = 0; i < fix.GetLength(0); i++)
                if (w.EndsWith(fix[i, 0]) && w.Length > fix[i, 0].Length) return ReplaceTail(s, fix[i, 0].Length, fix[i, 1], null);
            if (n >= 3)
            {
                Syl p = s[n - 3];
                if (p.IsHangul && !p.Locked && p.T == Jamo.T_B)
                {
                    if (w.EndsWith("니다")) return ReplaceTail(s, 2, polite, p);
                    if (w.EndsWith("니까")) return ReplaceTail(s, 2, "{ㄴㆍ}니잇가", p);
                }
            }
            if (n >= 3 && w.EndsWith("므로"))
            {
                Syl p = s[n - 3];
                if (p.IsHangul && p.T == 0) { p.T = Jamo.T_R; return ReplaceTail(s, 2, "{ㅆㆎ}", null); }
            }
            if (n >= 3 && w.EndsWith("니까")) return ReplaceTail(s, 2, "니", null);
            if (n >= 2 && w.EndsWith("까") && s[n - 2].IsHangul && s[n - 2].T == Jamo.T_R) return ReplaceTail(s, 1, wh ? "고" : "가", null);
            if (n >= 3 && w.EndsWith("이에요")) return ReplaceTail(s, 3, q ? "이니잇가" : "이{ㆁㅣ}다", null);
            if (n >= 3 && w.EndsWith("예요"))
            {
                Syl p = s[n - 3];
                if (p.IsHangul && p.T == 0 && Jamo.AddI(p.V) != 0) { p.V = Jamo.AddI(p.V); return ReplaceTail(s, 2, q ? "니잇가" : "{ㆁㅣ}다", null); }
            }
            if (n >= 2 && w.EndsWith("해요")) { s[n - 2].V = Jamo.V_A; return ReplaceTail(s, 1, polite, null); }
            if (n >= 3 && (w.EndsWith("어요") || w.EndsWith("아요")))
            {
                Syl p = s[n - 3];
                bool past = p.IsHangul && p.T == Jamo.T_SS;
                return ReplaceTail(s, 2, past ? (q ? "니잇가" : "니이다") : polite, null);
            }
            if (n >= 2 && w.EndsWith("요"))
            {
                Syl p = s[n - 2];
                if (p.IsHangul && !p.Locked && p.T == 0 && (p.V == Jamo.V_A || p.V == Jamo.V_EO || p.V == Jamo.V_YEO
                    || p.V == Jamo.V_WA || p.V == Jamo.V_WEO || p.V == Jamo.V_AE || p.V == Jamo.V_E))
                    return ReplaceTail(s, 1, polite, null);
            }
            if (n >= 2)
            {
                Syl p = s[n - 2], z = s[n - 1];
                if (p.IsHangul && !p.Locked && z.IsHangul && !z.Locked)
                {
                    if (w.EndsWith("네") && !BANMAL_EXCEPT.Contains(w)) return ReplaceTail(s, 1, "도다", null);
                    if (w.EndsWith("자") && !BANMAL_EXCEPT.Contains(w) && !q) return ReplaceTail(s, 1, "{ㅈㅕ}", null);
                    if ((w.EndsWith("게") || w.EndsWith("께")) && p.T == Jamo.T_R)
                    {
                        p.T = 0;
                        if (p.L == Jamo.L_H && p.V == Jamo.V_A) { p.V = Jamo.V_O; p.Locked = true; }
                        return ReplaceTail(s, 1, "리라", null);
                    }
                    if (n >= 3 && w.EndsWith("이다") && s[n - 3].IsHangul && s[n - 3].T != 0) return ReplaceTail(s, 1, "라", null);
                }
            }
            if (n >= 2 && w.EndsWith("다"))
            {
                Syl p = s[n - 2];
                if (p.IsHangul && !p.Locked && p.T == Jamo.T_N) return ReplaceTail(s, 1, "{ㄴㆍ}다", p);
            }
            return s;
        }

        List<Syl> LetterEndings(List<Syl> s, bool q)
        {
            int n = s.Count;
            string w = Modern(s);
            const string HON = "{ㅅㆍㅂ}{ㄴㆍ}이다", HON_V = "{ㅇㆍㅂ}{ㄴㆍ}이다";
            if (q)
            {
                string[,] fq = { {"셨어요", "시니잇가"}, {"셨나요", "시니잇가"}, {"으세요", "으시니잇가"}, {"세요", "시니잇가"},
                    {"으신가요", "으신가"}, {"신가요", "신가"}, {"습니까", "{ㅅㆍㅂ}{ㄴㆍ}니잇가"}, {"입니까", "이니잇가"},
                    {"이에요", "이니잇가"}, {"나요", "{ㄴㆍㄴ}가"}, {"느냐", "{ㄴㆍㄴ}다"} };
                for (int i = 0; i < fq.GetLength(0); i++)
                    if (w.EndsWith(fq[i, 0]) && w.Length > fq[i, 0].Length) return ReplaceTail(s, fq[i, 0].Length, fq[i, 1], null);
                if (n >= 3 && w.EndsWith("니까") && s[n - 3].IsHangul && s[n - 3].T == Jamo.T_B)
                    return ReplaceTail(s, 2, "{ㅇㆍㅂ}{ㄴㆍ}니잇가", s[n - 3]);
                if (n >= 3 && (w.EndsWith("어요") || w.EndsWith("아요"))) return ReplaceTail(s, 2, "{ㄴㆍㄴ}가", null);
                if (n >= 2 && (w.EndsWith("니") || w.EndsWith("냐") || w.EndsWith("어") || w.EndsWith("아")))
                {
                    Syl pq = s[n - 2];
                    if (pq.IsHangul && !pq.Locked && pq.T != Jamo.T_B) return ReplaceTail(s, 1, "{ㄴㆍㄴ}다", null);
                }
                return s;
            }
            string[,] fx = {
                {"입니다", "이" + HON_V}, {"이에요", "이" + HON_V}, {"습니다", HON},
                {"으십시오", "으{ㅅㅛ}{ㅅㅕ}"}, {"십시오", "{ㅇㆍㅂ}{ㅅㅛ}{ㅅㅕ}"}, {"으세요", "으{ㅅㅛ}{ㅅㅕ}"}, {"세요", "{ㅇㆍㅂ}{ㅅㅛ}{ㅅㅕ}"},
                {"으므로", "으니"}, {"니까", "니"}, {"해라", "{ㅎㆍ}여라"}, {"있어라", "잇거라"},
                {"겠습니다", "리" + HON_V}, {"겠다", "리라"}, {"겠어요", "리" + HON_V} };
            for (int i = 0; i < fx.GetLength(0); i++)
                if (w.EndsWith(fx[i, 0]) && (w.Length > fx[i, 0].Length || fx[i, 0] == "있어라" || fx[i, 0] == "해라"))
                    return ReplaceTail(s, fx[i, 0].Length, fx[i, 1], null);
            if (n >= 3 && w.EndsWith("니다"))
            {
                Syl p = s[n - 3];
                if (p.IsHangul && !p.Locked && p.T == Jamo.T_B) return ReplaceTail(s, 2, HON_V, p);
            }
            if (n >= 2 && w.EndsWith("예요")) return ReplaceTail(s, 2, "이" + HON_V, null);
            if (n >= 2 && w.EndsWith("해요")) { s[n - 2].V = Jamo.V_A; return ReplaceTail(s, 1, HON_V, null); }
            if (n >= 3 && (w.EndsWith("어요") || w.EndsWith("아요")))
            {
                Syl p = s[n - 3];
                if (p.IsHangul && p.T != 0) return ReplaceTail(s, 2, HON, null);
                return ReplaceTail(s, 1, HON_V, null);
            }
            if (n >= 2 && w.EndsWith("요"))
            {
                Syl p = s[n - 2];
                if (p.IsHangul && !p.Locked && p.T == 0) return ReplaceTail(s, 1, HON_V, null);
            }
            if (w == "있다") return ReplaceTail(s, 1, "노라", null);
            if (n >= 2)
            {
                Syl p = s[n - 2], z = s[n - 1];
                if (p.IsHangul && !p.Locked && z.IsHangul && !z.Locked)
                {
                    if (w.EndsWith("네") && !BANMAL_EXCEPT.Contains(w)) return ReplaceTail(s, 1, "{ㄴㆎ}", null);
                    if (w.EndsWith("자") && !BANMAL_EXCEPT.Contains(w)) return ReplaceTail(s, 1, "{ㅈㅑ}", null);
                    if ((w.EndsWith("게") || w.EndsWith("께")) && p.T == Jamo.T_R) { p.T = 0; return ReplaceTail(s, 1, "마", null); }
                }
            }
            if (n >= 1 && w.EndsWith("해") && !s[n - 1].Locked) { s[n - 1].V = Jamo.V_A; return ReplaceTail(s, 0, "여", null); }
            return s;
        }

        static void LetterFix(List<Syl> s)
        {
            for (int i = 1; i < s.Count; i++)
            {
                Syl a = s[i - 1], b = s[i];
                if (!a.IsHangul || !b.IsHangul) continue;
                if (b.Locked && b.L == Jamo.L_S && b.V == Jamo.V_ARAEA && b.T == Jamo.T_B && a.T == 0) b.L = Jamo.L_NULL;
                if (a.L == Jamo.L_H && (a.V == Jamo.V_A || a.V == Jamo.V_ARAEA) && a.T == 0 && b.L == Jamo.L_NULL && b.V == Jamo.V_YA && b.T == 0) b.V = Jamo.V_YEO;
            }
        }

        List<Syl> ReplaceTail(List<Syl> s, int k, string spec, Syl stripT)
        {
            if (stripT != null) stripT.T = 0;
            List<Syl> o2 = s.GetRange(0, s.Count - k);
            int g = ++groupSeq;
            foreach (Syl x in Jamo.ParseSpec(spec)) { x.Group = g; x.Locked = true; o2.Add(x); }
            return o2;
        }

        static string Modern(List<Syl> s)
        {
            StringBuilder sb = new StringBuilder();
            foreach (Syl x in s)
            {
                string c = x.IsHangul && !x.Locked ? Jamo.Compose(x.L, x.V, x.T) : "\0";
                sb.Append(c.Length == 1 ? c : "\0");
            }
            return sb.ToString();
        }

        List<Syl> Dictionary(List<Syl> s, string word)
        {
            string m = Modern(s);
            List<Syl> outp = new List<Syl>();
            int i = 0;
            while (i < s.Count)
            {
                Entry hit = null;
                int carryT = 0;
                foreach (Entry e in o.Letter ? LETTER_FIRST : ENTRIES)
                {
                    carryT = 0;
                    if (i + e.M.Length > m.Length || string.CompareOrdinal(m, i, e.M, 0, e.M.Length) != 0)
                    {
                        if (e.K != Kind.VERB || i != 0) continue;
                        int k = e.M.Length;
                        if (k > m.Length || string.CompareOrdinal(m, 0, e.M, 0, k - 1) != 0) continue;
                        char want = e.M[k - 1], have = m[k - 1];
                        if (want < 0xAC00 || have < 0xAC00 || have > 0xD7A3 || (want - 0xAC00) % 28 != 0
                            || (have - 0xAC00) / 28 != (want - 0xAC00) / 28) continue;
                        carryT = 0x11A7 + (have - 0xAC00) % 28;
                    }
                    if (e.K == Kind.ANY && e.M.Length < 2) continue;
                    if (e.K != Kind.ANY && i != 0) continue;
                    if (carryT != 0 && e.K != Kind.VERB) continue;
                    if (e.K == Kind.NOUN)
                    {
                        string rest = word.Substring(Math.Min(word.Length, e.M.Length));
                        if (!PARTICLES.Contains(rest)) continue;
                    }
                    hit = e; break;
                }
                if (hit == null) { outp.Add(s[i]); i++; continue; }
                int g = ++groupSeq;
                List<Syl> rep = Jamo.ParseSpec(hit.S);
                if (carryT != 0 && rep[rep.Count - 1].T == 0) rep[rep.Count - 1].T = carryT;
                foreach (Syl x in rep) { x.Group = g; x.Locked = true; outp.Add(x); }
                i += hit.M.Length;
            }
            return outp;
        }

        static Syl Lk(Syl x) { x.Locked = true; return x; }

        void AfterStem(List<Syl> s)
        {
            for (int i = 1; i < s.Count; i++)
            {
                Syl a = s[i - 1], b = s[i];
                if (!a.IsHangul || !b.IsHangul) continue;
                if (a.Locked && a.T == 0 && !b.Locked && b.L == Jamo.L_NULL && b.V == Jamo.V_EU)
                {
                    if (b.T == 0 && i + 1 < s.Count) { s.RemoveAt(i); i--; continue; }
                    if (b.T == Jamo.T_M && i + 1 == s.Count) { a.T = Jamo.T_M; s.RemoveAt(i); i--; continue; }
                }
                if (b.Locked && b.L == Jamo.L_NULL && b.V == Jamo.V_ARAEA_I && b.T == 0 && i + 1 < s.Count)
                {
                    Syl c = s[i + 1];
                    if (c.IsHangul && c.L == Jamo.L_G && c.V == Jamo.V_EU && c.Group == b.Group && !Jamo.IsYang(a.V)) b.V = Jamo.V_UI;
                }
                if (b.L == Jamo.L_G && b.V == Jamo.V_O && b.T == 0 && (a.Group == 0 || a.Group != b.Group))
                {
                    bool end = i + 1 == s.Count && (!b.Locked || a.T == 0);
                    if (i + 1 < s.Count) { Syl c = s[i + 1]; end = c.IsHangul && c.L == Jamo.L_J && c.V == Jamo.V_YEO && c.T == 0; }
                    if (!end) continue;
                    if (a.T == Jamo.T_R) { b.L = Jamo.L_NULL; a.NoLink = true; }
                    else if (a.T == 0 && EndsInI(a.V) && a.Locked) b.L = Jamo.L_NULL;
                    else if (i >= 2 && !a.Locked && a.L == Jamo.L_NULL && a.V == Jamo.V_I && a.T == 0) b.L = Jamo.L_NULL;
                }
            }
        }

        static readonly HashSet<string> BANMAL_EXCEPT = new HashSet<string>(new[] {
            "그네", "우리네", "너네", "저네", "아저씨네", "의자", "과자", "모자", "사자", "부자", "감자", "상자", "기자",
            "왕자", "숫자", "액자", "판자", "제자", "환자", "남자", "여자", "주자", "타자", "효자",
            "피자", "원자", "적자", "투자", "혼자", "이자", "단자", "탁자", "자자", "글자", "문자", "한자", "선자" });

        static void Copula(List<Syl> s)
        {
            int n = s.Count;
            if (n < 2) return;
            Syl p = s[n - 2], z = s[n - 1];
            if (p.IsHangul && p.Locked && p.T != 0 && z.IsHangul && !z.Locked && z.L == Jamo.L_D && z.V == Jamo.V_A && z.T == 0)
            {
                z.L = Jamo.L_R;
                s.Insert(n - 1, new Syl(Jamo.L_NULL, Jamo.V_I, 0));
            }
        }

        static readonly HashSet<string> NOMINAL_EXCEPT = new HashSet<string>(new[] {
            "게임", "모임", "책임", "담임", "주임", "위임", "일임", "처임", "기름", "그믐", "고름", "거름", "소름",
            "녹음", "발음", "복음", "잡음", "탁음", "격음", "독음", "축음", "직음", "얼음" });

        void Nominal(List<Syl> s, string word)
        {
            int n = s.Count;
            if (n == 0) return;
            int k = n - 1;
            Syl z = s[k];
            if (!z.IsHangul) return;
            if (n >= 2 && !z.Locked && ((z.L == Jamo.L_NULL && z.T == 0 && (z.V == Jamo.V_E || z.V == Jamo.V_I || z.V == Jamo.V_UI))
                || (z.L == Jamo.L_NULL && z.V == Jamo.V_EU && (z.T == Jamo.T_R || z.T == Jamo.T_N))
                || (z.L == Jamo.L_G && z.V == Jamo.V_WA && z.T == 0) || (z.L == Jamo.L_D && z.V == Jamo.V_O && z.T == 0)))
            {
                k = n - 2; z = s[k];
            }
            string core = word.Substring(0, Math.Max(0, word.Length - (n - 1 - k)));
            if (NOMINAL_EXCEPT.Contains(core) || !z.IsHangul || z.T == 0) return;
            Syl p = k > 0 && s[k - 1].IsHangul ? s[k - 1] : null;
            bool yang = p != null && Jamo.IsYang(p.V);
            if (z.L == Jamo.L_H && (z.V == Jamo.V_A || (z.Locked && z.V == Jamo.V_ARAEA)) && z.T == Jamo.T_M && k >= 1)
            {
                z.T = 0; z.V = Jamo.V_ARAEA; z.Locked = true;
                s.Insert(k + 1, Lk(new Syl(Jamo.L_NULL, Jamo.V_YO, Jamo.T_M)));
                return;
            }
            if (z.Locked) return;
            if (z.L == Jamo.L_D && z.V == Jamo.V_OE && z.T == Jamo.T_M)
            {
                z.V = Jamo.V_ARAEA; z.T = 0; z.Locked = true;
                s.Insert(k + 1, Lk(new Syl(Jamo.L_NULL, Jamo.V_OE, 0)));
                s.Insert(k + 2, Lk(new Syl(Jamo.L_NULL, Jamo.V_YO, Jamo.T_M)));
                return;
            }
            if (k == 1 && p != null && !p.Locked && p.L == Jamo.L_NULL && p.V == Jamo.V_A && p.T == 0
                && z.L == Jamo.L_N && z.V == Jamo.V_I && z.T == Jamo.T_M)
            {
                z.T = 0;
                s.Insert(k + 1, Lk(new Syl(Jamo.L_R, Jamo.V_O, Jamo.T_M)));
                return;
            }
            if (z.L == Jamo.L_NULL && z.V == Jamo.V_I && z.T == Jamo.T_M && p != null)
            {
                if (p.T == 0 && Jamo.AddI(p.V) != 0) { p.V = Jamo.AddI(p.V); z.L = Jamo.L_R; z.V = Jamo.V_O; return; }
                z.T = 0;
                s.Insert(k + 1, Lk(new Syl(Jamo.L_R, Jamo.V_O, Jamo.T_M)));
                return;
            }
            if (z.T == Jamo.T_RM)
            {
                z.T = Jamo.T_R;
                s.Insert(k + 1, new Syl(Jamo.L_NULL, Jamo.IsYang(z.V) ? Jamo.V_O : Jamo.V_U, Jamo.T_M));
                return;
            }
            if ((z.L == Jamo.L_G || z.L == Jamo.L_K) && z.V == Jamo.V_I && z.T == Jamo.T_M && p != null && !p.Locked && p.T != 0)
            {
                z.V = yang ? Jamo.V_YO : Jamo.V_YU; return;
            }
            if (z.V != Jamo.V_EU || z.T != Jamo.T_M) return;
            bool eum = z.L == Jamo.L_NULL || z.L == Jamo.L_S;
            if (eum && p != null && p.T != 0)
            {
                if (p.T == Jamo.T_SS || (p.Locked && p.T == Jamo.T_S))
                {
                    p.T = 0; z.L = Jamo.L_S; z.V = Jamo.V_YO;
                    if (!p.Locked && p.L == Jamo.L_H && p.V == Jamo.V_AE)
                    {
                        p.V = Jamo.V_ARAEA; p.Locked = true;
                        s.Insert(k, Lk(new Syl(Jamo.L_NULL, Jamo.V_YA, 0)));
                    }
                    return;
                }
                if (p.T == Jamo.T_BS) { p.T = Jamo.T_B; z.L = Jamo.L_S; z.V = Jamo.V_U; return; }
                if (z.L == Jamo.L_NULL && p.T != Jamo.T_NG) z.V = yang ? Jamo.V_O : Jamo.V_U;
                return;
            }
            if (eum) return;
            if (core == "모름" && p != null) { p.T = Jamo.T_R; z.V = Jamo.V_O; return; }
            z.V = yang ? Jamo.V_O : Jamo.V_U;
        }

        void SubjectGa(List<Syl> s, string word)
        {
            int n = s.Count;
            if (n < 2 || GA_EXCEPT.Contains(word)) return;
            Syl z = s[n - 1], p = s[n - 2];
            if (!z.IsHangul || z.Locked || z.L != Jamo.L_G || z.V != Jamo.V_A || z.T != 0) return;
            if (!p.IsHangul) return;
            if (p.T != 0) { z.L = Jamo.L_NULL; z.V = Jamo.V_I; return; }
            int v = Jamo.AddI(p.V);
            if (v == 0) return;
            p.V = v;
            s.RemoveAt(n - 1);
        }

        void Particles(List<Syl> s)
        {
            int n = s.Count;
            if (n < 2) return;
            Syl z = s[n - 1];
            if (!z.IsHangul || z.Locked) return;
            int ei = -1;
            if (z.L == Jamo.L_NULL && z.V == Jamo.V_E && z.T == 0) ei = n - 1;
            else if (n >= 3 && z.L == Jamo.L_S && z.V == Jamo.V_EO && z.T == 0)
            {
                Syl y = s[n - 2];
                if (y.IsHangul && !y.Locked && y.L == Jamo.L_NULL && y.V == Jamo.V_E && y.T == 0) { ei = n - 2; z.V = Jamo.V_YEO; }
            }
            if (ei > 0)
            {
                Syl e = s[ei], pe = s[ei - 1];
                if (pe.IsHangul)
                {
                    if (pe.T == 0 && EndsInI(pe.V)) e.V = Jamo.V_YE;
                    else if (Jamo.IsYang(pe.V)) e.V = Jamo.V_AE;
                }
                return;
            }
            if (n >= 3 && z.L == Jamo.L_R && z.V == Jamo.V_O && z.T == 0)
            {
                Syl y = s[n - 2], x = s[n - 3];
                if (!y.Locked && y.L == Jamo.L_NULL && y.V == Jamo.V_EU && y.T == 0 && x.IsHangul && x.Locked && x.T == 0) { s.RemoveAt(n - 2); return; }
            }
            Syl p = s[n - 2];
            if (!p.IsHangul) return;
            if (p.Locked)
            {
                if (p.T != 0)
                {
                    if (z.L == Jamo.L_R && z.V == Jamo.V_EU && z.T == Jamo.T_R) z.L = Jamo.L_NULL;
                    else if (z.L == Jamo.L_N && z.V == Jamo.V_EU && z.T == Jamo.T_N) z.L = Jamo.L_NULL;
                    else if (z.L == Jamo.L_NULL && z.V == Jamo.V_WA && z.T == 0 && p.T != Jamo.T_R) z.L = Jamo.L_G;
                    else if (z.L == Jamo.L_R && z.V == Jamo.V_O && z.T == 0 && p.T != Jamo.T_R)
                    {
                        s.Insert(n - 1, new Syl(Jamo.L_NULL, Jamo.V_EU, 0)); n++; z = s[n - 1];
                    }
                }
                else
                {
                    if (z.L == Jamo.L_NULL && z.V == Jamo.V_EU && z.T == Jamo.T_R) z.L = Jamo.L_R;
                    else if (z.L == Jamo.L_NULL && z.V == Jamo.V_EU && z.T == Jamo.T_N) z.L = Jamo.L_N;
                    else if (z.L == Jamo.L_G && z.V == Jamo.V_WA && z.T == 0) z.L = Jamo.L_NULL;
                    else if (z.L == Jamo.L_NULL && z.V == Jamo.V_I && z.T == 0 && Jamo.AddI(p.V) != 0)
                    {
                        p.V = Jamo.AddI(p.V); s.RemoveAt(n - 1); return;
                    }
                }
            }
            if (z.L == Jamo.L_G && z.V == Jamo.V_WA && z.T == 0 && (p.T == Jamo.T_R || p.T == Jamo.T_R_YEORIN)) { z.L = Jamo.L_NULL; p.NoLink = true; return; }
            if (z.L == Jamo.L_GG && z.V == Jamo.V_E && z.T == 0) { z.L = Jamo.L_SG; z.V = Jamo.V_UI; z.Locked = true; }
        }

        static bool EndsInI(int v)
        {
            return v == Jamo.V_I || v == Jamo.V_AE || v == Jamo.V_E || v == Jamo.V_OE || v == Jamo.V_WI || v == Jamo.V_UI
                || v == Jamo.V_ARAEA_I || v == Jamo.V_YAE || v == Jamo.V_YE || v == Jamo.V_WAE || v == Jamo.V_WE;
        }

        static void Linking(List<Syl> s)
        {
            for (int i = 0; i + 1 < s.Count; i++)
            {
                Syl a = s[i], b = s[i + 1];
                if (!a.IsHangul || !b.IsHangul || a.T == 0 || b.L != Jamo.L_NULL) continue;
                if (a.T == Jamo.T_NG || a.T == Jamo.T_YESIEUNG || a.NoLink) continue;
                if (a.Group != 0 && a.Group == b.Group) continue;
                int[] sp = Jamo.SplitCluster(a.T);
                if (sp != null) { a.T = sp[0]; b.L = sp[1]; continue; }
                int l = a.T == Jamo.T_SS ? Jamo.L_S : Jamo.TToL(a.T);
                if (l != 0) { b.L = l; a.T = 0; }
            }
        }

        static void EightFinals(List<Syl> s)
        {
            for (int i = 0; i < s.Count; i++)
            {
                Syl a = s[i];
                if (!a.IsHangul || a.T == 0) continue;
                Syl b = i + 1 < s.Count && s[i + 1].IsHangul ? s[i + 1] : null;
                int bs = -1;
                if (a.T == Jamo.T_H) bs = 0; else if (a.T == Jamo.T_NH) bs = Jamo.T_N; else if (a.T == Jamo.T_RH) bs = Jamo.T_R;
                if (bs >= 0)
                {
                    if (b != null)
                    {
                        if (b.L == Jamo.L_G) b.L = Jamo.L_K;
                        else if (b.L == Jamo.L_D) b.L = Jamo.L_T;
                        else if (b.L == Jamo.L_J) b.L = Jamo.L_CH;
                        else if (b.L == Jamo.L_B) b.L = Jamo.L_P;
                    }
                    a.T = bs;
                    continue;
                }
                switch (a.T)
                {
                    case Jamo.T_J: case Jamo.T_CH: case Jamo.T_SS: a.T = Jamo.T_S; break;
                    case Jamo.T_T: a.T = Jamo.T_D; break;
                    case Jamo.T_K: case Jamo.T_GG: case Jamo.T_GS: a.T = Jamo.T_G; break;
                    case Jamo.T_P: case Jamo.T_BS: a.T = Jamo.T_B; break;
                    case Jamo.T_NJ: a.T = Jamo.T_N; break;
                    case Jamo.T_RS: case Jamo.T_RT: case Jamo.T_RP: a.T = Jamo.T_R; break;
                }
            }
        }

        string Render(Syl s)
        {
            if (!s.IsHangul) return s.Raw;
            int l = s.L, v = s.V, t = s.T;
            if (!o.Araea) { if (v == Jamo.V_ARAEA) v = Jamo.V_A; else if (v == Jamo.V_ARAEA_I) v = Jamo.V_AE; }
            if (!o.Pansios) { if (l == Jamo.L_PANSIOS) l = Jamo.L_NULL; if (t == Jamo.T_PANSIOS) t = Jamo.T_S; }
            if (!o.Yesieung) { if (l == Jamo.L_YESIEUNG) l = Jamo.L_NULL; if (t == Jamo.T_YESIEUNG) t = Jamo.T_NG; }
            if (!o.Yeorin && t == Jamo.T_R_YEORIN) t = Jamo.T_R;
            if (!o.Beop && l == Jamo.L_BEOP)
            {
                l = Jamo.L_NULL;
                if (v == Jamo.V_EO) v = Jamo.V_WEO; else if (v == Jamo.V_A) v = Jamo.V_WA;
                else if (v == Jamo.V_ARAEA) v = Jamo.V_O; else if (v == Jamo.V_EU) v = Jamo.V_U;
                else if (v == Jamo.V_ARAEA_I) v = Jamo.V_OE; else if (v == Jamo.V_I) v = Jamo.V_WI;
            }
            if (!o.Hapyong)
            {
                if (l == Jamo.L_SG || l == 0x1122) l = Jamo.L_GG;
                else if (l == Jamo.L_SD || l == 0x1120 || l == 0x1123) l = Jamo.L_DD;
                else if (l == Jamo.L_SB) l = Jamo.L_BB;
                else if (l == Jamo.L_BS) l = Jamo.L_SS;
                else if (l == Jamo.L_SJ || l == 0x1127) l = Jamo.L_JJ;
            }
            if (o.Compat && t != 0 && Jamo.Compose(l, v, t).Length > 1)
            {
                if (l == Jamo.L_SG || l == 0x1122) l = Jamo.L_GG;
                else if (l == Jamo.L_SD || l == 0x1120 || l == 0x1123) l = Jamo.L_DD;
                else if (l == Jamo.L_SB) l = Jamo.L_BB;
                else if (l == Jamo.L_BS) l = Jamo.L_SS;
                else if (l == Jamo.L_SJ || l == 0x1127) l = Jamo.L_JJ;
                else if (l == Jamo.L_PANSIOS || l == Jamo.L_YESIEUNG || l == Jamo.L_YEORIN) l = Jamo.L_NULL;
                else if (l == Jamo.L_BEOP) l = Jamo.L_B;
                if (v == Jamo.V_ARAEA) v = Jamo.V_A; else if (v == Jamo.V_ARAEA_I) v = Jamo.V_AE;
                else if (v == Jamo.V_YO_I) v = Jamo.V_YO; else if (v == Jamo.V_YU_I) v = Jamo.V_YU;
                if (t == Jamo.T_YESIEUNG) t = Jamo.T_NG; else if (t == Jamo.T_PANSIOS) t = Jamo.T_S;
                else if (t == Jamo.T_R_YEORIN) t = Jamo.T_R;
            }
            return Jamo.Compose(l, v, t);
        }
    }
}
