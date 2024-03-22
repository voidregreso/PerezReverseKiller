package com.perez.arsceditor.ResDecoder.data;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryszard Wi?niewski <brut.alll@gmail.com>
 */
public class ResConfig {
    private final ResConfigFlags mFlags;
    private final Map<ResResSpec, ResResource> mResources = new LinkedHashMap<ResResSpec, ResResource>();

    public ResConfig(ResConfigFlags flags) {
        this.mFlags = flags;
    }

    public void addResource(ResResource res) throws IOException {
        addResource(res, false);
    }

    public void addResource(ResResource res, boolean overwrite) throws IOException {
        ResResSpec spec = res.getResSpec();
        if(mResources.put(spec, res) != null && !overwrite) {
            /**
             * throw new IOException(String.format( "Multiple resources:
             * spec=%s, config=%s", spec, this));
             */
        }
    }

    public ResConfigFlags getFlags() {
        return mFlags;
    }

    public ResResource getResource(ResResSpec spec) throws IOException {
        ResResource res = mResources.get(spec);
        if(res == null) {
            /*
             * throw new IOException(String.format(
             * "resource: spec=%s, config=%s", spec, this));
             */
        }
        return res;
    }

    public Set<ResResource> listResources() {
        return new LinkedHashSet<ResResource>(mResources.values());
    }

    public Set<ResResSpec> listResSpecs() {
        return mResources.keySet();
    }

    @Override
    public String toString() {
        return mFlags.toString();
    }
}
