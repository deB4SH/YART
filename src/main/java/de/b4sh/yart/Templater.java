package de.b4sh.yart;

import com.hubspot.jinjava.Jinjava;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CommandLine.Command(name = "template",
        mixinStandardHelpOptions = true,
        version = "templater 1.0",
        description = "Creates templates based on given schema and templater directory")
public class Templater implements Callable<Integer> {

    @CommandLine.Option(names = {"-td","--templatedirectory"}, description = "Path to template directory")
    private String templateDirectory;

    @CommandLine.Option(names = {"-od","--outputdirectory"}, description = "Path to output data towards")
    private String outputDirectory;

    @CommandLine.Option(names = {"-s","--schemafilename"}, description = "Name of the schema file")
    private String schemaFilename;

    @CommandLine.Option(names = {"-sd","--schemadirectory"}, description = "Path to schema files")
    private String schemaDirectory;

    @CommandLine.Option(names = {"-c","--configfile"}, description = "Path to configuration file")
    private String configFileArg;

    private final Logger log = Logger.getLogger(Templater.class.getName());
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
        FileHelper.copyDirectory(templateDir,outputDir);
        //walk over output dir and check for template dirs to create
        List<Path> dirs = Files.walk(outputDir.toPath()).filter(Files::isDirectory).toList();
        dirs.stream().filter(s -> s.toString().contains("$")).forEach(element -> {
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
                        FileHelper.copyDirectory(new File(ref.configurationPath),new File(ref.configurationPathWithoutKey+map.get(ref.templateKey)));
                        templateDirectory(map,new File(ref.configurationPathWithoutKey+map.get(ref.templateKey)));
                        int a = 0;
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

    private void templateDirectory(final LinkedHashMap data, final File folder){
        Jinjava jinjava = new Jinjava();
        for (File file : folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jinja2"))) {
            try {
                String content = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
                Map<String, ?> jinJavaBindings = generateJinJavaBindings(data);
                String result = jinjava.render(content,jinJavaBindings);
                //remove jinja2 extension from filepath
                String path = file.getPath().replace(".jinja2","");
                Files.write(Path.of(path),result.getBytes(StandardCharsets.UTF_8));
                //delete old jinja2 template file
                if(file.delete() == false){
                    log.log(Level.WARNING, String.format("Could not delete jinja2 template file with path: %s",path));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, ?> generateJinJavaBindings(final LinkedHashMap data){
        Map<String, Object> result = new HashMap<>();
        data.forEach((key, value) -> {
            if(value instanceof LinkedHashMap<?,?>){
                //recursive call for generateJinJavaBindings
                result.put((String)key, generateJinJavaBindings((LinkedHashMap) value));
            }else {
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