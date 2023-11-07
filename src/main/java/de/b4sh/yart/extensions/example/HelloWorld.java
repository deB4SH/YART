package de.b4sh.yart.extensions.example;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyList;
import de.b4sh.yart.extensions.ExtensionProvider;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Simple example hello world.
 */
public class HelloWorld extends ExtensionProvider {

    private static final Logger log = Logger.getLogger(HelloWorld.class.getName());

    public static String transform(Object input){
        if(!(input instanceof String)){
            log.log(Level.WARNING, "Input was null or not a string. Could not stringify contents in an array");
            return null;
        }
        return String.format("Hello %s. This is world!", input);
    }

    @Override
    public String getNamespace() {
        return "example";
    }

    @Override
    public String getLocalName() {
        return "hello";
    }

    @Override
    public String getFunctionName() {
        return "transform";
    }
}
