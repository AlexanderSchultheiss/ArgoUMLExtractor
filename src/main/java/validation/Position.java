package validation;

import java.io.Serializable;
import java.nio.file.Path;

public abstract class Position implements Serializable {

    public static Position fromSerializedPosition(final String[] serializedPosition) {
        final String type = serializedPosition[0];
        return switch (type) {
            case "FILE" -> new FilePosition(serializedPosition[1]);
            case "LINE" -> new LinePosition(serializedPosition[1], Integer.parseInt(serializedPosition[2]), Integer.parseInt(serializedPosition[3]));
            case "ROOT" -> RootPosition.INSTANCE;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    public abstract String[] serializedPosition();

    public abstract Path filePath();

    public abstract int lineNumber();

    public abstract int columnNumber();
}
