/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.HvdcSetpointIncrease;

import java.util.Collections;
import java.util.List;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class HvdcRangeActionSensiHandler implements RangeActionSensiHandler {

    private final HvdcRangeAction hvdcRangeAction;

    public HvdcRangeActionSensiHandler(HvdcRangeAction hvdcRangeAction) {
        this.hvdcRangeAction = hvdcRangeAction;
    }

    @Override
    public List<SensitivityVariable> rangeActionToSensitivityVariable() {
        String elementId = hvdcRangeAction.getNetworkElement().getId();
        return Collections.singletonList(new HvdcSetpointIncrease(elementId, elementId, elementId));
    }

    @Override
    public double getSensitivityOnFlow(FlowCnec cnec, SystematicSensitivityResult sensitivityResult) {
        return sensitivityResult.getSensitivityOnFlow(hvdcRangeAction.getNetworkElement().getId(), cnec);
    }

    @Override
    public void checkConsistency(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(hvdcRangeAction.getNetworkElement().getId());
        if (!(identifiable instanceof HvdcLine)) {
            throw new SensitivityAnalysisException(String.format("Unable to create sensitivity variable for HvdcRangeAction %s, on element %s", hvdcRangeAction.getId(), hvdcRangeAction.getNetworkElement().getId()));
        }
    }
}