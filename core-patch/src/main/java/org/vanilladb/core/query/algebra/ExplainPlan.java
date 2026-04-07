package org.vanilladb.core.query.algebra;

import org.vanilladb.core.sql.Schema;
import org.vanilladb.core.sql.Type;
import org.vanilladb.core.storage.metadata.statistics.Histogram;
import org.vanilladb.core.query.algebra.materialize.SortPlan;
import org.vanilladb.core.query.algebra.materialize.GroupByPlan;

public class ExplainPlan implements Plan {
    private Plan p;
    private Schema mySchema;
	private Histogram Hist;

    public ExplainPlan(Plan p) {
        this.p = p;
        this.mySchema = new Schema();
        this.mySchema.addField("query-plan", Type.VARCHAR(70));
    }

    @Override
    public Scan open() {
        return new ExplainScan(this);
    }

    @Override
    public Schema schema() {
        return mySchema;
    }

    @Override
    public long blocksAccessed() {
        return p.blocksAccessed();
    }

    @Override
    public long recordsOutput() {
        return p.recordsOutput();
    }

    public String getTreeString() {
        StringBuilder sb = new StringBuilder();
		sb.append("\n");
        buildTree(p, sb, 0);
		sb.append("\n\nActual #recs: ").append(getActualRecs(p));
        return sb.toString();
    }

	private long getActualRecs(Plan currentPlan) {
    if (currentPlan instanceof ProjectPlan) {
        Plan child = getChildPlan(currentPlan);
        return (child != null) ? child.recordsOutput() : 0;
    }
    return currentPlan.recordsOutput();
	}


	private Plan getChildPlan(Plan currentPlan) {
    if (currentPlan instanceof ProjectPlan) {
        return ((ProjectPlan) currentPlan).getUnderlyingPlan();
    } else if (currentPlan instanceof SelectPlan) {
        return ((SelectPlan) currentPlan).getUnderlyingPlan();
    } else if (currentPlan instanceof SortPlan) {
        return ((SortPlan) currentPlan).getUnderlyingPlan();
    } else if (currentPlan instanceof GroupByPlan) {
        return ((GroupByPlan) currentPlan).getUnderlyingPlan();
    }
    return null;
	}	

    private void buildTree(Plan currentPlan, StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }

        sb.append("->")
          .append(currentPlan.getClass().getSimpleName())
          .append(" (#blks=").append(currentPlan.blocksAccessed())
          .append(", #recs=").append(currentPlan.recordsOutput())
          .append(")\n");
        if (currentPlan instanceof ProjectPlan) {
            buildTree(((ProjectPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof SelectPlan) {
            buildTree(((SelectPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof SortPlan) {
            buildTree(((SortPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof GroupByPlan) {
            buildTree(((GroupByPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof ProductPlan) {
            buildTree(((ProductPlan) currentPlan).getLeftPlan(), sb, level + 1);
            buildTree(((ProductPlan) currentPlan).getRightPlan(), sb, level + 1);
        }
    }

	public Histogram histogram() {
		return Hist;
	}

}