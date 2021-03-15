package net.sf.okapi.filters.autoxliff;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.codehaus.stax2.XMLInputFactory2;

import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.lib.xliff2.Const;

/**
 * A meta-filter that detects the version of an XLIFF file and then hands
 * parsing off to the appropriate filter.
 */
@UsingParameters(AutoXLIFFParameters.class)
public class AutoXLIFFFilter implements IFilter {

	private IFilter delegate = null;
	private IFilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
	private AutoXLIFFParameters params = new AutoXLIFFParameters();

	public AutoXLIFFFilter() {
		fcMapper.addConfigurations("net.sf.okapi.filters.xliff.XLIFFFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.xliff2.XLIFF2Filter");
	}

	@Override
	public void open(RawDocument input) {
		open(input, true);
	}

	@Override
	public void open(RawDocument input, boolean generateSkeleton) {
		XMLInputFactory fact = XMLInputFactory.newInstance();
		fact.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		fact.setProperty(XMLInputFactory2.P_REPORT_CDATA, Boolean.TRUE);
		fact.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		// Determine encoding based on BOM, if any
		input.setEncoding("UTF-8"); // Default for XML, other should be auto-detected
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(input.getStream(), input.getEncoding());
		detector.detectBom();

		String inStreamCharset = "UTF-8";
		if ( detector.isAutodetected() ) {
			inStreamCharset = detector.getEncoding();
		}

		boolean isXLIFF2 = false;
		XMLEventReader xmlEventReader = null;
		try (InputStreamReader r = new InputStreamReader(input.getStream(), inStreamCharset)) {
			xmlEventReader = fact.createXMLEventReader(r);
			isXLIFF2 = detectXLIFF2(xmlEventReader);
		}
		catch ( IOException | XMLStreamException e) {
			throw new OkapiIOException("Cannot open XML document.\n"+e.getMessage(), e);
		}
		finally {
			try {
				if (xmlEventReader != null) {
					xmlEventReader.close();
				}
			}
			catch (XMLStreamException e) { }
		}
		delegate = isXLIFF2 ? fcMapper.createFilter(params.getXLIFF20Config()) :
				fcMapper.createFilter(params.getXLIFF12Config());
		delegate.setFilterConfigurationMapper(fcMapper);
		delegate.open(input, generateSkeleton);
	}

	@Override
	public boolean hasNext() {
		if (delegate != null) {
			return delegate.hasNext();
		}
		return false;
	}

	@Override
	public Event next() {
		if (delegate != null) {
			Event e = delegate.next();
			if (e.isStartDocument()) {
				StartDocument sd = e.getStartDocument();
				sd.setFilterId(getName());
				sd.setFilterParameters(getParameters());
			}
			return e;
		}
		return null;
	}

	@Override
	public AutoXLIFFParameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (AutoXLIFFParameters)params;
	}

	private boolean detectXLIFF2(XMLEventReader xmlReader) throws XMLStreamException {
		XMLEvent e = null;
		for (e = xmlReader.nextEvent(); e != null; e = xmlReader.nextEvent()) {
			if (e.isStartElement()) {
				break;
			}
		}
		if (e == null) {
			return false;
		}
		// Otherwise it's the first element of the file
		StartElement start = e.asStartElement();
		QName qname = start.getName();
		if (!Const.NS_XLIFF_CORE20.equals(qname.getNamespaceURI()) ||
			!Const.ELEM_XLIFF.equals(qname.getLocalPart())) {
			return false;
		}
		Attribute version = start.getAttributeByName(new QName(Const.ATTR_VERSION));
		if (version == null) {
			return false;
		}
		try {
			double v = Double.parseDouble(version.getValue());
			return (v >= 2.0 && v < 3.0);
		}
		catch (Exception ex) { }
		return false;
	}

	@Override
	public String getName() {
		return "okf_autoxliff";
	}

	@Override
	public String getDisplayName() {
		return "XLIFF 1.2 and 2.0 Filter";
	}

	@Override
	public void close() {
		if (delegate != null) {
			delegate.close();
		}
	}

	@Override
	public void cancel() {
		if (delegate != null) {
			delegate.cancel();
		}
	}

	@Override
	public void setFilterConfigurationMapper(IFilterConfigurationMapper fcMapper) {
		if (delegate != null) {
			delegate.setFilterConfigurationMapper(fcMapper);
		}
		this.fcMapper = fcMapper;
	}

	@Override
	public ISkeletonWriter createSkeletonWriter() {
		if (delegate != null) {
			return delegate.createSkeletonWriter();
		}
		return null;
	}

	@Override
	public IFilterWriter createFilterWriter() {
		if (delegate != null) {
			IFilterWriter delegateWriter = delegate.createFilterWriter();
			return new ProxyFilterWriter(delegateWriter, delegate.getParameters());
		}
		return null;
	}

	@Override
	public EncoderManager getEncoderManager() {
		if (delegate != null) {
			return delegate.getEncoderManager();
		}
		return null;
	}

	@Override
	public String getMimeType() {
		return MimeTypeMapper.XLIFF_MIME_TYPE;
	}

	@Override
	public List<FilterConfiguration> getConfigurations() {
        return Collections.singletonList(new FilterConfiguration(getName(), MimeTypeMapper.XLIFF2_MIME_TYPE,
                getClass().getName(), getDisplayName(),
                "Calls the appropriate filter for any version of XLIFF", null, ".xlf;.xliff"));
	}

	static class ProxyFilterWriter implements IFilterWriter {
		private final IFilterWriter delegate;
		private final IParameters params;

		ProxyFilterWriter(IFilterWriter delegate, IParameters params) {
			this.delegate = delegate;
			this.params = params;
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public void setOptions(LocaleId locale, String defaultEncoding) {
			delegate.setOptions(locale, defaultEncoding);
		}

		@Override
		public void setOutput(String path) {
			delegate.setOutput(path);
		}

		@Override
		public void setOutput(OutputStream output) {
			delegate.setOutput(output);
		}

		@Override
		public Event handleEvent(Event event) {
			if (event.isStartDocument()) {
				event.getStartDocument().setFilterId(getName());
				event.getStartDocument().setFilterParameters(params);
			}
			return delegate.handleEvent(event);
		}

		@Override
		public void close() {
			delegate.close();
		}

		@Override
		public IParameters getParameters() {
			return delegate.getParameters();
		}

		@Override
		public void setParameters(IParameters params) {
			delegate.setParameters(params);
		}

		@Override
		public void cancel() {
			delegate.cancel();
		}

		@Override
		public EncoderManager getEncoderManager() {
			return delegate.getEncoderManager();
		}

		@Override
		public ISkeletonWriter getSkeletonWriter() {
			return delegate.getSkeletonWriter();
		}
	}
}
