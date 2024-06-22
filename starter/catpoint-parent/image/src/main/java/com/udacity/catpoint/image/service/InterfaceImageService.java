package com.udacity.catpoint.image.service;

import java.awt.image.BufferedImage;

public interface InterfaceImageService {
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
