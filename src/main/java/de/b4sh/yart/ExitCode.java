package de.b4sh.yart;

public enum ExitCode {
    //file related errors
    SCHEMA_FILE_NOT_FOUND(-1000, "Schema-File not found"),
    CONFIG_FILE_NOT_FOUND(-1001, "Config-File not found"),
    TEMPLATE_DIR_VAR_NOT_FOLDER(-1002, "Template directory is not a directory"),
    OUTPUT_DIR_NOT_CREATABLE(-1003, "Cannot create output dir. Is this mount ro?"),
    //config replated errors
    CONFIG_CONTAINS_ERRORS(-2000, "Config-File is not valid against schema. Please check system log for further details"),
    //templating related errors
    DYNAMIC_TEMPLATE_DOES_NOT_COMPLY_CONFIG(-6000,"Dynamic template structure does not comply with configuration structure. Please check both for validity"),
    ;

    private final int number;
    private final String reason;

    ExitCode(int number, String reason) {
        this.number = number;
        this.reason = reason;
    }

    public int getNumber() {
        return number;
    }

    public String getReason() {
        return reason;
    }
}
