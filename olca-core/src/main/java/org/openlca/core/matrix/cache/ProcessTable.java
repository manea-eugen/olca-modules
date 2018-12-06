package org.openlca.core.matrix.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.Provider;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

/**
 * TODO: tests + doc
 */
public class ProcessTable {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final TLongObjectHashMap<ProcessDescriptor> processes = new TLongObjectHashMap<>();
	private final TLongObjectHashMap<FlowDescriptor> flows = new TLongObjectHashMap<>();

	/**
	 * Stores the default allocation methods of the processes: processID =>
	 * AllocationMethod.
	 */
	private final TLongObjectHashMap<AllocationMethod> allocMap = new TLongObjectHashMap<>();

	/**
	 * Maps IDs of product and waste flows to process IDs that have the
	 * respective product as output or waste as input: flow-id ->
	 * provider-process-id. We need this when we build a product system
	 * automatically.
	 */
	private final TLongObjectHashMap<TLongArrayList> flowProviders = new TLongObjectHashMap<>();

	public static ProcessTable create(IDatabase db) {
		ProcessTable table = new ProcessTable(db);
		return table;
	}

	private ProcessTable(IDatabase db) {
		log.trace("build process index table");
		initAllocation(db);
		initProviderMap(db);
	}

	private void initProviderMap(IDatabase db) {
		log.trace("load provider map");

		// index processes and tech-flows
		ProcessDao pDao = new ProcessDao(db);
		for (ProcessDescriptor d : pDao.getDescriptors()) {
			processes.put(d.getId(), d);
		}
		FlowDao fDao = new FlowDao(db);
		for (FlowDescriptor d : fDao.getDescriptors()) {
			if (d.getFlowType() == FlowType.ELEMENTARY_FLOW)
				continue;
			flows.put(d.getId(), d);
		}

		// index flow -> process relations
		String query = "select f_owner, f_flow, is_input from tbl_exchanges";
		try {
			NativeSql.on(db).query(query, r -> {
				long processId = r.getLong(1);
				long flowId = r.getLong(2);
				boolean isInput = r.getBoolean(3);
				FlowDescriptor flow = flows.get(flowId);
				if (flow == null)
					return true;
				FlowType t = flow.getFlowType();
				if ((isInput && t == FlowType.WASTE_FLOW)
						|| (!isInput && t == FlowType.PRODUCT_FLOW)) {
					TLongArrayList list = flowProviders.get(flowId);
					if (list == null) {
						list = new TLongArrayList();
						flowProviders.put(flowId, list);
					}
					list.add(processId);
				}
				return true;
			});
			log.trace("{} providers mapped", processes.size());
		} catch (Exception e) {
			log.error("failed to load process products", e);
		}
	}

	private void initAllocation(IDatabase db) {
		log.trace("index allocation types");
		String sql = "select id, default_allocation_method"
				+ " from tbl_processes";
		try {
			NativeSql.on(db).query(sql, r -> {
				long id = r.getLong(1);
				String m = r.getString(2);
				if (m != null) {
					allocMap.put(
							id, AllocationMethod.valueOf(m));
				}
				return true;
			});
			log.trace("{} processes indexed", allocMap.size());

		} catch (Exception e) {
			log.error("failed to build process type index", e);
		}
	}

	/** Returns the process type for the given process-ID. */
	public ProcessType getType(long processId) {
		ProcessDescriptor d = processes.get(processId);
		return d == null ? null : d.getProcessType();
	}

	/** Note that this method can return <code>null</code> */
	public AllocationMethod getDefaultAllocationMethod(long processId) {
		return allocMap.get(processId);
	}

	public Provider getProvider(long id, long flowId) {
		ProcessDescriptor process = processes.get(id);
		FlowDescriptor flow = flows.get(flowId);
		if (flow == null || process == null)
			return null;
		return Provider.of(process, flow);
	}

	/**
	 * Returns the list of providers that have the flow with the given ID as
	 * product output or waste input.
	 */
	public List<Provider> getProviders(long flowId) {
		TLongArrayList list = flowProviders.get(flowId);
		if (list == null)
			return Collections.emptyList();
		FlowDescriptor flow = flows.get(flowId);
		if (flow == null)
			return Collections.emptyList();
		ArrayList<Provider> providers = new ArrayList<>();
		list.forEach(id -> {
			ProcessDescriptor d = processes.get(id);
			if (d != null) {
				providers.add(Provider.of(d, flow));
			}
			return true;
		});

		return providers;
	}

	/** Get all product or waste treatment providers from the database. */
	public List<Provider> getProviders() {
		List<Provider> list = new ArrayList<>();
		TLongObjectIterator<TLongArrayList> it = flowProviders.iterator();
		while (it.hasNext()) {
			it.advance();
			long flowId = it.key();
			for (long providerId : it.value().toArray()) {
				Provider p = getProvider(providerId, flowId);
				if (p != null) {
					list.add(p);
				}
			}
		}
		return list;
	}
}
