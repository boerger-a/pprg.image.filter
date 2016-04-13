package pprg.image.filter;

public class FilterGenerator {

	// Smoothing/Blurring filter mask
	public static double[][] smoothingFilter(int parameter) {
		if (parameter % 2 != 1) {
			throw new IllegalArgumentException("Filter has to be odd size");
		}
		double[][] filter = new double[parameter][parameter];
		for (int i = 0; i < filter.length; i++) {
			for (int j = 0; j < filter[0].length; j++) {
				filter[i][j] = 1d / (parameter * parameter);
			}
		}
		return filter;
	}
	
	// Filter doesn't change image - for testing purposes
	public static double[][] nonChangingFilter() {
		return new double[][] { { 0d, 0d, 0d }
							  , { 0d, 1d, 0d }
							  , { 0d, 0d, 0d } };
	}

	// Laplacian of Gaussian filter mask
	public static double[][] detectEdgesLoGFilter() {
		return new double[][] { { 0.0448, 0.0468,  0.0564, 0.0468, 0.0448 }
		  					  , { 0.0468, 0.3167,  0.7146, 0.3167, 0.0468 }
		  					  , { 0.0564, 0.7146, -4.9048, 0.7146, 0.0564 }
		  					  , { 0.0468, 0.3167,  0.7146, 0.3167, 0.0468 }
		  					  , { 0.0448, 0.0468,  0.0564, 0.0468, 0.0448 } };
	}

	// Sobel filter mask for vertical edges
	public static double[][] detectVerticalEdgesSobelFilter() {
		return new double[][] { {  1d,  2d,  1d }
		  					  , {  0d,  0d,  0d }		  
		  					  , { -1d, -2d, -1d } };
	}

	// Sobel filter mask for horizontal edges
	public static double[][] detectHorizontalEdgesSobelFilter() {
		return new double[][] { { 1d,  0d, -1d }
		  					  , { 2d,  0d, -2d }		  
		  					  , { 1d,  0d, -1d } };
	}

	// Unsharp filter mask
	public static double[][] unsharpFilter(int parameter) {
		if (parameter % 2 != 1) {
			throw new IllegalArgumentException("Filter has to be odd size");
		}
		double[][] filter = new double[parameter][parameter];
		for (int i = 0; i < filter.length; i++) {
			for (int j = 0; j < filter[0].length; j++) {
				if(i == (parameter-1)/2 && j == (parameter-1)/2) {
					filter[i][j] =  2d - (1d / (parameter * parameter));
				} else {
					filter[i][j] =  0d - (1d / (parameter * parameter));
				}
			}
		}
		return filter;
	}
}
