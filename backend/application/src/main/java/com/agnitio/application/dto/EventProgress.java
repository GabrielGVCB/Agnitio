package com.agnitio.application.dto;

public record EventProgress(int total, int processed, int failed) {
    public boolean isCompleted() {
        return total > 0 && (processed + failed) >= total;
    }

    public double getPercentage() {
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(100.0, ((double) (processed + failed) / total) * 100.0);
    }
}
