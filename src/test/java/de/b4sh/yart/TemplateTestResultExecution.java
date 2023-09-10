package de.b4sh.yart;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TemplateTestResultExecution implements AfterTestExecutionCallback {

    private static final Logger log = Logger.getLogger(TemplateTestResultExecution.class.getName());
    @Override
    public void afterTestExecution(final ExtensionContext extensionContext) throws Exception {
        if(!extensionContext.getExecutionException().isEmpty()){
            log.log(Level.WARNING, String.format("Test %s failed with error.", extensionContext.getDisplayName()));
            return;
        }
        //removing test-dir on green test
        log.log(Level.INFO, String.format("Test %s was successfull. Removing test-directory now for cleanups.",extensionContext.getDisplayName()));
        final TemplaterTest o = (TemplaterTest) extensionContext.getTestInstance().get();
        final File f = new File(o.getCurrentTestDir());
        if(f.isDirectory()){
            FileHelper.deleteFolderWithContent(f.toPath());
        }else{
            log.log(Level.INFO,String.format("Test %s did not create a directory?",extensionContext.getDisplayName()));
        }
    }
}
