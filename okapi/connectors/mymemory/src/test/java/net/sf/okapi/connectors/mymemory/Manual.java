package net.sf.okapi.connectors.mymemory;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;

/**
 * Simple program that performs direct queries with a {@link MyMemoryTMConnector} object
 * to test various parameters.
 *
 * Needs a working internet connection.
 */
public class Manual {

	public static void main (String[] args) {
		try (MyMemoryTMConnector conn = new MyMemoryTMConnector()) {
			System.out.println("Connector settings:");
			System.out.println(conn.getSettingsDisplay());
			System.out.println("\n************************\n");

			conn.setThreshold(10);
			conn.setMaximumHits(20);
			conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
			conn.open();

			System.out.println("Query of a plain text fragment");
			conn.query(new TextFragment("Hello world")); // Return exact match and more
			printResults(conn);

			System.out.println("\n************************\n");

			System.out.println("Query of a text fragment with codes");
			conn.setLanguages(LocaleId.ENGLISH, LocaleId.ITALIAN);
			final TextFragment tf = new TextFragment("Open the ");
			tf.append(TextFragment.TagType.OPENING, "i", "<i>");
			tf.append("Download");
			tf.append(TextFragment.TagType.CLOSING, "i", "</i>");
			tf.append(" window.");
			tf.append(TextFragment.TagType.PLACEHOLDER, null, "<br/>");
			conn.query(tf);
			printResults(conn);

			System.out.println("\n************************\n");

			conn.getParameters().setEmail("john.doe@nosite.com");
			conn.setLanguages(LocaleId.ENGLISH, LocaleId.SPANISH);
			System.out.println("Send the email parameter");
			System.out.println("Connector settings:");
			System.out.println(conn.getSettingsDisplay());
			conn.query(new TextFragment("Open the Download window"));
			printResults(conn);

			System.out.println("\n************************\n");

			conn.getParameters().setKey("non-existing-key");
			System.out.println("Trigger exception with non-existing key");
			System.out.println("Connector settings:");
			System.out.println(conn.getSettingsDisplay());
			conn.query(new TextFragment("Open the Download window"));
		}
	}

	private static void printResults(MyMemoryTMConnector conn) {
		for (int i = 1; conn.hasNext(); i++) {
			QueryResult qr = conn.next();
			System.out.printf("%02d\n", i);
			System.out.println("   Score = " + qr.getCombinedScore());
			System.out.println(" Created = " + qr.creationDate);
			System.out.println("    Type = " + qr.matchType);
			System.out.println("  Source = " + qr.source.toText());
			System.out.println("  Target = " + qr.target.toText());
		}
	}

}
