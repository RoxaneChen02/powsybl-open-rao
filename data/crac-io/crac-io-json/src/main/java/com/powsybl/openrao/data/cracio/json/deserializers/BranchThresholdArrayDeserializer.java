/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class BranchThresholdArrayDeserializer {

    private BranchThresholdArrayDeserializer() {
    }

    public record BranchThreshold(Unit unit, Double min, Double max, TwoSides side) {
    }

    public static Set<BranchThreshold> deserialize(JsonParser jsonParser, Pair<Double, Double> nominalV, String version) throws IOException {
        Set<BranchThreshold> branchThresholds = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            Unit unit = null;
            Double min = null;
            Double max = null;
            TwoSides side = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case UNIT:
                        unit = deserializeUnit(jsonParser.nextTextValue());
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        min = jsonParser.getDoubleValue();
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        max = jsonParser.getDoubleValue();
                        break;
                    case RULE:
                        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                            throw new OpenRaoException("Branch threshold rule is not handled since CRAC version 1.6");
                        } else {
                            side = convertBranchThresholdRuleToSide(jsonParser.nextTextValue(), nominalV);
                        }
                        break;
                    case SIDE:
                        JsonToken sideString = jsonParser.nextToken();
                        if (getPrimaryVersionNumber(version) > 2 || getPrimaryVersionNumber(version) == 2 && getSubVersionNumber(version) > 3) {
                            if (sideString == JsonToken.VALUE_NUMBER_INT) {
                                side = deserializeSide(jsonParser.getIntValue());
                            } else if (Objects.equals(jsonParser.getValueAsString(), LEFT_SIDE) || Objects.equals(jsonParser.getValueAsString(), RIGHT_SIDE)) {
                                throw new OpenRaoException(String.format("Side should be %d/%d and not %s/%s since CRAC version 2.4", SIDE_ONE, SIDE_TWO, LEFT_SIDE, RIGHT_SIDE));
                            } else {
                                throw new OpenRaoException(String.format("Unrecognized sideString %s", jsonParser.getValueAsString()));
                            }
                        } else {
                            side = deserializeSide(jsonParser.getValueAsString());
                        }
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in BranchThreshold: " + jsonParser.getCurrentName());
                }
            }
            branchThresholds.add(new BranchThreshold(unit, min, max, side));
        }
        return branchThresholds;
    }
}
