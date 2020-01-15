/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao.config;

import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRangeActionRaoParametersTest {

    private LinearRangeActionRaoParameters raoParameters;

    @Before
    public void setUp() throws Exception {
        raoParameters = new LinearRangeActionRaoParameters();
    }

    @Test
    public void getName() {
        assertEquals("LinearRangeActionRaoParameters", raoParameters.getName());
    }

    @Test
    public void setSensitivityComputationParameters() {
        SensitivityComputationParameters sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
        raoParameters.setSensitivityComputationParameters(sensitivityComputationParameters);

        assertSame(sensitivityComputationParameters, raoParameters.getSensitivityComputationParameters());
    }
}