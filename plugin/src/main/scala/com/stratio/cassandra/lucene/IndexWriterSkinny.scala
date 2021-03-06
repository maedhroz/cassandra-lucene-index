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
package com.stratio.cassandra.lucene

import com.stratio.cassandra.lucene.util.Tracer
import org.apache.cassandra.db.DecoratedKey
import org.apache.cassandra.db.rows.Row
import org.apache.cassandra.index.transactions.IndexTransaction
import org.apache.cassandra.index.transactions.IndexTransaction.Type._
import org.apache.cassandra.utils.concurrent.OpOrder

/** [[IndexWriter]] for skinny rows.
  *
  * @param service         the service to perform the indexing operation
  * @param key             key of the partition being modified
  * @param nowInSec        current time of the update operation
  * @param opGroup         operation group spanning the update operation
  * @param transactionType what kind of update is being performed on the base data
  * @author Andres de la Pena `adelapena@stratio.com`
  */
class IndexWriterSkinny(service: IndexServiceSkinny,
                        key: DecoratedKey,
                        nowInSec: Int,
                        opGroup: OpOrder.Group,
                        transactionType: IndexTransaction.Type)
  extends IndexWriter(service, key, nowInSec, opGroup, transactionType) {

  private var row: Option[Row] = None

  /** @inheritdoc */
  override def delete() {
    service.delete(key)
    row = None
  }

  /** @inheritdoc */
  override def index(row: Row) {
    this.row = Option(row)
  }

  /** @inheritdoc */
  override def finish() {
    if (transactionType != CLEANUP) {
      row.map(row => {
        if (transactionType == COMPACTION || service.needsReadBeforeWrite(key, row)) {
          Tracer.trace("Lucene index reading before write")
          val iterator = service.read(key, nowInSec, opGroup)
          if (iterator.hasNext) iterator.next.asInstanceOf[Row] else row
        } else row
      }).foreach(row => {
        if (row.hasLiveData(nowInSec)) {
          Tracer.trace("Lucene index writing document")
          service.upsert(key, row, nowInSec)
        } else {
          Tracer.trace("Lucene index deleting document")
          service.delete(key)
        }
      })
    }
  }
}
