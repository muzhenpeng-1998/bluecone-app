package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Abstract template for ArchUnit tests in business modules.
 * 
 * <p>Usage: Copy this template to your business module's test directory,
 * extend it, and implement test methods that call the rule checkers.</p>
 * 
 * <p>Example:</p>
 * <pre>
 * {@code
 * @AnalyzeClasses(packages = "com.bluecone.app.store", 
 *                 importOptions = {ImportOption.DoNotIncludeTests.class})
 * public class StoreArchTest extends AbstractArchTestTemplate {
 *     
 *     @Test
 *     void shouldFollowLayerRules() {
 *         LayerRules.checkAll(getClasses());
 *     }
 *     
 *     @Test
 *     void shouldFollowIdRules() {
 *         IdRules.checkAll(getClasses());
 *     }
 * }
 * }
 * </pre>
 */
public abstract class AbstractArchTestTemplate {

    /**
     * Import classes for the module under test.
     * 
     * @param packageName the root package to analyze (e.g., "com.bluecone.app.store")
     * @return imported Java classes
     */
    protected JavaClasses importClasses(String packageName) {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packageName);
    }

    /**
     * Import classes with custom options.
     * 
     * @param packageName the root package to analyze
     * @param options import options
     * @return imported Java classes
     */
    protected JavaClasses importClasses(String packageName, ImportOption... options) {
        ClassFileImporter importer = new ClassFileImporter();
        for (ImportOption option : options) {
            importer = importer.withImportOption(option);
        }
        return importer.importPackages(packageName);
    }
}

