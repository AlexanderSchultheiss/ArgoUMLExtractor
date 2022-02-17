import vevos.VEVOS;
import vevos.feature.Variant;
import vevos.feature.config.SimpleConfiguration;
import vevos.functjonal.Lazy;
import vevos.functjonal.Result;
import vevos.io.Resources;
import vevos.util.Logger;
import vevos.util.io.CaseSensitivePath;
import vevos.variability.SPLCommit;
import vevos.variability.VariabilityDataset;
import vevos.variability.pc.Artefact;
import vevos.variability.pc.SourceCodeFile;
import vevos.variability.pc.groundtruth.AnnotationGroundTruth;
import vevos.variability.pc.groundtruth.GroundTruth;
import vevos.variability.pc.options.ArtefactFilter;
import vevos.variability.pc.options.VariantGenerationOptions;

import java.nio.file.Files;
import java.util.*;

public class VariantGeneration {

    private static final Set<String> features = Set.of("COGNITIVE", "LOGGING", "ACTIVITYDIAGRAM", "STATEDIAGRAM", "SEQUENCEDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM", "DEPLOYMENTDIAGRAM");

    public static void main(String... args) throws Resources.ResourceIOException {
        VEVOS.Initialize();
        final CaseSensitivePath splRepositoryPath = CaseSensitivePath.of("/home/alex/develop/bachelor_projects/BA-Angelina/argouml-spl-benchmark/");
        final CaseSensitivePath groundTruthDatasetPath = CaseSensitivePath.of("dataset");
        final CaseSensitivePath variantsGenerationDir = CaseSensitivePath.of("variants");

        final VariabilityDataset dataset = Resources.Instance()
                .load(VariabilityDataset.class, groundTruthDatasetPath.path());
        System.out.println("DEBUG");

        final SPLCommit commit = dataset.getSuccessCommits().get(0);
        final Lazy<Optional<Artefact>> loadPresenceConditions = commit.presenceConditions();
        final Artefact pcs = loadPresenceConditions.run().orElseThrow();

        final ArtefactFilter<SourceCodeFile> artefactFilter = ArtefactFilter.KeepAll();
        final VariantGenerationOptions generationOptions = VariantGenerationOptions.ExitOnError(artefactFilter);

        for (final Variant variant : sampleAllVariants()) {
            /// Let's put the variant into our target directory but indexed by commit hash and its name.
            final CaseSensitivePath variantDir = variantsGenerationDir.resolve(commit.id(), variant.getName());
            if (!Files.exists(variantDir.path())) {
                Logger.info("Generating variant " + variant.getName());
                final Result<GroundTruth, Exception> result =
                        pcs.generateVariant(variant, splRepositoryPath, variantDir, generationOptions);
                if (result.isSuccess()) {
                    final GroundTruth groundTruth = result.getSuccess();/// 1. the presence conditions.
                    final Artefact presenceConditionsOfVariant = groundTruth.variant();

                    /// We can also export the ground truth PCs of the variant.
                    Resources.Instance().write(Artefact.class, presenceConditionsOfVariant, variantDir.resolve("pcs.variant.csv").path());
                }
            }
        }

        for (final Variant variant : sampleVariants(3)) {
            Logger.info("Loading variant " + variant.getName());
            /// Let's put the variant into our target directory but indexed by commit hash and its name.
            final CaseSensitivePath variantDir = variantsGenerationDir.resolve(commit.id(), variant.getName());
            Artefact result = Resources.Instance().load(Artefact.class, variantDir.resolve("pcs.variant.csv").path());
            var pc = result.getPresenceConditionOf(CaseSensitivePath.of("argouml-app/src/org/argouml/application/Main.java"), 700);
            System.out.println("Loaded.");
        }
    }

    private static List<Variant> sampleAllVariants() {
        // Get the power set of all features
        Set<Set<String>> featurePowerSet = powerSet(features);
        List<Variant> variants = new ArrayList<>(featurePowerSet.size());
        int i = 0;
        for (Set<String> configuration : featurePowerSet) {
            Variant variant = new Variant(String.format("var_%d", i), new SimpleConfiguration(configuration.stream().toList()));
            variants.add(variant);
            i++;
        }
        return variants;
    }

    private static Set<Set<String>> powerSet(final Set<String> input) {
        final Set<Set<String>> result = new HashSet<>();
        result.add(input);
        for (final String element : input) {
            Set<String> reducedSet = new HashSet<>(input);
            reducedSet.remove(element);
            result.addAll(powerSet(reducedSet));
        }
        return result;
    }

    private static List<Variant> sampleVariants(final int sampleSize) {
        final List<Variant> allVariants = sampleAllVariants();
        Collections.shuffle(allVariants);
        return allVariants.subList(0, sampleSize);
    }
}
