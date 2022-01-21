/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.range.RangeType;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class StandardRangeImplTest {

    private final double min = 1;
    private final double max = 32;

    private StandardRangeImpl fixedRange;

    @Before
    public void setUp() {
        fixedRange = new StandardRangeImpl(min, max);
    }

    @Test
    public void getMinTest() {
        assertEquals(min, fixedRange.getMin(), 1e-6);
    }

    @Test
    public void getMaxTest() {
        assertEquals(max, fixedRange.getMax(), 1e-6);
    }

    @Test
    public void getRangeTypeTest() {
        assertEquals(RangeType.ABSOLUTE, fixedRange.getRangeType());
    }

    @Test
    public void testEquals() {
        StandardRangeImpl range1 = new StandardRangeImpl(0, 10);
        StandardRangeImpl range2 = new StandardRangeImpl(0, 10);
        StandardRangeImpl range3 = new StandardRangeImpl(0, 11);

        assertEquals(range1, range2);
        assertNotEquals(range1, range3);
    }

    @Test
    public void testHashCode() {
        StandardRangeImpl range1 = new StandardRangeImpl(0, 10);
        StandardRangeImpl range2 = new StandardRangeImpl(0, 10);
        StandardRangeImpl range3 = new StandardRangeImpl(0, 11);

        assertEquals(range1.hashCode(), range2.hashCode());
        assertNotEquals(range1.hashCode(), range3.hashCode());
    }
}