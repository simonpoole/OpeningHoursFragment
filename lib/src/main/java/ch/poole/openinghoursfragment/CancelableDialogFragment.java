package ch.poole.openinghoursfragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class CancelableDialogFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    /**
     * Dismiss any instance of this dialog
     * 
     * @param parentFragment the Fragment calling this
     * @param tag the tag used by the Fragment
     */
    protected static void dismissDialog(@NonNull Fragment parentFragment, @NonNull String tag) {
        FragmentManager fm = parentFragment.getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment != null) {
            ft.remove(fragment);
        }
        ft.commit();
    }
}
