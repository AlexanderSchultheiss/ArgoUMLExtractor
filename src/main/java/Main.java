import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/*
We have to account for
- //#if defined()
- //#if defined() or defined()
- //#if defined() and defined()
- //#else
- //#endif
 */
public class Main {
    private static final Path workDir = Path.of("/home/alex/develop/bachelor_projects/BA-Angelina/argouml-spl-benchmark/argouml-app/src/org/argouml/");
    private static final FormulaFactory factory = new FormulaFactory();


    public static void main(String... args) throws IOException {
        // Retrieve List of all files
        final List<Path> javaFiles = searchJavaFiles(workDir);
        System.out.println("Found " + javaFiles.size() + " Java files.");
//        Formula test = factory.and(factory.or(factory.literal("A", false), factory.literal("B", true)), factory.or(factory.literal("C", false), factory.literal("D", true)));
//        System.out.println(test);
        // Parse each file
        final List<CodeBlock> blocks = new LinkedList<>();
        javaFiles.forEach(f -> blocks.addAll(determineBlocks(f)));
    }

    public static List<Path> searchJavaFiles(final Path root) throws IOException {
        return Files
                .find(root,
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".java"))
                .collect(Collectors.toList());
    }

    public static List<CodeBlock> determineBlocks(final Path pathToFile) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(pathToFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final List<CodeBlock> blocks = new LinkedList<>();
        final LinkedList<CodeBlock.UnderConstruction> unfinishedBlocks = new LinkedList<>();

        int blockStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (blockStart(line) || i == 0) {
                // Add a new unfinished block to the stack
                unfinishedBlocks.add(CodeBlock.startBuildingBlock(blockStart, toFormula(line)));
            } else if (blockSwitch(line)) {
                // Convert the last started block to a completed block
                CodeBlock finishedBlock = CodeBlock.finishBlock(unfinishedBlocks.pop(), i);
                blocks.add(finishedBlock);
                // Start a new block with the negated Formula
                unfinishedBlocks.add(CodeBlock.startBuildingBlock(i, finishedBlock.condition().negate()));
            } else if (blockEnd(line)) {
                // Convert the last started block to a completed block
                blocks.add(CodeBlock.finishBlock(unfinishedBlocks.pop(), i));
            }
        }

        // Finish the remaining block that must have condition 'true'
        if (!unfinishedBlocks.isEmpty()) {
            CodeBlock finishedBlock = CodeBlock.finishBlock(unfinishedBlocks.pop(), lines.size()-1);
            blocks.add(finishedBlock);
            if (!finishedBlock.condition().evaluate(new Assignment())) {
                throw new IllegalStateException("Remaining Block does not have the condition 'true'");
            }
        }

        if (!unfinishedBlocks.isEmpty()) {
            throw new IllegalStateException("Blocks remain unfinished.");
        }

        return blocks;
    }

    public static boolean blockStart(final String line) {
        return line.trim().startsWith("//#if defined");
    }

    public static boolean blockSwitch(final String line) {
        return line.trim().startsWith("//#else");
    }

    public static boolean blockEnd(final String line) {
        return line.trim().startsWith("//#endif");
    }

    public static Formula toFormula(String line) {
        if (!blockStart(line)) {
            return factory.verum();
        }
        // Trim whitespace
        line = line.trim();
        // Cut everything of till the first condition
        line = line.substring(line.indexOf("defined"));
        // Remove "defined(" and ")"
        line = line.replaceAll("defined\\(", "");
        line = line.replaceAll("\\)", "");
        try {
            return factory.parse(line);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }

}
