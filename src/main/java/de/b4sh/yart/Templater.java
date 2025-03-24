package de.b4sh.yart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import jakarta.json.*;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;
import org.leadpony.justify.api.*;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "template",
        mixinStandardHelpOptions = true,
        version = "templater 1.0",
        description = "Creates templates based on given schema and templater directory")
public class Templater implements Callable<Integer> {

    @CommandLine.Option(names = {"-td","--templatedirectory"}, description = "Path to template directory", defaultValue = "/template")
    private String templateDirectory;

    @CommandLine.Option(names = {"-od","--outputdirectory"}, description = "Path to output data towards", defaultValue = "/output")
    private String outputDirectory;

    @CommandLine.Option(names = {"-s","--schemafilename"}, description = "Name of the schema file", defaultValue = "schema.json")
    private String schemaFilename;

    @CommandLine.Option(names = {"-sd","--schemadirectory"}, description = "Path to schema files", defaultValue = "/schema")
    private String schemaDirectory;

    @CommandLine.Option(names = {"-c","--configfile"}, description = "Path to configuration file", defaultValue = "/data/config.yaml")
    private String configFileArg;

    private final Logger log = Logger.getLogger(Templater.class.getName());
    private final Pattern pathPattern = Pattern.compile(".+(?>\\$[a-z]+)$");

