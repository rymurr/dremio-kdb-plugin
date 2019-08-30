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
import java.util.Optional;

import org.apache.arrow.vector.types.pojo.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dremio.connector.ConnectorException;
import com.dremio.connector.metadata.DatasetHandle;
import com.dremio.connector.metadata.DatasetHandleListing;
import com.dremio.connector.metadata.DatasetMetadata;
import com.dremio.connector.metadata.EntityPath;
import com.dremio.connector.metadata.GetDatasetOption;
import com.dremio.connector.metadata.GetMetadataOption;
import com.dremio.connector.metadata.ListPartitionChunkOption;
import com.dremio.connector.metadata.PartitionChunkListing;
import com.dremio.connector.metadata.extensions.SupportsListingDatasets;
import com.dremio.exec.planner.logical.ViewTable;
import com.dremio.exec.record.BatchSchema;
import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.SchemaConfig;
import com.dremio.exec.store.StoragePlugin;
import com.dremio.exec.store.StoragePluginRulesFactory;
import com.dremio.extras.plugins.kdb.exec.KdbSchema;
import com.dremio.service.namespace.DatasetHelper;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.SourceState;
import com.dremio.service.namespace.capabilities.BooleanCapabilityValue;
import com.dremio.service.namespace.capabilities.SourceCapabilities;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.protostuff.ByteString;

/**
 * plugin definition for kdb source
 */
public class KdbStoragePlugin implements StoragePlugin, SupportsListingDatasets {
    private static final Logger LOGGER = LoggerFactory.getLogger(KdbStoragePlugin.class);
    private final SabotContext context;
    private final String name;
    private final KdbSchema kdbConnection;
    private final Map<EntityPath, DatasetHandle> setMap = Maps.newHashMap();
    private final int batchSize;
    private boolean built = false;
    private ArrayList<DatasetHandle> dataSets;

    public KdbStoragePlugin(KdbStoragePluginConfig kdbConfig, SabotContext context, String name) {
        this.context = context;
        this.name = name;
        int x = 0;
        try {
            x = kdbConfig.fetchSize;
        } catch (Throwable t) {
            x = 0;
        }
        this.batchSize = x;
        kdbConnection = new KdbSchema(
                kdbConfig.host, kdbConfig.port, kdbConfig.username, kdbConfig.password);
    }

    private void buildDataSets() {
        if (!built) {
            dataSets = Lists.newArrayList();

            for (String table : kdbConnection.getTableNames()) {
                EntityPath path = new EntityPath(ImmutableList.of(name, table));
                KdbTableDefinition def = new KdbTableDefinition(name, path, kdbConnection);
                dataSets.add(def);
                setMap.put(path, def);
            }
            built = true;
        }
    }

    @Override
    public SourceState getState() {
        try {
            kdbConnection.getTableNames();
            return SourceState.GOOD;
        } catch (Exception t) {
            return SourceState.badState(t);
        }
    }

    @Override
    public SourceCapabilities getSourceCapabilities() {
        return new SourceCapabilities(new BooleanCapabilityValue(SourceCapabilities.SUPPORTS_CONTAINS, true),
                new BooleanCapabilityValue(SourceCapabilities.SUBQUERY_PUSHDOWNABLE, true) //,
//      new BooleanCapabilityValue(SourceCapabilities.TREAT_CALCITE_SCAN_COST_AS_INFINITE, true)
        );
    }

    @Override
    public DatasetConfig createDatasetConfigFromSchema(DatasetConfig oldConfig, BatchSchema newSchema) {
        Preconditions.checkNotNull(oldConfig);
        Preconditions.checkNotNull(newSchema);

        final BatchSchema merge;
        if (DatasetHelper.getSchemaBytes(oldConfig) == null) {
            merge = newSchema;
        } else {
            final List<Field> oldFields = new ArrayList<>();
            BatchSchema.fromDataset(oldConfig).forEach(oldFields::add);

            merge = new BatchSchema(oldFields).merge(newSchema);
        }

        final DatasetConfig newConfig = serializer.deserialize(serializer.serialize(oldConfig));
        newConfig.setRecordSchema(ByteString.copyFrom(merge.serialize()));

        return newConfig;
    }


    @Override
    public ViewTable getView(List<String> tableSchemaPath, SchemaConfig schemaConfig) {
        return null;
    }

    @Override
    public Class<? extends StoragePluginRulesFactory> getRulesFactoryClass() {
        return context.getConfig().getClass("com.dremio.extras.plugins.kdb", StoragePluginRulesFactory.class, KdbRulesFactory.class);
    }

    @Override
    public void start() throws IOException {

    }

    @Override
    public boolean hasAccessPermission(String user, NamespaceKey key, DatasetConfig datasetConfig) {
        return true; //todo...everyone has access if the kdb table exists
    }

    public KdbSchema getKdbSchema() {
        return kdbConnection;
    }

    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void close() throws Exception {
        LOGGER.info("close kdb source");
    }

    @Override
    public Optional<DatasetHandle> getDatasetHandle(EntityPath entityPath, GetDatasetOption... getDatasetOptions) throws ConnectorException {
        buildDataSets();
        if (entityPath.getComponents().size() != 2) {
            return Optional.empty();
        }
        return Optional.ofNullable(setMap.get(entityPath));
    }

    @Override
    public PartitionChunkListing listPartitionChunks(DatasetHandle datasetHandle, ListPartitionChunkOption... listPartitionChunkOptions) throws ConnectorException {
        return datasetHandle.unwrap(KdbTableDefinition.class).listPartitionChunks(null);
    }

    @Override
    public DatasetMetadata getDatasetMetadata(DatasetHandle datasetHandle, PartitionChunkListing partitionChunkListing, GetMetadataOption... getMetadataOptions) throws ConnectorException {
        return datasetHandle.unwrap(KdbTableDefinition.class).getDatasetMetadata(null);
    }

    @Override
    public boolean containerExists(EntityPath entityPath) {
        buildDataSets();
        return setMap.containsKey(entityPath);
    }

    @Override
    public DatasetHandleListing listDatasetHandles(GetDatasetOption... getDatasetOptions) throws ConnectorException {
        return () -> setMap.values().stream().iterator();
    }
}
