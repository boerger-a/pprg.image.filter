package pprg.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

public class ImageFilter {

	private static final int NUM_THREADS = 100;
	private static TimeLogger t = new TimeLogger();

	public static void main(String[] args) throws IOException {

		t.start();
		BufferedImage img = ImageIO.read(new File("original.png"));
		t.end("Read image");

		t.start();
		int[][][] array = convertToArray(img, img.getWidth(), img.getHeight());
		t.end("Convert to array");

		t.start();
		applyFilter(array, FilterGenerator.smoothingFilter(9));
		t.end("Apply ITF");

		t.start();
		BufferedImage output = convertToImage(array, img.getWidth(), img.getHeight());
		t.end("Convert to image");

		t.start();
		ImageIO.write(output, "png", new File("output.png"));
		t.end("Write image");
	}

	private static void applyFilter(int[][][] image, double[][] filter) {
		int offset = (filter.length - 1) / 2;

		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

		// loop over all pixels (except border)
		for (int x = offset; x < image.length - offset; x++) {
			for (int y = offset; y < image[0].length - offset; y++) {

				// loop over pixels for 1 mask
				final int xx = x;
				final int yy = y;
				Runnable task = () -> {
					int[][][] pixels = new int[filter.length][filter[0].length][4];
					int i = 0, j = 0;
					for (int z = -offset; z <= offset; z++) {
						for (int a = -offset; a <= offset; a++) {
							pixels[i][j++] = image[xx + z][yy + a];
						}
						i++;
						j = 0;
					}

					// apply filter with found pixels
					image[xx][yy] = applyFilterOnPixel(pixels, filter);
				};
				executor.execute(task);
			}
		}

		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static int[] applyFilterOnPixel(int[][][] pixels, double[][] filter) {
		double alpha = 0, red = 0, green = 0, blue = 0;
		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels.length; j++) {
				red += pixels[i][j][0] * filter[i][j];
				green += pixels[i][j][1] * filter[i][j];
				blue += pixels[i][j][2] * filter[i][j];
				alpha = pixels[i][j][3];
			}
		}

		return new int[] { (int) Math.round(red), (int) Math.round(green), (int) Math.round(blue),
				(int) Math.round(alpha) };
	}

	private static int getAlpha(int color) {
		return (color >> 24) & 0xff;
	}

	private static int getRed(int color) {
		return (color >> 16) & 0xff;
	}

	private static int getGreen(int color) {
		return (color >> 8) & 0xff;
	}

	private static int getBlue(int color) {
		return color & 0xff;
	}

	private static int getColor(int alpha, int red, int green, int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	private static void traceColor(BufferedImage image, int x, int y) {
		int color = image.getRGB(x, y);
		System.out.printf("pixel (%4d,%4d) - alpha: %3d, red: %3d, green: %3d, blue: %3d\n", x, y, getAlpha(color),
				getRed(color), getGreen(color), getBlue(color));
	}

	private static int[][][] convertToArray(BufferedImage image, int width, int height) {
		int[][][] result = new int[width][height][4];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = image.getRGB(x, y);
				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = (pixel) & 0xff;
				result[x][y][0] = red;
				result[x][y][1] = green;
				result[x][y][2] = blue;
				result[x][y][3] = alpha;
			}
		}

		return result;
	}

	private static BufferedImage convertToImage(int[][][] array, int width, int height) {
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = getColor(array[x][y][3], array[x][y][0], array[x][y][1], array[x][y][2]);
				ret.setRGB(x, y, color);
			}
		}
		return ret;
	}

}
