package pprg.image.filter;

public class FilterGenerator {

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
	
	public static double[][] nonChangingFilter() {
		return new double[][] { { 0d, 0d, 0d }
							  , { 0d, 1d, 0d }
							  , { 0d, 0d, 0d } };
	}

	public static double[][] detectEdgesFilter() {
		return new double[][] { { -1d,  0d, 0d,  0d,  0d }
							  , {  0d, -2d, 0d,  0d,  0d }
							  , {  0d,  0d, 6d,  0d,  0d }
							  ,	{  0d,  0d, 0d, -2d,  0d }
							  , {  0d,  0d, 0d,  0d, -1d } };
	}

	public static double[][] sharpeningFilter() {
		return new double[][] { { -1d,  -1d, -1d, -1d, -1d }
		  					  , { -1d,   2d,  2d,  2d, -1d }		  
		  					  , { -1d,   2d,  8d,  2d, -1d }		  
		  					  ,	{ -1d,   2d,  2d,  2d, -1d }		  
		  					  , { -1d,  -1d, -1d, -1d, -1d } };
	}
}
