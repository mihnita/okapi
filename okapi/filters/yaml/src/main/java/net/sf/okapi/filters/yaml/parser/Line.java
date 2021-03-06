package net.sf.okapi.filters.yaml.parser;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;

public class Line {
	// if we see a comment of multiple newlines it's not throw away and goes in the skeleton
	private static final Pattern NOT_THROW_AWAY = Pattern.compile("((\\s*((\r\n)|\r|\n)){2})|[#]");
	public boolean isSkeleton;
	public String line;
	public int indent;
	// if true we don't store this in skeleton
	// the YamlSkeletonWritr will recreate the whitespace
	// and newlines
	public boolean isThrowAway = false;
	public boolean startContinuation = false;
	public boolean endContinuation = false;
	
	public Line(String line, int indent, boolean isSkeleton) {
		this.line = line;
		this.isSkeleton = isSkeleton;
		this.indent = indent;
		this.isThrowAway = isThrowAway(line);
	}
	
	public Line(String line, boolean isSkeleton) {
		this.line = line;
		this.isSkeleton = isSkeleton;
		this.indent = -1;
		this.isThrowAway = isThrowAway(line);
	}
	
	/**
	 * Add skeleton line
	 * @param line
	 */
	public Line(String line) {
		this.line = line;
		this.isSkeleton = true;
		this.indent = -1;
		this.isThrowAway = isThrowAway(line);
	}
	
	public void setContinuation(boolean start, boolean end) {
		this.startContinuation = start;
		this.endContinuation = end;
	}
	
	public static String prependWhitespace(int indent) {
		if (indent <= 0) return ""; 
		char[] repeat = new char[indent];
		Arrays.fill(repeat, ' ');
		return new String(repeat);
	}
	
	public static String decode(String encoded) {
		String decoded = encoded;
		try {
			Parser parser = new ParserImpl(new StreamReader(encoded));
			StringBuilder sb = new StringBuilder();
			for (Event e = parser.getEvent(); e != null; e = parser.getEvent()) {
				if (e.is(ID.Scalar)) {
					ScalarEvent val = (ScalarEvent)e;
					sb.append(val.getValue());
				}
			}
			return sb.toString();
		} catch(Exception e) {
			// case where snakeyaml blows up on surrogates and other "non-printables"
			// just pass through the uncide value and hope for the best
			// FXIME: Dump snakeyaml and use our own decoder
		}
		return decoded;
	}
	
	/*
	 * Throway line is the newline from previous line and indented whitespace
	 * If we find multiple newlines or a comment marker then keep the
	 * line as skeleton
	 */
	private boolean isThrowAway(String line) {
		if (isSkeleton) {
			return !NOT_THROW_AWAY.matcher(line).find();
		}
		return false;
	}

	@Override
	public String toString() {
		return line;
	}

	public boolean isEmpty() {
		if (line == null) return true;
		return line.isEmpty();
	}
}
