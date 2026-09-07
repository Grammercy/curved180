// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SharedFrameUniformsTest {
    @Test void newlyActivatedViewsUseTheCurrentEnvironment() {
        var shared = new SharedFrameUniforms();
        shared.beginFrame();
        assertEquals(0.8f, shared.resolve("eyeBrightnessM", 0.8f, 1));
        assertEquals(0.8f, shared.resolve("eyeBrightnessM", 0.2f, 4));
        assertEquals(0.8f, shared.resolve("eyeBrightnessM", 0.9f, 0));
        shared.beginFrame();
        shared.resolve("eyeBrightnessM", 0.6f, 1);
        assertEquals(0.6f, shared.resolve("eyeBrightnessM", 0.3f, 4));
    }
    @Test void historyClearsAndViewDependentValuesStaySeparate() {
        var shared = new SharedFrameUniforms();
        shared.resolve("rainFactor", 1f, 1);
        shared.beginFrame();
        assertEquals(0.2f, shared.resolve("rainFactor", 0.2f, 2));
        shared.resolve("centerDepthSmooth", 1f, 1);
        assertEquals(0.1f, shared.resolve("centerDepthSmooth", 0.1f, 2));
    }
}

