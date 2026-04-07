package org.vanilladb.core.query.algebra;

import org.vanilladb.core.sql.Constant;
import org.vanilladb.core.sql.VarcharConstant;

public class ExplainScan implements Scan {
    private Scan underlyingScan;
    private ExplainPlan plan;
    private String resultRecord = null;
    private boolean hasAccessed = false;

    public ExplainScan(Scan s, ExplainPlan p) {
        this.underlyingScan = s;
        this.plan = p;
    }

    @Override
    public boolean next() {
        if (hasAccessed) return false;

        long actualRecs = 0;
        underlyingScan.beforeFirst();
        while (underlyingScan.next()) {
            actualRecs++;
        }
        underlyingScan.close();
        
        StringBuilder sb = new StringBuilder();
        sb.append(plan.getTreeString());
        sb.append("\nActual #recs: ").append(actualRecs);
        
        resultRecord = sb.toString();
        hasAccessed = true;
        return true;
    }

    @Override
    public Constant getVal(String fldName) {
        if (fldName.equals("query-plan")) {
            return new VarcharConstant(resultRecord);
        }
        throw new RuntimeException("field not found");
    }

    @Override
    public void close() { underlyingScan.close(); }
    @Override
    public void beforeFirst() { hasAccessed = false; }
    @Override
    public boolean hasField(String fldName) { return fldName.equals("query-plan"); }
}