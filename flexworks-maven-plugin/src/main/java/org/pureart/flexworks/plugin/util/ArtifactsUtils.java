package org.pureart.flexworks.plugin.util;

import java.util.Collection;
import org.apache.maven.artifact.Artifact;

/**
 * @author pureart.org
 */
public class ArtifactsUtils {

    /**
     * Searchs the artifact which's given in the <code>artifacts</code>
     * 
     * @param artifacts
     * @param groupId
     * @param artifactId
     * @param type
     * @param version
     * @param classifier
     * @return artifact
     */
    public static Artifact searchFor(Collection<Artifact> artifacts,
            String groupId, String artifactId, String version, String type,
            String classifier) {
        for (Artifact artifact : artifacts) {
            if (equals(artifact.getGroupId(), groupId)
                    && equals(artifact.getArtifactId(), artifactId)
                    && equals(artifact.getVersion(), version)
                    && equals(artifact.getType(), type)
                    && equals(artifact.getClassifier(), classifier)) {
                return artifact;
            }
        }

        return null;
    }

    /**
     * @param str1
     * @param str2
     * @return
     */
    private static boolean equals(String str1, String str2) {
        // If is null is not relevant
        if (str1 == null || str2 == null) {
            return true;
        }

        return str1.equals(str2);
    }

}
