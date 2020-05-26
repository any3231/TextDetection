package com.myTest;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.sun.org.apache.xpath.internal.axes.SelfIteratorNoPredicate;

import javafx.util.Pair;
import jdk.nashorn.internal.ir.Flags;


public class VisualBoundaryDetector {
	int h, w, id, screenWidth, screenHeight;
	int fromX, endX, fromY, endY;
	int fontSize; //文字绝对高度
	int fontColor; //文字颜色
	double fontSizeTotalRatio; //文字高度占屏幕的比值
	double fontSizeNodeRatio; //文字高度占节点的比值
	BufferedImage img;
	int visualBound[];
	final int sizeThreshold = 500000;
	final double colorThreshold = 0.3;
	final double borderThreshold = 0.8;
	final double areaThreshold = 0.2;
	final double heightThreshold = 0.3;
	final int colorDiffThreshold = 10000;
	final double blankThreshold = 0.99;
	
	VisualBoundaryDetector(BufferedImage imgFile, int id, int screenWidth, int screenHeight) {
		this.w = imgFile.getWidth();
		this.h = imgFile.getHeight();
		this.img = imgFile;
		this.fontSize = this.h;
		this.fontColor = 0;
		this.fromX = 0;
		this.fromY = 0;
		this.endX = this.h-1;
		this.endY = this.w-1;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.visualBound = new int[4];
		this.visualBound[0] = this.visualBound[1] = 0;
		this.visualBound[2] = this.w-1;
		this.visualBound[3] = this.h-1;
		this.id = id;
	}
	
	boolean detectHorizontalLine() {
		ArrayList<Pair<Integer,Integer>> lines = new ArrayList();
		int i = 0;
		while (i<this.h) {
			int preColor = 0;
			int colorCnt = 0;
			int maxCnt = 0;
			int maxColor = 0;
			for (int j=0; j<this.w; j++) {
				int color = this.img.getRGB(j, i);
				if (color == preColor) {
					colorCnt += 1;
				}
				else {
					if (colorCnt>maxCnt) {
						maxCnt = colorCnt;
						maxColor = preColor;
					}
					preColor = color;
					colorCnt = 1;
				}
			}
			if (colorCnt>maxCnt) {
				maxCnt = colorCnt;
				maxColor = preColor;
			}
			if (((double)maxCnt)*1.0/(double)this.w > borderThreshold) {
				int k = i+1;
				while (k<this.h) {
					int cnt = 0;
					for (int j=0; j<this.w; j++) {
						int color = this.img.getRGB(j, k);
						if (color == maxColor) {
							cnt += 1;
						}
					}
					if (((double)cnt)/(double)this.w <= borderThreshold) {
						break;
					}
					k += 1;
				}
				if (k-i<5) {
					lines.add(new Pair<>(i,k-1));
				}
				i = k-1;
			}
			i += 1;
		}
		for (Pair<Integer, Integer>line : lines) {
			if (line.getKey()<this.h/2) {
				this.fromX = line.getValue()+1;
			}
			else {
				this.endX = line.getKey()-1;
			}
		}
		if (lines.isEmpty()) {
			return false;
		}
		return true;
	}
	
	void detectVerticalLimit() {
		if (this.fromX>this.endX) {
			return;
		}
		return;
	}
	
	int getColorDis(int a, int b) {
		int dis = 0;
		for (int i=0; i<3; i++) {
			int tmp1 = a&0xFF;
			int tmp2 = b&0xFF;
			dis += Math.pow(tmp1-tmp2,2);
			a>>=8;
			b>>=8;
		}
		return dis;
	}
	
