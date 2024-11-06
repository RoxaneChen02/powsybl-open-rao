/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.Action;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public abstract class AbstractSingleNetworkElementActionAdderImpl<I> {
    protected NetworkActionAdderImpl ownerAdder;
    protected String id;
    protected String networkElementId;
    private String networkElementName;

    AbstractSingleNetworkElementActionAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    public I withId(String id) {
        this.id = id;
        return (I) this;
    }

    public I withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return (I) this;
    }

    public I withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return (I) this;
    }

    public NetworkActionAdder add() {
        assertAttributeNotNull(networkElementId, getActionTypeName(), "network element", "withNetworkElement()");
        assertSpecificAttributes();
        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        ownerAdder.addElementaryAction(buildAction(), networkElement);
        return ownerAdder;
    }

    protected abstract Action buildAction();

    protected abstract void assertSpecificAttributes();

    protected abstract String getActionTypeName();

    protected String createActionName(Object specificAttribute) {
        return id == null ? String.format("%s_%s_%s", getActionTypeName(), networkElementId, specificAttribute) : id;
    }
}
