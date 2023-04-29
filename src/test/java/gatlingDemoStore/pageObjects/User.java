package gatlingDemoStore.pageObjects;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

// class to hold the User related actions
public final class User{
    //feeder to provide the login details
    private static final FeederBuilder<String> loginFeeder =
            csv("data/loginDetails.csv").circular();

    // UserLogin
    public static final ChainBuilder login =
            feed(loginFeeder)
                    .exec(
                            http("LoadLoginPage")
                                    .get("/login")
                                    .check(substring("Username:"))
                    )
                    .exec(
                            session -> {
                                System.out.println("customerLoggedIn: " + session.get("customerLoggedIn").toString());
                                return session;
                            }
                    )
                    .exec(
                            http("UserLogin")
                                    .post("/login")
                                    .formParam("_csrf", "#{csrfToken}")
                                    .formParam("username", "#{username}")
                                    .formParam("password", "#{password}")
                    )
                    .exec(session -> session.set("customerLoggedIn", true))
                    .exec(
                            session -> {
                                System.out.println("customerLoggedIn: " + session.get("customerLoggedIn").toString());
                                return session;
                            }
                    );
}