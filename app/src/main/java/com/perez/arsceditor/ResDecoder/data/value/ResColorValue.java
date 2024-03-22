package com.perez.arsceditor.ResDecoder.data.value;

/**
 * @author Ryszard Wi?niewski <brut.alll@gmail.com>
 */
public class ResColorValue extends ResIntValue {
    public ResColorValue(int value, String rawValue) {
        super(value, rawValue, "color");
    }

    @Override
    protected String encodeAsResValue() {
        return String.format("#%08x", mValue);
    }
}