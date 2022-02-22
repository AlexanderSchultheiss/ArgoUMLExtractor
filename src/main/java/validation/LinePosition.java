package validation;

import java.nio.file.Path;
import java.util.Objects;

public class LinePosition extends Position{
    private final String filePosition;
    private final int lineNumber;
    private final int columnNumber;

    public LinePosition(final String filePosition, final int lineNumber, final int columnNumber) {
        this.filePosition = filePosition;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof LinePosition)) return false;
        final LinePosition that = (LinePosition) o;
        return lineNumber == that.lineNumber && columnNumber == that.columnNumber && Objects.equals(filePosition, that.filePosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePosition, lineNumber, columnNumber);
    }

    @Override
    public String[] serializedPosition() {
        return new String[]{"LINE", filePosition, String.valueOf(lineNumber), String.valueOf(columnNumber)};
    }

    @Override
    public Path filePath() {
        return Path.of(filePosition);
    }

    @Override
    public int lineNumber() {
        return lineNumber;
    }

    @Override
    public int columnNumber() {
        return columnNumber;
    }


}
