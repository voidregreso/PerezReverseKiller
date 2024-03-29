package com.perez.arsceditor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import com.perez.arsceditor.ResDecoder.ARSCCallBack;
import com.perez.arsceditor.ResDecoder.ARSCDecoder;
import com.perez.arsceditor.ResDecoder.AXMLDecoder;
import com.perez.arsceditor.ResDecoder.GetResValues;
import com.perez.arsceditor.ResDecoder.data.ResPackage;
import com.perez.arsceditor.ResDecoder.data.ResResource;
import com.perez.arsceditor.ResDecoder.data.ResTable;
import com.perez.arsceditor.ResDecoder.data.ResValuesFile;

final public class AndrolibResources {

    // TODO: dirty static hack. I have to refactor decoding mechanisms.
    public static boolean sKeepBroken = true;





    private Context context;

    
    public ARSCDecoder mARSCDecoder;

    
    public AXMLDecoder mAXMLDecoder;

    
    //public Elf elfParser;

    private ResPackage pkg;

    public AndrolibResources(Context context) {
        this.context = context;
    }

    
    public void decodeARSC(ResTable resTable, ARSCCallBack callback) throws IOException {
        for(ResPackage pkg : resTable.listMainPackages()) {
            for(ResValuesFile valuesFile : pkg.listValuesFiles())
                generateValuesFile(valuesFile, callback);
        }
    }

    /**
     * AXML
     *
     * @param AXMLStream
     * @return 
     * @throws IOException
     */
    public void decodeAXML(InputStream AXMLStream) throws IOException {
        mAXMLDecoder = AXMLDecoder.read(AXMLStream);
    }

    @SuppressLint("NewApi")
    private void generateValuesFile(ResValuesFile valuesFile, ARSCCallBack callback) throws IOException {
        for(ResResource res : valuesFile.listResources()) {
            if(valuesFile.isSynthesized(res))
                continue;
            ((GetResValues) res.getValue()).getResValues(callback, res);
        }
    }

    public ResPackage getFramPackage() {
        return pkg;
    }

    private ResPackage[] getResPackagesFromARSC(ARSCDecoder decoder, InputStream ARSCStream, ResTable resTable,
            boolean keepBroken) throws IOException {
        return decoder.decode(decoder, new BufferedInputStream(ARSCStream), false, keepBroken, resTable).getPackages();
    }

    public ResTable getResTable() {
        ResTable resTable = new ResTable(this);
        return resTable;
    }

    public ResTable getResTable(InputStream ARSCStream) throws IOException {
        return getResTable(ARSCStream, true);
    }

    public ResTable getResTable(InputStream ARSCStream, boolean loadMainPkg) throws IOException {
        ResTable resTable = new ResTable(this);
        if(loadMainPkg)
            loadMainPkg(resTable, ARSCStream);
        return resTable;
    }

    public ResPackage loadMainPkg(ResTable resTable, InputStream ARSCStream) throws IOException {
        mARSCDecoder = new ARSCDecoder(new BufferedInputStream(ARSCStream), resTable, sKeepBroken);
        ResPackage[] pkgs = getResPackagesFromARSC(mARSCDecoder, ARSCStream, resTable, sKeepBroken);
        ResPackage pkg = null;
        switch(pkgs.length) {
        case 1:
            pkg = pkgs[0];
            break;
        case 2:
            if(pkgs[0].getName().equals("android")) {
                pkg = pkgs[1];
            } else if(pkgs[0].getName().equals("com.htc")) {
                pkg = pkgs[1];
            }
            break;
        default:
            pkg = selectPkgWithMostResSpecs(pkgs);
        }
        if(pkg == null)
            throw new IOException("Arsc files with zero or multiple packages");
        resTable.addPackage(pkg, true);
        return pkg;
    }

    public void pushFramResPackage(ResPackage pkg) {
        this.pkg = pkg;
    }

    public ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs) throws IOException {
        int id = 0;
        int value = 0;
        for(ResPackage resPackage : pkgs) {
            if(resPackage.getResSpecCount() > value && !resPackage.getName().equalsIgnoreCase("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
            }
        }
        // if id is still 0, we only have one pkgId which is "android" -> 1
        return (id == 0) ? pkgs[0] : pkgs[1];
    }
}
