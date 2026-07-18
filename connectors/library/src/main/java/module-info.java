module at.pegelhub.library {
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.slf4j;
    opens at.pegelhub.lib.model;
    opens at.pegelhub.lib.internal.dto;
    exports at.pegelhub.lib;
    exports at.pegelhub.lib.config;
    exports at.pegelhub.lib.model;
    exports at.pegelhub.lib.runtime;
}
