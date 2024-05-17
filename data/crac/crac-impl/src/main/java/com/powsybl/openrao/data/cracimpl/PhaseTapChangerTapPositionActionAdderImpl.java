/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.openrao.data.cracapi.networkaction.PhaseTapChangerTapPositionActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PhaseTapChangerTapPositionActionAdderImpl extends AbstractSingleNetworkElementActionAdderImpl<PhaseTapChangerTapPositionActionAdder> implements PhaseTapChangerTapPositionActionAdder {

    private Integer normalizedSetPoint;

    PhaseTapChangerTapPositionActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        super(ownerAdder);
    }

    @Override
    public PhaseTapChangerTapPositionActionAdder withNormalizedSetpoint(int normalizedSetPoint) {
        this.normalizedSetPoint = normalizedSetPoint;
        return this;
    }

    protected Action buildAction() {
        return new PhaseTapChangerTapPositionActionBuilder()
            .withId(String.format("%s_%s_%s", getActionName(), networkElementId, normalizedSetPoint))
            .withNetworkElementId(networkElementId)
            .withTapPosition(normalizedSetPoint)
            .withRelativeValue(false)
            .build();
    }

    protected void assertSpecificAttributes() {
        assertAttributeNotNull(normalizedSetPoint, getActionName(), "normalizedSetPoint", "withNormalizedSetPoint()");
    }

    protected String getActionName() {
        return "PhaseTapChangerTapPositionAction";
    }
}
