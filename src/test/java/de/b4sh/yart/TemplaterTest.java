package de.b4sh.yart;


import jakarta.json.stream.JsonParsingException;
import org.junit.jupiter.api.Assertions;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

class TemplaterTest {

    private static final Logger log = Logger.getLogger(TemplaterTest.class.getName());
    private Templater templater;

    private String testCaseBasePath = "src/test/resources/test_cases/";
    private String configPath = "config/";
    private String schemaPath = "schema/";
    private String templatePath = "template/template.yaml.jinja2";

    private String testid = "00_template";

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
        this.templater.setOutputDirectory(String.format("target/%s/", uuid));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        this.templater = null;
    }


    @org.junit.jupiter.api.Test
    void testCase01BrokenSchema() throws Exception {
        testid = "01_broken_schema";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        Assertions.assertThrows(JsonParsingException.class, () -> {
            int result = this.templater.call();
        });
    }

    @org.junit.jupiter.api.Test
    void testCase02SimpleHappyPath() throws Exception {
        testid = "02_simple_happy_path";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals("0",result); //check if call function returned happy path result
        //TODO: assert resulting files
    }

    @org.junit.jupiter.api.Test
    void testCase03SimpleHappyPathWithDynamic() throws Exception {
        testid = "03_simple_happy_path_with_dynamic_templates";
        this.templater.setTemplateDirectory(String.format("src/test/resources/test_cases/%s/template",testid));
        this.templater.setConfigFileArg(String.format("src/test/resources/test_cases/%s/config/config.yaml",testid));
        this.templater.setSchemaDirectory(String.format("src/test/resources/test_cases/%s/schema",testid));
        int result = this.templater.call();
        Assertions.assertEquals(0,result); //check if call function returned happy path result
        //TODO: assert resulting files
    }

}