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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
We have to account for
- //#if defined()
- //#if defined() or defined()
- //#if defined() and defined()
- //#else
- //#endif
 */
public class GroundTruthExtraction {

    private static final Path basePath = Path.of("/home/alex/develop/bachelor_projects/BA-Angelina/argouml-spl-benchmark/");
    private static final Path searchPath = basePath.resolve("argouml-app/src/");
    private static final Path outputFile = Path.of("dataset/data/1f767cbec3e9818a34b96dbe11937d1832753da3/code-variability.spl.csv");
    private static final FormulaFactory factory = new FormulaFactory();
    public static final String IF_DEFINED = "//#if defined";
    public static final String ELSE = "//#else";
    public static final String ENDIF = "//#endif";


    public static void main(String... args) throws IOException {
        // Retrieve List of all files
        final List<Path> javaFiles = searchJavaFiles(searchPath);
        System.out.println("Found " + javaFiles.size() + " Java files.");
        // Preprocess files
        preprocessFiles(javaFiles);

        List<String> csvLines = new LinkedList<>();
        csvLines.add("PATH;FC;FM;PC;START;END");
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

    public static void preprocessFiles(final List<Path> javaFiles) {
        for (final Path pathToFile : javaFiles) {
            List<String> lines = readLines(pathToFile);
            List<String> processedLines = new LinkedList<>();
            for (final String line : lines) {
                if (blockStart(line)) {
                    String[] parts = line.split(IF_DEFINED);
                    processedLines.add(parts[0]);
                    processedLines.add(IF_DEFINED + parts[1]);
                } else if (blockSwitch(line)) {
                    String[] parts = line.split(ELSE);
                    processedLines.add(parts[0]);
                    if (parts.length > 1) {
                        processedLines.add(ELSE + parts[1]);
                    } else {
                        processedLines.add(ELSE);
                    }
                } else if (blockEnd(line)) {
                    String[] parts = line.split(ENDIF);
                    if (parts.length == 0) {
                        processedLines.add("");
                        processedLines.add(ENDIF);
                    } else {
                        processedLines.add(parts[0]);
                        processedLines.add(ENDIF);
                    }
                } else {
                    processedLines.add(line);
                }
            }

            try {
                Files.write(pathToFile, processedLines);
            } catch (IOException e) {
                System.err.println("Was not able to write " + pathToFile);
                throw new RuntimeException(e);
            }
        }
    }

    private static List<String> readLines(Path pathToFile) {
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
        return lines;
    }

    public static List<Path> searchJavaFiles(final Path root) throws IOException {
        return Files
                .find(root,
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".java"))
                .collect(Collectors.toList());
    }

    public static List<CodeBlock> determineBlocks(final Path pathToFile) {
        List<String> lines = readLines(pathToFile);
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
        return line.contains(IF_DEFINED);
    }

    public static boolean blockSwitch(final String line) {
        return line.contains(ELSE);
    }

    public static boolean blockEnd(final String line) {
        return line.contains(ENDIF);
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
        return String.format("%s;%s;%s;%s;%d;%d", basePath.relativize(path), "true", fixCondition(codeBlock.blockCondition().toString()), fixCondition(codeBlock.getPresenceCondition().toString()), codeBlock.start(), codeBlock.end());
    }

    public static String fixCondition(String condition) {
        condition = condition.replaceAll("&", "&&");
        condition = condition.replaceAll("\\$true", "true");
        condition = condition.replaceAll("\\$false", "false");
        condition = condition.replaceAll("~", "!");
        return condition.replaceAll("\\|", "||");
    }
}
