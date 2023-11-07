package de.b4sh.yart.extensions.terraform;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyList;
import de.b4sh.yart.extensions.ExtensionProvider;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Transforms given schema structure into a tfstate conform structure.
 *
 * Schema Input:
 *      "appOwners": {
 *           "type": "array",
 *           "description": "The owners of the App Registration",
 *           "items": {
 *             "type": "string"
 *          }
 *      }
 *
 * Function call in jinja2 template:
 * {{ tf:arrayctnstring(YOUR_SCHEMA_REFERENCE_HERE) }}
 *
 * Expected result:
 *      ["Mr x","Mr y"]
 *
 */
public class ArrayContentStringify extends ExtensionProvider {

    private static final Logger log = Logger.getLogger(ArrayContentStringify.class.getName());

    public static String transform(Object input){
        if(input == null){
            log.log(Level.WARNING, "Input was null. Could not stringify contents in an array");
            return null;
        }
        if(input instanceof SizeLimitingPyList){
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            Iterator<Object> it = ((SizeLimitingPyList) input).iterator();
            while(it.hasNext()){
                Object elem = it.next();
                sb.append('\"').append(elem.toString()).append('\"');
                if(it.hasNext()){
                    sb.append(',');
                }
            }
            sb.append("]");
            return sb.toString();
        }
        return "";
    }

    @Override
    public String getNamespace() {
        return "tf";
    }

    @Override
    public String getLocalName() {
        return "arrayctnstring";
    }

    @Override
    public String getFunctionName() {
        return "transform";
    }
}
