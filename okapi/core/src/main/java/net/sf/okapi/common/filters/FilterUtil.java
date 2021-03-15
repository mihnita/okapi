/*===========================================================================
  Copyright (C) 2009-2012 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.common.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.DeepenSegmentationAnnotaton;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.Custom;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.IMultilingual;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.IWithAnnotations;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextUnitUtil;
import org.slf4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class FilterUtil {

	/**
	 * Create an {@link Custom} {@link Event} that tells the SegmenterStep that it needs
	 * to deepen existing segmentation.
	 * @return {@link Event} with {@link Custom} resource and {@link DeepenSegmentationAnnotaton}.
	 */
	public static Event createDeepenSegmentationEvent() {
		Custom cr = new Custom();
		DeepenSegmentationAnnotaton a = new DeepenSegmentationAnnotaton();
		cr.setAnnotation(a);
		return new Event(EventType.CUSTOM, cr);
	}

	/**
	 * Creates an instance of the filter for a given configuration identifier
	 * and loads its corresponding parameters. Only Okapi default filter configurations
	 * are accepted.
	 * @param configId the filter configuration identifier. Can only be one of default filter
	 * configurations.
	 * @return a new {@link IFilter} object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 */
	public static IFilter createFilter(String configId) {
		IFilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, true, true);
		return fcMapper.createFilter(configId);
	}

	/**
	 * Creates an instance of the filter for a given configuration identifier
	 * and loads its corresponding parameters.
	 * @param filterClass class of the filter.
	 * @param configId the filter configuration identifier. Can be either one of Okapi
	 * default filter configurations or one of the built-in configurations defined in
	 * the filter class.
	 * @return a new {@link IFilter} object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 */
	public static IFilter createFilter(Class<? extends IFilter> filterClass, String configId) {
		IFilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, true, true);
		fcMapper.addConfigurations(filterClass.getName());
		return fcMapper.createFilter(configId);
	}

	/**
	 * Creates an instance of the filter for a given configuration identifier
	 * and loads its corresponding parameters. This method accepts a list of the
	 * URLs of fprm files defining custom configurations, and can be used to create
	 * a filter and configure its sub-filters in one call.
	 * @param configId the filter configuration identifier. Can be either one of Okapi
	 * default filter configurations or one of the custom configurations defined in
	 * the fprm files.
	 * @param customConfigs a list of the URLs of fprm files defining custom configurations.
	 * Every file name should follow the pattern of custom filter configurations,
	 * i.e. contain a filter name like "okf_xmlstream@custom_config.fprm". The file extension
	 * should be .fprm.
	 * @return a new {@link IFilter} object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 */
	public static IFilter createFilter(String configId, URL... customConfigs) {
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, true, true);

		for (URL customConfig : customConfigs) {
			addCustomConfig(fcMapper, customConfig);
		}

		IFilter filter = fcMapper.createFilter(configId);
		filter.setFilterConfigurationMapper(fcMapper);
		return filter;
	}

	/**
	 * Adds to a given {@link FilterConfigurationMapper} object the custom configuration
	 * defined in the fprm file denoted by a given URL.
	 * @param fcMapper the given {@link FilterConfigurationMapper}.
	 * @param customConfig the URL of a fprm file defining the custom configuration
	 * the filter should be loaded from. The file extension should be .fprm.
	 * The file name should follow the pattern of custom filter configurations,
	 * i.e. contain a filter name like "okf_xmlstream@custom_config.fprm".
	 * @return the configuration identifier or null if the configuration was not added.
	 */
	public static String addCustomConfig(FilterConfigurationMapper fcMapper, URL customConfig) {
		String configId = null;
		try {
			String path = customConfig.toURI().getPath();
			String root = Util.getDirectoryName(path) + File.separator;
			configId = Util.getFilename(path, false);
			fcMapper.setCustomConfigurationsDirectory(root);
			fcMapper.addCustomConfiguration(configId);
			fcMapper.updateCustomConfigurations();
		} catch (URISyntaxException e) {
			throw new OkapiIOException(e);
		}
		return configId;
	}

	/**
	 * Creates an instance of the filter for a given URL of a fprm file defining a
	 * custom configuration.
	 * @param customConfig the URL of a fprm file defining the custom configuration
	 * the filter should be loaded from. The file extension should be .fprm.
	 * The file name should follow the pattern of custom filter configurations,
	 * i.e. contain a filter name like "okf_xmlstream@custom_config.fprm".
	 * @return a new {@link IFilter} object (with its parameters loaded) for the given
	 * configuration identifier, or null if the object could not be created.
	 */
	public static IFilter createFilter(URL customConfig) {
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcMapper, true, true);
		return fcMapper.createFilter(addCustomConfig(fcMapper, customConfig));
	}

	/**
	 * Logs events at the debug level.
	 * @param events The events to log
	 * @param logger The logger
	 */
	public static void logDebugEvents(Iterable<Event> events, Logger logger) {
		int k = 0;
		for (Event e : events) {
			logDebugEvent(e, String.format("Event[%d]: ", k), logger);
			++k;
		}
	}

	/**
	 * Logs an event at the debug level.
	 * 
	 * @param e
	 * @param leader A short string to print before the event (but after the
	 *               logger's standard prefix string)
	 * @param logger
	 */
	public static void logDebugEvent(Event e, String leader, Logger logger) {
		if (!logger.isDebugEnabled())
			return;

		if (leader == null) {
			leader = "";
		}

		StringBuilder sb = new StringBuilder(leader);
		sb.append('\n').append(leader).append(e.getEventType().name());

		if (e.isTextUnit()) {
			ITextUnit tu = e.getTextUnit();
			sb.append(" { id:").append(nqs(tu.getId())).append(",\n\t");

			printINameable(sb, "\t", tu);

			sb.append("referent:").append(tu.isReferent()).append(", ");
			sb.append("#refs:").append(tu.getReferenceCount()).append(", ");

			sb.append("\n\ttextParts:[\n");
			for (Segment seg : tu.getSourceSegments()) {
				sb.append("\t\t{seg:\"");
				appendEscapingNonAscii(sb, TextUnitUtil.toText(seg.text.getCodedText(), seg.text.getCodes()));
				sb.append("\"");
				if (seg.text.getCodes().size() == 0) {
					sb.append("}");
				} else {
					sb.append(",\n\t\t codes:{\n");
					sb.append("\t\t\t");
					sb.append(Code.codesToString(seg.text.getCodes()).replaceAll("\u009C", ", ").replace("\u009D",
							";\n\t\t\t"));
					sb.append("} }");
				}
			}
			sb.append("\n\t] }");
		} else if (e.isDocumentPart()) {
			DocumentPart dp = e.getDocumentPart();
			sb.append(" { id:").append(nqs(dp.getId())).append(",\n\t");
			printINameable(sb, "\t", dp);
			sb.append("referent:").append(dp.isReferent()).append(", ");
			sb.append("#refs:").append(dp.getReferenceCount());
			sb.append("}");
		} else if (e.isStartGroup()) {
			StartGroup sg = e.getStartGroup();
			sb.append(" { id:").append(nqs(sg.getId())).append(", ");
			printINameable(sb, "\t", sg);
			sb.append("}");
		} else if (e.isEndGroup()) {
			Ending eg = e.getEndGroup(); // Note: Ending is not an INameable.
			sb.append(" { id:").append(nqs(eg.getId())).append(",\n\t");
			printSkeleton(sb, "\t", eg);
			printAnnotations(sb, "\t", eg);
			sb.append("}");
		} else {
			IResource res = e.getResource();
			sb.append(" { id:").append(nqs(res.getId())).append(",\n\t");
			printSkeleton(sb, "\t", res);
			sb.append("}");
		}
		logger.debug(sb.toString());
	}

	/*
	 * Converts the null value to the string "null", otherwise double-quote the
	 * string.
	 */
	private static String nqs(String nullableStr) {
		if (nullableStr == null)
			return "null";
		else
			return String.format("\"%s\"", nullableStr);
	}

	private static StringBuilder appendEscapingNonAscii(StringBuilder sb, String codedString) {
		for (char c : codedString.toCharArray()) {
			if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\r') {
				sb.append("\\r");
			} else if (c == '\t') {
				sb.append("\\t");
			} else if (c < ' ' || c >= (char) 0x0100) {
				sb.append(String.format("\\u%04x", (int) c));
			} else {
				sb.append(c);
			}
		}
		return sb;
	}

	/*
	 * Print out INameable part of the resource.
	 */
	private static void printINameable(StringBuilder sb, String ind, IMultilingual res) {
		printSkeleton(sb, ind, res);
		if (res.getName() != null)
			sb.append("name:\"").append(res.getName()).append("\", ");
		if (res.getType() != null)
			sb.append("type:\"").append(res.getType()).append("\", ");
		if (res.getMimeType() != null)
			sb.append("mimeType:\"").append(res.getMimeType()).append("\", ");
		sb.append("translatable:").append(res.isTranslatable()).append(", ");

		if (res.getPropertyNames() != null && res.getPropertyNames().size() != 0) {
			sb.append(",\n").append(ind).append("properties:{");
			for (String pn : res.getPropertyNames()) {
				sb.append("\"").append(pn).append("\":\"").append(res.getProperty(pn).toString()).append("\", ");
			}
			sb.setLength(sb.length() - 2); // Remove the last ", "
			sb.append("}, ");
		}

		if (res.getSourcePropertyNames() != null && res.getSourcePropertyNames().size() != 0) {
			sb.append(",\n").append(ind).append("sourceProperties:{");
			for (String pn : res.getSourcePropertyNames()) {
				sb.append("\"").append(pn).append("\":\"").append(res.getSourceProperty(pn).toString()).append("\", ");
			}
			sb.setLength(sb.length() - 2); // Remove the last ", "
			sb.append("}, ");
		}

		printAnnotations(sb, ind, res);
	}

	private static void printSkeleton(StringBuilder sb, String ind, IResource res) {
		if (res.getSkeleton() != null) {
			sb.append("skeleton:\"");
			appendEscapingNonAscii(sb, res.getSkeleton().toString()).append("\",\n").append(ind);
		}
	}

	private static void printAnnotations(StringBuilder sb, String ind, IWithAnnotations res) {
		if (res.getAnnotations() != null && res.getAnnotations().iterator().hasNext()) {
			sb.append("\n").append(ind).append("annotations:[");
			for (IAnnotation ann : res.getAnnotations()) {
				sb.append('"').append(ann.toString()).append("\", ");
			}
			sb.setLength(sb.length() - 2); // Remove the last ", ".
			sb.append("]");
		}
	}

}
