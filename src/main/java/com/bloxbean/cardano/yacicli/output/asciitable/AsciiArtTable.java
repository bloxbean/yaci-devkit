package com.bloxbean.cardano.yacicli.output.asciitable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AsciiArtTable {

    private static String appendToLength(final Object subject, final int length) {
        final String subjectString = subject == null ? "" : subject.toString();
        int nonANSISubjectStringLength = getNonANSIStringLength(subjectString);
        if (nonANSISubjectStringLength < length) {
            return subjectString + StringUtils.repeat(' ', length - nonANSISubjectStringLength);
        }
        return subjectString;
    }

    private static String prependToLength(final Object subject, final int length) {
        final String subjectString = subject == null ? "" : subject.toString();
        int nonANSISubjectStringLength = getNonANSIStringLength(subjectString);
        if (nonANSISubjectStringLength < length) {
            return StringUtils.repeat(' ', length - nonANSISubjectStringLength) + subjectString;
        }
        return subjectString;
    }

    private String borderCharacters;
    private final List<Object> contentCols;
    private final List<Object> headerCols;
    private final List<Object> headlines;

    private int maxColumnWidth = 80;


    private final List<AsciiArtContentGroup> contentGroups;

    private boolean minimiseHeight = false;
    private final int padding;

    private int tableLength;



    public AsciiArtTable() {
        this(1);
    }

    public AsciiArtTable(final int padding) {
        this(padding, "╔═╤╗║╟─┬╢╪╠╣│╚╧╝┼");
    }

    public AsciiArtTable(final int padding, final String borderCharacters) {
        this.headerCols = new ArrayList<>();
        this.contentCols = new ArrayList<>();
        this.headlines = new ArrayList<>();
        this.contentGroups = new ArrayList<>();
        this.padding = padding;
        this.borderCharacters = borderCharacters;
    }

    public void addContentGroup(AsciiArtContentGroup contentGroup) {
        this.contentGroups.add(contentGroup);
    }

    public void add(final List<Object> contentCols) {
        this.contentCols.addAll(contentCols);
    }

    public void add(final Object... contentCols) {
        add(new ArrayList<>(Arrays.asList(contentCols)));
    }

    public void addHeaderCols(final List<Object> headerCols) {
        this.headerCols.addAll(headerCols);
    }

    public void addHeaderCols(final Object... headerCols) {
        addHeaderCols(new ArrayList<>(Arrays.asList(headerCols)));
    }

    public void addHeadline(final Object headline) {
        this.headlines.add(headline);
    }

    private boolean alignLeft(final List<List<String>> linesContents, final int col) {
        // TODO actual state: left-aligned, if multi lines in this cell. wanted state: left-aligned, if a multi line cell in the row.
        boolean result = false;
        if (linesContents.size() > 1) {
            // are lines > first line all empty?
            for (List<String> lineContents : linesContents) {
                if (linesContents.indexOf(lineContents) != 0 && lineContents.get(col).trim().isEmpty() == false) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void clear() {
        headerCols.clear();
        contentCols.clear();
        headlines.clear();
    }

    private void setTableLength(int tableLength){
        this.tableLength = tableLength;
    }

    private int[] getColWidths() {
        int[] result = new int[headerCols.size()];
        int col = 0;
        while (col < headerCols.size()) {
            int length = getNonANSIStringLength(headerCols.get(col).toString());
            result[col] = Math.min(length, this.maxColumnWidth);
            col++;
        }
        int index = 0;
        while (index < contentCols.size()) {
            col = index % headerCols.size();
            final String content = contentCols.get(index) == null ? "" : contentCols.get(index).toString();
            int nonANSIContentLength = getNonANSIStringLength(content);
            if (nonANSIContentLength > result[col]) {
                result[col] = Math.min(nonANSIContentLength, this.maxColumnWidth);
            }
            index++;
        }
        return result;
    }

    private int[] getContentGroupColWidths(AsciiArtContentGroup contentGroup) {
        int[] result = new int[contentGroup.getHeaderCols().size()];
        int maxColumnWidth = contentGroup.getMaxColumnWidth();
        int col = 0;
        while (col < contentGroup.getHeaderCols().size()) {
            int length = getNonANSIStringLength(contentGroup.getHeaderCols().get(col).toString());
            result[col] = Math.min(length, maxColumnWidth);
            col++;
        }
        int index = 0;
        while (index < contentGroup.getContentCols().size()) {
            col = index % contentGroup.getHeaderCols().size();
            final String content = contentGroup.getContentCols().get(index) == null ? "" : contentGroup.getContentCols().get(index).toString();
            int nonANSIContentLength = getNonANSIStringLength(content);
            if (nonANSIContentLength > result[col]) {
                result[col] = Math.min(nonANSIContentLength, maxColumnWidth);
            }
            index++;
        }
        return result;
    }

    public String getOutput() {
        // prepare data
        while (contentCols.size() % headerCols.size() != 0) {
            contentCols.add("");
        }
        setTableLength(getTableLength());
        // build header
        String result = "";
        if (headlines.isEmpty()) {
            result += borderRow(borderCharacters.charAt(0), borderCharacters.charAt(1), borderCharacters.charAt(3)) + System.lineSeparator();
        } else {
            result += borderRow(borderCharacters.charAt(0), borderCharacters.charAt(1), borderCharacters.charAt(3)) + System.lineSeparator();
            for (Object headline : headlines) {
                result += rowHeadline(headline.toString(), borderCharacters.charAt(4), borderCharacters.charAt(4));
//                if (headlines.indexOf(headline) == headlines.size() - 1) {
//                    result += borderRow(borderCharacters.charAt(10), borderCharacters.charAt(1), borderCharacters.charAt(11)) + System.lineSeparator();
//                } else if (!minimiseHeight) {
//                    result += row(borderCharacters.charAt(5), borderCharacters.charAt(6), borderCharacters.charAt(6), borderCharacters.charAt(8)) + System.lineSeparator();
//                }
            }
        }
        int currentGroupIndex = 0;
        for (AsciiArtContentGroup contentGroup : contentGroups ) {
            currentGroupIndex++;
            while (contentGroup.getContentCols().size() % contentGroup.getHeaderCols().size() != 0) {
                contentGroup.getContentCols().add("");
            }
            if ( contentGroup.getHeadLine() != null ) {
                result += rowHeadline(contentGroup.getHeadLine().toString(), borderCharacters.charAt(4), borderCharacters.charAt(4));
            }
            if ( contentGroup.getContentCols().isEmpty() ){
                result += borderRow(borderCharacters.charAt(10), borderCharacters.charAt(1), borderCharacters.charAt(11)) + System.lineSeparator();
            } else {
                result += contentGroupRow(contentGroup, borderCharacters.charAt(10), borderCharacters.charAt(1), borderCharacters.charAt(2), borderCharacters.charAt(11)) + System.lineSeparator();
            }

            if (contentGroup.outputOfHeaderColsIsRequested()) {
                result += contentGroupRow(contentGroup, contentGroup.getHeaderCols(), borderCharacters.charAt(4), borderCharacters.charAt(12), borderCharacters.charAt(4)) + System.lineSeparator();
                result += contentGroupRow(contentGroup, borderCharacters.charAt(10), borderCharacters.charAt(1), borderCharacters.charAt(9), borderCharacters.charAt(11)) + System.lineSeparator();
            }
            int col = 0;
            while (col < contentGroup.getContentCols().size()) {
                result += contentGroupRow(contentGroup, contentGroup.getContentCols().subList(col, col + contentGroup.getHeaderCols().size()), borderCharacters.charAt(4), borderCharacters.charAt(12), borderCharacters.charAt(4)) + System.lineSeparator();
                col += contentGroup.getHeaderCols().size();
                if (col == contentGroup.getContentCols().size()) {
                    if ( currentGroupIndex == contentGroups.size() ) {
                        result += contentGroupRow(contentGroup, borderCharacters.charAt(13), borderCharacters.charAt(1), borderCharacters.charAt(14), borderCharacters.charAt(15)) + System.lineSeparator();
                    } else {
                        result += contentGroupRow(contentGroup, borderCharacters.charAt(10), borderCharacters.charAt(1), borderCharacters.charAt(14), borderCharacters.charAt(11)) + System.lineSeparator();
                    }
                } else if (!minimiseHeight) {
                    result += contentGroupRow(contentGroup, borderCharacters.charAt(5), borderCharacters.charAt(6), borderCharacters.charAt(16), borderCharacters.charAt(8)) + System.lineSeparator();
                }
            }
        }
        return result;
    }

    private int getTableLength() {

        int tableLength = 0;

        for (AsciiArtContentGroup contentGroup: contentGroups ) {
            final int[] colWidths = getContentGroupColWidths(contentGroup);
            int result = 0;
            for (int colWidth : colWidths) {
                result += colWidth + 2 * padding;
            }
            tableLength =  Math.max(result + colWidths.length + 1, tableLength);
        }

        return tableLength;
    }

    public void minimiseHeight() {
        minimiseHeight = true;
    }

    private boolean outputOfHeaderColsIsRequested() {
        for (Object headerCol : headerCols) {
            if (headerCol.toString().length() > 0) {
                return true;
            }
        }
        return false;
    }

    public void print(final PrintStream printStream) {
        printStream.print(getOutput());
    }

    private String row(final char left, final char middle, final char columnSeparator, final char right) {
        final int[] colWidths = getColWidths();
        String result = left + "";
        int col = 0;
        while (col < headerCols.size()) {
            result += StringUtils.repeat(middle, padding + colWidths[col] + padding);
            col++;
            result += col == headerCols.size() ? right : columnSeparator;
        }
        return result;
    }

    private String borderRow(final char left, final char middle, final char right) {
        final int[] colWidths = getColWidths();
        String result = left + "";
        result += StringUtils.repeat(middle, this.tableLength - 2);
        result += right;
        return result;
    }

    private String row(final List<Object> contents, final char left, final char columnSeparator, final char right) {
        final int[] colWidths = getColWidths();
        String result = "";
        List<List<String>> linesContents = splitToMaxLength(null, contents, this.maxColumnWidth);
        for (List<String> lineContents : linesContents) {
            int col = 0;
            result += left;
            while (col < headerCols.size()) {
                if (alignLeft(linesContents, col)) {
                    result += StringUtils.repeat(' ', padding);
                    result += appendToLength(lineContents.get(col), padding + colWidths[col]);
                } else {
                    result += prependToLength(lineContents.get(col), padding + colWidths[col]);
                    result += StringUtils.repeat(' ', padding);
                }
                col++;
                result += col == headerCols.size() ? right : columnSeparator;
            }
            if (linesContents.indexOf(lineContents) != linesContents.size() - 1) {
                result += System.lineSeparator();
            }
        }
        return result;
    }

    private String contentGroupRow(final AsciiArtContentGroup contentGroup, final char left, final char middle, final char columnSeparator, final char right) {
        final int[] colWidths = getContentGroupColWidths(contentGroup);
        String result = left + "";
        int col = 0;

        while (col < contentGroup.getHeaderCols().size()) {

            result += StringUtils.repeat(middle, padding + colWidths[col] + padding);
            col++;
            if ( col == contentGroup.getHeaderCols().size() ){
                int size = result.length();
                if ( size < this.tableLength ){
                    result += StringUtils.repeat(middle, this.tableLength - size - 1);
                    result += right;
                }
            } else {
                result += columnSeparator;
            }

        }
        return result;
    }
    private String contentGroupRow(final AsciiArtContentGroup contentGroup, final List<Object> contents, final char left, final char columnSeparator, final char right) {
        final int[] colWidths = getContentGroupColWidths(contentGroup);
        final int[] separatorIndexes = getSeparatorIndexes(colWidths);
        String result = "";
        StringBuilder rowResult = new StringBuilder();
        List<List<String>> linesContents = splitToMaxLength(contentGroup, contents, contentGroup.getMaxColumnWidth());
        for (List<String> lineContents : linesContents) {
            int col = 0;
            result += left;
            while (col < contentGroup.getHeaderCols().size()) {
                if (contentGroup.isAlignLeft()) {
                    result += StringUtils.repeat(' ', padding);
                    result += appendToLength(lineContents.get(col), padding + colWidths[col]);
                } else {
                    result += prependToLength(lineContents.get(col), padding + colWidths[col]);
                    result += StringUtils.repeat(' ', padding);
                }
                col++;
                if ( col == contentGroup.getHeaderCols().size() ){
                    int currentRightIndex = getNonANSIStringLength(result);
                    int rightRequiredIndex = Math.max(this.tableLength, separatorIndexes[col-1]);
                    if ( currentRightIndex < this.tableLength ){
                        result += StringUtils.repeat(" ", rightRequiredIndex - currentRightIndex - 1);
                        result += right;
                    }
                } else {
                    int currentRightIndex = getNonANSIStringLength(result);
                    int currentRequiredIndex = separatorIndexes[col-1];
                    if ( currentRightIndex < currentRequiredIndex ){
                        result += StringUtils.repeat(' ', currentRequiredIndex - currentRightIndex );
                    }
                    result += columnSeparator;
                }

            }
            if (linesContents.indexOf(lineContents) != linesContents.size() - 1) {
                result += System.lineSeparator();
            }

            rowResult.append(result);
            result = "";
        }

        return rowResult.toString();
    }

    private int[] getSeparatorIndexes(int[] colWidths){
        int[] separatorIndexes = new int[colWidths.length];
        int lastIndex = 0;
        for (int i = 0; i < colWidths.length; i++) {
            lastIndex += 2*padding + colWidths[i];
            separatorIndexes[i] = lastIndex;
        }
        return separatorIndexes;
    }

    private String rowHeadline(final String headline, final char left, final char right) {
        final int tableLength = getTableLength();
        final int contentWidth = tableLength - (2 * padding) - 2;
        // FIXME a single word could be longer than the table
        // split into headline rows
        final List<String> headlineLines = new ArrayList<>();
        final String[] headlineWords = headline.split(" ");
        List<String> rowWords = new ArrayList<>();
        for (String headlineWord : headlineWords) {
            String nonANSIHeadlineWord = getNonANSIString(headlineWord);
            if ((StringUtils.join(rowWords, ' ') + ' ' + nonANSIHeadlineWord).length() > contentWidth) {
                headlineLines.add(StringUtils.join(rowWords, ' '));
                rowWords.clear();
            }
            rowWords.add(headlineWord);
        }
        if (!rowWords.isEmpty()) {
            headlineLines.add(StringUtils.join(rowWords, ' '));
        }
        // build result
        String result = "";
        for (String headlineLine : headlineLines) {
            int nonANSIHeadlineLength = getNonANSIStringLength(headlineLine);
            int ansiDiff = headlineLine.length() - nonANSIHeadlineLength;
            result += left + StringUtils.repeat(' ', padding) + StringUtils.rightPad(headlineLine, tableLength - padding - 2 + ansiDiff) + right + System.lineSeparator();
        }
        return result;
    }

    private static int getNonANSIStringLength(String data){
        return data.replaceAll("(\\x9B|\\x1B\\[)[0-?]*[ -\\/]*[@-~]", "").length();
    }

    private static String getNonANSIString(String data){
        return data.replaceAll("(\\x9B|\\x1B\\[)[0-?]*[ -\\/]*[@-~]", "");
    }

    public void setBorderCharacters(final String borderCharacters) {
        this.borderCharacters = borderCharacters;
    }

    public void setMaxColumnWidth(final int maxColumnWidth) {
        this.maxColumnWidth = maxColumnWidth;
    }

    public void setNoHeaderColumns(int withColumns) {
        this.headerCols.clear();
        while (withColumns-- > 0) {
            this.headerCols.add("");
        }
    }

    // XXX omg ...
    private List<List<String>> splitToMaxLength(AsciiArtContentGroup contentGroup, final List<Object> subjects, final int maxLength) {
        final List<List<String>> result = new ArrayList<>();
        // get the count of rows in a cell must be used for given subjects
        int countRows = 1;
        for (Object subject : subjects) {
            int nonANSISubjectLength = getNonANSIStringLength(subject.toString());
            if (nonANSISubjectLength > maxLength) {
                // FIXME a single word could be longer than allowed
                int countRowsForThisSubject = 1;
                final String[] words = subject.toString().split(" ");
                final List<String> columnWords = new ArrayList<>();
                for (String word : words) {
                    String nonANSIWord = getNonANSIString(word);
                    if ((StringUtils.join(columnWords, ' ') + ' ' + nonANSIWord).length() > maxLength) {
                        countRowsForThisSubject++;
                        columnWords.clear();
                    } else {
                        columnWords.add(word);
                    }
                }
                if (countRows < countRowsForThisSubject) {
                    countRows = countRowsForThisSubject;
                }
            }
        }
        // build the cellContents
        final List<List<String>> cellContents = new ArrayList<>();
        for (Object subject : subjects) {
            String content = subject.toString();
            final List<String> cellContentLines = new ArrayList<>();
            if (content.length() > maxLength) {
                final String[] words = content.split(" ");
                final List<String> cellContentLineWords = new ArrayList<>();
                for (String word : words) {
                    if ((StringUtils.join(cellContentLineWords, ' ') + ' ' + word).length() > maxLength) {
                        if ( ! cellContentLineWords.isEmpty() ) {
                            String cellContentLine = StringUtils.join(cellContentLineWords, ' ');
                            if (cellContentLine.length() > maxLength) {
                                cellContentLine = cellContentLine.substring(0, maxLength - 4);
                                cellContentLine += "...";
                            }
                            cellContentLines.add(cellContentLine);
                            cellContentLineWords.clear();
                        }
                    }
                    cellContentLineWords.add(word);

                }
                String cellContentLine = StringUtils.join(cellContentLineWords, ' ');
                if (cellContentLine.length() > maxLength) {
                    cellContentLine = cellContentLine.substring(0, maxLength - 4);
                    cellContentLine += "...";
                }
                cellContentLines.add(cellContentLine);
            } else {
                cellContentLines.add(content);
            }
            while (cellContentLines.size() < countRows) {
                cellContentLines.add("");
            }
            cellContents.add(cellContentLines);
        }
        // we have (pseudocode):
        // cellContents.get(0) == ["peter", ""]
        // cellContents.get(1) == ["a very", "long text"]
        // but we need (pseudocode):
        // result.add("peter", "a very")
        // result.add("", "long text")
        int row = 0;
        while (row < countRows) {
            final List<String> lineOverColumns = new ArrayList<>();
            for (int col = 0; col < contentGroup.getHeaderCols().size(); col++) {
                lineOverColumns.add(cellContents.get(col).get(row));
            }
            result.add(lineOverColumns);
            row++;
        }
        return result;
    }
}
