/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;

import static com.farao_community.farao.data.crac_creation_util.UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;

/**
 * A utility class, that stores network information so as to speed up
 * the identification of Ucte branches within a Iidm network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelper {

    private Network network;
    private UcteConnectableCollection connectablesInNetwork;
    private UcteNetworkHelperProperties properties;

    public UcteNetworkHelper(Network network, UcteNetworkHelperProperties properties) {
        if (!network.getSourceFormat().equals("UCTE")) {
            throw new IllegalArgumentException("UcteNetworkHelper can only be used for an UCTE network");
        }
        this.network = network;
        this.properties = properties;
        this.connectablesInNetwork = new UcteConnectableCollection(network);
    }

    public Network getNetwork() {
        return network;
    }

    public UcteNetworkHelperProperties getProperties() {
        return properties;
    }

    public UcteMatchingResult findNetworkElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix);
    }

    private String completeNodeName(String nodeName) {
        if (nodeName.length() == UcteUtils.UCTE_NODE_LENGTH) {
            return nodeName;
        } else if (properties.getBusIdMatchPolicy().equals(COMPLETE_WITH_WILDCARDS)) {
            return String.format("%1$-7s", nodeName) + UcteUtils.WILDCARD_CHARACTER;
        } else {
            return String.format("%1$-8s", nodeName);
        }
    }
}