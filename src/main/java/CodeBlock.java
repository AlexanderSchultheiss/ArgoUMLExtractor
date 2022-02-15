import org.logicng.formulas.Formula;


public final class CodeBlock {
    private final int start;
    private final int end;
    private final Formula blockCondition;
    private final Formula presenceCondition;

    private CodeBlock(int start, int end, Formula blockCondition, Formula presenceCondition) {
        this.start = start+1;
        this.end = end;
        this.blockCondition = blockCondition;
        this.presenceCondition = presenceCondition;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public Formula blockCondition() {
        return blockCondition;
    }

    public Formula getPresenceCondition() {
        return presenceCondition;
    }

    public static UnderConstruction startBuildingBlock(int startLine, Formula blockCondition, Formula presenceCondition) {
        return new UnderConstruction(startLine, blockCondition, presenceCondition);
    }

    public static CodeBlock finishBlock(final UnderConstruction unfinishedBlock, final int endLine) {
        return new CodeBlock(unfinishedBlock.start, endLine, unfinishedBlock.blockCondition, unfinishedBlock.presenceCondition);
    }

    public record UnderConstruction(int start, Formula blockCondition,
                                    Formula presenceCondition) {
    }

}
