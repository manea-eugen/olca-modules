package org.openlca.core.math;

import java.util.Collections;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openlca.core.Tests;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.expressions.Scope;

public class FormulaInterpretersTest {

	private IDatabase database = Tests.getDb();
	private Parameter globalParam;
	private Process process;
	private FormulaInterpreter interpreter;

	/**
	 * sets the following parameters:
	 * 
	 * fi_tests_global = 32
	 * 
	 * fi_tests_local = fi_tests_global + 10
	 */
	@Before
	public void setUp() throws Exception {
		globalParam = new Parameter();
		globalParam.name = "fi_tests_global";
		globalParam.isInputParameter = true;
		globalParam.scope = ParameterScope.GLOBAL;
		globalParam.value = (double) 32;
		new ParameterDao(database).insert(globalParam);
		process = new Process();
		Parameter localParam = new Parameter();
		localParam.name = "fi_tests_local";
		localParam.formula = "fi_tests_global + 10";
		localParam.isInputParameter = false;
		localParam.scope = ParameterScope.PROCESS;
		process.parameters.add(localParam);
		process = new ProcessDao(database).insert(process);
		Set<Long> context = Collections.singleton(process.id);
		interpreter = ParameterTable.interpreter(database, context,
				Collections.emptySet());
	}

	@After
	public void tearDown() throws Exception {
		new ParameterDao(database).delete(globalParam);
		new ProcessDao(database).delete(process);
	}

	@Test
	public void testEvalLocal() throws Exception {
		Scope scope = interpreter.getScope(process.id);
		Assert.assertEquals(42, scope.eval("fi_tests_local"), 1e-16);
	}

	@Test
	public void testEvalGlobal() throws Exception {
		Assert.assertEquals(32, interpreter.eval("fi_tests_global"), 1e-16);
	}

	@Test(expected = InterpreterException.class)
	public void testEvalLocalInGlobalFails() throws Exception {
		Assert.assertEquals(42, interpreter.eval("fi_tests_local"), 1e-16);
	}

}
