/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.remedialaction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.openrao.data.craccreation.util.OpenRaoImportException;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.triplestore.api.PropertyBag;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class NetworkActionCreator {
    public static final String NO_STATIC_PROPERTY_RANGE = " will not be imported because there is no StaticPropertyRange linked to that RA";
    private final Crac crac;
    private final Network network;

    public NetworkActionCreator(Crac crac, Network network) {
        this.crac = crac;
        this.network = network;
    }

    public NetworkActionAdder getNetworkActionAdder(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String gridStateAlterationId, String targetRaId) {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction().withId(targetRaId);
        if (linkedTopologyActions.containsKey(gridStateAlterationId)) {
            processLinkedTopologyActions(linkedTopologyActions, staticPropertyRanges, gridStateAlterationId, targetRaId, networkActionAdder);
        }

        if (linkedRotatingMachineActions.containsKey(gridStateAlterationId)) {
            processLinkedRotatingMachineActions(linkedRotatingMachineActions, staticPropertyRanges, gridStateAlterationId, targetRaId, networkActionAdder);
        }

        if (linkedShuntCompensatorModifications.containsKey(gridStateAlterationId)) {
            processLinkedShuntCompensatorModifications(linkedShuntCompensatorModifications, staticPropertyRanges, gridStateAlterationId, targetRaId, networkActionAdder);
        }
        return networkActionAdder;
    }

    private void processLinkedShuntCompensatorModifications(Map<String, Set<PropertyBag>> linkedShuntCompensatorModifications, Map<String, Set<PropertyBag>> staticPropertyRanges, String gridStateAlterationId, String targetRaId, NetworkActionAdder networkActionAdder) {
        for (PropertyBag shuntCompensatorModificationPropertyBag : linkedShuntCompensatorModifications.get(gridStateAlterationId)) {
            if (staticPropertyRanges.containsKey(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID))) {
                addInjectionSetPointFromShuntCompensatorModification(
                    staticPropertyRanges.get(shuntCompensatorModificationPropertyBag.getId(CsaProfileConstants.MRID)),
                    gridStateAlterationId, networkActionAdder, shuntCompensatorModificationPropertyBag);
            } else {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + NO_STATIC_PROPERTY_RANGE);
            }
        }
    }

    private void processLinkedRotatingMachineActions(Map<String, Set<PropertyBag>> linkedRotatingMachineActions, Map<String, Set<PropertyBag>> staticPropertyRanges, String gridStateAlterationId, String targetRaId, NetworkActionAdder networkActionAdder) {
        for (PropertyBag rotatingMachineActionPropertyBag : linkedRotatingMachineActions.get(gridStateAlterationId)) {
            if (staticPropertyRanges.containsKey(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID))) {
                addInjectionSetPointFromRotatingMachineAction(
                    staticPropertyRanges.get(rotatingMachineActionPropertyBag.getId(CsaProfileConstants.MRID)),
                    gridStateAlterationId, networkActionAdder, rotatingMachineActionPropertyBag);
            } else {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + NO_STATIC_PROPERTY_RANGE);
            }
        }
    }

    private void processLinkedTopologyActions(Map<String, Set<PropertyBag>> linkedTopologyActions, Map<String, Set<PropertyBag>> staticPropertyRanges, String gridStateAlterationId, String targetRaId, NetworkActionAdder networkActionAdder) {
        for (PropertyBag topologyActionPropertyBag : linkedTopologyActions.get(gridStateAlterationId)) {
            if (staticPropertyRanges.containsKey(topologyActionPropertyBag.getId(CsaProfileConstants.MRID))) {
                addTopologicalElementaryAction(staticPropertyRanges.get(topologyActionPropertyBag.getId(CsaProfileConstants.MRID)),
                    networkActionAdder, topologyActionPropertyBag, gridStateAlterationId);
            } else {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + targetRaId + NO_STATIC_PROPERTY_RANGE);
            }

        }
    }

    private void addInjectionSetPointFromRotatingMachineAction(Set<PropertyBag> staticPropertyRangesLinkedToRotatingMachineAction, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag rotatingMachineActionPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction");
        CsaProfileCracUtils.checkPropertyReference(rotatingMachineActionPropertyBag, remedialActionId, "RotatingMachineAction", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        String rawId = rotatingMachineActionPropertyBag.get(CsaProfileConstants.ROTATING_MACHINE);
        String rotatingMachineId = rawId.substring(rawId.lastIndexOf("#_") + 2).replace("+", " ");
        float initialSetPoint = getInitialSetPointRotatingMachine(rotatingMachineId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToRotatingMachineAction.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.ROTATING_MACHINE.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, false, initialSetPoint);

        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(setPointValue)
            .withNetworkElement(rotatingMachineId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private void addInjectionSetPointFromShuntCompensatorModification(Set<PropertyBag> staticPropertyRangesLinkedToShuntCompensatorModification, String remedialActionId, NetworkActionAdder networkActionAdder, PropertyBag shuntCompensatorModificationPropertyBag) {
        CsaProfileCracUtils.checkNormalEnabled(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification");
        CsaProfileCracUtils.checkPropertyReference(shuntCompensatorModificationPropertyBag, remedialActionId, "ShuntCompensatorModification", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        String rawId = shuntCompensatorModificationPropertyBag.get(CsaProfileConstants.SHUNT_COMPENSATOR_ID);
        String shuntCompensatorId = rawId.substring(rawId.lastIndexOf("_") + 1);
        float initialSetPoint = getInitialSetPointShuntCompensator(shuntCompensatorId, remedialActionId);

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToShuntCompensatorModification.iterator().next(); // get a random one because there is only one
        CsaProfileCracUtils.checkPropertyReference(staticPropertyRangePropertyBag, remedialActionId, "StaticPropertyRange", CsaProfileConstants.PropertyReference.SHUNT_COMPENSATOR.toString());
        float setPointValue = getSetPointValue(staticPropertyRangePropertyBag, remedialActionId, true, initialSetPoint);
        networkActionAdder.newInjectionSetPoint()
            .withSetpoint(setPointValue)
            .withNetworkElement(shuntCompensatorId)
            .withUnit(Unit.MEGAWATT)
            .add();
    }

    private float getInitialSetPointRotatingMachine(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        Optional<Generator> optionalGenerator = network.getGeneratorStream().filter(gen -> gen.getId().equals(injectionSetPointActionId)).findAny();
        Optional<Load> optionalLoad = findLoad(injectionSetPointActionId);
        if (optionalGenerator.isEmpty() && optionalLoad.isEmpty()) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Network model does not contain a generator, neither a load with id of RotatingMachine: " + injectionSetPointActionId);
        } else if (optionalGenerator.isPresent()) {
            initialSetPoint = (float) optionalGenerator.get().getTargetP();
        } else {
            initialSetPoint = (float) optionalLoad.get().getP0();
        }
        return initialSetPoint;
    }

    private float getInitialSetPointShuntCompensator(String injectionSetPointActionId, String remedialActionId) {
        float initialSetPoint;
        ShuntCompensator shuntCompensator = network.getShuntCompensator(injectionSetPointActionId);
        if (shuntCompensator == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because Network model does not contain a shunt compensator with id of ShuntCompensator: " + injectionSetPointActionId);
        } else {
            initialSetPoint = shuntCompensator.getSectionCount();
        }
        return initialSetPoint;
    }

    private float getSetPointValue(PropertyBag staticPropertyRangePropertyBag, String remedialActionId, boolean mustValueBePositiveInteger, float initialSetPoint) {
        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        checkCompatibility(remedialActionId, valueKind, direction);

        float normalValue;
        try {
            normalValue = Float.parseFloat(staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE));
        } catch (Exception e) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non float-castable normalValue so no set-point value was retrieved");
        }

        float setPointValue;
        if (CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind)) {
            setPointValue = normalValue;
        } else if (normalValue < 0) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
        } else if (CsaProfileConstants.ValueOffsetKind.INCREMENTAL.toString().equals(valueKind)) {
            setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                initialSetPoint + normalValue :
                initialSetPoint - normalValue;
        } else {
            setPointValue = CsaProfileConstants.RelativeDirectionKind.UP.toString().equals(direction) ?
                initialSetPoint + (normalValue * initialSetPoint) / 100 :
                initialSetPoint - (normalValue * initialSetPoint) / 100;
        }

        if (mustValueBePositiveInteger) {
            if (setPointValue < 0) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a negative normalValue so no set-point value was retrieved");
            }
            if (setPointValue != (int) setPointValue) {
                throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has a non integer-castable normalValue so no set-point value was retrieved");
            }
        }
        return setPointValue;
    }

    private static void checkCompatibility(String remedialActionId, String valueKind, String direction) {
        if (CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && !CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)
            || !CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind) && CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)
            || CsaProfileConstants.RelativeDirectionKind.UP_AND_DOWN.toString().equals(direction)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because StaticPropertyRange has wrong values of valueKind and direction");
        }
    }

    private Optional<Load> findLoad(String injectionSetPointId) {
        return network.getLoadStream().filter(load -> load.getId().equals(injectionSetPointId)).findAny();
    }

    private void addTopologicalElementaryAction(Set<PropertyBag> staticPropertyRangesLinkedToTopologicalElementaryAction, NetworkActionAdder networkActionAdder, PropertyBag topologyActionPropertyBag, String remedialActionId) {
        String switchId = topologyActionPropertyBag.getId(CsaProfileConstants.SWITCH);
        if (network.getSwitch(switchId) == null) {
            throw new OpenRaoImportException(ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because network model does not contain a switch with id: " + switchId);
        }
        CsaProfileCracUtils.checkPropertyReference(topologyActionPropertyBag, remedialActionId, "TopologyAction", CsaProfileConstants.PropertyReference.SWITCH.toString());

        PropertyBag staticPropertyRangePropertyBag = staticPropertyRangesLinkedToTopologicalElementaryAction.iterator().next();
        String normalValue = staticPropertyRangePropertyBag.get(CsaProfileConstants.NORMAL_VALUE);
        if (!"0".equals(normalValue) && !"1".equals(normalValue)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because the normalValue is " + normalValue + " which does not define a proper action type (open 1 / close 0)");
        }

        String valueKind = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND);
        if (!CsaProfileConstants.ValueOffsetKind.ABSOLUTE.toString().equals(valueKind)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because the ValueOffsetKind is " + valueKind + " but should be none.");
        }

        String direction = staticPropertyRangePropertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION);
        if (!CsaProfileConstants.RelativeDirectionKind.NONE.toString().equals(direction)) {
            throw new OpenRaoImportException(ImportStatus.INCONSISTENCY_IN_DATA, CsaProfileConstants.REMEDIAL_ACTION_MESSAGE + remedialActionId + " will not be imported because the RelativeDirectionKind is " + direction + " but should be absolute.");
        }

        networkActionAdder.newTopologicalAction()
            .withNetworkElement(switchId)
            .withActionType("0".equals(normalValue) ? ActionType.CLOSE : ActionType.OPEN).add();
    }
}