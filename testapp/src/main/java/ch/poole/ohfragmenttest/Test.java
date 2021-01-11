package ch.poole.ohfragmenttest;

import java.util.ArrayList;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import ch.poole.openinghoursfragment.OnSaveListener;
import ch.poole.openinghoursfragment.OpeningHoursFragment;
import ch.poole.openinghoursfragment.ValueWithDescription;

public class Test extends AppCompatActivity implements OnSaveListener {
    final String LONG_TEST  = "Mo[2] -2 days 10:00-12:00;24/7;week 4-40 PH+2days dawn-09:00;dawn-25:00/25;2010-2100/4 12:01-13:02, 14:00 , 10:00-(sunset+02:00) , 13:00+, 11:01-45:00/46, dawn-dusk, sunrise+ ; 12-16 closed \"ein test\" ; Mo, We 12:01-13:02 ; Apr-Sep 10:01-13:03, Dec 13:03-21:01";
    final String SHORT_TEST = "dawn+";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_openinghours");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        ArrayList<ValueWithDescription> textValues = new ArrayList<>();
        ValueWithDescription yes = new ValueWithDescription("yes", "Yes");
        textValues.add(yes);
        ValueWithDescription no = new ValueWithDescription("no", "No");
        textValues.add(no);

        // ValueWithDescription key = new ValueWithDescription("opening_hours", "Opening hours");
        ValueWithDescription key = new ValueWithDescription("collection_times", "Collection times");
        OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstance(key, null, R.style.Theme_AppCompat_Light_Dialog_Alert, 5, true, textValues);
        openingHoursDialog.show(fm, "fragment_openinghours");
    }

    @Override
    public void save(String key, String openingHours) {
        Log.d("Test", "save got " + openingHours + " for key " + key);
        TestFragment.showDialog(this, openingHours);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.recreate();
    }
}
