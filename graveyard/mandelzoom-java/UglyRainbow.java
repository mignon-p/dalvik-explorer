// Based on C code copyright (C) 2006 Michael Nidermayer (BSD license).
// Original found at http://guru.multimedia.cx/ascii-mandelbrot-realtime-zoom/

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class UglyRainbow extends Frame {
  private static final double X = -0.105;
  private static final double Y = 0.928;
  
  private static final Color[] colors = new Color[256];
  static {
    for (int i = 0; i < colors.length; ++i) {
      colors[i] = Color.getHSBColor((float) (100.0*i/256.0), 1.0f, 1.0f);
      colors[i] = Color.getHSBColor((float) (i/256.0), 1.0f, 1.0f);
    }
    colors[0] = Color.BLACK;
  }
  
  private ArrayList<Image> frames = new ArrayList<Image>();
  private int frame = -1;
  
  public UglyRainbow() {
    super("Ugly Rainbow");
    setBackground(Color.BLACK);
    setSize(new Dimension(640, 480));
    setVisible(true);
    new Thread(new Runnable() {
      public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        mandelbrot();
        new Thread(new Runnable() {
          public void run() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            cycle();
          }
        }).start();
      }
    }).start();
  }
  
  public static void main(String[] args) {
    new UglyRainbow();
  }
  
  public void mandelbrot() {
    double xStep = 2.6/(double) getWidth();
    double yStep = 2.4/(double) getHeight();
    for (double p = 1.0; p > 0.003; p *= 0.98) {
      BufferedImage image = new BufferedImage(getWidth() + 1, getHeight() + 1, BufferedImage.TYPE_INT_RGB);
      int gy = 0;
      for (double y = -1.2; y < 1.2; y += yStep/*0.07*/) {
        int gx = 0;
        for (double x = -1.6; x < 1.0; x += xStep/*0.03*/) {
          double r = 0.0;
          double i = 0.0;
          double c = 6.0;
          for (; c < 255.0 && (r*r + i*i) < 4.0; ++c) {
            double tmp = r*r - i*i + x*p + X*(1.0 - p);
            i = r*i*2.0 + y*p + Y*(1.0 - p);
            r = tmp;
          }
          int color = (int) (255.0 - c);
          image.setRGB(gx, gy, colors[color].getRGB());
          ++gx;
        }
        ++gy;
      }
      Graphics g = image.getGraphics();
      g.setColor(Color.WHITE);
      g.drawString("Hello, Monkey!", 5, 35);
      
      frames.add(image);
      frame = frames.size() - 1;
      repaint();
    }
  }
  
  public void cycle() {
    System.err.println("cycling " + frames.size() + " frames...");
    frame = 0;
    while (true) {
      ++frame;
      if (frame >= frames.size()) {
        frame = 0;
      }
      repaint();
      try {
        Thread.sleep(30);
      } catch (Exception ex) {
      }
    }
  }
  
  @Override public void update(Graphics g) {
    //System.err.println("frame " + frame);
    if (frame > -1) {
      g.drawImage(frames.get(frame), 0, 0, null);
    }
  }
}
