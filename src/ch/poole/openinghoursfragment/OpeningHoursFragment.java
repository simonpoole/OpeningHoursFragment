package ch.poole.openinghoursfragment;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


public class OpeningHoursFragment extends SherlockDialogFragment {
	
	private static final String DEBUG_TAG = OpeningHoursFragment.class.getSimpleName();
	
	private LayoutInflater inflater = null;
	
	private String openingHoursValue;
	
	/**
     */
    static public OpeningHoursFragment newInstance(String value) {
    	OpeningHoursFragment f = new OpeningHoursFragment();

        Bundle args = new Bundle();
        args.putSerializable("value", value);

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "onAttach");
//        try {
//            mListener = (OnPresetSelectedListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString() + " must implement OnPresetSelectedListener");
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		LinearLayout openingHoursLayout = null;


    	// Inflate the layout for this fragment
    	this.inflater = inflater;
    	openingHoursLayout = (LinearLayout) inflater.inflate(R.layout.openinghours,null);

//    	if (savedInstanceState != null) {
//    		Log.d(DEBUG_TAG,"Restoring from saved state");
//    		parents = (HashMap<Long, String>) savedInstanceState.getSerializable("PARENTS");
//    	} else if (savedParents != null ) {
//    		Log.d(DEBUG_TAG,"Restoring from instance variable");
//    		parents = savedParents;
//    	} else {
    	openingHoursValue = getArguments().getString("value");
//    	}
    	buildForm(openingHoursLayout,openingHoursValue);
    	
		
		return openingHoursLayout;
    }

    /**
     * 
     * @param openingHoursLayout
     * @param openingHoursValue2
     */
    private void buildForm(LinearLayout openingHoursLayout, String openingHoursValue) {
    	EditText text = (EditText) openingHoursLayout.findViewById(R.id.openinghours_string_edit);
    	LinearLayout openingHoursView = (LinearLayout) openingHoursLayout.findViewById(R.id.openinghours_view);
		if (text != null && openingHoursView != null) {
			text.setText((CharSequence) text);
			openingHoursView.removeAllViews();
			ScrollView sv = new ScrollView(getActivity());
			OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHoursValue.getBytes()));
			try {
				ArrayList<Rule> rules = parser.rules();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    	// outState.putSerializable("PARENTS", savedParents);
    }
    
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    	// savedParents  = getParentRelationMap();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
	


	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getOurView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.openinghours_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.openinghours_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.openinghours_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.openinghours_layoutt");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
}
