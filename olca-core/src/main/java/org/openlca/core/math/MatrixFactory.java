package org.openlca.core.math;

import org.openlca.jblas.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatrixFactory {

	/** Prefer double precision values in the matrix. */
	public static int PREFER_DOUBLES = 1;

	/** Prefer single precision values in the matrix. */
	public static int PREFER_FLOATS = 2;

	private static int flags = 0;

	private static Logger log = LoggerFactory.getLogger(MatrixFactory.class);

	private MatrixFactory() {
	}

	/** Allows bit-wise combination of constants defined in this class. */
	public static void configure(int flags) {
		MatrixFactory.flags = flags;
	}

	public static IMatrix create(int rowSize, int colSize) {
		if (Library.isLoaded()) {
			if ((flags & PREFER_FLOATS) == PREFER_FLOATS) {
				log.trace("create blas-matrix (single) {} x {}", rowSize,
						colSize);
				return new BlasFloatMatrix(rowSize, colSize);
			}
			log.trace("create blas-matrix {} x {}", rowSize, colSize);
			return new BlasMatrix(rowSize, colSize);
		}
		log.trace("create java-matrix {} x {}", rowSize, colSize);
		return new JavaMatrix(rowSize, colSize);
	}

}