    @Override
    public Integer call() throws Exception {
        final File schemaFile = new File(schemaDirectory +  File.separator + schemaFilename);
        if(!schemaFile.exists()){ //test if schema file exists
            log.log(Level.WARNING, ExitCode.SCHEMA_FILE_NOT_FOUND.getReason());
            log.log(Level.FINE, String.format("Path checked for schema file: %s",schemaFile.getPath()));
           return ExitCode.SCHEMA_FILE_NOT_FOUND.getNumber();
        }
        final File configFile = new File(configFileArg);
        if(!configFile.exists()){
            log.log(Level.WARNING, ExitCode.CONFIG_FILE_NOT_FOUND.getReason());
            log.log(Level.FINE, String.format("Path checked for config file: %s",configFile.getPath()));
            return ExitCode.CONFIG_FILE_NOT_FOUND.getNumber();
        }
        //check for failures against schema and add defaults to data structure
        final JsonValidationService service = JsonValidationService.newInstance();

        final SchemaHelper schemaHelper = new SchemaHelper(schemaFile.toPath());
        JsonSchema schema = schemaHelper.loadSchema();

        List<String> problemsFound = new ArrayList<>();
        ProblemHandler problemHandler = service.createProblemPrinter(problemsFound::add);
        ValidationConfig validationConfig = service.createValidationConfig().withSchema(schema).withProblemHandler(problemHandler).withDefaultValues(true);

        JsonReaderFactory readerFactory = service.createReaderFactory(validationConfig.getAsMap());
        JsonReader reader = readerFactory.createReader(Files.newInputStream(configFile.toPath()));
        //data structure as json value from config file
        JsonValue value = reader.readValue();
        //check for problems and jump out of templater if errors occured
        if(!problemsFound.isEmpty()){
            problemsFound.forEach(elem -> log.info(String.format("Found error while parsing config: %s",elem)));
            return -1; //TODO: find a nice exit code for this here
        }
        //create mapping for jinjava
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String,Object> dataBinding = mapper.readValue(value.toString(), HashMap.class);
        //create output dir if not existing
        File outputDir = new File(outputDirectory);
        if(!outputDir.exists()){
            if(!outputDir.mkdirs()){
                log.log(Level.WARNING, ExitCode.OUTPUT_DIR_NOT_CREATABLE.getReason());
                return ExitCode.OUTPUT_DIR_NOT_CREATABLE.getNumber();
            }
        }
        //check if template dir exists
        File templateDir = new File(templateDirectory);
        if(!templateDir.exists() || !templateDir.isDirectory()){
            log.log(Level.WARNING, ExitCode.TEMPLATE_DIR_VAR_NOT_FOLDER.getReason());
            return ExitCode.TEMPLATE_DIR_VAR_NOT_FOLDER.getNumber();
        }
        //copy template dir to output dir
        log.log(Level.INFO, "Initial Copy of all templates to target directory");
        FileHelper.copyDirectory(templateDir,outputDir);
        log.log(Level.INFO, "Done with initial copy");
        log.log(Level.INFO, "Next: searching and templating dynamic directories");
        //create filter pattern for paths to reduce to first dynamic dir
        Predicate<Path> reduceToDynamic = path -> pathPattern.matcher(path.toString()).find();
        //walk over output dir and check for template dirs to create
        List<Path> dirs = Files.walk(outputDir.toPath()).filter(Files::isDirectory).toList();
        dirs.stream().filter(s -> s.toString().contains("$")).filter(reduceToDynamic).forEach(element -> {
            log.log(Level.INFO, String.format("Found dynamic directory to template: %s",element.toString()));
            String[] configurationPath = element.toString().replace(outputDirectory, "").split("/");
            var ref = new Object() { //requested to cast into anonymous object to work in a lambda later
                String templateKey = "null";
                final String configurationPath = element.toString();
                final String configurationPathWithoutKey = element.toString().replaceAll("\\$.+","");
            };
            //traverse down the path in config
            Object dataResult = dataBinding;
            for(String s: configurationPath){
                if(s.isEmpty()){
                    log.log(Level.FINE, "Seems to be top level element. Which is empty all the time");
                    continue;
                }
                if(s.contains("$")){
                    ref.templateKey = s.replace("$","");
                    break;
                }
                if(dataResult instanceof Map<?,?>){
                    dataResult = ((Map<String,Object>)dataResult).get(s);
                }else{
                    log.log(Level.WARNING, "Seems like there is an issue with your dynamic templating? Templater can't select data like folder suggest");
                    break;
                }

            }
            if(!(dataResult instanceof ArrayList<?>)){
                log.log(Level.WARNING, ExitCode.DYNAMIC_TEMPLATE_DOES_NOT_COMPLY_CONFIG.getReason());
                return;
            }
            ArrayList<LinkedHashMap> dataArr = (ArrayList<LinkedHashMap>) dataResult;
            dataArr.forEach(map -> {
                if(!map.containsKey(ref.templateKey)){
                    log.log(Level.INFO, String.format("Map-Entry does not contain requested templateKey %s", ref.templateKey));
                }else{
                    //copy dir
                    try {
                        log.log(Level.INFO, String.format("Found dir to template. Creating dir: %s%s",ref.configurationPathWithoutKey,map.get(ref.templateKey)));
                        final File sourceDirectory = new File(ref.configurationPath);
                        final File targetDirectory = new File(ref.configurationPathWithoutKey+map.get(ref.templateKey));
                        FileHelper.copyDirectory(sourceDirectory,targetDirectory);
                        templateDirectory(map,targetDirectory);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            //cleanup and remove marker dir
            FileHelper.deleteFolderWithContent(element);
        });
        //TODO: render still open templates
        //done with dynamic dirs, template all other .jinja2 files
        templateDirectory(dataBinding, outputDir);
        return 0;
    }

    private void templateDirectory(final Map<String,?> data, final File folder) {
        Jinjava jinjava = new Jinjava();
        ExtensionLoader.loadExtensions(jinjava);
        try (Stream<Path> pathStream = Files.walk(folder.toPath(), Integer.MAX_VALUE)) {
            for (File file : pathStream.map(Path::toFile).filter(elem -> elem.toString().endsWith(".jinja2")).toList()) {
                final String content = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
                final String result = jinjava.render(content, data);
                //remove jinja2 extension from filepath
                final String path = file.getPath().replace(".jinja2", "");
                Files.write(Path.of(path), result.getBytes(StandardCharsets.UTF_8));
                //delete old jinja2 template file
                if (!file.delete()) {
                    log.log(Level.WARNING, String.format("Could not delete jinja2 template file with path: %s", path));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }

    void setSchemaFilename(String schemaFilename) {
        this.schemaFilename = schemaFilename;
    }

    void setSchemaDirectory(String schemaDirectory) {
        this.schemaDirectory = schemaDirectory;
    }

    void setConfigFileArg(String configFileArg) {
        this.configFileArg = configFileArg;
    }
}
