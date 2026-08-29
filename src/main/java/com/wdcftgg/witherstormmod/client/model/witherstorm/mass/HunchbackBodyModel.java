package com.wdcftgg.witherstormmod.client.model.witherstorm.mass;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.CubeDeformation;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.CubeListBuilder;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartDefinition;
import com.wdcftgg.witherstormmod.client.model.witherstorm.ModelBuilders.PartPose;



public class HunchbackBodyModel {
    public static PartDefinition createBodyModel(PartDefinition root, float texScale) {
        CubeListBuilder builder0 = CubeListBuilder.m_171558_();
        builder0.m_171514_(0, 148);
        builder0.m_171488_(-2.0f, -18.0f, 1.0f, 6.0f, 6.0f, 6.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 148);
        builder0.m_171488_(-1.0f, -22.0f, 0.0f, 6.0f, 6.0f, 6.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 148);
        builder0.m_171488_(-8.0f, -20.0f, 1.0f, 6.0f, 6.0f, 6.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 152);
        builder0.m_171488_(-2.0f, -13.0f, 2.0f, 4.0f, 4.0f, 4.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 152);
        builder0.m_171488_(-5.0f, -15.0f, 2.0f, 4.0f, 4.0f, 4.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 152);
        builder0.m_171488_(-2.0f, -11.0f, 1.0f, 4.0f, 4.0f, 4.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 152);
        builder0.m_171488_(4.0f, -19.0f, 2.0f, 4.0f, 4.0f, 4.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(1.0f, -12.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(-4.0f, -12.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(2.0f, -17.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(7.0f, -19.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(-9.0f, -21.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(5.0f, -22.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 154);
        builder0.m_171488_(-4.0f, -21.0f, 2.0f, 3.0f, 3.0f, 3.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(-1.0f, -7.0f, 2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(10.0f, -20.0f, 2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(-10.0f, -19.0f, 2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(-6.0f, -15.0f, 1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(-6.0f, -22.0f, 2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(4.0f, -18.0f, 5.0f, 1.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        builder0.m_171514_(0, 156);
        builder0.m_171488_(8.0f, -21.0f, 2.0f, 2.0f, 2.0f, 2.0f, CubeDeformation.f_171458_);
        return root.m_171599_("mass", builder0, PartPose.m_171419_((float)0.0f, (float)24.0f, (float)0.0f));
    }
}
