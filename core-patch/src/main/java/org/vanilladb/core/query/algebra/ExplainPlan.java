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
        // 定義 EXPLAIN 結果只會有一個欄位叫做 query-plan
        this.mySchema.addField("query-plan", Type.VARCHAR(70));
    }

    @Override
    public Scan open() {
        // 傳遞自己 (this) 入去，等 Scan 可以攞到 TreeString
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

    /**
     * 核心遞迴方法：用嚟產生好似 Sample Output 咁樣嘅樹狀字串
     */
    public String getTreeString() {
        StringBuilder sb = new StringBuilder();
		sb.append("\n");
        buildTree(p, sb, 0);
		sb.append("\n\nActual #recs: ").append(getActualRecs(p));
        return sb.toString();
    }

	private long getActualRecs(Plan currentPlan) {
    // 通常 ProjectPlan 嘅下一層就係經過過濾/排序後嘅最終紀錄數
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
        // 1. 處理縮排 (Indentation)
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }

        // 2. 印出當前節點資訊
        sb.append("->")
          .append(currentPlan.getClass().getSimpleName())
          .append(" (#blks=").append(currentPlan.blocksAccessed())
          .append(", #recs=").append(currentPlan.recordsOutput())
          .append(")\n");

        // 3. 遞迴處理子節點 (根據 VanillaDB 唔同嘅 Plan 類型)
        // 註：你需要根據你 Project 嘅繼承關係嚟轉型 (Type Casting)
        if (currentPlan instanceof ProjectPlan) {
            buildTree(((ProjectPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof SelectPlan) {
            buildTree(((SelectPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof SortPlan) {
            buildTree(((SortPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof GroupByPlan) {
            buildTree(((GroupByPlan) currentPlan).getUnderlyingPlan(), sb, level + 1);
        } else if (currentPlan instanceof ProductPlan) {
            // ProductPlan 通常有兩邊 (Left/Right)
            buildTree(((ProductPlan) currentPlan).getLeftPlan(), sb, level + 1);
            buildTree(((ProductPlan) currentPlan).getRightPlan(), sb, level + 1);
        }
        // TablePlan 係最底層，唔需要再遞迴
    }

	public Histogram histogram() {
		return Hist;
	}

}