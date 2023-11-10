package zju.cst.aces.logger;

import zju.cst.aces.api.Logger;
import org.apache.maven.plugin.logging.Log;

public class MavenLogger implements Logger {

    Log log;

    public MavenLogger(Log log) {
        this.log = log;
    }

    @Override
    public void info(String msg) {
        log.info(msg);
    }

    @Override
    public void warn(String msg) {
        log.warn(msg);
    }

    @Override
    public void error(String msg) {
        log.error(msg);
    }

    @Override
    public void debug(String msg) {
        log.debug(msg);
    }

}
