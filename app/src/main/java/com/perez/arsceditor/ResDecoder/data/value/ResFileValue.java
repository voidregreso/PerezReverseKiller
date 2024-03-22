package com.perez.arsceditor.ResDecoder.data.value;

import java.io.IOException;

import com.perez.arsceditor.ResDecoder.ARSCCallBack;
import com.perez.arsceditor.ResDecoder.GetResValues;
import com.perez.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wi?niewski <brut.alll@gmail.com>
 */
public class ResFileValue extends ResIntBasedValue implements GetResValues {
    private final String mPath;

    public ResFileValue(String path, int rawIntValue) {
        super(rawIntValue);
        this.mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(),
                  getStrippedPath());
    }

    public String getStrippedPath() throws IOException {
        if(!mPath.startsWith("res/"))
            throw new IOException("File path does not start with \"res/\": " + mPath);
        return mPath;/* .substring(4); */
    }

    @Override
    public String toString() {
        return mPath;
    }
}
