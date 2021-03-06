package org.openlca.jsonld.output;

import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;

import com.google.gson.JsonObject;

class Documentation {

	static JsonObject create(Process process, ExportConfig conf) {
		ProcessDocumentation d = process.documentation;
		if (d == null)
			return null;
		JsonObject o = new JsonObject();
		Out.put(o, "@type", ProcessDocumentation.class.getSimpleName());
		mapSimpleDocFields(d, o);
		Out.put(o, "reviewer", d.reviewer, conf);
		Out.put(o, "dataDocumentor", d.dataDocumentor, conf);
		Out.put(o, "dataGenerator", d.dataGenerator, conf);
		Out.put(o, "dataSetOwner", d.dataSetOwner, conf);
		Out.put(o, "publication", d.publication, conf);
		Out.put(o, "sources", d.sources, conf);
		return o;
	}

	private static void mapSimpleDocFields(ProcessDocumentation d, JsonObject o) {
		Out.put(o, "timeDescription", d.time);
		Out.put(o, "technologyDescription", d.technology);
		Out.put(o, "dataCollectionDescription", d.dataCollectionPeriod);
		Out.put(o, "completenessDescription", d.completeness);
		Out.put(o, "dataSelectionDescription", d.dataSelection);
		Out.put(o, "reviewDetails", d.reviewDetails);
		Out.put(o, "dataTreatmentDescription", d.dataTreatment);
		Out.put(o, "inventoryMethodDescription", d.inventoryMethod);
		Out.put(o, "modelingConstantsDescription", d.modelingConstants);
		Out.put(o, "samplingDescription", d.sampling);
		Out.put(o, "restrictionsDescription", d.restrictions);
		Out.put(o, "copyright", d.copyright);
		Out.put(o, "validFrom", d.validFrom, Out.DATE_ONLY);
		Out.put(o, "validUntil", d.validUntil, Out.DATE_ONLY);
		Out.put(o, "creationDate", d.creationDate);
		Out.put(o, "intendedApplication", d.intendedApplication);
		Out.put(o, "projectDescription", d.project);
		Out.put(o, "geographyDescription", d.geography);
	}

}
