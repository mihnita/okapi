package net.sf.okapi.common.integration;

import org.w3c.dom.Attr;
import org.xmlunit.util.Predicate;

public class Xliff2AttributesToIgnore implements Predicate<Attr>, java.util.function.Predicate<Attr> {
	public boolean test(Attr attr) {
        return "order".equals(attr.getOwnerElement().getNodeName());
    }
}
