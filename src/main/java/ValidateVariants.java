import validation.JavaAST;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ValidateVariants {

    public static void main(String... args) throws IOException {

        for (int i = 0; i < 256; i++) {
            String variantName = String.format("Variant_%d", i);
            System.out.println("Validating " + variantName);
            Path variantRoot = Path.of("variants", variantName);
            List<Path> sourceFiles = GroundTruthExtraction.searchJavaFiles(variantRoot);

            for (Path path : sourceFiles) {
                // Try parsing the file
                try {
                    JavaAST ast = new JavaAST(path.toFile());
                } catch (Exception e) {
                    System.out.println("Error while parsing " + path);
                    e.printStackTrace();
                }
            }
        }


    }
}
