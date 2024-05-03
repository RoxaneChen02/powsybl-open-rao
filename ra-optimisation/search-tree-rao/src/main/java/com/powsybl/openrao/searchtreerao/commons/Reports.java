/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;

import java.time.Instant;
import java.util.Locale;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class Reports {
    private static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }

    private Reports() {
    }

    public static ReportNode reportRao(String networkId, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("rao", "Running RAO on network '${networkId}'")
                .withUntypedValue("networkId", networkId)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Running RAO on network '{}'", networkId);
        return addedNode;
    }

    public static ReportNode reportRaoFailure(String state, Exception exception, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("raoFailure", "Optimizing state '${state}' failed with message: ${message}")
                .withUntypedValue("state", state)
                .withUntypedValue("message", exception.getMessage())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", state, exception);
        return addedNode;
    }

    public static ReportNode reportInitialSensitivity(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("initialSensitivity", "Initial sensitivity analysis")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Initial sensitivity analysis");
        return addedNode;
    }

    public static ReportNode reportSensitivityAnalysisResults(ReportNode reportNode, String prefix, double cost, double functionalCost, double virtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("sensitivityAnalysisResults", prefix + "cost = ${cost} (functional: ${functionalCost}, virtual: ${virtualCost})")
                .withUntypedValue("cost", cost)
                .withUntypedValue("functionalCost", functionalCost)
                .withUntypedValue("virtualCost", virtualCost)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("{}cost = {} (functional: {}, virtual: {})", prefix, formatDouble(cost), formatDouble(functionalCost), formatDouble(virtualCost));
        return addedNode;

    }

    public static ReportNode reportInitialSensitivityFailure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("initialSensitivityFailure", "Initial sensitivity analysis failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
        return addedNode;
    }

    public static ReportNode reportMostLimitingElement(ReportNode reportNode, int index, String isRelativeMargin, double cnecMargin, Unit unit, String ptdfIfRelative, String cnecNetworkElementName, String cnecStateId, String cnecId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("mostLimitingElement", "Limiting element ${index}:${isRelativeMargin} margin = ${cnecMargin} ${unit}${ptdfIfRelative}, element ${cnecNetworkElementName} at state ${cnecStateId}, CNEC ID = '${cnecId}'")
                .withUntypedValue("index", index)
                .withUntypedValue("isRelativeMargin", isRelativeMargin)
                .withUntypedValue("cnecMargin", cnecMargin)
                .withUntypedValue("unit", unit.toString())
                .withUntypedValue("ptdfIfRelative", ptdfIfRelative)
                .withUntypedValue("cnecNetworkElementName", cnecNetworkElementName)
                .withUntypedValue("cnecStateId", cnecStateId)
                .withUntypedValue("cnecId", cnecId)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info(String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s%s, element %s at state %s, CNEC ID = \"%s\"", index, isRelativeMargin, cnecMargin, unit.toString(), ptdfIfRelative, cnecNetworkElementName, cnecStateId, cnecId));
        return addedNode;
    }

    public static ReportNode reportPreventivePerimeter(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeter", "Preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreventivePerimeterEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeterEnd", "End of preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportPreCurativeSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preCurativeSensitivityFailure", "Systematic sensitivity analysis after preventive remedial actions failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
        return addedNode;
    }

    public static ReportNode reportDoNotOptimizeCurativeBecausePreventiveUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("doNotOptimizeCurativeBecausePreventiveUnsecure", "Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimeters(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postContingencyPerimeters", "Post contingency perimeters optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimetersEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postContingencyPerimetersEnd", "End of post contingency perimeters optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("resultsMergingPreventiveAndPostContingency", "Merging preventive and post-contingency RAO results")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveFailed", "Second preventive failed. Falling back to previous solution")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFixedSituation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveFixedSituation", "RAO has succeeded thanks to second preventive step when first preventive step had failed")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveIncreasedOverallCost(ReportNode reportNode, double firstPreventiveCost, double firstPreventiveFunctionalCost, double firstPreventiveVirtualCost, double secondPreventiveCost, double secondPreventiveFunctionalCost, double secondPreventiveVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveIncreasedOverallCost", "Second preventive step has increased the overall cost from ${firstPreventiveCost} (functional: ${firstPreventiveFunctionalCost}, virtual: ${firstPreventiveVirtualCost}) to ${secondPreventiveCost} (functional: ${secondPreventiveFunctionalCost}, virtual: ${secondPreventiveVirtualCost}). Falling back to previous solution:")
                .withUntypedValue("firstPreventiveCost", firstPreventiveCost)
                .withUntypedValue("firstPreventiveFunctionalCost", firstPreventiveFunctionalCost)
                .withUntypedValue("firstPreventiveVirtualCost", firstPreventiveVirtualCost)
                .withUntypedValue("secondPreventiveCost", secondPreventiveCost)
                .withUntypedValue("secondPreventiveFunctionalCost", secondPreventiveFunctionalCost)
                .withUntypedValue("secondPreventiveVirtualCost", secondPreventiveVirtualCost)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
                formatDouble(firstPreventiveCost), formatDouble(firstPreventiveFunctionalCost), formatDouble(firstPreventiveVirtualCost),
                formatDouble(secondPreventiveCost), formatDouble(secondPreventiveFunctionalCost), formatDouble(secondPreventiveVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoIncreasedOverallCost(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveIncreasedOverallCost", "RAO has increased the overall cost from ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}) to ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost}). Falling back to initial solution:")
                .withUntypedValue("initialCost", initialCost)
                .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
                .withUntypedValue("initialVirtualCost", initialVirtualCost)
                .withUntypedValue("finalCost", finalCost)
                .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
                .withUntypedValue("finalVirtualCost", finalVirtualCost)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
                formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoCostEvolution(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("raoCostEvolution", "Cost before RAO = ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}), cost after RAO = ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost})")
                .withUntypedValue("initialCost", initialCost)
                .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
                .withUntypedValue("initialVirtualCost", initialVirtualCost)
                .withUntypedValue("finalCost", finalCost)
                .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
                .withUntypedValue("finalVirtualCost", finalVirtualCost)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
                formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondAutomatonSimulation", "Second automaton simulation")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondAutomatonSimulationEnd", "End of second automaton simulation")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveFirstAndSecondAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("resultsMergingPreventiveFirstAndSecondAndPostContingency", "Merging first, second preventive and post-contingency RAO results")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportPostRaoSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postRaoSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimization(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveOptimization", "Second preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportPostSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimizationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveOptimizationEnd", "End of second preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportRangeActionRemovedFromSecondCurative(ReportNode reportNode, String rangeActionId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("rangeActionRemovedFromSecondCurative", "Range action ${rangeActionId} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)")
                .withUntypedValue("rangeActionId", rangeActionId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)", rangeActionId);
        return addedNode;
    }

    public static ReportNode reportNotEnoughTimeForSecondPreventive(ReportNode reportNode, Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("notEnoughTimeForSecondPreventive", "There is not enough time to run a 2nd preventive RAO (target end time: ${targetEndInstant}, estimated time needed based on first preventive RAO: ${estimatedPreventiveRaoTimeInSeconds} seconds)")
                .withUntypedValue("targetEndInstant", targetEndInstant.toString())
                .withUntypedValue("estimatedPreventiveRaoTimeInSeconds", estimatedPreventiveRaoTimeInSeconds)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao", "Cost has not increased during RAO, there is no need to run a 2nd preventive RAO")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure", "First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
        return addedNode;
    }
}
