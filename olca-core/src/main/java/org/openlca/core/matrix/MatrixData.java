package org.openlca.core.matrix;

import org.openlca.core.matrix.format.IMatrix;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

/**
 * Contains the matrices of that are input of a calculation.
 */
public class MatrixData {

	/**
	 * The matrix index of the product and waste flows of the technosphere (i.e.
	 * the row and column index of the technology matrix; the column index of
	 * the intervention matrix).
	 */
	public TechIndex techIndex;

	/**
	 * The matrix index of the environmental/elementary flows (i.e. the row
	 * index of the intervention matrix; the column index of the impact matrix).
	 */
	public FlowIndex enviIndex;

	/**
	 * The matrix index of the LCIA categories (i.e. the row index of the impact
	 * matrix).
	 */
	public DIndex<ImpactCategoryDescriptor> impactIndex;

	/**
	 * The technology matrix.
	 */
	public IMatrix techMatrix;

	/**
	 * The intervention matrix.
	 */
	public IMatrix enviMatrix;

	/**
	 * The matrix with the characterization factors: LCIA categories *
	 * elementary flows.
	 */
	public IMatrix impactMatrix;

	/**
	 * A cost vector contains the unscaled net-costs for a set of
	 * process-products. Unscaled means that these net-costs are related to the
	 * (allocated) product amount in the respective process. The vector is then
	 * scaled with the respective scaling factors in the result calculation.
	 */
	public double[] costVector;
}