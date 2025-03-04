package com.project.batch_service.domain.products;

public enum ProductCategory {

    ITEM_ONE("상품1"),
    ITEM_TWO("상품2"),
    ITEM_THREE("상품3"),
    ITEM_FORE("상품4"),
    ITEM_FIVE("상품5"),
    ITEM_SIX("상품6"),
    ITEM_SEVEN("상품7"),
    ITEM_EIGHT("상품8"),
    ITEM_NINE("상품9"),
    ITEM_TEN("상품10");

    private String description;

    ProductCategory(String description) {
        this.description = description;
    }

}
