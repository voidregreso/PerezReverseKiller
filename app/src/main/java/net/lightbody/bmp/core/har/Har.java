package net.lightbody.bmp.core.har;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Har {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private volatile HarLog log;

    public Har() {
    }

    public Har(HarLog log) {
        this.log = log;
    }

    public HarLog getLog() {
        return log;
    }

    public void setLog(HarLog log) {
        this.log = log;
    }

    public void writeTo(File file) throws IOException {
        OBJECT_MAPPER.writeValue(file, this);
    }
}
