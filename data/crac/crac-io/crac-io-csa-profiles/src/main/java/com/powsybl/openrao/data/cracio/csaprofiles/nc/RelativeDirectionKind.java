/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.csaprofiles.nc;

/**
 * @author Jean-Pierre Arnould {@literal <jean-pierre.arnould at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum RelativeDirectionKind {
    NONE("none"),
    DOWN("down"),
    UP("up"),
    UP_AND_DOWN("upAndDown");

    RelativeDirectionKind(String name) {
        this.name = name;
    }

    private final String name;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "." + name;
    }
}