	void detectTextBoundary() throws IOException {
		if (this.h*this.w>sizeThreshold) {
			System.out.println("OUT OF SIZE");
			return;
		}
		if (this.h<10) {
			System.out.println("TOO THIN");
			return;
		}
		this.detectHorizontalLine();
		this.detectVerticalLimit();
		if ((this.fromX>this.endX) || (this.fromY>this.endY)) {
			return;
		}
		ArrayList<Integer> colorList = new ArrayList<Integer>(); 
		ArrayList<Pair<Integer,Integer>> colorSet = new ArrayList<Pair<Integer,Integer>>();
		for (int i=this.fromX; i<=this.endX; i++)
			for (int j=this.fromY; j<=this.endY; j++) {
				int crtColor = this.img.getRGB(j,i);
				colorList.add(crtColor);
			}
		Collections.sort(colorList);
		int cnt = 0;
		int len = colorList.size();
		for (int i=0; i<len; i++) {
			Integer crtColor = colorList.get(i);
			cnt += 1;
			if (i==len-1 || (!crtColor.equals(colorList.get(i+1)))) {
				colorSet.add(new Pair<>(crtColor, cnt));
				cnt = 0;
			}
		}
		Collections.sort(colorSet,new Comparator<Pair<Integer,Integer>>() 
		{
			public int compare(Pair<Integer,Integer> o1,Pair<Integer,Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		int maxColor = -1;
		int minX = this.h;
		int minY = this.w;
		int maxX = 0;
		int maxY = 0;
		for (int cId=0; cId<colorSet.size(); cId++) {
			Pair<Integer, Integer> colorPair = colorSet.get(cId);
			int crtColor = colorPair.getKey();
			int crtCnt = colorPair.getValue();
			if (((double)crtCnt*1.0/(double)(this.h*this.w))<this.colorThreshold) {
				boolean flag = false;
				for (int preId=0; preId<cId; preId++) {
					Pair<Integer, Integer> preColorPair = colorSet.get(preId);
					int preColor = preColorPair.getKey();
					int colorDis = this.getColorDis(crtColor, preColor);
					if (colorDis>2000) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					continue;
				}
				maxColor = crtColor;
				minX = this.h;
				minY = this.w;
				maxX = 0;
				maxY = 0;
				for (int i=this.fromX; i<=this.endX; i++) {
					for (int j=this.fromY; j<=this.endY; j++) {
						int tmpColor = this.img.getRGB(j, i);
						int colorDis = this.getColorDis(maxColor, tmpColor);
						if (colorDis<1000) {
							minX = Math.min(minX, i);
							maxX = Math.max(maxX, i);
							minY = Math.min(minY, j);
							maxY = Math.max(maxY, j);
						}
					}
				}
				if (((double)crtCnt*1.0/(double)((maxX-minX+1)*(maxY-minY+1)))>=this.colorThreshold*1.5) {
					continue;
				}
				break;
			}
		}
		if (minX>maxX) {
			swap(minX,maxX);
		}
		if (minY>maxY) {
			swap(minY,maxY);
		}
		this.fontSize = maxX-minX+1;
		this.fontColor = maxColor;
		this.fontSizeNodeRatio = (double)(this.fontSize)*1.0/this.h;
		this.fontSizeTotalRatio = (double)(this.fontSize)*1.0/this.screenHeight;
		this.visualBound[0] = minY;
		this.visualBound[1] = minX;
		this.visualBound[2] = maxY;
		this.visualBound[3] = maxX;
		/*System.out.println("text detection:");
		System.out.println("size:");
		System.out.println(this.fontSize);
		System.out.println("size ratio1:");
		System.out.println(this.fontSizeNodeRatio);
		System.out.println("size ratio2:");
		System.out.println(this.fontSizeTotalRatio);
		System.out.println("color:");
		Color color = new Color(this.fontColor);
		System.out.println(color.getRed());
		System.out.println(color.getGreen());
		System.out.println(color.getBlue());
		System.out.println(this.fontColor);
		System.out.println("boundary:");
		for (int i : this.visualBound) {
			System.out.print(i);
			System.out.print(" ");
		}
		System.out.println();
		Graphics graphics = this.img.getGraphics();
		graphics.setColor(new Color(0, 255, 0));
		graphics.drawRect(this.visualBound[0],this.visualBound[1],this.visualBound[2]-this.visualBound[0],this.visualBound[3]-this.visualBound[1]);//画线框
		ImageIO.write(this.img, "jpg", new File("/Users/tinaht/Desktop/Lab/TextDetection/data/res_"+String.valueOf(this.id)+".jpg"));
		graphics.dispose();*/
	}
	
	public static void swap(Integer num1, Integer num2) {
        try {
            Field field = Integer.class.getDeclaredField("value");
            // 私有属性访问需要设置权限，
            field.setAccessible(true);
            int temp = num1;
            field.set(num1,num2);
            field.setInt(num2,temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
