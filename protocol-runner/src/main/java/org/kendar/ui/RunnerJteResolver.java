package org.kendar.ui;

import org.kendar.di.annotations.TpmService;

@TpmService
public class RunnerJteResolver extends JteResolver {
    public RunnerJteResolver() {
        super(RunnerJteResolver.class.getClassLoader());
    }
}
