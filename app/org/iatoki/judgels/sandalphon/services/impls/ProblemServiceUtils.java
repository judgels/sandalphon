package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.play.JidService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel;
import org.iatoki.judgels.sandalphon.models.entities.ProblemPartnerModel;

import java.util.Date;
import java.util.List;

final class ProblemServiceUtils {

    private ProblemServiceUtils() {
        // prevent instantiation
    }

    static ProblemType getProblemType(ProblemModel problemModel) {
        String prefix = JidService.getInstance().parsePrefix(problemModel.jid);

        if (prefix.equals("PROG")) {
            return ProblemType.PROGRAMMING;
        } else if (prefix.equals("BUND")) {
            return ProblemType.BUNDLE;
        } else {
            throw new IllegalStateException("Unknown problem type: " + prefix);
        }
    }

    static Problem createProblemFromModel(ProblemModel problemModel) {
        return new Problem(problemModel.id, problemModel.jid, problemModel.slug, problemModel.userCreate, problemModel.additionalNote, new Date(problemModel.timeUpdate), getProblemType(problemModel));
    }

    static ProblemPartner createProblemPartnerFromModel(ProblemPartnerModel problemPartnerModel) {
        return new ProblemPartner(problemPartnerModel.id, problemPartnerModel.problemJid, problemPartnerModel.userJid, problemPartnerModel.baseConfig, problemPartnerModel.childConfig);
    }

    static List<String> getOriginDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    static List<String> getClonesDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemClonesDirKey(), problemJid);
    }

    static List<String> getCloneDirPath(String userJid, String problemJid) {
        return appendPath(getClonesDirPath(problemJid), userJid);
    }

    static List<String> getRootDirPath(FileSystemProvider fileSystemProvider, String userJid, String problemJid) {
        List<String> origin =  getOriginDirPath(problemJid);
        List<String> root = getCloneDirPath(userJid, problemJid);

        if (userJid == null || !fileSystemProvider.directoryExists(root)) {
            return origin;
        } else {
            return root;
        }
    }

    static List<String> appendPath(List<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
