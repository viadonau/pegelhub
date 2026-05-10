module at.pegelhub.library {
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires java.sql;
    opens at.pegelhub.lib.model;
    opens at.pegelhub.lib.internal.dto;
    exports at.pegelhub.lib;
    exports at.pegelhub.lib.model;
}