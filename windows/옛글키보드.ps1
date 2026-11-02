# 옛글 키보드 PC — 한글을 15세기 중세국어 표기로 바꿔 줍니다.
# 실행: 이 파일을 오른쪽 클릭 → "PowerShell에서 실행"  (설치·관리자 권한 필요 없음)
# 사용: 아무 곳에서나 치고 Ctrl+Alt+Y → 그 줄(또는 고른 글자)이 옛 표기로 바뀜. 다시 누르면 원래대로.
#       Ctrl+Alt+W 변환 창, Ctrl+Alt+O 켜기/끄기. 오른쪽 아래 알림 영역 아이콘에서 설정.
# 이 파일은 windows/gen.py가 만듭니다. 고칠 때는 windows/app.ps1.tpl과 엔진(Java)을 고치세요.
param([switch]$SelfTest)

$ErrorActionPreference = 'Stop'

$engineSource = @'
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
            Hapyong = true, Yeoneol = true, PalJong = true, Grammar = true;
    }

    public class Converter
    {
        enum Kind { ANY, NOUN, STEM }
        class Entry { public string M, S; public Kind K; public Entry(string m, string s, Kind k) { M = m; S = s; K = k; } }

        static readonly List<Entry> ENTRIES = new List<Entry>();
        static readonly HashSet<string> PARTICLES = new HashSet<string>(new[] { "", "이", "가", "은", "는", "을", "를", "의", "에", "에서", "에게", "도", "만", "과", "와", "로", "으로", "처럼", "까지", "부터", "마다", "이다", "이고", "이며", "이나", "나", "께", "한테", "보다", "들", "들이", "들은", "들을", "들의", "이여", "아", "야", "입니다", "이에요", "예요", "이요" });
        static readonly HashSet<string> GA_EXCEPT = new HashSet<string>(new[] {
            "휴가", "요가", "대가", "고가", "시가", "주가", "사가", "가가", "노가", "부가", "지가", "도가", "자가", "어디가" });

        static void any(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.ANY)); }
        static void noun(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.NOUN)); }
        static void stem(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.STEM)); }

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
            any("에게서", "{ㅇㆎ}그{ㆁㅔ}셔");
            any("에게", "{ㅇㆎ}그{ㆁㅔ}");
            any("한테", "{ㅇㆎ}그{ㆁㅔ}");
            any("께서", "{ㅺㅢ}셔");
            any("에서", "{ㅇㆎ}셔");
            any("처럼", "{ㄱㆍ}티");
            any("까지", "{ㅺㆍ}지");
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
            ENTRIES.Sort(delegate(Entry a, Entry b) { return b.M.Length - a.M.Length; });
        }

        readonly Options o;
        int groupSeq = 0;

        public Converter(Options options) { o = options; }
        public Converter() : this(new Options()) { }

        static readonly Regex WORD = new Regex("[가-힣ᄀ-ᇿꥠ-꥿ힰ-퟿]+");
        static readonly Regex P_DIFFER = new Regex("([가-힣]+?)(과|와)(\\s+)(다르|달라|달리|다른|다릅|다를|달랐)");
        static readonly Regex P_NEG = new Regex("([가-힣]+?)지(\\s+)(않|아니)");
        static readonly Regex P_WANT = new Regex("([가-힣]+?)고(\\s+)싶(습니다|어요|으면|었다|다|어|은)");
        static readonly Regex P_WILL = new Regex("([가-힣]*)([가-힣])\\s+(것입니다|것이에요|거예요|것이다|거야|거다)");

        public string Convert(string text)
        {
            if (o.Grammar) text = PhraseRules(text);
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
                List<Syl> syls = ConvertWord(parts[i]);
                if (o.Yeorin && o.Araea && syls.Count > 0 && i + 2 < parts.Count && parts[i + 1] == " " && isWord[i + 2])
                {
                    Syl z = syls[syls.Count - 1];
                    if (z.IsHangul && z.L == Jamo.L_H && z.V == Jamo.V_ARAEA && z.T == Jamo.T_R) z.T = Jamo.T_R_YEORIN;
                }
                foreach (Syl s in syls) sb.Append(Render(s));
            }
            return sb.ToString();
        }

        static string PhraseRules(string t)
        {
            t = P_DIFFER.Replace(t, "$1에$3$4");
            t = P_NEG.Replace(t, "$1디$2$3");
            t = P_WANT.Replace(t, delegate(Match m)
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

        public List<Syl> ConvertWord(string word)
        {
            List<Syl> s = Jamo.Parse(word, true);
            if (o.Grammar) s = Endings(s);
            s = Dictionary(s, word);
            if (o.Grammar) SubjectGa(s, word);
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
            if (o.Yesieung) foreach (Syl a in s) if (a.IsHangul && a.T == Jamo.T_NG) a.T = Jamo.T_YESIEUNG;
            return s;
        }

        List<Syl> Endings(List<Syl> s)
        {
            int n = s.Count;
            string w = Modern(s);
            string[,] fix = {
                {"입니다", "이{ㆁㅣ}다"}, {"입니까", "이{ㆁㅣ}잇가"},
                {"습니다", "{ㄴㆍ}이다"}, {"습니까", "{ㄴㆍ}니잇가"},
                {"으십시오", "으{ㅅㅛ}{ㅅㅕ}"}, {"십시오", "{ㅅㅛ}{ㅅㅕ}"},
                {"으세요", "으{ㅅㅛ}{ㅅㅕ}"}, {"세요", "{ㅅㅛ}{ㅅㅕ}"},
                {"으므로", "을{ㅆㆎ}"}, {"으니까", "으니"}, {"는다", "{ㄴㆍ}다"} };
            for (int i = 0; i < fix.GetLength(0); i++)
                if (w.EndsWith(fix[i, 0]) && w.Length > fix[i, 0].Length) return ReplaceTail(s, fix[i, 0].Length, fix[i, 1], null);
            if (n >= 3)
            {
                Syl p = s[n - 3];
                if (p.IsHangul && !p.Locked && p.T == Jamo.T_B)
                {
                    if (w.EndsWith("니다")) return ReplaceTail(s, 2, "{ㄴㆍ}이다", p);
                    if (w.EndsWith("니까")) return ReplaceTail(s, 2, "{ㄴㆍ}니잇가", p);
                }
            }
            if (n >= 3 && w.EndsWith("므로"))
            {
                Syl p = s[n - 3];
                if (p.IsHangul && p.T == 0) { p.T = Jamo.T_R; return ReplaceTail(s, 2, "{ㅆㆎ}", null); }
            }
            if (n >= 3 && w.EndsWith("니까")) return ReplaceTail(s, 2, "니", null);
            if (n >= 2 && w.EndsWith("다"))
            {
                Syl p = s[n - 2];
                if (p.IsHangul && !p.Locked && p.T == Jamo.T_N && p.L != Jamo.L_NULL) return ReplaceTail(s, 1, "{ㄴㆍ}다", p);
            }
            return s;
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
                foreach (Entry e in ENTRIES)
                {
                    if (string.CompareOrdinal(m, i, e.M, 0, e.M.Length) != 0 || i + e.M.Length > m.Length) continue;
                    if (e.K == Kind.ANY && e.M.Length < 2) continue;
                    if (e.K != Kind.ANY && i != 0) continue;
                    if (e.K == Kind.NOUN)
                    {
                        string rest = word.Substring(Math.Min(word.Length, e.M.Length));
                        if (!PARTICLES.Contains(rest)) continue;
                    }
                    hit = e; break;
                }
                if (hit == null) { outp.Add(s[i]); i++; continue; }
                int g = ++groupSeq;
                foreach (Syl x in Jamo.ParseSpec(hit.S)) { x.Group = g; x.Locked = true; outp.Add(x); }
                i += hit.M.Length;
            }
            return outp;
        }

        void SubjectGa(List<Syl> s, string word)
        {
            int n = s.Count;
            if (n < 2 || GA_EXCEPT.Contains(word)) return;
            Syl z = s[n - 1], p = s[n - 2];
            if (!z.IsHangul || z.Locked || z.L != Jamo.L_G || z.V != Jamo.V_A || z.T != 0) return;
            if (!p.IsHangul || p.T != 0) return;
            int v = Jamo.AddI(p.V);
            if (v == 0) return;
            p.V = v;
            s.RemoveAt(n - 1);
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
            return Jamo.Compose(l, v, t);
        }
    }
}

