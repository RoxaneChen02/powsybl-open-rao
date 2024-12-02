/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class RaoResultJsonConstants {

    private RaoResultJsonConstants() {
    }

    public static final String RAO_RESULT_IO_VERSION = "1.6";
    // v1.6 : voltage cnecs' voltage values are divided into minVoltage and maxVoltage

    // header
    public static final String TYPE = "type";
    public static final String VERSION = "version";
    public static final String INFO = "info";
    public static final String RAO_RESULT_TYPE = "RAO_RESULT";
    public static final String RAO_RESULT_INFO = "Generated by Open RAO http://farao-community.github.io";

    public static final String CONTINGENCY_ID = "contingency";

    // costs
    public static final String COST_RESULTS = "costResults";
    public static final String FUNCTIONAL_COST = "functionalCost";
    public static final String VIRTUAL_COSTS = "virtualCost";

    // flowCnecResults and angleCnecResults
    public static final String FLOWCNEC_RESULTS = "flowCnecResults";
    public static final String FLOWCNEC_ID = "flowCnecId";
    public static final String FLOW = "flow";
    public static final String ANGLECNEC_RESULTS = "angleCnecResults";
    public static final String ANGLECNEC_ID = "angleCnecId";
    public static final String ANGLE = "angle";
    public static final String VOLTAGECNEC_RESULTS = "voltageCnecResults";
    public static final String VOLTAGECNEC_ID = "voltageCnecId";
    public static final String VOLTAGE = "voltage";
    public static final String MIN_VOLTAGE = "minVoltage";
    public static final String MAX_VOLTAGE = "maxVoltage";
    public static final String MARGIN = "margin";
    public static final String RELATIVE_MARGIN = "relativeMargin";
    public static final String COMMERCIAL_FLOW = "commercialFlow";
    public static final String LOOP_FLOW = "loopFlow";
    public static final String ZONAL_PTDF_SUM = "zonalPtdfSum";

    // remedial action results
    public static final String STATES_ACTIVATED = "activatedStates";

    // networkActionResults
    public static final String NETWORKACTION_RESULTS = "networkActionResults";
    public static final String NETWORKACTION_ID = "networkActionId";

    // rangeActionResults
    public static final String PSTRANGEACTION_RESULTS = "pstRangeActionResults";
    public static final String PSTRANGEACTION_ID = "pstRangeActionId";
    public static final String STANDARDRANGEACTION_RESULTS = "standardRangeActionResults";
    public static final String RANGEACTION_RESULTS = "rangeActionResults";
    public static final String RANGEACTION_ID = "rangeActionId";
    public static final String INITIAL_TAP = "initialTap";
    public static final String INITIAL_SETPOINT = "initialSetpoint";
    public static final String AFTER_PRA_TAP = "afterPraTap";
    public static final String AFTER_PRA_SETPOINT = "afterPraSetpoint";
    public static final String TAP = "tap";
    public static final String SETPOINT = "setpoint";

    // instants
    public static final String INSTANT = "instant";
    public static final String INITIAL_INSTANT_ID = "initial";
    public static final String PREVENTIVE_INSTANT_ID = "preventive";
    public static final String OUTAGE_INSTANT_ID = "outage";
    public static final String AUTO_INSTANT_ID = "auto";
    public static final String CURATIVE_INSTANT_ID = "curative";

    // units
    public static final String AMPERE_UNIT = "ampere";
    public static final String MEGAWATT_UNIT = "megawatt";
    public static final String DEGREE_UNIT = "degree";
    public static final String KILOVOLT_UNIT = "kilovolt";
    public static final String PERCENT_IMAX_UNIT = "percent_imax";
    public static final String TAP_UNIT = "tap";

    // branch side
    public static final String LEFT_SIDE = "leftSide";
    public static final String RIGHT_SIDE = "rightSide";
    public static final String SIDE_ONE = "side1";
    public static final String SIDE_TWO = "side2";

    // optimization states - for retro-compatibility only
    public static final String INITIAL_OPT_STATE = "initial";
    public static final String AFTER_PRA_OPT_STATE = "afterPRA";
    public static final String AFTER_ARA_OPT_STATE = "afterARA";
    public static final String AFTER_CRA_OPT_STATE = "afterCRA";

    // computation statuses
    public static final String COMPUTATION_STATUS = "computationStatus";
    public static final String DEFAULT_STATUS = "default";
    public static final String PARTIAL_FAILURE_STATUS = "partial-failure";
    public static final String FAILURE_STATUS = "failure";
    public static final String COMPUTATION_STATUS_MAP = "computationStatusMap";

    // optimized steps executed by the RAO
    public static final String OPTIMIZATION_STEPS_EXECUTED = "optimizationStepsExecuted";
    public static final String EXECUTION_DETAILS = "executionDetails";
    public static final String FIRST_PREVENTIVE_ONLY = "The RAO only went through first preventive";
    public static final String FIRST_PREVENTIVE_FELLBACK = "First preventive fellback to initial situation";
    public static final String SECOND_PREVENTIVE_IMPROVED_FIRST = "Second preventive improved first preventive results";
    public static final String SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION = "Second preventive fellback to first preventive results";
    public static final String SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION = "Second preventive fellback to initial situation";

    // manipulate version
    public static int getPrimaryVersionNumber(String fullVersion) {
        return Integer.parseInt(divideVersionNumber(fullVersion)[0]);
    }

    public static int getSubVersionNumber(String fullVersion) {
        return Integer.parseInt(divideVersionNumber(fullVersion)[1]);
    }

    private static String[] divideVersionNumber(String fullVersion) {
        String[] dividedV = fullVersion.split("\\.");
        if (dividedV.length != 2 || !Arrays.stream(dividedV).allMatch(StringUtils::isNumeric)) {
            throw new OpenRaoException("json CRAC version number must be of the form vX.Y");
        }
        return dividedV;
    }

    // serialization of enums
    public static String serializeUnit(Unit unit) {
        switch (unit) {
            case AMPERE:
                return AMPERE_UNIT;
            case DEGREE:
                return DEGREE_UNIT;
            case MEGAWATT:
                return MEGAWATT_UNIT;
            case KILOVOLT:
                return KILOVOLT_UNIT;
            case PERCENT_IMAX:
                return PERCENT_IMAX_UNIT;
            case TAP:
                return TAP_UNIT;
            default:
                throw new OpenRaoException(String.format("Unsupported unit %s", unit));
        }
    }

    public static Unit deserializeUnit(String stringValue) {
        switch (stringValue) {
            case AMPERE_UNIT:
                return Unit.AMPERE;
            case DEGREE_UNIT:
                return Unit.DEGREE;
            case MEGAWATT_UNIT:
                return Unit.MEGAWATT;
            case KILOVOLT_UNIT:
                return Unit.KILOVOLT;
            case PERCENT_IMAX_UNIT:
                return Unit.PERCENT_IMAX;
            case TAP_UNIT:
                return Unit.TAP;
            default:
                throw new OpenRaoException(String.format("Unrecognized unit %s", stringValue));
        }
    }

    // serialization of enums
    public static String serializeSide(TwoSides side) {
        return switch (side) {
            case ONE -> SIDE_ONE;
            case TWO -> SIDE_TWO;
        };
    }

    public static String serializeInstantId(Instant instant) {
        if (instant == null) {
            return INITIAL_INSTANT_ID;
        }
        return instant.getId();
    }

    public static Instant deserializeOptimizedInstant(String stringValue, String jsonFileVersion, Crac crac) {
        String instantId = deserializeOptimizedInstantId(stringValue, jsonFileVersion, crac);
        if (Objects.equals(instantId, INITIAL_INSTANT_ID)) {
            return null;
        }
        return crac.getInstant(instantId);
    }

    public static String deserializeOptimizedInstantId(String stringValue, String jsonFileVersion, Crac crac) {
        int primaryVersionNumber = getPrimaryVersionNumber(jsonFileVersion);
        int subVersionNumber = getSubVersionNumber(jsonFileVersion);
        if (primaryVersionNumber <= 1 && subVersionNumber <= 3) {
            switch (stringValue) {
                case INITIAL_OPT_STATE:
                    return INITIAL_INSTANT_ID;
                case AFTER_PRA_OPT_STATE:
                    return PREVENTIVE_INSTANT_ID;
                case AFTER_ARA_OPT_STATE:
                    return (primaryVersionNumber == 1 && subVersionNumber == 1 && !crac.hasAutoInstant()) ? PREVENTIVE_INSTANT_ID : AUTO_INSTANT_ID;
                case AFTER_CRA_OPT_STATE:
                    return CURATIVE_INSTANT_ID;
                default:
                    throw new OpenRaoException(String.format("Unrecognized optimization state %s", stringValue));
            }
        } else {
            return stringValue;
        }
    }

    public static String serializeStatus(ComputationStatus computationStatus) {
        return switch (computationStatus) {
            case DEFAULT -> DEFAULT_STATUS;
            case PARTIAL_FAILURE -> PARTIAL_FAILURE_STATUS;
            case FAILURE -> FAILURE_STATUS;
            default ->
                throw new OpenRaoException(String.format("Unsupported computation status %s", computationStatus));
        };
    }

    public static ComputationStatus deserializeStatus(String stringValue) {
        return switch (stringValue) {
            case DEFAULT_STATUS -> ComputationStatus.DEFAULT;
            case PARTIAL_FAILURE_STATUS -> ComputationStatus.PARTIAL_FAILURE;
            case FAILURE_STATUS -> ComputationStatus.FAILURE;
            default -> throw new OpenRaoException(String.format("Unrecognized computation status %s", stringValue));
        };
    }

    // state comparator
    public static final Comparator<State> STATE_COMPARATOR = (s1, s2) -> {
        if (s1.getInstant().getOrder() != s2.getInstant().getOrder()) {
            return s1.compareTo(s2);
        } else if (s1.getInstant().isPreventive()) {
            return 0;
        } else {
            // Since instant is not preventive, there is a contingency for sure
            return s1.getContingency().get().getId().compareTo(s2.getContingency().get().getId());
        }
    };

}
