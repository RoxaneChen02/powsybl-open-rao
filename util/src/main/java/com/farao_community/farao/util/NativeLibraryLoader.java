/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NativeLibraryLoader {

    private static volatile boolean nativeLibrariesLoaded = false;

    private NativeLibraryLoader() {
    }

    public static synchronized void loadNativeLibraries() {
        if (!nativeLibrariesLoaded) {
            try {
                System.loadLibrary("jniortools");
                nativeLibrariesLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                throw new FaraoException("Failed to load the library.");
            }
        }
    }
}