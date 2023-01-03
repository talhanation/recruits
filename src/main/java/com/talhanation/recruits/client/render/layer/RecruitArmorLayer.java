package com.talhanation.recruits.client.render.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class RecruitArmorLayer extends HumanoidModel {
    public RecruitArmorLayer(ModelPart p_170677_) {
        super(p_170677_);
    }

    public static LayerDefinition createOuterArmorLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
}
