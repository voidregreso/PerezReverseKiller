package com.perez.arsceditor.ResDecoder;

import java.io.IOException;

import com.perez.arsceditor.ResDecoder.data.ResResource;


/**
 * @author Ryszard Wi?niewski <brut.alll@gmail.com>
 * @author com.perezHai
 */
public interface GetResValues {
    public void getResValues(ARSCCallBack back, ResResource res) throws IOException;
}
