package com.bloxbean.cardano.yacicli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.shell.ShellRunner;
import org.springframework.shell.jline.InteractiveShellRunner;
import org.springframework.shell.jline.NonInteractiveShellRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(-100)
public class CustomShellRunner implements ShellRunner {
    private final NonInteractiveShellRunner nonInteractiveShellRunner;
    private final InteractiveShellRunner interactiveShellRunner;

    @Override
    public boolean canRun(ApplicationArguments args) {
        return true;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean canRun = nonInteractiveShellRunner.canRun(args);
        if (canRun)
            nonInteractiveShellRunner.run(args);

        var interactive = args.containsOption("i") || args.containsOption("interactive");

        if (interactive) {
            interactiveShellRunner.run(args);
        }
    }
}
