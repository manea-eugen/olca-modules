package org.openlca.core.model.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.model.Indexable;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessGroup;
import org.openlca.core.model.ProcessGroupSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A group of processes for result analysis. If this group is tagged as rest it
 * contains all processes that are not assigned to other groups.
 */
public class ProcessGrouping implements Indexable {

	private String name;
	private List<Process> processes = new ArrayList<>();
	private boolean rest;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Process> getProcesses() {
		return processes;
	}

	public void setRest(boolean rest) {
		this.rest = rest;
	}

	public boolean isRest() {
		return rest;
	}

	@Override
	public String getId() {
		return name != null ? name : "no name";
	}

	@Override
	public int hashCode() {
		String id = getId();
		if (id != null)
			return id.hashCode();
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof ProcessGrouping))
			return false;
		ProcessGrouping other = (ProcessGrouping) obj;
		String id = getId();
		String otherId = other.getId();
		if (id == null && otherId == null)
			return true;
		if (id == null || otherId == null)
			return false;
		return id.equals(otherId);
	}

	/**
	 * Applies the given group set on the given processes. If there are
	 * processes not assignable to a group of the group set a group with these
	 * processes is created using the given parameter restName.
	 */
	public static List<ProcessGrouping> applyOn(List<Process> processes,
			ProcessGroupSet groupSet, String restName) {
		if (processes == null)
			return Collections.emptyList();
		List<ProcessGroup> groups = getGroups(groupSet);
		List<ProcessGrouping> groupings = new ArrayList<>();
		List<Process> rest = new ArrayList<>(processes);
		for (ProcessGroup group : groups) {
			ProcessGrouping grouping = new ProcessGrouping();
			grouping.setName(group.getName());
			grouping.setRest(false);
			List<Process> matches = split(group.getProcessIds(), rest);
			grouping.getProcesses().addAll(matches);
			groupings.add(grouping);
		}
		if (!rest.isEmpty()) {
			ProcessGrouping grouping = new ProcessGrouping();
			grouping.setName(restName);
			grouping.setRest(true);
			grouping.getProcesses().addAll(rest);
			groupings.add(grouping);
		}
		return groupings;
	}

	private static List<ProcessGroup> getGroups(ProcessGroupSet groupSet) {
		try {
			if (groupSet == null)
				return Collections.emptyList();
			return groupSet.getGroups();
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(ProcessGrouping.class);
			log.error("Failed to read groups from set", e);
			return Collections.emptyList();
		}
	}

	private static List<Process> split(List<String> processIds,
			List<Process> processes) {
		List<Process> matches = new ArrayList<>();
		for (String id : processIds) {
			for (Process p : processes) {
				if (p.getId() != null && p.getId().equals(id))
					matches.add(p);
			}
		}
		processes.removeAll(matches);
		return matches;
	}

}
