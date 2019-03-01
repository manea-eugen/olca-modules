package org.openlca.core.math;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.matrix.solvers.IMatrixSolver;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.SimpleResult;
import org.openlca.util.TopoSort;

/**
 * When running the Monte Carlo simulation on a product system $s_r$ that has a
 * sub-system (which again can have sub-systems etc.) we need to first run the
 * number generation and calculation for that sub-system and integrate these
 * results into the matrices of $s_r$ in each iteration step of the simulation.
 * In general, we have to do this for each relation $s_i \prec s_j$, where $s_i$
 * is a sub-system of $s_j$, of all product systems $S$ of the recursively
 * expanded sub-system dependencies.
 *
 * $S$ is a [strict partial ordered
 * set](https://en.wikipedia.org/wiki/Partially_ordered_set#Strict_and_non-strict_partial_orders)
 * as we do not allow cycles in the sub-system dependencies. Thus, we can define
 * a linear order of all systems via [topological
 * sorting](https://en.wikipedia.org/wiki/Topological_sorting) which maps each
 * product system $s_i$ to a position $pos_i$ with $pos_i < pos_j$ when $s_i
 * \prec s_j$.
 *
 * In the simulation, we then run the number generation and calculation for each
 * sub-system starting from the lowest position in $pos = [1 \dots n]$ where the
 * top-most product system $s_r$ has the position $pos_r = n$. Thus, in a
 * simulation step a product system $s_j$ can access and integrate the result of
 * a sub-system $s_i$ when $s_i \prec s_j$. With this, the number generation and
 * calculation have to be done only once for each simulation step for each
 * product system $s_i \in S$.
 */
class SimulationGraph {

	/**
	 * A node contains the data for the simulation of a single product (sub-)
	 * system.
	 */
	private static class Node {
		long systemID;
		MatrixData data;
		ParameterTable parameters;
		SimpleResult lastResult;
	}

	private final IMatrixSolver solver;

	/**
	 * The node of the host-system. This is the node that privides the final
	 * data of the Monte-Carlo simulation.
	 */
	private Node root;

	/**
	 * The topological ordered sub-systems (this is empty when the host-system
	 * does not contain sub-systems). The matrix data of the sub-systems do not
	 * contain LCIA data as we only need the LCI (and LCC) results of them. The
	 * same calculation properties of the host-system (allocation method etc.)
	 * are shared with these sub-systems. In a simulation run we can just
	 * calculate the results of the sub-system from $0...n$. When a system $j$
	 * depends on a sub-system $i$ the topological order assures that it was
	 * already calculated before.
	 */
	private final List<Node> nodes = new ArrayList<>();

	/**
	 * Maps the ID of a product system to the respective node in the simulation
	 * graph.
	 */
	private final Map<Long, Node> nodeIndex = new HashMap<>();

	private SimulationGraph(IMatrixSolver solver) {
		this.solver = solver;
	}

	public static SimulationGraph build(
			MatrixCache mcache,
			CalculationSetup setup,
			IMatrixSolver solver) {

		// initialize the root node of the host system
		SimulationGraph g = new SimulationGraph(solver);
		g.root = g.makeNode(setup, mcache);
		g.nodeIndex.put(g.root.systemID, g.root);

		// collect and create the sub-system nodes
		List<LongPair> subLinks = new ArrayList<>();
		Queue<Node> q = new ArrayDeque<>();
		q.add(g.root);
		while (!q.isEmpty()) {
			Node n = q.poll();
			for (Long subSystem : g.getSubSystems(n)) {
				subLinks.add(LongPair.of(subSystem, n.systemID));
				if (g.nodeIndex.containsKey(subSystem))
					continue;

				// create node for LCI and LCC data simulation
				ProductSystemDao dao = new ProductSystemDao(
						mcache.getDatabase());
				ProductSystem sub = dao.getForId(subSystem);
				CalculationSetup subSetup = new CalculationSetup(
						CalculationType.SIMPLE_CALCULATION, sub);
				subSetup.parameterRedefs.addAll(sub.parameterRedefs);
				subSetup.withCosts = setup.withCosts;
				subSetup.allocationMethod = setup.allocationMethod;
				Node subNode = g.makeNode(subSetup, mcache);
				g.nodeIndex.put(subNode.systemID, subNode);
				q.add(subNode);
			}
		}

		// create the topological order
		List<Long> order = TopoSort.of(subLinks);
		if (order == null)
			throw new RuntimeException(
					"there are sub-system cycles in the product system");
		for (Long id : order) {
			if (id == g.root.systemID)
				continue;
			g.nodes.add(g.nodeIndex.get(id));
		}
		return g;
	}

	private Node makeNode(CalculationSetup setup, MatrixCache mcache) {
		Node n = new Node();
		n.systemID = setup.productSystem.id;
		n.data = DataStructures.matrixData(
				setup, solver, mcache, Collections.emptyMap());
		HashSet<Long> paramContexts = new HashSet<>();
		n.data.techIndex.each((i, p) -> {
			if (p.process != null
					&& p.process.type == ModelType.PROCESS) {
				paramContexts.add(p.id());
			}
		});
		if (setup.impactMethod != null) {
			paramContexts.add(setup.impactMethod.id);
		}
		n.parameters = ParameterTable.forSimulation(
				mcache.getDatabase(), paramContexts, setup.parameterRedefs);
		return n;
	}

	private Set<Long> getSubSystems(Node node) {
		if (node == null)
			return Collections.emptySet();
		Set<Long> subSystems = new HashSet<>();
		node.data.techIndex.each((i, p) -> {
			if (p.process != null
					&& p.process.type == ModelType.PRODUCT_SYSTEM) {
				subSystems.add(p.id());
			}
		});
		return subSystems;
	}

}
