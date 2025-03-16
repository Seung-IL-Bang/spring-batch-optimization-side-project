package com.project.batch_service.domain.products;

public enum TaxType {

    TAXABLE("과세"),
    TAX_FREE("비과세");

    private String description;

    TaxType(String description) {
        this.description = description;
    }

}
