package net.sf.okapi.common.pipeline.integration;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.html.HtmlFilter;

public final class PipelineTestUtil
{
	public static String getFirstTUSource (RawDocument rd) {
		try (IFilter filter = new HtmlFilter()) {
			filter.open(rd);
			Event event;
			while ( filter.hasNext() ) {
				event = filter.next();
				if ( event.getEventType() == EventType.TEXT_UNIT ) {
					ITextUnit tu = event.getTextUnit();
					return tu.getSource().toString();
				}
			}
		}
		return null;
	}
}
