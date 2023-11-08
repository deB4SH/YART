package de.b4sh.yart;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import jakarta.json.stream.JsonParser;
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

    private JsonValidationService service;
    private JsonSchemaReaderFactory readerFactory;

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
        //validate configuration file again schema
        this.service = JsonValidationService.newInstance();
        this.readerFactory = this.service.createSchemaReaderFactoryBuilder().withSchemaResolver(this::resolveSchema).build();
        JsonSchema schema = this.readSchema(schemaFile.toPath());

        List<String> problemList = validateConfigurationParser(service,schema,configFile.toPath());
        if(!problemList.isEmpty()){
            log.log(Level.WARNING, ExitCode.CONFIG_CONTAINS_ERRORS.getReason());
            problemList.forEach(elem -> log.log(Level.INFO, String.format("Validation Error: %s",elem)));
            return ExitCode.CONFIG_CONTAINS_ERRORS.getNumber();
        }
        //create output dir if not existing
        File outputDir = new File(outputDirectory);
        if(!outputDir.exists()){
            if(!outputDir.mkdirs()){
                log.log(Level.WARNING, ExitCode.OUTPUT_DIR_NOT_CREATABLE.getReason());
                return ExitCode.OUTPUT_DIR_NOT_CREATABLE.getNumber();
            }
        }
        //load configuration yaml
        Yaml yaml = new Yaml();
        Map<String,Object> data = yaml.load(new FileInputStream(configFile));
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
            Object dataResult = null;
            for(String s: configurationPath){
                if(s.contains("$")){
                    ref.templateKey = s.replace("$","");
                    break;
                }
                dataResult = data.get(s);
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
        //done with dynamic dirs, template all other .jinja2 files
        templateDirectory((LinkedHashMap) data,outputDir);
        return 0;
    }

    private void templateDirectory(final LinkedHashMap data, final File folder) {
        Jinjava jinjava = new Jinjava();
        ExtensionLoader.loadExtensions(jinjava);
        try (Stream<Path> pathStream = Files.walk(folder.toPath(), Integer.MAX_VALUE)) {
            for (File file : pathStream.map(Path::toFile).filter(elem -> elem.toString().endsWith(".jinja2")).toList()) {
                String content = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
                Map<String, ?> jinJavaBindings = generateJinJavaBindings(data);
                String result = jinjava.render(content, jinJavaBindings);
                //remove jinja2 extension from filepath
                String path = file.getPath().replace(".jinja2", "");
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

    private Map<String, ?> generateJinJavaBindings(final LinkedHashMap data){
        Map<String, Object> result = new HashMap<>();
        data.forEach((key, value) -> {
            if(value instanceof LinkedHashMap<?,?>){
                //recursive call for generateJinJavaBindings
                result.put((String)key, generateJinJavaBindings((LinkedHashMap) value));
            } else if(value instanceof ArrayList<?>){
                result.put((String)key, value); //arraylist directly
            }
            else {
                result.put((String)key, value.toString());
            }
        });
        return result;
    }

    private JsonSchema readSchema(Path path){
        try(JsonSchemaReader reader = this.readerFactory.createSchemaReader(path)){
            return reader.read();
        }
    }

    private JsonSchema resolveSchema(URI id){
        Path path = Paths.get(schemaDirectory,id.getPath());
        log.log(Level.INFO, String.format("Resolving schema with path: %s",id.getPath()));
        return readSchema(path);
    }

    private List<String> validateConfigurationParser(JsonValidationService service, JsonSchema schema, Path config){
        List<String> problemList = new ArrayList<>();
        ProblemHandler problemHandler = service.createProblemPrinter(problemList::add);
        try(JsonParser parser = service.createParser(config,schema,problemHandler)){
            while (parser.hasNext()) { //parse through all elements
                JsonParser.Event event = parser.next();
            }
        }
        return problemList;
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
