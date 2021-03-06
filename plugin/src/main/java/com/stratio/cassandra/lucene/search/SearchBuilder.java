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
package com.stratio.cassandra.lucene.search;

import com.stratio.cassandra.lucene.IndexException;
import com.stratio.cassandra.lucene.IndexPagingState;
import com.stratio.cassandra.lucene.search.condition.builder.ConditionBuilder;
import com.stratio.cassandra.lucene.search.sort.builder.SortFieldBuilder;
import com.stratio.cassandra.lucene.util.Builder;
import com.stratio.cassandra.lucene.util.ByteBufferUtils;
import com.stratio.cassandra.lucene.util.JsonSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Class for building a new {@link Search}.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
public class SearchBuilder implements Builder<Search> {

    /** The filtering conditions not participating in scoring. */
    @JsonProperty("filter")
    protected final List<ConditionBuilder<?, ?>> filter = new LinkedList<>();

    /** The querying conditions participating in scoring. */
    @JsonProperty("query")
    protected final List<ConditionBuilder<?, ?>> query = new LinkedList<>();

    /** The {@link SortFieldBuilder}s for the query. */
    @JsonProperty("sort")
    private List<SortFieldBuilder> sort = new LinkedList<>();

    /** If this search must force the refresh the index before reading it. */
    @JsonProperty("refresh")
    private boolean refresh;

    @JsonProperty("paging")
    private String paging;

    /** Default constructor. */
    SearchBuilder() {
    }

    /**
     * Returns this builder with the specified filtering conditions not participating in scoring.
     *
     * @param builders the conditions to be added
     * @return this builder with the specified conditions
     */
    public SearchBuilder filter(ConditionBuilder<?, ?>... builders) {
        filter.addAll(Arrays.asList(builders));
        return this;
    }

    /**
     * Returns this builder with the specified querying conditions participating in scoring.
     *
     * @param builders the conditions to be added
     * @return this builder with the specified conditions
     */
    public SearchBuilder query(ConditionBuilder<?, ?>... builders) {
        query.addAll(Arrays.asList(builders));
        return this;
    }

    /**
     * Adds the specified sorting fields.
     *
     * @param builders the sorting fields to be added
     * @return this builder with the specified sorting fields
     */
    public SearchBuilder sort(SortFieldBuilder... builders) {
        sort.addAll(Arrays.asList(builders));
        return this;
    }

    /**
     * Sets if the {@link Search} to be built must refresh the index before reading it. Refresh is a costly operation so
     * you should use it only when it is strictly required.
     *
     * @param refresh {@code true} if the {@link Search} to be built must refresh the Lucene's index searcher before
     * searching, {@code false} otherwise
     * @return this builder with the specified refresh
     */
    public SearchBuilder refresh(boolean refresh) {
        this.refresh = refresh;
        return this;
    }

    /**
     * Sets the specified starting partition key.
     *
     * @param pagingState a paging state
     * @return this builder with the specified partition key
     */
    public SearchBuilder paging(IndexPagingState pagingState) {
        this.paging = ByteBufferUtils.toHex(pagingState.toByteBuffer());
        return this;
    }

    /**
     * Returns the {@link Search} represented by this builder.
     *
     * @return the search represented by this builder
     */
    @Override
    public Search build() {
        return new Search(filter.stream().map(ConditionBuilder::build).collect(toList()),
                          query.stream().map(ConditionBuilder::build).collect(toList()),
                          sort.stream().map(SortFieldBuilder::build).collect(toList()),
                          paging == null ? null : IndexPagingState.fromByteBuffer(ByteBufferUtils.byteBuffer(paging)),
                          refresh);
    }

    /**
     * Returns the JSON representation of this object.
     *
     * @return a JSON representation of this object
     */
    public String toJson() {
        build();
        try {
            return JsonSerializer.toString(this);
        } catch (IOException e) {
            throw new IndexException(e, "Unformateable JSON search: {}", e.getMessage());
        }
    }

    /**
     * Returns the {@link SearchBuilder} represented by the specified JSON {@code String}.
     *
     * @param json the JSON {@code String} representing a {@link SearchBuilder}
     * @return the {@link SearchBuilder} represented by the specified JSON {@code String}
     */
    public static SearchBuilder fromJson(String json) {
        try {
            return JsonSerializer.fromString(json, SearchBuilder.class);
        } catch (IOException e) {
            return SearchBuilderLegacy.fromJson(json);
        }
    }

}
