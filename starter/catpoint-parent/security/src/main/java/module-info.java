module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires org.slf4j;
    requires miglayout.swing;
//    requires org.junit.jupiter.api;
//    requires org.junit.jupiter.engine;
//    requires org.junit.jupiter.params;
//    opens com.udacity.catpoint.security.service to org.junit.platform.commons;
    opens com.udacity.catpoint.security.data to com.google.gson;
}