/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.linear_rao.fillers.CoreProblemFiller;
import com.farao_community.farao.linear_rao.fillers.PositiveMinMarginFiller;
import com.farao_community.farao.linear_rao.post_processors.PstTapPostProcessor;
import com.farao_community.farao.linear_rao.post_processors.RaoResultPostProcessor;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoModeller {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRaoModeller.class);

    private LinearRaoProblem linearRaoProblem;
    private Crac crac;
    private Network network;
    private LinearRaoData linearRaoData;
    private List<AbstractProblemFiller> fillerList;
    private List<AbstractPostProcessor> postProcessorList;

    public LinearRaoModeller(Crac crac,
                             Network network,
                             SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult,
                             LinearRaoProblem linearRaoProblem) {
        this.crac = crac;
        this.network = network;
        this.linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);
        this.linearRaoProblem = linearRaoProblem;

        // TODO : load the filler list from the config file and make sure they are ordered properly
        fillerList = new ArrayList<>();
        fillerList.add(new CoreProblemFiller(linearRaoProblem, linearRaoData));
        fillerList.add(new PositiveMinMarginFiller(linearRaoProblem, linearRaoData));

        postProcessorList = new ArrayList<>();
        postProcessorList.add(new PstTapPostProcessor());
        postProcessorList.add(new RaoResultPostProcessor());
    }

    public void buildProblem() {
        fillerList.forEach(AbstractProblemFiller::fill);
    }

    public void updateProblem(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult, List<RemedialActionResult> remedialActionResultList) {
        this.linearRaoData = new LinearRaoData(crac, network, systematicSensitivityAnalysisResult);
        fillerList.forEach(filler -> filler.update(linearRaoProblem, linearRaoData, remedialActionResultList));
    }

    public RaoComputationResult solve() {
        Enum solverResultStatus = linearRaoProblem.solve();
        RaoComputationResult raoComputationResult;
        String solverResultStatusString = solverResultStatus.name();
        if (solverResultStatusString.equals("OPTIMAL")) {
            RaoComputationResult.Status status = RaoComputationResult.Status.SUCCESS;
            raoComputationResult = new RaoComputationResult(status);
            postProcessorList.forEach(postProcessor -> postProcessor.process(linearRaoProblem, linearRaoData, raoComputationResult));
        } else {
            RaoComputationResult.Status status = RaoComputationResult.Status.FAILURE;
            raoComputationResult = new RaoComputationResult(status);
            LOGGER.warn("Linear rao computation failed: MPSolver status is {}", solverResultStatusString);
        }
        return raoComputationResult;
    }
}