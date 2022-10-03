/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.farao_community.farao.monitoring.voltage_monitoring.ExtremeVoltageValues;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.monitoring.voltage_monitoring.json.JsonVoltageMonitoringResultConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultSerializer extends JsonSerializer<VoltageMonitoringResult> {

    VoltageMonitoringResultSerializer() {

    }

    @Override
    public void serialize(VoltageMonitoringResult voltageMonitoringResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField(TYPE, VOLTAGE_MONITORING_RESULT);
        jsonGenerator.writeArrayFieldStart(VOLTAGE_VALUES);
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> entry :
            voltageMonitoringResult.getExtremeVoltageValues().entrySet()
                .stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                .collect(Collectors.toList())) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(CNEC_ID, entry.getKey().getId());
            jsonGenerator.writeNumberField(MIN, entry.getValue().getMin());
            jsonGenerator.writeNumberField(MAX, entry.getValue().getMax());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

}
