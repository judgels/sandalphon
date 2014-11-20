package org.iatoki.judgels.sandalphon.models.meta;

import org.iatoki.judgels.commons.models.meta.MetaModel;
import org.iatoki.judgels.sandalphon.models.schema.Problem;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Problem.class)
public abstract class MetaProblem extends MetaModel {

    public static volatile SingularAttribute<Problem, String> problemId;
    public static volatile SingularAttribute<Problem, String> title;

}

