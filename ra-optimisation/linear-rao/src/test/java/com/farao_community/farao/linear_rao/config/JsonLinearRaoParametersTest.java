/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonLinearRaoParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(LinearRaoParameters.class, new LinearRaoParameters());
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/LinearRaoParameters.json");
    }

    @Test
    public void readError() throws IOException {
        try {
            JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersError.json"));
            fail();
        } catch (AssertionError e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }
}