package ch.poole.openinghoursfragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

public class Test extends SherlockFragmentActivity {

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

        OpeningHoursFragment openingHoursDialog = OpeningHoursFragment.newInstance("12:01-13:02, 14:00 , 10:00-sunset , 11:01-45:00/46 ; 12-16 closed \"ein test\" ; Mo, We 12:01-13:02 ; Apr-Sep 10:01-13:03, Dec 13:03-21:01");
        openingHoursDialog.show(fm, "fragment_openinghours");
	}
}
