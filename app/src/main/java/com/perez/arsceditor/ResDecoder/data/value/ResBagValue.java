package com.perez.arsceditor.ResDecoder.data.value;

import java.io.IOException;

import com.perez.arsceditor.ResDecoder.ARSCCallBack;
import com.perez.arsceditor.ResDecoder.GetResValues;
import com.perez.arsceditor.ResDecoder.IO.Duo;
import com.perez.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wi?niewski <brut.alll@gmail.com>
 */
public class ResBagValue extends ResValue implements GetResValues {
    protected final ResReferenceValue mParent;

    public ResBagValue(ResReferenceValue parent) {
        this.mParent = parent;
    }

    public ResReferenceValue getParent() {
        return mParent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
        String type = res.getResSpec().getType().getName();
        if("style".equals(type)) {
            new ResStyleValue(mParent, new Duo[0], null).getResValues(back, res);
            return;
        }
        if("array".equals(type)) {
            new ResArrayValue(mParent, new Duo[0]).getResValues(back, res);
            return;
        }
    }
}
