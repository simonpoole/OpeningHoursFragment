package ch.poole.openinghoursfragment;

import java.io.Serializable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ValueWithDescription implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String value;
    private final String description;
    
    public ValueWithDescription(@NonNull final String value, @Nullable final String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * @return the value
     */
    String getValue() {
        return value;
    }

    /**
     * @return the description
     */
    String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value;
    }
}
