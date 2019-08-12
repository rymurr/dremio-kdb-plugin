/*
 * Copyright (C) 2017-2019 UBS Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.extras.plugins.kdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;

import com.dremio.connector.ConnectorException;
import com.dremio.connector.metadata.DatasetHandle;
import com.dremio.connector.metadata.DatasetMetadata;
import com.dremio.connector.metadata.DatasetSplit;
import com.dremio.connector.metadata.DatasetStats;
import com.dremio.connector.metadata.EntityPath;
import com.dremio.connector.metadata.PartitionChunk;
import com.dremio.connector.metadata.PartitionChunkListing;
import com.dremio.exec.planner.cost.ScanCostFactor;
import com.dremio.exec.record.BatchSchema;
import com.dremio.exec.record.SchemaBuilder;
import com.dremio.extras.plugins.kdb.exec.KdbSchema;
import com.dremio.extras.plugins.kdb.exec.KdbTable;
import com.dremio.extras.plugins.kdb.exec.c;
import com.dremio.extras.plugins.kdb.proto.KdbReaderProto;
import com.dremio.service.namespace.NamespaceKey;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * kdb table
 */
public class KdbTableDefinition implements DatasetHandle {
    private final EntityPath tableName;
    private KdbSchema connection;
    private DatasetMetadata datasetMetadata;
    private List<PartitionChunk> partitionChunks;

    public KdbTableDefinition(String name, EntityPath tableName, KdbSchema connection) {
        this.tableName = tableName;
        this.connection = connection;
    }

    private Pair<BatchSchema, List<String>> getSchema() {
        SchemaBuilder builder = BatchSchema.newBuilder();
        KdbTable table = connection.getTable(tableName.getComponents().get(1));
        List<String> symbolList = Lists.newArrayList();
        RelDataType rowType = table.getRowType(new JavaTypeFactoryImpl());
        for (String col : rowType.getFieldNames()) {
            RelDataTypeField field = rowType.getField(col, false, false);
            RelDataType value = field.getValue();
            Field arrowField;
            if (value.getSqlTypeName().equals(SqlTypeName.OTHER)) {
                Class clazz = ((RelDataTypeFactoryImpl.JavaType) (value)).getJavaClass();
                arrowField = KdbSchemaConverter.getArrowFieldFromJavaClass(col, clazz);
            } else {
                SqlTypeName typeName = field.getValue().getSqlTypeName();
                arrowField = KdbSchemaConverter.getArrowFieldFromJdbcType(col, typeName);
                if (SqlTypeName.SYMBOL.equals(typeName)) {
                    symbolList.add(col);
                }
            }

            builder.addField(arrowField);
        }
        return Pair.of(builder.build(), symbolList);
    }

    private KdbReaderProto.KdbTableXattr getXattr() {
        Pair<BatchSchema, List<String>> schemaList = getSchema();
//        BatchSchema schema = schemaList.left;
        boolean isPartitioned = isPartitioned();
        KdbReaderProto.KdbTableXattr.Builder builder = KdbReaderProto.KdbTableXattr.newBuilder()
                .addAllSymbolList(schemaList.right)
                .setVersion(version())
                .setIsPartitioned(isPartitioned);
        if (isPartitioned) {
            builder.setPartitionColumn("date"); //todo
        }
        return builder.build();
    }

    private void populateSplits(List<String> partitions) {
        List<PartitionChunk> splits = new ArrayList<>();
        if (partitions == null || partitions.isEmpty()) {
            splits.add(PartitionChunk.of(DatasetSplit.of(11, 1)));
        } else {
            //todo build metadata store to hold count per partition
            //split into datasets based on partitions and make sure Dremio allocates the partition ranges to executors sensibly
            splits.add(PartitionChunk.of(DatasetSplit.of(11, 1)));
        }
        partitionChunks = Lists.newArrayList(splits);
    }

    private void buildIfNecessary(BatchSchema oldSchema) {

        Pair<BatchSchema, List<String>> schemaList = getSchema();
        BatchSchema schema = schemaList.left;

        List<String> partitions = partitionColumns();
        populateSplits(partitions);

        KdbReaderProto.KdbTableXattr extended = getXattr();
        datasetMetadata = DatasetMetadata.of(
                DatasetStats.of(1000L, false, ScanCostFactor.OTHER.getFactor()), //todo
                schema,
                partitions,
                sortedColumns(),
                extended::writeTo);
    }

    private List<String> sortedColumns() {
        Map<String, String> attrs = connection.getTable(tableName.getComponents().get(1)).getAttrs(new JavaTypeFactoryImpl());
        List<String> cols = Lists.newArrayList();
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            if (e.getValue().contains("s")) {
                cols.add(e.getKey());
            }
        }
        return cols;
    }

    private double version() {
        try {
            double version = connection.getVersion();
            return version;
        } catch (IOException | c.KException e) {
            return -1;
        }
    }

    private boolean isPartitioned() {
        try {
            boolean version = connection.getPartitioned(tableName.getComponents().get(1));
            return version;
        } catch (IOException | c.KException e) {
            return false;
        }
    }

    private List<String> partitionColumns() {
        Map<String, String> attrs = connection.getTable(tableName.getComponents().get(1)).getAttrs(new JavaTypeFactoryImpl());
        List<String> cols = Lists.newArrayList();
        if (attrs.keySet().contains("date")) {
            cols.add("date");//todo assuming date partition (safe for most cases) and it goes first!
        }
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            if (e.getValue().contains("p")) {
                cols.add(e.getKey());
            }
        }
        return cols;
    }

    public DatasetMetadata getDatasetMetadata(BatchSchema oldSchema) throws ConnectorException {
        buildIfNecessary(oldSchema);

        return datasetMetadata;
    }

    public PartitionChunkListing listPartitionChunks(BatchSchema oldSchema) throws ConnectorException {
        buildIfNecessary(oldSchema);

        return () -> partitionChunks.iterator();
    }

    @Override
    public EntityPath getDatasetPath() {
        return tableName;
    }
}
