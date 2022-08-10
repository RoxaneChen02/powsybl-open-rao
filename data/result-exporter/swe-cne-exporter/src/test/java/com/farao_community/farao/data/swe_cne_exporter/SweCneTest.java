/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.RangeActionSpeed;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultComparisonFormatter;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.Assert.assertFalse;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SweCneTest {
    private Crac crac;
    private CracCreationContext cracCreationContext;
    private Network network;
    private RaoResult raoResult;

    @Before
    public void setUp() {
        network = Importers.loadNetwork(new File(SweCneTest.class.getResource("/TestCase16NodesWith2Hvdc.xiidm").getFile()).toString());
        InputStream is = getClass().getResourceAsStream("/CIM_CRAC.xml");
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 1));
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters = Mockito.spy(cracCreationParameters);
        CimCracCreationParameters cimCracCreationParameters = Mockito.mock(CimCracCreationParameters.class);
        Mockito.when(cracCreationParameters.getExtension(CimCracCreationParameters.class)).thenReturn(cimCracCreationParameters);
        Mockito.when(cimCracCreationParameters.getRangeActionSpeedSet()).thenReturn(rangeActionSpeeds);

        cracCreationContext = cimCracCreator.createCrac(cimCrac, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), cracCreationParameters);
        crac = cracCreationContext.getCrac();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(SweCneTest.class.getResource("/RaoResult.json").getFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        raoResult = new RaoResultImporter().importRaoResult(inputStream, crac);
    }

    @Test
    public void testExport() {
        CneExporterParameters params = new CneExporterParameters(
            "documentId", 3, "domainId", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "senderId", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
            "receiverId", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "2021-04-02T12:00:00Z/2021-04-02T13:00:00Z");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new SweCneExporter().exportCne(crac, network, (CimCracCreationContext) cracCreationContext, raoResult, new RaoParameters(), params, outputStream);
        try {
            InputStream inputStream = new FileInputStream(SweCneTest.class.getResource("/SweCNE.xml").getFile());
            compareCneFiles(new ByteArrayInputStream(outputStream.toByteArray()), inputStream);
        } catch (IOException e) {
            Assert.fail();
        }
    }

    public static void compareCneFiles(InputStream expectedCneInputStream, InputStream actualCneInputStream) throws AssertionError {
        DiffBuilder db = DiffBuilder
            .compare(Input.fromStream(expectedCneInputStream))
            .withTest(Input.fromStream(actualCneInputStream))
            .ignoreComments()
            .withNodeFilter(SweCneTest::shouldCompareNode);
        Diff d = db.build();

        if (d.hasDifferences()) {
            DefaultComparisonFormatter formatter = new DefaultComparisonFormatter();
            StringBuffer buffer = new StringBuffer();
            for (Difference ds : d.getDifferences()) {
                buffer.append(formatter.getDescription(ds.getComparison()) + "\n");
            }
            throw new AssertionError("There are XML differences in CNE files\n" + buffer);
        }
        assertFalse(d.hasDifferences());
    }

    private static boolean shouldCompareNode(Node node) {
        if (node.getNodeName().equals("mRID")) {
            // For the following fields, mRID is generated randomly as per the CNE specifications
            // We should not compare them with the test file
            return !node.getParentNode().getNodeName().equals("TimeSeries")
                && (!node.getParentNode().getNodeName().equals("Constraint_Series"));
        } else {
            return !(node.getNodeName().equals("createdDateTime"));
        }
    }
}