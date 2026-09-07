// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProjectionPlanTest {
    @Test void horizonIsUnchangedAcrossSliderAndZoomRange() {
        for (int d=1; d<=360; d++) {
            var p=ProjectionPlan.create(d,3440,1440);
            assertEquals(Math.toRadians(d),p.horizontalRadians(),1e-12);
            for(int x=0;x<=40;x++) for(int y=0;y<=16;y++) {
                double u=x/40.0,v=y/16.0,theta=(u-.5)*p.horizontalRadians();
                double yy=(2*v-1)*p.verticalTangent(),n=Math.sqrt(1+yy*yy);
                assertArrayEquals(new double[]{Math.sin(theta)/n,yy/n,-Math.cos(theta)/n},p.ray(u,v),1e-12);
            }
        }
    }
    @Test void allRaysAndBlendedFacesFitAtEveryPitch() {
        for (int d : new int[]{1,30,60,61,90,110,180,270,360})
            for (int height : new int[]{720,1440,3440})
                for(int pitch=-90;pitch<=90;pitch++) {
                    var p=ProjectionPlan.create(d,3440,height,pitch);
                    for(int x=0;x<=40;x++) for(int y=0;y<=16;y++) {
                        double[] r=p.ray(x/40.0,y/16.0);
                        assertEquals(1,r[0]*r[0]+r[1]*r[1]+r[2]*r[2],1e-10);
                        if(!p.multiView()) {
                            assertTrue(-r[2]>0);
                            assertTrue(Math.abs(r[0]/r[2])<=p.captureTangent()+1e-10);
                            assertTrue(Math.abs(r[1]/r[2])<=p.captureTangent()+1e-10);
                        } else {
                            double max=Math.max(Math.abs(r[0]),Math.max(Math.abs(r[1]),Math.abs(r[2])));
                            for(int axis=0;axis<3;axis++) if(Math.abs(r[axis])>max*.9)
                                for(int other=0;other<3;other++)
                                    assertTrue(Math.abs(r[other]/r[axis])<=p.captureTangent());
                        }
                        assertArrayEquals(new double[]{0,0,-1},p.ray(.5,.5),1e-12);
                    }
                }
    }
    @Test void narrowingIsSmoothSymmetricAndDoesNotOverrideZoom() {
        double prior=360;
        for(double pitch=0;pitch<=90;pitch+=.05) {
            var p=ProjectionPlan.create(360,3440,1440,pitch);
            double d=Math.toDegrees(p.horizontalRadians());
            assertTrue(d<=prior+1e-10); assertTrue(prior-d<1);
            assertEquals(p.horizontalRadians(),ProjectionPlan.create(360,3440,1440,-pitch).horizontalRadians(),1e-12);
            prior=d;
        }
        assertEquals(100,Math.toDegrees(ProjectionPlan.create(360,3440,1440,90).horizontalRadians()),1e-10);
        assertEquals(30,Math.toDegrees(ProjectionPlan.create(30,3440,1440,90).horizontalRadians()),1e-10);
        assertEquals(Math.toRadians(420),ProjectionPlan.create(420,3440,1440).horizontalRadians(),1e-12);
    }
    @Test void rayDirectionsHaveNoJumpAcrossTransitionThresholds() {
        for(double pitch : new double[]{20,25,75,85}) for(int x=0;x<=30;x++) for(int y=0;y<=12;y++) {
            double[] a=ProjectionPlan.create(360,3440,1440,pitch-.001).ray(x/30.0,y/12.0);
            double[] b=ProjectionPlan.create(360,3440,1440,pitch+.001).ray(x/30.0,y/12.0);
            assertTrue(a[0]*b[0]+a[1]*b[1]+a[2]*b[2]>.999999);
        }
    }
}



