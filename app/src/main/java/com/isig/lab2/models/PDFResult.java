package com.isig.lab2.models;

import java.io.Serializable;
import java.util.List;

public class PDFResult implements Serializable {
    private List<PDFRes> results;

    public class PDFRes implements Serializable {
        private PDF_URL value;
    }

    public class PDF_URL implements Serializable {
        private String url;
    }

    public String getPdfUrl() {
        if (!results.isEmpty()) {
            if (results.get(0).value != null) {
                if (results.get(0).value.url != null) {
                    return results.get(0).value.url;
                }
            }
        }
        return null;
    }

}
