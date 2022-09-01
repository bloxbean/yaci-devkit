package com.bloxbean.cardano.yacicli.output.ansitable;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class AsciiArtContentGroup {

    private final Object headLine;

    private final List<Object> headerCols;

    private int maxColumnWidth = 80;

    private boolean alignLeft = true;

    private final List<Object> contentCols;

    public AsciiArtContentGroup() {
        this(null);
    }

    public AsciiArtContentGroup(Object headLine) {
        this.headLine = headLine;
        this.headerCols = new ArrayList<>();
        this.contentCols = new ArrayList<>();
    }

    public void addList(final List<Object> contentCols) {
        this.contentCols.addAll(contentCols);
    }

    public void add(final Object... contentCols) {
        addList(new ArrayList<>(Arrays.asList(contentCols)));
    }

    public boolean isAlignLeft() {
        return alignLeft;
    }

    public void setAlignLeft(boolean alignLeft) {
        this.alignLeft = alignLeft;
    }

    public int getMaxColumnWidth(){
        return this.maxColumnWidth;
    }

    public void setMaxColumnWidth(int width){
        this.maxColumnWidth = width;
    }

    public void addHeaderCols(final List<Object> headerCols) {
        this.headerCols.addAll(headerCols);
    }

    public void addHeaderCols(final Object... headerCols) {
        addHeaderCols(new ArrayList<>(Arrays.asList(headerCols)));
    }

    public boolean outputOfHeaderColsIsRequested() {
        for (Object headerCol : headerCols) {
            if (headerCol.toString().length() > 0) {
                return true;
            }
        }
        return false;
    }

    public void setNoHeaderColumns(int withColumns) {
        this.headerCols.clear();
        while (withColumns-- > 0) {
            this.headerCols.add("");
        }
    }

}
