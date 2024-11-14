/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RemedialActionCostEvaluatorImplTest {
    private OptimizationPerimeter optimizationPerimeter;
    private State state;
    private MarginEvaluator marginEvaluator;
    private RemedialActionActivationResult remedialActionActivationResult;

    @BeforeEach
    void setUp() {
        PstRangeAction pstRangeAction1 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction1.getActivationCost()).thenReturn(Optional.empty());
        Mockito.when(pstRangeAction1.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.of(1d));
        Mockito.when(pstRangeAction1.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.empty());

        PstRangeAction pstRangeAction2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction2.getActivationCost()).thenReturn(Optional.of(10d));
        Mockito.when(pstRangeAction2.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.empty());
        Mockito.when(pstRangeAction2.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.empty());

        InjectionRangeAction injectionRangeAction1 = Mockito.mock(InjectionRangeAction.class);
        Mockito.when(injectionRangeAction1.getActivationCost()).thenReturn(Optional.of(5d));
        Mockito.when(injectionRangeAction1.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.of(150d));
        Mockito.when(injectionRangeAction1.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.of(200d));

        InjectionRangeAction injectionRangeAction2 = Mockito.mock(InjectionRangeAction.class);
        Mockito.when(injectionRangeAction2.getActivationCost()).thenReturn(Optional.of(0.25));
        Mockito.when(injectionRangeAction2.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.of(200d));
        Mockito.when(injectionRangeAction2.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.empty());

        HvdcRangeAction hvdcRangeAction1 = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdcRangeAction1.getActivationCost()).thenReturn(Optional.of(100d));
        Mockito.when(hvdcRangeAction1.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.of(10d));
        Mockito.when(hvdcRangeAction1.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.of(15d));

        HvdcRangeAction hvdcRangeAction2 = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdcRangeAction2.getActivationCost()).thenReturn(Optional.of(200d));
        Mockito.when(hvdcRangeAction2.getVariationCost(RangeAction.VariationDirection.UP)).thenReturn(Optional.of(0.1));
        Mockito.when(hvdcRangeAction2.getVariationCost(RangeAction.VariationDirection.DOWN)).thenReturn(Optional.empty());

        NetworkAction topologyAction = Mockito.mock(NetworkAction.class);
        Mockito.when(topologyAction.getActivationCost()).thenReturn(Optional.of(20d));

        state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(state);
        Mockito.when(optimizationPerimeter.getMonitoredStates()).thenReturn(Set.of());

        marginEvaluator = new BasicMarginEvaluator();

        remedialActionActivationResult = Mockito.mock(RemedialActionActivationResultImpl.class);
        Mockito.when(remedialActionActivationResult.getActivatedNetworkActions()).thenReturn(Set.of(topologyAction));
        Mockito.when(remedialActionActivationResult.getActivatedRangeActions(state)).thenReturn(Set.of(pstRangeAction1, pstRangeAction2, injectionRangeAction1, injectionRangeAction2, hvdcRangeAction1, hvdcRangeAction2));
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction1, state)).thenReturn(2);
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction2, state)).thenReturn(-5);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction1, state)).thenReturn(35d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction2, state)).thenReturn(-75d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction1, state)).thenReturn(600d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction2, state)).thenReturn(-300d);
    }

    @Test
    void testBasicData() {
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state), Set.of(), Unit.MEGAWATT, marginEvaluator);
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
        assertEquals("remedial-action-cost-evaluator", evaluator.getName());
    }

    @Test
    void testTotalRemedialActionCost() {
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state), Set.of(), Unit.MEGAWATT, marginEvaluator);

        FlowResult flowResult = Mockito.mock(FlowResult.class);

        Pair<Double, List<FlowCnec>> costAndLimitingElements = evaluator.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, Set.of());
        assertEquals(11587.25, costAndLimitingElements.getLeft());
        assertTrue(costAndLimitingElements.getRight().isEmpty());
    }
}