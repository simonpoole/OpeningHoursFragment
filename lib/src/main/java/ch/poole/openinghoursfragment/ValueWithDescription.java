package ch.poole.openinghoursfragment;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ValueWithDescription implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String      value;
    private final String      description;

    /**
     * Construct a new instance
     * 
     * @param value the value
     * @param description the description if any
     */
    public ValueWithDescription(@NonNull final String value, @Nullable final String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getDescription() != null ? getDescription() : getValue();
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ValueWithDescription other = (ValueWithDescription) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
