package de.b4sh.yart.extensions.array;

import com.hubspot.jinjava.objects.collections.SizeLimitingPyList;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArrayContentStringify {

    private static final Logger log = Logger.getLogger(ArrayContentStringify.class.getName());

    public static String stringifyArray(Object input){
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

}
