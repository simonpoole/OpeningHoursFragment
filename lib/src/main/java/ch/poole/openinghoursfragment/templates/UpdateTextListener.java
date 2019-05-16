package ch.poole.openinghoursfragment.templates;

import android.support.annotation.NonNull;

public interface UpdateTextListener {

    /**
     * Update the OH string with a new text
     * 
     * @param newText the new text
     */
    public void updateText(@NonNull String newText);
}
