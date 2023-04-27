package gatlingDemoStore;

import java.util.concurrent.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class DemoStoreSimulation extends Simulation {

  private static final String domainURL = "demostore.gatling.io";
  private static final String protocol = "https";

  private static final HttpProtocolBuilder httpProtocol = http.baseUrl(protocol + "://" + domainURL);

  // feeder to provide categories endpoint in a random manner
  private static final FeederBuilder<String> categoryFeeder =
  csv("data/categories.csv").random();

  // feeder to provide product details endpoints in random
  private static final FeederBuilder<Object> prodDetailsFeeder =
  jsonFile("data/prodDetails.json").random();

  //feeder to provide the login details
  private static final FeederBuilder<String> loginFeeder =
  csv("data/loginDetails.csv").circular();

  // init session - to declare session level variables, clear cache, drop cookies etc.
  private static final ChainBuilder initSession =
    exec(flushHttpCache())
    .exec(flushCookieJar())
    .exec(session -> session.set("randomNum", ThreadLocalRandom.current().nextInt()))
    .exec(session -> session.set("customerLoggedIn", false))
    .exec(session -> session.set("totalCartValue", 0.00))
    .exec(addCookie(Cookie("sessionId", "sid-#{randomNum}").withDomain(domainURL)));

  // class to hold the CMS page actions
  private static class cmsPages{
    // LoadHomePage
    private static final ChainBuilder homePage =
      exec(
        http("LoadHomePage")
        .get("/")
        .check(regex("<title>Gatling Demo-Store</title>"))
        .check(css("#_csrf", "content").saveAs("csrfToken"))
      );
    
    // LoadAboutUsPage
    private static final ChainBuilder aboutUsPage =
      exec(
        http("LoadAboutUsPage")
        .get("/about-us")
        .check(substring("About Us"))
        //.check(regex("<h2>About Us</h2>"))
      );

  }

  // class to hold the catalog related actions
  private static class Catalog{
    private static class Category{
      // LoadCategoriesPage
      private static final ChainBuilder view =
        feed(categoryFeeder)
        .exec(
          http("LoadCategoriesPage - #{categoryName}")
          .get("/category/#{categoryUrl}")
          .check(css("#CategoryName").isEL("#{categoryName}"))
        );
    }

    private static class Product{
      // LoadProductDetailsPage
      private static final ChainBuilder view =
        feed(prodDetailsFeeder)
        .exec(
          http("LoadProductDetailsPage-#{name}")
          .get("/product/#{slug}")
          .check(css("#ProductDescription").isEL("#{description}"))
        );

      // AddProductToCart
      private static final ChainBuilder addToCart =
      exec(view)
      .exec(
        http("AddProductToCart-#{name}")
        .get("/cart/add/#{id}")
        .check(substring("items in your cart."))
      )
      .exec(session -> {
          double newTotalCartValue = session.getDouble("totalCartValue") + session.getDouble("price");
          System.out.println("newCartValue: " + newTotalCartValue);
          return session.set("totalCartValue", newTotalCartValue);
        }
      );
    }
  }

  // class to hold the cart related actions
  private static class Cart{
    // ViewCart
    private static final ChainBuilder viewCart =
    doIf(session -> !session.getBoolean("customerLoggedIn"))
      .then(exec(User.login))
    .exec(
      http("ViewCart")
      .get("/cart/view")
      .check(css("#grandTotal").isEL("$#{totalCartValue}"))
    ); 

    // CheckoutCart
    private static final ChainBuilder checkoutCart =
      exec(
        http("CheckoutCart")
        .get("/cart/checkout")
        .check(substring("Thanks for your order! See you soon!"))
      );
  }

  // class to hold the User related actions
  private static class User{
    // UserLogin
    private static final ChainBuilder login =
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

  private static final ScenarioBuilder scn = scenario("DemoStoreSimulation")
    .exec(initSession)
    .exec(cmsPages.homePage)
    .pause(2)
    .exec(cmsPages.aboutUsPage)
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

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
