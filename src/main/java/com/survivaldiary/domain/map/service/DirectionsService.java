package com.survivaldiary.domain.map.service;

import com.survivaldiary.domain.map.client.TmapDirectionsClient;
import com.survivaldiary.domain.map.dto.DirectionsRequest;
import com.survivaldiary.domain.map.dto.DirectionsResponse;
import com.survivaldiary.global.exception.BusinessException;
import com.survivaldiary.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class DirectionsService {

    private final TmapDirectionsClient directionsClient;

    public DirectionsService(TmapDirectionsClient directionsClient) {
        this.directionsClient = directionsClient;
    }

    public DirectionsResponse findOptimalRoute(DirectionsRequest request) {
        validate(request);
        return directionsClient.findOptimalRoute(request);
    }

    private void validate(DirectionsRequest request) {
        if (request == null
                || !isLatitude(request.startLatitude())
                || !isLongitude(request.startLongitude())
                || !isLatitude(request.goalLatitude())
                || !isLongitude(request.goalLongitude())) {
            throw new BusinessException(ErrorCode.INVALID_MAP_FILTER);
        }
    }

    private boolean isLatitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= -90
                && value <= 90;
    }

    private boolean isLongitude(Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= -180
                && value <= 180;
    }
}
