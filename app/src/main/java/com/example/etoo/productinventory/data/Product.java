package com.example.etoo.productinventory.data;

import android.os.Parcelable;

import java.io.Serializable;

public class Product implements Serializable{
    private String productName;
    private String purchasePrice;
    private String sellingPrice;
    private String productQuantity;
    private String productSupplier;
    private String productSupplierPhone;
    private String productBarcode;
    private String Image;
    private String productGroup;
    public Product(){

    }

    public Product(String productBarcode, String productName, String sellingPrice,
                   String productQuantity, String productSupplier, String productSupplierPhone, String Image,String productGroup) {
        this.productBarcode = productBarcode;
        this.productName = productName;
        this.sellingPrice = sellingPrice;
        this.productQuantity = productQuantity;
        this.productSupplier = productSupplier;
        this.productSupplierPhone = productSupplierPhone;
        this.productGroup = productGroup;
        this.Image = Image;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public void setProductGroup(String productGroup) {
        this.productGroup = productGroup;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public String getProductBarcode() {
        return productBarcode;
    }

    public void setProductBarcode(String productBarcode) {
        this.productBarcode = productBarcode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(String purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(String sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(String productQuantity) {
        this.productQuantity = productQuantity;
    }

    public String getProductSupplier() {
        return productSupplier;
    }

    public void setProductSupplier(String productSupplier) {
        this.productSupplier = productSupplier;
    }

    public String getProductSupplierPhone() {
        return productSupplierPhone;
    }

    public void setProductSupplierPhone(String productSupplierPhone) {
        this.productSupplierPhone = productSupplierPhone;
    }

}
