/*
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class VoltageCnecResultArraySerializer {

    private VoltageCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<VoltageCnec> sortedListOfVoltageCnecs = crac.getVoltageCnecs().stream()
            .sorted(Comparator.comparing(VoltageCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(VOLTAGECNEC_RESULTS);
        for (VoltageCnec voltageCnec : sortedListOfVoltageCnecs) {
            serializeVoltageCnecResult(voltageCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeVoltageCnecResult(VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForVoltageCnec(raoResult, voltageCnec)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(VOLTAGECNEC_ID, voltageCnec.getId());

            serializeVoltageCnecResultForOptimizationState(OptimizationState.INITIAL, voltageCnec, raoResult, jsonGenerator);
            serializeVoltageCnecResultForOptimizationState(OptimizationState.AFTER_PRA, voltageCnec, raoResult, jsonGenerator);

            if (!voltageCnec.getState().isPreventive()) {
                serializeVoltageCnecResultForOptimizationState(OptimizationState.AFTER_ARA, voltageCnec, raoResult, jsonGenerator);
                serializeVoltageCnecResultForOptimizationState(OptimizationState.AFTER_CRA, voltageCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationState(OptimizationState optState, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, voltageCnec, optState)) {
            jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
            serializeVoltageCnecResultForOptimizationStateAndUnit(optState, Unit.KILOVOLT, voltageCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double voltage = safeGetVoltage(raoResult, voltageCnec, optState, unit);
        double margin = safeGetMargin(raoResult, voltageCnec, optState, unit);

        if (Double.isNaN(voltage) && Double.isNaN(margin)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(voltage)) {
            jsonGenerator.writeNumberField(VOLTAGE, voltage);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, margin);
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForVoltageCnec(RaoResult raoResult, VoltageCnec voltageCnec) {

        if (voltageCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.INITIAL) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.AFTER_PRA);
        } else {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.INITIAL) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.AFTER_PRA) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.AFTER_ARA) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.AFTER_CRA);
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState) {
        return !Double.isNaN(safeGetVoltage(raoResult, voltageCnec, optState, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMargin(raoResult, voltageCnec, optState, Unit.KILOVOLT));
    }

    private static double safeGetVoltage(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        try {
            return raoResult.getVoltage(optState, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optState, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
