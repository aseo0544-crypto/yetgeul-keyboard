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
        static readonly int[] CHO = { @@CHO@@ };
        static readonly int[] JUNG = { @@JUNG@@ };
        static readonly int[] JONG = { @@JONG@@ };

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
        static readonly HashSet<string> PARTICLES = new HashSet<string>(new[] { @@PARTICLES@@ });
        static readonly HashSet<string> GA_EXCEPT = new HashSet<string>(new[] {
            "휴가", "요가", "대가", "고가", "시가", "주가", "사가", "가가", "노가", "부가", "지가", "도가", "자가", "어디가" });

        static void any(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.ANY)); }
        static void noun(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.NOUN)); }
        static void stem(string m, string s) { ENTRIES.Add(new Entry(m, s, Kind.STEM)); }

        static Converter()
        {
@@DICT@@
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
