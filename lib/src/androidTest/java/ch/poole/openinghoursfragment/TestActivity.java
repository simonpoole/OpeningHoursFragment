package ch.poole.openinghoursfragment;

import android.support.v7.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity implements OnSaveListener {

    String result;

    @Override
    public void save(String key, String openingHours) {
        System.out.println("save got " + openingHours + " for key " + key);
        result = openingHours;
    }

    public String getResult() {
        return result;
    }
}
