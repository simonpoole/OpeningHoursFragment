package ch.poole.openinghoursfragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class CancelableDialogFragment extends DialogFragment {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }
}
