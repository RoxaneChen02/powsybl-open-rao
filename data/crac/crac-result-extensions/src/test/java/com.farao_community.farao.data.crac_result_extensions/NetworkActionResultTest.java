/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionResultTest {
    private NetworkActionResult networkActionResult;
    private Set<State> states;
    private State initialState;
    private State outage1;
    private State curative1;
    private State outage2;
    private State curative2;

    @Before
    public void setUp() {
        states = new HashSet<>();
        initialState = new SimpleState(Optional.empty(), new Instant("initial", 0));
        outage1 = new SimpleState(Optional.of(new ComplexContingency("co1")), new Instant("after-co1", 10));
        curative1 = new SimpleState(Optional.of(new ComplexContingency("co1")), new Instant("curative-co1", 50));
        outage2 = new SimpleState(Optional.of(new ComplexContingency("co2")), new Instant("after-co2", 10));
        curative2 = new SimpleState(Optional.of(new ComplexContingency("co2")), new Instant("curative-co2", 50));
        states.add(initialState);
        states.add(outage1);
        states.add(curative1);
        states.add(outage2);
        states.add(curative2);
        networkActionResult = new NetworkActionResult(states);
    }

    @Test
    public void constructor() {
        assertTrue(networkActionResult.activationMap.containsKey(initialState));
        assertTrue(networkActionResult.activationMap.containsKey(outage1));
        assertTrue(networkActionResult.activationMap.containsKey(curative1));
        assertTrue(networkActionResult.activationMap.containsKey(outage2));
        assertTrue(networkActionResult.activationMap.containsKey(curative2));
        assertEquals(5, networkActionResult.activationMap.size());
    }

    @Test
    public void activate() {
        networkActionResult.activate(initialState);
        assertTrue(networkActionResult.isActivated(initialState));
        assertFalse(networkActionResult.isActivated(outage1));
    }

    @Test
    public void deactivate() {
        networkActionResult.activate(initialState);
        assertTrue(networkActionResult.isActivated(initialState));
        networkActionResult.deactivate(initialState);
        assertFalse(networkActionResult.isActivated(initialState));
    }

    @Test
    public void getName() {
        assertEquals("NetworkActionResult", networkActionResult.getName());
    }
}