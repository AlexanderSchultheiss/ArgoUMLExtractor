import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
    private static final Path outputFile = Path.of("result/argouml.spl.csv");
    private static final FormulaFactory factory = new FormulaFactory();


    public static void main(String... args) throws IOException {
        // Retrieve List of all files
        final List<Path> javaFiles = searchJavaFiles(workDir);
        System.out.println("Found " + javaFiles.size() + " Java files.");
//        Formula test = factory.and(factory.or(factory.literal("A", false), factory.literal("B", true)), factory.or(factory.literal("C", false), factory.literal("D", true)));
//        System.out.println(test);
        // Parse each file
        List<String> csvLines = new LinkedList<>();
        for (final Path path : javaFiles) {
            final List<CodeBlock> blocks = determineBlocks(path);
            for (CodeBlock block: blocks) {
                csvLines.add(toCSVLine(path, block));
            }
        }
        if (outputFile.getParent().toFile().mkdirs()) {
            System.out.println("Created directory " + outputFile.getParent());
        }
        Files.write(outputFile, csvLines);
    }

    public static List<Path> searchJavaFiles(final Path root) throws IOException {
        return Files
                .find(root,
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".java"))
                .collect(Collectors.toList());
    }

    public static List<CodeBlock> determineBlocks(final Path pathToFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(pathToFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            try {
                lines = Files.readAllLines(pathToFile, StandardCharsets.ISO_8859_1);
            } catch (IOException ex) {
                System.err.println("Was not able to read " + pathToFile);
                throw new RuntimeException(e);
            }

        }
        final List<CodeBlock> blocks = new LinkedList<>();
        final LinkedList<CodeBlock.UnderConstruction> unfinishedBlocks = new LinkedList<>();
        final LinkedList<Formula> nestedFormulas = new LinkedList<>();

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (blockStart(line) || i == 0) {
                // Add a new unfinished block to the stack
                final Formula formula = toFormula(line);
                nestedFormulas.add(formula);
                unfinishedBlocks.add(CodeBlock.startBuildingBlock(i, formula, toPC(nestedFormulas)));
            } else if (blockSwitch(line)) {
                // Convert the last started block to a completed block
                if (unfinishedBlocks.isEmpty()) {
                    throw new IllegalStateException("No unfinished blocks remain in " + pathToFile + " at line " + i);
                }
                CodeBlock finishedBlock = CodeBlock.finishBlock(Objects.requireNonNull(unfinishedBlocks.pollLast()), i);
                blocks.add(finishedBlock);
                nestedFormulas.pollLast();
                // Start a new block with the negated Formula
                final Formula formula = finishedBlock.blockCondition().negate();
                nestedFormulas.add(formula);
                unfinishedBlocks.add(CodeBlock.startBuildingBlock(i, formula, toPC(nestedFormulas)));
            } else if (blockEnd(line)) {
                // Convert the last started block to a completed block
                if (unfinishedBlocks.isEmpty()) {
                    throw new IllegalStateException("No unfinished blocks remain in " + pathToFile + " at line " + i);
                }
                blocks.add(CodeBlock.finishBlock(Objects.requireNonNull(unfinishedBlocks.pollLast()), i));
                nestedFormulas.pollLast();
            }
        }

        // Finish the remaining block that must have blockCondition 'true'
        if (!unfinishedBlocks.isEmpty()) {
            CodeBlock finishedBlock = CodeBlock.finishBlock(unfinishedBlocks.pollLast(), lines.size());
            blocks.add(finishedBlock);
            if (!finishedBlock.blockCondition().evaluate(new Assignment())) {
                throw new IllegalStateException("Remaining Block does not have the blockCondition 'true'");
            }
        }

        if (!unfinishedBlocks.isEmpty()) {
            throw new IllegalStateException("Blocks remain unfinished.");
        }

        blocks.sort(Comparator.comparingInt(CodeBlock::start));
        return blocks;
    }

    public static boolean blockStart(final String line) {
        return line.contains("//#if defined");
    }

    public static boolean blockSwitch(final String line) {
        return line.contains("//#else");
    }

    public static boolean blockEnd(final String line) {
        return line.contains("//#endif");
    }

    public static Formula toFormula(String line) {
        if (!blockStart(line)) {
            return factory.verum();
        }
        // Trim whitespace
        line = line.trim();
        // Cut everything of till the first blockCondition
        line = line.substring(line.indexOf("defined("));
        // Remove "defined(" and ")"
        line = line.replaceAll("defined\\(", "");
        line = line.replaceAll("\\)", "");
        // Replace 'and' and 'or'
        line = line.replaceAll("and", "&");
        line = line.replaceAll("or", "|");
        try {
            return factory.parse(line);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }

    public static Formula toPC(List<Formula> nestedFormulas) {
        return factory.and(nestedFormulas);
    }

    public static String toCSVLine(Path path, CodeBlock codeBlock) {
        return String.format("%s;%s;%s;%s;%d;%d", path.toString(), "true", fixCondition(codeBlock.blockCondition().toString()), fixCondition(codeBlock.getPresenceCondition().toString()), codeBlock.start(), codeBlock.end());
    }

    public static String fixCondition(String condition) {
        condition = condition.replaceAll("&", "&&");
        return condition.replaceAll("\\|", "||");
    }
}
