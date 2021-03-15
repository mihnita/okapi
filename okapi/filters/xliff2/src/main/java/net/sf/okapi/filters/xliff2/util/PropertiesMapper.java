/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.filters.xliff2.util;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.IWithProperties;
import net.sf.okapi.common.resource.InlineAnnotation;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.filters.xliff2.model.XLIFF2PropertyStrings;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.CanReorder;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.TargetState;
import net.sf.okapi.lib.xliff2.core.Unit;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saves parameters and other data from the XLIFF Toolkit into Okapi Core and
 * back. These properties are all saved as read only.
 * <p>
 * Since the mapping operation to and from should be equivalent, we put both
 * operations in here to make it easier to compare them. All the methods in here
 * have at least 2 parameters. The first parameter is where the data is being
 * read from, and the second parameter is where the data is being written to.
 */
public class PropertiesMapper {

	private static final Pattern EXTENDED_ATTRIBUTE_KEY_PATTERN = Pattern
			.compile("^" + Pattern.quote(XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_PREFIX) + "(.*)"
					+ Pattern.quote(XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_DELIMITER) + "\\{(.*?)\\}" + "(.*)$");

	/**
	 * Transfers properties from the XLIFF Toolkit StartXliffData to the Okapi Core
	 * StartDocument
	 * <p>
	 * Relates to &lt;xliff> elements in XLIFF 2.0 file
	 *
	 * @param xliffStartXliff    The properties to read from
	 * @param okapiStartDocument The properties to write to
	 */
	public static void setStartXliffProperties(StartXliffData xliffStartXliff, StartDocument okapiStartDocument) {
		setProperty(XLIFF2PropertyStrings.SRC_LANG, xliffStartXliff.getSourceLanguage(), okapiStartDocument);
		setProperty(XLIFF2PropertyStrings.TRG_LANG, xliffStartXliff.getTargetLanguage(), okapiStartDocument);
		setProperty(XLIFF2PropertyStrings.VERSION, xliffStartXliff.getVersion(), okapiStartDocument);
		setExtendedAttributes(xliffStartXliff, okapiStartDocument);
	}

	/**
	 * Transfers properties from the Okapi Core StartDocument to the XLIFF Toolkit
	 * StartXliffData
	 * <p>
	 * Relates to &lt;xliff> elements in XLIFF 2.0 file
	 *
	 * @param okapiStartDocument The properties to read from
	 * @param xliffStartXliff    The properties to write to
	 */
	public static void setStartXliffProperties(StartDocument okapiStartDocument, StartXliffData xliffStartXliff) {
		xliffStartXliff.setSourceLanguage(okapiStartDocument.getLocale().toString());
		setExtendedAttributes(okapiStartDocument, xliffStartXliff);
	}

