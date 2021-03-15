package net.sf.okapi.lib.xliff2.validation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RulesProcessingTest {
	
	@Test
	public void testIsPresent () {
		Unit unit = create();
		Validation val = unit.getValidation();
		val.add(new Rule("isPresent", "xyz"));
		val.prepare();
		assertEquals(0, val.processRules(unit, null).size());
	}
	
	@Test
	public void testIsPresentWrongCount () {
		Unit unit = create();
		Validation val = unit.getValidation();
		Rule rule = val.add(new Rule("isPresent", "xyz"));
		rule.setOccurs(2);
		rule.prepare();
		List<Issue> res = val.processRules(unit, null);
		assertEquals("isPresent-WrongCount", res.get(0).getCode());
	}		
		
	@Test
	public void testIsPresentNotInSource () {
		Unit unit = create();
		Validation val = unit.getValidation();
		Rule rule = val.add(new Rule("isPresent", "qwerty"));
		rule.setExistsInSource(true);
		rule.prepare();
		List<Issue> res = val.processRules(unit, null);
		assertEquals("isPresent-NotInSource", res.get(0).getCode());
	}		

	@Test
	public void testIsPresentNotInTarget () {
		Unit unit = create();
		Validation val = unit.getValidation();
		Rule rule = val.add(new Rule("isPresent", "def"));
		rule.setExistsInSource(true);
		rule.prepare();
		List<Issue> res = val.processRules(unit, null);
		assertEquals("isPresent-NotInTarget", res.get(0).getCode());
	}		
		
	@Test
	public void testStartsWithNotInTarget () {
		Unit unit = create();
		Validation val = unit.getValidation();
		Rule rule = val.add(new Rule("startsWith", "abc"));
		rule.prepare();
		List<Issue> res = val.processRules(unit, null);
		assertEquals("startsWith", res.get(0).getCode());
	}		
		
	private Unit create () {
		Unit unit = new Unit("u1");
		unit.appendSegment().getSource().append("abc def xyz");
		unit.getSegment(0).getTarget(GetTarget.CREATE_EMPTY).append("ABC iop xyz");
		return unit;
	}

	@Test
	public void testTargetOrder () {
		String snippet = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" trgLang=\"fr\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule startsWith=\"-\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n" // Target order means the '-' should be in the other target part
			+ "<source>- AAA-1. </source>\n"
			+ "<target order='2'>- AAA-2. </target>\n"
			+ "</segment>\n"
			+ "<segment>\n"
			+ "<source>BBB-2. </source>\n"
			+ "<target order='1'>BBB-1. </target>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		Unit unit = U.getUnit(snippet);
		List<Issue> issues = unit.getValidation().processRules(unit, null);
		assertEquals(1, issues.size());
		assertEquals("startsWith", issues.get(0).getCode());
	}

	@Test
	public void testFoundInIgnorable () {
		String snippet = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" trgLang=\"fr\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule isPresent=\"zzz\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n"
			+ "<source>before </source>\n"
			+ "<target>before-trg </target>\n"
			+ "</segment>\n"
			+ "<ignorable>\n"
			+ "<source>zzz </source>\n"
			+ "<target>zzz </target>\n"
			+ "</ignorable>\n"
			+ "<segment>\n"
			+ "<source>after </source>\n"
			+ "<target>after-trg </target>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		Unit unit = U.getUnit(snippet);
		List<Issue> issues = unit.getValidation().processRules(unit, null);
		assertEquals(0, issues.size()); // Pattern found in the ignorable part
	}

}