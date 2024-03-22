package com.perez.code;

public class CodeEntity {

    String code;
    Language language;

    public CodeEntity(String code) {
        this(code, Language.AUTO);
    }

    public CodeEntity(String code, Language language) {
        this.code = code;
        this.language = language;
    }

    public  enum Language {
        AUTO;
        Language() {
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

}