	/**
	 * Transfers properties from the XLIFF Toolkit StartFileData to the Okapi Core
	 * StartSubDocument
	 * <p>
	 * Relates to &lt;file> elements in XLIFF 2.0 file
	 *
	 * @param xliffStartFileData The properties to read from
	 * @param okapiStartSubDoc   The properties to write to
	 */
	public static void setStartFileProperties(StartFileData xliffStartFileData, StartSubDocument okapiStartSubDoc) {
		okapiStartSubDoc.setId(xliffStartFileData.getId());
		okapiStartSubDoc.setIsTranslatable(xliffStartFileData.getTranslate());
		setProperty(XLIFF2PropertyStrings.SRC_DIR, xliffStartFileData.getSourceDir(), okapiStartSubDoc);
		setProperty(XLIFF2PropertyStrings.TRG_DIR, xliffStartFileData.getTargetDir(), okapiStartSubDoc);
		setProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, xliffStartFileData.getCanResegment(), okapiStartSubDoc);
		okapiStartSubDoc.setName(xliffStartFileData.getOriginal());
		setExtendedAttributes(xliffStartFileData, okapiStartSubDoc);
	}

	/**
	 * Transfers properties from the Okapi Core StartSubDocument to the XLIFF
	 * Toolkit StartFileData
	 * <p>
	 * Relates to &lt;file> elements in XLIFF 2.0 file
	 *
	 * @param xliffStartFileData The properties to read from
	 * @param okapiStartSubDoc   The properties to write to
	 */
	public static void setStartFileProperties(StartSubDocument okapiStartSubDoc, StartFileData xliffStartFileData) {
		xliffStartFileData.setId(okapiStartSubDoc.getId());
		xliffStartFileData.setTranslate(okapiStartSubDoc.isTranslatable());
		xliffStartFileData.setOriginal(okapiStartSubDoc.getName());
		xliffStartFileData
				.setSourceDir(stringToDirection(getProperty(XLIFF2PropertyStrings.SRC_DIR, okapiStartSubDoc)));
		xliffStartFileData
				.setTargetDir(stringToDirection(getProperty(XLIFF2PropertyStrings.TRG_DIR, okapiStartSubDoc)));
		xliffStartFileData
				.setCanResegment(stringToBoolean(getProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, okapiStartSubDoc)));

		setExtendedAttributes(okapiStartSubDoc, xliffStartFileData);
	}

	/**
	 * Transfers properties from the XLIFF 2.0 Start Group Data to the Okapi Core
	 * Start Group
	 *
	 * @param xliffStartGroupData The properties to read from
	 * @param okapiStartGroup     The properties to write to
	 */
	public static void setGroupProperties(StartGroupData xliffStartGroupData, StartGroup okapiStartGroup) {
		okapiStartGroup.setId(xliffStartGroupData.getId());
		okapiStartGroup.setIsTranslatable(xliffStartGroupData.getTranslate());
		okapiStartGroup.setName(xliffStartGroupData.getName());
		okapiStartGroup.setType(xliffStartGroupData.getType());
		setProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, xliffStartGroupData.getCanResegment(), okapiStartGroup);
		setProperty(XLIFF2PropertyStrings.SRC_DIR, xliffStartGroupData.getSourceDir(), okapiStartGroup);
		setProperty(XLIFF2PropertyStrings.TRG_DIR, xliffStartGroupData.getTargetDir(), okapiStartGroup);
		setExtendedAttributes(xliffStartGroupData, okapiStartGroup);
	}

	/**
	 * Transfers properties from the Okapi Core Start Group to the XLIFF 2.0 Start
	 * Group Data
	 *
	 * @param xliffStartGroupData The properties to write to
	 * @param okapiStartGroup     The properties to read from
	 */
	public static void setGroupProperties(StartGroup okapiStartGroup, StartGroupData xliffStartGroupData) {
		xliffStartGroupData.setId(okapiStartGroup.getId());
		xliffStartGroupData.setTranslate(okapiStartGroup.isTranslatable());
		final String canResegment = getProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, okapiStartGroup);
		if (canResegment != null) {
			xliffStartGroupData.setCanResegment(stringToBoolean(canResegment));
		}
		xliffStartGroupData
				.setSourceDir(stringToDirection(getProperty(XLIFF2PropertyStrings.SRC_DIR, okapiStartGroup)));
		xliffStartGroupData
				.setTargetDir(stringToDirection(getProperty(XLIFF2PropertyStrings.TRG_DIR, okapiStartGroup)));
		xliffStartGroupData.setType(okapiStartGroup.getType());
		setExtendedAttributes(okapiStartGroup, xliffStartGroupData);
		xliffStartGroupData.setName(okapiStartGroup.getName());
	}

	/**
	 * Transfers properties from the XLIFF Toolkit TextUnit to the Okapi Core
	 * TextUnit
	 * <p>
	 * Relates to &lt;unit> elements in XLIFF 2.0 file
	 *
	 * @param unit The properties to read from
	 * @param tu   The properties to write to
	 */
	public static void setTextUnitProperties(Unit unit, ITextUnit tu) {
		tu.setId(unit.getId());
		tu.setIsTranslatable(unit.getTranslate());
		setProperty(XLIFF2PropertyStrings.NAME, unit.getName(), tu);
		setProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, unit.getCanResegment(), tu);
		setProperty(XLIFF2PropertyStrings.TYPE, unit.getType(), tu);
		setProperty(XLIFF2PropertyStrings.SRC_DIR, unit.getSourceDir(), tu);
		setProperty(XLIFF2PropertyStrings.TRG_DIR, unit.getTargetDir(), tu);

		// If all xliff2 Unit segments preserve whitespace then Okapi TextUnit does as
		// well. If one segment does not preserve whitespace the TextUnit preserveWhitespace
		// is set to false. Later set each individual TextPart to match the xliff2 Unit.part
		boolean preserveWhitespace = true;
		for (Segment segment : unit.getSegments()) {
            if (!segment.getPreserveWS()) {
                preserveWhitespace = false;
                break;
            }
		}
		tu.setPreserveWhitespaces(preserveWhitespace);

		setExtendedAttributes(unit, tu);
	}

	/**
	 * Transfers properties from the Okapi Core TextUnit to the XLIFF Toolkit
	 * TextUnit
	 * <p>
	 * Relates to &lt;unit> elements in XLIFF 2.0 file
	 *
	 * @param okapiTextUnit The properties to read from
	 * @param xliffTextUnit The properties to write to
	 */
	public static void setTextUnitProperties(ITextUnit okapiTextUnit, Unit xliffTextUnit) {

		xliffTextUnit.setId(okapiTextUnit.getId());
		xliffTextUnit.setName(okapiTextUnit.getName());
		xliffTextUnit.setType(okapiTextUnit.getType());
		xliffTextUnit.setTranslate(okapiTextUnit.isTranslatable());

		final String canResegment = getProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, okapiTextUnit);
		if (canResegment != null) {
			xliffTextUnit.setCanResegment(stringToBoolean(canResegment));
		}

		final String sourceDirection = getProperty(XLIFF2PropertyStrings.SRC_DIR, okapiTextUnit);
		if (sourceDirection != null) {
			xliffTextUnit.setTargetDir(stringToDirection(sourceDirection));
		}

		final String targetDirection = getProperty(XLIFF2PropertyStrings.TRG_DIR, okapiTextUnit);
		if (targetDirection != null) {
			xliffTextUnit.setSourceDir(stringToDirection(targetDirection));
		}

		setExtendedAttributes(okapiTextUnit, xliffTextUnit);

	}

	/**
	 * Transfers properties from the XLIFF Toolkit Part to the Okapi Core TextPart
	 * <p>
	 * Relates to &lt;segment> and &lt;ignorable> elements in XLIFF 2.0 file
	 *
	 * @param xliffPart The properties to write to
	 * @param okapiPart The properties to read from
	 */
	public static void setPartProperties(Part xliffPart, net.sf.okapi.common.resource.TextPart okapiPart) {
		okapiPart.originalId = xliffPart.getId();
		okapiPart.setPreserveWhitespaces(xliffPart.getPreserveWS());
		if (xliffPart.isSegment()) {
			final Segment xliffSegment = (Segment) xliffPart;
			setProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, xliffSegment.getCanResegment(), okapiPart);
			setProperty(XLIFF2PropertyStrings.STATE, xliffSegment.getState(), okapiPart);
			setProperty(XLIFF2PropertyStrings.SUB_STATE, xliffSegment.getSubState(), okapiPart);

		}
	}

	/**
	 * Transfers properties from the Okapi Core TextPart to the XLIFF Toolkit Part
	 * <p>
	 * Relates to &lt;segment> and &lt;ignorable> elements in XLIFF 2.0 file
	 *
	 * @param okapiSourcePart The properties to read from
	 * @param xliffPart       The properties to write to
	 */
	public static void setPartProperties(net.sf.okapi.common.resource.TextPart okapiSourcePart, Part xliffPart,
			ITextUnit okapiTextUnit) {

		// If the XML_PRESERVE_WHITESPACE property is set, use that. Otherwise, use the
		// TextUnit's property
		xliffPart.setPreserveWS(okapiSourcePart.preserveWhitespaces());
		xliffPart.setId(okapiSourcePart.originalId);

		if (xliffPart.isSegment()) {
			final Segment xliffSegment = (Segment) xliffPart;
			final net.sf.okapi.common.resource.Segment okapiSegment = (net.sf.okapi.common.resource.Segment) okapiSourcePart;

			final String canResegment = getProperty(XLIFF2PropertyStrings.CAN_RESEGMENT, okapiSegment);
			if (canResegment != null) {
				xliffSegment.setCanResegment(stringToBoolean(canResegment));
			}

			final String state = getProperty(XLIFF2PropertyStrings.STATE, okapiSegment);
			if (state != null) {
				xliffSegment.setState(stringToTargetState(state));
			}

			final String substate = getProperty(XLIFF2PropertyStrings.SUB_STATE, okapiSegment);
			if (substate != null) {
				xliffSegment.setSubState(substate);
			}
		}
	}

	/**
	 * Transfers properties from the Okapi Core Code Tag to the XLIFF Toolkit Code
	 * Tag
	 * <p>
	 * Relates to &lt;ph>, &lt;pc>, and &lt;sc> elements in XLIFF 2.0 file
	 * 
	 * @param okapiCode  The properties to read from
	 * @param xliff2Ctag The properties to write to
	 */
	public static void setCodeProperties(Code okapiCode, CTag xliff2Ctag) {
		xliff2Ctag.setCanCopy(okapiCode.isCloneable());
		xliff2Ctag.setCanDelete(okapiCode.isDeleteable());
		xliff2Ctag.setData(okapiCode.getData());
		xliff2Ctag.setDisp(okapiCode.getDisplayText());
		if (okapiCode.getType() != null && !okapiCode.getType().equals("null"))
			try {
				xliff2Ctag.setType(okapiCode.getType());
			} catch (InvalidParameterException e) {
				LoggerFactory.getLogger(PropertiesMapper.class).debug("Could net set CTag type of {}: {}", xliff2Ctag,
						e.getMessage());
			}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.CAN_COPY)) {
			xliff2Ctag.setCanCopy(stringToBoolean(okapiCode.getAnnotation(XLIFF2PropertyStrings.CAN_COPY).getData()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.CAN_DELETE)) {
			xliff2Ctag
					.setCanDelete(stringToBoolean(okapiCode.getAnnotation(XLIFF2PropertyStrings.CAN_DELETE).getData()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.CAN_REORDER)) {
			xliff2Ctag.setCanReorder(CanReorder
					.valueOf(okapiCode.getAnnotation(XLIFF2PropertyStrings.CAN_REORDER).getData().toUpperCase()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.CAN_OVERLAP)) {
			xliff2Ctag.setCanOverlap(
					stringToBoolean(okapiCode.getAnnotation(XLIFF2PropertyStrings.CAN_OVERLAP).getData()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.SUB_FLOWS)) {
			xliff2Ctag.setSubFlows(okapiCode.getAnnotation(XLIFF2PropertyStrings.SUB_FLOWS).getData());
		}
		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.SUB_TYPE)) {
			xliff2Ctag.setSubType(okapiCode.getAnnotation(XLIFF2PropertyStrings.SUB_TYPE).getData());
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.DISP)) {
			xliff2Ctag.setDisp(okapiCode.getAnnotation(XLIFF2PropertyStrings.DISP).getData());
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.DIR)) {
			xliff2Ctag.setDir(stringToDirection(okapiCode.getAnnotation(XLIFF2PropertyStrings.DIR).getData()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.DATA_DIR)) {
			xliff2Ctag.setDataDir(stringToDirection(okapiCode.getAnnotation(XLIFF2PropertyStrings.DATA_DIR).getData()));
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.DATA_REF)) {
			xliff2Ctag.setDataRef(okapiCode.getAnnotation(XLIFF2PropertyStrings.DATA_REF).getData());
		}

		if (okapiCode.hasAnnotation(XLIFF2PropertyStrings.EQUIV)) {
			xliff2Ctag.setEquiv(okapiCode.getAnnotation(XLIFF2PropertyStrings.EQUIV).getData());
		}

		setExtendedAttributes(okapiCode, xliff2Ctag);
	}

	/**
	 * Transfers properties from the XLIFF Toolkit Code Tag to the Okapi Core Code
	 * Tag.
	 * <p>
	 * Relates to &lt;ph>, &lt;pc>, and &lt;sc> elements in XLIFF 2.0 file
	 * <p>
	 * Due to the Okapi Core Code only allowing integers as a valid ID, the integer
	 * used instead is a hash code of the ID and the actual ID is saved in the
	 * original ID.
	 *
	 * @param okapiCode  The properties to read from
	 * @param xliff2Ctag The properties to write to
	 */
	public static void setCodeProperties(CTag xliff2Ctag, Code okapiCode) {

		final String id = xliff2Ctag.getId();
		int okapiId = id.hashCode();

		/*
		 * FIXME: use this code in the future but breaks many golden comparisons int
		 * okapiId; try { okapiId = Integer.parseInt(id); } catch (NumberFormatException
		 * nfe) { okapiId = id.hashCode(); }
		 */

		okapiCode.setId(okapiId);
		okapiCode.setDeleteable(xliff2Ctag.getCanDelete());
		okapiCode.setCloneable(xliff2Ctag.getCanCopy());
		okapiCode.setData(xliff2Ctag.getData());
		okapiCode.setDisplayText(xliff2Ctag.getDisp());
		okapiCode.setOriginalId(xliff2Ctag.getId());
		okapiCode.setTagType(convertTagType(xliff2Ctag.getTagType()));

		if (xliff2Ctag.getType() != null) {
			okapiCode.setType(xliff2Ctag.getType());
		}

		okapiCode.setAnnotation(XLIFF2PropertyStrings.CAN_DELETE,
				new InlineAnnotation(booleanToString(xliff2Ctag.getCanDelete())));
		okapiCode.setAnnotation(XLIFF2PropertyStrings.CAN_COPY,
				new InlineAnnotation(booleanToString(xliff2Ctag.getCanCopy())));
		okapiCode.setAnnotation(XLIFF2PropertyStrings.CAN_OVERLAP,
				new InlineAnnotation(booleanToString(xliff2Ctag.getCanOverlap())));

		if (xliff2Ctag.getCanReorder() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.CAN_REORDER,
					new InlineAnnotation(xliff2Ctag.getCanReorder().toString()));
		}

		if (xliff2Ctag.getSubType() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.SUB_TYPE, new InlineAnnotation(xliff2Ctag.getSubType()));
		}

		if (xliff2Ctag.getSubFlows() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.SUB_FLOWS, new InlineAnnotation(xliff2Ctag.getSubFlows()));
		}

		if (xliff2Ctag.getDisp() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.DISP, new InlineAnnotation(xliff2Ctag.getDisp()));
		}

		if (xliff2Ctag.getDir() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.DIR,
					new InlineAnnotation(directionToString(xliff2Ctag.getDir())));
		}

		if (xliff2Ctag.getDataDir() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.DATA_DIR,
					new InlineAnnotation(directionToString(xliff2Ctag.getDataDir())));
		}

		if (xliff2Ctag.getDataRef() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.DATA_REF, new InlineAnnotation(xliff2Ctag.getDataRef()));
		}

		if (xliff2Ctag.getEquiv() != null) {
			okapiCode.setAnnotation(XLIFF2PropertyStrings.EQUIV, new InlineAnnotation(xliff2Ctag.getEquiv()));
		}

		setExtendedAttributes(xliff2Ctag, okapiCode);
	}

	/**
	 * Transfers extended attributes from the XLIFF Toolkit to the Okapi Core
	 * <p>
	 * This relates to the &lt;code> elements, since their attributes have to be
	 * stored differently.
	 *
	 * @param xliffElement The element to read from
	 * @param code         The Code to write to
	 */
	private static void setExtendedAttributes(IWithExtAttributes xliffElement, Code code) {
		final ExtAttributes extAttributes = xliffElement.getExtAttributes();
		final Set<String> namespaces = extAttributes.getNamespaces();
		namespaces.forEach(namespaceURI -> {
			final String propertyName = XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX + namespaceURI;
			final String value = extAttributes.getNamespacePrefix(namespaceURI);
			code.setAnnotation(propertyName, new InlineAnnotation(value));

		});

		extAttributes.forEach(extAttribute -> {
			// Most of the data is stored in the key, to reduce the chance of collisions.
			final String propertyName = XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_PREFIX + extAttribute.getPrefix()
					+ XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_DELIMITER + extAttribute.getQName();
			final String value = extAttribute.getValue();
			code.setAnnotation(propertyName, new InlineAnnotation(value));
		});
	}

	/**
	 * Transfers extended attributes from the Okapi Core to the XLIFF Toolkit
	 * <p>
	 * This relates to the &lt;code> elements, since their attributes have to be
	 * stored differently.
	 *
	 * @param code         The Code to read from
	 * @param xliffElement The element to write to
	 */
	private static void setExtendedAttributes(Code code, IWithExtAttributes xliffElement) {
		final ExtAttributes extAttributes = xliffElement.getExtAttributes();

		code.getAnnotationsTypes().stream().filter(p -> p.startsWith(XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX))
				.forEach(annotationName -> {
					final String namespaceURI = annotationName.replace(XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX,
							"");
					final String prefix = code.getAnnotation(annotationName).getData();
					extAttributes.setNamespace(prefix, namespaceURI);
				});

		code.getAnnotationsTypes().stream().filter(p -> p.startsWith(XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_PREFIX))
				.forEach(annotationName -> {
					final Matcher matcher = EXTENDED_ATTRIBUTE_KEY_PATTERN.matcher(annotationName);
					final boolean found = matcher.find();
					if (found) {
						final String namespaceURI = matcher.group(2);
						final String localeName = matcher.group(3);
						final String value = code.getAnnotation(annotationName).getData();
						extAttributes.setAttribute(namespaceURI, localeName, value);
					} else {
						LoggerFactory.getLogger(PropertiesMapper.class)
								.warn("Could not find extended attribute information from {}", annotationName);
					}
				});
	}

	/**
	 * Transfers extended attributes from the XLIFF Toolkit to the Okapi Core.
	 * <p>
	 * This relates to any attribute which is not part of the XLIFF Toolkit spec,
	 * and therefore can affect any element.
	 *
	 * @param xliffElement    The attributes to read from
	 * @param okapiProperties The properties to write to
	 */
	private static void setExtendedAttributes(IWithExtAttributes xliffElement, IWithProperties okapiProperties) {

		final ExtAttributes extAttributes = xliffElement.getExtAttributes();
		final Set<String> namespaces = extAttributes.getNamespaces();
		namespaces.forEach(namespaceURI -> {
			final String propertyName = XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX + namespaceURI;
			final String value = extAttributes.getNamespacePrefix(namespaceURI);
			setProperty(propertyName, value, okapiProperties);

		});

		extAttributes.forEach(extAttribute -> {
			// Most of the data is stored in the key, to reduce the chance of collisions.
			final String propertyName = XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_PREFIX + extAttribute.getPrefix()
					+ XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_DELIMITER + extAttribute.getQName();
			final String value = extAttribute.getValue();
			setProperty(propertyName, value, okapiProperties);
		});
	}

	/**
	 * Transfers extended attributes from the Okapi Core to the XLIFF Toolkit
	 * <p>
	 * This relates to any attribute which is not part of the XLIFF Toolkit spec,
	 * and therefore can affect any element.
	 *
	 * @param xliffElement    The properties to write to
	 * @param okapiProperties The attributes to read from
	 */
	private static void setExtendedAttributes(IWithProperties okapiProperties, IWithExtAttributes xliffElement) {
		final ExtAttributes extAttributes = xliffElement.getExtAttributes();

		okapiProperties.getPropertyNames().stream()
				.filter(p -> p.startsWith(XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX)).forEach(propertyName -> {
					final String namespaceURI = propertyName.replace(XLIFF2PropertyStrings.EXTENDED_NAMESPACE_PREFIX,
							"");
					final String prefix = getProperty(propertyName, okapiProperties);
					extAttributes.setNamespace(prefix, namespaceURI);
				});

		okapiProperties.getPropertyNames().stream()
				.filter(p -> p.startsWith(XLIFF2PropertyStrings.EXTENDED_ATTRIBUTE_PREFIX)).forEach(propertyName -> {
					final Matcher matcher = EXTENDED_ATTRIBUTE_KEY_PATTERN.matcher(propertyName);
					final boolean found = matcher.find();
					if (found) {
						final String namespaceURI = matcher.group(2);
						final String localeName = matcher.group(3);
						final String value = getProperty(propertyName, okapiProperties);
						extAttributes.setAttribute(namespaceURI, localeName, value);
					} else {
						LoggerFactory.getLogger(PropertiesMapper.class)
								.warn("Could not find extended attribute information from {}", propertyName);
					}
				});
	}

	/**
	 * This is almost the same as {@link IWithProperties#getProperty(String)}, but
	 * will also get the value too. This way, you don't have to perform a null check
	 * on the property.
	 *
	 * @param key                 The key that the property is stored under
	 * @param eventWithProperties The event which has the properties
	 * @return The String value of the property, or null if the property wasn't
	 *         found.
	 */
	private static String getProperty(String key, IWithProperties eventWithProperties) {
		final Property property = eventWithProperties.getProperty(key);
		if (property == null) {
			return null;
		}
		return property.getValue();
	}

	// Ensure that the conversion to String happens with the conversion methods

	private static void setProperty(String key, String value, IWithProperties eventWithProperties) {
		eventWithProperties.setProperty(new Property(key, value, true));
	}

	private static void setProperty(String key, boolean value, IWithProperties eventWithProperties) {
		eventWithProperties.setProperty(new Property(key, booleanToString(value), true));
	}

	private static void setProperty(String key, Directionality value, IWithProperties eventWithProperties) {
		eventWithProperties.setProperty(new Property(key, directionToString(value), true));
	}

	private static void setProperty(String key, TargetState value, IWithProperties eventWithProperties) {
		eventWithProperties.setProperty(new Property(key, targetStateToString(value), true));
	}

	// Okapi Core only allows storing Properties as strings. So to ensure that no
	// data converted improperly, the
	// conversion happens in these methods.

	private static boolean stringToBoolean(String value) {
		return "yes".equals(value);
	}

	private static String booleanToString(boolean value) {
		return value ? "yes" : "no";
	}

	private static Directionality stringToDirection(String value) {
		if (value == null)
			return Directionality.AUTO;
		if ("NOT ALLOWED".equals(value))
			return Directionality.INHERITED;
		return Directionality.valueOf(value.toUpperCase());
	}

	private static String directionToString(Directionality value) {
		return value.toString();
	}

	private static TargetState stringToTargetState(String value) {
		return TargetState.valueOf(value.toUpperCase());
	}

	private static String targetStateToString(TargetState value) {
		return value.toString();
	}

	private static TextFragment.TagType convertTagType(net.sf.okapi.lib.xliff2.core.TagType tagType) {
		switch (tagType) {
		case CLOSING:
			return TextFragment.TagType.CLOSING;
		case OPENING:
			return TextFragment.TagType.OPENING;
		case STANDALONE:
			return TextFragment.TagType.PLACEHOLDER;
		default:
			LoggerFactory.getLogger(PropertiesMapper.class).warn("TagType {} unrecognized. Treating it as {}.", tagType,
					net.sf.okapi.lib.xliff2.core.TagType.STANDALONE);
			return TextFragment.TagType.PLACEHOLDER;

		}
	}

}
