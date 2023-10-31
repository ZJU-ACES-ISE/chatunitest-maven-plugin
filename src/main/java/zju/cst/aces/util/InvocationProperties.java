package zju.cst.aces.util;

import org.apache.maven.shared.invoker.InvocationRequest;

import java.util.Properties;

public class InvocationProperties {
    public static InvocationRequest setSkipProperties(InvocationRequest request) {
        Properties properties = new Properties();

        properties.setProperty("gpg.skip", "true");
        properties.setProperty("enforcer.skip", "true");
        properties.setProperty("license.skip", "true");
        properties.setProperty("sortpom.skip", "true");
        properties.setProperty("maven.javadoc.skip", "true");
        properties.setProperty("checkstyle.skip", "true");
        properties.setProperty("animal.sniffer.skip", "true");
        properties.setProperty("cobertura.skip", "true");
        properties.setProperty("rat.skip", "true");
        properties.setProperty("dependencyVersionsCheck.skip", "true");

        Properties existingProperties = request.getProperties();
        if (existingProperties != null) {
            existingProperties.putAll(properties);
            request.setProperties(existingProperties);
        } else {
            request.setProperties(properties);
        }

        return request;
    }
}
