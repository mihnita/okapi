package net.sf.okapi.common.integration;

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DifferenceEvaluator;

class IgnorePrologXmlEvaluator implements DifferenceEvaluator {
	@Override
	public ComparisonResult evaluate(final Comparison c,
			final ComparisonResult r) {
		if (r == ComparisonResult.DIFFERENT) {
			if (c.getType() == ComparisonType.XML_STANDALONE || c.getType() == ComparisonType.XML_ENCODING) {
				return ComparisonResult.EQUAL;
			}
		}
		return r;
	}
}