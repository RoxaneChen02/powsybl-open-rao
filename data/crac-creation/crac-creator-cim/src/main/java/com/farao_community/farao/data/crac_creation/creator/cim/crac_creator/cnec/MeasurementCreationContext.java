/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_creation.creator.api.ImportStatus;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MeasurementCreationContext {
    private final ImportStatus importStatus;
    private final String importStatusDetail;
    private final MultiKeyMap<Object, CnecCreationContext> cnecCreationContexts;

    private MeasurementCreationContext(ImportStatus importStatus, String importStatusDetail) {
        this.importStatus = importStatus;
        this.importStatusDetail = importStatusDetail;
        cnecCreationContexts = new MultiKeyMap<>();
    }

    static MeasurementCreationContext notImported(ImportStatus importStatus, String importStatusDetail) {
        return new MeasurementCreationContext(importStatus, importStatusDetail);
    }

    static MeasurementCreationContext imported() {
        return new MeasurementCreationContext(ImportStatus.IMPORTED, null);
    }

    public boolean isImported() {
        return importStatus.equals(ImportStatus.IMPORTED);
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public String getImportStatusDetail() {
        return importStatusDetail;
    }

    public MultiKeyMap<Object, CnecCreationContext> getCnecCreationContexts() {
        return cnecCreationContexts;
    }

    public void addCnecCreationContext(String contingencyId, Instant instant, CnecCreationContext cnecCreationContext) {
        MultiKey<Object> key = new MultiKey<>(contingencyId, instant);
        cnecCreationContexts.put(key, cnecCreationContext);
    }
}
