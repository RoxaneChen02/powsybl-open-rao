/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.cse.remedialaction;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import com.powsybl.openrao.data.craccreation.creator.api.ImportStatus;
import com.powsybl.openrao.data.craccreation.creator.cse.*;
import com.powsybl.openrao.data.craccreation.creator.cse.xsd.*;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.BusBarChangeSwitches;
import com.powsybl.openrao.data.craccreation.creator.cse.parameters.CseCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.RangeActionGroup;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.craccreation.util.ucte.UctePstHelper;
import com.powsybl.openrao.data.craccreation.util.ucte.UcteTopologicalElementHelper;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class TRemedialActionAdder {
    private final TCRACSeries tcracSeries;
    private final Crac crac;
    private final Network network;
    private final UcteNetworkAnalyzer ucteNetworkAnalyzer;
    private final CseCracCreationContext cseCracCreationContext;
    private final Map<String, Set<String>> remedialActionsForCnecsMap;
    private final CseCracCreationParameters cseCracCreationParameters;

    private static final String ABSOLUTE_VARIATION_TYPE = "ABSOLUTE";

    public TRemedialActionAdder(TCRACSeries tcracSeries, Crac crac, Network network, UcteNetworkAnalyzer ucteNetworkAnalyzer, Map<String, Set<String>> remedialActionsForCnecsMap, CseCracCreationContext cseCracCreationContext, CseCracCreationParameters cseCracCreationParameters) {
        this.tcracSeries = tcracSeries;
        this.crac = crac;
        this.network = network;
        this.ucteNetworkAnalyzer = ucteNetworkAnalyzer;
        this.cseCracCreationContext = cseCracCreationContext;
        this.remedialActionsForCnecsMap = remedialActionsForCnecsMap;
        this.cseCracCreationParameters = cseCracCreationParameters;
    }

    public void add() {
        List<TRemedialActions> tRemedialActionsList = tcracSeries.getRemedialActions();
        for (TRemedialActions tRemedialActions : tRemedialActionsList) {
            if (tRemedialActions != null) {
                tRemedialActions.getRemedialAction().forEach(tRemedialAction -> {
                    try {
                        importRemedialAction(tRemedialAction);
                    } catch (OpenRaoException e) {
                        // unsupported remedial action type
                        cseCracCreationContext.addRemedialActionCreationContext(
                            CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, e.getMessage())
                        );
                    }
                });
            }
        }
    }

    private void importRemedialAction(TRemedialAction tRemedialAction) {
        if (tRemedialAction.getStatus() != null) {
            importTopologicalAction(tRemedialAction);
        } else if (tRemedialAction.getGeneration() != null) {
            importInjectionAction(tRemedialAction);
        } else if (tRemedialAction.getPstRange() != null) {
            importPstRangeAction(tRemedialAction);
        } else if (tRemedialAction.getHVDCRange() != null) {
            importHvdcRangeAction(tRemedialAction);
        } else if (tRemedialAction.getBusBar() != null) {
            importBusBarChangeAction(tRemedialAction);
        } else {
            throw new OpenRaoException("unknown remedial action type");
        }
    }

    private void importTopologicalAction(TRemedialAction tRemedialAction) {
        String createdRAId = tRemedialAction.getName().getV();
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(createdRAId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV());

        if (tRemedialAction.getStatus().getBranch().isEmpty()) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.INCOMPLETE_DATA, "field 'Status' of a topological remedial action cannot be empty"));
            return;
        }

        for (TBranch tBranch : tRemedialAction.getStatus().getBranch()) {
            UcteTopologicalElementHelper branchHelper = new UcteTopologicalElementHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), createdRAId, ucteNetworkAnalyzer);
            if (!branchHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, branchHelper.getInvalidReason()));
                return;
            }
            networkActionAdder.newTopologicalAction()
                .withNetworkElement(branchHelper.getIdInNetwork())
                .withActionType(convertActionType(tBranch.getStatus()))
                .add();
        }

        addTriggerConditions(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, createdRAId, false, null));
    }

    private void importInjectionAction(TRemedialAction tRemedialAction) {
        String createdRAId = tRemedialAction.getName().getV();

        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(createdRAId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV());

        boolean isAltered = false;
        StringBuilder alteringDetail = null;
        for (TNode tNode : tRemedialAction.getGeneration().getNode()) {
            if (!tNode.getVariationType().getV().equals(ABSOLUTE_VARIATION_TYPE)) {
                cseCracCreationContext.addRemedialActionCreationContext(
                    CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, String.format("node %s is not defined as an ABSOLUTE injectionSetpoint (only ABSOLUTE is implemented).", tNode.getName().getV()))
                );
                return;
            }

            GeneratorHelper generatorHelper = new GeneratorHelper(tNode.getName().getV(), ucteNetworkAnalyzer);
            if (!generatorHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, generatorHelper.getImportStatus(), generatorHelper.getDetail()));
                return;
            } else if (generatorHelper.isAltered()) {
                isAltered = true;
                if (alteringDetail == null) {
                    alteringDetail = Optional.ofNullable(generatorHelper.getDetail()).map(StringBuilder::new).orElse(null);
                } else {
                    alteringDetail.append(", ").append(generatorHelper.getDetail());
                }
            }
            try {
                networkActionAdder.newInjectionSetPoint()
                    .withNetworkElement(generatorHelper.getGeneratorId())
                    .withSetpoint(tNode.getValue().getV())
                    .withUnit(Unit.MEGAWATT)
                    .add();
            } catch (OpenRaoException e) {
                cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.OTHER, e.getMessage()));
                return;
            }

        }
        // After looping on all nodes
        addTriggerConditions(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, createdRAId, isAltered, alteringDetail == null ? null : alteringDetail.toString()));
    }

    private void importPstRangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();
        tRemedialAction.getPstRange().getBranch().forEach(tBranch -> {
            UctePstHelper pstHelper = new UctePstHelper(tBranch.getFromNode().getV(), tBranch.getToNode().getV(), String.valueOf(tBranch.getOrder().getV()), raId, ucteNetworkAnalyzer);
            if (!pstHelper.isValid()) {
                cseCracCreationContext.addRemedialActionCreationContext(CsePstCreationContext.notImported(tRemedialAction, pstHelper.getUcteId(), ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, pstHelper.getInvalidReason()));
                return;
            }
            String id = "PST_" + raId + "_" + pstHelper.getIdInNetwork();
            int pstInitialTap = pstHelper.getInitialTap();
            Map<Integer, Double> conversionMap = pstHelper.getTapToAngleConversionMap();

            PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction()
                .withId(id)
                .withName(tRemedialAction.getName().getV())
                .withOperator(tRemedialAction.getOperator().getV())
                .withNetworkElement(pstHelper.getIdInNetwork())
                .withInitialTap(pstInitialTap)
                .withTapToAngleConversionMap(conversionMap)
                .newTapRange()
                .withMinTap(tRemedialAction.getPstRange().getMin().getV())
                .withMaxTap(tRemedialAction.getPstRange().getMax().getV())
                .withRangeType(convertRangeType(tRemedialAction.getPstRange().getVariationType()))
                .add();

            addTriggerConditions(pstRangeActionAdder, tRemedialAction);
            pstRangeActionAdder.add();
            String nativeNetworkElementId = String.format("%1$-8s %2$-8s %3$s", pstHelper.getOriginalFrom(), pstHelper.getOriginalTo(), pstHelper.getSuffix());
            cseCracCreationContext.addRemedialActionCreationContext(CsePstCreationContext.imported(tRemedialAction, nativeNetworkElementId, id, false, null));
        });
    }

    private void importHvdcRangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();

        // ----  HVDC Nodes
        THVDCNode hvdcNodes = tRemedialAction.getHVDCRange().getHVDCNode().get(0);
        GeneratorHelper generatorFromHelper = new GeneratorHelper(hvdcNodes.getFromNode().getV(), ucteNetworkAnalyzer);
        GeneratorHelper generatorToHelper = new GeneratorHelper(hvdcNodes.getToNode().getV(), ucteNetworkAnalyzer);

        // ---- Only handle ABSOLUTE variation type
        if (!tRemedialAction.getHVDCRange().getVariationType().getV().equals(ABSOLUTE_VARIATION_TYPE)) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO,
                    String.format("HVDC %s is not defined with an ABSOLUTE variation type (only ABSOLUTE is handled)", raId),
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- Only handle one HVDC Node
        if (tRemedialAction.getHVDCRange().getHVDCNode().size() > 1) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.INCONSISTENCY_IN_DATA,
                    String.format("HVDC %s has %s (>1) HVDC nodes", raId, tRemedialAction.getHVDCRange().getHVDCNode().size()),
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- check if generators are present
        if (!generatorFromHelper.isValid() || !generatorToHelper.isValid()) {

            String importStatusDetails;
            if (generatorToHelper.isValid()) {
                importStatusDetails = generatorFromHelper.getDetail();
            } else if (generatorFromHelper.isValid()) {
                importStatusDetails = generatorToHelper.getDetail();
            } else {
                importStatusDetails = generatorFromHelper.getDetail() + " & " + generatorToHelper.getDetail();
            }

            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK,
                    importStatusDetails,
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- check if generator have inverted signs
        if (Math.abs(generatorFromHelper.getCurrentP() + generatorToHelper.getCurrentP()) > 0.1) {
            cseCracCreationContext.addRemedialActionCreationContext(
                CseHvdcCreationContext.notImported(tRemedialAction,
                    ImportStatus.INCONSISTENCY_IN_DATA,
                    "the two generators of the HVDC must have opposite power output values",
                    hvdcNodes.getFromNode().getV(),
                    hvdcNodes.getToNode().getV()));
            return;
        }

        // ---- create range action
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId(raId)
            .withName(tRemedialAction.getName().getV())
            .withOperator(tRemedialAction.getOperator().getV())
            .withNetworkElementAndKey(-1., generatorFromHelper.getGeneratorId())
            .withNetworkElementAndKey(1., generatorToHelper.getGeneratorId())
            .withInitialSetpoint(generatorToHelper.getCurrentP())
            .newRange()
            .withMin(tRemedialAction.getHVDCRange().getMin().getV())
            .withMax(tRemedialAction.getHVDCRange().getMax().getV())
            .add()
            .newRange()
            .withMin(Math.max(-generatorFromHelper.getPmax(), generatorToHelper.getPmin()))
            .withMax(Math.min(-generatorFromHelper.getPmin(), generatorToHelper.getPmax()))
            .add();

        // ---- add groupId if present
        if (cseCracCreationParameters != null && cseCracCreationParameters.getRangeActionGroups() != null) {
            List<RangeActionGroup> groups = cseCracCreationParameters.getRangeActionGroups().stream()
                .filter(rangeActionGroup -> rangeActionGroup.getRangeActionsIds().contains(raId))
                .toList();
            if (groups.size() == 1) {
                injectionRangeActionAdder.withGroupId(groups.get(0).toString());
            } else if (groups.size() > 1) {
                injectionRangeActionAdder.withGroupId(groups.get(0).toString());
                cseCracCreationContext.getCreationReport().warn(String.format("GroupId defined multiple times for HVDC %s, only group %s is used.", raId, groups.get(0)));
            }
        }

        addTriggerConditions(injectionRangeActionAdder, tRemedialAction);
        injectionRangeActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseHvdcCreationContext.imported(tRemedialAction,
            raId,
            hvdcNodes.getFromNode().getV(),
            generatorFromHelper.getGeneratorId(),
            hvdcNodes.getToNode().getV(),
            generatorToHelper.getGeneratorId()));
    }

    private static ActionType convertActionType(TStatusType tStatusType) {
        switch (tStatusType.getV()) {
            case "CLOSE":
                return ActionType.CLOSE;
            case "OPEN":
            default:
                return ActionType.OPEN;
        }
    }

    private static RangeType convertRangeType(TVariationType tVariationType) {
        if (tVariationType.getV().equals(ABSOLUTE_VARIATION_TYPE)) {
            return RangeType.ABSOLUTE;
        } else {
            throw new OpenRaoException(String.format("%s type is not handled by the importer", tVariationType.getV()));
        }
    }

    private Instant getInstant(TApplication tApplication) {
        switch (tApplication.getV()) {
            case "PREVENTIVE":
                return crac.getPreventiveInstant();
            case "SPS":
                return crac.getInstant(InstantKind.AUTO);
            case "CURATIVE":
                return crac.getInstant(InstantKind.CURATIVE);
            default:
                throw new OpenRaoException(String.format("%s is not a recognized application type for remedial action", tApplication.getV()));
        }
    }

    void addTriggerConditions(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction) {
        Instant raApplicationInstant = getInstant(tRemedialAction.getApplication());
        addOnFlowConstraintTriggerConditions(remedialActionAdder, tRemedialAction, raApplicationInstant);

        // According to <SharedWith> tag :
        String sharedWithId = tRemedialAction.getSharedWith().getV();
        if (sharedWithId.equals("CSE")) {
            if (raApplicationInstant.isAuto()) {
                throw new OpenRaoException("Cannot import automatons from CSE CRAC yet");
            } else {
                addOnInstantTriggerConditions(remedialActionAdder, raApplicationInstant);
            }
        } else {
            addOnFlowConstraintTriggerConditionsAfterSpecificCountry(remedialActionAdder, tRemedialAction, raApplicationInstant, sharedWithId);
        }
    }

    private void addOnFlowConstraintTriggerConditionsAfterSpecificCountry(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction, Instant raApplicationInstant, String sharedWithId) {
        // Check that sharedWithID is a UCTE country
        if (sharedWithId.equals("None")) {
            return;
        }

        Country country;
        try {
            country = Country.valueOf(sharedWithId);
        } catch (IllegalArgumentException e) {
            cseCracCreationContext.getCreationReport().removed(String.format("RA %s has a non-UCTE sharedWith country : %s. The trigger condition was not created.", tRemedialAction.getName().getV(), sharedWithId));
            return;
        }

        // RA is available for specific UCTE country
        remedialActionAdder.newTriggerCondition()
            .withInstant(raApplicationInstant.getId())
            .withUsageMethod(UsageMethod.AVAILABLE)
            .withCountry(country)
            .add();
    }

    private void addOnInstantTriggerConditions(RemedialActionAdder<?> remedialActionAdder, Instant raApplicationInstant) {
        // RA is available for all countries
        remedialActionAdder.newTriggerCondition()
            .withInstant(raApplicationInstant.getId())
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    private void addOnFlowConstraintTriggerConditions(RemedialActionAdder<?> remedialActionAdder, TRemedialAction tRemedialAction, Instant raApplicationInstant) {
        if (remedialActionsForCnecsMap.containsKey(tRemedialAction.getName().getV())) {
            for (String flowCnecId : remedialActionsForCnecsMap.get(tRemedialAction.getName().getV())) {
                // Only add the trigger condition if the RemedialAction can be applied before or during CNEC instant
                if (!crac.getFlowCnec(flowCnecId).getState().getInstant().comesBefore(raApplicationInstant)) {
                    remedialActionAdder.newTriggerCondition()
                        .withInstant(raApplicationInstant.getId())
                        .withUsageMethod(UsageMethod.AVAILABLE)
                        .withCnec(flowCnecId)
                        .add();
                }
            }
        }
    }

    void importBusBarChangeAction(TRemedialAction tRemedialAction) {
        String raId = tRemedialAction.getName().getV();
        if (cseCracCreationParameters == null || cseCracCreationParameters.getBusBarChangeSwitches(raId) == null) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.INCOMPLETE_DATA, "CSE CRAC creation parameters is missing or does not contain information for the switches to open/close"));
            return;
        }

        BusBarChangeSwitches busBarChangeSwitches = cseCracCreationParameters.getBusBarChangeSwitches(raId);

        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId(raId)
            .withOperator(tRemedialAction.getOperator().getV());
        try {
            busBarChangeSwitches.getSwitchPairs().forEach(switchPairId -> {
                assertIsSwitch(switchPairId.getSwitchToOpenId());
                assertIsSwitch(switchPairId.getSwitchToCloseId());
                networkActionAdder.newSwitchPair()
                    .withSwitchToOpen(switchPairId.getSwitchToOpenId())
                    .withSwitchToClose(switchPairId.getSwitchToCloseId())
                    .add();
            });
        } catch (OpenRaoException e) {
            cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.notImported(tRemedialAction, ImportStatus.ELEMENT_NOT_FOUND_IN_NETWORK, e.getMessage()));
            return;
        }
        addTriggerConditions(networkActionAdder, tRemedialAction);
        networkActionAdder.add();
        cseCracCreationContext.addRemedialActionCreationContext(CseRemedialActionCreationContext.imported(tRemedialAction, raId, false, null));
    }

    private void assertIsSwitch(String switchId) {
        UcteTopologicalElementHelper topoHelper = new UcteTopologicalElementHelper(switchId, ucteNetworkAnalyzer);
        if (!topoHelper.isValid()) {
            throw new OpenRaoException(topoHelper.getInvalidReason());
        }
        if (network.getSwitch(topoHelper.getIdInNetwork()) == null) {
            throw new OpenRaoException(String.format("%s is not a switch", switchId));
        }
    }
}
