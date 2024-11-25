package ch.poole.openinghoursfragment;

import android.text.TextWatcher;

abstract class DefaultTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // empty
    }
}