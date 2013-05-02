package org.openlca.ilcd.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.bind.JAXB;

import org.junit.Test;
import org.openlca.ilcd.processes.Exchange;
import org.openlca.ilcd.util.ExchangeExtension;

public class ExchangeExtensionTest {

	@Test
	public void testEmpty() {
		Exchange raw = new Exchange();
		raw.setDataSetInternalID(new BigInteger("1"));
		Exchange exchange = io(raw);
		assertTrue(exchange != raw);
		ExchangeExtension extension = new ExchangeExtension(exchange);
		assertNull(extension.getAmount());
		assertNull(extension.getFormula());
		assertNull(extension.getPropertyId());
		assertNull(extension.getUnitId());
		assertFalse(extension.isValid());
	}

	@Test
	public void testFormula() {
		assertEquals("2 * Pi * sqr(r)", extension().getFormula());
	}

	@Test
	public void testUnitId() throws Exception {
		assertEquals("unit-id", extension().getUnitId());
	}

	@Test
	public void testPropertyId() throws Exception {
		assertEquals("property-id", extension().getPropertyId());
	}

	@Test
	public void testAmount() throws Exception {
		assertEquals(42d, extension().getAmount().doubleValue(), 1e-15);
	}

	@Test
	public void testAvoidedProduct() {
		assertTrue(extension().isAvoidedProduct());
	}

	@Test
	public void testDefaultProvider() {
		assertEquals("abc-def", extension().getDefaultProvider());
	}

	private ExchangeExtension extension() {
		Exchange exchange = io(createExchange());
		return new ExchangeExtension(exchange);
	}

	private Exchange createExchange() {
		Exchange exchange = new Exchange();
		exchange.setDataSetInternalID(new BigInteger("1"));
		exchange.setMeanAmount(500);
		ExchangeExtension extension = new ExchangeExtension(exchange);
		extension.setAmount(42);
		extension.setFormula("2 * Pi * sqr(r)");
		extension.setPropertyId("property-id");
		extension.setUnitId("unit-id");
		extension.setDefaultProvider("abc-def");
		extension.setAvoidedProduct(true);
		return exchange;
	}

	private Exchange io(Exchange exchange) {
		StringWriter writer = new StringWriter();
		JAXB.marshal(exchange, writer);
		writer.flush();
		String xml = writer.toString();
		StringReader reader = new StringReader(xml);
		return JAXB.unmarshal(reader, Exchange.class);
	}

}