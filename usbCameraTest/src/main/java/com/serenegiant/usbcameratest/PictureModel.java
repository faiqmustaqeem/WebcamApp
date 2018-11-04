package com.serenegiant.usbcameratest;

import android.graphics.Bitmap;

public class PictureModel {
    private String image;

    public PictureModel(String image )
    {
        this.setImage(image);

    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

}
