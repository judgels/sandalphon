package org.iatoki.judgels.sandalphon.helpers.checkers.interfaces.problem;

import org.iatoki.judgels.sandalphon.helpers.checkers.classes.problem.ProblemCheckAction;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@With(ProblemCheckAction.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProblemCheck {
}
