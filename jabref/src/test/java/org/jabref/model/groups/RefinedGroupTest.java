package org.jabref.model.groups;

import com.sun.star.util.XSearchable;
import javafx.util.converter.LocalDateStringConverter;
import org.jabref.gui.search.SearchTextField;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RefinedGroupTest {
    Integer from_int = 2019;
    Integer to_int = 2021;
    LocalDate from_date = LocalDate.of(2019, 1 , 1);
    LocalDate to_date = LocalDate.of(2021, 12, 28);
    private RefinedGroup group_int;
    private RefinedGroup group_date;

    @BeforeEach
    void setup(){
        group_date = new RefinedGroup("RefinedGroup", GroupHierarchyType.INDEPENDENT, from_date, to_date, StandardField.DATE);
        group_int = new RefinedGroup("RefinedGroup", GroupHierarchyType.INDEPENDENT, from_int, to_int, StandardField.YEAR);
    }

    @Test
    void contains() {
        //Group Date
        //Inbounds
        BibEntry entry = new BibEntry(StandardEntryType.Dataset);
        entry.setField(StandardField.DATE, "2020-12-02");
        assert group_date.contains(entry);

        //Less than from date
        entry.setField(StandardField.DATE, "2018-12-02");
        assert !group_date.contains(entry);

        //Greater than to date
        entry.setField(StandardField.DATE, "2022-12-02");
        assert !group_date.contains(entry);

        //From frontier
        entry.setField(StandardField.DATE, "2019-01-01");
        assert group_date.contains(entry);

        entry.setField(StandardField.DATE, "2019-01-02");
        assert group_date.contains(entry);

        entry.setField(StandardField.DATE, "2018-12-31");
        assert !group_date.contains(entry);

        //To frontier
        entry.setField(StandardField.DATE, "2021-12-28");
        assert group_date.contains(entry);

        entry.setField(StandardField.DATE, "2021-12-29");
        assert !group_date.contains(entry);

        entry.setField(StandardField.DATE, "2021-12-27");
        assert group_date.contains(entry);

        //Group Int
        //Inbounds
        entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.YEAR, "2020");
        assert group_int.contains(entry);

        //From frontier
        entry.setField(StandardField.YEAR, "2018");
        assert !group_int.contains(entry);

        entry.setField(StandardField.YEAR, "2019");
        assert group_int.contains(entry);

        //To frontier
        entry.setField(StandardField.YEAR, "2021");
        assert group_int.contains(entry);

        entry.setField(StandardField.YEAR, "2022");
        assert !group_int.contains(entry);

        //Checks entry type
        entry.setType(StandardEntryType.Online);
        assert !group_int.contains(entry);
    }

    @Test
    void isDynamic() {
        assert group_date.isDynamic();
        assert group_int.isDynamic();
    }

    @Test
    void deepCopy() {
        //Group Date
        AbstractGroup new_group1 = group_date.deepCopy();
        BibEntry entry = new BibEntry(StandardEntryType.Dataset);
        entry.setField(StandardField.DATE, "2020-12-02");
        assert new_group1.contains(entry);

        AbstractGroup new_group2 = new_group1.deepCopy();
        entry = new BibEntry(StandardEntryType.Dataset);
        entry.setField(StandardField.DATE, "2018-12-02");
        assert !new_group2.contains(entry);

        //Group Int
        new_group1 = group_int.deepCopy();
        entry = new BibEntry(StandardEntryType.Dataset);
        entry.setField(StandardField.YEAR, "2020");
        assert new_group1.contains(entry);

        new_group2 = new_group1.deepCopy();
        entry = new BibEntry(StandardEntryType.Dataset);
        entry.setField(StandardField.YEAR, "2018");
        assert !new_group2.contains(entry);
    }

    @Test
    void isNumberFilter() {
        assert !group_date.isNumberFilter();
        assert group_int.isNumberFilter();
    }

    @Test
    void testEquals() {
        assert group_date.equals(group_date);
        assert !group_date.equals(null);
        assert group_int.equals(group_int);
        assert !group_int.equals(null);
        assert !group_int.equals(group_date);
        assert !group_date.equals(group_int);

        AbstractGroup group_aux = new RefinedGroup("RefinedGroupAux", GroupHierarchyType.REFINING, from_date, to_date, StandardField.DATE);
        assert !group_date.equals(group_aux);

        group_aux = new RefinedGroup("RefinedGroupAux", GroupHierarchyType.INDEPENDENT, LocalDate.of(2020,12,2), to_date, StandardField.DATE);
        assert !group_date.equals(group_aux);

        group_aux = new RefinedGroup("RefinedGroupAux", GroupHierarchyType.INDEPENDENT, from_date, LocalDate.of(2222, 12, 1), StandardField.DATE);
        assert !group_date.equals(group_aux);

        group_aux = new RefinedGroup("RefinedGroupAux", GroupHierarchyType.INDEPENDENT, from_int, to_int, StandardField.YEAR);
        assert !group_int.equals(group_aux);
    }

}
