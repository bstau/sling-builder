package org.apache.sling.jcr.contentloader.it;

import org.apache.sling.jcr.contentloader.internal.BundleContentLoader;

import javax.jcr.Session;

public class ConditionFactory {
    private static Session s;

    public static RetryLoop.Condition expectPropertyValue(
            final String itemPath, final String propertyName, final String propertyValue){

        return new RetryLoop.Condition() {
            public String getDescription() {
                return "Checking that property is equal to value";
            }

            public boolean isTrue() throws Exception {
                //Since uninstall and overwrite parameters are not set, content should not be deleted
                return BundleContentLoader.isIdle() &&
                        s.getNode(itemPath).getProperty(propertyName).getString().equals(propertyValue);
            }
        };
    }

    public static RetryLoop.Condition noItemByPath(final String itemPath){

        return new RetryLoop.Condition() {
            public String getDescription() {
                return "Checking that node was deleted";
            }

            public boolean isTrue() throws Exception {
                return BundleContentLoader.isIdle() && !s.itemExists(itemPath);
            }
        };
    }

    public static RetryLoop.Condition expectItemByPath(final String itemPath){
        return new RetryLoop.Condition() {

            public String getDescription() {
                return "Checking that node was not deleted";
            }

            public boolean isTrue() throws Exception {
                return BundleContentLoader.isIdle() && s.itemExists(itemPath);
            }
        };
    }

    public static void setSession(Session session){
        s = session;
    }
}
