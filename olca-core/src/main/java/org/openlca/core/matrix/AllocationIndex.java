package org.openlca.core.matrix;

import java.util.HashMap;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.AllocationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * An instances of this class provides fast access to the allocation factors
 * related to the products of a product system. It maps a product $p$ and an
 * exchange of flow $i$ to the respective allocation factor $\lambda_{p,i}$:
 * 
 * $$Idx_{Alloc}: (p, i) \mapsto \lambda_{p,i}$$
 * 
 */
public class AllocationIndex {

	/**
	 * Used for physical and economic allocation: directly stores the allocation
	 * factors for the given process-products.
	 */
	private HashMap<ProcessProduct, Double> factors;

	/**
	 * Used for causal allocation: stores the relation process-product ->
	 * exchange -> allocation factor.
	 */
	private HashMap<ProcessProduct, TLongDoubleHashMap> causalFactors;

	/**
	 * Creates a new allocation index for the given database, product index, and
	 * allocation method.
	 */
	public static AllocationIndex create(
			IDatabase db, TechIndex techIndex, AllocationMethod method) {
		AllocationIndex idx = new AllocationIndex();
		if (method == null || method == AllocationMethod.NONE)
			return idx;
		try {
			idx.build(db, techIndex, method);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(AllocationIndex.class);
			log.error("Failed to load allocation index", e);
		}
		return idx;
	}

	private AllocationIndex() {
	}

	/**
	 * Returns the allocation factor $\lambda_{p,i}$ for the given product $p$
	 * and (ID of the exchange with the) flow $i$. **It is very important** that
	 * this method is only called with exchanges that can be allocated to a
	 * product output or waste input, which are: product inputs, waste outputs,
	 * or elementary flows.
	 */
	public double get(ProcessProduct product, long exchangeID) {
		if (product == null)
			return 1.0;
		if (factors != null) {
			Double f = factors.get(product);
			if (f != null)
				return f;
		}
		if (causalFactors == null)
			return 1.0;
		TLongDoubleHashMap m = causalFactors.get(product);
		if (m == null)
			return 1.0;
		return m.get(exchangeID);
	}

	private void build(IDatabase db, TechIndex techIndex,
			AllocationMethod method) throws Exception {

		// load process specific default allocation methods if required
		final TLongObjectHashMap<AllocationMethod> defMethods;
		defMethods = method == AllocationMethod.USE_DEFAULT
				? loadDefaultMethods(db)
				: null;

		String sql = "SELECT allocation_type, f_process, f_product, value, "
				+ "f_exchange FROM tbl_allocation_factors";
		NativeSql.on(db).query(sql, r -> {
			long processID = r.getLong(2);

			// check if the allocation method matches
			AllocationMethod _method = method;
			if (defMethods != null) {
				_method = defMethods.get(processID);
			}
			String m = r.getString(1);
			if (m == null || AllocationMethod.valueOf(m) != _method)
				return true;

			// get the related product
			long flowID = r.getLong(3);
			ProcessProduct product = techIndex.getProvider(processID, flowID);
			if (product == null)
				return true;

			// index the factor
			double factor = r.getDouble(4);
			if (_method != AllocationMethod.CAUSAL) {
				if (factors == null) {
					factors = new HashMap<>();
				}
				factors.put(product, factor);
				return true;
			}

			// causal allocation factors
			long exchangeID = r.getLong(5);
			if (causalFactors == null) {
				causalFactors = new HashMap<>();
			}
			TLongDoubleHashMap map = causalFactors.get(product);
			if (map == null) {
				// 1.0 is the default value -> means no allocation
				map = new TLongDoubleHashMap(
						Constants.DEFAULT_CAPACITY,
						Constants.DEFAULT_LOAD_FACTOR,
						Constants.DEFAULT_LONG_NO_ENTRY_VALUE,
						1d);
				causalFactors.put(product, map);
			}
			map.put(exchangeID, factor);
			return true;
		});
	}

	private TLongObjectHashMap<AllocationMethod> loadDefaultMethods(
			IDatabase db) throws Exception {
		TLongObjectHashMap<AllocationMethod> m = new TLongObjectHashMap<>();
		String sql = "select id, default_allocation_method from tbl_processes";
		NativeSql.on(db).query(sql, r -> {
			long id = r.getLong(1);
			String method = r.getString(2);
			if (method != null) {
				m.put(id, AllocationMethod.valueOf(method));
			}
			return true;
		});
		return m;
	}

}
