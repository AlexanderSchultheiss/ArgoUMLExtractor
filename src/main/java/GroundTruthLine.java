import java.nio.file.Path;

public record GroundTruthLine(Path path, String fileCondition, String blockCondition,
                              String presenceCondition, int start, int end) {


    @Override
    public String toString() {
        return String.format("%s;%s;%s;%s;%d;%d", path.toString(), fileCondition, blockCondition, presenceCondition, start, end);
    }
}
