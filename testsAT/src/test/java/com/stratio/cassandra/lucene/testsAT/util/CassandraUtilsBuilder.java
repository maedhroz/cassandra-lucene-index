/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.lucene.testsAT.util;

import com.stratio.cassandra.lucene.builder.index.schema.mapping.Mapper;
import com.stratio.cassandra.lucene.builder.index.schema.mapping.SingleColumnMapper;

import java.util.*;

import static com.stratio.cassandra.lucene.builder.Builder.*;
import static com.stratio.cassandra.lucene.testsAT.util.CassandraConfig.*;

/**
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class CassandraUtilsBuilder {

    private final String name;
    private String table = TABLE;
    private String indexName = INDEX;
    private String indexColumn = COLUMN;
    private Map<String, String> columns;
    private Map<String, Mapper> mappers;
    private List<String> partitionKey;
    private List<String> clusteringKey;
    private Map<String, Map<String, String>> udts;

    CassandraUtilsBuilder(String name) {
        super();
        this.name = name;
        this.columns = new HashMap<>();
        this.mappers = new HashMap<>();
        this.partitionKey = new ArrayList<>();
        this.clusteringKey = new ArrayList<>();
        this.udts = new LinkedHashMap<>();
    }

    public CassandraUtilsBuilder withTable(String table) {
        this.table = table;
        return this;
    }

    public CassandraUtilsBuilder withIndexName(String indexName) {
        this.indexName = indexName;
        return this;
    }

    public CassandraUtilsBuilder withIndexColumn(String indexColumn) {
        this.indexColumn = indexColumn;
        return this;
    }

    public CassandraUtilsBuilder withColumn(String name, String type, Mapper mapper) {
        columns.put(name, type);
        if (mapper != null) {
            mappers.put(name, mapper);
        }
        return this;
    }

    public CassandraUtilsBuilder withColumn(String name, String type) {
        columns.put(name, type);
        String baseType = type.replaceAll("(.*)(<|,)", "").replace(">", "");
        SingleColumnMapper<?> mapper = defaultMapper(baseType);
        if (mapper != null) {
            mappers.put(name, mapper);
        }
        return this;
    }

    public CassandraUtilsBuilder withStaticColumn(String name, String type, boolean createMapper) {
        columns.put(name, type + " static");
        if (createMapper) {
            String baseType = type.replaceAll("(.*)(<|,)", "").replace(">", "");
            SingleColumnMapper<?> mapper = defaultMapper(baseType);
            mappers.put(name, mapper);
        }
        return this;
    }

    public CassandraUtilsBuilder withUDT(String column, String field, String type) {
        Map<String, String> udt = udts.get(column);
        if (udt == null) {
            udt = new HashMap<>();
            udts.put(column, udt);
        }
        udt.put(field, type);
        return this;
    }

    public CassandraUtilsBuilder withPartitionKey(String... columns) {
        partitionKey.addAll(Arrays.asList(columns));
        return this;
    }

    public CassandraUtilsBuilder withClusteringKey(String... columns) {
        clusteringKey.addAll(Arrays.asList(columns));
        return this;
    }

    public CassandraUtilsBuilder withMapper(String name, Mapper mapper) {
        mappers.put(name, mapper);
        return this;
    }

    private SingleColumnMapper<?> defaultMapper(String name) {
        switch (name) {
            case "ascii":
                return stringMapper();
            case "bigint":
                return longMapper();
            case "blob":
                return blobMapper();
            case "boolean":
                return booleanMapper();
            case "counter":
                return longMapper();
            case "decimal":
                return bigDecimalMapper().integerDigits(10).decimalDigits(10);
            case "double":
                return doubleMapper();
            case "float":
                return floatMapper();
            case "inet":
                return inetMapper();
            case "int":
                return integerMapper();
            case "smallint":
                return integerMapper();
            case "text":
                return textMapper();
            case "timestamp":
                return dateMapper().pattern("yyyy/MM/dd");
            case "timeuuid":
                return uuidMapper();
            case "tinyint":
                return integerMapper();
            case "uuid":
                return uuidMapper();
            case "varchar":
                return stringMapper();
            case "varint":
                return bigIntegerMapper().digits(10);
            default:
                return null;
        }
    }

    public CassandraUtils build() {
        String keyspace = name + "_" + Math.abs(new Random().nextLong());
        return new CassandraUtils(keyspace,
                                  table,
                                  indexName,
                                  indexColumn,
                                  columns,
                                  mappers,
                                  partitionKey,
                                  clusteringKey,
                                  udts);
    }

    @Override
    public CassandraUtilsBuilder clone() {
        CassandraUtilsBuilder clone = new CassandraUtilsBuilder(name);

        clone.table = table;
        clone.indexName = INDEX;
        clone.indexColumn = COLUMN;
        for (String key : columns.keySet()) {
            clone.columns.put(key, columns.get(key));
        }
        for (String key : mappers.keySet()) {
            clone.mappers.put(key, mappers.get(key));
        }
        for (String value : partitionKey) {
            clone.partitionKey.add(value);
        }
        for (String value : clusteringKey) {
            clone.clusteringKey.add(value);
        }
        for (String key : udts.keySet()) {
            Map<String, String> internalMap = new HashMap<>();
            for (String key2 : udts.get(key).keySet()) {
                internalMap.put(key2, udts.get(key).get(key2));
            }
            clone.udts.put(key, internalMap);
        }

        return clone;
    }
}
