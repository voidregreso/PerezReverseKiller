package com.perez.code;
import org.jb.dexlib.*;

public class FieldEditor {
    ClassDataItem.EncodedField field;
    String classType;
    String fieldType;
    String fieldName;
    int accessFlags;

    public FieldEditor() {
    }

    public FieldEditor(ClassDataItem.EncodedField field) {
        this.field = field;
        classType = field.field.getContainingClass().getTypeDescriptor();
        fieldType = field.field.getFieldType().getTypeDescriptor();
        fieldName = field.field.getFieldName().getStringValue();
        accessFlags = field.accessFlags;
    }

    public ClassDataItem.EncodedField copyEncodedField(DexFile dexFile) {
        FieldIdItem field = CodeEditor.copyFieldIdItem(dexFile,
                            this.field.field);
        return new ClassDataItem.EncodedField(field, accessFlags);
    }
}
