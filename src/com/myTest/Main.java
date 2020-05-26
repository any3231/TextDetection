package com.myTest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import com.myTest.VisualBoundaryDetector;

public class Main {
	
	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis();
			for (int i=0; i<36; i++) {
				BufferedImage originalFile = ImageIO.read(new File("/Users/tinaht/Desktop/Lab/TextDetection/data/"+String.valueOf(i)+".jpg"));
				VisualBoundaryDetector visualBoundaryDetector;
				visualBoundaryDetector = new VisualBoundaryDetector(originalFile,i, 1080, 2244);
				visualBoundaryDetector.detectTextBoundary();
			}
			long endTime = System.currentTimeMillis();
			System.out.println("运行时间:" + (endTime - startTime) + "ms");
		} catch (IOException e) {
			   e.printStackTrace();
		}
	}
}
