package com.wdcftgg.witherstormmod.client.model.witherstorm.mass;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.CubeDeformation;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.CubeListBuilder;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartDefinition;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartPose;



public class LowResSegmentBodyModel {
    public static PartDefinition createBodyModel(PartDefinition root, float texScale) {
        CubeListBuilder builder0 = CubeListBuilder.m_171558_();
        builder0.m_171514_(17, 40);
        builder0.m_171496_(-6.0f, -0.5f, -10.5f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.0f), texScale, texScale);
        builder0.m_171514_(17, 40);
        builder0.m_171496_(-3.0f, -0.5f, -10.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f), texScale, texScale);
        builder0.m_171514_(17, 40);
        builder0.m_171496_(0.0f, -0.5f, -10.5f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.0f), texScale, texScale);
        builder0.m_171514_(17, 40);
        builder0.m_171496_(-3.0f, 0.5f, -10.5f, 5.0f, 2.0f, 1.0f, new CubeDeformation(0.0f), texScale, texScale);
        PartDefinition body = root.m_171599_("lowResMass", builder0, PartPose.m_171419_((float)2.0f, (float)-4.0f, (float)0.0f));
        CubeListBuilder builder1 = CubeListBuilder.m_171558_();
        builder1.m_171514_(0, 24);
        builder1.m_171496_(-3.0f, -10.0f, -6.0f, 7.0f, 9.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part1", builder1, PartPose.m_171423_((float)0.0f, (float)2.5f, (float)-0.5f, (float)0.0f, (float)0.2618f, (float)0.0436f));
        CubeListBuilder builder2 = CubeListBuilder.m_171558_();
        builder2.m_171514_(0, 24);
        builder2.m_171496_(-7.0f, -4.3f, -9.0f, 12.0f, 5.0f, 2.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part2", builder2, PartPose.m_171423_((float)0.0f, (float)2.5f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        CubeListBuilder builder3 = CubeListBuilder.m_171558_();
        builder3.m_171514_(0, 24);
        builder3.m_171496_(-7.0f, -7.0f, -7.0f, 14.0f, 9.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part3", builder3, PartPose.m_171423_((float)0.0f, (float)2.5f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)-0.2182f));
        CubeListBuilder builder4 = CubeListBuilder.m_171558_();
        builder4.m_171514_(0, 24);
        builder4.m_171496_(-2.1745f, -3.0038f, 4.0f, 5.0f, 7.0f, 6.0f, new CubeDeformation(0.0f), texScale, texScale);
        builder4.m_171514_(0, 24);
        builder4.m_171496_(-2.1745f, -6.0038f, -1.0f, 6.0f, 7.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part4", builder4, PartPose.m_171423_((float)0.0f, (float)0.5f, (float)-0.5f, (float)0.0f, (float)0.2618f, (float)0.1309f));
        CubeListBuilder builder5 = CubeListBuilder.m_171558_();
        builder5.m_171514_(0, 24);
        builder5.m_171496_(-10.0f, -5.0f, -3.2f, 5.0f, 6.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part5", builder5, PartPose.m_171423_((float)0.0f, (float)4.5f, (float)-0.5f, (float)-2.4771f, (float)1.2923f, (float)-2.5016f));
        CubeListBuilder builder6 = CubeListBuilder.m_171558_();
        builder6.m_171514_(0, 24);
        builder6.m_171496_(-10.0f, -5.0f, -5.0f, 11.0f, 6.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part6", builder6, PartPose.m_171423_((float)0.0f, (float)4.5f, (float)-0.5f, (float)-0.2618f, (float)0.0f, (float)-0.0436f));
        CubeListBuilder builder7 = CubeListBuilder.m_171558_();
        builder7.m_171514_(0, 24);
        builder7.m_171496_(-4.0f, -5.0f, 0.0f, 11.0f, 7.0f, 7.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part7", builder7, PartPose.m_171423_((float)0.0f, (float)4.5f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        CubeListBuilder builder8 = CubeListBuilder.m_171558_();
        builder8.m_171514_(0, 24);
        builder8.m_171496_(-1.5f, -9.0f, -0.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f), texScale, texScale);
        body.m_171599_("part8", builder8, PartPose.m_171423_((float)2.5f, (float)11.5f, (float)10.0f, (float)0.7854f, (float)0.0f, (float)0.0f));
        return body;
    }
}
