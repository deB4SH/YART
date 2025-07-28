package de.b4sh.yart.extensions.terraform;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyMap;
import de.b4sh.yart.extensions.ExtensionProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transfers a given property pattern schema into a 'quotified' dictionary.
 *
 * Schema input:
 * "label":{
 *       "type": "object",
 *       "patternProperties": {
 *         ".{1,}": {"type": "string"}
 *       }
 *     }
 *
 * Funtion call in jinja2 template:
 * {{ tf:qoutedict(YOUR_SCHEMA_REFERENCE_HERE) }}
 *
 * Expected result:
 *  "foo": "bar"
 *  "baz": "100"
 */
public class DictionaryPrint extends ExtensionProvider {

    private static final Logger log = Logger.getLogger(DictionaryPrint.class.getName());

    @Override
    public String getNamespace() {
        return "tf";
    }

    @Override
    public String getLocalName() {
        return "quotedict";
    }

    @Override
    public String getFunctionName() {
        return "print";
    }

    public static String print(Object input){
        log.log(Level.INFO, "Received object to print out");
        StringBuilder bob = new StringBuilder();

        if(!(input instanceof SizeLimitingPyMap)){
            log.log(Level.WARNING, "Received a non list element. Can't print element. Returning empty string.");
            return "";
        }
        SizeLimitingPyMap map = (SizeLimitingPyMap) input;
        map.entrySet().forEach(entry -> {
            bob.append("\"").append(entry.getKey()).append("\"")
                    .append(": ")
                    .append("\"").append(entry.getValue()).append("\"").append("\n");
        });
        return bob.toString();
    }
}
