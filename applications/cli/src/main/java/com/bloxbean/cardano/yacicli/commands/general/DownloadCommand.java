package com.bloxbean.cardano.yacicli.commands.general;

import com.bloxbean.cardano.yacicli.commands.common.DownloadService;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@ShellComponent
@ShellCommandGroup(Groups.GENERAL_CMD_GROUP)
@RequiredArgsConstructor
public class DownloadCommand {
    private final DownloadService downloadService;

    @ShellMethod(value = "Download", key = "download")
    @ShellMethodAvailability("generalCmdAvailability")
    public void download(
            @ShellOption(value = {"--component", "-c"}, defaultValue = "all",  help = "node,ogmios,kupo,yaci-store,yaci-store-jar") String component,
            @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing installation. default: false") boolean overwrite
            ) {

        try {

            if (component == null) {
                writeLn(error("Component is not provided. Please provide the component to download"));
                return;
            }

            if (component.equals("node")) {
                downloadService.downloadNode(overwrite);
            } else if (component.equals("yaci-store")) {
                downloadService.downloadYaciStoreNative(overwrite);
            } else if (component.equals("yaci-store-jar")) {
                downloadService.downloadYaciStoreJar(overwrite);
            } else if (component.equals("ogmios")) {
                downloadService.downloadOgmios(overwrite);
            } else if (component.equals("kupo")) {
                downloadService.downloadKupo(overwrite);
            } else if (component.equals("all")) {
                downloadService.downloadNode(overwrite);
                downloadService.downloadYaciStoreNative(overwrite);
                downloadService.downloadOgmios(overwrite);
                downloadService.downloadKupo(overwrite);
            } else {
                writeLn(error("Invalid component : " + component));
            }

        } catch (Exception e) {
            writeLn(error("Error downloading component : " + e.getMessage()));
        }
    }
}
