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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

/**
 * Schema for kdb instance
 */
public class KdbSchema {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public KdbSchema(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Set<String> getTableNames() {
        return this.getTableMap().keySet();
    }

    public KdbTable getTable(String name) {
        return this.getTableMap().get(name);
    }

    private Map<String, KdbTable> getTableMap() {
        final ImmutableMap.Builder<String, KdbTable> builder = ImmutableMap.builder();
        KdbConnection kdbConnection = getKdbConnection();
        for (String collectionName : kdbConnection.getTables()) {
            builder.put(collectionName, new KdbTable(collectionName, kdbConnection, null));
        }
        return builder.build();
    }

    public double getVersion() throws IOException, c.KException {
        Object x = getKdbConnection().select(".z.K");
        return (double) x;
    }

    public KdbConnection getKdbConnection() {
        return KdbConnection.Builder.newInstance()
                .withHostname(host)
                .withPort(port)
                .withUsername(username)
                .withPassword(password)
                .build();
    }

    public boolean getPartitioned(String table) throws IOException, c.KException {
        Object x = getKdbConnection().select(".Q.qp " + table);
        try {
            return (boolean) x;
        } catch (Throwable t) {
            return false;
        }
    }

    public static class Builder {

        private String hostname;
        private int port;
        private String username;
        private String password;

        public static Builder newInstance() {
            return new KdbSchema.Builder();
        }

        private Builder() {}

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public KdbSchema build() {
            return new KdbSchema(this.hostname, this.port, this.username, this.password);
        }
    }

}
