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
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
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
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters;
    private OptimizationPerimeter optimizationPerimeter;
    private State state;
    private MarginEvaluator marginEvaluator;
    private RemedialActionActivationResult remedialActionActivationResult;

    @BeforeEach
    void setUp() {
        rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
        rangeActionsOptimizationParameters.setPstPenaltyCost(0.01);
        rangeActionsOptimizationParameters.setInjectionRaPenaltyCost(0.02);
        rangeActionsOptimizationParameters.setHvdcPenaltyCost(0.5);

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
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state), Set.of(), Unit.MEGAWATT, marginEvaluator, rangeActionsOptimizationParameters);
        assertEquals(Unit.MEGAWATT, evaluator.getUnit());
        assertEquals("remedial-action-cost-evaluator", evaluator.getName());
    }

    @Test
    void testTotalRemedialActionCostNoOverload() {
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state), Set.of(), Unit.MEGAWATT, marginEvaluator, rangeActionsOptimizationParameters);

        FlowResult flowResult = Mockito.mock(FlowResult.class);

        Pair<Double, List<FlowCnec>> costAndLimitingElements = evaluator.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, Set.of());
        assertEquals(11738.8, costAndLimitingElements.getLeft());
        assertTrue(costAndLimitingElements.getRight().isEmpty());
    }

    @Test
    void testTotalRemedialActionCostWithOverload() {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnec.isOptimized()).thenReturn(true);
        Mockito.when(flowCnec.getState()).thenReturn(state);
        Mockito.when(flowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(-1d);

        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state), Set.of(flowCnec), Unit.MEGAWATT, marginEvaluator, rangeActionsOptimizationParameters);

        Pair<Double, List<FlowCnec>> costAndLimitingElements = evaluator.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, Set.of());
        assertEquals(21738.8, costAndLimitingElements.getLeft());
        assertEquals(List.of(flowCnec), costAndLimitingElements.getRight());
    }
}