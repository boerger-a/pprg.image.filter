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
		
		// Applies all different filters on image and create separate output file for each filter output
		applyAllDifferentFiltersOnImage(img);		

		// Compares serial and parallel execution with smoothing filter mask of different sizes
		compareSerialAndParallelExecutionWithSmoothingFilter(img);

		// Performs parallel execution with different chunk sizes (pixels per task) to check if there is a difference
		performParallelExecutionWithDifferentTaskSizes(img);
	}

	// Applies all different filters in parallel way and creates output file for each filter output
	public static void applyAllDifferentFiltersOnImage(BufferedImage img) throws IOException {
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
		applyFilter(img, output, FilterGenerator.unsharpFilter(7));
		t.end("Apply filter: sharpening");

		t.start();
		ImageIO.write(output, "png", new File("./images/output_sharpening.png"));
		t.end("Write image");
	}
	
	// Apply given filter on image in parallel
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
						// loop over pixels for mask							
						int i = 0, j = 0;
						double alpha = 0, red = 0, green = 0, blue = 0;
						for (int z = -offset; z <= offset; z++) {
							for (int a = -offset; a <= offset; a++) {
								int color = image.getRGB(x + z, y + a);
								alpha = getAlpha(color);
								red += getRed(color) * filter[i][j];
								green += getGreen(color) * filter[i][j];
								blue += getBlue(color) * filter[i][j];
								j++;
							}
							i++;
							j = 0;
						}
						
						// set new value
						output.setRGB(x, y, getColor((int) Math.round(alpha), (int) Math.round(red), (int) Math.round(green), (int) Math.round(blue)));
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
	
	// Apply given filter on image in serial
	private static void applyFilterSerial(BufferedImage image, BufferedImage output, double[][] filter) {
		int offset = (filter.length - 1) / 2;

		for (int x = offset; x < image.getWidth() - offset; x += 1) {
			for (int y = offset; y < image.getHeight() - offset; y++) {
				// loop over pixels for mask							
				int i = 0, j = 0;
				double alpha = 0, red = 0, green = 0, blue = 0;
				for (int z = -offset; z <= offset; z++) {
					for (int a = -offset; a <= offset; a++) {
						int color = image.getRGB(x + z, y + a);
						alpha = getAlpha(color);
						red += getRed(color) * filter[i][j];
						green += getGreen(color) * filter[i][j];
						blue += getBlue(color) * filter[i][j];
						j++;
					}
					i++;
					j = 0;
				}
				
				// set new value
				output.setRGB(x, y, getColor((int) Math.round(alpha), (int) Math.round(red), (int) Math.round(green), (int) Math.round(blue)));
			}
		}
	}
	
	// Calculate new pixel value according to filter mask
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

	// Only for debugging purposes.
	private static void traceColor(BufferedImage image, int x, int y) {
		int color = image.getRGB(x, y);
		System.out.printf("pixel (%4d,%4d) - alpha: %3d, red: %3d, green: %3d, blue: %3d\n", x, y, getAlpha(color),
				getRed(color), getGreen(color), getBlue(color));
	}

	/* Not used anymore. 
	It was for testing a version where filter was applied to an array with copied rgb values of image. 
	But this approach was slower than directly accessing rgb values in image. 
	(And as far as we could find out Java's BufferedImage class should be thread safe.)
	*/
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

	/* Not used anymore. 
	It was for testing a version where filter was applied to an array with copied rgb values of image. 
	But this approach was slower than directly accessing rgb values in image. 
	(And as far as we could find out Java's BufferedImage class should be thread safe.)
	*/
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

	// Applies filter in parallel with tasks of given size
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
	
	// Compares serial and parallel execution with smoothing filter mask of different sizes
	public static void compareSerialAndParallelExecutionWithSmoothingFilter(BufferedImage img) throws IOException {
		BufferedImage output;
		
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
	
	// Performs parallel execution with different chunk sizes (pixels per task) to check if there is a difference
	public static void performParallelExecutionWithDifferentTaskSizes(BufferedImage img) throws IOException {
		BufferedImage output;	
		int filterSize = 15;		
		
		int[] taskSizes = new int[]{1, 4, 10, 50, 100, 250, 500, 750};
		
		for(int i = 0; i < taskSizes.length; i++){
			int taskSize = taskSizes[i];
			
			t.start();
			output = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
			applyFilterWithThreshold(img, output, FilterGenerator.smoothingFilter(filterSize), taskSize);
			t.end("Parallel: Task size = " + taskSize);
	
			t.start();
			ImageIO.write(output, "png", new File("./images/output_smoothing_tasksize_" + taskSize + ".png"));
			t.end("Write image");
		}
	}

}
