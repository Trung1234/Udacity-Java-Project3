module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.google.gson;
    requires org.slf4j;
    requires miglayout.swing;
    opens com.udacity.catpoint.security.data to com.google.gson;
}