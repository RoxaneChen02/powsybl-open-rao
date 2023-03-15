/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionLimitationParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Handles constraints for maximum number od RAs to activate (ma-ra), maximum number of TSOs that can activate RAs (max-tso),
 * maximum number of RAs per TSO (max-ra-per-tso), and maximum number of PSTs per TSO (max-pst-per-tso).
 * Beware: this introduces binary variables to define if an RA is used.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaUsageLimitsFiller implements ProblemFiller {

    private final Map<State, Set<RangeAction<?>>> rangeActions;
    private final RangeActionSetpointResult prePerimeterRangeActionSetpoints;
    private final RangeActionLimitationParameters rangeActionLimitationParameters;
    private boolean arePstSetpointsApproximated;
    private static final double RANGE_ACTION_SETPOINT_EPSILON = 1e-5;

    public RaUsageLimitsFiller(Map<State, Set<RangeAction<?>>> rangeActions,
                               RangeActionSetpointResult prePerimeterRangeActionSetpoints,
                               RangeActionLimitationParameters rangeActionLimitationParameters,
                               boolean arePstSetpointsApproximated) {
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionSetpoints = prePerimeterRangeActionSetpoints;
        this.rangeActionLimitationParameters = rangeActionLimitationParameters;
        this.arePstSetpointsApproximated = arePstSetpointsApproximated;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        rangeActions.forEach((state, rangeActionSet) -> {
            if (!rangeActionLimitationParameters.areRangeActionLimitedForState(state)) {
                return;
            }
            rangeActionSet.forEach(ra -> buildIsVariationVariableAndConstraints(linearProblem, ra, state));
            if (rangeActionLimitationParameters.getMaxRangeActions(state) != null) {
                addMaxRaConstraint(linearProblem, state);
            }
            if (rangeActionLimitationParameters.getMaxTso(state) != null) {
                addMaxTsoConstraint(linearProblem, state);
            }
            if (!rangeActionLimitationParameters.getMaxRangeActionPerTso(state).isEmpty()) {
                addMaxRaPerTsoConstraint(linearProblem, state);
            }
            if (!rangeActionLimitationParameters.getMaxPstPerTso(state).isEmpty()) {
                addMaxPstPerTsoConstraint(linearProblem, state);
            }
        });
    }

    @Override
    public void updateBetweenSensiIteration(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do, we are only comparing optimal and pre-perimeter setpoints
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // nothing to do, we are only comparing optimal and pre-perimeter setpoints
    }

    private double getAverageAbsoluteTapToAngleConversionFactor(PstRangeAction pstRangeAction) {
        int minTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().min(Integer::compareTo).orElseThrow();
        int maxTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        double minAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().min(Double::compareTo).orElseThrow();
        double maxAngle = pstRangeAction.getTapToAngleConversionMap().values().stream().max(Double::compareTo).orElseThrow();
        return Math.abs((maxAngle - minAngle) / (maxTap - minTap));
    }

    /**
     *  Get relaxation term to add to correct the initial setpoint, to ensure problem feasibility depending on the approximations.
     *  If PSTs are modelled with approximate integers, make sure that the initial setpoint is feasible (it should be at
     *  a distance smaller then 0.3 * getAverageAbsoluteTapToAngleConversionFactor from a feasible setpoint in the MIP)
     */
    private double getInitialSetpointRelaxation(RangeAction rangeAction) {
        if (rangeAction instanceof PstRangeAction && arePstSetpointsApproximated) {
            // The BestTapFinder is accurate at 35% of the setpoint difference between 2 taps. Using 30% here to be safe.
            return 0.3 * getAverageAbsoluteTapToAngleConversionFactor((PstRangeAction) rangeAction);
        } else {
            return RANGE_ACTION_SETPOINT_EPSILON;
        }
    }

    private void buildIsVariationVariableAndConstraints(LinearProblem linearProblem, RangeAction<?> rangeAction, State state) {
        FaraoMPVariable isVariationVariable = linearProblem.addRangeActionVariationBinary(rangeAction, state);

        FaraoMPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state);
        if (absoluteVariationVariable == null) {
            throw new FaraoException(format("Range action absolute variation variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double initialSetpointRelaxation = getInitialSetpointRelaxation(rangeAction);

        // range action absolute variation <= isVariationVariable * (max setpoint - min setpoint) + initialSetpointRelaxation
        // RANGE_ACTION_SETPOINT_EPSILON is used to mitigate rounding issues, ensuring that the maximum setpoint is feasible
        // initialSetpointRelaxation is used to ensure that the initial setpoint is feasible
        FaraoMPConstraint constraint = linearProblem.addIsVariationConstraint(-LinearProblem.infinity(), initialSetpointRelaxation, rangeAction, state);
        constraint.setCoefficient(absoluteVariationVariable, 1);
        double initialSetpoint = prePerimeterRangeActionSetpoints.getSetpoint(rangeAction);
        constraint.setCoefficient(isVariationVariable, -(rangeAction.getMaxAdmissibleSetpoint(initialSetpoint) + RANGE_ACTION_SETPOINT_EPSILON - rangeAction.getMinAdmissibleSetpoint(initialSetpoint)));
    }

    private void addMaxRaConstraint(LinearProblem linearProblem, State state) {
        Integer maxRa = rangeActionLimitationParameters.getMaxRangeActions(state);
        if (maxRa == null) {
            return;
        }
        FaraoMPConstraint maxRaConstraint = linearProblem.addMaxRaConstraint(0, maxRa, state);
        rangeActions.get(state).forEach(ra -> {
            FaraoMPVariable isVariationVariable = linearProblem.getRangeActionVariationBinary(ra, state);
            maxRaConstraint.setCoefficient(isVariationVariable, 1);
        });
    }

    private void addMaxTsoConstraint(LinearProblem linearProblem, State state) {
        Integer maxTso = rangeActionLimitationParameters.getMaxTso(state);
        if (maxTso == null) {
            return;
        }
        Set<String> maxTsoExclusions = rangeActionLimitationParameters.getMaxTsoExclusion(state);
        Set<String> constraintTsos = rangeActions.get(state).stream()
            .map(RemedialAction::getOperator)
            .filter(Objects::nonNull)
            .filter(tso -> !maxTsoExclusions.contains(tso))
            .collect(Collectors.toSet());
        FaraoMPConstraint maxTsoConstraint = linearProblem.addMaxTsoConstraint(0, maxTso, state);
        constraintTsos.forEach(tso -> {
            // Create "is at least one RA for TSO used" binary variable ...
            FaraoMPVariable tsoRaUsedVariable = linearProblem.addTsoRaUsedVariable(0, 1, tso, state);
            maxTsoConstraint.setCoefficient(tsoRaUsedVariable, 1);
            // ... and the constraints that will define it
            // tsoRaUsed >= ra1_used, tsoRaUsed >= ra2_used + ...

            rangeActions.get(state).stream().filter(ra -> tso.equals(ra.getOperator()))
                .forEach(ra -> {
                    FaraoMPConstraint tsoRaUsedConstraint = linearProblem.addTsoRaUsedConstraint(0, LinearProblem.infinity(), tso, ra, state);
                    tsoRaUsedConstraint.setCoefficient(tsoRaUsedVariable, 1);
                    tsoRaUsedConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state), -1);
                });
        });
    }

    private void addMaxRaPerTsoConstraint(LinearProblem linearProblem, State state) {
        Map<String, Integer> maxRaPerTso = rangeActionLimitationParameters.getMaxRangeActionPerTso(state);
        if (maxRaPerTso.isEmpty()) {
            return;
        }
        maxRaPerTso.forEach((tso, maxRaForTso) -> {
            FaraoMPConstraint maxRaPerTsoConstraint = linearProblem.addMaxRaPerTsoConstraint(0, maxRaForTso, tso, state);
            rangeActions.get(state).stream().filter(ra -> tso.equals(ra.getOperator()))
                .forEach(ra -> maxRaPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state), 1));
        });
    }

    private void addMaxPstPerTsoConstraint(LinearProblem linearProblem, State state) {
        Map<String, Integer> maxPstPerTso = rangeActionLimitationParameters.getMaxPstPerTso(state);
        if (maxPstPerTso == null) {
            return;
        }
        maxPstPerTso.forEach((tso, maxPstForTso) -> {
            FaraoMPConstraint maxPstPerTsoConstraint = linearProblem.addMaxPstPerTsoConstraint(0, maxPstForTso, tso, state);
            rangeActions.get(state).stream().filter(ra -> ra instanceof PstRangeAction && tso.equals(ra.getOperator()))
                .forEach(ra -> maxPstPerTsoConstraint.setCoefficient(linearProblem.getRangeActionVariationBinary(ra, state), 1));
        });
    }
}
