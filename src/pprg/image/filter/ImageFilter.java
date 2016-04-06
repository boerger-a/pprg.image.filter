package pprg.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

public class ImageFilter {

	private static final int NUM_THREADS = 4;
	private static TimeLogger t = new TimeLogger();

	public static void main(String[] args) throws IOException {

		t.start();
		BufferedImage img = ImageIO.read(new File("./images/original.jpg"));
		t.end("Read image");

		t.start();
		BufferedImage output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilter(img, output, FilterGenerator.smoothingFilter(15));
		t.end("Apply filter: smoothing");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing.png"));
		t.end("Write image");
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilter(img, output, FilterGenerator.detectEdgesLoGFilter());
		t.end("Apply filter: detect edges (LoG)");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_log_edges.png"));
		t.end("Write image");
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilter(img, output, FilterGenerator.detectVerticalEdgesSobelFilter());
		t.end("Apply filter: detect vertical edges (sobel)");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_sobel_edges_v.png"));
		t.end("Write image");
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilter(img, output, FilterGenerator.detectHorizontalEdgesSobelFilter());
		t.end("Apply filter: detect horizontal edges (sobel)");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_sobel_edges_h.png"));
		t.end("Write image");
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilter(img, output, FilterGenerator.sharpeningFilter(7));
		t.end("Apply filter: sharpening");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_sharpening.png"));
		t.end("Write image");
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterSerial(img, output, FilterGenerator.sharpeningFilter(7));
		t.end("Apply filter (serial): sharpening");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_serial_sharpening.png"));
		t.end("Write image");
	}
	
	
	private static void applyFilter(BufferedImage image, BufferedImage output, double[][] filter) {
		int offset = (filter.length - 1) / 2;

		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

		// generate chunks on x axis and start NUM_THREADS
		for (int chunk = offset; chunk < image.getWidth() - offset; chunk += NUM_THREADS) {

			// loop over all pixels (except border)
			final int chunkk = chunk;
			Runnable task = () -> {
				for (int x = chunkk; x < (chunkk + NUM_THREADS) && (x < image.getWidth() - offset); x++) {
					for (int y = offset; y < image.getHeight() - offset; y++) {
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
			};

			executor.execute(task);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void applyFilterSerial(BufferedImage image, BufferedImage output, double[][] filter) {
		int offset = (filter.length - 1) / 2;

		for (int x = offset; x < image.getWidth() - offset; x += 1) {
			for (int y = offset; y < image.getHeight() - offset; y++) {
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
	
	// To compare serial and parallel execution
	public static void main2(String[] args) throws IOException {
		BufferedImage output;		
		
		t.start();
		BufferedImage img = ImageIO.read(new File("./images/P6184511.jpg"));
		t.end("Read image");
		
		for(int i = 1; i <= 5; i += 2) {
			int filterSize = i*3;
			
			t.start();
			output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			applyFilterSerial(img, output, FilterGenerator.smoothingFilter(filterSize));
			t.end("Serial: Filter size = " + filterSize);
	
			t.start();
			ImageIO.write(output, "png", new File("./images/output_smoothing_serial_" + filterSize + ".png"));
			t.end("Write image");		
	
			t.start();
			output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			applyFilter(img, output, FilterGenerator.smoothingFilter(filterSize));
			t.end("Parallel: Filter size = " + filterSize);
	
			t.start();
			ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + filterSize + ".png"));
			t.end("Write image");
		}
	}
	
	// To experiment with different task sizes
	public static void main3(String[] args) throws IOException {
		BufferedImage output;	
		int filterSize = 15;
		
		t.start();
		BufferedImage img = ImageIO.read(new File("./images/P6184511.jpg"));
		t.end("Read image");
		
		int taskSize = 1;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
		
		taskSize = 4;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
		
		taskSize = 10;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
		
		taskSize = 50;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
		
		taskSize = 100;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
		
		taskSize = 500;
		
		t.start();
		output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
		t.end("Parallel: Task size = " + taskSize);

		t.start();
		ImageIO.write(output, "png", new File("./images/output_smoothing_parallel_" + taskSize + ".png"));
		t.end("Write image");
	}

	private static void applyFilterWithThreshold(BufferedImage image, BufferedImage output, double[][] filter, int taskSize) {
		int offset = (filter.length - 1) / 2;

		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

		// generate chunks on x axis and start NUM_THREADS
		for (int chunk = offset; chunk < image.getWidth() - offset; chunk += taskSize) {

			// loop over all pixels (except border)
			final int chunkk = chunk;
			Runnable task = () -> {
				for (int x = chunkk; x < (chunkk + taskSize) && (x < image.getWidth() - offset); x++) {
					for (int y = offset; y < image.getHeight() - offset; y++) {
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
			};

			executor.execute(task);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
