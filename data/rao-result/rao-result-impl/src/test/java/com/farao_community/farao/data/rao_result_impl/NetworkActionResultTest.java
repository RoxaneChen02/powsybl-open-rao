/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionResultTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = CommonCracCreation.create();
    }

    @Test
    void defaultValuesTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();

        assertFalse(networkActionResult.isActivated(crac.getPreventiveState()));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertTrue(networkActionResult.getStatesWithActivation().isEmpty());
    }

    @Test
    void activatedInOnePreventiveTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();
        networkActionResult.addActivationForState(crac.getPreventiveState());

        assertTrue(networkActionResult.isActivated(crac.getPreventiveState()));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertFalse(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(1, networkActionResult.getStatesWithActivation().size());
    }

    @Test
    void activatedInTwoCurativeStatesTest() {
        NetworkActionResult networkActionResult = new NetworkActionResult();
        networkActionResult.addActivationForStates(Set.of(crac.getState("Contingency FR1 FR3", CURATIVE), crac.getState("Contingency FR1 FR2", CURATIVE)));

        assertFalse(networkActionResult.isActivated(crac.getPreventiveState()));
        assertTrue(networkActionResult.isActivated(crac.getState("Contingency FR1 FR2", CURATIVE)));
        assertTrue(networkActionResult.isActivated(crac.getState("Contingency FR1 FR3", CURATIVE)));
        assertEquals(2, networkActionResult.getStatesWithActivation().size());
    }
}
