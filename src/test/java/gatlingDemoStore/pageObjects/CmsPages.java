package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

// class to hold the CMS page actions
public final class CmsPages{
    // LoadHomePage
    public static final ChainBuilder homePage =
            exec(
                    http("LoadHomePage")
                            .get("/")
                            .check(regex("<title>Gatling Demo-Store</title>"))
                            .check(css("#_csrf", "content").saveAs("csrfToken"))
            );

    // LoadAboutUsPage
    public static final ChainBuilder aboutUsPage =
            exec(
                    http("LoadAboutUsPage")
                            .get("/about-us")
                            .check(substring("About Us"))
                    //.check(regex("<h2>About Us</h2>"))
            );
}
