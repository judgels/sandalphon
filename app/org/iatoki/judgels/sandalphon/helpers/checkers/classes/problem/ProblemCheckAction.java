package org.iatoki.judgels.sandalphon.helpers.checkers.classes.problem;

import org.iatoki.judgels.sandalphon.helpers.checkers.interfaces.problem.ProblemCheck;
import org.iatoki.judgels.commons.models.dao.DaoSingletons;
import org.iatoki.judgels.sandalphon.models.dao.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.schema.Problem;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public final class ProblemCheckAction extends Action<ProblemCheck> {

    @Override
    public F.Promise<Result> call(Http.Context context) throws Throwable {
        if (context.args.get("problem") != null) {
            return delegate.call(context);
        } else  {
            String path = context.request().path();
            try {
                String problemUrl = "problem/";
                int problemPos = path.indexOf(problemUrl) + problemUrl.length();
                String startId = path.substring(problemPos);
                String problemId = path.substring(problemPos, problemPos + ((Math.min(startId.indexOf('/'), startId.length()) <= 0) ? startId.length() : Math.min(startId.indexOf('/'), startId
                        .length())));
                if (problemId != null) {
                    Problem problem = DaoSingletons.getInstance().getDao(ProblemDao.class).findById(problemId);
                    if (problem != null) {
                        context.args.put("problem", problem);
                        return delegate.call(context);
                    } else {
                        return F.Promise.<Result>pure(notFound("<h1>Page not found</h1>"));
                    }
                } else {
                    return F.Promise.<Result>pure(notFound("<h1>Page not found</h1>"));
                }
            } catch (NumberFormatException e) {
                return F.Promise.<Result>pure(notFound("<h1>Page not found</h1>"));
            }
        }
    }

}
