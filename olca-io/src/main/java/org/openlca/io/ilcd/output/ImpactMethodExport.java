package org.openlca.io.ilcd.output;

import java.util.Map;

import javax.xml.namespace.QName;

import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Unit;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.commons.Ref;
import org.openlca.ilcd.methods.DataSetInfo;
import org.openlca.ilcd.methods.Factor;
import org.openlca.ilcd.methods.FactorList;
import org.openlca.ilcd.methods.LCIAMethod;
import org.openlca.ilcd.methods.MethodInfo;

public class ImpactMethodExport {

	private final ExportConfig config;

	public ImpactMethodExport(ExportConfig config) {
		this.config = config;
	}

	public void run(ImpactMethod method) {
		if (method == null)
			return;
		if (config.store.contains(LCIAMethod.class, method.refId))
			return;
		for (ImpactCategory impact : method.impactCategories) {
			LCIAMethod lciaMethod = new LCIAMethod();
			putAttribute("olca_method_uuid", method.refId,
					lciaMethod.otherAttributes);
			addMethodInfo(method, impact, lciaMethod);
			addFactors(impact, lciaMethod);
			config.store.put(lciaMethod);
		}
	}

	private void addMethodInfo(ImpactMethod method, ImpactCategory impact,
			LCIAMethod lciaMethod) {
		MethodInfo info = new MethodInfo();
		lciaMethod.methodInfo = info;
		DataSetInfo dataSetInfo = new DataSetInfo();
		info.dataSetInfo = dataSetInfo;
		dataSetInfo.uuid = impact.refId;
		dataSetInfo.methods.add(method.name);
		dataSetInfo.impactCategories.add(impact.name);
		putAttribute("olca_category_unit", impact.referenceUnit,
				dataSetInfo.otherAttributes);
		if (impact.description != null)
			LangString.set(dataSetInfo.comment,
					impact.description, config.lang);
	}

	private void putAttribute(String name, String value,
			Map<QName, String> map) {
		if (name == null || value == null || map == null)
			return;
		QName qName = new QName("http://openlca.org/ilcd-extensions", name);
		map.put(qName, value);
	}

	private void addFactors(ImpactCategory impact, LCIAMethod lciaMethod) {
		FactorList list = new FactorList();
		lciaMethod.characterisationFactors = list;
		for (ImpactFactor olcaFactor : impact.impactFactors) {
			Factor ilcdFactor = new Factor();
			list.factors.add(ilcdFactor);
			// TODO: uncertainty values + formulas
			ilcdFactor.meanValue = getRefAmount(olcaFactor);
			Ref ref = ExportDispatch.forwardExport(
					olcaFactor.flow, config);
			ilcdFactor.flow = ref;
		}
	}

	private double getRefAmount(ImpactFactor factor) {
		double val = factor.value;
		Unit unit = factor.unit;
		if (unit != null)
			val = val / unit.conversionFactor;
		FlowPropertyFactor propFactor = factor.flowPropertyFactor;
		if (propFactor != null)
			val = val * propFactor.conversionFactor;
		return val;
	}
}
