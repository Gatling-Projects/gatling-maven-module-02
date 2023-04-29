package gatlingDemoStore;

import java.time.Duration;
import java.util.concurrent.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
//import net.sf.saxon.om.Chain;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import gatlingDemoStore.pageObjects.*;

public class DemoStoreSimulation extends Simulation {

  // Run time properties expected during test trigger. If not passed defaulted values will be used!
  private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "3"));
  private static final Duration RAMP_DURATION = Duration.ofSeconds(Integer.parseInt(System.getProperty("RAMP_DURATION", "10")));
  private static final Duration TEST_DURATION = Duration.ofSeconds(Integer.parseInt(System.getProperty("TEST_DURATION", "32")));

  private static final String domainURL = "demostore.gatling.io";
  private static final String protocol = "https";
  private static final HttpProtocolBuilder httpProtocol = http.baseUrl(protocol + "://" + domainURL);

  // define before and after steps for the test

  @Override
  public void before(){
    System.out.println("Load test starting...");
    System.out.println("Running with " + USER_COUNT + " users");
    System.out.println("Test Duration: " + TEST_DURATION.getSeconds() + " secs");
  }
  @Override
  public void after(){
    System.out.println("Load test completed...");
  }

  // init session - to declare session level variables, clear cache, drop cookies etc.
  private static final ChainBuilder initSession =
    exec(flushHttpCache())
    .exec(flushCookieJar())
    .exec(session -> session.set("randomNum", ThreadLocalRandom.current().nextInt()))
    .exec(session -> session.set("customerLoggedIn", false))
    .exec(session -> session.set("totalCartValue", 0.00))
    .exec(addCookie(Cookie("sessionId", "sid-#{randomNum}").withDomain(domainURL)));

  private static final ScenarioBuilder scn = 
    scenario("DemoStoreSimulation")
    .exec(initSession)
    .exec(CmsPages.homePage)
    .pause(2)
    .exec(CmsPages.aboutUsPage)
    .pause(2)
    .exec(Catalog.Category.view)
    .pause(2)
    .exec(Catalog.Product.addToCart)
    .pause(2)
    // add a second product
    .exec(Catalog.Product.addToCart)
    .pause(2)
    .exec(Cart.viewCart)
    .pause(2)
    .exec(Cart.checkoutCart);

  private static class UserJourneys {
    private static final Duration MIN_PAUSE = Duration.ofMillis(100);
    private static final Duration MAX_PAUSE = Duration.ofMillis(500);

    private static final ChainBuilder browseCart = 
      exec(initSession)
      .exec(CmsPages.homePage)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(CmsPages.aboutUsPage)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .repeat(5)
        .on(
          exec(Catalog.Category.view)
          .pause(MIN_PAUSE, MAX_PAUSE)
          .exec(Catalog.Product.view)
        );

    private static final ChainBuilder abandonCart = 
      exec(initSession)
      .exec(CmsPages.homePage)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Category.view)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Product.view)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Product.addToCart);
      
    private static final ChainBuilder completePurchase = 
      exec(initSession)
      .exec(CmsPages.homePage)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Category.view)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Product.view)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Catalog.Product.addToCart)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Cart.viewCart)
      .pause(MIN_PAUSE, MAX_PAUSE)
      .exec(Cart.checkoutCart);
  }

  private static final class Scenarios {
    private static final ScenarioBuilder regularPurchase =
      scenario("Regular Purchase Scenario")
      .during(TEST_DURATION)
      .on(
        randomSwitch()
        .on(
          Choice.withWeight(75.0, exec(UserJourneys.browseCart)),
          Choice.withWeight(15.0, exec(UserJourneys.abandonCart)),
          Choice.withWeight(10.0, exec(UserJourneys.completePurchase))
        )
      );

    private static final ScenarioBuilder highPurchase =
      scenario("High Purchase Scenario")
      .during(Duration.ofSeconds(65))
      .on(
        randomSwitch()
        .on(
          Choice.withWeight(25.0, exec(UserJourneys.browseCart)),
          Choice.withWeight(25.0, exec(UserJourneys.abandonCart)),
          Choice.withWeight(50.0, exec(UserJourneys.completePurchase))
        )
      );
  }
  
  {
	  /* Regular Simulation as in case of an open work model:*/
    /*setUp(
      scn.injectOpen(
        atOnceUsers(3),
        nothingFor(5),
        rampUsers(10).during(20),
        nothingFor(10),
        constantUsersPerSec(1).during(20)
      )
    ).protocols(httpProtocol);
    */

    /* Closed work model simulation */
    /*setUp(
      scn.injectClosed(
        constantConcurrentUsers(5).during(10),
        rampConcurrentUsers(1).to(5).during(40)
      )
    ).protocols(httpProtocol);*/

    /* Throttle simulation: when you want to cap your throughput in the load 
        - once requests/sec reaches a defines limit, the excess load is pushed to an unbounded queue  
        - beware of OOM exceptions! 
        - can be applied globally as below or on each inject profiles separately */
    /*setUp(
      scn.injectOpen(
        constantUsersPerSec(1).during(180)
      )
    )
    .protocols(httpProtocol)
    .throttle(
      reachRps(5).in(Duration.ofSeconds(30)),
      holdFor(Duration.ofSeconds(60)),
      jumpToRps(10),
      holdFor(Duration.ofSeconds(60))
    )
    .maxDuration(Duration.ofSeconds(180));*/

    /* Simulation design with run time parameters */
    /*setUp(
      Scenarios.regularPurchase.injectOpen(
        rampUsers(USER_COUNT).during(RAMP_DURATION)
      ).protocols(httpProtocol)
    );*/

    /* Simulation design: sequential simulation + run time params used */
    /*setUp(
      Scenarios.regularPurchase.injectOpen(
        rampUsers(USER_COUNT).during(RAMP_DURATION)
      )
      .andThen(
        Scenarios.highPurchase.injectOpen(
          rampUsers(2).during(10)
        )
      )
    ).protocols(httpProtocol);*/

    /* Simulation design: parallel simulation + run time params used */
    setUp(
      Scenarios.regularPurchase.injectOpen(
        rampUsers(USER_COUNT).during(RAMP_DURATION)
      ),
      Scenarios.highPurchase.injectOpen(
        rampUsers(2).during(Duration.ofSeconds(3))
      )
    ).protocols(httpProtocol);
  }
}
