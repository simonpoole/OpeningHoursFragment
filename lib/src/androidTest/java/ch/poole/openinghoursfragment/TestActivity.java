package ch.poole.openinghoursfragment;

import androidx.appcompat.app.AppCompatActivity;

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
