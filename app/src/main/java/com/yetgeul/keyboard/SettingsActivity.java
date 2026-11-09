package com.yetgeul.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.yetgeul.keyboard.engine.Converter;

/** 앱을 열면 나오는 화면: 키보드 켜는 법, 규칙 설정, 미리 써 보기. */
public class SettingsActivity extends Activity {

    private Typeface oldFace;
    private TextView status;
    private TextView preview;
    private EditText sample;
    private int accent;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try { oldFace = Typeface.createFromAsset(getAssets(), "fonts/yetgeul.ttf"); } catch (Exception e) { oldFace = Typeface.SERIF; }
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true);
        accent = tv.data;

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, dp(40));
        scroll.addView(root);
        scroll.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("옛글 키보드");
        title.setTypeface(oldFace);
        title.setTextSize(34);
        root.addView(title);

        TextView lede = text("문자 앱이나 검색창에서 평소처럼 한글을 치면, 띄어쓸 때마다 15세기 중세국어 표기로 바뀌어 들어갑니다. "
                + "[옛] 키로 켜고 끌 수 있고, ㅅ·ㅇ·ㅂ·ㅏ 같은 키를 길게 누르면 ㅿ·ㆁ·ㅸ·ㆍ도 칠 수 있습니다.", 15);
        root.addView(lede);

        root.addView(header("1. 키보드 켜기"));
        status = text("", 14);
        root.addView(status);
        Button enable = new Button(this);
        enable.setText("설정에서 옛글 키보드 켜기");
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        root.addView(enable);
        Button pick = new Button(this);
        pick.setText("지금 쓰는 키보드를 옛글 키보드로 바꾸기");
        pick.setOnClickListener(v -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker());
        root.addView(pick);

        root.addView(header("2. 써 보기"));
        sample = new EditText(this);
        sample.setHint("여기에 한글을 쳐 보세요");
        sample.setTypeface(oldFace);
        sample.setTextSize(20);
        root.addView(sample);
        TextView pvLabel = text("옛글 키보드를 켜지 않아도, 아래에 변환 결과가 미리 보입니다.", 13);
        root.addView(pvLabel);
        preview = new TextView(this);
        preview.setTypeface(oldFace);
        preview.setTextSize(24);
        preview.setPadding(0, dp(8), 0, dp(8));
        preview.setTextIsSelectable(true);
        root.addView(preview);
        sample.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) { refreshPreview(); }
        });
        sample.setText("나라의 말씀이 중국과 달라 문자와 서로 통하지 아니하므로");

        root.addView(header("3. 바꿀 말투"));
        RadioGroup era = new RadioGroup(this);
        era.setOrientation(RadioGroup.VERTICAL);
        RadioButton c15 = new RadioButton(this);
        c15.setId(View.generateViewId());
        c15.setText("15세기 (훈민정음)  —  ㆍ ㅿ ㆁ ㅸ, 이어적기, 8종성");
        RadioButton cLetter = new RadioButton(this);
        cLetter.setId(View.generateViewId());
        cLetter.setText("조선 후기 편지체  —  걱정하지 마세요 → 념녀ᄒᆞ디 마ᄋᆞᆸ쇼셔");
        cLetter.setTypeface(oldFace);
        era.addView(c15);
        era.addView(cLetter);
        boolean letterOn = Prefs.get(this).getBoolean("letter", false);
        era.check(letterOn ? cLetter.getId() : c15.getId());
        int cLetterId = cLetter.getId();
        era.setOnCheckedChangeListener((g, id) -> {
            Prefs.get(this).edit().putBoolean("letter", id == cLetterId).apply();
            refreshPreview();
        });
        root.addView(era);
        root.addView(text("편지체는 17~19세기 한글 편지 말투예요. 높임말로 치면 윗사람께(ᄒᆞᄋᆞᆸᄂᆞ이다), 반말로 치면 아랫사람에게(ᄒᆞ노라·ᄒᆞ여라) 쓰는 편지가 됩니다. "
                + "편지체에서는 ㅿ·ㆁ·ㅸ·ㆆ 규칙이 꺼집니다.", 13));

        root.addView(header("4. 바꿀 규칙"));
        SharedPreferences p = Prefs.get(this);
        for (String[] r : Prefs.RULES) {
            root.addView(toggle(r[1], r[2], r[0], !"compat".equals(r[0])));
        }
        root.addView(header("5. 자판"));
        root.addView(toggle("길게 누르면 옛 자모", "ㅅ → ㅿ ㅺ ㅼ ㅽ, ㅇ → ㆁ ㆆ, ㅏ → ㆍ", "longPress", true));

        TextView note = text("\n받는 사람의 폰 글꼴이 옛한글을 지원하지 않으면 ㆍ·ㅿ 같은 글자가 풀어져 보일 수 있습니다. "
                + "글꼴: 나눔명조 옛한글 (SIL OFL 1.1).", 12);
        root.addView(note);

        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        boolean enabled = false;
        for (InputMethodInfo info : imm.getEnabledInputMethodList()) {
            if (info.getPackageName().equals(getPackageName())) enabled = true;
        }
        String cur = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = cur != null && cur.startsWith(getPackageName());
        status.setText(selected ? "✓ 옛글 키보드를 쓰고 있습니다."
                : enabled ? "켜져 있습니다. 아래 두 번째 버튼으로 옛글 키보드를 고르세요."
                : "아직 꺼져 있습니다. 첫 번째 버튼을 눌러 '옛글 키보드'를 켜 주세요.");
    }

    private void refreshPreview() {
        preview.setText(new Converter(Prefs.options(this)).convert(sample.getText().toString()));
    }

    private View toggle(String label, String desc, String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 16);
        l.setTypeface(oldFace);
        TextView d = text(desc, 13);
        d.setTypeface(oldFace);
        d.setAlpha(0.7f);
        texts.addView(l);
        texts.addView(d);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.get(this).getBoolean(key, def));
        sw.setOnCheckedChangeListener((v, on) -> {
            Prefs.get(this).edit().putBoolean(key, on).apply();
            refreshPreview();
        });
        row.addView(sw);
        return row;
    }

    private TextView header(String s) {
        TextView t = text(s, 13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(accent);
        t.setPadding(0, dp(24), 0, dp(6));
        return t;
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setLineSpacing(0, 1.25f);
        return t;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
