package com.bkm.excel;

public class ConversionRequest {
    private final int rowIndex;
    private final String roadNumber;
    private final int startMeter;
    private final String direction;
    private final String startBlockNumber;

    public ConversionRequest(int rowIndex, String roadNumber, int startMeter, String direction,
            String startBlockNumber) {
        this.rowIndex = rowIndex;
        this.roadNumber = roadNumber;
        this.startMeter = startMeter;
        this.direction = direction;
        this.startBlockNumber = startBlockNumber;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String getRoadNumber() {
        return roadNumber;
    }

    public int getStartMeter() {
        return startMeter;
    }

    public String getDirection() {
        return direction;
    }

    public String getStartBlockNumber() {
        return startBlockNumber;
    }
}
