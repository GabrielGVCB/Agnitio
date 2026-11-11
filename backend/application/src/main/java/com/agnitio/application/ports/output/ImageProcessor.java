package com.agnitio.application.ports.output;

import com.agnitio.application.dto.ProcessedVariants;

public interface ImageProcessor {
    ProcessedVariants process(byte[] originalImageBytes);
}
