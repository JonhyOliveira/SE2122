package org.jabref.model.groups;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;


public class RefinedGroup extends AbstractGroup{
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer fromNumber;
    private Integer toNumber;
    private Field searchField;

    public RefinedGroup(String name, GroupHierarchyType context, Object from, Object to, Field searchField){
        super(name, context);
        this.searchField = searchField;
        if(searchField.isNumeric()){
            this.fromNumber = (Integer) from;
            this.toNumber = (Integer) to;
        }
        else if(searchField.getProperties().contains(FieldProperty.DATE)){
            this.fromDate = (LocalDate) from;
            this.toDate = (LocalDate) to;
        } else {
            throw new IllegalArgumentException("Wrong field type for Refined Group");
        }
    }

    @Override
    public boolean contains(BibEntry entry) {
        boolean withinLower = false;
        boolean withinUpper = false;
        if(!entry.hasField(searchField)){
            return false;
        }
        if(searchField.isNumeric()){
            Double value;
            try {
                value = Double.parseDouble(entry.getField(searchField).orElse(""));
            } catch (NumberFormatException e){
                return false;
            }
            withinLower = fromNumber == null || value.compareTo(fromNumber.doubleValue()) >= 0;
            withinUpper = toNumber == null || value.compareTo(toNumber.doubleValue()) <= 0;
        } else{
            LocalDate value;
            try{
                value = LocalDate.parse(entry.getField(searchField).orElse(""));
            } catch (DateTimeParseException e){
                return false;
            }
            withinLower = fromDate == null || value.compareTo(fromDate) >= 0;
            withinUpper = toDate == null || value.compareTo(toDate) <= 0;
        }
        return withinLower && withinUpper;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public AbstractGroup deepCopy() {
        RefinedGroup copy = null;
        if(searchField.isNumeric()) {
            copy = new RefinedGroup(getName(), getHierarchicalContext(), fromNumber, toNumber, searchField);
            return copy;
        } else{
            copy = new RefinedGroup(getName(), getHierarchicalContext(), fromDate, toDate, searchField);
            return copy;
        }
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public Integer getFromNumber() {
        return fromNumber;
    }

    public Integer getToNumber() {
        return toNumber;
    }

    public Field getSearchField() {
        return searchField;
    }

    public boolean isNumberFilter(){
        return searchField.isNumeric();
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        RefinedGroup that = (RefinedGroup) o;
        return Objects.equals(this.searchField, that.searchField)
                && Objects.equals(this.toDate, that.toDate)
                && Objects.equals(this.fromDate, that.fromDate)
                && Objects.equals(this.fromNumber, that.fromNumber)
                && Objects.equals(this.toNumber, that.toNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                getSearchField(),
                getFromDate(),
                getToDate(),
                getFromNumber(),
                getToNumber());
    }
}
