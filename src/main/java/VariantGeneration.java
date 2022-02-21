import vevos.VEVOS;
import vevos.feature.Variant;
import vevos.feature.config.FeatureIDEConfiguration;
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
import vevos.variability.pc.groundtruth.GroundTruth;
import vevos.variability.pc.options.ArtefactFilter;
import vevos.variability.pc.options.VariantGenerationOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class VariantGeneration {

    private static final Set<String> features = Set.of("COGNITIVE", "LOGGING", "ACTIVITYDIAGRAM", "STATEDIAGRAM", "SEQUENCEDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM", "DEPLOYMENTDIAGRAM");

    public static void main(String... args) throws Resources.ResourceIOException, IOException {
        VEVOS.Initialize();
        final CaseSensitivePath splRepositoryPath = CaseSensitivePath.of("/home/alex/develop/student-projects/BA-Angelina/argouml-spl-benchmark/");
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

        for (final Map.Entry<Variant, List<String>> entry : sampleAllVariants().entrySet()) {
            Variant variant = entry.getKey();
            /// Let's put the variant into our target directory but indexed by commit hash and its name.
            final CaseSensitivePath variantDir = variantsGenerationDir.resolve(variant.getName());
            if (!Files.exists(variantDir.path())) {
                Logger.info("Generating variant " + variant.getName());
                final Result<GroundTruth, Exception> result =
                        pcs.generateVariant(variant, splRepositoryPath, variantDir, generationOptions);
                if (result.isSuccess()) {
                    final GroundTruth groundTruth = result.getSuccess();/// 1. the presence conditions.
                    final Artefact presenceConditionsOfVariant = groundTruth.variant();

                    /// We can also export the ground truth PCs of the variant.
                    Resources.Instance().write(Artefact.class, presenceConditionsOfVariant, variantDir.resolve("pcs.variant.csv").path());
                    /// Save the configuration
                    Path configFolder = variantsGenerationDir.path().resolve("configs");
                    Files.createDirectories(configFolder);
                    Files.write(configFolder.resolve(variant.getName() + ".config"), entry.getValue());
                }
            }
        }

        for (final Variant variant : sampleVariants(3).keySet()) {
            Logger.info("Loading variant " + variant.getName());
            /// Let's put the variant into our target directory but indexed by commit hash and its name.
            final CaseSensitivePath variantDir = variantsGenerationDir.resolve(commit.id(), variant.getName());
            Artefact result = Resources.Instance().load(Artefact.class, variantDir.resolve("pcs.variant.csv").path());
            var pc = result.getPresenceConditionOf(CaseSensitivePath.of("argouml-app/src/org/argouml/application/Main.java"), 700);
            System.out.println("Loaded.");
        }
    }

    private static Map<Variant, List<String>> sampleAllVariants() {
        // Get the power set of all features
        Set<Set<String>> featurePowerSet = powerSet(features);
        Map<Variant, List<String>> variants = new HashMap<>(featurePowerSet.size());
        int i = 0;
        for (Set<String> configuration : featurePowerSet) {
            final List<String> features = new ArrayList<>(configuration);
            Variant variant = new Variant(String.format("var_%d", i), new SimpleConfiguration(features));
            variants.put(variant, features);
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

    private static Map<Variant, List<String>> sampleVariants(final int sampleSize) {
        final Map<Variant,List<String>> allVariants = sampleAllVariants();
        List<Variant> variants = new ArrayList<>(allVariants.keySet());
        Collections.shuffle(variants);
        Map<Variant, List<String>> subset = new HashMap<>();
        variants.subList(0, sampleSize).forEach(v -> subset.put(v, allVariants.get(v)));
        return subset;
    }
}
