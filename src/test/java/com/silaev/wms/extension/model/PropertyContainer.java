package com.silaev.wms.extension.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyContainer {
    private MongoReplicaSetProperties mongoReplicaSetProperties;
}