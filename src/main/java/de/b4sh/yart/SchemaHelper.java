package de.b4sh.yart;

import org.leadpony.justify.api.*;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SchemaHelper {

    private final Logger log = Logger.getLogger(SchemaHelper.class.getName());

    private final Path schemaFile;
    final JsonValidationService jsonValidationService;

    /**
     * Constructs the SchemaHelper.
     * @param schemaFile path to first schema file
     */
    public SchemaHelper(Path schemaFile) {
        this.schemaFile = schemaFile;
        this.jsonValidationService = JsonValidationService.newInstance();
    }

    public JsonSchema loadSchema(){
        JsonSchemaReaderFactory factory = this.jsonValidationService.createSchemaReaderFactoryBuilder().withSchemaResolver(this::resolveSchema).build();
        JsonSchemaReader reader = factory.createSchemaReader(schemaFile);
        JsonSchema schema = reader.read();
        return schema;
    }

    private JsonSchema resolveSchema(URI schemaId){
        final Path path = Paths.get(schemaFile.getParent().toString(), schemaId.getPath());
        log.log(Level.INFO, String.format("Trying to resolve schema under following Path: %s", path));
        final JsonSchema read = this.jsonValidationService.createSchemaReader(path).read();
        log.log(Level.FINE, String.format("Successfully read schema under following path: %s",path));
        return read;
    }


}
