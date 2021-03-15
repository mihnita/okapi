package net.sf.okapi.common.integration;

import org.w3c.dom.Attr;
import org.xmlunit.util.Predicate;

public class XmlStandardAttributesToIgnore implements Predicate<Attr>, java.util.function.Predicate<Attr> {
	@Override
	public boolean test(final Attr attr) {
		return "xml:lang".equals(attr.getOwnerElement().getNodeName());
	}
}
