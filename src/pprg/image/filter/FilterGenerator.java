package pprg.image.filter;

public class FilterGenerator {

	public static double[][] smoothingFilter(int parameter) {
		double[][] filter = new double[parameter][parameter];
		for (int i = 0; i < filter.length; i++) {
			for (int j = 0; j < filter[0].length; j++) {
				filter[i][j] = 1d / (parameter * parameter);
			}
		}
		return filter;
	}

	public static double[][] nonChangingFilter() {
		return new double[][] { { 0d, 0d, 0d }, { 0d, 1d, 0d }, { 0d, 0d, 0d } };
	}
}
