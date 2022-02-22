package validation;

import java.nio.file.Path;
import java.util.Objects;

public class FilePosition extends Position{
    private final String path;

    public FilePosition(final String path) {
        this.path = path;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FilePosition)) return false;
        final FilePosition that = (FilePosition) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public String[] serializedPosition() {
        return new String[] {"FILE", path};
    }

    @Override
    public Path filePath() {
        return Path.of(path);
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
        return Objects.hash(path);
    }


}
