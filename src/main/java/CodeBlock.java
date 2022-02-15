import org.logicng.formulas.Formula;

import java.util.Objects;


public final class CodeBlock {
    private final int start;
    private final int end;
    private final Formula condition;

    private CodeBlock(int start, int end, Formula condition) {
        this.start = start;
        this.end = end;
        this.condition = condition;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public Formula condition() {
        return condition;
    }

    public static UnderConstruction startBuildingBlock(int startLine, Formula condition) {
        return new UnderConstruction(startLine, condition);
    }

    public static CodeBlock finishBlock(final UnderConstruction unfinishedBlock, final int endLine) {
        return new CodeBlock(unfinishedBlock.start, endLine, unfinishedBlock.condition);
    }

    public static final class UnderConstruction {
        private final int start;
        private final Formula condition;

        private UnderConstruction(int start, Formula condition) {
            this.start = start;
            this.condition = condition;
        }

        public int start() {
            return start;
        }

        public Formula condition() {
            return condition;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (UnderConstruction) obj;
            return this.start == that.start &&
                    Objects.equals(this.condition, that.condition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, condition);
        }

        @Override
        public String toString() {
            return "UnderConstruction[" +
                    "start=" + start + ", " +
                    "condition=" + condition + ']';
        }

    }

}
