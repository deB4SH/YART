package de.b4sh.yart;


import jakarta.json.stream.JsonParsingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(TemplateTestResultExecution.class)
class TemplaterTest {

    private static final Logger log = Logger.getLogger(TemplaterTest.class.getName());
    private Templater templater;

    private String testid = "00_template";
    private String currentTestDir = "target/00";

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.templater = new Templater();
        //default setup happy path
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        this.templater.setSchemaFilename("schema.json");

        UUID uuid = UUID.randomUUID();
        log.log(Level.INFO, String.format("Output Directory for current test: target/%s/", uuid));
        currentTestDir = String.format("target/%s",uuid);
        this.templater.setOutputDirectory(currentTestDir);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        this.templater = null;
    }


    @org.junit.jupiter.api.Test
    void testCase01BrokenSchema() {
        testid = "01_broken_schema";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        Assertions.assertThrows(JsonParsingException.class, () -> {
            this.templater.call();
        });
    }

    @org.junit.jupiter.api.Test
    void testCase02SimpleHappyPath() throws Exception {
        testid = "02_simple_happy_path";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //verify if files are as expected
        log.log(Level.INFO, "Verify that outputs are equal");
        verifyDirsAreEqual(new File(String.format("src/test/resources/test_cases/%s/expected",testid)).toPath(),
                new File(currentTestDir).toPath());

    }

    @org.junit.jupiter.api.Test
    void testCase03SimpleHappyPathWithDynamic() throws Exception {
        testid = "03_simple_happy_path_with_dynamic_templates";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //verify if files are as expected
        log.log(Level.INFO, "Verify that outputs are equal");
        verifyDirsAreEqual(new File(String.format("src/test/resources/test_cases/%s/expected",testid)).toPath(),
                new File(currentTestDir).toPath());
    }

    @org.junit.jupiter.api.Test
    void testCase04Subschema() throws Exception {
        testid = "04_subschema";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //verify if files are as expected
        log.log(Level.INFO, "Verify that outputs are equal");
        verifyDirsAreEqual(new File(String.format("src/test/resources/test_cases/%s/expected",testid)).toPath(),
                new File(currentTestDir).toPath());
    }

    @org.junit.jupiter.api.Test
    void testCase05ComplexSubschema() throws Exception {
        testid = "05_complex_subschema";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //verify if files are as expected
        log.log(Level.INFO, "Verify that outputs are equal");
        verifyDirsAreEqual(new File(String.format("src/test/resources/test_cases/%s/expected",testid)).toPath(),
                new File(currentTestDir).toPath());
    }

    @org.junit.jupiter.api.Test
    void testCase06TFVars() throws Exception {
        testid = "06_tf_alike_writeouts";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //verify if files are as expected
        log.log(Level.INFO, "Verify that outputs are equal");
        verifyDirsAreEqual(new File(String.format("src/test/resources/test_cases/%s/expected",testid)).toPath(),
                new File(currentTestDir).toPath());
    }

    void verifyDirsAreEqual(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FileVisitResult result = super.visitFile(file,attrs);
                // get the relative file name from path "one"
                Path relativize = source.relativize(file);
                // construct the path for the counterpart file in "other"
                Path fileInOther = destination.resolve(relativize);
                log.log(Level.INFO, String.format("[Verify]comparing: %s to %s", file, fileInOther));

                byte[] otherBytes = Files.readAllBytes(fileInOther);
                byte[] theseBytes = Files.readAllBytes(file);
                if (!Arrays.equals(otherBytes, theseBytes)) {
                    throw new AssertionFailedError(file + " is not equal to " + fileInOther);
                }
                return result;
            }
        });
    }

    public String getCurrentTestDir() {
        return currentTestDir;
    }
}