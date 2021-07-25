package ch.poole.openinghoursfragment;

import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity implements OnSaveListener {

    String result;

    @Override
    public void save(String key, String openingHours) {
        result = openingHours;
    }

    public String getResult() {
        return result;
    }
}
