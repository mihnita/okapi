package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.sf.okapi.lib.xliff2.core.Note.AppliesTo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NotesTest {

	@Test
	public void testSimple () {
		Notes notes = new Notes();
		assertTrue(notes.isEmpty());
		assertEquals(0, notes.size());
		Note note = notes.add(new Note());
		assertFalse(notes.isEmpty());
		assertEquals(1, notes.size());
		assertSame(notes.get(0), note);
	}
	
	@Test
	public void testRemove () {
		Notes notes = new Notes();
		Note note1 = notes.add(new Note("cont1"));
		Note note2 = notes.add(new Note("cont2"));
		assertEquals(2, notes.size());
		notes.remove(0); // note1
		assertEquals(1, notes.size());
		notes.remove(note1); // Should not find it
		assertEquals(1, notes.size());
		notes.remove(note2);
		assertEquals(0, notes.size());
	}
	
	@Test
	public void testSet () {
		Notes notes = new Notes();
		notes.add(new Note("cont1"));
		Note note2 = notes.add(new Note("cont2"));
		assertSame(note2, notes.get(1));
		Note note3 = notes.set(1, new Note());
		assertSame(note3, notes.get(1));
	}
	
	@Test
	public void testCopy () {
		Notes notes1 = new Notes();
		notes1.getExtAttributes().setAttribute("ns", "attr", "val");
		Note n1 = notes1.add(new Note("cont1"));
		n1.setAppliesTo(AppliesTo.SOURCE);
		n1.setCategory("cat1");
		n1.setPriority(5);
		n1.setId("n1");
		Note n2 = notes1.add(new Note("cont2"));
		n2.setAppliesTo(AppliesTo.TARGET);
		n2.setCategory("cat2");
		n2.setPriority(8);
		n2.setId("n2");

		Notes notes2 = new Notes(notes1);
		for ( int i=0; i<notes1.size(); i++ ) {
			Note note1 = notes1.get(i);
			Note note2 = notes2.get(i);
			assertEquals(note1.getAppliesTo(), note2.getAppliesTo());
			assertEquals(note1.getCategory(), note2.getCategory());
			assertEquals(note1.getText(), note2.getText());
			assertEquals(note1.getId(), note2.getId());
			assertEquals(note1.getPriority(), note2.getPriority());
			ExtAttribute ea1 = notes1.getExtAttributes().getAttribute("ns", "attr");
			ExtAttribute ea2 = notes2.getExtAttributes().getAttribute("ns", "attr");
			assertNotSame(ea1, ea2);
			assertEquals(ea1.getValue(), ea2.getValue());
		}
	}
	
}
