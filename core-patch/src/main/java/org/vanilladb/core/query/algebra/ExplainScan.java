package org.vanilladb.core.query.algebra;

import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.sql.VarcharConstant;

public class ExplainScan implements Scan {
    private String planStr;
    private boolean isRead = false;

    public ExplainScan(ExplainPlan p) {
        this.planStr = p.getTreeString();
    }

    @Override
    public void beforeFirst() {
        isRead = false;
    }

    @Override
    public boolean next() {
        if (!isRead) {
            isRead = true;
            return true; 
        }
        return false;
    }

    @Override
    public Constant getVal(String fldName) {
        if (fldName.equals("query-plan")) {
            return new VarcharConstant(planStr);
        }
        throw new RuntimeException("Field not found: " + fldName);
    }

    @Override
    public void close() {
        
    }

    @Override
    public boolean hasField(String fldName) {
        return fldName.equals("query-plan");
    }
}