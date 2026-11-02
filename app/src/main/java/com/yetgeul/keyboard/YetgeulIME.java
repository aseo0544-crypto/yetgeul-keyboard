package com.yetgeul.keyboard;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.yetgeul.keyboard.engine.ComposeSession;
import com.yetgeul.keyboard.engine.Converter;
import com.yetgeul.keyboard.engine.HangulComposer;

import java.util.Collections;

/** 옛글 키보드 입력기: 문자 앱·검색창 어디서나 치는 한글을 중세국어 표기로 바꿔 넣음. */
public class YetgeulIME extends InputMethodService implements KeyboardView.Listener {

    private KeyboardView view;
    private final ComposeSession session = new ComposeSession(new Converter());

    @Override public View onCreateInputView() {
        view = new KeyboardView(this);
        view.setListener(this);
        return view;
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        session.setConverter(new Converter(Prefs.options(this)));
        session.setOldMode(Prefs.oldMode(this));
        session.clear();
        if (view != null) {
            view.setOldMode(session.isOldMode());
            view.setLongPressEnabled(Prefs.longPress(this));
            int cls = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            if (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE) view.setLayer(KeyboardView.Layer.SYM);
            else if (view.getLayer() == KeyboardView.Layer.SYM) view.setLayer(KeyboardView.Layer.KO);
            view.setCandidates(Collections.emptyList());
        }
    }

    @Override public void onFinishInput() {
        super.onFinishInput();
        session.clear();
    }

    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        // 사용자가 커서를 옮기면 조합을 끝냄
        if (session.isActive() && (candidatesStart == -1 || newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            session.clear();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.finishComposingText();
            if (view != null) view.setCandidates(Collections.emptyList());
        }
    }

    // ───────────────────────── 키 처리

    @Override public void onKey(KeyboardView.Key key, char ch) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (key.type) {
            case CHAR:
                if (HangulComposer.isVowel(ch) || HangulComposer.isConsonant(ch)) {
                    session.type(ch);
                    if (view.isShift()) view.setShift(false);
                    update();
                } else {
                    commitAll(ic);
                    ic.commitText(String.valueOf(ch), 1);
                    if (view.isShift() && view.getLayer() == KeyboardView.Layer.EN) view.setShift(false);
                }
                break;
            case SHIFT:
                view.setShift(!view.isShift());
                break;
            case DEL:
                if (session.backspace()) update();
                else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                break;
            case SPACE:
                onSpace(ic);
                break;
            case ENTER:
                commitAll(ic);
                EditorInfo ei = getCurrentInputEditorInfo();
                int action = ei == null ? EditorInfo.IME_ACTION_NONE : ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                boolean noAction = ei == null || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
                if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action);
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
                }
                break;
            case SYM:
                commitAll(ic);
                view.setLayer(KeyboardView.Layer.SYM);
                break;
            case ABC:
                commitAll(ic);
                view.setLayer(KeyboardView.Layer.KO);
                break;
            case LANG:
                commitAll(ic);
                view.setLayer(view.getLayer() == KeyboardView.Layer.EN ? KeyboardView.Layer.KO : KeyboardView.Layer.EN);
                break;
            case OLD:
                commitAll(ic);
                boolean on = !session.isOldMode();
                session.setOldMode(on);
                Prefs.setOldMode(this, on);
                view.setOldMode(on);
                break;
        }
    }

    private void onSpace(InputConnection ic) {
        if (!session.isOldMode()) {
            commitAll(ic);
            ic.commitText(" ", 1);
            return;
        }
        switch (session.space()) {
            case WORD_ADDED:
                if (session.wordCount() >= ComposeSession.MAX_WORDS) commitAll(ic);
                else update();
                break;
            case FLUSH:
                commitAll(ic);
                break;
            case NOTHING:
                ic.commitText(" ", 1);
                break;
        }
    }

    @Override public void onCandidate(int index) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (index == 0) onSpace(ic);
        else { session.keepCurrentRaw(); update(); }
    }

    private void update() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.setComposingText(session.composing(), 1);
        view.setCandidates(session.candidates());
    }

    private void commitAll(InputConnection ic) {
        if (!session.isActive()) return;
        String text = session.finishAll();
        ic.commitText(text, 1);
        view.setCandidates(Collections.emptyList());
    }
}
