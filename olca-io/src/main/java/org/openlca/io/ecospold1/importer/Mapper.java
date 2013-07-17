package org.openlca.io.ecospold1.importer;

import java.util.Date;

import org.openlca.core.model.Actor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.Source;
import org.openlca.ecospold.IDataGeneratorAndPublication;
import org.openlca.ecospold.IDataSetInformation;
import org.openlca.ecospold.IExchange;
import org.openlca.ecospold.IPerson;
import org.openlca.ecospold.IRepresentativeness;
import org.openlca.ecospold.ISource;
import org.openlca.ecospold.IValidation;
import org.openlca.ecospold.io.DataSet;

class Mapper {

	public static void mapPerson(IPerson inPerson, Actor ioActor) {
		ioActor.setName(inPerson.getName());
		ioActor.setAddress(inPerson.getAddress());
		if (inPerson.getCountryCode() != null)
			ioActor.setCountry(inPerson.getCountryCode().value());
		ioActor.setEmail(inPerson.getEmail());
		ioActor.setTelefax(inPerson.getTelefax());
		ioActor.setTelephone(inPerson.getTelephone());
	}

	public static void mapSource(ISource inSource, Source ioSource) {
		ioSource.setName(inSource.getFirstAuthor() + " " + inSource.getYear());
		ioSource.setDescription(inSource.getText());
		ioSource.setTextReference(inSource.getTitle());
		if (inSource.getYear() != null) {
			ioSource.setYear((short) inSource.getYear().getYear());
		}
	}

	public static FlowType getFlowType(IExchange inExchange) {
		if (inExchange.getInputGroup() != null) {
			return FlowTypes.forInputGroup(inExchange.getInputGroup());
		} else if (inExchange.getOutputGroup() != null) {
			return FlowTypes.forOutputGroup(inExchange.getOutputGroup());
		} else {
			return FlowType.ELEMENTARY_FLOW;
		}
	}

	public static ProcessType getProcessType(DataSet dataset) {
		if (dataset == null || dataset.getDataSetInformation() == null)
			return ProcessType.UNIT_PROCESS;
		IDataSetInformation info = dataset.getDataSetInformation();
		if (info.getType() == 2)
			return ProcessType.LCI_RESULT;
		else
			return ProcessType.UNIT_PROCESS;
	}

	public static void mapModellingAndValidation(DataSet dataSet,
			ProcessDocumentation doc) {
		String evaluation = null;
		IValidation validation = dataSet.getValidation();
		if (validation != null) {
			if (validation.getProofReadingDetails() != null) {
				evaluation = "Proof reading details: "
						+ validation.getProofReadingDetails();
			}
			if (validation.getOtherDetails() != null) {
				if (evaluation == null)
					evaluation = "";
				evaluation += "\nOther details: "
						+ validation.getOtherDetails();
			}
		}
		doc.setReviewDetails(evaluation);

		IRepresentativeness representativeness = dataSet
				.getRepresentativeness();
		if (representativeness != null
				&& representativeness.getSamplingProcedure() != null) {
			String sampling = representativeness.getSamplingProcedure();
			doc.setSampling(sampling);
		}
	}

	public static void mapAdminInfo(DataSet dataSet, ProcessDocumentation doc) {

		IDataGeneratorAndPublication generator = dataSet
				.getDataGeneratorAndPublication();
		if (generator != null) {
			doc.setCopyright(generator.isCopyright());
			String accessAndUseRestrictions = null;
			Integer restrictedTo = generator.getAccessRestrictedTo();
			if (restrictedTo != null) {
				accessAndUseRestrictions = "";
				switch (restrictedTo) {
				case 0:
					accessAndUseRestrictions = accessAndUseRestrictions
							.concat("All information can be accessed by everybody.");
					break;
				case 2:
					accessAndUseRestrictions = accessAndUseRestrictions
							.concat("Ecoinvent clients have access to LCI results "
									+ "but not to unit process raw data. Members of "
									+ "the ecoinvent quality network (ecoinvent centre) "
									+ "have access to all information.");
					break;
				case 3:
					accessAndUseRestrictions = accessAndUseRestrictions
							.concat("The ecoinvent administrator has full access to "
									+ "information. Via the web only LCI results are "
									+ "accessible (for ecoinvent clients and "
									+ "for members of the ecoinvent centre).");
					break;
				}
			}
			doc.setRestrictions(accessAndUseRestrictions);
		}

		// timestamp
		IDataSetInformation info = dataSet.getDataSetInformation();
		if (info != null && info.getTimestamp() != null) {
			Date lastChange = info.getTimestamp().toGregorianCalendar()
					.getTime();
			doc.setLastChange(lastChange);
			doc.setCreationDate(lastChange);
		}
	}

}
