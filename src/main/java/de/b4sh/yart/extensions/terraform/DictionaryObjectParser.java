package de.b4sh.yart.extensions.terraform;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyList;
import com.hubspot.jinjava.objects.collections.SizeLimitingPyMap;
import de.b4sh.yart.extensions.ExtensionProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transform given schema structure into a tfstate conform structure.
 *
 * Schema input:
 *      "roles": {
 *       "type": "object",
 *       "description": "The roles of the App Registration",
 *       "patternProperties": {
 *         ".*": {
 *           "type": "object",
 *           "properties": {
 *             "app_role_id": {
 *               "type": "string",
 *               "description": "The ID of the app role"
 *             },
 *             "principal_object_id": {
 *               "type": "string",
 *               "description": "The principal object ID"
 *             }
 *           },
 *           "required": ["app_role_id", "principal_object_id"]
 *         }
 *       }
 *
 * Expected result:
 *     roles = {
 *       "veng_developer" = {
 *         app_role_id         = "12341234-1234-1234-1234-12341234"
 *         principal_object_id = "43214321-1234-1234-1234-43214321"
 *       },
 *       "admin" = {
 *         app_role_id         = "12341234-1234-1234-1234-12341234"
 *         principal_object_id = "43214321-1234-1234-1234-43214321"
 *       }
 *     }
 */
public class DictionaryObjectParser extends ExtensionProvider {

    private static final Logger log = Logger.getLogger(DictionaryObjectParser.class.getName());

    public static String transform(Object input){
        if(input == null){
            log.log(Level.WARNING, "Input was null. Could not translate into dict.");
            return "";
        }
        if(!(input instanceof SizeLimitingPyMap)){
            log.log(Level.WARNING, "Input seems not to be a map. Maybe wrong reference?");
            return "";
        }
        SizeLimitingPyMap map = (SizeLimitingPyMap) input;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Iterator<Map.Entry<String,Object>> it = map.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Object> elem = it.next();
            sb.append(generateTreeResponse(elem.getKey(),elem.getValue(),0));
            if(it.hasNext()){
                sb.append(",").append("\n");
            }
        }
        sb.append("}");


        return sb.toString();
    }

    private static String generateTreeResponse(final String key, final Object value,final int depth){
        final StringBuilder sb = new StringBuilder();
        sb.append("    ".repeat(Math.max(0, depth))) //add possible indentation first
                .append("\"").append(key).append("\"");
        if(value instanceof HashMap<?,?>){
            log.log(Level.INFO, "Input is a hashmap - building mapped structure");
            sb.append(" = {").append("\n"); //add map head
            for (Map.Entry<?, ?> next : ((HashMap<?, ?>) value).entrySet()) {
                sb.append(generateTreeResponse((String) next.getKey(), next.getValue(), depth + 1));
            }
            sb.append("}");
        }else if(value instanceof String){

            sb.append(" ").append("=").append(" ").append("\"").append(value).append("\"").append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getNamespace() {
        return "tf";
    }

    @Override
    public String getLocalName() {
        return "dictobjparser";
    }

    @Override
    public String getFunctionName() {
        return "transform";
    }
}
