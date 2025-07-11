package ch.poole.ohfragmenttest;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import ch.poole.openinghoursfragment.OnSaveListener;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursfragment.ValueWithDescription;

public class TestFragment extends DialogFragment implements OnSaveListener {

    private static final String DEBUG_TAG = TestFragment.class.getSimpleName();

    private static final String TAG = "fragment_test";

    private static final String KEY_OH = "oh";

    /**
     
     */
    static public void showDialog(FragmentActivity activity, String oh) {
        dismissDialog(activity);
        try {

            FragmentManager fm = activity.getSupportFragmentManager();
            TestFragment testFragment = newInstance(oh);
            testFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    private static void dismissDialog(FragmentActivity activity) {
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     */
    static private TestFragment newInstance(String oh) {
        TestFragment f = new TestFragment();

        Bundle args = new Bundle();
        args.putSerializable(KEY_OH, oh);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    private String oh;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("test fragment mode");
        oh = getArguments().getString(KEY_OH);

        builder.setPositiveButton(android.R.string.ok, null);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        ArrayList<ValueWithDescription> textValues = new ArrayList<>();
        ValueWithDescription textValue = new ValueWithDescription("test_value", "Test description");
        textValues.add(textValue);
        OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstanceForFragment(new ValueWithDescription("fee", null), oh,
                ch.poole.openinghoursfragment.R.style.Theme_DialogLight, 5, false, textValues);
        FragmentManager fm = getChildFragmentManager();
        openingHoursDialog.show(fm, "fragment_openinghours");
    }

    @Override
    public void save(String key, String openingHours) {
        Log.d("TestFragment", "save got " + openingHours + " for key " + key);
    }
}
