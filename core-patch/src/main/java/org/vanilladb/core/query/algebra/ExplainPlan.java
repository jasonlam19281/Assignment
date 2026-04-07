/*******************************************************************************
 * Copyright 2016, 2017 vanilladb.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.vanilladb.core.query.algebra;

import org.vanilladb.core.sql.Type;
import org.vanilladb.core.server.VanillaDb;
import org.vanilladb.core.sql.Schema;
import org.vanilladb.core.storage.metadata.TableInfo;
import org.vanilladb.core.storage.metadata.TableNotFoundException;
import org.vanilladb.core.storage.metadata.statistics.Histogram;
import org.vanilladb.core.storage.metadata.statistics.TableStatInfo;
import org.vanilladb.core.storage.tx.Transaction;

/**
 * The {@link Plan} class corresponding to a table.
 */
public class ExplainPlan implements Plan {
    private Plan p;
    private Schema schema;

	/**
	 * Creates a leaf node in the query tree corresponding to the specified
	 * table.
	 * 
	 * @param tblName
	 *            the name of the table
	 * @param tx
	 *            the calling transaction
	 */
	public ExplainPlan(Plan p) {
		this.p = p;
        this.schema = new Schema();
        this.schema.addField("query-plan", Type.VARCHAR(500));
	}

	/**
	 * Creates a table scan for this query.
	 * 
	 * @see Plan#open()
	 */
	@Override
	public Scan open() {
		return new ExplainScan(p.open(),this);
	}

    public String getTreeString(){
        return TreeString(p, 0);
    }

    private String TreeString(Plan p, int level){
        StringBuilder b = new StringBuilder();
        for(int i=0;i<level;i++) b.append('\t');
        b.append("->")
        .append(p.getClass().getSimpleName())
        .append(" (#blks=").append(p.blocksAccessed())
        .append(", #recs=").append(p.recordsOutput())
        .append(")\n");

        return b.toString();
    }

	/**
	 * Estimates the number of block accesses for the table, which is obtainable
	 * from the statistics manager.
	 * 
	 * @see Plan#blocksAccessed()
	 */
	@Override
	public long blocksAccessed() {
		return p.blocksAccessed();
	}
    /** 
    * @see Plan#histogram()
	*/
	@Override
	public Histogram histogram() {
		return p.histogram();
	}
	/**
	 * Determines the schema of the table, which is obtainable from the catalog
	 * manager.
	 * 
	 * @see Plan#schema()
	 */
	@Override
	public Schema schema() {
		return p.schema();
	}

	@Override
	public long recordsOutput() {
		return 1;
	}
}
