package com.agnitio.application.ports.output;

import com.agnitio.application.dto.FaceMatchDto;

import java.util.List;
import java.util.UUID;

public interface FaceSearchClient {
    List<FaceMatchDto> searchFacesBySelfie(
            UUID eventId,
            byte[] selfieBytes,
            String fileName,
            double threshold,
            int limit
    );
}
