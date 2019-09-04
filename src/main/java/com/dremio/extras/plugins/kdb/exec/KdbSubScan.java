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
package com.dremio.extras.plugins.kdb.exec;

import java.util.List;

import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.catalog.StoragePluginId;
import com.dremio.exec.physical.base.OpProps;
import com.dremio.exec.physical.base.SubScanWithProjection;
import com.dremio.exec.record.BatchSchema;
import com.dremio.service.namespace.dataset.proto.PartitionProtobuf;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableList;

import io.protostuff.ByteString;

/**
 * bean to turn queries into subjobs
 */
@JsonTypeName("kdb-sub-scan")
public class KdbSubScan extends SubScanWithProjection {

    @JsonIgnore
    private List<PartitionProtobuf.DatasetSplit> splits;

    private final String sql;
    private final StoragePluginId pluginId;
    private final ByteString extendedProperty;
    private final List<String> partitionColumns;
    private final int batchSize;

    @JsonCreator
    public KdbSubScan(
            @JsonProperty("props") OpProps props,
            @JsonProperty("pluginId") StoragePluginId pluginId,
            @JsonProperty("columns") List<SchemaPath> columns,
            @JsonProperty("tableSchemaPath") List<String> tableSchemaPath,
            @JsonProperty("fullSchema") BatchSchema fullSchema,
            @JsonProperty("extendedProperty") ByteString extendedProperty,
            @JsonProperty("partitionColumns") List<String> partitionColumns,
            @JsonProperty("sql") String sql,
            @JsonProperty("batchSize") int batchSize
     ) {
        super(props, fullSchema, tableSchemaPath, columns);
        this.sql = sql;
        this.pluginId = pluginId;
        this.extendedProperty = extendedProperty;
        this.partitionColumns = partitionColumns != null ? ImmutableList.copyOf(partitionColumns) : null;
        this.batchSize = batchSize;
    }

    public KdbSubScan(OpProps opProps,
                      List<PartitionProtobuf.DatasetSplit> splits,
                      BatchSchema schema,
                      List<String> pathComponents,
                      String sql,
                      StoragePluginId storagePluginId,
                      List<SchemaPath> columns,
                      ByteString extendedProperty,
                      List<String> partitionColumnsList,
                      int batchSize) {
        super(opProps, schema, pathComponents, columns);
        this.splits = splits;
        this.sql = sql;
        this.pluginId = storagePluginId;
        this.extendedProperty = extendedProperty;
        this.partitionColumns = partitionColumnsList != null ? ImmutableList.copyOf(partitionColumnsList) : null;
        this.batchSize = batchSize;
    }

    public StoragePluginId getPluginId() {
        return pluginId;
    }

    public String getSql() {
        return sql;
    }

    public List<PartitionProtobuf.DatasetSplit> getSplits() {
        return splits;
    }

    public ByteString getExtendedProperty() {
        return extendedProperty;
    }

    public List<String> getPartitionColumns() {
        return partitionColumns;
    }

    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public int getOperatorType() {
        return 73;
    }

}
