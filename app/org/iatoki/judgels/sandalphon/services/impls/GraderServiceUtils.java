package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.Grader;
import org.iatoki.judgels.sandalphon.models.entities.GraderModel;

final class GraderServiceUtils {

    private GraderServiceUtils() {
        // prevent instantiation
    }

    static Grader createGraderFromModel(GraderModel graderModel) {
        return new Grader(graderModel.id, graderModel.jid, graderModel.name, graderModel.secret);
    }
}
