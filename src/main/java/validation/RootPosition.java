package validation;

import java.nio.file.Path;
import java.util.Objects;

public class RootPosition extends Position {
    private final String value = "ROOT";
    public static final RootPosition INSTANCE = new RootPosition();

    private RootPosition() {
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        return o instanceof RootPosition;
    }

    @Override
    public String[] serializedPosition() {
        return new String[] {value};
    }

    @Override
    public Path filePath() {
        return null;
    }

    @Override
    public int lineNumber() {
        return -1;
    }

    @Override
    public int columnNumber() {
        return -1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
