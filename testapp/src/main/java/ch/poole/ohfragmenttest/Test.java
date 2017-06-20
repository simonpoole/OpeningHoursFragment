package ch.poole.ohfragmenttest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import ch.poole.openinghoursfragment.OnSaveListener;
import ch.poole.openinghoursfragment.OpeningHoursFragment;


public class Test extends AppCompatActivity implements OnSaveListener {

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

        OpeningHoursFragment openingHoursDialog 
        	= OpeningHoursFragment.newInstance("opening_hours","Mo[2] -2 days 10:00-12:00;24/7;week 4-40 PH+2days dawn-09:00;dawn-25:00/25;2010-2100/4 12:01-13:02, 14:00 , 10:00-(sunset+02:00) , 13:00+, 11:01-45:00/46, dawn-dusk, sunrise+ ; 12-16 closed \"ein test\" ; Mo, We 12:01-13:02 ; Apr-Sep 10:01-13:03, Dec 13:03-21:01",
        			R.style.Theme_AppCompat_Light_Dialog_Alert, 5);
        openingHoursDialog.show(fm, "fragment_openinghours");
	}

	@Override
	public void save(String key, String openingHours) {
		Log.d("TEST", "save got " + openingHours + " for key " + key);
		
	}
}
