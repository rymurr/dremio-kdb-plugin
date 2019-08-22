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
package com.dremio.extras.plugins.kdb.rels.translate;

import com.dremio.exec.planner.physical.ExchangePrel;
import com.dremio.exec.planner.physical.JoinPrel;
import com.dremio.exec.planner.physical.LeafPrel;
import com.dremio.exec.planner.physical.Prel;
import com.dremio.exec.planner.physical.ScreenPrel;
import com.dremio.exec.planner.physical.WriterPrel;
import com.dremio.extras.plugins.kdb.rels.KdbAggregate;
import com.dremio.extras.plugins.kdb.rels.KdbFilter;
import com.dremio.extras.plugins.kdb.rels.KdbLimit;
import com.dremio.extras.plugins.kdb.rels.KdbProject;
import com.dremio.extras.plugins.kdb.rels.KdbSort;

/**
 * visitor to generate kdb query info
 */
public interface KdbPrelVisitor {

    KdbQueryParameters visitExchange(ExchangePrel prel, KdbQueryParameters value);

    KdbQueryParameters visitScreen(ScreenPrel prel, KdbQueryParameters value);

    KdbQueryParameters visitWriter(WriterPrel prel, KdbQueryParameters value);

    KdbQueryParameters visitLeaf(LeafPrel prel, KdbQueryParameters value);

    KdbQueryParameters visitJoin(JoinPrel prel, KdbQueryParameters value);

    KdbQueryParameters visitProject(KdbProject prel, KdbQueryParameters value);

    KdbQueryParameters visitAggregate(KdbAggregate prel, KdbQueryParameters value);

    KdbQueryParameters visitLimit(KdbLimit prel, KdbQueryParameters value);

    KdbQueryParameters visitSort(KdbSort prel, KdbQueryParameters value);

    KdbQueryParameters visitFilter(KdbFilter prel, KdbQueryParameters value);

    KdbQueryParameters visitPrel(Prel prel, KdbQueryParameters value);

}
