package org.iatoki.judgels.sandalphon.models.metas;

import org.iatoki.judgels.commons.models.metas.MetaModel;
import org.iatoki.judgels.sandalphon.models.domains.Problem;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Problem.class)
public abstract class MetaProblem extends MetaModel {

    public static volatile SingularAttribute<Problem, String> problemId;
    public static volatile SingularAttribute<Problem, String> title;

}

