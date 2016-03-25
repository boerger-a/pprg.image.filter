package pprg.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageFilter {

	private static TimeLogger t = new TimeLogger();

	public static void main(String[] args) throws IOException {

		t.start();
		BufferedImage img = ImageIO.read(new File("original.png"));
		t.end("Read image");
		BufferedImage output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);

		traceColor(img, 2, 2);
		traceColor(img, 2, 3);
		traceColor(img, 2, 4);
		traceColor(img, 3, 2);
		traceColor(img, 3, 3);
		traceColor(img, 3, 4);
		traceColor(img, 4, 2);
		traceColor(img, 4, 3);
		traceColor(img, 4, 4);

		t.start();
		applyFilter(img, output, FilterGenerator.sharpeningFilter());
		t.end("Apply ITF");

		traceColor(output, 3, 3);

		t.start();
		ImageIO.write(output, "png", new File("output.png"));
		t.end("Write image");

		// ScheduledExecutorService executor =
		// Executors.newScheduledThreadPool(256);
		// Runnable task = () -> {
		// String threadName = Thread.currentThread().getName();
		// System.out.println("Hello " + threadName);
		// };
		//
		// executor.scheduleWithFixedDelay(task, 0, 1, TimeUnit.SECONDS);
	}

	private static void applyFilter(BufferedImage image, BufferedImage output, double[][] filter) {
		int offset = (filter.length - 1) / 2;

		// loop over all pixels (except border)
		for (int y = offset; y < image.getHeight() - offset; y++) {
			for (int x = offset; x < image.getWidth() - offset; x++) {

				// loop over pixels for 1 mask
				int[][] pixels = new int[filter.length][filter[0].length];
				int i = 0, j = 0;
				for (int z = -offset; z <= offset; z++) {
					for (int a = -offset; a <= offset; a++) {
						pixels[i][j++] = image.getRGB(x + z, y + a);
					}
					i++;
					j = 0;
				}

				// apply filter with found pixels
				output.setRGB(x, y, applyFilter(pixels, filter));
			}
		}
	}

	private static int applyFilter(int[][] pixels, double[][] filter) {
		double alpha = 0, red = 0, green = 0, blue = 0;
		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels.length; j++) {
				alpha = getAlpha(pixels[i][j]);
				red += getRed(pixels[i][j]) * filter[i][j];
				green += getGreen(pixels[i][j]) * filter[i][j];
				blue += getBlue(pixels[i][j]) * filter[i][j];
			}
		}

		return getColor((int) Math.round(alpha), (int) Math.round(red), (int) Math.round(green),
				(int) Math.round(blue));
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
}
