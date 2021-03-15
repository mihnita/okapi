package net.sf.okapi.filters.openxml;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExcelWorksheetTransUnitPropertyTest
{
    @Test
    public void sanityCheckTest()
    {
        assertEquals("cellReference", ExcelWorksheetTransUnitProperty.CELL_REFERENCE.getKeyName());
        assertEquals("sheetName", ExcelWorksheetTransUnitProperty.SHEET_NAME.getKeyName());
    }

    @Test
    public void getColumnIndexFromCellRefTest()
    {
        assertEquals("A", ExcelWorksheetTransUnitProperty.getColumnIndexFromCellRef("A1"));
        assertEquals("Z", ExcelWorksheetTransUnitProperty.getColumnIndexFromCellRef("Z1000"));
        assertEquals("AA", ExcelWorksheetTransUnitProperty.getColumnIndexFromCellRef("AA1000"));
    }

    @Test
    public void getRowNumberFromCellRefTest()
    {
        assertEquals(0, ExcelWorksheetTransUnitProperty.getColumnIndexFromColumnRef("A"));
        assertEquals(25, ExcelWorksheetTransUnitProperty.getColumnIndexFromColumnRef("Z"));
        assertEquals(26, ExcelWorksheetTransUnitProperty.getColumnIndexFromColumnRef("AA"));
    }

    @Test
    public void getColumnIndexFromColumnRefTest()
    {
        assertEquals(Integer.valueOf(1), ExcelWorksheetTransUnitProperty.getRowNumberFromCellRef("A1"));
        assertEquals(Integer.valueOf(1000), ExcelWorksheetTransUnitProperty.getRowNumberFromCellRef("Z1000"));
        assertEquals(Integer.valueOf(1000), ExcelWorksheetTransUnitProperty.getRowNumberFromCellRef("AA1000"));
    }
}