package de.b4sh.yart;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import de.b4sh.yart.extensions.ExtensionProvider;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.reflections.scanners.Scanners.SubTypes;

public class ExtensionLoader {

    private static final Logger log = Logger.getLogger(ExtensionLoader.class.getName());

    public static void loadExtensions(final Jinjava jinjava) {
        Reflections reflections = new Reflections("de.b4sh.yart");
        Set<Class<?>> subTypes = reflections.get(SubTypes.of(ExtensionProvider.class).asClass());
        subTypes.forEach(elemClass -> {
            try {
             Constructor<?> ctr = elemClass.getConstructor(null);
             ExtensionProvider elem = (ExtensionProvider) ctr.newInstance(new Object[]{});
             log.log(Level.INFO, String.format("Adding function: %s in namespace: %s to jinja",elem.getLocalName(),elem.getNamespace()));
             jinjava.getGlobalContext().registerFunction(
                     new ELFunctionDefinition(elem.getNamespace(),
                             elem.getLocalName(),
                             elemClass,
                             elem.getFunctionName(),
                             Object.class));
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                  IllegalAccessException e) {
                log.log(Level.WARNING, "Cloud no construct default constructor for implementation. Throw runtime execption for breaking things up.");
                throw new RuntimeException(e);
            }
        });
    }
}
