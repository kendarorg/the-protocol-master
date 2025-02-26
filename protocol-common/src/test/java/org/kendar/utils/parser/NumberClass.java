package org.kendar.utils.parser;

import java.math.BigDecimal;

public class NumberClass {
    public NumberClass(int intValue, double doubleValue, float floatValue, BigDecimal bigDecimalValue, boolean booleanValue) {
        this.intValue = intValue;
        this.doubleValue = doubleValue;
        this.floatValue = floatValue;
        this.bigDecimalValue = bigDecimalValue;
        this.booleanValue = booleanValue;
    }

    private int intValue;
    private double doubleValue;
    private float floatValue;
    private BigDecimal bigDecimalValue;
    private boolean booleanValue;

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(float floatValue) {
        this.floatValue = floatValue;
    }

    public BigDecimal getBigDecimalValue() {
        return bigDecimalValue;
    }

    public void setBigDecimalValue(BigDecimal bigDecimalValue) {
        this.bigDecimalValue = bigDecimalValue;
    }
}
