package iped.app.ui.filters;

import java.io.IOException;
import java.util.function.Predicate;

import iped.exception.ParseException;
import iped.exception.QueryNodeException;
import iped.search.IMultiSearchResult;

/*
 * A PreQueryValueFilter that has a predicate to check if the field value starts with the 
 * defined value.
 * 
 * @author Patrick Dalla Bernardina
 */
public class StartsWithFilter extends PreQueryValueFilter {
    public StartsWithFilter(String field, String value) {
        super(field, value, new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return t.startsWith(value);
            }
        });
        this.queryStr = field + ":" + value + "*";
    }

    public String toString() {
        return field + "^=\"" + value + "\"";
    }
}