'@

$helperSource = @'
using System;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace Yetgeul
{
    public delegate void HotkeyHandler(int id);

    public class HotkeyWindow : NativeWindow, IDisposable
    {
        [DllImport("user32.dll")] static extern bool RegisterHotKey(IntPtr hWnd, int id, uint mod, uint vk);
        [DllImport("user32.dll")] static extern bool UnregisterHotKey(IntPtr hWnd, int id);
        public event HotkeyHandler Pressed;
        public HotkeyWindow() { CreateHandle(new CreateParams()); }
        public bool Register(int id, uint mod, Keys key) { return RegisterHotKey(Handle, id, mod, (uint)key); }
        protected override void WndProc(ref Message m)
        {
            if (m.Msg == 0x0312 && Pressed != null) Pressed(m.WParam.ToInt32());
            base.WndProc(ref m);
        }
        public void Dispose() { for (int i = 1; i <= 3; i++) UnregisterHotKey(Handle, i); DestroyHandle(); }
    }

    public static class Native
    {
        [DllImport("user32.dll")] public static extern short GetAsyncKeyState(int vk);
        [DllImport("kernel32.dll")] public static extern IntPtr GetConsoleWindow();
        [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
        public static bool ModifiersDown()
        {
            int[] vks = { 0x10, 0x11, 0x12, 0x5B, 0x5C }; // Shift Ctrl Alt Win
            foreach (int v in vks) if ((GetAsyncKeyState(v) & 0x8000) != 0) return true;
            return false;
        }
    }
}
'@

Add-Type -TypeDefinition $engineSource -Language CSharp
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
Add-Type -TypeDefinition $helperSource -Language CSharp -ReferencedAssemblies System.Windows.Forms

if ($SelfTest) {
    $c = New-Object Yetgeul.Converter
    $samples = @('나라의 말씀이 중국과 달라 문자와 서로 통하지 아니하므로', '사랑합니다', '안녕하세요', '하늘과 땅이 처음 생길 때')
    foreach ($s in $samples) { "$s`n  -> $($c.Convert($s))" }
    return
}

# 한 번만 실행되게
$mutex = New-Object System.Threading.Mutex($false, 'Local\YetgeulKeyboardPC')
if (-not $mutex.WaitOne(0)) {
    [System.Windows.Forms.MessageBox]::Show('옛글 키보드가 이미 실행 중입니다. 오른쪽 아래 알림 영역을 확인하세요.', '옛글 키보드') | Out-Null
    return
}

# 콘솔 창 숨기기
[Yetgeul.Native]::ShowWindow([Yetgeul.Native]::GetConsoleWindow(), 0) | Out-Null

# ───────────── 설정 (%APPDATA%\Yetgeul\settings.json)
$settingsDir = Join-Path $env:APPDATA 'Yetgeul'
$settingsFile = Join-Path $settingsDir 'settings.json'
$ruleNames = [ordered]@{
    Araea = '아래아 ㆍ  (하늘 → ᄒᆞᄂᆞᆯ)'
    Pansios = '반치음 ㅿ  (마음 → ᄆᆞᅀᆞᆷ)'
    Yesieung = '옛이응 ㆁ  (백성 → ᄇᆡᆨ셔ᇰ)'
    Beop = '순경음 비읍 ㅸ  (더워 → 더ᄫᅥ)'
    Hapyong = '합용병서  (땅이 → ᄯᅡ히)'
    Yeoneol = '이어적기  (사람이 → 사ᄅᆞ미)'
    PalJong = '8종성  (꽃 → 곳)'
    Yeorin = '여린히읗 ㆆ  (할 바 → ᄒᆞᇙ 바)'
    Grammar = '옛 문법·말투  (합니다 → ᄒᆞᄂᆞ이다)'
}
$state = @{ Enabled = $true }
foreach ($k in $ruleNames.Keys) { $state[$k] = $true }
if (Test-Path $settingsFile) {
    try {
        $saved = Get-Content $settingsFile -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($p in $saved.PSObject.Properties) { $state[$p.Name] = [bool]$p.Value }
    } catch { }
}
function Save-Settings {
    New-Item -ItemType Directory -Force $settingsDir | Out-Null
    ($state | ConvertTo-Json) | Set-Content $settingsFile -Encoding UTF8
}
function New-Converter {
    $o = New-Object Yetgeul.Options
    foreach ($k in $ruleNames.Keys) { $o.$k = $state[$k] }
    New-Object Yetgeul.Converter -ArgumentList $o
}
$script:conv = New-Converter

# ───────────── 단축키로 바꾸기
$script:lastOut = $null
$script:lastIn = $null

function Get-ClipText {
    try { if ([System.Windows.Forms.Clipboard]::ContainsText()) { return [System.Windows.Forms.Clipboard]::GetText() } } catch { }
    return $null
}

function Convert-InPlace {
    if (-not $state.Enabled) { return }
    # Ctrl·Alt를 뗄 때까지 기다림 (누른 채로 복사하면 엉뚱한 키가 됨)
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ([Yetgeul.Native]::ModifiersDown() -and $sw.ElapsedMilliseconds -lt 1500) { Start-Sleep -Milliseconds 20 }
    $saved = Get-ClipText
    try {
        [System.Windows.Forms.Clipboard]::Clear()
        [System.Windows.Forms.SendKeys]::SendWait('^c')
        Start-Sleep -Milliseconds 120
        $text = Get-ClipText
        if ([string]::IsNullOrEmpty($text)) {
            # 고른 글자가 없으면 커서가 있는 줄 전체
            [System.Windows.Forms.SendKeys]::SendWait('{END}+{HOME}')
            Start-Sleep -Milliseconds 60
            [System.Windows.Forms.SendKeys]::SendWait('^c')
            Start-Sleep -Milliseconds 120
            $text = Get-ClipText
        }
        if ([string]::IsNullOrEmpty($text)) { return }
        if ($text -eq $script:lastOut -and $script:lastIn) { $out = $script:lastIn }   # 다시 누르면 원래대로
        else { $out = $script:conv.Convert($text); $script:lastIn = $text; $script:lastOut = $out }
        [System.Windows.Forms.Clipboard]::SetText($out)
        [System.Windows.Forms.SendKeys]::SendWait('^v')
        Start-Sleep -Milliseconds 150
    } finally {
        try {
            if ($saved) { [System.Windows.Forms.Clipboard]::SetText($saved) } else { [System.Windows.Forms.Clipboard]::Clear() }
        } catch { }
    }
}

# ───────────── 변환 창
$script:form = $null
function Show-ConvertWindow {
    if ($script:form -and -not $script:form.IsDisposed) { $script:form.Activate(); return }
    $f = New-Object System.Windows.Forms.Form
    $f.Text = '옛글 변환 창'
    $f.TopMost = $true
    $f.Size = New-Object System.Drawing.Size(520, 360)
    $f.StartPosition = 'CenterScreen'
    $f.Font = New-Object System.Drawing.Font('Malgun Gothic', 10)
    $inBox = New-Object System.Windows.Forms.TextBox
    $inBox.Multiline = $true; $inBox.ScrollBars = 'Vertical'
    $inBox.Dock = 'Top'; $inBox.Height = 110
    $inBox.Font = New-Object System.Drawing.Font('Malgun Gothic', 12)
    $outBox = New-Object System.Windows.Forms.TextBox
    $outBox.Multiline = $true; $outBox.ReadOnly = $true; $outBox.ScrollBars = 'Vertical'
    $outBox.Dock = 'Fill'
    $outBox.Font = New-Object System.Drawing.Font('Malgun Gothic', 16)
    $bar = New-Object System.Windows.Forms.FlowLayoutPanel
    $bar.Dock = 'Bottom'; $bar.Height = 44; $bar.FlowDirection = 'RightToLeft'; $bar.Padding = '6,6,6,6'
    $copy = New-Object System.Windows.Forms.Button
    $copy.Text = '복사'; $copy.Width = 90; $copy.Height = 30
    $clear = New-Object System.Windows.Forms.Button
    $clear.Text = '지우기'; $clear.Width = 90; $clear.Height = 30
    $bar.Controls.AddRange(@($copy, $clear))
    $inBox.Add_TextChanged({ $outBox.Text = $script:conv.Convert($inBox.Text) })
    $copy.Add_Click({ if ($outBox.Text) { [System.Windows.Forms.Clipboard]::SetText($outBox.Text); $copy.Text = '복사됨' } })
    $clear.Add_Click({ $inBox.Clear(); $copy.Text = '복사'; $inBox.Focus() })
    $f.Controls.Add($outBox); $f.Controls.Add($inBox); $f.Controls.Add($bar)
    $inBox.Text = '나라의 말씀이 중국과 달라'
    $f.Add_Shown({ $inBox.SelectAll(); $inBox.Focus() })
    $script:form = $f
    $f.Show()
}

# ───────────── 알림 영역 아이콘
function New-TrayIcon([bool]$on) {
    $bmp = New-Object System.Drawing.Bitmap(32, 32)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $bg = if ($on) { [System.Drawing.Color]::FromArgb(38, 68, 107) } else { [System.Drawing.Color]::FromArgb(120, 120, 120) }
    $g.FillRectangle((New-Object System.Drawing.SolidBrush($bg)), 0, 0, 32, 32)
    $font = New-Object System.Drawing.Font('Malgun Gothic', 15, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = 'Center'; $fmt.LineAlignment = 'Center'
    $g.DrawString('옛', $font, [System.Drawing.Brushes]::White, (New-Object System.Drawing.RectangleF(0, 0, 32, 32)), $fmt)
    $g.Dispose()
    [System.Drawing.Icon]::FromHandle($bmp.GetHicon())
}

$tray = New-Object System.Windows.Forms.NotifyIcon
$tray.Text = '옛글 키보드 (Ctrl+Alt+Y)'
$menu = New-Object System.Windows.Forms.ContextMenuStrip
$miEnabled = $menu.Items.Add('옛글 변환 켜짐   Ctrl+Alt+O')
$miWindow = $menu.Items.Add('변환 창 열기   Ctrl+Alt+W')
$menu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null
$miRules = New-Object System.Windows.Forms.ToolStripMenuItem('바꿀 규칙')
$menu.Items.Add($miRules) | Out-Null
foreach ($k in $ruleNames.Keys) {
    $item = New-Object System.Windows.Forms.ToolStripMenuItem($ruleNames[$k])
    $item.Checked = $state[$k]
    $item.Tag = $k
    $item.CheckOnClick = $true
    $item.Add_CheckedChanged({ param($s, $e) $state[$s.Tag] = $s.Checked; Save-Settings; $script:conv = New-Converter })
    $miRules.DropDownItems.Add($item) | Out-Null
}
$startupLink = Join-Path ([Environment]::GetFolderPath('Startup')) '옛글 키보드.lnk'
$miStartup = New-Object System.Windows.Forms.ToolStripMenuItem('Windows 켤 때 자동 실행')
$miStartup.Checked = Test-Path $startupLink
$miStartup.Add_Click({
    if (Test-Path $startupLink) { Remove-Item $startupLink -Force; $miStartup.Checked = $false }
    else {
        $ws = New-Object -ComObject WScript.Shell
        $lnk = $ws.CreateShortcut($startupLink)
        $lnk.TargetPath = (Get-Command powershell.exe).Source
        $lnk.Arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$PSCommandPath`""
        $lnk.Save()
        $miStartup.Checked = $true
    }
})
$menu.Items.Add($miStartup) | Out-Null
$menu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null
$miQuit = $menu.Items.Add('끝내기')

function Update-Enabled {
    $miEnabled.Text = $(if ($state.Enabled) { '✓ 옛글 변환 켜짐   Ctrl+Alt+O' } else { '옛글 변환 꺼짐   Ctrl+Alt+O' })
    $tray.Icon = New-TrayIcon $state.Enabled
}
function Toggle-Enabled { $state.Enabled = -not $state.Enabled; Save-Settings; Update-Enabled
    $tray.ShowBalloonTip(1500, '옛글 키보드', $(if ($state.Enabled) { '켜짐: Ctrl+Alt+Y로 바꿉니다' } else { '꺼짐' }), 'Info') }

$miEnabled.Add_Click({ Toggle-Enabled })
$miWindow.Add_Click({ Show-ConvertWindow })
$tray.ContextMenuStrip = $menu
$tray.Add_DoubleClick({ Show-ConvertWindow })
Update-Enabled
$tray.Visible = $true

# ───────────── 전역 단축키
$hot = New-Object Yetgeul.HotkeyWindow
$MOD_ALT = 1; $MOD_CTRL = 2; $MOD_NOREPEAT = 0x4000
$ok1 = $hot.Register(1, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::Y)
$ok2 = $hot.Register(2, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::W)
$ok3 = $hot.Register(3, $MOD_CTRL + $MOD_ALT + $MOD_NOREPEAT, [System.Windows.Forms.Keys]::O)
$hot.add_Pressed({
    param($id)
    switch ($id) {
        1 { Convert-InPlace }
        2 { Show-ConvertWindow }
        3 { Toggle-Enabled }
    }
})

$msg = if ($ok1) { '실행됐습니다. 치고 나서 Ctrl+Alt+Y를 누르면 옛 표기로 바뀝니다.' } else { 'Ctrl+Alt+Y를 다른 프로그램이 쓰고 있어 등록하지 못했습니다. 변환 창(아이콘 더블클릭)을 쓰세요.' }
$tray.ShowBalloonTip(3000, '옛글 키보드', $msg, 'Info')

$miQuit.Add_Click({
    $tray.Visible = $false
    $hot.Dispose()
    [System.Windows.Forms.Application]::Exit()
})

$ctx = New-Object System.Windows.Forms.ApplicationContext
[System.Windows.Forms.Application]::Run($ctx)
$tray.Dispose()
$mutex.ReleaseMutex()
