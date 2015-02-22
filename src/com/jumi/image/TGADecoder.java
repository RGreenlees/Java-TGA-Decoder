/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jumi.image;

import com.jumi.image.exception.InvalidColorDepthException;
import com.jumi.image.exception.InvalidDataTypeException;
import com.jumi.image.exception.InvalidImageSizeException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * TGADecoder
 * 
 * A simple TGA decoder. Current supports 24 and 32-bit images, uncompressed or RLE compressed
 * 
 * @author Richard Greenlees
 */
public class TGADecoder {

    private static final int UNCOMPRESSED_RGB = 0x2;
    private static final int RLE_RGB = 0xA;

    public static enum ImageType {
        RGB, RGBA, INVALID
    };

    private int imageWidth;
    private int imageHeight;
    private int bpp;
    private int bytesPerPixel;
    private int imageSize;
    private int imagePixels;
    private InputStream input;
    private ImageType imageType;
    private int compression;

    public TGADecoder(InputStream in) {
        try {
            input = in;
            byte[] header = new byte[18];

            input.read(header, 0, 18);

            imageWidth = (int) (((header[13] & 0xFF) << 8) | (header[12] & 0xFF));
            imageHeight = (int) (((header[15] & 0xFF) << 8) | (header[14] & 0xFF));
            bpp = header[16];
            compression = header[2];

            if (bpp == 32) {
                imageType = ImageType.RGBA;
            } else if (bpp == 24) {
                imageType = ImageType.RGB;
            } else {
                imageType = ImageType.INVALID;
                throw new InvalidColorDepthException();
            }

            if (imageWidth <= 0 || imageHeight <= 0) {
                throw new InvalidImageSizeException();
            }

            if (compression != UNCOMPRESSED_RGB && compression != RLE_RGB) {
                throw new InvalidDataTypeException();
            }

            bytesPerPixel = bpp / 8;

            imageSize = imageWidth * imageHeight * bytesPerPixel;
            imagePixels = imageWidth * imageHeight;

        } catch (IOException e) {
            System.err.println("TGA Decoder Error: Unexpected error while decoding");
            e.printStackTrace();
        } catch (InvalidColorDepthException e) {
            System.err.println("TGA Decoder Error: The color depth (bpp) must be 24 or 32");
        } catch (InvalidImageSizeException e) {
            System.err.println("TGA Decoder Error: The image width and height must be at least 1 pixel");
        } catch (InvalidDataTypeException e) {
            System.err.println("TGA Decoder Error: The decoder only supports uncompressed RGB or Runlength Encoded RGB");
        }
    }

    public boolean decode(ByteBuffer buf) {
        if (compression == RLE_RGB) {
            return decodeCompressed(buf);
        } else {
            byte[] imageData = new byte[imageSize];
            try {
                input.read(imageData, 0, imageSize);
            } catch (IOException ex) {
                System.err.println("TGA Decoder Error: The decoder encountered an unexpected issue while reading the data");
                ex.printStackTrace();
                return false;
            }

            for (int i = 0; i < imageData.length - bytesPerPixel; i += bytesPerPixel) {
                buf.put(imageData[i + 2]);
                buf.put(imageData[i + 1]);
                buf.put(imageData[i]);
                if (bytesPerPixel > 3) {
                    buf.put(imageData[i + 3]);
                }
            }
        }
        return true;
    }

    private boolean decodeCompressed(ByteBuffer buf) {
        int currentPixel = 1;
        int totalBytes = 0;
        int currentByte = 0;
        byte imageData[];
        try {
            totalBytes = input.available();
            imageData = new byte[totalBytes];
            input.read(imageData, 0, totalBytes);
        } catch (IOException ex) {
            System.err.println("TGA Decoder Error: The decoder encountered an unexpected issue while reading the data");
            ex.printStackTrace();
            return false;
        }

        while (currentPixel <= imagePixels) {

            int chunkSize = (imageData[currentByte] & 0xFF);
            currentByte++;

            if (chunkSize < 128) {
                int rawPixels = chunkSize + 1;

                for (int i = 0; i < rawPixels; i++) {
                    buf.put(imageData[currentByte + 2]);
                    buf.put(imageData[currentByte + 1]);
                    buf.put(imageData[currentByte]);

                    if (bytesPerPixel > 3) {
                        buf.put(imageData[currentByte + 3]);
                    }
                    currentByte += bytesPerPixel;
                    currentPixel++;
                }
            } else {
                chunkSize -= 127;
                for (int i = 0; i < chunkSize; i++) {
                    buf.put(imageData[currentByte + 2]);
                    buf.put(imageData[currentByte + 1]);
                    buf.put(imageData[currentByte]);
                    if (bytesPerPixel > 3) {
                        buf.put(imageData[currentByte + 3]);
                    }
                    currentPixel++;
                }
                currentByte += bytesPerPixel;
            }
        }
        return true;
    }

    public int getWidth() {
        return imageWidth;
    }

    public int getHeight() {
        return imageHeight;
    }

    public int getImageSizeInBytes() {
        return imageSize;
    }

    public int getColorDepth() {
        return bpp;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public int getPixelCount() {
        return imagePixels;
    }

}
