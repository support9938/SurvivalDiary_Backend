package com.survivaldiary.domain.map.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NaverPlaceSearchClientTest {

    @Test
    void convertsScaledWgs84CoordinatesReturnedByLocalSearch() {
        assertThat(NaverPlaceSearchClient.normalizeCoordinate(1_269_873_882d))
                .isEqualTo(126.9873882d);
        assertThat(NaverPlaceSearchClient.normalizeCoordinate(375_666_103d))
                .isEqualTo(37.5666103d);
    }

    @Test
    void keepsDecimalWgs84CoordinatesUnchanged() {
        assertThat(NaverPlaceSearchClient.normalizeCoordinate(129.0592d))
                .isEqualTo(129.0592d);
        assertThat(NaverPlaceSearchClient.normalizeCoordinate(35.1578d))
                .isEqualTo(35.1578d);
    }
}
