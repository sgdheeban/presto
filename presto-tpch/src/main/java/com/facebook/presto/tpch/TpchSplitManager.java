/*
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
 */
package com.facebook.presto.tpch;

import com.facebook.presto.spi.ConnectorSplitManager;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.Node;
import com.facebook.presto.spi.NodeManager;
import com.facebook.presto.spi.Partition;
import com.facebook.presto.spi.PartitionResult;
import com.facebook.presto.spi.Split;
import com.facebook.presto.spi.SplitSource;
import com.facebook.presto.spi.TableHandle;
import com.facebook.presto.spi.TupleDomain;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class TpchSplitManager
        implements ConnectorSplitManager
{
    private final String connectorId;
    private final NodeManager nodeManager;
    private final int splitsPerNode;

    public TpchSplitManager(String connectorId, NodeManager nodeManager, int splitsPerNode)
    {
        this.connectorId = connectorId;
        this.nodeManager = nodeManager;
        checkArgument(splitsPerNode > 0, "splitsPerNode must be at least 1");
        this.splitsPerNode = splitsPerNode;
    }

    @Override
    public String getConnectorId()
    {
        return connectorId;
    }

    @Override
    public boolean canHandle(TableHandle handle)
    {
        return handle instanceof TpchTableHandle && ((TpchTableHandle) handle).getConnectorId().equals(connectorId);
    }

    @Override
    public PartitionResult getPartitions(TableHandle table, TupleDomain tupleDomain)
    {
        ImmutableList<Partition> partitions = ImmutableList.<Partition>of(new TpchPartition((TpchTableHandle) table));
        return new PartitionResult(partitions, tupleDomain);
    }

    @Override
    public SplitSource getPartitionSplits(TableHandle table, List<Partition> partitions)
    {
        checkNotNull(partitions, "partitions is null");
        if (partitions.isEmpty()) {
            return new FixedSplitSource(connectorId, ImmutableList.<Split>of());
        }

        Partition partition = Iterables.getOnlyElement(partitions);
        checkArgument(partition instanceof TpchPartition, "Partition must be a tpch partition");
        TpchTableHandle tableHandle = ((TpchPartition) partition).getTable();

        Set<Node> nodes = nodeManager.getActiveDatasourceNodes(connectorId);
        checkState(!nodes.isEmpty(), "No TPCH nodes available: Add '%s' to the datasources property of each worker node", connectorId);

        int totalParts = nodes.size() * splitsPerNode;
        int partNumber = 0;

        // Split the data using split and skew by the number of nodes available.
        ImmutableList.Builder<Split> splits = ImmutableList.builder();
        for (Node node : nodes) {
            for (int i = 0; i < splitsPerNode; i++) {
                splits.add(new TpchSplit(tableHandle, partNumber++, totalParts, ImmutableList.of(node.getHostAndPort())));
            }
        }
        return new FixedSplitSource(connectorId, splits.build());
    }

    public static class TpchPartition
            implements Partition
    {
        private final TpchTableHandle table;

        public TpchPartition(TpchTableHandle table)
        {
            this.table = checkNotNull(table, "table is null");
        }

        public TpchTableHandle getTable()
        {
            return table;
        }

        @Override
        public String getPartitionId()
        {
            return table.getTableName();
        }

        @Override
        public TupleDomain getTupleDomain()
        {
            return TupleDomain.all();
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper(this)
                    .add("table", table)
                    .toString();
        }
    }
}
