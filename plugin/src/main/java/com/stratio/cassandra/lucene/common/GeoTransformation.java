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
package com.stratio.cassandra.lucene.common;

import com.google.common.base.MoreObjects;
import com.spatial4j.core.shape.Rectangle;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Geometry;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import static com.stratio.cassandra.lucene.util.GeospatialUtilsJTS.CONTEXT;

/**
 * Class representing the transformation of a JTS geographical shape into a new shape.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = GeoTransformation.BBox.class, name = "bbox"),
               @JsonSubTypes.Type(value = GeoTransformation.Buffer.class, name = "buffer"),
               @JsonSubTypes.Type(value = GeoTransformation.Centroid.class, name = "centroid"),
               @JsonSubTypes.Type(value = GeoTransformation.ConvexHull.class, name = "convex_hull")})
public interface GeoTransformation {

    /**
     * Returns the {@link JtsGeometry} resulting of applying this transformation to the specified {@link JtsGeometry}.
     *
     * @param shape the JTS shape to be transformed
     * @return the transformed JTS shape
     */
    JtsGeometry apply(JtsGeometry shape);

    /**
     * {@link GeoTransformation} that returns the bounding box of a JTS geographical shape. The bounding box of shape is
     * the minimal rectangle containing the shape.
     */
    class BBox implements GeoTransformation {

        /**
         * Returns the bounding box of the specified {@link JtsGeometry}.
         *
         * @param shape the JTS shape to be transformed
         * @return the convex hull
         */
        @Override
        public JtsGeometry apply(JtsGeometry shape) {
            Rectangle rectangle = shape.getBoundingBox();
            Geometry geometry = CONTEXT.getGeometryFrom(rectangle);
            return CONTEXT.makeShape(geometry);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).toString();
        }
    }

    /**
     * {@link GeoTransformation} that returns the bounding shape of a JTS geographical shape.
     */
    class Buffer implements GeoTransformation {

        /** The max allowed distance. */
        @JsonProperty("max_distance")
        public final GeoDistance maxDistance;

        /** The min allowed distance. */
        @JsonProperty("min_distance")
        public final GeoDistance minDistance;

        /**
         * Constructor take the distance range.
         *
         * @param minDistance the min allowed distance
         * @param maxDistance the max allowed distance
         */
        @JsonCreator
        public Buffer(@JsonProperty("min_distance") GeoDistance minDistance,
                      @JsonProperty("max_distance") GeoDistance maxDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        /**
         * Returns the buffer of the specified {@link JtsGeometry}.
         *
         * @param shape the JTS shape to be transformed
         * @return the buffer
         */
        @Override
        public JtsGeometry apply(JtsGeometry shape) {

            JtsGeometry max = maxDistance == null
                              ? CONTEXT.makeShape(shape.getGeom())
                              : shape.getBuffered(maxDistance.getDegrees(), CONTEXT);

            if (minDistance != null) {
                JtsGeometry min = shape.getBuffered(minDistance.getDegrees(), CONTEXT);
                Geometry difference = max.getGeom().difference(min.getGeom());
                return CONTEXT.makeShape(difference);
            }
            return max;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("minDistance", minDistance)
                              .add("maxDistance", maxDistance)
                              .toString();
        }
    }

    /**
     * {@link GeoTransformation} that returns the center point of a JTS geographical shape.
     */
    class Centroid implements GeoTransformation {

        /**
         * Returns the center of the specified {@link JtsGeometry}.
         *
         * @param shape the JTS shape to be transformed
         * @return the center
         */
        @Override
        public JtsGeometry apply(JtsGeometry shape) {
            Geometry centroid = shape.getGeom().getCentroid();
            return CONTEXT.makeShape(centroid);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).toString();
        }
    }

    /**
     * {@link GeoTransformation} that returns the convex hull of a JTS geographical shape.
     */
    class ConvexHull implements GeoTransformation {

        /**
         * Returns the convex hull of the specified {@link JtsGeometry}.
         *
         * @param shape the JTS shape to be transformed
         * @return the convex hull
         */
        @Override
        public JtsGeometry apply(JtsGeometry shape) {
            Geometry centroid = shape.getGeom().convexHull();
            return CONTEXT.makeShape(centroid);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).toString();
        }
    }
}
