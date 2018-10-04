package com.silaev.wms.converter;

import com.silaev.wms.entity.Brand;

import java.beans.PropertyEditorSupport;

public class StringToBrandConverter extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(Brand.byName(text));
    }
}
